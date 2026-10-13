package com.poker.controller.game;

import com.poker.network.client.ClientJson;
import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển vòng đời và snapshot ván Poker. */
public final class GameController {
    private final PokerSocketClient api;
    public GameController(PokerSocketClient api) { this.api = api; }
    public CompletableFuture<JsonObject> activeForMe() {
        return api.request("GET", "/api/v1/games/active/me", null);
    }
    public CompletableFuture<Long> activeRoomId() {
        return activeForMe().thenApply(active -> ClientJson.number(active, "roomId"));
    }
    public CompletableFuture<JsonObject> activeForRoom(long roomId) {
        return api.request("GET", "/api/v1/games/active/room/" + roomId, null);
    }
    public CompletableFuture<JsonObject> start(long roomId) {
        return api.request("POST", "/api/v1/games/rooms/" + roomId + "/start", new JsonObject());
    }
    public CompletableFuture<JsonObject> snapshot(String gameId) {
        return api.request("GET", "/api/v1/games/" + gameId + "/snapshot", null);
    }
    public CompletableFuture<JsonObject> leave(String gameId) {
        return api.request("POST", "/api/v1/games/" + gameId + "/leave", new JsonObject());
    }
}
