package com.poker.model.room;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý các lời mời vào phòng trong bộ nhớ server.
 * Cho phép người được mời vào phòng riêng (có mật khẩu) trực tiếp thông qua token lời mời
 * mà không cần phải nhập mật khẩu.
 */
public final class RoomInvitationRegistry {
    public record InvitationKey(long roomId, long userId) { }
    public record InvitationRecord(String token, long expiresAtMillis) { }

    private final Map<InvitationKey, InvitationRecord> invitations = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public RoomInvitationRegistry() {
        this(15 * 60 * 1000L); // Mặc định hết hạn sau 15 phút
    }

    public RoomInvitationRegistry(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public void register(long roomId, long userId, String token) {
        if (token == null || token.isBlank()) return;
        cleanExpired();
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        invitations.put(new InvitationKey(roomId, userId), new InvitationRecord(token, expiresAt));
    }

    public boolean validateAndConsume(long roomId, long userId, String token) {
        if (token == null || token.isBlank()) return false;
        InvitationKey key = new InvitationKey(roomId, userId);
        InvitationRecord record = invitations.get(key);
        if (record == null) return false;

        if (System.currentTimeMillis() > record.expiresAtMillis()) {
            invitations.remove(key);
            return false;
        }

        if (Objects.equals(record.token(), token)) {
            invitations.remove(key);
            return true;
        }
        return false;
    }

    public boolean hasValidInvitation(long roomId, long userId) {
        InvitationKey key = new InvitationKey(roomId, userId);
        InvitationRecord record = invitations.get(key);
        if (record == null) return false;
        if (System.currentTimeMillis() > record.expiresAtMillis()) {
            invitations.remove(key);
            return false;
        }
        return true;
    }

    public void cleanExpired() {
        long now = System.currentTimeMillis();
        invitations.entrySet().removeIf(entry -> now > entry.getValue().expiresAtMillis());
    }

    public void clear() {
        invitations.clear();
    }
}
