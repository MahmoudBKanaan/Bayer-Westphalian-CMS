package com.bayerwestphalian.campaign.communication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Provider-ready SMS adapter placeholder. Real SMS delivery is intentionally not enabled yet. */
@Service
@ConditionalOnProperty(prefix = "app.providers.sms", name = "mode", havingValue = "provider")
public class ProviderReadySmsProvider implements SmsProvider {

    static final String NOT_CONFIGURED_CODE = "SMS_PROVIDER_NOT_CONFIGURED";
    static final String REAL_SENDING_DISABLED_CODE = "REAL_SENDING_DISABLED";

    private final String apiKey;
    private final boolean realSendingEnabled;

    public ProviderReadySmsProvider(
            @Value("${app.providers.sms.api-key:}") String apiKey,
            @Value("${app.providers.real-sending-enabled:false}") boolean realSendingEnabled) {
        this.apiKey = apiKey;
        this.realSendingEnabled = realSendingEnabled;
    }

    @Override
    public SmsDeliveryResult send(SmsMessage message) {
        SmsDeliveryResult validationResult = validate(message);
        if (!validationResult.accepted()) {
            return validationResult;
        }
        if (!realSendingEnabled) {
            return SmsDeliveryResult.failed(
                    REAL_SENDING_DISABLED_CODE,
                    "Real provider sending is disabled by environment configuration");
        }
        return SmsDeliveryResult.failed(
                NOT_CONFIGURED_CODE,
                "SMS provider is configured as a placeholder; real delivery is not enabled");
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean realSendingEnabled() {
        return realSendingEnabled;
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
