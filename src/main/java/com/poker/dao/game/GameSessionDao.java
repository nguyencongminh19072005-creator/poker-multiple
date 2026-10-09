package com.poker.dao.game;

import com.poker.dao.JdbcDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SQL persistence for the currently running hand. Rules are enforced by GameOperations. */
public final class GameSessionDao {
    private final JdbcDatabase database;

    public GameSessionDao(JdbcDatabase database) { this.database = database; }

    public record Seat(long userId, String username, int number, long chips, String state) { }
    public record StartData(long roomId, long ownerId, String status, long smallBlind,
                            long bigBlind, List<Seat> seats) { }

    @FunctionalInterface
    public interface StartValidator { void validate(StartData data); }

    public StartData start(long roomId, UUID gameId, StartValidator validator) throws SQLException {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                long owner;
                String status;
                long small;
                long big;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT owner_user_id,status,small_blind,big_blind FROM rooms WHERE id=? FOR UPDATE")) {
                    ps.setLong(1, roomId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Phòng không tồn tại");
                        owner = rs.getLong(1);
                        status = rs.getString(2);
                        small = rs.getLong(3);
                        big = rs.getLong(4);
                    }
                }
                List<Seat> seats = new ArrayList<>();
                String sql = "SELECT rp.user_id,u.username,rp.seat_number,rp.table_chips,rp.player_state "
                        + "FROM room_players rp JOIN users u ON u.id=rp.user_id "
                        + "WHERE rp.room_id=? AND rp.left_at IS NULL AND rp.seat_number IS NOT NULL "
                        + "ORDER BY rp.seat_number FOR UPDATE";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, roomId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) seats.add(new Seat(rs.getLong(1), rs.getString(2),
                                rs.getInt(3), rs.getLong(4), rs.getString(5)));
                    }
                }
                StartData data = new StartData(roomId, owner, status, small, big, List.copyOf(seats));
                validator.validate(data);
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO game_sessions(room_id,game_id,status,started_at) "
                                + "VALUES(?,?,'ACTIVE',CURRENT_TIMESTAMP(6))")) {
                    ps.setLong(1, roomId);
                    ps.setString(2, gameId.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rooms SET status='PLAYING',last_activity_at=CURRENT_TIMESTAMP(6) WHERE id=?")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE room_players SET player_state='PLAYING' WHERE room_id=? "
                                + "AND left_at IS NULL AND seat_number IS NOT NULL")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
                c.commit();
                return data;
            } catch (SQLException | RuntimeException error) {
                c.rollback();
                throw error;
            }
        }
    }

    public void abort(UUID gameId, long roomId) throws SQLException {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE game_sessions SET status='ABORTED',ended_at=CURRENT_TIMESTAMP(6) "
                                + "WHERE game_id=? AND status='ACTIVE'")) {
                    ps.setString(1, gameId.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rooms SET status='WAITING' WHERE id=? AND status='PLAYING'")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE room_players SET player_state='NOT_READY' WHERE room_id=? "
                                + "AND left_at IS NULL AND seat_number IS NOT NULL")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException error) {
                c.rollback();
                throw error;
            }
        }
    }

    public void finish(UUID gameId, long roomId, Map<Long, Long> finalChips) throws SQLException {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                for (var entry : finalChips.entrySet()) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE room_players SET table_chips=?,player_state='NOT_READY' "
                                    + "WHERE room_id=? AND user_id=? AND left_at IS NULL")) {
                        ps.setLong(1, entry.getValue());
                        ps.setLong(2, roomId);
                        ps.setLong(3, entry.getKey());
                        if (ps.executeUpdate() != 1) throw new SQLException("Player seat changed during hand");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE game_sessions SET status='FINISHED',ended_at=CURRENT_TIMESTAMP(6) "
                                + "WHERE game_id=? AND status='ACTIVE'")) {
                    ps.setString(1, gameId.toString());
                    if (ps.executeUpdate() != 1) throw new SQLException("Game session is no longer active");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rooms SET status='WAITING',last_activity_at=CURRENT_TIMESTAMP(6) WHERE id=?")) {
                    ps.setLong(1, roomId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException error) {
                c.rollback();
                throw error;
            }
        }
    }

    /** On a fresh process, in-memory bets were never applied to table_chips; abort them safely. */
    public void abortOrphanedGames() throws SQLException {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE rooms r JOIN game_sessions g ON g.room_id=r.id "
                                + "SET r.status='WAITING' WHERE g.status='ACTIVE' AND r.status='PLAYING'")) {
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE room_players rp JOIN rooms r ON r.id=rp.room_id "
                                + "SET rp.player_state='NOT_READY' WHERE r.status='WAITING' "
                                + "AND rp.left_at IS NULL AND rp.seat_number IS NOT NULL "
                                + "AND rp.player_state IN ('PLAYING','DISCONNECTED')")) {
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE game_sessions SET status='ABORTED',ended_at=CURRENT_TIMESTAMP(6) "
                                + "WHERE status='ACTIVE'")) {
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException error) {
                c.rollback();
                throw error;
            }
        }
    }
}
