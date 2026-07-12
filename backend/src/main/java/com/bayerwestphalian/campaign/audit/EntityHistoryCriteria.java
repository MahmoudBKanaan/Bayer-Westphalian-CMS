package com.bayerwestphalian.campaign.audit;

import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * Criteria for {@code AuditService.getEntityHistory(entityType, entityId)} (KB item 517 / E22 /
 * AuditController {@code getEntityHistory}).
 *
 * <p>Loads immutable audit rows for a single business entity (campaign, consent, user, product,
 * etc.) newest first via {@code AuditLogRepository.findByEntityTypeAndEntityId}.
 */
public record EntityHistoryCriteria(String entityType, UUID entityId) {

    public EntityHistoryCriteria {
        if (!StringUtils.hasText(entityType)) {
            throw new IllegalArgumentException("entityType is required");
        }
        entityType = entityType.trim();
        Objects.requireNonNull(entityId, "entityId is required");
    }
}
