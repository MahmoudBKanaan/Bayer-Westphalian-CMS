package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 219: campaign list endpoint {@code GET /api/campaigns} with optional filters for read
 * roles.
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignCanBeListedTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID CAMPAIGN_ID_2 =
            UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T10:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanListCampaignsWithoutFilters() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(draftView(), submittedView()));

        mockMvc.perform(
                        get("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaigns loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data[1].id").value(CAMPAIGN_ID_2.toString()))
                .andExpect(jsonPath("$.data[1].status").value("SUBMITTED"));

        ArgumentCaptor<CampaignSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CampaignSearchCriteria.class);
        verify(campaignService).searchCampaigns(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().ownerUserId()).isNull();
        assertThat(criteriaCaptor.getValue().status()).isNull();
        assertThat(criteriaCaptor.getValue().segmentId()).isNull();
    }

    @Test
    void campaignManagerCanListCampaignsWithKbFilters() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(draftView()));

        mockMvc.perform(
                        get("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .param("term", "life")
                                .param("ownerUserId", OWNER_ID.toString())
                                .param("status", "DRAFT")
                                .param("segmentId", SEGMENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data[0].segmentId").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"));

        ArgumentCaptor<CampaignSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CampaignSearchCriteria.class);
        verify(campaignService).searchCampaigns(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("life");
        assertThat(criteriaCaptor.getValue().ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(criteriaCaptor.getValue().segmentId()).isEqualTo(SEGMENT_ID);
    }

    @Test
    void biAnalystCanListCampaignsReadOnly() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(draftView()));

        mockMvc.perform(
                        get("/api/campaigns")
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaigns loaded"))
                .andExpect(jsonPath("$.data[0].name").value("Life renewal outreach"));

        verify(campaignService).searchCampaigns(any(CampaignSearchCriteria.class));
    }

    @Test
    void complianceOfficerCanListCampaignsForReviewQueue() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.COMPLIANCE_OFFICER));
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(submittedView()));

        mockMvc.perform(
                        get("/api/campaigns")
                                .header("Authorization", "Bearer compliance-token")
                                .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"));

        ArgumentCaptor<CampaignSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CampaignSearchCriteria.class);
        verify(campaignService).searchCampaigns(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(CampaignStatus.SUBMITTED);
    }

    @Test
    void returnsEmptyListWhenNoCampaignsMatch() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .param("term", "no-match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void rejectsOversizedListSearchTerm() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        get("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .param("term", "x".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns"));

        verify(campaignService, never()).searchCampaigns(any(CampaignSearchCriteria.class));
    }

    @Test
    void unauthenticatedCallerCannotListCampaigns() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaigns loaded"))));

        verify(campaignService, never()).searchCampaigns(any(CampaignSearchCriteria.class));
    }

    @ParameterizedTest
    @MethodSource("rolesAllowedCampaignList")
    void readRolesCanAccessCampaignListEndpoint(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-list-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(draftView()));

        mockMvc.perform(get("/api/campaigns").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaigns loaded"));
    }

    @Test
    void securityConfigurationCampaignReadRolesIncludeListConsumers() {
        assertThat(SecurityConfiguration.CAMPAIGN_READ_ROLES)
                .contains(
                        SystemRoleName.ADMIN.name(),
                        SystemRoleName.CAMPAIGN_MANAGER.name(),
                        SystemRoleName.BI_ANALYST.name(),
                        SystemRoleName.COMPLIANCE_OFFICER.name());
    }

    private static Stream<SystemRoleName> rolesAllowedCampaignList() {
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

    private static CampaignView draftView() {
        return view(
                CAMPAIGN_ID,
                "Life renewal outreach",
                CampaignStatus.DRAFT,
                CampaignChannel.EMAIL);
    }

    private static CampaignView submittedView() {
        return view(
                CAMPAIGN_ID_2,
                "Submitted compliance review",
                CampaignStatus.SUBMITTED,
                CampaignChannel.EMAIL);
    }

    private static CampaignView view(
            UUID id, String name, CampaignStatus status, CampaignChannel channel) {
        return new CampaignView(
                id,
                name,
                "Promote life insurance renewals",
                status,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
                channel,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                null,
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }
}
