package com.bayerwestphalian.campaign.schedule;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Product-expiration reminder windows (KB BR-023).
 *
 * <p>Product-expiration campaigns/reminders may start 3, 6, or 12 months before expiration:
 *
 * <ul>
 *   <li>3 months → {@link ReminderLevel#RED} (nearest / highest urgency)
 *   <li>6 months → {@link ReminderLevel#YELLOW}
 *   <li>12 months → {@link ReminderLevel#GREEN}
 * </ul>
 *
 * <p>Level meanings for payment escalation differ; see {@code
 * docs/modules/green-yellow-red-reminder-rules.md} (item 405).
 */
public final class ProductExpirationReminderRules {

    /** KB BR-023: generate product-expiration reminders 3 months before expiration. */
    public static final int THREE_MONTH_WINDOW = 3;

    /** KB BR-023: generate product-expiration reminders 6 months before expiration. */
    public static final int SIX_MONTH_WINDOW = 6;

    /** KB BR-023: generate product-expiration reminders 12 months before expiration. */
    public static final int TWELVE_MONTH_WINDOW = 12;

    private ProductExpirationReminderRules() {}

    /** Reminder level used for the 3-month product-expiration window. */
    public static ReminderLevel threeMonthReminderLevel() {
        return ReminderLevel.RED;
    }

    /** Reminder level used for the 6-month product-expiration window. */
    public static ReminderLevel sixMonthReminderLevel() {
        return ReminderLevel.YELLOW;
    }

    /** Reminder level used for the 12-month product-expiration window. */
    public static ReminderLevel twelveMonthReminderLevel() {
        return ReminderLevel.GREEN;
    }

    /**
     * Inclusive end date of the ownership expiration search window starting at {@code asOfDate}.
     *
     * <p>Ownerships expiring between {@code asOfDate} and {@code asOfDate.plusMonths(windowMonths)}
     * are candidates. Supported windows: 3, 6, and 12 months (KB BR-023).
     */
    public static LocalDate windowEnd(LocalDate asOfDate, int windowMonths) {
        Objects.requireNonNull(asOfDate, "asOfDate is required");
        if (windowMonths <= 0) {
            throw new IllegalArgumentException("windowMonths must be positive");
        }
        return asOfDate.plusMonths(windowMonths);
    }

    /** End date for the 3-month product-expiration generation window. */
    public static LocalDate threeMonthWindowEnd(LocalDate asOfDate) {
        return windowEnd(asOfDate, THREE_MONTH_WINDOW);
    }

    /** End date for the 6-month product-expiration generation window. */
    public static LocalDate sixMonthWindowEnd(LocalDate asOfDate) {
        return windowEnd(asOfDate, SIX_MONTH_WINDOW);
    }

    /** End date for the 12-month product-expiration generation window. */
    public static LocalDate twelveMonthWindowEnd(LocalDate asOfDate) {
        return windowEnd(asOfDate, TWELVE_MONTH_WINDOW);
    }

    public static boolean isThreeMonthWindow(int windowMonths) {
        return windowMonths == THREE_MONTH_WINDOW;
    }

    public static boolean isSixMonthWindow(int windowMonths) {
        return windowMonths == SIX_MONTH_WINDOW;
    }

    public static boolean isTwelveMonthWindow(int windowMonths) {
        return windowMonths == TWELVE_MONTH_WINDOW;
    }
}
