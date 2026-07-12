# AI Feature Documentation

The AI module provides **decision-support** assistance for internal Bayer-Westphalian employees
(KB epic **E21**). Features include innovative customer search, segment suggestions, product
recommendations, default-risk scoring, campaign copy suggestions with mandatory human approval, and
duplicate-contact risk warnings (**AI-001–AI-006**).

AI **supports human decision-making only**. It must not automatically make final legal, financial,
or marketing decisions, and must never override compliance approval, consent, opt-out,
do-not-contact, eligibility, or human approval rules (**COMP-005**, item **468**, item **512**).

## Package Boundary

Primary backend package:

```text
com.bayerwestphalian.campaign.ai
```

| Component | Responsibility |
| --- | --- |
| `AiController` | REST API under `/api/ai` |
| `AiSearchService` | Fuzzy/weighted customer search + `explainScore` (AI-001 / FR-015) |
| `AiRecommendationService` | Segment, product, default-risk, and duplicate-contact rules (AI-002–AI-004, AI-006) |
| `CampaignCopyService` | Campaign copy generation + human approval workflow (AI-005 / COMP-005) |
| `AiRecommendation` | JPA entity → `ai_recommendations` |
| `AiRecommendationRepository` | Persist/load recommendations by target and type |
| `AiRecommendationType` | `PRODUCT`, `SEGMENT`, `COPY`, `RISK`, `DUPLICATE_WARNING` |
| Request/view DTOs | Search, recommendation, risk, and copy payloads (item 471) |

Related domain packages:

```text
com.bayerwestphalian.campaign.customer
com.bayerwestphalian.campaign.product
com.bayerwestphalian.campaign.campaign
com.bayerwestphalian.campaign.segment
```

## KB Traceability

| KB / FR / rule | AI capability |
| --- | --- |
| Epic **E21** | AI-assisted features |
| **AI-001** / **FR-015** | Innovative fuzzy/weighted customer search |
| **AI-002** | Rule-based segment suggestions |
| **AI-003** | Rule-based product recommendations |
| **AI-004** | Default-risk score from payment / reminder history |
| **AI-005** | Campaign copy suggestion requiring human approval |
| **AI-006** | Duplicate-contact risk warning |
| **COMP-005** | AI suggestions require human review |
| Item **468** / **512** | AI must not bypass compliance, consent, eligibility, or human approval |
| Sprint 16 **661** | AI recommendation cannot bypass consent rules — `AiRecommendationCannotBypassConsentRulesTests` |
| Sprint 16 **662** | AI-generated campaign copy requires human approval — `AiGeneratedCampaignCopyRequiresHumanApprovalTests` |
| Item **469** | `AiRecommendation` entity |
| Item **470** | `AiRecommendationRepository` |
| Item **471** | AI DTOs |
| Item **472–484** | Services, rules, explanation + confidence storage |
| Item **485–489** | AI REST endpoints |
| Item **506** | This AI feature documentation |
| Item **512** | Production gate: human decision-making only; no compliance/consent/eligibility override |

## Non-Bypass Guarantees (COMP-005)

| Forbidden for AI | Authority remains with |
| --- | --- |
| Approve / reject / launch campaigns | Compliance Officer / Campaign Manager + status machine |
| Override consent or marketing opt-out (critical **661**) | Consent records + `EligibilityService` |
| Bypass do-not-contact | Customer `doNotContact` + eligibility rules |
| Replace recipient eligibility decisions | `EligibilityService` at preview and launch |
| Auto-apply campaign copy to live campaigns | Human approval via `CampaignCopyService.approveCampaignCopy` |

AI endpoints only **suggest**, **score**, or **warn**. Operational side effects (sending messages,
changing campaign status, recording consent) stay outside this package.

## Features

### AI-001 — Innovative customer search

- Service: `AiSearchService.fuzzyCustomerSearch` / `weightedSearch` / `explainScore`
- Endpoint: `GET /api/ai/customer-search?q={query}&limit={optional}`
- Response: `AiCustomerSearchView` with ranked `AiCustomerSearchHitView` rows
- Each hit includes a numeric **score** and **`explainScore`** factors (`ScoreExplanationView`:
  factor, weight, contribution, detail)
- Scoring considers name, email, phone, city, product ownership context, and related signals
- Does **not** grant contact permission; `doNotContact` is exposed for operator awareness only

Frontend: Customers page AI search via `frontend/src/api/ai.ts` (`searchAiCustomers`).

### AI-002 — Segment suggestions

- Service: `AiRecommendationService.suggestSegments`
- Endpoint: `POST /api/ai/segment-suggestions`
- Body: optional `SegmentSuggestionRequest` (customer seed, city/country, product type hint,
  expiration window)
- Response: `SegmentSuggestionView.ListResponse` with suggested name, criteria summary, and
  **explanation**
- Does **not** create or save a segment; Campaign Manager still owns segment CRUD

### AI-003 — Product recommendations

- Service: `AiRecommendationService.recommendProducts`
- Endpoint: `POST /api/ai/product-recommendations`
- Body: `ProductRecommendationRequest` (`customerId` required)
- Response: `ProductRecommendationView.ListResponse` with product identity, recommendation text,
  **explanation**, optional confidence, optional `storedRecommendationId`
- Rules use customer profile and owned products; recommendations may be persisted to
  `ai_recommendations` (`PRODUCT` type)

### AI-004 — Default-risk score

- Service: `AiRecommendationService.calculateDefaultRisk`
- Input: `DefaultRiskScoreRequest` (`customerId`)
- Output: `DefaultRiskScoreView` — risk score, risk level band, explanation, factor list
- Factors include missed payments, overdue days, reminder / default-risk payment signals
- Decision-support only: does **not** auto-exclude customers or launch reminders

### AI-005 — Campaign copy suggestion

- Service: `CampaignCopyService.generateCopySuggestion` / `approveCampaignCopy`
- Endpoints:
  - `POST /api/ai/campaign-copy`
  - `POST /api/ai/campaign-copy/{recommendationId}/approve`
- Response: `CampaignCopySuggestionView` with subject, body, call-to-action, explanation,
  confidence, and flags
- **`requiresHumanApproval` is always `true`** (compact constructor enforces COMP-005)
- Suggestions are stored as `AiRecommendationType.COPY` with explanation; approval records a human
  approver via `approve(User)` and does **not** mutate live campaign message fields automatically
  beyond the approval workflow provided by the service

Frontend: Campaign Builder AI copy helpers in `frontend/src/api/ai.ts` and
`CampaignBuilderPage`.

### AI-006 — Duplicate-contact risk warning

- Service: `AiRecommendationService.detectDuplicateRisk`
- Endpoint: `POST /api/ai/duplicate-contact-warning`
- Body: `DuplicateContactRiskRequest` (`customerId`, optional `campaignId`)
- Response: `DuplicateContactRiskView` — risk flag, warning text, explanation, monthly contact
  counts, same-campaign contact flag
- Aligns with **BR-010** (same campaign twice) and **BR-011** (monthly contact limit) as **warnings**
- Does **not** replace launch-time eligibility enforcement

## REST API Surface

Base path: `/api/ai`

| Method | Path | Description | Feature |
| --- | --- | --- | --- |
| `GET` | `/api/ai/customer-search` | Fuzzy/weighted customer search (`q`, optional `limit`) | AI-001 |
| `POST` | `/api/ai/segment-suggestions` | Segment suggestion list | AI-002 |
| `POST` | `/api/ai/product-recommendations` | Product recommendation list | AI-003 |
| `POST` | `/api/ai/duplicate-contact-warning` | Duplicate-contact risk warning | AI-006 |
| `POST` | `/api/ai/campaign-copy` | Generate campaign copy suggestion | AI-005 |
| `POST` | `/api/ai/campaign-copy/{recommendationId}/approve` | Human-approve stored copy suggestion | AI-005 / COMP-005 |

Responses use the standard `ApiResponse` envelope (`success`, `message`, `data`).

Service-level default-risk scoring (**AI-004**) is implemented on `AiRecommendationService`; expose
it through controllers when product needs a dedicated HTTP surface.

## Persistence (`ai_recommendations`)

Table created in Flyway `V1` / enhanced in `V13`.

| Field | Notes |
| --- | --- |
| `recommendation_type` | PostgreSQL enum `ai_recommendation_type` |
| `target_entity_type` / `target_entity_id` | Soft reference to customer, campaign, etc. |
| `input_summary` | What the rule engine considered |
| `recommendation` | Suggestion text |
| `explanation` | **Required** human-readable rationale (item 483 / 505) |
| `confidence_score` | Optional 0–100 (item 484) |
| `approved_by_user_id` | Human approver when reviewed |
| `created_at` | Audit timestamp |

Repository lookups (item 470): `findByTargetEntity`, `findByRecommendationType` (newest first).

## Authorization

Backend is authoritative. Path rules in `SecurityConfiguration` plus method-level `@PreAuthorize`:

| Surface | Typical allowed roles |
| --- | --- |
| `GET /api/ai/**` (search) | Admin, Campaign Manager, BI Analyst, Product Manager, Compliance Officer, Executive Viewer, System Auditor (`AI_RECOMMENDATION_ROLES` / customer-read style access) |
| Segment suggestions / product recommendations | `BI_ANALYST`, `CAMPAIGN_MANAGER` |
| Duplicate-contact warning | Authorized customer-read roles |
| Campaign copy generate / approve | `CAMPAIGN_MANAGER` |

Unauthenticated → **401**. Wrong role → **403**.

## Frontend Surfaces

| Area | Location |
| --- | --- |
| API client | `frontend/src/api/ai.ts` |
| Customer AI search UI | `CustomersPage` |
| Campaign copy + approve UI | `CampaignBuilderPage` |
| Tests | `frontend/src/api/ai.test.ts`, page tests for search/copy flows |

## Human Approval Policy (summary)

Full policy: [AI Limitations and Human Approval Policy](ai-limitations-and-human-approval.md)
(item **507**). Minimum rules for this module:

1. Every `CampaignCopySuggestionView` has `requiresHumanApproval = true`.
2. Stored `COPY` recommendations start unapproved (`approved_by_user_id` null).
3. Only an authorized human may call approve; AI never self-approves.
4. Explanations are always required on stored recommendations.
5. Warnings and scores never silently rewrite consent, eligibility, or campaign status.

## Related Documentation

- [AI Limitations and Human Approval Policy](ai-limitations-and-human-approval.md) — COMP-005 limitations and approval rules (item 507)
- [Eligibility Rules Documentation](../architecture/eligibility-rules.md) — non-bypassable contact gates
- [Consent Module Documentation](consent-module.md) — consent / opt-out source of truth
- [Campaign Lifecycle Documentation](campaign-lifecycle.md) — human campaign approval workflow
- [Communication Tracking Module](communication-tracking.md) — contact events for risk / search context
- [Role-Based Access Documentation](../architecture/role-based-access.md) — role matrix
- [Segmentation Module Documentation](segmentation-module.md) — where segment suggestions may be applied
- [AI Decision-Support Explanation](ai-decision-support-explanation.md) — explainScore, narrative rationale, UI guidance (item 508)
- [AI Test Evidence](ai-test-evidence.md) — acceptance-to-test catalog (item 509)

## Implementation Evidence

| Area | Location |
| --- | --- |
| Package | `backend/.../ai/` |
| Entity / type | `AiRecommendation.java`, `AiRecommendationType.java` |
| Repository | `AiRecommendationRepository.java` |
| Search | `AiSearchService.java` |
| Recommendations | `AiRecommendationService.java` |
| Copy | `CampaignCopyService.java` |
| Controller | `AiController.java` |
| Unit / service tests | `AiSearchServiceTests`, `AiRecommendationServiceTests`, `CampaignCopyServiceTests`, `AiDtoTests`, entity/repository tests |
| Controller tests | `AiControllerTests` |
| Production gate **512** | `AiSupportsHumanDecisionMakingOnlyTests` |
| Frontend API | `frontend/src/api/ai.ts` |
| This document | `docs/modules/ai-features.md` (item **506**) |
| Documentation tests | `AiFeatureDocumentationTests` |
| Full test evidence catalog | [AI Test Evidence](ai-test-evidence.md) (item **509**) |
