package com.poker.controller.server.room;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.room.RoomDao;
import com.poker.dto.ChatMessageDto;
import com.poker.dto.RoomDto;
import com.poker.dto.RoomMemberDto;
import com.poker.model.room.RoomOperations;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class ServerRoomController {
    private final RoomOperations rooms;

    public ServerRoomController(JdbcDatabase database) { rooms = new RoomOperations(database); }
    public ServerRoomController(RoomDao dao) { rooms = new RoomOperations(dao); }
    public List<RoomDto> listOpen() throws SQLException { return rooms.listOpen(); }
    public Optional<RoomDto> find(long roomId) throws SQLException { return rooms.find(roomId); }
    public List<RoomMemberDto> members(long roomId) throws SQLException { return rooms.members(roomId); }
    public boolean hasActiveMembership(long roomId, long userId) throws SQLException {
        return rooms.hasActiveMembership(roomId, userId);
    }
    public long create(String name, long owner, String type, String password,
                       int maxPlayers, long smallBlind, long bigBlind, long buyIn) throws SQLException {
        return rooms.create(name, owner, type, password, maxPlayers, smallBlind, bigBlind, buyIn);
    }
    public void join(long roomId, long userId, boolean spectator, Integer seat,
                     Long buyIn, String password) throws SQLException {
        rooms.join(roomId, userId, spectator, seat, buyIn, password);
    }
    public void leave(long roomId, long userId) throws SQLException { rooms.leave(roomId, userId); }
    public void setReady(long roomId, long userId, boolean ready) throws SQLException {
        rooms.setReady(roomId, userId, ready);
    }
    public List<ChatMessageDto> chatHistory(long roomId, int limit) throws SQLException {
        return rooms.chatHistory(roomId, limit);
    }
    public ChatMessageDto sendChat(long roomId, long userId, String commandId, String content)
            throws SQLException {
        return rooms.sendChat(roomId, userId, commandId, content);
    }
    public void disconnectUser(long userId) throws SQLException { rooms.disconnectUser(userId); }
    public void cleanupWaitingMemberships() throws SQLException { rooms.cleanupWaitingMemberships(); }
}
