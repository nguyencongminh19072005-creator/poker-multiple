package com.poker.dao.room;

import com.poker.dao.JdbcDatabase;
import com.poker.dto.RoomDto;
import com.poker.dto.RoomMemberDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL access for rooms and memberships. Admission/departure policy belongs to RoomOperations. */
public final class RoomDao {
    private final JdbcDatabase database;

    public RoomDao(JdbcDatabase database) { this.database = database; }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T run(RoomTransaction transaction) throws SQLException;
    }

    public <T> T transaction(TransactionWork<T> work) throws SQLException {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(new RoomTransaction(connection));
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public List<Long> waitingMemberUserIds() throws SQLException {
        String sql = "SELECT DISTINCT rp.user_id FROM room_players rp JOIN rooms r ON r.id=rp.room_id "
                + "WHERE rp.left_at IS NULL AND r.status='WAITING'";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Long> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getLong(1));
            return ids;
        }
    }

    public void closeEmptyWaitingRooms() throws SQLException {
        String sql = "UPDATE rooms r SET r.status='CLOSED',r.last_activity_at=CURRENT_TIMESTAMP(6) "
                + "WHERE r.status='WAITING' AND NOT EXISTS (SELECT 1 FROM room_players rp "
                + "WHERE rp.room_id=r.id AND rp.left_at IS NULL)";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public boolean hasActiveMembership(long roomId, long userId) throws SQLException {
        String sql = "SELECT 1 FROM room_players WHERE room_id=? AND user_id=? AND left_at IS NULL";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void markPlayingMembershipConnected(long userId) throws SQLException {
        String sql = "UPDATE room_players rp JOIN rooms r ON r.id=rp.room_id "
                + "SET rp.player_state='PLAYING' WHERE rp.user_id=? AND rp.left_at IS NULL "
                + "AND r.status='PLAYING' AND rp.player_state='DISCONNECTED'";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public List<RoomDto> listOpen() throws SQLException {
        String sql = "SELECT r.id,r.name,r.owner_user_id,r.room_type,r.max_players,COUNT(rp.seat_number),"
                + "r.small_blind,r.big_blind,r.buy_in,r.status FROM rooms r LEFT JOIN room_players rp "
                + "ON rp.room_id=r.id AND rp.left_at IS NULL WHERE r.status IN ('WAITING','PLAYING') "
                + "GROUP BY r.id ORDER BY r.last_activity_at DESC";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<RoomDto> result = new ArrayList<>();
            while (rs.next()) result.add(room(rs));
            return result;
        }
    }

    public Optional<RoomDto> find(long roomId) throws SQLException {
        String sql = "SELECT r.id,r.name,r.owner_user_id,r.room_type,r.max_players,COUNT(rp.seat_number),"
                + "r.small_blind,r.big_blind,r.buy_in,r.status FROM rooms r LEFT JOIN room_players rp "
                + "ON rp.room_id=r.id AND rp.left_at IS NULL WHERE r.id=? GROUP BY r.id";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(room(rs)) : Optional.empty();
            }
        }
    }

    public List<RoomMemberDto> members(long roomId) throws SQLException {
        String sql = "SELECT rp.user_id,u.username,rp.seat_number,rp.player_state,rp.table_chips "
                + "FROM room_players rp JOIN users u ON u.id=rp.user_id "
                + "WHERE rp.room_id=? AND rp.left_at IS NULL ORDER BY rp.seat_number";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RoomMemberDto> result = new ArrayList<>();
                while (rs.next()) result.add(new RoomMemberDto(rs.getLong(1), rs.getString(2),
                        (Integer) rs.getObject(3), rs.getString(4), rs.getLong(5)));
                return result;
            }
        }
    }

    private static RoomDto room(ResultSet rs) throws SQLException {
        return new RoomDto(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4),
                rs.getInt(5), rs.getInt(6), rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getString(10));
    }

    public record RoomRow(String type, String passwordHash, String status, int maxPlayers, long buyIn) { }
    public record Membership(boolean exists, boolean active, Integer seatNumber, long tableChips) { }

    /** SQL primitives sharing a single transaction; no game or room policy is decided here. */
    public static final class RoomTransaction {
        private final Connection connection;

        private RoomTransaction(Connection connection) { this.connection = connection; }

        public long insertRoom(String name, long owner, String type, String passwordHash, int max,
                               long small, long big, long buyIn) throws SQLException {
            String sql = "INSERT INTO rooms(name,owner_user_id,room_type,password_hash,max_players,"
                    + "small_blind,big_blind,buy_in) VALUES(?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setLong(2, owner);
                ps.setString(3, type);
                ps.setString(4, passwordHash);
                ps.setInt(5, max);
                ps.setLong(6, small);
                ps.setLong(7, big);
                ps.setLong(8, buyIn);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Không lấy được room id");
                    return rs.getLong(1);
                }
            }
        }

        public Optional<RoomRow> lockRoom(long roomId) throws SQLException {
            String sql = "SELECT room_type,password_hash,status,max_players,buy_in FROM rooms WHERE id=? FOR UPDATE";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(new RoomRow(rs.getString(1), rs.getString(2),
                            rs.getString(3), rs.getInt(4), rs.getLong(5))) : Optional.empty();
                }
            }
        }

        public Membership lockMembership(long roomId, long userId) throws SQLException {
            String sql = "SELECT seat_number,left_at IS NULL,table_chips FROM room_players "
                    + "WHERE room_id=? AND user_id=? FOR UPDATE";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new Membership(true, rs.getBoolean(2),
                            (Integer) rs.getObject(1), rs.getLong(3))
                            : new Membership(false, false, null, 0);
                }
            }
        }

        public boolean seatOccupied(long roomId, int seat) throws SQLException {
            String sql = "SELECT COUNT(*) FROM room_players WHERE room_id=? AND seat_number=? AND left_at IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                ps.setInt(2, seat);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1) > 0; }
            }
        }

        public boolean debitWallet(long userId, long amount) throws SQLException {
            String sql = "UPDATE users SET account_chips=account_chips-? WHERE id=? AND account_chips>=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, amount);
                ps.setLong(2, userId);
                ps.setLong(3, amount);
                return ps.executeUpdate() == 1;
            }
        }

        public void creditWallet(long userId, long amount) throws SQLException {
            String sql = "UPDATE users SET account_chips=account_chips+? WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, amount);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        }

        public void upsertMember(long roomId, long userId, Integer seat, String state,
                                 long amount, boolean exists) throws SQLException {
            if (exists) {
                String sql = "UPDATE room_players SET seat_number=?,player_state=?,table_chips=?,left_at=NULL "
                        + "WHERE room_id=? AND user_id=?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    setSeat(ps, 1, seat);
                    ps.setString(2, state);
                    ps.setLong(3, amount);
                    ps.setLong(4, roomId);
                    ps.setLong(5, userId);
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO room_players(room_id,user_id,seat_number,player_state,table_chips) "
                        + "VALUES(?,?,?,?,?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, roomId);
                    ps.setLong(2, userId);
                    setSeat(ps, 3, seat);
                    ps.setString(4, state);
                    ps.setLong(5, amount);
                    ps.executeUpdate();
                }
            }
        }

        public Optional<Long> lockActiveTableChips(long roomId, long userId) throws SQLException {
            String sql = "SELECT table_chips FROM room_players WHERE room_id=? AND user_id=? "
                    + "AND left_at IS NULL FOR UPDATE";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
                }
            }
        }

        public void markLeft(long roomId, long userId) throws SQLException {
            String sql = "UPDATE room_players SET seat_number=NULL,player_state='SPECTATING',table_chips=0,"
                    + "left_at=CURRENT_TIMESTAMP(6) WHERE room_id=? AND user_id=? AND left_at IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        }

        public Optional<Long> nextMember(long roomId) throws SQLException {
            String sql = "SELECT user_id FROM room_players WHERE room_id=? AND left_at IS NULL "
                    + "ORDER BY (seat_number IS NULL),joined_at,id LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
                }
            }
        }

        public void closeRoom(long roomId) throws SQLException {
            String sql = "UPDATE rooms SET status='CLOSED',last_activity_at=CURRENT_TIMESTAMP(6) WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, roomId);
                ps.executeUpdate();
            }
        }

        public void transferOwnership(long roomId, long departingUser, long nextUser) throws SQLException {
            String sql = "UPDATE rooms SET owner_user_id=?,last_activity_at=CURRENT_TIMESTAMP(6) "
                    + "WHERE id=? AND owner_user_id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, nextUser);
                ps.setLong(2, roomId);
                ps.setLong(3, departingUser);
                ps.executeUpdate();
            }
        }

        public int updateReady(long roomId, long userId, String state) throws SQLException {
            String sql = "UPDATE room_players rp JOIN rooms r ON r.id=rp.room_id SET rp.player_state=? "
                    + "WHERE rp.room_id=? AND rp.user_id=? AND rp.left_at IS NULL "
                    + "AND rp.seat_number IS NOT NULL AND r.status='WAITING'";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, state);
                ps.setLong(2, roomId);
                ps.setLong(3, userId);
                return ps.executeUpdate();
            }
        }

        public List<Long> waitingRoomsForUser(long userId) throws SQLException {
            String sql = "SELECT rp.room_id FROM room_players rp JOIN rooms r ON r.id=rp.room_id "
                    + "WHERE rp.user_id=? AND rp.left_at IS NULL AND r.status='WAITING' FOR UPDATE";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Long> result = new ArrayList<>();
                    while (rs.next()) result.add(rs.getLong(1));
                    return result;
                }
            }
        }

        public void markPlayingMembershipsDisconnected(long userId) throws SQLException {
            String sql = "UPDATE room_players rp JOIN rooms r ON r.id=rp.room_id "
                    + "SET rp.player_state='DISCONNECTED' WHERE rp.user_id=? "
                    + "AND rp.left_at IS NULL AND r.status='PLAYING'";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }
        }

        private static void setSeat(PreparedStatement ps, int index, Integer seat)
                throws SQLException {
            if (seat == null) ps.setNull(index, Types.INTEGER);
            else ps.setInt(index, seat);
        }
    }
}
