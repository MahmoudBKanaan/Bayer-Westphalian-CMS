package com.bayerwestphalian.campaign.user;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record UserView(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        Instant lastLoginAt,
        List<SystemRoleName> roles) {

    public UserView {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public static UserView from(User user, List<UserRole> userRoles) {
        List<SystemRoleName> roleNames =
                userRoles.stream()
                        .map(UserRole::getRole)
                        .map(Role::getName)
                        .sorted(Comparator.comparing(SystemRoleName::name))
                        .toList();

        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getLastLoginAt(),
                roleNames);
    }
}
