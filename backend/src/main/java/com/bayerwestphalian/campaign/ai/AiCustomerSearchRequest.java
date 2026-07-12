package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

/**
 * AI customer search input (KB AI-001 / {@code GET /api/ai/customer-search?q=} / item 471).
 *
 * <p>Maps the query string used by {@code AiSearchService.fuzzyCustomerSearch} /
 * {@code weightedSearch}.
 */
public record AiCustomerSearchRequest(
        @NotBlank @Size(max = 255) String query, Integer limit) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    public AiCustomerSearchRequest {
        query = StringUtils.hasText(query) ? query.trim() : query;
        if (limit != null && limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (limit != null && limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must not exceed " + MAX_LIMIT);
        }
    }

    /** Effective page size for search services. */
    public int effectiveLimit() {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    public static AiCustomerSearchRequest of(String query) {
        return new AiCustomerSearchRequest(query, null);
    }
}
