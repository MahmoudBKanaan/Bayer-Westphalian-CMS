/**
 * Sprint 16 item **622** — Map tests to non-functional requirements.
 *
 * Catalog of NFR-xxx IDs from the knowledge base with primary automated
 * evidence (backend JUnit, frontend Vitest/Playwright, docs). Suite execution
 * remains later Sprint 16 run items (623+, 639–640, 617 pattern).
 */

/** Repo-relative path for the NFR test map document. */
export const NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH =
  "docs/testing/non-functional-requirements-test-map.md";

export const NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_TITLE =
  "Non-Functional Requirements Test Map";

export const NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_BACKLOG_ITEM = 622;

export type NonFunctionalRequirementDomain =
  | "security"
  | "privacy"
  | "performance"
  | "availability"
  | "usability"
  | "maintainability"
  | "scalability"
  | "auditability"
  | "reliability"
  | "testability"
  | "accessibility"
  | "data-integrity"
  | "backup-recovery"
  | "observability";

export type NonFunctionalRequirementMapping = {
  /** KB NFR ID (e.g. NFR-001). */
  id: string;
  /** Short KB requirement name. */
  name: string;
  /** KB target / description. */
  target: string;
  domain: NonFunctionalRequirementDomain;
  /**
   * Primary backend test class simple names (under
   * backend/src/test/java/.../campaign/).
   */
  backendTests: string[];
  /**
   * Primary frontend evidence paths under frontend/.
   * Empty allowed only when NFR is infrastructure/docs-heavy (availability,
   * backup) but still requires at least docs + backend or scripts.
   */
  frontendTests: string[];
  /** Architecture / development / deployment docs (repo-relative). */
  docs: string[];
  /**
   * Optional Sprint 16 critical or run item that exercises this NFR
   * (e.g. 638 a11y, 639 perf smoke, 640 security regression, 664–665).
   */
  relatedSprint16Items?: number[];
};

export type NonFunctionalRequirementsDocSectionId =
  | "purpose"
  | "scope"
  | "how-to-read"
  | "security-privacy"
  | "performance-availability"
  | "usability-a11y"
  | "engineering-qualities"
  | "audit-reliability"
  | "data-ops"
  | "critical-crosswalk"
  | "coverage-summary"
  | "related-items"
  | "acceptance";

export type NonFunctionalRequirementsDocSection = {
  id: NonFunctionalRequirementsDocSectionId;
  index: number;
  title: string;
  docHeading: string;
};

/** Ordered documentation sections for item 622. */
export const NON_FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS: NonFunctionalRequirementsDocSection[] =
  [
    { id: "purpose", index: 0, title: "Purpose", docHeading: "## Purpose" },
    { id: "scope", index: 1, title: "Scope", docHeading: "## Scope" },
    {
      id: "how-to-read",
      index: 2,
      title: "How to read this map",
      docHeading: "## How to read this map",
    },
    {
      id: "security-privacy",
      index: 3,
      title: "Security and privacy",
      docHeading: "## Security and privacy (NFR-001–NFR-002)",
    },
    {
      id: "performance-availability",
      index: 4,
      title: "Performance and availability",
      docHeading: "## Performance and availability (NFR-003–NFR-004)",
    },
    {
      id: "usability-a11y",
      index: 5,
      title: "Usability and accessibility",
      docHeading: "## Usability and accessibility (NFR-005, NFR-011)",
    },
    {
      id: "engineering-qualities",
      index: 6,
      title: "Maintainability, scalability, testability",
      docHeading: "## Maintainability, scalability, and testability (NFR-006, NFR-007, NFR-010)",
    },
    {
      id: "audit-reliability",
      index: 7,
      title: "Auditability and reliability",
      docHeading: "## Auditability and reliability (NFR-008–NFR-009)",
    },
    {
      id: "data-ops",
      index: 8,
      title: "Data integrity, backup, observability",
      docHeading: "## Data integrity, backup, and observability (NFR-012–NFR-014)",
    },
    {
      id: "critical-crosswalk",
      index: 9,
      title: "Critical and run-item crosswalk",
      docHeading: "## Critical and run-item crosswalk",
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
      docHeading: "## Acceptance (item 622)",
    },
  ];

export const NON_FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS: string[] = [
  "item **622**",
  "Map tests to non-functional requirements",
  "NFR-001",
  "NFR-014",
  "nonFunctionalRequirementsTestMap.ts",
  "do not run any tests",
  "SecurityHardeningDocumentationTests",
];

/**
 * Full KB non-functional requirements mapped to primary automated evidence.
 */
export const NON_FUNCTIONAL_REQUIREMENT_MAPPINGS: NonFunctionalRequirementMapping[] = [
  {
    id: "NFR-001",
    name: "Security",
    target: "Role-based access, password hashing, JWT/session security",
    domain: "security",
    backendTests: [
      "ProtectedEndpointSecurityTests",
      "DisabledUserCannotLogInTests",
      "ReportExportIsRestrictedToAuthorizedRolesTests",
      "UnauthorizedUserCannotExportRestrictedReportsTests",
      "ProductManagerCannotLaunchCampaignsTests",
      "BiAnalystCannotEditCustomersTests",
      "ComplianceOfficerCanApproveRejectCampaignsTests",
      "MethodAuthorizationAnnotationTests",
      "PasswordHashingServiceTests",
      "JwtServiceTests",
      "JwtAuthenticationFilterTests",
      "SecurityConfigurationTests",
      "SecurityHardeningDocumentationTests",
      "ProductionProfileHidesStackTracesTests",
      "ProductionStackTraceHiddenTests",
      "MissingSecretsAreDetectedTests",
      "SecureErrorResponsesTests",
      "SecretPresenceValidatorTests",
      "HttpsEnforcementFilterTests",
      "ApiSecurityHeadersFilterTests",
      "LoginAttemptTrackerTests",
    ],
    frontendTests: [
      "src/features/auth/disabledUserCannotLogIn.test.ts",
      "src/features/security/productionProfileHidesStackTraces.test.ts",
      "src/features/security/missingSecretsAreDetected.test.ts",
      "src/features/reports/reportExportIsRestrictedToAuthorizedRoles.test.ts",
      "src/features/auth/permissions.test.ts",
      "src/features/auth/roleBasedMenu.test.ts",
      "src/test/integration/roleBasedMenu.integration.test.tsx",
      "tests/e2e/role-based-menu.spec.ts",
      "src/auth/AuthProvider.test.tsx",
      "src/features/auth/loginFlow.test.ts",
    ],
    docs: [
      "docs/architecture/security-hardening.md",
      "docs/architecture/authentication-design.md",
      "docs/architecture/role-based-access.md",
      "docs/deployment/production-security-checklist.md",
      "docs/testing/ui-role-based-menu.md",
      "docs/modules/report-export.md",
    ],
    relatedSprint16Items: [627, 640, 653, 654, 659, 663, 664, 665],
  },
  {
    id: "NFR-002",
    name: "Privacy",
    target: "GDPR-aware consent, opt-out, data minimization",
    domain: "privacy",
    backendTests: [
      "AiRecommendationCannotBypassConsentRulesTests",
      "ConsentServiceTests",
      "ConsentModuleDocumentationTests",
      "EligibilityServiceTests",
      "OptOutChangeCreatesAuditLogTests",
      "AuditLogIsCreatedAfterConsentChangeTests",
      "ConsentChangeCreatesAuditLogTests",
      "SensitiveAuditAndProductionSafetyAcceptanceTests",
      "SafeApiErrorLoggingDocumentationTests",
    ],
    frontendTests: [
      "src/features/ai/aiRecommendationCannotBypassConsentRules.test.ts",
      "src/features/customers/consentUpdateFlow.test.ts",
      "src/test/integration/consentUpdate.integration.test.tsx",
      "tests/e2e/consent-update.spec.ts",
      "src/components/ConsentStatusBadge.test.tsx",
    ],
    docs: [
      "docs/modules/consent-module.md",
      "docs/modules/ai-limitations-and-human-approval.md",
      "docs/architecture/eligibility-rules.md",
      "docs/architecture/security-hardening.md",
    ],
    relatedSprint16Items: [628, 649, 661],
  },
  {
    id: "NFR-003",
    name: "Performance",
    target: "Normal searches under 1 second for project dataset",
    domain: "performance",
    backendTests: [
      "PerformanceSmokeTests",
      "PerformanceSmokeDocumentationTests",
      "CustomerRepositoryTests",
      "ProductSearchEndpointTests",
      "SegmentServiceTests",
      "DashboardEndpointTests",
      "FlywayMigrationResourceTests",
    ],
    frontendTests: [
      "src/features/testing/performanceSmoke.test.ts",
      "src/pages/CustomersPage.test.tsx",
      "src/features/products/productSearch.test.ts",
      "src/features/dashboard/dashboardAnalyticsFlow.test.ts",
      "src/pages/DashboardPage.test.tsx",
    ],
    docs: [
      "docs/testing/performance-smoke.md",
      "docs/database/migration-strategy.md",
      "docs/modules/customer-module.md",
      "docs/modules/analytics-module.md",
    ],
    relatedSprint16Items: [639],
  },
  {
    id: "NFR-004",
    name: "Availability",
    target: "99% target for project-level deployment",
    domain: "availability",
    backendTests: [
      "HealthEndpointIntegrationTests",
      "HealthControllerTests",
      "CampaignApplicationTests",
      "PostgreSqlConnectionIntegrationTests",
    ],
    frontendTests: [
      "src/app/App.test.tsx",
      "src/components/ErrorState.test.tsx",
    ],
    docs: [
      "docs/deployment/production-security-checklist.md",
      "docker/README.md",
      "docs/development/developer-setup.md",
    ],
    relatedSprint16Items: [646, 674],
  },
  {
    id: "NFR-005",
    name: "Usability",
    target: "Clear dashboards, forms, filters, validation",
    domain: "usability",
    backendTests: [
      "ProductSearchAndFilterUiTests",
      "CustomerModuleDocumentationTests",
      "CampaignManagerUserGuideDocumentationTests",
    ],
    frontendTests: [
      "src/features/dashboard/dashboardReadability.test.ts",
      "src/features/campaigns/campaignFormValidation.test.ts",
      "src/components/FormValidationMessage.test.tsx",
      "src/components/EmptyState.test.tsx",
      "src/components/SuccessNotification.test.tsx",
      "src/components/ConfirmationDialog.test.tsx",
      "src/features/ui/uiStyleNotes.test.ts",
      "src/features/readiness/businessUserWorkflowGate.test.ts",
      "src/test/integration/workflowRoutes.integration.test.tsx",
    ],
    docs: [
      "docs/development/ui-style-notes.md",
      "docs/agile/sprint-15-production-gate.md",
      "docs/testing/ui-dashboard-analytics.md",
      "docs/testing/ui-campaign-creation.md",
    ],
    relatedSprint16Items: [635, 636, 637],
  },
  {
    id: "NFR-006",
    name: "Maintainability",
    target: "Layered backend, reusable frontend components",
    domain: "maintainability",
    backendTests: [
      "BackendPackageReadmeTests",
      "BaseEntityTests",
      "GlobalExceptionHandlerTests",
      "OpenApiConfigurationTests",
    ],
    frontendTests: [
      "src/components/frontendComponentInventory.test.tsx",
      "src/components/StatusBadge.test.tsx",
      "src/components/MetricCard.test.tsx",
      "src/app/styles.test.ts",
      "src/features/testing/frontendTestingNotes.test.ts",
    ],
    docs: [
      "docs/architecture/initial-architecture.md",
      "docs/testing/frontend-component-tests.md",
      "docs/development/developer-setup.md",
      "backend/README.md",
      "frontend/README.md",
    ],
    relatedSprint16Items: [619, 667],
  },
  {
    id: "NFR-007",
    name: "Scalability",
    target: "Pagination, indexes, async jobs",
    domain: "scalability",
    backendTests: [
      "CustomerControllerTests",
      "CustomerRepositoryTests",
      "PageResponseTests",
      "FlywayMigrationResourceTests",
      "ReminderProcessingSchedulerTests",
      "SendRetryServiceTests",
    ],
    frontendTests: [
      "src/pages/CustomersPage.test.tsx",
      "src/api/customers.test.ts",
      "src/pages/RemindersPage.test.tsx",
    ],
    docs: [
      "docs/database/migration-strategy.md",
      "docs/modules/reminder-scheduler.md",
      "docs/modules/customer-module.md",
    ],
    relatedSprint16Items: [624, 625, 630],
  },
  {
    id: "NFR-008",
    name: "Auditability",
    target: "Sensitive actions logged",
    domain: "auditability",
    backendTests: [
      "AuditLogIsCreatedAfterConsentChangeTests",
      "AuditServiceTests",
      "AuditControllerTests",
      "AuditLoggingDocumentationTests",
      "ConsentChangeCreatesAuditLogTests",
      "CampaignApprovalCreatesAuditLogTests",
      "CampaignLaunchCreatesAuditLogTests",
      "ProductChangesCreateAuditLogTests",
      "UserCreationCreatesAuditLogTests",
      "ReportExportCreatesAuditLogTests",
    ],
    frontendTests: [
      "src/features/customers/auditLogIsCreatedAfterConsentChange.test.ts",
      "src/pages/AuditPage.test.tsx",
      "src/api/auditLogs.test.ts",
      "src/components/AuditActionBadge.test.tsx",
    ],
    docs: [
      "docs/modules/audit-logging.md",
      "docs/modules/consent-module.md",
      "docs/modules/campaign-audit-logging.md",
      "docs/modules/product-audit-logging.md",
      "docs/user-guides/system-auditor-guide.md",
    ],
    relatedSprint16Items: [633, 658],
  },
  {
    id: "NFR-009",
    name: "Reliability",
    target: "Failed sends can be retried",
    domain: "reliability",
    backendTests: [
      "SendRetryServiceTests",
      "ConfigurableSendRetryLimitTests",
      "CommunicationServiceTests",
      "MockEmailProviderTests",
      "SmtpEmailProviderTests",
      "ProviderReadySmsProviderTests",
      "ReminderProcessingSchedulerTests",
    ],
    frontendTests: [
      "src/api/contactEvents.test.ts",
      "src/pages/ContactHistoryPage.test.tsx",
      "src/components/ErrorState.test.tsx",
    ],
    docs: [
      "docs/modules/communication-tracking.md",
      "docs/modules/system-settings.md",
      "docs/modules/reminder-scheduler.md",
    ],
    relatedSprint16Items: [629, 630],
  },
  {
    id: "NFR-010",
    name: "Testability",
    target: "Unit, integration, API, UI tests",
    domain: "testability",
    backendTests: [
      "CampaignApplicationTests",
      "HealthEndpointIntegrationTests",
      "FlywayMigrationIntegrationTests",
      "ProtectedEndpointSecurityTests",
    ],
    frontendTests: [
      "src/features/testing/frontendTestingNotes.test.ts",
      "src/features/testing/functionalRequirementsTestMap.test.ts",
      "src/features/testing/businessRulesTestMap.test.ts",
      "src/features/e2e/happyPathFlow.test.ts",
      "src/components/frontendComponentInventory.test.tsx",
      "src/test/integration/renderApp.tsx",
    ],
    docs: [
      "docs/testing/frontend-testing-notes.md",
      "docs/testing/frontend-component-tests.md",
      "docs/testing/frontend-integration-tests.md",
      "docs/testing/playwright-e2e.md",
      "docs/testing/functional-requirements-test-map.md",
      "docs/testing/business-rules-test-map.md",
    ],
    relatedSprint16Items: [619, 620, 621, 622, 623, 635, 636, 637, 667, 670],
  },
  {
    id: "NFR-011",
    name: "Accessibility",
    target: "Labels, keyboard support, contrast",
    domain: "accessibility",
    backendTests: [
      // Accessibility is frontend-primary; backend docs tests only when present.
    ],
    frontendTests: [
      "src/features/a11y/keyboardNavigationFlow.test.ts",
      "src/features/a11y/mainScreensAccessibility.test.ts",
      "src/features/a11y/accessibilityNotes.test.ts",
      "src/test/integration/keyboardNavigation.integration.test.tsx",
      "src/test/integration/mainScreensAccessibility.integration.test.tsx",
      "tests/e2e/keyboard-navigation.spec.ts",
      "tests/e2e/main-screens-accessibility.spec.ts",
      "src/app/styles.test.ts",
    ],
    docs: [
      "docs/development/accessibility-notes.md",
      "docs/testing/ui-keyboard-navigation.md",
      "docs/testing/ui-main-screens-accessibility.md",
      "docs/development/ui-style-notes.md",
    ],
    relatedSprint16Items: [638],
  },
  {
    id: "NFR-012",
    name: "Data integrity",
    target: "Foreign keys, constraints, transactions",
    domain: "data-integrity",
    backendTests: [
      "FlywayMigrationIntegrationTests",
      "FlywayMigrationResourceTests",
      "PostgreSqlConnectionIntegrationTests",
      "SoftDeletableEntityTests",
      "BaseEntityTests",
      "CampaignEntityIntegrationTests",
      "SegmentEntityIntegrationTests",
      "ProductRepositoryIntegrationTests",
    ],
    frontendTests: [
      // Integrity enforced server-side; API client contract tests exercise payloads.
      "src/api/client.test.ts",
      "src/api/customers.test.ts",
      "src/api/campaigns.test.ts",
    ],
    docs: [
      "docs/database/migration-strategy.md",
      "docs/architecture/initial-architecture.md",
    ],
    relatedSprint16Items: [624, 625],
  },
  {
    id: "NFR-013",
    name: "Backup/recovery",
    target: "Database backup strategy",
    domain: "backup-recovery",
    backendTests: [
      "BackupAndRestoreProcessIsDocumentedAndTestableTests",
      "FlywayMigrationIntegrationTests",
      "PostgreSqlConnectionIntegrationTests",
      "ProductionSecurityChecklistDocumentationTests",
    ],
    frontendTests: [
      // No backup UI — ops/documentation catalog only (item 666).
      "src/features/ops/backupAndRestoreProcessIsDocumentedAndTestable.test.ts",
    ],
    docs: [
      "docs/deployment/backup-and-restore.md",
      "docs/deployment/production-security-checklist.md",
      "docs/database/migration-strategy.md",
      "docker/README.md",
    ],
    relatedSprint16Items: [666],
  },
  {
    id: "NFR-014",
    name: "Observability",
    target: "Logs, health endpoints, error tracking",
    domain: "observability",
    backendTests: [
      "HealthEndpointIntegrationTests",
      "HealthControllerTests",
      "HealthResponseTests",
      "GlobalExceptionHandlerTests",
      "SafeApiErrorLoggerTests",
      "SecureErrorResponsesTests",
      "ProductionProfileHidesStackTracesTests",
      "ProductionStackTraceHiddenTests",
      "AuditServiceTests",
    ],
    frontendTests: [
      "src/features/security/productionProfileHidesStackTraces.test.ts",
      "src/components/ErrorState.test.tsx",
      "src/api/client.test.ts",
      "src/app/App.test.tsx",
    ],
    docs: [
      "docs/architecture/security-hardening.md",
      "docs/deployment/production-security-checklist.md",
      "docs/modules/audit-logging.md",
    ],
    relatedSprint16Items: [640, 664],
  },
];

/** Expected NFR IDs in KB section 12 order. */
export const EXPECTED_NON_FUNCTIONAL_REQUIREMENT_IDS: string[] = [
  "NFR-001",
  "NFR-002",
  "NFR-003",
  "NFR-004",
  "NFR-005",
  "NFR-006",
  "NFR-007",
  "NFR-008",
  "NFR-009",
  "NFR-010",
  "NFR-011",
  "NFR-012",
  "NFR-013",
  "NFR-014",
];

export function nonFunctionalRequirementIds(): string[] {
  return NON_FUNCTIONAL_REQUIREMENT_MAPPINGS.map((m) => m.id);
}

export function getNonFunctionalRequirementMapping(
  id: string,
): NonFunctionalRequirementMapping | undefined {
  return NON_FUNCTIONAL_REQUIREMENT_MAPPINGS.find((m) => m.id === id);
}

export function catalogIdsMatchExpectedOrder(): boolean {
  const ids = nonFunctionalRequirementIds();
  if (ids.length !== EXPECTED_NON_FUNCTIONAL_REQUIREMENT_IDS.length) {
    return false;
  }
  return ids.every((id, i) => id === EXPECTED_NON_FUNCTIONAL_REQUIREMENT_IDS[i]);
}

/**
 * Every mapping needs a name, target, at least one backend or frontend test,
 * and at least one doc. NFR-011 may be frontend-only; NFR-013 is ops/docs backup
 * (item 666) without a product backup UI.
 */
export function everyMappingHasEvidence(): boolean {
  return NON_FUNCTIONAL_REQUIREMENT_MAPPINGS.every((m) => {
    if (m.name.trim().length === 0 || m.target.trim().length === 0) {
      return false;
    }
    if (m.docs.length === 0) {
      return false;
    }
    const hasAutomated =
      m.backendTests.length > 0 || m.frontendTests.length > 0;
    return hasAutomated;
  });
}

export function countByDomain(): Record<NonFunctionalRequirementDomain, number> {
  const counts = {
    security: 0,
    privacy: 0,
    performance: 0,
    availability: 0,
    usability: 0,
    maintainability: 0,
    scalability: 0,
    auditability: 0,
    reliability: 0,
    testability: 0,
    accessibility: 0,
    "data-integrity": 0,
    "backup-recovery": 0,
    observability: 0,
  } as Record<NonFunctionalRequirementDomain, number>;
  for (const m of NON_FUNCTIONAL_REQUIREMENT_MAPPINGS) {
    counts[m.domain] += 1;
  }
  return counts;
}

export function nonFunctionalRequirementsDocSectionIdsInOrder(): NonFunctionalRequirementsDocSectionId[] {
  return NON_FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS.map((s) => s.id);
}

export function isValidNonFunctionalRequirementsDocSectionOrder(
  ids: NonFunctionalRequirementsDocSectionId[],
): boolean {
  const expected = nonFunctionalRequirementsDocSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, i) => id === expected[i]);
}

export function formatNonFunctionalRequirementsDocOutline(): string {
  return NON_FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS.map(
    (s) => `${s.index}. ${s.title}`,
  ).join("\n");
}

export function documentationContainsRequiredSnippets(documentation: string): boolean {
  return NON_FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentation.includes(snippet),
  );
}

export function docsIndexMustLinkNonFunctionalRequirementsMap(
  indexMarkdown: string,
): boolean {
  return (
    indexMarkdown.includes("non-functional-requirements-test-map.md") &&
    indexMarkdown.includes("622")
  );
}

/** NFRs tightly coupled to security hardening and critical tests 663–665. */
export const SECURITY_HARDENING_NFR_IDS: string[] = ["NFR-001", "NFR-002", "NFR-014"];

/** NFRs covered primarily by Sprint 15 UX / a11y work. */
export const UX_ACCESSIBILITY_NFR_IDS: string[] = ["NFR-005", "NFR-011"];

/** NFRs that drive the testing pyramid and Sprint 16 mapping work. */
export const TESTABILITY_QUALITY_NFR_IDS: string[] = ["NFR-006", "NFR-010"];

export const NON_FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS: number[] = [
  619, 620, 621, 622, 638, 639, 640, 664, 665, 666, 670, 674, 617,
];
