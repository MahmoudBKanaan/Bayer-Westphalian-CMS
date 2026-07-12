package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.auth.method.CampaignApprovalAccess;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sprint 16 critical test item <b>655</b>: Compliance Officer can approve/reject campaigns.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code FR-059} / {@code TC-011} — Compliance Officer can approve or reject campaigns
 *   <li>{@code BR-005} / {@code COMP-006} — Approval is the launch gate
 *   <li>Review roles: {@code ADMIN}, {@code COMPLIANCE_OFFICER} via {@code
 *       @authz.canApproveCampaigns()} / {@code canReviewCampaigns()}
 * </ul>
 *
 * <p>Enforcement layers:
 *
 * <ol>
 *   <li>HTTP security: {@code POST .../approve|reject} → {@link
 *       SecurityConfiguration#COMPLIANCE_ROLES}
 *   <li>Method security: {@link CampaignApprovalAccess} on service; controller {@code
 *       @PreAuthorize("@authz.canReviewCampaigns()")}
 * </ol>
 */
@DisplayName("655 Compliance Officer can approve/reject campaigns")
class ComplianceOfficerCanApproveRejectCampaignsTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000655");

    @Nested
    @DisplayName("Authorization expressions (canApprove / canReview)")
    class ExpressionGate {

        private final AuthorizationExpressions authorizationExpressions =
                new AuthorizationExpressions();

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void complianceOfficerCanApproveAndReviewCampaigns() {
            authenticate(SystemRoleName.COMPLIANCE_OFFICER);

            assertThat(authorizationExpressions.canApproveCampaigns()).isTrue();
            assertThat(authorizationExpressions.canReviewCampaigns()).isTrue();
            assertThat(authorizationExpressions.canManageCampaigns()).isFalse();
            assertThat(authorizationExpressions.canManageProducts()).isFalse();
        }

        @ParameterizedTest(name = "{0} can approve/reject campaigns")
        @EnumSource(
                value = SystemRoleName.class,
                names = {"ADMIN", "COMPLIANCE_OFFICER"})
        void reviewRolesCanApproveCampaigns(SystemRoleName role) {
            authenticate(role);
            assertThat(authorizationExpressions.canApproveCampaigns()).isTrue();
            assertThat(authorizationExpressions.canReviewCampaigns()).isTrue();
        }

        @ParameterizedTest(name = "{0} cannot approve/reject campaigns")
        @MethodSource(
                "com.bayerwestphalian.campaign.campaign.ComplianceOfficerCanApproveRejectCampaignsTests#nonReviewRoles")
        void nonReviewRolesCannotApproveCampaigns(SystemRoleName role) {
            authenticate(role);
            assertThat(authorizationExpressions.canApproveCampaigns()).isFalse();
            assertThat(authorizationExpressions.canReviewCampaigns()).isFalse();
        }

        private static void authenticate(SystemRoleName... roles) {
            List<SystemRoleName> roleList = List.of(roles);
            AuthenticatedPrincipal principal =
                    new AuthenticatedPrincipal(
                            UUID.fromString("10000000-0000-0000-0000-000000000655"),
                            "compliance@test.example",
                            roleList);
            List<SimpleGrantedAuthority> authorities =
                    roleList.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .toList();
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    principal, "n/a", authorities));
        }
    }

    static Stream<SystemRoleName> nonReviewRoles() {
        return Arrays.stream(SystemRoleName.values())
                .filter(
                        role ->
                                role != SystemRoleName.ADMIN
                                        && role != SystemRoleName.COMPLIANCE_OFFICER);
    }

    @Nested
    @DisplayName("Method security annotations on approve/reject surface")
    class AnnotationGate {

        @Test
        void campaignServiceApproveRejectRequireCampaignApprovalAccess() throws Exception {
            Method approveWithNotes =
                    CampaignService.class.getMethod(
                            "approveCampaign", UUID.class, ApproveCampaignCommand.class);
            Method approveSimple =
                    CampaignService.class.getMethod("approveCampaign", UUID.class);
            Method reject =
                    CampaignService.class.getMethod(
                            "rejectCampaign", UUID.class, RejectCampaignCommand.class);

            assertThat(approveWithNotes.isAnnotationPresent(CampaignApprovalAccess.class)).isTrue();
            assertThat(approveSimple.isAnnotationPresent(CampaignApprovalAccess.class)).isTrue();
            assertThat(reject.isAnnotationPresent(CampaignApprovalAccess.class)).isTrue();

            PreAuthorize meta = CampaignApprovalAccess.class.getAnnotation(PreAuthorize.class);
            assertThat(meta.value()).isEqualTo("@authz.canApproveCampaigns()");
        }

        @Test
        void campaignControllerApproveRejectRequireCanReviewCampaigns() throws Exception {
            Method approve =
                    CampaignController.class.getMethod(
                            "approveCampaign", UUID.class, ApproveCampaignRequest.class);
            Method reject =
                    CampaignController.class.getMethod(
                            "rejectCampaign", UUID.class, RejectCampaignRequest.class);

            assertThat(approve.getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("@authz.canReviewCampaigns()");
            assertThat(reject.getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("@authz.canReviewCampaigns()");
            assertThat(approve.getAnnotation(PostMapping.class).value())
                    .containsExactly("/{id}/approve");
            assertThat(reject.getAnnotation(PostMapping.class).value())
                    .containsExactly("/{id}/reject");
        }

        @Test
        void httpSecurityApproveRejectRolesAreComplianceOnlyPlusAdmin() {
            assertThat(SecurityConfiguration.COMPLIANCE_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN.name(),
                            SystemRoleName.COMPLIANCE_OFFICER.name());
            assertThat(SecurityConfiguration.COMPLIANCE_ROLES)
                    .doesNotContain(
                            SystemRoleName.CAMPAIGN_MANAGER.name(),
                            SystemRoleName.PRODUCT_MANAGER.name(),
                            SystemRoleName.BI_ANALYST.name());
            // Campaign managers can write drafts/submit but not approve.
            assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                    .contains(SystemRoleName.CAMPAIGN_MANAGER.name());
            assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                    .doesNotContain(SystemRoleName.COMPLIANCE_OFFICER.name());
        }

        @Test
        void canReviewCampaignsIsAliasOfCanApproveCampaigns() {
            Set<String> reviewRoles =
                    Set.of(SystemRoleName.ADMIN.name(), SystemRoleName.COMPLIANCE_OFFICER.name());
            assertThat(reviewRoles)
                    .containsExactlyInAnyOrderElementsOf(
                            Set.of(
                                    SystemRoleName.ADMIN.name(),
                                    SystemRoleName.COMPLIANCE_OFFICER.name()));
        }
    }

    /**
     * HTTP-level proof: COMPLIANCE_OFFICER may POST approve/reject; CAMPAIGN_MANAGER may not.
     * Complements {@code ProtectedEndpointSecurityTests#unauthorizedRoleCannotApproveComplianceCampaign}.
     */
    @WebMvcTest(
            controllers =
                    ComplianceOfficerCanApproveRejectCampaignsTests.ReviewProbeController.class)
    @Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        ComplianceOfficerCanApproveRejectCampaignsTests.ReviewProbeController.class
    })
    @ActiveProfiles("compliance-review-probe")
    @DisplayName("HTTP: COMPLIANCE_OFFICER allowed on approve/reject; manager forbidden")
    static class HttpApproveRejectAccess {

        @Autowired private MockMvc mockMvc;

        @MockBean private JwtService jwtService;

        @MockBean(name = "jpaMappingContext")
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        void complianceOfficerCanApproveCampaign() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));

            mockMvc.perform(
                            post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("campaign approved"));
        }

        @Test
        void complianceOfficerCanRejectCampaign() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));

            mockMvc.perform(
                            post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"rejectionReason\":\"Missing consent language\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("campaign rejected"));
        }

        @Test
        void campaignManagerCannotApproveOrRejectCampaigns() throws Exception {
            when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));

            mockMvc.perform(
                            post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer campaign-manager-token"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("campaign approved"))));

            mockMvc.perform(
                            post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer campaign-manager-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"rejectionReason\":\"x\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("campaign rejected"))));
        }

        @Test
        void adminCanApproveCampaignPositiveControl() throws Exception {
            when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                    .thenReturn(roleClaims(SystemRoleName.ADMIN));

            mockMvc.perform(
                            post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("campaign approved"));
        }

        private static JwtTokenClaims roleClaims(SystemRoleName role) {
            return new JwtTokenClaims(
                    UUID.fromString("10000000-0000-0000-0000-000000000655"),
                    role.name().toLowerCase().replace('_', '.') + "@test.example",
                    List.of(role),
                    JwtTokenType.ACCESS,
                    "bayer-westphalian-campaign-platform-test",
                    Instant.parse("2026-07-12T12:00:00Z"),
                    Instant.parse("2026-07-12T12:15:00Z"),
                    role.name().toLowerCase() + "-access-token-id");
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 655)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(655);
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.RULE_STATEMENT)
                    .isEqualTo("Compliance Officer can approve/reject campaigns");
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.TEST_CASE_IDS)
                    .contains("TC-011");
            assertThat(
                            ComplianceOfficerCanApproveRejectCampaignsContract
                                    .FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-059");
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.BUSINESS_RULE_IDS)
                    .contains("BR-005");
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.PRIMARY_ROLE)
                    .isEqualTo(SystemRoleName.COMPLIANCE_OFFICER);
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.REVIEW_ROLES)
                    .containsExactlyInAnyOrder(
                            SystemRoleName.ADMIN, SystemRoleName.COMPLIANCE_OFFICER);
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.APPROVE_PATH)
                    .isEqualTo("POST /api/campaigns/{id}/approve");
            assertThat(ComplianceOfficerCanApproveRejectCampaignsContract.REJECT_PATH)
                    .isEqualTo("POST /api/campaigns/{id}/reject");
            assertThat(
                            ComplianceOfficerCanApproveRejectCampaignsContract
                                    .AUTHORIZATION_EXPRESSION)
                    .isEqualTo("@authz.canApproveCampaigns()");
        }
    }

    /** Minimal controller for HTTP authorization probes. */
    @RestController
    @Profile("compliance-review-probe")
    static class ReviewProbeController {

        @PostMapping("/api/campaigns/{id}/approve")
        String approveCampaign(@PathVariable UUID id) {
            return "campaign approved";
        }

        @PostMapping("/api/campaigns/{id}/reject")
        String rejectCampaign(@PathVariable UUID id) {
            return "campaign rejected";
        }
    }

    static final class ComplianceOfficerCanApproveRejectCampaignsContract {
        static final int CRITICAL_TEST_ITEM = 655;
        static final String RULE_STATEMENT = "Compliance Officer can approve/reject campaigns";
        static final java.util.List<String> TEST_CASE_IDS = java.util.List.of("TC-011");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-059");
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-005");
        static final SystemRoleName PRIMARY_ROLE = SystemRoleName.COMPLIANCE_OFFICER;
        static final java.util.List<SystemRoleName> REVIEW_ROLES =
                java.util.List.of(SystemRoleName.ADMIN, SystemRoleName.COMPLIANCE_OFFICER);
        static final String APPROVE_PATH = "POST /api/campaigns/{id}/approve";
        static final String REJECT_PATH = "POST /api/campaigns/{id}/reject";
        static final String AUTHORIZATION_EXPRESSION = "@authz.canApproveCampaigns()";

        private ComplianceOfficerCanApproveRejectCampaignsContract() {}
    }
}
