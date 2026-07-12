package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.util.UUID;

public record ProductChangeRequestView(
        UUID id,
        UUID productId,
        String productName,
        ProductType productType,
        UUID requestedByUserId,
        String requestedByFullName,
        ProductChangeType requestType,
        String description,
        ProductChangeStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductChangeRequestView from(ProductChangeRequest request) {
        Product product = request.getProduct();
        User requestedBy = request.getRequestedBy();

        return new ProductChangeRequestView(
                request.getId(),
                productId(product),
                productName(product),
                productType(product),
                userId(requestedBy),
                fullName(requestedBy),
                request.getRequestType(),
                request.getDescription(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private static UUID productId(Product product) {
        return product == null ? null : product.getId();
    }

    private static String productName(Product product) {
        return product == null ? null : product.getName();
    }

    private static ProductType productType(Product product) {
        return product == null ? null : product.getProductType();
    }

    private static UUID userId(User user) {
        return user == null ? null : user.getId();
    }

    private static String fullName(User user) {
        return user == null ? null : user.getFullName();
    }
}
