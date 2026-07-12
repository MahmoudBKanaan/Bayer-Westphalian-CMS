package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

/** Command for recording a provider/tracking opened-event placeholder. */
public record RecordOpenedEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        Instant occurredAt,
        String providerMessageId,
        String trackingReference) {}
