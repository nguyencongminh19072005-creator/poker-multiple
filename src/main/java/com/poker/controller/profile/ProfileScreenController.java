package com.poker.controller.profile;

import com.poker.controller.auth.AuthController;
import com.poker.network.client.ClientJson;
import com.poker.controller.statistics.StatisticsController;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.Parent;
import com.poker.view.ProfileHistoryView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Luồng hồ sơ và lịch sử của Trịnh Quốc An; dữ liệu đi qua WebSocket. */
public final class ProfileScreenController {
    private final ProfileController profile;
    private final StatisticsController statistics;
    private final AuthController auth;
    private final Consumer<Parent> show;
    private final Runnable back;
    private final Runnable logout;
    private final Consumer<String> error;

    public ProfileScreenController(ProfileController profile, StatisticsController statistics,
                            AuthController auth, Consumer<Parent> show, Runnable back,
                            Runnable logout, Consumer<String> error) {
        this.profile = profile;
        this.statistics = statistics;
        this.auth = auth;
        this.show = show;
        this.back = back;
        this.logout = logout;
        this.error = error;
    }

    public void show() {
        String displayName = ClientJson.string(profile.cachedProfile(), "displayName");
        ProfileHistoryView view = new ProfileHistoryView(displayName.isBlank() ? auth.username() : displayName,
                auth.userId(), new ProfileHistoryView.ProfileActions() {
                    @Override public void onBackToLobby() { back.run(); }
                    @Override public void onLogout() { logout.run(); }
                }, true, name -> updateName(name));
        show.accept(view);

        CompletableFuture<JsonObject> me = profile.profile();
        CompletableFuture<JsonObject> rank = profile.ranking().exceptionally(failure -> emptyRanking());
        CompletableFuture<JsonObject> stats = statistics.playerStatistics().exceptionally(failure -> emptyStatistics());
        CompletableFuture.allOf(me, rank, stats).thenRun(() -> Platform.runLater(() -> {
            JsonObject valuesFromServer = stats.join();
            Map<String, String> values = new HashMap<>();
            values.put("🎮 Tổng số trận", Long.toString(ClientJson.number(valuesFromServer, "totalGames")));
            values.put("🥇 Trận thắng", Long.toString(ClientJson.number(valuesFromServer, "totalWins")));
            values.put("❌ Trận thua", Long.toString(ClientJson.number(valuesFromServer, "totalLosses")));
            values.put("📈 Tỷ lệ thắng", ClientJson.string(valuesFromServer, "winRate") + "%");
            values.put("⏱️ Thời gian chơi", ClientJson.number(valuesFromServer, "averagePlayingSeconds") / 60 + " phút/trận");
            values.put("⭐ Ranking Points", ClientJson.number(rank.join(), "rating") + " pts");
            view.showOnlineData(ClientJson.number(me.join(), "accountChips"),
                    "Hạng #" + ClientJson.number(rank.join(), "rank") + " (" +
                            ClientJson.number(rank.join(), "rating") + " pts)", values, List.of());
        })).exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }

    private static JsonObject emptyRanking() {
        JsonObject value = new JsonObject(); value.addProperty("rank", 0); value.addProperty("rating", 0); return value;
    }

    private static JsonObject emptyStatistics() {
        JsonObject value = new JsonObject(); value.addProperty("totalGames", 0); value.addProperty("totalWins", 0);
        value.addProperty("totalLosses", 0); value.addProperty("winRate", "0");
        value.addProperty("averagePlayingSeconds", 0); return value;
    }

    private void updateName(String name) {
        profile.updateDisplayName(name).thenRun(() -> Platform.runLater(this::show))
                .exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }
}
