package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** KB item 325: real provider sending is disabled unless explicitly enabled by environment. */
class ProviderSendingConfigurationTests {

    private static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");

    @Test
    void applicationConfigurationDisablesRealProviderSendingByDefault() throws Exception {
        String configuration = Files.readString(APPLICATION_YML, StandardCharsets.UTF_8);

        assertThat(configuration)
                .contains("providers:")
                .contains("real-sending-enabled: ${PROVIDER_REAL_SENDING_ENABLED:false}")
                .contains("retry-limit: ${CONTACT_RETRY_LIMIT:3}")
                .contains("email:")
                .contains("sms:");
    }
}
