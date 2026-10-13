package com.poker.dao;

import java.sql.SQLException;

/** Read-only database connectivity probe. */
public final class DatabaseHealthDao {
    private final JdbcDatabase database;

    public DatabaseHealthDao(JdbcDatabase database) { this.database = database; }

    public boolean reachable() {
        try (var connection = database.open()) {
            return connection.isValid(2);
        } catch (SQLException error) {
            return false;
        }
    }
}
