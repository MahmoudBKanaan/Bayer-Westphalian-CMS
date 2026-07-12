/**
 * Sprint 16 critical test item **663**: Report export is restricted to authorized roles.
 *
 * KB: FR-109 / FR-110 campaign CSV/PDF exports; NFR-001 RBAC. Backend
 * `AuthorizationExpressions.canViewReports` / `SecurityConfiguration.BI_CAMPAIGN_EXECUTIVE_ROLES`
 * and frontend `REPORT_READ_ROLES` must stay aligned.
 */

import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import { REPORT_READ_ROLES } from "@/features/auth/permissions";

export const REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_ITEM = 663;

export const REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_STATEMENT =
  "Report export is restricted to authorized roles";

export const REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_FR = [
  "FR-109",
  "FR-110",
] as const;

export const REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_NFR = ["NFR-001"] as const;

/** Campaign report export roles (must match backend REPORT_READ / canViewReports). */
export const AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES: readonly SystemRoleName[] = [
  "ADMIN",
  "BI_ANALYST",
  "CAMPAIGN_MANAGER",
  "MARKETING_ANALYST",
  "EXECUTIVE_VIEWER",
];

/** Roles that must not access campaign report export endpoints. */
export const UNAUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES: readonly SystemRoleName[] = [
  "PRODUCT_MANAGER",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "SYSTEM_AUDITOR",
];

/** Audit-history export roles (separate restricted surface from FR-109/110). */
export const AUTHORIZED_AUDIT_REPORT_EXPORT_ROLES: readonly SystemRoleName[] = [
  "ADMIN",
  "COMPLIANCE_OFFICER",
  "SYSTEM_AUDITOR",
];

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.report.ReportExportIsRestrictedToAuthorizedRolesTests";

export const COMPANION_UNAUTHORIZED_EXPORT_TEST_CLASS =
  "com.bayerwestphalian.campaign.report.UnauthorizedUserCannotExportRestrictedReportsTests";

export const REPORT_EXPORT_DOC_PATH = "docs/modules/report-export.md";

export const CAMPAIGN_REPORT_EXPORT_PATHS = [
  "/api/reports/campaigns/{campaignId}/csv",
  "/api/reports/campaigns/{campaignId}/pdf",
  "/api/reports/exports",
] as const;

/**
 * True when the role may export campaign CSV/PDF reports under KB FR-109/110.
 */
export function canExportCampaignReports(role: SystemRoleName | string | null | undefined): boolean {
  if (role == null) {
    return false;
  }
  return (AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES as readonly string[]).includes(role);
}

/**
 * True when frontend permission matrix matches the locked authorized export role set.
 */
export function frontendReportReadRolesMatchAuthorizedExportRoles(): boolean {
  if (REPORT_READ_ROLES.length !== AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES.length) {
    return false;
  }
  const locked = new Set(AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES);
  return REPORT_READ_ROLES.every((role) => locked.has(role));
}

/**
 * Expected HTTP status when a principal attempts campaign report export.
 */
export function expectedReportExportHttpStatus(options: {
  authenticated: boolean;
  role?: SystemRoleName | string | null;
}): 200 | 401 | 403 {
  if (!options.authenticated) {
    return 401;
  }
  if (canExportCampaignReports(options.role)) {
    return 200;
  }
  return 403;
}
