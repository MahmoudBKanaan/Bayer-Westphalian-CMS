package com.bayerwestphalian.campaign.campaign;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Service command for creating a draft campaign (FR-050–053, FR-057, FR-223–225 fields). */
public record CreateCampaignCommand(
        String name,
        String objective,
        UUID segmentId,
        CampaignChannel channel,
        String messageSubject,
        String messageBody,
        LocalDate startDate,
        LocalDate endDate,
        List<UUID> productIds) {}
