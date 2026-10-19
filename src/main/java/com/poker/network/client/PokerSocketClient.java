
package com.poker.network.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client WebSocket cho ứng dụng Poker.
 *
 * Một kết nối xử lý hai loại dữ liệu:
 * - request/response: gửi lệnh và nhận kết quả theo ID.
 * - event: nhận cập nhật realtime theo destination.
 */
public final class PokerSocketClient
        implements WebSocket.Listener, AutoCloseable {

    private static final String DEFAULT_URL =
            "ws://localhost:8080/ws";

    private static final int REQUEST_TIMEOUT_SECONDS = 12;

    private final String url;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Lưu các request đang chờ Server phản hồi
    private final Map<String, CompletableFuture<JsonObject>> pending =
            new ConcurrentHashMap<>();

    // Lưu các kênh nhận sự kiện realtime
    private final Map<String, Consumer<JsonObject>> subscriptions =
            new ConcurrentHashMap<>();

    // Ghép các phần của thông điệp WebSocket
    private final StringBuilder incoming = new StringBuilder();

    private volatile CompletableFuture<WebSocket> connection;
    private volatile WebSocket socket;
    private volatile boolean userClosed;

    // Thông tin phiên đăng nhập
    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile String sessionId;
    private volatile JsonObject me;

    // Callback thông báo trạng thái
    private volatile Consumer<String> errorHandler = message -> {};
    private volatile Runnable connectedHandler = () -> {};


    // =====================================================
    // 1. KHỞI TẠO VÀ THIẾT LẬP KẾT NỐI
    // =====================================================

    public PokerSocketClient() {
        this(configuredUrl());
    }

    private static String configuredUrl() {
        String environment = System.getenv("POKER_WS_URL");
        if (environment != null && !environment.isBlank()) return environment.trim();
        // VS Code can inject envFile; direct ClientMain/run-client.cmd also reads
        // the project-local URL. The client never uses database settings.
        String projectSetting = urlFromEnvFile(Path.of("poker-mvc", ".env"));
        if (projectSetting != null) return projectSetting;
        String localSetting = urlFromEnvFile(Path.of(".env"));
        return localSetting != null ? localSetting : DEFAULT_URL;
    }

    static String urlFromEnvFile(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String setting = line.trim();
                if (!setting.startsWith("POKER_WS_URL=")) continue;
                String value = setting.substring("POKER_WS_URL=".length()).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value.isBlank() ? null : value;
            }
        } catch (IOException ignored) {
            // Keep the documented localhost default when no local config is readable.
        }
        return null;
    }

    public PokerSocketClient(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();

        if ((!"ws".equalsIgnoreCase(scheme)
                && !"wss".equalsIgnoreCase(scheme))
                || uri.getHost() == null
                || !"/ws".equals(uri.getPath())) {

            throw new IllegalArgumentException(
                    "POKER_WS_URL phải có dạng ws://<máy-server>:<cổng>/ws");
        }

        this.url = url;
    }

    public String baseUrl() {
        return url;
    }

    public String accessToken() {
        return accessToken;
    }

    public JsonObject me() {
        return me;
    }

    public long userId() {
        return me == null ? 0 : me.get("id").getAsLong();
    }

    public String username() {
        return me == null
                ? ""
                : me.get("username").getAsString();
    }

    public boolean isConnected() {
        return socket != null && !socket.isOutputClosed();
    }

    public void onError(Consumer<String> handler) {
        errorHandler = handler;
    }

    public void onConnected(Runnable handler) {
        connectedHandler = handler;
    }

    /**
     * Thiết lập kết nối WebSocket bất đồng bộ.
     */
    public synchronized CompletableFuture<Void> connect() {

        // Đã kết nối thì không tạo kết nối mới
        if (isConnected()) {
            return CompletableFuture.completedFuture(null);
        }

        // Nếu đang kết nối thì chờ kết nối hiện tại
        if (connection != null && !connection.isDone()) {
            return connection.thenAccept(ignored -> {});
        }

        userClosed = false;

        connection = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(url), this);

        return connection.thenAccept(ws -> {
            socket = ws;

            // Đăng ký lại các kênh sau khi kết nối
            subscriptions.keySet().forEach(this::subscribeNow);

            connectedHandler.run();
        });
    }

    /**
     * Giữ chữ ký cũ để tương thích với các màn hình.
     */
    public CompletableFuture<Void> connect(
            String ignoredBase, String ignoredToken) {

        return connect();
    }


    // =====================================================
    // 2. TÀI KHOẢN VÀ PHIÊN ĐĂNG NHẬP
    // =====================================================

    /**
     * Đăng nhập và lấy thông tin người chơi.
     */
    public CompletableFuture<String> login(
            String username, String password) {

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        return request("POST", "/api/v1/auth/login", body, false)
                .thenCompose(auth -> {

                    // Lưu thông tin xác thực
                    accessToken = auth.get("accessToken").getAsString();
                    refreshToken = auth.get("refreshToken").getAsString();
                    sessionId = auth.get("sessionId").getAsString();

                    // Lấy hồ sơ người dùng
                    return request("GET", "/api/v1/me", null);
                })
                .thenApply(profile -> {
                    me = profile;
                    return username();
                });
    }

    /**
     * Đăng ký tài khoản mới.
     */
    public CompletableFuture<Void> register(
            String username, String email, String password) {

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("displayName", username);

        return request("POST", "/api/v1/auth/register", body, false)
                .thenApply(ignored -> null);
    }

    /**
     * Cập nhật tên hiển thị của người chơi.
     */
    public CompletableFuture<JsonObject> updateDisplayName(
            String displayName) {
        return updateProfile(displayName, null);
    }

    public CompletableFuture<JsonObject> updateProfile(String displayName, String avatarUrl) {

        JsonObject body = new JsonObject();
        body.addProperty("displayName", displayName);
        if (avatarUrl != null) body.addProperty("avatarUrl", avatarUrl);

        return request("PATCH", "/api/v1/me", body)
                .thenApply(updated -> {
                    me = updated;
                    return updated;
                });
    }

    /**
     * Đăng xuất và xóa thông tin đăng nhập.
     */
    public CompletableFuture<Void> logout() {

        String token = refreshToken;

        if (token == null) {
            return CompletableFuture.completedFuture(null);
        }

        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", token);
        body.addProperty("sessionId", sessionId);

        return request("POST", "/api/v1/auth/logout", body)
                .handle((ignored, error) -> {
                    clear();
                    return null;
                });
    }

    /**
     * Gửi heartbeat để duy trì phiên đăng nhập.
     */
    public CompletableFuture<Void> heartbeat() {

        if (sessionId == null || accessToken == null) {
            return CompletableFuture.completedFuture(null);
        }

        JsonObject body = new JsonObject();
        body.addProperty("sessionId", sessionId);

        return request("POST", "/api/v1/auth/heartbeat", body)
                .thenApply(ignored -> null);
    }

    /**
     * Xóa thông tin xác thực trên Client.
     */
    public void clear() {
        accessToken = null;
        refreshToken = null;
        sessionId = null;
        me = null;
    }

    /**
     * Làm mới access token bằng refresh token.
     */
    public CompletableFuture<String> refreshAccessToken() {

        if (refreshToken == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Phiên đã hết hạn"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);

        return request("POST", "/api/v1/auth/refresh", body, false)
                .thenApply(auth -> {

                    accessToken = auth.get("accessToken").getAsString();

                    if (auth.has("sessionId")) {
                        sessionId = auth.get("sessionId").getAsString();
                    }

                    return accessToken;
                });
    }


    // =====================================================
    // 3. REQUEST / RESPONSE
    // =====================================================

    /**
     * Gửi request có xác thực đến Server.
     */
    public CompletableFuture<JsonObject> request(
            String method, String path, JsonObject body) {

        return request(method, path, body, true);
    }

    private CompletableFuture<JsonObject> request(
            String method,
            String path,
            JsonObject body,
            boolean authenticated) {

        return request(method, path, body, authenticated, true);
    }

    /**
     * Gửi yêu cầu và xử lý phản hồi.
     *
     * Nếu access token hết hạn:
     * - Refresh token.
     * - Gửi lại request tối đa một lần.
     */
    private CompletableFuture<JsonObject> request(
            String method,
            String path,
            JsonObject body,
            boolean authenticated,
            boolean allowRetry) {

        String token = authenticated ? accessToken : null;

        return exchange(method, path, body, token)
                .thenCompose(response -> {

                    int status = response.get("status").getAsInt();

                    // Token hết hạn: refresh và gửi lại
                    if (status == 401
                            && authenticated
                            && allowRetry
                            && refreshToken != null) {

                        return refreshAccessToken()
                                .thenCompose(ignored ->
                                        request(method, path, body, true, false));
                    }

                    // Server trả về lỗi
                    if (status < 200 || status >= 300) {

                        return CompletableFuture.failedFuture(
                                new IllegalStateException(
                                        formatServerError(path, status, response)));
                    }

                    // Request thành công
                    return CompletableFuture.completedFuture(
                            extractBody(response));
                });
    }

    /**
     * Tạo thông báo lỗi từ phản hồi Server.
     */
    private String formatServerError(
            String path, int status, JsonObject response) {

        JsonElement data = response.get("body");
        String message = "Yêu cầu không thành công";

        if (data != null
                && data.isJsonObject()
                && data.getAsJsonObject().has("message")) {

            message = data.getAsJsonObject()
                    .get("message").getAsString();
        }

        return "Server WebSocket " + status
                + " [" + path + "]: " + message;
    }

    /**
     * Trích xuất dữ liệu body từ response.
     */
    private JsonObject extractBody(JsonObject response) {

        JsonElement data = response.get("body");

        if (data == null || data.isJsonNull()) {
            return new JsonObject();
        }

        if (data.isJsonObject()) {
            return data.getAsJsonObject();
        }

        // Bọc dữ liệu không phải object vào trường items
        JsonObject result = new JsonObject();
        result.add("items", data);

        return result;
    }

    /**
     * Lấy danh sách dữ liệu từ Server.
     */
    public CompletableFuture<JsonArray> list(String path) {

        return request("GET", path, null)
                .thenApply(result ->
                        result.getAsJsonArray("items"));
    }

    /**
     * Đóng gói request thành JSON, gửi qua WebSocket
     * và chờ Server phản hồi theo request ID.
     */
    private CompletableFuture<JsonObject> exchange(
            String method,
            String path,
            JsonObject body,
            String token) {

        return connect().thenCompose(ignored -> {

            // Mỗi request có một ID riêng
            String id = UUID.randomUUID().toString();

            // Tạo thông điệp gửi Server
            JsonObject frame = new JsonObject();

            frame.addProperty("type", "request");
            frame.addProperty("id", id);
            frame.addProperty("method", method);
            frame.addProperty("path", path);

            if (token != null) {
                frame.addProperty("token", token);
            }

            frame.add(
                    "body",
                    body == null ? new JsonObject() : body
            );

            // Tạo Future chờ response tương ứng
            CompletableFuture<JsonObject> result =
                    new CompletableFuture<>();

            pending.put(id, result);

            // Xóa request khỏi pending khi hoàn thành
            result.whenComplete((value, error) ->
                    pending.remove(id, result));

            WebSocket current = socket;

            if (current == null || current.isOutputClosed()) {

                result.completeExceptionally(
                        new IllegalStateException(
                                "WebSocket chưa kết nối"));

                return result;
            }

            // Gửi JSON qua WebSocket
            current.sendText(frame.toString(), true)
                    .whenComplete((sent, error) -> {

                        if (error != null) {
                            result.completeExceptionally(error);
                        }
                    });

            // Chờ response tối đa 12 giây
            return result.orTimeout(
                    REQUEST_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
        });
    }


    // =====================================================
    // 4. SUBSCRIBE VÀ GỬI HÀNH ĐỘNG REALTIME
    // =====================================================

    /**
     * Đăng ký nhận sự kiện realtime từ một destination.
     */
    public void subscribe(
            String destination,
            Consumer<JsonObject> consumer) {

        subscriptions.put(destination, consumer);

        if (isConnected()) {
            subscribeNow(destination);
        }
    }

    /**
     * Hủy đăng ký một destination.
     */
    public void unsubscribe(String destination) {

        subscriptions.remove(destination);

        if (isConnected()) {
            control("unsubscribe", destination);
        }
    }

    /**
     * Gửi yêu cầu subscribe tới Server.
     */
    private void subscribeNow(String destination) {
        control("subscribe", destination);
    }

    /**
     * Gửi thông điệp điều khiển subscribe/unsubscribe.
     */
    private void control(String type, String destination) {

        JsonObject frame = new JsonObject();

        frame.addProperty("type", type);
        frame.addProperty("destination", destination);

        socket.sendText(frame.toString(), true);
    }

    /**
     * Gửi hành động game tới Server.
     */
    public void send(String destination, JsonObject body) {

        if (!isConnected()) {
            throw new IllegalStateException(
                    "WebSocket chưa kết nối");
        }

        String path = resolvePath(destination);

        request("POST", path, body)
                .exceptionally(error -> {

                    errorHandler.accept(
                            ClientJson.cause(error));

                    return null;
                });
    }

    /**
     * Chuyển đường dẫn hành động thành API path.
     */
    private String resolvePath(String destination) {

        if (destination.matches("/app/room/\\d+/ready")) {

            return destination.replaceFirst(
                    "^/app/room/",
                    "/api/v1/rooms/"
            );
        }

        if (destination.matches("/app/room/\\d+/chat")) {

            return destination.replaceFirst(
                    "^/app/room/",
                    "/api/v1/rooms/"
            ) + "/messages";
        }

        return destination;
    }


    // =====================================================
    // 5. NHẬN VÀ XỬ LÝ THÔNG ĐIỆP TỪ SERVER
    // =====================================================

    /**
     * Được gọi khi kết nối WebSocket được mở.
     */
    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    /**
     * Nhận dữ liệu WebSocket.
     *
     * Thông điệp có thể được gửi thành nhiều phần.
     * Chỉ xử lý JSON khi nhận đủ thông điệp (last = true).
     */
    @Override
    public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last) {

        synchronized (incoming) {

            // Ghép dữ liệu nhận được
            incoming.append(data);

            if (last) {
                try {

                    JsonObject frame = JsonParser
                            .parseString(incoming.toString())
                            .getAsJsonObject();

                    handleFrame(frame);

                } catch (RuntimeException error) {

                    errorHandler.accept(
                            "Thông báo WebSocket không hợp lệ");

                } finally {

                    incoming.setLength(0);
                }
            }
        }

        // Yêu cầu nhận thông điệp tiếp theo
        webSocket.request(1);

        return null;
    }

    /**
     * Phân loại thông điệp từ Server.
     *
     * response: phản hồi cho request đã gửi.
     * event: sự kiện realtime của phòng chơi.
     */
    private void handleFrame(JsonObject frame) {

        String type = frame.get("type").getAsString();

        if ("response".equals(type)) {

            String id = frame.get("id").getAsString();

            // Tìm request đang đợi theo ID
            CompletableFuture<JsonObject> waiting =
                    pending.get(id);

            if (waiting != null) {
                waiting.complete(frame);
            }

        } else if ("event".equals(type)) {

            String destination = frame
                    .get("destination").getAsString();

            // Tìm callback đã đăng ký với destination
            Consumer<JsonObject> receiver =
                    subscriptions.get(destination);

            if (receiver != null) {

                receiver.accept(
                        frame.getAsJsonObject("body"));
            }
        }
    }


    // =====================================================
    // 6. XỬ LÝ LỖI VÀ ĐÓNG KẾT NỐI
    // =====================================================

    /**
     * Xử lý khi WebSocket gặp lỗi.
     */
    @Override
    public void onError(
            WebSocket webSocket, Throwable error) {

        if (socket != null && webSocket != socket) {
            return;
        }

        disconnected(
                "Mất kết nối WebSocket: " + error.getMessage());
    }

    /**
     * Xử lý khi Server đóng kết nối.
     */
    @Override
    public CompletionStage<?> onClose(
            WebSocket webSocket,
            int status,
            String reason) {

        if (socket != null && webSocket != socket) {
            return null;
        }

        disconnected("WebSocket đã đóng: " + reason);

        return null;
    }

    /**
     * Dọn trạng thái khi mất kết nối.
     */
    private synchronized void disconnected(String message) {

        socket = null;
        connection = null;

        failPending(message);

        if (!userClosed) {
            errorHandler.accept(message);
        }
    }

    /**
     * Báo lỗi cho tất cả request đang chờ.
     */
    private void failPending(String message) {

        pending.values().forEach(future ->
                future.completeExceptionally(
                        new IllegalStateException(message)));

        pending.clear();
    }

    /**
     * Chủ động đóng kết nối WebSocket.
     */
    @Override
    public synchronized void close() {

        userClosed = true;

        WebSocket current = socket;

        socket = null;
        connection = null;

        subscriptions.clear();

        failPending("WebSocket đã đóng");

        if (current != null) {
            current.sendClose(
                    WebSocket.NORMAL_CLOSURE,
                    "logout"
            );
        }
    }
}
