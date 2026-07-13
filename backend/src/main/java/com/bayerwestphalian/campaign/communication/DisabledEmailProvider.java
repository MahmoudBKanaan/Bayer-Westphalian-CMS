package com.bayerwestphalian.campaign.communication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Production-safe email provider that never transmits messages. */
@Service
@Profile("prod")
@ConditionalOnProperty(
        prefix = "app.providers.email",
        name = "mode",
        havingValue = "disabled",
        matchIfMissing = true)
public class DisabledEmailProvider implements EmailProvider {

    public static final String DISABLED_CODE = "EMAIL_SENDING_DISABLED";

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        return EmailDeliveryResult.failed(
                DISABLED_CODE,
                "Production email sending is disabled until a real provider is configured");
    }
}
