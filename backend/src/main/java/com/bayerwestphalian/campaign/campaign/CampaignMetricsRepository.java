package com.bayerwestphalian.campaign.campaign;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for campaign performance counters (KB {@code CampaignMetricRepository} / table {@code
 * campaign_metrics}).
 *
 * <p>One metrics row per campaign ({@code campaign_id} unique). Primary lookup: {@link
 * #findByCampaignId(UUID)}.
 */
public interface CampaignMetricsRepository extends JpaRepository<CampaignMetrics, UUID> {

    /**
     * Spring Data property path for the unique campaign association.
     *
     * @see #findByCampaignId(UUID)
     */
    Optional<CampaignMetrics> findByCampaign_Id(UUID campaignId);

    /** Whether a metrics row already exists for the given campaign. */
    boolean existsByCampaign_Id(UUID campaignId);

    /**
     * KB {@code CampaignMetricRepository.findByCampaignId()} — load metrics for a campaign id.
     */
    default Optional<CampaignMetrics> findByCampaignId(UUID campaignId) {
        return findByCampaign_Id(campaignId);
    }

    /** KB-friendly existence check for a campaign's metrics row. */
    default boolean existsByCampaignId(UUID campaignId) {
        return existsByCampaign_Id(campaignId);
    }
}
