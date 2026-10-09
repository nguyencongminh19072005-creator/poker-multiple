package com.poker.network.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Một kết nối WebSocket dùng cho cả lệnh, phản hồi và sự kiện realtime. */
public final class PokerSocketClient implements WebSocket.Listener, AutoCloseable {
    private final String url;
    private final Map<String, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonObject>> subscriptions = new ConcurrentHashMap<>();
    private final StringBuilder incoming = new StringBuilder();
    private volatile CompletableFuture<WebSocket> connection;
    private volatile WebSocket socket;
    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile String sessionId;
    private volatile JsonObject me;
    private volatile boolean userClosed;
    private volatile Consumer<String> errorHandler = message -> { };
    private volatile Runnable connectedHandler = () -> { };

    public PokerSocketClient() {
        this(System.getenv().getOrDefault("POKER_WS_URL", "ws://localhost:8080/ws"));
    }

    public PokerSocketClient(String url) {
        URI address = URI.create(url);
        if (!("ws".equalsIgnoreCase(address.getScheme())
                || "wss".equalsIgnoreCase(address.getScheme()))
                || address.getHost() == null || !"/ws".equals(address.getPath())) {
            throw new IllegalArgumentException("POKER_WS_URL phải có dạng ws://<máy-server>:<cổng>/ws");
        }
        this.url = url;
    }

    public String baseUrl() { return url; }
    public String accessToken() { return accessToken; }
    public JsonObject me() { return me; }
    public long userId() { return me == null ? 0 : me.get("id").getAsLong(); }
    public String username() { return me == null ? "" : me.get("username").getAsString(); }
    public boolean isConnected() { return socket != null && !socket.isOutputClosed(); }
    public void onError(Consumer<String> handler) { errorHandler = handler; }
    public void onConnected(Runnable handler) { connectedHandler = handler; }

    public synchronized CompletableFuture<Void> connect() {
        if (isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        if (connection != null && !connection.isCompletedExceptionally()) {
            return connection.thenAccept(ignored -> { });
        }
        userClosed = false;
        // JDK HttpClient performs only the WebSocket upgrade here; no REST request is sent.
        connection = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(url), this);
        return connection.thenAccept(ws -> {
            socket = ws;
            subscriptions.keySet().forEach(this::subscribeNow);
            connectedHandler.run();
        });
    }

    public CompletableFuture<Void> connect(String ignoredBase, String ignoredToken) {
        return connect();
    }

    public CompletableFuture<String> login(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        return request("POST", "/api/v1/auth/login", body, false).thenCompose(auth -> {
            accessToken = auth.get("accessToken").getAsString();
            refreshToken = auth.get("refreshToken").getAsString();
            sessionId = auth.get("sessionId").getAsString();
            return request("GET", "/api/v1/me", null, true).thenApply(profile -> {
                me = profile;
                return username();
            });
        });
    }

    public CompletableFuture<Void> register(String username, String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("displayName", username);
        return request("POST", "/api/v1/auth/register", body, false).thenApply(ignored -> null);
    }

    public CompletableFuture<JsonObject> updateDisplayName(String displayName) {
        JsonObject body = new JsonObject();
        body.addProperty("displayName", displayName);
        if (me != null && me.has("avatarUrl") && !me.get("avatarUrl").isJsonNull()) {
            body.addProperty("avatarUrl", me.get("avatarUrl").getAsString());
        }
        return request("PATCH", "/api/v1/me", body).thenApply(updated -> {
            me = updated;
            return updated;
        });
    }

    public CompletableFuture<Void> logout() {
        String oldRefresh = refreshToken;
        if (oldRefresh == null) return CompletableFuture.completedFuture(null);
        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", oldRefresh);
        body.addProperty("sessionId", sessionId);
        return request("POST", "/api/v1/auth/logout", body, true).handle((ignored, error) -> {
            clear();
            return null;
        });
    }

    public CompletableFuture<Void> heartbeat() {
        if (sessionId == null || accessToken == null) return CompletableFuture.completedFuture(null);
        JsonObject body = new JsonObject();
        body.addProperty("sessionId", sessionId);
        return request("POST", "/api/v1/auth/heartbeat", body).thenApply(ignored -> null);
    }

    public void clear() {
        accessToken = null;
        refreshToken = null;
        sessionId = null;
        me = null;
    }

    public CompletableFuture<JsonObject> request(String method, String path, JsonObject body) {
        return request(method, path, body, true);
    }

    private CompletableFuture<JsonObject> request(String method, String path, JsonObject body,
                                                  boolean authenticated) {
        return request(method, path, body, authenticated, true);
    }

    private CompletableFuture<JsonObject> request(String method, String path, JsonObject body,
                                                  boolean authenticated, boolean retry) {
        String token = authenticated ? accessToken : null;
        return exchange(method, path, body, token).thenCompose(reply -> {
            int status = reply.get("status").getAsInt();
            if (status == 401 && authenticated && retry && refreshToken != null) {
                return refreshAccessToken().thenCompose(ignored ->
                        request(method, path, body, true, false));
            }
            JsonElement data = reply.get("body");
            if (status < 200 || status >= 300) {
                String message = data != null && data.isJsonObject()
                        && data.getAsJsonObject().has("message")
                        ? data.getAsJsonObject().get("message").getAsString()
                        : "Yêu cầu không thành công";
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Server WebSocket " + status + " [" + path + "]: " + message));
            }
            if (data == null || data.isJsonNull()) return CompletableFuture.completedFuture(new JsonObject());
            if (data.isJsonObject()) return CompletableFuture.completedFuture(data.getAsJsonObject());
            JsonObject wrapper = new JsonObject();
            wrapper.add("items", data);
            return CompletableFuture.completedFuture(wrapper);
        });
    }

    public CompletableFuture<String> refreshAccessToken() {
        if (refreshToken == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Phiên đã hết hạn"));
        }
        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);
        return request("POST", "/api/v1/auth/refresh", body, false).thenApply(json -> {
            accessToken = json.get("accessToken").getAsString();
            if (json.has("sessionId")) sessionId = json.get("sessionId").getAsString();
            return accessToken;
        });
    }

    public CompletableFuture<JsonArray> list(String path) {
        return request("GET", path, null).thenApply(json -> json.getAsJsonArray("items"));
    }

    private CompletableFuture<JsonObject> exchange(String method, String path, JsonObject body, String token) {
        return connect().thenCompose(ignored -> {
            String id = UUID.randomUUID().toString();
            JsonObject command = new JsonObject();
            command.addProperty("type", "request");
            command.addProperty("id", id);
            command.addProperty("method", method);
            command.addProperty("path", path);
            if (token != null) command.addProperty("token", token);
            command.add("body", body == null ? new JsonObject() : body);
            CompletableFuture<JsonObject> result = new CompletableFuture<>();
            pending.put(id, result);
            result.whenComplete((value, error) -> pending.remove(id));
            socket.sendText(command.toString(), true).exceptionally(error -> {
                result.completeExceptionally(error);
                return null;
            });
            return result.orTimeout(12, TimeUnit.SECONDS);
        });
    }

    public void subscribe(String destination, Consumer<JsonObject> consumer) {
        subscriptions.put(destination, consumer);
        if (isConnected()) subscribeNow(destination);
    }

    public void unsubscribe(String destination) {
        subscriptions.remove(destination);
        if (isConnected()) control("unsubscribe", destination);
    }

    private void subscribeNow(String destination) {
        control("subscribe", destination);
    }

    private void control(String type, String destination) {
        JsonObject frame = new JsonObject();
        frame.addProperty("type", type);
        frame.addProperty("destination", destination);
        socket.sendText(frame.toString(), true);
    }

    public void send(String destination, JsonObject body) {
        if (!isConnected()) throw new IllegalStateException("WebSocket chưa kết nối");
        String path = destination;
        if (destination.matches("/app/room/\\d+/ready")) {
            path = destination.replaceFirst("^/app/room/", "/api/v1/rooms/");
        } else if (destination.matches("/app/room/\\d+/chat")) {
            path = destination.replaceFirst("^/app/room/", "/api/v1/rooms/") + "/messages";
        }
        request("POST", path, body).exceptionally(error -> {
            errorHandler.accept(ClientJson.cause(error));
            return null;
        });
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        synchronized (incoming) {
            incoming.append(data);
            if (last) {
                try {
                    JsonObject frame = JsonParser.parseString(incoming.toString()).getAsJsonObject();
                    String type = frame.get("type").getAsString();
                    if ("response".equals(type)) {
                        CompletableFuture<JsonObject> future = pending.get(frame.get("id").getAsString());
                        if (future != null) future.complete(frame);
                    } else if ("event".equals(type)) {
                        Consumer<JsonObject> receiver = subscriptions.get(frame.get("destination").getAsString());
                        if (receiver != null) receiver.accept(frame.getAsJsonObject("body"));
                    }
                } catch (RuntimeException error) {
                    errorHandler.accept("Thông báo WebSocket không hợp lệ");
                } finally {
                    incoming.setLength(0);
                }
            }
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (socket != null && webSocket != socket) return;
        disconnected("Mất kết nối WebSocket: " + error.getMessage());
    }

    @Override
    public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket, int status, String reason) {
        if (socket != null && webSocket != socket) return null;
        disconnected("WebSocket đã đóng: " + reason);
        return null;
    }

    private synchronized void disconnected(String message) {
        socket = null;
        connection = null;
        pending.values().forEach(future -> future.completeExceptionally(new IllegalStateException(message)));
        pending.clear();
        if (!userClosed) errorHandler.accept(message);
    }

    @Override
    public synchronized void close() {
        userClosed = true;
        WebSocket current = socket;
        socket = null;
        connection = null;
        subscriptions.clear();
        if (current != null) current.sendClose(WebSocket.NORMAL_CLOSURE, "logout");
    }
}
