package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One ranked customer hit from AI fuzzy/weighted search (KB AI-001 / item 471 / item 473–474).
 *
 * <p>{@code score} is a non-negative relevance score. {@code explainScore} lists contributing
 * factors (name, email, city, product, notes, …).
 */
public record AiCustomerSearchHitView(
        UUID customerId,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String city,
        String country,
        CustomerType customerType,
        CustomerStatus status,
        boolean doNotContact,
        BigDecimal score,
        List<ScoreExplanationView> explainScore) {

    public AiCustomerSearchHitView {
        Objects.requireNonNull(customerId, "customerId is required");
        explainScore = explainScore == null ? List.of() : List.copyOf(explainScore);
    }
}
