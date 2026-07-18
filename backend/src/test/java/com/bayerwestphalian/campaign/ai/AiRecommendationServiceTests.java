package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CommunicationChannel;
import com.bayerwestphalian.campaign.campaign.ContactEvent;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
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
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** KB item 475: Implement AiRecommendationService for AI-002, AI-003, AI-004, and AI-006. */
@DisplayName("475 Implement AiRecommendationService")
class AiRecommendationServiceTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000475");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000475");
    private static final UUID OWNED_PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000476");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000475");

    private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
    private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);
    private final ProductOwnershipRepository ownershipRepository =
            Mockito.mock(ProductOwnershipRepository.class);
    private final PaymentRecordRepository paymentRecordRepository =
            Mockito.mock(PaymentRecordRepository.class);
    private final ContactEventRepository contactEventRepository =
            Mockito.mock(ContactEventRepository.class);
    private final AiRecommendationRepository aiRecommendationRepository =
            Mockito.mock(AiRecommendationRepository.class);
    private final SystemSettingsService systemSettingsService =
            Mockito.mock(SystemSettingsService.class);
    private final AiRecommendationService service =
            new AiRecommendationService(
                    customerRepository,
                    productRepository,
                    ownershipRepository,
                    paymentRecordRepository,
                    contactEventRepository,
                    aiRecommendationRepository,
                    systemSettingsService);

    {
        lenient().when(systemSettingsService.monthlyContactLimit()).thenReturn(3);
    }

    @Test
    void declaresKbServiceContractAndAuthorization() throws Exception {
        assertThat(AiRecommendationService.class.getAnnotation(Service.class)).isNotNull();
        Transactional transactional =
                AiRecommendationService.class.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();

        assertMethodAuthorization(
                "recommendProducts",
                new Class<?>[] {ProductRecommendationRequest.class},
                "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')");
        assertMethodAuthorization(
                "suggestSegments",
                new Class<?>[] {SegmentSuggestionRequest.class},
                "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')");
        assertMethodAuthorization(
                "calculateDefaultRisk",
                new Class<?>[] {DefaultRiskScoreRequest.class},
                "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')");
        assertMethodAuthorization(
                "detectDuplicateRisk",
                new Class<?>[] {DuplicateContactRiskRequest.class},
                "@authz.canReadCustomers()");
    }

    @Test
    void recommendProductsExcludesOwnedProductsAndStoresRecommendation() {
        Customer customer = customer();
        Product owned = product(OWNED_PRODUCT_ID, "Life Protect", ProductType.LIFE_INSURANCE);
        Product recommended = product(PRODUCT_ID, "Health Plus", ProductType.HEALTH_INSURANCE);
        ProductOwnership ownership = ownership(customer, owned);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(ownership));
        when(productRepository.findActive()).thenReturn(List.of(owned, recommended));
        stubRecommendationSave();

        ProductRecommendationView.ListResponse response =
                service.recommendProducts(new ProductRecommendationRequest(CUSTOMER_ID));

        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(response.recommendations())
                .singleElement()
                .satisfies(
                        recommendation -> {
                            assertThat(recommendation.productId()).isEqualTo(PRODUCT_ID);
                            assertThat(recommendation.productType())
                                    .isEqualTo(ProductType.HEALTH_INSURANCE);
                            assertThat(recommendation.recommendation())
                                    .contains("Health Plus", "Ada Lovelace");
                            assertThat(recommendation.explanation())
                                    .contains("customer type", "owned product types");
                            assertThat(recommendation.confidenceScore())
                                    .isGreaterThan(BigDecimal.ZERO);
                            assertThat(recommendation.storedRecommendationId()).isNotNull();
                        });
        verify(aiRecommendationRepository).save(any(AiRecommendation.class));
    }

    @Test
    void recommendProductsRanksByProfileAndOwnedProductRules() {
        Customer customer = customer();
        customer.recordSource("LIFE_INSURANCE_BENEFICIARY");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_41_60);
        Product owned = product(OWNED_PRODUCT_ID, "Life Protect", ProductType.LIFE_INSURANCE);
        Product investment =
                product(
                        PRODUCT_ID,
                        "Wealth Builder",
                        ProductType.INVESTMENT_FUND,
                        BigDecimal.valueOf(99),
                        12);
        Product health =
                product(
                        UUID.fromString("40000000-0000-0000-0000-000000000477"),
                        "Health Plus",
                        ProductType.HEALTH_INSURANCE,
                        BigDecimal.valueOf(250),
                        24);
        Product auto =
                product(
                        UUID.fromString("40000000-0000-0000-0000-000000000478"),
                        "Auto Secure",
                        ProductType.AUTO_INSURANCE,
                        BigDecimal.valueOf(90),
                        12);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(ownership(customer, owned)));
        when(productRepository.findActive()).thenReturn(List.of(auto, health, investment, owned));
        stubRecommendationSave();

        ProductRecommendationView.ListResponse response =
                service.recommendProducts(new ProductRecommendationRequest(CUSTOMER_ID));

        assertThat(response.recommendations())
                .extracting(ProductRecommendationView::productName)
                .containsExactly("Wealth Builder", "Health Plus", "Auto Secure");
        ProductRecommendationView topRecommendation = response.recommendations().get(0);
        assertThat(topRecommendation.confidenceScore()).isEqualByComparingTo("95");
        assertThat(topRecommendation.explanation())
                .contains(
                        "beneficiary investment cross-sell",
                        "owned LIFE_INSURANCE complements INVESTMENT_FUND",
                        "mid-life protection and wealth profile");
        assertThat(topRecommendation.recommendation())
                .isEqualTo("Recommend Wealth Builder for Ada Lovelace");
    }

    @Test
    @DisplayName("496 Product recommendation returns explanation")
    void recommendProductsReturnsHumanReadableExplanationAndStoresIt() {
        Customer customer = customer();
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        Product owned = product(OWNED_PRODUCT_ID, "Life Protect", ProductType.LIFE_INSURANCE);
        Product recommended = product(PRODUCT_ID, "Home Shield", ProductType.HOMEOWNER_INSURANCE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(ownership(customer, owned)));
        when(productRepository.findActive()).thenReturn(List.of(owned, recommended));
        stubRecommendationSave();

        ProductRecommendationView.ListResponse response =
                service.recommendProducts(new ProductRecommendationRequest(CUSTOMER_ID));

        ProductRecommendationView recommendation = response.recommendations().get(0);
        assertThat(recommendation.explanation())
                .contains(
                        "AI-003 rule-based product recommendation",
                        "human decision support",
                        "customer type PROSPECT",
                        "profile signals",
                        "owned product types",
                        "not currently owned product type");
        assertThat(recommendation.explanation()).isNotBlank();
        ArgumentCaptor<AiRecommendation> storedRecommendation =
                ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(storedRecommendation.capture());
        assertThat(storedRecommendation.getValue().getRecommendation())
                .isEqualTo(recommendation.recommendation());
        assertThat(storedRecommendation.getValue().getExplanation())
                .isEqualTo(recommendation.explanation());
    }

    @Test
    @DisplayName("505 AI recommendation is stored with explanation")
    void aiRecommendationIsStoredWithExplanation() {
        Customer customer = customer();
        Product owned = product(OWNED_PRODUCT_ID, "Life Protect", ProductType.LIFE_INSURANCE);
        Product recommended = product(PRODUCT_ID, "Home Shield", ProductType.HOMEOWNER_INSURANCE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(ownership(customer, owned)));
        when(productRepository.findActive()).thenReturn(List.of(owned, recommended));
        stubRecommendationSave();

        ProductRecommendationView.ListResponse response =
                service.recommendProducts(new ProductRecommendationRequest(CUSTOMER_ID));

        ProductRecommendationView recommendation = response.recommendations().getFirst();
        ArgumentCaptor<AiRecommendation> storedRecommendation =
                ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(storedRecommendation.capture());
        assertThat(recommendation.explanation()).isNotBlank();
        assertThat(storedRecommendation.getValue().getExplanation())
                .isEqualTo(recommendation.explanation());
        assertThat(storedRecommendation.getValue().getExplanation())
                .contains("human decision support");
    }

    @Test
    void suggestSegmentsUsesRequestAndOwnershipSignals() {
        Customer customer = customer();
        Product product = product(OWNED_PRODUCT_ID, "Life Protect", ProductType.LIFE_INSURANCE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(ownership(customer, product)));
        stubRecommendationSave();

        SegmentSuggestionView.ListResponse response =
                service.suggestSegments(
                        new SegmentSuggestionRequest(CUSTOMER_ID, "Berlin", "Germany", null, 3));

        assertThat(response.suggestions()).hasSize(4);
        assertThat(response.suggestions())
                .extracting(SegmentSuggestionView::suggestedName)
                .contains(
                        "Berlin audience",
                        "PROSPECT profile audience",
                        "LIFE_INSURANCE owners",
                        "3-month expiration audience");
        assertThat(response.suggestions())
                .allSatisfy(
                        suggestion -> {
                            assertThat(suggestion.explanation()).isNotBlank();
                            assertThat(suggestion.confidenceScore()).isGreaterThan(BigDecimal.ZERO);
                            assertThat(suggestion.storedRecommendationId()).isNotNull();
                        });
    }

    @Test
    @DisplayName("497 Segment suggestion returns explanation")
    void suggestSegmentsReturnsHumanReadableExplanationAndStoresIt() {
        Customer customer = customer();
        Product product = product(OWNED_PRODUCT_ID, "Life Protect", ProductType.LIFE_INSURANCE);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(ownership(customer, product)));
        stubRecommendationSave();

        SegmentSuggestionView.ListResponse response =
                service.suggestSegments(
                        new SegmentSuggestionRequest(CUSTOMER_ID, "Berlin", "Germany", null, 3));

        assertThat(response.suggestions()).isNotEmpty();
        assertThat(response.suggestions())
                .allSatisfy(
                        suggestion ->
                                assertThat(suggestion.explanation())
                                        .contains(
                                                "AI-002 rule-based segment suggestion",
                                                "human decision support"));
        SegmentSuggestionView locationSuggestion =
                response.suggestions().stream()
                        .filter(suggestion -> suggestion.suggestedName().equals("Berlin audience"))
                        .findFirst()
                        .orElseThrow();
        assertThat(locationSuggestion.explanation())
                .contains("customer/location signal", "city EQUALS Berlin");
        assertThat(locationSuggestion.suggestedCriteria())
                .extracting(SuggestedSegmentCriterion::fieldName)
                .contains("city");
        ArgumentCaptor<AiRecommendation> storedRecommendations =
                ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository, Mockito.atLeastOnce()).save(storedRecommendations.capture());
        assertThat(storedRecommendations.getAllValues())
                .filteredOn(stored -> stored.getRecommendation().equals(locationSuggestion.suggestedName()))
                .singleElement()
                .satisfies(
                        stored ->
                                assertThat(stored.getExplanation())
                                        .isEqualTo(locationSuggestion.explanation()));
    }

    @Test
    void suggestSegmentsUsesPaymentHistoryAndProfileRules() {
        Customer customer = customer();
        customer.recordSource("LIFE_INSURANCE_BENEFICIARY");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_41_60);
        PaymentRecord defaultRisk =
                payment(customer, PaymentStatus.DEFAULT_RISK, 3, LocalDate.now().minusDays(40));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(ownershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(defaultRisk));
        stubRecommendationSave();

        SegmentSuggestionView.ListResponse response =
                service.suggestSegments(
                        new SegmentSuggestionRequest(CUSTOMER_ID, null, null, null, 6));

        assertThat(response.suggestions())
                .extracting(SegmentSuggestionView::suggestedName)
                .contains(
                        "Berlin audience",
                        "PROSPECT profile audience",
                        "Default-risk payment audience",
                        "6-month expiration audience");
        assertThat(response.suggestions())
                .filteredOn(
                        suggestion ->
                                suggestion.suggestedName().equals("PROSPECT profile audience"))
                .singleElement()
                .satisfies(
                        suggestion ->
                                assertThat(suggestion.suggestedCriteriaSummary())
                                        .contains(
                                                "customer_type EQUALS PROSPECT",
                                                "age_group EQUALS 41_60",
                                                "status EQUALS ACTIVE",
                                                "source CONTAINS LIFE_INSURANCE_BENEFICIARY"));
        assertThat(response.suggestions())
                .filteredOn(
                        suggestion ->
                                suggestion.suggestedName()
                                        .equals("Default-risk payment audience"))
                .singleElement()
                .satisfies(
                        suggestion -> {
                            assertThat(suggestion.suggestedCriteriaSummary())
                                    .contains(
                                            "default_risk EQUALS true",
                                            "reminder_count GREATER_THAN 0",
                                            "days_overdue GREATER_THAN 0");
                            assertThat(suggestion.explanation())
                                    .contains("payment history", "default-risk");
                            assertThat(suggestion.confidenceScore()).isEqualByComparingTo("88");
                        });
    }

    @Test
    void calculateDefaultRiskScoresPaymentHistoryAndStoresRiskRecommendation() {
        Customer customer = customer();
        PaymentRecord overdue =
                payment(customer, PaymentStatus.OVERDUE, 2, LocalDate.now().minusDays(45));
        PaymentRecord defaultRisk =
                payment(customer, PaymentStatus.DEFAULT_RISK, 3, LocalDate.now().minusDays(75));
        PaymentRecord paid =
                payment(customer, PaymentStatus.PAID, 0, LocalDate.now().minusDays(10));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(overdue, defaultRisk, paid));
        stubRecommendationSave();

        DefaultRiskScoreView view =
                service.calculateDefaultRisk(new DefaultRiskScoreRequest(CUSTOMER_ID));

        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.riskLevel()).isEqualTo("HIGH");
        assertThat(view.riskScore()).isEqualByComparingTo("85");
        assertThat(view.explanation())
                .contains("missed payments", "overdue days", "reminder escalation");
        assertThat(view.factors())
                .extracting(ScoreExplanationView::factor)
                .contains(
                        "missed payments",
                        "default risk payments",
                        "overdue days",
                        "reminder escalation",
                        "paid history");
        assertThat(view.factors())
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("reminder escalation");
                            assertThat(factor.contribution()).isEqualByComparingTo("20");
                            assertThat(factor.detail()).contains("Red=2");
                        });
        assertThat(view.factors())
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("paid history");
                            assertThat(factor.contribution()).isEqualByComparingTo("-2");
                        });
        assertThat(view.storedRecommendationId()).isNotNull();
    }

    @Test
    @DisplayName("498 Default-risk scoring works from payment history")
    void calculateDefaultRiskUsesPaymentHistorySignalsAndStoresAuditRecord() {
        Customer customer = customer();
        PaymentRecord duePastDeadline =
                payment(customer, PaymentStatus.DUE, 1, LocalDate.now().minusDays(12));
        PaymentRecord overdue =
                payment(customer, PaymentStatus.OVERDUE, 2, LocalDate.now().minusDays(28));
        PaymentRecord defaultRisk =
                payment(customer, PaymentStatus.DEFAULT_RISK, 3, LocalDate.now().minusDays(44));
        PaymentRecord paid =
                payment(customer, PaymentStatus.PAID, 0, LocalDate.now().minusDays(5));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(duePastDeadline, overdue, defaultRisk, paid));
        stubRecommendationSave();

        DefaultRiskScoreView view =
                service.calculateDefaultRisk(new DefaultRiskScoreRequest(CUSTOMER_ID));

        assertThat(view.riskScore()).isEqualByComparingTo("79");
        assertThat(view.riskLevel()).isEqualTo("HIGH");
        assertThat(view.explanation())
                .contains(
                        "missed payments",
                        "overdue days",
                        "reminder escalation",
                        "payment history");
        assertThat(view.factors())
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("missed payments");
                            assertThat(factor.contribution()).isEqualByComparingTo("25");
                            assertThat(factor.detail())
                                    .contains("3 unpaid overdue/default-risk payment(s)");
                        })
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("default risk payments");
                            assertThat(factor.contribution()).isEqualByComparingTo("25");
                            assertThat(factor.detail())
                                    .contains("1 payment(s) already marked default risk");
                        })
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("overdue days");
                            assertThat(factor.contribution()).isEqualByComparingTo("11");
                            assertThat(factor.detail()).contains("Maximum overdue age is 44 day(s)");
                        })
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("reminder escalation");
                            assertThat(factor.contribution()).isEqualByComparingTo("20");
                            assertThat(factor.detail()).contains("Green=0", "Yellow=1", "Red=2");
                        })
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("paid history");
                            assertThat(factor.contribution()).isEqualByComparingTo("-2");
                            assertThat(factor.detail()).contains("1 paid payment(s) reduce risk");
                        });
        ArgumentCaptor<AiRecommendation> storedRecommendation =
                ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(storedRecommendation.capture());
        assertThat(storedRecommendation.getValue().getRecommendation())
                .isEqualTo("Default risk level: HIGH");
        assertThat(storedRecommendation.getValue().getExplanation())
                .isEqualTo(view.explanation());
        assertThat(storedRecommendation.getValue().getConfidenceScore()).isEqualByComparingTo("79");
    }

    @Test
    void calculateDefaultRiskReturnsLowScoreForPaidHistoryOnly() {
        Customer customer = customer();
        PaymentRecord paid =
                payment(customer, PaymentStatus.PAID, 0, LocalDate.now().minusDays(20));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paid));
        stubRecommendationSave();

        DefaultRiskScoreView view =
                service.calculateDefaultRisk(new DefaultRiskScoreRequest(CUSTOMER_ID));

        assertThat(view.riskScore()).isEqualByComparingTo("0");
        assertThat(view.riskLevel()).isEqualTo("LOW");
        assertThat(view.explanation()).contains("payment history");
        assertThat(view.factors())
                .anySatisfy(
                        factor -> {
                            assertThat(factor.factor()).isEqualTo("paid history");
                            assertThat(factor.contribution()).isEqualByComparingTo("-2");
                            assertThat(factor.detail()).contains("1 paid payment");
                        });
    }

    @Test
    void detectDuplicateRiskWarnsForMonthlyLimitAndSameCampaign() {
        Customer customer = customer();
        Campaign campaign =
                Campaign.create("Renewal", "Renew customers", null, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ContactEvent event =
                ContactEvent.sent(customer, campaign, CommunicationChannel.EMAIL, Instant.now(), null);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(contactEventRepository.countRecentCustomerMarketingContacts(
                        any(UUID.class), any(Instant.class)))
                .thenReturn(3L);
        when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(event));
        stubRecommendationSave();

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(
                        new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID));

        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.riskDetected()).isTrue();
        assertThat(view.warning())
                .isEqualTo(
                        "Duplicate-contact risk detected: BR-010 same campaign "
                                + "and BR-011 monthly limit");
        assertThat(view.explanation())
                .contains(
                        "3 marketing contact attempt(s)",
                        "monthly limit=3",
                        "AI warning only");
        assertThat(view.contactsInCurrentMonth()).isEqualTo(3);
        assertThat(view.monthlyContactLimit()).isEqualTo(3);
        assertThat(view.sameCampaignAlreadyContacted()).isTrue();
        assertThat(view.storedRecommendationId()).isNotNull();
    }

    @Test
    void detectDuplicateRiskWarnsForSameCampaignOnly() {
        Customer customer = customer();
        Campaign campaign =
                Campaign.create("Renewal", "Renew customers", null, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ContactEvent event =
                ContactEvent.sent(customer, campaign, CommunicationChannel.EMAIL, Instant.now(), null);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(contactEventRepository.countRecentCustomerMarketingContacts(
                        any(UUID.class), any(Instant.class)))
                .thenReturn(1L);
        when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(event));
        stubRecommendationSave();

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(
                        new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID));

        assertThat(view.riskDetected()).isTrue();
        assertThat(view.warning())
                .isEqualTo("Duplicate-contact risk detected: BR-010 same campaign");
        assertThat(view.contactsInCurrentMonth()).isEqualTo(1);
        assertThat(view.sameCampaignAlreadyContacted()).isTrue();
    }

    @Test
    @DisplayName("499 Duplicate-contact warning detects repeated contact risk")
    void detectDuplicateRiskDetectsRepeatedSameCampaignMarketingContact() {
        Customer customer = customer();
        Campaign campaign =
                Campaign.create("Renewal", "Renew customers", null, null, CampaignChannel.PHONE);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ContactEvent note =
                ContactEvent.record(
                        customer,
                        campaign,
                        CommunicationChannel.EMAIL,
                        ContactEventType.NOTE,
                        Instant.now().minus(2, ChronoUnit.DAYS),
                        null,
                        null,
                        "Internal note only");
        ContactEvent repeatedCall =
                ContactEvent.record(
                        customer,
                        campaign,
                        CommunicationChannel.PHONE,
                        ContactEventType.CALLED,
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        null,
                        null,
                        "Customer was already called for this campaign");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(contactEventRepository.countRecentCustomerMarketingContacts(
                        any(UUID.class), any(Instant.class)))
                .thenReturn(1L);
        when(contactEventRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(note, repeatedCall));
        stubRecommendationSave();

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(
                        new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID));

        assertThat(view.riskDetected()).isTrue();
        assertThat(view.warning())
                .isEqualTo("Duplicate-contact risk detected: BR-010 same campaign");
        assertThat(view.contactsInCurrentMonth()).isEqualTo(1);
        assertThat(view.monthlyContactLimit()).isEqualTo(3);
        assertThat(view.sameCampaignAlreadyContacted()).isTrue();
        assertThat(view.explanation())
                .contains(
                        "1 marketing contact attempt(s)",
                        "monthly limit=3",
                        "campaign " + CAMPAIGN_ID + " contacted=true",
                        "AI warning only");
        ArgumentCaptor<AiRecommendation> storedRecommendation =
                ArgumentCaptor.forClass(AiRecommendation.class);
        verify(aiRecommendationRepository).save(storedRecommendation.capture());
        assertThat(storedRecommendation.getValue().getRecommendation()).isEqualTo(view.warning());
        assertThat(storedRecommendation.getValue().getExplanation()).isEqualTo(view.explanation());
        assertThat(storedRecommendation.getValue().getConfidenceScore()).isEqualByComparingTo("88");
    }

    @Test
    void detectDuplicateRiskWarnsForMonthlyLimitOnly() {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(contactEventRepository.countRecentCustomerMarketingContacts(
                        any(UUID.class), any(Instant.class)))
                .thenReturn(3L);
        when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        stubRecommendationSave();

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(
                        new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID));

        assertThat(view.riskDetected()).isTrue();
        assertThat(view.warning())
                .isEqualTo("Duplicate-contact risk detected: BR-011 monthly contact limit");
        assertThat(view.contactsInCurrentMonth()).isEqualTo(3);
        assertThat(view.sameCampaignAlreadyContacted()).isFalse();
    }

    @Test
    void detectDuplicateRiskReturnsNoRiskWhenBelowLimitAndNoSameCampaignAttempt() {
        Customer customer = customer();
        Campaign campaign =
                Campaign.create("Renewal", "Renew customers", null, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        ContactEvent note =
                ContactEvent.record(
                        customer,
                        campaign,
                        CommunicationChannel.EMAIL,
                        ContactEventType.NOTE,
                        Instant.now(),
                        null,
                        null,
                        "Internal note only");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(contactEventRepository.countRecentCustomerMarketingContacts(
                        any(UUID.class), any(Instant.class)))
                .thenReturn(2L);
        when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(note));
        stubRecommendationSave();

        DuplicateContactRiskView view =
                service.detectDuplicateRisk(
                        new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID));

        assertThat(view.riskDetected()).isFalse();
        assertThat(view.warning()).isEqualTo("No duplicate-contact risk detected");
        assertThat(view.contactsInCurrentMonth()).isEqualTo(2);
        assertThat(view.sameCampaignAlreadyContacted()).isFalse();
        assertThat(view.explanation()).contains("eligibility rules remain authoritative");
    }

    @Test
    void validatesRequiredCustomerId() {
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.recommendProducts(new ProductRecommendationRequest(null)))
                .withMessage("AI recommendation validation failed");
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.calculateDefaultRisk(new DefaultRiskScoreRequest(null)))
                .withMessage("AI recommendation validation failed");
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(
                        () ->
                                service.detectDuplicateRisk(
                                        new DuplicateContactRiskRequest(null, null)))
                .withMessage("AI recommendation validation failed");
    }

    @Test
    void validatesSegmentExpirationWindow() {
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(
                        () ->
                                service.suggestSegments(
                                        new SegmentSuggestionRequest(null, null, null, null, 0)))
                .withMessage("AI segment suggestion validation failed");
    }

    private void stubRecommendationSave() {
        when(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .thenAnswer(
                        invocation -> {
                            AiRecommendation recommendation = invocation.getArgument(0);
                            ReflectionTestUtils.setField(
                                    recommendation,
                                    "id",
                                    UUID.fromString("80000000-0000-0000-0000-000000000475"));
                            ReflectionTestUtils.setField(
                                    recommendation, "createdAt", Instant.EPOCH);
                            return recommendation;
                        });
    }

    private static void assertMethodAuthorization(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = AiRecommendationService.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedExpression);
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Lovelace");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        customer.updateContactDetails("ada.lovelace@example.test", "+49 30 111111");
        customer.updateAddress("Main Street 1", "Berlin", "Germany");
        return customer;
    }

    private static Product product(UUID id, String name, ProductType productType) {
        return product(id, name, productType, BigDecimal.valueOf(99), 12);
    }

    private static Product product(
            UUID id,
            String name,
            ProductType productType,
            BigDecimal price,
            Integer durationMonths) {
        Product product = Product.create(name, productType, price, durationMonths);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private static ProductOwnership ownership(Customer customer, Product product) {
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, LocalDate.now().minusMonths(6), null);
        ReflectionTestUtils.setField(
                ownership, "id", UUID.fromString("30000000-0000-0000-0000-000000000475"));
        return ownership;
    }

    private static PaymentRecord payment(
            Customer customer, PaymentStatus status, int reminderCount, LocalDate dueDate) {
        Product product = product(PRODUCT_ID, "Health Plus", ProductType.HEALTH_INSURANCE);
        ProductOwnership ownership = ownership(customer, product);
        PaymentRecord payment = PaymentRecord.create(customer, ownership, dueDate, BigDecimal.TEN);
        ReflectionTestUtils.setField(payment, "status", status);
        ReflectionTestUtils.setField(payment, "reminderCount", reminderCount);
        return payment;
    }
}
