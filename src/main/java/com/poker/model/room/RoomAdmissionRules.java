package com.poker.model.room;

/** Điều kiện tham gia phòng; DAO giữ trách nhiệm khóa hàng và ghi dữ liệu giao dịch. */
public final class RoomAdmissionRules {
    private RoomAdmissionRules() {
    }

    public static void requireOpen(String status, boolean spectator) {
        if (!"WAITING".equals(status) && !(spectator && "PLAYING".equals(status))) {
            throw new IllegalArgumentException("Phòng không còn nhận người chơi");
        }
    }

    public static void requireAvailableMembership(boolean active, boolean converting) {
        if (active && !converting) {
            throw new IllegalArgumentException("Bạn đã ở trong phòng này");
        }
    }

    public static void validateSeatAndBuyIn(boolean spectator, Integer seat, int maxPlayers,
                                            long amount, long roomBuyIn) {
        if (!spectator && (seat == null || seat < 1 || seat > maxPlayers)) {
            throw new IllegalArgumentException("Ghế không hợp lệ");
        }
        if (!spectator && amount != roomBuyIn) {
            throw new IllegalArgumentException("Buy-in phải đúng cấu hình phòng");
        }
    }
}
