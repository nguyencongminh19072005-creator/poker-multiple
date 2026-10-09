package com.poker.network.server.websocket;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.profile.ProfileDao;
import com.poker.dao.room.RoomDao;
import com.poker.controller.server.room.ServerRoomController;
import com.poker.controller.server.profile.ServerProfileController;
import com.poker.controller.server.game.ServerGameController;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One active client lease per account on this single-server deployment. */
final class ClientPresence {
    static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final Map<Long, Lease> leases = new ConcurrentHashMap<>();
    private final ServerRoomController rooms;
    private final ServerProfileController profiles;
    private final ServerGameController games;
    private final Clock clock;

    ClientPresence(JdbcDatabase database) {
        this(new ServerRoomController(database), new ServerProfileController(database),
                new ServerGameController(database), Clock.systemUTC());
    }

    ClientPresence(JdbcDatabase database, ServerGameController games) {
        this(new ServerRoomController(database), new ServerProfileController(database),
                games, Clock.systemUTC());
    }

    ClientPresence(RoomDao rooms, ProfileDao profiles, Clock clock) {
        this(new ServerRoomController(rooms), new ServerProfileController(profiles), null, clock);
    }

    ClientPresence(ServerRoomController rooms, ServerProfileController profiles,
                   ServerGameController games, Clock clock) {
        this.rooms = rooms;
        this.profiles = profiles;
        this.games = games;
        this.clock = clock;
    }

    synchronized String login(long userId) {
        String id = UUID.randomUUID().toString();
        leases.put(userId, new Lease(id, clock.millis()));
        return id;
    }

    synchronized boolean heartbeat(long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        long now = clock.millis();
        boolean[] renewed = {false};
        leases.computeIfPresent(userId, (id, lease) -> {
            if (!lease.sessionId().equals(sessionId)
                    || now - lease.lastSeenMillis() > TIMEOUT.toMillis()) return lease;
            renewed[0] = true;
            return new Lease(sessionId, now);
        });
        return renewed[0];
    }

    synchronized boolean logout(long userId, String sessionId) throws SQLException {
        Lease lease = leases.get(userId);
        if (lease == null || !lease.sessionId().equals(sessionId)) return false;
        disconnect(userId);
        leases.remove(userId, lease);
        return true;
    }

    synchronized int expireStale() {
        long now = clock.millis();
        int expired = 0;
        for (var entry : leases.entrySet()) {
            Lease lease = entry.getValue();
            if (now - lease.lastSeenMillis() <= TIMEOUT.toMillis()) continue;
            try {
                disconnect(entry.getKey());
                leases.remove(entry.getKey(), lease);
                expired++;
            }
            catch (SQLException error) {
                // The lease remains stale so the next sweep retries cleanup.
                error.printStackTrace();
            }
        }
        return expired;
    }

    private void disconnect(long userId) throws SQLException {
        if (games != null) games.disconnectUser(userId);
        rooms.disconnectUser(userId);
        profiles.setPresence(userId, "OFFLINE");
    }

    private record Lease(String sessionId, long lastSeenMillis) { }
}
