package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.room.ServerRoomController;
import com.poker.controller.server.game.ServerGameController;
import com.poker.dto.RoomDto;

import java.io.IOException;
import java.util.Map;

final class RoomCommandHandler implements CommandHandler {
    private final ServerRoomController rooms;
    private final ServerGameController games;
    RoomCommandHandler(JdbcDatabase database) {
        this(database, null);
    }

    RoomCommandHandler(JdbcDatabase database, ServerGameController games) {
        rooms = new ServerRoomController(database);
        this.games = games;
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
            if ("GET".equals(method) && "/api/v1/rooms".equals(path)) {
                CommandJson.send(exchange, 200, rooms.listOpen());
                return;
            }
            if ("POST".equals(method) && "/api/v1/rooms".equals(path)) {
                create(exchange, userId);
                return;
            }
            if ("POST".equals(method) && path.matches("/api/v1/rooms/\\d+/join")) {
                join(exchange, userId, roomId(path, "/join"));
                return;
            }
            if ("POST".equals(method) && path.matches("/api/v1/rooms/\\d+/leave")) {
                leave(exchange, userId, roomId(path, "/leave"));
                return;
            }
            if ("POST".equals(method) && path.matches("/api/v1/rooms/\\d+/ready")) {
                ready(exchange, userId, roomId(path, "/ready"));
                return;
            }
            if (path.matches("/api/v1/rooms/\\d+/chat/messages")) {
                long roomId = roomId(path, "/chat/messages");
                if (!rooms.hasActiveMembership(roomId, userId)) {
                    CommandJson.error(exchange, 403, "Bạn chưa vào phòng này");
                    return;
                }
                if ("GET".equals(method)) {
                    chatHistory(exchange, roomId);
                    return;
                }
                if ("POST".equals(method)) {
                    chatSend(exchange, roomId, userId);
                    return;
                }
            }
            if ("GET".equals(method) && path.matches("/api/v1/rooms/\\d+")) {
                detail(exchange, Long.parseLong(path.substring(path.lastIndexOf('/') + 1)));
                return;
            }
            CommandJson.error(exchange, 404, "Không tìm thấy endpoint phòng");
        } catch (IllegalArgumentException invalid) {
            CommandJson.error(exchange, 400, invalid.getMessage());
        } catch (Exception error) {
            error.printStackTrace();
            CommandJson.error(exchange, 500,
                    error.getMessage() == null ? "Lỗi phòng" : error.getMessage());
        }
    }

    private void create(CommandExchange exchange, long owner) throws Exception {
        JsonObject body = CommandJson.body(exchange);
        String name = CommandJson.text(body, "name");
        String type = CommandJson.text(body, "roomType");
        int max = body.get("maxPlayers").getAsInt();
        long small = body.get("smallBlind").getAsLong();
        long big = body.get("bigBlind").getAsLong();
        long buyIn = body.get("buyIn").getAsLong();
        String password = CommandJson.text(body, "password");
        long id = rooms.create(name, owner, type, password, max, small, big, buyIn);
        detail(exchange, id);
    }

    private void detail(CommandExchange exchange, long roomId) throws Exception {
        RoomDto room = rooms.find(roomId).orElse(null);
        if (room == null) {
            CommandJson.error(exchange, 404, "Phòng không tồn tại");
            return;
        }
        CommandJson.send(exchange, 200, Map.of("room", room, "members", rooms.members(roomId)));
    }

    private void join(CommandExchange exchange, long userId, long roomId) throws Exception {
        JsonObject body = CommandJson.body(exchange);
        boolean spectator = body.has("spectator") && body.get("spectator").getAsBoolean();
        Integer seat = body.has("seatNumber") && !body.get("seatNumber").isJsonNull()
                ? body.get("seatNumber").getAsInt()
                : null;
        Long buyIn = body.has("buyInAmount") && !body.get("buyInAmount").isJsonNull()
                ? body.get("buyInAmount").getAsLong()
                : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;
        rooms.join(roomId, userId, spectator, seat, buyIn, password);
        if (games != null) games.memberJoined(roomId, userId);
        detail(exchange, roomId);
    }

    private void leave(CommandExchange exchange, long userId, long roomId) throws Exception {
        rooms.leave(roomId, userId);
        if (games != null) games.memberLeft(roomId, userId);
        detail(exchange, roomId);
    }

    private void ready(CommandExchange exchange, long userId, long roomId) throws Exception {
        JsonObject body = CommandJson.body(exchange);
        rooms.setReady(roomId, userId, body.has("ready") && body.get("ready").getAsBoolean());
        detail(exchange, roomId);
    }

    private void chatHistory(CommandExchange exchange, long roomId) throws Exception {
        int limit = 50;
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null && query.matches("(?:^|.*&)limit=\\d+(?:&.*|$)")) {
            for (String part : query.split("&")) {
                if (part.startsWith("limit=")) {
                    limit = Integer.parseInt(part.substring(6));
                }
            }
        }
        com.google.gson.JsonArray items = new com.google.gson.JsonArray();
        for (var message : rooms.chatHistory(roomId, limit)) {
            items.add(chatJson(message));
        }
        JsonObject response = new JsonObject();
        response.add("items", items);
        CommandJson.send(exchange, 200, response);
    }

    private void chatSend(CommandExchange exchange, long roomId, long userId) throws Exception {
        JsonObject body = CommandJson.body(exchange);
        String content = CommandJson.text(body, "content");
        String commandId = CommandJson.text(body, "clientMessageId");
        CommandJson.send(exchange, 201,
                chatJson(rooms.sendChat(roomId, userId, commandId, content)));
    }

    private static JsonObject chatJson(com.poker.dto.ChatMessageDto message) {
        JsonObject result = new JsonObject();
        result.addProperty("id", message.id());
        result.addProperty("kind", "PLAYER");
        result.addProperty("content", message.content());
        result.addProperty("createdAt", message.createdAt().toString());
        JsonObject sender = new JsonObject();
        sender.addProperty("userId", message.senderId());
        sender.addProperty("displayName", message.senderName());
        result.add("sender", sender);
        return result;
    }

    private static long roomId(String path, String suffix) {
        String value = path.substring("/api/v1/rooms/".length(), path.length() - suffix.length());
        return Long.parseLong(value);
    }
}
