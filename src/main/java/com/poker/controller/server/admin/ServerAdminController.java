package com.poker.controller.server.admin;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.admin.AdminDao;
import com.poker.dto.RoomDto;
import com.poker.model.User;
import com.poker.model.admin.AdminOperations;

import java.sql.SQLException;
import java.util.List;

public final class ServerAdminController {
    private final AdminOperations admin;

    public ServerAdminController(JdbcDatabase database) { admin = new AdminOperations(database); }
    public boolean isAdministrator(long userId) throws SQLException { return admin.isAdministrator(userId); }
    public AdminDao.Overview overview() throws SQLException { return admin.overview(); }
    public List<User> users(int limit) throws SQLException { return admin.users(limit); }
    public List<RoomDto> rooms() throws SQLException { return admin.rooms(); }
    public boolean setUserLocked(long adminId, long targetId, boolean locked) throws SQLException {
        return admin.setUserLocked(adminId, targetId, locked);
    }
}
