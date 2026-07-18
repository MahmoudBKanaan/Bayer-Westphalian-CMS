package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Communication provider configuration documentation (KB provider adapters)")
class CommunicationProviderConfigurationDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/communication-provider-configuration.md");
    private static final Path APPLICATION = Path.of("src/main/resources/application.yml");
    private static final Path APPLICATION_PROD = Path.of("src/main/resources/application-prod.yml");

    @Test
    void guideExistsAndCoversKbProviderConfiguration() throws Exception {
        assertThat(GUIDE).exists();
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .containsIgnoringCase("Communication Provider Configuration")
                .contains("PROVIDER_REAL_SENDING_ENABLED")
                .contains("EMAIL_PROVIDER_MODE")
                .contains("SMS_PROVIDER_MODE")
                .contains("SMTP_PASSWORD")
                .contains("SMS_API_KEY")
                .contains("MockEmailProvider")
                .contains("DisabledEmailProvider")
                .contains("SmtpEmailProvider")
                .contains("ProviderReadySmsProvider")
                .contains("SendRetryService")
                .contains("SecretPresenceValidator")
                .contains("deployment/email-provider.md")
                .contains("deployment/sms-provider.md")
                .contains("modules/communication-tracking.md")
                .contains("CommunicationProviderConfigurationDocumentationTests");
    }

    @Test
    void applicationYamlExposesProviderConfigurationKeys() throws Exception {
        String yaml = Files.readString(APPLICATION, StandardCharsets.UTF_8);
        String prod = Files.readString(APPLICATION_PROD, StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains("real-sending-enabled: ${PROVIDER_REAL_SENDING_ENABLED:false}")
                .contains("mode: ${EMAIL_PROVIDER_MODE:mock}")
                .contains("mode: ${SMS_PROVIDER_MODE:mock}");
        assertThat(prod)
                .contains("real-sending-enabled: ${PROVIDER_REAL_SENDING_ENABLED:false}")
                .contains("mode: ${EMAIL_PROVIDER_MODE:disabled}")
                .contains("mode: ${SMS_PROVIDER_MODE:disabled}");
    }
}
