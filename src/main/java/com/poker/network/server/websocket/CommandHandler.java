package com.poker.network.server.websocket;

import java.io.IOException;

/** Xử lý một lệnh WebSocket sau khi tầng mạng đã parse frame. */
@FunctionalInterface
interface CommandHandler {
    void handle(CommandExchange exchange) throws IOException;
}
