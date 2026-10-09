package com.poker.model.auth;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.auth.RefreshTokenDao;
import com.poker.dao.auth.UserDao;
import com.poker.dao.profile.ProfileDao;
import com.poker.model.room.RoomOperations;
import com.poker.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/** Account rules and persistence coordination, independent of WebSocket frames. */
public final class AccountOperations {
    private final UserDao users;
    private final RefreshTokenDao refreshTokens;
    private final ProfileDao profiles;
    private final RoomOperations rooms;

    public AccountOperations(JdbcDatabase database) {
        users = new UserDao(database);
        refreshTokens = new RefreshTokenDao(database);
        profiles = new ProfileDao(database);
        rooms = new RoomOperations(database);
    }

    public long register(String username, String email, String password, String displayName)
            throws SQLException {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim();
        String normalizedDisplayName = displayName.trim();
        RegistrationRules.validate(normalizedUsername, password);
        return users.create(normalizedUsername, normalizedEmail,
                BCrypt.hashpw(password, BCrypt.gensalt(12)),
                normalizedDisplayName.isBlank() ? normalizedUsername : normalizedDisplayName);
    }

    public LoginResult login(String username, String password) throws SQLException, LoginDeniedException {
        UserDao.AccountRow account = users.findByUsername(username.trim()).orElse(null);
        if (account == null || !BCrypt.checkpw(password, account.passwordHash())) {
            throw new LoginDeniedException(LoginDeniedException.Reason.INVALID_CREDENTIALS);
        }
        if (!"ACTIVE".equals(account.status())) {
            throw new LoginDeniedException(LoginDeniedException.Reason.LOCKED);
        }
        rooms.disconnectUser(account.id());
        users.markLogin(account.id());
        profiles.setPresence(account.id(), "ONLINE");
        return new LoginResult(account.id(), account.username(), refreshTokens.create(account.id()));
    }

    public Optional<User> refreshUser(String refreshToken) throws SQLException {
        var userId = refreshTokens.activeUser(refreshToken);
        if (userId.isEmpty()) return Optional.empty();
        Optional<User> user = users.findById(userId.getAsLong());
        if (user.isPresent()) profiles.setPresence(user.get().id(), "ONLINE");
        return user;
    }

    public boolean belongsTo(String refreshToken, long userId) throws SQLException {
        var owner = refreshTokens.activeUser(refreshToken);
        return owner.isPresent() && owner.getAsLong() == userId;
    }

    public void revoke(String refreshToken) throws SQLException {
        refreshTokens.revoke(refreshToken);
    }

    public record LoginResult(long userId, String username, String refreshToken) { }

    public static final class LoginDeniedException extends Exception {
        public enum Reason { INVALID_CREDENTIALS, LOCKED }

        private final Reason reason;

        public LoginDeniedException(Reason reason) {
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }
}
