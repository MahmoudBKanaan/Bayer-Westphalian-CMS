package com.bayerwestphalian.campaign.communication;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Retries failed provider sends before the caller records SENT or FAILED contact events.
 *
 * <p>Item 536: maximum attempts come from Admin System Settings ({@link
 * SystemSettingsService#sendRetryLimit()}) at send time so configuration changes apply without
 * restart. Application property {@code app.contact.retry-limit} only seeds the settings default.
 */
@Service
@ConditionalOnBean({EmailProvider.class, SmsProvider.class})
public class SendRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendRetryService.class);

    /** Seed / documentation default (matches {@code app.contact.retry-limit} default). */
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    /**
     * Safety ceiling aligned with System Settings max ({@code sendRetryLimit} 1–20).
     */
    public static final int HARD_MAX_ATTEMPTS = 20;

    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final SystemSettingsService systemSettingsService;

    public SendRetryService(
            EmailProvider emailProvider,
            SmsProvider smsProvider,
            SystemSettingsService systemSettingsService) {
        this.emailProvider = emailProvider;
        this.smsProvider = smsProvider;
        this.systemSettingsService = systemSettingsService;
    }

    public EmailDeliveryResult sendEmailWithRetry(EmailMessage message) {
        int maxAttempts = configuredMaxAttempts();
        validateMaxAttempts(maxAttempts);

        EmailDeliveryResult result = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logSendAttempt("EMAIL", message == null ? null : message.correlationId(), attempt, maxAttempts);
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
                    maxAttempts,
                    result.errorCode(),
                    result.errorMessage());
        }
        return result;
    }

    public SmsDeliveryResult sendSmsWithRetry(SmsMessage message) {
        int maxAttempts = configuredMaxAttempts();
        validateMaxAttempts(maxAttempts);

        SmsDeliveryResult result = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logSendAttempt("SMS", message == null ? null : message.correlationId(), attempt, maxAttempts);
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
                    maxAttempts,
                    result.errorCode(),
                    result.errorMessage());
        }
        return result;
    }

    /**
     * Admin-configured send retry limit (item 536).
     *
     * <p>Read on each send so System Settings updates apply without restart.
     */
    private int configuredMaxAttempts() {
        return systemSettingsService.sendRetryLimit();
    }

    private void logSendAttempt(
            String channel, String correlationId, int attempt, int maxAttempts) {
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
            String channel,
            String correlationId,
            int attempt,
            int maxAttempts,
            String errorCode,
            String errorMessage) {
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

    private void validateMaxAttempts(int maxAttempts) {
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
