import { describe, expect, it } from "vitest";
import {
  ADMIN_ONLY_MENU_LABELS,
  CAMPAIGN_BUILDER_MENU_LABEL,
  EXPECTED_MENU_LABELS_BY_ROLE,
  filterNavMenuSections,
  formatRoleBasedMenuJourney,
  isAdminOnlyMenuLabel,
  isMenuLabelVisibleForRoles,
  isValidRoleBasedMenuOrder,
  NAV_MENU_SECTIONS,
  roleBasedMenuStepIdsInOrder,
  roleHasMenuAccess,
  visibleMenuLabelsForRoles,
} from "@/features/auth/roleBasedMenu";

describe("roleBasedMenu (item 607)", () => {
  it("documents the UI role-based menu journey", () => {
    expect(roleBasedMenuStepIdsInOrder()).toEqual([
      "authenticate",
      "resolve-roles",
      "filter-nav-items",
      "render-visible-menu",
    ]);
    expect(formatRoleBasedMenuJourney()).toBe(
      "Authenticate → Resolve roles → Filter nav items → Render visible menu",
    );
    expect(isValidRoleBasedMenuOrder(roleBasedMenuStepIdsInOrder())).toBe(true);
  });

  it("hides admin-only Users and Settings from non-admins", () => {
    expect(isAdminOnlyMenuLabel("Users")).toBe(true);
    expect(isAdminOnlyMenuLabel("Settings")).toBe(true);
    for (const label of ADMIN_ONLY_MENU_LABELS) {
      expect(isMenuLabelVisibleForRoles(label, ["CAMPAIGN_MANAGER"])).toBe(false);
      expect(isMenuLabelVisibleForRoles(label, ["BI_ANALYST"])).toBe(false);
      expect(isMenuLabelVisibleForRoles(label, ["COMPLIANCE_OFFICER"])).toBe(false);
      expect(isMenuLabelVisibleForRoles(label, ["ADMIN"])).toBe(true);
    }
  });

  it("hides Campaign Builder from roles that cannot manage campaigns", () => {
    expect(isMenuLabelVisibleForRoles(CAMPAIGN_BUILDER_MENU_LABEL, ["ADMIN"])).toBe(true);
    expect(isMenuLabelVisibleForRoles(CAMPAIGN_BUILDER_MENU_LABEL, ["CAMPAIGN_MANAGER"])).toBe(
      true,
    );
    expect(isMenuLabelVisibleForRoles(CAMPAIGN_BUILDER_MENU_LABEL, ["BI_ANALYST"])).toBe(false);
    expect(isMenuLabelVisibleForRoles(CAMPAIGN_BUILDER_MENU_LABEL, ["PRODUCT_MANAGER"])).toBe(
      false,
    );
    expect(isMenuLabelVisibleForRoles(CAMPAIGN_BUILDER_MENU_LABEL, ["COMPLIANCE_OFFICER"])).toBe(
      false,
    );
  });

  it("matches KB single-role menu visibility matrices", () => {
    expect(EXPECTED_MENU_LABELS_BY_ROLE.CAMPAIGN_MANAGER).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Segments",
      "Campaigns",
      "Builder",
      "Compliance",
      "Contact history",
      "Follow-ups",
      "Reminders",
      "Analytics",
      "Executive",
      "Reports",
    ]);
    expect(EXPECTED_MENU_LABELS_BY_ROLE.BI_ANALYST).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Change requests",
      "Segments",
      "Contact history",
      "Analytics",
      "Executive",
      "Reports",
    ]);
    expect(EXPECTED_MENU_LABELS_BY_ROLE.MARKETING_ANALYST).toEqual([
      "Dashboard",
      "Analytics",
      "Executive",
      "Reports",
    ]);
    expect(EXPECTED_MENU_LABELS_BY_ROLE.SYSTEM_AUDITOR).toEqual([
      "Dashboard",
      "Customers",
      "Contact history",
      "Audit",
    ]);
  });

  it("unions menu labels across multiple roles while preserving nav order", () => {
    const labels = visibleMenuLabelsForRoles(["PRODUCT_MANAGER", "COMPLIANCE_OFFICER"]);
    expect(labels).toContain("Products");
    expect(labels).toContain("Compliance");
    expect(labels).toContain("Audit");
    expect(labels).not.toContain("Users");
    expect(labels).not.toContain("Builder");
    // Order follows NAV_MENU_SECTIONS, not input role order.
    expect(labels.indexOf("Products")).toBeLessThan(labels.indexOf("Compliance"));
  });

  it("drops empty sections when no items remain for a role", () => {
    const sections = filterNavMenuSections(["MARKETING_ANALYST"]);
    expect(sections.map((section) => section.label)).toEqual(["Workspace", "Insights"]);
    expect(sections.some((section) => section.label === "Administration")).toBe(false);
    expect(sections.some((section) => section.label === "Campaign Operations")).toBe(false);
  });

  it("returns no sections for users with no roles", () => {
    expect(filterNavMenuSections([])).toEqual([]);
    expect(visibleMenuLabelsForRoles([])).toEqual([]);
  });

  it("checks allow-lists with role intersection", () => {
    expect(roleHasMenuAccess(["BI_ANALYST"], ["ADMIN", "BI_ANALYST"])).toBe(true);
    expect(roleHasMenuAccess(["SALES_AGENT"], ["ADMIN", "BI_ANALYST"])).toBe(false);
  });

  it("defines a complete nav tree with unique labels", () => {
    const labels = NAV_MENU_SECTIONS.flatMap((section) => section.items.map((item) => item.label));
    expect(new Set(labels).size).toBe(labels.length);
    expect(labels).toContain("Dashboard");
    expect(labels).toContain("Users");
  });
});
