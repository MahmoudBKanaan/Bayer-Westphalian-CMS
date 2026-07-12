package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.user.User;
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
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 469: Implement AiRecommendation entity.
 *
 * <p>Maps table {@code ai_recommendations} / enum {@code ai_recommendation_type} with KB fields
 * (type, target, input, recommendation, explanation, confidence, approver) and domain methods
 * {@code approve()}, {@code reject()}, {@code updateConfidence()}.
 */
@DisplayName("469 Implement AiRecommendation entity")
class AiRecommendationTests {

    private static final UUID TARGET_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000469");
    private static final UUID APPROVER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000469");

    @Nested
    @DisplayName("JPA mapping")
    class JpaMapping {

        @Test
        void mapsKbAiRecommendationsTableAsJpaEntity() {
            assertThat(AiRecommendation.class.isAnnotationPresent(Entity.class)).isTrue();
            assertThat(AiRecommendation.class.getAnnotation(Table.class).name())
                    .isEqualTo("ai_recommendations");
        }

        @Test
        void providesProtectedNoArgsConstructorForJpa() throws Exception {
            Constructor<AiRecommendation> constructor =
                    AiRecommendation.class.getDeclaredConstructor();
            assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
        }

        @Test
        void mapsAllKbAiRecommendationColumns() throws Exception {
            assertThat(field("id").isAnnotationPresent(Id.class)).isTrue();
            assertColumn("id", "id", false);
            assertColumn("recommendationType", "recommendation_type", false);
            assertColumn("targetEntityType", "target_entity_type", false);
            assertColumn("targetEntityId", "target_entity_id", true);
            assertColumn("inputSummary", "input_summary", false);
            assertColumn("recommendation", "recommendation", false);
            assertColumn("explanation", "explanation", false);
            assertColumn("confidenceScore", "confidence_score", true);
            assertColumn("reviewNotes", "review_notes", true);
            assertColumn("createdAt", "created_at", false);

            assertThat(field("targetEntityType").getAnnotation(Column.class).length())
                    .isEqualTo(100);
            assertThat(field("inputSummary").getAnnotation(Column.class).columnDefinition())
                    .isEqualTo("text");
            assertThat(field("recommendation").getAnnotation(Column.class).columnDefinition())
                    .isEqualTo("text");
            assertThat(field("explanation").getAnnotation(Column.class).columnDefinition())
                    .isEqualTo("text");
            assertThat(field("confidenceScore").getAnnotation(Column.class).precision())
                    .isEqualTo(5);
            assertThat(field("confidenceScore").getAnnotation(Column.class).scale()).isEqualTo(2);
        }

        @Test
        void mapsValidationAnnotationsOnRequiredFields() throws Exception {
            assertThat(field("recommendationType").isAnnotationPresent(NotNull.class)).isTrue();
            assertThat(field("targetEntityType").isAnnotationPresent(NotBlank.class)).isTrue();
            assertThat(field("targetEntityType").isAnnotationPresent(Size.class)).isTrue();
            assertThat(field("inputSummary").isAnnotationPresent(NotBlank.class)).isTrue();
            assertThat(field("recommendation").isAnnotationPresent(NotBlank.class)).isTrue();
            assertThat(field("explanation").isAnnotationPresent(NotBlank.class)).isTrue();
            assertThat(field("createdAt").isAnnotationPresent(NotNull.class)).isTrue();
        }

        @Test
        void mapsOptionalApprovedByRelationship() throws Exception {
            Field approvedBy = field("approvedBy");
            ManyToOne manyToOne = approvedBy.getAnnotation(ManyToOne.class);
            JoinColumn joinColumn = approvedBy.getAnnotation(JoinColumn.class);

            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            assertThat(manyToOne.optional()).isTrue();
            assertThat(joinColumn.name()).isEqualTo("approved_by_user_id");
            assertThat(approvedBy.getType()).isEqualTo(User.class);
        }

        @Test
        void mapsKbPostgreSqlRecommendationTypeEnum() throws Exception {
            Field typeField = field("recommendationType");
            Column column = typeField.getAnnotation(Column.class);
            Enumerated enumerated = typeField.getAnnotation(Enumerated.class);
            JdbcTypeCode jdbcTypeCode = typeField.getAnnotation(JdbcTypeCode.class);

            assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
            assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
            assertThat(column.columnDefinition()).isEqualTo("ai_recommendation_type");
            assertThat(column.nullable()).isFalse();
        }

        @Test
        void exposesKbDomainMethods() throws Exception {
            assertThat(AiRecommendation.class.getMethod("approve", User.class)).isNotNull();
            assertThat(AiRecommendation.class.getMethod("approve", User.class, String.class))
                    .isNotNull();
            assertThat(AiRecommendation.class.getMethod("reject")).isNotNull();
            assertThat(AiRecommendation.class.getMethod("updateConfidence", BigDecimal.class))
                    .isNotNull();
            assertThat(AiRecommendation.class.getMethod("updateReviewNotes", String.class))
                    .isNotNull();
            assertThat(AiRecommendation.class.getMethod("isApproved")).isNotNull();

            Method create =
                    AiRecommendation.class.getMethod(
                            "create",
                            AiRecommendationType.class,
                            String.class,
                            UUID.class,
                            String.class,
                            String.class,
                            String.class);
            assertThat(Modifier.isStatic(create.getModifiers())).isTrue();
        }
    }

    @Nested
    @DisplayName("Factory and field behaviour")
    class FactoryAndFields {

        @Test
        void createStoresKbConstructorFieldsWithoutApproverOrConfidence() {
            AiRecommendation recommendation =
                    AiRecommendation.create(
                            AiRecommendationType.PRODUCT,
                            "customer",
                            TARGET_ID,
                            "Age 35-45, owns life policy",
                            "Recommend homeowner insurance cross-sell",
                            "Customer owns life insurance but no homeowner product");

            assertThat(recommendation.getRecommendationType())
                    .isEqualTo(AiRecommendationType.PRODUCT);
            assertThat(recommendation.getTargetEntityType()).isEqualTo("customer");
            assertThat(recommendation.getTargetEntityId()).isEqualTo(TARGET_ID);
            assertThat(recommendation.getInputSummary())
                    .isEqualTo("Age 35-45, owns life policy");
            assertThat(recommendation.getRecommendation())
                    .isEqualTo("Recommend homeowner insurance cross-sell");
            assertThat(recommendation.getExplanation())
                    .isEqualTo("Customer owns life insurance but no homeowner product");
            assertThat(recommendation.getConfidenceScore()).isNull();
            assertThat(recommendation.getReviewNotes()).isNull();
            assertThat(recommendation.getApprovedBy()).isNull();
            assertThat(recommendation.getApprovedByUserId()).isNull();
            assertThat(recommendation.isApproved()).isFalse();
            assertThat(recommendation.getId()).isNull();
            assertThat(recommendation.getCreatedAt()).isNull();
        }

        @Test
        void createWithConfidenceNormalizesScaleTwo() {
            AiRecommendation recommendation =
                    AiRecommendation.create(
                            AiRecommendationType.RISK,
                            "customer",
                            TARGET_ID,
                            "2 missed payments, 1 red reminder",
                            "Elevated default risk",
                            "Missed payments and red reminder history increase risk",
                            new BigDecimal("72.5"));

            assertThat(recommendation.getConfidenceScore())
                    .isEqualByComparingTo(new BigDecimal("72.50"));
            assertThat(recommendation.getRecommendationType()).isEqualTo(AiRecommendationType.RISK);
        }

        @Test
        void createTrimsRequiredTextFields() {
            AiRecommendation recommendation =
                    AiRecommendation.create(
                            AiRecommendationType.SEGMENT,
                            "  campaign  ",
                            null,
                            "  Munich + expiring products  ",
                            "  Suggest location + expiration segment  ",
                            "  High product-expiration density in Munich  ");

            assertThat(recommendation.getTargetEntityType()).isEqualTo("campaign");
            assertThat(recommendation.getTargetEntityId()).isNull();
            assertThat(recommendation.getInputSummary())
                    .isEqualTo("Munich + expiring products");
            assertThat(recommendation.getRecommendation())
                    .isEqualTo("Suggest location + expiration segment");
            assertThat(recommendation.getExplanation())
                    .isEqualTo("High product-expiration density in Munich");
        }

        @Test
        void createRejectsMissingRecommendationType() {
            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            null,
                                            "customer",
                                            TARGET_ID,
                                            "input",
                                            "rec",
                                            "why"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Recommendation type");
        }

        @Test
        void createRejectsBlankTargetEntityTypeInputRecommendationOrExplanation() {
            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.COPY,
                                            "  ",
                                            TARGET_ID,
                                            "input",
                                            "rec",
                                            "why"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target entity type");

            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.COPY,
                                            "campaign",
                                            TARGET_ID,
                                            null,
                                            "rec",
                                            "why"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Input summary");

            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.COPY,
                                            "campaign",
                                            TARGET_ID,
                                            "input",
                                            "   ",
                                            "why"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Recommendation");

            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.COPY,
                                            "campaign",
                                            TARGET_ID,
                                            "input",
                                            "rec",
                                            ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Explanation");
        }

        @Test
        void createRejectsTargetEntityTypeLongerThan100() {
            String tooLong = "t".repeat(101);

            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.PRODUCT,
                                            tooLong,
                                            TARGET_ID,
                                            "input",
                                            "rec",
                                            "why"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }

        @Test
        void createRejectsConfidenceOutsideZeroToOneHundred() {
            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.DUPLICATE_WARNING,
                                            "customer",
                                            TARGET_ID,
                                            "3 campaigns this month",
                                            "Duplicate contact risk",
                                            "Monthly contact limit approaching",
                                            new BigDecimal("-0.01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 and 100");

            assertThatThrownBy(
                            () ->
                                    AiRecommendation.create(
                                            AiRecommendationType.DUPLICATE_WARNING,
                                            "customer",
                                            TARGET_ID,
                                            "3 campaigns this month",
                                            "Duplicate contact risk",
                                            "Monthly contact limit approaching",
                                            new BigDecimal("100.01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 and 100");
        }

        @Test
        void createAcceptsBoundaryConfidenceScores() {
            AiRecommendation zero =
                    AiRecommendation.create(
                            AiRecommendationType.RISK,
                            "customer",
                            TARGET_ID,
                            "input",
                            "rec",
                            "why",
                            BigDecimal.ZERO);
            AiRecommendation hundred =
                    AiRecommendation.create(
                            AiRecommendationType.RISK,
                            "customer",
                            TARGET_ID,
                            "input",
                            "rec",
                            "why",
                            new BigDecimal("100"));

            assertThat(zero.getConfidenceScore()).isEqualByComparingTo("0.00");
            assertThat(hundred.getConfidenceScore()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Human approval and confidence updates (COMP-005)")
    class ApprovalAndConfidence {

        @Test
        void approveSetsHumanApproverWithoutChangingRecommendationText() {
            AiRecommendation recommendation = sampleCopyRecommendation();
            User approver = sampleApprover();

            recommendation.approve(approver);

            assertThat(recommendation.isApproved()).isTrue();
            assertThat(recommendation.getApprovedBy()).isSameAs(approver);
            assertThat(recommendation.getApprovedByUserId()).isEqualTo(APPROVER_ID);
            assertThat(recommendation.getRecommendation())
                    .isEqualTo("Subject: Protect what matters");
            assertThat(recommendation.getExplanation())
                    .contains("Suggested subject and body for human review");
        }

        @Test
        void approveStoresTrimmedAuditReviewNotes() {
            AiRecommendation recommendation = sampleCopyRecommendation();

            recommendation.approve(sampleApprover(), "  Reviewed against campaign objective  ");

            assertThat(recommendation.isApproved()).isTrue();
            assertThat(recommendation.getReviewNotes())
                    .isEqualTo("Reviewed against campaign objective");
        }

        @Test
        void approveRejectsNullApprover() {
            AiRecommendation recommendation = sampleCopyRecommendation();

            assertThatThrownBy(() -> recommendation.approve(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Approver");
        }

        @Test
        void rejectClearsHumanApprover() {
            AiRecommendation recommendation = sampleCopyRecommendation();
            recommendation.approve(sampleApprover(), "Approved copy");

            recommendation.reject();

            assertThat(recommendation.isApproved()).isFalse();
            assertThat(recommendation.getApprovedBy()).isNull();
            assertThat(recommendation.getApprovedByUserId()).isNull();
            assertThat(recommendation.getReviewNotes()).isNull();
        }

        @Test
        void updateReviewNotesStoresNullForBlankText() {
            AiRecommendation recommendation = sampleCopyRecommendation();

            recommendation.updateReviewNotes("  ");

            assertThat(recommendation.getReviewNotes()).isNull();
        }

        @Test
        void updateConfidenceSetsAndClearsScore() {
            AiRecommendation recommendation = sampleCopyRecommendation();

            recommendation.updateConfidence(new BigDecimal("88.1"));
            assertThat(recommendation.getConfidenceScore())
                    .isEqualByComparingTo(new BigDecimal("88.10"));

            recommendation.updateConfidence(null);
            assertThat(recommendation.getConfidenceScore()).isNull();
        }

        @Test
        void updateConfidenceRejectsOutOfRange() {
            AiRecommendation recommendation = sampleCopyRecommendation();

            assertThatThrownBy(() -> recommendation.updateConfidence(new BigDecimal("101")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 and 100");
        }

        @Test
        void onCreateAssignsIdAndCreatedAt() {
            AiRecommendation recommendation = sampleCopyRecommendation();

            ReflectionTestUtils.invokeMethod(recommendation, "onCreate");

            assertThat(recommendation.getId()).isNotNull();
            assertThat(recommendation.getCreatedAt()).isNotNull();
        }
    }

    private static AiRecommendation sampleCopyRecommendation() {
        return AiRecommendation.create(
                AiRecommendationType.COPY,
                "campaign",
                TARGET_ID,
                "Objective: life cross-sell; channel EMAIL",
                "Subject: Protect what matters",
                "Suggested subject and body for human review only (COMP-005)");
    }

    private static User sampleApprover() {
        User user =
                User.create(
                        "ai.approver@bayer-westphalian.test",
                        "{noop}x",
                        "AI Approver");
        ReflectionTestUtils.setField(user, "id", APPROVER_ID);
        return user;
    }

    private static Field field(String name) throws Exception {
        Field field = AiRecommendation.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void assertColumn(String fieldName, String columnName, boolean nullable)
            throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
    }
}
