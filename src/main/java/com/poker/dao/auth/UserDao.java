package com.poker.dao.auth;

import com.poker.dao.JdbcDatabase;
import com.poker.model.User;
import com.poker.model.auth.AccountStatus;
import com.poker.model.auth.Role;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

/** JDBC cho tài khoản và khởi tạo dữ liệu mặc định của người chơi. */
public final class UserDao {
    private final JdbcDatabase database;
    public UserDao(JdbcDatabase database) { this.database = database; }

    public Optional<AccountRow> findByUsername(String username) throws SQLException {
        String sql = "SELECT id,username,password_hash,email,role,account_status,account_chips FROM users WHERE username=?";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(row(rs)) : Optional.empty(); }
        }
    }

    public Optional<User> findById(long userId) throws SQLException {
        String sql = "SELECT u.id,u.username,p.display_name,u.email,u.role,u.account_status,u.account_chips,p.avatar_url " +
                "FROM users u JOIN player_profiles p ON p.user_id=u.id WHERE u.id=?";
        try (Connection c = database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new User(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), Role.valueOf(rs.getString(5)),
                        AccountStatus.valueOf(rs.getString(6)), rs.getLong(7), rs.getString(8)))
                        : Optional.empty();
            }
        }
    }

    public long create(String username, String email, String passwordHash, String displayName) throws SQLException {
        try (Connection c = database.open()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(username,password_hash,email,account_chips) VALUES(?,?,?,100000)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username); ps.setString(2, passwordHash); ps.setString(3, email); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Không lấy được user id"); id = keys.getLong(1); }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO player_profiles(user_id,display_name) VALUES(?,?)")) {
                    ps.setLong(1, id); ps.setString(2, displayName); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO player_statistics(user_id,updated_at) VALUES(?,?)")) {
                    ps.setLong(1, id); ps.setTimestamp(2, Timestamp.from(Instant.now())); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO player_rankings(user_id,updated_at) VALUES(?,?)")) {
                    ps.setLong(1, id); ps.setTimestamp(2, Timestamp.from(Instant.now())); ps.executeUpdate();
                }
                c.commit(); return id;
            } catch (SQLException error) { c.rollback(); throw error; }
        }
    }

    public void markLogin(long userId) throws SQLException {
        try (Connection c=database.open(); PreparedStatement ps=c.prepareStatement("UPDATE users SET last_login_at=CURRENT_TIMESTAMP(6) WHERE id=?")) {
            ps.setLong(1,userId); ps.executeUpdate();
        }
    }

    private static AccountRow row(ResultSet rs) throws SQLException {
        return new AccountRow(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getLong(7));
    }
    public record AccountRow(long id,String username,String passwordHash,String email,String role,String status,long chips) { }
}
