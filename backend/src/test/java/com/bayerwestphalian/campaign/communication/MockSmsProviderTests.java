package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** KB item 321: mock SMS provider is available only for development/test provider mode. */
class MockSmsProviderTests {

    @Test
    void mockSmsProviderIsDevTestBeanForMockModeOnly() {
        assertThat(SmsProvider.class).isAssignableFrom(MockSmsProvider.class);
        assertThat(MockSmsProvider.class.getAnnotation(Service.class)).isNotNull();

        Profile profile = MockSmsProvider.class.getAnnotation(Profile.class);
        ConditionalOnProperty property =
                MockSmsProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(profile.value()).containsExactly("dev", "test");
        assertThat(property.prefix()).isEqualTo("app.providers.sms");
        assertThat(property.name()).containsExactly("mode");
        assertThat(property.havingValue()).isEqualTo("mock");
        assertThat(property.matchIfMissing()).isTrue();
    }

    @Test
    void acceptsValidSmsAndStoresMessageForInspection() {
        MockSmsProvider provider = new MockSmsProvider();
        SmsMessage message =
                new SmsMessage(
                        "+4915112345678",
                        "Campaign body",
                        "campaign-321",
                        Map.of("campaignId", "50000000-0000-0000-0000-000000000321"));

        SmsDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("mock-sms-1");
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(provider.sentMessages()).containsExactly(message);
    }

    @Test
    void rejectsInvalidSmsWithoutStoringMessage() {
        MockSmsProvider provider = new MockSmsProvider();

        SmsDeliveryResult result = provider.send(new SmsMessage(" ", "Body", null, Map.of()));

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo("INVALID_RECIPIENT");
        assertThat(result.errorMessage()).isEqualTo("Recipient phone is required");
        assertThat(provider.sentMessages()).isEmpty();
    }

    @Test
    void clearRemovesMessagesAndResetsProviderMessageSequence() {
        MockSmsProvider provider = new MockSmsProvider();
        SmsMessage message = new SmsMessage("+4915112345678", "Body", "first", Map.of());
        provider.send(message);

        provider.clear();
        SmsDeliveryResult result = provider.send(message);

        assertThat(provider.sentMessages()).containsExactly(message);
        assertThat(result.providerMessageId()).isEqualTo("mock-sms-1");
    }
}
