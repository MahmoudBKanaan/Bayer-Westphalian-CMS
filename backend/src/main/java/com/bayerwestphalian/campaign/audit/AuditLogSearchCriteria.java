package com.bayerwestphalian.campaign.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * Filter criteria for listing audit logs (KB item 517; supports Audit Log screen filters by actor,
 * entity, and date — item 533 / System Auditor persona).
 *
 * <p>All fields are optional. Empty criteria means “recent logs” (repository {@code findRecent}).
 * When both {@code entityType} and {@code entityId} are set, criteria align with {@link
 * EntityHistoryCriteria} / {@code getEntityHistory}.
 */
public record AuditLogSearchCriteria(
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        Instant createdFrom,
        Instant createdTo) {

    /** True when no filter dimensions are set. */
    public boolean isEmpty() {
        return actorUserId == null
                && !StringUtils.hasText(action)
                && !StringUtils.hasText(entityType)
                && entityId == null
                && createdFrom == null
                && createdTo == null;
    }

    /** True when entity type and id are both present (entity-history style filter). */
    public boolean hasEntityFilter() {
        return StringUtils.hasText(entityType) && entityId != null;
    }

    /** True when a created-at range bound is present. */
    public boolean hasDateFilter() {
        return createdFrom != null || createdTo != null;
    }

    /**
     * Builds entity-history criteria when {@link #hasEntityFilter()} is true.
     *
     * @throws IllegalStateException when entity type/id are incomplete
     */
    public EntityHistoryCriteria toEntityHistoryCriteria() {
        if (!hasEntityFilter()) {
            throw new IllegalStateException(
                    "entityType and entityId are required for entity history criteria");
        }
        return new EntityHistoryCriteria(entityType.trim(), entityId);
    }
}
