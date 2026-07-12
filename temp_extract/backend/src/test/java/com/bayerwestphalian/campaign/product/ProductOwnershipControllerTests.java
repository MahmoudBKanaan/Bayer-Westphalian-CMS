package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class ProductOwnershipControllerTests {

    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000201");

    @Mock private ProductOwnershipService productOwnershipService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                ControllerTestSupport.standaloneController(
                        new ProductOwnershipController(productOwnershipService),
                        new GlobalExceptionHandler());
    }

    @Test
    void exposesProductOwnershipApiRoute() {
        assertThat(ProductOwnershipController.class.isAnnotationPresent(RestController.class))
                .isTrue();
        assertThat(ProductOwnershipController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/product-ownerships");
    }

    @Test
    void listsCustomerProductOwnershipsForProfile() throws Exception {
        when(productOwnershipService.listCustomerProducts(CUSTOMER_ID))
                .thenReturn(List.of(ownershipView(), secondOwnershipView()));

        mockMvc.perform(
                        get("/api/product-ownerships").param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownerships loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(OWNERSHIP_ID.toString()))
                .andExpect(jsonPath("$.data[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].policyNumber").value("POL-1000"))
                .andExpect(jsonPath("$.data[0].expirationDate").value("2027-01-15"))
                .andExpect(jsonPath("$.data[1].productName").value("Home Protection"))
                .andExpect(jsonPath("$.data[1].policyNumber").value("POL-2000"))
                .andExpect(jsonPath("$.data[1].expirationDate").value("2027-02-01"));

        verify(productOwnershipService).listCustomerProducts(CUSTOMER_ID);
    }

    @Test
    void returnsEmptyProductOwnershipListForCustomerProfile() throws Exception {
        when(productOwnershipService.listCustomerProducts(CUSTOMER_ID)).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/product-ownerships").param("customerId", CUSTOMER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownerships loaded"))
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(productOwnershipService).listCustomerProducts(CUSTOMER_ID);
    }

    @Test
    void rejectsCustomerProductOwnershipListWithoutCustomerId() throws Exception {
        mockMvc.perform(get("/api/product-ownerships"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void productCanBeAssignedToCustomer() throws Exception {
        when(productOwnershipService.assignProduct(any(CreateProductOwnershipCommand.class)))
                .thenReturn(ownershipView());

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "productId": "%s",
                                          "startDate": "2026-01-15",
                                          "expirationDate": "2027-01-15",
                                          "policyNumber": "POL-1000"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, PRODUCT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownership assigned"))
                .andExpect(jsonPath("$.data.id").value(OWNERSHIP_ID.toString()))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.customerFullName").value("Ada Owner"))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data.policyNumber").value("POL-1000"))
                .andExpect(jsonPath("$.data.startDate").value("2026-01-15"))
                .andExpect(jsonPath("$.data.expirationDate").value("2027-01-15"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.active").value(true));

        ArgumentCaptor<CreateProductOwnershipCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateProductOwnershipCommand.class);
        verify(productOwnershipService).assignProduct(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(commandCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(commandCaptor.getValue().startDate()).isEqualTo(LocalDate.parse("2026-01-15"));
        assertThat(commandCaptor.getValue().expirationDate())
                .isEqualTo(LocalDate.parse("2027-01-15"));
        assertThat(commandCaptor.getValue().policyNumber()).isEqualTo("POL-1000");
    }

    @Test
    void rejectsInvalidAssignProductOwnershipRequest() throws Exception {
        mockMvc.perform(
                        post("/api/product-ownerships")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/product-ownerships"));
    }

    @Test
    void updatesProductOwnershipExpirationAndPolicyNumber() throws Exception {
        ProductOwnershipView updatedOwnership =
                new ProductOwnershipView(
                        OWNERSHIP_ID,
                        CUSTOMER_ID,
                        "Ada Owner",
                        PRODUCT_ID,
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        "POL-1000-UPDATED",
                        LocalDate.parse("2026-01-15"),
                        LocalDate.parse("2028-01-15"),
                        OwnershipStatus.ACTIVE,
                        true,
                        Instant.parse("2026-07-05T12:00:00Z"));
        when(productOwnershipService.updateOwnership(
                        any(UUID.class), any(UpdateProductOwnershipCommand.class)))
                .thenReturn(updatedOwnership);

        mockMvc.perform(
                        put("/api/product-ownerships/{id}", OWNERSHIP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "expirationDate": "2028-01-15",
                                          "policyNumber": "POL-1000-UPDATED"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product ownership updated"))
                .andExpect(jsonPath("$.data.id").value(OWNERSHIP_ID.toString()))
                .andExpect(jsonPath("$.data.policyNumber").value("POL-1000-UPDATED"))
                .andExpect(jsonPath("$.data.expirationDate").value("2028-01-15"));

        ArgumentCaptor<UpdateProductOwnershipCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateProductOwnershipCommand.class);
        verify(productOwnershipService).updateOwnership(any(UUID.class), commandCaptor.capture());
        assertThat(commandCaptor.getValue().expirationDate())
                .isEqualTo(LocalDate.parse("2028-01-15"));
        assertThat(commandCaptor.getValue().policyNumber()).isEqualTo("POL-1000-UPDATED");
    }

    @Test
    void rejectsInvalidUpdateProductOwnershipRequest() throws Exception {
        mockMvc.perform(
                        put("/api/product-ownerships/{id}", OWNERSHIP_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "policyNumber": "%s"
                                        }
                                        """
                                                .formatted("P".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/product-ownerships/" + OWNERSHIP_ID));
    }

    @Test
    void mapsMissingCustomerToNotFoundResponse() throws Exception {
        when(productOwnershipService.assignProduct(any(CreateProductOwnershipCommand.class)))
                .thenThrow(new ResourceNotFoundException("Customer", CUSTOMER_ID));

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "productId": "%s",
                                          "startDate": "2026-01-15"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, PRODUCT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/product-ownerships"));
    }

    @Test
    void mapsInactiveProductToValidationResponse() throws Exception {
        when(productOwnershipService.assignProduct(any(CreateProductOwnershipCommand.class)))
                .thenThrow(
                        new ValidationException(
                                "Product ownership validation failed",
                                java.util.List.of("productId: product must be active")));

        mockMvc.perform(
                        post("/api/product-ownerships")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "productId": "%s",
                                          "startDate": "2026-01-15"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, PRODUCT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/product-ownerships"));
    }

    private static ProductOwnershipView ownershipView() {
        return new ProductOwnershipView(
                OWNERSHIP_ID,
                CUSTOMER_ID,
                "Ada Owner",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "POL-1000",
                LocalDate.parse("2026-01-15"),
                LocalDate.parse("2027-01-15"),
                OwnershipStatus.ACTIVE,
                true,
                Instant.parse("2026-07-03T12:00:00Z"));
    }

    private static ProductOwnershipView secondOwnershipView() {
        return new ProductOwnershipView(
                UUID.fromString("41000000-0000-0000-0000-000000000002"),
                CUSTOMER_ID,
                "Ada Owner",
                UUID.fromString("41000000-0000-0000-0000-000000000202"),
                "Home Protection",
                ProductType.HOMEOWNER_INSURANCE,
                "POL-2000",
                LocalDate.parse("2026-02-01"),
                LocalDate.parse("2027-02-01"),
                OwnershipStatus.ACTIVE,
                true,
                Instant.parse("2026-07-04T12:00:00Z"));
    }

}
