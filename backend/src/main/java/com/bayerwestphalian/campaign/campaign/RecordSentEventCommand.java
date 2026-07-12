package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

public record RecordSentEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        Instant occurredAt,
        String providerMessageId) {}
