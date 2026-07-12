package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link ProductExpirationReminderRules} (KB BR-023): 3-, 6-, and 12-month
 * product-expiration windows (items 398–400).
 */
class ProductExpirationReminderRulesTests {

    @Test
    void threeMonthWindowIsThreeMonthsBeforeExpiration() {
        assertThat(ProductExpirationReminderRules.THREE_MONTH_WINDOW).isEqualTo(3);
        assertThat(ProductExpirationReminderRules.isThreeMonthWindow(3)).isTrue();
        assertThat(ProductExpirationReminderRules.isThreeMonthWindow(6)).isFalse();
    }

    @Test
    void threeMonthWindowEndIsAsOfDatePlusThreeMonths() {
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        assertThat(ProductExpirationReminderRules.threeMonthWindowEnd(asOf))
                .isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(ProductExpirationReminderRules.windowEnd(
                        asOf, ProductExpirationReminderRules.THREE_MONTH_WINDOW))
                .isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @Test
    void threeMonthWindowUsesRedReminderLevel() {
        assertThat(ProductExpirationReminderRules.threeMonthReminderLevel())
                .isEqualTo(ReminderLevel.RED);
    }

    @Test
    void sixMonthWindowIsSixMonthsBeforeExpiration() {
        assertThat(ProductExpirationReminderRules.SIX_MONTH_WINDOW).isEqualTo(6);
        assertThat(ProductExpirationReminderRules.isSixMonthWindow(6)).isTrue();
        assertThat(ProductExpirationReminderRules.isSixMonthWindow(3)).isFalse();
        assertThat(ProductExpirationReminderRules.isSixMonthWindow(12)).isFalse();
    }

    @Test
    void sixMonthWindowEndIsAsOfDatePlusSixMonths() {
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        assertThat(ProductExpirationReminderRules.sixMonthWindowEnd(asOf))
                .isEqualTo(LocalDate.of(2027, 1, 11));
        assertThat(ProductExpirationReminderRules.windowEnd(
                        asOf, ProductExpirationReminderRules.SIX_MONTH_WINDOW))
                .isEqualTo(LocalDate.of(2027, 1, 11));
    }

    @Test
    void sixMonthWindowUsesYellowReminderLevel() {
        assertThat(ProductExpirationReminderRules.sixMonthReminderLevel())
                .isEqualTo(ReminderLevel.YELLOW);
    }

    @Test
    void twelveMonthWindowIsTwelveMonthsBeforeExpiration() {
        assertThat(ProductExpirationReminderRules.TWELVE_MONTH_WINDOW).isEqualTo(12);
        assertThat(ProductExpirationReminderRules.isTwelveMonthWindow(12)).isTrue();
        assertThat(ProductExpirationReminderRules.isTwelveMonthWindow(3)).isFalse();
        assertThat(ProductExpirationReminderRules.isTwelveMonthWindow(6)).isFalse();
    }

    @Test
    void twelveMonthWindowEndIsAsOfDatePlusTwelveMonths() {
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        assertThat(ProductExpirationReminderRules.twelveMonthWindowEnd(asOf))
                .isEqualTo(LocalDate.of(2027, 7, 11));
        assertThat(ProductExpirationReminderRules.windowEnd(
                        asOf, ProductExpirationReminderRules.TWELVE_MONTH_WINDOW))
                .isEqualTo(LocalDate.of(2027, 7, 11));
    }

    @Test
    void twelveMonthWindowUsesGreenReminderLevel() {
        assertThat(ProductExpirationReminderRules.twelveMonthReminderLevel())
                .isEqualTo(ReminderLevel.GREEN);
    }

    @Test
    void threeSixAndTwelveMonthWindowsAreDistinct() {
        assertThat(ProductExpirationReminderRules.THREE_MONTH_WINDOW).isEqualTo(3);
        assertThat(ProductExpirationReminderRules.SIX_MONTH_WINDOW).isEqualTo(6);
        assertThat(ProductExpirationReminderRules.TWELVE_MONTH_WINDOW).isEqualTo(12);
        assertThat(ProductExpirationReminderRules.threeMonthReminderLevel())
                .isEqualTo(ReminderLevel.RED);
        assertThat(ProductExpirationReminderRules.sixMonthReminderLevel())
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(ProductExpirationReminderRules.twelveMonthReminderLevel())
                .isEqualTo(ReminderLevel.GREEN);
    }

    @Test
    void windowEndRejectsInvalidInput() {
        assertThatThrownBy(() -> ProductExpirationReminderRules.windowEnd(null, 3))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                ProductExpirationReminderRules.windowEnd(
                                        LocalDate.of(2026, 7, 11), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
