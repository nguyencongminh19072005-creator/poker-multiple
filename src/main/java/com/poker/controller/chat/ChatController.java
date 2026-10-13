package com.poker.controller.chat;

import com.poker.network.client.PokerSocketClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Điều khiển chat phòng qua WebSocket dùng chung. */
public final class ChatController {
    private final PokerSocketClient socket;
    public ChatController(PokerSocketClient socket) {
        this.socket = socket;
    }
    public CompletableFuture<JsonArray> roomMessages(long roomId) {
        return socket.list("/api/v1/rooms/" + roomId + "/chat/messages?limit=50");
    }
    public void sendRoomMessage(long roomId, String content) {
        JsonObject command = new JsonObject();
        command.addProperty("clientMessageId", UUID.randomUUID().toString());
        command.addProperty("content", content);
        socket.send("/app/room/" + roomId + "/chat", command);
    }
}
