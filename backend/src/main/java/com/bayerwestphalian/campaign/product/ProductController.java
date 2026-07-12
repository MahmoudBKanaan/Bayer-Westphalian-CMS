package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductView>>> searchProducts(
            @Valid @ModelAttribute ProductSearchRequest searchRequest) {
        List<ProductView> products = productService.searchProducts(searchRequest.toCriteria());

        return ResponseEntity.ok(ApiResponse.success("Products loaded", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductView>> getProduct(@PathVariable UUID id) {
        ProductView product = productService.findById(id);

        return ResponseEntity.ok(ApiResponse.success("Product loaded", product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductView>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductView product = productService.createProduct(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductView>> updateProduct(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        ProductView product = productService.updateProduct(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Product updated", product));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<ProductView>> disableProduct(@PathVariable UUID id) {
        ProductView product = productService.deactivateProduct(id);

        return ResponseEntity.ok(ApiResponse.success("Product disabled", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductView>> deleteProduct(@PathVariable UUID id) {
        ProductView product = productService.softDeleteProduct(id);

        return ResponseEntity.ok(ApiResponse.success("Product deleted", product));
    }
}
