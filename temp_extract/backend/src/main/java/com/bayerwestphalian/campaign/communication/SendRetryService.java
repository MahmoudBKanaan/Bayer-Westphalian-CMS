package com.bayerwestphalian.campaign.communication;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/** Retries failed provider sends before caller records SENT or FAILED contact events. */
@Service
@ConditionalOnBean({EmailProvider.class, SmsProvider.class})
public class SendRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendRetryService.class);

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int HARD_MAX_ATTEMPTS = 5;

    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final int maxAttempts;

    public SendRetryService(
            EmailProvider emailProvider,
            SmsProvider smsProvider,
            @Value("${app.contact.retry-limit:3}") int maxAttempts) {
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
        this.maxAttempts = maxAttempts;
    }

    public EmailDeliveryResult sendEmailWithRetry(EmailMessage message) {
        validateMaxAttempts();

        EmailDeliveryResult result = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logSendAttempt("EMAIL", message == null ? null : message.correlationId(), attempt);
            result = emailProvider.send(message);
            if (result.accepted()) {
                logSendAccepted(
                        "EMAIL",
                        message == null ? null : message.correlationId(),
                        attempt,
                        result.providerMessageId());
                return result;
            }
            logSendFailed(
                    "EMAIL",
                    message == null ? null : message.correlationId(),
                    attempt,
                    result.errorCode(),
                    result.errorMessage());
        }
        return result;
    }

    public SmsDeliveryResult sendSmsWithRetry(SmsMessage message) {
        validateMaxAttempts();

        SmsDeliveryResult result = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logSendAttempt("SMS", message == null ? null : message.correlationId(), attempt);
            result = smsProvider.send(message);
            if (result.accepted()) {
                logSendAccepted(
                        "SMS",
                        message == null ? null : message.correlationId(),
                        attempt,
                        result.providerMessageId());
                return result;
            }
            logSendFailed(
                    "SMS",
                    message == null ? null : message.correlationId(),
                    attempt,
                    result.errorCode(),
                    result.errorMessage());
        }
        return result;
    }

    private void logSendAttempt(String channel, String correlationId, int attempt) {
        LOGGER.info(
                "Communication send attempt channel={} correlationId={} attempt={} maxAttempts={}",
                channel,
                correlationId,
                attempt,
                maxAttempts);
    }

    private static void logSendAccepted(
            String channel, String correlationId, int attempt, String providerMessageId) {
        LOGGER.info(
                "Communication send accepted channel={} correlationId={} attempt={} providerMessageId={}",
                channel,
                correlationId,
                attempt,
                providerMessageId);
    }

    private void logSendFailed(
            String channel, String correlationId, int attempt, String errorCode, String errorMessage) {
        LOGGER.warn(
                "Communication send failed channel={} correlationId={} attempt={} maxAttempts={} "
                        + "errorCode={} errorMessage={}",
                channel,
                correlationId,
                attempt,
                maxAttempts,
                errorCode,
                errorMessage);
    }

    private void validateMaxAttempts() {
        if (maxAttempts < 1) {
            throw new ValidationException(
                    "Send retry validation failed",
                    List.of("maxAttempts must be greater than or equal to 1"));
        }
        if (maxAttempts > HARD_MAX_ATTEMPTS) {
            throw new ValidationException(
                    "Send retry validation failed",
                    List.of("maxAttempts must be less than or equal to " + HARD_MAX_ATTEMPTS));
        }
    }
}
