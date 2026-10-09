package com.poker.controller.server.system;

import com.poker.dao.JdbcDatabase;
import com.poker.model.system.SystemOperations;

public final class ServerSystemController {
    private final SystemOperations system;

    public ServerSystemController(JdbcDatabase database) { system = new SystemOperations(database); }
    public boolean databaseHealthy() { return system.databaseHealthy(); }
}
