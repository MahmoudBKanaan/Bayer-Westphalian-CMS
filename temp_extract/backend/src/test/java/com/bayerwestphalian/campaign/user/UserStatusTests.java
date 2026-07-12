package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserStatusTests {

    @Test
    void matchesKbUserStatusValues() {
        assertThat(UserStatus.values())
                .containsExactly(UserStatus.ACTIVE, UserStatus.DISABLED, UserStatus.LOCKED);
    }
}
