package com.poker.controller.server.statistics;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.statistics.StatisticsDao;
import com.poker.model.analytics.PlayerStatistics;
import com.poker.model.analytics.StatisticsOperations;
import com.poker.model.profile.ProfileOperations;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class ServerStatisticsController {
    private final StatisticsOperations statistics;
    private final ProfileOperations profiles;

    public ServerStatisticsController(JdbcDatabase database) {
        statistics = new StatisticsOperations(database);
        profiles = new ProfileOperations(database);
    }
    public long onlineCount() throws SQLException { return profiles.countOnline(); }
    public Optional<PlayerStatistics> player(long userId) throws SQLException {
        return statistics.player(userId);
    }
    public List<StatisticsDao.Bucket> buckets(long userId, LocalDate from, LocalDate to,
                                               boolean weekly) throws SQLException {
        return statistics.buckets(userId, from, to, weekly);
    }
}
