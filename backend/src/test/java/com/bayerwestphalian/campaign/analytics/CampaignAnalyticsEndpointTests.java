package com.bayerwestphalian.campaign.analytics;

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
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KB item 432: Implement campaign analytics endpoint.
 *
 * <p>{@code GET /api/analytics/campaigns/{campaignId}} returns campaign identity and optional
 * performance metrics for authorized analytics roles.
 */
@WebMvcTest(controllers = AnalyticsController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class CampaignAnalyticsEndpointTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000432");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000432");
    private static final UUID MISSING_ID =
            UUID.fromString("50000000-0000-0000-0000-00000000dead");

    private static final String CAMPAIGN_ANALYTICS_PATH = "/api/analytics/campaigns/{campaignId}";

    @Autowired private MockMvc mockMvc;

    @MockBean private AnalyticsService analyticsService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignAnalyticsEndpointIsMappedUnderAnalyticsApi() throws Exception {
        assertThat(AnalyticsController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(AnalyticsController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/analytics");

        Method method = AnalyticsController.class.getMethod("getCampaignAnalytics", UUID.class);
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/campaigns/{campaignId}");
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("EXECUTIVE_VIEWER")
                .contains("MARKETING_ANALYST");
    }

    @Test
    void biAnalystReceivesCampaignIdentityAndMetricsPayload() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenReturn(sampleCampaignAnalytics());

        mockMvc.perform(
                        get(CAMPAIGN_ANALYTICS_PATH, CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-token")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign analytics loaded"))
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.campaignName").value("Campaign analytics campaign"))
                .andExpect(jsonPath("$.data.objective").value("Raise renewal awareness"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.startDate").value("2026-07-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-08-01"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.ownerFullName").value("Analytics Owner"))
                .andExpect(jsonPath("$.data.metrics.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.metrics.audienceSize").value(12))
                .andExpect(jsonPath("$.data.metrics.eligibleCount").value(10))
                .andExpect(jsonPath("$.data.metrics.excludedCount").value(2))
                .andExpect(jsonPath("$.data.metrics.sentCount").value(10))
                .andExpect(jsonPath("$.data.metrics.openedCount").value(5))
                .andExpect(jsonPath("$.data.metrics.clickedCount").value(2))
                .andExpect(jsonPath("$.data.metrics.repliedCount").value(1))
                .andExpect(jsonPath("$.data.metrics.convertedCount").value(1))
                .andExpect(jsonPath("$.data.metrics.openRate").value(0.5))
                .andExpect(jsonPath("$.data.metrics.clickRate").value(0.2))
                .andExpect(jsonPath("$.data.metrics.conversionRate").value(0.1))
                .andExpect(jsonPath("$.data.metrics.estimatedCost").value(40.0))
                .andExpect(jsonPath("$.data.metrics.estimatedRevenue").value(60.0))
                .andExpect(jsonPath("$.data.metrics.estimatedRoi").value(0.5))
                .andExpect(jsonPath("$.data.generatedAt").exists());

        verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
    }

    @Test
    void campaignAnalyticsAllowsNullMetricsWhenCampaignNotLaunched() throws Exception {
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenReturn(sampleCampaignAnalyticsWithoutMetrics());

        mockMvc.perform(
                        get(CAMPAIGN_ANALYTICS_PATH, CAMPAIGN_ID)
                                .header("Authorization", "Bearer cm-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.campaignName").value("Draft campaign"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.metrics").value(nullValue()));

        verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
    }

    @Test
    void campaignAnalyticsReturnsNotFoundWhenCampaignMissing() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(analyticsService.getCampaignAnalytics(MISSING_ID))
                .thenThrow(new ResourceNotFoundException("Campaign", MISSING_ID));

        mockMvc.perform(
                        get(CAMPAIGN_ANALYTICS_PATH, MISSING_ID)
                                .header("Authorization", "Bearer bi-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verify(analyticsService).getCampaignAnalytics(MISSING_ID);
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanAccessCampaignAnalyticsEndpoint(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenReturn(sampleCampaignAnalyticsWithoutMetrics());

        mockMvc.perform(
                        get(CAMPAIGN_ANALYTICS_PATH, CAMPAIGN_ID)
                                .header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()));

        verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(CAMPAIGN_ANALYTICS_PATH, CAMPAIGN_ID))
                .andExpect(status().isUnauthorized());
        verify(analyticsService, never()).getCampaignAnalytics(CAMPAIGN_ID);
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotAccessCampaignAnalyticsEndpoint(SystemRoleName role)
            throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        get(CAMPAIGN_ANALYTICS_PATH, CAMPAIGN_ID)
                                .header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Campaign analytics loaded"))));

        verify(analyticsService, never()).getCampaignAnalytics(CAMPAIGN_ID);
    }

    static Stream<SystemRoleName> authorizedRoles() {
        return Stream.of(
                SystemRoleName.ADMIN,
                SystemRoleName.BI_ANALYST,
                SystemRoleName.CAMPAIGN_MANAGER,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER);
    }

    static Stream<SystemRoleName> unauthorizedRoles() {
        return Stream.of(
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                OWNER_ID, "campaign-analytics.user@bayer-westphalian.test", List.of(role));
    }

    private static CampaignAnalyticsView sampleCampaignAnalytics() {
        CampaignMetricsView metrics =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Campaign analytics campaign",
                        CampaignStatus.ACTIVE,
                        12,
                        10,
                        2,
                        10,
                        5,
                        2,
                        1,
                        1,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.1000"),
                        new BigDecimal("40.00"),
                        new BigDecimal("60.00"),
                        new BigDecimal("0.50"),
                        Instant.parse("2026-07-11T10:00:00Z"));
        return new CampaignAnalyticsView(
                CAMPAIGN_ID,
                "Campaign analytics campaign",
                "Raise renewal awareness",
                CampaignStatus.ACTIVE,
                CampaignChannel.EMAIL,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                OWNER_ID,
                "Analytics Owner",
                metrics,
                Instant.parse("2026-07-11T11:00:00Z"));
    }

    private static CampaignAnalyticsView sampleCampaignAnalyticsWithoutMetrics() {
        return new CampaignAnalyticsView(
                CAMPAIGN_ID,
                "Draft campaign",
                "Not launched yet",
                CampaignStatus.DRAFT,
                CampaignChannel.EMAIL,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                OWNER_ID,
                "Analytics Owner",
                null,
                Instant.parse("2026-07-11T11:00:00Z"));
    }
}
