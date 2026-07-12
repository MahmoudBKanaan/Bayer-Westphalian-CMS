package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CampaignChannelTests {

    @Test
    void exposesKbCampaignChannelValues() {
        assertThat(CampaignChannel.values())
                .containsExactly(
                        CampaignChannel.EMAIL,
                        CampaignChannel.PHONE,
                        CampaignChannel.SMS,
                        CampaignChannel.MIXED);
    }
}
