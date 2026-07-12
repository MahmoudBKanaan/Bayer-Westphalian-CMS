package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JacksonConfigurationTests {

    @Test
    void configuresIsoDateSerializationForApiResponses() {
        JacksonConfiguration configuration = new JacksonConfiguration();
        assertThat(configuration.apiJsonSerializationCustomizer()).isNotNull();

        SamplePayload payload =
                new SamplePayload(LocalDate.parse("2026-07-15"), Instant.parse("2026-07-10T09:30:00Z"));
        JsonNode json = ControllerTestSupport.apiObjectMapper().valueToTree(payload);

        assertThat(json.get("dueDate").asText()).isEqualTo("2026-07-15");
        assertThat(json.get("paidAt").asText()).isEqualTo("2026-07-10T09:30:00Z");
    }

    private record SamplePayload(LocalDate dueDate, Instant paidAt) {}
}