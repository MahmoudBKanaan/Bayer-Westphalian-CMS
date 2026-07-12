package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** KB item 321: mock email provider is available only for development/test provider mode. */
class MockEmailProviderTests {

    @Test
    void mockEmailProviderIsDevTestBeanForMockModeOnly() {
        assertThat(EmailProvider.class).isAssignableFrom(MockEmailProvider.class);
        assertThat(MockEmailProvider.class.getAnnotation(Service.class)).isNotNull();

        Profile profile = MockEmailProvider.class.getAnnotation(Profile.class);
        ConditionalOnProperty property =
                MockEmailProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(profile.value()).containsExactly("dev", "test");
        assertThat(property.prefix()).isEqualTo("app.providers.email");
        assertThat(property.name()).containsExactly("mode");
        assertThat(property.havingValue()).isEqualTo("mock");
        assertThat(property.matchIfMissing()).isTrue();
    }

    @Test
    void acceptsValidEmailAndStoresMessageForInspection() {
        MockEmailProvider provider = new MockEmailProvider();
        EmailMessage message =
                new EmailMessage(
                        "ada@example.test",
                        "Campaign subject",
                        "Campaign body",
                        "campaign-321",
                        Map.of("campaignId", "50000000-0000-0000-0000-000000000321"));

        EmailDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("mock-email-1");
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(provider.sentMessages()).containsExactly(message);
    }

    @Test
    void rejectsInvalidEmailWithoutStoringMessage() {
        MockEmailProvider provider = new MockEmailProvider();

        EmailDeliveryResult result =
                provider.send(new EmailMessage(" ", "Campaign subject", "Body", null, Map.of()));

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo("INVALID_RECIPIENT");
        assertThat(result.errorMessage()).isEqualTo("Recipient email is required");
        assertThat(provider.sentMessages()).isEmpty();
    }

    @Test
    void clearRemovesMessagesAndResetsProviderMessageSequence() {
        MockEmailProvider provider = new MockEmailProvider();
        EmailMessage message =
                new EmailMessage("ada@example.test", "Subject", "Body", "first", Map.of());
        provider.send(message);

        provider.clear();
        EmailDeliveryResult result = provider.send(message);

        assertThat(provider.sentMessages()).containsExactly(message);
        assertThat(result.providerMessageId()).isEqualTo("mock-email-1");
    }
}
