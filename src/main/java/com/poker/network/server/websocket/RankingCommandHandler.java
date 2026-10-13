package com.poker.network.server.websocket;

import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.ranking.ServerRankingController;

import java.io.IOException;
import java.util.Map;

/** JDBC-backed ranking routes expected by the JavaFX client. */
final class RankingCommandHandler implements CommandHandler {
    private final ServerRankingController rankings;

    RankingCommandHandler(JdbcDatabase database) { rankings = new ServerRankingController(database); }

    @Override public void handle(CommandExchange exchange) throws IOException {
        try {
            Long userId = CommandJson.userId(exchange);
            if (userId == null) { CommandJson.error(exchange, 401, "Bạn chưa đăng nhập"); return; }
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equals(exchange.getRequestMethod())) {
                CommandJson.error(exchange, 405, "Phương thức không được hỗ trợ"); return;
            }
            if ("/api/v1/rankings/leaderboard".equals(path)) {
                CommandJson.send(exchange, 200, Map.of("items", rankings.leaderboard(100))); return;
            }
            if ("/api/v1/rankings/me".equals(path)) {
                var entry = rankings.mine(userId);
                if (entry.isEmpty()) { CommandJson.error(exchange, 404, "Chưa có xếp hạng"); return; }
                CommandJson.send(exchange, 200, entry.get()); return;
            }
            CommandJson.error(exchange, 404, "Không tìm thấy endpoint xếp hạng");
        } catch (Exception error) {
            error.printStackTrace();
            CommandJson.error(exchange, 500, "Lỗi truy vấn xếp hạng");
        }
    }
}
