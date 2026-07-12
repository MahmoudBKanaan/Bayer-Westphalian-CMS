package com.bayerwestphalian.campaign.product;

import java.math.BigDecimal;

public record UpdateProductCommand(
        String name,
        ProductType productType,
        String description,
        BigDecimal price,
        Integer durationMonths,
        String expirationPolicy,
        Boolean active) {}