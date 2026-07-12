package com.bayerwestphalian.campaign.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

class BaseEntityTests {

    @Test
    void declaresJpaMappedSuperclassWithAuditingListener() {
        assertThat(BaseEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();

        EntityListeners listeners = BaseEntity.class.getAnnotation(EntityListeners.class);

        assertThat(listeners.value()).containsExactly(AuditingEntityListener.class);
    }

    @Test
    void mapsKbIdAndAuditColumns() throws Exception {
        Field id = BaseEntity.class.getDeclaredField("id");
        Field createdAt = BaseEntity.class.getDeclaredField("createdAt");
        Field updatedAt = BaseEntity.class.getDeclaredField("updatedAt");

        assertThat(id.isAnnotationPresent(Id.class)).isTrue();
        assertColumn(id, "id", false, false);

        assertThat(createdAt.isAnnotationPresent(CreatedDate.class)).isTrue();
        assertColumn(createdAt, "created_at", false, false);

        assertThat(updatedAt.isAnnotationPresent(LastModifiedDate.class)).isTrue();
        assertColumn(updatedAt, "updated_at", false, true);
    }

    @Test
    void initializesUuidAndTimestampsBeforePersist() {
        TestEntity entity = new TestEntity();

        entity.create();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void preservesExistingUuidAndCreatedAtBeforePersist() {
        UUID id = UUID.fromString("10000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-07-03T12:00:00Z");
        TestEntity entity = new TestEntity();

        entity.assignId(id);
        entity.assignCreatedAt(createdAt);
        entity.create();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void refreshesUpdatedAtBeforeUpdate() {
        TestEntity entity = new TestEntity();
        entity.create();
        Instant originalUpdatedAt = entity.getUpdatedAt();

        entity.update();

        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    private static void assertColumn(
            Field field, String name, boolean nullable, boolean updatable) {
        Column column = field.getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(name);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.updatable()).isEqualTo(updatable);
    }

    private static final class TestEntity extends BaseEntity {

        void assignId(UUID id) {
            setId(id);
        }

        void assignCreatedAt(Instant createdAt) {
            setCreatedAt(createdAt);
        }

        void create() {
            onCreate();
        }

        void update() {
            onUpdate();
        }
    }
}
