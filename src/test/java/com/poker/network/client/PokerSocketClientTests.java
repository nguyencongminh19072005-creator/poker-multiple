package com.poker.network.client;

import com.poker.dao.JdbcDatabase;
import com.poker.network.server.websocket.PokerWebSocketServer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokerSocketClientTests {
    @Test
    void clientRejectsAnHttpUrlInsteadOfOpeningAnHttpApi() {
        assertThatThrownBy(() -> new PokerSocketClient("http://127.0.0.1:8081/ws"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("POKER_WS_URL");
    }

    @Test
    void separateClientsKeepIndependentWebSocketConnections() throws Exception {
        PokerWebSocketServer server = new PokerWebSocketServer(
                new JdbcDatabase("jdbc:mysql://127.0.0.1:1/unused", "unused", "unused"), 0);
        server.startServer();
        try (PokerSocketClient first = new PokerSocketClient("ws://127.0.0.1:" + server.port() + "/ws");
             PokerSocketClient second = new PokerSocketClient("ws://127.0.0.1:" + server.port() + "/ws")) {
            first.connect().get(5, TimeUnit.SECONDS);
            second.connect().get(5, TimeUnit.SECONDS);
            assertThat(first.isConnected()).isTrue();
            assertThat(second.isConnected()).isTrue();

            first.close();
            assertThat(second.isConnected()).isTrue();
            assertThat(second.request("GET", "/api/v1/not-implemented", null)
                    .handle((value, error) -> error).get(5, TimeUnit.SECONDS)).isNotNull();
        } finally {
            server.stopServer();
        }
    }

    @Test
    void clientExchangesCommandsWithoutHttpEndpoint() throws Exception {
        PokerWebSocketServer server = new PokerWebSocketServer(
                new JdbcDatabase("jdbc:mysql://127.0.0.1:1/unused", "unused", "unused"), 0);
        server.startServer();
        try (PokerSocketClient client = new PokerSocketClient(
                "ws://127.0.0.1:" + server.port() + "/ws")) {
            assertThat(client.request("GET", "/api/v1/rooms", null)
                    .handle((value, error) -> error).get(5, TimeUnit.SECONDS))
                    .isNotNull();
            assertThatThrownBy(() -> client.request("GET", "/api/v1/rooms", null)
                    .get(5, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Server WebSocket 401");
        } finally {
            server.stopServer();
        }
    }

    @Test
    void alreadyConnectedRequestsDoNotFireReconnectCallbackAgain() throws Exception {
        PokerWebSocketServer server = new PokerWebSocketServer(
                new JdbcDatabase("jdbc:mysql://127.0.0.1:1/unused", "unused", "unused"), 0);
        server.startServer();
        try (PokerSocketClient client = new PokerSocketClient(
                "ws://127.0.0.1:" + server.port() + "/ws")) {
            AtomicInteger connected = new AtomicInteger();
            client.onConnected(connected::incrementAndGet);
            client.connect().get(5, TimeUnit.SECONDS);
            client.connect().get(5, TimeUnit.SECONDS);
            client.request("GET", "/api/v1/rooms", null)
                    .handle((value, error) -> null).get(5, TimeUnit.SECONDS);
            assertThat(connected).hasValue(1);
        } finally {
            server.stopServer();
        }
    }
}
