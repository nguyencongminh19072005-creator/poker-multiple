package com.poker.controller.admin;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển các chức năng quản trị hệ thống. */
public final class AdminController {
    private final PokerSocketClient api;
    public AdminController(PokerSocketClient api) { this.api = api; }
    public CompletableFuture<JsonObject> overview() { return api.request("GET", "/api/v1/admin/overview", null); }
    public CompletableFuture<JsonObject> users() { return api.request("GET", "/api/v1/admin/users", null); }
    public CompletableFuture<JsonObject> rooms() { return api.request("GET", "/api/v1/admin/rooms", null); }
    public CompletableFuture<JsonObject> setLocked(long userId, boolean locked) {
        return api.request("POST", "/api/v1/admin/users/" + userId +
                (locked ? "/suspend" : "/reactivate"), new JsonObject());
    }
}
