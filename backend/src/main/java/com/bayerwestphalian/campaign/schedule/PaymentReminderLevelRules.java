package com.bayerwestphalian.campaign.schedule;

import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import java.util.Objects;

/**
 * Maps unpaid payment reminder history to Green/Yellow/Red levels (KB BR-020–BR-022 / item 405).
 *
 * <ul>
 *   <li>BR-020: Green is the first reminder ({@code reminder_count == 0})
 *   <li>BR-021: Yellow is the second reminder ({@code reminder_count == 1})
 *   <li>BR-022: Red is the third reminder ({@code reminder_count >= 2}) and likely default risk
 * </ul>
 *
 * <p>{@link PaymentStatus#DEFAULT_RISK} always resolves to {@link ReminderLevel#RED}. Product-
 * expiration windows use the same {@link ReminderLevel} values with different semantics (see {@link
 * ProductExpirationReminderRules} and {@code docs/modules/green-yellow-red-reminder-rules.md}).
 */
public final class PaymentReminderLevelRules {

    /** First payment reminder has not been escalated yet ({@code reminder_count == 0}). */
    public static final int FIRST_REMINDER_COUNT = 0;

    /** Second payment reminder after one prior reminder ({@code reminder_count == 1}). */
    public static final int SECOND_REMINDER_COUNT = 1;

    /** Third payment reminder after two prior reminders ({@code reminder_count >= 2}). */
    public static final int THIRD_REMINDER_COUNT = 2;

    private PaymentReminderLevelRules() {}

    /**
     * Resolves the payment reminder level from the payment record.
     *
     * <p>Default-risk status always maps to Red. Otherwise level follows reminder count: Green
     * first, Yellow second, Red third and beyond.
     */
    public static ReminderLevel resolve(PaymentRecord payment) {
        Objects.requireNonNull(payment, "payment is required");
        if (payment.getStatus() == PaymentStatus.DEFAULT_RISK) {
            return ReminderLevel.RED;
        }
        return resolveFromReminderCount(payment.getReminderCount());
    }

    /**
     * Resolves level from prior reminder count alone (KB BR-020–BR-022).
     *
     * <ul>
     *   <li>{@code 0} → {@link ReminderLevel#GREEN} (first reminder)
     *   <li>{@code 1} → {@link ReminderLevel#YELLOW} (second reminder)
     *   <li>{@code >= 2} → {@link ReminderLevel#RED} (third reminder)
     * </ul>
     */
    public static ReminderLevel resolveFromReminderCount(int reminderCount) {
        if (reminderCount >= THIRD_REMINDER_COUNT) {
            return ReminderLevel.RED;
        }
        if (reminderCount == SECOND_REMINDER_COUNT) {
            return ReminderLevel.YELLOW;
        }
        // BR-020: Green is the first reminder (count 0, and defensive for unexpected negatives).
        return ReminderLevel.GREEN;
    }

    /** Returns {@code true} when the level is the first (Green) payment reminder. */
    public static boolean isFirstReminder(ReminderLevel level) {
        return level == ReminderLevel.GREEN;
    }

    /** Returns {@code true} when the level is the second (Yellow) payment reminder (KB BR-021). */
    public static boolean isSecondReminder(ReminderLevel level) {
        return level == ReminderLevel.YELLOW;
    }

    /**
     * Returns {@code true} when the level is the third (Red) payment reminder (KB BR-022). Red also
     * indicates likely default risk.
     */
    public static boolean isThirdReminder(ReminderLevel level) {
        return level == ReminderLevel.RED;
    }
}
