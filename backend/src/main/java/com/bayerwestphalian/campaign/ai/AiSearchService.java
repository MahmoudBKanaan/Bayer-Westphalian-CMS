package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.campaign.ContactEvent;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** KB FR-015 fuzzy customer search with weighted scoring and score explanations. */
@Service
@Transactional(readOnly = true)
public class AiSearchService {

    private static final int MIN_SCORE = 8;

    private final CustomerRepository customerRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final ContactEventRepository contactEventRepository;

    public AiSearchService(
            CustomerRepository customerRepository,
            ProductOwnershipRepository productOwnershipRepository,
            ContactEventRepository contactEventRepository) {
        this.customerRepository = customerRepository;
        this.productOwnershipRepository = productOwnershipRepository;
        this.contactEventRepository = contactEventRepository;
    }

    @PreAuthorize("@authz.canReadCustomers()")
    public AiCustomerSearchView fuzzyCustomerSearch(String query) {
        return weightedSearch(query, AiCustomerSearchRequest.DEFAULT_LIMIT);
    }

    @PreAuthorize("@authz.canReadCustomers()")
    public AiCustomerSearchView weightedSearch(String query, int limit) {
        String normalizedQuery = normalizeQuery(query);
        validateLimit(limit);

        List<AiCustomerSearchHitView> hits =
                customerRepository.findActiveProfiles().stream()
                        .map(customer -> scoreCustomer(customer, normalizedQuery))
                        .filter(result -> result.score() >= MIN_SCORE)
                        .sorted(
                                Comparator.comparingInt(ScoredCustomer::score)
                                        .reversed()
                                        .thenComparing(result -> result.customer().getLastName())
                                        .thenComparing(result -> result.customer().getFirstName()))
                        .limit(limit)
                        .map(ScoredCustomer::toHitView)
                        .toList();
        return AiCustomerSearchView.of(query.trim(), hits);
    }

    @PreAuthorize("@authz.canReadCustomers()")
    public List<ScoreExplanationView> explainScore(Customer customer, String query) {
        return scoreCustomer(customer, normalizeQuery(query)).explanations();
    }

    private ScoredCustomer scoreCustomer(Customer customer, String normalizedQuery) {
        List<FieldScore> fieldScores =
                List.of(
                        fieldScore("full name", customer.getFullName(), normalizedQuery, 45),
                        fieldScore("first name", customer.getFirstName(), normalizedQuery, 28),
                        fieldScore("last name", customer.getLastName(), normalizedQuery, 28),
                        fieldScore("email", customer.getEmail(), normalizedQuery, 24),
                        fieldScore("phone", customer.getPhone(), normalizedQuery, 20),
                        fieldScore("city", customer.getCity(), normalizedQuery, 14),
                        fieldScore("product", productSearchContext(customer), normalizedQuery, 18),
                        fieldScore("notes", notesSearchContext(customer), normalizedQuery, 16),
                        fieldScore("country", customer.getCountry(), normalizedQuery, 12),
                        fieldScore("source", customer.getSource(), normalizedQuery, 10),
                        fieldScore(
                                "customer type",
                                customer.getCustomerType() == null
                                        ? null
                                        : customer.getCustomerType().name(),
                                normalizedQuery,
                                10),
                        fieldScore(
                                "status",
                                customer.getStatus() == null ? null : customer.getStatus().name(),
                                normalizedQuery,
                                8));
        int score = Math.min(100, fieldScores.stream().mapToInt(FieldScore::score).sum());
        List<ScoreExplanationView> explanations =
                fieldScores.stream()
                        .filter(fieldScore -> fieldScore.score() > 0)
                        .map(FieldScore::toExplanation)
                        .toList();
        if (explanations.isEmpty()) {
            explanations =
                    List.of(
                            ScoreExplanationView.of(
                                    "overall",
                                    BigDecimal.ZERO,
                                    BigDecimal.ZERO,
                                    "No meaningful fuzzy customer match"));
        }
        return new ScoredCustomer(customer, score, explanations);
    }

    private String productSearchContext(Customer customer) {
        UUID customerId = customer.getId();
        if (customerId == null) {
            return "";
        }
        return safeList(productOwnershipRepository.findByCustomerId(customerId)).stream()
                .map(ProductOwnership::getProduct)
                .map(AiSearchService::productSearchText)
                .filter(value -> !value.isBlank())
                .distinct()
                .reduce("", AiSearchService::joinSearchText);
    }

    private String notesSearchContext(Customer customer) {
        UUID customerId = customer.getId();
        if (customerId == null) {
            return "";
        }
        return safeList(contactEventRepository.findByCustomerId(customerId)).stream()
                .map(ContactEvent::getNotes)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .reduce("", AiSearchService::joinSearchText);
    }

    private static String productSearchText(Product product) {
        if (product == null) {
            return "";
        }
        String productType =
                product.getProductType() == null ? "" : product.getProductType().name();
        return joinSearchText(
                joinSearchText(product.getName(), productType),
                joinSearchText(product.getDescription(), product.getExpirationPolicy()));
    }

    private static String joinSearchText(String left, String right) {
        if (left == null || left.isBlank()) {
            return right == null ? "" : right.trim();
        }
        if (right == null || right.isBlank()) {
            return left.trim();
        }
        return left.trim() + " " + right.trim();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static FieldScore fieldScore(
            String fieldName, String fieldValue, String normalizedQuery, int weight) {
        String normalizedValue = normalize(fieldValue);
        String displayValue = displayValue(fieldValue);
        if (normalizedValue.isBlank()) {
            return new FieldScore(fieldName, weight, 0, "blank", displayValue);
        }
        if (normalizedValue.equals(normalizedQuery)) {
            return new FieldScore(fieldName, weight, weight, "exact match", displayValue);
        }
        if (normalizedValue.contains(normalizedQuery)) {
            return new FieldScore(
                    fieldName, weight, Math.max(1, weight - 5), "contains query", displayValue);
        }
        if (allTokensMatch(normalizedQuery, normalizedValue)) {
            return new FieldScore(
                    fieldName, weight, Math.max(1, weight - 8), "token match", displayValue);
        }
        double similarity = similarity(normalizedQuery, normalizedValue);
        if (similarity >= 0.72D) {
            return new FieldScore(
                    fieldName,
                    weight,
                    Math.max(1, (int) Math.round(weight * similarity)),
                    "fuzzy match",
                    displayValue);
        }
        return new FieldScore(fieldName, weight, 0, "no match", displayValue);
    }

    private static boolean allTokensMatch(String normalizedQuery, String normalizedValue) {
        String[] tokens = normalizedQuery.split("\\s+");
        for (String token : tokens) {
            if (!normalizedValue.contains(token) && similarity(token, normalizedValue) < 0.72D) {
                return false;
            }
        }
        return tokens.length > 0;
    }

    private static String normalizeQuery(String query) {
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            throw new ValidationException(
                    "AI customer search validation failed",
                    List.of("query must not be blank"));
        }
        return normalized;
    }

    private static void validateLimit(int limit) {
        if (limit < 1) {
            throw new ValidationException(
                    "AI customer search validation failed",
                    List.of("limit must be greater than or equal to 1"));
        }
        if (limit > AiCustomerSearchRequest.MAX_LIMIT) {
            throw new ValidationException(
                    "AI customer search validation failed",
                    List.of(
                            "limit must be less than or equal to "
                                    + AiCustomerSearchRequest.MAX_LIMIT));
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static double similarity(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0D;
        }
        int distance = levenshtein(left, right);
        int longest = Math.max(left.length(), right.length());
        return longest == 0 ? 1D : 1D - ((double) distance / longest);
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) {
            previous[index] = index;
        }
        for (int leftIndex = 1; leftIndex <= left.length(); leftIndex++) {
            current[0] = leftIndex;
            for (int rightIndex = 1; rightIndex <= right.length(); rightIndex++) {
                int cost = left.charAt(leftIndex - 1) == right.charAt(rightIndex - 1) ? 0 : 1;
                current[rightIndex] =
                        Math.min(
                                Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                                previous[rightIndex - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private record FieldScore(
            String fieldName, int weight, int score, String reason, String displayValue) {

        ScoreExplanationView toExplanation() {
            return ScoreExplanationView.of(
                    fieldName,
                    BigDecimal.valueOf(weight),
                    BigDecimal.valueOf(score),
                    reason + " (" + fieldName + ": " + displayValue + ")");
        }
    }

    private record ScoredCustomer(
            Customer customer, int score, List<ScoreExplanationView> explanations) {

        AiCustomerSearchHitView toHitView() {
            return new AiCustomerSearchHitView(
                    customer.getId(),
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getFullName(),
                    customer.getEmail(),
                    customer.getCity(),
                    customer.getCountry(),
                    customer.getCustomerType(),
                    customer.getStatus(),
                    customer.isDoNotContact(),
                    BigDecimal.valueOf(score),
                    explanations);
        }
    }
}
