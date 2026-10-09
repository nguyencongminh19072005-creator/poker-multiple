package com.poker.network.server.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.statistics.ServerStatisticsController;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** Player statistics and time-bucket analytics using the existing JDBC DAO. */
final class StatisticsCommandHandler implements CommandHandler {
    private final ServerStatisticsController statistics;

    StatisticsCommandHandler(JdbcDatabase database) {
        statistics = new ServerStatisticsController(database);
    }

    @Override public void handle(CommandExchange exchange) throws IOException {
        try {
            Long userId = CommandJson.userId(exchange);
            if (userId == null) { CommandJson.error(exchange, 401, "Bạn chưa đăng nhập"); return; }
            if (!"GET".equals(exchange.getRequestMethod())) {
                CommandJson.error(exchange, 405, "Phương thức không được hỗ trợ"); return;
            }
            String path = exchange.getRequestURI().getPath();
            if ("/api/v1/players/online/count".equals(path)) {
                JsonObject response = new JsonObject();
                response.addProperty("onlinePlayers", statistics.onlineCount());
                CommandJson.send(exchange, 200, response); return;
            }
            if ("/api/v1/players/me/statistics".equals(path)
                    || "/api/v1/analytics/me/summary".equals(path)) {
                var result = statistics.player(userId);
                if (result.isEmpty()) { CommandJson.error(exchange, 404, "Chưa có thống kê"); return; }
                var stats = result.get();
                JsonObject response = new JsonObject();
                response.addProperty("userId", stats.userId());
                response.addProperty("totalGames", stats.totalGames());
                response.addProperty("totalHands", stats.totalHands());
                response.addProperty("totalWins", stats.totalWins());
                response.addProperty("totalLosses", stats.totalLosses());
                response.addProperty("winRate", stats.winRate());
                response.addProperty("totalChipsWon", stats.totalChipsWon());
                response.addProperty("totalChipsLost", stats.totalChipsLost());
                response.addProperty("netChip", stats.netChip());
                response.addProperty("largestPotWon", stats.largestPotWon());
                response.addProperty("averagePlayingSeconds", stats.averagePlayingSeconds());
                CommandJson.send(exchange, 200, response); return;
            }
            if ("/api/v1/analytics/me/daily".equals(path)
                    || "/api/v1/analytics/me/weekly".equals(path)) {
                Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
                LocalDate from = LocalDate.parse(query.get("from"));
                LocalDate to = LocalDate.parse(query.get("to"));
                var buckets = statistics.buckets(userId, from, to, path.endsWith("/weekly"));
                JsonArray items = new JsonArray();
                for (var bucket : buckets) {
                    JsonObject row = new JsonObject();
                    row.addProperty("date", bucket.date().toString());
                    row.addProperty("handsPlayed", bucket.handsPlayed());
                    row.addProperty("handsWon", bucket.handsWon());
                    row.addProperty("handsLost", bucket.handsLost());
                    row.addProperty("handsTied", bucket.handsTied());
                    row.addProperty("chipsWon", bucket.chipsWon());
                    row.addProperty("chipsLost", bucket.chipsLost());
                    row.addProperty("netChips", bucket.netChips());
                    row.addProperty("largestPotWon", bucket.largestPot());
                    row.addProperty("playingSeconds", bucket.playingSeconds());
                    row.addProperty("sessions", bucket.sessions());
                    items.add(row);
                }
                JsonObject response = new JsonObject(); response.add("items", items);
                CommandJson.send(exchange, 200, response); return;
            }
            CommandJson.error(exchange, 404, "Không tìm thấy endpoint thống kê");
        } catch (IllegalArgumentException | NullPointerException invalid) {
            CommandJson.error(exchange, 400, "Tham số thống kê không hợp lệ");
        } catch (Exception error) {
            error.printStackTrace(); CommandJson.error(exchange, 500, "Lỗi truy vấn thống kê");
        }
    }

    private static Map<String, String> query(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        return Arrays.stream(raw.split("&")).map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8), (first, last) -> last));
    }
}
