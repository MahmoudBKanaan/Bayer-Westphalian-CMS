package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

public record ContactEventSearchRequest(
        UUID customerId, UUID campaignId, ContactEventType eventType) {

    public ContactEventSearchCriteria toCriteria() {
        return new ContactEventSearchCriteria(customerId, campaignId, eventType);
    }
}
