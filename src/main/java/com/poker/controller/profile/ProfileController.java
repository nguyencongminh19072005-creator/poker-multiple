package com.poker.controller.profile;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển hồ sơ, thống kê và lịch sử của người chơi hiện tại. */
public final class ProfileController {
    private final PokerSocketClient api;

    public ProfileController(PokerSocketClient api) { this.api = api; }

    public CompletableFuture<JsonObject> profile() { return api.request("GET", "/api/v1/me", null); }
    public JsonObject cachedProfile() { return api.me(); }
    public CompletableFuture<JsonObject> ranking() { return api.request("GET", "/api/v1/rankings/me", null); }
    public CompletableFuture<JsonObject> updateDisplayName(String name) { return api.updateDisplayName(name); }
}
