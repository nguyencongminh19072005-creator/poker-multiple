package com.poker.model.game;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.game.GameSessionDao;
import com.poker.model.game.betting.BettingAction;
import com.poker.model.game.betting.LegalActions;
import com.poker.model.game.betting.PokerActionType;
import com.poker.model.game.card.Card;
import com.poker.model.game.card.Deck;
import com.poker.model.game.hand.HandEvaluator;
import com.poker.model.game.round.BettingRoundState;
import com.poker.model.game.round.PokerRoundEngine;
import com.poker.model.game.settlement.HandSettlementEngine;
import com.poker.model.game.settlement.HandSettlementResult;
import com.poker.model.game.settlement.PotAward;
import com.poker.model.game.state.GamePhase;
import com.poker.model.game.state.GameState;
import com.poker.model.game.state.PokerPlayer;
import com.poker.model.game.state.PokerPlayerState;
import com.poker.model.room.RoomOperations;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Authoritative single-process hand coordinator; no transport or SQL parsing belongs here. */
public final class GameOperations {
    private final GameSessionDao sessions;
    private final RoomOperations rooms;
    private final ConcurrentMap<UUID, RunningHand> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, UUID> byRoom = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, UUID> lastFinishedByRoom = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Long> nextHandAtNanos = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Integer> lastDealerSeat = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Long> handCounts = new ConcurrentHashMap<>();
    private final Set<Long> disconnectedUsers = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, Object> roomLocks = new ConcurrentHashMap<>();

    public GameOperations(JdbcDatabase database) {
        sessions = new GameSessionDao(database);
        rooms = new RoomOperations(database);
    }

    public void abortOrphanedGames() throws SQLException { sessions.abortOrphanedGames(); }

    public UUID start(long roomId, long hostId) throws SQLException {
        synchronized (lockFor(roomId)) {
            if (nextHandAtNanos.containsKey(roomId))
                throw new IllegalArgumentException("Ván tiếp theo sẽ tự bắt đầu sau 5 giây");
            return startHand(roomId, data -> validateStart(data, hostId));
        }
    }

    private UUID startHand(long roomId, GameSessionDao.StartValidator validator) throws SQLException {
        if (byRoom.containsKey(roomId)) throw new IllegalArgumentException("Phòng đang có ván bài");
        UUID gameId = UUID.randomUUID();
        GameSessionDao.StartData data = sessions.start(roomId, gameId, validator);
        try {
            Set<Long> members = ConcurrentHashMap.newKeySet();
            rooms.members(roomId).forEach(member -> members.add(member.userId()));
            RunningHand hand = createHand(gameId, data, members);
            byId.put(gameId, hand);
            byRoom.put(roomId, gameId);
            hand.settleIfComplete();
            lastDealerSeat.put(roomId, hand.state.dealerPosition());
            handCounts.put(roomId, hand.handNumber);
            return gameId;
        } catch (RuntimeException | SQLException failure) {
            sessions.abort(gameId, roomId);
            byId.remove(gameId);
            byRoom.remove(roomId);
            throw failure;
        }
    }

    public UUID activeForRoom(long roomId, long userId) throws SQLException {
        UUID id = byRoom.get(roomId);
        RunningHand hand = id == null ? null : byId.get(id);
        return hand != null && hand.active && allowed(hand, userId) ? id : null;
    }

    public Long activeRoomForUser(long userId) {
        for (RunningHand hand : byId.values()) {
            if (hand.active && hand.members.contains(userId)) return hand.roomId;
        }
        return null;
    }

    public long roomForGame(UUID gameId) { return requireHand(gameId).roomId; }

    public record TurnTick(UUID gameId, long roomId, UUID turnId,
                           long remainingSeconds, boolean stateChanged, boolean finished,
                           boolean started, boolean cancelled) { }

    /** Advance expired turns on the server; the client timer is display-only. */
    public List<TurnTick> tickTurns() throws SQLException {
        List<TurnTick> ticks = new ArrayList<>();
        for (RunningHand hand : byId.values()) {
            synchronized (lockFor(hand.roomId)) {
                if (!hand.active || hand.state.currentTurnUserId() == null) continue;
                hand.syncTurnDeadline();
                boolean changed = false;
                if (System.nanoTime() - hand.turnDeadlineNanos >= 0) {
                    long actorId = hand.state.currentTurnUserId();
                    LegalActions legal = hand.engine.legalActionsForAutomaticAction(
                            hand.state, hand.round, actorId);
                    BettingAction action = legal.allows(PokerActionType.CHECK)
                            ? BettingAction.check(actorId, hand.state.turnId())
                            : BettingAction.fold(actorId, hand.state.turnId());
                    hand.engine.actAutomaticAction(hand.state, hand.round, action, hand.deck);
                    hand.lastAction.put(actorId, action.type().name());
                    hand.automateDisconnectedTurns();
                    hand.settleIfComplete();
                    hand.syncTurnDeadline();
                    changed = true;
                }
                UUID turnId = hand.state.turnId();
                long remaining = turnId == null ? 0 : Math.max(0,
                        TimeUnit.NANOSECONDS.toSeconds(Math.max(0,
                                hand.turnDeadlineNanos - System.nanoTime()) + 999_999_999L));
                ticks.add(new TurnTick(hand.state.gameId(), hand.roomId, turnId,
                        remaining, changed, !hand.active, false, false));
            }
        }
        for (var pending : nextHandAtNanos.entrySet()) {
            long roomId = pending.getKey();
            synchronized (lockFor(roomId)) {
                if (!pending.getValue().equals(nextHandAtNanos.get(roomId))
                        || System.nanoTime() - pending.getValue() < 0) continue;
                if (byRoom.containsKey(roomId)) continue;
                try {
                    nextHandAtNanos.remove(roomId);
                    UUID nextId = startHand(roomId, this::validateAutomaticStart);
                    ticks.add(new TurnTick(nextId, roomId, null, 0, true, false, true, false));
                } catch (IllegalArgumentException noLongerReady) {
                    // Someone left, lost their seat, or ran out of chips during the pause.
                    ticks.add(new TurnTick(lastFinishedByRoom.get(roomId), roomId,
                            null, 0, false, false, false, true));
                } catch (SQLException temporaryFailure) {
                    nextHandAtNanos.put(roomId, System.nanoTime() + TimeUnit.SECONDS.toNanos(1));
                    throw temporaryFailure;
                }
            }
        }
        return ticks;
    }

    public Snapshot snapshot(UUID gameId, long userId) throws SQLException {
        RunningHand hand = requireHand(gameId);
        synchronized (lockFor(hand.roomId)) {
            if (!allowed(hand, userId)) {
                throw new IllegalArgumentException("Bạn chưa vào phòng này");
            }
            return hand.snapshot(userId);
        }
    }

    public Snapshot act(UUID gameId, long userId, String actionType, long amount,
                                     String turnId, String clientActionId) throws SQLException {
        RunningHand hand = requireHand(gameId);
        synchronized (lockFor(hand.roomId)) {
            // A betting action must come from a seated player; spectators never reach the engine.
            PokerPlayer player = hand.state.requirePlayer(userId);
            if (player.isLeaving() || !hand.members.contains(userId)) {
                throw new IllegalArgumentException("Bạn không còn ngồi trong ván này");
            }
            if (clientActionId == null || clientActionId.isBlank()) {
                throw new IllegalArgumentException("Thiếu clientActionId");
            }
            String key = userId + ":" + clientActionId;
            if (hand.acceptedActions.contains(key)) return hand.snapshot(userId);
            if (!hand.active) throw new IllegalArgumentException("Ván đã kết thúc");
            PokerActionType type = PokerActionType.valueOf(actionType);
            UUID turn = UUID.fromString(turnId);
            BettingAction action = new BettingAction(userId, turn, type,
                    type == PokerActionType.BET || type == PokerActionType.RAISE ? amount : 0);
            hand.engine.act(hand.state, hand.round, action, hand.deck);
            hand.lastAction.put(userId, type.name());
            hand.acceptedActions.add(key);
            hand.automateDisconnectedTurns();
            hand.settleIfComplete();
            hand.syncTurnDeadline();
            return hand.snapshot(userId);
        }
    }

    public void disconnectUser(long userId) throws SQLException {
        disconnectedUsers.add(userId);
        for (RunningHand hand : byId.values()) {
            synchronized (lockFor(hand.roomId)) {
                if (!hand.active) continue;
                hand.state.players().stream().filter(p -> p.userId() == userId)
                        .findFirst().ifPresent(PokerPlayer::markDisconnected);
                hand.automateDisconnectedTurns();
                hand.settleIfComplete();
                hand.syncTurnDeadline();
            }
        }
    }

    public void reconnectUser(long userId) throws SQLException {
        disconnectedUsers.remove(userId);
        for (RunningHand hand : byId.values()) {
            synchronized (lockFor(hand.roomId)) {
                if (!hand.active) continue;
                hand.state.players().stream().filter(p -> p.userId() == userId)
                        .findFirst().ifPresent(PokerPlayer::markConnected);
            }
        }
        rooms.markPlayingMembershipConnected(userId);
    }

    public void leave(UUID gameId, long userId) throws SQLException {
        RunningHand hand = requireHand(gameId);
        if (!allowed(hand, userId)) {
            throw new IllegalArgumentException("Bạn chưa vào phòng này");
        }
        synchronized (lockFor(hand.roomId)) {
            if (!hand.active) {
                UUID currentId = byRoom.get(hand.roomId);
                if (currentId != null && !currentId.equals(gameId)) {
                    leave(currentId, userId);
                    return;
                }
            }
            PokerPlayer player = hand.state.players().stream()
                    .filter(item -> item.userId() == userId).findFirst().orElse(null);
            if (player == null || !hand.active) {
                rooms.leave(hand.roomId, userId);
                hand.members.remove(userId);
                return;
            }
            if (player.isLeaving()) return;
            if (Long.valueOf(userId).equals(hand.state.currentTurnUserId())) {
                hand.engine.actAutomaticAction(hand.state, hand.round,
                        BettingAction.fold(userId, hand.state.turnId()), hand.deck);
            }
            player.markLeaving();
            hand.leaving.add(userId);
            if (hand.state.players().stream().filter(PokerPlayer::isEligibleToWin).count() == 1
                    && hand.state.phase() != GamePhase.FINISHED) hand.state.finishByFolds();
            hand.settleIfComplete();
            hand.syncTurnDeadline();
        }
    }

    public void memberJoined(long roomId, long userId) {
        synchronized (lockFor(roomId)) {
            UUID id = byRoom.get(roomId);
            RunningHand hand = id == null ? null : byId.get(id);
            if (hand != null) hand.members.add(userId);
        }
    }

    public void memberLeft(long roomId, long userId) {
        synchronized (lockFor(roomId)) {
            UUID id = byRoom.get(roomId);
            RunningHand hand = id == null ? null : byId.get(id);
            if (hand != null) hand.members.remove(userId);
        }
    }

    private boolean allowed(RunningHand hand, long userId) throws SQLException {
        if (hand.members.contains(userId)) return true;
        // One DB lookup for a newly joined spectator if their room event raced game creation.
        if (!rooms.hasActiveMembership(hand.roomId, userId)) return false;
        hand.members.add(userId);
        return true;
    }

    private Object lockFor(long roomId) { return roomLocks.computeIfAbsent(roomId, ignored -> new Object()); }

    private RunningHand requireHand(UUID id) {
        RunningHand hand = byId.get(id);
        if (hand == null) throw new IllegalArgumentException("Ván bài không tồn tại hoặc đã bị hủy");
        return hand;
    }

    private static void validateStart(GameSessionDao.StartData data, long hostId) {
        if (!"WAITING".equals(data.status()) || data.ownerId() != hostId) {
            throw new IllegalArgumentException("Chỉ chủ phòng chờ mới được bắt đầu ván");
        }
        if (data.seats().size() < 2) throw new IllegalArgumentException("Cần ít nhất 2 người ngồi vào bàn");
        if (data.seats().stream().anyMatch(seat -> seat.userId() != hostId
                && !"READY".equals(seat.state()))) {
            throw new IllegalArgumentException("Các người chơi khác phải sẵn sàng");
        }
        if (data.seats().stream().anyMatch(seat -> seat.chips() <= 0)) {
            throw new IllegalArgumentException("Người chơi phải có chip trên bàn");
        }
    }

    private void validateAutomaticStart(GameSessionDao.StartData data) {
        if (!"WAITING".equals(data.status()) || data.seats().size() < 2
                || data.seats().stream().anyMatch(seat -> seat.chips() <= 0
                        || disconnectedUsers.contains(seat.userId()))) {
            throw new IllegalArgumentException("Không đủ người hoặc chip để bắt đầu ván tiếp theo");
        }
    }

    private RunningHand createHand(UUID id, GameSessionDao.StartData data, Set<Long> members) {
        Deck deck = new Deck();
        deck.shuffle();
        List<GameSessionDao.Seat> seats = data.seats();
        int previousDealer = lastDealerSeat.getOrDefault(data.roomId(), 0);
        int dealerIndex = 0;
        if (previousDealer != 0) {
            while (dealerIndex < seats.size() && seats.get(dealerIndex).number() <= previousDealer)
                dealerIndex++;
            if (dealerIndex == seats.size()) dealerIndex = 0;
        }
        int dealer = seats.get(dealerIndex).number();
        int small = seats.size() == 2 ? dealer
                : seats.get((dealerIndex + 1) % seats.size()).number();
        int big = seats.get((dealerIndex + (seats.size() == 2 ? 1 : 2)) % seats.size()).number();
        List<PokerPlayer> players = new ArrayList<>();
        Map<Long, String> names = new HashMap<>();
        for (GameSessionDao.Seat seat : seats) {
            PokerPlayer player = new PokerPlayer(seat.userId(), seat.number(), seat.chips(),
                    0, 0, PokerPlayerState.ACTIVE, List.of());
            players.add(player);
            names.put(seat.userId(), seat.username());
        }
        PokerPlayer sb = players.stream().filter(p -> p.seatNumber() == small).findFirst().orElseThrow();
        PokerPlayer bb = players.stream().filter(p -> p.seatNumber() == big).findFirst().orElseThrow();
        sb.commitChips(Math.min(sb.tableChips(), data.smallBlind()));
        bb.commitChips(Math.min(bb.tableChips(), data.bigBlind()));
        for (PokerPlayer player : players) player.dealHoleCards(deck.draw(2));
        long currentBet = Math.max(sb.currentBet(), bb.currentBet());
        GameState state = new GameState(id, UUID.randomUUID(), GamePhase.PRE_FLOP,
                dealer, small, big, null, currentBet, data.bigBlind(), List.of(), players,
                Duration.ofSeconds(15), 0, null, data.bigBlind());
        PokerRoundEngine engine = new PokerRoundEngine();
        BettingRoundState round = engine.start(state, deck);
        long handNumber = handCounts.getOrDefault(data.roomId(), 0L) + 1;
        Map<Long, Long> startingChips = new HashMap<>();
        seats.forEach(seat -> startingChips.put(seat.userId(), seat.chips()));
        return new RunningHand(data.roomId(), handNumber, state, deck, engine, round,
                names, members, startingChips);
    }

    public record PublicPlayer(int seat, long userId, String username, long tableChips,
                               long currentBet, long totalCommitted, String participation,
                               boolean connected, String lastAction, List<Card> showdownCards) { }
    public record PublicState(List<Card> communityCards, List<PublicPlayer> players, String phase,
                              Long currentTurnUserId, int dealerSeat, int smallBlindSeat,
                              int bigBlindSeat, long handNumber) { }
    public record Turn(List<String> legalActions, long callAmount, long minimumTarget,
                       long maximumTarget, String turnId) { }
    /** Result is null until the authoritative settlement has completed. */
    public record HandResult(long totalPot, Map<Long, Long> payouts,
                             List<Long> tiedWinnerUserIds, boolean foldOnly) {
        static HandResult from(HandSettlementResult settlement) {
            Map<Long, Long> payouts = new LinkedHashMap<>();
            Set<Long> tied = new LinkedHashSet<>();
            long totalPot = 0;
            for (PotAward award : settlement.potAwards()) {
                totalPot = Math.addExact(totalPot, award.potAmount());
                award.winnerPayouts().forEach((userId, chips) ->
                        payouts.merge(userId, chips, Math::addExact));
                if (award.winnerUserIds().size() > 1) tied.addAll(award.winnerUserIds());
            }
            return new HandResult(totalPot, Map.copyOf(payouts), List.copyOf(tied),
                    settlement.foldOnly());
        }
    }
    public record Snapshot(PublicState publicState, List<Card> holeCards, Turn turn,
                           HandResult result) { }

    static List<Card> showdownCardsFor(GamePhase phase, HandResult result, PokerPlayer player) {
        return phase == GamePhase.FINISHED && result != null ? player.holeCards() : List.of();
    }

    private final class RunningHand {
        final long roomId;
        final long handNumber;
        final GameState state;
        final Deck deck;
        final PokerRoundEngine engine;
        final BettingRoundState round;
        final Map<Long, String> names;
        final Set<Long> members;
        final Map<Long, Long> startingChips;
        final Map<Long, String> lastAction = new HashMap<>();
        final Set<Long> leaving = new HashSet<>();
        final Set<String> acceptedActions = new HashSet<>();
        volatile boolean active = true;
        UUID timedTurnId;
        long turnDeadlineNanos;
        HandResult result;

        RunningHand(long roomId, long handNumber, GameState state, Deck deck, PokerRoundEngine engine,
                    BettingRoundState round, Map<Long, String> names, Set<Long> members,
                    Map<Long, Long> startingChips) {
            this.roomId = roomId;
            this.handNumber = handNumber;
            this.state = state;
            this.deck = deck;
            this.engine = engine;
            this.round = round;
            this.names = names;
            this.members = members;
            this.startingChips = Map.copyOf(startingChips);
            syncTurnDeadline();
        }

        void syncTurnDeadline() {
            UUID current = state.turnId();
            if (java.util.Objects.equals(current, timedTurnId)) return;
            timedTurnId = current;
            turnDeadlineNanos = current == null ? 0
                    : System.nanoTime() + state.remainingTime().toNanos();
        }

        Snapshot snapshot(long viewerId) {
            List<PublicPlayer> players = state.players().stream().map(player ->
                    new PublicPlayer(player.seatNumber(), player.userId(), names.get(player.userId()),
                            player.tableChips(), player.currentBet(), player.totalCommitted(),
                            player.playerState().name(), player.isConnected(),
                            lastAction.getOrDefault(player.userId(), ""),
                            showdownCardsFor(state.phase(), result, player))).toList();
            List<Card> mine = state.players().stream().filter(p -> p.userId() == viewerId)
                    .findFirst().map(PokerPlayer::holeCards).orElse(List.of());
            LegalActions legal = active && Long.valueOf(viewerId).equals(state.currentTurnUserId())
                    ? engine.legalActions(state, round, viewerId) : LegalActions.none();
            Turn turn = new Turn(legal.actions().stream().map(Enum::name).sorted().toList(),
                    legal.callAmount(), legal.minimumTarget(), legal.maximumTarget(),
                    state.turnId() == null ? "" : state.turnId().toString());
            return new Snapshot(new PublicState(state.communityCards(), players,
                    state.phase().name(), state.currentTurnUserId(), state.dealerPosition(),
                    state.smallBlindPosition(), state.bigBlindPosition(), handNumber), mine, turn, result);
        }

        void settleIfComplete() throws SQLException {
            if (state.phase() != GamePhase.SHOWDOWN && state.phase() != GamePhase.FINISHED) return;
            if (!state.isFinanciallySettled()) {
                HandSettlementEngine settlement = new HandSettlementEngine(new HandEvaluator());
                HandSettlementResult settled = state.phase() == GamePhase.SHOWDOWN
                        ? settlement.settleShowdown(state) : settlement.settleByFolds(state);
                result = HandResult.from(settled);
            }
            Map<Long, Long> chips = new HashMap<>();
            state.players().forEach(player -> chips.put(player.userId(), player.tableChips()));
            sessions.finish(state.gameId(), roomId, startingChips, chips,
                    result == null ? Map.of() : result.payouts());
            active = false;
            byRoom.remove(roomId);
            for (long userId : leaving) rooms.leave(roomId, userId);
            UUID olderFinishedId = lastFinishedByRoom.put(roomId, state.gameId());
            if (olderFinishedId != null) byId.remove(olderFinishedId);
            nextHandAtNanos.put(roomId, System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
        }

        void automateDisconnectedTurns() {
            int maxAutomaticActions = state.players().size() * 5;
            for (int index = 0; index < maxAutomaticActions && state.currentTurnUserId() != null; index++) {
                PokerPlayer actor = state.requirePlayer(state.currentTurnUserId());
                if (actor.isConnected()) return;
                LegalActions legal = engine.legalActionsForAutomaticAction(state, round, actor.userId());
                BettingAction action = legal.allows(PokerActionType.CHECK)
                        ? BettingAction.check(actor.userId(), state.turnId())
                        : BettingAction.fold(actor.userId(), state.turnId());
                engine.actAutomaticAction(state, round, action, deck);
                lastAction.put(actor.userId(), action.type().name());
            }
        }
    }
}
