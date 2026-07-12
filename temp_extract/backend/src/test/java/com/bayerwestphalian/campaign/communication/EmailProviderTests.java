package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** KB item 319: email delivery is hidden behind a replaceable provider adapter interface. */
class EmailProviderTests {

    @Test
    void definesReplaceableEmailProviderSendBoundary() throws Exception {
        assertThat(EmailProvider.class.isInterface()).isTrue();

        Method send = EmailProvider.class.getMethod("send", EmailMessage.class);

        assertThat(send.getReturnType()).isEqualTo(EmailDeliveryResult.class);
        assertThat(send.getParameterCount()).isEqualTo(1);
        assertThat(send.getParameters()[0].getType()).isEqualTo(EmailMessage.class);
    }

    @Test
    void emailMessageCarriesProviderNeutralPayload() {
        EmailMessage message =
                new EmailMessage(
                        "ada@example.test",
                        "Campaign subject",
                        "Campaign body",
                        "campaign-123",
                        Map.of("campaignId", "50000000-0000-0000-0000-000000000319"));

        assertThat(message.to()).isEqualTo("ada@example.test");
        assertThat(message.subject()).isEqualTo("Campaign subject");
        assertThat(message.body()).isEqualTo("Campaign body");
        assertThat(message.correlationId()).isEqualTo("campaign-123");
        assertThat(message.metadata())
                .containsEntry("campaignId", "50000000-0000-0000-0000-000000000319");
    }

    @Test
    void deliveryResultExposesAcceptedAndFailedOutcomes() {
        EmailDeliveryResult accepted = EmailDeliveryResult.accepted("provider-message-1");
        EmailDeliveryResult failed = EmailDeliveryResult.failed("BOUNCE", "Mailbox unavailable");

        assertThat(accepted.accepted()).isTrue();
        assertThat(accepted.providerMessageId()).isEqualTo("provider-message-1");
        assertThat(accepted.errorCode()).isNull();
        assertThat(accepted.errorMessage()).isNull();
        assertThat(failed.accepted()).isFalse();
        assertThat(failed.providerMessageId()).isNull();
        assertThat(failed.errorCode()).isEqualTo("BOUNCE");
        assertThat(failed.errorMessage()).isEqualTo("Mailbox unavailable");
    }
}
