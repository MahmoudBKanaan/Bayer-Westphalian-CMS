package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** KB item 323: SMTP email adapter placeholder is provider-ready but does not send externally. */
class SmtpEmailProviderTests {

    @Test
    void smtpEmailProviderIsBeanForSmtpProviderMode() {
        assertThat(EmailProvider.class).isAssignableFrom(SmtpEmailProvider.class);
        assertThat(SmtpEmailProvider.class.getAnnotation(Service.class)).isNotNull();

        ConditionalOnProperty property =
                SmtpEmailProvider.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(property.prefix()).isEqualTo("app.providers.email");
        assertThat(property.name()).containsExactly("mode");
        assertThat(property.havingValue()).isEqualTo("smtp");
        assertThat(property.matchIfMissing()).isFalse();
    }

    @Test
    void constructorReadsSmtpConfigurationProperties() throws Exception {
        Constructor<SmtpEmailProvider> constructor =
                SmtpEmailProvider.class.getConstructor(
                        String.class, int.class, String.class, boolean.class);

        assertThat(constructor.getParameters()[0].getAnnotation(Value.class).value())
                .isEqualTo("${app.providers.email.smtp-host:}");
        assertThat(constructor.getParameters()[1].getAnnotation(Value.class).value())
                .isEqualTo("${app.providers.email.smtp-port:587}");
        assertThat(constructor.getParameters()[2].getAnnotation(Value.class).value())
                .isEqualTo("${app.providers.email.smtp-username:}");
        assertThat(constructor.getParameters()[3].getAnnotation(Value.class).value())
                .isEqualTo("${app.providers.real-sending-enabled:false}");

        SmtpEmailProvider provider =
                new SmtpEmailProvider("smtp.example.test", 2525, "campaign-user", true);

        assertThat(provider.smtpHost()).isEqualTo("smtp.example.test");
        assertThat(provider.smtpPort()).isEqualTo(2525);
        assertThat(provider.smtpUsername()).isEqualTo("campaign-user");
        assertThat(provider.realSendingEnabled()).isTrue();
    }

    @Test
    void validEmailReturnsRealSendingDisabledWhenEnvironmentFlagIsFalse() {
        SmtpEmailProvider provider =
                new SmtpEmailProvider("smtp.example.test", 2525, "campaign-user", false);

        EmailDeliveryResult result = provider.send(validMessage());

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo(SmtpEmailProvider.REAL_SENDING_DISABLED_CODE);
        assertThat(result.errorMessage())
                .isEqualTo("Real provider sending is disabled by environment configuration");
    }

    @Test
    void validEmailReturnsExplicitPlaceholderFailure() {
        SmtpEmailProvider provider =
                new SmtpEmailProvider("smtp.example.test", 2525, "campaign-user", true);

        EmailDeliveryResult result = provider.send(validMessage());

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo(SmtpEmailProvider.NOT_CONFIGURED_CODE);
        assertThat(result.errorMessage())
                .isEqualTo(
                        "SMTP email provider is configured as a placeholder; real delivery is not enabled");
    }

    @Test
    void invalidEmailReturnsValidationFailureBeforePlaceholderFailure() {
        SmtpEmailProvider provider =
                new SmtpEmailProvider("smtp.example.test", 2525, "campaign-user", true);

        EmailDeliveryResult result =
                provider.send(new EmailMessage(" ", "Campaign subject", "Body", null, Map.of()));

        assertThat(result.accepted()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorCode()).isEqualTo("INVALID_RECIPIENT");
        assertThat(result.errorMessage()).isEqualTo("Recipient email is required");
    }

    private static EmailMessage validMessage() {
        return new EmailMessage(
                "ada@example.test",
                "Campaign subject",
                "Campaign body",
                "campaign-323",
                Map.of("campaignId", "50000000-0000-0000-0000-000000000323"));
    }
}
