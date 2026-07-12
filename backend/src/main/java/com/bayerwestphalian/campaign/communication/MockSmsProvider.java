package com.bayerwestphalian.campaign.communication;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Development/test SMS provider that records accepted messages without external delivery. */
@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(
        prefix = "app.providers.sms",
        name = "mode",
        havingValue = "mock",
        matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    private final AtomicLong messageSequence = new AtomicLong();
    private final CopyOnWriteArrayList<SmsMessage> sentMessages = new CopyOnWriteArrayList<>();

    @Override
    public SmsDeliveryResult send(SmsMessage message) {
        SmsDeliveryResult validationResult = validate(message);
        if (!validationResult.accepted()) {
            return validationResult;
        }

        sentMessages.add(message);
        return SmsDeliveryResult.accepted("mock-sms-" + messageSequence.incrementAndGet());
    }

    public List<SmsMessage> sentMessages() {
        return List.copyOf(sentMessages);
    }

    public void clear() {
        sentMessages.clear();
        messageSequence.set(0);
    }

    private static SmsDeliveryResult validate(SmsMessage message) {
        if (message == null) {
            return SmsDeliveryResult.failed("INVALID_MESSAGE", "SMS message is required");
        }
        if (!StringUtils.hasText(message.to())) {
            return SmsDeliveryResult.failed("INVALID_RECIPIENT", "Recipient phone is required");
        }
        if (!StringUtils.hasText(message.body())) {
            return SmsDeliveryResult.failed("INVALID_BODY", "SMS body is required");
        }
        return SmsDeliveryResult.accepted(null);
    }
}
