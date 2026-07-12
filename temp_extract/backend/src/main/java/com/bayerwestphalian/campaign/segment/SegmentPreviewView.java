package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.customer.CustomerView;
import java.util.List;

/**
 * Segment preview payload (KB FR-079 audience size, FR-054 eligible recipients, FR-055 exclusions,
 * BR-006 exclusion reasons, items 198–199). Built by {@code SegmentService.previewSegment} after
 * applying {@code EligibilityService.evaluateForSegmentPreview} to every criteria match.
 *
 * <p>{@code totalAudienceCount} is the number of active customers matching the segment criteria
 * <em>before</em> eligibility filtering.
 *
 * <p>{@code eligibleCount} is the number of those matches that pass {@code EligibilityService}
 * (item 199 — preview returns eligible count).
 *
 * <p>{@code excludedCount} is the number of criteria matches that fail eligibility ({@code
 * totalAudienceCount - eligibleCount}; item 199 — preview returns excluded count). Invariant:
 * {@code eligibleCount + excludedCount == totalAudienceCount}.
 *
 * <p>{@code matchingCustomers} lists only eligibility-filtered (eligible) customers — never the
 * raw criteria set when exclusions apply (preview always applies EligibilityService). Size equals
 * {@code eligibleCount}.
 *
 * <p>{@code exclusionReasonSummary} aggregates how many matches were excluded per stable
 * eligibility reason code from {@code EligibilityService}; counts sum to {@code excludedCount}.
 */
public record SegmentPreviewView(
        int totalAudienceCount,
        int eligibleCount,
        int excludedCount,
        List<CustomerView> matchingCustomers,
        List<SegmentExclusionReasonSummary> exclusionReasonSummary) {

    public SegmentPreviewView {
        if (totalAudienceCount < 0) {
            throw new IllegalArgumentException("totalAudienceCount must not be negative");
        }
        if (eligibleCount < 0) {
            throw new IllegalArgumentException("eligibleCount must not be negative");
        }
        if (excludedCount < 0) {
            throw new IllegalArgumentException("excludedCount must not be negative");
        }
        if (eligibleCount > totalAudienceCount) {
            throw new IllegalArgumentException(
                    "eligibleCount must not exceed totalAudienceCount");
        }
        if (excludedCount > totalAudienceCount) {
            throw new IllegalArgumentException(
                    "excludedCount must not exceed totalAudienceCount");
        }
        if (eligibleCount + excludedCount != totalAudienceCount) {
            throw new IllegalArgumentException(
                    "eligibleCount + excludedCount must equal totalAudienceCount");
        }
        matchingCustomers =
                matchingCustomers == null ? List.of() : List.copyOf(matchingCustomers);
        exclusionReasonSummary =
                exclusionReasonSummary == null
                        ? List.of()
                        : List.copyOf(exclusionReasonSummary);

        int summaryTotal =
                exclusionReasonSummary.stream()
                        .mapToInt(SegmentExclusionReasonSummary::count)
                        .sum();
        if (summaryTotal != excludedCount) {
            throw new IllegalArgumentException(
                    "exclusionReasonSummary counts must sum to excludedCount");
        }
    }

    /**
     * Builds a preview where total and eligible counts both equal the customer list size (all
     * listed customers are treated as eligible; excluded count and summary are empty).
     */
    public static SegmentPreviewView from(List<CustomerView> matchingCustomers) {
        List<CustomerView> customers =
                matchingCustomers == null ? List.of() : List.copyOf(matchingCustomers);
        int count = customers.size();
        return new SegmentPreviewView(count, count, 0, customers, List.of());
    }

    /**
     * Builds a preview with an explicit total audience count; eligible count defaults to the size
     * of the listed customer list and excluded count / summary are derived.
     */
    public static SegmentPreviewView of(
            int totalAudienceCount, List<CustomerView> matchingCustomers) {
        List<CustomerView> customers =
                matchingCustomers == null ? List.of() : List.copyOf(matchingCustomers);
        return of(totalAudienceCount, customers.size(), customers);
    }

    /**
     * Builds a preview with explicit total and eligible counts; excluded count is derived and a
     * placeholder summary is used when exclusions exist but detailed reasons are not provided.
     */
    public static SegmentPreviewView of(
            int totalAudienceCount, int eligibleCount, List<CustomerView> matchingCustomers) {
        int excludedCount = totalAudienceCount - eligibleCount;
        return of(
                totalAudienceCount,
                eligibleCount,
                excludedCount,
                matchingCustomers,
                placeholderSummary(excludedCount));
    }

    /**
     * Builds a preview with total/eligible counts, eligible customer list, and exclusion reason
     * summary. Excluded count is derived as {@code totalAudienceCount - eligibleCount}.
     */
    public static SegmentPreviewView of(
            int totalAudienceCount,
            int eligibleCount,
            List<CustomerView> matchingCustomers,
            List<SegmentExclusionReasonSummary> exclusionReasonSummary) {
        int excludedCount = totalAudienceCount - eligibleCount;
        return new SegmentPreviewView(
                totalAudienceCount,
                eligibleCount,
                excludedCount,
                matchingCustomers,
                exclusionReasonSummary);
    }

    /**
     * Builds a preview with explicit total, eligible, and excluded counts plus the eligible
     * customer list. Uses a placeholder summary when exclusions exist.
     */
    public static SegmentPreviewView of(
            int totalAudienceCount,
            int eligibleCount,
            int excludedCount,
            List<CustomerView> matchingCustomers) {
        return of(
                totalAudienceCount,
                eligibleCount,
                excludedCount,
                matchingCustomers,
                placeholderSummary(excludedCount));
    }

    /**
     * Builds a preview with explicit total, eligible, and excluded counts, eligible customer list,
     * and exclusion reason summary.
     */
    public static SegmentPreviewView of(
            int totalAudienceCount,
            int eligibleCount,
            int excludedCount,
            List<CustomerView> matchingCustomers,
            List<SegmentExclusionReasonSummary> exclusionReasonSummary) {
        return new SegmentPreviewView(
                totalAudienceCount,
                eligibleCount,
                excludedCount,
                matchingCustomers,
                exclusionReasonSummary);
    }

    private static List<SegmentExclusionReasonSummary> placeholderSummary(int excludedCount) {
        if (excludedCount <= 0) {
            return List.of();
        }
        return List.of(
                SegmentExclusionReasonSummary.of(
                        SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_CODE,
                        SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_MESSAGE,
                        excludedCount));
    }
}
