package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.schedule.PaymentReminderLevelRules;
import com.bayerwestphalian.campaign.schedule.ReminderLevel;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KB AI-002/AI-003/AI-004/AI-006 rule-based decision-support recommendations.
 *
 * <p>Item 535: duplicate-contact risk (AI-006 / BR-011) uses the Admin-configured monthly contact
 * limit from {@link SystemSettingsService}.
 */
@Service
@Transactional(readOnly = true)
public class AiRecommendationService {

    private static final int DEFAULT_SEGMENT_EXPIRATION_MONTHS = 6;

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductOwnershipRepository ownershipRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ContactEventRepository contactEventRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final SystemSettingsService systemSettingsService;

    public AiRecommendationService(
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ProductOwnershipRepository ownershipRepository,
            PaymentRecordRepository paymentRecordRepository,
            ContactEventRepository contactEventRepository,
            AiRecommendationRepository aiRecommendationRepository,
            SystemSettingsService systemSettingsService) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ownershipRepository = ownershipRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.contactEventRepository = contactEventRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.systemSettingsService = systemSettingsService;
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')")
    @Transactional
    public SegmentSuggestionView.ListResponse suggestSegments(SegmentSuggestionRequest request) {
        SegmentSuggestionRequest normalized = request == null ? emptySegmentRequest() : request;
        Customer customer =
                normalized.customerId() == null ? null : findCustomer(normalized.customerId());
        List<ProductOwnership> ownerships =
                customer == null
                        ? List.of()
                        : ownershipRepository.findByCustomerId(customer.getId());
        List<PaymentRecord> payments =
                customer == null
                        ? List.of()
                        : paymentRecordRepository.findByCustomerId(customer.getId());

        List<SegmentSuggestionView> suggestions =
                Stream.of(
                                locationSuggestion(normalized, customer),
                                profileSuggestion(customer),
                                productSuggestion(normalized, ownerships),
                                paymentHistorySuggestion(payments),
                                expirationSuggestion(normalized))
                        .filter(Objects::nonNull)
                        .toList();
        return new SegmentSuggestionView.ListResponse(suggestions);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')")
    @Transactional
    public ProductRecommendationView.ListResponse recommendProducts(
            ProductRecommendationRequest request) {
        validateCustomerRequest(request == null ? null : request.customerId());
        Customer customer = findCustomer(request.customerId());
        List<ProductOwnership> ownerships = ownershipRepository.findByCustomerId(customer.getId());
        Set<UUID> ownedProductIds =
                ownerships.stream()
                        .filter(ProductOwnership::isActive)
                        .map(ProductOwnership::getProduct)
                        .filter(Objects::nonNull)
                        .map(Product::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Set<ProductType> ownedTypes =
                ownerships.stream()
                        .filter(ProductOwnership::isActive)
                        .map(ProductOwnership::getProduct)
                        .filter(Objects::nonNull)
                        .map(Product::getProductType)
                        .collect(
                                Collectors.toCollection(() -> EnumSet.noneOf(ProductType.class)));

        List<ProductRecommendationView> recommendations =
                productRepository.findActive().stream()
                        .filter(product -> product.getId() != null)
                        .filter(product -> !ownedProductIds.contains(product.getId()))
                        .map(product -> productRecommendation(customer, product, ownedTypes))
                        .sorted(
                                Comparator.comparing(ProductRecommendationView::confidenceScore)
                                        .reversed()
                                        .thenComparing(ProductRecommendationView::productName))
                        .limit(5)
                        .toList();
        return new ProductRecommendationView.ListResponse(customer.getId(), recommendations);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public DefaultRiskScoreView calculateDefaultRisk(DefaultRiskScoreRequest request) {
        validateCustomerRequest(request == null ? null : request.customerId());
        Customer customer = findCustomer(request.customerId());
        List<PaymentRecord> payments = paymentRecordRepository.findByCustomerId(customer.getId());

        RiskScoreBreakdown breakdown = riskScoreBreakdown(payments);
        BigDecimal score = BigDecimal.valueOf(breakdown.score());
        String riskLevel = riskLevel(score);
        List<ScoreExplanationView> factors =
                List.of(
                        ScoreExplanationView.of(
                                "missed payments",
                                BigDecimal.valueOf(25),
                                BigDecimal.valueOf(breakdown.missedPaymentContribution()),
                                breakdown.missedPayments()
                                        + " unpaid overdue/default-risk payment(s)"),
                        ScoreExplanationView.of(
                                "default risk payments",
                                BigDecimal.valueOf(25),
                                BigDecimal.valueOf(breakdown.defaultRiskContribution()),
                                breakdown.defaultRiskPayments()
                                        + " payment(s) already marked default risk"),
                        ScoreExplanationView.of(
                                "overdue days",
                                BigDecimal.valueOf(20),
                                BigDecimal.valueOf(breakdown.overdueDaysContribution()),
                                "Maximum overdue age is "
                                        + breakdown.maxOverdueDays()
                                        + " day(s)"),
                        ScoreExplanationView.of(
                                "reminder escalation",
                                BigDecimal.valueOf(20),
                                BigDecimal.valueOf(breakdown.reminderContribution()),
                                "Green="
                                        + breakdown.greenReminders()
                                        + ", Yellow="
                                        + breakdown.yellowReminders()
                                        + ", Red="
                                        + breakdown.redReminders()
                                        + " reminder level(s)"),
                        ScoreExplanationView.of(
                                "paid history",
                                BigDecimal.valueOf(10),
                                BigDecimal.valueOf(breakdown.paidHistoryContribution()),
                                breakdown.paidPayments() + " paid payment(s) reduce risk"));
        String explanation =
                "Default-risk score is "
                        + score
                        + " ("
                        + riskLevel
                        + ") from missed payments, overdue days, reminder escalation, "
                        + "and payment history.";
        AiRecommendation stored =
                store(
                        AiRecommendationType.RISK,
                        "customer",
                        customer.getId(),
                        "customerId=" + customer.getId(),
                        "Default risk level: " + riskLevel,
                        explanation,
                        score);
        return new DefaultRiskScoreView(
                customer.getId(), score, riskLevel, explanation, factors, stored.getId());
    }

    private static RiskScoreBreakdown riskScoreBreakdown(List<PaymentRecord> payments) {
        List<PaymentRecord> customerPayments = payments == null ? List.of() : payments;
        long missedPayments =
                customerPayments.stream()
                        .filter(AiRecommendationService::isMissedPayment)
                        .count();
        long defaultRiskCount =
                customerPayments.stream().filter(PaymentRecord::isDefaultRisk).count();
        long maxOverdueDays =
                customerPayments.stream()
                        .mapToLong(PaymentRecord::calculateDaysOverdue)
                        .max()
                        .orElse(0);
        long paidCount =
                customerPayments.stream()
                        .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                        .count();
        long greenReminders =
                customerPayments.stream()
                        .filter(AiRecommendationService::isUnpaidPayment)
                        .filter(
                                payment ->
                                        PaymentReminderLevelRules.resolve(payment)
                                                == ReminderLevel.GREEN)
                        .count();
        long yellowReminders =
                customerPayments.stream()
                        .filter(AiRecommendationService::isUnpaidPayment)
                        .filter(
                                payment ->
                                        PaymentReminderLevelRules.resolve(payment)
                                                == ReminderLevel.YELLOW)
                        .count();
        long redReminders =
                customerPayments.stream()
                        .filter(AiRecommendationService::isUnpaidPayment)
                        .filter(
                                payment ->
                                        PaymentReminderLevelRules.resolve(payment)
                                                == ReminderLevel.RED)
                        .count();

        long missedPaymentContribution = Math.min(25, missedPayments * 12);
        long defaultRiskContribution = Math.min(25, defaultRiskCount * 25);
        long overdueDaysContribution = Math.min(20, maxOverdueDays / 4);
        long reminderContribution =
                Math.min(20, (greenReminders * 2) + (yellowReminders * 6) + (redReminders * 10));
        long paidHistoryContribution = -Math.min(10, paidCount * 2);
        long rawScore =
                missedPaymentContribution
                        + defaultRiskContribution
                        + overdueDaysContribution
                        + reminderContribution
                        + paidHistoryContribution;
        long score = Math.max(0, Math.min(100, rawScore));
        return new RiskScoreBreakdown(
                score,
                missedPayments,
                defaultRiskCount,
                maxOverdueDays,
                paidCount,
                greenReminders,
                yellowReminders,
                redReminders,
                missedPaymentContribution,
                defaultRiskContribution,
                overdueDaysContribution,
                reminderContribution,
                paidHistoryContribution);
    }

    private static boolean isMissedPayment(PaymentRecord payment) {
        return payment.getStatus() == PaymentStatus.OVERDUE
                || payment.getStatus() == PaymentStatus.DEFAULT_RISK
                || (payment.getStatus() == PaymentStatus.DUE && payment.calculateDaysOverdue() > 0);
    }

    private static boolean isUnpaidPayment(PaymentRecord payment) {
        return payment.getStatus() != PaymentStatus.PAID;
    }

    @PreAuthorize("@authz.canReadCustomers()")
    @Transactional
    public DuplicateContactRiskView detectDuplicateRisk(DuplicateContactRiskRequest request) {
        validateCustomerRequest(request == null ? null : request.customerId());
        Customer customer = findCustomer(request.customerId());
        Instant windowStart = Instant.now().minus(30, ChronoUnit.DAYS);
        int contactsInWindow =
                Math.toIntExact(
                        contactEventRepository.countRecentCustomerMarketingContacts(
                                customer.getId(), windowStart));
        boolean sameCampaignContacted =
                request.campaignId() != null
                        && contactEventRepository.findByCustomerId(customer.getId()).stream()
                                .anyMatch(
                                        event ->
                                                request.campaignId().equals(event.getCampaignId())
                                                        && isMarketingContactAttempt(
                                                                event.getEventType()));
        int monthlyContactLimit = systemSettingsService.monthlyContactLimit();
        boolean riskDetected =
                contactsInWindow >= monthlyContactLimit || sameCampaignContacted;
        String warning =
                duplicateRiskWarning(contactsInWindow, sameCampaignContacted, monthlyContactLimit);
        String explanation =
                duplicateRiskExplanation(
                        contactsInWindow,
                        sameCampaignContacted,
                        request.campaignId(),
                        monthlyContactLimit);
        AiRecommendation stored =
                store(
                        AiRecommendationType.DUPLICATE_WARNING,
                        "customer",
                        customer.getId(),
                        "customerId=" + customer.getId() + ", campaignId=" + request.campaignId(),
                        warning,
                        explanation,
                        duplicateRiskConfidence(
                                contactsInWindow, sameCampaignContacted, monthlyContactLimit));
        return new DuplicateContactRiskView(
                customer.getId(),
                request.campaignId(),
                riskDetected,
                warning,
                explanation,
                contactsInWindow,
                monthlyContactLimit,
                sameCampaignContacted,
                stored.getId());
    }

    private static boolean isMarketingContactAttempt(ContactEventType eventType) {
        return eventType == ContactEventType.SENT || eventType == ContactEventType.CALLED;
    }

    private static String duplicateRiskWarning(
            int contactsInWindow, boolean sameCampaignContacted, int monthlyContactLimit) {
        if (sameCampaignContacted && contactsInWindow >= monthlyContactLimit) {
            return "Duplicate-contact risk detected: BR-010 same campaign and BR-011 monthly limit";
        }
        if (sameCampaignContacted) {
            return "Duplicate-contact risk detected: BR-010 same campaign";
        }
        if (contactsInWindow >= monthlyContactLimit) {
            return "Duplicate-contact risk detected: BR-011 monthly contact limit";
        }
        return "No duplicate-contact risk detected";
    }

    private static String duplicateRiskExplanation(
            int contactsInWindow,
            boolean sameCampaignContacted,
            UUID campaignId,
            int monthlyContactLimit) {
        String campaignDetail =
                campaignId == null
                        ? "no campaign id supplied"
                        : "campaign " + campaignId + " contacted=" + sameCampaignContacted;
        return contactsInWindow
                + " marketing contact attempt(s) in the last 30 days; monthly limit="
                + monthlyContactLimit
                + "; "
                + campaignDetail
                + ". AI warning only; eligibility rules remain authoritative.";
    }

    private static BigDecimal duplicateRiskConfidence(
            int contactsInWindow, boolean sameCampaignContacted, int monthlyContactLimit) {
        if (sameCampaignContacted && contactsInWindow >= monthlyContactLimit) {
            return BigDecimal.valueOf(95);
        }
        if (sameCampaignContacted) {
            return BigDecimal.valueOf(88);
        }
        if (contactsInWindow >= monthlyContactLimit) {
            return BigDecimal.valueOf(82);
        }
        return BigDecimal.valueOf(10);
    }

    private SegmentSuggestionView locationSuggestion(
            SegmentSuggestionRequest request, Customer customer) {
        String city = firstText(request.city(), customer == null ? null : customer.getCity());
        String country =
                firstText(request.country(), customer == null ? null : customer.getCountry());
        if (city == null && country == null) {
            return null;
        }
        ArrayList<SuggestedSegmentCriterion> structured = new ArrayList<>();
        if (city != null) {
            structured.add(SuggestedSegmentCriterion.equals("city", city));
        }
        if (country != null) {
            structured.add(SuggestedSegmentCriterion.equals("country", country));
        }
        String name = (city != null ? city : country) + " audience";
        String inputSummary =
                structured.stream()
                        .map(SuggestedSegmentCriterion::toSummary)
                        .collect(Collectors.joining("; "));
        AiRecommendation stored =
                store(
                        AiRecommendationType.SEGMENT,
                        "segment",
                        null,
                        inputSummary,
                        name,
                        segmentExplanation("customer/location signal: " + inputSummary),
                        BigDecimal.valueOf(72));
        return new SegmentSuggestionView(
                name,
                "Customers matching a strong location signal.",
                structured,
                null,
                stored.getExplanation(),
                stored.getConfidenceScore(),
                stored.getId());
    }

    private SegmentSuggestionView profileSuggestion(Customer customer) {
        if (customer == null) {
            return null;
        }
        ArrayList<SuggestedSegmentCriterion> structured = new ArrayList<>();
        structured.add(
                SuggestedSegmentCriterion.equals(
                        "customer_type", customer.getCustomerType().name()));
        if (customer.getAgeGroup() != null) {
            structured.add(
                    SuggestedSegmentCriterion.equals(
                            "age_group", customer.getAgeGroup().getDatabaseValue()));
        }
        if (customer.getStatus() != null) {
            structured.add(
                    SuggestedSegmentCriterion.equals("status", customer.getStatus().name()));
        }
        if (customer.getSource() != null && !customer.getSource().isBlank()) {
            structured.add(
                    SuggestedSegmentCriterion.contains("source", customer.getSource().trim()));
        }

        String name = customer.getCustomerType().name() + " profile audience";
        String inputSummary =
                structured.stream()
                        .map(SuggestedSegmentCriterion::toSummary)
                        .collect(Collectors.joining("; "));
        AiRecommendation stored =
                store(
                        AiRecommendationType.SEGMENT,
                        "segment",
                        null,
                        inputSummary,
                        name,
                        segmentExplanation(
                                "customer profile, age, status, and behavior/source signals."),
                        BigDecimal.valueOf(76));
        return new SegmentSuggestionView(
                name,
                "Customers matching profile and behavior/status criteria.",
                structured,
                null,
                stored.getExplanation(),
                stored.getConfidenceScore(),
                stored.getId());
    }

    private SegmentSuggestionView productSuggestion(
            SegmentSuggestionRequest request, List<ProductOwnership> ownerships) {
        String productType = firstText(request.productTypeHint(), dominantProductType(ownerships));
        if (productType == null) {
            return null;
        }
        List<SuggestedSegmentCriterion> structured =
                List.of(SuggestedSegmentCriterion.equals("product_type", productType));
        AiRecommendation stored =
                store(
                        AiRecommendationType.SEGMENT,
                        "segment",
                        null,
                        "product_type EQUALS " + productType,
                        productType + " owners",
                        segmentExplanation("product ownership/type signal: " + productType),
                        BigDecimal.valueOf(78));
        return new SegmentSuggestionView(
                productType + " owners",
                "Customers or prospects associated with the product type.",
                structured,
                null,
                stored.getExplanation(),
                stored.getConfidenceScore(),
                stored.getId());
    }

    private SegmentSuggestionView paymentHistorySuggestion(List<PaymentRecord> payments) {
        List<PaymentRecord> customerPayments = payments == null ? List.of() : payments;
        if (customerPayments.isEmpty()) {
            return null;
        }
        boolean hasDefaultRisk =
                customerPayments.stream().anyMatch(PaymentRecord::isDefaultRisk);
        boolean hasOverdue =
                customerPayments.stream()
                        .anyMatch(payment -> payment.getStatus() == PaymentStatus.OVERDUE);
        int maxReminderCount =
                customerPayments.stream().mapToInt(PaymentRecord::getReminderCount).max().orElse(0);
        long maxDaysOverdue =
                customerPayments.stream()
                        .mapToLong(PaymentRecord::calculateDaysOverdue)
                        .max()
                        .orElse(0);
        if (!hasDefaultRisk && !hasOverdue && maxReminderCount == 0 && maxDaysOverdue == 0) {
            return null;
        }

        ArrayList<SuggestedSegmentCriterion> structured = new ArrayList<>();
        if (hasDefaultRisk) {
            structured.add(SuggestedSegmentCriterion.equals("default_risk", "true"));
        } else if (hasOverdue) {
            structured.add(SuggestedSegmentCriterion.equals("payment_status", "OVERDUE"));
        }
        if (maxReminderCount > 0) {
            structured.add(
                    new SuggestedSegmentCriterion(
                            "reminder_count", "GREATER_THAN", "0", null, "AND"));
        }
        if (maxDaysOverdue > 0) {
            structured.add(
                    new SuggestedSegmentCriterion(
                            "days_overdue", "GREATER_THAN", "0", null, "AND"));
        }

        String name = hasDefaultRisk ? "Default-risk payment audience" : "Overdue payment audience";
        String inputSummary =
                structured.stream()
                        .map(SuggestedSegmentCriterion::toSummary)
                        .collect(Collectors.joining("; "));
        AiRecommendation stored =
                store(
                        AiRecommendationType.SEGMENT,
                        "segment",
                        null,
                        inputSummary,
                        name,
                        segmentExplanation(
                                "payment history, reminders, overdue days, and default-risk signals."),
                        BigDecimal.valueOf(hasDefaultRisk ? 88 : 82));
        return new SegmentSuggestionView(
                name,
                "Customers matching payment-history risk criteria.",
                structured,
                null,
                stored.getExplanation(),
                stored.getConfidenceScore(),
                stored.getId());
    }

    private SegmentSuggestionView expirationSuggestion(SegmentSuggestionRequest request) {
        int months =
                request.expirationWithinMonths() == null
                        ? DEFAULT_SEGMENT_EXPIRATION_MONTHS
                        : request.expirationWithinMonths();
        if (months < 1) {
            throw new ValidationException(
                    "AI segment suggestion validation failed",
                    List.of("expirationWithinMonths must be greater than or equal to 1"));
        }
        List<SuggestedSegmentCriterion> structured =
                List.of(
                        SuggestedSegmentCriterion.equals(
                                "expiring_within_months", String.valueOf(months)));
        AiRecommendation stored =
                store(
                        AiRecommendationType.SEGMENT,
                        "segment",
                        null,
                        "expiring_within_months EQUALS " + months,
                        months + "-month expiration audience",
                        segmentExplanation(
                                "renewal/expiration campaign rule: expiring_within_months EQUALS "
                                        + months),
                        BigDecimal.valueOf(70));
        return new SegmentSuggestionView(
                months + "-month expiration audience",
                "Customers with products approaching expiration.",
                structured,
                null,
                stored.getExplanation(),
                stored.getConfidenceScore(),
                stored.getId());
    }

    private static String segmentExplanation(String source) {
        return "AI-002 rule-based segment suggestion for human decision support from " + source;
    }

    private ProductRecommendationView productRecommendation(
            Customer customer, Product product, Set<ProductType> ownedTypes) {
        ProductRuleResult ruleResult = productRuleResult(customer, product, ownedTypes);
        BigDecimal confidence = BigDecimal.valueOf(ruleResult.score());
        String recommendation =
                "Recommend " + product.getName() + " for " + customer.getFullName();
        String explanation =
                "AI-003 rule-based product recommendation for human decision support from customer type "
                        + customer.getCustomerType()
                        + ", profile signals "
                        + ruleResult.signals()
                        + ", owned product types "
                        + ownedTypes
                        + ".";
        AiRecommendation stored =
                customer.isDoNotContact()
                        ? null
                        : store(
                                AiRecommendationType.PRODUCT,
                                "customer",
                                customer.getId(),
                                "customerId="
                                        + customer.getId()
                                        + ", productId="
                                        + product.getId(),
                                recommendation,
                                explanation,
                                confidence);
        return new ProductRecommendationView(
                product.getId(),
                product.getName(),
                product.getProductType(),
                recommendation,
                explanation,
                confidence,
                stored == null ? null : stored.getId());
    }

    private static ProductRuleResult productRuleResult(
            Customer customer, Product product, Set<ProductType> ownedTypes) {
        int score = 40;
        ArrayList<String> signals = new ArrayList<>();
        signals.add("active product availability");
        if (!ownedTypes.contains(product.getProductType())) {
            score += 10;
            signals.add("not currently owned product type");
        }
        if (customer.getCustomerType() == CustomerType.PROSPECT) {
            score += 8;
            signals.add("prospect acquisition profile");
        }
        if (customer.getCustomerType() == CustomerType.BENEFICIARY
                || containsIgnoreCase(customer.getSource(), "BENEFICIARY")) {
            if (product.getProductType() == ProductType.LIFE_INSURANCE) {
                score += 20;
                signals.add("beneficiary life-insurance fit");
            } else if (product.getProductType() == ProductType.INVESTMENT_FUND) {
                score += 16;
                signals.add("beneficiary investment cross-sell");
            } else if (product.getProductType() == ProductType.HEALTH_INSURANCE) {
                score += 10;
                signals.add("beneficiary health coverage fit");
            }
        }
        if (customer.getCustomerType() == CustomerType.CUSTOMER
                && Set.of(
                                ProductType.HOMEOWNER_INSURANCE,
                                ProductType.AUTO_INSURANCE,
                                ProductType.HEALTH_INSURANCE)
                        .contains(product.getProductType())) {
            score += 10;
            signals.add("existing customer cross-sell");
        }
        score += ageGroupScore(customer.getAgeGroup(), product.getProductType(), signals);
        score += ownershipComplementScore(ownedTypes, product.getProductType(), signals);
        if (product.getPrice() != null
                && product.getPrice().compareTo(BigDecimal.valueOf(100)) <= 0) {
            score += 3;
            signals.add("accessible price point");
        }
        if (product.getDurationMonths() != null && product.getDurationMonths() <= 12) {
            score += 2;
            signals.add("short product duration");
        }
        return new ProductRuleResult(Math.min(95, score), List.copyOf(signals));
    }

    private static int ageGroupScore(
            CustomerAgeGroup ageGroup, ProductType productType, List<String> signals) {
        if (ageGroup == null) {
            return 0;
        }
        if (ageGroup == CustomerAgeGroup.MINOR
                && (productType == ProductType.LIFE_INSURANCE
                        || productType == ProductType.HEALTH_INSURANCE)) {
            signals.add("minor protection profile");
            return 12;
        }
        if (ageGroup == CustomerAgeGroup.AGE_18_25
                && (productType == ProductType.AUTO_INSURANCE
                        || productType == ProductType.INVESTMENT_FUND)) {
            signals.add("young adult mobility/investment profile");
            return 10;
        }
        if (ageGroup == CustomerAgeGroup.AGE_26_40
                && (productType == ProductType.HOMEOWNER_INSURANCE
                        || productType == ProductType.LIFE_INSURANCE)) {
            signals.add("family/home-building profile");
            return 12;
        }
        if (ageGroup == CustomerAgeGroup.AGE_41_60
                && (productType == ProductType.LIFE_INSURANCE
                        || productType == ProductType.HEALTH_INSURANCE
                        || productType == ProductType.INVESTMENT_FUND)) {
            signals.add("mid-life protection and wealth profile");
            return 12;
        }
        if (ageGroup == CustomerAgeGroup.AGE_60_PLUS
                && (productType == ProductType.HEALTH_INSURANCE
                        || productType == ProductType.LIFE_INSURANCE)) {
            signals.add("senior protection profile");
            return 12;
        }
        return 0;
    }

    private static int ownershipComplementScore(
            Set<ProductType> ownedTypes, ProductType candidateType, List<String> signals) {
        if (ownedTypes.contains(ProductType.LIFE_INSURANCE)
                && candidateType == ProductType.INVESTMENT_FUND) {
            signals.add("owned LIFE_INSURANCE complements INVESTMENT_FUND");
            return 14;
        }
        if (ownedTypes.contains(ProductType.LIFE_INSURANCE)
                && candidateType == ProductType.HEALTH_INSURANCE) {
            signals.add("owned LIFE_INSURANCE complements HEALTH_INSURANCE");
            return 10;
        }
        if (ownedTypes.contains(ProductType.HOMEOWNER_INSURANCE)
                && candidateType == ProductType.AUTO_INSURANCE) {
            signals.add("owned HOMEOWNER_INSURANCE complements AUTO_INSURANCE");
            return 12;
        }
        if (ownedTypes.contains(ProductType.AUTO_INSURANCE)
                && candidateType == ProductType.HOMEOWNER_INSURANCE) {
            signals.add("owned AUTO_INSURANCE complements HOMEOWNER_INSURANCE");
            return 12;
        }
        if (ownedTypes.contains(ProductType.HEALTH_INSURANCE)
                && candidateType == ProductType.LIFE_INSURANCE) {
            signals.add("owned HEALTH_INSURANCE complements LIFE_INSURANCE");
            return 8;
        }
        return 0;
    }

    private static boolean containsIgnoreCase(String value, String expected) {
        return value != null
                && expected != null
                && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private static String dominantProductType(List<ProductOwnership> ownerships) {
        return ownerships.stream()
                .map(ProductOwnership::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getProductType)
                .filter(Objects::nonNull)
                .findFirst()
                .map(Enum::name)
                .orElse(null);
    }

    private static String riskLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "HIGH";
        }
        if (score.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private AiRecommendation store(
            AiRecommendationType type,
            String targetEntityType,
            UUID targetEntityId,
            String inputSummary,
            String recommendation,
            String explanation,
            BigDecimal confidenceScore) {
        return aiRecommendationRepository.save(
                AiRecommendation.create(
                        type,
                        targetEntityType,
                        targetEntityId,
                        inputSummary,
                        recommendation,
                        explanation,
                        confidenceScore));
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private static SegmentSuggestionRequest emptySegmentRequest() {
        return new SegmentSuggestionRequest(null, null, null, null, null);
    }

    private static void validateCustomerRequest(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException(
                    "AI recommendation validation failed", List.of("customerId is required"));
        }
    }

    private static String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim().replaceAll("\\s+", " ");
        }
        if (second != null && !second.isBlank()) {
            return second.trim().replaceAll("\\s+", " ");
        }
        return null;
    }

    private record RiskScoreBreakdown(
            long score,
            long missedPayments,
            long defaultRiskPayments,
            long maxOverdueDays,
            long paidPayments,
            long greenReminders,
            long yellowReminders,
            long redReminders,
            long missedPaymentContribution,
            long defaultRiskContribution,
            long overdueDaysContribution,
            long reminderContribution,
            long paidHistoryContribution) {}

    private record ProductRuleResult(int score, List<String> signals) {}
}
