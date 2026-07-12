package com.bayerwestphalian.campaign.campaign;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service command for updating a draft (or rejected) campaign. {@code productIds} replaces the
 * promoted product set when non-null; {@code null} leaves products unchanged.
 */
public record UpdateCampaignCommand(
        String name,
        String objective,
        UUID segmentId,
        CampaignChannel channel,
        String messageSubject,
        String messageBody,
        LocalDate startDate,
        LocalDate endDate,
        List<UUID> productIds) {}
