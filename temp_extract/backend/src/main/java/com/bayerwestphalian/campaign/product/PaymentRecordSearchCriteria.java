package com.bayerwestphalian.campaign.product;

import java.util.UUID;

public record PaymentRecordSearchCriteria(UUID customerId, PaymentStatus status) {}