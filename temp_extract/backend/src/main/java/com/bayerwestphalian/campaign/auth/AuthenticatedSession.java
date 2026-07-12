package com.bayerwestphalian.campaign.auth;

public record AuthenticatedSession(AuthenticatedUser user, JwtTokenPair tokens) {}
