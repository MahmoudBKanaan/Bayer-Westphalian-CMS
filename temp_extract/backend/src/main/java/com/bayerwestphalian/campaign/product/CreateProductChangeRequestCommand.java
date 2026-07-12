package com.bayerwestphalian.campaign.product;

import java.util.UUID;

public record CreateProductChangeRequestCommand(
        UUID productId, ProductChangeType requestType, String description) {}