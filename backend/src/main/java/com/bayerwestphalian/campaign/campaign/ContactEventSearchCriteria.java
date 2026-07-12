package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

public record ContactEventSearchCriteria(
        UUID customerId, UUID campaignId, ContactEventType eventType) {}
