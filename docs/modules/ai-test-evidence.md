# AI Test Evidence

This document is the **test evidence catalog** for AI-assisted decision support (KB epic **E21**,
items **468–512**, item **509**). It maps acceptance backlog items to automated unit, integration,
controller, documentation, and frontend tests that prove AI features behave as decision support
under **COMP-005**.

Related:

- [AI Feature Documentation](ai-features.md) (item **506**)
- [AI Limitations and Human Approval Policy](ai-limitations-and-human-approval.md) (item **507**)
- [AI Decision-Support Explanation](ai-decision-support-explanation.md) (item **508**)

**Important:** This catalog **lists** tests. Item **509** does not require executing the suite;
execution remains a separate full-suite run (e.g. item **513**) when requested.

## Evidence Summary

| Layer | Location | Role |
| --- | --- | --- |
| Backend unit / service | `backend/src/test/java/.../ai/` | Entity, DTO, repository contract, search, recommendations, copy, controller |
| Backend integration | `AiRecommendationRepositoryIntegrationTests` (+ Flyway AI table tests) | PostgreSQL persistence of `ai_recommendations` |
| Backend documentation | `Ai*DocumentationTests` under `.../ai/` | Docs for features, policy, explanation, this evidence file |
| Frontend unit | `frontend/src/api/ai.test.ts`, AI components | API client + explanation/section UI |
| Frontend page | `CustomersPage.test.tsx`, `CampaignBuilderPage.test.tsx` | Search explanations, copy human-approval UX |
| Database migrations | `FlywayMigrationResourceTests` / `FlywayMigrationIntegrationTests` | `ai_recommendations` schema |

## Backend Test Inventory

| Test class | Primary KB coverage |
| --- | --- |
| `AiRecommendationTests` | Item **469** entity mapping, factory, approve/reject/confidence |
| `AiRecommendationTypeTests` | Item **469** enum values PRODUCT/SEGMENT/COPY/RISK/DUPLICATE_WARNING |
| `AiRecommendationRepositoryTests` | Item **470** `findByTargetEntity` / `findByRecommendationType` |
| `AiRecommendationRepositoryIntegrationTests` | Item **470** PostgreSQL save + queries |
| `AiDtoTests` | Item **471** AI request/view DTOs, explainScore, copy requires approval |
| `AiSearchServiceTests` | Items **472–474**, **494** fuzzy/weighted search + explainScore |
| `AiRecommendationServiceTests` | Items **475–479**, **496–499**, **505** rules + explanations + storage |
| `CampaignCopyServiceTests` | Items **480–482**, **500–501** copy, human approval, no campaign lifecycle approve |
| `AiControllerTests` | Items **485–489** HTTP endpoints + role authorization |
| `AiFeatureDocumentationTests` | Item **506** |
| `AiLimitationsAndHumanApprovalPolicyDocumentationTests` | Item **507** |
| `AiDecisionSupportExplanationDocumentationTests` | Item **508** |
| `AiTestEvidenceDocumentationTests` | Item **509** (this document) |
| `AiSupportsHumanDecisionMakingOnlyTests` | Item **512** COMP-005 production gate (no lifecycle/consent/DNC/eligibility override) |

Database companion evidence (not under `ai/` package tests):

| Test | Evidence |
| --- | --- |
| `FlywayMigrationResourceTests` | `ai_recommendations` DDL + V13 constraints (explanation not blank, confidence range) |
| `FlywayMigrationIntegrationTests` | Live table columns, FKs, indexes, sample insert/filter |

## Acceptance Mapping (Build Items)

| Item | Statement (short) | Primary evidence |
| --- | --- | --- |
| **469** | AiRecommendation entity | `AiRecommendationTests`, `AiRecommendationTypeTests` |
| **470** | AiRecommendationRepository | `AiRecommendationRepositoryTests`, `AiRecommendationRepositoryIntegrationTests` |
| **471** | AI DTOs | `AiDtoTests` |
| **472** | AiSearchService | `AiSearchServiceTests` |
| **473** | Fuzzy/weighted customer search | `AiSearchServiceTests` (weighted/fuzzy ranking) |
| **474** | explainScore output | `AiSearchServiceTests.explainScore*`, DTO search nested tests |
| **475** | AiRecommendationService | `AiRecommendationServiceTests` |
| **476** | Product recommendation rules | `AiRecommendationServiceTests.recommendProducts*` |
| **477** | Segment suggestion rules | `AiRecommendationServiceTests.suggestSegments*` |
| **478** | Default-risk scoring | `AiRecommendationServiceTests` **498** nested + risk methods |
| **479** | Duplicate-contact risk warning | `AiRecommendationServiceTests` **499** nested + detect methods |
| **480** | CampaignCopyService | `CampaignCopyServiceTests` |
| **481** | Campaign copy suggestion | `CampaignCopyServiceTests.generateCopySuggestion*` |
| **482** | Require human approval for copy | `CampaignCopyServiceTests` **500**, DTO compact ctor |
| **483** | Store recommendation explanation | Entity validation + service save paths; **505** nested |
| **484** | Store confidence when available | Entity confidence + copy/product risk views |
| **485** | AI customer search endpoint | `AiControllerTests` customer-search cases |
| **486** | AI segment suggestions endpoint | `AiControllerTests` segment-suggestions cases |
| **487** | AI product recommendations endpoint | `AiControllerTests` product-recommendations cases |
| **488** | AI campaign copy endpoint | `AiControllerTests` campaign-copy generate/approve |
| **489** | Duplicate-contact warning endpoint | `AiControllerTests` duplicate-contact-warning cases |

## Acceptance Mapping (Quality / Safety Items)

| Item | Statement (short) | Primary evidence |
| --- | --- | --- |
| **494** | Fuzzy search returns relevant customers | `AiSearchServiceTests` `@DisplayName("494 …")` |
| **495** | Search explanation is shown | Backend: explainScore assertions; Frontend: `CustomersPage.test.tsx` AI explanation UI |
| **496** | Product recommendation returns explanation | `AiRecommendationServiceTests` **496** |
| **497** | Segment suggestion returns explanation | `AiRecommendationServiceTests` **497** |
| **498** | Default-risk from payment history | `AiRecommendationServiceTests` **498** |
| **499** | Duplicate-contact warning detects risk | `AiRecommendationServiceTests` **499** |
| **500** | Campaign copy requires human approval | `CampaignCopyServiceTests` **500**; UI: `CampaignBuilderPage.test.tsx` |
| **501** | AI cannot approve campaign | `CampaignCopyServiceTests` **501** (approve copy ≠ campaign lifecycle approve) |
| **502** | AI cannot override consent | Structural: AI package has no consent mutation APIs; policy **507**; eligibility remains external |
| **503** | AI cannot bypass do-not-contact | Search exposes `doNotContact` for awareness only; no AI clear/set DNC |
| **504** | AI cannot bypass EligibilityService | AI does not call eligibility override; launch/preview remain authoritative |
| **505** | Recommendation stored with explanation | `AiRecommendationServiceTests` **505**; entity not-blank explanation |
| **512** | AI human decision-making only; never override compliance/consent/opt-out/DNC/eligibility/human approval | `AiSupportsHumanDecisionMakingOnlyTests` (+ items 500–501, policy **507**) |

Items **502–504** are enforced by **architecture and package boundaries** (documented in items
**507–508**) and consolidated under acceptance item **512**
(`AiSupportsHumanDecisionMakingOnlyTests`): no consent/eligibility service dependencies, no
lifecycle mutation APIs, DNC read-only in search, copy approval ≠ campaign approval.

## Documentation Evidence (Items 506–509)

| Item | Document | Documentation test |
| --- | --- | --- |
| **506** | `docs/modules/ai-features.md` | `AiFeatureDocumentationTests` |
| **507** | `docs/modules/ai-limitations-and-human-approval.md` | `AiLimitationsAndHumanApprovalPolicyDocumentationTests` |
| **508** | `docs/modules/ai-decision-support-explanation.md` | `AiDecisionSupportExplanationDocumentationTests` |
| **509** | `docs/modules/ai-test-evidence.md` | `AiTestEvidenceDocumentationTests` |

## Frontend Test Evidence

| File | Coverage |
| --- | --- |
| `frontend/src/api/ai.test.ts` | Client for customer-search, campaign-copy, approve |
| `frontend/src/components/AiExplanationDisplay.test.tsx` | Item **491** explanation, confidence, score factors |
| `frontend/src/components/AiRecommendationSections.test.tsx` | Item **490** role-scoped AI section links |
| `frontend/src/pages/CustomersPage.test.tsx` | AI search results + **AI explanation** / score factors labels |
| `frontend/src/pages/CampaignBuilderPage.test.tsx` | Item **492** human approval before apply approved copy |

## COMP-005 Safety Themes Covered by Tests

| Theme | How tests show it |
| --- | --- |
| Human review required for copy | `requiresHumanApproval` always true; pending until approve; UI apply disabled until approved |
| Explanation required | Entity + save paths reject blank explanation; **505** stores explanation |
| explainScore available | Search hits and `explainScore()` method assert factors |
| Role authorization | `AiControllerTests` 401/403 style role denials for wrong roles |
| No campaign auto-approve | **501** approve copy does not change campaign lifecycle status |
| Advisory risk/warnings | Risk and duplicate tests return warnings/scores, not launch side effects |

## How to Run (when requested)

Backend AI package (example):

```text
mvn -pl backend -Dtest=com.bayerwestphalian.campaign.ai.** test
```

Frontend AI-related (example):

```text
npm --prefix frontend test -- src/api/ai.test.ts src/components/AiExplanationDisplay.test.tsx src/components/AiRecommendationSections.test.tsx
```

Full local suite remains backlog item **513** (or project-equivalent full run item). **Do not treat
this catalog as a substitute for a green full-suite report** until that run is executed and attached
separately.

## Related Documentation

- [AI Feature Documentation](ai-features.md)
- [AI Limitations and Human Approval Policy](ai-limitations-and-human-approval.md)
- [AI Decision-Support Explanation](ai-decision-support-explanation.md)
- [Eligibility Rules Documentation](../architecture/eligibility-rules.md)
- [Role-Based Access Documentation](../architecture/role-based-access.md)

## Implementation Evidence

| Area | Location |
| --- | --- |
| This catalog | `docs/modules/ai-test-evidence.md` (item **509**) |
| Backend tests package | `backend/src/test/java/com/bayerwestphalian/campaign/ai/` |
| Frontend AI API tests | `frontend/src/api/ai.test.ts` |
| Frontend AI UI tests | `AiExplanationDisplay.test.tsx`, `AiRecommendationSections.test.tsx` |
| Documentation test | `AiTestEvidenceDocumentationTests` |
