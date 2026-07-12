package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * KB items 178 / 198: Apply EligibilityService to segment preview (FR-054, FR-055, BR-001–003,
 * BR-006).
 *
 * <p>Proves preview never returns criteria matches as contactable without {@link
 * EligibilityService#evaluateForSegmentPreview(UUID)}. Item 198 acceptance expands these proofs in
 * {@link SegmentPreviewAppliesEligibilityServiceWorksTests}.
 */
@ExtendWith(MockitoExtension.class)
class SegmentPreviewAppliesEligibilityServiceTests {

    private static final UUID CUSTOMER_ELIGIBLE =
            UUID.fromString("20000000-0000-0000-0000-000000000201");
    private static final UUID CUSTOMER_DNC =
            UUID.fromString("20000000-0000-0000-0000-000000000202");
    private static final UUID CUSTOMER_OPT_OUT =
            UUID.fromString("20000000-0000-0000-0000-000000000203");
    private static final UUID CUSTOMER_CONSENT =
            UUID.fromString("20000000-0000-0000-0000-000000000204");
    private static final UUID CUSTOMER_LIMIT =
            UUID.fromString("20000000-0000-0000-0000-000000000205");

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
    void previewSegmentInvokesEligibilityServiceForEveryCriteriaMatch() throws Exception {
        Customer eligible = munichCustomer("Lena", "Mueller", CUSTOMER_ELIGIBLE);
        Customer blocked = munichCustomer("Tom", "Schmidt", CUSTOMER_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, blocked));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        verify(eligibilityService).evaluateForSegmentPreview(CUSTOMER_ELIGIBLE);
        verify(eligibilityService).evaluateForSegmentPreview(CUSTOMER_DNC);
        verify(eligibilityService, times(2)).evaluateForSegmentPreview(any(UUID.class));
        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::id)
                .containsExactly(CUSTOMER_ELIGIBLE);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::id)
                .doesNotContain(CUSTOMER_DNC);
    }

    @Test
    void previewSegmentDoesNotListIneligibleCustomersAsMatching() throws Exception {
        Customer dnc = munichCustomer("Tom", "Schmidt", CUSTOMER_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(dnc));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).isEmpty();
        assertThat(preview.exclusionReasonSummary()).hasSize(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code()).isEqualTo("DO_NOT_CONTACT");
        verify(eligibilityService, atLeastOnce()).evaluateForSegmentPreview(CUSTOMER_DNC);
    }

    @Test
    void previewSegmentAppliesMarketingOptOutExclusionsFromEligibilityService() throws Exception {
        Customer eligible = munichCustomer("Lena", "Mueller", CUSTOMER_ELIGIBLE);
        Customer optOut = munichCustomer("Anna", "Weber", CUSTOMER_OPT_OUT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, optOut));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_OPT_OUT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code())
                .isEqualTo("MARKETING_OPT_OUT");
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void previewSegmentAppliesInvalidConsentExclusionsFromEligibilityService() throws Exception {
        Customer noConsent = munichCustomer("Kai", "Fischer", CUSTOMER_CONSENT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(noConsent));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_CONSENT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.exclusionReasonSummary().getFirst().code()).isEqualTo("INVALID_CONSENT");
        verify(eligibilityService).evaluateForSegmentPreview(CUSTOMER_CONSENT);
    }

    @Test
    void previewSegmentAppliesMonthlyContactLimitExclusionsFromEligibilityService()
            throws Exception {
        Customer limited = munichCustomer("Mia", "Braun", CUSTOMER_LIMIT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(limited));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_LIMIT))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code())
                .isEqualTo("MONTHLY_CONTACT_LIMIT");
    }

    @Test
    void previewSegmentAggregatesMultipleEligibilityExclusionReasons() throws Exception {
        Customer eligible = munichCustomer("Lena", "Mueller", CUSTOMER_ELIGIBLE);
        Customer dnc = munichCustomer("Tom", "Schmidt", CUSTOMER_DNC);
        Customer optOut = munichCustomer("Anna", "Weber", CUSTOMER_OPT_OUT);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, dnc, optOut));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_OPT_OUT))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactlyInAnyOrder("DO_NOT_CONTACT", "MARKETING_OPT_OUT");
        verify(eligibilityService, times(3)).evaluateForSegmentPreview(any(UUID.class));
    }

    @Test
    void emptyCriteriaStillAppliesEligibilityServiceToAllActiveProfiles() throws Exception {
        Customer eligible = munichCustomer("Lena", "Mueller", CUSTOMER_ELIGIBLE);
        Customer blocked = munichCustomer("Tom", "Schmidt", CUSTOMER_DNC);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, blocked));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ELIGIBLE))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_DNC))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(new SegmentPreviewCommand(List.of()));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        verify(eligibilityService).evaluateForSegmentPreview(CUSTOMER_ELIGIBLE);
        verify(eligibilityService).evaluateForSegmentPreview(CUSTOMER_DNC);
    }

    @Test
    void customersWithoutIdsAreExcludedWithoutCallingEligibilityService() throws Exception {
        Customer missingId = Customer.create(CustomerType.PROSPECT, "No", "Id");
        missingId.updateAddress(null, "Munich", "Germany");
        missingId.changeStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(missingId));

        SegmentPreviewView preview = segmentService.previewSegment(munichCityPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).isEmpty();
        verify(eligibilityService, never()).evaluateForSegmentPreview(any(UUID.class));
    }

    @Test
    void previewSegmentMethodRequiresCanPreviewSegmentsAuthorization() throws Exception {
        PreAuthorize preAuthorize =
                SegmentService.class
                        .getMethod("previewSegment", SegmentPreviewCommand.class)
                        .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canPreviewSegments()");
    }

    @Test
    void eligibilityServiceSegmentPreviewMethodsDeclareCanPreviewSegmentsAuthorization()
            throws Exception {
        PreAuthorize singleArg =
                EligibilityService.class
                        .getMethod("evaluateForSegmentPreview", UUID.class)
                        .getAnnotation(PreAuthorize.class);
        PreAuthorize withConsent =
                EligibilityService.class
                        .getMethod(
                                "evaluateForSegmentPreview",
                                UUID.class,
                                com.bayerwestphalian.campaign.consent.ConsentType.class)
                        .getAnnotation(PreAuthorize.class);

        assertThat(singleArg).isNotNull();
        assertThat(singleArg.value()).isEqualTo("@authz.canPreviewSegments()");
        assertThat(withConsent).isNotNull();
        assertThat(withConsent.value()).isEqualTo("@authz.canPreviewSegments()");
    }

    private static SegmentPreviewCommand munichCityPreview() {
        return new SegmentPreviewCommand(
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND)));
    }

    private static Customer munichCustomer(String first, String last, UUID id) throws Exception {
        Customer customer = Customer.create(CustomerType.PROSPECT, first, last);
        customer.updateAddress(null, "Munich", "Germany");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
