package com.poker.controller.server.ranking;

import com.poker.dao.JdbcDatabase;
import com.poker.dto.RankingEntryDto;
import com.poker.model.ranking.RankingOperations;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class ServerRankingController {
    private final RankingOperations rankings;

    public ServerRankingController(JdbcDatabase database) { rankings = new RankingOperations(database); }
    public List<RankingEntryDto> leaderboard(int limit) throws SQLException {
        return rankings.leaderboard(limit);
    }
    public Optional<RankingEntryDto> mine(long userId) throws SQLException { return rankings.mine(userId); }
}
