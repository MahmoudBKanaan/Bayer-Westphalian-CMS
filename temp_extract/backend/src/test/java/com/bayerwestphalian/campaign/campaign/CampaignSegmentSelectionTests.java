package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 222 / FR-053: campaign segment selection via {@code PUT/GET /api/campaigns/{id}/segment}.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignSegmentSelectionTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T11:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanSelectTargetSegment() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectSegment(eq(CAMPAIGN_ID), any(SelectCampaignSegmentCommand.class)))
                .thenReturn(viewWithSegment(SEGMENT_ID, "Munich prospects"));

        mockMvc.perform(
                        put("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "segmentId": "%s"
                                        }
                                        """
                                                .formatted(SEGMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign segment updated"))
                .andExpect(jsonPath("$.data.segmentId").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.segmentName").value("Munich prospects"));

        ArgumentCaptor<SelectCampaignSegmentCommand> commandCaptor =
                ArgumentCaptor.forClass(SelectCampaignSegmentCommand.class);
        verify(campaignService).selectSegment(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().segmentId()).isEqualTo(SEGMENT_ID);
    }

    @Test
    void campaignManagerCanClearSegmentSelectionWithNullSegmentId() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectSegment(eq(CAMPAIGN_ID), any(SelectCampaignSegmentCommand.class)))
                .thenReturn(viewWithSegment(null, null));

        mockMvc.perform(
                        put("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "segmentId": null
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign segment updated"))
                .andExpect(jsonPath("$.data.segmentId").value(nullValue()))
                .andExpect(jsonPath("$.data.segmentName").value(nullValue()));

        ArgumentCaptor<SelectCampaignSegmentCommand> commandCaptor =
                ArgumentCaptor.forClass(SelectCampaignSegmentCommand.class);
        verify(campaignService).selectSegment(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().segmentId()).isNull();
    }

    @Test
    void campaignManagerCanGetSelectedSegmentId() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.getSelectedSegmentId(CAMPAIGN_ID)).thenReturn(SEGMENT_ID);

        mockMvc.perform(
                        get("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign segment loaded"))
                .andExpect(jsonPath("$.data").value(SEGMENT_ID.toString()));

        verify(campaignService).getSelectedSegmentId(CAMPAIGN_ID);
    }

    @Test
    void campaignManagerCanGetNullWhenNoSegmentSelected() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.getSelectedSegmentId(CAMPAIGN_ID)).thenReturn(null);

        mockMvc.perform(
                        get("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign segment loaded"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void adminCanSelectCampaignSegment() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(campaignService.selectSegment(eq(CAMPAIGN_ID), any(SelectCampaignSegmentCommand.class)))
                .thenReturn(viewWithSegment(SEGMENT_ID, "Munich prospects"));

        mockMvc.perform(
                        put("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "segmentId": "%s"
                                        }
                                        """
                                                .formatted(SEGMENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign segment updated"));

        verify(campaignService)
                .selectSegment(eq(CAMPAIGN_ID), any(SelectCampaignSegmentCommand.class));
    }

    @Test
    void biAnalystCannotSelectCampaignSegment() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

        mockMvc.perform(
                        put("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "segmentId": "%s"
                                        }
                                        """
                                                .formatted(SEGMENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign segment updated"))));

        verify(campaignService, never())
                .selectSegment(any(UUID.class), any(SelectCampaignSegmentCommand.class));
    }

    @Test
    void returnsNotFoundWhenSegmentMissing() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectSegment(eq(CAMPAIGN_ID), any(SelectCampaignSegmentCommand.class)))
                .thenThrow(new ResourceNotFoundException("Segment", SEGMENT_ID));

        mockMvc.perform(
                        put("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "segmentId": "%s"
                                        }
                                        """
                                                .formatted(SEGMENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsBusinessRuleWhenSegmentChangedOnSubmittedCampaign() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.selectSegment(eq(CAMPAIGN_ID), any(SelectCampaignSegmentCommand.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Campaign targeting (segment/products) cannot be changed in status SUBMITTED; only DRAFT or REJECTED"));

        mockMvc.perform(
                        put("/api/campaigns/{id}/segment", CAMPAIGN_ID)
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "segmentId": "%s"
                                        }
                                        """
                                                .formatted(SEGMENT_ID)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    @Test
    void createAndUpdatePayloadsSupportSegmentSelectionField() {
        CreateCampaignCommand create =
                new CreateCampaignRequest(
                                "Name",
                                "Objective",
                                SEGMENT_ID,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of())
                        .toCommand();
        UpdateCampaignCommand update =
                new UpdateCampaignRequest(
                                "Name",
                                "Objective",
                                SEGMENT_ID,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                null)
                        .toCommand();

        assertThat(create.segmentId()).isEqualTo(SEGMENT_ID);
        assertThat(update.segmentId()).isEqualTo(SEGMENT_ID);
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

    private static CampaignView viewWithSegment(UUID segmentId, String segmentName) {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.DRAFT,
                OWNER_ID,
                "Campaign Manager",
                segmentId,
                segmentName,
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                null,
                List.of(),
                CREATED_AT,
                UPDATED_AT);
    }
}
