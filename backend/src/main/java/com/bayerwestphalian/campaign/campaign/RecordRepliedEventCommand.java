package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.UUID;

/** Command for recording a provider/tracking replied-event placeholder. */
public record RecordRepliedEventCommand(
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        Instant occurredAt,
        String providerMessageId,
        String inboundMessageId,
        String replyText) {}
