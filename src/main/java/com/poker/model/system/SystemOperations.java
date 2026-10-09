package com.poker.model.system;

import com.poker.dao.DatabaseHealthDao;
import com.poker.dao.JdbcDatabase;

public final class SystemOperations {
    private final DatabaseHealthDao health;

    public SystemOperations(JdbcDatabase database) { health = new DatabaseHealthDao(database); }
    public boolean databaseHealthy() { return health.reachable(); }
}
