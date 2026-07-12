package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Method;
import java.time.Instant;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class ProductChangeRequestControllerTests {

    private static final UUID REQUEST_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("42000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-03T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-04T12:00:00Z");

    @Mock private ProductChangeRequestService productChangeRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new ProductChangeRequestController(productChangeRequestService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesProductChangeRequestApiRoute() {
        assertThat(ProductChangeRequestController.class.isAnnotationPresent(RestController.class))
                .isTrue();
        assertThat(ProductChangeRequestController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/product-change-requests");
    }

    @Test
    void exposesKbProductChangeRequestWorkflowEndpoints() throws Exception {
        Method listMethod =
                ProductChangeRequestController.class.getMethod(
                        "listRequests", ProductChangeRequestSearchRequest.class);
        Method createMethod =
                ProductChangeRequestController.class.getMethod(
                        "createRequest", CreateProductChangeRequestRequest.class);
        Method updateMethod =
                ProductChangeRequestController.class.getMethod(
                        "updateRequest", UUID.class, UpdateProductChangeRequestRequest.class);
        Method approveMethod =
                ProductChangeRequestController.class.getMethod("approveRequest", UUID.class);
        Method rejectMethod =
                ProductChangeRequestController.class.getMethod("rejectRequest", UUID.class);
        Method markImplementedMethod =
                ProductChangeRequestController.class.getMethod("markImplemented", UUID.class);

        assertThat(listMethod.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(createMethod.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(updateMethod.isAnnotationPresent(PutMapping.class)).isTrue();
        assertThat(updateMethod.getAnnotation(PutMapping.class).value()).containsExactly("/{id}");
        assertThat(approveMethod.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(approveMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/approve");
        assertThat(rejectMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/reject");
        assertThat(markImplementedMethod.getAnnotation(PatchMapping.class).value())
                .containsExactly("/{id}/mark-implemented");
    }

    @Test
    void listsProductChangeRequestsWithKbFilters() throws Exception {
        when(productChangeRequestService.listRequests(
                        any(ProductChangeRequestSearchCriteria.class)))
                .thenReturn(List.of(openRequestView(), approvedRequestView()));

        mockMvc.perform(
                        get("/api/product-change-requests")
                                .param("productId", PRODUCT_ID.toString())
                                .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change requests loaded"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data[0].productName").value("Life Protection"))
                .andExpect(jsonPath("$.data[0].requestType").value("PRICE_CHANGE"))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[1].status").value("APPROVED"));

        ArgumentCaptor<ProductChangeRequestSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ProductChangeRequestSearchCriteria.class);
        verify(productChangeRequestService).listRequests(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(ProductChangeStatus.OPEN);
    }

    @Test
    void createsProductChangeRequestFromKbRequest() throws Exception {
        when(productChangeRequestService.createRequest(
                        any(CreateProductChangeRequestCommand.class)))
                .thenReturn(openRequestView());

        mockMvc.perform(
                        post("/api/product-change-requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productId": "%s",
                                          "requestType": "PRICE_CHANGE",
                                          "description": "Adjust monthly price for the new tariff."
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request created"))
                .andExpect(jsonPath("$.data.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.productName").value("Life Protection"))
                .andExpect(jsonPath("$.data.productType").value("LIFE_INSURANCE"))
                .andExpect(jsonPath("$.data.requestedByUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.requestedByFullName").value("Product Manager"))
                .andExpect(jsonPath("$.data.requestType").value("PRICE_CHANGE"))
                .andExpect(
                        jsonPath("$.data.description")
                                .value("Adjust monthly price for the new tariff."))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        ArgumentCaptor<CreateProductChangeRequestCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateProductChangeRequestCommand.class);
        verify(productChangeRequestService).createRequest(commandCaptor.capture());
        assertThat(commandCaptor.getValue().productId()).isEqualTo(PRODUCT_ID);
        assertThat(commandCaptor.getValue().requestType())
                .isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(commandCaptor.getValue().description())
                .isEqualTo("Adjust monthly price for the new tariff.");
    }

    @Test
    void rejectsInvalidCreateProductChangeRequest() throws Exception {
        mockMvc.perform(
                        post("/api/product-change-requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/product-change-requests"));
    }

    @Test
    void updatesOpenProductChangeRequestDescription() throws Exception {
        when(productChangeRequestService.updateRequest(
                        eq(REQUEST_ID), any(UpdateProductChangeRequestCommand.class)))
                .thenReturn(updatedRequestView());

        mockMvc.perform(
                        put("/api/product-change-requests/{id}", REQUEST_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "description": "Use the updated 6-month expiration reminder policy."
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request updated"))
                .andExpect(
                        jsonPath("$.data.description")
                                .value("Use the updated 6-month expiration reminder policy."))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        ArgumentCaptor<UpdateProductChangeRequestCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateProductChangeRequestCommand.class);
        verify(productChangeRequestService).updateRequest(eq(REQUEST_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().description())
                .isEqualTo("Use the updated 6-month expiration reminder policy.");
    }

    @Test
    void rejectsInvalidUpdateProductChangeRequest() throws Exception {
        mockMvc.perform(
                        put("/api/product-change-requests/{id}", REQUEST_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/product-change-requests/" + REQUEST_ID));
    }

    @Test
    void approvesOpenProductChangeRequest() throws Exception {
        when(productChangeRequestService.approveRequest(REQUEST_ID))
                .thenReturn(approvedRequestView());

        mockMvc.perform(patch("/api/product-change-requests/{id}/approve", REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request approved"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(productChangeRequestService).approveRequest(REQUEST_ID);
    }

    @Test
    void rejectsOpenProductChangeRequest() throws Exception {
        when(productChangeRequestService.rejectRequest(REQUEST_ID))
                .thenReturn(rejectedRequestView());

        mockMvc.perform(patch("/api/product-change-requests/{id}/reject", REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request rejected"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(productChangeRequestService).rejectRequest(REQUEST_ID);
    }

    @Test
    void marksApprovedProductChangeRequestAsImplemented() throws Exception {
        when(productChangeRequestService.markImplemented(REQUEST_ID))
                .thenReturn(implementedRequestView());

        mockMvc.perform(patch("/api/product-change-requests/{id}/mark-implemented", REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product change request marked implemented"))
                .andExpect(jsonPath("$.data.status").value("IMPLEMENTED"));

        verify(productChangeRequestService).markImplemented(REQUEST_ID);
    }

    @Test
    void mapsMissingProductToNotFoundResponseOnCreate() throws Exception {
        when(productChangeRequestService.createRequest(
                        any(CreateProductChangeRequestCommand.class)))
                .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

        mockMvc.perform(
                        post("/api/product-change-requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "productId": "%s",
                                          "requestType": "STATUS_CHANGE",
                                          "description": "Deactivate legacy tariff."
                                        }
                                        """
                                                .formatted(PRODUCT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/product-change-requests"));
    }

    @Test
    void mapsInvalidWorkflowTransitionToValidationResponse() throws Exception {
        when(productChangeRequestService.approveRequest(REQUEST_ID))
                .thenThrow(
                        new ValidationException(
                                "Product change request validation failed",
                                java.util.List.of(
                                        "status: request must be OPEN before transitioning to"
                                                + " APPROVED")));

        mockMvc.perform(patch("/api/product-change-requests/{id}/approve", REQUEST_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/product-change-requests/" + REQUEST_ID + "/approve"));
    }

    @Test
    void mapsMissingRequestToNotFoundResponseOnApprove() throws Exception {
        when(productChangeRequestService.approveRequest(REQUEST_ID))
                .thenThrow(new ResourceNotFoundException("Product change request", REQUEST_ID));

        mockMvc.perform(patch("/api/product-change-requests/{id}/approve", REQUEST_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/product-change-requests/" + REQUEST_ID + "/approve"));
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

    private static ProductChangeRequestView updatedRequestView() {
        return new ProductChangeRequestView(
                REQUEST_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                USER_ID,
                "Product Manager",
                ProductChangeType.PRICE_CHANGE,
                "Use the updated 6-month expiration reminder policy.",
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
                ProductChangeType.PRICE_CHANGE,
                "Adjust monthly price for the new tariff.",
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
                REQUEST_ID,
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                USER_ID,
                "Product Manager",
                ProductChangeType.PRICE_CHANGE,
                "Adjust monthly price for the new tariff.",
                ProductChangeStatus.IMPLEMENTED,
                CREATED_AT,
                UPDATED_AT);
    }
}
