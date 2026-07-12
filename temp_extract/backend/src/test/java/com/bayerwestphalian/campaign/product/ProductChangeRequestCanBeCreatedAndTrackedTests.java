package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductChangeRequestController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ProductChangeRequestCanBeCreatedAndTrackedTests {

    private static final UUID REQUEST_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID APPROVED_REQUEST_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-03T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-04T12:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductChangeRequestService productChangeRequestService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void productManagerCanCreateProductChangeRequest() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productChangeRequestService.createRequest(any(CreateProductChangeRequestCommand.class)))
                .thenReturn(openRequestView());

        mockMvc.perform(
                        post("/api/product-change-requests")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request created"))
                .andExpect(jsonPath("$.data.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.requestType").value("PRICE_CHANGE"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.description")
                        .value("Adjust monthly price for the new tariff."));

        ArgumentCaptor<CreateProductChangeRequestCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateProductChangeRequestCommand.class);
        verify(productChangeRequestService).createRequest(commandCaptor.capture());
        assertThat(commandCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(commandCaptor.getValue().requestType()).isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(commandCaptor.getValue().description())
                .isEqualTo("Adjust monthly price for the new tariff.");
    }

    @Test
    void adminCanCreateProductChangeRequest() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(productChangeRequestService.createRequest(any(CreateProductChangeRequestCommand.class)))
                .thenReturn(openRequestView());

        mockMvc.perform(
                        post("/api/product-change-requests")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product change request created"))
                .andExpect(jsonPath("$.data.requestedByFullName").value("Product Manager"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(productChangeRequestService).createRequest(any(CreateProductChangeRequestCommand.class));
    }

    @Test
    void biAnalystCanListProductChangeRequestsForTracking() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(productChangeRequestService.listRequests(any(ProductChangeRequestSearchCriteria.class)))
                .thenReturn(List.of(openRequestView(), approvedRequestView()));

        mockMvc.perform(
                        get("/api/product-change-requests")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .param("productId", PRODUCT_ID.toString())
                                .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change requests loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[1].status").value("APPROVED"));

        verify(productChangeRequestService).listRequests(any(ProductChangeRequestSearchCriteria.class));
    }

    @Test
    void biAnalystCannotCreateProductChangeRequest() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());

        mockMvc.perform(
                        post("/api/product-change-requests")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Product change request created"))));
    }

    @Test
    void campaignManagerCannotCreateProductChangeRequest() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        post("/api/product-change-requests")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Product change request created"))));
    }

    @Test
    void savedProductChangeRequestIsReturnedWhenListingRequests() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productChangeRequestService.listRequests(any(ProductChangeRequestSearchCriteria.class)))
                .thenReturn(List.of(openRequestView()));

        mockMvc.perform(
                        get("/api/product-change-requests")
                                .header("Authorization", "Bearer product-manager-token")
                                .param("productId", PRODUCT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].requestType").value("PRICE_CHANGE"))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].description")
                        .value("Adjust monthly price for the new tariff."));
    }

    @Test
    void productManagerCanApproveRequestAndTrackApprovedStatus() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productChangeRequestService.approveRequest(REQUEST_ID)).thenReturn(approvedRequestView());

        mockMvc.perform(
                        patch("/api/product-change-requests/{id}/approve", REQUEST_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request approved"))
                .andExpect(jsonPath("$.data.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(productChangeRequestService).approveRequest(REQUEST_ID);
    }

    @Test
    void productManagerCanMarkApprovedRequestImplementedAndTrackStatus() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productChangeRequestService.markImplemented(APPROVED_REQUEST_ID))
                .thenReturn(implementedRequestView());

        mockMvc.perform(
                        patch("/api/product-change-requests/{id}/mark-implemented", APPROVED_REQUEST_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request marked implemented"))
                .andExpect(jsonPath("$.data.id").value(APPROVED_REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("IMPLEMENTED"));

        verify(productChangeRequestService).markImplemented(APPROVED_REQUEST_ID);
    }

    @Test
    void productManagerCanRejectRequestAndTrackRejectedStatus() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());
        when(productChangeRequestService.rejectRequest(REQUEST_ID)).thenReturn(rejectedRequestView());

        mockMvc.perform(
                        patch("/api/product-change-requests/{id}/reject", REQUEST_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request rejected"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(productChangeRequestService).rejectRequest(REQUEST_ID);
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

    private static JwtTokenClaims biAnalystClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                "bi.analyst@bayer-westphalian.test",
                List.of(SystemRoleName.BI_ANALYST));
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static String createRequestPayload() {
        return """
                {
                  "productId": "%s",
                  "requestType": "PRICE_CHANGE",
                  "description": "Adjust monthly price for the new tariff."
                }
                """
                .formatted(PRODUCT_ID);
    }

    private static ProductChangeRequestView openRequestView() {
        return new ProductChangeRequestView(
                REQUEST_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                USER_ID,
                "Product Manager",
                ProductChangeType.PRICE_CHANGE,
                "Adjust monthly price for the new tariff.",
                ProductChangeStatus.OPEN,
                CREATED_AT,
                UPDATED_AT);
    }

    private static ProductChangeRequestView approvedRequestView() {
        return new ProductChangeRequestView(
                REQUEST_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                USER_ID,
                "Product Manager",
                ProductChangeType.DURATION_CHANGE,
                "Extend standard contract duration to 24 months.",
                ProductChangeStatus.APPROVED,
                CREATED_AT,
                UPDATED_AT);
    }

    private static ProductChangeRequestView rejectedRequestView() {
        return new ProductChangeRequestView(
                REQUEST_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                USER_ID,
                "Product Manager",
                ProductChangeType.PRICE_CHANGE,
                "Adjust monthly price for the new tariff.",
                ProductChangeStatus.REJECTED,
                CREATED_AT,
                UPDATED_AT);
    }

    private static ProductChangeRequestView implementedRequestView() {
        return new ProductChangeRequestView(
                APPROVED_REQUEST_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                USER_ID,
                "Product Manager",
                ProductChangeType.DURATION_CHANGE,
                "Extend standard contract duration to 24 months.",
                ProductChangeStatus.IMPLEMENTED,
                CREATED_AT,
                UPDATED_AT);
    }
}