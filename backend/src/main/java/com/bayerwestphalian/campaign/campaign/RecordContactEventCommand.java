package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

public record RecordContactEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        ContactEventType eventType,
        ContactOutcome outcome,
        String notes,
        Instant occurredAt) {}
