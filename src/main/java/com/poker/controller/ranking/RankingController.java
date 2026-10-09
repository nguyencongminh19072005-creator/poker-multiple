package com.poker.controller.ranking;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển bảng xếp hạng. */
public final class RankingController {
    private final PokerSocketClient api;
    public RankingController(PokerSocketClient api) { this.api = api; }
    public CompletableFuture<JsonObject> leaderboard() {
        return api.request("GET", "/api/v1/rankings/leaderboard", null);
    }
}
