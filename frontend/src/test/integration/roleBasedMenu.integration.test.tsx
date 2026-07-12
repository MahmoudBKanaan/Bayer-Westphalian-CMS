/**
 * Role-based menu visibility integration (KB item 607 / NFR-001).
 *
 * Full route tree: after auth, Main navigation only exposes links allowed for
 * the signed-in role(s); admin-only and builder links stay hidden when unauthorized.
 */
import { screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ADMIN_ONLY_MENU_LABELS,
  CAMPAIGN_BUILDER_MENU_LABEL,
  EXPECTED_MENU_LABELS_BY_ROLE,
  MAIN_NAV_ARIA_LABEL,
  NAV_EMPTY_TITLE,
  visibleMenuLabelsForRoles,
} from "@/features/auth/roleBasedMenu";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  createFetchRouter,
  defaultSystemSettings,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

function stubShellApis() {
  vi.stubGlobal(
    "fetch",
    createFetchRouter([
      {
        match: (url) => url.includes("/analytics/dashboard"),
        response: () => jsonOk(emptyDashboardPayload),
      },
      {
        match: (url) => url.includes("/analytics/executive"),
        response: () =>
          jsonOk({
            totalCampaigns: 0,
            activeCampaigns: 0,
            completedCampaigns: 0,
            totalAudience: 0,
            totalEligible: 0,
            totalExcluded: 0,
            totalSent: 0,
            totalOpened: 0,
            totalClicked: 0,
            totalReplied: 0,
            totalConverted: 0,
            overallOpenRate: 0,
            overallClickRate: 0,
            overallConversionRate: 0,
            totalEstimatedCost: 0,
            totalEstimatedRevenue: 0,
            overallEstimatedRoi: 0,
            productPerformance: [],
          }),
      },
      {
        match: (url) => url.includes("/system-settings"),
        response: () => jsonOk(defaultSystemSettings),
      },
      {
        match: (url) => url.includes("/audit-logs"),
        response: () => jsonOk([]),
      },
      {
        match: () => true,
        response: () => jsonOk([]),
      },
    ]),
  );
}

function navLabelsFromDom(): string[] {
  const nav = screen.getByLabelText(MAIN_NAV_ARIA_LABEL);
  return within(nav)
    .getAllByRole("link")
    .map((link) => link.textContent?.trim() ?? "")
    .filter((label) => label.length > 0);
}

describe("role-based menu hides unauthorized features (item 607)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("hides Users, Settings, and Builder for campaign managers", async () => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: ["CAMPAIGN_MANAGER"] });

    await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    expect(navLabelsFromDom()).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.CAMPAIGN_MANAGER);
    expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Settings" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: CAMPAIGN_BUILDER_MENU_LABEL })).toBeInTheDocument();
  });

  it("hides campaign builder and admin links for BI analysts", async () => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: ["BI_ANALYST"] });

    await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    expect(navLabelsFromDom()).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.BI_ANALYST);
    expect(screen.queryByRole("link", { name: CAMPAIGN_BUILDER_MENU_LABEL })).not.toBeInTheDocument();
    for (const label of ADMIN_ONLY_MENU_LABELS) {
      expect(screen.queryByRole("link", { name: label })).not.toBeInTheDocument();
    }
  });

  it("shows full administration menu only for admins", async () => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: ["ADMIN"] });

    await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    expect(navLabelsFromDom()).toEqual(visibleMenuLabelsForRoles(["ADMIN"]));
    expect(screen.getByRole("link", { name: "Users" })).toHaveAttribute("href", "/users");
    expect(screen.getByRole("link", { name: "Settings" })).toHaveAttribute("href", "/settings");
    expect(screen.getByRole("link", { name: "Audit" })).toBeInTheDocument();
  });

  it("shows compliance and audit without builder for compliance officers", async () => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: ["COMPLIANCE_OFFICER"] });

    await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    expect(navLabelsFromDom()).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.COMPLIANCE_OFFICER);
    expect(screen.getByRole("link", { name: "Compliance" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Audit" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: CAMPAIGN_BUILDER_MENU_LABEL })).not.toBeInTheDocument();
  });

  it.each([
    "CUSTOMER_SERVICE_AGENT",
    "SALES_AGENT",
    "MARKETING_ANALYST",
    "EXECUTIVE_VIEWER",
    "SYSTEM_AUDITOR",
  ] as SystemRoleName[])("matches expected menu labels for %s", async (role) => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: [role] });

    await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    expect(navLabelsFromDom()).toEqual(EXPECTED_MENU_LABELS_BY_ROLE[role]);
    expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Settings" })).not.toBeInTheDocument();
  });

  it("unions menus for multi-role users without exposing admin links", async () => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: ["PRODUCT_MANAGER", "COMPLIANCE_OFFICER"] });

    await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    const labels = navLabelsFromDom();
    expect(labels).toEqual(
      visibleMenuLabelsForRoles(["PRODUCT_MANAGER", "COMPLIANCE_OFFICER"]),
    );
    expect(labels).toContain("Products");
    expect(labels).toContain("Compliance");
    expect(labels).not.toContain("Users");
  });

  it("shows empty navigation when the user has no roles", async () => {
    stubShellApis();
    renderApp({ path: "/dashboard", roles: [] });

    const nav = await screen.findByLabelText(MAIN_NAV_ARIA_LABEL);
    expect(within(nav).getByText(NAV_EMPTY_TITLE)).toBeInTheDocument();
    expect(within(nav).queryByRole("link")).not.toBeInTheDocument();
  });
});
