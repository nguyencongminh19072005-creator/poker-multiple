package com.poker;

import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.profile.ServerProfileController;
import com.poker.controller.server.room.ServerRoomController;
import com.poker.network.server.websocket.PokerWebSocketServer;

/** Điểm khởi động server Java thuần; kiểm tra MySQL trước khi mở dịch vụ. */
public final class ServerMain {
    private ServerMain() { }

    public static void main(String[] args) throws Exception {
        JdbcDatabase database = JdbcDatabase.fromEnvironment();
        try (var connection = database.open()) {
            if (!connection.isValid(3)) throw new IllegalStateException("Không kết nối được MySQL");
            System.out.println("[SERVER] MySQL connected: " + connection.getCatalog());
        }
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
        PokerWebSocketServer server = new PokerWebSocketServer(database, port);
        // Mở cổng trước khi dọn phiên cũ: server thứ hai không được phép
        // dọn membership của server thứ nhất nếu cổng đã bị chiếm.
        server.startServer();
        try {
            server.abortOrphanedGames();
            // Không client nào của tiến trình trước còn giữ phiên online hợp lệ.
            new ServerRoomController(database).cleanupWaitingMemberships();
            new ServerProfileController(database).markAllOffline();
        } catch (Exception error) {
            server.stopServer();
            throw error;
        }
        System.out.println("[SERVER] WebSocket running at ws://0.0.0.0:" + port + "/ws");
    }
}
