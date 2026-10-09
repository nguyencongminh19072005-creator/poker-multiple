package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.game.ServerGameController;
import com.poker.model.game.GameOperations;
import com.poker.controller.server.system.ServerSystemController;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Điểm vào mạng duy nhất: yêu cầu/phản hồi và sự kiện đều qua WebSocket. */
public final class PokerWebSocketServer extends WebSocketServer {
    private final ServerSystemController system;
    private final ClientPresence presence;
    private final ConnectionManager connections = new ConnectionManager();
    private final ScheduledExecutorService sweep = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "poker-presence-sweep");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService turnClock = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "poker-turn-clock");
        thread.setDaemon(true);
        return thread;
    });
    private final CountDownLatch started = new CountDownLatch(1);
    private final ExecutorService commands = Executors.newFixedThreadPool(
            Math.max(4, Math.min(16, Runtime.getRuntime().availableProcessors())), r -> {
                Thread thread = new Thread(r, "poker-ws-command");
                thread.setDaemon(true);
                return thread;
            });
    private volatile Exception startupError;

    private final CommandHandler auth;
    private final CommandHandler profile;
    private final CommandHandler rooms;
    private final CommandHandler friends;
    private final CommandHandler rankings;
    private final CommandHandler statistics;
    private final CommandHandler admin;
    private final CommandHandler games;
    private final ServerGameController gameController;

    public PokerWebSocketServer(JdbcDatabase database, int port) {
        super(new InetSocketAddress(port));
        system = new ServerSystemController(database);
        gameController = new ServerGameController(database);
        presence = new ClientPresence(database, gameController);
        auth = new AuthCommandHandler(database, presence);
        profile = new ProfileCommandHandler(database);
        rooms = new RoomCommandHandler(database, gameController);
        friends = new FriendCommandHandler(database);
        rankings = new RankingCommandHandler(database);
        statistics = new StatisticsCommandHandler(database);
        admin = new AdminCommandHandler(database);
        games = new GameCommandHandler(gameController);
    }

    public void abortOrphanedGames() throws SQLException { gameController.abortOrphanedGames(); }

    public void startServer() throws InterruptedException, IOException {
        start();
        if (!started.await(5, TimeUnit.SECONDS)) {
            throw new IOException("WebSocket server không khởi động trong 5 giây");
        }
        if (startupError != null) {
            throw new IOException("Không mở được cổng WebSocket", startupError);
        }
        sweep.scheduleWithFixedDelay(() -> {
            if (presence.expireStale() > 0) {
                broadcastEvent("/topic/lobby");
                broadcastEvent("/user/queue/notifications");
                connections.broadcastSubscribedStateTopics();
            }
        }, 5, 5, TimeUnit.SECONDS);
        turnClock.scheduleAtFixedRate(this::broadcastTurnTicks, 0, 1, TimeUnit.SECONDS);
    }

    public void stopServer() throws InterruptedException {
        sweep.shutdownNow();
        turnClock.shutdownNow();
        stop(1);
        commands.shutdown();
        commands.awaitTermination(5, TimeUnit.SECONDS);
    }

    public int port() {
        return getPort();
    }

    @Override
    public void onOpen(WebSocket socket, ClientHandshake handshake) {
        if (!"/ws".equals(handshake.getResourceDescriptor())) {
            socket.close(1008, "Chỉ hỗ trợ /ws");
            return;
        }
        connections.open(socket);
    }

    @Override
    public void onClose(WebSocket socket, int code, String reason, boolean remote) {
        ConnectionManager.Session connection = connections.close(socket);
        if (connection == null || connection.userId == null) return;
        try {
            commands.execute(() -> {
                try {
                    presence.logout(connection.userId, connection.sessionId);
                    broadcastEvent("/topic/lobby");
                    broadcastEvent("/user/queue/notifications");
                    connections.broadcastSubscribedStateTopics();
                } catch (SQLException error) {
                    error.printStackTrace();
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException stopping) {
            // Server shutdown already closed the worker pool.
        }
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        ConnectionManager.Session connection = connections.get(socket);
        if (connection == null || commands.isShutdown()) return;
        try {
            if (!connection.enqueue(commands, () -> processMessage(socket, text))) {
                sendError(socket, "", 503, "Quá nhiều lệnh đang chờ xử lý");
            }
        } catch (java.util.concurrent.RejectedExecutionException stopping) {
            // A command may race the worker pool shutdown after the check above.
        }
    }

    private void processMessage(WebSocket socket, String text) {
        JsonObject frame;
        try {
            frame = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException invalid) {
            sendError(socket, "", 400, "Lệnh WebSocket không hợp lệ");
            return;
        }
        ConnectionManager.Session connection = connections.get(socket);
        if (connection == null) return;
        String type = value(frame, "type");
        if ("subscribe".equals(type)) {
            connection.subscriptions.add(value(frame, "destination"));
            return;
        }
        if ("unsubscribe".equals(type)) {
            connection.subscriptions.remove(value(frame, "destination"));
            return;
        }
        String id = value(frame, "id");
        String method = value(frame, "method");
        String path = value(frame, "path");
        if (!"request".equals(type) || id.isBlank() || method.isBlank() || path.isBlank()) {
            sendError(socket, id, 400, "Thiếu thông tin lệnh WebSocket");
            return;
        }
        JsonObject body = frame.has("body") && frame.get("body").isJsonObject()
                ? frame.getAsJsonObject("body") : new JsonObject();
        CommandExchange exchange;
        try {
            exchange = new CommandExchange(method, path, body, value(frame, "token"));
            route(exchange);
        } catch (Exception error) {
            error.printStackTrace();
            sendError(socket, id, 500, "Lỗi xử lý lệnh WebSocket");
            return;
        }
        JsonObject reply = new JsonObject();
        reply.addProperty("type", "response");
        reply.addProperty("id", id);
        reply.addProperty("status", exchange.status());
        reply.add("body", exchange.response() == null ? new JsonObject() : exchange.response());
        socket.send(reply.toString());
        if (exchange.status() >= 200 && exchange.status() < 300) {
            afterSuccess(socket, connection, method, exchange.getRequestURI().getPath(), exchange.response());
        }
    }

    private void route(CommandExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/health".equals(path)) {
            boolean healthy = system.databaseHealthy();
            CommandJson.send(exchange, healthy ? 200 : 503,
                    Map.of("status", healthy ? "UP" : "DOWN", "database", healthy ? "UP" : "DOWN"));
        } else if (path.startsWith("/api/v1/auth/")) {
            auth.handle(exchange);
        } else if (path.equals("/api/v1/me")) {
            profile.handle(exchange);
        } else if (path.startsWith("/api/v1/rooms")) {
            rooms.handle(exchange);
        } else if (path.startsWith("/api/v1/games/") || path.startsWith("/app/game/")) {
            games.handle(exchange);
        } else if (path.startsWith("/api/v1/friends") || path.startsWith("/api/v1/friend-requests")) {
            friends.handle(exchange);
        } else if (path.startsWith("/api/v1/rankings")) {
            rankings.handle(exchange);
        } else if (path.startsWith("/api/v1/players") || path.startsWith("/api/v1/analytics")) {
            statistics.handle(exchange);
        } else if (path.startsWith("/api/v1/admin")) {
            admin.handle(exchange);
        } else {
            CommandJson.error(exchange, 404, "Server chưa hỗ trợ lệnh: " + path);
        }
    }

    private void afterSuccess(WebSocket socket, ConnectionManager.Session connection, String method,
                              String path, com.google.gson.JsonElement response) {
        if ("POST".equals(method) && (path.endsWith("/login") || path.endsWith("/refresh"))
                && response != null && response.isJsonObject()) {
            JsonObject data = response.getAsJsonObject();
            connection.sessionId = value(data, "sessionId");
            connection.userId = CommandJson.userId(
                    new CommandExchange("GET", "/", null, value(data, "accessToken")));
            if (connection.userId != null) {
                try { gameController.reconnectUser(connection.userId); }
                catch (SQLException error) { error.printStackTrace(); }
            }
            broadcastEvent("/topic/lobby");
            broadcastEvent("/user/queue/notifications");
            connections.broadcastSubscribedStateTopics();
        } else if ("POST".equals(method) && path.endsWith("/logout")) {
            connection.userId = null;
            connection.sessionId = null;
            broadcastEvent("/topic/lobby");
            broadcastEvent("/user/queue/notifications");
            connections.broadcastSubscribedStateTopics();
        } else if (path.startsWith("/api/v1/games/") || path.startsWith("/app/game/")) {
            if (!"GET".equals(method)) {
                boolean action = path.startsWith("/app/game/");
                JsonObject data = response != null && response.isJsonObject()
                        ? response.getAsJsonObject() : new JsonObject();
                JsonObject publicState = data.has("publicState") && data.get("publicState").isJsonObject()
                        ? data.getAsJsonObject("publicState") : null;
                boolean finished = publicState != null && "FINISHED".equals(value(publicState, "phase"));
                // Ordinary actions only invalidate the in-memory game snapshot. Room/lobby
                // refreshes query MySQL and are needed only for lifecycle changes.
                if (!action || finished) broadcastEvent("/topic/lobby");
                String[] parts = path.split("/");
                try {
                    long roomId = path.matches("/api/v1/games/rooms/\\d+/start")
                            ? Long.parseLong(parts[5])
                            : gameController.roomForGame(UUID.fromString(
                                    path.startsWith("/app/game/") ? parts[3] : parts[4]));
                    if (!action || finished) broadcastEvent("/topic/room/" + roomId);
                } catch (IllegalArgumentException ignored) {
                    // A command may complete after its room has already been removed.
                }
                if (path.startsWith("/app/game/") || path.matches("/api/v1/games/[0-9a-fA-F-]+/leave")) {
                    int gamePart = path.startsWith("/app/game/") ? 3 : 4;
                    if (parts.length > gamePart) broadcastEvent("/topic/game/" + parts[gamePart]);
                } else if (response != null && response.isJsonObject()) {
                    if (data.has("gameId")) broadcastEvent("/topic/game/" + value(data, "gameId"));
                }
            }
        } else if (path.startsWith("/api/v1/friends")
                || path.startsWith("/api/v1/friend-requests")) {
            if (!"GET".equals(method)) broadcastEvent("/user/queue/notifications");
        } else if (path.startsWith("/api/v1/rooms")) {
            if (!"GET".equals(method)) {
                if ("POST".equals(method) && path.matches("/api/v1/rooms/\\d+/chat/messages")) {
                    // Broadcast only an invalidation signal: message content must be
                    // fetched through the membership-checked chat command.
                    JsonObject changed = new JsonObject();
                    changed.addProperty("type", "CHAT_CHANGED");
                    broadcastEvent("/topic/room/" + path.split("/")[4], changed);
                    return;
                }
                broadcastEvent("/topic/lobby");
                String[] parts = path.split("/");
                if (parts.length > 4 && parts[4].matches("\\d+")) {
                    broadcastEvent("/topic/room/" + parts[4]);
                } else if (response != null && response.isJsonObject()) {
                    JsonObject data = response.getAsJsonObject();
                    if (data.has("room") && data.get("room").isJsonObject()) {
                        broadcastEvent("/topic/room/" + value(data.getAsJsonObject("room"), "id"));
                    }
                }
            }
        }
    }

    private void broadcastEvent(String destination) {
        connections.broadcast(destination);
    }

    private void broadcastTurnTicks() {
        try {
            for (GameOperations.TurnTick tick : gameController.tickTurns()) {
                String destination = "/topic/game/" + tick.gameId();
                if (tick.stateChanged()) {
                    broadcastEvent(destination);
                    if (tick.finished()) {
                        broadcastEvent("/topic/room/" + tick.roomId());
                        broadcastEvent("/topic/lobby");
                    }
                }
                if (tick.turnId() != null) {
                    JsonObject payload = new JsonObject();
                    payload.addProperty("remainingSeconds", tick.remainingSeconds());
                    payload.addProperty("turnId", tick.turnId().toString());
                    JsonObject event = new JsonObject();
                    event.addProperty("type", "TIMER_UPDATE");
                    event.add("payload", payload);
                    broadcastEvent(destination, event);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private void broadcastEvent(String destination, JsonObject body) {
        connections.broadcast(destination, body);
    }

    private static void sendError(WebSocket socket, String id, int status, String message) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        JsonObject reply = new JsonObject();
        reply.addProperty("type", "response");
        reply.addProperty("id", id);
        reply.addProperty("status", status);
        reply.add("body", body);
        socket.send(reply.toString());
    }

    private static String value(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsString() : "";
    }

    @Override
    public void onError(WebSocket socket, Exception error) {
        if (socket == null) {
            startupError = error;
            started.countDown();
        }
        error.printStackTrace();
    }

    @Override
    public void onStart() {
        started.countDown();
    }

}
