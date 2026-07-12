package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 229 / FR-059: approve campaign endpoint {@code POST /api/campaigns/{id}/approve} moves a
 * submitted campaign into the approved state.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignCanBeApprovedTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID APPROVER_ID = UUID.fromString("10000000-0000-0000-0000-000000000106");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T11:00:00Z");
    private static final Instant APPROVED_AT = Instant.parse("2026-07-09T10:55:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void complianceOfficerCanApproveSubmittedCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenReturn(approvedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign approved"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedByUserId").value(APPROVER_ID.toString()))
                .andExpect(jsonPath("$.data.approvedByFullName").value("Compliance Officer"))
                .andExpect(jsonPath("$.data.approvedAt").value(APPROVED_AT.toString()));

        ArgumentCaptor<ApproveCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(ApproveCampaignCommand.class);
        verify(campaignService).approveCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().complianceReviewNotes()).isNull();
    }

    @Test
    void adminCanApproveSubmittedCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenReturn(approvedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign approved"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(campaignService).approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class));
    }

    @Test
    void approveEndpointPassesOptionalReviewNotes() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenReturn(approvedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "complianceReviewNotes": "Approved after compliance review"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign approved"));

        ArgumentCaptor<ApproveCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(ApproveCampaignCommand.class);
        verify(campaignService).approveCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().complianceReviewNotes())
                .isEqualTo("Approved after compliance review");
    }

    @ParameterizedTest
    @MethodSource("rolesDeniedCampaignApproval")
    void deniedRolesCannotApproveCampaignViaPostApi(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign approved"))));

        verify(campaignService, never())
                .approveCampaign(any(UUID.class), any(ApproveCampaignCommand.class));
    }

    @Test
    void campaignManagerCannotApproveOwnCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign approved"))));

        verify(campaignService, never())
                .approveCampaign(any(UUID.class), any(ApproveCampaignCommand.class));
    }

    @Test
    void productManagerCannotApproveCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.PRODUCT_MANAGER));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign approved"))));

        verify(campaignService, never())
                .approveCampaign(any(UUID.class), any(ApproveCampaignCommand.class));
    }

    @Test
    void unauthenticatedCallerCannotApproveCampaign() throws Exception {
        mockMvc.perform(post("/api/campaigns/{id}/approve", CAMPAIGN_ID))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaign approved"))));

        verify(campaignService, never())
                .approveCampaign(any(UUID.class), any(ApproveCampaignCommand.class));
    }

    @Test
    void returnsNotFoundWhenCampaignForApprovalDoesNotExist() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsForbiddenWhenOwnerApprovesOwnCampaign() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenThrow(
                        new ForbiddenException(
                                "Campaign owner cannot approve or reject own campaign"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsBusinessRuleWhenCampaignCannotBeApproved() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Only SUBMITTED campaigns can be approved; current status is DRAFT"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    @Test
    void securityConfigurationAllowsAdminAndComplianceToApproveCampaigns() {
        assertThat(SecurityConfiguration.COMPLIANCE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    private static Stream<SystemRoleName> rolesDeniedCampaignApproval() {
        return Stream.of(
                SystemRoleName.CAMPAIGN_MANAGER,
                SystemRoleName.BI_ANALYST,
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims complianceClaims() {
        return new JwtTokenClaims(
                APPROVER_ID,
                "compliance.officer@bayer-westphalian.test",
                List.of(SystemRoleName.COMPLIANCE_OFFICER));
    }

    private static JwtTokenClaims adminClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                "admin@bayer-westphalian.test",
                List.of(SystemRoleName.ADMIN));
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                role.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                List.of(role));
    }

    private static CampaignView approvedCampaignView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.APPROVED,
                OWNER_ID,
                "Campaign Manager",
                null,
                null,
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                APPROVER_ID,
                "Compliance Officer",
                APPROVED_AT,
                null,
                null,
                List.of(),
                CREATED_AT,
                UPDATED_AT);
    }
}
