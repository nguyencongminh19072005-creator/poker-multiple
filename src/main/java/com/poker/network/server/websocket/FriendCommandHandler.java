package com.poker.network.server.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.friend.ServerFriendController;
import com.poker.dto.FriendDto;

import java.io.IOException;

/** Lệnh bạn bè và lời mời kết bạn dùng bởi JavaFX client. */
final class FriendCommandHandler implements CommandHandler {
    private final ServerFriendController friends;

    FriendCommandHandler(JdbcDatabase database) {
        friends = new ServerFriendController(database);
    }

    @Override
    public void handle(CommandExchange exchange) throws IOException {
        try {
            Long userId = CommandJson.userId(exchange);
            if (userId == null) {
                CommandJson.error(exchange, 401, "Bạn chưa đăng nhập");
                return;
            }
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && "/api/v1/friends".equals(path)) {
                CommandJson.send(exchange, 200, list(friends.friends(userId)));
                return;
            }
            if ("GET".equals(method) && "/api/v1/friend-requests".equals(path)) {
                CommandJson.send(exchange, 200, list(friends.incoming(userId)));
                return;
            }
            if ("POST".equals(method) && "/api/v1/friend-requests".equals(path)) {
                long recipient = CommandJson.body(exchange).get("recipientUserId").getAsLong();
                long id = friends.request(userId, recipient);
                JsonObject result = new JsonObject();
                result.addProperty("requestId", id);
                CommandJson.send(exchange, 201, result);
                return;
            }
            if ("POST".equals(method) && path.matches("/api/v1/friend-requests/\\d+/(accept|reject)")) {
                String[] parts = path.split("/");
                long requestId = Long.parseLong(parts[4]);
                boolean accepted = "accept".equals(parts[5]);
                if (!friends.respond(requestId, userId, accepted)) {
                    CommandJson.error(exchange, 404, "Không tìm thấy lời mời");
                    return;
                }
                JsonObject result = new JsonObject();
                result.addProperty("status", accepted ? "ACCEPTED" : "REJECTED");
                CommandJson.send(exchange, 200, result);
                return;
            }
            if ("DELETE".equals(method) && path.matches("/api/v1/friends/\\d+")) {
                long friendId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                if (!friends.remove(userId, friendId)) {
                    CommandJson.error(exchange, 404, "Không tìm thấy bạn bè");
                    return;
                }
                CommandJson.send(exchange, 204, "");
                return;
            }
            CommandJson.error(exchange, 404, "Không tìm thấy endpoint bạn bè");
        } catch (IllegalArgumentException error) {
            CommandJson.error(exchange, 400, error.getMessage());
        } catch (Exception error) {
            error.printStackTrace();
            CommandJson.error(exchange, 500, "Lỗi hệ thống bạn bè");
        }
    }

    private static JsonArray list(java.util.List<FriendDto> values) {
        JsonArray result = new JsonArray();
        for (FriendDto value : values) {
            JsonObject item = new JsonObject();
            item.addProperty("requestId", value.requestId());
            item.addProperty("status", value.status());
            item.addProperty("presenceStatus", value.presenceStatus());
            JsonObject player = new JsonObject();
            player.addProperty("userId", value.userId());
            player.addProperty("displayName", value.displayName());
            if (value.avatarUrl() == null) {
                player.add("avatarUrl", null);
            } else {
                player.addProperty("avatarUrl", value.avatarUrl());
            }
            item.add("otherPlayer", player);
            result.add(item);
        }
        return result;
    }
}
