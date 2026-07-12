package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/** KB item 334: failed provider sends are retried before final delivery result is returned. */
class SendRetryServiceTests {

    @Test
    void exposesSendRetryServiceWithConfigurableAttemptCount() throws Exception {
        assertThat(SendRetryService.class.getAnnotation(Service.class)).isNotNull();
        ConditionalOnBean conditionalOnBean =
                SendRetryService.class.getAnnotation(ConditionalOnBean.class);
        assertThat(conditionalOnBean.value()).containsExactly(EmailProvider.class, SmsProvider.class);
        assertThat(SendRetryService.DEFAULT_MAX_ATTEMPTS).isEqualTo(3);
        assertThat(SendRetryService.HARD_MAX_ATTEMPTS).isEqualTo(5);
        Field logger = SendRetryService.class.getDeclaredField("LOGGER");
        assertThat(logger.getType()).isEqualTo(org.slf4j.Logger.class);

        Constructor<SendRetryService> constructor =
                SendRetryService.class.getConstructor(
                        EmailProvider.class, SmsProvider.class, int.class);

        Value value = constructor.getParameters()[2].getAnnotation(Value.class);
        assertThat(value.value()).isEqualTo("${app.contact.retry-limit:3}");
    }

    @Test
    void retriesFailedEmailSendUntilAccepted() {
        EmailProvider emailProvider = org.mockito.Mockito.mock(EmailProvider.class);
        SmsProvider smsProvider = org.mockito.Mockito.mock(SmsProvider.class);
        EmailMessage message = emailMessage();
        when(emailProvider.send(message))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.accepted("provider-email-334"));

        SendRetryService service = new SendRetryService(emailProvider, smsProvider, 3);

        EmailDeliveryResult result = service.sendEmailWithRetry(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("provider-email-334");
        verify(emailProvider, org.mockito.Mockito.times(2)).send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    @Test
    void logsCommunicationAttemptsFailuresAndAcceptanceWithoutMessageContent() {
        EmailProvider emailProvider = org.mockito.Mockito.mock(EmailProvider.class);
        SmsProvider smsProvider = org.mockito.Mockito.mock(SmsProvider.class);
        EmailMessage message = emailMessage();
        when(emailProvider.send(message))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.accepted("provider-email-336"));
        Logger logger = (Logger) LoggerFactory.getLogger(SendRetryService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            SendRetryService service = new SendRetryService(emailProvider, smsProvider, 3);

            service.sendEmailWithRetry(message);
        } finally {
            logger.detachAppender(appender);
        }

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(4);
        assertThat(events)
                .extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.WARN, Level.INFO, Level.INFO);
        assertThat(events)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                "Communication send failed",
                                                "channel=EMAIL",
                                                "correlationId=campaign-recipient-334",
                                                "attempt=1",
                                                "maxAttempts=3",
                                                "errorCode=TEMPORARY_FAILURE"))
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                "Communication send accepted",
                                                "providerMessageId=provider-email-336",
                                                "attempt=2"));
        assertThat(events)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .doesNotContain(
                                                "ada@example.test",
                                                "Your renewal offer is ready."));
    }

    @Test
    void stopsRetryingEmailAfterMaxAttemptsAndReturnsFinalFailure() {
        EmailProvider emailProvider = org.mockito.Mockito.mock(EmailProvider.class);
        SmsProvider smsProvider = org.mockito.Mockito.mock(SmsProvider.class);
        EmailMessage message = emailMessage();
        when(emailProvider.send(message))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.failed("BOUNCE", "Mailbox unavailable"));

        SendRetryService service = new SendRetryService(emailProvider, smsProvider, 2);

        EmailDeliveryResult result = service.sendEmailWithRetry(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorCode()).isEqualTo("BOUNCE");
        assertThat(result.errorMessage()).isEqualTo("Mailbox unavailable");
        verify(emailProvider, org.mockito.Mockito.times(2)).send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    @Test
    void doesNotRetryAcceptedSmsSend() {
        EmailProvider emailProvider = org.mockito.Mockito.mock(EmailProvider.class);
        SmsProvider smsProvider = org.mockito.Mockito.mock(SmsProvider.class);
        SmsMessage message = smsMessage();
        when(smsProvider.send(message)).thenReturn(SmsDeliveryResult.accepted("provider-sms-334"));

        SendRetryService service = new SendRetryService(emailProvider, smsProvider, 3);

        SmsDeliveryResult result = service.sendSmsWithRetry(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("provider-sms-334");
        verify(smsProvider).send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    @Test
    void retriesFailedSmsSendUntilAccepted() {
        EmailProvider emailProvider = org.mockito.Mockito.mock(EmailProvider.class);
        SmsProvider smsProvider = org.mockito.Mockito.mock(SmsProvider.class);
        SmsMessage message = smsMessage();
        when(smsProvider.send(message))
                .thenReturn(
                        SmsDeliveryResult.failed("TEMPORARY_FAILURE", "SMS provider timeout"),
                        SmsDeliveryResult.accepted("provider-sms-retry-334"));

        SendRetryService service = new SendRetryService(emailProvider, smsProvider, 3);

        SmsDeliveryResult result = service.sendSmsWithRetry(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("provider-sms-retry-334");
        verify(smsProvider, org.mockito.Mockito.times(2)).send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    @Test
    void rejectsInvalidRetryAttemptConfiguration() {
        SendRetryService service =
                new SendRetryService(
                        org.mockito.Mockito.mock(EmailProvider.class),
                        org.mockito.Mockito.mock(SmsProvider.class),
                        0);

        assertThatThrownBy(() -> service.sendEmailWithRetry(emailMessage()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Send retry validation failed");
    }

    @Test
    void rejectsRetryAttemptConfigurationAboveHardMaximum() {
        SendRetryService service =
                new SendRetryService(
                        org.mockito.Mockito.mock(EmailProvider.class),
                        org.mockito.Mockito.mock(SmsProvider.class),
                        SendRetryService.HARD_MAX_ATTEMPTS + 1);

        assertThatThrownBy(() -> service.sendSmsWithRetry(smsMessage()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Send retry validation failed")
                .hasMessageContaining("less than or equal to 5");
    }

    @Test
    void allowsRetryAttemptConfigurationAtHardMaximum() {
        EmailProvider emailProvider = org.mockito.Mockito.mock(EmailProvider.class);
        SmsProvider smsProvider = org.mockito.Mockito.mock(SmsProvider.class);
        EmailMessage message = emailMessage();
        when(emailProvider.send(message))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "Provider timeout"),
                        EmailDeliveryResult.accepted("provider-email-max-retry-335"));

        SendRetryService service =
                new SendRetryService(
                        emailProvider, smsProvider, SendRetryService.HARD_MAX_ATTEMPTS);

        EmailDeliveryResult result = service.sendEmailWithRetry(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("provider-email-max-retry-335");
        verify(emailProvider, org.mockito.Mockito.times(SendRetryService.HARD_MAX_ATTEMPTS))
                .send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    private static EmailMessage emailMessage() {
        return new EmailMessage(
                "ada@example.test",
                "Policy renewal",
                "Your renewal offer is ready.",
                "campaign-recipient-334",
                Map.of("campaignId", "50000000-0000-0000-0000-000000000334"));
    }

    private static SmsMessage smsMessage() {
        return new SmsMessage(
                "+4915112345678",
                "Your renewal offer is ready.",
                "campaign-recipient-334",
                Map.of("campaignId", "50000000-0000-0000-0000-000000000334"));
    }
}
