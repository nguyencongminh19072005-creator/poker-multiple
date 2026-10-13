package com.poker.controller.server.profile;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.profile.ProfileDao;
import com.poker.model.User;
import com.poker.model.profile.ProfileOperations;

import java.sql.SQLException;
import java.util.Optional;

public final class ServerProfileController {
    private final ProfileOperations profiles;

    public ServerProfileController(JdbcDatabase database) { profiles = new ProfileOperations(database); }
    public ServerProfileController(ProfileDao dao) { profiles = new ProfileOperations(dao); }
    public Optional<User> find(long userId) throws SQLException { return profiles.find(userId); }
    public void update(long userId, String name, String avatarUrl) throws SQLException {
        profiles.update(userId, name, avatarUrl);
    }
    public long countOnline() throws SQLException { return profiles.countOnline(); }
    public void setPresence(long userId, String status) throws SQLException {
        profiles.setPresence(userId, status);
    }
    public void markAllOffline() throws SQLException { profiles.markAllOffline(); }
}
