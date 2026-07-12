/**
 * AI-assisted decision support package (KB epic E21 / COMP-005).
 *
 * <p>Provides stored recommendations, search assistance, product/segment suggestions, risk scoring,
 * and campaign copy ideas that support human operators. AI outputs must never automatically approve
 * campaigns, override consent or opt-out, bypass do-not-contact, or skip eligibility checks.
 *
 * <p>Feature documentation (item 506): {@code docs/modules/ai-features.md}.
 *
 * <p>Limitations and human approval policy (item 507): {@code
 * docs/modules/ai-limitations-and-human-approval.md} — COMP-005 non-bypass rules and mandatory
 * human review (especially AI-005 campaign copy).
 *
 * <p>Decision-support explanation guide (item 508): {@code
 * docs/modules/ai-decision-support-explanation.md} — {@code explainScore} factors, required
 * narrative explanations, and operator/UI presentation rules.
 *
 * <p>Test evidence catalog (item 509): {@code docs/modules/ai-test-evidence.md} — maps AI backlog
 * items to backend/frontend automated tests (COMP-005).
 *
 * <p>Production gate (item 512): AI supports human decision-making only and must never override
 * compliance approval, consent, opt-out, do-not-contact, eligibility, or human approval rules —
 * formalized by {@code AiSupportsHumanDecisionMakingOnlyTests}.
 *
 * <p>Sprint 16 critical item 661 (AI recommendation cannot bypass consent rules): {@code
 * AiRecommendationCannotBypassConsentRulesTests} — COMP-005 / FR-034 / NFR-002 release gate.
 *
 * <p>Sprint 16 critical item 662 (AI-generated campaign copy requires human approval): {@code
 * AiGeneratedCampaignCopyRequiresHumanApprovalTests} — AI-005 / COMP-005 release gate.
 *
 * <p>Entity (item 469): {@link com.bayerwestphalian.campaign.ai.AiRecommendation} maps table {@code
 * ai_recommendations} with {@link com.bayerwestphalian.campaign.ai.AiRecommendationType}
 * ({@code PRODUCT}, {@code SEGMENT}, {@code COPY}, {@code RISK}, {@code DUPLICATE_WARNING}).
 *
 * <p>Repository (item 470): {@link com.bayerwestphalian.campaign.ai.AiRecommendationRepository}
 * extends {@code JpaRepository} with KB lookups {@code findByTargetEntity(type, id)} and {@code
 * findByRecommendationType(type)} (newest-first by {@code created_at}).
 *
 * <p>DTOs (item 471): request/view records for AI-001–AI-006 surfaces used by {@link
 * com.bayerwestphalian.campaign.ai.AiController} / services — {@link
 * com.bayerwestphalian.campaign.ai.AiRecommendationView}, {@link
 * com.bayerwestphalian.campaign.ai.AiCustomerSearchView} (+ hit / request / {@link
 * com.bayerwestphalian.campaign.ai.ScoreExplanationView} for {@code explainScore}), product and
 * segment recommendation request/views, {@link
 * com.bayerwestphalian.campaign.ai.DefaultRiskScoreView}, {@link
 * com.bayerwestphalian.campaign.ai.DuplicateContactRiskView}, {@link
 * com.bayerwestphalian.campaign.ai.CampaignCopySuggestionView} (always requires human approval),
 * and {@link com.bayerwestphalian.campaign.ai.ApproveAiRecommendationRequest}.
 *
 * <p>Services: {@link com.bayerwestphalian.campaign.ai.AiSearchService} (AI-001), {@link
 * com.bayerwestphalian.campaign.ai.AiRecommendationService} (AI-002–AI-004, AI-006), {@link
 * com.bayerwestphalian.campaign.ai.CampaignCopyService} (AI-005).
 *
 * <p>Required stored fields: recommendation type, target entity type/id, input summary,
 * recommendation text, explanation, optional confidence score, optional human approver, created
 * timestamp. Domain transitions: {@code approve(User)}, {@code reject()}, {@code
 * updateConfidence(BigDecimal)}.
 */
package com.bayerwestphalian.campaign.ai;
