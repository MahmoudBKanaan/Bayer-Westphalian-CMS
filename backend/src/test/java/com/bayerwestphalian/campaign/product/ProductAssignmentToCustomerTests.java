package com.bayerwestphalian.campaign.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductOwnershipController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProductAssignmentToCustomerTests {

    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductOwnershipService productOwnershipService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void productCanBeAssignedToCustomerByProductManager() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productOwnershipService.assignProduct(any(CreateProductOwnershipCommand.class)))
                .thenReturn(ownershipView());

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownership assigned"))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.policyNumber").value("POL-3000"))
                .andExpect(jsonPath("$.data.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.expirationDate").value("2027-03-01"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(productOwnershipService).assignProduct(any(CreateProductOwnershipCommand.class));
    }

    @Test
    void productCanBeAssignedToCustomerByAdmin() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(productOwnershipService.assignProduct(any(CreateProductOwnershipCommand.class)))
                .thenReturn(ownershipView());

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignProductPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product ownership assigned"))
                .andExpect(jsonPath("$.data.customerFullName").value("Ada Policyholder"))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"));

        verify(productOwnershipService).assignProduct(any(CreateProductOwnershipCommand.class));
    }

    private static JwtTokenClaims productManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009904"),
                "product.manager@bayer-westphalian.test",
                List.of(SystemRoleName.PRODUCT_MANAGER));
    }

    private static JwtTokenClaims adminClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                "admin@bayer-westphalian.test",
                List.of(SystemRoleName.ADMIN));
    }

    private static String assignProductPayload() {
        return """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "startDate": "2026-03-01",
                  "expirationDate": "2027-03-01",
                  "policyNumber": "POL-3000"
                }
                """
                .formatted(CUSTOMER_ID, PRODUCT_ID);
    }

    private static ProductOwnershipView ownershipView() {
        return new ProductOwnershipView(
                OWNERSHIP_ID,
                CUSTOMER_ID,
                "Ada Policyholder",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "POL-3000",
                LocalDate.parse("2026-03-01"),
                LocalDate.parse("2027-03-01"),
                OwnershipStatus.ACTIVE,
                true,
                Instant.parse("2026-07-05T12:00:00Z"));
    }
}
