package com.poker.network.server.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.poker.util.JsonUtil;
import com.poker.util.JwtUtil;

import java.io.IOException;

final class CommandJson {
    private CommandJson() {
    }

    static JsonObject body(CommandExchange exchange) throws IOException {
        return exchange.body();
    }

    static Long userId(CommandExchange exchange) {
        String token = exchange.token();
        if (token == null || token.isBlank()) {
            return null;
        }
        return JwtUtil.validateTokenAndGetUserId(token);
    }

    static void send(CommandExchange exchange, int status, Object body) throws IOException {
        JsonElement data = status == 204
                ? new JsonObject()
                : JsonParser.parseString(body instanceof String text ? text : JsonUtil.toJson(body));
        exchange.respond(status, data);
    }

    static void error(CommandExchange exchange, int status, String message) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        send(exchange, status, body);
    }

    static String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : "";
    }
}
