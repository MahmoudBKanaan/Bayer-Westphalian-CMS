package com.bayerwestphalian.campaign.settings;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * System settings service (KB item 534).
 *
 * <p>Loads and updates the singleton {@link SystemSettings} row used by the Admin System Settings
 * screen. Application-property defaults are used only when seeding a missing row (migration seeds
 * the production row). {@link #monthlyContactLimit()} is the runtime source for BR-011 eligibility
 * (item 535). {@link #sendRetryLimit()} is the runtime source for delivery retries (item 536).
 * {@link #uninterestedExclusionDays()} is the runtime source for uninterested exclusion windows
 * (item 537).
 */
@Service
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final int defaultMonthlyContactLimit;
    private final int defaultSendRetryLimit;
    private final int defaultUninterestedExclusionDays;

    public SystemSettingsService(
            SystemSettingsRepository systemSettingsRepository,
            AuthorizationExpressions authorizationExpressions,
            @Value("${app.contact.monthly-limit:3}") int defaultMonthlyContactLimit,
            @Value("${app.contact.retry-limit:3}") int defaultSendRetryLimit,
            @Value("${app.contact.uninterested-exclusion-days:90}")
                    int defaultUninterestedExclusionDays) {
        this.systemSettingsRepository = systemSettingsRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.defaultMonthlyContactLimit = defaultMonthlyContactLimit;
        this.defaultSendRetryLimit = defaultSendRetryLimit;
        this.defaultUninterestedExclusionDays = defaultUninterestedExclusionDays;
    }

    /**
     * Returns the current system settings for the Admin screen.
     */
    @PreAuthorize("@authz.canManageSystemSettings()")
    @Transactional(readOnly = true)
    public SystemSettingsView getSettings() {
        return SystemSettingsView.from(requireSettings());
    }

    /**
     * Updates contact/retry/uninterested limits (Admin only).
     */
    @PreAuthorize("@authz.canManageSystemSettings()")
    @Transactional
    public SystemSettingsView updateSettings(UpdateSystemSettingsCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "System settings validation failed", List.of("command: is required"));
        }
        validateLimits(
                command.monthlyContactLimit(),
                command.sendRetryLimit(),
                command.uninterestedExclusionDays());

        SystemSettings settings = requireSettings();
        UUID actorUserId = authorizationExpressions.currentUserId();
        settings.updateLimits(
                command.monthlyContactLimit(),
                command.sendRetryLimit(),
                command.uninterestedExclusionDays(),
                actorUserId);
        return SystemSettingsView.from(systemSettingsRepository.save(settings));
    }

    /**
     * Monthly marketing contact limit for eligibility (items 534–535 / BR-011).
     *
     * <p>Callable from domain services without Admin pre-authorize (read path).
     */
    @Transactional(readOnly = true)
    public int monthlyContactLimit() {
        return requireSettings().getMonthlyContactLimit();
    }

    /**
     * Maximum send retry attempts for {@code SendRetryService} (items 534 / 536).
     *
     * <p>Callable from domain services without Admin pre-authorize (read path).
     */
    @Transactional(readOnly = true)
    public int sendRetryLimit() {
        return requireSettings().getSendRetryLimit();
    }

    /**
     * Uninterested exclusion period in days for eligibility (items 534 / 537).
     *
     * <p>Callable from domain services without Admin pre-authorize (read path).
     */
    @Transactional(readOnly = true)
    public int uninterestedExclusionDays() {
        return requireSettings().getUninterestedExclusionDays();
    }

    private SystemSettings requireSettings() {
        return systemSettingsRepository
                .findById(SystemSettings.SINGLETON_ID)
                .orElseGet(
                        () ->
                                systemSettingsRepository.save(
                                        SystemSettings.createDefaults(
                                                defaultMonthlyContactLimit,
                                                defaultSendRetryLimit,
                                                defaultUninterestedExclusionDays)));
    }

    private void validateLimits(
            int monthlyContactLimit, int sendRetryLimit, int uninterestedExclusionDays) {
        List<String> errors = new ArrayList<>();
        if (monthlyContactLimit < 1 || monthlyContactLimit > 100) {
            errors.add("monthlyContactLimit: must be between 1 and 100");
        }
        if (sendRetryLimit < 1 || sendRetryLimit > 20) {
            errors.add("sendRetryLimit: must be between 1 and 20");
        }
        if (uninterestedExclusionDays < 1 || uninterestedExclusionDays > 3650) {
            errors.add("uninterestedExclusionDays: must be between 1 and 3650");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("System settings validation failed", errors);
        }
    }
}
