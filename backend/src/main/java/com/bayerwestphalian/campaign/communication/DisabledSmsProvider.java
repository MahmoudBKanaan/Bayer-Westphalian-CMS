package com.bayerwestphalian.campaign.communication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Production-safe SMS provider that never transmits messages. */
@Service
@Profile("prod")
@ConditionalOnProperty(
        prefix = "app.providers.sms",
        name = "mode",
        havingValue = "disabled",
        matchIfMissing = true)
public class DisabledSmsProvider implements SmsProvider {

    public static final String DISABLED_CODE = "SMS_SENDING_DISABLED";

    @Override
    public SmsDeliveryResult send(SmsMessage message) {
        return SmsDeliveryResult.failed(
                DISABLED_CODE,
                "Production SMS sending is disabled until a real provider is configured");
    }
}
