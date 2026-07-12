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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * KB item 198 acceptance: preview applies EligibilityService (counts covered by item 199).
 *
 * <p>Proves {@link SegmentService#previewSegment} always routes every criteria match through {@link
 * EligibilityService#evaluateForSegmentPreview(UUID)}, keeps totalAudienceCount as pre-eligibility
 * size, returns only eligible customers in matchingCustomers, and never treats criteria-only
 * matches as contactable without the eligibility gate (FR-054 / FR-055 / FR-079 / BR-001–003).
 */
@ExtendWith(MockitoExtension.class)
class SegmentPreviewAppliesEligibilityServiceWorksTests {

    private static final UUID ID_ELIGIBLE = UUID.fromString("20000000-0000-0000-0000-000000000b01");
    private static final UUID ID_DNC = UUID.fromString("20000000-0000-0000-0000-000000000b02");
    private static final UUID ID_OPT_OUT = UUID.fromString("20000000-0000-0000-0000-000000000b03");
    private static final UUID ID_INVALID_CONSENT =
            UUID.fromString("20000000-0000-0000-0000-000000000b04");
    private static final UUID ID_LIMIT = UUID.fromString("20000000-0000-0000-0000-000000000b05");
    private static final UUID ID_BERLIN = UUID.fromString("20000000-0000-0000-0000-000000000b06");

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
    void previewAlwaysCallsEligibilityServiceOncePerCriteriaMatch() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer dnc = munich("Tom", "Schmidt", ID_DNC);
        Customer optOut = munich("Anna", "Weber", ID_OPT_OUT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, dnc, optOut));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(ID_OPT_OUT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        verify(eligibilityService, times(3)).evaluateForSegmentPreview(any(UUID.class));
        verify(eligibilityService).evaluateForSegmentPreview(ID_ELIGIBLE);
        verify(eligibilityService).evaluateForSegmentPreview(ID_DNC);
        verify(eligibilityService).evaluateForSegmentPreview(ID_OPT_OUT);
        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::id)
                .containsExactly(ID_ELIGIBLE);
    }

    @Test
    void previewNeverReturnsCriteriaOnlyMatchesAsContactableWithoutEligibility() throws Exception {
        Customer blocked = munich("Tom", "Schmidt", ID_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(blocked));
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        // findMatchingCustomers would include the DNC profile (criteria-only).
        List<CustomerView> criteriaOnly =
                segmentService.findMatchingCustomers(munichPreview().criteria());
        assertThat(criteriaOnly).extracting(CustomerView::id).containsExactly(ID_DNC);

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).isEmpty();
        verify(eligibilityService).evaluateForSegmentPreview(ID_DNC);
    }

    @Test
    void findMatchingCustomersDoesNotCallEligibilityService() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(munichPreview().criteria());

        assertThat(matches).hasSize(1);
        verify(eligibilityService, never()).evaluateForSegmentPreview(any(UUID.class));
    }

    @ParameterizedTest(name = "preview excludes {0} from matchingCustomers")
    @EnumSource(
            value = EligibilityExclusionReason.class,
            names = {
                "DO_NOT_CONTACT",
                "MARKETING_OPT_OUT",
                "INVALID_CONSENT",
                "MONTHLY_CONTACT_LIMIT"
            })
    void previewExcludesEachSegmentEligibilityReason(EligibilityExclusionReason reason)
            throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer blocked = munich("Blocked", "User", ID_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, blocked));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(EligibilityDecision.excluded(reason));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::id)
                .containsExactly(ID_ELIGIBLE);
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactly(reason.code());
    }

    @Test
    void previewAppliesEligibilityAfterCriteriaFilterNotBefore() throws Exception {
        Customer munichEligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer munichBlocked = munich("Tom", "Schmidt", ID_DNC);
        Customer berlinEligible = berlin("Max", "Bauer", ID_BERLIN);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichEligible, munichBlocked, berlinEligible));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        // Berlin profile fails city criteria — EligibilityService must not be called for them.
        verify(eligibilityService, never()).evaluateForSegmentPreview(ID_BERLIN);
        verify(eligibilityService, times(2)).evaluateForSegmentPreview(any(UUID.class));
        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::id)
                .containsExactly(ID_ELIGIBLE);
    }

    @Test
    void emptyCriteriaPreviewStillAppliesEligibilityToAllActiveProfiles() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer limited = munich("Mia", "Braun", ID_LIMIT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, limited));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_LIMIT))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        SegmentPreviewView preview =
                segmentService.previewSegment(new SegmentPreviewCommand(List.of()));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code())
                .isEqualTo("MONTHLY_CONTACT_LIMIT");
        verify(eligibilityService, times(2)).evaluateForSegmentPreview(any(UUID.class));
    }

    @Test
    void whenAllCriteriaMatchesAreIneligiblePreviewReturnsEmptyMatchingCustomers()
            throws Exception {
        Customer dnc = munich("Tom", "Schmidt", ID_DNC);
        Customer optOut = munich("Anna", "Weber", ID_OPT_OUT);
        Customer noConsent = munich("Kai", "Fischer", ID_INVALID_CONSENT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(dnc, optOut, noConsent));
        when(eligibilityService.evaluateForSegmentPreview(ID_DNC))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(ID_OPT_OUT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));
        when(eligibilityService.evaluateForSegmentPreview(ID_INVALID_CONSENT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.excludedCount()).isEqualTo(3);
        assertThat(preview.matchingCustomers()).isEmpty();
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactlyInAnyOrder(
                        "DO_NOT_CONTACT", "MARKETING_OPT_OUT", "INVALID_CONSENT");
        verify(eligibilityService, times(3)).evaluateForSegmentPreview(any(UUID.class));
    }

    @Test
    void whenAllCriteriaMatchesAreEligiblePreviewListsAllMatches() throws Exception {
        Customer one = munich("Lena", "Mueller", ID_ELIGIBLE);
        Customer two = munich("Sara", "Klein", ID_OPT_OUT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(one, two));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_OPT_OUT))
                .thenReturn(EligibilityDecision.included());

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).hasSize(2);
        assertThat(preview.exclusionReasonSummary()).isEmpty();
        verify(eligibilityService, times(2)).evaluateForSegmentPreview(any(UUID.class));
    }

    @Test
    void zeroCriteriaMatchesNeverCallsEligibilityService() throws Exception {
        Customer berlin = berlin("Max", "Bauer", ID_BERLIN);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(berlin));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(0);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).isEmpty();
        verify(eligibilityService, never()).evaluateForSegmentPreview(any(UUID.class));
    }

    @Test
    void previewSegmentAndEvaluateForSegmentPreviewRequireCanPreviewSegments() throws Exception {
        PreAuthorize previewAuth =
                SegmentService.class
                        .getMethod("previewSegment", SegmentPreviewCommand.class)
                        .getAnnotation(PreAuthorize.class);
        Method evaluate =
                EligibilityService.class.getMethod("evaluateForSegmentPreview", UUID.class);
        PreAuthorize eligibilityAuth = evaluate.getAnnotation(PreAuthorize.class);

        assertThat(previewAuth).isNotNull();
        assertThat(previewAuth.value()).isEqualTo("@authz.canPreviewSegments()");
        assertThat(eligibilityAuth).isNotNull();
        assertThat(eligibilityAuth.value()).isEqualTo("@authz.canPreviewSegments()");
    }

    @Test
    void applyEligibilityPathDelegatesToEvaluateForSegmentPreview() throws Exception {
        Method apply =
                SegmentService.class.getDeclaredMethod(
                        "applyEligibilityServiceToPreviewMatch", CustomerView.class);
        apply.setAccessible(true);

        CustomerView view = CustomerView.from(munich("Lena", "Mueller", ID_ELIGIBLE));
        when(eligibilityService.evaluateForSegmentPreview(ID_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());

        EligibilityDecision decision = (EligibilityDecision) apply.invoke(segmentService, view);

        assertThat(decision.eligible()).isTrue();
        verify(eligibilityService).evaluateForSegmentPreview(ID_ELIGIBLE);
    }

    private static SegmentPreviewCommand munichPreview() {
        return new SegmentPreviewCommand(
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND)));
    }

    private static Customer munich(String first, String last, UUID id) throws Exception {
        return profile(first, last, id, "Munich");
    }

    private static Customer berlin(String first, String last, UUID id) throws Exception {
        return profile(first, last, id, "Berlin");
    }

    private static Customer profile(String first, String last, UUID id, String city)
            throws Exception {
        Customer customer = Customer.create(CustomerType.PROSPECT, first, last);
        customer.updateAddress(null, city, "Germany");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
