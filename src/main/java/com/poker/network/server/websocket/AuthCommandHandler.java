package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import com.poker.dao.JdbcDatabase;
import com.poker.controller.server.auth.ServerAuthController;
import com.poker.model.auth.AccountOperations;
import com.poker.util.JwtUtil;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;

final class AuthCommandHandler implements CommandHandler {
    private final ServerAuthController accounts;
    private final ClientPresence presence;

    AuthCommandHandler(JdbcDatabase database, ClientPresence presence) {
        accounts = new ServerAuthController(database);
        this.presence = presence;
    }

    @Override
    public void handle(CommandExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/register")) {
                register(exchange);
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/login")) {
                login(exchange);
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/refresh")) {
                refresh(exchange);
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/heartbeat")) {
                heartbeat(exchange);
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/logout")) {
                logout(exchange);
                return;
            }
            CommandJson.error(exchange, 404, "Không tìm thấy endpoint authentication");
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            CommandJson.error(exchange, 409, "Tên đăng nhập hoặc email đã tồn tại");
        } catch (AccountOperations.LoginDeniedException denied) {
            if (denied.reason() == AccountOperations.LoginDeniedException.Reason.LOCKED) {
                CommandJson.error(exchange, 403, "Tài khoản đã bị khóa");
            } else {
                CommandJson.error(exchange, 401, "Sai tên đăng nhập hoặc mật khẩu");
            }
        } catch (IllegalArgumentException bad) {
            CommandJson.error(exchange, 400, bad.getMessage());
        } catch (Exception error) {
            error.printStackTrace();
            CommandJson.error(exchange, 500, "Lỗi authentication server");
        }
    }

    private void register(CommandExchange e) throws Exception {
        JsonObject b = CommandJson.body(e);
        String username = CommandJson.text(b, "username").trim();
        String email = CommandJson.text(b, "email").trim();
        String password = CommandJson.text(b, "password");
        String display = CommandJson.text(b, "displayName").trim();
        long id = accounts.register(username, email, password, display);
        JsonObject out = new JsonObject();
        out.addProperty("id", id);
        out.addProperty("username", username);
        CommandJson.send(e, 201, out);
    }

    private void login(CommandExchange e) throws Exception {
        JsonObject b = CommandJson.body(e);
        var account = accounts.login(CommandJson.text(b, "username"), CommandJson.text(b, "password"));
        tokens(e, account.userId(), account.username(), account.refreshToken(),
                presence.login(account.userId()));
    }

    private void refresh(CommandExchange e) throws Exception {
        String token = CommandJson.text(CommandJson.body(e), "refreshToken");
        var user = accounts.refreshUser(token);
        if (user.isEmpty()) {
            CommandJson.error(e, 401, "Refresh token không hợp lệ hoặc đã hết hạn");
            return;
        }
        JsonObject out = new JsonObject();
        out.addProperty("accessToken", JwtUtil.generateToken(user.get().id(), user.get().username()));
        out.addProperty("sessionId", presence.login(user.get().id()));
        CommandJson.send(e, 200, out);
    }

    private void heartbeat(CommandExchange e) throws Exception {
        Long userId = CommandJson.userId(e);
        if (userId == null) {
            CommandJson.error(e, 401, "Bạn chưa đăng nhập");
            return;
        }
        if (!presence.heartbeat(userId, CommandJson.text(CommandJson.body(e), "sessionId"))) {
            CommandJson.error(e, 401, "Phiên đăng nhập đã kết thúc");
            return;
        }
        CommandJson.send(e, 204, "");
    }

    private void logout(CommandExchange e) throws Exception {
        Long userId = CommandJson.userId(e);
        JsonObject body = CommandJson.body(e);
        String refresh = CommandJson.text(body, "refreshToken");
        if (userId == null || !accounts.belongsTo(refresh, userId)) {
            CommandJson.error(e, 401, "Phiên đăng nhập không hợp lệ");
            return;
        }
        String sessionId = CommandJson.text(body, "sessionId");
        if (!presence.logout(userId, sessionId)) {
            CommandJson.error(e, 401, "Phiên đăng nhập đã kết thúc");
            return;
        }
        accounts.revoke(refresh);
        CommandJson.send(e, 204, "");
    }

    private void tokens(CommandExchange e, long id, String username, String refresh, String sessionId)
            throws IOException {
        JsonObject out = new JsonObject();
        out.addProperty("accessToken", JwtUtil.generateToken(id, username));
        out.addProperty("refreshToken", refresh);
        out.addProperty("sessionId", sessionId);
        out.addProperty("tokenType", "Bearer");
        out.addProperty("expiresIn", 86400);
        CommandJson.send(e, 200, out);
    }
}
