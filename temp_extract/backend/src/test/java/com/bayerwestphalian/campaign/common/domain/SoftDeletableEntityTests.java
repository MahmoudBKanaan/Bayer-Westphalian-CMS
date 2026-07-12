package com.bayerwestphalian.campaign.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SoftDeletableEntityTests {

    @Test
    void declaresJpaMappedSuperclassAndExtendsBaseEntity() {
        assertThat(SoftDeletableEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();
        assertThat(BaseEntity.class).isAssignableFrom(SoftDeletableEntity.class);
    }

    @Test
    void mapsKbDeletedAtColumn() throws Exception {
        Field deletedAt = SoftDeletableEntity.class.getDeclaredField("deletedAt");
        Column column = deletedAt.getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo("deleted_at");
        assertThat(column.nullable()).isTrue();
        assertThat(column.updatable()).isTrue();
    }

    @Test
    void startsAsActiveWhenDeletedAtIsEmpty() {
        TestEntity entity = new TestEntity();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    void marksEntityDeletedWithTimestamp() {
        TestEntity entity = new TestEntity();

        entity.markDeleted();

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    void doesNotOverwriteExistingDeletionTimestamp() {
        Instant deletedAt = Instant.parse("2026-07-03T12:00:00Z");
        TestEntity entity = new TestEntity();

        entity.assignDeletedAt(deletedAt);
        entity.markDeleted();

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void restoresSoftDeletedEntity() {
        TestEntity entity = new TestEntity();

        entity.markDeleted();
        entity.restore();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }

    private static final class TestEntity extends SoftDeletableEntity {

        void assignDeletedAt(Instant deletedAt) {
            setDeletedAt(deletedAt);
        }
    }
}
