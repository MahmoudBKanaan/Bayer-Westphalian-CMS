package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProductSearchAndFilterUiTests {

    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void biAnalystCanSearchProductsWithNameTypeAndActiveFilters() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(lifeInsuranceProduct()));

        mockMvc.perform(
                        get("/api/products")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .param("term", "life")
                                .param("productType", "LIFE_INSURANCE")
                                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Products loaded"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data[0].active").value(true));

        ArgumentCaptor<ProductSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(productService).searchProducts(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("life");
        assertThat(criteriaCaptor.getValue().productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(criteriaCaptor.getValue().active()).isTrue();
    }

    @Test
    void campaignManagerCanSearchProductsWithTypeFilterOnly() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(productService.searchProducts(any(ProductSearchCriteria.class)))
                .thenReturn(List.of(investmentProduct()));

        mockMvc.perform(
                        get("/api/products")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .param("productType", "INVESTMENT_FUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Growth Fund"))
                .andExpect(jsonPath("$.data[0].productType").value("INVESTMENT_FUND"))
                .andExpect(jsonPath("$.data[0].active").value(false));

        verify(productService).searchProducts(any(ProductSearchCriteria.class));
    }

    @Test
    void systemAuditorCannotSearchProductsForCatalogUi() throws Exception {
        when(jwtService.validateToken("auditor-token", JwtTokenType.ACCESS))
                .thenReturn(systemAuditorClaims());

        mockMvc.perform(
                        get("/api/products")
                                .header("Authorization", "Bearer auditor-token")
                                .param("term", "life"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Products loaded"))));
    }

    private static JwtTokenClaims biAnalystClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                "analyst@bayer-westphalian.test",
                List.of(SystemRoleName.BI_ANALYST));
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static JwtTokenClaims systemAuditorClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009910"),
                "auditor@bayer-westphalian.test",
                List.of(SystemRoleName.SYSTEM_AUDITOR));
    }

    private static ProductView lifeInsuranceProduct() {
        return new ProductView(
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                new BigDecimal("129.99"),
                24,
                "EXPIRES_AT_TERM_END",
                true,
                false,
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-03T12:00:00Z"),
                null);
    }

    private static ProductView investmentProduct() {
        return new ProductView(
                UUID.fromString("40000000-0000-0000-0000-000000000002"),
                "Growth Fund",
                ProductType.INVESTMENT_FUND,
                "Balanced investment portfolio",
                new BigDecimal("500.00"),
                24,
                "Biennial review",
                false,
                false,
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T12:00:00Z"),
                null);
    }
}
