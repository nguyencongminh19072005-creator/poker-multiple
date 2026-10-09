package com.poker.controller.room;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.poker.network.client.PokerSocketClient;
import com.poker.view.room.SeatViewModel;
import com.poker.model.game.card.Card;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import com.poker.view.room.ControlPanelView;
import com.poker.view.room.PokerRoomView;
import com.poker.view.room.PokerTableContainer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.poker.network.client.ClientJson.bool;
import static com.poker.network.client.ClientJson.cards;
import static com.poker.network.client.ClientJson.cause;
import static com.poker.network.client.ClientJson.number;
import static com.poker.network.client.ClientJson.string;

/** Room, game and room-chat UI; authoritative state belongs to the server. */
public final class RoomScreens {
    private final PokerSocketClient api;
    private final Consumer<Parent> show;
    private final Runnable lobby;
    private final Runnable onEnterRoom;
    private final Consumer<String> error;
    private long roomId;
    private JsonObject room;
    private JsonObject snapshot;
    private String gameId;
    private String animatedGameId;
    private String displayedResultGameId;
    private PokerTableContainer displayedResultTable;
    private long animatedHandNumber = -1;
    private int animatedBoardCount;
    private PokerRoomView roomView;
    private PokerTableContainer table;
    private String seatSignature = "";
    private final Set<Long> seenChatMessageIds = new HashSet<>();
    private ControlPanelView controls;
    private Button readyButton;
    private Button startButton;
    private Button sitButton;
    private boolean ready;
    private RoomUiState uiState = RoomUiState.SPECTATING;
    private long turnSeconds = 15;
    private String displayedTurnId = "";

    public RoomScreens(PokerSocketClient api, Consumer<Parent> show,
                Runnable lobby, Runnable onEnterRoom, Consumer<String> error) {
        this.api = api;
        this.show = show;
        this.lobby = lobby;
        this.onEnterRoom = onEnterRoom;
        this.error = error;
    }

    public void reset() {
        if (table != null) table.stopCardAnimations();
        if (roomId != 0) api.unsubscribe("/topic/room/" + roomId);
        if (gameId != null) api.unsubscribe("/topic/game/" + gameId);
        roomId = 0;
        room = null;
        snapshot = null;
        gameId = null;
        displayedTurnId = "";
        turnSeconds = 15;
        animatedGameId = null;
        displayedResultGameId = null;
        displayedResultTable = null;
        animatedHandNumber = -1;
        animatedBoardCount = 0;
        seatSignature = "";
        seenChatMessageIds.clear();
        roomView = null;
        table = null;
        controls = null;
    }

    public void setReconnecting() {
        if (roomView != null && roomId != 0) roomView.updateHeader(
                room == null ? "" : string(room.getAsJsonObject("room"), "name"), roomId,
                room == null ? 0 : number(room.getAsJsonObject("room"), "smallBlind"),
                room == null ? 0 : number(room.getAsJsonObject("room"), "bigBlind"),
                0, 0, "Đang kết nối lại...");
    }

    public void setHeroSeatIndex(int seatIndex) {
        if (table != null) table.setHeroSeatIndex(seatIndex);
    }

    public void join(long id, String password, boolean spectator) {
        if (id <= 0) { error.accept("Mã phòng không hợp lệ"); return; }
        perform(api.request("GET", "/api/v1/rooms/" + id, null), detail -> {
            for (JsonElement element : detail.getAsJsonArray("members")) {
                if (number(element.getAsJsonObject(), "userId") == api.userId()) {
                    enterRoom(id, detail);
                    return;
                }
            }
            JsonObject body = new JsonObject();
            // Entering a room always creates a spectator membership first. Sitting
            // down is a separate, explicit action from inside the room.
            body.addProperty("spectator", true);
            if (password != null) body.addProperty("password", password);
            perform(api.request("POST", "/api/v1/rooms/" + id + "/join", body), joined -> enterRoom(id, joined));
        });
    }

    private static int firstFreeSeat(JsonObject detail, int capacity) {
        Set<Integer> used = new HashSet<>();
        for (JsonElement element : detail.getAsJsonArray("members")) {
            JsonObject member = element.getAsJsonObject();
            if (hasSeat(member))
                used.add(member.get("seatNumber").getAsInt());
        }
        for (int seat = 1; seat <= capacity; seat++) if (!used.contains(seat)) return seat;
        return 0;
    }

    private static boolean hasSeat(JsonObject member) {
        return member != null && member.has("seatNumber") && !member.get("seatNumber").isJsonNull();
    }

    public void enterRoom(long id, JsonObject detail) {
        if (roomId != 0 && roomId != id) reset();
        roomId = id;
        room = detail;
        seenChatMessageIds.clear();
        onEnterRoom.run();
        api.subscribe("/topic/room/" + id, event -> Platform.runLater(() -> {
            String type = string(event, "type");
            if ("CHAT_MESSAGE".equals(type)) appendChat(event.getAsJsonObject("payload"));
            else if ("CHAT_CHANGED".equals(type)) refreshChat();
            else { refreshRoom(); refreshGame(); }
        }));
        roomScreen();
        refreshChat();
        refreshGame();
    }

    private void roomScreen() {
        roomView = new PokerRoomView(this::leaveRoom);
        table = createTable();
        roomView.setTable(table);
        controls = roomView.actions();
        readyButton = roomView.readyButton();
        readyButton.setOnAction(e -> {
            JsonObject command = new JsonObject();
            command.addProperty("clientCommandId", UUID.randomUUID().toString());
            command.addProperty("ready", !ready);
            perform(api.request("POST", "/api/v1/rooms/" + roomId + "/ready", command), detail -> {
                room = detail;
                renderRoom();
            });
        });
        startButton = roomView.startButton();
        startButton.setOnAction(e -> perform(api.request("POST", "/api/v1/games/rooms/" + roomId + "/start", new JsonObject()),
                ignored -> refreshGame()));
        sitButton = roomView.sitButton();
        sitButton.setOnAction(e -> {
            JsonObject summary = room.getAsJsonObject("room");
            int seat = firstFreeSeat(room, (int) number(summary, "maxPlayers"));
            if (seat == 0) { error.accept("Phòng đã đầy"); return; }
            JsonObject request = new JsonObject();
            request.addProperty("spectator", false);
            request.addProperty("seatNumber", seat);
            request.addProperty("buyInAmount", number(summary, "buyIn"));
            perform(api.request("POST", "/api/v1/rooms/" + roomId + "/join", request), detail -> {
                room = detail;
                renderRoom();
            });
        });
        roomView.chat().setOnSend(this::sendChat);
        show.accept(roomView);
        renderRoom();
    }

    private PokerTableContainer createTable() {
        List<SeatViewModel> seats = new ArrayList<>();
        Map<Integer, JsonObject> membersBySeat = new HashMap<>();
        if (room != null) for (JsonElement element : room.getAsJsonArray("members")) {
            JsonObject member = element.getAsJsonObject();
            if (hasSeat(member))
                membersBySeat.put((int) number(member, "seatNumber"), member);
        }
        int capacity = room == null ? 6 : (int) number(room.getAsJsonObject("room"), "maxPlayers");
        for (int i = 0; i < capacity; i++) {
            JsonObject member = membersBySeat.get(i + 1);
            seats.add(new SeatViewModel(i + 1, member == null ? "Ghế " + (i + 1) : string(member, "username"),
                    "avatar_player_" + (i % 6 + 1) + ".png",
                    member == null ? 0 : (int) Math.min(Integer.MAX_VALUE, number(member, "tableChips")), false));
        }
        PokerTableContainer result = new PokerTableContainer(seats);
        long ownerId = room == null ? -1 : number(room.getAsJsonObject("room"), "ownerUserId");
        for (int i = 0; i < capacity; i++) {
            JsonObject member = membersBySeat.get(i + 1);
            result.getPlayerNodes().get(i).updateWaitingSeat(member == null ? 0 : number(member, "tableChips"),
                    member != null, member != null && number(member, "userId") == ownerId);
        }
        boolean seatedHere = false;
        if (room != null) for (JsonElement element : room.getAsJsonArray("members")) {
            JsonObject member = element.getAsJsonObject();
            if (number(member, "userId") == api.userId() && hasSeat(member)) {
                result.setHeroSeatIndex((int) number(member, "seatNumber") - 1);
                seatedHere = true;
            }
        }
        if (!seatedHere) result.setHeroSeatIndex(-1);
        return result;
    }

    public void refreshRoom() {
        if (roomId == 0) return;
        perform(api.request("GET", "/api/v1/rooms/" + roomId, null), detail -> {
            room = detail;
            renderRoom();
        });
    }

    private void renderRoom() {
        if (room == null || roomView == null) return;
        List<String> occupiedSeats = new ArrayList<>();
        for (JsonElement element : room.getAsJsonArray("members")) {
            JsonObject member = element.getAsJsonObject();
            occupiedSeats.add(number(member, "userId") + ":" +
                    (!hasSeat(member) ? "spectator" : member.get("seatNumber").getAsString()));
        }
        Collections.sort(occupiedSeats);
        String signature = String.join("|", occupiedSeats);
        if (!signature.equals(seatSignature)) {
            seatSignature = signature;
            if (table != null) table.stopCardAnimations();
            table = createTable();
            roomView.setTable(table);
        }
        JsonObject info = room.getAsJsonObject("room");
        ready = false;
        boolean seated = false;
        boolean allGuestsReady = true;
        int seatedPlayers = 0;
        long ownerId = number(info, "ownerUserId");
        for (JsonElement element : room.getAsJsonArray("members")) {
            JsonObject member = element.getAsJsonObject();
            String state = string(member, "state");
            boolean memberSeated = hasSeat(member);
            if (memberSeated) {
                seatedPlayers++;
                if (number(member, "userId") != ownerId) allGuestsReady &= "READY".equals(state);
                int seatIndex = (int) number(member, "seatNumber") - 1;
                if (table != null && seatIndex >= 0 && seatIndex < table.getPlayerNodes().size())
                    table.getPlayerNodes().get(seatIndex).setReadyStatus(true, "READY".equals(state));
            }
            if (number(member, "userId") == api.userId()) {
                seated = memberSeated;
                ready = "READY".equals(state);
            }
        }
        boolean waitingRoom = "WAITING".equals(string(info, "status"));
        uiState = !seated ? RoomUiState.SPECTATING : ready ? RoomUiState.READY : RoomUiState.WAITING;
        boolean host = ownerId == api.userId();
        readyButton.setDisable(!seated || !waitingRoom);
        sitButton.setDisable(seated || !waitingRoom ||
                firstFreeSeat(room, (int) number(info, "maxPlayers")) == 0);
        sitButton.setVisible(!seated);
        sitButton.setManaged(!seated);
        readyButton.setVisible(seated && waitingRoom && !host);
        readyButton.setManaged(seated && waitingRoom && !host);
        readyButton.setText(ready ? "Hủy sẵn sàng" : "Sẵn sàng");
        startButton.setVisible(host);
        startButton.setManaged(host);
        startButton.setDisable(!host || !seated || !waitingRoom || seatedPlayers < 2 || !allGuestsReady);
        roomView.updateHeader(string(info, "name"), roomId,
                number(info, "smallBlind"), number(info, "bigBlind"), 0, 0,
                string(info, "status"));
        roomView.updateMode(uiState, !waitingRoom);
        if (snapshot != null) renderGame();
        else controls.updateOnline(Set.of(), 0, 0, 0, 0, uiState,
                uiState == RoomUiState.SPECTATING ? "Bạn đang xem bàn"
                        : uiState == RoomUiState.READY ? "✓ ĐÃ SẴN SÀNG" : "Đang chờ bắt đầu",
                this::sendAction);
    }

    public void refreshGame() {
        if (roomId == 0) return;
        api.request("GET", "/api/v1/games/active/room/" + roomId, null).thenAccept(active -> {
            String id = string(active, "gameId");
            api.subscribe("/topic/game/" + id, event -> Platform.runLater(() -> {
                if ("TIMER_UPDATE".equals(string(event, "type"))) {
                    JsonObject timer = event.getAsJsonObject("payload");
                    JsonObject currentTurn = snapshot == null ? null : snapshot.getAsJsonObject("turn");
                    if (timer != null && currentTurn != null
                            && string(timer, "turnId").equals(string(currentTurn, "turnId")))
                        renderTimer(number(timer, "remainingSeconds"));
                } else refreshSnapshot(id);
            }));
            Platform.runLater(() -> { gameId = id; refreshSnapshot(id); });
        }).exceptionally(ex -> null); // 404 is normal while waiting for a game.
    }

    private void refreshSnapshot(String id) {
        perform(api.request("GET", "/api/v1/games/" + id + "/snapshot", null), value -> {
            if (!id.equals(gameId) || roomId == 0) return;
            snapshot = value;
            renderGame();
        });
    }

    private void renderGame() {
        if (snapshot == null || table == null) return;
        JsonObject state = snapshot.getAsJsonObject("publicState");
        if (state == null) return;
        List<Card> board = cards(state.getAsJsonArray("communityCards"));
        long committed = 0;
        JsonArray players = state.getAsJsonArray("players");
        Map<Integer, JsonObject> bySeat = new HashMap<>();
        for (JsonElement element : players) {
            JsonObject player = element.getAsJsonObject();
            bySeat.put((int) number(player, "seat"), player);
            committed += number(player, "totalCommitted");
        }
        String phase = string(state, "phase");
        JsonObject result = snapshot.has("result") && snapshot.get("result").isJsonObject()
                ? snapshot.getAsJsonObject("result") : null;
        long displayPot = "FINISHED".equals(phase) && result != null
                ? number(result, "totalPot") : committed;
        boolean firstResultDisplay = "FINISHED".equals(phase) && result != null
                && (!Objects.equals(displayedResultGameId, gameId) || displayedResultTable != table);
        table.getTableAreaNode().updateOnline(board, displayPot, phase);
        List<Card> mine = cards(snapshot.getAsJsonArray("holeCards"));
        for (int index = 0; index < table.getPlayerNodes().size(); index++) {
            int seat = index + 1;
            JsonObject player = bySeat.get(seat);
            long user = player == null ? 0 : number(player, "userId");
            List<Card> revealed = player == null ? List.of() : cards(player.getAsJsonArray("showdownCards"));
            boolean flipOpponent = firstResultDisplay && user != api.userId() && revealed.size() == 2;
            var playerNode = table.getPlayerNodes().get(index);
            playerNode.updateOnline(player != null,
                    player == null ? 0 : number(player, "tableChips"),
                    player == null ? 0 : number(player, "currentBet"), player == null ? "" : string(player, "participation"),
                    player != null && bool(player, "connected"),
                    player != null && user == number(state, "currentTurnUserId"),
                    user == api.userId() ? mine : flipOpponent ? List.of() : revealed,
                    player != null && user != api.userId(),
                    room != null && user == number(room.getAsJsonObject("room"), "ownerUserId"),
                    seat == number(state, "dealerSeat") ? "D" : seat == number(state, "smallBlindSeat") ? "SB" :
                            seat == number(state, "bigBlindSeat") ? "BB" : "",
                    player == null ? "" : string(player, "lastAction"));
            if (!"FINISHED".equals(phase)) playerNode.hideWin();
            if ("FINISHED".equals(phase) && revealed.size() == 2) playerNode.showFinishedCardsClearly();
            if (flipOpponent) playerNode.revealShowdownCards(revealed);
        }
        JsonObject turn = snapshot.has("turn") && snapshot.get("turn").isJsonObject()
                ? snapshot.getAsJsonObject("turn") : null;
        String turnId = turn == null ? "" : string(turn, "turnId");
        if (!turnId.equals(displayedTurnId)) {
            displayedTurnId = turnId;
            turnSeconds = 15;
        }
        Set<String> legal = new HashSet<>();
        if (turn != null) for (JsonElement action : turn.getAsJsonArray("legalActions")) legal.add(action.getAsString());
        long actorId = number(state, "currentTurnUserId");
        boolean myTurn = actorId != 0 && actorId == api.userId();
        String actorName = playerName(players, actorId);
        boolean seated = false;
        if (room != null) for (JsonElement memberElement : room.getAsJsonArray("members")) {
            JsonObject member = memberElement.getAsJsonObject();
            if (number(member, "userId") == api.userId() && hasSeat(member)) seated = true;
        }
        uiState = !seated ? RoomUiState.SPECTATING
                : "FINISHED".equals(phase) ? RoomUiState.GAME_FINISHED
                : myTurn ? RoomUiState.PLAYING_MY_TURN : RoomUiState.PLAYING_NOT_MY_TURN;
        ResultView resultView = "FINISHED".equals(phase) && result != null
                ? resultView(result, players, bySeat, seated) : null;
        controls.updateOnline(legal, turn == null ? 0 : number(turn, "callAmount"),
                turn == null ? 0 : number(turn, "minimumTarget"),
                turn == null ? 0 : number(turn, "maximumTarget"), displayPot, uiState,
                resultView != null ? resultView.status()
                        : myTurn ? "YOUR TURN  ⏱ " + turnSeconds + "s"
                        : actorId == 0 ? "Đang xử lý kết quả ván"
                        : "Đang chờ " + actorName + "...",
                this::sendAction);
        animateSnapshot(state, board.size(), bySeat);
        if (resultView != null && firstResultDisplay) {
            displayedResultGameId = gameId;
            displayedResultTable = table;
            PokerTableContainer resultTable = table;
            String resultGameId = gameId;
            javafx.animation.PauseTransition afterFlip = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(420));
            afterFlip.setOnFinished(event -> {
                if (table == resultTable && Objects.equals(gameId, resultGameId)) {
                    resultTable.showHandResult(resultView.title(), resultView.details(), displayPot,
                            resultView.winnerSeats(), resultView.loserSeats(), resultView.drawSeats());
                }
            });
            afterFlip.play();
        }
        JsonObject info = room.getAsJsonObject("room");
        roomView.updateHeader(string(info, "name"), roomId,
                number(info, "smallBlind"), number(info, "bigBlind"),
                number(state, "handNumber"), displayPot, phase);
        roomView.updateMode(uiState, true);
        renderTimer(turnId.isEmpty() ? 0 : turnSeconds);
    }

    private ResultView resultView(JsonObject result, JsonArray players,
                                  Map<Integer, JsonObject> bySeat, boolean seated) {
        JsonObject payouts = result.getAsJsonObject("payouts");
        Set<Long> tied = new HashSet<>();
        JsonArray ties = result.getAsJsonArray("tiedWinnerUserIds");
        if (ties != null) for (JsonElement id : ties) tied.add(id.getAsLong());
        Map<Integer, Long> winnerSeats = new HashMap<>();
        Set<Integer> loserSeats = new HashSet<>();
        Set<Integer> drawSeats = new HashSet<>();
        List<String> winners = new ArrayList<>();
        long ownPayout = 0;
        if (payouts != null) for (Map.Entry<String, JsonElement> payout : payouts.entrySet()) {
            long userId = Long.parseLong(payout.getKey());
            long chips = payout.getValue().getAsLong();
            if (userId == api.userId()) ownPayout = chips;
            if (chips <= 0) continue;
            winners.add(playerName(players, userId) + " +$" + String.format("%,d", chips));
            for (Map.Entry<Integer, JsonObject> seat : bySeat.entrySet()) {
                if (number(seat.getValue(), "userId") == userId) {
                    winnerSeats.put(seat.getKey(), chips);
                    if (tied.contains(userId)) drawSeats.add(seat.getKey());
                }
            }
        }
        if (!winnerSeats.isEmpty()) for (Map.Entry<Integer, JsonObject> seat : bySeat.entrySet()) {
            if (number(seat.getValue(), "userId") > 0 && !winnerSeats.containsKey(seat.getKey()))
                loserSeats.add(seat.getKey());
        }
        String details = winners.isEmpty() ? "Không có pot tranh chấp" : String.join("  •  ", winners);
        String title = !seated ? "KẾT QUẢ VÁN"
                : ownPayout > 0 ? tied.contains(api.userId()) ? "HÒA / CHIA POT" : "THẮNG"
                : "THUA";
        String status = !seated ? details : ownPayout > 0
                ? title + "  +$" + String.format("%,d", ownPayout)
                : "THUA  •  " + details;
        return new ResultView(title, details, status, winnerSeats, loserSeats, drawSeats);
    }

    private record ResultView(String title, String details, String status,
                              Map<Integer, Long> winnerSeats, Set<Integer> loserSeats,
                              Set<Integer> drawSeats) { }

    private static String playerName(JsonArray players, long userId) {
        for (JsonElement element : players) {
            JsonObject player = element.getAsJsonObject();
            if (number(player, "userId") == userId) {
                String name = string(player, "username");
                return name.isBlank() ? "người chơi ghế " + number(player, "seat") : name;
            }
        }
        return "người chơi khác";
    }

    private void renderTimer(long remainingSeconds) {
        turnSeconds = Math.max(0, remainingSeconds);
        controls.updateTurnTimer(turnSeconds, 15);
        if (snapshot == null || table == null) return;
        JsonObject state = snapshot.getAsJsonObject("publicState");
        if (state == null) return;
        long actorId = number(state, "currentTurnUserId");
        JsonArray players = state.getAsJsonArray("players");
        for (JsonElement element : players) {
            JsonObject player = element.getAsJsonObject();
            int index = (int) number(player, "seat") - 1;
            if (index >= 0 && index < table.getPlayerNodes().size())
                table.getPlayerNodes().get(index).showTurnTimer(number(player, "userId") == actorId,
                        turnSeconds, 15);
        }
    }

    private void animateSnapshot(JsonObject state, int boardCount, Map<Integer, JsonObject> bySeat) {
        long hand = number(state, "handNumber");
        if (gameId == null || hand <= 0) return;
        boolean newHand = !gameId.equals(animatedGameId) || hand != animatedHandNumber;
        if (newHand) {
            animatedGameId = gameId;
            animatedHandNumber = hand;
            animatedBoardCount = boardCount;
            if ("PRE_FLOP".equals(string(state, "phase")) && boardCount == 0) {
                List<Integer> dealtSeats = new ArrayList<>(bySeat.keySet());
                Collections.sort(dealtSeats);
                table.animateHandDeal(dealtSeats);
            } else table.stopCardAnimations();
        } else if (boardCount > animatedBoardCount) {
            int previousCount = animatedBoardCount;
            animatedBoardCount = boardCount;
            table.animateCommunityDeal(previousCount, boardCount);
        }
    }

    private void sendAction(String action, long amount) {
        JsonObject turn = snapshot == null || !snapshot.has("turn") || snapshot.get("turn").isJsonNull()
                ? null : snapshot.getAsJsonObject("turn");
        if (turn == null) return;
        JsonObject command = new JsonObject();
        command.addProperty("actionType", action);
        command.addProperty("amount", amount);
        command.addProperty("turnId", string(turn, "turnId"));
        command.addProperty("clientActionId", UUID.randomUUID().toString());
        try { api.send("/app/game/" + gameId + "/action", command); }
        catch (RuntimeException ex) { error.accept(ex.getMessage()); }
    }

    public void refreshChat() {
        if (roomId == 0 || roomView == null) return;
        long requestedRoomId = roomId;
        var requestedChatPanel = roomView.chat();
        api.list("/api/v1/rooms/" + requestedRoomId + "/chat/messages?limit=50").thenAccept(messages ->
                Platform.runLater(() -> {
                    if (roomId != requestedRoomId || roomView == null
                            || roomView.chat() != requestedChatPanel) return;
                    for (JsonElement item : messages) appendChat(item.getAsJsonObject());
                })).exceptionally(ex -> null);
    }

    private void appendChat(JsonObject event) {
        if (roomView == null || event == null) return;
        JsonObject message = event.has("message") && event.get("message").isJsonObject()
                ? event.getAsJsonObject("message") : event;
        long messageId = number(message, "id");
        if (messageId > 0 && !seenChatMessageIds.add(messageId)) return;
        JsonObject sender = message.has("sender") && message.get("sender").isJsonObject()
                ? message.getAsJsonObject("sender") : null;
        String kind = string(message, "kind");
        String prefix = "SYSTEM".equals(kind) ? "[System] "
                : "GAME".equals(kind) ? "[Game] "
                : (sender == null ? "Người chơi" : string(sender, "displayName")) + ": ";
        roomView.chat().append(prefix, string(message, "content"), kind);
    }

    private void sendChat() {
        if (roomView == null) return;
        String content = roomView.chat().messageText();
        if (content.isEmpty()) return;
        long sendingRoomId = roomId;
        JsonObject command = new JsonObject();
        command.addProperty("clientMessageId", UUID.randomUUID().toString());
        command.addProperty("content", content);
        perform(api.request("POST", "/api/v1/rooms/" + sendingRoomId + "/chat/messages", command),
                message -> {
                    if (roomId != sendingRoomId || roomView == null) return;
                    roomView.chat().clearInputIfUnchanged(content);
                    appendChat(message);
                });
    }

    public void leaveRoom() {
        if (roomId == 0 || roomView == null || !roomView.confirmLeave(gameId != null)) return;
        if (gameId != null) perform(api.request("POST", "/api/v1/games/" + gameId + "/leave", new JsonObject()),
                ignored -> lobby.run());
        else perform(api.request("POST", "/api/v1/rooms/" + roomId + "/leave", new JsonObject()), ignored -> lobby.run());
    }

    private <T> void perform(CompletableFuture<T> task, Consumer<T> success) {
        task.thenAccept(value -> Platform.runLater(() -> success.accept(value)))
                .exceptionally(ex -> { Platform.runLater(() -> error.accept(cause(ex))); return null; });
    }
}
