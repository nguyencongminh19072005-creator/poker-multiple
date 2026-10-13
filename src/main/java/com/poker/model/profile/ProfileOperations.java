package com.poker.model.profile;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.auth.UserDao;
import com.poker.dao.profile.ProfileDao;
import com.poker.model.User;

import java.sql.SQLException;
import java.util.Optional;

/** Profile rules and queries, independent of network frames. */
public final class ProfileOperations {
    private final UserDao users;
    private final ProfileDao profiles;

    public ProfileOperations(JdbcDatabase database) {
        users = new UserDao(database);
        profiles = new ProfileDao(database);
    }

    public ProfileOperations(ProfileDao profiles) {
        users = null;
        this.profiles = profiles;
    }

    public Optional<User> find(long userId) throws SQLException { return users.findById(userId); }

    public void update(long userId, String displayName, String avatarUrl) throws SQLException {
        String name = displayName.trim();
        if (name.isBlank()) throw new IllegalArgumentException("Tên hiển thị không được để trống");
        profiles.update(userId, name, avatarUrl);
    }

    public long countOnline() throws SQLException { return profiles.countOnline(); }
    public void setPresence(long userId, String status) throws SQLException {
        profiles.setPresence(userId, status);
    }
    public void markAllOffline() throws SQLException { profiles.markAllOffline(); }
}
