package com.bayerwestphalian.campaign.auth;

import java.time.Instant;

public record JwtTokenPair(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {}
