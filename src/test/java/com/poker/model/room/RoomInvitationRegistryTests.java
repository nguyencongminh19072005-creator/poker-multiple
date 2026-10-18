package com.poker.model.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomInvitationRegistryTests {

    @Test
    void registerAndValidateInvitationSuccessfully() {
        RoomInvitationRegistry registry = new RoomInvitationRegistry(60_000);
        long roomId = 9;
        long userId = 42;
        String token = "test-token-123";

        registry.register(roomId, userId, token);

        assertThat(registry.hasValidInvitation(roomId, userId)).isTrue();
        // Validating with correct token consumes it
        assertThat(registry.validateAndConsume(roomId, userId, token)).isTrue();
        // Once consumed, it is no longer valid
        assertThat(registry.validateAndConsume(roomId, userId, token)).isFalse();
        assertThat(registry.hasValidInvitation(roomId, userId)).isFalse();
    }

    @Test
    void invalidTokenFails() {
        RoomInvitationRegistry registry = new RoomInvitationRegistry(60_000);
        registry.register(1, 2, "secret-token");

        assertThat(registry.validateAndConsume(1, 2, "wrong-token")).isFalse();
        assertThat(registry.validateAndConsume(1, 2, null)).isFalse();
        assertThat(registry.validateAndConsume(1, 2, "")).isFalse();
        assertThat(registry.validateAndConsume(1, 999, "secret-token")).isFalse();
        assertThat(registry.validateAndConsume(999, 2, "secret-token")).isFalse();

        // Original is still preserved because invalid token did not consume it
        assertThat(registry.validateAndConsume(1, 2, "secret-token")).isTrue();
    }

    @Test
    void expiredInvitationFails() throws InterruptedException {
        // 50ms TTL
        RoomInvitationRegistry registry = new RoomInvitationRegistry(50);
        registry.register(5, 10, "quick-token");

        Thread.sleep(80);

        assertThat(registry.validateAndConsume(5, 10, "quick-token")).isFalse();
        assertThat(registry.hasValidInvitation(5, 10)).isFalse();
    }
}
