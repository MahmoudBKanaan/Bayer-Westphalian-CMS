package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * KB item 218: update draft campaign endpoint {@code PUT /api/campaigns/{id}} for Campaign Manager
 * and Admin (FR-057).
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignDraftCanBeUpdatedTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID SEGMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T11:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanUpdateDraftCampaignViaPutApi() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
                .thenReturn(updatedDraftView());

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign updated"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Updated life renewal"))
                .andExpect(jsonPath("$.data.objective").value("Refined renewal objective"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.channel").value("SMS"))
                .andExpect(jsonPath("$.data.messageSubject").value("Updated subject"))
                .andExpect(jsonPath("$.data.segmentId").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.productIds[0]").value(PRODUCT_ID.toString()));

        ArgumentCaptor<UpdateCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateCampaignCommand.class);
        verify(campaignService).updateCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        UpdateCampaignCommand command = commandCaptor.getValue();
        assertThat(command.name()).isEqualTo("Updated life renewal");
        assertThat(command.objective()).isEqualTo("Refined renewal objective");
        assertThat(command.channel()).isEqualTo(CampaignChannel.SMS);
        assertThat(command.segmentId()).isEqualTo(SEGMENT_ID);
        assertThat(command.messageSubject()).isEqualTo("Updated subject");
        assertThat(command.messageBody()).isEqualTo("Updated body");
        assertThat(command.startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(command.endDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(command.productIds()).containsExactly(PRODUCT_ID);
    }

    @Test
    void adminCanUpdateDraftCampaignViaPutApi() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(campaignService.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
                .thenReturn(updatedDraftView());

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign updated"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(campaignService).updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class));
    }

    @ParameterizedTest
    @MethodSource("rolesDeniedCampaignUpdate")
    void deniedRolesCannotUpdateDraftCampaignViaPutApi(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign updated"))));

        verify(campaignService, never())
                .updateCampaign(any(UUID.class), any(UpdateCampaignCommand.class));
    }

    @Test
    void unauthenticatedCallerCannotUpdateDraftCampaign() throws Exception {
        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaign updated"))));

        verify(campaignService, never())
                .updateCampaign(any(UUID.class), any(UpdateCampaignCommand.class));
    }

    @Test
    void rejectsInvalidUpdateDraftCampaignPayload() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": " ",
                                          "objective": " ",
                                          "channel": null
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns/" + CAMPAIGN_ID));

        verify(campaignService, never())
                .updateCampaign(any(UUID.class), any(UpdateCampaignCommand.class));
    }

    @Test
    void returnsNotFoundWhenDraftCampaignMissing() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsForbiddenWhenNonOwnerUpdatesDraft() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
                .thenThrow(new ForbiddenException("Campaign is not owned by the current user"));

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void returnsBusinessRuleWhenSubmittedCampaignCannotBeEditedAsDraft() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Campaign cannot be edited in status SUBMITTED; only DRAFT or REJECTED"));

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateCampaignPayload()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    @Test
    void securityConfigurationAllowsAdminAndCampaignManagerToPutCampaigns() {
        assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    private static Stream<SystemRoleName> rolesDeniedCampaignUpdate() {
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

    private static String updateCampaignPayload() {
        return """
                {
                  "name": "Updated life renewal",
                  "objective": "Refined renewal objective",
                  "segmentId": "%s",
                  "channel": "SMS",
                  "messageSubject": "Updated subject",
                  "messageBody": "Updated body",
                  "startDate": "2026-10-01",
                  "endDate": "2026-10-31",
                  "productIds": ["%s"]
                }
                """
                .formatted(SEGMENT_ID, PRODUCT_ID);
    }

    private static CampaignView updatedDraftView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Updated life renewal",
                "Refined renewal objective",
                CampaignStatus.DRAFT,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
                CampaignChannel.SMS,
                "Updated subject",
                "Updated body",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                null,
                null,
                null,
                null,
                null,
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }
}
