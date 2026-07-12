package com.bayerwestphalian.campaign.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for AI recommendations (KB {@code AiRecommendationRepository} / table {@code
 * ai_recommendations} / item 470 / epic E21).
 *
 * <p>KB lookups:
 *
 * <ul>
 *   <li>{@link #findByTargetEntity(String, UUID)} — recommendations for a target entity
 *   <li>{@link #findByRecommendationType(AiRecommendationType)} — filter by PRODUCT / SEGMENT /
 *       COPY / RISK / DUPLICATE_WARNING
 * </ul>
 *
 * <p>Results are newest-first by {@code created_at} so recent decision-support suggestions surface
 * first for human review (COMP-005).
 */
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, UUID> {

    /**
     * Spring Data property-path lookup by target type and optional target id (newest first).
     *
     * @see #findByTargetEntity(String, UUID)
     */
    List<AiRecommendation> findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
            String targetEntityType, UUID targetEntityId);

    /**
     * Spring Data property-path lookup by recommendation type (newest first).
     *
     * @see #findByRecommendationType(AiRecommendationType)
     */
    List<AiRecommendation> findByRecommendationTypeOrderByCreatedAtDesc(
            AiRecommendationType recommendationType);

    /** Newest-first full recommendation history. */
    List<AiRecommendation> findAllByOrderByCreatedAtDesc();

    /** Recommendations for a target entity type only (any id), newest first. */
    List<AiRecommendation> findByTargetEntityTypeOrderByCreatedAtDesc(String targetEntityType);

    /** Recommendations approved by a given human user, newest first. */
    List<AiRecommendation> findByApprovedBy_IdOrderByCreatedAtDesc(UUID approvedByUserId);

    /**
     * KB {@code AiRecommendationRepository.findByTargetEntity()} — load suggestions for a target
     * entity (type + id).
     */
    default List<AiRecommendation> findByTargetEntity(
            String targetEntityType, UUID targetEntityId) {
        return findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                targetEntityType, targetEntityId);
    }

    /**
     * KB {@code AiRecommendationRepository.findByRecommendationType()} — load suggestions of a
     * given AI category.
     */
    default List<AiRecommendation> findByRecommendationType(
            AiRecommendationType recommendationType) {
        return findByRecommendationTypeOrderByCreatedAtDesc(recommendationType);
    }
}
