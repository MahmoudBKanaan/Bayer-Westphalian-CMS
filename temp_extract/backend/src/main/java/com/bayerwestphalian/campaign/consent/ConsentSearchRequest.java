package com.bayerwestphalian.campaign.consent;

import java.util.UUID;

public record ConsentSearchRequest(
        UUID customerId, ConsentType consentType, ConsentStatus status, Boolean validOnly) {

    ConsentSearchCriteria toCriteria() {
        return new ConsentSearchCriteria(customerId, consentType, status, validOnly);
    }
}
