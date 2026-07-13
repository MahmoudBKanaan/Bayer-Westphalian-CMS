package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

class DisabledEmailProviderTests {

    @Test
    void isTheProductionDefaultForDisabledEmailMode() {
        Profile profile = DisabledEmailProvider.class.getAnnotation(Profile.class);
        ConditionalOnProperty condition =
                DisabledEmailProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(profile.value()).containsExactly("prod");
        assertThat(condition.prefix()).isEqualTo("app.providers.email");
        assertThat(condition.name()).containsExactly("mode");
        assertThat(condition.havingValue()).isEqualTo("disabled");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    @Test
    void alwaysRejectsDeliveryWithoutTransmitting() {
        DisabledEmailProvider provider = new DisabledEmailProvider();
        EmailMessage message =
                new EmailMessage(
                        "customer@example.com", "Subject", "Body", "correlation-id", Map.of());

        EmailDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo("EMAIL_SENDING_DISABLED");
        assertThat(result.errorMessage()).contains("disabled until a real provider is configured");
    }
}
