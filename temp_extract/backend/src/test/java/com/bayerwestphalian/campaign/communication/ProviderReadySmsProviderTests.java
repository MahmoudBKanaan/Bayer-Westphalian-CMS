package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** KB item 324: provider-ready SMS adapter placeholder does not send externally. */
class ProviderReadySmsProviderTests {

    @Test
    void providerReadySmsProviderIsBeanForProviderMode() {
        assertThat(SmsProvider.class).isAssignableFrom(ProviderReadySmsProvider.class);
        assertThat(ProviderReadySmsProvider.class.getAnnotation(Service.class)).isNotNull();

        ConditionalOnProperty property =
                ProviderReadySmsProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(property.prefix()).isEqualTo("app.providers.sms");
        assertThat(property.name()).containsExactly("mode");
        assertThat(property.havingValue()).isEqualTo("provider");
        assertThat(property.matchIfMissing()).isFalse();
    }

    @Test
    void constructorReadsSmsProviderConfigurationProperties() throws Exception {
        Constructor<ProviderReadySmsProvider> constructor =
                ProviderReadySmsProvider.class.getConstructor(String.class, boolean.class);

        assertThat(constructor.getParameters()[0].getAnnotation(Value.class).value())
                .isEqualTo("${app.providers.sms.api-key:}");
        assertThat(constructor.getParameters()[1].getAnnotation(Value.class).value())
                .isEqualTo("${app.providers.real-sending-enabled:false}");

        ProviderReadySmsProvider provider = new ProviderReadySmsProvider("sms-api-key", true);

        assertThat(provider.apiKey()).isEqualTo("sms-api-key");
        assertThat(provider.realSendingEnabled()).isTrue();
    }

    @Test
    void validSmsReturnsRealSendingDisabledWhenEnvironmentFlagIsFalse() {
        ProviderReadySmsProvider provider = new ProviderReadySmsProvider("sms-api-key", false);

        SmsDeliveryResult result = provider.send(validMessage());

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo(ProviderReadySmsProvider.REAL_SENDING_DISABLED_CODE);
        assertThat(result.errorMessage())
                .isEqualTo("Real provider sending is disabled by environment configuration");
    }

    @Test
    void validSmsReturnsExplicitPlaceholderFailure() {
        ProviderReadySmsProvider provider = new ProviderReadySmsProvider("sms-api-key", true);

        SmsDeliveryResult result = provider.send(validMessage());

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo(ProviderReadySmsProvider.NOT_CONFIGURED_CODE);
        assertThat(result.errorMessage())
                .isEqualTo("SMS provider is configured as a placeholder; real delivery is not enabled");
    }

    @Test
    void invalidSmsReturnsValidationFailureBeforePlaceholderFailure() {
        ProviderReadySmsProvider provider = new ProviderReadySmsProvider("sms-api-key", true);

        SmsDeliveryResult result =
                provider.send(new SmsMessage(" ", "Campaign SMS body", null, Map.of()));

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo("INVALID_RECIPIENT");
        assertThat(result.errorMessage()).isEqualTo("Recipient phone is required");
    }

    private static SmsMessage validMessage() {
        return new SmsMessage(
                "+4915112345678",
                "Campaign SMS body",
                "campaign-324",
                Map.of("campaignId", "50000000-0000-0000-0000-000000000324"));
    }
}
