package com.poker.dao.game;

import com.poker.dao.JdbcDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

    public void finish(UUID gameId, long roomId, Map<Long, Long> startingChips,
                       Map<Long, Long> finalChips, Map<Long, Long> payouts) throws SQLException {
        if (!startingChips.keySet().equals(finalChips.keySet()))
            throw new IllegalArgumentException("Danh sách người chơi đã thay đổi khi chốt ván");
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                long sessionId;
                long playingSeconds;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id,started_at FROM game_sessions "
                                + "WHERE game_id=? AND room_id=? AND status='ACTIVE' FOR UPDATE")) {
                    ps.setString(1, gameId.toString());
                    ps.setLong(2, roomId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Game session is no longer active");
                        sessionId = rs.getLong(1);
                        playingSeconds = Math.max(0, Duration.between(
                                rs.getTimestamp(2).toInstant(), Instant.now()).toSeconds());
                    }
                }
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
                                + "WHERE id=? AND status='ACTIVE'")) {
                    ps.setLong(1, sessionId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Game session is no longer active");
                }
                recordResults(c, sessionId, startingChips, finalChips, payouts, playingSeconds);
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

    private static void recordResults(Connection c, long sessionId, Map<Long, Long> startingChips,
                                      Map<Long, Long> finalChips, Map<Long, Long> payouts,
                                      long playingSeconds) throws SQLException {
        List<PlayerResult> results = new ArrayList<>();
        for (var entry : startingChips.entrySet()) {
            long userId = entry.getKey();
            long net = finalChips.get(userId) - entry.getValue();
            results.add(new PlayerResult(userId, net, payouts.getOrDefault(userId, 0L)));
        }
        results.sort(Comparator.comparingLong(PlayerResult::net).reversed());
        for (PlayerResult result : results) {
            long userId = result.userId();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO player_statistics(user_id,updated_at) "
                            + "VALUES(?,CURRENT_TIMESTAMP(6))")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            long games;
            long wins;
            long losses;
            long chipsWon;
            long chipsLost;
            long netChip;
            long largestPot;
            long averageSeconds;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT total_games,total_wins,total_losses,total_chips_won,total_chips_lost,"
                            + "net_chip,largest_pot_won,average_playing_seconds "
                            + "FROM player_statistics WHERE user_id=? FOR UPDATE")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Player statistics row missing");
                    games = rs.getLong(1);
                    wins = rs.getLong(2);
                    losses = rs.getLong(3);
                    chipsWon = rs.getLong(4);
                    chipsLost = rs.getLong(5);
                    netChip = rs.getLong(6);
                    largestPot = rs.getLong(7);
                    averageSeconds = rs.getLong(8);
                }
            }
            long newGames = games + 1;
            long newWins = wins + (result.net() > 0 ? 1 : 0);
            long newLosses = losses + (result.net() < 0 ? 1 : 0);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE player_statistics SET total_games=?,total_hands=total_hands+1,"
                            + "total_wins=?,total_losses=?,win_rate=?,total_chips_won=?,"
                            + "total_chips_lost=?,net_chip=?,largest_pot_won=?,"
                            + "average_playing_seconds=?,updated_at=CURRENT_TIMESTAMP(6) "
                            + "WHERE user_id=?")) {
                ps.setLong(1, newGames);
                ps.setLong(2, newWins);
                ps.setLong(3, newLosses);
                ps.setDouble(4, 100.0 * newWins / newGames);
                ps.setLong(5, chipsWon + Math.max(0, result.net()));
                ps.setLong(6, chipsLost + Math.max(0, -result.net()));
                ps.setLong(7, netChip + result.net());
                ps.setLong(8, Math.max(largestPot, result.payout()));
                ps.setLong(9, (averageSeconds * games + playingSeconds) / newGames);
                ps.setLong(10, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO player_rankings(user_id,updated_at) "
                            + "VALUES(?,CURRENT_TIMESTAMP(6))")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
            int oldRating;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT rating FROM player_rankings WHERE user_id=? FOR UPDATE")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Player ranking row missing");
                    oldRating = rs.getInt(1);
                }
            }
            int delta = result.net() > 0 ? 20 : result.net() < 0 ? -20 : 0;
            int newRating = Math.max(0, oldRating + delta);
            int placement = 1 + (int) results.stream()
                    .filter(other -> other.net() > result.net()).count();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE player_rankings SET rating=?,games_rated=games_rated+1,"
                            + "peak_rating=GREATEST(peak_rating,?),updated_at=CURRENT_TIMESTAMP(6) "
                            + "WHERE user_id=?")) {
                ps.setInt(1, newRating);
                ps.setInt(2, newRating);
                ps.setLong(3, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO ranking_history(user_id,game_session_id,old_rating,new_rating,"
                            + "rating_delta,session_net,placement,participant_count,created_at) "
                            + "VALUES(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP(6))")) {
                ps.setLong(1, userId);
                ps.setLong(2, sessionId);
                ps.setInt(3, oldRating);
                ps.setInt(4, newRating);
                ps.setInt(5, newRating - oldRating);
                ps.setLong(6, result.net());
                ps.setInt(7, placement);
                ps.setInt(8, results.size());
                ps.executeUpdate();
            }
        }
    }

    private record PlayerResult(long userId, long net, long payout) { }

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
