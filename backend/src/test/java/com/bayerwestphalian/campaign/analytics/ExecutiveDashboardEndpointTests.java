package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.math.BigDecimal;
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
 * KB item 434: Implement executive aggregate dashboard endpoint.
 *
 * <p>{@code GET /api/analytics/executive} returns platform-level aggregated KPIs and product
 * performance summaries (COMP-010) for authorized analytics roles.
 *
 * <p>Acceptance coverage for COMP-010 (executive report uses aggregated data) is also formalized
 * under KB item 457 in {@link ExecutiveReportUsesAggregatedDataTests}.
 */
@WebMvcTest(controllers = AnalyticsController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class ExecutiveDashboardEndpointTests {

    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000434");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000434");

    private static final String EXECUTIVE_PATH = "/api/analytics/executive";

    @Autowired private MockMvc mockMvc;

    @MockBean private AnalyticsService analyticsService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void executiveDashboardEndpointIsMappedUnderAnalyticsApi() throws Exception {
        assertThat(AnalyticsController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(AnalyticsController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/analytics");

        Method method = AnalyticsController.class.getMethod("getExecutiveDashboard");
        assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly("/executive");
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("EXECUTIVE_VIEWER")
                .contains("MARKETING_ANALYST");
    }

    @Test
    void executiveViewerReceivesFullAggregatePayload() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));
        when(analyticsService.getExecutiveDashboard()).thenReturn(sampleExecutiveDashboard());

        mockMvc.perform(
                        get(EXECUTIVE_PATH)
                                .header("Authorization", "Bearer exec-token")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Executive dashboard loaded"))
                // Campaign inventory aggregates (item 434 / COMP-010)
                .andExpect(jsonPath("$.data.totalCampaigns").value(5))
                .andExpect(jsonPath("$.data.activeCampaigns").value(2))
                .andExpect(jsonPath("$.data.completedCampaigns").value(1))
                // Audience funnel
                .andExpect(jsonPath("$.data.totalAudience").value(500))
                .andExpect(jsonPath("$.data.totalEligible").value(400))
                .andExpect(jsonPath("$.data.totalExcluded").value(100))
                .andExpect(jsonPath("$.data.totalSent").value(300))
                // Engagement totals
                .andExpect(jsonPath("$.data.totalOpened").value(150))
                .andExpect(jsonPath("$.data.totalClicked").value(60))
                .andExpect(jsonPath("$.data.totalReplied").value(30))
                .andExpect(jsonPath("$.data.totalConverted").value(15))
                // Rates from aggregates (FR-104–FR-106 style)
                .andExpect(jsonPath("$.data.overallOpenRate").value(0.5))
                .andExpect(jsonPath("$.data.overallClickRate").value(0.2))
                .andExpect(jsonPath("$.data.overallConversionRate").value(0.05))
                // Financial aggregates (FR-107 style)
                .andExpect(jsonPath("$.data.totalEstimatedCost").value(1000.0))
                .andExpect(jsonPath("$.data.totalEstimatedRevenue").value(1400.0))
                .andExpect(jsonPath("$.data.overallEstimatedRoi").value(0.4))
                // Embedded product performance summary (item 433 aggregation)
                .andExpect(jsonPath("$.data.productPerformance", hasSize(1)))
                .andExpect(jsonPath("$.data.productPerformance[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.productPerformance[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.productPerformance[0].productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data.productPerformance[0].campaignCount").value(2))
                .andExpect(jsonPath("$.data.productPerformance[0].openRate").value(0.5))
                .andExpect(jsonPath("$.data.productPerformance[0].estimatedRoi").value(0.6));

        verify(analyticsService).getExecutiveDashboard();
    }

    @Test
    void emptyExecutiveDashboardReturnsZeroedAggregates() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.ADMIN));
        when(analyticsService.getExecutiveDashboard()).thenReturn(ExecutiveDashboardView.empty());

        mockMvc.perform(get(EXECUTIVE_PATH).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Executive dashboard loaded"))
                .andExpect(jsonPath("$.data.totalCampaigns").value(0))
                .andExpect(jsonPath("$.data.activeCampaigns").value(0))
                .andExpect(jsonPath("$.data.completedCampaigns").value(0))
                .andExpect(jsonPath("$.data.totalAudience").value(0))
                .andExpect(jsonPath("$.data.totalEligible").value(0))
                .andExpect(jsonPath("$.data.totalExcluded").value(0))
                .andExpect(jsonPath("$.data.totalSent").value(0))
                .andExpect(jsonPath("$.data.totalOpened").value(0))
                .andExpect(jsonPath("$.data.totalClicked").value(0))
                .andExpect(jsonPath("$.data.totalReplied").value(0))
                .andExpect(jsonPath("$.data.totalConverted").value(0))
                .andExpect(jsonPath("$.data.overallOpenRate").value(0))
                .andExpect(jsonPath("$.data.overallClickRate").value(0))
                .andExpect(jsonPath("$.data.overallConversionRate").value(0))
                .andExpect(jsonPath("$.data.totalEstimatedCost").value(nullValue()))
                .andExpect(jsonPath("$.data.totalEstimatedRevenue").value(nullValue()))
                .andExpect(jsonPath("$.data.overallEstimatedRoi").value(nullValue()))
                .andExpect(jsonPath("$.data.productPerformance", hasSize(0)));

        verify(analyticsService).getExecutiveDashboard();
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanAccessExecutiveDashboardEndpoint(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(analyticsService.getExecutiveDashboard()).thenReturn(ExecutiveDashboardView.empty());

        mockMvc.perform(get(EXECUTIVE_PATH).header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCampaigns").value(0))
                .andExpect(jsonPath("$.data.productPerformance", hasSize(0)));

        verify(analyticsService).getExecutiveDashboard();
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(EXECUTIVE_PATH)).andExpect(status().isUnauthorized());
        verify(analyticsService, never()).getExecutiveDashboard();
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotAccessExecutiveDashboardEndpoint(SystemRoleName role)
            throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(get(EXECUTIVE_PATH).header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Executive dashboard loaded"))));

        verify(analyticsService, never()).getExecutiveDashboard();
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
                OWNER_ID, "executive.user@bayer-westphalian.test", List.of(role));
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
}
