package com.bayerwestphalian.campaign.product;

import java.util.UUID;

public record PaymentRecordSearchRequest(UUID customerId, PaymentStatus status) {

    PaymentRecordSearchCriteria toCriteria() {
        return new PaymentRecordSearchCriteria(customerId, status);
    }
}