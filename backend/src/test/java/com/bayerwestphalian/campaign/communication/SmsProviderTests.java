package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** KB item 320: SMS delivery is hidden behind a replaceable provider adapter interface. */
class SmsProviderTests {

    @Test
    void definesReplaceableSmsProviderSendBoundary() throws Exception {
        assertThat(SmsProvider.class.isInterface()).isTrue();

        Method send = SmsProvider.class.getMethod("send", SmsMessage.class);

        assertThat(send.getReturnType()).isEqualTo(SmsDeliveryResult.class);
        assertThat(send.getParameterCount()).isEqualTo(1);
        assertThat(send.getParameters()[0].getType()).isEqualTo(SmsMessage.class);
    }

    @Test
    void smsMessageCarriesProviderNeutralPayload() {
        SmsMessage message =
                new SmsMessage(
                        "+4915112345678",
                        "Campaign body",
                        "campaign-123",
                        Map.of("campaignId", "50000000-0000-0000-0000-000000000320"));

        assertThat(message.to()).isEqualTo("+4915112345678");
        assertThat(message.body()).isEqualTo("Campaign body");
        assertThat(message.correlationId()).isEqualTo("campaign-123");
        assertThat(message.metadata())
                .containsEntry("campaignId", "50000000-0000-0000-0000-000000000320");
    }

    @Test
    void deliveryResultExposesAcceptedAndFailedOutcomes() {
        SmsDeliveryResult accepted = SmsDeliveryResult.accepted("provider-sms-1");
        SmsDeliveryResult failed = SmsDeliveryResult.failed("INVALID_PHONE", "Invalid phone");

        assertThat(accepted.accepted()).isTrue();
        assertThat(accepted.providerMessageId()).isEqualTo("provider-sms-1");
        assertThat(accepted.errorCode()).isNull();
        assertThat(accepted.errorMessage()).isNull();
        assertThat(failed.accepted()).isFalse();
        assertThat(failed.providerMessageId()).isNull();
        assertThat(failed.errorCode()).isEqualTo("INVALID_PHONE");
        assertThat(failed.errorMessage()).isEqualTo("Invalid phone");
    }
}
