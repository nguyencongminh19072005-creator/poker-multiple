package com.poker.view;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

class UserFacingErrorsTests {
    @Test
    void loginFailureDoesNotShowStatusOrEndpoint() {
        Throwable failure = new CompletionException(new IllegalStateException(
                "Server WebSocket 401 [/api/v1/auth/login]: Sai tên đăng nhập hoặc mật khẩu"));
        assertThat(UserFacingErrors.fromFailure(failure))
                .isEqualTo("Tài khoản hoặc mật khẩu không đúng.");
    }

    @Test
    void duplicateRegistrationIsExplainedWithoutProtocolDetails() {
        assertThat(UserFacingErrors.fromMessage(
                "Server WebSocket 409 [/api/v1/auth/register]: Duplicate key"))
                .isEqualTo("Tên đăng nhập hoặc email đã tồn tại.");
    }

    @Test
    void roomValidationRemainsUsefulButServerFailuresAreHidden() {
        assertThat(UserFacingErrors.fromMessage(
                "Server WebSocket 400 [/api/v1/rooms/7/join]: Mật khẩu phòng không đúng"))
                .isEqualTo("Mật khẩu phòng không đúng");
        assertThat(UserFacingErrors.fromMessage(
                "Server WebSocket 500 [/api/v1/me]: java.sql.SQLException: connection failed"))
                .isEqualTo("Máy chủ đang gặp sự cố. Vui lòng thử lại sau.");
        assertThat(UserFacingErrors.fromMessage(
                "Server WebSocket 400 [/api/v1/me]: SQL column user_id missing"))
                .isEqualTo("Thông tin chưa hợp lệ. Vui lòng kiểm tra và thử lại.");
    }

    @Test
    void unknownTechnicalFailureIsNotExposed() {
        assertThat(UserFacingErrors.fromMessage(
                "Mất kết nối WebSocket: java.net.ConnectException: Connection refused"))
                .isEqualTo("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.");
        assertThat(UserFacingErrors.fromMessage("java.lang.IllegalStateException: secret"))
                .isEqualTo("Không thể thực hiện thao tác. Vui lòng thử lại.");
        assertThat(UserFacingErrors.fromFailure(new CompletionException(new ConnectException())))
                .isEqualTo("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.");
    }
}
