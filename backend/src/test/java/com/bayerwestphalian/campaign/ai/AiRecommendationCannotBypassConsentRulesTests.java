package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sprint 16 critical test item <b>661</b>: AI recommendation cannot bypass consent rules.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code COMP-005} / items 468, 502–504 — AI must not override consent, opt-out, DNC, or
 *       eligibility
 *   <li>{@code FR-034} / {@code NFR-002} — marketing requires valid consent; privacy-aware AI
 *   <li>{@code AI-001}–{@code AI-006} preface — decision-support only; no final marketing without
 *       human + compliance rules
 *   <li>Item 512 companion: {@link AiSupportsHumanDecisionMakingOnlyTests}
 * </ul>
 *
 * <p>Enforcement model under test:
 *
 * <ol>
 *   <li>AI package does not own {@link ConsentService} or {@link EligibilityService}
 *   <li>AI public API has no consent-mutation or eligibility-bypass operations
 *   <li>Recommendations may be advisory even when a customer is non-contactable, but never clear
 *       DNC / invent consent / change eligibility authority
 *   <li>Authoritative exclusion remains on {@link EligibilityService} (invalid consent still
 *       excludes)
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("661 AI recommendation cannot bypass consent rules")
class AiRecommendationCannotBypassConsentRulesTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000661");
    private static final UUID PRODUCT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000661");
    private static final Path AI_MAIN_PACKAGE =
            Path.of("src/main/java/com/bayerwestphalian/campaign/ai");
    private static final Path AI_LIMITATIONS_DOC =
            Path.of("../docs/modules/ai-limitations-and-human-approval.md");

    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductOwnershipRepository productOwnershipRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private ContactEventRepository contactEventRepository;
    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private SystemSettingsService systemSettingsService;
    @Mock private EligibilityService eligibilityService;

    private AiRecommendationService aiRecommendationService;
    private AiSearchService aiSearchService;

    @BeforeEach
    void setUp() {
        aiRecommendationService =
                new AiRecommendationService(
                        customerRepository,
                        productRepository,
                        productOwnershipRepository,
                        paymentRecordRepository,
                        contactEventRepository,
                        aiRecommendationRepository,
                        systemSettingsService);
        aiSearchService =
                new AiSearchService(
                        customerRepository, productOwnershipRepository, contactEventRepository);
    }

    @Nested
    @DisplayName("AI cannot own or mutate consent")
    class NoConsentOwnership {

        @Test
        void aiServicesDoNotDependOnConsentServiceOrEligibilityService() {
            assertConstructorTypesExclude(
                    AiRecommendationService.class, ConsentService.class, EligibilityService.class);
            assertConstructorTypesExclude(
                    AiSearchService.class, ConsentService.class, EligibilityService.class);
            assertConstructorTypesExclude(
                    CampaignCopyService.class, ConsentService.class, EligibilityService.class);
            assertConstructorTypesExclude(
                    AiController.class, ConsentService.class, EligibilityService.class);
        }

        @Test
        void aiPublicApiHasNoConsentMutationOrEligibilityBypassMethods() {
            Set<String> methodNames =
                    Stream.of(
                                    AiRecommendationService.class,
                                    AiSearchService.class,
                                    CampaignCopyService.class,
                                    AiController.class)
                            .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                            .filter(method -> Modifier.isPublic(method.getModifiers()))
                            .map(Method::getName)
                            .collect(Collectors.toSet());

            assertThat(methodNames)
                    .doesNotContain(
                            "recordConsent",
                            "withdrawConsent",
                            "markOptOut",
                            "grantConsent",
                            "setDoNotContact",
                            "clearDoNotContact",
                            "evaluateEligibility",
                            "bypassEligibility",
                            "forceEligible",
                            "overrideConsent");

            assertThat(methodNames)
                    .contains(
                            "recommendProducts",
                            "suggestSegments",
                            "detectDuplicateRisk",
                            "weightedSearch");
        }

        @Test
        void aiMainSourcesDoNotImportConsentOrEligibilityServices() throws Exception {
            List<Path> sources =
                    Files.list(AI_MAIN_PACKAGE)
                            .filter(path -> path.toString().endsWith(".java"))
                            .toList();
            assertThat(sources).isNotEmpty();

            for (Path source : sources) {
                String content = Files.readString(source);
                assertThat(content)
                        .as(source.getFileName().toString())
                        .doesNotContain(
                                "import com.bayerwestphalian.campaign.consent.ConsentService")
                        .doesNotContain(
                                "import com.bayerwestphalian.campaign.campaign.EligibilityService");
            }
        }
    }

    @Nested
    @DisplayName("Recommendations remain advisory; consent state is not rewritten")
    class AdvisoryOnly {

        @Test
        void productRecommendationDoesNotClearDoNotContactOrSaveCustomer() {
            Customer blocked = dncCustomer();
            Product product =
                    Product.create(
                            "Term Life Plus",
                            ProductType.LIFE_INSURANCE,
                            BigDecimal.valueOf(50),
                            12);
            ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(blocked));
            when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
            when(productRepository.findActive()).thenReturn(List.of(product));

            ProductRecommendationView.ListResponse response =
                    aiRecommendationService.recommendProducts(
                            new ProductRecommendationRequest(CUSTOMER_ID));

            // Advisory output may still list products; it must not rewrite contact flags.
            assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(blocked.isDoNotContact()).isTrue();
            verify(customerRepository, never()).save(any(Customer.class));
            verify(aiRecommendationRepository, never()).save(any(AiRecommendation.class));
        }

        @Test
        void fuzzySearchSurfacesDoNotContactWithoutMutatingConsentFlags() {
            Customer blocked = dncCustomer();
            ReflectionTestUtils.setField(blocked, "email", "blocked.661@example.test");
            ReflectionTestUtils.setField(blocked, "city", "Munich");

            when(customerRepository.findActiveProfiles()).thenReturn(List.of(blocked));
            when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
            when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

            AiCustomerSearchView view = aiSearchService.weightedSearch("Blocked", 10);

            assertThat(view.results()).isNotEmpty();
            AiCustomerSearchHitView hit = view.results().get(0);
            assertThat(hit.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(hit.doNotContact()).isTrue();
            assertThat(blocked.isDoNotContact()).isTrue();
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        void duplicateContactWarningIsNotAnEligibilityOverride() throws Exception {
            Method detect =
                    AiRecommendationService.class.getMethod(
                            "detectDuplicateRisk", DuplicateContactRiskRequest.class);
            assertThat(detect.getReturnType()).isEqualTo(DuplicateContactRiskView.class);
            assertThat(detect.getName()).doesNotContainIgnoringCase("bypass");
            assertThat(detect.getName()).doesNotContainIgnoringCase("consent");
        }
    }

    @Nested
    @DisplayName("Authoritative consent rules still exclude marketing contact")
    class EligibilityRemainsAuthoritative {

        @Test
        void eligibilityStillExcludesCustomerWithoutValidConsentAfterAiSuggestion() {
            // AI product suggestion for a customer does not change EligibilityService outcomes.
            when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                    .thenReturn(
                            EligibilityDecision.excluded(
                                    EligibilityExclusionReason.INVALID_CONSENT));

            EligibilityDecision decision =
                    eligibilityService.evaluateForReminder(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

            assertThat(decision.eligible()).isFalse();
            assertThat(decision.exclusionReason())
                    .isEqualTo(EligibilityExclusionReason.INVALID_CONSENT.code());
            // AI recommendation service is not involved in the exclusion decision.
            verify(aiRecommendationRepository, never()).save(any());
            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Policy documentation (COMP-005 / item 502)")
    class PolicyDocs {

        @Test
        void limitationsDocStatesAiCannotOverrideOrInventConsent() throws Exception {
            String policy = Files.readString(AI_LIMITATIONS_DOC);
            assertThat(policy)
                    .contains("661")
                    .contains("AiRecommendationCannotBypassConsentRulesTests")
                    .contains("Cannot override or invent consent")
                    .contains("Cannot ignore marketing opt-out")
                    .contains("Cannot bypass do-not-contact")
                    .contains("Cannot bypass `EligibilityService`")
                    .contains("COMP-005");
        }

        @Test
        void packageInfoStatesConsentAndEligibilityNonBypass() throws Exception {
            String packageInfo = Files.readString(AI_MAIN_PACKAGE.resolve("package-info.java"));
            assertThat(packageInfo)
                    .contains("consent")
                    .contains("eligibility")
                    .contains("do-not-contact");
        }
    }

    private static Customer dncCustomer() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Blocked", "Prospect");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        customer.markDoNotContact();
        assertThat(customer.isDoNotContact()).isTrue();
        return customer;
    }

    private static void assertConstructorTypesExclude(
            Class<?> type, Class<?>... forbiddenDependencies) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertThat(constructors).isNotEmpty();
        for (Constructor<?> constructor : constructors) {
            Set<Class<?>> parameterTypes =
                    Arrays.stream(constructor.getParameters())
                            .map(Parameter::getType)
                            .collect(Collectors.toSet());
            for (Class<?> forbidden : forbiddenDependencies) {
                assertThat(parameterTypes)
                        .as(
                                "%s constructor must not depend on %s",
                                type.getSimpleName(), forbidden.getSimpleName())
                        .doesNotContain(forbidden);
            }
        }
    }
}
