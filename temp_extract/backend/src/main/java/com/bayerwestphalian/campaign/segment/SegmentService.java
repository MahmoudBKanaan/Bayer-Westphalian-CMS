package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.method.SegmentCreateAccess;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.campaign.CampaignRecipientCandidate;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SegmentService {

    static final String AUDIT_ENTITY_TYPE = "segments";

    private final SegmentRepository segmentRepository;
    private final SegmentCriteriaRepository segmentCriteriaRepository;
    private final CustomerRepository customerRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;
    @SuppressWarnings("unused")
    private final ConsentService consentService;
    private final EligibilityService eligibilityService;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public SegmentService(
            SegmentRepository segmentRepository,
            SegmentCriteriaRepository segmentCriteriaRepository,
            CustomerRepository customerRepository,
            ProductOwnershipRepository productOwnershipRepository,
            PaymentRecordRepository paymentRecordRepository,
            ConsentRepository consentRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            ConsentService consentService,
            EligibilityService eligibilityService,
            AuditService auditService) {
        this(
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
                auditService,
                Clock.systemUTC());
    }

    SegmentService(
            SegmentRepository segmentRepository,
            SegmentCriteriaRepository segmentCriteriaRepository,
            CustomerRepository customerRepository,
            ProductOwnershipRepository productOwnershipRepository,
            PaymentRecordRepository paymentRecordRepository,
            ConsentRepository consentRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            ConsentService consentService,
            EligibilityService eligibilityService,
            AuditService auditService,
            Clock clock) {
        this.segmentRepository = segmentRepository;
        this.segmentCriteriaRepository = segmentCriteriaRepository;
        this.customerRepository = customerRepository;
        this.productOwnershipRepository = productOwnershipRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.consentService = consentService;
        this.eligibilityService = eligibilityService;
        this.auditService = auditService;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * KB FR-077 / items 189 and 201: Campaign Manager (and Admin) may create and save reusable
     * audience segments (name, optional description, visibility PRIVATE/TEAM/GLOBAL, criteria).
     * Saved definitions are owned by the current user and can be reloaded for later campaign
     * targeting. Enforced via {@link SegmentCreateAccess} ({@code @authz.canCreateSegments()}).
     */
    @SegmentCreateAccess
    @Transactional
    public SegmentView createSegment(CreateSegmentCommand command) {
        validateCreateCommand(command);
        User owner = findOwner(authorizationExpressions.currentUserId());

        Segment segment =
                Segment.create(
                        command.name().trim(),
                        normalize(command.description()),
                        owner,
                        command.visibility());
        applyCriteria(segment, command.criteria());

        Segment savedSegment = segmentRepository.save(segment);
        // SEC-009 / KB sensitive-action audit: persist create of reusable segment definitions.
        auditService.logCreate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedSegment.getId(),
                segmentAuditPayload(savedSegment));
        return SegmentView.from(savedSegment);
    }

    /**
     * Update saved segment definition. Requires {@code canManageSegments()} (Admin / Campaign
     * Manager). BI Analyst cannot edit unless also granted a manage role (item 200).
     */
    @PreAuthorize("@authz.canManageSegments()")
    @Transactional
    public SegmentView updateSegment(UUID segmentId, UpdateSegmentCommand command) {
        validateSegmentId(segmentId);
        validateUpdateCommand(command);
        Segment segment = findSegment(segmentId);
        requireSegmentManagementAccess(segment);
        Map<String, ?> oldValue = segmentAuditPayload(segment);

        segment.updateName(command.name().trim());
        segment.updateDescription(normalize(command.description()));
        if (command.visibility() != null) {
            segment.changeVisibility(command.visibility());
        }
        if (command.criteria() != null) {
            replaceCriteria(segment, command.criteria());
        }

        Segment savedSegment = segmentRepository.save(segment);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedSegment.getId(),
                oldValue,
                segmentAuditPayload(savedSegment));
        return SegmentView.from(savedSegment);
    }

    @PreAuthorize("@authz.canManageSegments()")
    @Transactional
    public void deleteSegment(UUID segmentId) {
        validateSegmentId(segmentId);
        Segment segment = findSegment(segmentId);
        requireSegmentManagementAccess(segment);
        Map<String, ?> oldValue = segmentAuditPayload(segment);
        segmentRepository.delete(segment);
        auditService.logDelete(
                currentActorUserId(), AUDIT_ENTITY_TYPE, segmentId, oldValue, null);
    }

    @PreAuthorize("@authz.canReadSegments()")
    @Transactional(readOnly = true)
    public SegmentView findById(UUID segmentId) {
        validateSegmentId(segmentId);
        Segment segment = findSegment(segmentId);
        requireSegmentReadAccess(segment);
        return SegmentView.from(segment);
    }

    @PreAuthorize("@authz.canReadSegments()")
    @Transactional(readOnly = true)
    public List<SegmentView> searchSegments(SegmentSearchCriteria criteria) {
        SegmentSearchCriteria normalized = normalize(criteria);
        UUID currentUserId = authorizationExpressions.currentUserId();
        boolean isAdmin = authorizationExpressions.hasRole(SystemRoleName.ADMIN.name());

        return loadCandidateSegments(normalized).stream()
                .filter(segment -> canViewSegment(segment, currentUserId, isAdmin))
                .filter(segment -> matchesSearch(segment, normalized))
                .map(SegmentView::from)
                .toList();
    }

    @PreAuthorize("@authz.canManageSegments()")
    @Transactional
    public SegmentView saveCriteria(UUID segmentId, List<CreateSegmentCriteriaCommand> criteria) {
        validateSegmentId(segmentId);
        validateCriteriaCommands(criteria);
        Segment segment = findSegment(segmentId);
        requireSegmentManagementAccess(segment);
        Map<String, ?> oldValue = segmentAuditPayload(segment);
        replaceCriteria(segment, criteria);
        Segment savedSegment = segmentRepository.save(segment);
        // Criteria-only saves are still segment definition changes and must be audited.
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                savedSegment.getId(),
                oldValue,
                segmentAuditPayload(savedSegment));
        return SegmentView.from(savedSegment);
    }

    /**
     * Criteria-only matching over active customer profiles (no eligibility).
     *
     * <p><strong>Production gate (item 208):</strong> this method must never be treated as a final
     * campaign audience and is intentionally package-private so it is not a public contactability
     * API. Callers that need a contactable audience must use {@link #previewSegment}, which always
     * applies {@link EligibilityService}. Campaign launch recipient generation (later sprint) must
     * also apply eligibility and must not use criteria-only results as the final contact list.
     */
    @PreAuthorize("@authz.canReadSegments()")
    @Transactional(readOnly = true)
    List<CustomerView> findMatchingCustomers(List<CreateSegmentCriteriaCommand> criteria) {
        validateCriteriaCommands(criteria);
        List<CreateSegmentCriteriaCommand> normalizedCriteria = normalizeCriteria(criteria);

        return customerRepository.findActiveProfiles().stream()
                .filter(customer -> matchesCriteria(customer, normalizedCriteria))
                .map(CustomerView::from)
                .toList();
    }

    /**
     * KB FR-054 / FR-055 / FR-079 / BR-001–003 / BR-006 / items 198 and <strong>208</strong>:
     * criteria-based audience preview with {@link EligibilityService} applied to every criteria
     * match. This is the only segment-module path that returns a contactable audience view.
     *
     * <p>Criteria matches set total audience size; only customers that pass {@link
     * EligibilityService#evaluateForSegmentPreview(UUID)} are returned as eligible matching
     * customers. Ineligible matches contribute to excluded counts and exclusion reason summaries.
     * Preview never returns criteria-only audiences as contactable without this gate (production
     * gate: segmentation must never return a final campaign audience without eligibility checks).
     * Campaign-duplicate checks are not applied (preview is not campaign-scoped).
     */
    @PreAuthorize("@authz.canPreviewSegments()")
    @Transactional(readOnly = true)
    public SegmentPreviewView previewSegment(SegmentPreviewCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Segment preview validation failed", List.of("command: is required"));
        }
        // Phase 1: criteria only (size). Phase 2: eligibility gate (item 208) — never skip.
        List<CustomerView> criteriaMatches = findMatchingCustomers(command.criteria());
        // KB FR-079: total audience count = criteria matches (audience size before eligibility).
        int totalAudienceCount = criteriaMatches.size();

        // KB items 178 / 198 / 208: Apply EligibilityService to every match — never return
        // criteria-only audiences as the contactable / final campaign audience.
        List<CustomerView> eligibleCustomers = new ArrayList<>();
        List<EligibilityDecision> exclusionDecisions = new ArrayList<>();
        for (CustomerView customer : criteriaMatches) {
            EligibilityDecision decision = applyEligibilityServiceToPreviewMatch(customer);
            if (decision != null && decision.eligible()) {
                eligibleCustomers.add(customer);
            } else if (decision != null) {
                exclusionDecisions.add(decision);
            } else {
                exclusionDecisions.add(
                        EligibilityDecision.excluded(
                                SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_CODE,
                                SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_MESSAGE));
            }
        }

        // KB FR-079 / items 179–181 / 199: eligible and excluded counts from eligibility gate.
        int eligibleCount = eligibleCustomers.size();
        int excludedCount = totalAudienceCount - eligibleCount;
        // KB BR-006 / FR-055: aggregated exclusion reasons for preview UI.
        List<SegmentExclusionReasonSummary> exclusionReasonSummary =
                SegmentExclusionReasonSummarySupport.summarize(exclusionDecisions);

        return SegmentPreviewView.of(
                totalAudienceCount,
                eligibleCount,
                excludedCount,
                eligibleCustomers,
                exclusionReasonSummary);
    }

    /**
     * Campaign-scoped recipient preview (KB item 268 / FR-054 / FR-055 / BR-006).
     *
     * <p>Uses the selected segment criteria when present, then applies campaign eligibility to
     * every criteria match. Unlike generic segment preview, this path includes campaign duplicate
     * recipient checks through {@link EligibilityService#evaluateForCampaignPreview(UUID, UUID)}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public SegmentPreviewView previewCampaignRecipients(UUID campaignId, UUID segmentId) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Campaign recipient preview validation failed",
                    List.of("campaignId: is required"));
        }
        List<CreateSegmentCriteriaCommand> criteria =
                segmentId == null ? List.of() : criteriaCommands(findSegment(segmentId));
        List<CustomerView> criteriaMatches = findMatchingCustomers(criteria);
        int totalAudienceCount = criteriaMatches.size();

        List<CustomerView> eligibleCustomers = new ArrayList<>();
        List<EligibilityDecision> exclusionDecisions = new ArrayList<>();
        for (CustomerView customer : criteriaMatches) {
            EligibilityDecision decision =
                    customer == null || customer.id() == null
                            ? EligibilityDecision.excluded(
                                    SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_CODE,
                                    SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_MESSAGE)
                            : eligibilityService.evaluateForCampaignPreview(
                                    customer.id(), campaignId);
            if (decision != null && decision.eligible()) {
                eligibleCustomers.add(customer);
            } else if (decision != null) {
                exclusionDecisions.add(decision);
            }
        }

        List<SegmentExclusionReasonSummary> exclusionReasonSummary =
                SegmentExclusionReasonSummarySupport.summarize(exclusionDecisions);
        return SegmentPreviewView.of(
                totalAudienceCount,
                eligibleCustomers.size(),
                totalAudienceCount - eligibleCustomers.size(),
                eligibleCustomers,
                exclusionReasonSummary);
    }

    /**
     * Row-level campaign recipient evaluation for recipient generation (KB item 267).
     *
     * <p>Unlike {@link #previewCampaignRecipients(UUID, UUID)}, this keeps every candidate decision
     * so excluded recipients can be persisted with their specific reason and explanation.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public List<CampaignRecipientCandidate> evaluateCampaignRecipientCandidates(
            UUID campaignId, UUID segmentId) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Campaign recipient generation validation failed",
                    List.of("campaignId: is required"));
        }
        List<CreateSegmentCriteriaCommand> criteria =
                segmentId == null ? List.of() : criteriaCommands(findSegment(segmentId));
        return findMatchingCustomers(criteria).stream()
                .map(
                        customer ->
                                new CampaignRecipientCandidate(
                                        customer.id(),
                                        eligibilityService.evaluateForCampaignPreview(
                                                customer.id(), campaignId)))
                .toList();
    }

    /**
     * Applies {@link EligibilityService} to a single criteria match for segment preview (KB FR-054,
     * FR-055, BR-001–003, BR-011, items 198 and 208). Delegates to {@link
     * EligibilityService#evaluateForSegmentPreview(UUID)} (marketing-email consent, DNC, opt-out,
     * guardian consent, monthly contact limit). Never skipped for criteria matches with an id —
     * production gate requires eligibility before treating any match as contactable.
     */
    private EligibilityDecision applyEligibilityServiceToPreviewMatch(CustomerView customer) {
        if (customer == null || customer.id() == null) {
            return EligibilityDecision.excluded(
                    SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_CODE,
                    SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_MESSAGE);
        }
        return eligibilityService.evaluateForSegmentPreview(customer.id());
    }

    private List<CreateSegmentCriteriaCommand> criteriaCommands(Segment segment) {
        if (segment == null) {
            return List.of();
        }
        return segment.getCriteria().stream()
                .map(
                        criterion ->
                                new CreateSegmentCriteriaCommand(
                                        criterion.getFieldName(),
                                        criterion.getOperator(),
                                        criterion.getValue(),
                                        criterion.getLogicalGroup(),
                                        criterion.getJoinOperator()))
                .toList();
    }

    private List<Segment> loadCandidateSegments(SegmentSearchCriteria criteria) {
        if (criteria.ownerUserId() != null) {
            return segmentRepository.findByOwner(criteria.ownerUserId());
        }
        if (criteria.visibility() != null) {
            return segmentRepository.findByVisibility(criteria.visibility());
        }
        return segmentRepository.findAll();
    }

    private boolean matchesSearch(Segment segment, SegmentSearchCriteria criteria) {
        if (!StringUtils.hasText(criteria.term())) {
            return true;
        }
        String term = criteria.term().trim().toLowerCase(Locale.ROOT);
        return segment.getName().toLowerCase(Locale.ROOT).contains(term)
                || (segment.getDescription() != null
                        && segment.getDescription().toLowerCase(Locale.ROOT).contains(term));
    }

    private boolean canViewSegment(Segment segment, UUID currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        if (segment.getVisibility() != SegmentVisibility.PRIVATE) {
            return true;
        }
        return segment.isOwnedBy(currentUserId);
    }

    private void requireSegmentReadAccess(Segment segment) {
        UUID currentUserId = authorizationExpressions.currentUserId();
        boolean isAdmin = authorizationExpressions.hasRole(SystemRoleName.ADMIN.name());
        if (!canViewSegment(segment, currentUserId, isAdmin)) {
            throw new ForbiddenException("Private segment is not accessible");
        }
    }

    private void requireSegmentManagementAccess(Segment segment) {
        if (authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())) {
            return;
        }
        if (!segment.isOwnedBy(authorizationExpressions.currentUserId())) {
            throw new ForbiddenException("Segment is not owned by the current user");
        }
    }

    private Segment findSegment(UUID segmentId) {
        return segmentRepository
                .findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment", segmentId));
    }

    private User findOwner(UUID ownerUserId) {
        return userRepository
                .findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerUserId));
    }

    private void applyCriteria(Segment segment, List<CreateSegmentCriteriaCommand> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return;
        }
        replaceCriteria(segment, criteria);
    }

    private void replaceCriteria(Segment segment, List<CreateSegmentCriteriaCommand> criteria) {
        List<SegmentCriteria> existingCriteria = new ArrayList<>(segment.getCriteria());
        existingCriteria.forEach(segment::removeCriteria);
        segmentCriteriaRepository.deleteAll(existingCriteria);

        for (CreateSegmentCriteriaCommand command : normalizeCriteria(criteria)) {
            segment.addCriteria(
                    command.fieldName(),
                    command.operator(),
                    command.value(),
                    command.logicalGroup(),
                    command.joinOperator());
        }
    }

    private boolean matchesCriteria(
            Customer customer, List<CreateSegmentCriteriaCommand> criteria) {
        // KB FR-078 AND/OR: left-to-right combination. Default join is AND (every criterion must
        // match). OR on a later criterion unions that match with the accumulated prior result.
        return SegmentCriteriaLogicSupport.matchesAllCriteria(
                criteria, command -> matchesCriterion(customer, command));
    }

    private boolean matchesCriterion(Customer customer, CreateSegmentCriteriaCommand command) {
        if (SegmentProductOwnershipSupport.isProductOwnershipField(command.fieldName())) {
            List<ProductOwnership> ownerships =
                    productOwnershipRepository.findByCustomerId(customer.getId());
            return SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                    ownerships, command.operator(), command.fieldName(), command.value());
        }

        if (SegmentProductExpirationSupport.isProductExpirationField(command.fieldName())) {
            List<ProductOwnership> ownerships =
                    productOwnershipRepository.findByCustomerId(customer.getId());
            return SegmentProductExpirationSupport.matchesCustomerOwnerships(
                    ownerships,
                    command.operator(),
                    command.fieldName(),
                    command.value(),
                    LocalDate.now(clock));
        }

        if (SegmentPaymentHistorySupport.isPaymentHistoryField(command.fieldName())) {
            List<PaymentRecord> payments =
                    paymentRecordRepository.findByCustomerId(customer.getId());
            return SegmentPaymentHistorySupport.matchesCustomerPayments(
                    payments, command.operator(), command.fieldName(), command.value());
        }

        if (SegmentConsentStatusSupport.isConsentStatusField(command.fieldName())) {
            List<ConsentRecord> consents = consentRepository.findByCustomerId(customer.getId());
            return SegmentConsentStatusSupport.matchesCustomerConsents(
                    consents,
                    command.operator(),
                    command.fieldName(),
                    command.value(),
                    Instant.now(clock));
        }

        return SegmentCriteria.matchesValue(
                command.operator(),
                command.value(),
                resolveFieldValue(customer, command.fieldName()));
    }

    private String resolveFieldValue(Customer customer, String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return null;
        }

        if (SegmentLocationSupport.isLocationField(fieldName)) {
            return SegmentLocationSupport.resolveCustomerValue(customer, fieldName);
        }

        if (SegmentCustomerTypeSupport.isCustomerTypeField(fieldName)) {
            return SegmentCustomerTypeSupport.resolveCustomerValue(customer);
        }

        if (SegmentBehaviorStatusSupport.isBehaviorStatusField(fieldName)) {
            return SegmentBehaviorStatusSupport.resolveCustomerValue(customer, fieldName);
        }

        return switch (normalizeFieldName(fieldName)) {
            case "age_group", "agegroup" -> SegmentAgeGroupSupport.resolveCustomerValue(customer);
            default -> null;
        };
    }

    private String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }

    private List<CreateSegmentCriteriaCommand> normalizeCriteria(
            List<CreateSegmentCriteriaCommand> criteria) {
        if (criteria == null) {
            return List.of();
        }
        return criteria.stream().map(this::normalizeCriterion).toList();
    }

    private CreateSegmentCriteriaCommand normalizeCriterion(CreateSegmentCriteriaCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Segment criteria validation failed", List.of("criteria: must not contain null"));
        }
        String fieldName = canonicalizeCriterionFieldName(command.fieldName());
        String value = normalizeCriterionValue(fieldName, command.operator(), command.value());

        return new CreateSegmentCriteriaCommand(
                fieldName,
                command.operator(),
                value,
                normalize(command.logicalGroup()),
                SegmentCriteriaLogicSupport.defaultJoinOperator(command.joinOperator()));
    }

    private SegmentSearchCriteria normalize(SegmentSearchCriteria criteria) {
        if (criteria == null) {
            return new SegmentSearchCriteria(null, null, null);
        }
        return new SegmentSearchCriteria(
                normalize(criteria.term()), criteria.ownerUserId(), criteria.visibility());
    }

    private void validateCreateCommand(CreateSegmentCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Segment validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("name", command.name()),
                                validateLength("name", command.name(), 255),
                                validateCriteriaList(command.criteria()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Segment validation failed", errors);
        }
        validateCriteriaCommands(command.criteria());
    }

    private void validateUpdateCommand(UpdateSegmentCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Segment validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("name", command.name()),
                                validateLength("name", command.name(), 255),
                                validateCriteriaList(command.criteria()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Segment validation failed", errors);
        }
        if (command.criteria() != null) {
            validateCriteriaCommands(command.criteria());
        }
    }

    private void validateCriteriaCommands(List<CreateSegmentCriteriaCommand> criteria) {
        if (criteria == null) {
            return;
        }
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < criteria.size(); index++) {
            CreateSegmentCriteriaCommand command = criteria.get(index);
            if (command == null) {
                errors.add("criteria[" + index + "]: must not be null");
                continue;
            }
            errors.add(required("criteria[" + index + "].fieldName", command.fieldName()));
            errors.add(required("criteria[" + index + "].operator", command.operator()));
            errors.add(required("criteria[" + index + "].value", command.value()));
            errors.add(validateLength("criteria[" + index + "].fieldName", command.fieldName(), 100));
            errors.add(
                    validateLength("criteria[" + index + "].logicalGroup", command.logicalGroup(), 50));
            errors.add(validateAgeGroupCriterionValue(index, command));
            errors.add(validateLocationCriterionValue(index, command));
            errors.add(validateCustomerTypeCriterionValue(index, command));
            errors.add(validateProductOwnershipCriterionValue(index, command));
            errors.add(validateProductExpirationCriterionValue(index, command));
            errors.add(validatePaymentHistoryCriterionValue(index, command));
            errors.add(validateBehaviorStatusCriterionValue(index, command));
            errors.add(validateConsentStatusCriterionValue(index, command));
        }
        List<String> filteredErrors = errors.stream().filter(StringUtils::hasText).toList();
        if (!filteredErrors.isEmpty()) {
            throw new ValidationException("Segment criteria validation failed", filteredErrors);
        }
    }

    private String canonicalizeCriterionFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        String trimmed = fieldName.trim();
        if (SegmentLocationSupport.isLocationField(trimmed)) {
            return SegmentLocationSupport.canonicalizeFieldName(trimmed);
        }
        if (SegmentCustomerTypeSupport.isCustomerTypeField(trimmed)) {
            return SegmentCustomerTypeSupport.canonicalizeFieldName(trimmed);
        }
        if (SegmentProductOwnershipSupport.isProductOwnershipField(trimmed)) {
            return SegmentProductOwnershipSupport.canonicalizeFieldName(trimmed);
        }
        if (SegmentProductExpirationSupport.isProductExpirationField(trimmed)) {
            return SegmentProductExpirationSupport.canonicalizeFieldName(trimmed);
        }
        if (SegmentPaymentHistorySupport.isPaymentHistoryField(trimmed)) {
            return SegmentPaymentHistorySupport.canonicalizeFieldName(trimmed);
        }
        if (SegmentBehaviorStatusSupport.isBehaviorStatusField(trimmed)) {
            return SegmentBehaviorStatusSupport.canonicalizeFieldName(trimmed);
        }
        if (SegmentConsentStatusSupport.isConsentStatusField(trimmed)) {
            return SegmentConsentStatusSupport.canonicalizeFieldName(trimmed);
        }
        return trimmed;
    }

    private String normalizeCriterionValue(
            String fieldName, SegmentOperator operator, String rawValue) {
        if (SegmentAgeGroupSupport.isAgeGroupField(fieldName)) {
            return SegmentAgeGroupSupport.normalizeFilterValue(operator, rawValue);
        }
        if (SegmentLocationSupport.isLocationField(fieldName)) {
            return SegmentLocationSupport.normalizeFilterValue(operator, fieldName, rawValue);
        }
        if (SegmentCustomerTypeSupport.isCustomerTypeField(fieldName)) {
            return SegmentCustomerTypeSupport.normalizeFilterValue(operator, rawValue);
        }
        if (SegmentProductOwnershipSupport.isProductOwnershipField(fieldName)) {
            return SegmentProductOwnershipSupport.normalizeFilterValue(operator, fieldName, rawValue);
        }
        if (SegmentProductExpirationSupport.isProductExpirationField(fieldName)) {
            return SegmentProductExpirationSupport.normalizeFilterValue(operator, fieldName, rawValue);
        }
        if (SegmentPaymentHistorySupport.isPaymentHistoryField(fieldName)) {
            return SegmentPaymentHistorySupport.normalizeFilterValue(operator, fieldName, rawValue);
        }
        if (SegmentBehaviorStatusSupport.isBehaviorStatusField(fieldName)) {
            return SegmentBehaviorStatusSupport.normalizeFilterValue(operator, fieldName, rawValue);
        }
        if (SegmentConsentStatusSupport.isConsentStatusField(fieldName)) {
            return SegmentConsentStatusSupport.normalizeFilterValue(operator, fieldName, rawValue);
        }
        return rawValue.trim();
    }

    private String validateProductOwnershipCriterionValue(
            int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentProductOwnershipSupport.isProductOwnershipField(command.fieldName())) {
            return "";
        }

        try {
            SegmentProductOwnershipSupport.validateFilterValue(
                    command.operator(), command.fieldName(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateProductExpirationCriterionValue(
            int index, CreateSegmentCriteriaCommand command) {
        if (command == null
                || !SegmentProductExpirationSupport.isProductExpirationField(command.fieldName())) {
            return "";
        }

        try {
            SegmentProductExpirationSupport.validateFilterValue(
                    command.operator(), command.fieldName(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validatePaymentHistoryCriterionValue(
            int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentPaymentHistorySupport.isPaymentHistoryField(command.fieldName())) {
            return "";
        }

        try {
            SegmentPaymentHistorySupport.validateFilterValue(
                    command.operator(), command.fieldName(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateBehaviorStatusCriterionValue(
            int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentBehaviorStatusSupport.isBehaviorStatusField(command.fieldName())) {
            return "";
        }

        try {
            SegmentBehaviorStatusSupport.validateFilterValue(
                    command.operator(), command.fieldName(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateConsentStatusCriterionValue(
            int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentConsentStatusSupport.isConsentStatusField(command.fieldName())) {
            return "";
        }

        try {
            SegmentConsentStatusSupport.validateFilterValue(
                    command.operator(), command.fieldName(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateCustomerTypeCriterionValue(int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentCustomerTypeSupport.isCustomerTypeField(command.fieldName())) {
            return "";
        }

        try {
            SegmentCustomerTypeSupport.validateFilterValue(command.operator(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateLocationCriterionValue(int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentLocationSupport.isLocationField(command.fieldName())) {
            return "";
        }

        try {
            SegmentLocationSupport.validateFilterValue(
                    command.operator(), command.fieldName(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateAgeGroupCriterionValue(int index, CreateSegmentCriteriaCommand command) {
        if (command == null || !SegmentAgeGroupSupport.isAgeGroupField(command.fieldName())) {
            return "";
        }

        try {
            SegmentAgeGroupSupport.validateFilterValue(command.operator(), command.value());
            return "";
        } catch (IllegalArgumentException exception) {
            return "criteria[" + index + "].value: " + exception.getMessage();
        }
    }

    private String validateCriteriaList(List<CreateSegmentCriteriaCommand> criteria) {
        if (criteria == null) {
            return "";
        }
        for (CreateSegmentCriteriaCommand command : criteria) {
            if (command == null) {
                return "criteria: must not contain null";
            }
        }
        return "";
    }

    private void validateSegmentId(UUID segmentId) {
        if (segmentId == null) {
            throw new ValidationException(
                    "Segment validation failed", List.of("segmentId: is required"));
        }
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String validateLength(String fieldName, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            return fieldName + ": must be at most " + maxLength + " characters";
        }
        return "";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> segmentAuditPayload(Segment segment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (segment == null) {
            return payload;
        }
        payload.put("name", segment.getName());
        if (segment.getDescription() != null) {
            payload.put("description", segment.getDescription());
        }
        if (segment.getOwnerUserId() != null) {
            payload.put("ownerUserId", segment.getOwnerUserId().toString());
        }
        if (segment.getVisibility() != null) {
            payload.put("visibility", segment.getVisibility().name());
        }
        payload.put("criteriaCount", segment.getCriteria() == null ? 0 : segment.getCriteria().size());
        payload.put("criteria", criteriaAuditPayload(segment.getCriteria()));
        return payload;
    }

    private List<Map<String, Object>> criteriaAuditPayload(List<SegmentCriteria> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> payload = new ArrayList<>(criteria.size());
        for (SegmentCriteria criterion : criteria) {
            if (criterion == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldName", criterion.getFieldName());
            if (criterion.getOperator() != null) {
                item.put("operator", criterion.getOperator().name());
            }
            item.put("value", criterion.getValue());
            if (criterion.getLogicalGroup() != null) {
                item.put("logicalGroup", criterion.getLogicalGroup());
            }
            if (criterion.getJoinOperator() != null) {
                item.put("joinOperator", criterion.getJoinOperator().name());
            }
            payload.add(item);
        }
        return payload;
    }

    private UUID currentActorUserId() {
        return authorizationExpressions.isAuthenticated()
                ? authorizationExpressions.currentUserId()
                : null;
    }
}
