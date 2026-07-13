package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("726 Configure real email provider or disable sending until configured")
class ProductionEmailProviderDocumentationTests {

    private static final Path PROFILE = Path.of("src/main/resources/application-prod.yml");
    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path TEMPLATE = Path.of(".env.production.example");
    private static final Path GUIDE = Path.of("../docs/deployment/email-provider.md");

    @Test
    void productionDefaultsToExplicitlyDisabledEmailSending() throws Exception {
        assertThat(Files.readString(PROFILE, StandardCharsets.UTF_8))
                .contains("real-sending-enabled: ${PROVIDER_REAL_SENDING_ENABLED:false}")
                .contains("mode: ${EMAIL_PROVIDER_MODE:disabled}");
        assertThat(Files.readString(COMPOSE, StandardCharsets.UTF_8))
                .contains("PROVIDER_REAL_SENDING_ENABLED: ${PROVIDER_REAL_SENDING_ENABLED:-false}")
                .contains("EMAIL_PROVIDER_MODE: ${EMAIL_PROVIDER_MODE:-disabled}");
        assertThat(Files.readString(TEMPLATE, StandardCharsets.UTF_8))
                .contains("PROVIDER_REAL_SENDING_ENABLED=false")
                .contains("EMAIL_PROVIDER_MODE=disabled")
                .contains("SMTP_PASSWORD=");
    }

    @Test
    void guideStatesPlaceholderCannotBeEnabledAsRealDelivery() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 726")
                .contains("provider-ready placeholder")
                .contains("EMAIL_SENDING_DISABLED")
                .contains("intentionally rejects that enabled SMTP combination")
                .contains("ProductionEmailProviderDocumentationTests");
    }
}
