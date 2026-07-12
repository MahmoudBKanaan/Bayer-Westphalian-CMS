package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthenticatedPrincipalTests {

    @Test
    void defensivelyCopiesRolesForSecurityContextPrincipal() {
        List<SystemRoleName> roles = new ArrayList<>(List.of(SystemRoleName.ADMIN));

        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(
                        UUID.fromString("10000000-0000-0000-0000-000000009901"),
                        "admin@bayer-westphalian.test",
                        roles);
        roles.clear();

        assertThat(principal.roles()).containsExactly(SystemRoleName.ADMIN);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> principal.roles().add(SystemRoleName.BI_ANALYST));
    }

    @Test
    void handlesMissingRolesAsEmptyList() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(
                        UUID.fromString("10000000-0000-0000-0000-000000009901"),
                        "admin@bayer-westphalian.test",
                        null);

        assertThat(principal.roles()).isEmpty();
    }
}
