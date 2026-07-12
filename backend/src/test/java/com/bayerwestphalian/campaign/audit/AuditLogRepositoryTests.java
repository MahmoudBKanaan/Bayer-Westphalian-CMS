package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.JpaRepository;

class AuditLogRepositoryTests {

    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000009902");
    private static final UUID ENTITY_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");

    @Test
    @DisplayName("516 Implement AuditLogRepository")
    void exposesKbAuditLogRepositoryContract() throws Exception {
        assertThat(AuditLogRepository.class.getInterfaces()).contains(JpaRepository.class);
        assertMethod("findRecent");
        assertMethod("findByEntityTypeAndEntityId", String.class, UUID.class);
        assertMethod("findByActorUserId", UUID.class);
        assertMethod("findAllByOrderByCreatedAtDesc");
        assertMethod("findByEntityTypeAndEntityIdOrderByCreatedAtDesc", String.class, UUID.class);
        assertMethod("findByActorUserIdOrderByCreatedAtDesc", UUID.class);
    }

    @Test
    void findRecentDelegatesToDescendingCreatedAtQuery() {
        AuditLogRepository repository =
                Mockito.mock(AuditLogRepository.class, Mockito.CALLS_REAL_METHODS);
        List<AuditLog> logs =
                List.of(AuditLog.recordCreate(ACTOR_ID, "campaigns", ENTITY_ID, null));
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(logs);

        List<AuditLog> recent = repository.findRecent();

        assertThat(recent).isSameAs(logs);
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void kbLookupMethodsDelegateToDescendingCreatedAtQueries() {
        AuditLogRepository repository =
                Mockito.mock(AuditLogRepository.class, Mockito.CALLS_REAL_METHODS);
        List<AuditLog> logs =
                List.of(
                        AuditLog.recordChange(
                                ACTOR_ID, "UPDATE", "campaigns", ENTITY_ID, null, null));
        when(repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("campaigns", ENTITY_ID))
                .thenReturn(logs);
        when(repository.findByActorUserIdOrderByCreatedAtDesc(ACTOR_ID)).thenReturn(logs);

        assertThat(repository.findByEntityTypeAndEntityId("campaigns", ENTITY_ID)).isSameAs(logs);
        assertThat(repository.findByActorUserId(ACTOR_ID)).isSameAs(logs);
        verify(repository).findByEntityTypeAndEntityIdOrderByCreatedAtDesc("campaigns", ENTITY_ID);
        verify(repository).findByActorUserIdOrderByCreatedAtDesc(ACTOR_ID);
    }

    private static void assertMethod(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = AuditLogRepository.class.getMethod(methodName, parameterTypes);

        assertThat(method.getReturnType()).isEqualTo(List.class);
    }
}
