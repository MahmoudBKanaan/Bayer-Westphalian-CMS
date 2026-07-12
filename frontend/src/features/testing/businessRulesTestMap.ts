/**
 * Sprint 16 item **621** — Map tests to KB business rules.
 *
 * Catalog of BR-xxx IDs from the knowledge base with primary automated evidence
 * (backend JUnit, frontend Vitest/Playwright, module docs). Suite execution
 * remains later Sprint 16 run items.
 */

/** Repo-relative path for the business rules test map document. */
export const BUSINESS_RULES_TEST_MAP_DOC_PATH = "docs/testing/business-rules-test-map.md";

export const BUSINESS_RULES_TEST_MAP_TITLE = "Business Rules Test Map";

export const BUSINESS_RULES_TEST_MAP_BACKLOG_ITEM = 621;

export type BusinessRuleDomain =
  | "eligibility-consent"
  | "campaign-compliance"
  | "contact-frequency"
  | "reminders-payments"
  | "campaign-lifecycle"
  | "metrics";

export type BusinessRuleMapping = {
  /** KB business rule ID (e.g. BR-001). */
  id: string;
  /** Short KB rule statement. */
  statement: string;
  domain: BusinessRuleDomain;
  /**
   * Primary backend test class simple names (under
   * backend/src/test/java/.../campaign/).
   */
  backendTests: string[];
  /**
   * Primary frontend evidence paths under frontend/ (unit, integration, e2e).
   * Business rules are largely backend-enforced; UI surfaces exclusion reasons,
   * lifecycle gates, and badges where applicable.
   */
  frontendTests: string[];
  /** Module / architecture / UI acceptance docs (repo-relative). */
  docs: string[];
  /**
   * Optional Sprint 16 critical-test backlog item that restates this rule
   * (647–665 band).
   */
  criticalTestItem?: number;
};

export type BusinessRulesDocSectionId =
  | "purpose"
  | "scope"
  | "how-to-read"
  | "eligibility-consent"
  | "campaign-compliance"
  | "contact-frequency"
  | "reminders-payments"
  | "campaign-lifecycle"
  | "metrics"
  | "critical-crosswalk"
  | "coverage-summary"
  | "related-items"
  | "acceptance";

export type BusinessRulesDocSection = {
  id: BusinessRulesDocSectionId;
  index: number;
  title: string;
  docHeading: string;
};

/** Ordered documentation sections for item 621. */
export const BUSINESS_RULES_DOC_SECTIONS: BusinessRulesDocSection[] = [
  { id: "purpose", index: 0, title: "Purpose", docHeading: "## Purpose" },
  { id: "scope", index: 1, title: "Scope", docHeading: "## Scope" },
  {
    id: "how-to-read",
    index: 2,
    title: "How to read this map",
    docHeading: "## How to read this map",
  },
  {
    id: "eligibility-consent",
    index: 3,
    title: "Eligibility and consent",
    docHeading: "## Eligibility and consent (BR-001–BR-004)",
  },
  {
    id: "campaign-compliance",
    index: 4,
    title: "Campaign compliance and exclusions",
    docHeading: "## Campaign compliance and exclusions (BR-005–BR-007)",
  },
  {
    id: "contact-frequency",
    index: 5,
    title: "Contact frequency and audience hygiene",
    docHeading: "## Contact frequency and audience hygiene (BR-010–BR-014)",
  },
  {
    id: "reminders-payments",
    index: 6,
    title: "Reminders and payments",
    docHeading: "## Reminders and payments (BR-020–BR-024)",
  },
  {
    id: "campaign-lifecycle",
    index: 7,
    title: "Campaign lifecycle constraints",
    docHeading: "## Campaign lifecycle constraints (BR-030–BR-033)",
  },
  {
    id: "metrics",
    index: 8,
    title: "Metrics integrity",
    docHeading: "## Metrics integrity (BR-034)",
  },
  {
    id: "critical-crosswalk",
    index: 9,
    title: "Critical test crosswalk",
    docHeading: "## Critical test crosswalk (items 647–665)",
  },
  {
    id: "coverage-summary",
    index: 10,
    title: "Coverage summary",
    docHeading: "## Coverage summary",
  },
  {
    id: "related-items",
    index: 11,
    title: "Related Sprint 16 items",
    docHeading: "## Related Sprint 16 items",
  },
  {
    id: "acceptance",
    index: 12,
    title: "Acceptance",
    docHeading: "## Acceptance (item 621)",
  },
];

export const BUSINESS_RULES_DOC_REQUIRED_SNIPPETS: string[] = [
  "item **621**",
  "Map tests to KB business rules",
  "BR-001",
  "BR-034",
  "businessRulesTestMap.ts",
  "do not run any tests",
  "EligibilityServiceTests",
];

/**
 * Full KB business rules mapped to primary automated evidence.
 * Lists are representative anchors, not exhaustive method inventories.
 */
export const BUSINESS_RULE_MAPPINGS: BusinessRuleMapping[] = [
  // —— Eligibility / consent ——
  {
    id: "BR-001",
    statement: "A person with do_not_contact = true must never be included in a campaign",
    domain: "eligibility-consent",
    backendTests: [
      "CustomerWithDoNotContactIsExcludedTests",
      "EligibilityServiceTests",
      "DoNotContactChangeCreatesAuditLogTests",
      "ReminderRespectsConsentAndContactLimitsTests",
    ],
    frontendTests: [
      "src/features/customers/customerWithDoNotContactIsExcluded.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
      "src/components/ExclusionReasonSummaryPanel.test.tsx",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/segmentation-module.md",
    ],
    criticalTestItem: 648,
  },
  {
    id: "BR-002",
    statement: "A person who opted out of marketing must be excluded from marketing",
    domain: "eligibility-consent",
    backendTests: [
      "CustomerWithoutValidConsentIsExcludedTests",
      "EligibilityServiceTests",
      "ConsentServiceTests",
      "OptOutChangeCreatesAuditLogTests",
      "SegmentCanFilterByConsentStatusTests",
    ],
    frontendTests: [
      "src/features/customers/customerWithoutValidConsentIsExcluded.test.ts",
      "src/features/customers/consentUpdateFlow.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/consent-module.md",
    ],
  },
  {
    id: "BR-003",
    statement:
      "A beneficiary requiring guardian consent cannot be contacted until guardian consent is valid",
    domain: "eligibility-consent",
    backendTests: [
      "MinorBeneficiaryWithoutGuardianConsentIsExcludedTests",
      "EligibilityServiceTests",
      "BeneficiaryServiceTests",
      "EligibilityRulesDocumentationTests",
    ],
    frontendTests: [
      "src/features/customers/minorBeneficiaryWithoutGuardianConsentIsExcluded.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
      "src/pages/CustomerDetailsPage.test.tsx",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/beneficiary-module.md",
    ],
    criticalTestItem: 650,
  },
  {
    id: "BR-004",
    statement: "Consent must include type, purpose, source, date, and status",
    domain: "eligibility-consent",
    backendTests: [
      "CustomerWithoutValidConsentIsExcludedTests",
      "ConsentRecordTests",
      "ConsentDtoTests",
      "ConsentServiceTests",
      "ConsentModuleDocumentationTests",
    ],
    frontendTests: [
      "src/features/customers/customerWithoutValidConsentIsExcluded.test.ts",
      "src/features/customers/consentUpdateFlow.test.ts",
      "src/api/consents.test.ts",
      "src/components/ConsentStatusBadge.test.tsx",
    ],
    docs: [
      "docs/modules/consent-module.md",
      "docs/testing/ui-consent-update.md",
      "docs/architecture/eligibility-rules.md",
    ],
    criticalTestItem: 649,
  },

  // —— Campaign compliance ——
  {
    id: "BR-005",
    statement: "Campaigns cannot launch before Compliance Officer approval",
    domain: "campaign-compliance",
    backendTests: [
      "CampaignCannotLaunchWithoutApprovalTests",
      "ComplianceOfficerCanApproveRejectCampaignsTests",
      "CampaignServiceTests",
      "CampaignServiceIntegrationTests",
      "CampaignStatusTests",
      "CampaignCanBeApprovedTests",
      "CampaignLaunchCreatesAuditLogTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignCannotLaunchWithoutApproval.test.ts",
      "src/features/campaigns/complianceOfficerCanApproveRejectCampaigns.test.ts",
      "src/features/campaigns/campaignLaunchFlow.test.ts",
      "src/test/integration/campaignLaunch.integration.test.tsx",
      "tests/e2e/campaign-launch.spec.ts",
      "src/features/campaigns/complianceApprovalFlow.test.ts",
    ],
    docs: [
      "docs/modules/campaign-launch.md",
      "docs/modules/compliance-review.md",
      "docs/testing/ui-campaign-launch.md",
    ],
    criticalTestItem: 647,
  },
  {
    id: "BR-006",
    statement: "Campaigns must show recipient eligibility reasons",
    domain: "campaign-compliance",
    backendTests: [
      "EligibilityServiceTests",
      "EligibilityResponseTests",
      "CampaignRecipientServiceTests",
      "RecipientPreviewDocumentationTests",
      "SegmentExclusionReasonSummaryTests",
    ],
    frontendTests: [
      "src/features/campaigns/recipientPreviewClarity.test.ts",
      "src/components/ExclusionReasonSummaryPanel.test.tsx",
      "src/pages/CampaignRecipientPreviewPage.test.tsx",
    ],
    docs: [
      "docs/modules/recipient-preview.md",
      "docs/architecture/eligibility-rules.md",
      "docs/modules/audience-preview-logic.md",
    ],
  },
  {
    id: "BR-007",
    statement: "Campaigns must record excluded contacts and exclusion reasons",
    domain: "campaign-compliance",
    backendTests: [
      "CampaignRecipientServiceTests",
      "CampaignRecipientRepositoryTests",
      "CampaignRecipientTests",
      "SegmentExclusionReasonSummarySupportTests",
    ],
    frontendTests: [
      "src/features/segments/exclusionReasons.test.ts",
      "src/features/campaigns/recipientPreviewClarity.test.ts",
    ],
    docs: [
      "docs/modules/recipient-preview.md",
      "docs/modules/campaign-launch.md",
    ],
  },

  // —— Contact frequency / audience hygiene ——
  {
    id: "BR-010",
    statement: "Same customer cannot receive the same campaign twice",
    domain: "contact-frequency",
    backendTests: [
      "SameCustomerCannotBeDuplicatedInSameCampaignTests",
      "EligibilityServiceTests",
      "CampaignRecipientServiceTests",
      "CampaignRecipientRepositoryTests",
    ],
    frontendTests: [
      "src/features/campaigns/sameCustomerCannotBeDuplicatedInSameCampaign.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
      "src/features/campaigns/recipientPreviewClarity.test.ts",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/recipient-preview.md",
    ],
    criticalTestItem: 651,
  },
  {
    id: "BR-011",
    statement:
      "Same customer cannot receive more than the configured number of marketing messages per month",
    domain: "contact-frequency",
    backendTests: [
      "CustomerCannotExceedMonthlyContactLimitTests",
      "ConfigurableMonthlyContactLimitTests",
      "EligibilityServiceTests",
      "ConfigurableMonthlyContactLimitAiTests",
      "ReminderRespectsConsentAndContactLimitsTests",
    ],
    frontendTests: [
      "src/features/customers/customerCannotExceedMonthlyContactLimit.test.ts",
      "src/api/ai.test.ts",
      "src/pages/SystemSettingsPage.test.tsx",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/system-settings.md",
    ],
    criticalTestItem: 652,
  },
  {
    id: "BR-012",
    statement: "Failed sends can be retried maximum 3 times",
    domain: "contact-frequency",
    backendTests: [
      "ConfigurableSendRetryLimitTests",
      "SendRetryServiceTests",
      "CommunicationServiceTests",
    ],
    frontendTests: ["src/api/contactEvents.test.ts"],
    docs: [
      "docs/modules/communication-tracking.md",
      "docs/modules/system-settings.md",
    ],
  },
  {
    id: "BR-013",
    statement:
      "Uninterested customers are excluded from similar campaigns for a configurable period",
    domain: "contact-frequency",
    backendTests: [
      "ConfigurableUninterestedExclusionPeriodTests",
      "EligibilityServiceTests",
      "CustomerStatusChangedAtTests",
    ],
    frontendTests: [
      "src/features/segments/exclusionReasons.test.ts",
      "src/pages/SystemSettingsPage.test.tsx",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/system-settings.md",
    ],
  },
  {
    id: "BR-014",
    statement: "Converted customers should not receive the same campaign again",
    domain: "contact-frequency",
    backendTests: ["EligibilityServiceTests", "EligibilityRulesDocumentationTests"],
    frontendTests: [
      "src/features/segments/exclusionReasons.test.ts",
      "src/components/CustomerStatusBadge.test.tsx",
    ],
    docs: ["docs/architecture/eligibility-rules.md"],
  },

  // —— Reminders / payments ——
  {
    id: "BR-020",
    statement: "Green reminder is the first reminder",
    domain: "reminders-payments",
    backendTests: [
      "GreenReminderIsFirstReminderTests",
      "PaymentReminderLevelRulesTests",
      "GreenYellowRedReminderRulesDocumentationTests",
    ],
    frontendTests: ["src/components/ReminderLevelBadge.test.tsx", "src/pages/RemindersPage.test.tsx"],
    docs: ["docs/modules/green-yellow-red-reminder-rules.md"],
  },
  {
    id: "BR-021",
    statement: "Yellow reminder is the second reminder",
    domain: "reminders-payments",
    backendTests: [
      "YellowReminderIsSecondReminderTests",
      "PaymentReminderLevelRulesTests",
      "GreenYellowRedReminderRulesDocumentationTests",
    ],
    frontendTests: ["src/components/ReminderLevelBadge.test.tsx"],
    docs: ["docs/modules/green-yellow-red-reminder-rules.md"],
  },
  {
    id: "BR-022",
    statement: "Red reminder is the third reminder and indicates likely default risk",
    domain: "reminders-payments",
    backendTests: [
      "RedReminderIsThirdReminderTests",
      "PaymentReminderLevelRulesTests",
      "AiRecommendationServiceTests",
    ],
    frontendTests: [
      "src/components/ReminderLevelBadge.test.tsx",
      "src/components/AiRecommendationSections.test.tsx",
    ],
    docs: [
      "docs/modules/green-yellow-red-reminder-rules.md",
      "docs/modules/ai-features.md",
    ],
  },
  {
    id: "BR-023",
    statement: "Product-expiration campaign can start 3, 6, or 12 months before expiration",
    domain: "reminders-payments",
    backendTests: [
      "ProductExpirationReminderIsGenerated3MonthsTests",
      "ProductExpirationReminderIsGenerated6MonthsTests",
      "ProductExpirationReminderIsGenerated12MonthsTests",
      "ProductExpirationReminderRulesTests",
    ],
    frontendTests: ["src/api/reminders.test.ts", "src/pages/RemindersPage.test.tsx"],
    docs: [
      "docs/modules/reminder-scheduling.md",
      "docs/modules/segmentation-module.md",
    ],
  },
  {
    id: "BR-024",
    statement: "Payment reminder must not be sent if payment is completed",
    domain: "reminders-payments",
    backendTests: [
      "PaymentReminderIsNotSentIfPaymentIsCompletedTests",
      "PaymentReminderNotSentIfPaymentCompletedTests",
      "PaymentReminderNotSentIfPaymentCompletedApiTests",
      "ReminderLogicRespectsConsentPaymentExpirationAndContactLimitsTests",
    ],
    frontendTests: [
      "src/features/schedules/paymentReminderIsNotSentIfPaymentIsCompleted.test.ts",
      "src/api/reminders.test.ts",
      "src/api/paymentRecords.test.ts",
    ],
    docs: ["docs/modules/reminder-scheduling.md", "docs/modules/payment-records.md"],
    criticalTestItem: 660,
  },

  // —— Campaign lifecycle ——
  {
    id: "BR-030",
    statement:
      "Campaign must have name, objective, target segment, product, message, schedule, owner",
    domain: "campaign-lifecycle",
    backendTests: [
      "CampaignServiceTests",
      "CampaignCanBeSubmittedTests",
      "CampaignDtoTests",
      "CampaignControllerTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignFormValidation.test.ts",
      "src/features/campaigns/campaignCreationFlow.test.ts",
      "src/pages/CampaignBuilderPage.test.tsx",
    ],
    docs: [
      "docs/modules/campaign-lifecycle.md",
      "docs/testing/ui-campaign-creation.md",
    ],
  },
  {
    id: "BR-031",
    statement: "Draft campaign can be edited",
    domain: "campaign-lifecycle",
    backendTests: [
      "CampaignDraftCanBeUpdatedTests",
      "CampaignUpdateDraftEndpointIntegrationTests",
      "CampaignManagerCanCreateDraftCampaignTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignCreationFlow.test.ts",
      "src/features/campaigns/campaignBuilderFlow.test.ts",
    ],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },
  {
    id: "BR-032",
    statement: "Submitted campaign cannot be launched before approval",
    domain: "campaign-lifecycle",
    backendTests: [
      "CampaignCannotLaunchWithoutApprovalTests",
      "CampaignServiceTests",
      "CampaignStatusTests",
      "CampaignCanBeSubmittedTests",
      "CampaignCanBeApprovedTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignCannotLaunchWithoutApproval.test.ts",
      "src/features/campaigns/campaignLaunchFlow.test.ts",
      "src/features/campaigns/complianceApprovalFlow.test.ts",
      "src/components/CampaignStatusBadge.test.tsx",
    ],
    docs: [
      "docs/modules/campaign-lifecycle.md",
      "docs/modules/campaign-launch.md",
      "docs/modules/compliance-review.md",
    ],
    criticalTestItem: 647,
  },
  {
    id: "BR-033",
    statement: "Approved campaign can be launched, paused, completed, or archived",
    domain: "campaign-lifecycle",
    backendTests: [
      "CampaignServiceTests",
      "CampaignStatusTests",
      "CampaignServiceIntegrationTests",
      "CampaignCanBeApprovedTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignLaunchFlow.test.ts",
      "src/pages/CampaignsPage.test.tsx",
      "src/components/CampaignStatusBadge.test.tsx",
    ],
    docs: ["docs/modules/campaign-lifecycle.md", "docs/modules/campaign-launch.md"],
  },

  // —— Metrics ——
  {
    id: "BR-034",
    statement: "Campaign metrics update after contact events",
    domain: "metrics",
    backendTests: [
      "ContactEventsUpdateAnalyticsTests",
      "EngagementCountsUpdateFromContactEventsTests",
      "SentCountUpdatesAfterLaunchTests",
      "AnalyticsServiceTests",
      "ReportsUseAggregatedDataAndMetricsAreTraceableTests",
    ],
    frontendTests: [
      "src/features/analytics/contactEventsUpdateAnalytics.test.ts",
      "src/features/dashboard/dashboardAnalyticsFlow.test.ts",
      "src/api/contactEvents.test.ts",
      "src/api/analytics.test.ts",
      "tests/e2e/dashboard-analytics.spec.ts",
    ],
    docs: [
      "docs/modules/communication-tracking.md",
      "docs/modules/analytics-module.md",
      "docs/modules/kpi-definitions.md",
      "docs/modules/report-export.md",
    ],
    criticalTestItem: 656,
  },
];

/** Expected BR IDs in KB section 13 order. */
export const EXPECTED_BUSINESS_RULE_IDS: string[] = [
  "BR-001",
  "BR-002",
  "BR-003",
  "BR-004",
  "BR-005",
  "BR-006",
  "BR-007",
  "BR-010",
  "BR-011",
  "BR-012",
  "BR-013",
  "BR-014",
  "BR-020",
  "BR-021",
  "BR-022",
  "BR-023",
  "BR-024",
  "BR-030",
  "BR-031",
  "BR-032",
  "BR-033",
  "BR-034",
];

export function businessRuleIds(): string[] {
  return BUSINESS_RULE_MAPPINGS.map((m) => m.id);
}

export function getBusinessRuleMapping(id: string): BusinessRuleMapping | undefined {
  return BUSINESS_RULE_MAPPINGS.find((m) => m.id === id);
}

export function catalogIdsMatchExpectedOrder(): boolean {
  const ids = businessRuleIds();
  if (ids.length !== EXPECTED_BUSINESS_RULE_IDS.length) {
    return false;
  }
  return ids.every((id, i) => id === EXPECTED_BUSINESS_RULE_IDS[i]);
}

export function everyMappingHasEvidence(): boolean {
  return BUSINESS_RULE_MAPPINGS.every(
    (m) =>
      m.statement.trim().length > 0 &&
      m.backendTests.length > 0 &&
      m.frontendTests.length > 0 &&
      m.docs.length > 0,
  );
}

export function countByDomain(): Record<BusinessRuleDomain, number> {
  const counts: Record<BusinessRuleDomain, number> = {
    "eligibility-consent": 0,
    "campaign-compliance": 0,
    "contact-frequency": 0,
    "reminders-payments": 0,
    "campaign-lifecycle": 0,
    metrics: 0,
  };
  for (const m of BUSINESS_RULE_MAPPINGS) {
    counts[m.domain] += 1;
  }
  return counts;
}

export function mappingsWithCriticalTestItems(): BusinessRuleMapping[] {
  return BUSINESS_RULE_MAPPINGS.filter((m) => m.criticalTestItem != null);
}

export function businessRulesDocSectionIdsInOrder(): BusinessRulesDocSectionId[] {
  return BUSINESS_RULES_DOC_SECTIONS.map((s) => s.id);
}

export function isValidBusinessRulesDocSectionOrder(ids: BusinessRulesDocSectionId[]): boolean {
  const expected = businessRulesDocSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, i) => id === expected[i]);
}

export function formatBusinessRulesDocOutline(): string {
  return BUSINESS_RULES_DOC_SECTIONS.map((s) => `${s.index}. ${s.title}`).join("\n");
}

export function documentationContainsRequiredSnippets(documentation: string): boolean {
  return BUSINESS_RULES_DOC_REQUIRED_SNIPPETS.every((snippet) => documentation.includes(snippet));
}

export function docsIndexMustLinkBusinessRulesMap(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("business-rules-test-map.md") && indexMarkdown.includes("621")
  );
}

/**
 * Rules that form the compliance gate before any customer can be contacted.
 * Order mirrors eligibility evaluation documentation.
 */
export const ELIGIBILITY_GATE_BUSINESS_RULE_IDS: string[] = [
  "BR-001",
  "BR-002",
  "BR-003",
  "BR-010",
  "BR-011",
  "BR-013",
  "BR-014",
];

/** Rules that block launch without compliance approval. */
export const LAUNCH_GATE_BUSINESS_RULE_IDS: string[] = ["BR-005", "BR-032"];

export const BUSINESS_RULES_RELATED_BACKLOG_ITEMS: number[] = [
  619, 620, 621, 622, 647, 648, 649, 650, 651, 652, 656, 660, 670, 674, 617,
];
