package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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
import com.bayerwestphalian.campaign.product.ProductType;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KB item 416: AnalyticsController exposes dashboard, campaign analytics, product performance, and
 * executive aggregate endpoints under {@code /api/analytics}.
 *
 * <p>Item 431 dashboard-endpoint contract is covered in depth by {@link DashboardEndpointTests}.
 * Item 432 campaign analytics endpoint contract is covered by {@link
 * CampaignAnalyticsEndpointTests}. Item 433 product performance endpoint contract is covered by
 * {@link ProductPerformanceEndpointTests}. Item 434 executive aggregate dashboard endpoint contract
 * is covered by {@link ExecutiveDashboardEndpointTests}.
 */
@WebMvcTest(controllers = AnalyticsController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class AnalyticsControllerTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000416");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000416");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000416");

    @Autowired private MockMvc mockMvc;

    @MockBean private AnalyticsService analyticsService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void exposesAnalyticsApiRouteAndGetEndpoints() throws Exception {
        assertThat(AnalyticsController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(AnalyticsController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/analytics");

        assertGetMapping("getDashboard", "/dashboard");
        assertGetMapping("getProductPerformance", "/products/performance");
        assertGetMapping("getExecutiveDashboard", "/executive");

        Method campaignAnalytics =
                AnalyticsController.class.getMethod("getCampaignAnalytics", UUID.class);
        assertThat(campaignAnalytics.getAnnotation(GetMapping.class).value())
                .containsExactly("/campaigns/{campaignId}");
        assertThat(campaignAnalytics.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    @Test
    void biAnalystCanLoadDashboard() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(analyticsService.getDashboard()).thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/analytics/dashboard").header("Authorization", "Bearer bi-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Analytics dashboard loaded"))
                .andExpect(jsonPath("$.data.campaignTotal").value(3))
                .andExpect(jsonPath("$.data.activeCampaigns").value(1))
                .andExpect(jsonPath("$.data.audienceSize").value(100))
                .andExpect(jsonPath("$.data.messagesSent").value(80))
                .andExpect(jsonPath("$.data.openRate").value(0.5))
                .andExpect(jsonPath("$.data.clickRate").value(0.2))
                .andExpect(jsonPath("$.data.conversionRate").value(0.05))
                .andExpect(jsonPath("$.data.estimatedRoi").value(0.5))
                .andExpect(jsonPath("$.data.recentCampaignMetrics", hasSize(1)));

        verify(analyticsService).getDashboard();
    }

    @Test
    void campaignManagerCanLoadCampaignAnalytics() throws Exception {
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID)).thenReturn(sampleCampaignAnalytics());

        mockMvc.perform(
                        get("/api/analytics/campaigns/{campaignId}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer cm-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign analytics loaded"))
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.campaignName").value("Analytics campaign"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.metrics.sentCount").value(10))
                .andExpect(jsonPath("$.data.metrics.openRate").value(0.5));

        verify(analyticsService).getCampaignAnalytics(CAMPAIGN_ID);
    }

    @Test
    void executiveViewerCanLoadProductPerformance() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));
        when(analyticsService.getProductPerformance()).thenReturn(List.of(sampleProductPerformance()));

        mockMvc.perform(
                        get("/api/analytics/products/performance")
                                .header("Authorization", "Bearer exec-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product performance loaded"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data[0].campaignCount").value(2))
                .andExpect(jsonPath("$.data[0].openRate").value(0.5));

        verify(analyticsService).getProductPerformance();
    }

    @Test
    void adminCanLoadExecutiveDashboard() throws Exception {
        // KB item 434: smoke coverage; full contract in ExecutiveDashboardEndpointTests.
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(analyticsService.getExecutiveDashboard()).thenReturn(sampleExecutiveDashboard());

        mockMvc.perform(
                        get("/api/analytics/executive").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Executive dashboard loaded"))
                .andExpect(jsonPath("$.data.totalCampaigns").value(5))
                .andExpect(jsonPath("$.data.activeCampaigns").value(2))
                .andExpect(jsonPath("$.data.completedCampaigns").value(1))
                .andExpect(jsonPath("$.data.totalSent").value(300))
                .andExpect(jsonPath("$.data.overallOpenRate").value(0.5))
                .andExpect(jsonPath("$.data.productPerformance", hasSize(1)));

        verify(analyticsService).getExecutiveDashboard();
    }

    @Test
    void marketingAnalystCanLoadDashboard() throws Exception {
        when(jwtService.validateToken("ma-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.MARKETING_ANALYST));
        when(analyticsService.getDashboard()).thenReturn(DashboardView.empty());

        mockMvc.perform(get("/api/analytics/dashboard").header("Authorization", "Bearer ma-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.campaignTotal").value(0));

        verify(analyticsService).getDashboard();
    }

    @Test
    void campaignAnalyticsReturnsNotFoundWhenServiceThrows() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(
                        get("/api/analytics/campaigns/{campaignId}", CAMPAIGN_ID)
                                .header("Authorization", "Bearer bi-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/analytics/dashboard")).andExpect(status().isUnauthorized());
        verify(analyticsService, never()).getDashboard();
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotAccessAnalytics(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        get("/api/analytics/dashboard")
                                .header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Analytics dashboard loaded"))));

        verify(analyticsService, never()).getDashboard();
    }

    static Stream<SystemRoleName> unauthorizedRoles() {
        return Stream.of(
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private static void assertGetMapping(String methodName, String path) throws Exception {
        Method method = AnalyticsController.class.getMethod(methodName);
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(path);
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                OWNER_ID, "analytics.user@bayer-westphalian.test", List.of(role));
    }

    private static DashboardView sampleDashboard() {
        CampaignMetricsView recent =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Analytics campaign",
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

    private static CampaignAnalyticsView sampleCampaignAnalytics() {
        CampaignMetricsView metrics =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Analytics campaign",
                        CampaignStatus.ACTIVE,
                        10,
                        10,
                        0,
                        10,
                        5,
                        2,
                        1,
                        1,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.1000"),
                        null,
                        null,
                        null,
                        Instant.parse("2026-07-11T10:00:00Z"));
        return new CampaignAnalyticsView(
                CAMPAIGN_ID,
                "Analytics campaign",
                "Raise awareness",
                CampaignStatus.ACTIVE,
                CampaignChannel.EMAIL,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                OWNER_ID,
                "Analytics Owner",
                metrics,
                Instant.parse("2026-07-11T11:00:00Z"));
    }

    private static ProductPerformanceView sampleProductPerformance() {
        return ProductPerformanceView.of(
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                2L,
                200L,
                150L,
                100L,
                50L,
                20L,
                10L,
                new BigDecimal("500.00"),
                new BigDecimal("800.00"),
                new BigDecimal("0.60"));
    }

    private static ExecutiveDashboardView sampleExecutiveDashboard() {
        return new ExecutiveDashboardView(
                5L,
                2L,
                1L,
                500L,
                400L,
                100L,
                300L,
                150L,
                60L,
                30L,
                15L,
                new BigDecimal("0.5000"),
                new BigDecimal("0.2000"),
                new BigDecimal("0.0500"),
                new BigDecimal("1000.00"),
                new BigDecimal("1400.00"),
                new BigDecimal("0.40"),
                List.of(sampleProductPerformance()));
    }
}
