package com.poker.controller.app;

import com.poker.controller.admin.AdminController;
import com.poker.controller.admin.AdminScreenController;
import com.poker.controller.auth.AuthController;
import com.poker.network.client.ClientJson;
import com.poker.controller.friend.FriendController;
import com.poker.controller.friend.FriendScreenController;
import com.poker.controller.game.GameController;
import com.poker.controller.lobby.LobbyController;
import com.poker.network.client.PokerSocketClient;
import com.poker.controller.profile.ProfileController;
import com.poker.controller.profile.ProfileScreenController;
import com.poker.controller.ranking.RankingController;
import com.poker.controller.ranking.RankingScreenController;
import com.poker.controller.realtime.RealtimeController;
import com.poker.controller.room.RoomController;
import com.poker.controller.room.RoomScreens;
import com.poker.controller.statistics.StatisticsController;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.poker.view.CreateRoomDialog;
import com.poker.view.HeaderNode;
import com.poker.view.LobbyView;
import com.poker.view.LoginView;
import com.poker.view.RegisterView;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Điều phối các controller chức năng, không trực tiếp gọi endpoint hoặc xử lý JSON. */
public final class PokerApplicationController implements HeaderNode.HeaderActions {
    private final PokerSocketClient socket = new PokerSocketClient();
    private final AuthController auth = new AuthController(socket);
    private final LobbyController lobbyController = new LobbyController(socket);
    private final RoomController roomController = new RoomController(socket);
    private final GameController gameController = new GameController(socket);
    private final StatisticsController statisticsController = new StatisticsController(socket);
    private final FriendController friendController = new FriendController(socket);
    private final ProfileController profileController = new ProfileController(socket);
    private final RankingController rankingController = new RankingController(socket);
    private final AdminController adminController = new AdminController(socket);
    private final SceneController scenes;
    private final RoomScreens roomScreens;
    private final FriendScreenController friendScreen;
    private final ProfileScreenController profileScreen;
    private final RankingScreenController rankingScreen;
    private final AdminScreenController adminScreen;
    private final RealtimeController realtime;
    private LobbyView lobbyView;
    private Timeline heartbeat;

    public PokerApplicationController(Stage stage) {
        scenes = new SceneController(stage);
        roomScreens = new RoomScreens(socket, scenes::show, this::showLobby,
                () -> lobbyView = null, scenes::showError);
        friendScreen = new FriendScreenController(friendController, scenes::show, this::showLobby, scenes::showError);
        profileScreen = new ProfileScreenController(profileController, statisticsController, auth, scenes::show,
                this::showLobby, this::logout, scenes::showError);
        rankingScreen = new RankingScreenController(rankingController, scenes::show, this::showLobby,
                scenes::showError);
        adminScreen = new AdminScreenController(adminController, auth, stage, scenes::show, this::logout,
                scenes::showError);
        realtime = new RealtimeController(socket, auth, roomScreens, this::refreshLobby,
                friendScreen::refresh, scenes::showError);
    }

    public void start() { showLogin(); }

    private void showLogin() {
        scenes.show(new LoginView(new LoginView.LoginActions() {
            @Override public void onLoginSuccess(String username) { afterLogin(); }
            @Override public void onSwitchToRegister() { showRegister(); }
        }, auth::login));
    }

    private void showRegister() {
        scenes.show(new RegisterView(new RegisterView.RegisterActions() {
            @Override public void onRegisterSuccess(String username) { showLogin(); }
            @Override public void onSwitchToLogin() { showLogin(); }
        }, auth::register));
    }

    private void afterLogin() {
        startHeartbeat();
        if ("ADMIN".equals(auth.role())) {
            adminScreen.show();
            return;
        }
        realtime.start();
        showLobby();
        gameController.activeRoomId()
                .thenCompose(roomController::details)
                .thenAccept(room -> Platform.runLater(() ->
                        roomScreens.enterRoom(room.roomId(), room.data())))
                .exceptionally(error -> null);
    }

    private void showLobby() {
        friendScreen.clear();
        roomScreens.reset();
        lobbyView = new LobbyView(auth.username(), new LobbyView.LobbyActions() {
            @Override public void onJoinRoom(String id, boolean privateRoom, boolean spectator) {
                roomScreens.join(ClientJson.parseId(id), null, spectator);
            }
            @Override public void onJoinPrivateRoom(String id, String password, boolean spectator) {
                roomScreens.join(ClientJson.parseId(id), password, spectator);
            }
            @Override public void onCreateRoomRequested() { showCreateRoom(); }
            @Override public void onOpenLeaderboard() { rankingScreen.show(); }
            @Override public void onOpenProfile() { profileScreen.show(); }
            @Override public void onOpenFriends() { friendScreen.show(); }
            @Override public void onOpenAdmin() { adminScreen.show(); }
            @Override public void onLogout() { logout(); }
        }, true);
        scenes.show(lobbyView);
        refreshLobby();
    }

    private void showCreateRoom() {
        CreateRoomDialog.show(scenes.stage(), (name, max, small, big, buyIn, privateRoom, password) ->
                perform(lobbyController.createRoom(name, max, small, big, buyIn, privateRoom, password),
                        created -> perform(roomController.details(created.roomId()),
                                verified -> roomScreens.enterRoom(verified.roomId(), verified.data()))), true);
    }

    private void refreshLobby() {
        if (lobbyView == null) return;
        perform(lobbyController.loadLobby(), data -> {
            if (lobbyView != null) lobbyView.showOnlineRooms(
                    data.rooms(), data.accountChips(), data.onlinePlayers());
        });
    }

    private void logout() {
        stopHeartbeat();
        lobbyView = null;
        roomScreens.reset();
        auth.logout().whenComplete((ignored, error) -> Platform.runLater(() -> {
            realtime.stop();
            showLogin();
        }));
    }

    private <T> void perform(CompletableFuture<T> task, Consumer<T> success) {
        task.thenAccept(value -> Platform.runLater(() -> success.accept(value)))
                .exceptionally(error -> {
                    Platform.runLater(() -> scenes.showError(ClientJson.cause(error)));
                    return null;
                });
    }

    @Override public void onResetGame() { }
    @Override public void onSwitchViewpoint(int seatIndex) { roomScreens.setHeroSeatIndex(seatIndex); }
    @Override public void onLeaveRoom() { roomScreens.leaveRoom(); }

    public void stop() {
        stopHeartbeat();
        if (auth.hasSession()) {
            try { auth.logout().orTimeout(2, java.util.concurrent.TimeUnit.SECONDS).join(); }
            catch (RuntimeException ignored) { auth.clearSession(); }
        }
        realtime.stop();
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeat = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            if (auth.hasSession()) auth.heartbeat().exceptionally(error -> {
                // The server's lease timeout handles brief network outages and abrupt exits.
                return null;
            });
        }));
        heartbeat.setCycleCount(Timeline.INDEFINITE);
        heartbeat.play();
    }

    private void stopHeartbeat() {
        if (heartbeat != null) {
            heartbeat.stop();
            heartbeat = null;
        }
    }
}
