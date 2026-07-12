# AI Decision-Support Explanation

This document defines how **explanations** work for AI-assisted decision support in the
Bayer-Westphalian Campaign Management Platform (KB epic **E21**, **COMP-005**, items **474**,
**483**, **491**, **495–497**, **508**).

Every AI score, suggestion, warning, or draft must be **interpretable by a human**. Explanations
exist so operators can accept, reject, or ignore AI output with context — never so the system can
act autonomously.

Feature map: [AI Feature Documentation](ai-features.md) (item **506**).  
Hard limits: [AI Limitations and Human Approval Policy](ai-limitations-and-human-approval.md)
(item **507**).

Primary types:

```text
com.bayerwestphalian.campaign.ai.ScoreExplanationView
com.bayerwestphalian.campaign.ai.AiCustomerSearchHitView   (explainScore)
com.bayerwestphalian.campaign.ai.AiRecommendation          (explanation column)
com.bayerwestphalian.campaign.ai.AiRecommendationView
```

## Purpose

| Goal | Description |
| --- | --- |
| **Transparency** | Show *why* a customer ranked high, a product was suggested, or risk was elevated |
| **Human control (COMP-005)** | Enable informed review before operational use |
| **Auditability** | Persist narrative rationale on `ai_recommendations.explanation` |
| **Trust calibration** | Make rule-based heuristics inspectable; avoid “black box” automation |

Explanations are **decision support**, not legal, compliance, or eligibility determinations.

## KB Traceability

| KB / item | Explanation requirement |
| --- | --- |
| **COMP-005** | AI suggestions require human review — review needs readable rationale |
| **AI-001** / Item **474** | `explainScore` output on fuzzy/weighted search |
| **AI-002–AI-006** | Suggestions and scores include narrative **explanation** fields |
| Item **483** / **505** | Store recommendation explanation |
| Item **491** | AI explanation display (UI) |
| Item **495** | Search explanation is shown |
| Item **496** | Product recommendation returns explanation |
| Item **497** | Segment suggestion returns explanation |
| Item **508** | This decision-support explanation document |

## Explanation Forms

The platform uses two complementary explanation shapes.

### 1. Factor list — `ScoreExplanationView` / `explainScore`

Used when a **numeric score** is produced (search relevance, default-risk factors).

| Field | Meaning |
| --- | --- |
| `factor` | Named signal (e.g. `full name`, `email`, `missedPayments`) |
| `weight` | Relative importance of the factor in the rule set |
| `contribution` | How much this factor added to the score for this case |
| `detail` | Short human-readable note (e.g. “Exact email match”) |

DTO: `ScoreExplanationView`.  
Search hits embed `List<ScoreExplanationView> explainScore` on `AiCustomerSearchHitView`.  
Default-risk embeds factors on `DefaultRiskScoreView.factors`.

Service entry points:

- `AiSearchService.explainScore(customer, query)`
- `AiSearchService.weightedSearch` / `fuzzyCustomerSearch` (includes explainScore on each hit)
- `AiRecommendationService.calculateDefaultRisk` (builds factor list + summary explanation)

### 2. Narrative text — `explanation`

Used for **recommendations, warnings, and copy** that must be stored and reviewed as prose.

| Consumer | Field |
| --- | --- |
| Stored row | `ai_recommendations.explanation` (**required**, non-blank) |
| API view | `AiRecommendationView.explanation` |
| Product suggestion | `ProductRecommendationView.explanation` |
| Segment suggestion | `SegmentSuggestionView.explanation` |
| Risk score summary | `DefaultRiskScoreView.explanation` |
| Duplicate-contact warning | `DuplicateContactRiskView.explanation` (+ `warning`) |
| Campaign copy | `CampaignCopySuggestionView.explanation` |

`input_summary` records **what inputs** were considered; `recommendation` is the **suggested
action/text**; `explanation` is the **why**.

## Feature-by-Feature Explanation Guide

### AI-001 — Customer search

| Element | Operator should understand |
| --- | --- |
| `score` | Relative relevance to the query (rule-weighted), not contact permission |
| `explainScore` | Which fields matched (name, email, city, product, …) and how strongly |
| `doNotContact` | Displayed for awareness; still subject to eligibility rules |

**UI expectation (item 495):** show score **and** at least the top explanation factors when
presenting search hits (Customers page AI search).

### AI-002 — Segment suggestions

| Element | Operator should understand |
| --- | --- |
| `suggestedCriteriaSummary` | Machine-oriented criteria hints |
| `explanation` | Why this audience idea was proposed (location, ownership, expiration, payments) |

**Does not** create a segment or final eligible audience. Eligibility still applies at preview/launch.

### AI-003 — Product recommendations

| Element | Operator should understand |
| --- | --- |
| `recommendation` | What product / action is suggested |
| `explanation` | Profile/ownership gap rationale |
| `confidenceScore` | Optional 0–100 heuristic confidence — **not** probability of conversion |

**UI expectation (item 496):** always show `explanation` with the recommendation text.

### AI-004 — Default-risk score

| Element | Operator should understand |
| --- | --- |
| `riskScore` / `riskLevel` | Advisory band from payment/reminder history |
| `factors` | Contributing payment signals |
| `explanation` | Summary narrative tying factors together |

**Does not** auto-block campaigns or send red reminders by itself.

### AI-005 — Campaign copy

| Element | Operator should understand |
| --- | --- |
| `subject` / `body` / `callToAction` | Draft content only |
| `explanation` | Why this draft was generated (objective, product, audience hints) |
| `requiresHumanApproval` | Always **true** — content must be human-approved before operational use |

Stored `COPY` rows keep the same explanation for audit after approve/reject.

### AI-006 — Duplicate-contact warning

| Element | Operator should understand |
| --- | --- |
| `warning` | Short alert title |
| `explanation` | Link to BR-010 / BR-011 style risk (same campaign / monthly volume) |
| Counts / flags | Supporting numbers; launch still enforces limits |

## Storage and Audit Rules

1. **Explanation is mandatory** on every persisted `AiRecommendation` (DB check + entity validation).
2. Services that save recommendations must reject blank explanations (e.g. campaign copy
   `requireStoredExplanation`).
3. Optional **confidence** never replaces explanation or human approval.
4. `input_summary` should remain stable enough for audit (“what we looked at”).
5. Human approval records **who** reviewed (`approved_by_user_id`), not a new AI narrative.

## Presentation Principles (Operators & UI)

| Principle | Guidance |
| --- | --- |
| **Explain before act** | Show explanation (and factors when available) near the primary action |
| **Separate score from permission** | High search/risk score is not the same as eligible to contact |
| **Plain language** | Prefer `detail` / narrative text over raw weights alone |
| **No auto-apply** | Copy and recommendations require explicit human confirmation (items 491–492) |
| **Pair with policy** | Link or copy limitations when AI might be over-trusted |

Frontend surfaces that should surface explanations:

- Customer AI search results — `explainScore`
- Campaign Builder AI copy — `explanation` + approval state
- Any product/segment recommendation panel — `explanation` field

API client: `frontend/src/api/ai.ts`.

## What Explanations Are Not

Explanations **are not**:

- Legal advice or proof of valid consent; AI explanations are not legal advice
- A substitute for `EligibilityService` decisions
- Guarantees of conversion, payment, or compliance outcomes
- Automatic authorization to launch campaigns or send messages
- Model “chain-of-thought” from an external LLM (MVP uses **rule-based** narratives)

If an operator needs a contactability decision, they must use recipient preview / eligibility —
not AI explanation text alone.

## Engineering Checklist

When adding or changing AI features:

1. Every response DTO that recommends or scores includes either `explanation` and/or
   `explainScore` / `factors`.
2. Persisted recommendations always set non-blank `explanation`.
3. Unit tests assert explanation presence (items 495–497, 500, 505 patterns).
4. UI tests or components render explanation content where features are exposed.
5. Documentation stays aligned with [AI Feature Documentation](ai-features.md) and the
   [limitations policy](ai-limitations-and-human-approval.md).

## Related Documentation

- [AI Feature Documentation](ai-features.md) — endpoints and feature inventory (item 506)
- [AI Limitations and Human Approval Policy](ai-limitations-and-human-approval.md) — COMP-005
  non-bypass rules (item 507)
- [AI Test Evidence](ai-test-evidence.md) — which tests assert explanations (item 509)
- [Eligibility Rules Documentation](../architecture/eligibility-rules.md)
- [KPI Definition Document](kpi-definitions.md) — analytics metrics (distinct from AI scores)
- [Role-Based Access Documentation](../architecture/role-based-access.md)

## Implementation Evidence

| Area | Location |
| --- | --- |
| This document | `docs/modules/ai-decision-support-explanation.md` (item **508**) |
| Factor DTO | `ScoreExplanationView.java` |
| Search explainScore | `AiSearchService`, `AiCustomerSearchHitView` |
| Stored explanation | `AiRecommendation.explanation`, `AiRecommendationView` |
| Risk factors | `DefaultRiskScoreView` |
| Product / segment / copy / duplicate | Respective `*View` records under `.../ai/` |
| Tests | `AiSearchServiceTests`, `AiRecommendationServiceTests`, `CampaignCopyServiceTests`, `AiDtoTests` |
| Test evidence catalog | [AI Test Evidence](ai-test-evidence.md) (item **509**) |
| Documentation tests | `AiDecisionSupportExplanationDocumentationTests` |
