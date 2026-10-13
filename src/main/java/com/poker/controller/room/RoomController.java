package com.poker.controller.room;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển tham gia, theo dõi và rời phòng Poker. */
public final class RoomController {
    private final PokerSocketClient api;
    public RoomController(PokerSocketClient api) { this.api = api; }
    public CompletableFuture<JsonObject> detail(long roomId) {
        return api.request("GET", "/api/v1/rooms/" + roomId, null);
    }
    public CompletableFuture<RoomDetails> details(long roomId) {
        return detail(roomId).thenApply(data -> new RoomDetails(roomId, data));
    }
    public CompletableFuture<JsonObject> join(long roomId, JsonObject request) {
        return api.request("POST", "/api/v1/rooms/" + roomId + "/join", request);
    }
    public CompletableFuture<JsonObject> leave(long roomId) {
        return api.request("POST", "/api/v1/rooms/" + roomId + "/leave", new JsonObject());
    }

    public record RoomDetails(long roomId, JsonObject data) { }
}
