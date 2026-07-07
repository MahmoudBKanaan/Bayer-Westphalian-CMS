package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id, String email, String fullName, UserStatus status, Instant lastLoginAt) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getLastLoginAt());
    }
}
