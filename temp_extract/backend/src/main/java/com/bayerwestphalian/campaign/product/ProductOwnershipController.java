package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-ownerships")
public class ProductOwnershipController {

    private final ProductOwnershipService productOwnershipService;

    public ProductOwnershipController(ProductOwnershipService productOwnershipService) {
        this.productOwnershipService = productOwnershipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductOwnershipView>>> listCustomerProductOwnerships(
            @RequestParam UUID customerId) {
        List<ProductOwnershipView> ownerships =
                productOwnershipService.listCustomerProducts(customerId);

        return ResponseEntity.ok(ApiResponse.success("Product ownerships loaded", ownerships));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductOwnershipView>> assignProduct(
            @Valid @RequestBody CreateProductOwnershipRequest request) {
        ProductOwnershipView ownership =
                productOwnershipService.assignProduct(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product ownership assigned", ownership));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductOwnershipView>> updateOwnership(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductOwnershipRequest request) {
        ProductOwnershipView ownership =
                productOwnershipService.updateOwnership(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Product ownership updated", ownership));
    }
}
