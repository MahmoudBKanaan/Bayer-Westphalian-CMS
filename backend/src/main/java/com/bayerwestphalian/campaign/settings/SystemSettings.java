package com.bayerwestphalian.campaign.settings;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Singleton system configuration row (KB item 534).
 *
 * <p>Stores admin-editable business limits shown on the System Settings screen. Defaults match
 * {@code app.contact.monthly-limit}, {@code app.contact.retry-limit}, and {@code
 * app.contact.uninterested-exclusion-days}.
 */
@Entity
@Table(name = "system_settings")
public class SystemSettings extends BaseEntity {

    /** Stable primary key for the singleton configuration row. */
    public static final UUID SINGLETON_ID =
            UUID.fromString("a1000000-0000-0000-0000-000000000001");

    @Column(name = "monthly_contact_limit", nullable = false)
    private int monthlyContactLimit;

    @Column(name = "send_retry_limit", nullable = false)
    private int sendRetryLimit;

    @Column(name = "uninterested_exclusion_days", nullable = false)
    private int uninterestedExclusionDays;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    protected SystemSettings() {}

    public static SystemSettings createDefaults(
            int monthlyContactLimit, int sendRetryLimit, int uninterestedExclusionDays) {
        SystemSettings settings = new SystemSettings();
        settings.setId(SINGLETON_ID);
        settings.monthlyContactLimit = monthlyContactLimit;
        settings.sendRetryLimit = sendRetryLimit;
        settings.uninterestedExclusionDays = uninterestedExclusionDays;
        return settings;
    }

    public void updateLimits(
            int monthlyContactLimit,
            int sendRetryLimit,
            int uninterestedExclusionDays,
            UUID updatedByUserId) {
        this.monthlyContactLimit = monthlyContactLimit;
        this.sendRetryLimit = sendRetryLimit;
        this.uninterestedExclusionDays = uninterestedExclusionDays;
        this.updatedByUserId = updatedByUserId;
    }

    public int getMonthlyContactLimit() {
        return monthlyContactLimit;
    }

    public int getSendRetryLimit() {
        return sendRetryLimit;
    }

    public int getUninterestedExclusionDays() {
        return uninterestedExclusionDays;
    }

    public UUID getUpdatedByUserId() {
        return updatedByUserId;
    }
}
