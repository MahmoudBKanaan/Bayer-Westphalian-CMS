package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.List;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, String email, List<SystemRoleName> roles) {

    public AuthenticatedPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
