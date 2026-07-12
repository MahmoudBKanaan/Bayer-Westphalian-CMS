package com.bayerwestphalian.campaign.campaign;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record RecordContactEventRequest(
        @NotNull UUID customerId,
        UUID campaignId,
        @NotNull CommunicationChannel channel,
        @NotNull ContactEventType eventType,
        ContactOutcome outcome,
        String notes,
        @NotNull Instant occurredAt) {

    public RecordContactEventCommand toCommand() {
        return new RecordContactEventCommand(
                customerId, campaignId, channel, eventType, outcome, notes, occurredAt);
    }
}
