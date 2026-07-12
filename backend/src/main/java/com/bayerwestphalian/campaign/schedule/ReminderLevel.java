package com.bayerwestphalian.campaign.schedule;

/**
 * Reminder severity levels (KB BR-020–BR-022 for payment due; BR-023 windows for expiration; item
 * 405 rules documentation).
 *
 * <p>For payment-due reminders ({@link PaymentReminderLevelRules}):
 *
 * <ul>
 *   <li>{@link #GREEN} — first reminder
 *   <li>{@link #YELLOW} — second reminder
 *   <li>{@link #RED} — third reminder / likely default risk
 * </ul>
 *
 * <p>For product-expiration windows ({@link ProductExpirationReminderRules}): 12 months → GREEN, 6
 * months → YELLOW, 3 months → RED.
 *
 * <p>See {@code docs/modules/green-yellow-red-reminder-rules.md}.
 */
public enum ReminderLevel {
    /** First payment reminder (KB BR-020); also 12-month expiration urgency. */
    GREEN,
    /** Second payment reminder (KB BR-021); also 6-month expiration urgency. */
    YELLOW,
    /** Third payment reminder and likely default risk (KB BR-022); also 3-month expiration urgency. */
    RED
}
