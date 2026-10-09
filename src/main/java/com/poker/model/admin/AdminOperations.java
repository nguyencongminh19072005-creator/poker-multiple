package com.poker.model.admin;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.admin.AdminDao;
import com.poker.dao.auth.UserDao;
import com.poker.dao.room.RoomDao;
import com.poker.dto.RoomDto;
import com.poker.model.User;
import com.poker.model.auth.Role;

import java.sql.SQLException;
import java.util.List;

/** Moderation action and its audit entry belong to one domain operation. */
public final class AdminOperations {
    private final AdminDao admin;
    private final UserDao users;
    private final RoomDao rooms;

    public AdminOperations(JdbcDatabase database) {
        admin = new AdminDao(database);
        users = new UserDao(database);
        rooms = new RoomDao(database);
    }

    public boolean isAdministrator(long userId) throws SQLException {
        return users.findById(userId).map(user -> user.role() == Role.ADMIN).orElse(false);
    }
    public AdminDao.Overview overview() throws SQLException { return admin.overview(); }
    public List<User> users(int limit) throws SQLException { return admin.users(limit); }
    public List<RoomDto> rooms() throws SQLException { return rooms.listOpen(); }

    public boolean setUserLocked(long adminId, long targetId, boolean locked) throws SQLException {
        if (!admin.setLocked(targetId, locked)) return false;
        admin.audit(adminId, locked ? "SUSPEND_USER" : "REACTIVATE_USER", "USER", targetId, null);
        return true;
    }
}
