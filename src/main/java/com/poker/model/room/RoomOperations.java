package com.poker.model.room;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.chat.ChatMessageDao;
import com.poker.dao.room.RoomDao;
import com.poker.dto.ChatMessageDto;
import com.poker.dto.RoomDto;
import com.poker.dto.RoomMemberDto;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Room and room-chat rules shared by the WebSocket command adapter. */
public final class RoomOperations {
    private final RoomDao rooms;
    private final ChatMessageDao chat;

    public RoomOperations(JdbcDatabase database) {
        rooms = new RoomDao(database);
        chat = new ChatMessageDao(database);
    }

    public RoomOperations(RoomDao rooms) {
        this.rooms = rooms;
        this.chat = null;
    }

    public List<RoomDto> listOpen() throws SQLException { return rooms.listOpen(); }
    public Optional<RoomDto> find(long roomId) throws SQLException { return rooms.find(roomId); }
    public List<RoomMemberDto> members(long roomId) throws SQLException { return rooms.members(roomId); }
    public boolean hasActiveMembership(long roomId, long userId) throws SQLException {
        return rooms.hasActiveMembership(roomId, userId);
    }
    public void markPlayingMembershipConnected(long userId) throws SQLException {
        rooms.markPlayingMembershipConnected(userId);
    }
    public void join(long roomId, long userId, boolean spectator, Integer seat, Long buyIn,
                     String password) throws SQLException {
        rooms.transaction(tx -> {
            RoomDao.RoomRow room = tx.lockRoom(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
            RoomAdmissionRules.requireOpen(room.status(), spectator);
            RoomDao.Membership member = tx.lockMembership(roomId, userId);
            boolean converting = member.active() && member.seatNumber() == null && !spectator;
            if ("PRIVATE".equals(room.type()) && !converting
                    && (password == null || room.passwordHash() == null
                    || !BCrypt.checkpw(password, room.passwordHash()))) {
                throw new IllegalArgumentException("Mật khẩu phòng không đúng");
            }
            RoomAdmissionRules.requireAvailableMembership(member.active(), converting);
            long amount = spectator ? 0 : buyIn == null ? room.buyIn() : buyIn;
            RoomAdmissionRules.validateSeatAndBuyIn(spectator, seat, room.maxPlayers(), amount, room.buyIn());
            if (!spectator) {
                if (tx.seatOccupied(roomId, seat)) throw new IllegalArgumentException("Ghế đã có người");
                if (!tx.debitWallet(userId, amount)) throw new IllegalArgumentException("Không đủ chip Buy-in");
            }
            tx.upsertMember(roomId, userId, spectator ? null : seat,
                    spectator ? "SPECTATING" : "NOT_READY", amount, member.exists());
            return null;
        });
    }
    public void leave(long roomId, long userId) throws SQLException {
        rooms.transaction(tx -> {
            RoomDao.RoomRow room = tx.lockRoom(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại"));
            RoomDao.Membership member = tx.lockMembership(roomId, userId);
            if ("PLAYING".equals(room.status()) && member.seatNumber() != null && member.active()) {
                throw new IllegalArgumentException("Hãy dùng lệnh rời ván để bảo toàn chip đang cược");
            }
            long chips = tx.lockActiveTableChips(roomId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Bạn không ở trong phòng"));
            tx.markLeft(roomId, userId);
            if (chips > 0) tx.creditWallet(userId, chips);
            updateRoomAfterDeparture(tx, roomId, userId);
            return null;
        });
    }
    public void setReady(long roomId, long userId, boolean ready) throws SQLException {
        rooms.transaction(tx -> {
            if (tx.updateReady(roomId, userId, ready ? "READY" : "NOT_READY") != 1) {
                throw new IllegalArgumentException("Chỉ người đã ngồi trong phòng chờ mới có thể sẵn sàng");
            }
            return null;
        });
    }

    /** A previous server process has no live waiting-room clients. */
    public void cleanupWaitingMemberships() throws SQLException {
        for (long userId : rooms.waitingMemberUserIds()) disconnectUser(userId);
        rooms.closeEmptyWaitingRooms();
    }

    /** Refund waiting-room seats; preserve seats in active games as disconnected. */
    public void disconnectUser(long userId) throws SQLException {
        rooms.transaction(tx -> {
            for (long roomId : tx.waitingRoomsForUser(userId)) {
                long chips = tx.lockActiveTableChips(roomId, userId).orElse(0L);
                if (chips > 0) tx.creditWallet(userId, chips);
                tx.markLeft(roomId, userId);
                updateRoomAfterDeparture(tx, roomId, userId);
            }
            tx.markPlayingMembershipsDisconnected(userId);
            return null;
        });
    }

    private static void updateRoomAfterDeparture(RoomDao.RoomTransaction tx, long roomId,
                                                  long departingUser) throws SQLException {
        Optional<Long> next = tx.nextMember(roomId);
        if (next.isEmpty()) tx.closeRoom(roomId);
        else tx.transferOwnership(roomId, departingUser, next.get());
    }
    public List<ChatMessageDto> chatHistory(long roomId, int limit) throws SQLException {
        return chat.history(roomId, 0, limit);
    }

    public long create(String name, long owner, String type, String password,
                       int maxPlayers, long smallBlind, long bigBlind, long buyIn) throws SQLException {
        String normalizedName = name.trim();
        RoomCreationRules.validate(normalizedName, maxPlayers, smallBlind, bigBlind, buyIn);
        String passwordHash = null;
        if ("PRIVATE".equals(type)) {
            RoomCreationRules.requirePrivatePassword(password);
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        } else {
            type = "PUBLIC";
        }
        String finalType = type;
        String finalPasswordHash = passwordHash;
        return rooms.transaction(tx -> {
            long roomId = tx.insertRoom(normalizedName, owner, finalType, finalPasswordHash,
                    maxPlayers, smallBlind, bigBlind, buyIn);
            tx.upsertMember(roomId, owner, null, "SPECTATING", 0, false);
            return roomId;
        });
    }

    public ChatMessageDto sendChat(long roomId, long userId, String commandId,
                                   String content) throws SQLException {
        String normalizedContent = content.trim();
        if (normalizedContent.isEmpty() || normalizedContent.length() > 500) {
            throw new IllegalArgumentException("Tin nhắn phải dài từ 1 đến 500 ký tự");
        }
        String id = commandId.isBlank() ? UUID.randomUUID().toString() : commandId;
        return chat.save(roomId, userId, id, normalizedContent);
    }
}
