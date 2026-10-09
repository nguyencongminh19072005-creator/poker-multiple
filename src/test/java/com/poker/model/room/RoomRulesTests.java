package com.poker.model.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomRulesTests {
    @Test
    void spectatorCanEnterPlayingRoomButPlayerCannot() {
        assertThatCode(() -> RoomAdmissionRules.requireOpen("PLAYING", true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RoomAdmissionRules.requireOpen("PLAYING", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void playerMustUseFreeSeatAndConfiguredBuyIn() {
        assertThatCode(() -> RoomAdmissionRules.validateSeatAndBuyIn(false, 2, 6, 1_000, 1_000))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RoomAdmissionRules.validateSeatAndBuyIn(false, null, 6, 1_000, 1_000))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Ghế không hợp lệ");
        assertThatThrownBy(() -> RoomAdmissionRules.validateSeatAndBuyIn(false, 2, 6, 500, 1_000))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Buy-in phải đúng cấu hình phòng");
    }

    @Test
    void creationUsesExistingLimits() {
        assertThatCode(() -> RoomCreationRules.validate("Poker", 6, 50, 100, 1_000))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> RoomCreationRules.validate("Poker", 5, 50, 100, 1_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
