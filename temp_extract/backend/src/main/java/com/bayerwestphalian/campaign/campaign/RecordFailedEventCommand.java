package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

public record RecordFailedEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        Instant occurredAt,
        String providerMessageId,
        String failureCode,
        String failureMessage) {}
