package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthResponseTests {

    @Test
    void createsUpHealthResponseForService() {
        HealthResponse response = HealthResponse.up("bayer-westphalian-campaign-platform");

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.service()).isEqualTo("bayer-westphalian-campaign-platform");
        assertThat(response.timestamp()).isNotNull();
    }
}
