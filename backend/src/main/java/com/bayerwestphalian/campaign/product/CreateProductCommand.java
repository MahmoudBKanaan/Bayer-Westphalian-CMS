package com.bayerwestphalian.campaign.product;

import java.math.BigDecimal;

public record CreateProductCommand(
        String name,
        ProductType productType,
        String description,
        BigDecimal price,
        Integer durationMonths,
        String expirationPolicy) {}
