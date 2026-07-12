package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record ProductSearchRequest(
        @Size(max = 255) String term, ProductType productType, Boolean active) {

    ProductSearchCriteria toCriteria() {
        return new ProductSearchCriteria(normalize(term), productType, active);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}