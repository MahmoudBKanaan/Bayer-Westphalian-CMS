package com.bayerwestphalian.campaign.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigurationTests {

    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    void createsProjectOpenApiMetadata() {
        OpenAPI openApi = configuration.bayerWestphalianOpenApi();

        assertThat(openApi.getInfo().getTitle())
                .isEqualTo("Bayer-Westphalian Campaign Management Platform API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("0.1.0");
        assertThat(openApi.getInfo().getDescription())
                .contains("consent-aware segmentation", "AI-assisted recommendations");
        assertThat(openApi.getInfo().getContact().getName())
                .isEqualTo("Bayer-Westphalian Internal Project Team");
        assertThat(openApi.getInfo().getLicense().getName()).isEqualTo("Internal use only");
    }
}
