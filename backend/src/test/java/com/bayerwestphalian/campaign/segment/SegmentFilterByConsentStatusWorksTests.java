package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 194 acceptance: segment filter by consent status works.
 *
 * <p>Proves consent_status, consent_type, has_valid_marketing_consent / marketing_consent, opt_out,
 * and guardian_consent filters correctly select active profiles via EQUALS / NOT_EQUALS / IN,
 * including segment preview and multi-criteria AND combinations.
 */
@ExtendWith(MockitoExtension.class)
class SegmentFilterByConsentStatusWorksTests {

    private static final UUID ID_GIVEN = UUID.fromString("20000000-0000-0000-0000-000000000701");
    private static final UUID ID_WITHDRAWN =
            UUID.fromString("20000000-0000-0000-0000-000000000702");
    private static final UUID ID_REJECTED = UUID.fromString("20000000-0000-0000-0000-000000000703");
    private static final UUID ID_EXPIRED = UUID.fromString("20000000-0000-0000-0000-000000000704");
    private static final UUID ID_REQUIRED = UUID.fromString("20000000-0000-0000-0000-000000000705");
    private static final UUID ID_GUARDIAN = UUID.fromString("20000000-0000-0000-0000-000000000706");
    private static final UUID ID_NONE = UUID.fromString("20000000-0000-0000-0000-000000000707");

    private static final Instant NOW = Instant.parse("2026-07-09T12:00:00Z");

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
        lenient()
                .when(eligibilityService.evaluateForSegmentPreview(any(UUID.class)))
                .thenReturn(EligibilityDecision.included());
        lenient().when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @ParameterizedTest(name = "EQUALS consent_status {0} matches {1}")
    @CsvSource({
        "GIVEN,Given Consent",
        "given,Given Consent",
        "WITHDRAWN,Withdrawn Consent",
        "REJECTED,Rejected Consent",
        "EXPIRED,Expired Consent",
        "REQUIRED,Required Consent"
    })
    void equalsConsentStatusFilterMatchesOnlyThatStatus(String filterValue, String expectedName)
            throws Exception {
        stubConsentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(consentStatusEquals(filterValue));

        if ("Given Consent".equals(expectedName)) {
            assertThat(matches)
                    .extracting(CustomerView::fullName)
                    .containsExactlyInAnyOrder("Given Consent", "Guardian Ready");
        } else {
            assertThat(matches).hasSize(1);
            assertThat(matches.getFirst().fullName()).isEqualTo(expectedName);
        }
    }

    @Test
    void notEqualsConsentStatusExcludesTargetStatus() throws Exception {
        stubConsentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_status",
                                        SegmentOperator.NOT_EQUALS,
                                        "WITHDRAWN",
                                        "consent",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Given Consent",
                        "Rejected Consent",
                        "Expired Consent",
                        "Required Consent",
                        "Guardian Ready",
                        "No Consent");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Withdrawn Consent"));
    }

    @Test
    void inOperatorMatchesAnyListedConsentStatus() throws Exception {
        stubConsentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_status",
                                        SegmentOperator.IN,
                                        "WITHDRAWN, REJECTED, EXPIRED",
                                        "consent",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Withdrawn Consent", "Rejected Consent", "Expired Consent");
    }

    @Test
    void consentAliasFieldNameFiltersByConsentStatus() throws Exception {
        stubConsentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent",
                                        SegmentOperator.EQUALS,
                                        "GIVEN",
                                        null,
                                        SegmentJoinOperator.AND)));

        // Guardian also has GIVEN status
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Given Consent", "Guardian Ready");
    }

    @Test
    void equalsConsentTypeFilterMatchesMarketingAndGuardian() throws Exception {
        stubConsentProfiles();

        List<CustomerView> marketingEmail =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_type",
                                        SegmentOperator.EQUALS,
                                        "MARKETING_EMAIL",
                                        "consent",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> guardian =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_type",
                                        SegmentOperator.EQUALS,
                                        "GUARDIAN",
                                        "consent",
                                        SegmentJoinOperator.AND)));

        assertThat(marketingEmail)
                .extracting(CustomerView::fullName)
                .contains(
                        "Given Consent",
                        "Withdrawn Consent",
                        "Rejected Consent",
                        "Expired Consent",
                        "Required Consent");
        assertThat(marketingEmail).noneMatch(view -> view.fullName().equals("Guardian Ready"));
        assertThat(guardian).extracting(CustomerView::fullName).containsExactly("Guardian Ready");
    }

    @Test
    void consentTypeInOperatorMatchesListedTypes() throws Exception {
        stubConsentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_type",
                                        SegmentOperator.IN,
                                        "GUARDIAN, DATA_PROCESSING",
                                        "consent",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Guardian Ready");
    }

    @Test
    void hasValidMarketingConsentFilterWorks() throws Exception {
        stubConsentProfiles();

        List<CustomerView> valid =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "has_valid_marketing_consent",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        "consent",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> marketingAlias =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "marketing_consent",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(valid).extracting(CustomerView::fullName).containsExactly("Given Consent");
        assertThat(marketingAlias)
                .extracting(CustomerView::fullName)
                .containsExactly("Given Consent");
    }

    @Test
    void optOutFilterMatchesWithdrawnAndRejectedMarketing() throws Exception {
        stubConsentProfiles();

        List<CustomerView> optedOut =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "opt_out",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        "consent",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> notOptedOut =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "opt_out",
                                        SegmentOperator.EQUALS,
                                        "false",
                                        "consent",
                                        SegmentJoinOperator.AND)));

        assertThat(optedOut)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Withdrawn Consent", "Rejected Consent");
        assertThat(notOptedOut)
                .extracting(CustomerView::fullName)
                .contains(
                        "Given Consent",
                        "Expired Consent",
                        "Required Consent",
                        "Guardian Ready",
                        "No Consent");
        assertThat(notOptedOut).noneMatch(view -> view.fullName().equals("Withdrawn Consent"));
    }

    @Test
    void guardianConsentFilterWorksWithAlias() throws Exception {
        stubConsentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "guardian_consent",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        "consent",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Guardian Ready");
    }

    @Test
    void customersWithoutConsentDoNotMatchEqualsGiven() throws Exception {
        Customer none = profile("No", "Consent", ID_NONE);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(none));
        when(consentRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(consentStatusEquals("GIVEN"));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAppliesConsentStatusFilterAndReturnsEligibleMatches() throws Exception {
        stubConsentProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(consentStatusEquals("GIVEN")));

        // GIVEN marketing + GIVEN guardian both match status; eligibility is stubbed as included
        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Given Consent", "Guardian Ready");
    }

    @Test
    void previewWithValidMarketingConsentAndNotOptOutAndWorks() throws Exception {
        stubConsentProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "has_valid_marketing_consent",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                "consent",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "opt_out",
                                                SegmentOperator.EQUALS,
                                                "false",
                                                "consent",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Given Consent");
    }

    @Test
    void consentStatusAndCityAndCombinationWorks() throws Exception {
        Customer munichGiven = profile("Lena", "Mueller", ID_GIVEN, "Munich");
        Customer berlinGiven = profile("Tom", "Schmidt", ID_WITHDRAWN, "Berlin");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(munichGiven, berlinGiven));
        when(consentRepository.findByCustomerId(ID_GIVEN))
                .thenReturn(List.of(givenMarketing(munichGiven)));
        when(consentRepository.findByCustomerId(ID_WITHDRAWN))
                .thenReturn(List.of(givenMarketing(berlinGiven)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_status",
                                        SegmentOperator.EQUALS,
                                        "GIVEN",
                                        "consent",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    @ParameterizedTest
    @EnumSource(ConsentStatus.class)
    void everyKbConsentStatusValueIsAcceptedAndNormalized(ConsentStatus status) {
        String normalized =
                SegmentConsentStatusSupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, "consent_status", status.name().toLowerCase());
        assertThat(normalized).isEqualTo(status.name());
        SegmentConsentStatusSupport.validateFilterValue(
                SegmentOperator.EQUALS, "consent_status", status.name());
    }

    @ParameterizedTest
    @EnumSource(ConsentType.class)
    void everyKbConsentTypeValueIsAcceptedAndNormalized(ConsentType type) {
        String normalized =
                SegmentConsentStatusSupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, "consent_type", type.name().toLowerCase());
        assertThat(normalized).isEqualTo(type.name());
        SegmentConsentStatusSupport.validateFilterValue(
                SegmentOperator.EQUALS, "consent_type", type.name());
    }

    @Test
    void unsupportedConsentStatusIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () -> segmentService.findMatchingCustomers(consentStatusEquals("PENDING")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void unsupportedConsentTypeIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "consent_type",
                                                        SegmentOperator.EQUALS,
                                                        "POSTAL",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void invalidBooleanOptOutIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "opt_out",
                                                        SegmentOperator.EQUALS,
                                                        "maybe",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void consentSupportRecognizesKbFieldsAndAliases() {
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("consent_status")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("consent_type")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("marketing_consent")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("opt_out")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("guardian_consent")).isTrue();
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("consent"))
                .isEqualTo("consent_status");
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("marketing_opt_out"))
                .isEqualTo("opt_out");
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("guardian_consent"))
                .isEqualTo("has_valid_guardian_consent");
    }

    private void stubConsentProfiles() throws Exception {
        Customer given = profile("Given", "Consent", ID_GIVEN);
        Customer withdrawn = profile("Withdrawn", "Consent", ID_WITHDRAWN);
        Customer rejected = profile("Rejected", "Consent", ID_REJECTED);
        Customer expired = profile("Expired", "Consent", ID_EXPIRED);
        Customer required = profile("Required", "Consent", ID_REQUIRED);
        Customer guardian = profile("Guardian", "Ready", ID_GUARDIAN, CustomerType.BENEFICIARY);
        Customer none = profile("No", "Consent", ID_NONE);

        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(given, withdrawn, rejected, expired, required, guardian, none));

        when(consentRepository.findByCustomerId(ID_GIVEN))
                .thenReturn(List.of(givenMarketing(given)));
        when(consentRepository.findByCustomerId(ID_WITHDRAWN))
                .thenReturn(List.of(withdrawnMarketing(withdrawn)));
        when(consentRepository.findByCustomerId(ID_REJECTED))
                .thenReturn(List.of(rejectedMarketing(rejected)));
        when(consentRepository.findByCustomerId(ID_EXPIRED))
                .thenReturn(List.of(expiredMarketing(expired)));
        when(consentRepository.findByCustomerId(ID_REQUIRED))
                .thenReturn(List.of(requiredMarketing(required)));
        when(consentRepository.findByCustomerId(ID_GUARDIAN))
                .thenReturn(List.of(givenGuardian(guardian)));
        when(consentRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());
    }

    private static List<CreateSegmentCriteriaCommand> consentStatusEquals(String value) {
        return List.of(
                new CreateSegmentCriteriaCommand(
                        "consent_status",
                        SegmentOperator.EQUALS,
                        value,
                        "consent",
                        SegmentJoinOperator.AND));
    }

    private static Customer profile(String first, String last, UUID id) throws Exception {
        return profile(first, last, id, "Berlin", CustomerType.PROSPECT);
    }

    private static Customer profile(String first, String last, UUID id, String city)
            throws Exception {
        return profile(first, last, id, city, CustomerType.PROSPECT);
    }

    private static Customer profile(String first, String last, UUID id, CustomerType type)
            throws Exception {
        return profile(first, last, id, "Berlin", type);
    }

    private static Customer profile(
            String first, String last, UUID id, String city, CustomerType type) throws Exception {
        Customer customer = Customer.create(type, first, last);
        customer.updateAddress(null, city, "Germany");
        customer.changeStatus(CustomerStatus.ACTIVE);
        setEntityId(customer, id);
        return customer;
    }

    private static ConsentRecord givenMarketing(Customer customer) {
        return ConsentRecord.create(
                customer,
                ConsentType.MARKETING_EMAIL,
                ConsentStatus.GIVEN,
                "Marketing email consent",
                "phone");
    }

    private static ConsentRecord withdrawnMarketing(Customer customer) {
        ConsentRecord consent = givenMarketing(customer);
        consent.withdraw(NOW);
        return consent;
    }

    private static ConsentRecord rejectedMarketing(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        consent.reject();
        return consent;
    }

    private static ConsentRecord expiredMarketing(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        consent.expire();
        return consent;
    }

    private static ConsentRecord requiredMarketing(Customer customer) {
        return ConsentRecord.create(
                customer,
                ConsentType.MARKETING_EMAIL,
                ConsentStatus.REQUIRED,
                "Marketing email consent required",
                "import");
    }

    private static ConsentRecord givenGuardian(Customer customer) {
        return ConsentRecord.create(
                customer,
                ConsentType.GUARDIAN,
                ConsentStatus.GIVEN,
                "Guardian consent for minor",
                "letter");
    }

    private static void setEntityId(BaseEntity entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to assign entity id for test fixture", exception);
        }
    }
}
