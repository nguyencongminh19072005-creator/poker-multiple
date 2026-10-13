package com.poker.network.server.websocket;

import com.poker.dao.JdbcDatabase;
import com.poker.dao.profile.ProfileDao;
import com.poker.dao.room.RoomDao;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClientPresenceTests {
    @Test void heartbeatRequiresCurrentSessionAndExpiresWithoutHeartbeat() {
        MutableClock clock = new MutableClock();
        JdbcDatabase unused = new JdbcDatabase("jdbc:mysql://127.0.0.1:1/unused", "unused", "unused");
        ClientPresence presence = new ClientPresence(new RoomDao(unused), new ProfileDao(unused), clock);
        String first = presence.login(7);
        assertThat(presence.heartbeat(7, first)).isTrue();

        String second = presence.login(7);
        assertThat(presence.heartbeat(7, first)).isFalse();
        assertThat(presence.heartbeat(7, second)).isTrue();

        clock.advanceSeconds(21);
        assertThat(presence.heartbeat(7, second)).isFalse();
    }

    private static final class MutableClock extends Clock {
        private long millis;
        void advanceSeconds(long seconds) { millis += seconds * 1_000; }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }
}
