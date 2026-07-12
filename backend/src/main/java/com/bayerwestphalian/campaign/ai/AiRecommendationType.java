package com.bayerwestphalian.campaign.ai;

/**
 * AI recommendation category (KB PostgreSQL enum {@code ai_recommendation_type} / epic E21).
 *
 * <p>Maps AI-001–AI-006 decision-support outputs stored on {@link AiRecommendation}:
 *
 * <ul>
 *   <li>{@link #PRODUCT} — product recommendations (AI-003)
 *   <li>{@link #SEGMENT} — segment suggestions (AI-002)
 *   <li>{@link #COPY} — campaign copy suggestions requiring human approval (AI-005 / COMP-005)
 *   <li>{@link #RISK} — default-risk scoring (AI-004)
 *   <li>{@link #DUPLICATE_WARNING} — duplicate-contact risk warning (AI-006)
 * </ul>
 */
public enum AiRecommendationType {
    /** Rule-based product recommendation by profile and owned products (AI-003). */
    PRODUCT,

    /** Rule-based audience / segment suggestion (AI-002). */
    SEGMENT,

    /** Campaign copy suggestion; must not auto-apply without human approval (AI-005 / COMP-005). */
    COPY,

    /** Default-risk score from payment / reminder history (AI-004). */
    RISK,

    /** Warning when contact frequency or repeated-campaign rules are at risk (AI-006). */
    DUPLICATE_WARNING
}
