package com.poker.model.auth;

/** Quy tắc tối thiểu của tài khoản mới, độc lập với HTTP và JDBC. */
public final class RegistrationRules {
    private RegistrationRules() {
    }

    public static void validate(String username, String password) {
        if (username.length() < 3 || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Username tối thiểu 3 ký tự, mật khẩu tối thiểu 6 ký tự");
        }
    }
}
