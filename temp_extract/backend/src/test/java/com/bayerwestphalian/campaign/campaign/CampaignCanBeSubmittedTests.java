package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
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
 * KB item 228 / FR-058: submit campaign endpoint {@code POST /api/campaigns/{id}/submit} moves a
 * draft campaign into compliance review.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignCanBeSubmittedTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T10:45:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanSubmitDraftCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.submitCampaign(CAMPAIGN_ID)).thenReturn(submittedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign submitted"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"));

        verify(campaignService).submitCampaign(CAMPAIGN_ID);
    }

    @Test
    void adminCanSubmitDraftCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(campaignService.submitCampaign(CAMPAIGN_ID)).thenReturn(submittedCampaignView());

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign submitted"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        verify(campaignService).submitCampaign(CAMPAIGN_ID);
    }

    @ParameterizedTest
    @MethodSource("rolesDeniedCampaignSubmission")
    void deniedRolesCannotSubmitCampaignViaPostApi(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign submitted"))));

        verify(campaignService, never()).submitCampaign(any(UUID.class));
    }

    @Test
    void unauthenticatedCallerCannotSubmitCampaign() throws Exception {
        mockMvc.perform(post("/api/campaigns/{id}/submit", CAMPAIGN_ID))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaign submitted"))));

        verify(campaignService, never()).submitCampaign(any(UUID.class));
    }

    @Test
    void returnsNotFoundWhenCampaignForSubmissionDoesNotExist() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.submitCampaign(CAMPAIGN_ID))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsForbiddenWhenNonOwnerSubmitsCampaign() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.submitCampaign(CAMPAIGN_ID))
                .thenThrow(new ForbiddenException("Campaign is not owned by the current user"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsBusinessRuleWhenCampaignCannotBeSubmitted() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.submitCampaign(CAMPAIGN_ID))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Only DRAFT or REJECTED campaigns can be submitted; current status is ACTIVE"));

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    @Test
    void returnsValidationErrorWhenCampaignMissingRequiredFieldsForSubmission() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.submitCampaign(CAMPAIGN_ID))
                .thenThrow(
                        new ValidationException(
                                "Campaign submission validation failed",
                                List.of(
                                        "Campaign name is required.",
                                        "Campaign objective is required.",
                                        "Campaign channel is required.")));

        mockMvc.perform(
                        post("/api/campaigns/{id}/submit", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Campaign submission validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Campaign name is required."))
                .andExpect(jsonPath("$.details[1]").value("Campaign objective is required."))
                .andExpect(jsonPath("$.details[2]").value("Campaign channel is required."));
    }

    @Test
    void securityConfigurationAllowsAdminAndCampaignManagerToSubmitCampaigns() {
        assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    private static Stream<SystemRoleName> rolesDeniedCampaignSubmission() {
        return Stream.of(
                SystemRoleName.BI_ANALYST,
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                OWNER_ID,
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
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

    private static CampaignView submittedCampaignView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.SUBMITTED,
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
                null,
                null,
                List.of(),
                CREATED_AT,
                UPDATED_AT);
    }
}
