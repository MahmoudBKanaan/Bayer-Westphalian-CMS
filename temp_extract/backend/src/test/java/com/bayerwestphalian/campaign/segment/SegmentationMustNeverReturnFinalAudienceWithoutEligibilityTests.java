package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * KB item 208 production gate: segmentation must never return a final campaign audience without
 * eligibility checks.
 *
 * <p>Proves preview always applies {@link EligibilityService}, criteria-only matching is not a
 * public contactability API, and documentation records the gate.
 */
@ExtendWith(MockitoExtension.class)
class SegmentationMustNeverReturnFinalAudienceWithoutEligibilityTests {

    private static final UUID ID_ELIGIBLE =
            UUID.fromString("20000000-0000-0000-0000-000000000c01");
    private static final UUID ID_DNC = UUID.fromString("20000000-0000-0000-0000-000000000c02");
    private static final UUID ID_OPT_OUT =
            UUID.fromString("20000000-0000-0000-0000-000000000c03");

    private static final Path SEGMENTATION_MODULE_DOC =
            Path.of("../docs/modules/segmentation-module.md");
    private static final Path AUDIENCE_PREVIEW_DOC =
            Path.of("../docs/modules/audience-preview-logic.md");
    private static final Path ELIGIBILITY_RULES_DOC =
            Path.of("../docs/architecture/eligibility-rules.md");
    private static final Path SEGMENT_CRITERIA_GUIDE =
            Path.of("../docs/modules/segment-criteria-guide.md");
    private static final Path SEGMENTATION_USER_GUIDE =
            Path.of("../docs/user-guides/segmentation-user-guide.md");

    @Mock private SegmentRepository segmentRepository;
    @Mock private SegmentCriteriaRepository segmentCriteriaRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductOwnershipRepository productOwnershipRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;
    @Mock private ConsentService consentService;
    @Mock private EligibilityService eligibilityService;
    @Mock private AuditService auditService;

    private SegmentService segmentService;

    @BeforeEach
    void setUp() {
        segmentService =
                new SegmentService(
                        segmentRepository,
                        segmentCriteriaRepository,
                        customerRepository,
                        productOwnershipRepository,
                        paymentRecordRepository,
                        consentRepository,
                        userRepository,
                        authorizationExpressions,
                        consentService,
                        eligibilityService,
                        auditService);
    }

    @Test
    void previewNeverReturnsIneligibleCustomersAsContactableAudience() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer dnc = munich("Tom", "Schmidt", ID_DNC);
        Customer optOut = munich("Anna", "Weber", ID_OPT_OUT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, dnc, optOut));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(ID_OPT_OUT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCriteriaPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::id)
                .containsExactly(ID_ELIGIBLE)
                .doesNotContain(ID_DNC, ID_OPT_OUT);
        assertThat(preview.matchingCustomers()).hasSize(preview.eligibleCount());

        verify(eligibilityService, times(1)).evaluateForSegmentPreview(ID_ELIGIBLE);
        verify(eligibilityService, times(1)).evaluateForSegmentPreview(ID_DNC);
        verify(eligibilityService, times(1)).evaluateForSegmentPreview(ID_OPT_OUT);
    }

    @Test
    void criteriaOnlyMatchingIsNotAnEligibilityGatedFinalAudience() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer dnc = munich("Tom", "Schmidt", ID_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, dnc));

        List<CustomerView> criteriaOnly =
                segmentService.findMatchingCustomers(munichCriteriaPreview().criteria());

        // Criteria-only includes DNC — must never be treated as final campaign audience (item 208).
        assertThat(criteriaOnly).extracting(CustomerView::id).containsExactly(ID_ELIGIBLE, ID_DNC);
        verify(eligibilityService, never()).evaluateForSegmentPreview(any(UUID.class));

        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCriteriaPreview());
        assertThat(preview.matchingCustomers()).extracting(CustomerView::id).containsExactly(ID_ELIGIBLE);
        assertThat(preview.excludedCount()).isEqualTo(1);
    }

    @Test
    void findMatchingCustomersIsPackagePrivateNotPublicFinalAudienceApi() throws Exception {
        Method findMatching =
                SegmentService.class.getDeclaredMethod(
                        "findMatchingCustomers", List.class);

        assertThat(Modifier.isPublic(findMatching.getModifiers())).isFalse();
        assertThat(Modifier.isPrivate(findMatching.getModifiers())).isFalse();
        assertThat(Modifier.isProtected(findMatching.getModifiers())).isFalse();
        // package-private: not public/private/protected
        assertThat(findMatching.canAccess(segmentService)).isTrue();
    }

    @Test
    void previewSegmentIsPublicContactableAudienceApiAndRequiresEligibilityPath() throws Exception {
        Method preview =
                SegmentService.class.getMethod("previewSegment", SegmentPreviewCommand.class);

        assertThat(Modifier.isPublic(preview.getModifiers())).isTrue();
        assertThat(preview.getReturnType()).isEqualTo(SegmentPreviewView.class);

        // Source-level gate: preview method body always routes through eligibility helper / service.
        Path source =
                Path.of(
                        "src/main/java/com/bayerwestphalian/campaign/segment/SegmentService.java");
        String serviceSource = Files.readString(source, StandardCharsets.UTF_8);
        assertThat(serviceSource)
                .contains("evaluateForSegmentPreview")
                .contains("applyEligibilityServiceToPreviewMatch")
                .contains("item 208")
                .contains("never return a final campaign audience without eligibility");
    }

    @Test
    void segmentControllerExposesPreviewButNotCriteriaOnlyMatchingEndpoint() throws Exception {
        Method[] methods = SegmentController.class.getDeclaredMethods();
        List<String> names =
                Arrays.stream(methods).map(Method::getName).filter(n -> !n.contains("$")).toList();

        assertThat(names).contains("previewSegment");
        assertThat(names)
                .doesNotContain("findMatchingCustomers")
                .doesNotContain("matchingCustomers");

        Method preview =
                SegmentController.class.getDeclaredMethod(
                        "previewSegment", SegmentPreviewRequest.class);
        RequestMapping typeMapping = SegmentController.class.getAnnotation(RequestMapping.class);
        assertThat(typeMapping).isNotNull();
        assertThat(typeMapping.value()).contains("/api/segments");

        Path controllerSource =
                Path.of(
                        "src/main/java/com/bayerwestphalian/campaign/segment/SegmentController.java");
        String source = Files.readString(controllerSource, StandardCharsets.UTF_8);
        assertThat(source)
                .contains("item 208")
                .contains("no REST endpoint for criteria-only")
                .doesNotContain("findMatchingCustomers(");
    }

    @Test
    void emptyCriteriaPreviewStillAppliesEligibilityToActiveProfiles() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer dnc = munich("Tom", "Schmidt", ID_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, dnc));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(new SegmentPreviewCommand(List.of()));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers()).extracting(CustomerView::id).containsExactly(ID_ELIGIBLE);
        verify(eligibilityService, times(2)).evaluateForSegmentPreview(any(UUID.class));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "../docs/modules/segmentation-module.md",
                "../docs/modules/audience-preview-logic.md"
            })
    void productionGateDocumentationStatesNeverFinalAudienceWithoutEligibility(String relativePath)
            throws Exception {
        String documentation = Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Production gate")
                .contains("item 208")
                .contains("never return a final campaign audience without eligibility");
    }

    @Test
    void relatedDocsRecordEligibilityRequiredForContactableAudience() throws Exception {
        String eligibility = Files.readString(ELIGIBILITY_RULES_DOC, StandardCharsets.UTF_8);
        String criteria = Files.readString(SEGMENT_CRITERIA_GUIDE, StandardCharsets.UTF_8);
        String userGuide = Files.readString(SEGMENTATION_USER_GUIDE, StandardCharsets.UTF_8);
        String module = Files.readString(SEGMENTATION_MODULE_DOC, StandardCharsets.UTF_8);
        String preview = Files.readString(AUDIENCE_PREVIEW_DOC, StandardCharsets.UTF_8);

        assertThat(eligibility)
                .contains("evaluateForSegmentPreview")
                .contains("Preview never returns a criteria-only audience");
        assertThat(criteria)
                .contains("criteria matching is not final contact permission without eligibility");
        assertThat(userGuide).containsIgnoringCase("eligibility");
        assertThat(module)
                .contains("findMatchingCustomers")
                .contains("not exposed as a public REST audience endpoint");
        assertThat(preview)
                .contains("package-private")
                .contains("POST /api/segments/preview");
    }

    @Test
    void packageInfoDocumentsProductionGateItem208() throws Exception {
        String packageInfo =
                Files.readString(
                        Path.of(
                                "src/main/java/com/bayerwestphalian/campaign/segment/package-info.java"),
                        StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("item 208")
                .contains("never return a final campaign audience without eligibility checks")
                .contains("previewSegment");
    }

    private static SegmentPreviewCommand munichCriteriaPreview() {
        return new SegmentPreviewCommand(
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND)));
    }

    private static Customer munich(String firstName, String lastName, UUID id) throws Exception {
        Customer customer = Customer.create(CustomerType.PROSPECT, firstName, lastName);
        customer.updateAddress(null, "Munich", "Germany");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        customer.changeStatus(CustomerStatus.ACTIVE);
        setId(customer, id);
        return customer;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
