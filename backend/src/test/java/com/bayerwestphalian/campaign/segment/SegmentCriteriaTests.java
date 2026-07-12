package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class SegmentCriteriaTests {

    @Test
    void mapsKbSegmentCriteriaTableAsJpaEntity() {
        assertThat(SegmentCriteria.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(SegmentCriteria.class.getAnnotation(Table.class).name())
                .isEqualTo("segment_criteria");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<SegmentCriteria> constructor = SegmentCriteria.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbSegmentCriteriaColumnsAndValidationRules() throws Exception {
        assertColumn("id", "id", false, 255);
        assertColumn("fieldName", "field_name", false, 100);
        assertColumn("value", "value", false, 255);
        assertColumn("logicalGroup", "logical_group", true, 50);
        assertColumn("operator", "operator", false, 255);
        assertColumn("joinOperator", "join_operator", false, 255);

        assertThat(field("id").isAnnotationPresent(Id.class)).isTrue();
        assertThat(field("segment").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("fieldName").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("fieldName").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("value").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("value").getAnnotation(Column.class).columnDefinition()).isEqualTo("text");
        assertThat(field("logicalGroup").getAnnotation(Size.class).max()).isEqualTo(50);
        assertThat(field("operator").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("joinOperator").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("id").getAnnotation(Column.class).updatable()).isFalse();
    }

    @Test
    void mapsSegmentRelationshipToKbForeignKey() throws Exception {
        Field segmentField = field("segment");
        ManyToOne manyToOne = segmentField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = segmentField.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("segment_id");
        assertThat(joinColumn.nullable()).isFalse();
    }

    @Test
    void mapsOperatorAndJoinOperatorToKbPostgreSqlEnums() throws Exception {
        assertNativeEnumColumn("operator", "segment_operator");
        assertNativeEnumColumn("joinOperator", "segment_join_operator");
    }

    @Test
    void exposesKbSegmentOperatorValues() {
        assertThat(SegmentOperator.values())
                .containsExactly(
                        SegmentOperator.EQUALS,
                        SegmentOperator.NOT_EQUALS,
                        SegmentOperator.CONTAINS,
                        SegmentOperator.IN,
                        SegmentOperator.BETWEEN,
                        SegmentOperator.BEFORE,
                        SegmentOperator.AFTER);
    }

    @Test
    void exposesKbSegmentJoinOperatorValues() {
        assertThat(SegmentJoinOperator.values())
                .containsExactly(SegmentJoinOperator.AND, SegmentJoinOperator.OR);
    }

    @Test
    void createsCriterionWithKbDefaultsAndFields() {
        Segment segment =
                Segment.create("Customer audience", null, null, SegmentVisibility.PRIVATE);

        SegmentCriteria criterion =
                SegmentCriteria.create(
                        segment, "customer_type", SegmentOperator.EQUALS, "CUSTOMER");

        assertThat(criterion.getSegment()).isSameAs(segment);
        assertThat(criterion.getFieldName()).isEqualTo("customer_type");
        assertThat(criterion.getOperator()).isEqualTo(SegmentOperator.EQUALS);
        assertThat(criterion.getValue()).isEqualTo("CUSTOMER");
        assertThat(criterion.getLogicalGroup()).isNull();
        assertThat(criterion.getJoinOperator()).isEqualTo(SegmentJoinOperator.AND);
        assertThat(criterion.matches("CUSTOMER")).isTrue();
        assertThat(criterion.matches("PROSPECT")).isFalse();
    }

    @Test
    void supportsGroupedCriteriaWithOrJoinOperator() {
        Segment segment = Segment.create("Retirement audience", null, null, SegmentVisibility.TEAM);
        SegmentCriteria criterion =
                SegmentCriteria.create(
                        segment,
                        "age",
                        SegmentOperator.BETWEEN,
                        "30..65",
                        "retirement-readiness",
                        SegmentJoinOperator.OR);

        assertThat(criterion.getLogicalGroup()).isEqualTo("retirement-readiness");
        assertThat(criterion.getJoinOperator()).isEqualTo(SegmentJoinOperator.OR);
        assertThat(criterion.matches("40")).isTrue();
        assertThat(criterion.matches("70")).isFalse();
    }

    @Test
    void supportsKbUpdateMethods() {
        Segment segment = Segment.create("Draft audience", null, null, SegmentVisibility.PRIVATE);
        SegmentCriteria criterion =
                SegmentCriteria.create(segment, "city", SegmentOperator.EQUALS, "Berlin");

        criterion.updateFieldName("country");
        criterion.updateOperator(SegmentOperator.CONTAINS);
        criterion.updateValue("Germany");
        criterion.updateLogicalGroup("location");
        criterion.updateJoinOperator(SegmentJoinOperator.OR);

        assertThat(criterion.getFieldName()).isEqualTo("country");
        assertThat(criterion.getOperator()).isEqualTo(SegmentOperator.CONTAINS);
        assertThat(criterion.getValue()).isEqualTo("Germany");
        assertThat(criterion.getLogicalGroup()).isEqualTo("location");
        assertThat(criterion.getJoinOperator()).isEqualTo(SegmentJoinOperator.OR);
        assertThat(criterion.matches("North Germany")).isTrue();
    }

    @Test
    void matchesKbOperatorSemanticsForInBeforeAndAfter() {
        Segment segment = Segment.create("Policy audience", null, null, SegmentVisibility.PRIVATE);
        SegmentCriteria inCriterion =
                SegmentCriteria.create(
                        segment, "policy_status", SegmentOperator.IN, "ACTIVE,LAPSED");
        SegmentCriteria beforeCriterion =
                SegmentCriteria.create(segment, "due_date", SegmentOperator.BEFORE, "2026-12-31");
        SegmentCriteria afterCriterion =
                SegmentCriteria.create(segment, "due_date", SegmentOperator.AFTER, "2026-01-01");

        assertThat(inCriterion.matches("ACTIVE")).isTrue();
        assertThat(inCriterion.matches("CANCELLED")).isFalse();
        assertThat(beforeCriterion.matches("2026-06-01")).isTrue();
        assertThat(beforeCriterion.matches("2027-01-01")).isFalse();
        assertThat(afterCriterion.matches("2026-06-01")).isTrue();
        assertThat(afterCriterion.matches("2025-12-31")).isFalse();
    }

    @Test
    void rejectsBlankFieldNamesValuesAndLogicalGroups() {
        Segment segment = Segment.create("Audience", null, null, SegmentVisibility.PRIVATE);
        SegmentCriteria criterion =
                SegmentCriteria.create(segment, "status", SegmentOperator.EQUALS, "ACTIVE");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> criterion.updateFieldName("  "))
                .withMessageContaining("Field name must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> criterion.updateValue(""))
                .withMessageContaining("Criterion value must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> criterion.updateLogicalGroup("   "))
                .withMessageContaining("Logical group must not be blank");
    }

    @Test
    void prePersistCreatesIdForKbPrimaryKey() throws Exception {
        Segment segment = Segment.create("Audience", null, null, SegmentVisibility.PRIVATE);
        SegmentCriteria criterion =
                SegmentCriteria.create(segment, "status", SegmentOperator.NOT_EQUALS, "INACTIVE");
        Method onCreate = SegmentCriteria.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        onCreate.invoke(criterion);

        assertThat(criterion.getId()).isNotNull();
    }

    private static void assertNativeEnumColumn(String fieldName, String columnDefinition)
            throws Exception {
        Field enumField = field(fieldName);
        Column column = enumField.getAnnotation(Column.class);
        Enumerated enumerated = enumField.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = enumField.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return SegmentCriteria.class.getDeclaredField(fieldName);
    }
}
