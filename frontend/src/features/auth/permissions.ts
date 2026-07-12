import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

export type RoleChecker = (roles: SystemRoleName[]) => boolean;

export const PRODUCT_MANAGE_ROLES: SystemRoleName[] = ["ADMIN", "PRODUCT_MANAGER"];

export const PRODUCT_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "PRODUCT_MANAGER",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "EXECUTIVE_VIEWER",
];

export const PRODUCT_OWNERSHIP_MANAGE_ROLES: SystemRoleName[] = ["ADMIN", "PRODUCT_MANAGER"];

export const CUSTOMER_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "PRODUCT_MANAGER",
];

export const CUSTOMER_CREATE_ROLES: SystemRoleName[] = ["ADMIN", "CUSTOMER_SERVICE_AGENT"];

export const CUSTOMER_UPDATE_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "COMPLIANCE_OFFICER",
];

export const CUSTOMER_DELETE_ROLES: SystemRoleName[] = ["ADMIN"];

export const CUSTOMER_IMPORT_ROLES: SystemRoleName[] = ["ADMIN", "CUSTOMER_SERVICE_AGENT"];

export const PAYMENT_RECORD_MANAGE_ROLES: SystemRoleName[] = ["ADMIN", "CUSTOMER_SERVICE_AGENT"];

export const PAYMENT_RECORD_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
];

export const CONSENT_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SYSTEM_AUDITOR",
];

export const BENEFICIARY_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SYSTEM_AUDITOR",
];

export const CAMPAIGN_MANAGE_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];
export const CAMPAIGN_REVIEW_ROLES: SystemRoleName[] = ["ADMIN", "COMPLIANCE_OFFICER"];

export const CONTACT_HISTORY_MANAGE_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
];

export const FOLLOW_UP_TASK_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "CAMPAIGN_MANAGER",
];

export const FOLLOW_UP_TASK_CREATE_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "CAMPAIGN_MANAGER",
];

export const FOLLOW_UP_TASK_ASSIGN_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "CAMPAIGN_MANAGER",
];

export const REMINDER_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "COMPLIANCE_OFFICER",
];

export const REMINDER_MANAGE_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

export const REMINDER_MANUAL_TRIGGER_ROLES: SystemRoleName[] = ["ADMIN"];

export const CAMPAIGN_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "PRODUCT_MANAGER",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "EXECUTIVE_VIEWER",
  "SYSTEM_AUDITOR",
];

/**
 * Matches backend @authz.canManageSegments() — edit/delete saved segments.
 * BI_ANALYST is excluded unless the user also holds ADMIN or CAMPAIGN_MANAGER (item 200).
 */
export const SEGMENT_MANAGE_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

/**
 * Matches backend @authz.canCreateSegments() — FR-077 / item 201: Campaign Manager (and Admin)
 * can create reusable audience segments.
 */
export const SEGMENT_CREATE_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

/** Matches backend @authz.canReadSegments() */
export const SEGMENT_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "COMPLIANCE_OFFICER",
];

/** Matches backend @authz.canPreviewSegments() */
export const SEGMENT_PREVIEW_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER", "BI_ANALYST"];

/**
 * Matches backend analytics/report read roles for {@code GET /api/analytics/**}
 * (Admin, BI Analyst, Campaign Manager, Marketing Analyst, Executive Viewer).
 */
export const ANALYTICS_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "BI_ANALYST",
  "CAMPAIGN_MANAGER",
  "MARKETING_ANALYST",
  "EXECUTIVE_VIEWER",
];

/**
 * Matches backend report export roles for {@code GET /api/reports/**}
 * (same matrix as analytics: Admin, BI Analyst, Campaign Manager, Marketing Analyst,
 * Executive Viewer).
 */
export const REPORT_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "BI_ANALYST",
  "CAMPAIGN_MANAGER",
  "MARKETING_ANALYST",
  "EXECUTIVE_VIEWER",
];

/**
 * Matches backend audit-log read roles for {@code GET /api/audit-logs/**}
 * (Admin, Compliance Officer, System Auditor — KB E22 / item 532).
 */
export const AUDIT_READ_ROLES: SystemRoleName[] = [
  "ADMIN",
  "COMPLIANCE_OFFICER",
  "SYSTEM_AUDITOR",
];

/**
 * Matches backend System Settings manage roles for {@code /api/system-settings/**}
 * (Admin only — KB item 534).
 */
export const SYSTEM_SETTINGS_MANAGE_ROLES: SystemRoleName[] = ["ADMIN"];

export const AI_SEGMENT_RECOMMENDATION_ROLES: SystemRoleName[] = [
  "BI_ANALYST",
  "CAMPAIGN_MANAGER",
];

export const AI_PRODUCT_RECOMMENDATION_ROLES: SystemRoleName[] = [
  "BI_ANALYST",
  "CAMPAIGN_MANAGER",
];

export const AI_CAMPAIGN_COPY_ROLES: SystemRoleName[] = ["CAMPAIGN_MANAGER"];

export function createPermissionChecks(hasAnyRole: RoleChecker) {
  return {
    canManageProducts: () => hasAnyRole(PRODUCT_MANAGE_ROLES),
    canReadProducts: () => hasAnyRole(PRODUCT_READ_ROLES),
    canManageProductOwnership: () => hasAnyRole(PRODUCT_OWNERSHIP_MANAGE_ROLES),
    canReadCustomers: () => hasAnyRole(CUSTOMER_READ_ROLES),
    canCreateCustomers: () => hasAnyRole(CUSTOMER_CREATE_ROLES),
    canUpdateCustomers: () => hasAnyRole(CUSTOMER_UPDATE_ROLES),
    canDeleteCustomers: () => hasAnyRole(CUSTOMER_DELETE_ROLES),
    canImportCustomers: () => hasAnyRole(CUSTOMER_IMPORT_ROLES),
    canReadPaymentRecords: () => hasAnyRole(PAYMENT_RECORD_READ_ROLES),
    canManagePaymentRecords: () => hasAnyRole(PAYMENT_RECORD_MANAGE_ROLES),
    canReadConsentRecords: () => hasAnyRole(CONSENT_READ_ROLES),
    canReadBeneficiaries: () => hasAnyRole(BENEFICIARY_READ_ROLES),
    canManageCampaigns: () => hasAnyRole(CAMPAIGN_MANAGE_ROLES),
    canReviewCampaigns: () => hasAnyRole(CAMPAIGN_REVIEW_ROLES),
    canReadCampaigns: () => hasAnyRole(CAMPAIGN_READ_ROLES),
    canManageSegments: () => hasAnyRole(SEGMENT_MANAGE_ROLES),
    /** Campaign Manager + Admin may create reusable audience segments (KB FR-077). */
    canCreateSegments: () => hasAnyRole(SEGMENT_CREATE_ROLES),
    canReadSegments: () => hasAnyRole(SEGMENT_READ_ROLES),
    canPreviewSegments: () => hasAnyRole(SEGMENT_PREVIEW_ROLES),
    canManageContactHistory: () => hasAnyRole(CONTACT_HISTORY_MANAGE_ROLES),
    canReadFollowUpTasks: () => hasAnyRole(FOLLOW_UP_TASK_READ_ROLES),
    canCreateFollowUpTasks: () => hasAnyRole(FOLLOW_UP_TASK_CREATE_ROLES),
    canAssignFollowUpTasks: () => hasAnyRole(FOLLOW_UP_TASK_ASSIGN_ROLES),
    canReadReminders: () => hasAnyRole(REMINDER_READ_ROLES),
    canManageReminders: () => hasAnyRole(REMINDER_MANAGE_ROLES),
    canManuallyTriggerReminderProcessing: () => hasAnyRole(REMINDER_MANUAL_TRIGGER_ROLES),
    /** Dashboard / analytics KPI screens (KB item 440 / FR-100–FR-107). */
    canViewAnalytics: () => hasAnyRole(ANALYTICS_READ_ROLES),
    /**
     * Executive aggregate dashboard (KB item 443 / COMP-010).
     * Same analytics read matrix as backend {@code GET /api/analytics/executive}.
     */
    canViewExecutiveDashboard: () => hasAnyRole(ANALYTICS_READ_ROLES),
    /** Reports screen CSV/PDF exports and export history (KB item 442 / FR-109–FR-110). */
    canViewReports: () => hasAnyRole(REPORT_READ_ROLES),
    /** Alias for exporting restricted campaign reports (same as canViewReports). */
    canExportReports: () => hasAnyRole(REPORT_READ_ROLES),
    /**
     * Audit Log screen — sensitive action history (KB item 532 / E22 / COMP-008).
     * Read-only; no UI mutates audit rows.
     */
    canViewAuditLogs: () => hasAnyRole(AUDIT_READ_ROLES),
    /**
     * System Settings screen — contact limits and exclusion configuration (KB item 534).
     * Admin only.
     */
    canManageSystemSettings: () => hasAnyRole(SYSTEM_SETTINGS_MANAGE_ROLES),
    /** AI-001 customer search and AI-006 duplicate-contact warnings. */
    canUseAiCustomerSignals: () => hasAnyRole(CUSTOMER_READ_ROLES),
    /** AI-002 segment suggestions. */
    canUseAiSegmentSuggestions: () => hasAnyRole(AI_SEGMENT_RECOMMENDATION_ROLES),
    /** AI-003 product recommendations. */
    canUseAiProductRecommendations: () => hasAnyRole(AI_PRODUCT_RECOMMENDATION_ROLES),
    /** AI-005 campaign copy generation. */
    canUseAiCampaignCopy: () => hasAnyRole(AI_CAMPAIGN_COPY_ROLES),
  };
}

export type PermissionChecks = ReturnType<typeof createPermissionChecks>;
