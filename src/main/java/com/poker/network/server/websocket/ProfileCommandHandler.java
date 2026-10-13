package com.poker.network.server.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.dao.history.MatchHistoryDao;
import com.poker.controller.server.profile.ServerProfileController;

import java.io.IOException;

final class ProfileCommandHandler implements CommandHandler {
    private final ServerProfileController profiles;
    private final MatchHistoryDao history;

    ProfileCommandHandler(JdbcDatabase database) {
        profiles = new ServerProfileController(database);
        history = new MatchHistoryDao(database);
    }

    @Override
    public void handle(CommandExchange exchange) throws IOException {
        try {
            Long userId = CommandJson.userId(exchange);
            if (userId == null) {
                CommandJson.error(exchange, 401, "Bạn chưa đăng nhập");
                return;
            }
            if ("/api/v1/me/history".equals(exchange.getRequestURI().getPath())) {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    CommandJson.error(exchange, 405, "Phương thức không được hỗ trợ");
                    return;
                }
                JsonArray items = new JsonArray();
                for (var match : history.recent(userId, 5)) {
                    JsonObject row = new JsonObject();
                    row.addProperty("gameSessionId", match.gameSessionId());
                    row.addProperty("roomName", match.roomName());
                    row.addProperty("placement", match.placement());
                    row.addProperty("chipChange", match.chipChange());
                    row.addProperty("handsPlayed", match.handsPlayed());
                    row.addProperty("finishedAt", match.finishedAt().toString());
                    items.add(row);
                }
                JsonObject out = new JsonObject();
                out.add("items", items);
                CommandJson.send(exchange, 200, out);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                sendProfile(exchange, userId);
                return;
            }
            if ("PATCH".equals(exchange.getRequestMethod())) {
                JsonObject body = CommandJson.body(exchange);
                profiles.update(userId, CommandJson.text(body, "displayName"),
                        body.has("avatarUrl") && !body.get("avatarUrl").isJsonNull()
                                ? body.get("avatarUrl").getAsString()
                                : null);
                sendProfile(exchange, userId);
                return;
            }
            CommandJson.error(exchange, 405, "Phương thức không được hỗ trợ");
        } catch (IllegalArgumentException invalid) {
            CommandJson.error(exchange, 400, invalid.getMessage());
        } catch (Exception error) {
            error.printStackTrace();
            CommandJson.error(exchange, 500, "Lỗi hồ sơ người chơi");
        }
    }

    private void sendProfile(CommandExchange exchange, long userId) throws Exception {
        var user = profiles.find(userId).orElse(null);
        if (user == null) {
            CommandJson.error(exchange, 404, "Không tìm thấy người chơi");
            return;
        }
        JsonObject out = new JsonObject();
        out.addProperty("id", user.id());
        out.addProperty("username", user.username());
        out.addProperty("displayName", user.displayName());
        out.addProperty("email", user.email());
        out.addProperty("role", user.role().name());
        out.addProperty("accountStatus", user.status().name());
        out.addProperty("accountChips", user.accountChips());
        if (user.avatarUrl() == null) {
            out.add("avatarUrl", null);
        } else {
            out.addProperty("avatarUrl", user.avatarUrl());
        }
        CommandJson.send(exchange, 200, out);
    }
}
