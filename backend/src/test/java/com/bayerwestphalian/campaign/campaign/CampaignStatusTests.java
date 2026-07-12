package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CampaignStatusTests {

    @Test
    void exposesKbCampaignStatusValuesInLifecycleOrder() {
        assertThat(CampaignStatus.values())
                .containsExactly(
                        CampaignStatus.DRAFT,
                        CampaignStatus.SUBMITTED,
                        CampaignStatus.APPROVED,
                        CampaignStatus.REJECTED,
                        CampaignStatus.ACTIVE,
                        CampaignStatus.PAUSED,
                        CampaignStatus.COMPLETED,
                        CampaignStatus.ARCHIVED);
    }
}
