package com.bayerwestphalian.campaign.audit;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Audit logging service (KB item 518 / epic E22 / COMP-008 / NFR-008).
 *
 * <p>Write helpers run with {@link Propagation#MANDATORY} so sensitive domain operations must share
 * the same transaction as the audit row (immutable application-level trail). Read APIs support
 * {@code listAuditLogs} and {@code getEntityHistory} for Admin, Compliance Officer, and System
 * Auditor.
 *
 * <p>KB methods: {@code logCreate}, {@code logUpdate}, {@code logDelete}, {@code logApproval},
 * {@code logConsentChange}, {@code logRoleChange}, plus campaign workflow and report-export
 * helpers used by later backlog items (520–531).
 */
@Service
public class AuditService {

    private static final String AUDIT_READ =
            "@authz.hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'SYSTEM_AUDITOR')";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Lists recent audit logs newest first (KB {@code listAuditLogs} / {@code GET /api/audit-logs}).
     */
    @PreAuthorize(AUDIT_READ)
    @Transactional(readOnly = true)
    public List<AuditLogView> listAuditLogs() {
        return auditLogRepository.findRecent().stream().map(AuditLogView::from).toList();
    }

    /**
     * Lists audit logs filtered by actor, action, entity, and/or created-at range (item 517
     * criteria; Audit Log screen filters — item 533).
     */
    @PreAuthorize(AUDIT_READ)
    @Transactional(readOnly = true)
    public List<AuditLogView> listAuditLogs(AuditLogSearchCriteria criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return listAuditLogs();
        }

        List<AuditLog> rows;
        if (criteria.hasEntityFilter()) {
            rows =
                    auditLogRepository.findByEntityTypeAndEntityId(
                            criteria.entityType().trim(), criteria.entityId());
        } else if (criteria.actorUserId() != null) {
            rows = auditLogRepository.findByActorUserId(criteria.actorUserId());
        } else {
            rows = auditLogRepository.findRecent();
        }

        return rows.stream()
                .filter(log -> matchesSearch(criteria, log))
                .map(AuditLogView::from)
                .toList();
    }

    /**
     * Entity history newest first (KB {@code getEntityHistory}).
     *
     * @throws ValidationException when entity type is blank or entity id is null
     */
    @PreAuthorize(AUDIT_READ)
    @Transactional(readOnly = true)
    public List<AuditLogView> getEntityHistory(String entityType, UUID entityId) {
        if (!StringUtils.hasText(entityType)) {
            throw new ValidationException(
                    "Audit validation failed", List.of("entityType: must not be blank"));
        }
        if (entityId == null) {
            throw new ValidationException(
                    "Audit validation failed", List.of("entityId: is required"));
        }
        return getEntityHistory(new EntityHistoryCriteria(entityType, entityId));
    }

    /**
     * Entity history using validated {@link EntityHistoryCriteria}.
     *
     * @throws NullPointerException when {@code criteria} is null
     */
    @PreAuthorize(AUDIT_READ)
    @Transactional(readOnly = true)
    public List<AuditLogView> getEntityHistory(EntityHistoryCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria is required");
        return auditLogRepository
                .findByEntityTypeAndEntityId(criteria.entityType(), criteria.entityId())
                .stream()
                .map(AuditLogView::from)
                .toList();
    }

    /**
     * Records a generic sensitive-action change from a {@link RecordAuditChangeCommand} (KB {@code
     * AuditLog.recordChange} / item 517 command).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog recordChange(RecordAuditChangeCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Audit validation failed", List.of("command: is required"));
        }
        return auditLogRepository.save(command.toEntity());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logCreate(
            UUID actorUserId, String entityType, UUID entityId, Map<String, ?> newValue) {
        validateEntityType(entityType);

        return auditLogRepository.save(
                AuditLog.recordCreate(actorUserId, entityType, entityId, normalize(newValue)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logConsentCreation(
            UUID actorUserId, UUID consentRecordId, Map<String, ?> newValue) {
        return logConsentChange(actorUserId, "CREATE", consentRecordId, null, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logConsentWithdrawal(
            UUID actorUserId,
            UUID consentRecordId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return logConsentChange(
                actorUserId, "WITHDRAW_CONSENT", consentRecordId, oldValue, newValue);
    }

    /**
     * Marketing opt-out / consent status change (backlog item 525 / COMP-002).
     *
     * <p>Persists under {@code consent_records} with action {@code OPT_OUT}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logOptOutChange(
            UUID actorUserId,
            UUID consentRecordId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return logConsentChange(actorUserId, "OPT_OUT", consentRecordId, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logDoNotContactUpdate(
            UUID actorUserId, UUID customerId, Map<String, ?> oldValue, Map<String, ?> newValue) {
        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        "UPDATE_DO_NOT_CONTACT",
                        "customers",
                        customerId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logConsentChange(
            UUID actorUserId,
            String action,
            UUID consentRecordId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        validateAction(action);

        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        action,
                        "consent_records",
                        consentRecordId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logUpdate(
            UUID actorUserId,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        validateEntityType(entityType);

        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        "UPDATE",
                        entityType,
                        entityId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logDelete(
            UUID actorUserId,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        validateEntityType(entityType);

        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        "DELETE",
                        entityType,
                        entityId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logSubmission(
            UUID actorUserId,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return logWorkflowAction(actorUserId, "SUBMIT", entityType, entityId, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logApproval(
            UUID actorUserId,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return logWorkflowAction(actorUserId, "APPROVE", entityType, entityId, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logRejection(
            UUID actorUserId,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return logWorkflowAction(actorUserId, "REJECT", entityType, entityId, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logLaunch(
            UUID actorUserId,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return logWorkflowAction(actorUserId, "LAUNCH", entityType, entityId, oldValue, newValue);
    }

    /**
     * KB {@code logRoleChange} — role assignment or role set change on a user (SEC-012 / item 521).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logRoleChange(
            UUID actorUserId, UUID userId, Map<String, ?> oldValue, Map<String, ?> newValue) {
        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        "ASSIGN_ROLE",
                        "users",
                        userId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    /**
     * Convenience for new role assignment when only the resulting role payload is known.
     *
     * <p>Delegates to {@link #logRoleChange(UUID, UUID, Map, Map)}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logRoleAssignment(UUID actorUserId, UUID userId, Map<String, ?> newValue) {
        return logRoleChange(actorUserId, userId, null, newValue);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logUserDisable(
            UUID actorUserId, UUID userId, Map<String, ?> oldValue, Map<String, ?> newValue) {
        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        "DISABLE_USER",
                        "users",
                        userId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    /**
     * Report export audit trail (backlog item 531 / FR-109–FR-110 evidence).
     *
     * <p>Entity type {@code report_exports}, action {@code EXPORT_REPORT}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logReportExport(
            UUID actorUserId, UUID exportId, Map<String, ?> newValue) {
        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        "EXPORT_REPORT",
                        "report_exports",
                        exportId,
                        null,
                        normalize(newValue)));
    }

    private AuditLog logWorkflowAction(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        validateAction(action);
        validateEntityType(entityType);

        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId,
                        action,
                        entityType,
                        entityId,
                        normalize(oldValue),
                        normalize(newValue)));
    }

    private static boolean matchesSearch(AuditLogSearchCriteria criteria, AuditLog log) {
        if (criteria.actorUserId() != null
                && !criteria.actorUserId().equals(log.getActorUserId())) {
            return false;
        }
        if (StringUtils.hasText(criteria.action())
                && !criteria.action().trim().equalsIgnoreCase(log.getAction())) {
            return false;
        }
        if (StringUtils.hasText(criteria.entityType())
                && !criteria.entityType().trim().equalsIgnoreCase(log.getEntityType())) {
            return false;
        }
        if (criteria.entityId() != null && !criteria.entityId().equals(log.getEntityId())) {
            return false;
        }
        if (criteria.createdFrom() != null
                && (log.getCreatedAt() == null
                        || log.getCreatedAt().isBefore(criteria.createdFrom()))) {
            return false;
        }
        if (criteria.createdTo() != null
                && (log.getCreatedAt() == null
                        || log.getCreatedAt().isAfter(criteria.createdTo()))) {
            return false;
        }
        return true;
    }

    private void validateEntityType(String entityType) {
        if (!StringUtils.hasText(entityType)) {
            throw new ValidationException(
                    "Audit validation failed", List.of("entityType: must not be blank"));
        }
    }

    private void validateAction(String action) {
        if (!StringUtils.hasText(action)) {
            throw new ValidationException(
                    "Audit validation failed", List.of("action: must not be blank"));
        }
    }

    private Map<String, Object> normalize(Map<String, ?> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(value);
    }
}
