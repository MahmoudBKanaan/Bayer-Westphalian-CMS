# AI Limitations and Human Approval Policy

This policy defines what AI-assisted features in the Bayer-Westphalian Campaign Management Platform
**may** and **must not** do (KB epic **E21**, **COMP-005**, items **468**, **507**, **512**).

AI is **decision-support only**. It must support human decision-making and must **never** override
compliance approval, consent, opt-out, do-not-contact, eligibility, or human approval rules.

Module implementation context: [AI Feature Documentation](ai-features.md) (item **506**).

Primary implementation package:

```text
com.bayerwestphalian.campaign.ai
```

## Policy Statement (KB)

| Source | Statement |
| --- | --- |
| **COMP-005** | AI suggestions require human review |
| **AI-001–AI-006** preface | AI must support human decision-making only; it must not automatically make final legal, financial, or marketing decisions without human approval |
| Item **468** | Implement AI-assisted decision support without allowing AI to bypass compliance, consent, or human approval |
| Item **482** / **500** | Campaign copy suggestion requires human approval |
| Item **501–504** | AI cannot approve campaign, override consent, bypass do-not-contact, or bypass `EligibilityService` |
| Item **512** | AI must never override compliance approval, consent, opt-out, do-not-contact, eligibility, or human approval rules |
| Item **507** | This limitations and human approval policy document |

## What AI May Do

AI features **may**:

1. **Search and rank** customers with fuzzy/weighted scores and explain factors (AI-001 / FR-015).
2. **Suggest** reusable segment ideas without creating final campaign audiences (AI-002).
3. **Recommend** products based on profile and ownership gaps (AI-003).
4. **Score** default risk from payment and reminder history as advisory output (AI-004).
5. **Draft** campaign subject/body/call-to-action text for human review (AI-005).
6. **Warn** about possible duplicate or excessive contact risk (AI-006 / BR-010 / BR-011).
7. **Persist** suggestions to `ai_recommendations` with required **explanation** and optional
   confidence for auditability (items 483–484, 505).
8. Record that a **human** reviewed/approved a stored suggestion (`approve(User)`).

## What AI Must Not Do (Hard Limitations)

| Limitation | Why | Authoritative system instead |
| --- | --- | --- |
| **Cannot approve, reject, submit, launch, pause, complete, or archive campaigns** | Campaign lifecycle is compliance-controlled (BR-005, BR-032) | `CampaignService` + Compliance Officer / Campaign Manager workflows |
| **Cannot override or invent consent** | Consent is legal evidence (COMP-001, FR-034) | `ConsentService` / consent records |

Sprint 16 critical item **661** (*AI recommendation cannot bypass consent rules*):

- Restates COMP-005 / items 502–504 / NFR-002 / FR-034 as a release gate.
- Primary suite: `AiRecommendationCannotBypassConsentRulesTests` (companion:
  `AiSupportsHumanDecisionMakingOnlyTests` item 512).
- Frontend catalog: `frontend/src/features/ai/aiRecommendationCannotBypassConsentRules.ts`.
- Asserts AI package has no `ConsentService` / `EligibilityService` ownership, no consent-mutation
  API, recommendations never clear DNC or invent consent, and eligibility remains authoritative for
  marketing inclusion.
| **Cannot ignore marketing opt-out** | BR-002 must be enforced immediately | Eligibility + consent checks |
| **Cannot bypass do-not-contact** | BR-001 / COMP-003 override all marketing logic | Customer `doNotContact` + `EligibilityService` |
| **Cannot bypass `EligibilityService`** | Recipient preview and launch re-check eligibility (BR-006, BR-007) | Segment preview + campaign launch paths |
| **Cannot auto-apply campaign copy to a live campaign message** | AI-005 / COMP-005 require human approval | `CampaignCopyService.approveCampaignCopy` by Campaign Manager only |
| **Cannot self-approve recommendations** | Human review is mandatory | Authenticated user id via `AuthorizationExpressions.currentUserId()` |
| **Cannot send marketing messages** | Sending is a controlled operational action | `CommunicationService` after approved launch |
| **Cannot disable users, change roles, or alter audit immutability** | Outside AI scope | User management + audit packages |
| **Cannot replace monthly contact-limit or duplicate-campaign enforcement** | BR-010 / BR-011 are rules at eligibility/launch | Warnings (AI-006) are advisory; launch still enforces |

Acceptance themes: items **501–504** (AI cannot approve campaign / override consent / bypass DNC /
bypass EligibilityService).

## Human Approval Policy

### Scope of mandatory human review

| AI output type | Human review required before operational use? | Approval mechanism in MVP |
| --- | --- | --- |
| Customer search hits | Yes (operator chooses who to open/act on) | Implicit: no auto-contact from search |
| Segment suggestions | Yes before saving/using in a campaign | Campaign Manager segment CRUD |
| Product recommendations | Yes before product assignment or campaign product selection | Product/campaign operator actions |
| Default-risk scores | Yes before using in outreach decisions | Operator judgment; not an auto-block |
| Duplicate-contact warnings | Yes before ignoring or acting | Operator judgment; launch still enforces |
| **Campaign copy** | **Always yes** before use | Explicit `POST /api/ai/campaign-copy/{id}/approve` |

### Campaign copy approval rules (AI-005)

1. Every `CampaignCopySuggestionView` forces **`requiresHumanApproval = true`** (DTO compact
   constructor).
2. Generated copy is stored as `AiRecommendationType.COPY` with a **non-blank explanation**.
3. New copy rows start **unapproved** (`approved_by_user_id` is null).
4. Only roles allowed to manage campaigns (MVP: **Campaign Manager**, plus Admin where granted)
   may call `approveCampaignCopy`.
5. Approval sets the **authenticated human** as approver; optional review notes may be stored for
   audit context.
6. AI services **must not** mark copy approved without a human principal.
7. Approval of a stored recommendation **records human review**; it does not itself grant campaign
   compliance approval or launch rights.

Sprint 16 critical item **662** (*AI-generated campaign copy requires human approval*):

- Primary suite: `AiGeneratedCampaignCopyRequiresHumanApprovalTests` (companions:
  `CampaignCopyServiceTests` items 480/500–501, `AiSupportsHumanDecisionMakingOnlyTests`).
- Frontend catalog: `frontend/src/features/ai/aiGeneratedCampaignCopyRequiresHumanApproval.ts`.
- Asserts forced `requiresHumanApproval`, unapproved `COPY` storage, human-only approve path, and
  that copy approval never changes campaign lifecycle status.

### Stored recommendation integrity

| Rule | Implementation expectation |
| --- | --- |
| Explanation required | Entity + services reject blank explanations |
| Confidence optional | 0–100 scale when present; never substitutes for human judgment |
| Approver is human user FK | `approved_by_user_id` → `users`; cleared on reject |
| Types separated | Only `COPY` uses the campaign-copy approve endpoint |

## Decision Authority Matrix

| Decision | AI role | Human / system role |
| --- | --- | --- |
| Who appears in search results | Ranks candidates | Operator selects and acts |
| Which segment definition to save | Suggests criteria text | Campaign Manager creates segment |
| Which product to promote | Suggests products | Product/Campaign Manager decides |
| Whether customer is contactable | May surface DNC flag / warnings | **EligibilityService** decides at preview/launch |
| Whether campaign may launch | None | **Compliance approval** + Campaign Manager launch |
| Whether copy is ready to use | Draft only | Human approve (AI-005) |
| Whether marketing is lawful | None | Consent + DNC + eligibility rules |

## Operator Obligations

Operators using AI features must:

1. Read **explanations** and score factors before acting on suggestions.
2. Treat risk scores and duplicate warnings as **alerts**, not automated blocks or permissions.
3. Never assume a high AI score implies valid consent or eligibility.
4. Keep final campaign content under human ownership after copy approval.
5. Escalate compliance concerns to Compliance Officers; AI cannot resolve them.

## Engineering Obligations

Implementers must:

1. Keep AI packages free of campaign lifecycle transitions (approve/reject/launch).
2. Keep AI packages free of consent mutation and eligibility overrides.
3. Persist explanations with stored recommendations.
4. Enforce method-level authorization on AI endpoints.
5. Ensure frontend never presents AI output as “auto-applied” without confirmation UI (items 491–492).
6. Document limitations next to features (this document + [AI Feature Documentation](ai-features.md)).

## Explicit Non-Goals

The following are **out of scope** for MVP AI:

- Fully automated campaign creation and launch
- Autonomous compliance decisions
- Legal advice or regulatory certification
- Guaranteed prediction accuracy for conversion or default
- Replacement of PostgreSQL/business-rule eligibility with model inference alone

Mock providers and rule-based heuristics are allowed; core compliance logic must remain real
(KB production-readiness rule: core production logic must not be simulated).

## Related Documentation

- [AI Feature Documentation](ai-features.md) — APIs, features, package map (item 506)
- [Eligibility Rules Documentation](../architecture/eligibility-rules.md) — contact gates AI cannot bypass
- [Consent Module Documentation](consent-module.md) — consent / opt-out truth
- [Campaign Lifecycle Documentation](campaign-lifecycle.md) — human campaign approval
- [Compliance Review Documentation](compliance-review.md) — compliance officer workflow
- [Role-Based Access Documentation](../architecture/role-based-access.md) — who may call AI endpoints
- [AI Decision-Support Explanation](ai-decision-support-explanation.md) — how to present explanations to operators (item 508)
- [AI Test Evidence](ai-test-evidence.md) — mapped automated tests for COMP-005 themes (item 509)

## Implementation Evidence

| Area | Location |
| --- | --- |
| Policy (this doc) | `docs/modules/ai-limitations-and-human-approval.md` (item **507**) |
| Feature overview | `docs/modules/ai-features.md` (item 506) |
| Copy always requires approval | `CampaignCopySuggestionView` (`requiresHumanApproval = true`) |
| Human approve endpoint | `CampaignCopyService.approveCampaignCopy`, `AiController` |
| Entity approve/reject | `AiRecommendation.approve` / `reject` |
| Acceptance themes | Items 500–504 service/controller tests under `backend/.../ai/` |
| Production gate **512** | `AiSupportsHumanDecisionMakingOnlyTests` |
| Test evidence catalog | [AI Test Evidence](ai-test-evidence.md) (item **509**) |
| Documentation tests | `AiLimitationsAndHumanApprovalPolicyDocumentationTests` |
