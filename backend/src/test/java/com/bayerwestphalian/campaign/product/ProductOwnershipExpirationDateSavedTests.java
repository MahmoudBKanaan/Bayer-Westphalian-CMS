package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductOwnershipController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProductOwnershipExpirationDateSavedTests {

    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final LocalDate EXPIRATION_DATE = LocalDate.parse("2027-06-30");

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductOwnershipService productOwnershipService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void productOwnershipExpirationDateIsSavedOnAssign() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productOwnershipService.assignProduct(any(CreateProductOwnershipCommand.class)))
                .thenReturn(ownershipView(EXPIRATION_DATE));

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(assignProductPayload(EXPIRATION_DATE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownership assigned"))
                .andExpect(jsonPath("$.data.expirationDate").value(EXPIRATION_DATE.toString()))
                .andExpect(jsonPath("$.data.startDate").value("2026-06-01"));

        ArgumentCaptor<CreateProductOwnershipCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateProductOwnershipCommand.class);
        verify(productOwnershipService).assignProduct(commandCaptor.capture());
        assertThat(commandCaptor.getValue().expirationDate()).isEqualTo(EXPIRATION_DATE);
    }

    @Test
    void savedProductOwnershipExpirationDateIsReturnedWhenListingCustomerOwnerships()
            throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(productOwnershipService.listCustomerProducts(CUSTOMER_ID))
                .thenReturn(
                        List.of(
                                ownershipView(LocalDate.parse("2027-01-15")),
                                secondOwnershipView(LocalDate.parse("2027-02-01"))));

        mockMvc.perform(
                        get("/api/product-ownerships")
                                .header("Authorization", "Bearer admin-token")
                                .param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownerships loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].expirationDate").value("2027-01-15"))
                .andExpect(jsonPath("$.data[1].expirationDate").value("2027-02-01"));

        verify(productOwnershipService).listCustomerProducts(CUSTOMER_ID);
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

    private static String assignProductPayload(LocalDate expirationDate) {
        return """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "startDate": "2026-06-01",
                  "expirationDate": "%s",
                  "policyNumber": "POL-5000"
                }
                """
                .formatted(CUSTOMER_ID, PRODUCT_ID, expirationDate);
    }

    private static ProductOwnershipView ownershipView(LocalDate expirationDate) {
        return new ProductOwnershipView(
                OWNERSHIP_ID,
                CUSTOMER_ID,
                "Ada Policyholder",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "POL-5000",
                LocalDate.parse("2026-06-01"),
                expirationDate,
                OwnershipStatus.ACTIVE,
                true,
                Instant.parse("2026-07-05T12:00:00Z"));
    }

    private static ProductOwnershipView secondOwnershipView(LocalDate expirationDate) {
        return new ProductOwnershipView(
                UUID.fromString("41000000-0000-0000-0000-000000000002"),
                CUSTOMER_ID,
                "Ada Policyholder",
                UUID.fromString("41000000-0000-0000-0000-000000000202"),
                "Home Protection",
                ProductType.HOMEOWNER_INSURANCE,
                "POL-2000",
                LocalDate.parse("2026-02-01"),
                expirationDate,
                OwnershipStatus.ACTIVE,
                true,
                Instant.parse("2026-07-04T12:00:00Z"));
    }
}
