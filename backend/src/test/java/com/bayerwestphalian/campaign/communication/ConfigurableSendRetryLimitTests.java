package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Item 536: send retry maximum attempts are read from {@link SystemSettingsService} at send time
 * (Admin System Settings), not a fixed {@code @Value} snapshot.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("536 Configurable send retry limit")
class ConfigurableSendRetryLimitTests {

    @Mock private EmailProvider emailProvider;
    @Mock private SmsProvider smsProvider;
    @Mock private SystemSettingsService systemSettingsService;

    @Test
    void emailRetryCountFollowsAdminConfiguredLimit() {
        EmailMessage message = emailMessage();
        when(systemSettingsService.sendRetryLimit()).thenReturn(4);
        when(emailProvider.send(message))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 1"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 2"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 3"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 4"),
                        EmailDeliveryResult.accepted("should-not-reach"));

        SendRetryService service =
                new SendRetryService(emailProvider, smsProvider, systemSettingsService);

        EmailDeliveryResult result = service.sendEmailWithRetry(message);

        assertThat(result.accepted()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("timeout 4");
        verify(systemSettingsService).sendRetryLimit();
        verify(emailProvider, times(4)).send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    @Test
    void raisedConfiguredLimitAllowsAdditionalEmailAttempts() {
        EmailMessage message = emailMessage();
        when(systemSettingsService.sendRetryLimit()).thenReturn(5);
        when(emailProvider.send(message))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 1"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 2"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 3"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "timeout 4"),
                        EmailDeliveryResult.accepted("provider-email-536"));

        SendRetryService service =
                new SendRetryService(emailProvider, smsProvider, systemSettingsService);

        EmailDeliveryResult result = service.sendEmailWithRetry(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("provider-email-536");
        verify(systemSettingsService).sendRetryLimit();
        verify(emailProvider, times(5)).send(message);
    }

    @Test
    void smsRetryCountFollowsAdminConfiguredLimit() {
        SmsMessage message = smsMessage();
        when(systemSettingsService.sendRetryLimit()).thenReturn(2);
        when(smsProvider.send(message))
                .thenReturn(
                        SmsDeliveryResult.failed("TEMPORARY_FAILURE", "sms timeout"),
                        SmsDeliveryResult.accepted("provider-sms-536"));

        SendRetryService service =
                new SendRetryService(emailProvider, smsProvider, systemSettingsService);

        SmsDeliveryResult result = service.sendSmsWithRetry(message);

        assertThat(result.accepted()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("provider-sms-536");
        verify(systemSettingsService).sendRetryLimit();
        verify(smsProvider, times(2)).send(message);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    @Test
    void readsRetryLimitFromSystemSettingsOnEachSend() {
        EmailMessage message = emailMessage();
        when(systemSettingsService.sendRetryLimit()).thenReturn(1);
        when(emailProvider.send(message))
                .thenReturn(EmailDeliveryResult.failed("TEMPORARY_FAILURE", "only once"));

        SendRetryService service =
                new SendRetryService(emailProvider, smsProvider, systemSettingsService);

        EmailDeliveryResult result = service.sendEmailWithRetry(message);

        assertThat(result.accepted()).isFalse();
        verify(systemSettingsService).sendRetryLimit();
        verify(emailProvider, times(1)).send(message);
    }

    @Test
    @DisplayName("558 Configurable retry limit is applied")
    void changedConfiguredRetryLimitAppliesToNextSendWithoutRestart() {
        EmailMessage firstMessage = emailMessage("campaign-recipient-558-a");
        EmailMessage secondMessage = emailMessage("campaign-recipient-558-b");
        when(systemSettingsService.sendRetryLimit()).thenReturn(1, 3);
        when(emailProvider.send(firstMessage))
                .thenReturn(EmailDeliveryResult.failed("TEMPORARY_FAILURE", "first attempt only"));
        when(emailProvider.send(secondMessage))
                .thenReturn(
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "retry 1"),
                        EmailDeliveryResult.failed("TEMPORARY_FAILURE", "retry 2"),
                        EmailDeliveryResult.accepted("provider-email-558"));

        SendRetryService service =
                new SendRetryService(emailProvider, smsProvider, systemSettingsService);

        EmailDeliveryResult firstResult = service.sendEmailWithRetry(firstMessage);
        EmailDeliveryResult secondResult = service.sendEmailWithRetry(secondMessage);

        assertThat(firstResult.accepted()).isFalse();
        assertThat(firstResult.errorMessage()).isEqualTo("first attempt only");
        assertThat(secondResult.accepted()).isTrue();
        assertThat(secondResult.providerMessageId()).isEqualTo("provider-email-558");
        verify(systemSettingsService, times(2)).sendRetryLimit();
        verify(emailProvider, times(1)).send(firstMessage);
        verify(emailProvider, times(3)).send(secondMessage);
        verifyNoMoreInteractions(emailProvider, smsProvider);
    }

    private static EmailMessage emailMessage() {
        return emailMessage("campaign-recipient-536");
    }

    private static EmailMessage emailMessage(String correlationId) {
        return new EmailMessage(
                "item536@example.test",
                "Retry limit",
                "Body",
                correlationId,
                Map.of("campaignId", "50000000-0000-0000-0000-000000000536"));
    }

    private static SmsMessage smsMessage() {
        return new SmsMessage(
                "+4915112345678",
                "Body",
                "campaign-recipient-536",
                Map.of("campaignId", "50000000-0000-0000-0000-000000000536"));
    }
}
