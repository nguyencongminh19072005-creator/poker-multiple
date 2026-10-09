package com.poker.controller.friend;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển danh sách bạn bè và lời mời kết bạn. */
public final class FriendController {
    private final PokerSocketClient api;

    public FriendController(PokerSocketClient api) { this.api = api; }

    public CompletableFuture<JsonArray> listFriends() { return api.list("/api/v1/friends"); }
    public CompletableFuture<JsonArray> incomingRequests() {
        return api.list("/api/v1/friend-requests?direction=INCOMING");
    }
    public CompletableFuture<JsonObject> sendRequest(long userId) {
        JsonObject body = new JsonObject();
        body.addProperty("recipientUserId", userId);
        return api.request("POST", "/api/v1/friend-requests", body);
    }
    public CompletableFuture<JsonObject> accept(long requestId) {
        return api.request("POST", "/api/v1/friend-requests/" + requestId + "/accept", new JsonObject());
    }
    public CompletableFuture<JsonObject> reject(long requestId) {
        return api.request("POST", "/api/v1/friend-requests/" + requestId + "/reject", new JsonObject());
    }
    public CompletableFuture<JsonObject> remove(long userId) {
        return api.request("DELETE", "/api/v1/friends/" + userId, null);
    }
}
