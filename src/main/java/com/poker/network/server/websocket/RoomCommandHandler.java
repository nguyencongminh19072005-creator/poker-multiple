package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.room.ServerRoomController;
import com.poker.controller.server.game.ServerGameController;
import com.poker.dto.RoomDto;
import com.poker.model.room.RoomInvitationRegistry;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

final class RoomCommandHandler implements CommandHandler {
    @FunctionalInterface
    interface InvitationSender {
        void send(long targetUserId, JsonObject payload);
    }

    private final JdbcDatabase database;
    private final ServerRoomController rooms;
    private final ServerGameController games;
    private final InvitationSender invitationSender;
    private final RoomInvitationRegistry roomInvitations;

    RoomCommandHandler(JdbcDatabase database) {
        this(database, null, null);
    }

    RoomCommandHandler(JdbcDatabase database, ServerGameController games) {
        this(database, games, null);
    }

    RoomCommandHandler(JdbcDatabase database, ServerGameController games, InvitationSender invitationSender) {
        this(database, games, invitationSender, new RoomInvitationRegistry());
    }

    RoomCommandHandler(JdbcDatabase database, ServerGameController games,
                       InvitationSender invitationSender, RoomInvitationRegistry roomInvitations) {
        this.database = database;
        this.rooms = new ServerRoomController(database);
        this.games = games;
        this.invitationSender = invitationSender;
        this.roomInvitations = roomInvitations != null ? roomInvitations : new RoomInvitationRegistry();
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
            if ("POST".equals(method) && path.matches("/api/v1/rooms/\\d+/invite")) {
                invite(exchange, userId, roomId(path, "/invite"));
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
        String password = body.has("password") && !body.get("password").isJsonNull()
                ? body.get("password").getAsString()
                : null;
        String inviteToken = body.has("inviteToken") && !body.get("inviteToken").isJsonNull()
                ? body.get("inviteToken").getAsString()
                : null;

        boolean isInvited = false;
        if (inviteToken != null && !inviteToken.isBlank()) {
            isInvited = roomInvitations.validateAndConsume(roomId, userId, inviteToken);
        }

        rooms.join(roomId, userId, spectator, seat, buyIn, password, isInvited);
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

    private void invite(CommandExchange exchange, long userId, long roomId) throws Exception {
        if (!rooms.hasActiveMembership(roomId, userId)) {
            CommandJson.error(exchange, 403, "Bạn chưa vào phòng này");
            return;
        }
        JsonObject body = CommandJson.body(exchange);
        if (!body.has("friendUserId") || body.get("friendUserId").isJsonNull()) {
            CommandJson.error(exchange, 400, "Thiếu ID người bạn cần mời");
            return;
        }
        long friendUserId = body.get("friendUserId").getAsLong();
        if (friendUserId == userId) {
            CommandJson.error(exchange, 400, "Không thể tự mời chính mình");
            return;
        }
        if (rooms.hasActiveMembership(roomId, friendUserId)) {
            CommandJson.error(exchange, 400, "Người này đã ở trong phòng");
            return;
        }
        RoomDto room = rooms.find(roomId).orElse(null);
        if (room == null) {
            CommandJson.error(exchange, 404, "Phòng không tồn tại");
            return;
        }
        com.poker.dao.profile.ProfileDao profileDao = new com.poker.dao.profile.ProfileDao(database);
        com.poker.dto.ProfileDto friendProfile = profileDao.find(friendUserId).orElse(null);
        if (friendProfile == null) {
            CommandJson.error(exchange, 404, "Không tìm thấy người chơi này");
            return;
        }
        if (!"ONLINE".equalsIgnoreCase(friendProfile.onlineStatus())) {
            CommandJson.error(exchange, 400, "Người chơi này hiện đang ngoại tuyến, không thể mời");
            return;
        }
        String inviterName = profileDao.find(userId).map(p -> p.displayName()).orElse("Một người bạn");

        boolean isPrivate = "PRIVATE".equalsIgnoreCase(room.roomType());
        String inviteToken = UUID.randomUUID().toString();
        roomInvitations.register(roomId, friendUserId, inviteToken);

        JsonObject payload = new JsonObject();
        payload.addProperty("type", "ROOM_INVITATION");
        payload.addProperty("roomId", roomId);
        payload.addProperty("roomName", room.name());
        payload.addProperty("inviterId", userId);
        payload.addProperty("inviterName", inviterName);
        payload.addProperty("smallBlind", room.smallBlind());
        payload.addProperty("bigBlind", room.bigBlind());
        payload.addProperty("buyIn", room.buyIn());
        payload.addProperty("isPrivate", isPrivate);
        payload.addProperty("inviteToken", inviteToken);

        if (invitationSender != null) {
            invitationSender.send(friendUserId, payload);
        }
        CommandJson.send(exchange, 200, Map.of("message", "Đã gửi lời mời thành công"));
    }

    private static long roomId(String path, String suffix) {
        String value = path.substring("/api/v1/rooms/".length(), path.length() - suffix.length());
        return Long.parseLong(value);
    }
}
