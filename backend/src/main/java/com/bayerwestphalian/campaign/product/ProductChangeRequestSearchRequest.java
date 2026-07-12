package com.bayerwestphalian.campaign.product;

import java.util.UUID;

public record ProductChangeRequestSearchRequest(UUID productId, ProductChangeStatus status) {

    ProductChangeRequestSearchCriteria toCriteria() {
        return new ProductChangeRequestSearchCriteria(productId, status);
    }
}
