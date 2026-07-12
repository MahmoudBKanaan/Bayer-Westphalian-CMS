package com.bayerwestphalian.campaign.audit;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditLogView::from)
                .toList();
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

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog logRoleAssignment(UUID actorUserId, UUID userId, Map<String, ?> newValue) {
        return auditLogRepository.save(
                AuditLog.recordAction(
                        actorUserId, "ASSIGN_ROLE", "users", userId, null, normalize(newValue)));
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
