package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

class SmtpEmailProviderTests {

    @Test
    void isConditionalOnSmtpEmailMode() {
        assertThat(EmailProvider.class).isAssignableFrom(SmtpEmailProvider.class);
        assertThat(SmtpEmailProvider.class.getAnnotation(Service.class)).isNotNull();

        ConditionalOnProperty property =
                SmtpEmailProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(property.prefix()).isEqualTo("app.providers.email");
        assertThat(property.name()).containsExactly("mode");
        assertThat(property.havingValue()).isEqualTo("smtp");
    }

    @Test
    void realSendingIsDisabledByDefaultAndReturnsFailedResult() {
        SmtpEmailProvider provider = new SmtpEmailProvider("smtp.test.example", 587, "user", false);
        EmailMessage message =
                new EmailMessage("test@example.com", "Subject", "Body", "correlation-id", Map.of());

        EmailDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo(SmtpEmailProvider.REAL_SENDING_DISABLED_CODE);
        assertThat(result.errorMessage()).contains("Real provider sending is disabled");
    }

    @Test
    void realSendingReturnsNotConfiguredWhenRealSendingIsEnabled() {
        SmtpEmailProvider provider = new SmtpEmailProvider("smtp.test.example", 587, "user", true);
        EmailMessage message =
                new EmailMessage("test@example.com", "Subject", "Body", "correlation-id", Map.of());

        EmailDeliveryResult result = provider.send(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo(SmtpEmailProvider.NOT_CONFIGURED_CODE);
        assertThat(result.errorMessage()).contains("real delivery is not enabled");
    }
}
