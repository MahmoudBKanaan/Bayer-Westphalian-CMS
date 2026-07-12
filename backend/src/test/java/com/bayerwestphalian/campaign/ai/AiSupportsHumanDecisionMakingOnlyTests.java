package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * KB item 512 acceptance: AI must support human decision-making only and must never override
 * compliance approval, consent, opt-out, do-not-contact, eligibility, or human approval rules.
 *
 * <p>Formalizes COMP-005 / items 468, 501–504, 507 at the package boundary: AI services suggest,
 * score, warn, and record human review — they do not own campaign lifecycle, consent mutation, DNC
 * clearing, or eligibility decisions.
 *
 * <p>Companion coverage: {@link CampaignCopyServiceTests} (items 500–501), policy doc item 507,
 * feature doc item 506. Sprint 16 critical restatement of consent non-bypass: item <b>661</b> —
 * {@link AiRecommendationCannotBypassConsentRulesTests}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName(
        "512 AI must support human decision-making only and must never override compliance"
                + " approval, consent, opt-out, do-not-contact, eligibility, or human approval"
                + " rules")
class AiSupportsHumanDecisionMakingOnlyTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000512");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000512");
    private static final UUID RECOMMENDATION_ID =
            UUID.fromString("57000000-0000-0000-0000-000000000512");
    private static final UUID APPROVER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000512");

    private static final Path AI_MAIN_PACKAGE =
            Path.of("src/main/java/com/bayerwestphalian/campaign/ai");

    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductOwnershipRepository productOwnershipRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private ContactEventRepository contactEventRepository;
    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private CampaignCopyService campaignCopyService;
    private AiSearchService aiSearchService;

    @BeforeEach
    void setUp() {
        campaignCopyService =
                new CampaignCopyService(
                        productRepository,
                        segmentRepository,
                        campaignRepository,
                        aiRecommendationRepository,
                        userRepository,
                        authorizationExpressions);
        aiSearchService =
                new AiSearchService(
                        customerRepository, productOwnershipRepository, contactEventRepository);
    }

    @Nested
    @DisplayName("Package boundary: no compliance / consent / eligibility ownership")
    class PackageBoundary {

        @Test
        void aiServiceConstructorsDoNotDependOnConsentOrEligibilityServices() {
            assertConstructorTypesExclude(
                    AiSearchService.class, ConsentService.class, EligibilityService.class);
            assertConstructorTypesExclude(
                    AiRecommendationService.class,
                    ConsentService.class,
                    EligibilityService.class);
            assertConstructorTypesExclude(
                    CampaignCopyService.class, ConsentService.class, EligibilityService.class);
            assertConstructorTypesExclude(
                    AiController.class, ConsentService.class, EligibilityService.class);
        }

        @Test
        void aiPublicApiDoesNotExposeCampaignLifecycleOrConsentMutations() {
            Set<String> methodNames =
                    Stream.of(
                                    AiSearchService.class,
                                    AiRecommendationService.class,
                                    CampaignCopyService.class,
                                    AiController.class)
                            .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                            .filter(method -> Modifier.isPublic(method.getModifiers()))
                            .map(Method::getName)
                            .collect(Collectors.toSet());

            assertThat(methodNames)
                    .doesNotContain(
                            "approveCampaign",
                            "rejectCampaign",
                            "submitCampaign",
                            "launchCampaign",
                            "launch",
                            "recordConsent",
                            "withdrawConsent",
                            "markOptOut",
                            "setDoNotContact",
                            "clearDoNotContact",
                            "evaluateEligibility",
                            "bypassEligibility");

            // Decision-support surface that must remain present.
            assertThat(methodNames)
                    .contains(
                            "weightedSearch",
                            "suggestSegments",
                            "recommendProducts",
                            "detectDuplicateRisk",
                            "generateCopySuggestion",
                            "approveCampaignCopy");
        }

        @Test
        void aiMainSourcesDoNotImportConsentServiceOrEligibilityService() throws Exception {
            List<Path> sources =
                    Files.list(AI_MAIN_PACKAGE)
                            .filter(path -> path.toString().endsWith(".java"))
                            .toList();
            assertThat(sources).isNotEmpty();

            for (Path source : sources) {
                String content = Files.readString(source);
                assertThat(content)
                        .as(source.getFileName().toString())
                        .doesNotContain("import com.bayerwestphalian.campaign.consent.ConsentService")
                        .doesNotContain(
                                "import com.bayerwestphalian.campaign.campaign.EligibilityService");
            }
        }

        @Test
        void aiControllerOnlyExposesDecisionSupportRoutes() {
            assertThat(AiController.class.getAnnotation(RequestMapping.class).value())
                    .containsExactly("/api/ai");

            Set<String> getPaths =
                    Arrays.stream(AiController.class.getDeclaredMethods())
                            .filter(method -> method.isAnnotationPresent(GetMapping.class))
                            .flatMap(method -> Arrays.stream(method.getAnnotation(GetMapping.class).value()))
                            .collect(Collectors.toSet());
            Set<String> postPaths =
                    Arrays.stream(AiController.class.getDeclaredMethods())
                            .filter(method -> method.isAnnotationPresent(PostMapping.class))
                            .flatMap(
                                    method ->
                                            Arrays.stream(
                                                    method.getAnnotation(PostMapping.class).value()))
                            .collect(Collectors.toSet());

            assertThat(getPaths).containsExactlyInAnyOrder("/customer-search");
            assertThat(postPaths)
                    .containsExactlyInAnyOrder(
                            "/segment-suggestions",
                            "/product-recommendations",
                            "/duplicate-contact-warning",
                            "/campaign-copy",
                            "/campaign-copy/{recommendationId}/approve");

            assertThat(postPaths)
                    .noneMatch(path -> path.contains("launch"))
                    .noneMatch(path -> path.contains("consent"))
                    .noneMatch(path -> path.contains("eligibility"));
        }
    }

    @Nested
    @DisplayName("Human approval rules (AI-005 / COMP-005)")
    class HumanApproval {

        @Test
        void campaignCopySuggestionAlwaysRequiresHumanApproval() {
            CampaignCopySuggestionView forcedFalse =
                    new CampaignCopySuggestionView(
                            CAMPAIGN_ID,
                            "Subject",
                            "Body",
                            "CTA",
                            "Explanation for human review",
                            null,
                            false,
                            true,
                            APPROVER_ID,
                            RECOMMENDATION_ID);

            assertThat(forcedFalse.requiresHumanApproval()).isTrue();

            CampaignCopySuggestionView pending =
                    CampaignCopySuggestionView.pending(
                            CAMPAIGN_ID,
                            "Subject",
                            "Body",
                            "CTA",
                            "Pending human approval",
                            RECOMMENDATION_ID);
            assertThat(pending.requiresHumanApproval()).isTrue();
            assertThat(pending.humanApproved()).isFalse();
            assertThat(pending.approvedByUserId()).isNull();
        }

        @Test
        void recommendationApproveRequiresHumanUserAndRejectClearsApprover() {
            AiRecommendation recommendation =
                    AiRecommendation.create(
                            AiRecommendationType.COPY,
                            "campaign",
                            CAMPAIGN_ID,
                            "objective",
                            "Draft subject",
                            "Requires human review (COMP-005)");

            assertThatThrownBy(() -> recommendation.approve(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Approver");

            User human = sampleApprover();
            recommendation.approve(human);
            assertThat(recommendation.isApproved()).isTrue();
            assertThat(recommendation.getApprovedByUserId()).isEqualTo(APPROVER_ID);

            recommendation.reject();
            assertThat(recommendation.isApproved()).isFalse();
            assertThat(recommendation.getApprovedBy()).isNull();
        }

        @Test
        void approveCampaignCopyDoesNotPerformComplianceCampaignApproval() {
            Campaign campaign =
                    Campaign.create(
                            "Submitted campaign",
                            "Awaiting compliance",
                            sampleApprover(),
                            null,
                            CampaignChannel.EMAIL);
            ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
            ReflectionTestUtils.setField(campaign, "status", CampaignStatus.SUBMITTED);

            AiRecommendation recommendation =
                    AiRecommendation.create(
                            AiRecommendationType.COPY,
                            "campaign",
                            CAMPAIGN_ID,
                            "objective",
                            "Subject line",
                            "Human review only");
            ReflectionTestUtils.setField(recommendation, "id", RECOMMENDATION_ID);
            ReflectionTestUtils.setField(recommendation, "createdAt", Instant.now());

            when(aiRecommendationRepository.findById(RECOMMENDATION_ID))
                    .thenReturn(Optional.of(recommendation));
            when(authorizationExpressions.currentUserId()).thenReturn(APPROVER_ID);
            when(userRepository.findById(APPROVER_ID)).thenReturn(Optional.of(sampleApprover()));
            when(aiRecommendationRepository.save(recommendation)).thenReturn(recommendation);

            AiRecommendationView view =
                    campaignCopyService.approveCampaignCopy(
                            RECOMMENDATION_ID,
                            new ApproveAiRecommendationRequest("Copy reviewed"));

            assertThat(view.approved()).isTrue();
            assertThat(view.recommendationType()).isEqualTo(AiRecommendationType.COPY);
            // Item 501 / 512: recommendation approval ≠ campaign compliance approval.
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            assertThat(campaign.getApprovedBy()).isNull();
            assertThat(campaign.getApprovedAt()).isNull();
            verify(campaignRepository, never()).save(any(Campaign.class));
        }
    }

    @Nested
    @DisplayName("Do-not-contact, consent, and eligibility remain authoritative")
    class NonBypassSignals {

        @Test
        void searchSurfacesDoNotContactWithoutClearingIt() {
            Customer blocked = Customer.create(CustomerType.CUSTOMER, "Dana", "Blocked");
            ReflectionTestUtils.setField(blocked, "id", CUSTOMER_ID);
            ReflectionTestUtils.setField(blocked, "email", "dana.blocked@example.test");
            ReflectionTestUtils.setField(blocked, "city", "Munich");
            ReflectionTestUtils.setField(blocked, "status", CustomerStatus.ACTIVE);
            blocked.markDoNotContact();

            when(customerRepository.findActiveProfiles()).thenReturn(List.of(blocked));
            when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
            when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

            // Exact first-name match scores well above the fuzzy search threshold.
            AiCustomerSearchView view = aiSearchService.weightedSearch("Dana", 10);

            assertThat(view.results()).isNotEmpty();
            AiCustomerSearchHitView hit = view.results().get(0);
            assertThat(hit.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(hit.doNotContact()).isTrue();
            assertThat(blocked.isDoNotContact()).isTrue();
            // Search service never mutates the customer aggregate beyond reads.
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        void duplicateContactWarningIsAdvisoryAndDoesNotLaunchOrMutateConsent() throws Exception {
            Method detect =
                    AiRecommendationService.class.getMethod(
                            "detectDuplicateRisk", DuplicateContactRiskRequest.class);
            assertThat(detect.getReturnType()).isEqualTo(DuplicateContactRiskView.class);

            // Structural: method name and package must not claim eligibility override.
            assertThat(detect.getName()).isEqualTo("detectDuplicateRisk");
            assertThat(AiRecommendationService.class.getPackageName())
                    .isEqualTo("com.bayerwestphalian.campaign.ai");
        }

        @Test
        void packageInfoAndPolicyDocumentsStateHumanDecisionSupportOnly() throws Exception {
            String packageInfo =
                    Files.readString(
                            AI_MAIN_PACKAGE.resolve("package-info.java"));
            String policy =
                    Files.readString(
                            Path.of("../docs/modules/ai-limitations-and-human-approval.md"));

            assertThat(packageInfo)
                    .contains("never automatically approve")
                    .contains("consent")
                    .contains("do-not-contact")
                    .contains("eligibility");
            assertThat(policy)
                    .contains("Item **512**")
                    .contains("human decision-making only")
                    .contains("Cannot bypass `EligibilityService`")
                    .contains("Cannot override or invent consent")
                    .contains("Cannot bypass do-not-contact")
                    .contains("Cannot approve, reject, submit, launch");
        }
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
                        .as("%s constructor must not depend on %s", type.getSimpleName(), forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    private static User sampleApprover() {
        User user =
                User.create(
                        "ai.512@bayer-westphalian.test",
                        "{noop}x",
                        "Human Approver 512");
        ReflectionTestUtils.setField(user, "id", APPROVER_ID);
        return user;
    }
}
