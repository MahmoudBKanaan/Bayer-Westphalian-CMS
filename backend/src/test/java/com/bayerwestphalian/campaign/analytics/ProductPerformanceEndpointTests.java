package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
 * KB item 433: Implement product performance endpoint.
 *
 * <p>{@code GET /api/analytics/products/performance} returns per-product aggregated campaign
 * metrics for authorized analytics roles.
 */
@WebMvcTest(controllers = AnalyticsController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class ProductPerformanceEndpointTests {

    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000433");
    private static final UUID PRODUCT_B_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000434");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000433");

    private static final String PRODUCT_PERFORMANCE_PATH = "/api/analytics/products/performance";

    @Autowired private MockMvc mockMvc;

    @MockBean private AnalyticsService analyticsService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void productPerformanceEndpointIsMappedUnderAnalyticsApi() throws Exception {
        assertThat(AnalyticsController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(AnalyticsController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/analytics");

        Method method = AnalyticsController.class.getMethod("getProductPerformance");
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/products/performance");
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("ADMIN")
                .contains("EXECUTIVE_VIEWER")
                .contains("MARKETING_ANALYST");
    }

    @Test
    void biAnalystReceivesProductPerformanceRows() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(analyticsService.getProductPerformance())
                .thenReturn(List.of(sampleProductPerformance(), sampleSecondaryProduct()));

        mockMvc.perform(
                        get(PRODUCT_PERFORMANCE_PATH)
                                .header("Authorization", "Bearer bi-token")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product performance loaded"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data[0].campaignCount").value(2))
                .andExpect(jsonPath("$.data[0].audienceSize").value(200))
                .andExpect(jsonPath("$.data[0].eligibleCount").value(150))
                .andExpect(jsonPath("$.data[0].sentCount").value(100))
                .andExpect(jsonPath("$.data[0].openedCount").value(50))
                .andExpect(jsonPath("$.data[0].clickedCount").value(20))
                .andExpect(jsonPath("$.data[0].convertedCount").value(10))
                .andExpect(jsonPath("$.data[0].openRate").value(0.5))
                .andExpect(jsonPath("$.data[0].clickRate").value(0.2))
                .andExpect(jsonPath("$.data[0].conversionRate").value(0.1))
                .andExpect(jsonPath("$.data[0].estimatedCost").value(500.0))
                .andExpect(jsonPath("$.data[0].estimatedRevenue").value(800.0))
                .andExpect(jsonPath("$.data[0].estimatedRoi").value(0.6))
                .andExpect(jsonPath("$.data[1].productId").value(PRODUCT_B_ID.toString()))
                .andExpect(jsonPath("$.data[1].productName").value("Auto Cover"));

        verify(analyticsService).getProductPerformance();
    }

    @Test
    void emptyProductPerformanceReturnsEmptyArray() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));
        when(analyticsService.getProductPerformance()).thenReturn(List.of());

        mockMvc.perform(
                        get(PRODUCT_PERFORMANCE_PATH)
                                .header("Authorization", "Bearer exec-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(analyticsService).getProductPerformance();
    }

    @ParameterizedTest
    @MethodSource("authorizedRoles")
    void authorizedRolesCanAccessProductPerformanceEndpoint(SystemRoleName role) throws Exception {
        when(jwtService.validateToken("ok-token", JwtTokenType.ACCESS)).thenReturn(roleClaims(role));
        when(analyticsService.getProductPerformance())
                .thenReturn(List.of(sampleProductPerformance()));

        mockMvc.perform(get(PRODUCT_PERFORMANCE_PATH).header("Authorization", "Bearer ok-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"));

        verify(analyticsService).getProductPerformance();
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(PRODUCT_PERFORMANCE_PATH)).andExpect(status().isUnauthorized());
        verify(analyticsService, never()).getProductPerformance();
    }

    @ParameterizedTest
    @MethodSource("unauthorizedRoles")
    void unauthorizedRolesCannotAccessProductPerformanceEndpoint(SystemRoleName role)
            throws Exception {
        when(jwtService.validateToken("denied-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(role));

        mockMvc.perform(
                        get(PRODUCT_PERFORMANCE_PATH)
                                .header("Authorization", "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "Product performance loaded"))));

        verify(analyticsService, never()).getProductPerformance();
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
                OWNER_ID, "product-perf.user@bayer-westphalian.test", List.of(role));
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

    private static ProductPerformanceView sampleSecondaryProduct() {
        return ProductPerformanceView.of(
                PRODUCT_B_ID,
                "Auto Cover",
                ProductType.AUTO_INSURANCE,
                1L,
                50L,
                40L,
                30L,
                10L,
                4L,
                1L,
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                new BigDecimal("0.20"));
    }
}
