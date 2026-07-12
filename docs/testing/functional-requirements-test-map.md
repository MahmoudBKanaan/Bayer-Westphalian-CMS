# Functional Requirements Test Map

**Backlog item 620 / item **620****: Map tests to KB functional requirements.

This document maps knowledge-base **Functional Requirements (FR-xxx)** and
**AI-Assisted Functional Requirements (AI-xxx)** to primary automated evidence
already present in the Bayer-Westphalian Campaign Management Platform repository.

Code catalog (locked by Vitest):  
`frontend/src/features/testing/functionalRequirementsTestMap.ts`

> Mapping and documentation only for this item — **do not run any tests** as
> part of item 620 delivery. Suite execution is covered by later Sprint 16 run
> items and the full-suite gate pattern (e.g. **617** / **623+**).

## Purpose

Provide requirement-to-test traceability so Sprint 16 QA can:

1. Prove each KB functional requirement has identifiable automated coverage.
2. Feed the requirement-to-test matrix (**670**) and critical tests (**647–665**).
3. Avoid ad-hoc hunting for “which test covers FR-0xx?”

## Scope

| In scope | Out of scope (other backlog items) |
| --- | --- |
| FR-001–FR-005, FR-010–FR-020, FR-030–FR-034, FR-040–FR-046, FR-050–FR-062, FR-070–FR-097, FR-100–FR-110 | Business rules map (**621**) |
| AI-001–AI-006 | Non-functional requirements map (**622**) |
| Primary backend JUnit class names + frontend/Playwright paths | Executing the full suite (**623–642**, **617**) |
| Representative *primary* anchors per requirement | Exhaustive listing of every related test method |

## How to read this map

Each requirement lists:

| Column | Meaning |
| --- | --- |
| **ID** | KB functional / AI requirement ID |
| **Requirement** | Short KB statement |
| **Backend tests** | Primary JUnit class simple names under `backend/src/test/java/.../campaign/` |
| **Frontend tests** | Paths under `frontend/` (unit, integration, or Playwright) |
| **Docs** | Module or UI acceptance documentation |

Evidence types are complementary: backend for domain rules and APIs; frontend for UI contracts and E2E journeys.

## Auth and RBAC (FR-001–FR-005)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-001 | Users can log in with email and password | **`DisabledUserCannotLogInTests`** (item **659**: non-ACTIVE blocked), `AuthControllerTests`, `AuthServiceTests`, `JwtServiceTests` | disabledUserCannotLogIn, `src/features/auth/loginFlow.test.ts`, login integration + e2e | authentication-design, ui-login-flow, user-management-guide |
| FR-002 | Users have assigned roles | `UserRoleTests`, `RoleRepositoryTests`, `UserServiceTests` | `src/features/auth/permissions.test.ts`, `src/auth/AuthProvider.test.tsx` | role-based-access |
| FR-003 | Pages and APIs are restricted by role | `ProtectedEndpointSecurityTests`, `MethodAuthorizationAnnotationTests`, `AuthorizationExpressionsTests` | `src/features/auth/roleBasedMenu.test.ts`, role-based-menu integration + e2e | role-based-access, ui-role-based-menu |
| FR-004 | Users can log out securely | `AuthControllerTests`, `AuthServiceTests` | `src/auth/AuthProvider.test.tsx`, `sessionStorageStrategy.test.ts` | authentication-design |
| FR-005 | Admin can create, update, disable users | `UserControllerTests`, `UserServiceTests`, user audit tests; disable effect on login: **`DisabledUserCannotLogInTests`** (**659**) | UsersPage, users API, disabledUserCannotLogIn | user-management-guide, authentication-design |

## Customer and prospect (FR-010–FR-020)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-010 | Users can view paginated customers/prospects | **`SoftDeletedCustomersDoNotAppearInActiveListsTests`** (item **657**), **`BiAnalystCannotEditCustomersTests`** (item **654**), customer list tests | softDeletedCustomersDoNotAppearInActiveLists, CustomersPage | customer-module |
| FR-011 | Authorized users can create customers/prospects | `CustomerControllerTests`, `CustomerServiceTests` | customerCreationFlow + integration + e2e | customer-module, ui-customer-creation |
| FR-012 | Authorized users can edit customer details | **`BiAnalystCannotEditCustomersTests`** (item **654** / TC-009: BI blocked), `CustomerServiceTests` | biAnalystCannotEditCustomers, CustomerDetailsPage | customer-module, bi-analyst-guide |
| FR-013 | Authorized users can soft-delete customers/prospects | **`SoftDeletedCustomersDoNotAppearInActiveListsTests`** (item **657**), soft-delete audit tests | softDeletedCustomersDoNotAppearInActiveLists | customer-module |
| FR-014 | Users can search customers | `CustomerControllerTests`, `CustomerServiceTests` | `CustomersPage.test.tsx` | customer-module |
| FR-015 | System supports innovative/fuzzy customer search | `AiSearchServiceTests`, `AiControllerTests` | `src/api/ai.test.ts` | ai-features |
| FR-016 | Users can view customer profiles | `CustomerControllerTests`, `CustomerServiceTests` | `CustomerDetailsPage.test.tsx` | customer-module |
| FR-017 | Users can view contact history | `ContactEventRepositoryTests`, `CampaignControllerTests` | `ContactHistoryPage.test.tsx` | communication-tracking |
| FR-018 | Users can view consent/opt-out status | `ConsentControllerTests`, `ConsentServiceTests` | Customer details + ConsentStatusBadge | consent-module |
| FR-019 | Users can mark customer status | `CustomerServiceTests`, `CustomerStatusChangedAtTests` | Customer details + CustomerStatusBadge | customer-module |
| FR-020 | Users can import customers/prospects from CSV | `CustomerControllerTests`, `CustomerCsvImportDocumentationTests` | `CustomersPage.test.tsx` | customer-csv-import-guide |

## Beneficiary and consent (FR-030–FR-034)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-030 | Users can add beneficiaries linked to customers | `BeneficiaryControllerTests`, `BeneficiaryServiceTests` | Customer details, `beneficiaries.test.ts` | beneficiary-module |
| FR-031 | Users can store beneficiary contact details | `BeneficiaryServiceTests`, `BeneficiaryDtoTests` | `beneficiaries.test.ts` | beneficiary-module |
| FR-032 | System tracks guardian consent requirement | **`MinorBeneficiaryWithoutGuardianConsentIsExcludedTests`** (item **650**), `EligibilityServiceTests`, `BeneficiaryServiceTests` | minorBeneficiaryWithoutGuardianConsentIsExcluded, Customer details | eligibility-rules, beneficiary-module |
| FR-033 | Users can record consent status | **`AuditLogIsCreatedAfterConsentChangeTests`** (item **658**), `ConsentControllerTests`, `ConsentServiceTests`, `ConsentChangeCreatesAuditLogTests` | auditLogIsCreatedAfterConsentChange, consentUpdateFlow + integration + e2e | consent-module, ui-consent-update, audit-logging |
| FR-034 | System blocks marketing without valid consent | **`CustomerWithoutValidConsentIsExcludedTests`** (item **649**), **`AiRecommendationCannotBypassConsentRulesTests`** (item **661**), `EligibilityServiceTests`, segment/reminder consent tests | customerWithoutValidConsentIsExcluded, aiRecommendationCannotBypassConsentRules, exclusionReasons | eligibility-rules, consent-module, ai-limitations-and-human-approval |

## Product catalog (FR-040–FR-046)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-040 | Users can view products | `ProductControllerTests`, `ProductServiceTests` | `ProductsPage.test.tsx` | product-module |
| FR-041 | Product Manager/Admin can create products | `ProductManagerCreateProductTests`, `UnauthorizedCreateProductTests` | productCreationFlow + integration + e2e | product-module, ui-product-creation |
| FR-042 | Product Manager/Admin can edit products | `ProductManagerEditProductTests`, `ProductServiceTests` | `ProductDetailsPage.test.tsx` | product-module |
| FR-043 | Product Manager/Admin can disable/delete products | `ProductManagerDisableProductTests`, `ProductDisableSoftDeleteEndpointTests` | `ProductsPage.test.tsx` | product-module |
| FR-044 | Users can search products | `ProductSearchEndpointTests`, `ProductSearchAndFilterUiTests` | productSearch + ProductSearchFilters | product-module |
| FR-045 | Product Manager can create product-change requests | `ProductChangeRequestCanBeCreatedAndTrackedTests` | ProductChangeRequests page + API tests | product-module |
| FR-046 | Products can be assigned to campaigns | `CampaignProductSelectionTests` | campaignCreationFlow, CampaignBuilder | campaign-lifecycle |

## Campaign lifecycle (FR-050–FR-062)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-050 | Campaign Manager can create campaigns | `CampaignCanBeCreatedTests`, draft create tests | campaignCreationFlow + integration + e2e | campaign-lifecycle, ui-campaign-creation |
| FR-051 | Campaign Manager can define campaign objective | `CampaignServiceTests`, `CampaignDtoTests` | campaignFormValidation, CampaignBuilder | campaign-lifecycle |
| FR-052 | Campaign Manager can select promoted products | `CampaignProductSelectionTests` | campaignBuilderFlow | campaign-lifecycle |
| FR-053 | Campaign Manager can define target audience | `CampaignSegmentSelectionTests` | campaignBuilderFlow | campaign-lifecycle |
| FR-054 | System previews eligible recipients | `CampaignRecipientServiceTests`, `RecipientPreviewDocumentationTests` | recipientPreviewClarity, CampaignRecipientPreviewPage | recipient-preview |
| FR-055 | System excludes opt-outs and invalid consent | `EligibilityServiceTests`, segmentation eligibility gate tests | exclusionReasons, ExclusionReasonSummaryPanel | eligibility-rules |
| FR-056 | System prevents duplicate/excessive marketing | **`SameCustomerCannotBeDuplicatedInSameCampaignTests`** (item **651**), **`CustomerCannotExceedMonthlyContactLimitTests`** (item **652**), `EligibilityServiceTests` | sameCustomerCannotBeDuplicatedInSameCampaign, customerCannotExceedMonthlyContactLimit | eligibility-rules, recipient-preview |
| FR-057 | Campaign Manager can save campaign as draft | `CampaignDraftCanBeUpdatedTests`, draft create tests | campaignCreationFlow | campaign-lifecycle |
| FR-058 | Campaign Manager can submit campaign for review | `CampaignCanBeSubmittedTests`, submission audit tests | campaignCreationFlow | campaign-lifecycle, compliance-review |
| FR-059 | Compliance Officer can approve/reject campaign | **`CampaignCanBeApprovedTests`**, **`ComplianceOfficerCanApproveRejectCampaignsTests`** (item **655** / TC-011), approve/reject + audit tests | complianceOfficerCanApproveRejectCampaigns, complianceApprovalFlow + e2e | compliance-review, ui-compliance-approval |
| FR-060 | Campaign Manager can launch approved campaign | **`CampaignCannotLaunchWithoutApprovalTests`** (item **647**), **`ProductManagerCannotLaunchCampaignsTests`** (item **653** / TC-013), launch audit, sent-count tests | campaignCannotLaunchWithoutApproval, productManagerCannotLaunchCampaigns, campaignLaunchFlow | campaign-launch, ui-campaign-launch |
| FR-061 | Campaign Manager can pause/cancel campaign | `CampaignServiceTests`, `CampaignStatusTests` | CampaignsPage, CampaignStatusBadge | campaign-lifecycle |
| FR-062 | Campaign Manager can archive completed campaign | `CampaignServiceTests`, `CampaignStatusTests` | CampaignsPage | campaign-lifecycle |

## Segmentation (FR-070–FR-079)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-070 | Users can segment by age group | `SegmentCanFilterByAgeGroupTests`, `SegmentFilterByAgeGroupWorksTests` | criteriaFields | segment-criteria-guide |
| FR-071 | Users can segment by location | `SegmentCanFilterByLocationTests`, `SegmentFilterByLocationWorksTests` | criteriaFields | segment-criteria-guide |
| FR-072 | Users can segment by customer/prospect type | `SegmentCanFilterByCustomerTypeTests` | criteriaFields | segment-criteria-guide |
| FR-073 | Users can segment by product ownership | `SegmentCanFilterByProductOwnershipTests` | criteriaFields | segment-criteria-guide |
| FR-074 | Users can segment by payment history | `SegmentCanFilterByPaymentHistoryTests` | criteriaFields | segment-criteria-guide |
| FR-075 | Users can segment by behavior/interests | `SegmentCanFilterByBehaviorStatusTests` | criteriaFields | segment-criteria-guide |
| FR-076 | Users can segment by product expiration | `SegmentCanFilterByProductExpirationTests` | criteriaFields | segment-criteria-guide |
| FR-077 | Users can save reusable segments | `SegmentCanBeCreatedTests`, reusable segment permission tests | segmentCreationFlow + integration + e2e | segmentation-module, ui-segment-creation |
| FR-078 | Users can combine criteria with AND/OR logic | `SegmentAndLogicReturnsCorrectResultTests`, `SegmentOrLogicReturnsCorrectResultTests` | SegmentCriteriaBuilder | segmentation-module |
| FR-079 | System previews audience size | `SegmentCanBePreviewedTests`, eligible/excluded count tests | SegmentPreviewResults, exclusionReasons | audience-preview-logic |

## Reminders and expiration campaigns (FR-080–FR-089)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-080 | System schedules payment reminders | **`PaymentReminderIsNotSentIfPaymentIsCompletedTests`** (item **660** / BR-024), `PaymentDueReminderIsGeneratedTests`, `ReminderServiceTests` | paymentReminderIsNotSentIfPaymentIsCompleted, RemindersPage, reminders API | reminder-scheduling, payment-records |
| FR-081 | System sends Green first reminder | `GreenReminderIsFirstReminderTests` | ReminderLevelBadge | green-yellow-red-reminder-rules |
| FR-082 | System sends Yellow second reminder | `YellowReminderIsSecondReminderTests` | ReminderLevelBadge | green-yellow-red-reminder-rules |
| FR-083 | System sends Red third reminder | `RedReminderIsThirdReminderTests` | ReminderLevelBadge | green-yellow-red-reminder-rules |
| FR-084 | System identifies likely payment default | `AiRecommendationServiceTests`, `AiControllerTests` | ai API + AiRecommendationSections | ai-features |
| FR-085 | Product-expiration campaigns 3 months before | `ProductExpirationReminderIsGenerated3MonthsTests` (+ API) | reminders API | reminder-scheduling |
| FR-086 | Product-expiration campaigns 6 months before | `ProductExpirationReminderIsGenerated6MonthsTests` (+ API) | reminders API | reminder-scheduling |
| FR-087 | Product-expiration campaigns 12 months before | `ProductExpirationReminderIsGenerated12MonthsTests` (+ API) | reminders API | reminder-scheduling |
| FR-088 | System creates follow-up tasks after reminders | `FollowUpServiceTests`, `FollowUpTaskCanBeAssignedTests` | FollowUpTasksPage | follow-up-tasks, reminder-scheduler |
| FR-089 | System logs all reminder attempts | `SchedulerLogsReminderAttemptsTests`, `ReminderProcessingSchedulerTests` | RemindersPage | reminder-scheduler |

## Communication and follow-up (FR-090–FR-097)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-090 | System records contact attempts | `ContactEventTests`, `CommunicationServiceTests`, engagement count tests | contactEvents API, ContactHistoryPage | communication-tracking |
| FR-091 | System shows contact timeline | `ContactEventRepositoryTests` | ContactHistoryPage | communication-tracking |
| FR-092 | System prevents excessive contact frequency | **`CustomerCannotExceedMonthlyContactLimitTests`** (item **652**), `ConfigurableMonthlyContactLimitTests`, AI contact-limit tests | customerCannotExceedMonthlyContactLimit, ai API | eligibility-rules, system-settings |
| FR-093 | Users can create follow-up tasks | `FollowUpControllerTests`, `FollowUpServiceTests` | FollowUpTasksPage | follow-up-tasks |
| FR-094 | Users can record contact outcomes | `ContactEventDtoTests`, `CommunicationServiceTests` | contactEvents API | communication-tracking |
| FR-095 | Users can add communication notes | `FollowUpServiceTests`, `ContactEventTests` | FollowUpTasksPage, ContactHistoryPage | communication-tracking, follow-up-tasks |
| FR-096 | Users can remove uninterested parties from mailing lists | `ConfigurableUninterestedExclusionPeriodTests`, eligibility + consent | consentUpdateFlow | consent-module, eligibility-rules |
| FR-097 | System respects do-not-contact status | `EligibilityServiceTests`, `DoNotContactChangeCreatesAuditLogTests` | exclusionReasons | eligibility-rules |

## Analytics and reports (FR-100–FR-110)

| ID | Requirement | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| FR-100 | Dashboard shows campaign totals | `DashboardEndpointTests`, `AnalyticsServiceTests` | dashboardAnalyticsFlow + integration + e2e | analytics-module, ui-dashboard-analytics |
| FR-101 | Dashboard shows active campaigns | `DashboardEndpointTests` | DashboardPage, dashboardCharts | analytics-module |
| FR-102 | Dashboard shows audience size | `AudienceSizeIsCalculatedCorrectlyTests`, `CalculateAudienceSizeTests` | dashboardAnalyticsFlow | kpi-definitions |
| FR-103 | Dashboard shows messages sent | **`ContactEventsUpdateAnalyticsTests`** (item **656**), sent-count tests | contactEventsUpdateAnalytics, dashboardAnalyticsFlow | kpi-definitions, communication-tracking |
| FR-104 | Dashboard shows open rate | **`ContactEventsUpdateAnalyticsTests`** (item **656**), open-rate tests | contactEventsUpdateAnalytics, dashboardCharts | kpi-definitions, communication-tracking |
| FR-105 | Dashboard shows click rate | **`ContactEventsUpdateAnalyticsTests`** (item **656**), click-rate tests | contactEventsUpdateAnalytics, dashboardCharts | kpi-definitions, communication-tracking |
| FR-106 | Dashboard shows conversion rate | **`ContactEventsUpdateAnalyticsTests`** (item **656**), conversion-rate tests | contactEventsUpdateAnalytics, dashboardCharts | kpi-definitions, communication-tracking |
| FR-107 | Dashboard shows estimated ROI | `RoiIsCalculatedCorrectlyTests`, `CalculateEstimatedRoiTests` | Dashboard + ExecutiveDashboard pages | kpi-definitions |
| FR-108 | Users can view performance charts | `CampaignAnalyticsEndpointTests`, `ProductPerformanceEndpointTests` | analyticsCharts, charts components, AnalyticsPage | analytics-module |
| FR-109 | Users can export CSV reports | **`ReportExportIsRestrictedToAuthorizedRolesTests`** (item **663**), `CsvExportWorksTests`, `CampaignCsvReportEndpointTests` | reportExportIsRestrictedToAuthorizedRoles, reportDownload, ReportDownloadPanel, ReportsPage | report-export |
| FR-110 | Users can generate PDF reports | **`ReportExportIsRestrictedToAuthorizedRolesTests`** (item **663**), `PdfExportWorksTests`, `CampaignPdfReportEndpointTests` | reportExportIsRestrictedToAuthorizedRoles, reportDownload, ReportDownloadPanel | report-export |

## AI-assisted features (AI-001–AI-006)

AI must support human decision-making only and must not automatically make final
legal, financial, or marketing decisions without human approval.

| ID | Feature | Backend tests | Frontend tests | Docs |
| --- | --- | --- | --- | --- |
| AI-001 | Innovative customer search | `AiSearchServiceTests`, `AiControllerTests`, `AiFeatureDocumentationTests` | `src/api/ai.test.ts` | ai-features, ai-test-evidence |
| AI-002 | Segment suggestions | `AiRecommendationServiceTests`, `AiControllerTests` | AiRecommendationSections, ai API | ai-features |
| AI-003 | Product recommendations | `AiRecommendationServiceTests`, `AiControllerTests`, **`AiRecommendationCannotBypassConsentRulesTests`** (**661**) | aiRecommendationCannotBypassConsentRules, AiRecommendationSections, ai API | ai-features, ai-limitations-and-human-approval |
| AI-004 | Default-risk score | `AiRecommendationServiceTests`, `AiDtoTests` | AiExplanationDisplay, ai API | ai-features, ai-decision-support-explanation |
| AI-005 | Campaign copy suggestion (human-approved) | **`AiGeneratedCampaignCopyRequiresHumanApprovalTests`** (item **662**), `CampaignCopyServiceTests`, `AiSupportsHumanDecisionMakingOnlyTests` | aiGeneratedCampaignCopyRequiresHumanApproval, AiRecommendationSections | ai-limitations-and-human-approval, ai-features |
| AI-006 | Duplicate-contact risk warning | `AiRecommendationServiceTests`, `ConfigurableMonthlyContactLimitAiTests` | ai API, AiExplanationDisplay | ai-features, ai-test-evidence |

## Coverage summary

| Domain | IDs | Count |
| --- | --- | --- |
| Auth and RBAC | FR-001–FR-005 | 5 |
| Customer and prospect | FR-010–FR-020 | 11 |
| Beneficiary and consent | FR-030–FR-034 | 5 |
| Product catalog | FR-040–FR-046 | 7 |
| Campaign lifecycle | FR-050–FR-062 | 13 |
| Segmentation | FR-070–FR-079 | 10 |
| Reminders / expiration | FR-080–FR-089 | 10 |
| Communication / follow-up | FR-090–FR-097 | 8 |
| Analytics / reports | FR-100–FR-110 | 11 |
| AI-assisted | AI-001–AI-006 | 6 |
| **Total** | | **86** |

### Happy-path FR chain (E2E)

Aligned with Playwright happy-path and UI acceptance items **598–606**:

```text
FR-001 → FR-011 → FR-033 → FR-041 → FR-077 → FR-050 → FR-059 → FR-060 → FR-100
```

(Login → create customer → consent → product → segment → campaign → approval → launch → dashboard.)

## Related Sprint 16 items

| Item | Topic |
| --- | --- |
| **619** | Master test plan |
| **620** | This map (functional requirements) |
| **621** | [Business Rules Test Map](business-rules-test-map.md) |
| **622** | [Non-Functional Requirements Test Map](non-functional-requirements-test-map.md) |
| **647–665** | Critical business-rule / security tests |
| **670** | Requirement-to-test traceability matrix |
| **617** / **623+** | Run suites and fix failures |

## Acceptance (item 620)

Item **620** is complete when:

1. This document exists and states the backlog goal in KB language.
2. Every KB FR-xxx and AI-xxx functional ID appears with mapped backend + frontend + docs anchors.
3. A code catalog (`functionalRequirementsTestMap.ts`) locks IDs, domains, and evidence.
4. Unit tests assert catalog completeness, doc snippets, and `docs/README.md` linkage.
5. The map does not claim suite execution; wording notes **do not run any tests** for this item.

Catalog path: `frontend/src/features/testing/functionalRequirementsTestMap.ts`  
Backend documentation test: `backend/src/test/java/.../support/FunctionalRequirementsTestMapDocumentationTests.java`
