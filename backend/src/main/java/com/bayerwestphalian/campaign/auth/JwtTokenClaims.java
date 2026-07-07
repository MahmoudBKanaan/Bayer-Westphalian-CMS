package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JwtTokenClaims(
        UUID userId,
        String email,
        List<SystemRoleName> roles,
        JwtTokenType tokenType,
        String issuer,
        Instant issuedAt,
        Instant expiresAt,
        String tokenId) {}
