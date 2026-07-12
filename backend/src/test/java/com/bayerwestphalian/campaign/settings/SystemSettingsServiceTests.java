package com.bayerwestphalian.campaign.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingsService (item 534)")
class SystemSettingsServiceTests {

    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");

    @Mock private SystemSettingsRepository systemSettingsRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private SystemSettingsService systemSettingsService;

    @BeforeEach
    void setUp() {
        systemSettingsService =
                new SystemSettingsService(
                        systemSettingsRepository, authorizationExpressions, 3, 3, 90);
    }

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        void returnsSingletonRowWhenPresent() {
            SystemSettings settings = SystemSettings.createDefaults(3, 3, 90);
            when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID))
                    .thenReturn(Optional.of(settings));

            SystemSettingsView view = systemSettingsService.getSettings();

            assertThat(view.id()).isEqualTo(SystemSettings.SINGLETON_ID);
            assertThat(view.monthlyContactLimit()).isEqualTo(3);
            assertThat(view.sendRetryLimit()).isEqualTo(3);
            assertThat(view.uninterestedExclusionDays()).isEqualTo(90);
        }

        @Test
        void seedsDefaultsWhenRowMissing() {
            when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID))
                    .thenReturn(Optional.empty());
            when(systemSettingsRepository.save(any(SystemSettings.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SystemSettingsView view = systemSettingsService.getSettings();

            assertThat(view.monthlyContactLimit()).isEqualTo(3);
            assertThat(view.sendRetryLimit()).isEqualTo(3);
            assertThat(view.uninterestedExclusionDays()).isEqualTo(90);
            verify(systemSettingsRepository).save(any(SystemSettings.class));
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        void updatesLimitsAndRecordsActor() {
            SystemSettings settings = SystemSettings.createDefaults(3, 3, 90);
            when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID))
                    .thenReturn(Optional.of(settings));
            when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
            when(systemSettingsRepository.save(any(SystemSettings.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SystemSettingsView view =
                    systemSettingsService.updateSettings(new UpdateSystemSettingsCommand(5, 4, 120));

            assertThat(view.monthlyContactLimit()).isEqualTo(5);
            assertThat(view.sendRetryLimit()).isEqualTo(4);
            assertThat(view.uninterestedExclusionDays()).isEqualTo(120);
            assertThat(view.updatedByUserId()).isEqualTo(ACTOR_ID);

            ArgumentCaptor<SystemSettings> captor = ArgumentCaptor.forClass(SystemSettings.class);
            verify(systemSettingsRepository).save(captor.capture());
            assertThat(captor.getValue().getMonthlyContactLimit()).isEqualTo(5);
        }

        @Test
        void rejectsInvalidLimits() {
            assertThatThrownBy(
                            () ->
                                    systemSettingsService.updateSettings(
                                            new UpdateSystemSettingsCommand(0, 3, 90)))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("System settings validation failed");
        }

        @Test
        void rejectsNullCommand() {
            assertThatThrownBy(() -> systemSettingsService.updateSettings(null))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("limit accessors")
    class LimitAccessors {

        @Test
        void exposesLimitsForDomainWiring() {
            SystemSettings settings = SystemSettings.createDefaults(7, 2, 45);
            when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID))
                    .thenReturn(Optional.of(settings));

            assertThat(systemSettingsService.monthlyContactLimit()).isEqualTo(7);
            assertThat(systemSettingsService.sendRetryLimit()).isEqualTo(2);
            assertThat(systemSettingsService.uninterestedExclusionDays()).isEqualTo(45);
        }
    }
}
