package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 220: campaign details endpoint {@code GET /api/campaigns/{id}} returns full campaign
 * definition for read roles.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignDetailsCanBeLoadedTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID SEGMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID APPROVER_ID = UUID.fromString("10000000-0000-0000-0000-000000000106");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T10:30:00Z");
    private static final Instant APPROVED_AT = Instant.parse("2026-07-09T12:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanLoadCampaignDetails() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(fullDraftView());

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign loaded"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data.objective").value("Promote life insurance renewals"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.ownerFullName").value("Campaign Manager"))
                .andExpect(jsonPath("$.data.segmentId").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.segmentName").value("Munich prospects"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.messageSubject").value("Renew your cover"))
                .andExpect(jsonPath("$.data.messageBody").value("Dear customer, ..."))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-30"))
                .andExpect(jsonPath("$.data.productIds[0]").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        verify(campaignService).findById(CAMPAIGN_ID);
    }

    @Test
    void biAnalystCanLoadCampaignDetailsReadOnly() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(fullDraftView());

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign loaded"))
                .andExpect(jsonPath("$.data.name").value("Life renewal outreach"));

        verify(campaignService).findById(CAMPAIGN_ID);
    }

    @Test
    void complianceOfficerCanLoadSubmittedCampaignDetails() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(submittedView());

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.name").value("Life renewal outreach"));

        verify(campaignService).findById(CAMPAIGN_ID);
    }

    @Test
    void loadsApprovedCampaignDetailsWithApproverFields() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(approvedView());

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedByUserId").value(APPROVER_ID.toString()))
                .andExpect(jsonPath("$.data.approvedByFullName").value("Compliance Officer"))
                .andExpect(jsonPath("$.data.approvedAt").exists())
                .andExpect(jsonPath("$.data.rejectionReason").value(nullValue()));

        verify(campaignService).findById(CAMPAIGN_ID);
    }

    @Test
    void loadsRejectedCampaignDetailsWithRejectionReason() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(rejectedView());

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Missing consent language"))
                .andExpect(jsonPath("$.data.approvedByUserId").value(nullValue()));

        verify(campaignService).findById(CAMPAIGN_ID);
    }

    @Test
    void returnsNotFoundWhenCampaignMissing() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.findById(CAMPAIGN_ID))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/campaigns/" + CAMPAIGN_ID));
    }

    @Test
    void unauthenticatedCallerCannotLoadCampaignDetails() throws Exception {
        mockMvc.perform(get("/api/campaigns/{id}", CAMPAIGN_ID))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaign loaded"))));

        verify(campaignService, never()).findById(CAMPAIGN_ID);
    }

    @ParameterizedTest
    @MethodSource("rolesAllowedCampaignDetails")
    void readRolesCanAccessCampaignDetailsEndpoint(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-details-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(fullDraftView());

        mockMvc.perform(
                        get("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign loaded"));
    }

    @Test
    void securityConfigurationCampaignReadRolesCoverDetailsEndpoint() {
        assertThat(SecurityConfiguration.CAMPAIGN_READ_ROLES)
                .contains(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    private static Stream<SystemRoleName> rolesAllowedCampaignDetails() {
        return Stream.of(
                SystemRoleName.ADMIN,
                SystemRoleName.CAMPAIGN_MANAGER,
                SystemRoleName.BI_ANALYST,
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.EXECUTIVE_VIEWER,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                OWNER_ID,
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                role.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                List.of(role));
    }

    private static CampaignView fullDraftView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.DRAFT,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                null,
                null,
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CampaignView submittedView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.SUBMITTED,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                null,
                null,
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CampaignView approvedView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.APPROVED,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
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
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CampaignView rejectedView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.REJECTED,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                "Missing consent language",
                null,
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }
}
