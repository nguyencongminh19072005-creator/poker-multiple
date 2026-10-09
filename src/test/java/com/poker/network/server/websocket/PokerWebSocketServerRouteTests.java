package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poker.dao.JdbcDatabase;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PokerWebSocketServerRouteTests {
    @Test
    void featureCommandsRequireAuthentication() throws Exception {
        PokerWebSocketServer server = new PokerWebSocketServer(
                new JdbcDatabase("jdbc:mysql://127.0.0.1:1/unused", "unused", "unused"), 0);
        server.startServer();
        try {
            for (String path : new String[]{"/api/v1/rankings/leaderboard",
                    "/api/v1/players/me/statistics", "/api/v1/admin/overview",
                    "/api/v1/rooms/1/chat/messages", "/api/v1/games/active/me",
                    "/api/v1/games/rooms/1/start",
                    "/app/game/00000000-0000-0000-0000-000000000001/action"}) {
                JsonObject reply = exchange(server.port(), path);
                assertThat(reply.get("status").getAsInt()).isEqualTo(401);
                assertThat(reply.getAsJsonObject("body").get("message").getAsString())
                        .contains("đăng nhập");
            }
            JsonObject missing = exchange(server.port(), "/api/v1/not-implemented");
            assertThat(missing.get("status").getAsInt()).isEqualTo(404);
            assertThat(missing.getAsJsonObject("body").get("message").getAsString())
                    .contains("not-implemented");
        } finally {
            server.stopServer();
        }
    }

    private static JsonObject exchange(int port, String path) throws Exception {
        CompletableFuture<JsonObject> response = new CompletableFuture<>();
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws"), new WebSocket.Listener() {
                    private final StringBuilder text = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket socket) {
                        socket.request(1);
                    }

                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(
                            WebSocket socket, CharSequence data, boolean last) {
                        text.append(data);
                        if (last) response.complete(JsonParser.parseString(text.toString()).getAsJsonObject());
                        socket.request(1);
                        return null;
                    }
                }).get(5, TimeUnit.SECONDS);
        try {
            JsonObject command = new JsonObject();
            command.addProperty("type", "request");
            command.addProperty("id", "route-test");
            command.addProperty("method", "GET");
            command.addProperty("path", path);
            socket.sendText(command.toString(), true).get(5, TimeUnit.SECONDS);
            return response.get(5, TimeUnit.SECONDS);
        } finally {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }
}
