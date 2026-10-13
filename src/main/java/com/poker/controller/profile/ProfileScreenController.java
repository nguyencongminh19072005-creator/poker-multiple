package com.poker.controller.profile;

import com.poker.controller.auth.AuthController;
import com.poker.network.client.ClientJson;
import com.poker.controller.statistics.StatisticsController;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import javafx.application.Platform;
import javafx.scene.Parent;
import com.poker.view.ProfileHistoryView;

import java.util.HashMap;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        CompletableFuture<JsonObject> rank = profile.ranking();
        CompletableFuture<JsonObject> stats = statistics.playerStatistics();
        CompletableFuture<JsonObject> history = profile.history();
        CompletableFuture.allOf(me, rank, stats, history).thenRun(() -> Platform.runLater(() -> {
            JsonObject valuesFromServer = stats.join();
            Map<String, String> values = new HashMap<>();
            values.put("🎮 Tổng số trận", Long.toString(ClientJson.number(valuesFromServer, "totalGames")));
            values.put("🥇 Trận thắng", Long.toString(ClientJson.number(valuesFromServer, "totalWins")));
            values.put("❌ Trận thua", Long.toString(ClientJson.number(valuesFromServer, "totalLosses")));
            values.put("📈 Tỷ lệ thắng", ClientJson.string(valuesFromServer, "winRate") + "%");
            values.put("⏱️ Thời gian chơi", ClientJson.number(valuesFromServer, "averagePlayingSeconds") / 60 + " phút/trận");
            values.put("⭐ Ranking Points", ClientJson.number(rank.join(), "rating") + " pts");
            List<ProfileHistoryView.MatchModel> matches = new ArrayList<>();
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());
            for (JsonElement item : history.join().getAsJsonArray("items")) {
                JsonObject match = item.getAsJsonObject();
                long change = ClientJson.number(match, "chipChange");
                matches.add(new ProfileHistoryView.MatchModel(
                        "#M-" + ClientJson.number(match, "gameSessionId"),
                        dateFormat.format(Instant.parse(ClientJson.string(match, "finishedAt"))),
                        ClientJson.string(match, "roomName"),
                        "Hạng " + ClientJson.number(match, "placement"),
                        ClientJson.number(match, "handsPlayed") + " ván",
                        (change >= 0 ? "+$" : "-$") + String.format("%,d", Math.abs(change))));
            }
            view.showOnlineData(ClientJson.number(me.join(), "accountChips"),
                    "Hạng #" + ClientJson.number(rank.join(), "rank") + " (" +
                            ClientJson.number(rank.join(), "rating") + " pts)", values, matches);
        })).exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }

    private void updateName(String name) {
        profile.updateDisplayName(name).thenRun(() -> Platform.runLater(this::show))
                .exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }
}
