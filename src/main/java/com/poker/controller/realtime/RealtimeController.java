package com.poker.controller.realtime;

import com.poker.controller.auth.AuthController;
import com.poker.network.client.PokerSocketClient;
import com.poker.controller.room.RoomScreens;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

/** Quản lý đăng ký sự kiện và kết nối lại trên WebSocket dùng chung. */
public final class RealtimeController {
    private final PokerSocketClient socket;
    private final AuthController auth;
    private final RoomScreens rooms;
    private final Runnable refreshLobby;
    private final Runnable refreshFriends;
    private final java.util.function.Consumer<String> showError;
    private boolean reconnectPending;

    public RealtimeController(PokerSocketClient socket, AuthController auth, RoomScreens rooms,
                              Runnable refreshLobby, Runnable refreshFriends,
                              java.util.function.Consumer<String> showError) {
        this.socket = socket;
        this.auth = auth;
        this.rooms = rooms;
        this.refreshLobby = refreshLobby;
        this.refreshFriends = refreshFriends;
        this.showError = showError;
    }

    public void start() {
        socket.onError(message -> Platform.runLater(() -> {
            if (!socket.isConnected()) reconnect(); else showError.accept(message);
        }));
        socket.onConnected(() -> Platform.runLater(() -> {
            reconnectPending = false;
            refreshLobby.run();
            rooms.refreshRoom();
            rooms.refreshGame();
        }));
        socket.subscribe("/topic/lobby", event -> Platform.runLater(refreshLobby));
        socket.subscribe("/user/queue/private", event -> Platform.runLater(rooms::refreshGame));
        socket.subscribe("/user/queue/notifications", event -> Platform.runLater(refreshFriends));
        connect();
    }

    private void connect() {
        socket.connect().exceptionally(error -> {
            Platform.runLater(this::reconnect);
            return null;
        });
    }

    private void reconnect() {
        if (reconnectPending || !auth.hasSession() || socket.isConnected()) return;
        reconnectPending = true;
        rooms.setReconnecting();
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> auth.refreshAccessToken().whenComplete((token, error) ->
                Platform.runLater(() -> {
                    reconnectPending = false;
                    if (error == null) connect();
                    else if (auth.hasSession()) reconnect();
                })));
        delay.play();
    }

    public void stop() {
        reconnectPending = false;
        socket.close();
    }
}
