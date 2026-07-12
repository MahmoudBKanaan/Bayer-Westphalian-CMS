import { describe, expect, it } from "vitest";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  ANALYTICS_READ_ROLES,
  AI_CAMPAIGN_COPY_ROLES,
  AI_PRODUCT_RECOMMENDATION_ROLES,
  AI_SEGMENT_RECOMMENDATION_ROLES,
  CAMPAIGN_MANAGE_ROLES,
  CAMPAIGN_READ_ROLES,
  CAMPAIGN_REVIEW_ROLES,
  FOLLOW_UP_TASK_ASSIGN_ROLES,
  FOLLOW_UP_TASK_CREATE_ROLES,
  FOLLOW_UP_TASK_READ_ROLES,
  REMINDER_MANAGE_ROLES,
  REMINDER_MANUAL_TRIGGER_ROLES,
  REMINDER_READ_ROLES,
  AUDIT_READ_ROLES,
  REPORT_READ_ROLES,
  SEGMENT_CREATE_ROLES,
  SYSTEM_SETTINGS_MANAGE_ROLES,
  createPermissionChecks,
} from "@/features/auth/permissions";

function checkerFor(roles: SystemRoleName[]) {
  return (requiredRoles: SystemRoleName[]) => requiredRoles.some((role) => roles.includes(role));
}

describe("product manager permissions", () => {
  const permissions = createPermissionChecks(checkerFor(["PRODUCT_MANAGER"]));

  it("allows product creation, editing, disabling, and customer assignment", () => {
    expect(permissions.canManageProducts()).toBe(true);
    expect(permissions.canReadProducts()).toBe(true);
    expect(permissions.canManageProductOwnership()).toBe(true);
  });

  it("allows read-only customer access for ownership workflows", () => {
    expect(permissions.canReadCustomers()).toBe(true);
    expect(permissions.canCreateCustomers()).toBe(false);
    expect(permissions.canUpdateCustomers()).toBe(false);
    expect(permissions.canDeleteCustomers()).toBe(false);
    expect(permissions.canImportCustomers()).toBe(false);
  });

  it("denies payment, consent, beneficiary, and campaign management access", () => {
    expect(permissions.canReadPaymentRecords()).toBe(false);
    expect(permissions.canManagePaymentRecords()).toBe(false);
    expect(permissions.canReadConsentRecords()).toBe(false);
    expect(permissions.canReadBeneficiaries()).toBe(false);
    expect(permissions.canManageCampaigns()).toBe(false);
    expect(permissions.canReviewCampaigns()).toBe(false);
  });

  it("allows read-only campaign visibility", () => {
    expect(permissions.canReadCampaigns()).toBe(true);
    expect(permissions.canReviewCampaigns()).toBe(false);
  });
});

describe("campaign review permissions", () => {
  it("allows admins and compliance officers to approve or reject campaigns", () => {
    expect(createPermissionChecks(checkerFor(["ADMIN"])).canReviewCampaigns()).toBe(true);
    expect(createPermissionChecks(checkerFor(["COMPLIANCE_OFFICER"])).canReviewCampaigns()).toBe(
      true,
    );
  });

  it("denies campaign review actions to read-only campaign roles", () => {
    expect(createPermissionChecks(checkerFor(["BI_ANALYST"])).canReviewCampaigns()).toBe(false);
    expect(createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"])).canReviewCampaigns()).toBe(
      false,
    );
  });
});

describe("role-based campaign permissions", () => {
  it("matches the KB campaign manage role matrix", () => {
    expect(CAMPAIGN_MANAGE_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);

    for (const role of CAMPAIGN_MANAGE_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canManageCampaigns()).toBe(true);
      expect(permissions.canReadCampaigns()).toBe(true);
    }
  });

  it("matches the KB campaign compliance review role matrix", () => {
    expect(CAMPAIGN_REVIEW_ROLES).toEqual(["ADMIN", "COMPLIANCE_OFFICER"]);

    for (const role of CAMPAIGN_REVIEW_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canReviewCampaigns()).toBe(true);
      expect(permissions.canReadCampaigns()).toBe(true);
    }
  });

  it("allows read-only campaign visibility without write or review actions", () => {
    const readOnlyRoles = CAMPAIGN_READ_ROLES.filter(
      (role) => !CAMPAIGN_MANAGE_ROLES.includes(role) && !CAMPAIGN_REVIEW_ROLES.includes(role),
    );

    expect(readOnlyRoles).toEqual([
      "BI_ANALYST",
      "PRODUCT_MANAGER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "EXECUTIVE_VIEWER",
      "SYSTEM_AUDITOR",
    ]);

    for (const role of readOnlyRoles) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canReadCampaigns()).toBe(true);
      expect(permissions.canManageCampaigns()).toBe(false);
      expect(permissions.canReviewCampaigns()).toBe(false);
    }
  });

  it("denies all campaign permissions to roles outside the campaign matrix", () => {
    const permissions = createPermissionChecks(checkerFor(["MARKETING_ANALYST"]));

    expect(permissions.canReadCampaigns()).toBe(false);
    expect(permissions.canManageCampaigns()).toBe(false);
    expect(permissions.canReviewCampaigns()).toBe(false);
  });
});

describe("unauthorized product creation", () => {
  it("denies product management for roles without create access", () => {
    const biAnalyst = createPermissionChecks(checkerFor(["BI_ANALYST"]));
    const campaignManager = createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"]));
    const customerServiceAgent = createPermissionChecks(checkerFor(["CUSTOMER_SERVICE_AGENT"]));
    const complianceOfficer = createPermissionChecks(checkerFor(["COMPLIANCE_OFFICER"]));

    expect(biAnalyst.canManageProducts()).toBe(false);
    expect(campaignManager.canManageProducts()).toBe(false);
    expect(customerServiceAgent.canManageProducts()).toBe(false);
    expect(complianceOfficer.canManageProducts()).toBe(false);
  });

  it("still allows read-only product catalog access where permitted", () => {
    const biAnalyst = createPermissionChecks(checkerFor(["BI_ANALYST"]));
    const campaignManager = createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"]));

    expect(biAnalyst.canReadProducts()).toBe(true);
    expect(campaignManager.canReadProducts()).toBe(true);
  });
});

describe("customer service agent permissions", () => {
  const permissions = createPermissionChecks(checkerFor(["CUSTOMER_SERVICE_AGENT"]));

  it("allows payment record management on customer profiles", () => {
    expect(permissions.canReadPaymentRecords()).toBe(true);
    expect(permissions.canManagePaymentRecords()).toBe(true);
    expect(permissions.canReadCustomers()).toBe(true);
  });

  it("denies product and campaign management access", () => {
    expect(permissions.canManageProducts()).toBe(false);
    expect(permissions.canManageProductOwnership()).toBe(false);
    expect(permissions.canManageCampaigns()).toBe(false);
  });
});

describe("follow-up task permissions", () => {
  it("matches the KB follow-up read role matrix", () => {
    expect(FOLLOW_UP_TASK_READ_ROLES).toEqual([
      "ADMIN",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "CAMPAIGN_MANAGER",
    ]);

    for (const role of FOLLOW_UP_TASK_READ_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canReadFollowUpTasks()).toBe(true);
    }
  });

  it("denies follow-up task access outside the KB matrix", () => {
    for (const role of [
      "PRODUCT_MANAGER",
      "BI_ANALYST",
      "COMPLIANCE_OFFICER",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
      "SYSTEM_AUDITOR",
    ] as const) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canReadFollowUpTasks()).toBe(false);
    }
  });

  it("allows KB create roles to create follow-up tasks", () => {
    expect(FOLLOW_UP_TASK_CREATE_ROLES).toEqual([
      "ADMIN",
      "CUSTOMER_SERVICE_AGENT",
      "CAMPAIGN_MANAGER",
    ]);

    for (const role of FOLLOW_UP_TASK_CREATE_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canCreateFollowUpTasks()).toBe(true);
    }
    expect(createPermissionChecks(checkerFor(["SALES_AGENT"])).canCreateFollowUpTasks()).toBe(
      false,
    );
  });

  it("allows KB assignment roles to assign follow-up tasks", () => {
    expect(FOLLOW_UP_TASK_ASSIGN_ROLES).toEqual([
      "ADMIN",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "CAMPAIGN_MANAGER",
    ]);

    for (const role of FOLLOW_UP_TASK_ASSIGN_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canAssignFollowUpTasks()).toBe(true);
    }
    expect(createPermissionChecks(checkerFor(["PRODUCT_MANAGER"])).canAssignFollowUpTasks()).toBe(
      false,
    );
  });
});

describe("reminder permissions", () => {
  it("matches the KB reminder read role matrix", () => {
    expect(REMINDER_READ_ROLES).toEqual([
      "ADMIN",
      "CAMPAIGN_MANAGER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "COMPLIANCE_OFFICER",
    ]);

    for (const role of REMINDER_READ_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canReadReminders()).toBe(true);
    }
  });

  it("allows only admins and campaign managers to manage reminder schedules", () => {
    expect(REMINDER_MANAGE_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);

    for (const role of REMINDER_MANAGE_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canManageReminders()).toBe(true);
      expect(permissions.canReadReminders()).toBe(true);
    }
  });

  it("allows only admins to use the manual reminder trigger", () => {
    expect(REMINDER_MANUAL_TRIGGER_ROLES).toEqual(["ADMIN"]);
    expect(
      createPermissionChecks(checkerFor(["ADMIN"])).canManuallyTriggerReminderProcessing(),
    ).toBe(true);
    expect(
      createPermissionChecks(
        checkerFor(["CAMPAIGN_MANAGER"]),
      ).canManuallyTriggerReminderProcessing(),
    ).toBe(false);
  });

  it("denies reminder access outside the KB screen access matrix", () => {
    for (const role of [
      "PRODUCT_MANAGER",
      "BI_ANALYST",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
      "SYSTEM_AUDITOR",
    ] as const) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canReadReminders()).toBe(false);
      expect(permissions.canManageReminders()).toBe(false);
      expect(permissions.canManuallyTriggerReminderProcessing()).toBe(false);
    }
  });
});

describe("AI recommendation permissions", () => {
  it("matches KB AI segment and product recommendation roles", () => {
    expect(AI_SEGMENT_RECOMMENDATION_ROLES).toEqual(["BI_ANALYST", "CAMPAIGN_MANAGER"]);
    expect(AI_PRODUCT_RECOMMENDATION_ROLES).toEqual(["BI_ANALYST", "CAMPAIGN_MANAGER"]);

    expect(createPermissionChecks(checkerFor(["BI_ANALYST"])).canUseAiSegmentSuggestions()).toBe(
      true,
    );
    expect(
      createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"])).canUseAiProductRecommendations(),
    ).toBe(true);
    expect(
      createPermissionChecks(checkerFor(["EXECUTIVE_VIEWER"])).canUseAiProductRecommendations(),
    ).toBe(false);
  });

  it("limits campaign copy generation to campaign managers", () => {
    expect(AI_CAMPAIGN_COPY_ROLES).toEqual(["CAMPAIGN_MANAGER"]);
    expect(createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"])).canUseAiCampaignCopy()).toBe(
      true,
    );
    expect(createPermissionChecks(checkerFor(["BI_ANALYST"])).canUseAiCampaignCopy()).toBe(false);
  });

  it("uses customer-read roles for customer signals and duplicate warnings", () => {
    expect(createPermissionChecks(checkerFor(["CUSTOMER_SERVICE_AGENT"])).canUseAiCustomerSignals())
      .toBe(true);
    expect(createPermissionChecks(checkerFor(["EXECUTIVE_VIEWER"])).canUseAiCustomerSignals()).toBe(
      false,
    );
  });
});

describe("analytics / dashboard permissions (item 440)", () => {
  it("allows analytics roles to view dashboard KPIs", () => {
    expect(ANALYTICS_READ_ROLES).toEqual([
      "ADMIN",
      "BI_ANALYST",
      "CAMPAIGN_MANAGER",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
    ]);

    for (const role of ANALYTICS_READ_ROLES) {
      expect(createPermissionChecks(checkerFor([role])).canViewAnalytics()).toBe(true);
    }
  });

  it("denies dashboard analytics outside the analytics read matrix", () => {
    for (const role of [
      "PRODUCT_MANAGER",
      "COMPLIANCE_OFFICER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "SYSTEM_AUDITOR",
    ] as const) {
      expect(createPermissionChecks(checkerFor([role])).canViewAnalytics()).toBe(false);
    }
  });
});

describe("audit log permissions (item 532)", () => {
  it("matches the KB audit read role matrix", () => {
    expect(AUDIT_READ_ROLES).toEqual(["ADMIN", "COMPLIANCE_OFFICER", "SYSTEM_AUDITOR"]);

    for (const role of AUDIT_READ_ROLES) {
      expect(createPermissionChecks(checkerFor([role])).canViewAuditLogs()).toBe(true);
    }
  });

  it("denies Audit Log screen access outside Admin, Compliance, and System Auditor", () => {
    for (const role of [
      "CAMPAIGN_MANAGER",
      "BI_ANALYST",
      "PRODUCT_MANAGER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
    ] as const) {
      expect(createPermissionChecks(checkerFor([role])).canViewAuditLogs()).toBe(false);
    }
  });
});

describe("system settings permissions (item 534)", () => {
  it("allows only admins to manage system settings", () => {
    expect(SYSTEM_SETTINGS_MANAGE_ROLES).toEqual(["ADMIN"]);
    expect(createPermissionChecks(checkerFor(["ADMIN"])).canManageSystemSettings()).toBe(true);
  });

  it("denies System Settings outside the admin role", () => {
    for (const role of [
      "CAMPAIGN_MANAGER",
      "COMPLIANCE_OFFICER",
      "SYSTEM_AUDITOR",
      "BI_ANALYST",
      "PRODUCT_MANAGER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
    ] as const) {
      expect(createPermissionChecks(checkerFor([role])).canManageSystemSettings()).toBe(false);
    }
  });
});

describe("executive dashboard permissions (item 443)", () => {
  it("allows analytics roles to view the executive aggregate dashboard", () => {
    for (const role of ANALYTICS_READ_ROLES) {
      expect(createPermissionChecks(checkerFor([role])).canViewExecutiveDashboard()).toBe(true);
    }
  });

  it("denies executive dashboard outside the analytics read matrix", () => {
    for (const role of [
      "PRODUCT_MANAGER",
      "COMPLIANCE_OFFICER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "SYSTEM_AUDITOR",
    ] as const) {
      expect(createPermissionChecks(checkerFor([role])).canViewExecutiveDashboard()).toBe(false);
    }
  });
});

describe("reports permissions (item 442)", () => {
  it("allows report roles to view and export campaign reports", () => {
    expect(REPORT_READ_ROLES).toEqual([
      "ADMIN",
      "BI_ANALYST",
      "CAMPAIGN_MANAGER",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
    ]);

    for (const role of REPORT_READ_ROLES) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canViewReports()).toBe(true);
      expect(permissions.canExportReports()).toBe(true);
    }
  });

  it("denies reports outside the report read matrix", () => {
    for (const role of [
      "PRODUCT_MANAGER",
      "COMPLIANCE_OFFICER",
      "CUSTOMER_SERVICE_AGENT",
      "SALES_AGENT",
      "SYSTEM_AUDITOR",
    ] as const) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canViewReports()).toBe(false);
      expect(permissions.canExportReports()).toBe(false);
    }
  });
});

describe("segment permissions", () => {
  it("allows campaign managers to create, manage, and preview segments", () => {
    const permissions = createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"]));
    expect(permissions.canCreateSegments()).toBe(true);
    expect(permissions.canManageSegments()).toBe(true);
    expect(permissions.canReadSegments()).toBe(true);
    expect(permissions.canPreviewSegments()).toBe(true);
  });

  it("grants campaign manager segment creation permissions per KB FR-077", () => {
    const permissions = createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"]));
    expect(permissions.canCreateSegments()).toBe(true);
    expect(SEGMENT_CREATE_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);
  });

  it("allows campaign manager to create reusable segments (item 201)", () => {
    const permissions = createPermissionChecks(checkerFor(["CAMPAIGN_MANAGER"]));
    expect(permissions.canCreateSegments()).toBe(true);
    expect(permissions.canManageSegments()).toBe(true);
    expect(SEGMENT_CREATE_ROLES).toContain("CAMPAIGN_MANAGER");
  });

  it("denies sales and marketing roles segment creation", () => {
    for (const role of [
      "SALES_AGENT",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
      "SYSTEM_AUDITOR",
    ] as const) {
      const permissions = createPermissionChecks(checkerFor([role]));
      expect(permissions.canCreateSegments()).toBe(false);
    }
  });

  it("allows BI analysts read and preview but not create or manage", () => {
    const permissions = createPermissionChecks(checkerFor(["BI_ANALYST"]));
    expect(permissions.canCreateSegments()).toBe(false);
    expect(permissions.canManageSegments()).toBe(false);
    expect(permissions.canReadSegments()).toBe(true);
    expect(permissions.canPreviewSegments()).toBe(true);
  });

  it("denies BI analyst segment edit unless also granted a manage role (item 200)", () => {
    const biOnly = createPermissionChecks(checkerFor(["BI_ANALYST"]));
    expect(biOnly.canManageSegments()).toBe(false);
    expect(biOnly.canCreateSegments()).toBe(false);

    const biWithCampaignManager = createPermissionChecks(
      checkerFor(["BI_ANALYST", "CAMPAIGN_MANAGER"]),
    );
    expect(biWithCampaignManager.canManageSegments()).toBe(true);
    expect(biWithCampaignManager.canCreateSegments()).toBe(true);

    const biWithAdmin = createPermissionChecks(checkerFor(["BI_ANALYST", "ADMIN"]));
    expect(biWithAdmin.canManageSegments()).toBe(true);
    expect(biWithAdmin.canCreateSegments()).toBe(true);
  });

  it("allows compliance officers read-only segment access", () => {
    const permissions = createPermissionChecks(checkerFor(["COMPLIANCE_OFFICER"]));
    expect(permissions.canCreateSegments()).toBe(false);
    expect(permissions.canManageSegments()).toBe(false);
    expect(permissions.canReadSegments()).toBe(true);
    expect(permissions.canPreviewSegments()).toBe(false);
  });

  it("denies product managers segment access", () => {
    const permissions = createPermissionChecks(checkerFor(["PRODUCT_MANAGER"]));
    expect(permissions.canCreateSegments()).toBe(false);
    expect(permissions.canManageSegments()).toBe(false);
    expect(permissions.canReadSegments()).toBe(false);
    expect(permissions.canPreviewSegments()).toBe(false);
  });

  it.each([
    "CUSTOMER_SERVICE_AGENT",
    "SALES_AGENT",
    "MARKETING_ANALYST",
    "EXECUTIVE_VIEWER",
    "SYSTEM_AUDITOR",
  ] as const)("denies segment creation for %s", (role) => {
    const permissions = createPermissionChecks(checkerFor([role]));
    expect(permissions.canCreateSegments()).toBe(false);
  });
});

describe("admin permissions", () => {
  const permissions = createPermissionChecks(checkerFor(["ADMIN"]));

  it("allows full segment management", () => {
    expect(permissions.canCreateSegments()).toBe(true);
    expect(permissions.canManageSegments()).toBe(true);
    expect(permissions.canReadSegments()).toBe(true);
    expect(permissions.canPreviewSegments()).toBe(true);
  });

  it("allows full product and customer management", () => {
    expect(permissions.canManageProducts()).toBe(true);
    expect(permissions.canManageProductOwnership()).toBe(true);
    expect(permissions.canReadCustomers()).toBe(true);
    expect(permissions.canCreateCustomers()).toBe(true);
    expect(permissions.canUpdateCustomers()).toBe(true);
    expect(permissions.canDeleteCustomers()).toBe(true);
    expect(permissions.canManagePaymentRecords()).toBe(true);
    expect(permissions.canManageCampaigns()).toBe(true);
    expect(permissions.canReviewCampaigns()).toBe(true);
  });
});
