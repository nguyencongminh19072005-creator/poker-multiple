package com.poker.dao.auth;

import com.poker.dao.JdbcDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.OptionalLong;
import java.util.UUID;

public final class RefreshTokenDao {
    private final JdbcDatabase database;
    public RefreshTokenDao(JdbcDatabase database) { this.database = database; }

    public String create(long userId) throws SQLException {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        String sql = "INSERT INTO refresh_tokens(user_id,token_hash,expires_at) VALUES(?,?,?)";
        try (Connection c=database.open(); PreparedStatement ps=c.prepareStatement(sql)) {
            ps.setLong(1,userId); ps.setString(2,hash(raw));
            ps.setTimestamp(3,Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS))); ps.executeUpdate();
        }
        return raw;
    }

    public OptionalLong activeUser(String raw) throws SQLException {
        String sql="SELECT user_id FROM refresh_tokens WHERE token_hash=? AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP(6)";
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,hash(raw));try(ResultSet rs=ps.executeQuery()){return rs.next()?OptionalLong.of(rs.getLong(1)):OptionalLong.empty();}}
    }

    public void revoke(String raw)throws SQLException{
        try(Connection c=database.open();PreparedStatement ps=c.prepareStatement("UPDATE refresh_tokens SET revoked_at=CURRENT_TIMESTAMP(6) WHERE token_hash=? AND revoked_at IS NULL")){ps.setString(1,hash(raw));ps.executeUpdate();}
    }

    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception error){throw new IllegalStateException(error);}}
}
