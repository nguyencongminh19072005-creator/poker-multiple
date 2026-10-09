package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import com.poker.controller.server.game.ServerGameController;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Parses the existing game paths and delegates game rules to the MVC controller/model. */
final class GameCommandHandler implements CommandHandler {
    private final ServerGameController games;

    GameCommandHandler(ServerGameController games) { this.games = games; }

    @Override
    public void handle(CommandExchange exchange) throws IOException {
        Long userId = CommandJson.userId(exchange);
        if (userId == null) {
            CommandJson.error(exchange, 401, "Bạn chưa đăng nhập");
            return;
        }
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        try {
            if ("GET".equals(method) && path.equals("/api/v1/games/active/me")) {
                Long roomId = games.activeRoomForUser(userId);
                if (roomId == null) notFound(exchange);
                else CommandJson.send(exchange, 200, Map.of("roomId", roomId));
            } else if ("GET".equals(method) && path.matches("/api/v1/games/active/room/\\d+")) {
                long roomId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                UUID gameId = games.activeForRoom(roomId, userId);
                if (gameId == null) notFound(exchange);
                else CommandJson.send(exchange, 200, Map.of("gameId", gameId.toString()));
            } else if ("POST".equals(method) && path.matches("/api/v1/games/rooms/\\d+/start")) {
                long roomId = Long.parseLong(path.substring("/api/v1/games/rooms/".length(),
                        path.length() - "/start".length()));
                CommandJson.send(exchange, 200, Map.of("gameId", games.start(roomId, userId).toString()));
            } else if ("GET".equals(method) && path.matches("/api/v1/games/[0-9a-fA-F-]+/snapshot")) {
                CommandJson.send(exchange, 200, games.snapshot(gameId(path, "/snapshot"), userId));
            } else if ("POST".equals(method) && path.matches("/api/v1/games/[0-9a-fA-F-]+/leave")) {
                games.leave(gameId(path, "/leave"), userId);
                CommandJson.send(exchange, 200, Map.of("status", "LEFT"));
            } else if ("POST".equals(method) && path.matches("/app/game/[0-9a-fA-F-]+/action")) {
                JsonObject body = CommandJson.body(exchange);
                UUID gameId = UUID.fromString(path.substring("/app/game/".length(),
                        path.length() - "/action".length()));
                long amount = body.has("amount") ? body.get("amount").getAsLong() : 0;
                CommandJson.send(exchange, 200, games.act(gameId, userId,
                        CommandJson.text(body, "actionType"), amount,
                        CommandJson.text(body, "turnId"), CommandJson.text(body, "clientActionId")));
            } else {
                notFound(exchange);
            }
        } catch (IllegalArgumentException invalid) {
            CommandJson.error(exchange, 400, invalid.getMessage());
        } catch (Exception error) {
            error.printStackTrace();
            CommandJson.error(exchange, 500, "Lỗi xử lý ván bài");
        }
    }

    private static UUID gameId(String path, String suffix) {
        return UUID.fromString(path.substring("/api/v1/games/".length(), path.length() - suffix.length()));
    }

    private static void notFound(CommandExchange exchange) throws IOException {
        CommandJson.error(exchange, 404, "Không tìm thấy ván đang chơi");
    }
}
