package com.bayerwestphalian.campaign.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthorizationExpressionsTests {

    private final AuthorizationExpressions authorizationExpressions =
            new AuthorizationExpressions();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniesAccessWhenAuthenticationIsMissing() {
        assertThat(authorizationExpressions.isAuthenticated()).isFalse();
        assertThat(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).isFalse();
        assertThat(authorizationExpressions.canManageUsers()).isFalse();
    }

    @Test
    void readsCurrentPrincipalAndRoleAuthoritiesFromSecurityContext() {
        UUID userId = UUID.fromString("10000000-0000-0000-0000-000000009901");
        authenticate(userId, SystemRoleName.ADMIN, SystemRoleName.BI_ANALYST);

        assertThat(authorizationExpressions.isAuthenticated()).isTrue();
        assertThat(authorizationExpressions.currentUserId()).isEqualTo(userId);
        assertThat(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).isTrue();
        assertThat(authorizationExpressions.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(authorizationExpressions.hasAnyRole("PRODUCT_MANAGER", "BI_ANALYST")).isTrue();
        assertThat(authorizationExpressions.hasRole(SystemRoleName.PRODUCT_MANAGER.name()))
                .isFalse();
    }

    @Test
    void mapsKbAdminPermissions() {
        authenticate(SystemRoleName.ADMIN);

        assertThat(authorizationExpressions.canManageUsers()).isTrue();
        assertThat(authorizationExpressions.canManageRoles()).isTrue();
        assertThat(authorizationExpressions.canManageCampaigns()).isTrue();
        assertThat(authorizationExpressions.canManageSegments()).isTrue();
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
        assertThat(authorizationExpressions.canReadSegments()).isTrue();
        assertThat(authorizationExpressions.canPreviewSegments()).isTrue();
        assertThat(authorizationExpressions.canManageProducts()).isTrue();
        assertThat(authorizationExpressions.canApproveCampaigns()).isTrue();
        assertThat(authorizationExpressions.canReviewCampaigns()).isTrue();
        assertThat(authorizationExpressions.canViewAuditLogs()).isTrue();
        assertThat(authorizationExpressions.canViewReports()).isTrue();
    }

    @Test
    void mapsKbCampaignManagerPermissions() {
        authenticate(SystemRoleName.CAMPAIGN_MANAGER);

        assertThat(authorizationExpressions.canManageCampaigns()).isTrue();
        assertThat(authorizationExpressions.canReadCampaigns()).isTrue();
        assertThat(authorizationExpressions.canManageSegments()).isTrue();
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
        assertThat(authorizationExpressions.canReadSegments()).isTrue();
        assertThat(authorizationExpressions.canPreviewSegments()).isTrue();
        assertThat(authorizationExpressions.canViewReports()).isTrue();
        assertThat(authorizationExpressions.canManageUsers()).isFalse();
        assertThat(authorizationExpressions.canApproveCampaigns()).isFalse();
        assertThat(authorizationExpressions.canReviewCampaigns()).isFalse();
        assertThat(authorizationExpressions.canManageProducts()).isFalse();
    }

    @Test
    void mapsKbCampaignManagerSegmentCreationPermissions() {
        // KB FR-077 / item 201: Campaign Manager can create reusable segments.
        authenticate(SystemRoleName.CAMPAIGN_MANAGER);

        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
        assertThat(authorizationExpressions.canManageSegments()).isTrue();
    }

    @Test
    void segmentCreationPermissionsMatchManageRolesForAdminAndCampaignManager() {
        authenticate(SystemRoleName.ADMIN);
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();

        authenticate(SystemRoleName.CAMPAIGN_MANAGER);
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
    }

    @Test
    void unauthorizedRolesCannotManageProducts() {
        authenticate(SystemRoleName.BI_ANALYST);
        assertThat(authorizationExpressions.canManageProducts()).isFalse();

        authenticate(SystemRoleName.CUSTOMER_SERVICE_AGENT);
        assertThat(authorizationExpressions.canManageProducts()).isFalse();

        authenticate(SystemRoleName.SALES_AGENT);
        assertThat(authorizationExpressions.canManageProducts()).isFalse();
    }

    @Test
    void mapsKbSegmentReadPermissionsForBiAnalystAndComplianceOfficer() {
        authenticate(SystemRoleName.BI_ANALYST);

        assertThat(authorizationExpressions.canReadSegments()).isTrue();
        assertThat(authorizationExpressions.canPreviewSegments()).isTrue();
        assertThat(authorizationExpressions.canManageSegments()).isFalse();
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.COMPLIANCE_OFFICER);

        assertThat(authorizationExpressions.canReadSegments()).isTrue();
        assertThat(authorizationExpressions.canPreviewSegments()).isFalse();
        assertThat(authorizationExpressions.canManageSegments()).isFalse();
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();
    }

    @Test
    void biAnalystCannotEditSegmentUnlessAlsoGrantedManageRole() {
        // KB item 200: BI alone denied; dual-role with CAMPAIGN_MANAGER or ADMIN is allowed.
        authenticate(SystemRoleName.BI_ANALYST);
        assertThat(authorizationExpressions.canManageSegments()).isFalse();
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.BI_ANALYST, SystemRoleName.CAMPAIGN_MANAGER);
        assertThat(authorizationExpressions.canManageSegments()).isTrue();
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();

        authenticate(SystemRoleName.BI_ANALYST, SystemRoleName.ADMIN);
        assertThat(authorizationExpressions.canManageSegments()).isTrue();
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
    }

    @Test
    void nonCampaignRolesCannotCreateSegments() {
        authenticate(SystemRoleName.BI_ANALYST);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.PRODUCT_MANAGER);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.CUSTOMER_SERVICE_AGENT);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.SALES_AGENT);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.EXECUTIVE_VIEWER);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.SYSTEM_AUDITOR);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();

        authenticate(SystemRoleName.MARKETING_ANALYST);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();
    }

    @Test
    void mapsKbComplianceAndAuditorPermissions() {
        authenticate(SystemRoleName.COMPLIANCE_OFFICER);

        assertThat(authorizationExpressions.canApproveCampaigns()).isTrue();
        assertThat(authorizationExpressions.canReviewCampaigns()).isTrue();
        assertThat(authorizationExpressions.canViewAuditLogs()).isTrue();
        assertThat(authorizationExpressions.canManageProducts()).isFalse();

        authenticate(SystemRoleName.SYSTEM_AUDITOR);

        assertThat(authorizationExpressions.canViewAuditLogs()).isTrue();
        assertThat(authorizationExpressions.canApproveCampaigns()).isFalse();
        assertThat(authorizationExpressions.canReviewCampaigns()).isFalse();
    }

    @Test
    void mapsKbProductAndReportPermissions() {
        authenticate(SystemRoleName.PRODUCT_MANAGER);

        assertThat(authorizationExpressions.canManageProducts()).isTrue();
        assertThat(authorizationExpressions.canManageProductOwnership()).isTrue();
        assertThat(authorizationExpressions.canReadCustomers()).isTrue();
        assertThat(authorizationExpressions.canManageCampaigns()).isFalse();
        assertThat(authorizationExpressions.canApproveCampaigns()).isFalse();
        assertThat(authorizationExpressions.canReviewCampaigns()).isFalse();
        assertThat(authorizationExpressions.canManageUsers()).isFalse();
        assertThat(authorizationExpressions.canViewAuditLogs()).isFalse();
        assertThat(authorizationExpressions.canViewReports()).isFalse();

        authenticate(SystemRoleName.EXECUTIVE_VIEWER);

        assertThat(authorizationExpressions.canViewReports()).isTrue();
        assertThat(authorizationExpressions.canViewAuditLogs()).isFalse();

        // KB item 458: Marketing Analyst is authorized for restricted campaign report export.
        authenticate(SystemRoleName.MARKETING_ANALYST);
        assertThat(authorizationExpressions.canViewReports()).isTrue();
        assertThat(authorizationExpressions.canViewAuditLogs()).isFalse();

        // KB item 458: unauthorized roles cannot export restricted campaign reports.
        authenticate(SystemRoleName.COMPLIANCE_OFFICER);
        assertThat(authorizationExpressions.canViewReports()).isFalse();
        authenticate(SystemRoleName.CUSTOMER_SERVICE_AGENT);
        assertThat(authorizationExpressions.canViewReports()).isFalse();
        authenticate(SystemRoleName.SALES_AGENT);
        assertThat(authorizationExpressions.canViewReports()).isFalse();
        authenticate(SystemRoleName.SYSTEM_AUDITOR);
        assertThat(authorizationExpressions.canViewReports()).isFalse();
    }

    @Test
    void requireRoleThrowsAccessDeniedWhenRoleIsMissing() {
        authenticate(SystemRoleName.BI_ANALYST);

        assertThatThrownBy(() -> authorizationExpressions.requireRole(SystemRoleName.ADMIN.name()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Role is not allowed to perform this action");
    }

    @Test
    void currentUserIdThrowsAccessDeniedWhenPrincipalIsMissing() {
        assertThatThrownBy(authorizationExpressions::currentUserId)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication is required");
    }

    private static void authenticate(SystemRoleName... roles) {
        authenticate(UUID.fromString("10000000-0000-0000-0000-000000009901"), roles);
    }

    private static void authenticate(UUID userId, SystemRoleName... roles) {
        List<SystemRoleName> roleList = List.of(roles);
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(userId, "admin@bayer-westphalian.test", roleList);
        List<SimpleGrantedAuthority> authorities =
                roleList.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, "access-token", authorities));
    }
}
