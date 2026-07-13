package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("727 Configure real SMS provider or disable sending until configured")
class ProductionSmsProviderDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/sms-provider.md");

    @Test
    void productionDefaultsToExplicitlyDisabledSmsSending() throws Exception {
        assertThat(Files.readString(PROFILE, StandardCharsets.UTF_8))
                .contains("mode: ${SMS_PROVIDER_MODE:disabled}")
                .contains("api-key: ${SMS_API_KEY:}");
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("PROVIDER_REAL_SENDING_ENABLED: ${PROVIDER_REAL_SENDING_ENABLED:-false}")
                .contains("SMS_PROVIDER_MODE: ${SMS_PROVIDER_MODE:-disabled}");
        assertThat(Files.readString(TEMPLATE, StandardCharsets.UTF_8))
                .contains("PROVIDER_REAL_SENDING_ENABLED=false")
                .contains("SMS_PROVIDER_MODE=disabled")
                .contains("SMS_API_KEY=");
    }

    @Test
    void guideStatesPlaceholderCannotBeEnabledAsRealDelivery() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 727")
                .contains("provider placeholder")
                .contains("SMS_SENDING_DISABLED")
                .contains("intentionally rejects that enabled provider combination")
                .contains("ProductionSmsProviderDocumentationTests");
    }
}
