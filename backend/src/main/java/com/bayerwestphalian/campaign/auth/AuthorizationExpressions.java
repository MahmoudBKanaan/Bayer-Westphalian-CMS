package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authz")
public class AuthorizationExpressions {

    public boolean isAuthenticated() {
        Authentication authentication = authentication();
        return authentication != null && authentication.isAuthenticated();
    }

    public boolean hasRole(String roleName) {
        return hasAnyRole(roleName);
    }

    public boolean hasAnyRole(String... roleNames) {
        if (roleNames == null || roleNames.length == 0) {
            return false;
        }
        Authentication authentication = authentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return Arrays.stream(roleNames)
                .map(this::authorityName)
                .anyMatch(expected -> hasAuthority(authentication, expected));
    }

    public boolean canManageUsers() {
        return hasRole(SystemRoleName.ADMIN.name());
    }

    public boolean canManageRoles() {
        return hasRole(SystemRoleName.ADMIN.name());
    }

    public boolean canManageProducts() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.PRODUCT_MANAGER.name());
    }

    public boolean canManageCampaigns() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    public boolean canApproveCampaigns() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    public boolean canViewAuditLogs() {
        return hasAnyRole(
                SystemRoleName.ADMIN.name(),
                SystemRoleName.COMPLIANCE_OFFICER.name(),
                SystemRoleName.SYSTEM_AUDITOR.name());
    }

    public boolean canViewReports() {
        return hasAnyRole(
                SystemRoleName.ADMIN.name(),
                SystemRoleName.BI_ANALYST.name(),
                SystemRoleName.CAMPAIGN_MANAGER.name(),
                SystemRoleName.EXECUTIVE_VIEWER.name());
    }

    public UUID currentUserId() {
        return currentPrincipal()
                .map(AuthenticatedPrincipal::userId)
                .orElseThrow(() -> new AccessDeniedException("Authentication is required"));
    }

    public void requireRole(String roleName) {
        if (!hasRole(roleName)) {
            throw new AccessDeniedException("Role is not allowed to perform this action");
        }
    }

    private Optional<AuthenticatedPrincipal> currentPrincipal() {
        Authentication authentication = authentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean hasAuthority(Authentication authentication, String authorityName) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authorityName::equals);
    }

    private String authorityName(String roleName) {
        String normalizedRole = roleName == null ? "" : roleName.trim();
        return normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
    }
}
