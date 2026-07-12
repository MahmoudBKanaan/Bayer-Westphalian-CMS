package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Stored AI decision-support recommendation (KB entity {@code AiRecommendation} / table {@code
 * ai_recommendations} / item 469 / epic E21).
 *
 * <p>Captures recommendation type, optional target entity, input summary, recommendation text,
 * human-readable explanation (required — COMP-005 / AI must support human decision-making),
 * optional confidence score, and optional human approver.
 *
 * <p>AI must never auto-approve campaigns, override consent, or bypass eligibility. This entity
 * only records suggestions for human review ({@link #approve(User)} / {@link #reject()}).
 *
 * <p>Factory: {@link #create(AiRecommendationType, String, UUID, String, String, String)}. Domain
 * helpers: {@link #approve(User)}, {@link #reject()}, {@link #updateConfidence(BigDecimal)}.
 */
@Entity
@Table(name = "ai_recommendations")
public class AiRecommendation {

    private static final int TARGET_ENTITY_TYPE_MAX_LENGTH = 100;
    private static final int CONFIDENCE_SCALE = 2;
    private static final BigDecimal CONFIDENCE_MIN = BigDecimal.ZERO;
    private static final BigDecimal CONFIDENCE_MAX = new BigDecimal("100.00");

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
            name = "recommendation_type",
            nullable = false,
            columnDefinition = "ai_recommendation_type")
    private AiRecommendationType recommendationType;

    @NotBlank
    @Size(max = TARGET_ENTITY_TYPE_MAX_LENGTH)
    @Column(
            name = "target_entity_type",
            nullable = false,
            length = TARGET_ENTITY_TYPE_MAX_LENGTH)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @NotBlank
    @Column(name = "input_summary", nullable = false, columnDefinition = "text")
    private String inputSummary;

    @NotBlank
    @Column(name = "recommendation", nullable = false, columnDefinition = "text")
    private String recommendation;

    @NotBlank
    @Column(name = "explanation", nullable = false, columnDefinition = "text")
    private String explanation;

    /** Optional confidence 0–100 (scale 2); null when the generator does not supply a score. */
    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "review_notes", columnDefinition = "text")
    private String reviewNotes;

    /**
     * Optional human approver (COMP-005). Null until a human approves; cleared on {@link
     * #reject()}. ON DELETE SET NULL at the database level.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiRecommendation() {}

    private AiRecommendation(
            AiRecommendationType recommendationType,
            String targetEntityType,
            UUID targetEntityId,
            String inputSummary,
            String recommendation,
            String explanation,
            BigDecimal confidenceScore) {
        this.recommendationType =
                Objects.requireNonNull(recommendationType, "Recommendation type is required");
        this.targetEntityType = normalizeRequiredText(targetEntityType, "Target entity type");
        if (this.targetEntityType.length() > TARGET_ENTITY_TYPE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Target entity type must not exceed "
                            + TARGET_ENTITY_TYPE_MAX_LENGTH
                            + " characters");
        }
        this.targetEntityId = targetEntityId;
        this.inputSummary = normalizeRequiredText(inputSummary, "Input summary");
        this.recommendation = normalizeRequiredText(recommendation, "Recommendation");
        this.explanation = normalizeRequiredText(explanation, "Explanation");
        this.confidenceScore = normalizeConfidence(confidenceScore);
    }

    /**
     * Creates a new recommendation awaiting human review (KB constructor {@code
     * AiRecommendation(type, targetType, targetId, input, recommendation, explanation)} / item
     * 469).
     *
     * @param recommendationType PRODUCT / SEGMENT / COPY / RISK / DUPLICATE_WARNING
     * @param targetEntityType entity type string (e.g. {@code customer}, {@code campaign})
     * @param targetEntityId optional target id
     * @param inputSummary summary of inputs used to produce the suggestion
     * @param recommendation suggestion text or structured description
     * @param explanation human-readable rationale (required for auditability / COMP-005)
     */
    public static AiRecommendation create(
            AiRecommendationType recommendationType,
            String targetEntityType,
            UUID targetEntityId,
            String inputSummary,
            String recommendation,
            String explanation) {
        return new AiRecommendation(
                recommendationType,
                targetEntityType,
                targetEntityId,
                inputSummary,
                recommendation,
                explanation,
                null);
    }

    /**
     * Creates a recommendation with an optional confidence score (0–100, item 484).
     */
    public static AiRecommendation create(
            AiRecommendationType recommendationType,
            String targetEntityType,
            UUID targetEntityId,
            String inputSummary,
            String recommendation,
            String explanation,
            BigDecimal confidenceScore) {
        return new AiRecommendation(
                recommendationType,
                targetEntityType,
                targetEntityId,
                inputSummary,
                recommendation,
                explanation,
                confidenceScore);
    }

    /**
     * Records human approval of this suggestion (COMP-005 / AI-005). Does not apply campaign,
     * consent, or eligibility side effects — callers remain responsible for operational actions.
     *
     * @param approver authenticated human user who reviewed the recommendation
     */
    public void approve(User approver) {
        this.approvedBy = Objects.requireNonNull(approver, "Approver is required");
    }

    public void approve(User approver, String reviewNotes) {
        approve(approver);
        updateReviewNotes(reviewNotes);
    }

    /**
     * Clears human approval (reject / withdraw approval). The suggestion row remains stored for
     * audit; {@code approved_by_user_id} is set to null.
     */
    public void reject() {
        this.approvedBy = null;
        this.reviewNotes = null;
    }

    /**
     * Updates optional confidence score (0–100 inclusive, scale 2). Pass {@code null} to clear.
     */
    public void updateConfidence(BigDecimal confidenceScore) {
        this.confidenceScore = normalizeConfidence(confidenceScore);
    }

    public void updateReviewNotes(String reviewNotes) {
        this.reviewNotes = normalizeOptionalText(reviewNotes);
    }

    public boolean isApproved() {
        return approvedBy != null;
    }

    public UUID getId() {
        return id;
    }

    public AiRecommendationType getRecommendationType() {
        return recommendationType;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getExplanation() {
        return explanation;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public UUID getApprovedByUserId() {
        return approvedBy == null ? null : approvedBy.getId();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static String normalizeRequiredText(String value, String fieldLabel) {
        Objects.requireNonNull(value, fieldLabel + " is required");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " is required");
        }
        return trimmed;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Normalizes confidence to scale 2; enforces 0–100 range matching DB check
     * {@code ai_recommendations_confidence_score_range}.
     */
    private static BigDecimal normalizeConfidence(BigDecimal confidenceScore) {
        if (confidenceScore == null) {
            return null;
        }
        BigDecimal normalized = confidenceScore.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
        if (normalized.compareTo(CONFIDENCE_MIN) < 0 || normalized.compareTo(CONFIDENCE_MAX) > 0) {
            throw new IllegalArgumentException("Confidence score must be between 0 and 100");
        }
        return normalized;
    }
}
