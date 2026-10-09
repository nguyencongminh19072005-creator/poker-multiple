package com.poker.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Nhà máy kết nối MySQL dùng chung cho các DAO của server Java thuần. */
public final class JdbcDatabase {
    private final String url;
    private final String username;
    private final String password;

    public JdbcDatabase(String url, String username, String password) {
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("DB_URL, DB_USERNAME và DB_PASSWORD là bắt buộc");
        }
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static JdbcDatabase fromEnvironment() {
        return new JdbcDatabase(System.getenv("DB_URL"), System.getenv("DB_USERNAME"),
                System.getenv("DB_PASSWORD"));
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
