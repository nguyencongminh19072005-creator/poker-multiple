package com.poker.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {
    @Test
    void generatedAccessTokenReturnsTheSameUserId() {
        String token = JwtUtil.generateToken(42L, "anhminh1");
        assertThat(JwtUtil.validateTokenAndGetUserId(token)).isEqualTo(42L);
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(JwtUtil.validateTokenAndGetUserId("not-a-token")).isNull();
    }
}
