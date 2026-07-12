package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.method.CampaignApprovalAccess;
import com.bayerwestphalian.campaign.auth.method.CampaignWriteAccess;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Campaign lifecycle service (KB CampaignService: create, update draft, list, submit, approve,
 * reject, launch, pause, complete, archive). Enforces FR-050–062 domain rules and audit for
 * sensitive transitions.
 */
@Service
public class CampaignService {

    static final String AUDIT_ENTITY_TYPE = "campaigns";

    private final CampaignRepository campaignRepository;
    private final CampaignProductRepository campaignProductRepository;
    private final SegmentRepository segmentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;

    @Autowired(required = false)
    private CampaignRecipientRepository campaignRecipientRepository;

    @Autowired(required = false)
    private ContactEventRepository contactEventRepository;

    @Autowired(required = false)
    private CampaignMetricsRepository campaignMetricsRepository;

    public CampaignService(
            CampaignRepository campaignRepository,
            CampaignProductRepository campaignProductRepository,
            SegmentRepository segmentRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this.campaignRepository = campaignRepository;
        this.campaignProductRepository = campaignProductRepository;
        this.segmentRepository = segmentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
    }

    /**
     * Creates a draft campaign (item 217 / item 243 / FR-050 / FR-057).
     *
     * <p>Callable by Campaign Manager (and Admin) via {@link CampaignWriteAccess}. The campaign is
     * owned by the authenticated actor and starts in {@link CampaignStatus#DRAFT}.
     *
     * <p>Persists a CREATE audit log for entity type {@value #AUDIT_ENTITY_TYPE} with the new
     * campaign payload (item 233). The audit entry is written in the same transaction as the
     * campaign insert so creation evidence is not lost on rollback.
     */
    @CampaignWriteAccess
    @Transactional
    public CampaignView createCampaign(CreateCampaignCommand command) {
        validateCreateCommand(command);
        User owner = findUser(authorizationExpressions.currentUserId());
        Segment segment = resolveSegment(command.segmentId());

        Campaign campaign =
                Campaign.create(
                        command.name().trim(),
                        command.objective().trim(),
                        owner,
                        segment,
                        command.channel());
        campaign.updateMessage(command.messageSubject(), command.messageBody());
        campaign.updateSchedule(command.startDate(), command.endDate());

        Campaign saved = campaignRepository.save(campaign);
        replaceProducts(saved, command.productIds());

        // Item 233: sensitive campaign creation must leave an immutable CREATE audit trail.
        auditService.logCreate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * Updates a draft (or rejected) campaign definition (item 218 / FR-057). Non-editable statuses
     * raise a lifecycle business rule via domain {@link Campaign#canEdit()}.
     */
    @CampaignWriteAccess
    @Transactional
    public CampaignView updateCampaign(UUID campaignId, UpdateCampaignCommand command) {
        validateCampaignId(campaignId);
        validateUpdateCommand(command);
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);

        try {
            campaign.updateName(command.name().trim());
            campaign.updateObjective(command.objective().trim());
            campaign.changeChannel(command.channel());
            campaign.assignSegment(resolveSegment(command.segmentId()));
            campaign.updateMessage(command.messageSubject(), command.messageBody());
            campaign.updateSchedule(command.startDate(), command.endDate());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BusinessRuleException("CAMPAIGN_LIFECYCLE", ex.getMessage());
        }
        if (command.productIds() != null) {
            replaceProducts(campaign, command.productIds());
        }

        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * Loads campaign details by id (item 220), including promoted product ids from {@code
     * campaign_products}.
     */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public CampaignView findById(UUID campaignId) {
        validateCampaignId(campaignId);
        return toView(findCampaign(campaignId));
    }

    /**
     * Lists campaigns with optional filters (item 219). Supports term match on name/objective plus
     * owner, status, and segment filters.
     */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public List<CampaignView> searchCampaigns(CampaignSearchCriteria criteria) {
        CampaignSearchCriteria normalized = normalize(criteria);
        return loadCandidates(normalized).stream()
                .filter(campaign -> matchesSearch(campaign, normalized))
                .map(this::toView)
                .toList();
    }

    /**
     * Selects promoted products for a draft/rejected campaign (KB FR-052 / item 221).
     *
     * <p>Replaces the full {@code campaign_products} set. Only active, non-deleted products may be
     * linked. Empty {@code productIds} clears all selections.
     */
    @CampaignWriteAccess
    @Transactional
    public CampaignView selectProducts(UUID campaignId, SelectCampaignProductsCommand command) {
        validateCampaignId(campaignId);
        if (command == null) {
            throw new ValidationException(
                    "Campaign product validation failed", List.of("command: is required"));
        }
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        requireEditableForTargetingSelection(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);

        replaceProducts(campaign, command.productIds() == null ? List.of() : command.productIds());

        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * Returns selected promoted product ids for a campaign (FR-052 read side of product selection).
     */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public List<UUID> listSelectedProductIds(UUID campaignId) {
        validateCampaignId(campaignId);
        findCampaign(campaignId);
        return campaignProductRepository.findByCampaignId(campaignId).stream()
                .map(CampaignProduct::getProductId)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Selects or clears the target segment for a draft/rejected campaign (KB FR-053 / item 222).
     *
     * <p>{@code segmentId} must reference an existing segment, or be null to clear the assignment.
     */
    @CampaignWriteAccess
    @Transactional
    public CampaignView selectSegment(UUID campaignId, SelectCampaignSegmentCommand command) {
        validateCampaignId(campaignId);
        if (command == null) {
            throw new ValidationException(
                    "Campaign segment validation failed", List.of("command: is required"));
        }
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        requireEditableForTargetingSelection(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);

        try {
            campaign.assignSegment(resolveSegment(command.segmentId()));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BusinessRuleException("CAMPAIGN_LIFECYCLE", ex.getMessage());
        }

        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /** Returns the selected segment id for a campaign, or null when none is linked (FR-053). */
    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public UUID getSelectedSegmentId(UUID campaignId) {
        validateCampaignId(campaignId);
        return findCampaign(campaignId).getSegmentId();
    }

    /**
     * Submits a draft (or rejected) campaign for compliance review (FR-058 / BR-032).
     *
     * <p>Item 528 (also sprint evidence item 234): persists a {@code SUBMIT} audit log for entity
     * type {@value #AUDIT_ENTITY_TYPE} with old ({@code DRAFT} or {@code REJECTED}) and new ({@code
     * SUBMITTED}) payloads via {@link AuditService#logSubmission}. The audit entry is written in
     * the same transaction as the status change. Validation failures and unauthorized access do not
     * write an audit row.
     */
    @CampaignWriteAccess
    @Transactional
    public CampaignView submitCampaign(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        validateCampaignReadyForSubmission(campaign);
        Map<String, Object> oldValue = campaignAuditPayload(campaign);

        applyLifecycle(campaign::submit);
        Campaign saved = campaignRepository.save(campaign);
        // Item 528: campaign submission must leave an immutable SUBMIT audit trail.
        auditService.logSubmission(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * Approves a submitted campaign (FR-059 / COMP-006). Optional compliance review notes (item
     * 231) are stored on the campaign for the Campaign Manager.
     *
     * <p>Item 529 (also sprint evidence item 235): persists an {@code APPROVE} audit log for entity
     * type {@value #AUDIT_ENTITY_TYPE} with old ({@code SUBMITTED}) and new ({@code APPROVED})
     * payloads via {@link AuditService#logApproval}. The audit entry is written in the same
     * transaction as the status change. Owner self-approve and invalid lifecycle status do not
     * write an audit row.
     */
    @CampaignApprovalAccess
    @Transactional
    public CampaignView approveCampaign(UUID campaignId, ApproveCampaignCommand command) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireNotApprovingOwnCampaign(campaign);
        Map<String, Object> oldValue = campaignAuditPayload(campaign);

        User approver = findUser(authorizationExpressions.currentUserId());
        String notes = command == null ? null : command.complianceReviewNotes();
        applyLifecycle(() -> campaign.approve(approver, notes));
        Campaign saved = campaignRepository.save(campaign);
        // Item 529: campaign approval must leave an immutable APPROVE audit trail.
        auditService.logApproval(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /** Convenience overload when no review notes are provided. */
    @CampaignApprovalAccess
    @Transactional
    public CampaignView approveCampaign(UUID campaignId) {
        return approveCampaign(campaignId, new ApproveCampaignCommand(null));
    }

    /**
     * Rejects a submitted campaign with a required formal reason (FR-059 / item 232). Optional
     * compliance review notes (item 231) may be supplied alongside the reason.
     *
     * <p>Item 529 (also sprint evidence item 236): persists a {@code REJECT} audit log for entity
     * type {@value #AUDIT_ENTITY_TYPE} with old ({@code SUBMITTED}) and new ({@code REJECTED})
     * payloads via {@link AuditService#logRejection}, including {@code rejectionReason} and optional
     * {@code complianceReviewNotes}. Missing reason, owner self-reject, and invalid lifecycle status
     * do not write an audit row.
     */
    @CampaignApprovalAccess
    @Transactional
    public CampaignView rejectCampaign(UUID campaignId, RejectCampaignCommand command) {
        validateCampaignId(campaignId);
        if (command == null || !StringUtils.hasText(command.rejectionReason())) {
            throw new ValidationException(
                    "Campaign rejection validation failed",
                    List.of("Rejection reason is required."));
        }
        Campaign campaign = findCampaign(campaignId);
        requireNotApprovingOwnCampaign(campaign);
        Map<String, Object> oldValue = campaignAuditPayload(campaign);

        applyLifecycle(
                () -> campaign.reject(command.rejectionReason(), command.complianceReviewNotes()));
        Campaign saved = campaignRepository.save(campaign);
        // Item 529: campaign rejection must leave an immutable REJECT audit trail.
        auditService.logRejection(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * Records compliance review notes without changing lifecycle status (item 231). Allowed for
     * SUBMITTED, APPROVED, or REJECTED campaigns by compliance/admin roles.
     */
    @CampaignApprovalAccess
    @Transactional
    public CampaignView recordComplianceReviewNotes(UUID campaignId, String complianceReviewNotes) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireNotApprovingOwnCampaign(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);
        applyLifecycle(() -> campaign.recordComplianceReviewNotes(complianceReviewNotes));
        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * Launches an approved campaign (FR-060 / BR-005 / BR-033).
     *
     * <p>Item 530 (also sprint evidence items 279–281): domain launch only allows {@code APPROVED}
     * status, creates contact events for eligible recipients, refreshes metrics, then persists a
     * {@code LAUNCH} audit log for entity type {@value #AUDIT_ENTITY_TYPE} with old ({@code
     * APPROVED}) and new ({@code ACTIVE}) payloads via {@link AuditService#logLaunch}. The audit
     * entry is written in the same transaction as the status change. Unauthorized access, missing
     * campaigns, or invalid lifecycle status (draft/submitted/etc.) do not write a launch audit row.
     */
    @CampaignWriteAccess
    @Transactional
    public CampaignView launchCampaign(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        Map<String, Object> oldValue = campaignAuditPayload(campaign);

        // KB item 279 / BR-005 / TC-001: domain launch only allows APPROVED status.
        applyLifecycle(campaign::launch);
        // KB item 280 / item 449: create SENT contact events for eligible recipients, then refresh
        // campaign_metrics.sent_count (and audience counters) from the launch outcome.
        int sentCount = createContactEventsForLaunch(campaign);
        updateMetricsForLaunch(campaign, sentCount);
        Campaign saved = campaignRepository.save(campaign);
        // Item 530: campaign launch must leave an immutable LAUNCH audit trail.
        auditService.logLaunch(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    /**
     * KB item 280: Launching a campaign creates contact history for every stored eligible
     * recipient. The same transaction marks recipients as SENT so future duplicate and
     * monthly-limit checks see the launch.
     */
    private int createContactEventsForLaunch(Campaign campaign) {
        if (campaignRecipientRepository == null || contactEventRepository == null) {
            return 0;
        }
        User actor = findUser(currentActorUserId());
        CommunicationChannel channel = launchContactChannel(campaign.getChannel());
        java.time.Instant occurredAt = java.time.Instant.now();
        List<ContactEvent> events = new ArrayList<>();
        for (CampaignRecipient recipient :
                campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        campaign.getId(), CampaignRecipientStatus.ELIGIBLE)) {
            if (recipient.getCustomer() == null) {
                continue;
            }
            recipient.markSent();
            events.add(
                    ContactEvent.sent(
                            recipient.getCustomer(), campaign, channel, occurredAt, actor));
        }
        if (!events.isEmpty()) {
            contactEventRepository.saveAll(events);
        }
        return events.size();
    }

    /**
     * KB item 282 / item 449: launch refreshes campaign metrics from stored recipient counts.
     *
     * <p>KB item 418 / item 447: eligible count is calculated from {@code campaign_recipients} with
     * status {@link CampaignRecipientStatus#ELIGIBLE}.
     *
     * <p>KB item 419 / item 448: excluded count is calculated from {@code campaign_recipients} with
     * status {@link CampaignRecipientStatus#EXCLUDED}.
     *
     * <p>KB item 420 / item 449 / FR-103: sent count is calculated from the number of SENT contact
     * events created for eligible recipients at launch and written to {@code
     * campaign_metrics.sent_count}.
     */
    private void updateMetricsForLaunch(Campaign campaign, int sentCount) {
        if (campaignMetricsRepository == null || campaignRecipientRepository == null) {
            return;
        }
        int eligibleCount =
                CampaignMetrics.calculateEligibleCount(
                        campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                                campaign.getId(), CampaignRecipientStatus.ELIGIBLE));
        int excludedCount =
                CampaignMetrics.calculateExcludedCount(
                        campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                                campaign.getId(), CampaignRecipientStatus.EXCLUDED));
        int calculatedSentCount = CampaignMetrics.calculateSentCount(sentCount);
        CampaignMetrics metrics =
                campaignMetricsRepository
                        .findByCampaignId(campaign.getId())
                        .orElseGet(() -> CampaignMetrics.forCampaign(campaign));
        metrics.recordLaunchCounts(eligibleCount, excludedCount, calculatedSentCount);
        campaignMetricsRepository.save(metrics);
    }

    private CommunicationChannel launchContactChannel(CampaignChannel channel) {
        return switch (channel) {
            case SMS -> CommunicationChannel.SMS;
            case PHONE -> CommunicationChannel.PHONE;
            case EMAIL, MIXED -> CommunicationChannel.EMAIL;
        };
    }

    @CampaignWriteAccess
    @Transactional
    public CampaignView pauseCampaign(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);
        applyLifecycle(campaign::pause);
        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    @CampaignWriteAccess
    @Transactional
    public CampaignView completeCampaign(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);
        applyLifecycle(campaign::complete);
        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    @CampaignWriteAccess
    @Transactional
    public CampaignView archiveCampaign(UUID campaignId) {
        validateCampaignId(campaignId);
        Campaign campaign = findCampaign(campaignId);
        requireCampaignManagementAccess(campaign);
        Map<String, ?> oldValue = campaignAuditPayload(campaign);
        applyLifecycle(campaign::archive);
        Campaign saved = campaignRepository.save(campaign);
        auditService.logUpdate(
                currentActorUserId(),
                AUDIT_ENTITY_TYPE,
                saved.getId(),
                oldValue,
                campaignAuditPayload(saved));
        return toView(saved);
    }

    private void applyLifecycle(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new BusinessRuleException("CAMPAIGN_LIFECYCLE", ex.getMessage());
        }
    }

    private CampaignView toView(Campaign campaign) {
        List<UUID> productIds =
                campaignProductRepository.findByCampaignId(campaign.getId()).stream()
                        .map(CampaignProduct::getProductId)
                        .filter(Objects::nonNull)
                        .toList();
        return CampaignView.from(campaign, productIds);
    }

    private void requireEditableForTargetingSelection(Campaign campaign) {
        if (!campaign.canEdit()) {
            throw new BusinessRuleException(
                    "CAMPAIGN_LIFECYCLE",
                    "Campaign targeting (segment/products) cannot be changed in status "
                            + campaign.getStatus()
                            + "; only DRAFT or REJECTED");
        }
    }

    /**
     * Replaces promoted products for FR-052. Validates products exist and are active; deduplicates
     * ids; rejects soft-deleted catalog entries via {@link Product#isActive()}.
     */
    private void replaceProducts(Campaign campaign, List<UUID> productIds) {
        campaignProductRepository.deleteByCampaign_Id(campaign.getId());
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        Set<UUID> unique = new LinkedHashSet<>(productIds);
        List<CampaignProduct> links = new ArrayList<>();
        for (UUID productId : unique) {
            if (productId == null) {
                throw new ValidationException(
                        "Campaign product validation failed",
                        List.of("productIds: must not contain null"));
            }
            Product product =
                    productRepository
                            .findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
            if (!product.isActive()) {
                throw new ValidationException(
                        "Campaign product validation failed",
                        List.of(
                                "productIds: product "
                                        + productId
                                        + " is not active or is deleted"));
            }
            links.add(CampaignProduct.link(campaign, product));
        }
        campaignProductRepository.saveAll(links);
    }

    private Segment resolveSegment(UUID segmentId) {
        if (segmentId == null) {
            return null;
        }
        return segmentRepository
                .findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment", segmentId));
    }

    private Campaign findCampaign(UUID campaignId) {
        return campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void requireCampaignManagementAccess(Campaign campaign) {
        if (authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())) {
            return;
        }
        if (!campaign.isOwnedBy(authorizationExpressions.currentUserId())) {
            throw new ForbiddenException("Campaign is not owned by the current user");
        }
    }

    /**
     * KB item 250: Campaign Manager cannot approve own campaign. Admins and Compliance Officers may
     * approve; self-approval by the campaign owner is denied.
     */
    private void requireNotApprovingOwnCampaign(Campaign campaign) {
        UUID actorId = authorizationExpressions.currentUserId();
        if (campaign.isOwnedBy(actorId)
                && !authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())) {
            throw new ForbiddenException("Campaign owner cannot approve or reject own campaign");
        }
    }

    private List<Campaign> loadCandidates(CampaignSearchCriteria criteria) {
        if (criteria.ownerUserId() != null) {
            return campaignRepository.findByOwnerUserId(criteria.ownerUserId());
        }
        if (criteria.status() != null) {
            return campaignRepository.findByStatus(criteria.status());
        }
        if (criteria.segmentId() != null) {
            return campaignRepository.findBySegment_IdOrderByNameAsc(criteria.segmentId());
        }
        return campaignRepository.findAll();
    }

    private boolean matchesSearch(Campaign campaign, CampaignSearchCriteria criteria) {
        if (criteria.status() != null && campaign.getStatus() != criteria.status()) {
            return false;
        }
        if (criteria.ownerUserId() != null
                && !Objects.equals(campaign.getOwnerUserId(), criteria.ownerUserId())) {
            return false;
        }
        if (criteria.segmentId() != null
                && !Objects.equals(campaign.getSegmentId(), criteria.segmentId())) {
            return false;
        }
        if (!StringUtils.hasText(criteria.term())) {
            return true;
        }
        String term = criteria.term().trim().toLowerCase(Locale.ROOT);
        return campaign.getName().toLowerCase(Locale.ROOT).contains(term)
                || (campaign.getObjective() != null
                        && campaign.getObjective().toLowerCase(Locale.ROOT).contains(term));
    }

    private CampaignSearchCriteria normalize(CampaignSearchCriteria criteria) {
        if (criteria == null) {
            return new CampaignSearchCriteria(null, null, null, null);
        }
        return new CampaignSearchCriteria(
                normalize(criteria.term()),
                criteria.ownerUserId(),
                criteria.status(),
                criteria.segmentId());
    }

    private void validateCreateCommand(CreateCampaignCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Campaign validation failed", List.of("command: is required"));
        }
        List<String> errors =
                collectCampaignFormValidationErrors(
                        command.name(),
                        command.objective(),
                        command.channel(),
                        command.messageSubject(),
                        command.startDate(),
                        command.endDate());
        if (!errors.isEmpty()) {
            throw new ValidationException("Campaign validation failed", errors);
        }
    }

    private void validateUpdateCommand(UpdateCampaignCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Campaign validation failed", List.of("command: is required"));
        }
        List<String> errors =
                collectCampaignFormValidationErrors(
                        command.name(),
                        command.objective(),
                        command.channel(),
                        command.messageSubject(),
                        command.startDate(),
                        command.endDate());
        if (!errors.isEmpty()) {
            throw new ValidationException("Campaign validation failed", errors);
        }
    }

    private void validateCampaignReadyForSubmission(Campaign campaign) {
        List<String> errors =
                collectCampaignFormValidationErrors(
                        campaign.getName(),
                        campaign.getObjective(),
                        campaign.getChannel(),
                        campaign.getMessageSubject(),
                        campaign.getStartDate(),
                        campaign.getEndDate());
        if (!errors.isEmpty()) {
            throw new ValidationException("Campaign submission validation failed", errors);
        }
    }

    /**
     * User-facing campaign form validation messages (item 242). Kept aligned with frontend form
     * copy and Jakarta constraint messages on create/update request DTOs.
     */
    private List<String> collectCampaignFormValidationErrors(
            String name,
            String objective,
            CampaignChannel channel,
            String messageSubject,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(name)) {
            errors.add("Campaign name is required.");
        } else if (name.length() > 255) {
            errors.add("Campaign name must be 255 characters or fewer.");
        }
        if (!StringUtils.hasText(objective)) {
            errors.add("Campaign objective is required.");
        }
        if (channel == null) {
            errors.add("Campaign channel is required.");
        }
        if (messageSubject != null && messageSubject.length() > 255) {
            errors.add("Message subject must be 255 characters or fewer.");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            errors.add("End date must not be before start date.");
        }
        return errors;
    }

    private void validateCampaignId(UUID campaignId) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Campaign validation failed", List.of("campaignId: is required"));
        }
    }

    private UUID currentActorUserId() {
        try {
            return authorizationExpressions.currentUserId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> campaignAuditPayload(Campaign campaign) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", campaign.getId() == null ? null : campaign.getId().toString());
        payload.put("name", campaign.getName());
        payload.put("objective", campaign.getObjective());
        payload.put("status", campaign.getStatus() == null ? null : campaign.getStatus().name());
        payload.put(
                "ownerUserId",
                campaign.getOwnerUserId() == null ? null : campaign.getOwnerUserId().toString());
        payload.put(
                "segmentId",
                campaign.getSegmentId() == null ? null : campaign.getSegmentId().toString());
        payload.put("channel", campaign.getChannel() == null ? null : campaign.getChannel().name());
        payload.put("messageSubject", campaign.getMessageSubject());
        payload.put("messageBody", campaign.getMessageBody());
        payload.put(
                "startDate",
                campaign.getStartDate() == null ? null : campaign.getStartDate().toString());
        payload.put(
                "endDate", campaign.getEndDate() == null ? null : campaign.getEndDate().toString());
        payload.put(
                "approvedByUserId",
                campaign.getApprovedByUserId() == null
                        ? null
                        : campaign.getApprovedByUserId().toString());
        payload.put(
                "approvedAt",
                campaign.getApprovedAt() == null ? null : campaign.getApprovedAt().toString());
        payload.put("rejectionReason", campaign.getRejectionReason());
        payload.put("complianceReviewNotes", campaign.getComplianceReviewNotes());
        if (campaign.getId() != null) {
            List<String> productIds =
                    campaignProductRepository.findByCampaignId(campaign.getId()).stream()
                            .map(CampaignProduct::getProductId)
                            .filter(Objects::nonNull)
                            .map(UUID::toString)
                            .toList();
            payload.put("productIds", productIds);
        }
        return payload;
    }
}
