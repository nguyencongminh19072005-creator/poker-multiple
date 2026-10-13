package com.poker.controller.server.game;

import com.poker.dao.JdbcDatabase;
import com.poker.model.game.GameOperations;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/** Network-independent orchestration entry point for game commands. */
public final class ServerGameController {
    private final GameOperations games;

    public ServerGameController(JdbcDatabase database) { games = new GameOperations(database); }
    public void abortOrphanedGames() throws SQLException { games.abortOrphanedGames(); }
    public UUID start(long roomId, long hostId) throws SQLException { return games.start(roomId, hostId); }
    public UUID activeForRoom(long roomId, long userId) throws SQLException {
        return games.activeForRoom(roomId, userId);
    }
    public Long activeRoomForUser(long userId) throws SQLException {
        return games.activeRoomForUser(userId);
    }
    public GameOperations.Snapshot snapshot(UUID gameId, long userId) throws SQLException {
        return games.snapshot(gameId, userId);
    }
    public GameOperations.Snapshot act(UUID gameId, long userId, String actionType, long amount,
                                       String turnId, String clientActionId) throws SQLException {
        return games.act(gameId, userId, actionType, amount, turnId, clientActionId);
    }
    public void leave(UUID gameId, long userId) throws SQLException { games.leave(gameId, userId); }
    public void disconnectUser(long userId) throws SQLException { games.disconnectUser(userId); }
    public void reconnectUser(long userId) throws SQLException { games.reconnectUser(userId); }
    public long roomForGame(UUID gameId) { return games.roomForGame(gameId); }
    public List<GameOperations.TurnTick> tickTurns() throws SQLException { return games.tickTurns(); }
    public void memberJoined(long roomId, long userId) { games.memberJoined(roomId, userId); }
    public void memberLeft(long roomId, long userId) { games.memberLeft(roomId, userId); }
}
