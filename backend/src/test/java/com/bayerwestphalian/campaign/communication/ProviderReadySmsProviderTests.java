package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

class ProviderReadySmsProviderTests {

    @Test
    void isConditionalOnProviderSmsMode() {
        assertThat(SmsProvider.class).isAssignableFrom(ProviderReadySmsProvider.class);
        assertThat(ProviderReadySmsProvider.class.getAnnotation(Service.class)).isNotNull();

        ConditionalOnProperty property =
                ProviderReadySmsProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(property.prefix()).isEqualTo("app.providers.sms");
        assertThat(property.name()).containsExactly("mode");
        assertThat(property.havingValue()).isEqualTo("provider");
    }

    @Test
    void realSendingIsDisabledByDefaultAndReturnsFailedResult() {
        ProviderReadySmsProvider provider = new ProviderReadySmsProvider("test-api-key", false);
        SmsMessage message = new SmsMessage("+4915112345678", "Body", "correlation-id", Map.of());

        SmsDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode())
                .isEqualTo(ProviderReadySmsProvider.REAL_SENDING_DISABLED_CODE);
        assertThat(result.errorMessage()).contains("Real provider sending is disabled");
    }

    @Test
    void realSendingReturnsNotConfiguredWhenRealSendingIsEnabled() {
        ProviderReadySmsProvider provider = new ProviderReadySmsProvider("test-api-key", true);
        SmsMessage message = new SmsMessage("+4915112345678", "Body", "correlation-id", Map.of());

        SmsDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ProviderReadySmsProvider.NOT_CONFIGURED_CODE);
        assertThat(result.errorMessage()).contains("real delivery is not enabled");
    }
}
