package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

/** Command for recording a provider/tracking clicked-event placeholder. */
public record RecordClickedEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        Instant occurredAt,
        String providerMessageId,
        String trackingReference,
        String clickedUrl) {}
