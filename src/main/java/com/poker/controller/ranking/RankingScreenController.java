package com.poker.controller.ranking;

import com.poker.network.client.ClientJson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.Parent;
import com.poker.view.LeaderboardView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Luồng bảng xếp hạng của Vũ Duy Thái; dữ liệu đi qua WebSocket. */
public final class RankingScreenController {
    private final RankingController ranking;
    private final Consumer<Parent> show;
    private final Runnable back;
    private final Consumer<String> error;

    public RankingScreenController(RankingController ranking, Consumer<Parent> show,
                            Runnable back, Consumer<String> error) {
        this.ranking = ranking;
        this.show = show;
        this.back = back;
        this.error = error;
    }

    public void show() {
        LeaderboardView view = new LeaderboardView(back::run, true);
        show.accept(view);
        ranking.leaderboard().thenAccept(data -> Platform.runLater(() -> {
            List<LeaderboardView.RankingEntry> entries = new ArrayList<>();
            for (JsonElement item : data.getAsJsonArray("items")) {
                JsonObject row = item.getAsJsonObject();
                entries.add(new LeaderboardView.RankingEntry(ClientJson.number(row, "rank"),
                        ClientJson.string(row, "username"), ClientJson.number(row, "rating"),
                        ClientJson.number(row, "gamesRated")));
            }
            view.showOnlineEntries(entries);
        })).exceptionally(failure -> { Platform.runLater(() -> error.accept(ClientJson.cause(failure))); return null; });
    }
}
