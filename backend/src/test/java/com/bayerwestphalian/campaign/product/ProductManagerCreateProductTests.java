package com.bayerwestphalian.campaign.product;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProductManagerCreateProductTests {

    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void productManagerCanCreateProduct() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productService.createProduct(any(CreateProductCommand.class)))
                .thenReturn(productView());

        mockMvc.perform(
                        post("/api/products")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created"))
                .andExpect(jsonPath("$.data.name").value("Investment Growth Fund"))
                .andExpect(jsonPath("$.data.productType").value("INVESTMENT_FUND"));

        verify(productService).createProduct(any(CreateProductCommand.class));
    }

    @Test
    void biAnalystCannotCreateProduct() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

        mockMvc.perform(
                        post("/api/products")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createProductPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Product created"))));
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

    private static String createProductPayload() {
        return """
                {
                  "name": "Investment Growth Fund",
                  "productType": "INVESTMENT_FUND",
                  "description": "Balanced investment portfolio for beneficiaries",
                  "price": 500.00,
                  "durationMonths": 24,
                  "expirationPolicy": "Biennial review"
                }
                """;
    }

    private static ProductView productView() {
        return new ProductView(
                PRODUCT_ID,
                "Investment Growth Fund",
                ProductType.INVESTMENT_FUND,
                "Balanced investment portfolio for beneficiaries",
                new BigDecimal("500.00"),
                24,
                "Biennial review",
                true,
                false,
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-03T12:00:00Z"),
                null);
    }
}
