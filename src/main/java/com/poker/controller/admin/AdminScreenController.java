package com.poker.controller.admin;

import com.poker.controller.auth.AuthController;
import com.poker.network.client.ClientJson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import com.poker.view.AdminDashboardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Luồng quản trị của Vũ Duy Thái; dữ liệu đi qua WebSocket. */
public final class AdminScreenController {
    private final AdminController admin;
    private final AuthController auth;
    private final Stage stage;
    private final Consumer<Parent> show;
    private final Runnable logout;
    private final Consumer<String> error;

    public AdminScreenController(AdminController admin, AuthController auth, Stage stage,
                          Consumer<Parent> show, Runnable logout, Consumer<String> error) {
        this.admin = admin;
        this.auth = auth;
        this.stage = stage;
        this.show = show;
        this.logout = logout;
        this.error = error;
    }

    public void show() {
        if (!"ADMIN".equals(auth.role())) { error.accept("Chỉ quản trị viên được truy cập"); return; }
        AdminDashboardView view = new AdminDashboardView(new AdminDashboardView.AdminActions() {
            @Override public void onBackToLobby() { logout.run(); }
            @Override public void onToggleAccount(AdminDashboardView.UserModel user) { toggle(user); }
        }, true);
        show.accept(view);

        var overview = admin.overview();
        var users = admin.users();
        var rooms = admin.rooms();
        CompletableFuture.allOf(overview, users, rooms).thenRun(() -> Platform.runLater(() -> {
            JsonObject summary = overview.join();
            Map<String, String> values = new HashMap<>();
            values.put("Total Users", Long.toString(ClientJson.number(summary, "totalUsers")));
            values.put("Active Users", Long.toString(ClientJson.number(summary, "activeUsers")));
            values.put("Active Rooms", Long.toString(ClientJson.number(summary, "openRooms")));
            values.put("System Chips", "$" + String.format("%,d", ClientJson.number(summary, "totalChipsInAccounts")));
            List<AdminDashboardView.UserModel> people = new ArrayList<>();
            for (JsonElement element : users.join().getAsJsonArray("items")) {
                JsonObject user = element.getAsJsonObject();
                people.add(new AdminDashboardView.UserModel("#" + ClientJson.number(user, "userId"),
                        ClientJson.string(user, "username"), "$" + ClientJson.number(user, "accountChips"),
                        ClientJson.string(user, "accountStatus")));
            }
            List<String> roomNames = new ArrayList<>();
            for (JsonElement element : rooms.join().getAsJsonArray("items")) {
                JsonObject room = element.getAsJsonObject();
                roomNames.add("#" + ClientJson.number(room, "roomId") + " " + ClientJson.string(room, "name") +
                        " • " + ClientJson.string(room, "status") + " • " + ClientJson.number(room, "seatedPlayers") + " người");
            }
            view.showOnlineData(values, people, roomNames);
        })).exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }

    private void toggle(AdminDashboardView.UserModel user) {
        long userId = ClientJson.parseId(user.idProperty().get());
        boolean locked = "LOCKED".equals(user.statusProperty().get());
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn " + (locked ? "mở khóa" : "khóa") + " tài khoản " +
                        user.nameProperty().get() + "?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(stage);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        admin.setLocked(userId, !locked).thenRun(() -> Platform.runLater(this::show))
                .exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }
}
