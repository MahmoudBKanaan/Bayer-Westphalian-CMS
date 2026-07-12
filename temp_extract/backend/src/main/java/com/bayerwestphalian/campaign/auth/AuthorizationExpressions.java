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

    public boolean canManageProductOwnership() {
        return canManageProducts();
    }

    public boolean canReadCustomers() {
        return hasAnyRole(
                SystemRoleName.ADMIN.name(),
                SystemRoleName.CAMPAIGN_MANAGER.name(),
                SystemRoleName.BI_ANALYST.name(),
                SystemRoleName.COMPLIANCE_OFFICER.name(),
                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                SystemRoleName.SALES_AGENT.name(),
                SystemRoleName.PRODUCT_MANAGER.name());
    }

    public boolean canManageCampaigns() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    /**
     * Read access for campaign list/details (KB campaign read matrix): Admin, Campaign Manager, BI,
     * Product Manager, Compliance, agents, executive, auditor.
     */
    public boolean canReadCampaigns() {
        return hasAnyRole(
                SystemRoleName.ADMIN.name(),
                SystemRoleName.CAMPAIGN_MANAGER.name(),
                SystemRoleName.BI_ANALYST.name(),
                SystemRoleName.PRODUCT_MANAGER.name(),
                SystemRoleName.COMPLIANCE_OFFICER.name(),
                SystemRoleName.CUSTOMER_SERVICE_AGENT.name(),
                SystemRoleName.SALES_AGENT.name(),
                SystemRoleName.EXECUTIVE_VIEWER.name(),
                SystemRoleName.SYSTEM_AUDITOR.name());
    }

    /**
     * Segment edit/delete/manage criteria (KB item 200): only {@link SystemRoleName#ADMIN} and
     * {@link SystemRoleName#CAMPAIGN_MANAGER}. {@link SystemRoleName#BI_ANALYST} cannot edit unless
     * also granted one of those manage roles (dual-role accounts).
     */
    public boolean canManageSegments() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    /**
     * KB segment creation permissions (FR-077, role matrix, Campaign Manager “define segments”,
     * item 201 — Campaign Manager can create reusable segment): only {@link SystemRoleName#ADMIN}
     * and {@link SystemRoleName#CAMPAIGN_MANAGER} may create and save reusable audience segments.
     * Kept separate from {@link #canManageSegments()} so create can diverge later (e.g. optional BI
     * analytical drafts without full manage — item 200). {@link SystemRoleName#BI_ANALYST} alone
     * cannot create/edit.
     */
    public boolean canCreateSegments() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    public boolean canReadSegments() {
        return hasAnyRole(
                SystemRoleName.ADMIN.name(),
                SystemRoleName.CAMPAIGN_MANAGER.name(),
                SystemRoleName.BI_ANALYST.name(),
                SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    public boolean canPreviewSegments() {
        return hasAnyRole(
                SystemRoleName.ADMIN.name(),
                SystemRoleName.CAMPAIGN_MANAGER.name(),
                SystemRoleName.BI_ANALYST.name());
    }

    public boolean canApproveCampaigns() {
        return hasAnyRole(SystemRoleName.ADMIN.name(), SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    /** Campaign compliance review decisions: approve, reject, and review notes. */
    public boolean canReviewCampaigns() {
        return canApproveCampaigns();
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
