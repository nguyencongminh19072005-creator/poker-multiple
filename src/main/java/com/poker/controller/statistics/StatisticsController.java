package com.poker.controller.statistics;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Điều khiển thống kê tổng, theo ngày và theo tuần của người chơi hiện tại. */
public final class StatisticsController {
    private final PokerSocketClient api;

    public StatisticsController(PokerSocketClient api) {
        this.api = Objects.requireNonNull(api);
    }

    public CompletableFuture<JsonObject> playerStatistics() {
        return api.request("GET", "/api/v1/players/me/statistics", null);
    }

    public CompletableFuture<JsonObject> summary() {
        return api.request("GET", "/api/v1/analytics/me/summary", null);
    }

    public CompletableFuture<JsonArray> daily(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return api.list("/api/v1/analytics/me/daily?from=" + from + "&to=" + to);
    }

    public CompletableFuture<JsonArray> weekly(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return api.list("/api/v1/analytics/me/weekly?from=" + from + "&to=" + to);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "Ngày bắt đầu không được để trống");
        Objects.requireNonNull(to, "Ngày kết thúc không được để trống");
        if (from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
    }
}
