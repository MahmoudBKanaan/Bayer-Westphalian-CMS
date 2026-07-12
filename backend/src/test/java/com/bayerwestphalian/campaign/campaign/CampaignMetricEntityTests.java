package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 412 acceptance: CampaignMetric ({@link CampaignMetrics}) entity implements required
 * counters and calculation methods from the knowledge base class system.
 */
class CampaignMetricEntityTests {

    @Test
    void campaignMetricEntityExposesKbConstructorFieldsAndCalculationMethods() {
        Campaign campaign = sampleCampaign();
        CampaignMetrics metric = CampaignMetrics.forCampaign(campaign);

        metric.recordLaunchCounts(100, 20, 90);
        metric.recordEngagementCounts(45, 18, 9, 5);
        metric.updateFinancialEstimates(new BigDecimal("1000.00"), new BigDecimal("1500.00"));
        metric.recalculate();

        assertThat(metric.getCampaignId()).isEqualTo(campaign.getId());
        assertThat(metric.getAudienceSize()).isEqualTo(120);
        assertThat(metric.getEligibleCount()).isEqualTo(100);
        assertThat(metric.getExcludedCount()).isEqualTo(20);
        assertThat(metric.getSentCount()).isEqualTo(90);
        assertThat(metric.getOpenedCount()).isEqualTo(45);
        assertThat(metric.getClickedCount()).isEqualTo(18);
        assertThat(metric.getRepliedCount()).isEqualTo(9);
        assertThat(metric.getConvertedCount()).isEqualTo(5);
        assertThat(metric.getEstimatedCost()).isEqualByComparingTo("1000.00");
        assertThat(metric.getEstimatedRevenue()).isEqualByComparingTo("1500.00");
        assertThat(metric.getEstimatedRoi()).isEqualByComparingTo("0.50");
        assertThat(metric.calculateOpenRate()).isEqualByComparingTo("0.5000");
        assertThat(metric.calculateClickRate()).isEqualByComparingTo("0.2000");
        assertThat(metric.calculateConversionRate()).isEqualByComparingTo("0.0556");
        assertThat(metric.calculateRoi()).isEqualByComparingTo("0.50");
    }

    private static Campaign sampleCampaign() {
        User owner = User.create("metric-entity@test.example", "{noop}x", "Metric Entity");
        Campaign campaign =
                Campaign.create(
                        "Metric entity campaign",
                        "KB CampaignMetric",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(
                campaign, "id", UUID.fromString("50000000-0000-0000-0000-000000000412"));
        return campaign;
    }
}
