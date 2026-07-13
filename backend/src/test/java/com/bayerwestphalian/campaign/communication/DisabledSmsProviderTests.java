package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

class DisabledSmsProviderTests {

    @Test
    void isTheProductionDefaultForDisabledSmsMode() {
        Profile profile = DisabledSmsProvider.class.getAnnotation(Profile.class);
        ConditionalOnProperty condition =
                DisabledSmsProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(profile.value()).containsExactly("prod");
        assertThat(condition.prefix()).isEqualTo("app.providers.sms");
        assertThat(condition.name()).containsExactly("mode");
        assertThat(condition.havingValue()).isEqualTo("disabled");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    @Test
    void alwaysRejectsDeliveryWithoutTransmitting() {
        DisabledSmsProvider provider = new DisabledSmsProvider();
        SmsMessage message =
                new SmsMessage("+4915112345678", "Message", "correlation-id", Map.of());

        SmsDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo("SMS_SENDING_DISABLED");
        assertThat(result.errorMessage()).contains("disabled until a real provider is configured");
    }
}
