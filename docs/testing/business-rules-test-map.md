# Business Rules Test Map

**Backlog item 621 / item **621****: Map tests to KB business rules.

This document maps knowledge-base **Business Rules (BR-xxx)** to primary automated
evidence in the Bayer-Westphalian Campaign Management Platform repository.

Code catalog (locked by Vitest):  
`frontend/src/features/testing/businessRulesTestMap.ts`

> Mapping and documentation only for this item — **do not run any tests** as
> part of item 621 delivery. Suite execution is covered by later Sprint 16 run
> items and the full-suite gate pattern (e.g. **617** / **623+**).

Companion map: [Functional Requirements Test Map](functional-requirements-test-map.md) (item **620**).

## Purpose

Provide business-rule-to-test traceability so Sprint 16 QA can:

1. Prove each KB business rule has identifiable automated coverage.
2. Cross-walk rules to **critical tests** (**647–665**) and the production gate (**674**).
3. Separate compliance/eligibility enforcement evidence from pure feature (FR) coverage.

## Scope

| In scope | Out of scope (other backlog items) |
| --- | --- |
| BR-001–BR-007, BR-010–BR-014, BR-020–BR-024, BR-030–BR-034 | Functional requirements map (**620**) |
| Primary backend JUnit + frontend/Playwright anchors | Non-functional requirements map (**622**) |
| Critical-test item crosswalk where applicable | Executing the full suite (**623–642**, **617**) |
| Representative *primary* anchors | Exhaustive listing of every related test method |

## How to read this map

| Column | Meaning |
| --- | --- |
| **ID** | KB business rule ID |
| **Rule** | Short KB statement |
| **Backend tests** | Primary JUnit class simple names |
| **Frontend tests** | Paths under `frontend/` (UI surfaces gates, reasons, badges) |
| **Docs** | Architecture / module documentation |
| **Critical item** | Sprint 16 critical-test backlog item when the rule is restated there |

Business rules are **primarily backend-enforced**. Frontend tests prove operators can see
exclusion reasons, status transitions, and validation — they do not replace server checks.

## Eligibility and consent (BR-001–BR-004)

| ID | Rule | Backend tests | Frontend tests | Docs | Critical |
| --- | --- | --- | --- | --- | --- |
| BR-001 | `do_not_contact = true` must never be included in a campaign | **`CustomerWithDoNotContactIsExcludedTests`**, `EligibilityServiceTests`, `DoNotContactChangeCreatesAuditLogTests` | customerWithDoNotContactIsExcluded, exclusionReasons, ExclusionReasonSummaryPanel | eligibility-rules, segmentation-module | **648** |
| BR-002 | Marketing opt-out must be excluded from marketing | `EligibilityServiceTests`, `ConsentServiceTests`, `OptOutChangeCreatesAuditLogTests`, related cases in **`CustomerWithoutValidConsentIsExcludedTests`** | consentUpdateFlow, exclusionReasons, customerWithoutValidConsentIsExcluded | eligibility-rules, consent-module | related to **649** (opt-out path); primary **649** is `INVALID_CONSENT` / FR-034 |
| BR-003 | Guardian consent required before contacting applicable beneficiaries | **`MinorBeneficiaryWithoutGuardianConsentIsExcludedTests`**, `EligibilityServiceTests`, `BeneficiaryServiceTests` | minorBeneficiaryWithoutGuardianConsentIsExcluded, exclusionReasons, CustomerDetailsPage | eligibility-rules, beneficiary-module | **650** |
| BR-004 | Consent must include type, purpose, source, date, and status | `ConsentRecordTests`, `ConsentDtoTests`, `ConsentServiceTests`, **`CustomerWithoutValidConsentIsExcludedTests`** (validity gate) | consentUpdateFlow, customerWithoutValidConsentIsExcluded | consent-module, ui-consent-update, eligibility-rules | **649** (valid consent required) |

## Campaign compliance and exclusions (BR-005–BR-007)

| ID | Rule | Backend tests | Frontend tests | Docs | Critical |
| --- | --- | --- | --- | --- | --- |
| BR-005 | Campaigns cannot launch before Compliance Officer approval | **`CampaignCannotLaunchWithoutApprovalTests`**, `CampaignServiceTests`, launch audit tests | campaignCannotLaunchWithoutApproval, campaignLaunchFlow + integration + e2e | campaign-launch, compliance-review | **647** |
| BR-006 | Campaigns must show recipient eligibility reasons | `EligibilityServiceTests`, `EligibilityResponseTests`, recipient + exclusion summary tests | recipientPreviewClarity, ExclusionReasonSummaryPanel | recipient-preview, eligibility-rules | — |
| BR-007 | Campaigns must record excluded contacts and exclusion reasons | `CampaignRecipientServiceTests`, recipient repository/entity tests | exclusionReasons, recipientPreviewClarity | recipient-preview, campaign-launch | — |

## Contact frequency and audience hygiene (BR-010–BR-014)

| ID | Rule | Backend tests | Frontend tests | Docs | Critical |
| --- | --- | --- | --- | --- | --- |
| BR-010 | Same customer cannot receive the same campaign twice | **`SameCustomerCannotBeDuplicatedInSameCampaignTests`**, `EligibilityServiceTests`, `CampaignRecipientServiceTests` | sameCustomerCannotBeDuplicatedInSameCampaign, exclusionReasons, recipientPreviewClarity | eligibility-rules, recipient-preview | **651** |
| BR-011 | Customer cannot exceed configured monthly marketing message limit | **`CustomerCannotExceedMonthlyContactLimitTests`**, `ConfigurableMonthlyContactLimitTests`, `EligibilityServiceTests` | customerCannotExceedMonthlyContactLimit, SystemSettingsPage | eligibility-rules, system-settings | **652** |
| BR-012 | Failed sends can be retried maximum 3 times | `ConfigurableSendRetryLimitTests`, `SendRetryServiceTests` | contactEvents API | communication-tracking, system-settings | — |
| BR-013 | Uninterested customers excluded for a configurable period | `ConfigurableUninterestedExclusionPeriodTests`, `EligibilityServiceTests`, `CustomerStatusChangedAtTests` | exclusionReasons, SystemSettingsPage | eligibility-rules, system-settings | — |
| BR-014 | Converted customers should not receive the same campaign again | `EligibilityServiceTests`, eligibility documentation tests | exclusionReasons, CustomerStatusBadge | eligibility-rules | — |

## Reminders and payments (BR-020–BR-024)

| ID | Rule | Backend tests | Frontend tests | Docs | Critical |
| --- | --- | --- | --- | --- | --- |
| BR-020 | Green reminder is the first reminder | `GreenReminderIsFirstReminderTests`, `PaymentReminderLevelRulesTests` | ReminderLevelBadge, RemindersPage | green-yellow-red-reminder-rules | — |
| BR-021 | Yellow reminder is the second reminder | `YellowReminderIsSecondReminderTests`, level rules tests | ReminderLevelBadge | green-yellow-red-reminder-rules | — |
| BR-022 | Red reminder is the third and indicates likely default risk | `RedReminderIsThirdReminderTests`, AI recommendation tests | ReminderLevelBadge, AiRecommendationSections | green-yellow-red-reminder-rules, ai-features | — |
| BR-023 | Product-expiration campaign can start 3, 6, or 12 months before expiration | 3/6/12 month generation tests, `ProductExpirationReminderRulesTests` | reminders API, RemindersPage | reminder-scheduling, segmentation-module | — |
| BR-024 | Payment reminder must not be sent if payment is completed | **`PaymentReminderIsNotSentIfPaymentIsCompletedTests`** (item **660**), `PaymentReminderNotSentIfPaymentCompletedTests` (+ API), reminder logic suite | paymentReminderIsNotSentIfPaymentIsCompleted, reminders + paymentRecords API | reminder-scheduling, payment-records | **660** |

## Campaign lifecycle constraints (BR-030–BR-033)

| ID | Rule | Backend tests | Frontend tests | Docs | Critical |
| --- | --- | --- | --- | --- | --- |
| BR-030 | Campaign must have name, objective, target segment, product, message, schedule, owner | `CampaignServiceTests`, submit/DTO/controller tests | campaignFormValidation, campaignCreationFlow, CampaignBuilder | campaign-lifecycle, ui-campaign-creation | — |
| BR-031 | Draft campaign can be edited | draft update endpoint + create draft tests | campaignCreationFlow, campaignBuilderFlow | campaign-lifecycle | — |
| BR-032 | Submitted campaign cannot be launched before approval | **`CampaignCannotLaunchWithoutApprovalTests`**, `CampaignServiceTests`, submit/approve tests | campaignCannotLaunchWithoutApproval, campaignLaunchFlow | campaign-lifecycle, campaign-launch | **647** |
| BR-033 | Approved campaign can be launched, paused, completed, or archived | `CampaignServiceTests`, status + integration tests | campaignLaunchFlow, CampaignsPage, CampaignStatusBadge | campaign-lifecycle, campaign-launch | — |

## Metrics integrity (BR-034)

| ID | Rule | Backend tests | Frontend tests | Docs | Critical |
| --- | --- | --- | --- | --- | --- |
| BR-034 | Campaign metrics update after contact events | **`ContactEventsUpdateAnalyticsTests`**, `EngagementCountsUpdateFromContactEventsTests`, sent-count after launch, analytics + report traceability | contactEventsUpdateAnalytics, dashboardAnalyticsFlow, dashboard e2e | communication-tracking, analytics-module, kpi-definitions | **656** |

## Critical test crosswalk (items 647–665)

Sprint 16 **critical tests** restate many business rules as release blockers. Primary links:

| Critical item | Statement (short) | Primary BR |
| --- | --- | --- |
| **647** | Campaign cannot launch without approval | BR-005, BR-032 |
| **648** | Customer with `do_not_contact` is excluded | BR-001 (`CustomerWithDoNotContactIsExcludedTests`) |
| **649** | Customer without valid consent is excluded | FR-034 / `INVALID_CONSENT` (**`CustomerWithoutValidConsentIsExcludedTests`**); related BR-002 opt-out + BR-004 validity |
| **650** | Minor beneficiary without guardian consent is excluded | BR-003 (`MinorBeneficiaryWithoutGuardianConsentIsExcludedTests`; `INVALID_CONSENT`) |
| **651** | Same customer cannot be duplicated in same campaign | BR-010 (`SameCustomerCannotBeDuplicatedInSameCampaignTests`; `DUPLICATE_CAMPAIGN_RECIPIENT`) |
| **652** | Customer cannot exceed monthly contact limit | BR-011 (`CustomerCannotExceedMonthlyContactLimitTests`; `MONTHLY_CONTACT_LIMIT`) |
| **656** | Contact events update analytics | BR-034 (`ContactEventsUpdateAnalyticsTests`; contact event → metrics → dashboard) |
| **660** | Payment reminder is not sent if payment is completed | BR-024 — `PaymentReminderIsNotSentIfPaymentIsCompletedTests` |

| **653** | Product Manager cannot launch campaigns | TC-013 / FR-060 (`ProductManagerCannotLaunchCampaignsTests`; `@authz.canManageCampaigns()`) |
| **654** | BI Analyst cannot edit customers | TC-009 / FR-010+FR-012 (`BiAnalystCannotEditCustomersTests`; PUT customers editors only) |
| **655** | Compliance Officer can approve/reject campaigns | TC-011 / FR-059 / BR-005 (`ComplianceOfficerCanApproveRejectCampaignsTests`) |

| **657** | Soft-deleted customers do not appear in active lists | FR-010+FR-013 (`SoftDeletedCustomersDoNotAppearInActiveListsTests`) |

Critical production-security and ops items through **666** are mapped on the FR / NFR maps (**620** / **622**) and production gate (**674**). Item **658** (*Audit log is created after consent change*) is covered under **NFR-008** / **FR-033** via `AuditLogIsCreatedAfterConsentChangeTests`. Item **659** (*Disabled user cannot log in*) is covered under **NFR-001** / **FR-001** via `DisabledUserCannotLogInTests`. Item **661** (*AI recommendation cannot bypass consent rules*) is covered under **NFR-002** / **FR-034** / **COMP-005** via `AiRecommendationCannotBypassConsentRulesTests`. Item **662** (*AI-generated campaign copy requires human approval*) is covered under **AI-005** / **COMP-005** via `AiGeneratedCampaignCopyRequiresHumanApprovalTests`. Item **663** (*Report export is restricted to authorized roles*) is covered under **NFR-001** / **FR-109–FR-110** via `ReportExportIsRestrictedToAuthorizedRolesTests`. Item **664** (*Production profile hides stack traces*) is covered under **NFR-001** / **NFR-014** via `ProductionProfileHidesStackTracesTests`. Item **665** (*Missing secrets are detected*) is covered under **NFR-001** via `MissingSecretsAreDetectedTests`. Item **666** (*Backup and restore process is documented and testable*) is covered under **NFR-013** via `BackupAndRestoreProcessIsDocumentedAndTestableTests`.

### Eligibility gate rule set

Rules that participate in contactability evaluation before marketing:

```text
BR-001, BR-002, BR-003, BR-010, BR-011, BR-013, BR-014
```

Primary engine: `EligibilityService` / `EligibilityServiceTests` (see [eligibility-rules.md](../architecture/eligibility-rules.md)).

### Launch gate rule set

```text
BR-005, BR-032
```

Submitted campaigns cannot go live until Compliance Officer approval.

## Coverage summary

| Domain | IDs | Count |
| --- | --- | --- |
| Eligibility and consent | BR-001–BR-004 | 4 |
| Campaign compliance / exclusions | BR-005–BR-007 | 3 |
| Contact frequency / audience hygiene | BR-010–BR-014 | 5 |
| Reminders and payments | BR-020–BR-024 | 5 |
| Campaign lifecycle constraints | BR-030–BR-033 | 4 |
| Metrics integrity | BR-034 | 1 |
| **Total** | | **22** |

## Related Sprint 16 items

| Item | Topic |
| --- | --- |
| **619** | Master test plan |
| **620** | Map tests to KB functional requirements |
| **621** | This map (business rules) |
| **622** | [Non-Functional Requirements Test Map](non-functional-requirements-test-map.md) |
| **647–665** | Critical tests |
| **670** | Requirement-to-test traceability matrix |
| **674** | No RC if critical business-rule / security / consent / auth / audit / approval tests fail |
| **617** / **623+** | Run suites and fix failures |

## Acceptance (item 621)

Item **621** is complete when:

1. This document exists and states the backlog goal in KB language.
2. Every KB BR-xxx ID appears with mapped backend + frontend + docs anchors.
3. A code catalog (`businessRulesTestMap.ts`) locks IDs, domains, critical-item links, and evidence.
4. Unit tests assert catalog completeness, doc snippets, and `docs/README.md` linkage.
5. The map does not claim suite execution; wording notes **do not run any tests** for this item.

Catalog path: `frontend/src/features/testing/businessRulesTestMap.ts`  
Backend documentation test: `backend/src/test/java/.../support/BusinessRulesTestMapDocumentationTests.java`
