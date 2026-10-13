package com.poker.model;

import com.google.gson.JsonParser;
import com.poker.model.auth.AccountStatus;
import com.poker.model.auth.Role;
import com.poker.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserContractTests {
    @Test
    void modelKeepsExistingProfileJsonFieldsAndEnumValues() {
        User user = new User(7, "player", "Player", "player@example.com",
                Role.PLAYER, AccountStatus.ACTIVE, 100_000, null);

        var json = JsonParser.parseString(JsonUtil.toJson(user)).getAsJsonObject();

        assertThat(json.get("id").getAsLong()).isEqualTo(7);
        assertThat(json.get("role").getAsString()).isEqualTo("PLAYER");
        assertThat(json.get("status").getAsString()).isEqualTo("ACTIVE");
        assertThat(json.get("accountChips").getAsLong()).isEqualTo(100_000);
        assertThat(json.get("avatarUrl").isJsonNull()).isTrue();
    }
}
