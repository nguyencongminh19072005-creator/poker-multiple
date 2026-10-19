package com.poker.view;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts diagnostic failures to short messages suitable for the UI. */
public final class UserFacingErrors {
    private static final String GENERIC = "Không thể thực hiện thao tác. Vui lòng thử lại.";
    private static final String CONNECTION = "Không thể kết nối đến máy chủ. Vui lòng thử lại sau.";
    private static final Pattern SERVER_RESPONSE = Pattern.compile(
            "^Server WebSocket (\\d{3}) \\[([^]]+)]: (.*)$", Pattern.DOTALL);

    private UserFacingErrors() { }

    public static String fromFailure(Throwable failure) {
        if (failure == null) return GENERIC;
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        if (cause instanceof IOException || cause instanceof TimeoutException) return CONNECTION;
        return fromMessage(cause.getMessage());
    }

    public static String fromMessage(String raw) {
        if (raw == null || raw.isBlank()) return GENERIC;
        String message = raw.trim();
        Matcher response = SERVER_RESPONSE.matcher(message);
        if (response.matches()) {
            int status = Integer.parseInt(response.group(1));
            String path = response.group(2);
            String detail = response.group(3).trim();
            if (path.equals("/api/v1/auth/login")) {
                if (status == 401) return "Tài khoản hoặc mật khẩu không đúng.";
                if (status == 403) return "Tài khoản đã bị khóa.";
            }
            if (path.equals("/api/v1/auth/register") && status == 409) {
                return "Tên đăng nhập hoặc email đã tồn tại.";
            }
            if (status == 401) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
            if (status == 403) return "Bạn không có quyền thực hiện thao tác này.";
            if (status == 404) {
                return path.startsWith("/api/v1/rooms/")
                        ? "Phòng không tồn tại." : "Không tìm thấy dữ liệu yêu cầu.";
            }
            if (status == 409) return "Thông tin đã tồn tại hoặc bị trùng.";
            if (status == 400) {
                return isSafeValidationMessage(detail)
                        ? detail : "Thông tin chưa hợp lệ. Vui lòng kiểm tra và thử lại.";
            }
            if (status >= 500) return "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
            return GENERIC;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("websocket") || lower.contains("connect")
                || lower.contains("timed out") || lower.contains("timeout")
                || lower.contains("mất kết nối")) {
            return CONNECTION;
        }
        return isSafeValidationMessage(message) ? message : GENERIC;
    }

    private static boolean isSafeValidationMessage(String message) {
        if (message.length() > 120 || message.contains("\n") || message.contains("\r")
                || message.contains(":") || message.contains("/") || message.contains("\\")) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("server") || lower.contains("api") || lower.contains("http")
                || lower.contains("exception") || lower.contains("sql")
                || lower.contains("endpoint") || lower.contains("java.")) {
            return false;
        }
        return message.matches("[\\p{L}\\p{M}\\p{N} \\t.,!?()_+-]+")
                && (lower.startsWith("bạn ") || lower.startsWith("không ")
                || lower.startsWith("chưa ") || lower.startsWith("phòng ")
                || lower.startsWith("mật khẩu ") || lower.startsWith("ghế ")
                || lower.startsWith("tên ") || lower.startsWith("người ")
                || lower.startsWith("số ") || lower.startsWith("lời mời ")
                || lower.startsWith("phiên ") || lower.startsWith("vui lòng "));
    }
}
