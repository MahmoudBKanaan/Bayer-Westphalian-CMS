package com.bayerwestphalian.campaign.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductView(
        UUID id,
        String name,
        ProductType productType,
        String description,
        BigDecimal price,
        Integer durationMonths,
        String expirationPolicy,
        boolean active,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public static ProductView from(Product product) {
        return new ProductView(
                product.getId(),
                product.getName(),
                product.getProductType(),
                product.getDescription(),
                product.getPrice(),
                product.getDurationMonths(),
                product.getExpirationPolicy(),
                product.getActive(),
                product.isDeleted(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt());
    }
}
