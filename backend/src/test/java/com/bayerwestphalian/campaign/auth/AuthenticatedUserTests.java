package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTests {

    @Test
    void mapsUserWithoutExposingPasswordHash() {
        User user =
                User.create("advisor@bayer-westphalian.test", "$2a$10$examplehash", "Advisor User");
        Instant loginTime = Instant.parse("2026-07-03T12:00:00Z");
        user.recordLogin(loginTime);

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        assertThat(authenticatedUser.email()).isEqualTo("advisor@bayer-westphalian.test");
        assertThat(authenticatedUser.fullName()).isEqualTo("Advisor User");
        assertThat(authenticatedUser.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(authenticatedUser.lastLoginAt()).isEqualTo(loginTime);
    }
}
