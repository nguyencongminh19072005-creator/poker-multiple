package com.poker.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Chuyển đổi object Java và JSON cho giao thức WebSocket. */
public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder()
            // Keep nullable DTO properties in the wire contract (for example a
            // spectator has seatNumber: null), matching the original backend.
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private JsonUtil() { }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }
}
