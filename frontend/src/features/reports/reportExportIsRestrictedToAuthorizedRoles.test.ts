import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  AUTHORIZED_AUDIT_REPORT_EXPORT_ROLES,
  AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES,
  BACKEND_CRITICAL_TEST_CLASS,
  CAMPAIGN_REPORT_EXPORT_PATHS,
  COMPANION_UNAUTHORIZED_EXPORT_TEST_CLASS,
  REPORT_EXPORT_DOC_PATH,
  REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_FR,
  REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_ITEM,
  REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_NFR,
  REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_STATEMENT,
  UNAUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES,
  canExportCampaignReports,
  expectedReportExportHttpStatus,
  frontendReportReadRolesMatchAuthorizedExportRoles,
} from "@/features/reports/reportExportIsRestrictedToAuthorizedRoles";
import { REPORT_READ_ROLES } from "@/features/auth/permissions";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("reportExportIsRestrictedToAuthorizedRoles (item 663)", () => {
  it("locks the critical KB rule identity", () => {
    expect(REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_ITEM).toBe(663);
    expect(REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_STATEMENT).toBe(
      "Report export is restricted to authorized roles",
    );
    expect(REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_FR).toEqual(["FR-109", "FR-110"]);
    expect(REPORT_EXPORT_IS_RESTRICTED_TO_AUTHORIZED_ROLES_NFR).toEqual(["NFR-001"]);
    expect(AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES).toEqual([
      "ADMIN",
      "BI_ANALYST",
      "CAMPAIGN_MANAGER",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
    ]);
    expect(UNAUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES).toContain("PRODUCT_MANAGER");
    expect(UNAUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES).toContain("SYSTEM_AUDITOR");
    expect(AUTHORIZED_AUDIT_REPORT_EXPORT_ROLES).toEqual([
      "ADMIN",
      "COMPLIANCE_OFFICER",
      "SYSTEM_AUDITOR",
    ]);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "ReportExportIsRestrictedToAuthorizedRolesTests",
    );
    expect(COMPANION_UNAUTHORIZED_EXPORT_TEST_CLASS).toContain(
      "UnauthorizedUserCannotExportRestrictedReportsTests",
    );
    expect(CAMPAIGN_REPORT_EXPORT_PATHS[0]).toContain("/api/reports/campaigns");
  });

  it("allows only authorized roles and aligns with REPORT_READ_ROLES", () => {
    expect(canExportCampaignReports("BI_ANALYST")).toBe(true);
    expect(canExportCampaignReports("EXECUTIVE_VIEWER")).toBe(true);
    expect(canExportCampaignReports("PRODUCT_MANAGER")).toBe(false);
    expect(canExportCampaignReports("COMPLIANCE_OFFICER")).toBe(false);
    expect(canExportCampaignReports(null)).toBe(false);

    expect(frontendReportReadRolesMatchAuthorizedExportRoles()).toBe(true);
    expect(REPORT_READ_ROLES).toEqual([...AUTHORIZED_CAMPAIGN_REPORT_EXPORT_ROLES]);

    expect(expectedReportExportHttpStatus({ authenticated: false })).toBe(401);
    expect(
      expectedReportExportHttpStatus({ authenticated: true, role: "PRODUCT_MANAGER" }),
    ).toBe(403);
    expect(expectedReportExportHttpStatus({ authenticated: true, role: "BI_ANALYST" })).toBe(
      200,
    );
  });

  it("documents role-restricted report export in report-export module docs", () => {
    const docPath = path.join(repoRoot, REPORT_EXPORT_DOC_PATH);
    expect(existsSync(docPath)).toBe(true);
    const documentation = readRepoFile(REPORT_EXPORT_DOC_PATH);
    expect(documentation).toContain("663");
    expect(documentation).toContain("ReportExportIsRestrictedToAuthorizedRolesTests");
    expect(documentation).toMatch(/FR-109|FR-110/);
    expect(documentation).toMatch(/403|unauthorized|authorized/i);
    expect(documentation).toMatch(/BI_ANALYST|BI Analyst/i);
  });
});
