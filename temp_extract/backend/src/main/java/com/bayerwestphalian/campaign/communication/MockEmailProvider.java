package com.bayerwestphalian.campaign.communication;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Development/test email provider that records accepted messages without external delivery. */
@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(
        prefix = "app.providers.email",
        name = "mode",
        havingValue = "mock",
        matchIfMissing = true)
public class MockEmailProvider implements EmailProvider {

    private final AtomicLong messageSequence = new AtomicLong();
    private final CopyOnWriteArrayList<EmailMessage> sentMessages = new CopyOnWriteArrayList<>();

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        EmailDeliveryResult validationResult = validate(message);
        if (!validationResult.accepted()) {
            return validationResult;
        }

        sentMessages.add(message);
        return EmailDeliveryResult.accepted("mock-email-" + messageSequence.incrementAndGet());
    }

    public List<EmailMessage> sentMessages() {
        return List.copyOf(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
        messageSequence.set(0);
    }

    private static EmailDeliveryResult validate(EmailMessage message) {
        if (message == null) {
            return EmailDeliveryResult.failed("INVALID_MESSAGE", "Email message is required");
        }
        if (!StringUtils.hasText(message.to())) {
            return EmailDeliveryResult.failed("INVALID_RECIPIENT", "Recipient email is required");
        }
        if (!StringUtils.hasText(message.subject())) {
            return EmailDeliveryResult.failed("INVALID_SUBJECT", "Email subject is required");
        }
        if (!StringUtils.hasText(message.body())) {
            return EmailDeliveryResult.failed("INVALID_BODY", "Email body is required");
        }
        return EmailDeliveryResult.accepted(null);
    }
}
