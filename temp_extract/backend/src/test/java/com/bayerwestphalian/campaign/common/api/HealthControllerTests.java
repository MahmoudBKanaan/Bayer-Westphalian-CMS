package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTests {

    @Test
    void returnsUpHealthResponse() {
        HealthController controller = new HealthController("bayer-westphalian-campaign-platform");

        HealthResponse response = controller.health();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.service()).isEqualTo("bayer-westphalian-campaign-platform");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void exposesApiHealthEndpoint() throws Exception {
        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new HealthController("bayer-westphalian-campaign-platform"))
                        .build();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("bayer-westphalian-campaign-platform"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
