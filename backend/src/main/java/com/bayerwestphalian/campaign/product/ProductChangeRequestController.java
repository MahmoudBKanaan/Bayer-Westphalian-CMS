package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-change-requests")
public class ProductChangeRequestController {

    private final ProductChangeRequestService productChangeRequestService;

    public ProductChangeRequestController(ProductChangeRequestService productChangeRequestService) {
        this.productChangeRequestService = productChangeRequestService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductChangeRequestView>>> listRequests(
            @Valid @ModelAttribute ProductChangeRequestSearchRequest searchRequest) {
        List<ProductChangeRequestView> requests =
                productChangeRequestService.listRequests(searchRequest.toCriteria());

        return ResponseEntity.ok(ApiResponse.success("Product change requests loaded", requests));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductChangeRequestView>> createRequest(
            @Valid @RequestBody CreateProductChangeRequestRequest request) {
        ProductChangeRequestView changeRequest =
                productChangeRequestService.createRequest(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product change request created", changeRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductChangeRequestView>> updateRequest(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductChangeRequestRequest request) {
        ProductChangeRequestView changeRequest =
                productChangeRequestService.updateRequest(id, request.toCommand());

        return ResponseEntity.ok(
                ApiResponse.success("Product change request updated", changeRequest));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ProductChangeRequestView>> approveRequest(
            @PathVariable UUID id) {
        ProductChangeRequestView changeRequest = productChangeRequestService.approveRequest(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product change request approved", changeRequest));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ProductChangeRequestView>> rejectRequest(
            @PathVariable UUID id) {
        ProductChangeRequestView changeRequest = productChangeRequestService.rejectRequest(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product change request rejected", changeRequest));
    }

    @PatchMapping("/{id}/mark-implemented")
    public ResponseEntity<ApiResponse<ProductChangeRequestView>> markImplemented(
            @PathVariable UUID id) {
        ProductChangeRequestView changeRequest = productChangeRequestService.markImplemented(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product change request marked implemented", changeRequest));
    }
}
