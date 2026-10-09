package com.poker.network.server.websocket;

import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Keeps each client's subscriptions and authenticated session on its own socket. */
final class ConnectionManager {
    private final Map<WebSocket, Session> sessions = new ConcurrentHashMap<>();

    Session open(WebSocket socket) {
        Session session = new Session();
        sessions.put(socket, session);
        return session;
    }

    Session get(WebSocket socket) {
        return sessions.get(socket);
    }

    Session close(WebSocket socket) {
        return sessions.remove(socket);
    }

    void broadcast(String destination) {
        broadcast(destination, new JsonObject());
    }

    void broadcast(String destination, JsonObject body) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "event");
        event.addProperty("destination", destination);
        event.add("body", body);
        String text = event.toString();
        sessions.forEach((socket, session) -> {
            if (socket.isOpen() && session.subscriptions.contains(destination)) {
                socket.send(text);
            }
        });
    }

    void broadcastSubscribedStateTopics() {
        Set<String> destinations = new HashSet<>();
        sessions.values().forEach(session -> destinations.addAll(session.subscriptions));
        destinations.stream().filter(destination -> destination.startsWith("/topic/room/")
                || destination.startsWith("/topic/game/"))
                .forEach(this::broadcast);
    }

    static final class Session {
        final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
        volatile Long userId;
        volatile String sessionId;
        private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
        private int queued;

        synchronized boolean enqueue(Executor workers, Runnable task) {
            if (queued >= 128) return false;
            queued++;
            tail = tail.handle((ignored, failure) -> null).thenRunAsync(task, workers);
            tail.whenComplete((ignored, failure) -> {
                synchronized (Session.this) { queued--; }
            });
            return true;
        }
    }
}
