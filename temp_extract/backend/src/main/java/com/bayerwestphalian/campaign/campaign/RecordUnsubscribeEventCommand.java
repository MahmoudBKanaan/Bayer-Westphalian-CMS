package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

/** Command for handling a provider unsubscribe event and blocking future contact. */
public record RecordUnsubscribeEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        Instant occurredAt,
        String providerMessageId,
        String unsubscribeSource,
        String reason) {}
