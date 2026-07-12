package com.bayerwestphalian.campaign.ai;

import java.util.List;
import java.util.Objects;

/**
 * Fuzzy/weighted customer search response (KB AI-001 / {@code GET /api/ai/customer-search?q=} /
 * item 471 / AiSearchService.customerSearch).
 */
public record AiCustomerSearchView(
        String query, int totalHits, List<AiCustomerSearchHitView> results) {

    public AiCustomerSearchView {
        Objects.requireNonNull(query, "query is required");
        if (totalHits < 0) {
            throw new IllegalArgumentException("totalHits must not be negative");
        }
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static AiCustomerSearchView of(String query, List<AiCustomerSearchHitView> results) {
        List<AiCustomerSearchHitView> safe = results == null ? List.of() : List.copyOf(results);
        return new AiCustomerSearchView(query, safe.size(), safe);
    }
}
