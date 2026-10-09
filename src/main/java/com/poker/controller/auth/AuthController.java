package com.poker.controller.auth;

import com.poker.network.client.ClientJson;
import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/** Điều khiển đăng ký, đăng nhập, cập nhật hồ sơ và đăng xuất. */
public final class AuthController {
    private final PokerSocketClient api;

    public AuthController(PokerSocketClient api) { this.api = api; }

    public CompletableFuture<String> login(String username, String password) {
        return api.login(username, password);
    }

    public CompletableFuture<Void> register(String username, String email, String password) {
        return api.register(username, email, password);
    }

    public CompletableFuture<JsonObject> currentUser() {
        return api.request("GET", "/api/v1/me", null);
    }

    public CompletableFuture<JsonObject> updateDisplayName(String name) {
        return api.updateDisplayName(name);
    }

    public CompletableFuture<Void> logout() { return api.logout(); }
    public CompletableFuture<Void> heartbeat() { return api.heartbeat(); }

    public String username() { return api.username(); }
    public long userId() { return api.userId(); }
    public String role() { return ClientJson.string(api.me(), "role"); }
    public String baseUrl() { return api.baseUrl(); }
    public String accessToken() { return api.accessToken(); }
    public CompletableFuture<String> refreshAccessToken() { return api.refreshAccessToken(); }
    public boolean hasSession() { return api.accessToken() != null; }
    public void clearSession() { api.clear(); }
}
