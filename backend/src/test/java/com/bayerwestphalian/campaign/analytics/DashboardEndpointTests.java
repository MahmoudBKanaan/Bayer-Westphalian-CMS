package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
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
 * KB item 431: Implement dashboard endpoint.
 *
 * <p>{@code GET /api/analytics/dashboard} returns platform KPIs (FR-100–FR-107) for authorized
 * analytics roles.
 */
@WebMvcTest(controllers = AnalyticsController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class DashboardEndpointTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000431");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000431");

    private static final String DASHBOARD_PATH = "/api/analytics/dashboard";

    @Autowired private MockMvc mockMvc;

    @MockBean private AnalyticsService analyticsService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void dashboardEndpointIsMappedUnderAnalyticsApi() throws Exception {
        assertThat(AnalyticsController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(AnalyticsController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/analytics");

        Method getDashboard = AnalyticsController.class.getMethod("getDashboard");
        assertThat(getDashboard.getAnnotation(GetMapping.class).value())
                .containsExactly("/dashboard");
        assertThat(getDashboard.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(getDashboard.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("EXECUTIVE_VIEWER")
                .contains("MARKETING_ANALYST");
    }

    @Test
    void biAnalystReceivesFullDashboardPayloadFr100ToFr107() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(analyticsService.getDashboard()).thenReturn(sampleDashboard());

        mockMvc.perform(
                        get(DASHBOARD_PATH)
                                .header("Authorization", "Bearer bi-token")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Analytics dashboard loaded"))
                // FR-100
                .andExpect(jsonPath("$.data.campaignTotal").value(3))
                // FR-101
                .andExpect(jsonPath("$.data.activeCampaigns").value(1))
                // FR-102
                .andExpect(jsonPath("$.data.audienceSize").value(100))
                // FR-103
                .andExpect(jsonPath("$.data.messagesSent").value(80))
                .andExpect(jsonPath("$.data.eligibleCount").value(80))
                .andExpect(jsonPath("$.data.excludedCount").value(20))
                .andExpect(jsonPath("$.data.openedCount").value(40))
                .andExpect(jsonPath("$.data.clickedCount").value(16))
                .andExpect(jsonPath("$.data.repliedCount").value(8))
                .andExpect(jsonPath("$.data.convertedCount").value(4))
                // FR-104
                .andExpect(jsonPath("$.data.openRate").value(0.5))
                // FR-105
                .andExpect(jsonPath("$.data.clickRate").value(0.2))
                // FR-106
                .andExpect(jsonPath("$.data.conversionRate").value(0.05))
                .andExpect(jsonPath("$.data.estimatedCost").value(200.0))
                .andExpect(jsonPath("$.data.estimatedRevenue").value(300.0))
                // FR-107
                .andExpect(jsonPath("$.data.estimatedRoi").value(0.5))
                .andExpect(jsonPath("$.data.recentCampaignMetrics", hasSize(1)))
                .andExpect(
                        jsonPath("$.data.recentCampaignMetrics[0].campaignId")
                                .value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.recentCampaignMetrics[0].sentCount").value(80))
                .andExpect(jsonPath("$.data.recentCampaignMetrics[0].openRate").value(0.5));

        verify(analyticsService).getDashboard();
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanAccessDashboardEndpoint(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(analyticsService.getDashboard()).thenReturn(DashboardView.empty());

        mockMvc.perform(get(DASHBOARD_PATH).header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.campaignTotal").value(0))
                .andExpect(jsonPath("$.data.activeCampaigns").value(0))
                .andExpect(jsonPath("$.data.audienceSize").value(0))
                .andExpect(jsonPath("$.data.messagesSent").value(0));

        verify(analyticsService).getDashboard();
    }

    @Test
    void emptyDashboardReturnsZeroedKpis() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(analyticsService.getDashboard()).thenReturn(DashboardView.empty());

        mockMvc.perform(get(DASHBOARD_PATH).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignTotal").value(0))
                .andExpect(jsonPath("$.data.activeCampaigns").value(0))
                .andExpect(jsonPath("$.data.audienceSize").value(0))
                .andExpect(jsonPath("$.data.messagesSent").value(0))
                .andExpect(jsonPath("$.data.openRate").value(0))
                .andExpect(jsonPath("$.data.clickRate").value(0))
                .andExpect(jsonPath("$.data.conversionRate").value(0))
                .andExpect(jsonPath("$.data.estimatedCost").value(nullValue()))
                .andExpect(jsonPath("$.data.estimatedRevenue").value(nullValue()))
                .andExpect(jsonPath("$.data.estimatedRoi").value(nullValue()))
                .andExpect(jsonPath("$.data.recentCampaignMetrics", hasSize(0)));
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(DASHBOARD_PATH)).andExpect(status().isUnauthorized());
        verify(analyticsService, never()).getDashboard();
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotAccessDashboardEndpoint(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(get(DASHBOARD_PATH).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Analytics dashboard loaded"))));

        verify(analyticsService, never()).getDashboard();
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
                OWNER_ID, "dashboard.user@bayer-westphalian.test", List.of(role));
    }

    private static DashboardView sampleDashboard() {
        CampaignMetricsView recent =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Dashboard campaign",
                        CampaignStatus.ACTIVE,
                        100,
                        80,
                        20,
                        80,
                        40,
                        16,
                        8,
                        4,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("200.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("0.50"),
                        Instant.parse("2026-07-11T10:00:00Z"));
        return new DashboardView(
                3L,
                1L,
                100L,
                80L,
                80L,
                20L,
                40L,
                16L,
                8L,
                4L,
                new BigDecimal("0.5000"),
                new BigDecimal("0.2000"),
                new BigDecimal("0.0500"),
                new BigDecimal("200.00"),
                new BigDecimal("300.00"),
                new BigDecimal("0.50"),
                List.of(recent));
    }
}
