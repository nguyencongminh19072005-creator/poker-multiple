package com.poker.model.room;

/** Quy tắc nghiệp vụ khi tạo phòng; tầng WebSocket chỉ đọc và chuyển dữ liệu yêu cầu. */
public final class RoomCreationRules {
    private RoomCreationRules() {
    }

    public static void validate(String name, int maxPlayers, long smallBlind,
                                long bigBlind, long buyIn) {
        if (name.isBlank() || maxPlayers < 6 || maxPlayers > 9
                || smallBlind <= 0 || bigBlind <= smallBlind || buyIn <= 0) {
            throw new IllegalArgumentException("Cấu hình phòng không hợp lệ");
        }
    }

    public static void requirePrivatePassword(String password) {
        if (password.isBlank()) {
            throw new IllegalArgumentException("Phòng riêng cần mật khẩu");
        }
    }
}
