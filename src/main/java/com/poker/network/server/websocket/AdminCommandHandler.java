package com.poker.network.server.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.admin.ServerAdminController;

import java.io.IOException;

/** Admin-only endpoints used by the existing dashboard. */
final class AdminCommandHandler implements CommandHandler {
    private final ServerAdminController admin;

    AdminCommandHandler(JdbcDatabase database) {
        admin = new ServerAdminController(database);
    }

    @Override public void handle(CommandExchange exchange) throws IOException {
        try {
            Long userId = CommandJson.userId(exchange);
            if (userId == null) { CommandJson.error(exchange, 401, "Bạn chưa đăng nhập"); return; }
            if (!admin.isAdministrator(userId)) {
                CommandJson.error(exchange, 403, "Chỉ quản trị viên được truy cập"); return;
            }
            String method = exchange.getRequestMethod(), path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && "/api/v1/admin/overview".equals(path)) {
                var data = admin.overview();
                JsonObject result = new JsonObject();
                result.addProperty("totalUsers", data.totalUsers());
                result.addProperty("activeUsers", data.activeUsers());
                result.addProperty("openRooms", data.activeRooms());
                result.addProperty("totalChipsInAccounts", data.totalAccountChips());
                CommandJson.send(exchange, 200, result); return;
            }
            if ("GET".equals(method) && "/api/v1/admin/users".equals(path)) {
                JsonArray items = new JsonArray();
                for (var user : admin.users(500)) {
                    JsonObject item = new JsonObject();
                    item.addProperty("userId", user.id()); item.addProperty("username", user.username());
                    item.addProperty("accountChips", user.accountChips());
                    item.addProperty("accountStatus", user.status().name()); items.add(item);
                }
                JsonObject result = new JsonObject(); result.add("items", items);
                CommandJson.send(exchange, 200, result); return;
            }
            if ("GET".equals(method) && "/api/v1/admin/rooms".equals(path)) {
                JsonArray items = new JsonArray();
                for (var room : admin.rooms()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("roomId", room.id()); item.addProperty("name", room.name());
                    item.addProperty("status", room.status());
                    item.addProperty("seatedPlayers", room.seatedPlayers()); items.add(item);
                }
                JsonObject result = new JsonObject(); result.add("items", items);
                CommandJson.send(exchange, 200, result); return;
            }
            if ("POST".equals(method) && path.matches("/api/v1/admin/users/\\d+/(suspend|reactivate)")) {
                String[] parts = path.split("/");
                long targetId = Long.parseLong(parts[5]);
                boolean lock = "suspend".equals(parts[6]);
                if (!admin.setUserLocked(userId, targetId, lock)) {
                    CommandJson.error(exchange, 404, "Không tìm thấy người chơi thường"); return;
                }
                JsonObject result = new JsonObject();
                result.addProperty("userId", targetId);
                result.addProperty("accountStatus", lock ? "LOCKED" : "ACTIVE");
                CommandJson.send(exchange, 200, result); return;
            }
            CommandJson.error(exchange, 404, "Không tìm thấy endpoint quản trị");
        } catch (Exception error) {
            error.printStackTrace(); CommandJson.error(exchange, 500, "Lỗi quản trị hệ thống");
        }
    }
}
