package com.bayerwestphalian.campaign.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** SMTP-ready adapter placeholder. Real SMTP delivery is intentionally not enabled yet. */
@Service
@ConditionalOnProperty(prefix = "app.providers.email", name = "mode", havingValue = "smtp")
public class SmtpEmailProvider implements EmailProvider {

    static final String NOT_CONFIGURED_CODE = "SMTP_PROVIDER_NOT_CONFIGURED";
    static final String REAL_SENDING_DISABLED_CODE = "REAL_SENDING_DISABLED";

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final boolean realSendingEnabled;

    public SmtpEmailProvider(
            @Value("${app.providers.email.smtp-host:}") String smtpHost,
            @Value("${app.providers.email.smtp-port:587}") int smtpPort,
            @Value("${app.providers.email.smtp-username:}") String smtpUsername,
            @Value("${app.providers.real-sending-enabled:false}") boolean realSendingEnabled) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.realSendingEnabled = realSendingEnabled;
    }

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        EmailDeliveryResult validationResult = validate(message);
        if (!validationResult.accepted()) {
            return validationResult;
        }
        if (!realSendingEnabled) {
            return EmailDeliveryResult.failed(
                    REAL_SENDING_DISABLED_CODE,
                    "Real provider sending is disabled by environment configuration");
        }
        return EmailDeliveryResult.failed(
                NOT_CONFIGURED_CODE,
                "SMTP email provider is configured as a placeholder; real delivery is not enabled");
    }

    public String smtpHost() {
        return smtpHost;
    }

    public int smtpPort() {
        return smtpPort;
    }

    public String smtpUsername() {
        return smtpUsername;
    }

    public boolean realSendingEnabled() {
        return realSendingEnabled;
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
