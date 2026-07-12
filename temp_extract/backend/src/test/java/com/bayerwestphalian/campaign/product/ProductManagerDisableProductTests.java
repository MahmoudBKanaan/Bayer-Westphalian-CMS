package com.bayerwestphalian.campaign.product;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProductManagerDisableProductTests {

    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void productManagerCanDisableProduct() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productService.deactivateProduct(PRODUCT_ID)).thenReturn(disabledProductView());

        mockMvc.perform(
                        patch("/api/products/{id}/disable", PRODUCT_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product disabled"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.deleted").value(false));

        verify(productService).deactivateProduct(PRODUCT_ID);
    }

    @Test
    void biAnalystCannotDisableProduct() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

        mockMvc.perform(
                        patch("/api/products/{id}/disable", PRODUCT_ID)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Product disabled"))));
    }

    private static JwtTokenClaims productManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009904"),
                "product.manager@bayer-westphalian.test",
                List.of(SystemRoleName.PRODUCT_MANAGER));
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                "analyst@bayer-westphalian.test",
                List.of(role));
    }

    private static ProductView disabledProductView() {
        return new ProductView(
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                new BigDecimal("129.99"),
                24,
                "EXPIRES_AT_TERM_END",
                false,
                false,
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-04T12:00:00Z"),
                null);
    }
}