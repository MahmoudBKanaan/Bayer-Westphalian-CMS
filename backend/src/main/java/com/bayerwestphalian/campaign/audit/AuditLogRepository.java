package com.bayerwestphalian.campaign.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    default List<AuditLog> findRecent() {
        return findAllByOrderByCreatedAtDesc();
    }

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, UUID entityId);

    default List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId) {
        return findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);

    default List<AuditLog> findByActorUserId(UUID actorUserId) {
        return findByActorUserIdOrderByCreatedAtDesc(actorUserId);
    }
}
