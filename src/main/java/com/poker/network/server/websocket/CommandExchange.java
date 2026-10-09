package com.poker.network.server.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;

/** Một yêu cầu và phản hồi trên cùng kết nối WebSocket, không phụ thuộc HTTP. */
final class CommandExchange {
    private final String method;
    private final URI requestUri;
    private final JsonObject body;
    private final String token;
    private int status = 500;
    private JsonElement response;

    CommandExchange(String method, String path, JsonObject body, String token) {
        this.method = method;
        this.requestUri = URI.create(path);
        this.body = body == null ? new JsonObject() : body;
        this.token = token;
    }

    String getRequestMethod() {
        return method;
    }

    URI getRequestURI() {
        return requestUri;
    }

    JsonObject body() {
        return body;
    }

    String token() {
        return token;
    }

    void respond(int status, JsonElement response) {
        this.status = status;
        this.response = response;
    }

    int status() {
        return status;
    }

    JsonElement response() {
        return response;
    }
}
