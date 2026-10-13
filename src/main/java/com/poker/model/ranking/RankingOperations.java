package com.poker.model.ranking;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.ranking.RankingDao;
import com.poker.dto.RankingEntryDto;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class RankingOperations {
    private final RankingDao rankings;

    public RankingOperations(JdbcDatabase database) { rankings = new RankingDao(database); }
    public List<RankingEntryDto> leaderboard(int limit) throws SQLException {
        return rankings.leaderboard(limit);
    }
    public Optional<RankingEntryDto> mine(long userId) throws SQLException {
        return rankings.mine(userId);
    }
}
