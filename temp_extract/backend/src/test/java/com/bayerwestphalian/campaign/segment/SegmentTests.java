package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SegmentTests {

    @Test
    void mapsKbSegmentsTableAsJpaEntity() {
        assertThat(Segment.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Segment.class.getAnnotation(Table.class).name()).isEqualTo("segments");
        assertThat(BaseEntity.class).isAssignableFrom(Segment.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<Segment> constructor = Segment.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbSegmentColumnsAndValidationRules() throws Exception {
        assertColumn("name", "name", false, 255);
        assertColumn("description", "description", true, 255);
        assertColumn("visibility", "visibility", false, 255);

        assertThat(field("name").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("name").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("visibility").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("description").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
    }

    @Test
    void mapsOwnerRelationshipToKbForeignKey() throws Exception {
        Field ownerField = field("owner");
        ManyToOne manyToOne = ownerField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = ownerField.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isTrue();
        assertThat(joinColumn.name()).isEqualTo("owner_user_id");
        assertThat(joinColumn.nullable()).isTrue();
    }

    @Test
    void mapsVisibilityToKbPostgreSqlEnum() throws Exception {
        Field visibility = field("visibility");
        Column column = visibility.getAnnotation(Column.class);
        Enumerated enumerated = visibility.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = visibility.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo("segment_visibility");
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    @Test
    void exposesKbSegmentVisibilityValues() {
        assertThat(SegmentVisibility.values())
                .containsExactly(
                        SegmentVisibility.PRIVATE,
                        SegmentVisibility.TEAM,
                        SegmentVisibility.GLOBAL);
    }

    @Test
    void createsSegmentWithKbDefaultsAndFields() {
        User owner =
                User.create(
                        "campaign.manager@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Campaign Manager");
        ReflectionTestUtils.setField(owner, "id", UUID.fromString("10000000-0000-0000-0000-000000000001"));

        Segment segment =
                Segment.create(
                        "Expiring homeowner policies",
                        "Customers with homeowner insurance expiring within six months",
                        owner,
                        SegmentVisibility.TEAM);

        assertThat(segment.getName()).isEqualTo("Expiring homeowner policies");
        assertThat(segment.getDescription())
                .isEqualTo("Customers with homeowner insurance expiring within six months");
        assertThat(segment.getOwner()).isSameAs(owner);
        assertThat(segment.getOwnerUserId()).isEqualTo(owner.getId());
        assertThat(segment.getVisibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(segment.isGlobal()).isFalse();
        assertThat(segment.isOwnedBy(owner.getId())).isTrue();
    }

    @Test
    void defaultsVisibilityToPrivateWhenNotProvided() {
        Segment segment = Segment.create("Private audience", null, null, null);

        assertThat(segment.getVisibility()).isEqualTo(SegmentVisibility.PRIVATE);
        assertThat(segment.getOwner()).isNull();
        assertThat(segment.getOwnerUserId()).isNull();
        assertThat(segment.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    void supportsKbSegmentMaintenanceMethods() {
        User owner =
                User.create(
                        "bi.analyst@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "BI Analyst");
        ReflectionTestUtils.setField(owner, "id", UUID.fromString("10000000-0000-0000-0000-000000000002"));
        Segment segment = Segment.create("Draft audience", "Initial draft", owner, SegmentVisibility.PRIVATE);

        segment.updateName("  Global renewal audience  ");
        segment.updateDescription("Shared renewal segment for campaign managers");
        segment.changeVisibility(SegmentVisibility.GLOBAL);
        segment.assignOwner(null);

        assertThat(segment.getName()).isEqualTo("Global renewal audience");
        assertThat(segment.getDescription())
                .isEqualTo("Shared renewal segment for campaign managers");
        assertThat(segment.getVisibility()).isEqualTo(SegmentVisibility.GLOBAL);
        assertThat(segment.isGlobal()).isTrue();
        assertThat(segment.getOwner()).isNull();
        assertThat(segment.isOwnedBy(owner.getId())).isFalse();
    }

    @Test
    void rejectsBlankSegmentNamesToMatchKbConstraint() {
        Segment segment = Segment.create("Valid audience", null, null, SegmentVisibility.PRIVATE);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> segment.updateName("   "))
                .withMessageContaining("Segment name must not be blank");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Segment.create("", null, null, SegmentVisibility.PRIVATE))
                .withMessageContaining("Segment name must not be blank");
    }

    @Test
    void rejectsNullVisibilityChanges() {
        Segment segment = Segment.create("Audience", null, null, SegmentVisibility.PRIVATE);

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> segment.changeVisibility(null))
                .withMessageContaining("Segment visibility is required");
    }

    @Test
    void managesCriteriaCollectionThroughKbAggregateMethods() {
        Segment segment = Segment.create("Audience with criteria", null, null, SegmentVisibility.TEAM);
        SegmentCriteria criterion =
                segment.addCriteria("customer_type", SegmentOperator.EQUALS, "CUSTOMER");

        assertThat(segment.getCriteria()).containsExactly(criterion);
        assertThat(criterion.getSegment()).isSameAs(segment);

        segment.removeCriteria(criterion);

        assertThat(segment.getCriteria()).isEmpty();
        assertThat(criterion.getSegment()).isNull();
    }

    @Test
    void mapsCriteriaRelationshipWithCascadeAndOrphanRemoval() throws Exception {
        Field criteriaField = Segment.class.getDeclaredField("criteria");
        OneToMany oneToMany = criteriaField.getAnnotation(OneToMany.class);

        assertThat(oneToMany.mappedBy()).isEqualTo("segment");
        assertThat(oneToMany.cascade()).containsExactly(CascadeType.ALL);
        assertThat(oneToMany.orphanRemoval()).isTrue();
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return Segment.class.getDeclaredField(fieldName);
    }
}