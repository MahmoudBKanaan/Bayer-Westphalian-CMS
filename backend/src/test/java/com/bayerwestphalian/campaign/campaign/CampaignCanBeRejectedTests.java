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
 * KB item 230 / FR-059: reject campaign endpoint {@code POST /api/campaigns/{id}/reject} moves a
 * submitted campaign into the rejected state with a required rejection reason (item 232).
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignCanBeRejectedTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T11:05:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void complianceOfficerCanRejectSubmittedCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenReturn(rejectedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign rejected"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.rejectionReason").value("Missing consent language"));

        ArgumentCaptor<RejectCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(RejectCampaignCommand.class);
        verify(campaignService).rejectCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().rejectionReason())
                .isEqualTo("Missing consent language");
        assertThat(commandCaptor.getValue().complianceReviewNotes()).isNull();
    }

    @Test
    void adminCanRejectSubmittedCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenReturn(rejectedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign rejected"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(campaignService).rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class));
    }

    @Test
    void rejectEndpointPassesOptionalComplianceReviewNotes() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenReturn(rejectedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "rejectionReason": "Missing consent language",
                                          "complianceReviewNotes": "Add explicit consent wording"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign rejected"));

        ArgumentCaptor<RejectCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(RejectCampaignCommand.class);
        verify(campaignService).rejectCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().complianceReviewNotes())
                .isEqualTo("Add explicit consent wording");
    }

    @Test
    void rejectsBlankRejectionReasonBeforeCallingService() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "rejectionReason": " "
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns/" + CAMPAIGN_ID + "/reject"));

        verify(campaignService, never())
                .rejectCampaign(any(UUID.class), any(RejectCampaignCommand.class));
    }

    @ParameterizedTest
    @MethodSource("rolesDeniedCampaignRejection")
    void deniedRolesCannotRejectCampaignViaPostApi(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign rejected"))));

        verify(campaignService, never())
                .rejectCampaign(any(UUID.class), any(RejectCampaignCommand.class));
    }

    @Test
    void unauthenticatedCallerCannotRejectCampaign() throws Exception {
        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaign rejected"))));

        verify(campaignService, never())
                .rejectCampaign(any(UUID.class), any(RejectCampaignCommand.class));
    }

    @Test
    void returnsNotFoundWhenCampaignForRejectionDoesNotExist() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsForbiddenWhenOwnerRejectsOwnCampaign() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenThrow(
                        new ForbiddenException(
                                "Campaign owner cannot approve or reject own campaign"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsBusinessRuleWhenCampaignCannotBeRejected() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceClaims());
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Only SUBMITTED campaigns can be rejected; current status is DRAFT"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(rejectPayload()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    @Test
    void securityConfigurationAllowsAdminAndComplianceToRejectCampaigns() {
        assertThat(SecurityConfiguration.COMPLIANCE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    private static Stream<SystemRoleName> rolesDeniedCampaignRejection() {
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
                UUID.fromString("10000000-0000-0000-0000-000000000106"),
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

    private static String rejectPayload() {
        return """
                {
                  "rejectionReason": "Missing consent language"
                }
                """;
    }

    private static CampaignView rejectedCampaignView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.REJECTED,
                OWNER_ID,
                "Campaign Manager",
                null,
                null,
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
                List.of(),
                CREATED_AT,
                UPDATED_AT);
    }
}
