package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.auth.method.CampaignWriteAccess;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentService;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 267: CampaignRecipientService generates persisted recipient snapshots. */
@ExtendWith(MockitoExtension.class)
class CampaignRecipientServiceTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000267");
    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000267");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000267");
    private static final UUID ELIGIBLE_CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000267");
    private static final UUID EXCLUDED_CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000268");

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignRecipientRepository campaignRecipientRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SegmentService segmentService;

    private CampaignRecipientService campaignRecipientService;

    @BeforeEach
    void setUp() {
        campaignRecipientService =
                new CampaignRecipientService(
                        campaignRepository,
                        campaignRecipientRepository,
                        customerRepository,
                        segmentService);
    }

    @Test
    void generateRecipientsDeclaresCampaignWriteAccess() throws Exception {
        Method method = CampaignRecipientService.class.getMethod("generateRecipients", UUID.class);

        assertThat(method.isAnnotationPresent(CampaignWriteAccess.class)).isTrue();
    }

    @Test
    void listEligibleRecipientsDeclaresCampaignReadAccess() throws Exception {
        Method method =
                CampaignRecipientService.class.getMethod("listEligibleRecipients", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canReadCampaigns()");
    }

    @Test
    void listExcludedRecipientsDeclaresCampaignReadAccess() throws Exception {
        Method method =
                CampaignRecipientService.class.getMethod("listExcludedRecipients", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canReadCampaigns()");
    }

    @Test
    void summarizeRecipientsDeclaresCampaignReadAccess() throws Exception {
        Method method =
                CampaignRecipientService.class.getMethod("summarizeRecipients", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canReadCampaigns()");
    }

    @Test
    void generatesEligibleAndExcludedRecipientsFromCampaignScopedCandidates() {
        Campaign campaign = campaign();
        Customer eligibleCustomer = customer(ELIGIBLE_CUSTOMER_ID, "Ada", "Eligible");
        Customer excludedCustomer = customer(EXCLUDED_CUSTOMER_ID, "Grace", "Excluded");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        ELIGIBLE_CUSTOMER_ID, EligibilityDecision.included()),
                                new CampaignRecipientCandidate(
                                        EXCLUDED_CUSTOMER_ID,
                                        EligibilityDecision.excluded(
                                                EligibilityExclusionReason.DO_NOT_CONTACT))));
        when(customerRepository.findById(ELIGIBLE_CUSTOMER_ID))
                .thenReturn(Optional.of(eligibleCustomer));
        when(customerRepository.findById(EXCLUDED_CUSTOMER_ID))
                .thenReturn(Optional.of(excludedCustomer));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CampaignRecipientView> views =
                campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampaignRecipient>> recipientsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(campaignRecipientRepository).saveAll(recipientsCaptor.capture());
        List<CampaignRecipient> recipients = recipientsCaptor.getValue();
        assertThat(recipients).hasSize(2);
        assertThat(recipients)
                .extracting(CampaignRecipient::getCustomerId)
                .containsExactly(ELIGIBLE_CUSTOMER_ID, EXCLUDED_CUSTOMER_ID);
        assertThat(recipients)
                .extracting(CampaignRecipient::getEligibilityStatus)
                .containsExactly(
                        CampaignRecipientStatus.ELIGIBLE, CampaignRecipientStatus.EXCLUDED);
        assertThat(recipients.getFirst().getEligibilityExplanation())
                .isEqualTo("Customer is eligible for campaign contact");
        assertThat(recipients.get(1).getExclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(recipients.get(1).getEligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");
        assertThat(views)
                .extracting(CampaignRecipientView::eligibilityStatus)
                .containsExactly(
                        CampaignRecipientStatus.ELIGIBLE, CampaignRecipientStatus.EXCLUDED);
        assertThat(views.getFirst().eligibilityExplanation())
                .isEqualTo("Customer is eligible for campaign contact");
        assertThat(views.get(1).exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(views.get(1).eligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");

        InOrder inOrder = inOrder(campaignRecipientRepository);
        inOrder.verify(campaignRecipientRepository).deleteByCampaign_Id(CAMPAIGN_ID);
        inOrder.verify(campaignRecipientRepository).saveAll(anyList());
    }

    @Test
    void storesMonthlyContactLimitExclusionWhenGeneratingRecipients() {
        Campaign campaign = campaign();
        Customer limitedCustomer = customer(EXCLUDED_CUSTOMER_ID, "Grace", "Limited");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        EXCLUDED_CUSTOMER_ID,
                                        EligibilityDecision.excluded(
                                                EligibilityExclusionReason
                                                        .MONTHLY_CONTACT_LIMIT))));
        when(customerRepository.findById(EXCLUDED_CUSTOMER_ID))
                .thenReturn(Optional.of(limitedCustomer));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CampaignRecipientView> views =
                campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampaignRecipient>> recipientsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(campaignRecipientRepository).saveAll(recipientsCaptor.capture());
        CampaignRecipient storedRecipient = recipientsCaptor.getValue().getFirst();
        assertThat(storedRecipient.getEligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(storedRecipient.getExclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(storedRecipient.getEligibilityExplanation())
                .isEqualTo("Customer has reached the monthly marketing contact limit");
        assertThat(views.getFirst().exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(views.getFirst().eligibilityExplanation())
                .isEqualTo("Customer has reached the monthly marketing contact limit");
    }

    @Test
    void storesUninterestedExclusionWhenGeneratingRecipients() {
        Campaign campaign = campaign();
        Customer uninterestedCustomer = customer(EXCLUDED_CUSTOMER_ID, "Grace", "Uninterested");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        EXCLUDED_CUSTOMER_ID,
                                        EligibilityDecision.excluded(
                                                EligibilityExclusionReason.UNINTERESTED))));
        when(customerRepository.findById(EXCLUDED_CUSTOMER_ID))
                .thenReturn(Optional.of(uninterestedCustomer));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CampaignRecipientView> views =
                campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampaignRecipient>> recipientsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(campaignRecipientRepository).saveAll(recipientsCaptor.capture());
        CampaignRecipient storedRecipient = recipientsCaptor.getValue().getFirst();
        assertThat(storedRecipient.getEligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(storedRecipient.getExclusionReason()).isEqualTo("UNINTERESTED");
        assertThat(storedRecipient.getEligibilityExplanation())
                .isEqualTo("Customer is marked as uninterested");
        assertThat(views.getFirst().exclusionReason()).isEqualTo("UNINTERESTED");
        assertThat(views.getFirst().eligibilityExplanation())
                .isEqualTo("Customer is marked as uninterested");
    }

    @Test
    void storesConvertedExclusionWhenGeneratingRecipients() {
        Campaign campaign = campaign();
        Customer convertedCustomer = customer(EXCLUDED_CUSTOMER_ID, "Grace", "Converted");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        EXCLUDED_CUSTOMER_ID,
                                        EligibilityDecision.excluded(
                                                EligibilityExclusionReason.CONVERTED))));
        when(customerRepository.findById(EXCLUDED_CUSTOMER_ID))
                .thenReturn(Optional.of(convertedCustomer));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CampaignRecipientView> views =
                campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampaignRecipient>> recipientsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(campaignRecipientRepository).saveAll(recipientsCaptor.capture());
        CampaignRecipient storedRecipient = recipientsCaptor.getValue().getFirst();
        assertThat(storedRecipient.getEligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(storedRecipient.getExclusionReason()).isEqualTo("CONVERTED");
        assertThat(storedRecipient.getEligibilityExplanation())
                .isEqualTo("Customer has already converted");
        assertThat(views.getFirst().exclusionReason()).isEqualTo("CONVERTED");
        assertThat(views.getFirst().eligibilityExplanation())
                .isEqualTo("Customer has already converted");
    }

    @Test
    void storesCustomerWithoutConsentAsExcludedRecipient() {
        Campaign campaign = campaign();
        Customer customerWithoutConsent = customer(EXCLUDED_CUSTOMER_ID, "Grace", "NoConsent");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        EXCLUDED_CUSTOMER_ID,
                                        EligibilityDecision.excluded(
                                                EligibilityExclusionReason.INVALID_CONSENT))));
        when(customerRepository.findById(EXCLUDED_CUSTOMER_ID))
                .thenReturn(Optional.of(customerWithoutConsent));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CampaignRecipientView> views =
                campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampaignRecipient>> recipientsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(campaignRecipientRepository).saveAll(recipientsCaptor.capture());
        CampaignRecipient storedRecipient = recipientsCaptor.getValue().getFirst();
        assertThat(storedRecipient.getEligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(storedRecipient.getExclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(storedRecipient.getEligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        assertThat(views.getFirst().eligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(views.getFirst().exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(views.getFirst().eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
    }

    @Test
    void replacesExistingCampaignRecipientSnapshotBeforeSavingNewRows() {
        Campaign campaign = campaign();
        Customer customer = customer(ELIGIBLE_CUSTOMER_ID, "Ada", "Eligible");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        ELIGIBLE_CUSTOMER_ID, EligibilityDecision.included())));
        when(customerRepository.findById(ELIGIBLE_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        InOrder inOrder = inOrder(campaignRecipientRepository);
        inOrder.verify(campaignRecipientRepository).deleteByCampaign_Id(CAMPAIGN_ID);
        inOrder.verify(campaignRecipientRepository).saveAll(anyList());
    }

    @Test
    void preventsDuplicateRecipientRowsForSameCampaignAndCustomer() {
        Campaign campaign = campaign();
        Customer customer = customer(ELIGIBLE_CUSTOMER_ID, "Ada", "Eligible");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        ELIGIBLE_CUSTOMER_ID, EligibilityDecision.included()),
                                new CampaignRecipientCandidate(
                                        ELIGIBLE_CUSTOMER_ID,
                                        EligibilityDecision.excluded(
                                                EligibilityExclusionReason.DO_NOT_CONTACT))));
        when(customerRepository.findById(ELIGIBLE_CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRecipientRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CampaignRecipientView> views =
                campaignRecipientService.generateRecipients(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CampaignRecipient>> recipientsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(campaignRecipientRepository).saveAll(recipientsCaptor.capture());
        assertThat(recipientsCaptor.getValue()).hasSize(1);
        assertThat(recipientsCaptor.getValue().getFirst().getCustomerId())
                .isEqualTo(ELIGIBLE_CUSTOMER_ID);
        assertThat(recipientsCaptor.getValue().getFirst().getEligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.ELIGIBLE);
        assertThat(views).hasSize(1);
        verify(customerRepository, times(1)).findById(ELIGIBLE_CUSTOMER_ID);
    }

    @Test
    void rejectsMissingCampaignId() {
        assertThatThrownBy(() -> campaignRecipientService.generateRecipients(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign recipient generation validation failed");
    }

    @Test
    void rejectsUnknownCampaign() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignRecipientService.generateRecipients(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign");
    }

    @Test
    void rejectsCandidateWithUnknownCustomer() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign()));
        when(segmentService.evaluateCampaignRecipientCandidates(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientCandidate(
                                        ELIGIBLE_CUSTOMER_ID, EligibilityDecision.included())));
        when(customerRepository.findById(ELIGIBLE_CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignRecipientService.generateRecipients(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    void listsOnlyStoredEligibleRecipientsForCampaign() {
        Campaign campaign = campaign();
        Customer eligibleCustomer = customer(ELIGIBLE_CUSTOMER_ID, "Ada", "Eligible");
        CampaignRecipient eligible = CampaignRecipient.eligible(campaign, eligibleCustomer);
        ReflectionTestUtils.setField(eligible, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(
                eligible, "createdAt", Instant.parse("2026-07-09T10:15:30Z"));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(List.of(eligible));

        List<CampaignRecipientView> recipients =
                campaignRecipientService.listEligibleRecipients(CAMPAIGN_ID);

        assertThat(recipients).hasSize(1);
        assertThat(recipients.getFirst().campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(recipients.getFirst().customerId()).isEqualTo(ELIGIBLE_CUSTOMER_ID);
        assertThat(recipients.getFirst().eligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.ELIGIBLE);
        verify(campaignRecipientRepository)
                .findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE);
    }

    @Test
    void listsOnlyStoredExcludedRecipientsForCampaign() {
        Campaign campaign = campaign();
        Customer excludedCustomer = customer(EXCLUDED_CUSTOMER_ID, "Grace", "Excluded");
        CampaignRecipient excluded =
                CampaignRecipient.excluded(
                        campaign,
                        excludedCustomer,
                        "INVALID_CONSENT",
                        "Customer does not have valid required consent");
        ReflectionTestUtils.setField(excluded, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(
                excluded, "createdAt", Instant.parse("2026-07-09T10:45:30Z"));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                .thenReturn(List.of(excluded));

        List<CampaignRecipientView> recipients =
                campaignRecipientService.listExcludedRecipients(CAMPAIGN_ID);

        assertThat(recipients).hasSize(1);
        assertThat(recipients.getFirst().campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(recipients.getFirst().customerId()).isEqualTo(EXCLUDED_CUSTOMER_ID);
        assertThat(recipients.getFirst().eligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(recipients.getFirst().exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(recipients.getFirst().eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        verify(campaignRecipientRepository)
                .findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED);
    }

    @Test
    void summarizesEligibleExcludedSentAndFailedRecipientCounts() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign()));
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(8L);
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                .thenReturn(2L);
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.SENT))
                .thenReturn(7L);
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.FAILED))
                .thenReturn(1L);

        CampaignRecipientSummaryView summary =
                campaignRecipientService.summarizeRecipients(CAMPAIGN_ID);

        assertThat(summary.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(summary.eligible()).isEqualTo(8L);
        assertThat(summary.excluded()).isEqualTo(2L);
        assertThat(summary.sent()).isEqualTo(7L);
        assertThat(summary.failed()).isEqualTo(1L);
        verify(campaignRecipientRepository)
                .countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE);
        verify(campaignRecipientRepository)
                .countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED);
        verify(campaignRecipientRepository)
                .countByCampaignIdAndEligibilityStatus(CAMPAIGN_ID, CampaignRecipientStatus.SENT);
        verify(campaignRecipientRepository)
                .countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.FAILED);
    }

    private static Campaign campaign() {
        User owner =
                User.create(
                        "campaign-recipient-generation-owner@test.example",
                        "{noop}password",
                        "Campaign Recipient Generation Owner");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Segment segment =
                Segment.create("Generation segment", null, owner, SegmentVisibility.TEAM);
        ReflectionTestUtils.setField(segment, "id", SEGMENT_ID);
        Campaign campaign =
                Campaign.create(
                        "Recipient generation campaign",
                        "Generate recipient rows",
                        owner,
                        segment,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Customer customer(UUID id, String firstName, String lastName) {
        Customer customer = Customer.create(CustomerType.INDIVIDUAL, firstName, lastName);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }
}
