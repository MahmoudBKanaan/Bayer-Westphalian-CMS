/**
 * Sprint 16 item **620** — Map tests to KB functional requirements.
 *
 * Catalog of FR-xxx / AI-xxx IDs from the knowledge base with primary automated
 * evidence (backend JUnit, frontend Vitest, Playwright, module docs).
 * Execution of suites remains later Sprint 16 run items (623+ / 617 pattern).
 */

/** Repo-relative path for the functional requirements test map document. */
export const FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH =
  "docs/testing/functional-requirements-test-map.md";

export const FUNCTIONAL_REQUIREMENTS_TEST_MAP_TITLE =
  "Functional Requirements Test Map";

export const FUNCTIONAL_REQUIREMENTS_TEST_MAP_BACKLOG_ITEM = 620;

export type FunctionalRequirementDomain =
  | "auth-rbac"
  | "customer"
  | "beneficiary-consent"
  | "product"
  | "campaign"
  | "segmentation"
  | "reminders"
  | "communication-followup"
  | "analytics-reports"
  | "ai-assist";

export type FunctionalRequirementMapping = {
  /** KB requirement ID (e.g. FR-001, AI-001). */
  id: string;
  /** Short KB statement. */
  statement: string;
  domain: FunctionalRequirementDomain;
  /**
   * Primary backend test class simple names (under
   * backend/src/test/java/.../campaign/).
   */
  backendTests: string[];
  /**
   * Primary frontend evidence: feature unit tests, page tests, integration, or
   * Playwright specs (repo-relative under frontend/).
   */
  frontendTests: string[];
  /** Optional module or UI acceptance docs (repo-relative). */
  docs: string[];
};

export type FunctionalRequirementsDocSectionId =
  | "purpose"
  | "scope"
  | "how-to-read"
  | "auth-rbac"
  | "customer"
  | "beneficiary-consent"
  | "product"
  | "campaign"
  | "segmentation"
  | "reminders"
  | "communication-followup"
  | "analytics-reports"
  | "ai-assist"
  | "coverage-summary"
  | "related-items"
  | "acceptance";

export type FunctionalRequirementsDocSection = {
  id: FunctionalRequirementsDocSectionId;
  index: number;
  title: string;
  docHeading: string;
};

/** Ordered documentation sections for item 620. */
export const FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS: FunctionalRequirementsDocSection[] =
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
      id: "auth-rbac",
      index: 3,
      title: "Auth and RBAC",
      docHeading: "## Auth and RBAC (FR-001–FR-005)",
    },
    {
      id: "customer",
      index: 4,
      title: "Customer and prospect",
      docHeading: "## Customer and prospect (FR-010–FR-020)",
    },
    {
      id: "beneficiary-consent",
      index: 5,
      title: "Beneficiary and consent",
      docHeading: "## Beneficiary and consent (FR-030–FR-034)",
    },
    {
      id: "product",
      index: 6,
      title: "Product catalog",
      docHeading: "## Product catalog (FR-040–FR-046)",
    },
    {
      id: "campaign",
      index: 7,
      title: "Campaign lifecycle",
      docHeading: "## Campaign lifecycle (FR-050–FR-062)",
    },
    {
      id: "segmentation",
      index: 8,
      title: "Segmentation",
      docHeading: "## Segmentation (FR-070–FR-079)",
    },
    {
      id: "reminders",
      index: 9,
      title: "Reminders and expiration campaigns",
      docHeading: "## Reminders and expiration campaigns (FR-080–FR-089)",
    },
    {
      id: "communication-followup",
      index: 10,
      title: "Communication and follow-up",
      docHeading: "## Communication and follow-up (FR-090–FR-097)",
    },
    {
      id: "analytics-reports",
      index: 11,
      title: "Analytics and reports",
      docHeading: "## Analytics and reports (FR-100–FR-110)",
    },
    {
      id: "ai-assist",
      index: 12,
      title: "AI-assisted features",
      docHeading: "## AI-assisted features (AI-001–AI-006)",
    },
    {
      id: "coverage-summary",
      index: 13,
      title: "Coverage summary",
      docHeading: "## Coverage summary",
    },
    {
      id: "related-items",
      index: 14,
      title: "Related Sprint 16 items",
      docHeading: "## Related Sprint 16 items",
    },
    {
      id: "acceptance",
      index: 15,
      title: "Acceptance",
      docHeading: "## Acceptance (item 620)",
    },
  ];

export const FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS: string[] = [
  "item **620**",
  "Map tests to KB functional requirements",
  "FR-001",
  "FR-110",
  "AI-001",
  "AI-006",
  "functionalRequirementsTestMap.ts",
  "do not run any tests",
];

/**
 * Full KB functional + AI-assisted functional requirements mapped to primary
 * automated evidence. Lists are representative primary anchors, not exhaustive
 * inventories of every related class.
 */
export const FUNCTIONAL_REQUIREMENT_MAPPINGS: FunctionalRequirementMapping[] = [
  // —— Auth / RBAC ——
  {
    id: "FR-001",
    statement: "Users can log in with email and password",
    domain: "auth-rbac",
    backendTests: [
      "DisabledUserCannotLogInTests",
      "AuthControllerTests",
      "AuthServiceTests",
      "JwtServiceTests",
    ],
    frontendTests: [
      "src/features/auth/disabledUserCannotLogIn.test.ts",
      "src/features/auth/loginFlow.test.ts",
      "src/test/integration/loginFlow.integration.test.tsx",
      "tests/e2e/login-flow.spec.ts",
    ],
    docs: [
      "docs/architecture/authentication-design.md",
      "docs/testing/ui-login-flow.md",
      "docs/admin/user-management-guide.md",
    ],
  },
  {
    id: "FR-002",
    statement: "Users have assigned roles",
    domain: "auth-rbac",
    backendTests: ["UserRoleTests", "RoleRepositoryTests", "UserServiceTests"],
    frontendTests: ["src/features/auth/permissions.test.ts", "src/auth/AuthProvider.test.tsx"],
    docs: ["docs/architecture/role-based-access.md"],
  },
  {
    id: "FR-003",
    statement: "Pages and APIs are restricted by role",
    domain: "auth-rbac",
    backendTests: [
      "ProtectedEndpointSecurityTests",
      "MethodAuthorizationAnnotationTests",
      "AuthorizationExpressionsTests",
    ],
    frontendTests: [
      "src/features/auth/roleBasedMenu.test.ts",
      "src/test/integration/roleBasedMenu.integration.test.tsx",
      "tests/e2e/role-based-menu.spec.ts",
    ],
    docs: ["docs/architecture/role-based-access.md", "docs/testing/ui-role-based-menu.md"],
  },
  {
    id: "FR-004",
    statement: "Users can log out securely",
    domain: "auth-rbac",
    backendTests: ["AuthControllerTests", "AuthServiceTests"],
    frontendTests: ["src/auth/AuthProvider.test.tsx", "src/auth/sessionStorageStrategy.test.ts"],
    docs: ["docs/architecture/authentication-design.md"],
  },
  {
    id: "FR-005",
    statement: "Admin can create, update, disable users",
    domain: "auth-rbac",
    backendTests: [
      "UserControllerTests",
      "UserServiceTests",
      "UserCreationCreatesAuditLogTests",
      "UserDisableCreatesAuditLogTests",
      "DisabledUserCannotLogInTests",
    ],
    frontendTests: [
      "src/pages/UsersPage.test.tsx",
      "src/api/users.test.ts",
      "src/features/auth/disabledUserCannotLogIn.test.ts",
    ],
    docs: [
      "docs/admin/user-management-guide.md",
      "docs/architecture/authentication-design.md",
    ],
  },

  // —— Customer ——
  {
    id: "FR-010",
    statement: "Users can view paginated customers/prospects",
    domain: "customer",
    backendTests: [
      "SoftDeletedCustomersDoNotAppearInActiveListsTests",
      "BiAnalystCannotEditCustomersTests",
      "CustomerControllerTests",
      "CustomerServiceTests",
      "CustomerRepositoryTests",
    ],
    frontendTests: [
      "src/features/customers/softDeletedCustomersDoNotAppearInActiveLists.test.ts",
      "src/features/customers/biAnalystCannotEditCustomers.test.ts",
      "src/pages/CustomersPage.test.tsx",
      "src/api/customers.test.ts",
    ],
    docs: ["docs/modules/customer-module.md", "docs/user-guides/bi-analyst-guide.md"],
  },
  {
    id: "FR-011",
    statement: "Authorized users can create customers/prospects",
    domain: "customer",
    backendTests: ["CustomerControllerTests", "CustomerServiceTests"],
    frontendTests: [
      "src/features/customers/customerCreationFlow.test.ts",
      "src/test/integration/customerCreation.integration.test.tsx",
      "tests/e2e/customer-creation.spec.ts",
    ],
    docs: ["docs/modules/customer-module.md", "docs/testing/ui-customer-creation.md"],
  },
  {
    id: "FR-012",
    statement: "Authorized users can edit customer details",
    domain: "customer",
    backendTests: [
      "BiAnalystCannotEditCustomersTests",
      "CustomerControllerTests",
      "CustomerServiceTests",
    ],
    frontendTests: [
      "src/features/customers/biAnalystCannotEditCustomers.test.ts",
      "src/pages/CustomerDetailsPage.test.tsx",
      "src/api/customers.test.ts",
    ],
    docs: ["docs/modules/customer-module.md", "docs/user-guides/bi-analyst-guide.md"],
  },
  {
    id: "FR-013",
    statement: "Authorized users can soft-delete customers/prospects",
    domain: "customer",
    backendTests: [
      "SoftDeletedCustomersDoNotAppearInActiveListsTests",
      "CustomerServiceTests",
      "CustomerDeletionCreatesAuditLogTests",
    ],
    frontendTests: [
      "src/features/customers/softDeletedCustomersDoNotAppearInActiveLists.test.ts",
      "src/pages/CustomersPage.test.tsx",
    ],
    docs: ["docs/modules/customer-module.md"],
  },
  {
    id: "FR-014",
    statement: "Users can search customers",
    domain: "customer",
    backendTests: ["CustomerControllerTests", "CustomerServiceTests", "CustomerRepositoryTests"],
    frontendTests: ["src/pages/CustomersPage.test.tsx", "src/api/customers.test.ts"],
    docs: ["docs/modules/customer-module.md"],
  },
  {
    id: "FR-015",
    statement: "System supports innovative/fuzzy customer search",
    domain: "customer",
    backendTests: ["AiSearchServiceTests", "AiControllerTests"],
    frontendTests: ["src/api/ai.test.ts", "src/components/AiRecommendationSections.test.tsx"],
    docs: ["docs/modules/ai-features.md"],
  },
  {
    id: "FR-016",
    statement: "Users can view customer profiles",
    domain: "customer",
    backendTests: ["CustomerControllerTests", "CustomerServiceTests"],
    frontendTests: ["src/pages/CustomerDetailsPage.test.tsx"],
    docs: ["docs/modules/customer-module.md"],
  },
  {
    id: "FR-017",
    statement: "Users can view contact history",
    domain: "customer",
    backendTests: ["ContactEventRepositoryTests", "CampaignControllerTests"],
    frontendTests: [
      "src/pages/ContactHistoryPage.test.tsx",
      "src/api/contactEvents.test.ts",
    ],
    docs: ["docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-018",
    statement: "Users can view consent/opt-out status",
    domain: "customer",
    backendTests: ["ConsentControllerTests", "ConsentServiceTests"],
    frontendTests: [
      "src/pages/CustomerDetailsPage.test.tsx",
      "src/components/ConsentStatusBadge.test.tsx",
    ],
    docs: ["docs/modules/consent-module.md"],
  },
  {
    id: "FR-019",
    statement: "Users can mark customer status",
    domain: "customer",
    backendTests: ["CustomerServiceTests", "CustomerStatusChangedAtTests"],
    frontendTests: [
      "src/pages/CustomerDetailsPage.test.tsx",
      "src/components/CustomerStatusBadge.test.tsx",
    ],
    docs: ["docs/modules/customer-module.md"],
  },
  {
    id: "FR-020",
    statement: "Users can import customers/prospects from CSV",
    domain: "customer",
    backendTests: ["CustomerControllerTests", "CustomerCsvImportDocumentationTests"],
    frontendTests: ["src/pages/CustomersPage.test.tsx"],
    docs: ["docs/admin/customer-csv-import-guide.md"],
  },

  // —— Beneficiary / consent ——
  {
    id: "FR-030",
    statement: "Users can add beneficiaries linked to customers",
    domain: "beneficiary-consent",
    backendTests: ["BeneficiaryControllerTests", "BeneficiaryServiceTests"],
    frontendTests: [
      "src/pages/CustomerDetailsPage.test.tsx",
      "src/api/beneficiaries.test.ts",
    ],
    docs: ["docs/modules/beneficiary-module.md"],
  },
  {
    id: "FR-031",
    statement: "Users can store beneficiary contact details",
    domain: "beneficiary-consent",
    backendTests: ["BeneficiaryServiceTests", "BeneficiaryDtoTests", "BeneficiaryTests"],
    frontendTests: ["src/api/beneficiaries.test.ts"],
    docs: ["docs/modules/beneficiary-module.md"],
  },
  {
    id: "FR-032",
    statement: "System tracks guardian consent requirement",
    domain: "beneficiary-consent",
    backendTests: [
      "MinorBeneficiaryWithoutGuardianConsentIsExcludedTests",
      "EligibilityServiceTests",
      "BeneficiaryServiceTests",
    ],
    frontendTests: [
      "src/features/customers/minorBeneficiaryWithoutGuardianConsentIsExcluded.test.ts",
      "src/pages/CustomerDetailsPage.test.tsx",
    ],
    docs: ["docs/architecture/eligibility-rules.md", "docs/modules/beneficiary-module.md"],
  },
  {
    id: "FR-033",
    statement: "Users can record consent status",
    domain: "beneficiary-consent",
    backendTests: [
      "AuditLogIsCreatedAfterConsentChangeTests",
      "ConsentControllerTests",
      "ConsentServiceTests",
      "ConsentChangeCreatesAuditLogTests",
    ],
    frontendTests: [
      "src/features/customers/auditLogIsCreatedAfterConsentChange.test.ts",
      "src/features/customers/consentUpdateFlow.test.ts",
      "src/test/integration/consentUpdate.integration.test.tsx",
      "tests/e2e/consent-update.spec.ts",
    ],
    docs: [
      "docs/modules/consent-module.md",
      "docs/modules/audit-logging.md",
      "docs/testing/ui-consent-update.md",
    ],
  },
  {
    id: "FR-034",
    statement: "System blocks marketing without valid consent",
    domain: "beneficiary-consent",
    backendTests: [
      "CustomerWithoutValidConsentIsExcludedTests",
      "AiRecommendationCannotBypassConsentRulesTests",
      "MinorBeneficiaryWithoutGuardianConsentIsExcludedTests",
      "EligibilityServiceTests",
      "SegmentPreviewAppliesEligibilityServiceTests",
      "ReminderRespectsConsentAndContactLimitsTests",
    ],
    frontendTests: [
      "src/features/customers/customerWithoutValidConsentIsExcluded.test.ts",
      "src/features/ai/aiRecommendationCannotBypassConsentRules.test.ts",
      "src/features/customers/minorBeneficiaryWithoutGuardianConsentIsExcluded.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/consent-module.md",
      "docs/modules/ai-limitations-and-human-approval.md",
    ],
  },

  // —— Product ——
  {
    id: "FR-040",
    statement: "Users can view products",
    domain: "product",
    backendTests: ["ProductControllerTests", "ProductServiceTests"],
    frontendTests: ["src/pages/ProductsPage.test.tsx", "src/api/products.test.ts"],
    docs: ["docs/modules/product-module.md"],
  },
  {
    id: "FR-041",
    statement: "Product Manager/Admin can create products",
    domain: "product",
    backendTests: ["ProductManagerCreateProductTests", "UnauthorizedCreateProductTests"],
    frontendTests: [
      "src/features/products/productCreationFlow.test.ts",
      "src/test/integration/productCreation.integration.test.tsx",
      "tests/e2e/product-creation.spec.ts",
    ],
    docs: ["docs/modules/product-module.md", "docs/testing/ui-product-creation.md"],
  },
  {
    id: "FR-042",
    statement: "Product Manager/Admin can edit products",
    domain: "product",
    backendTests: ["ProductManagerEditProductTests", "ProductServiceTests"],
    frontendTests: ["src/pages/ProductDetailsPage.test.tsx", "src/api/products.test.ts"],
    docs: ["docs/modules/product-module.md"],
  },
  {
    id: "FR-043",
    statement: "Product Manager/Admin can disable/delete products",
    domain: "product",
    backendTests: ["ProductManagerDisableProductTests", "ProductDisableSoftDeleteEndpointTests"],
    frontendTests: ["src/pages/ProductsPage.test.tsx"],
    docs: ["docs/modules/product-module.md"],
  },
  {
    id: "FR-044",
    statement: "Users can search products",
    domain: "product",
    backendTests: ["ProductSearchEndpointTests", "ProductSearchAndFilterUiTests"],
    frontendTests: [
      "src/features/products/productSearch.test.ts",
      "src/components/ProductSearchFilters.test.tsx",
    ],
    docs: ["docs/modules/product-module.md"],
  },
  {
    id: "FR-045",
    statement: "Product Manager can create product-change requests",
    domain: "product",
    backendTests: [
      "ProductChangeRequestCanBeCreatedAndTrackedTests",
      "ProductChangeRequestServiceTests",
    ],
    frontendTests: [
      "src/pages/ProductChangeRequestsPage.test.tsx",
      "src/api/productChangeRequests.test.ts",
    ],
    docs: ["docs/modules/product-module.md"],
  },
  {
    id: "FR-046",
    statement: "Products can be assigned to campaigns",
    domain: "product",
    backendTests: ["CampaignProductSelectionTests", "CampaignProductSelectionIntegrationTests"],
    frontendTests: [
      "src/features/campaigns/campaignCreationFlow.test.ts",
      "src/pages/CampaignBuilderPage.test.tsx",
    ],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },

  // —— Campaign ——
  {
    id: "FR-050",
    statement: "Campaign Manager can create campaigns",
    domain: "campaign",
    backendTests: [
      "CampaignCanBeCreatedTests",
      "CampaignManagerCanCreateDraftCampaignTests",
      "CampaignCreateEndpointIntegrationTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignCreationFlow.test.ts",
      "src/test/integration/campaignCreation.integration.test.tsx",
      "tests/e2e/campaign-creation.spec.ts",
    ],
    docs: ["docs/modules/campaign-lifecycle.md", "docs/testing/ui-campaign-creation.md"],
  },
  {
    id: "FR-051",
    statement: "Campaign Manager can define campaign objective",
    domain: "campaign",
    backendTests: ["CampaignServiceTests", "CampaignDtoTests", "CampaignCanBeCreatedTests"],
    frontendTests: [
      "src/features/campaigns/campaignFormValidation.test.ts",
      "src/pages/CampaignBuilderPage.test.tsx",
    ],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },
  {
    id: "FR-052",
    statement: "Campaign Manager can select promoted products",
    domain: "campaign",
    backendTests: ["CampaignProductSelectionTests", "CampaignServiceTests"],
    frontendTests: ["src/features/campaigns/campaignBuilderFlow.test.ts"],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },
  {
    id: "FR-053",
    statement: "Campaign Manager can define target audience",
    domain: "campaign",
    backendTests: ["CampaignSegmentSelectionTests", "CampaignSegmentSelectionIntegrationTests"],
    frontendTests: ["src/features/campaigns/campaignBuilderFlow.test.ts"],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },
  {
    id: "FR-054",
    statement: "System previews eligible recipients",
    domain: "campaign",
    backendTests: ["CampaignRecipientServiceTests", "RecipientPreviewDocumentationTests"],
    frontendTests: [
      "src/features/campaigns/recipientPreviewClarity.test.ts",
      "src/pages/CampaignRecipientPreviewPage.test.tsx",
    ],
    docs: ["docs/modules/recipient-preview.md"],
  },
  {
    id: "FR-055",
    statement: "System excludes opt-outs and invalid consent",
    domain: "campaign",
    backendTests: [
      "CustomerWithoutValidConsentIsExcludedTests",
      "CustomerWithDoNotContactIsExcludedTests",
      "EligibilityServiceTests",
      "SegmentPreviewAppliesEligibilityServiceWorksTests",
      "SegmentationMustNeverReturnFinalAudienceWithoutEligibilityTests",
    ],
    frontendTests: [
      "src/features/customers/customerWithoutValidConsentIsExcluded.test.ts",
      "src/features/customers/customerWithDoNotContactIsExcluded.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
      "src/components/ExclusionReasonSummaryPanel.test.tsx",
    ],
    docs: ["docs/architecture/eligibility-rules.md"],
  },
  {
    id: "FR-056",
    statement: "System prevents duplicate/excessive marketing",
    domain: "campaign",
    backendTests: [
      "SameCustomerCannotBeDuplicatedInSameCampaignTests",
      "CustomerCannotExceedMonthlyContactLimitTests",
      "ConfigurableMonthlyContactLimitTests",
      "EligibilityServiceTests",
      "ReminderRespectsConsentAndContactLimitsTests",
    ],
    frontendTests: [
      "src/features/campaigns/sameCustomerCannotBeDuplicatedInSameCampaign.test.ts",
      "src/features/customers/customerCannotExceedMonthlyContactLimit.test.ts",
      "src/api/ai.test.ts",
    ],
    docs: ["docs/architecture/eligibility-rules.md", "docs/modules/recipient-preview.md"],
  },
  {
    id: "FR-057",
    statement: "Campaign Manager can save campaign as draft",
    domain: "campaign",
    backendTests: [
      "CampaignDraftCanBeUpdatedTests",
      "CampaignManagerCanCreateDraftCampaignTests",
      "CampaignUpdateDraftEndpointIntegrationTests",
    ],
    frontendTests: ["src/features/campaigns/campaignCreationFlow.test.ts"],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },
  {
    id: "FR-058",
    statement: "Campaign Manager can submit campaign for review",
    domain: "campaign",
    backendTests: ["CampaignCanBeSubmittedTests", "CampaignSubmissionCreatesAuditLogTests"],
    frontendTests: ["src/features/campaigns/campaignCreationFlow.test.ts"],
    docs: ["docs/modules/campaign-lifecycle.md", "docs/modules/compliance-review.md"],
  },
  {
    id: "FR-059",
    statement: "Compliance Officer can approve/reject campaign",
    domain: "campaign",
    backendTests: [
      "ComplianceOfficerCanApproveRejectCampaignsTests",
      "CampaignCanBeApprovedTests",
      "CampaignCanBeRejectedTests",
      "CampaignApprovalCreatesAuditLogTests",
    ],
    frontendTests: [
      "src/features/campaigns/complianceOfficerCanApproveRejectCampaigns.test.ts",
      "src/features/campaigns/complianceApprovalFlow.test.ts",
      "src/test/integration/complianceApproval.integration.test.tsx",
      "tests/e2e/compliance-approval.spec.ts",
    ],
    docs: ["docs/modules/compliance-review.md", "docs/testing/ui-compliance-approval.md"],
  },
  {
    id: "FR-060",
    statement: "Campaign Manager can launch approved campaign",
    domain: "campaign",
    backendTests: [
      "CampaignCannotLaunchWithoutApprovalTests",
      "ProductManagerCannotLaunchCampaignsTests",
      "CampaignServiceTests",
      "CampaignServiceIntegrationTests",
      "CampaignLaunchCreatesAuditLogTests",
      "SentCountUpdatesAfterLaunchTests",
    ],
    frontendTests: [
      "src/features/campaigns/campaignCannotLaunchWithoutApproval.test.ts",
      "src/features/campaigns/productManagerCannotLaunchCampaigns.test.ts",
      "src/features/campaigns/campaignLaunchFlow.test.ts",
      "src/test/integration/campaignLaunch.integration.test.tsx",
      "tests/e2e/campaign-launch.spec.ts",
    ],
    docs: ["docs/modules/campaign-launch.md", "docs/testing/ui-campaign-launch.md"],
  },
  {
    id: "FR-061",
    statement: "Campaign Manager can pause/cancel campaign",
    domain: "campaign",
    backendTests: ["CampaignServiceTests", "CampaignStatusTests", "CampaignControllerTests"],
    frontendTests: ["src/pages/CampaignsPage.test.tsx", "src/components/CampaignStatusBadge.test.tsx"],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },
  {
    id: "FR-062",
    statement: "Campaign Manager can archive completed campaign",
    domain: "campaign",
    backendTests: ["CampaignServiceTests", "CampaignStatusTests"],
    frontendTests: ["src/pages/CampaignsPage.test.tsx"],
    docs: ["docs/modules/campaign-lifecycle.md"],
  },

  // —— Segmentation ——
  {
    id: "FR-070",
    statement: "Users can segment by age group",
    domain: "segmentation",
    backendTests: ["SegmentCanFilterByAgeGroupTests", "SegmentFilterByAgeGroupWorksTests"],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-071",
    statement: "Users can segment by location",
    domain: "segmentation",
    backendTests: ["SegmentCanFilterByLocationTests", "SegmentFilterByLocationWorksTests"],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-072",
    statement: "Users can segment by customer/prospect type",
    domain: "segmentation",
    backendTests: ["SegmentCanFilterByCustomerTypeTests", "SegmentCustomerTypeFilteringTests"],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-073",
    statement: "Users can segment by product ownership",
    domain: "segmentation",
    backendTests: [
      "SegmentCanFilterByProductOwnershipTests",
      "SegmentFilterByProductOwnershipWorksTests",
    ],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-074",
    statement: "Users can segment by payment history",
    domain: "segmentation",
    backendTests: [
      "SegmentCanFilterByPaymentHistoryTests",
      "SegmentFilterByPaymentHistoryWorksTests",
    ],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-075",
    statement: "Users can segment by behavior/interests",
    domain: "segmentation",
    backendTests: [
      "SegmentCanFilterByBehaviorStatusTests",
      "SegmentBehaviorStatusFilteringTests",
    ],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-076",
    statement: "Users can segment by product expiration",
    domain: "segmentation",
    backendTests: [
      "SegmentCanFilterByProductExpirationTests",
      "SegmentFilterByProductExpirationWorksTests",
    ],
    frontendTests: ["src/features/segments/criteriaFields.test.ts"],
    docs: ["docs/modules/segment-criteria-guide.md"],
  },
  {
    id: "FR-077",
    statement: "Users can save reusable segments",
    domain: "segmentation",
    backendTests: [
      "SegmentCanBeCreatedTests",
      "CampaignManagerCanCreateReusableSegmentTests",
      "SegmentServiceTests",
    ],
    frontendTests: [
      "src/features/segments/segmentCreationFlow.test.ts",
      "src/test/integration/segmentCreation.integration.test.tsx",
      "tests/e2e/segment-creation.spec.ts",
    ],
    docs: ["docs/modules/segmentation-module.md", "docs/testing/ui-segment-creation.md"],
  },
  {
    id: "FR-078",
    statement: "Users can combine criteria with AND/OR logic",
    domain: "segmentation",
    backendTests: [
      "SegmentAndLogicReturnsCorrectResultTests",
      "SegmentOrLogicReturnsCorrectResultTests",
      "SegmentCanFilterWithAndCriteriaTests",
      "SegmentCanFilterWithOrCriteriaTests",
    ],
    frontendTests: ["src/components/SegmentCriteriaBuilder.test.tsx"],
    docs: ["docs/modules/segmentation-module.md"],
  },
  {
    id: "FR-079",
    statement: "System previews audience size",
    domain: "segmentation",
    backendTests: [
      "SegmentCanBePreviewedTests",
      "SegmentPreviewReturnsEligibleAndExcludedCountsWorksTests",
      "SegmentExclusionReasonSummaryTests",
    ],
    frontendTests: [
      "src/components/SegmentPreviewResults.test.tsx",
      "src/features/segments/exclusionReasons.test.ts",
    ],
    docs: ["docs/modules/audience-preview-logic.md"],
  },

  // —— Reminders ——
  {
    id: "FR-080",
    statement: "System schedules payment reminders",
    domain: "reminders",
    backendTests: [
      "PaymentReminderIsNotSentIfPaymentIsCompletedTests",
      "PaymentDueReminderIsGeneratedTests",
      "ReminderServiceTests",
    ],
    frontendTests: [
      "src/features/schedules/paymentReminderIsNotSentIfPaymentIsCompleted.test.ts",
      "src/pages/RemindersPage.test.tsx",
      "src/api/reminders.test.ts",
    ],
    docs: ["docs/modules/reminder-scheduling.md", "docs/modules/payment-records.md"],
  },
  {
    id: "FR-081",
    statement: "System sends Green first reminder",
    domain: "reminders",
    backendTests: ["GreenReminderIsFirstReminderTests", "PaymentReminderLevelRulesTests"],
    frontendTests: ["src/components/ReminderLevelBadge.test.tsx"],
    docs: ["docs/modules/green-yellow-red-reminder-rules.md"],
  },
  {
    id: "FR-082",
    statement: "System sends Yellow second reminder",
    domain: "reminders",
    backendTests: ["YellowReminderIsSecondReminderTests", "PaymentReminderLevelRulesTests"],
    frontendTests: ["src/components/ReminderLevelBadge.test.tsx"],
    docs: ["docs/modules/green-yellow-red-reminder-rules.md"],
  },
  {
    id: "FR-083",
    statement: "System sends Red third reminder",
    domain: "reminders",
    backendTests: ["RedReminderIsThirdReminderTests", "PaymentReminderLevelRulesTests"],
    frontendTests: ["src/components/ReminderLevelBadge.test.tsx"],
    docs: ["docs/modules/green-yellow-red-reminder-rules.md"],
  },
  {
    id: "FR-084",
    statement: "System identifies likely payment default",
    domain: "reminders",
    backendTests: ["AiRecommendationServiceTests", "AiControllerTests"],
    frontendTests: ["src/api/ai.test.ts", "src/components/AiRecommendationSections.test.tsx"],
    docs: ["docs/modules/ai-features.md"],
  },
  {
    id: "FR-085",
    statement: "Product-expiration campaigns can start 3 months before expiration",
    domain: "reminders",
    backendTests: [
      "ProductExpirationReminderIsGenerated3MonthsTests",
      "ProductExpirationReminderIsGenerated3MonthsApiTests",
    ],
    frontendTests: ["src/api/reminders.test.ts"],
    docs: ["docs/modules/reminder-scheduling.md"],
  },
  {
    id: "FR-086",
    statement: "Product-expiration campaigns can start 6 months before expiration",
    domain: "reminders",
    backendTests: [
      "ProductExpirationReminderIsGenerated6MonthsTests",
      "ProductExpirationReminderIsGenerated6MonthsApiTests",
    ],
    frontendTests: ["src/api/reminders.test.ts"],
    docs: ["docs/modules/reminder-scheduling.md"],
  },
  {
    id: "FR-087",
    statement: "Product-expiration campaigns can start 12 months before expiration",
    domain: "reminders",
    backendTests: [
      "ProductExpirationReminderIsGenerated12MonthsTests",
      "ProductExpirationReminderIsGenerated12MonthsApiTests",
    ],
    frontendTests: ["src/api/reminders.test.ts"],
    docs: ["docs/modules/reminder-scheduling.md"],
  },
  {
    id: "FR-088",
    statement: "System creates follow-up tasks after reminders",
    domain: "reminders",
    backendTests: ["FollowUpServiceTests", "FollowUpTaskCanBeAssignedTests", "ReminderServiceTests"],
    frontendTests: ["src/pages/FollowUpTasksPage.test.tsx", "src/api/followUpTasks.test.ts"],
    docs: ["docs/modules/follow-up-tasks.md", "docs/modules/reminder-scheduler.md"],
  },
  {
    id: "FR-089",
    statement: "System logs all reminder attempts",
    domain: "reminders",
    backendTests: ["SchedulerLogsReminderAttemptsTests", "ReminderProcessingSchedulerTests"],
    frontendTests: ["src/pages/RemindersPage.test.tsx"],
    docs: ["docs/modules/reminder-scheduler.md"],
  },

  // —— Communication / follow-up ——
  {
    id: "FR-090",
    statement: "System records contact attempts",
    domain: "communication-followup",
    backendTests: [
      "ContactEventTests",
      "ContactEventRepositoryTests",
      "CommunicationServiceTests",
      "EngagementCountsUpdateFromContactEventsTests",
    ],
    frontendTests: ["src/api/contactEvents.test.ts", "src/pages/ContactHistoryPage.test.tsx"],
    docs: ["docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-091",
    statement: "System shows contact timeline",
    domain: "communication-followup",
    backendTests: ["ContactEventRepositoryTests", "CampaignControllerTests"],
    frontendTests: ["src/pages/ContactHistoryPage.test.tsx"],
    docs: ["docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-092",
    statement: "System prevents excessive contact frequency",
    domain: "communication-followup",
    backendTests: [
      "CustomerCannotExceedMonthlyContactLimitTests",
      "ConfigurableMonthlyContactLimitTests",
      "EligibilityServiceTests",
      "ConfigurableMonthlyContactLimitAiTests",
    ],
    frontendTests: [
      "src/features/customers/customerCannotExceedMonthlyContactLimit.test.ts",
      "src/api/ai.test.ts",
    ],
    docs: [
      "docs/architecture/eligibility-rules.md",
      "docs/modules/system-settings.md",
    ],
  },
  {
    id: "FR-093",
    statement: "Users can create follow-up tasks",
    domain: "communication-followup",
    backendTests: ["FollowUpControllerTests", "FollowUpServiceTests", "FollowUpTaskCanBeAssignedTests"],
    frontendTests: ["src/pages/FollowUpTasksPage.test.tsx", "src/api/followUpTasks.test.ts"],
    docs: ["docs/modules/follow-up-tasks.md"],
  },
  {
    id: "FR-094",
    statement: "Users can record contact outcomes",
    domain: "communication-followup",
    backendTests: ["ContactEventDtoTests", "CommunicationServiceTests", "CampaignControllerTests"],
    frontendTests: ["src/api/contactEvents.test.ts"],
    docs: ["docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-095",
    statement: "Users can add communication notes",
    domain: "communication-followup",
    backendTests: ["FollowUpServiceTests", "ContactEventTests", "FollowUpDtoTests"],
    frontendTests: ["src/pages/FollowUpTasksPage.test.tsx", "src/pages/ContactHistoryPage.test.tsx"],
    docs: ["docs/modules/communication-tracking.md", "docs/modules/follow-up-tasks.md"],
  },
  {
    id: "FR-096",
    statement: "Users can remove uninterested parties from mailing lists",
    domain: "communication-followup",
    backendTests: [
      "ConfigurableUninterestedExclusionPeriodTests",
      "EligibilityServiceTests",
      "ConsentServiceTests",
    ],
    frontendTests: ["src/features/customers/consentUpdateFlow.test.ts"],
    docs: ["docs/modules/consent-module.md", "docs/architecture/eligibility-rules.md"],
  },
  {
    id: "FR-097",
    statement: "System respects do-not-contact status",
    domain: "communication-followup",
    backendTests: [
      "CustomerWithDoNotContactIsExcludedTests",
      "EligibilityServiceTests",
      "DoNotContactChangeCreatesAuditLogTests",
      "ReminderRespectsConsentAndContactLimitsTests",
    ],
    frontendTests: [
      "src/features/customers/customerWithDoNotContactIsExcluded.test.ts",
      "src/features/segments/exclusionReasons.test.ts",
    ],
    docs: ["docs/architecture/eligibility-rules.md"],
  },

  // —— Analytics / reports ——
  {
    id: "FR-100",
    statement: "Dashboard shows campaign totals",
    domain: "analytics-reports",
    backendTests: ["DashboardEndpointTests", "AnalyticsServiceTests"],
    frontendTests: [
      "src/features/dashboard/dashboardAnalyticsFlow.test.ts",
      "src/test/integration/dashboardAnalytics.integration.test.tsx",
      "tests/e2e/dashboard-analytics.spec.ts",
    ],
    docs: ["docs/modules/analytics-module.md", "docs/testing/ui-dashboard-analytics.md"],
  },
  {
    id: "FR-101",
    statement: "Dashboard shows active campaigns",
    domain: "analytics-reports",
    backendTests: ["DashboardEndpointTests", "AnalyticsServiceTests"],
    frontendTests: ["src/pages/DashboardPage.test.tsx", "src/features/dashboard/dashboardCharts.test.ts"],
    docs: ["docs/modules/analytics-module.md"],
  },
  {
    id: "FR-102",
    statement: "Dashboard shows audience size",
    domain: "analytics-reports",
    backendTests: ["AudienceSizeIsCalculatedCorrectlyTests", "CalculateAudienceSizeTests"],
    frontendTests: ["src/features/dashboard/dashboardAnalyticsFlow.test.ts"],
    docs: ["docs/modules/kpi-definitions.md"],
  },
  {
    id: "FR-103",
    statement: "Dashboard shows messages sent",
    domain: "analytics-reports",
    backendTests: [
      "ContactEventsUpdateAnalyticsTests",
      "CalculateSentCountTests",
      "SentCountUpdatesAfterLaunchTests",
    ],
    frontendTests: [
      "src/features/analytics/contactEventsUpdateAnalytics.test.ts",
      "src/features/dashboard/dashboardAnalyticsFlow.test.ts",
    ],
    docs: ["docs/modules/kpi-definitions.md", "docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-104",
    statement: "Dashboard shows open rate",
    domain: "analytics-reports",
    backendTests: [
      "ContactEventsUpdateAnalyticsTests",
      "OpenRateIsCalculatedCorrectlyTests",
      "CalculateOpenRateTests",
    ],
    frontendTests: [
      "src/features/analytics/contactEventsUpdateAnalytics.test.ts",
      "src/features/dashboard/dashboardCharts.test.ts",
    ],
    docs: ["docs/modules/kpi-definitions.md", "docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-105",
    statement: "Dashboard shows click rate",
    domain: "analytics-reports",
    backendTests: [
      "ContactEventsUpdateAnalyticsTests",
      "ClickRateIsCalculatedCorrectlyTests",
      "CalculateClickRateTests",
    ],
    frontendTests: [
      "src/features/analytics/contactEventsUpdateAnalytics.test.ts",
      "src/features/dashboard/dashboardCharts.test.ts",
    ],
    docs: ["docs/modules/kpi-definitions.md", "docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-106",
    statement: "Dashboard shows conversion rate",
    domain: "analytics-reports",
    backendTests: [
      "ContactEventsUpdateAnalyticsTests",
      "ConversionRateIsCalculatedCorrectlyTests",
      "CalculateConversionRateTests",
    ],
    frontendTests: [
      "src/features/analytics/contactEventsUpdateAnalytics.test.ts",
      "src/features/dashboard/dashboardCharts.test.ts",
    ],
    docs: ["docs/modules/kpi-definitions.md", "docs/modules/communication-tracking.md"],
  },
  {
    id: "FR-107",
    statement: "Dashboard shows estimated ROI",
    domain: "analytics-reports",
    backendTests: ["RoiIsCalculatedCorrectlyTests", "CalculateEstimatedRoiTests"],
    frontendTests: ["src/pages/DashboardPage.test.tsx", "src/pages/ExecutiveDashboardPage.test.tsx"],
    docs: ["docs/modules/kpi-definitions.md"],
  },
  {
    id: "FR-108",
    statement: "Users can view performance charts",
    domain: "analytics-reports",
    backendTests: ["CampaignAnalyticsEndpointTests", "ProductPerformanceEndpointTests"],
    frontendTests: [
      "src/features/analytics/analyticsCharts.test.ts",
      "src/components/charts/charts.test.tsx",
      "src/pages/AnalyticsPage.test.tsx",
    ],
    docs: ["docs/modules/analytics-module.md"],
  },
  {
    id: "FR-109",
    statement: "Users can export CSV reports",
    domain: "analytics-reports",
    backendTests: [
      "ReportExportIsRestrictedToAuthorizedRolesTests",
      "CsvExportWorksTests",
      "CampaignCsvReportEndpointTests",
      "ReportServiceTests",
      "UnauthorizedUserCannotExportRestrictedReportsTests",
    ],
    frontendTests: [
      "src/features/reports/reportExportIsRestrictedToAuthorizedRoles.test.ts",
      "src/features/reports/reportDownload.test.ts",
      "src/components/ReportDownloadPanel.test.tsx",
      "src/pages/ReportsPage.test.tsx",
    ],
    docs: ["docs/modules/report-export.md"],
  },
  {
    id: "FR-110",
    statement: "Users can generate PDF reports",
    domain: "analytics-reports",
    backendTests: [
      "ReportExportIsRestrictedToAuthorizedRolesTests",
      "PdfExportWorksTests",
      "CampaignPdfReportEndpointTests",
      "ReportServiceTests",
      "UnauthorizedUserCannotExportRestrictedReportsTests",
    ],
    frontendTests: [
      "src/features/reports/reportExportIsRestrictedToAuthorizedRoles.test.ts",
      "src/features/reports/reportDownload.test.ts",
      "src/components/ReportDownloadPanel.test.tsx",
    ],
    docs: ["docs/modules/report-export.md"],
  },

  // —— AI-assisted ——
  {
    id: "AI-001",
    statement: "Innovative customer search (fuzzy/weighted)",
    domain: "ai-assist",
    backendTests: ["AiSearchServiceTests", "AiControllerTests", "AiFeatureDocumentationTests"],
    frontendTests: ["src/api/ai.test.ts"],
    docs: ["docs/modules/ai-features.md", "docs/modules/ai-test-evidence.md"],
  },
  {
    id: "AI-002",
    statement: "Segment suggestions (rule-based)",
    domain: "ai-assist",
    backendTests: ["AiRecommendationServiceTests", "AiControllerTests"],
    frontendTests: ["src/components/AiRecommendationSections.test.tsx", "src/api/ai.test.ts"],
    docs: ["docs/modules/ai-features.md"],
  },
  {
    id: "AI-003",
    statement: "Product recommendations (rule-based)",
    domain: "ai-assist",
    backendTests: [
      "AiRecommendationServiceTests",
      "AiControllerTests",
      "AiRecommendationCannotBypassConsentRulesTests",
    ],
    frontendTests: [
      "src/features/ai/aiRecommendationCannotBypassConsentRules.test.ts",
      "src/components/AiRecommendationSections.test.tsx",
      "src/api/ai.test.ts",
    ],
    docs: [
      "docs/modules/ai-features.md",
      "docs/modules/ai-limitations-and-human-approval.md",
    ],
  },
  {
    id: "AI-004",
    statement: "Default-risk score",
    domain: "ai-assist",
    backendTests: ["AiRecommendationServiceTests", "AiDtoTests"],
    frontendTests: [
      "src/components/AiExplanationDisplay.test.tsx",
      "src/api/ai.test.ts",
    ],
    docs: [
      "docs/modules/ai-features.md",
      "docs/modules/ai-decision-support-explanation.md",
    ],
  },
  {
    id: "AI-005",
    statement: "Campaign copy suggestion (human-approved)",
    domain: "ai-assist",
    backendTests: [
      "AiGeneratedCampaignCopyRequiresHumanApprovalTests",
      "CampaignCopyServiceTests",
      "AiSupportsHumanDecisionMakingOnlyTests",
      "AiLimitationsAndHumanApprovalPolicyDocumentationTests",
    ],
    frontendTests: [
      "src/features/ai/aiGeneratedCampaignCopyRequiresHumanApproval.test.ts",
      "src/components/AiRecommendationSections.test.tsx",
      "src/api/ai.test.ts",
    ],
    docs: [
      "docs/modules/ai-limitations-and-human-approval.md",
      "docs/modules/ai-features.md",
    ],
  },
  {
    id: "AI-006",
    statement: "Duplicate-contact risk warning",
    domain: "ai-assist",
    backendTests: [
      "AiRecommendationServiceTests",
      "ConfigurableMonthlyContactLimitAiTests",
      "AiControllerTests",
    ],
    frontendTests: ["src/api/ai.test.ts", "src/components/AiExplanationDisplay.test.tsx"],
    docs: ["docs/modules/ai-features.md", "docs/modules/ai-test-evidence.md"],
  },
];

/** Expected FR and AI IDs in KB section 10–11 order. */
export const EXPECTED_FUNCTIONAL_REQUIREMENT_IDS: string[] = [
  "FR-001",
  "FR-002",
  "FR-003",
  "FR-004",
  "FR-005",
  "FR-010",
  "FR-011",
  "FR-012",
  "FR-013",
  "FR-014",
  "FR-015",
  "FR-016",
  "FR-017",
  "FR-018",
  "FR-019",
  "FR-020",
  "FR-030",
  "FR-031",
  "FR-032",
  "FR-033",
  "FR-034",
  "FR-040",
  "FR-041",
  "FR-042",
  "FR-043",
  "FR-044",
  "FR-045",
  "FR-046",
  "FR-050",
  "FR-051",
  "FR-052",
  "FR-053",
  "FR-054",
  "FR-055",
  "FR-056",
  "FR-057",
  "FR-058",
  "FR-059",
  "FR-060",
  "FR-061",
  "FR-062",
  "FR-070",
  "FR-071",
  "FR-072",
  "FR-073",
  "FR-074",
  "FR-075",
  "FR-076",
  "FR-077",
  "FR-078",
  "FR-079",
  "FR-080",
  "FR-081",
  "FR-082",
  "FR-083",
  "FR-084",
  "FR-085",
  "FR-086",
  "FR-087",
  "FR-088",
  "FR-089",
  "FR-090",
  "FR-091",
  "FR-092",
  "FR-093",
  "FR-094",
  "FR-095",
  "FR-096",
  "FR-097",
  "FR-100",
  "FR-101",
  "FR-102",
  "FR-103",
  "FR-104",
  "FR-105",
  "FR-106",
  "FR-107",
  "FR-108",
  "FR-109",
  "FR-110",
  "AI-001",
  "AI-002",
  "AI-003",
  "AI-004",
  "AI-005",
  "AI-006",
];

export function functionalRequirementIds(): string[] {
  return FUNCTIONAL_REQUIREMENT_MAPPINGS.map((m) => m.id);
}

export function getFunctionalRequirementMapping(
  id: string,
): FunctionalRequirementMapping | undefined {
  return FUNCTIONAL_REQUIREMENT_MAPPINGS.find((m) => m.id === id);
}

export function catalogIdsMatchExpectedOrder(): boolean {
  const ids = functionalRequirementIds();
  if (ids.length !== EXPECTED_FUNCTIONAL_REQUIREMENT_IDS.length) {
    return false;
  }
  return ids.every((id, i) => id === EXPECTED_FUNCTIONAL_REQUIREMENT_IDS[i]);
}

export function everyMappingHasEvidence(): boolean {
  return FUNCTIONAL_REQUIREMENT_MAPPINGS.every(
    (m) =>
      m.statement.trim().length > 0 &&
      m.backendTests.length > 0 &&
      m.frontendTests.length > 0 &&
      m.docs.length > 0,
  );
}

export function countByDomain(): Record<FunctionalRequirementDomain, number> {
  const counts: Record<FunctionalRequirementDomain, number> = {
    "auth-rbac": 0,
    customer: 0,
    "beneficiary-consent": 0,
    product: 0,
    campaign: 0,
    segmentation: 0,
    reminders: 0,
    "communication-followup": 0,
    "analytics-reports": 0,
    "ai-assist": 0,
  };
  for (const m of FUNCTIONAL_REQUIREMENT_MAPPINGS) {
    counts[m.domain] += 1;
  }
  return counts;
}

export function functionalRequirementsDocSectionIdsInOrder(): FunctionalRequirementsDocSectionId[] {
  return FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS.map((s) => s.id);
}

export function isValidFunctionalRequirementsDocSectionOrder(
  ids: FunctionalRequirementsDocSectionId[],
): boolean {
  const expected = functionalRequirementsDocSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, i) => id === expected[i]);
}

export function formatFunctionalRequirementsDocOutline(): string {
  return FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS.map(
    (s) => `${s.index}. ${s.title}`,
  ).join("\n");
}

export function documentationContainsRequiredSnippets(documentation: string): boolean {
  return FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentation.includes(snippet),
  );
}

export function docsIndexMustLinkFunctionalRequirementsMap(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("functional-requirements-test-map.md") &&
    indexMarkdown.includes("620")
  );
}

/** Happy-path E2E covers the critical FR journey end-to-end. */
export const HAPPY_PATH_FUNCTIONAL_REQUIREMENT_IDS: string[] = [
  "FR-001",
  "FR-011",
  "FR-033",
  "FR-041",
  "FR-077",
  "FR-050",
  "FR-059",
  "FR-060",
  "FR-100",
];

export const FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS: number[] = [
  619, 620, 621, 622, 647, 670, 617,
];
