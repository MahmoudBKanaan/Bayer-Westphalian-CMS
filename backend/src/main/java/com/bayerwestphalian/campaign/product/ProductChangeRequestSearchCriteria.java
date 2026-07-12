package com.bayerwestphalian.campaign.product;

import java.util.UUID;

public record ProductChangeRequestSearchCriteria(UUID productId, ProductChangeStatus status) {}
