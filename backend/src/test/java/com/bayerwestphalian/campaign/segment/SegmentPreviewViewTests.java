package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for FR-079 preview count fields (total / eligible / excluded) and item 199
 * invariants on {@link SegmentPreviewView}.
 */
class SegmentPreviewViewTests {

    private static final Instant NOW = Instant.parse("2026-07-09T10:00:00Z");

    @Test
    void fromSetsEligibleCountAndEmptyExclusionSummary() {
        CustomerView customer = customerView("Lena", "Mueller", "Munich");

        SegmentPreviewView preview = SegmentPreviewView.from(List.of(customer));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.exclusionReasonSummary()).isEmpty();
        assertThat(preview.matchingCustomers()).containsExactly(customer);
    }

    @Test
    void ofAcceptsExplicitExclusionReasonSummary() {
        CustomerView eligible = customerView("Lena", "Mueller", "Munich");
        List<SegmentExclusionReasonSummary> summary =
                List.of(
                        SegmentExclusionReasonSummary.of(
                                EligibilityExclusionReason.DO_NOT_CONTACT.code(),
                                EligibilityExclusionReason.DO_NOT_CONTACT.explanation(),
                                2),
                        SegmentExclusionReasonSummary.of(
                                EligibilityExclusionReason.MARKETING_OPT_OUT.code(),
                                EligibilityExclusionReason.MARKETING_OPT_OUT.explanation(),
                                1));

        SegmentPreviewView preview = SegmentPreviewView.of(4, 1, 3, List.of(eligible), summary);

        assertThat(preview.totalAudienceCount()).isEqualTo(4);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(3);
        assertThat(preview.exclusionReasonSummary()).hasSize(2);
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactly("DO_NOT_CONTACT", "MARKETING_OPT_OUT");
    }

    @Test
    void ofDerivesExcludedCountAndAcceptsSummaryWithoutExplicitExcludedArg() {
        CustomerView eligible = customerView("Lena", "Mueller", "Munich");
        List<SegmentExclusionReasonSummary> summary =
                List.of(
                        SegmentExclusionReasonSummary.of(
                                "INVALID_CONSENT",
                                "Customer does not have valid required consent",
                                2));

        SegmentPreviewView preview = SegmentPreviewView.of(3, 1, List.of(eligible), summary);

        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.exclusionReasonSummary()).hasSize(1);
        assertThat(preview.exclusionReasonSummary().getFirst().count()).isEqualTo(2);
    }

    @Test
    void ofUsesPlaceholderSummaryWhenExclusionsExistWithoutDetailedReasons() {
        CustomerView eligible = customerView("Lena", "Mueller", "Munich");

        SegmentPreviewView preview = SegmentPreviewView.of(3, 1, List.of(eligible));

        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.exclusionReasonSummary()).hasSize(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code())
                .isEqualTo(SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_CODE);
        assertThat(preview.exclusionReasonSummary().getFirst().count()).isEqualTo(2);
    }

    @Test
    void eligiblePlusExcludedAlwaysEqualsTotalForValidViews() {
        CustomerView customer = customerView("Lena", "Mueller", "Munich");

        SegmentPreviewView zero = SegmentPreviewView.of(0, List.of());
        assertThat(zero.eligibleCount() + zero.excludedCount())
                .isEqualTo(zero.totalAudienceCount());
        assertThat(zero.eligibleCount()).isEqualTo(0);
        assertThat(zero.excludedCount()).isEqualTo(0);

        SegmentPreviewView allEligible = SegmentPreviewView.of(2, List.of(customer, customer));
        assertThat(allEligible.eligibleCount()).isEqualTo(2);
        assertThat(allEligible.excludedCount()).isEqualTo(0);
        assertThat(allEligible.eligibleCount() + allEligible.excludedCount())
                .isEqualTo(allEligible.totalAudienceCount());

        SegmentPreviewView mixed =
                SegmentPreviewView.of(
                        5,
                        2,
                        List.of(customer, customer),
                        List.of(
                                SegmentExclusionReasonSummary.of(
                                        "DO_NOT_CONTACT",
                                        "Customer has do-not-contact enabled",
                                        3)));
        assertThat(mixed.totalAudienceCount()).isEqualTo(5);
        assertThat(mixed.eligibleCount()).isEqualTo(2);
        assertThat(mixed.excludedCount()).isEqualTo(3);
        assertThat(mixed.eligibleCount() + mixed.excludedCount())
                .isEqualTo(mixed.totalAudienceCount());
    }

    @Test
    void ofRejectsWhenEligiblePlusExcludedDoesNotEqualTotal() {
        assertThatThrownBy(() -> new SegmentPreviewView(5, 2, 2, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "eligibleCount + excludedCount must equal totalAudienceCount");
    }

    @Test
    void ofRejectsSummaryCountsThatDoNotMatchExcludedCount() {
        assertThatThrownBy(
                        () ->
                                SegmentPreviewView.of(
                                        3,
                                        1,
                                        2,
                                        List.of(),
                                        List.of(
                                                SegmentExclusionReasonSummary.of(
                                                        "DO_NOT_CONTACT",
                                                        "Customer has do-not-contact enabled",
                                                        1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exclusionReasonSummary counts must sum");
    }

    @Test
    void ofRejectsNegativeExcludedCount() {
        assertThatThrownBy(() -> SegmentPreviewView.of(1, 1, -1, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excludedCount");
    }

    @Test
    void ofRejectsInconsistentEligiblePlusExcludedVersusTotal() {
        assertThatThrownBy(
                        () ->
                                SegmentPreviewView.of(
                                        5,
                                        2,
                                        2,
                                        List.of(),
                                        List.of(
                                                SegmentExclusionReasonSummary.of(
                                                        "DO_NOT_CONTACT",
                                                        "Customer has do-not-contact enabled",
                                                        2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eligibleCount + excludedCount must equal");
    }

    @Test
    void exclusionReasonSummaryListIsImmutableCopy() {
        CustomerView customer = customerView("Tom", "Schmidt", "Berlin");
        List<SegmentExclusionReasonSummary> source =
                new java.util.ArrayList<>(
                        List.of(
                                SegmentExclusionReasonSummary.of(
                                        "DO_NOT_CONTACT",
                                        "Customer has do-not-contact enabled",
                                        1)));

        SegmentPreviewView preview = SegmentPreviewView.of(2, 1, 1, List.of(customer), source);
        source.clear();

        assertThat(preview.exclusionReasonSummary()).hasSize(1);
        assertThatThrownBy(
                        () ->
                                preview.exclusionReasonSummary()
                                        .add(
                                                SegmentExclusionReasonSummary.of(
                                                        "MARKETING_OPT_OUT", "opt-out", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static CustomerView customerView(String firstName, String lastName, String city) {
        return new CustomerView(
                UUID.fromString("20000000-0000-0000-0000-000000000201"),
                CustomerType.PROSPECT,
                firstName,
                lastName,
                firstName + " " + lastName,
                firstName.toLowerCase() + "@bayer-westphalian.test",
                null,
                null,
                city,
                "Germany",
                null,
                CustomerAgeGroup.AGE_26_40,
                CustomerStatus.ACTIVE,
                false,
                true,
                true,
                null,
                NOW,
                NOW,
                null);
    }
}
