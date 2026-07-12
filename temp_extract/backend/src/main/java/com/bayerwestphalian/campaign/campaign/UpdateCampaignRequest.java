package com.bayerwestphalian.campaign.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * HTTP request body for {@code PUT /api/campaigns/{id}} (update draft campaign).
 *
 * <p>When {@code productIds} is present, it replaces the campaign's promoted products; omit or pass
 * null to leave products unchanged (service layer interprets null vs empty).
 */
public record UpdateCampaignRequest(
        @NotBlank(message = "Campaign name is required.")
                @Size(max = 255, message = "Campaign name must be 255 characters or fewer.")
                String name,
        @NotBlank(message = "Campaign objective is required.") String objective,
        UUID segmentId,
        @NotNull(message = "Campaign channel is required.") CampaignChannel channel,
        @Size(max = 255, message = "Message subject must be 255 characters or fewer.")
                String messageSubject,
        String messageBody,
        LocalDate startDate,
        LocalDate endDate,
        List<UUID> productIds) {

    UpdateCampaignCommand toCommand() {
        return new UpdateCampaignCommand(
                name,
                objective,
                segmentId,
                channel,
                messageSubject,
                messageBody,
                startDate,
                endDate,
                productIds == null ? null : List.copyOf(productIds));
    }
}
