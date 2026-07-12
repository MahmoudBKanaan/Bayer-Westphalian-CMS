package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.method.SegmentCreateAccess;
import com.bayerwestphalian.campaign.campaign.CampaignRecipientCandidate;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductType;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class SegmentServiceTests {

    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID OTHER_OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000102");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000201");

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
        // Default: all customers eligible so existing preview tests keep criteria-only focus.
        lenient()
                .when(eligibilityService.evaluateForSegmentPreview(any(UUID.class)))
                .thenReturn(EligibilityDecision.included());
        lenient().when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        lenient().when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertSegmentCreateAccessAnnotation();
        assertPreAuthorizeWithExpression(
                "updateSegment",
                new Class<?>[] {UUID.class, UpdateSegmentCommand.class},
                "@authz.canManageSegments()");
        assertPreAuthorizeWithExpression(
                "deleteSegment", new Class<?>[] {UUID.class}, "@authz.canManageSegments()");
        assertPreAuthorizeWithExpression(
                "findById", new Class<?>[] {UUID.class}, "@authz.canReadSegments()");
        assertPreAuthorizeWithExpression(
                "searchSegments",
                new Class<?>[] {SegmentSearchCriteria.class},
                "@authz.canReadSegments()");
        assertPreAuthorizeWithExpression(
                "saveCriteria",
                new Class<?>[] {UUID.class, List.class},
                "@authz.canManageSegments()");
        assertPreAuthorizeWithExpression(
                "findMatchingCustomers",
                new Class<?>[] {List.class},
                "@authz.canReadSegments()");
        assertPreAuthorizeWithExpression(
                "previewSegment",
                new Class<?>[] {SegmentPreviewCommand.class},
                "@authz.canPreviewSegments()");
        assertPreAuthorizeWithExpression(
                "evaluateCampaignRecipientCandidates",
                new Class<?>[] {UUID.class, UUID.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')");
    }

    @Test
    void createsSegmentWithOwnerCriteriaAndNormalizedFields() throws Exception {
        User owner = owner();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(segmentRepository.save(any(Segment.class)))
                .thenAnswer(
                        invocation -> {
                            Segment segment = invocation.getArgument(0);
                            setId(segment, SEGMENT_ID);
                            return segment;
                        });

        SegmentView view =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "  Munich prospects  ",
                                "  Customers in Munich  ",
                                SegmentVisibility.TEAM,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                " Munich ",
                                                null,
                                                null))));

        ArgumentCaptor<Segment> segmentCaptor = ArgumentCaptor.forClass(Segment.class);
        verify(segmentRepository).save(segmentCaptor.capture());
        Segment saved = segmentCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Munich prospects");
        assertThat(saved.getDescription()).isEqualTo("Customers in Munich");
        assertThat(saved.getOwner().getId()).isEqualTo(OWNER_ID);
        assertThat(saved.getVisibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(saved.getCriteria()).hasSize(1);
        assertThat(saved.getCriteria().getFirst().getFieldName()).isEqualTo("city");
        assertThat(saved.getCriteria().getFirst().getValue()).isEqualTo("Munich");
        assertThat(saved.getCriteria().getFirst().getJoinOperator())
                .isEqualTo(SegmentJoinOperator.AND);
        assertThat(view.id()).isEqualTo(SEGMENT_ID);
        assertThat(view.name()).isEqualTo("Munich prospects");
        assertThat(view.ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(view.criteria()).hasSize(1);
        verify(auditService)
                .logCreate(
                        eq(OWNER_ID),
                        eq(SegmentService.AUDIT_ENTITY_TYPE),
                        eq(SEGMENT_ID),
                        any(Map.class));
    }

    @Test
    void createsSegmentWritesAuditLogWithSegmentPayload() throws Exception {
        User owner = owner();
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(segmentRepository.save(any(Segment.class)))
                .thenAnswer(
                        invocation -> {
                            Segment segment = invocation.getArgument(0);
                            setId(segment, SEGMENT_ID);
                            return segment;
                        });

        segmentService.createSegment(
                new CreateSegmentCommand(
                        "Audit audience",
                        "For audit verification",
                        SegmentVisibility.TEAM,
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logCreate(
                        eq(OWNER_ID),
                        eq("segments"),
                        eq(SEGMENT_ID),
                        payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("name", "Audit audience")
                .containsEntry("description", "For audit verification")
                .containsEntry("visibility", "TEAM")
                .containsEntry("ownerUserId", OWNER_ID.toString())
                .containsEntry("criteriaCount", 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> criteria =
                (List<Map<String, Object>>) payloadCaptor.getValue().get("criteria");
        assertThat(criteria).hasSize(1);
        assertThat(criteria.getFirst()).containsEntry("fieldName", "city");
        assertThat(criteria.getFirst()).containsEntry("value", "Munich");
        assertThat(criteria.getFirst()).containsEntry("joinOperator", "AND");
    }

    @Test
    void updatesOwnedSegmentAndReplacesCriteria() throws Exception {
        User owner = owner();
        Segment segment = ownedSegment(owner);
        segment.addCriteria("country", SegmentOperator.EQUALS, "Germany");
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SegmentView view =
                segmentService.updateSegment(
                        SEGMENT_ID,
                        new UpdateSegmentCommand(
                                "Updated audience",
                                "Refined targeting",
                                SegmentVisibility.PRIVATE,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                null,
                                                SegmentJoinOperator.AND))));

        verify(segmentCriteriaRepository).deleteAll(any());
        assertThat(segment.getName()).isEqualTo("Updated audience");
        assertThat(segment.getDescription()).isEqualTo("Refined targeting");
        assertThat(segment.getVisibility()).isEqualTo(SegmentVisibility.PRIVATE);
        assertThat(segment.getCriteria()).hasSize(1);
        assertThat(segment.getCriteria().getFirst().getFieldName()).isEqualTo("city");
        assertThat(view.name()).isEqualTo("Updated audience");
        verify(auditService)
                .logUpdate(
                        eq(OWNER_ID),
                        eq("segments"),
                        eq(SEGMENT_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void updatesSegmentWritesAuditLogWithOldAndNewValues() throws Exception {
        User owner = owner();
        Segment segment = ownedSegment(owner);
        segment.addCriteria("country", SegmentOperator.EQUALS, "Germany");
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        segmentService.updateSegment(
                SEGMENT_ID,
                new UpdateSegmentCommand(
                        "Renamed segment",
                        "New description",
                        SegmentVisibility.GLOBAL,
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Hamburg",
                                        null,
                                        SegmentJoinOperator.AND))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map> oldCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map> newCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logUpdate(
                        eq(OWNER_ID),
                        eq("segments"),
                        eq(SEGMENT_ID),
                        oldCaptor.capture(),
                        newCaptor.capture());
        assertThat(oldCaptor.getValue()).containsEntry("name", "Owned audience");
        assertThat(newCaptor.getValue())
                .containsEntry("name", "Renamed segment")
                .containsEntry("description", "New description")
                .containsEntry("visibility", "GLOBAL")
                .containsEntry("criteriaCount", 1);
    }

    @Test
    void rejectsSegmentUpdatesFromNonOwnerCampaignManager() throws Exception {
        Segment segment = ownedSegment(owner());
        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        assertThatThrownBy(
                        () ->
                                segmentService.updateSegment(
                                        SEGMENT_ID,
                                        new UpdateSegmentCommand(
                                                "Updated audience",
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Segment is not owned by the current user");
        verify(auditService, never()).logUpdate(any(), any(), any(), any(), any());
    }

    @Test
    void allowsAdminToDeleteSegmentTheyDoNotOwn() throws Exception {
        Segment segment = ownedSegment(owner());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(true);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        segmentService.deleteSegment(SEGMENT_ID);

        verify(segmentRepository).delete(segment);
        verify(auditService)
                .logDelete(
                        eq(OWNER_ID),
                        eq("segments"),
                        eq(SEGMENT_ID),
                        any(Map.class),
                        isNull());
    }

    @Test
    void deletesSegmentWritesAuditLogWithOldPayload() throws Exception {
        User owner = owner();
        Segment segment = ownedSegment(owner);
        segment.addCriteria("city", SegmentOperator.EQUALS, "Munich");
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        segmentService.deleteSegment(SEGMENT_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map> oldCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logDelete(
                        eq(OWNER_ID),
                        eq("segments"),
                        eq(SEGMENT_ID),
                        oldCaptor.capture(),
                        isNull());
        assertThat(oldCaptor.getValue())
                .containsEntry("name", segment.getName())
                .containsEntry("visibility", segment.getVisibility().name())
                .containsEntry("criteriaCount", 1);
        verify(segmentRepository).delete(segment);
    }

    @Test
    void saveCriteriaWritesAuditLogForSegmentChange() throws Exception {
        User owner = owner();
        Segment segment = ownedSegment(owner);
        segment.addCriteria("country", SegmentOperator.EQUALS, "Germany");
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        segmentService.saveCriteria(
                SEGMENT_ID,
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Cologne",
                                "location",
                                SegmentJoinOperator.AND)));

        verify(auditService)
                .logUpdate(
                        eq(OWNER_ID),
                        eq("segments"),
                        eq(SEGMENT_ID),
                        any(Map.class),
                        any(Map.class));
        assertThat(segment.getCriteria()).hasSize(1);
        assertThat(segment.getCriteria().getFirst().getFieldName()).isEqualTo("city");
        assertThat(segment.getCriteria().getFirst().getValue()).isEqualTo("Cologne");
    }

    @Test
    void rejectsSegmentDeletesFromNonOwnerCampaignManager() throws Exception {
        Segment segment = ownedSegment(owner());
        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        assertThatThrownBy(() -> segmentService.deleteSegment(SEGMENT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Segment is not owned by the current user");
    }

    @Test
    void loadsSegmentDetailsById() throws Exception {
        User owner = owner();
        Segment segment = ownedSegment(owner);
        segment.addCriteria("city", SegmentOperator.EQUALS, "Munich", "location", SegmentJoinOperator.AND);
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        SegmentView view = segmentService.findById(SEGMENT_ID);

        assertThat(view.id()).isEqualTo(SEGMENT_ID);
        assertThat(view.name()).isEqualTo("Owned audience");
        assertThat(view.description()).isEqualTo("Private draft");
        assertThat(view.ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(view.ownerFullName()).isEqualTo("Segment Owner");
        assertThat(view.visibility()).isEqualTo(SegmentVisibility.PRIVATE);
        assertThat(view.criteria()).hasSize(1);
        assertThat(view.criteria().getFirst().fieldName()).isEqualTo("city");
        assertThat(view.criteria().getFirst().value()).isEqualTo("Munich");
    }

    @Test
    void allowsAdminToLoadPrivateSegmentOwnedByAnotherUser() throws Exception {
        Segment segment = ownedSegment(owner());
        segment.changeVisibility(SegmentVisibility.PRIVATE);
        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(true);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        SegmentView view = segmentService.findById(SEGMENT_ID);

        assertThat(view.id()).isEqualTo(SEGMENT_ID);
        assertThat(view.visibility()).isEqualTo(SegmentVisibility.PRIVATE);
    }

    @Test
    void rejectsPrivateSegmentReadForNonOwner() throws Exception {
        Segment segment = ownedSegment(owner());
        segment.changeVisibility(SegmentVisibility.PRIVATE);
        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));

        assertThatThrownBy(() -> segmentService.findById(SEGMENT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Private segment is not accessible");
    }

    @Test
    void searchSegmentsFiltersPrivateSegmentsForNonOwners() throws Exception {
        User owner = owner();
        Segment privateOwned =
                Segment.create("Private audience", null, owner, SegmentVisibility.PRIVATE);
        Segment globalAudience =
                Segment.create("Global audience", null, null, SegmentVisibility.GLOBAL);
        setId(privateOwned, SEGMENT_ID);
        setId(
                globalAudience,
                UUID.fromString("42000000-0000-0000-0000-000000000002"));

        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findAll()).thenReturn(List.of(privateOwned, globalAudience));

        List<SegmentView> views = segmentService.searchSegments(new SegmentSearchCriteria(null, null, null));

        assertThat(views).extracting(SegmentView::name).containsExactly("Global audience");
    }

    @Test
    void saveCriteriaReplacesExistingRulesOnOwnedSegment() throws Exception {
        User owner = owner();
        Segment segment = ownedSegment(owner);
        segment.addCriteria("country", SegmentOperator.EQUALS, "Germany");
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(segmentRepository.findById(SEGMENT_ID)).thenReturn(Optional.of(segment));
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SegmentView view =
                segmentService.saveCriteria(
                        SEGMENT_ID,
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "status",
                                        SegmentOperator.EQUALS,
                                        "ACTIVE",
                                        null,
                                        SegmentJoinOperator.AND)));

        verify(segmentCriteriaRepository).deleteAll(any());
        assertThat(segment.getCriteria()).hasSize(1);
        assertThat(segment.getCriteria().getFirst().getFieldName()).isEqualTo("status");
        assertThat(view.criteria()).hasSize(1);
    }

    @Test
    void findMatchingCustomersAppliesCityAndCustomerTypeCriteria() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(munichProspect, CUSTOMER_ID);
        setId(berlinCustomer, UUID.fromString("20000000-0000-0000-0000-000000000202"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void findMatchingCustomersAppliesAndLogicWhenJoinOperatorOmitted() throws Exception {
        // KB FR-078: null join_operator defaults to AND — both city and type must match.
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer munichCustomer = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        Customer berlinProspect = customer("Tom", "Schmidt", "Berlin", CustomerType.PROSPECT);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, munichCustomer, berlinProspect));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        null),
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        null)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
    }

    @Test
    void findMatchingCustomersAndChainExcludesPartialMatches() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer munichCustomer = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        Customer berlinProspect = customer("Tom", "Schmidt", "Berlin", CustomerType.PROSPECT);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, munichCustomer, berlinProspect));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        "audience",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "country",
                                        SegmentOperator.EQUALS,
                                        "Germany",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
        assertThat(matches.getFirst().customerType()).isEqualTo(CustomerType.PROSPECT);
    }

    @Test
    void findMatchingCustomersAndLogicReturnsEmptyWhenNoIntersection() throws Exception {
        // KB item 196: AND never returns profiles that fail any criterion.
        Customer munichCustomer = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        Customer berlinProspect = customer("Tom", "Schmidt", "Berlin", CustomerType.PROSPECT);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichCustomer, berlinProspect));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        "audience",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAndLogicReturnsCorrectIntersectionCounts() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer munichCustomer = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        Customer berlinProspect = customer("Tom", "Schmidt", "Berlin", CustomerType.PROSPECT);
        setId(munichProspect, CUSTOMER_ID);
        setId(munichCustomer, UUID.fromString("20000000-0000-0000-0000-000000000203"));
        setId(berlinProspect, UUID.fromString("20000000-0000-0000-0000-000000000204"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, munichCustomer, berlinProspect));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void findMatchingCustomersSupportsOrJoinOperator() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Berlin",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches).extracting(CustomerView::city).containsExactlyInAnyOrder("Munich", "Berlin");
    }

    @Test
    void findMatchingCustomersOrLogicReturnsEmptyWhenNoBranchMatches() throws Exception {
        // KB item 197: OR returns empty when every disjunct fails.
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Vienna",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Zurich",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentOrLogicReturnsCorrectUnionCounts() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer hamburgCustomer = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        setId(munichProspect, CUSTOMER_ID);
        setId(berlinCustomer, UUID.fromString("20000000-0000-0000-0000-000000000205"));
        setId(hamburgCustomer, UUID.fromString("20000000-0000-0000-0000-000000000206"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer, hamburgCustomer));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                null,
                                                SegmentJoinOperator.OR))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Berlin");
        assertThat(preview.matchingCustomers())
                .noneMatch(view -> "Hamburg".equals(view.city()));
    }

    @Test
    void findMatchingCustomersSupportsCrossFieldOrJoinOperator() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer hamburgCustomer = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer, hamburgCustomer));

        // PROSPECT OR city=Munich => Lena (prospect) and any Munich residents
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactly("Lena Mueller");
    }

    @Test
    void findMatchingCustomersSupportsOrAcrossCustomerTypeAndCityForMunichCustomers()
            throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer munichCustomer = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, munichCustomer, berlinCustomer));

        // PROSPECT OR city=Munich => Lena + Anna (not Tom)
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
    }

    @Test
    void findMatchingCustomersSupportsThreeWayOrChain() throws Exception {
        Customer munich = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlin = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer hamburg = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        Customer cologne = customer("Kai", "Fischer", "Cologne", CustomerType.PROSPECT);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munich, berlin, hamburg, cologne));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Berlin",
                                        null,
                                        SegmentJoinOperator.OR),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Hamburg",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Berlin", "Hamburg");
    }

    @Test
    void findMatchingCustomersSupportsMixedAndOrLeftAssociativity() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer munichCustomer = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, munichCustomer, berlinCustomer));

        // (PROSPECT AND Munich) OR Berlin => Lena + Tom
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Berlin",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Tom Schmidt");
    }

    @Test
    void findMatchingCustomersOrDoesNotMatchWhenNoBranchMatches() throws Exception {
        Customer hamburg = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(hamburg));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Berlin",
                                        null,
                                        SegmentJoinOperator.OR)));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentReturnsAudienceCountAndMatches() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(munichProspect, CUSTOMER_ID);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(munichProspect));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.exclusionReasonSummary()).isEmpty();
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().city()).isEqualTo("Munich");
        verify(eligibilityService).evaluateForSegmentPreview(CUSTOMER_ID);
    }

    @Test
    void previewSegmentWithEmptyCriteriaReturnsAllActiveCustomers() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(munichProspect, CUSTOMER_ID);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(berlinCustomer, UUID.fromString("20000000-0000-0000-0000-000000000202"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        SegmentPreviewView preview =
                segmentService.previewSegment(new SegmentPreviewCommand(List.of()));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).hasSize(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Berlin");
    }

    @Test
    void previewSegmentTotalAudienceCountIncludesIneligibleCriteriaMatches() throws Exception {
        Customer eligible = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligible, CUSTOMER_ID);
        Customer ineligible = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        UUID ineligibleId = UUID.fromString("20000000-0000-0000-0000-000000000299");
        setId(ineligible, ineligibleId);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, ineligible));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ineligibleId))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        // FR-079: total audience = criteria matches (2), even when one is eligibility-excluded
        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void previewSegmentReturnsEligibleCountOfContactableMatches() throws Exception {
        Customer eligibleOne = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligibleOne, CUSTOMER_ID);
        Customer eligibleTwo = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        UUID eligibleTwoId = UUID.fromString("20000000-0000-0000-0000-000000000298");
        setId(eligibleTwo, eligibleTwoId);
        Customer blocked = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        UUID blockedId = UUID.fromString("20000000-0000-0000-0000-000000000297");
        setId(blocked, blockedId);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(eligibleOne, eligibleTwo, blocked));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(eligibleTwoId))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(blockedId))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
    }

    @Test
    void previewSegmentReturnsZeroEligibleCountWhenAllMatchesAreIneligible() throws Exception {
        Customer blocked = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        setId(blocked, CUSTOMER_ID);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(blocked));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).isEmpty();
    }

    @Test
    void previewSegmentReturnsExcludedCountOfIneligibleMatches() throws Exception {
        Customer eligible = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligible, CUSTOMER_ID);
        Customer optOut = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        UUID optOutId = UUID.fromString("20000000-0000-0000-0000-000000000296");
        setId(optOut, optOutId);
        Customer dnc = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        UUID dncId = UUID.fromString("20000000-0000-0000-0000-000000000295");
        setId(dnc, dncId);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, optOut, dnc));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(optOutId))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));
        when(eligibilityService.evaluateForSegmentPreview(dncId))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.exclusionReasonSummary()).hasSize(2);
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactlyInAnyOrder("DO_NOT_CONTACT", "MARKETING_OPT_OUT");
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::count)
                .containsExactlyInAnyOrder(1, 1);
    }

    @Test
    void previewSegmentReturnsEligibleAndExcludedCountsWithFr079Invariants() throws Exception {
        // KB item 199: eligible + excluded = total; matchingCustomers size = eligibleCount.
        Customer eligibleOne = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligibleOne, CUSTOMER_ID);
        Customer eligibleTwo = customer("Sara", "Klein", "Munich", CustomerType.CUSTOMER);
        UUID eligibleTwoId = UUID.fromString("20000000-0000-0000-0000-000000000291");
        setId(eligibleTwo, eligibleTwoId);
        Customer blocked = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        UUID blockedId = UUID.fromString("20000000-0000-0000-0000-000000000290");
        setId(blocked, blockedId);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(eligibleOne, eligibleTwo, blocked));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(eligibleTwoId))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(blockedId))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.matchingCustomers()).hasSize(preview.eligibleCount());
        assertThat(preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount() - preview.eligibleCount());
    }

    @Test
    void previewSegmentReturnsExclusionReasonSummaryAggregatedByCode() throws Exception {
        Customer eligible = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligible, CUSTOMER_ID);
        Customer dncOne = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        UUID dncOneId = UUID.fromString("20000000-0000-0000-0000-000000000294");
        setId(dncOne, dncOneId);
        Customer dncTwo = customer("Anna", "Weber", "Munich", CustomerType.CUSTOMER);
        UUID dncTwoId = UUID.fromString("20000000-0000-0000-0000-000000000293");
        setId(dncTwo, dncTwoId);
        Customer optOut = customer("Kai", "Fischer", "Munich", CustomerType.CUSTOMER);
        UUID optOutId = UUID.fromString("20000000-0000-0000-0000-000000000292");
        setId(optOut, optOutId);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(eligible, dncOne, dncTwo, optOut));
        when(eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(dncOneId))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(dncTwoId))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(optOutId))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(4);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(3);
        assertThat(preview.exclusionReasonSummary()).hasSize(2);
        assertThat(preview.exclusionReasonSummary().getFirst().code()).isEqualTo("DO_NOT_CONTACT");
        assertThat(preview.exclusionReasonSummary().getFirst().count()).isEqualTo(2);
        assertThat(preview.exclusionReasonSummary().getFirst().message())
                .isEqualTo(EligibilityExclusionReason.DO_NOT_CONTACT.explanation());
        assertThat(preview.exclusionReasonSummary().get(1).code()).isEqualTo("MARKETING_OPT_OUT");
        assertThat(preview.exclusionReasonSummary().get(1).count()).isEqualTo(1);
        assertThat(
                        preview.exclusionReasonSummary().stream()
                                .mapToInt(SegmentExclusionReasonSummary::count)
                                .sum())
                .isEqualTo(preview.excludedCount());
    }

    @Test
    void previewSegmentReturnsEmptyExclusionReasonSummaryWhenNoneExcluded() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(munichProspect, CUSTOMER_ID);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(munichProspect));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.exclusionReasonSummary()).isEmpty();
    }

    @Test
    void previewCampaignRecipientsAppliesCampaignScopedEligibility() throws Exception {
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000001");
        UUID duplicateId = UUID.fromString("20000000-0000-0000-0000-000000000202");
        Customer eligible = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligible, CUSTOMER_ID);
        Customer duplicate = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        setId(duplicate, duplicateId);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, duplicate));
        when(eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, campaignId))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForCampaignPreview(duplicateId, campaignId))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.DUPLICATE_CAMPAIGN_RECIPIENT));

        SegmentPreviewView preview = segmentService.previewCampaignRecipients(campaignId, null);

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().id()).isEqualTo(CUSTOMER_ID);
        assertThat(preview.exclusionReasonSummary()).hasSize(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code())
                .isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
        verify(eligibilityService).evaluateForCampaignPreview(CUSTOMER_ID, campaignId);
        verify(eligibilityService).evaluateForCampaignPreview(duplicateId, campaignId);
    }

    @Test
    void evaluatesCampaignRecipientCandidatesWithRowLevelEligibilityDecisions() throws Exception {
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000267");
        UUID blockedId = UUID.fromString("20000000-0000-0000-0000-000000000267");
        Customer eligible = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(eligible, CUSTOMER_ID);
        Customer blocked = customer("Tom", "Schmidt", "Munich", CustomerType.CUSTOMER);
        setId(blocked, blockedId);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, blocked));
        when(eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, campaignId))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForCampaignPreview(blockedId, campaignId))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        List<CampaignRecipientCandidate> candidates =
                segmentService.evaluateCampaignRecipientCandidates(campaignId, null);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(candidates.get(0).eligible()).isTrue();
        assertThat(candidates.get(1).customerId()).isEqualTo(blockedId);
        assertThat(candidates.get(1).eligible()).isFalse();
        assertThat(candidates.get(1).exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(candidates.get(1).eligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");
    }

    @Test
    void previewSegmentReturnsZeroTotalAudienceCountWhenNoCriteriaMatches() throws Exception {
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(berlinCustomer, CUSTOMER_ID);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(berlinCustomer));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(0);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).isEmpty();
    }

    @Test
    void findMatchingCustomersFiltersByAgeGroupUsingKbDatabaseValues() throws Exception {
        Customer youngProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        youngProspect.updateDemographics(null, CustomerAgeGroup.AGE_18_25);
        Customer matureCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        matureCustomer.updateDemographics(null, CustomerAgeGroup.AGE_41_60);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(youngProspect, matureCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "age_group",
                                        SegmentOperator.EQUALS,
                                        "AGE_18_25",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().ageGroup()).isEqualTo(CustomerAgeGroup.AGE_18_25);
    }

    @Test
    void findMatchingCustomersSupportsInOperatorForMultipleAgeGroups() throws Exception {
        Customer youngProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        youngProspect.updateDemographics(null, CustomerAgeGroup.AGE_18_25);
        Customer matureCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        matureCustomer.updateDemographics(null, CustomerAgeGroup.AGE_41_60);
        Customer seniorCustomer = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        seniorCustomer.updateDemographics(null, CustomerAgeGroup.AGE_60_PLUS);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(youngProspect, matureCustomer, seniorCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "agegroup",
                                        SegmentOperator.IN,
                                        "18_25,AGE_60_PLUS",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
    }

    @Test
    void findMatchingCustomersFiltersByCity() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void findMatchingCustomersFiltersByCountryAndCityTogether() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer viennaCustomer = customer("Anna", "Weber", "Vienna", CustomerType.CUSTOMER);
        viennaCustomer.updateAddress(null, "Vienna", "Austria");
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, viennaCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "country",
                                        SegmentOperator.EQUALS,
                                        "Germany",
                                        "location",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().country()).isEqualTo("Germany");
    }

    @Test
    void findMatchingCustomersSupportsLocationAliasAndContainsOperator() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "location",
                                        SegmentOperator.CONTAINS,
                                        "mun",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void findMatchingCustomersSupportsInOperatorForMultipleCities() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer hamburgCustomer = customer("Kai", "Fischer", "Hamburg", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer, hamburgCustomer));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.IN,
                                        "Munich, Berlin",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Berlin");
    }

    @Test
    void rejectsOverlongLocationCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "city",
                                                        SegmentOperator.EQUALS,
                                                        "x".repeat(101),
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void findMatchingCustomersFiltersByCustomerType() throws Exception {
        Customer prospect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer policyholder = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(prospect, policyholder));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "prospect",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().customerType()).isEqualTo(CustomerType.PROSPECT);
    }

    @Test
    void findMatchingCustomersFiltersByCustomerStatus() throws Exception {
        Customer interested = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        interested.changeStatus(CustomerStatus.INTERESTED);
        Customer uninterested = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        uninterested.changeStatus(CustomerStatus.UNINTERESTED);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(interested, uninterested));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "status",
                                        SegmentOperator.EQUALS,
                                        "interested",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().status()).isEqualTo(CustomerStatus.INTERESTED);
    }

    @Test
    void findMatchingCustomersSupportsBehaviorAliasAndExcludesUninterested() throws Exception {
        Customer interested = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        interested.changeStatus(CustomerStatus.INTERESTED);
        Customer converted = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        converted.changeStatus(CustomerStatus.CONVERTED);
        Customer uninterested = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        uninterested.changeStatus(CustomerStatus.UNINTERESTED);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(interested, converted, uninterested));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "behavior",
                                        SegmentOperator.IN,
                                        "INTERESTED, CONVERTED",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
    }

    @Test
    void findMatchingCustomersFiltersByDoNotContactAndInterestSource() throws Exception {
        Customer contactable =
                customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        contactable.recordSource("LIFE_INSURANCE_BENEFICIARY");
        Customer blocked = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        blocked.recordSource("LIFE_INSURANCE_BENEFICIARY");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(contactable, blocked));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "do_not_contact",
                                        SegmentOperator.EQUALS,
                                        "false",
                                        null,
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "interest",
                                        SegmentOperator.CONTAINS,
                                        "LIFE_INSURANCE",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().doNotContact()).isFalse();
    }

    @Test
    void rejectsUnsupportedBehaviorStatusCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "status",
                                                        SegmentOperator.EQUALS,
                                                        "WARM_LEAD",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void findMatchingCustomersFiltersByConsentStatus() throws Exception {
        Customer withGivenConsent = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(withGivenConsent, UUID.fromString("20000000-0000-0000-0000-000000000501"));
        Customer withoutConsent = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(withoutConsent, UUID.fromString("20000000-0000-0000-0000-000000000502"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(withGivenConsent, withoutConsent));
        when(consentRepository.findByCustomerId(withGivenConsent.getId()))
                .thenReturn(List.of(givenMarketingConsent(withGivenConsent)));
        when(consentRepository.findByCustomerId(withoutConsent.getId())).thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_status",
                                        SegmentOperator.EQUALS,
                                        "given",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void findMatchingCustomersFiltersByValidMarketingConsentAndOptOut() throws Exception {
        Customer consented = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(consented, UUID.fromString("20000000-0000-0000-0000-000000000511"));
        Customer optedOut = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(optedOut, UUID.fromString("20000000-0000-0000-0000-000000000512"));
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(consented, optedOut));
        when(consentRepository.findByCustomerId(consented.getId()))
                .thenReturn(List.of(givenMarketingConsent(consented)));
        when(consentRepository.findByCustomerId(optedOut.getId()))
                .thenReturn(List.of(withdrawnMarketingConsent(optedOut)));

        List<CustomerView> withValidConsent =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "has_valid_marketing_consent",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        null,
                                        SegmentJoinOperator.AND)));
        List<CustomerView> withoutOptOut =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "opt_out",
                                        SegmentOperator.EQUALS,
                                        "false",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(withValidConsent).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
        assertThat(withoutOptOut).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
    }

    @Test
    void findMatchingCustomersSupportsConsentTypeAndGuardianConsent() throws Exception {
        Customer guardianReady = customer("Lena", "Mueller", "Munich", CustomerType.BENEFICIARY);
        setId(guardianReady, UUID.fromString("20000000-0000-0000-0000-000000000521"));
        Customer emailOnly = customer("Tom", "Schmidt", "Berlin", CustomerType.PROSPECT);
        setId(emailOnly, UUID.fromString("20000000-0000-0000-0000-000000000522"));
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(guardianReady, emailOnly));
        when(consentRepository.findByCustomerId(guardianReady.getId()))
                .thenReturn(List.of(givenGuardianConsent(guardianReady)));
        when(consentRepository.findByCustomerId(emailOnly.getId()))
                .thenReturn(List.of(givenMarketingConsent(emailOnly)));

        List<CustomerView> byType =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "consent_type",
                                        SegmentOperator.EQUALS,
                                        "guardian",
                                        null,
                                        SegmentJoinOperator.AND)));
        List<CustomerView> byGuardian =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "guardian_consent",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(byType).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
        assertThat(byGuardian).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
    }

    @Test
    void rejectsUnsupportedConsentStatusCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "consent_status",
                                                        SegmentOperator.EQUALS,
                                                        "PENDING",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void findMatchingCustomersSupportsInOperatorForMultipleCustomerTypes() throws Exception {
        Customer prospect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer policyholder = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        Customer beneficiary = customer("Anna", "Weber", "Hamburg", CustomerType.BENEFICIARY);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(prospect, policyholder, beneficiary));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "type",
                                        SegmentOperator.IN,
                                        "CUSTOMER, beneficiary",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(CustomerView::customerType)
                .containsExactlyInAnyOrder(CustomerType.CUSTOMER, CustomerType.BENEFICIARY);
    }

    @Test
    void findMatchingCustomersSupportsNotEqualsForCustomerType() throws Exception {
        Customer prospect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer policyholder = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(prospect, policyholder));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "customertype",
                                        SegmentOperator.NOT_EQUALS,
                                        "CUSTOMER",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().customerType()).isEqualTo(CustomerType.PROSPECT);
    }

    @Test
    void findMatchingCustomersFiltersByOwnedProductType() throws Exception {
        Customer withLifeInsurance = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(withLifeInsurance, UUID.fromString("20000000-0000-0000-0000-000000000301"));
        Customer withoutHomeInsurance = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(withoutHomeInsurance, UUID.fromString("20000000-0000-0000-0000-000000000302"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(withLifeInsurance, withoutHomeInsurance));
        when(productOwnershipRepository.findByCustomerId(withLifeInsurance.getId()))
                .thenReturn(List.of(activeOwnership(withLifeInsurance, ProductType.LIFE_INSURANCE)));
        when(productOwnershipRepository.findByCustomerId(withoutHomeInsurance.getId()))
                .thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_type",
                                        SegmentOperator.EQUALS,
                                        "life_insurance",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void findMatchingCustomersSupportsNotEqualsForMissingProductOwnership() throws Exception {
        Customer withHomeInsurance = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(withHomeInsurance, UUID.fromString("20000000-0000-0000-0000-000000000311"));
        Customer withoutHomeInsurance = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(withoutHomeInsurance, UUID.fromString("20000000-0000-0000-0000-000000000312"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(withHomeInsurance, withoutHomeInsurance));
        when(productOwnershipRepository.findByCustomerId(withHomeInsurance.getId()))
                .thenReturn(
                        List.of(activeOwnership(withHomeInsurance, ProductType.HOMEOWNER_INSURANCE)));
        when(productOwnershipRepository.findByCustomerId(withoutHomeInsurance.getId()))
                .thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_ownership",
                                        SegmentOperator.NOT_EQUALS,
                                        "HOMEOWNER_INSURANCE",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void findMatchingCustomersSupportsInOperatorForMultipleOwnedProductTypes() throws Exception {
        Customer lifeCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(lifeCustomer, UUID.fromString("20000000-0000-0000-0000-000000000321"));
        Customer fundCustomer = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        setId(fundCustomer, UUID.fromString("20000000-0000-0000-0000-000000000322"));
        Customer otherCustomer = customer("Kai", "Fischer", "Cologne", CustomerType.PROSPECT);
        setId(otherCustomer, UUID.fromString("20000000-0000-0000-0000-000000000323"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(lifeCustomer, fundCustomer, otherCustomer));
        when(productOwnershipRepository.findByCustomerId(lifeCustomer.getId()))
                .thenReturn(List.of(activeOwnership(lifeCustomer, ProductType.LIFE_INSURANCE)));
        when(productOwnershipRepository.findByCustomerId(fundCustomer.getId()))
                .thenReturn(List.of(activeOwnership(fundCustomer, ProductType.INVESTMENT_FUND)));
        when(productOwnershipRepository.findByCustomerId(otherCustomer.getId()))
                .thenReturn(List.of(activeOwnership(otherCustomer, ProductType.AUTO_INSURANCE)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "owned_product_type",
                                        SegmentOperator.IN,
                                        "LIFE_INSURANCE, INVESTMENT_FUND",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
    }

    @Test
    void rejectsUnsupportedProductOwnershipCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "product_type",
                                                        SegmentOperator.EQUALS,
                                                        "TRAVEL_INSURANCE",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void findMatchingCustomersFiltersByProductExpirationWithinMonths() throws Exception {
        Customer expiringSoon = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(expiringSoon, UUID.fromString("20000000-0000-0000-0000-000000000331"));
        Customer farFuture = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(farFuture, UUID.fromString("20000000-0000-0000-0000-000000000332"));
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(expiringSoon, farFuture));
        when(productOwnershipRepository.findByCustomerId(expiringSoon.getId()))
                .thenReturn(List.of(ownershipExpiringInMonths(expiringSoon, 2)));
        when(productOwnershipRepository.findByCustomerId(farFuture.getId()))
                .thenReturn(List.of(ownershipExpiringInMonths(farFuture, 18)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiring_within_months",
                                        SegmentOperator.EQUALS,
                                        "3",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void findMatchingCustomersSupportsProductExpirationAliasAndIsExpiring() throws Exception {
        Customer expiring = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(expiring, UUID.fromString("20000000-0000-0000-0000-000000000341"));
        Customer notExpiring = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(notExpiring, UUID.fromString("20000000-0000-0000-0000-000000000342"));
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(expiring, notExpiring));
        when(productOwnershipRepository.findByCustomerId(expiring.getId()))
                .thenReturn(List.of(ownershipExpiringInMonths(expiring, 5)));
        when(productOwnershipRepository.findByCustomerId(notExpiring.getId()))
                .thenReturn(List.of(ownershipExpiringInMonths(notExpiring, 18)));

        List<CustomerView> byAlias =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_expiration",
                                        SegmentOperator.EQUALS,
                                        "6",
                                        null,
                                        SegmentJoinOperator.AND)));
        List<CustomerView> byIsExpiring =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "is_expiring",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(byAlias).extracting(CustomerView::fullName).containsExactly("Tom Schmidt");
        assertThat(byIsExpiring).extracting(CustomerView::fullName).containsExactly("Tom Schmidt");
    }

    @Test
    void findMatchingCustomersSupportsExpirationDateComparison() throws Exception {
        Customer expiringInOctober = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(expiringInOctober, UUID.fromString("20000000-0000-0000-0000-000000000351"));
        Customer expiringLater = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(expiringLater, UUID.fromString("20000000-0000-0000-0000-000000000352"));
        LocalDate octoberExpiration = LocalDate.now().plusMonths(3);
        LocalDate farExpiration = LocalDate.now().plusYears(2);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(expiringInOctober, expiringLater));
        when(productOwnershipRepository.findByCustomerId(expiringInOctober.getId()))
                .thenReturn(List.of(ownershipExpiringOn(expiringInOctober, octoberExpiration)));
        when(productOwnershipRepository.findByCustomerId(expiringLater.getId()))
                .thenReturn(List.of(ownershipExpiringOn(expiringLater, farExpiration)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiration_date",
                                        SegmentOperator.BEFORE,
                                        farExpiration.toString(),
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void rejectsUnsupportedProductExpirationCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "expiring_within_months",
                                                        SegmentOperator.EQUALS,
                                                        "soon",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void findMatchingCustomersFiltersByPaymentStatus() throws Exception {
        Customer overdueCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(overdueCustomer, UUID.fromString("20000000-0000-0000-0000-000000000401"));
        Customer paidCustomer = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(paidCustomer, UUID.fromString("20000000-0000-0000-0000-000000000402"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(overdueCustomer, paidCustomer));
        when(paymentRecordRepository.findByCustomerId(overdueCustomer.getId()))
                .thenReturn(List.of(overduePayment(overdueCustomer)));
        when(paymentRecordRepository.findByCustomerId(paidCustomer.getId()))
                .thenReturn(List.of(paidPayment(paidCustomer)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_status",
                                        SegmentOperator.EQUALS,
                                        "overdue",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void findMatchingCustomersSupportsPaymentHistoryAliasWithoutDefaultRisk() throws Exception {
        Customer defaultRiskCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(defaultRiskCustomer, UUID.fromString("20000000-0000-0000-0000-000000000411"));
        Customer healthyCustomer = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(healthyCustomer, UUID.fromString("20000000-0000-0000-0000-000000000412"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(defaultRiskCustomer, healthyCustomer));
        when(paymentRecordRepository.findByCustomerId(defaultRiskCustomer.getId()))
                .thenReturn(List.of(defaultRiskPayment(defaultRiskCustomer)));
        when(paymentRecordRepository.findByCustomerId(healthyCustomer.getId()))
                .thenReturn(List.of(paidPayment(healthyCustomer)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_history",
                                        SegmentOperator.NOT_EQUALS,
                                        "DEFAULT_RISK",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void findMatchingCustomersFiltersByReminderCountAndDefaultRisk() throws Exception {
        Customer remindedCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(remindedCustomer, UUID.fromString("20000000-0000-0000-0000-000000000421"));
        Customer cleanCustomer = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        setId(cleanCustomer, UUID.fromString("20000000-0000-0000-0000-000000000422"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(remindedCustomer, cleanCustomer));
        when(paymentRecordRepository.findByCustomerId(remindedCustomer.getId()))
                .thenReturn(List.of(defaultRiskPayment(remindedCustomer)));
        when(paymentRecordRepository.findByCustomerId(cleanCustomer.getId()))
                .thenReturn(List.of(paidPayment(cleanCustomer)));

        List<CustomerView> byReminderCount =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "reminder_count",
                                        SegmentOperator.AFTER,
                                        "2",
                                        null,
                                        SegmentJoinOperator.AND)));
        List<CustomerView> byDefaultRisk =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "default_risk",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(byReminderCount).extracting(CustomerView::fullName).containsExactly("Tom Schmidt");
        assertThat(byDefaultRisk).extracting(CustomerView::fullName).containsExactly("Tom Schmidt");
    }

    @Test
    void findMatchingCustomersSupportsInOperatorForPaymentStatuses() throws Exception {
        Customer overdueCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(overdueCustomer, UUID.fromString("20000000-0000-0000-0000-000000000431"));
        Customer defaultRiskCustomer = customer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        setId(defaultRiskCustomer, UUID.fromString("20000000-0000-0000-0000-000000000432"));
        Customer paidCustomer = customer("Kai", "Fischer", "Cologne", CustomerType.PROSPECT);
        setId(paidCustomer, UUID.fromString("20000000-0000-0000-0000-000000000433"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(overdueCustomer, defaultRiskCustomer, paidCustomer));
        when(paymentRecordRepository.findByCustomerId(overdueCustomer.getId()))
                .thenReturn(List.of(overduePayment(overdueCustomer)));
        when(paymentRecordRepository.findByCustomerId(defaultRiskCustomer.getId()))
                .thenReturn(List.of(defaultRiskPayment(defaultRiskCustomer)));
        when(paymentRecordRepository.findByCustomerId(paidCustomer.getId()))
                .thenReturn(List.of(paidPayment(paidCustomer)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_status",
                                        SegmentOperator.IN,
                                        "OVERDUE, DEFAULT_RISK",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
    }

    @Test
    void rejectsUnsupportedPaymentHistoryCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "payment_status",
                                                        SegmentOperator.EQUALS,
                                                        "PENDING",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void rejectsUnsupportedCustomerTypeCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "customer_type",
                                                        SegmentOperator.EQUALS,
                                                        "LEAD",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void rejectsUnsupportedAgeGroupCriterionValue() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "age_group",
                                                        SegmentOperator.EQUALS,
                                                        "TEENAGER",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void previewSegmentWithOrCriteriaReturnsMultipleMatches() throws Exception {
        Customer munichProspect = customer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        Customer berlinCustomer = customer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        setId(munichProspect, CUSTOMER_ID);
        setId(berlinCustomer, UUID.fromString("20000000-0000-0000-0000-000000000202"));
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichProspect, berlinCustomer));

        // Join operator on criterion i (i > 0) combines with the accumulated prior result;
        // first criterion join is ignored. OR on the second criterion unions Munich|Berlin.
        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                null,
                                                SegmentJoinOperator.OR))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Berlin");
    }

    @Test
    void validatesSegmentAndCriteriaCommands() {
        assertThatThrownBy(() -> segmentService.createSegment(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment validation failed");

        assertThatThrownBy(
                        () ->
                                segmentService.createSegment(
                                        new CreateSegmentCommand(" ", null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment validation failed");

        assertThatThrownBy(() -> segmentService.previewSegment(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment preview validation failed");

        assertThatThrownBy(
                        () ->
                                segmentService.saveCriteria(
                                        SEGMENT_ID,
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        " ",
                                                        SegmentOperator.EQUALS,
                                                        "Munich",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void throwsWhenSegmentOrOwnerIsMissing() throws Exception {
        UUID missingSegmentId = UUID.fromString("42000000-0000-0000-0000-000000009999");
        when(segmentRepository.findById(missingSegmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> segmentService.findById(missingSegmentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Segment was not found: " + missingSegmentId);

        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                segmentService.createSegment(
                                        new CreateSegmentCommand(
                                                "Audience",
                                                null,
                                                SegmentVisibility.PRIVATE,
                                                null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User was not found: " + OWNER_ID);

        verify(segmentRepository, never()).save(any());
    }

    private static User owner() throws Exception {
        User user = User.create("owner@segment-service.test", "{noop}password", "Segment Owner");
        setId(user, OWNER_ID);
        return user;
    }

    private static Segment ownedSegment(User owner) throws Exception {
        Segment segment =
                Segment.create("Owned audience", "Private draft", owner, SegmentVisibility.PRIVATE);
        setId(segment, SEGMENT_ID);
        return segment;
    }

    private static Customer customer(
            String firstName, String lastName, String city, CustomerType customerType)
            throws Exception {
        Customer customer = Customer.create(customerType, firstName, lastName);
        customer.updateAddress(null, city, "Germany");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        customer.changeStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    private static void assertSegmentCreateAccessAnnotation() throws Exception {
        Method createSegment =
                SegmentService.class.getMethod("createSegment", CreateSegmentCommand.class);
        SegmentCreateAccess createAccess = createSegment.getAnnotation(SegmentCreateAccess.class);
        assertThat(createAccess).isNotNull();
        PreAuthorize preAuthorize = SegmentCreateAccess.class.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canCreateSegments()");
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        // getDeclaredMethod finds package-private methods (e.g. findMatchingCustomers item 208).
        Method method = SegmentService.class.getDeclaredMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expectedExpression);
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    private static ProductOwnership activeOwnership(Customer customer, ProductType productType) {
        Product product =
                Product.create(
                        "Segment Product " + productType.name(),
                        productType,
                        new BigDecimal("99.00"),
                        12);
        return ProductOwnership.create(
                customer, product, LocalDate.now().minusMonths(6), LocalDate.now().plusYears(1));
    }

    private static ProductOwnership ownershipExpiringInMonths(Customer customer, int months) {
        return ownershipExpiringOn(customer, LocalDate.now().plusMonths(months));
    }

    private static ProductOwnership ownershipExpiringOn(Customer customer, LocalDate expirationDate) {
        Product product =
                Product.create(
                        "Expiring Product " + expirationDate,
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("99.00"),
                        12);
        return ProductOwnership.create(
                customer, product, LocalDate.now().minusMonths(6), expirationDate);
    }

    private static PaymentRecord overduePayment(Customer customer) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        activeOwnership(customer, ProductType.LIFE_INSURANCE),
                        LocalDate.now().minusDays(10),
                        new BigDecimal("120.00"));
        payment.markOverdue();
        return payment;
    }

    private static PaymentRecord paidPayment(Customer customer) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        activeOwnership(customer, ProductType.AUTO_INSURANCE),
                        LocalDate.now().minusDays(5),
                        new BigDecimal("80.00"));
        payment.markPaid(new BigDecimal("80.00"), java.time.Instant.now());
        return payment;
    }

    private static PaymentRecord defaultRiskPayment(Customer customer) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        activeOwnership(customer, ProductType.HOMEOWNER_INSURANCE),
                        LocalDate.now().minusDays(30),
                        new BigDecimal("200.00"));
        payment.incrementReminder();
        payment.incrementReminder();
        payment.incrementReminder();
        return payment;
    }

    private static ConsentRecord givenMarketingConsent(Customer customer) {
        return ConsentRecord.create(
                customer,
                ConsentType.MARKETING_EMAIL,
                ConsentStatus.GIVEN,
                "Marketing email consent",
                "phone");
    }

    private static ConsentRecord withdrawnMarketingConsent(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        consent.withdraw(java.time.Instant.now());
        return consent;
    }

    private static ConsentRecord givenGuardianConsent(Customer customer) {
        return ConsentRecord.create(
                customer,
                ConsentType.GUARDIAN,
                ConsentStatus.GIVEN,
                "Guardian consent for minor beneficiary",
                "letter");
    }

}
