package com.poker.controller.server.auth;

import com.poker.dao.JdbcDatabase;
import com.poker.model.User;
import com.poker.model.auth.AccountOperations;

import java.sql.SQLException;
import java.util.Optional;

/** Server-side MVC controller; WebSocket frames are handled elsewhere. */
public final class ServerAuthController {
    private final AccountOperations accounts;

    public ServerAuthController(JdbcDatabase database) { accounts = new AccountOperations(database); }
    public long register(String username, String email, String password, String displayName) throws SQLException {
        return accounts.register(username, email, password, displayName);
    }
    public AccountOperations.LoginResult login(String username, String password)
            throws SQLException, AccountOperations.LoginDeniedException {
        return accounts.login(username, password);
    }
    public Optional<User> refreshUser(String token) throws SQLException { return accounts.refreshUser(token); }
    public boolean belongsTo(String token, long userId) throws SQLException {
        return accounts.belongsTo(token, userId);
    }
    public void revoke(String token) throws SQLException { accounts.revoke(token); }
}
