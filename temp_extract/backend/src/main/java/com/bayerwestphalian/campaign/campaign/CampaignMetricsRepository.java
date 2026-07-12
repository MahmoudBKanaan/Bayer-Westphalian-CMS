package com.bayerwestphalian.campaign.campaign;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for campaign performance counters (KB {@code campaign_metrics}). */
public interface CampaignMetricsRepository extends JpaRepository<CampaignMetrics, UUID> {

    Optional<CampaignMetrics> findByCampaign_Id(UUID campaignId);
}
