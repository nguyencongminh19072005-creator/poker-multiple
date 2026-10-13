package com.poker.model.analytics;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.statistics.StatisticsDao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class StatisticsOperations {
    private final StatisticsDao statistics;

    public StatisticsOperations(JdbcDatabase database) { statistics = new StatisticsDao(database); }
    public Optional<PlayerStatistics> player(long userId) throws SQLException {
        return statistics.player(userId);
    }
    public List<StatisticsDao.Bucket> buckets(long userId, LocalDate from, LocalDate to,
                                               boolean weekly) throws SQLException {
        if (from.isAfter(to)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        return weekly ? statistics.weekly(userId, from, to) : statistics.daily(userId, from, to);
    }
}
