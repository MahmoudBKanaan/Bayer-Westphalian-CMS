package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.method.CampaignApprovalAccess;
import com.bayerwestphalian.campaign.auth.method.CampaignWriteAccess;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

/**
 * KB item 215: CampaignService create/update/list/lifecycle with authorization and audit (create
 * audit: item 233).
 */
@ExtendWith(MockitoExtension.class)
class CampaignServiceTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID OTHER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000102");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000106");
    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignProductRepository campaignProductRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;
    @Mock private AuditService auditService;
    @Mock private CampaignRecipientRepository campaignRecipientRepository;
    @Mock private ContactEventRepository contactEventRepository;
    @Mock private CampaignMetricsRepository campaignMetricsRepository;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService =
                new CampaignService(
                        campaignRepository,
                        campaignProductRepository,
                        segmentRepository,
                        productRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertThat(
                        CampaignService.class
                                .getMethod("createCampaign", CreateCampaignCommand.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod(
                                        "updateCampaign", UUID.class, UpdateCampaignCommand.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertPreAuthorize(
                "findById", new Class<?>[] {UUID.class}, "@authz.canReadCampaigns()");
        assertPreAuthorize(
                "searchCampaigns",
                new Class<?>[] {CampaignSearchCriteria.class},
                "@authz.canReadCampaigns()");
        assertThat(
                        CampaignService.class
                                .getMethod("submitCampaign", UUID.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod(
                                        "approveCampaign",
                                        UUID.class,
                                        ApproveCampaignCommand.class)
                                .isAnnotationPresent(CampaignApprovalAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod(
                                        "rejectCampaign", UUID.class, RejectCampaignCommand.class)
                                .isAnnotationPresent(CampaignApprovalAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod(
                                        "recordComplianceReviewNotes", UUID.class, String.class)
                                .isAnnotationPresent(CampaignApprovalAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod("launchCampaign", UUID.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod("pauseCampaign", UUID.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod("completeCampaign", UUID.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod("archiveCampaign", UUID.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertThat(
                        CampaignService.class
                                .getMethod(
                                        "selectProducts",
                                        UUID.class,
                                        SelectCampaignProductsCommand.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertPreAuthorize(
                "listSelectedProductIds",
                new Class<?>[] {UUID.class},
                "@authz.canReadCampaigns()");
        assertThat(
                        CampaignService.class
                                .getMethod(
                                        "selectSegment",
                                        UUID.class,
                                        SelectCampaignSegmentCommand.class)
                                .isAnnotationPresent(CampaignWriteAccess.class))
                .isTrue();
        assertPreAuthorize(
                "getSelectedSegmentId",
                new Class<?>[] {UUID.class},
                "@authz.canReadCampaigns()");
    }

    @Test
    void createsDraftCampaignWithSegmentProductsAndAudit() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Segment segment = segment(owner);
        Product product = product();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(
                        invocation -> {
                            Campaign campaign = invocation.getArgument(0);
                            setId(campaign, CAMPAIGN_ID);
                            return campaign;
                        });
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID))
                .thenReturn(List.of(CampaignProduct.link(campaignWithId(owner, segment), product)));

        CampaignView view =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "  Life renewal  ",
                                "  Promote renewals  ",
                                SEGMENT_ID,
                                CampaignChannel.EMAIL,
                                "Subject",
                                "Body",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                List.of(PRODUCT_ID)));

        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(campaignCaptor.capture());
        Campaign saved = campaignCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Life renewal");
        assertThat(saved.getObjective()).isEqualTo("Promote renewals");
        assertThat(saved.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(saved.getChannel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(saved.getMessageSubject()).isEqualTo("Subject");
        assertThat(saved.getMessageBody()).isEqualTo("Body");
        assertThat(saved.getSegmentId()).isEqualTo(SEGMENT_ID);
        assertThat(saved.getOwnerUserId()).isEqualTo(OWNER_ID);
        assertThat(view.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(view.messageSubject()).isEqualTo("Subject");
        assertThat(view.messageBody()).isEqualTo("Body");
        assertThat(view.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(view.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(view.productIds()).containsExactly(PRODUCT_ID);

        verify(campaignProductRepository).deleteByCampaign_Id(CAMPAIGN_ID);
        verify(campaignProductRepository).saveAll(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> createAuditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logCreate(
                        eq(OWNER_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        createAuditCaptor.capture());
        assertThat(castAuditPayload(createAuditCaptor.getValue()))
                .containsEntry("id", CAMPAIGN_ID.toString())
                .containsEntry("name", "Life renewal")
                .containsEntry("status", "DRAFT")
                .containsEntry("ownerUserId", OWNER_ID.toString())
                .containsEntry("channel", "EMAIL")
                .containsEntry("messageSubject", "Subject")
                .containsEntry("messageBody", "Body")
                .containsEntry("startDate", "2026-09-01")
                .containsEntry("endDate", "2026-09-30");
    }

    @Test
    void rejectsCreateWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                campaignService.createCampaign(
                                        new CreateCampaignCommand(
                                                " ",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign validation failed")
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .contains(
                                                "Campaign name is required.",
                                                "Campaign objective is required.",
                                                "Campaign channel is required."));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void rejectsUpdateWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                campaignService.updateCampaign(
                                        CAMPAIGN_ID,
                                        new UpdateCampaignCommand(
                                                "",
                                                " ",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign validation failed")
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .contains(
                                                "Campaign name is required.",
                                                "Campaign objective is required.",
                                                "Campaign channel is required."));
        verify(campaignRepository, never()).findById(any());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void rejectsCreateWhenScheduleEndIsBeforeStartWithFormMessage() {
        assertThatThrownBy(
                        () ->
                                campaignService.createCampaign(
                                        new CreateCampaignCommand(
                                                "Life renewal",
                                                "Promote renewals",
                                                null,
                                                CampaignChannel.EMAIL,
                                                null,
                                                null,
                                                java.time.LocalDate.of(2026, 10, 15),
                                                java.time.LocalDate.of(2026, 10, 1),
                                                List.of())))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .contains("End date must not be before start date."));
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void rejectsNullCreateAndUpdateCommands() {
        assertThatThrownBy(() -> campaignService.createCampaign(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .containsExactly("command: is required"));

        assertThatThrownBy(() -> campaignService.updateCampaign(CAMPAIGN_ID, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .containsExactly("command: is required"));

        verify(campaignRepository, never()).findById(any());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void updatesOwnedDraftCampaign() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView view =
                campaignService.updateCampaign(
                        CAMPAIGN_ID,
                        new UpdateCampaignCommand(
                                "Renamed",
                                "New objective",
                                null,
                                CampaignChannel.SMS,
                                "Sub",
                                "Body",
                                LocalDate.of(2026, 10, 1),
                                LocalDate.of(2026, 10, 31),
                                List.of()));

        assertThat(view.name()).isEqualTo("Renamed");
        assertThat(view.channel()).isEqualTo(CampaignChannel.SMS);
        assertThat(view.messageSubject()).isEqualTo("Sub");
        assertThat(view.messageBody()).isEqualTo("Body");
        assertThat(view.startDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(view.endDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(view.productIds()).isEmpty();
        verify(campaignProductRepository).deleteByCampaign_Id(CAMPAIGN_ID);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logUpdate(
                        eq(OWNER_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(castAuditPayload(oldValueCaptor.getValue()))
                .containsEntry("channel", "EMAIL")
                .containsEntry("messageSubject", null)
                .containsEntry("messageBody", null)
                .containsEntry("startDate", null)
                .containsEntry("endDate", null);
        assertThat(castAuditPayload(newValueCaptor.getValue()))
                .containsEntry("channel", "SMS")
                .containsEntry("messageSubject", "Sub")
                .containsEntry("messageBody", "Body")
                .containsEntry("startDate", "2026-10-01")
                .containsEntry("endDate", "2026-10-31");
    }

    @Test
    void nonOwnerCannotUpdateCampaign() throws Exception {
        User owner = user(OWNER_ID, "Owner");
        Campaign campaign = draftCampaign(owner);
        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(
                        () ->
                                campaignService.updateCampaign(
                                        CAMPAIGN_ID,
                                        new UpdateCampaignCommand(
                                                "X",
                                                "Y",
                                                null,
                                                CampaignChannel.EMAIL,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not owned");
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void submitsDraftAndLaunchesOnlyWhenApproved() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView submitted = campaignService.submitCampaign(CAMPAIGN_ID);
        assertThat(submitted.status()).isEqualTo(CampaignStatus.SUBMITTED);

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only APPROVED");

        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(userRepository.findById(COMPLIANCE_ID)).thenReturn(Optional.of(compliance));
        CampaignView approved = campaignService.approveCampaign(CAMPAIGN_ID);
        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.approvedByUserId()).isEqualTo(COMPLIANCE_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void submittedCampaignCannotBeLaunchedBeforeComplianceOfficerApproval() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only APPROVED campaigns can be launched")
                .hasMessageContaining("current status is SUBMITTED");

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(campaign.canLaunch()).isFalse();
        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditService, never()).logLaunch(any(), any(), any(), any(), any());
    }

    @Test
    void draftCampaignCannotBeLaunchedBeforeApproval() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only APPROVED campaigns can be launched")
                .hasMessageContaining("current status is DRAFT");

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(campaign.canLaunch()).isFalse();
        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditService, never()).logLaunch(any(), any(), any(), any(), any());
    }

    @Test
    void launchCampaignWritesAuditLogForApprovedToActiveTransition() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

        // KB item 281: launch updates the campaign status to ACTIVE.
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        InOrder inOrder = inOrder(campaignRepository, auditService);
        inOrder.verify(campaignRepository).save(campaign);
        inOrder
                .verify(auditService)
                .logLaunch(
                        eq(OWNER_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(castAuditPayload(oldValueCaptor.getValue()))
                .containsEntry("status", "APPROVED");
        assertThat(castAuditPayload(newValueCaptor.getValue()))
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void launchCampaignCreatesContactEventsForEligibleRecipients() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        Customer firstCustomer = customer(UUID.fromString("20000000-0000-0000-0000-000000000280"));
        Customer secondCustomer = customer(UUID.fromString("20000000-0000-0000-0000-000000000281"));
        CampaignRecipient firstRecipient = CampaignRecipient.eligible(campaign, firstCustomer);
        CampaignRecipient secondRecipient = CampaignRecipient.eligible(campaign, secondCustomer);
        ReflectionTestUtils.setField(
                campaignService, "campaignRecipientRepository", campaignRecipientRepository);
        ReflectionTestUtils.setField(
                campaignService, "contactEventRepository", contactEventRepository);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(List.of(firstRecipient, secondRecipient));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
        when(contactEventRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContactEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(contactEventRepository).saveAll(eventsCaptor.capture());
        List<ContactEvent> events = eventsCaptor.getValue();
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(events).hasSize(2);
        assertThat(events)
                .extracting(ContactEvent::getCustomerId)
                .containsExactly(firstCustomer.getId(), secondCustomer.getId());
        assertThat(events).allSatisfy(
                event -> {
                    assertThat(event.getCampaignId()).isEqualTo(CAMPAIGN_ID);
                    assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
                    assertThat(event.getEventType()).isEqualTo(ContactEventType.SENT);
                    assertThat(event.getCreatedByUserId()).isEqualTo(OWNER_ID);
                    assertThat(event.getOccurredAt()).isNotNull();
                });
        assertThat(firstRecipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.SENT);
        assertThat(secondRecipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.SENT);
        assertThat(firstRecipient.getSentAt()).isNotNull();
        assertThat(secondRecipient.getSentAt()).isNotNull();
    }

    @Test
    void launchCampaignDoesNotCreateContactEventsWithoutEligibleRecipients() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        ReflectionTestUtils.setField(
                campaignService, "campaignRecipientRepository", campaignRecipientRepository);
        ReflectionTestUtils.setField(
                campaignService, "contactEventRepository", contactEventRepository);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(List.of());
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
        verify(campaignRecipientRepository)
                .findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE);
        verify(contactEventRepository, never()).saveAll(any());
    }

    @Test
    void launchCampaignDoesNotBypassEligibilityFilteredRecipientSnapshot() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        Customer eligibleCustomer = customer(UUID.fromString("20000000-0000-0000-0000-000000000313"));
        Customer noConsentCustomer =
                customer(UUID.fromString("20000000-0000-0000-0000-000000000314"));
        Customer optOutCustomer = customer(UUID.fromString("20000000-0000-0000-0000-000000000315"));
        Customer doNotContactCustomer =
                customer(UUID.fromString("20000000-0000-0000-0000-000000000316"));
        Customer duplicateCustomer =
                customer(UUID.fromString("20000000-0000-0000-0000-000000000317"));
        Customer frequencyLimitCustomer =
                customer(UUID.fromString("20000000-0000-0000-0000-000000000318"));
        Customer guardianConsentCustomer =
                customer(UUID.fromString("20000000-0000-0000-0000-000000000319"));
        CampaignRecipient eligibleRecipient = CampaignRecipient.eligible(campaign, eligibleCustomer);
        List<CampaignRecipient> excludedRecipients =
                List.of(
                        CampaignRecipient.excluded(
                                campaign,
                                noConsentCustomer,
                                "INVALID_CONSENT",
                                "Customer has not granted valid marketing consent"),
                        CampaignRecipient.excluded(
                                campaign,
                                optOutCustomer,
                                "MARKETING_OPT_OUT",
                                "Customer has opted out of marketing"),
                        CampaignRecipient.excluded(
                                campaign,
                                doNotContactCustomer,
                                "DO_NOT_CONTACT",
                                "Customer is marked do not contact"),
                        CampaignRecipient.excluded(
                                campaign,
                                duplicateCustomer,
                                "DUPLICATE_CAMPAIGN_RECIPIENT",
                                "Customer is already a recipient for this campaign"),
                        CampaignRecipient.excluded(
                                campaign,
                                frequencyLimitCustomer,
                                "MONTHLY_CONTACT_LIMIT",
                                "Customer has reached the monthly contact limit"),
                        CampaignRecipient.excluded(
                                campaign,
                                guardianConsentCustomer,
                                "INVALID_CONSENT",
                                "Minor beneficiary is missing guardian consent"));
        ReflectionTestUtils.setField(
                campaignService, "campaignRecipientRepository", campaignRecipientRepository);
        ReflectionTestUtils.setField(
                campaignService, "contactEventRepository", contactEventRepository);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(List.of(eligibleRecipient));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
        when(contactEventRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContactEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(contactEventRepository).saveAll(eventsCaptor.capture());
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(eventsCaptor.getValue())
                .singleElement()
                .satisfies(
                        event ->
                                assertThat(event.getCustomerId())
                                        .isEqualTo(eligibleCustomer.getId()));
        assertThat(excludedRecipients)
                .extracting(CampaignRecipient::getExclusionReason)
                .containsExactlyInAnyOrder(
                        "INVALID_CONSENT",
                        "MARKETING_OPT_OUT",
                        "DO_NOT_CONTACT",
                        "DUPLICATE_CAMPAIGN_RECIPIENT",
                        "MONTHLY_CONTACT_LIMIT",
                        "INVALID_CONSENT");
        assertThat(excludedRecipients)
                .extracting(CampaignRecipient::getCustomerId)
                .doesNotContain(eligibleCustomer.getId());
        verify(campaignRecipientRepository)
                .findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE);
        verify(campaignRecipientRepository, never())
                .findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED);
    }

    @Test
    void launchCampaignUpdatesCampaignMetricsFromRecipientCounts() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        Customer customer = customer(UUID.fromString("20000000-0000-0000-0000-000000000282"));
        CampaignRecipient eligibleRecipient = CampaignRecipient.eligible(campaign, customer);
        ReflectionTestUtils.setField(
                campaignService, "campaignRecipientRepository", campaignRecipientRepository);
        ReflectionTestUtils.setField(
                campaignService, "contactEventRepository", contactEventRepository);
        ReflectionTestUtils.setField(
                campaignService, "campaignMetricsRepository", campaignMetricsRepository);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(List.of(eligibleRecipient));
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(1L);
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                .thenReturn(2L);
        when(campaignMetricsRepository.findByCampaign_Id(CAMPAIGN_ID)).thenReturn(Optional.empty());
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
        when(contactEventRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

        ArgumentCaptor<CampaignMetrics> metricsCaptor =
                ArgumentCaptor.forClass(CampaignMetrics.class);
        verify(campaignMetricsRepository).save(metricsCaptor.capture());
        CampaignMetrics metrics = metricsCaptor.getValue();
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(metrics.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(metrics.getAudienceSize()).isEqualTo(3);
        assertThat(metrics.getEligibleCount()).isEqualTo(1);
        assertThat(metrics.getExcludedCount()).isEqualTo(2);
        assertThat(metrics.getSentCount()).isEqualTo(1);
    }

    @Test
    void launchCampaignRefreshesExistingCampaignMetrics() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        Customer firstCustomer = customer(UUID.fromString("20000000-0000-0000-0000-000000000283"));
        Customer secondCustomer = customer(UUID.fromString("20000000-0000-0000-0000-000000000284"));
        CampaignRecipient firstRecipient = CampaignRecipient.eligible(campaign, firstCustomer);
        CampaignRecipient secondRecipient = CampaignRecipient.eligible(campaign, secondCustomer);
        CampaignMetrics existingMetrics = CampaignMetrics.forCampaign(campaign);
        existingMetrics.recordLaunchCounts(4, 1, 4);
        ReflectionTestUtils.setField(
                campaignService, "campaignRecipientRepository", campaignRecipientRepository);
        ReflectionTestUtils.setField(
                campaignService, "contactEventRepository", contactEventRepository);
        ReflectionTestUtils.setField(
                campaignService, "campaignMetricsRepository", campaignMetricsRepository);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(List.of(firstRecipient, secondRecipient));
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                .thenReturn(2L);
        when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                        CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                .thenReturn(3L);
        when(campaignMetricsRepository.findByCampaign_Id(CAMPAIGN_ID))
                .thenReturn(Optional.of(existingMetrics));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
        when(contactEventRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

        verify(campaignMetricsRepository).save(existingMetrics);
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(existingMetrics.getAudienceSize()).isEqualTo(5);
        assertThat(existingMetrics.getEligibleCount()).isEqualTo(2);
        assertThat(existingMetrics.getExcludedCount()).isEqualTo(3);
        assertThat(existingMetrics.getSentCount()).isEqualTo(2);
    }

    @Test
    void approvesSubmittedCampaignAndWritesApprovalAudit() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(COMPLIANCE_ID)).thenReturn(Optional.of(compliance));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView approved =
                campaignService.approveCampaign(
                        CAMPAIGN_ID,
                        new ApproveCampaignCommand("Consent and eligibility verified."));

        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.approvedByUserId()).isEqualTo(COMPLIANCE_ID);
        assertThat(approved.complianceReviewNotes())
                .isEqualTo("Consent and eligibility verified.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logApproval(
                        eq(COMPLIANCE_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(castAuditPayload(oldValueCaptor.getValue()))
                .containsEntry("status", "SUBMITTED")
                .containsEntry("approvedByUserId", null)
                .containsEntry("complianceReviewNotes", null);
        assertThat(castAuditPayload(newValueCaptor.getValue()))
                .containsEntry("status", "APPROVED")
                .containsEntry("approvedByUserId", COMPLIANCE_ID.toString())
                .containsEntry("complianceReviewNotes", "Consent and eligibility verified.");
    }

    @Test
    void submitsDraftCampaignAndWritesSubmissionAudit() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView submitted = campaignService.submitCampaign(CAMPAIGN_ID);

        assertThat(submitted.status()).isEqualTo(CampaignStatus.SUBMITTED);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logSubmission(
                        eq(OWNER_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(castAuditPayload(oldValueCaptor.getValue()))
                .containsEntry("status", "DRAFT");
        assertThat(castAuditPayload(newValueCaptor.getValue()))
                .containsEntry("status", "SUBMITTED");
    }

    @Test
    void rejectsSubmitWhenStoredDraftIsMissingRequiredFields() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        setCampaignField(campaign, "name", " ");
        setCampaignField(campaign, "objective", null);
        setCampaignField(campaign, "channel", null);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.submitCampaign(CAMPAIGN_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign submission validation failed")
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .contains(
                                                "Campaign name is required.",
                                                "Campaign objective is required.",
                                                "Campaign channel is required."));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        verify(campaignRepository, never()).save(any());
        verify(auditService, never()).logSubmission(any(), any(), any(), any(), any());
    }

    @Test
    void ownerCannotApproveOwnCampaign() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.approveCampaign(CAMPAIGN_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("cannot approve or reject own campaign");
    }

    @Test
    void rejectsSubmittedCampaignWithReason() throws Exception {
        User owner = user(OWNER_ID, "CM");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView rejected =
                campaignService.rejectCampaign(
                        CAMPAIGN_ID,
                        new RejectCampaignCommand(
                                "Incomplete message body", "Add required consent wording"));

        assertThat(rejected.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Incomplete message body");
        assertThat(rejected.complianceReviewNotes()).isEqualTo("Add required consent wording");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logRejection(
                        eq(COMPLIANCE_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(castAuditPayload(oldValueCaptor.getValue()))
                .containsEntry("status", "SUBMITTED")
                .containsEntry("rejectionReason", null)
                .containsEntry("complianceReviewNotes", null);
        assertThat(castAuditPayload(newValueCaptor.getValue()))
                .containsEntry("status", "REJECTED")
                .containsEntry("rejectionReason", "Incomplete message body")
                .containsEntry("complianceReviewNotes", "Add required consent wording");
    }

    @Test
    void rejectCampaignRequiresNonBlankRejectionReason() throws Exception {
        User owner = user(OWNER_ID, "CM");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(
                        () ->
                                campaignService.rejectCampaign(
                                        CAMPAIGN_ID, new RejectCampaignCommand("  ")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("rejection")
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .contains("Rejection reason is required."));
        assertThatThrownBy(() -> campaignService.rejectCampaign(CAMPAIGN_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("rejection")
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getDetails())
                                        .contains("Rejection reason is required."));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(campaign.getRejectionReason()).isNull();
        verify(campaignRepository, org.mockito.Mockito.never()).save(any(Campaign.class));
    }

    @Test
    void pausesCompletesAndArchivesCampaignThroughService() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        campaign.launch();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        CampaignView paused = campaignService.pauseCampaign(CAMPAIGN_ID);
        assertThat(paused.status()).isEqualTo(CampaignStatus.PAUSED);

        CampaignView completed = campaignService.completeCampaign(CAMPAIGN_ID);
        assertThat(completed.status()).isEqualTo(CampaignStatus.COMPLETED);

        CampaignView archived = campaignService.archiveCampaign(CAMPAIGN_ID);
        assertThat(archived.status()).isEqualTo(CampaignStatus.ARCHIVED);
        verify(campaignRepository, org.mockito.Mockito.times(3)).save(campaign);
        verify(auditService, org.mockito.Mockito.times(3))
                .logUpdate(
                        eq(OWNER_ID),
                        eq(CampaignService.AUDIT_ENTITY_TYPE),
                        eq(CAMPAIGN_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void findByIdReturnsNotFoundForMissingCampaign() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.findById(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchFiltersByTermAndStatus() throws Exception {
        User owner = user(OWNER_ID, "CM");
        Campaign match = draftCampaign(owner);
        match.updateName("Life renewal Munich");
        Campaign other =
                Campaign.create(
                        "Auto cross-sell",
                        "Other objective",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        setId(other, UUID.fromString("50000000-0000-0000-0000-000000000099"));
        other.submit();

        when(campaignRepository.findAll()).thenReturn(List.of(match, other));
        when(campaignProductRepository.findByCampaignId(any())).thenReturn(List.of());

        List<CampaignView> results =
                campaignService.searchCampaigns(
                        new CampaignSearchCriteria("life", null, CampaignStatus.DRAFT, null));

        assertThat(results).extracting(CampaignView::name).containsExactly("Life renewal Munich");
    }

    private void assertPreAuthorize(String methodName, Class<?>[] params, String expression)
            throws Exception {
        Method method = CampaignService.class.getMethod(methodName, params);
        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expression);
    }

    private Campaign draftCampaign(User owner) throws Exception {
        Campaign campaign =
                Campaign.create(
                        "Draft campaign",
                        "Draft objective",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        setId(campaign, CAMPAIGN_ID);
        return campaign;
    }

    private Campaign campaignWithId(User owner, Segment segment) throws Exception {
        Campaign campaign =
                Campaign.create(
                        "Linked",
                        "Objective",
                        owner,
                        segment,
                        CampaignChannel.EMAIL);
        setId(campaign, CAMPAIGN_ID);
        return campaign;
    }

    private User user(UUID id, String name) throws Exception {
        User user =
                User.create(
                        name.toLowerCase().replace(' ', '.') + "@bayer-westphalian.test",
                        "$2a$10$hash",
                        name);
        setId(user, id);
        return user;
    }

    private Segment segment(User owner) throws Exception {
        Segment segment =
                Segment.create("Audience", null, owner, SegmentVisibility.TEAM);
        setId(segment, SEGMENT_ID);
        return segment;
    }

    private Product product() throws Exception {
        Product product =
                Product.create(
                        "Life Plan",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("49.99"),
                        12);
        setId(product, PRODUCT_ID);
        return product;
    }

    private Customer customer(UUID id) throws Exception {
        Customer customer = Customer.create(CustomerType.INDIVIDUAL, "Launch", "Customer");
        setId(customer, id);
        return customer;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private static void setCampaignField(Campaign campaign, String fieldName, Object value)
            throws Exception {
        Field field = Campaign.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(campaign, value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castAuditPayload(Map<String, ?> payload) {
        return (Map<String, Object>) payload;
    }
}
