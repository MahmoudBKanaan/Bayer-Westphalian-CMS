import { screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createFetchRouter,
  defaultSystemSettings,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

function stubDashboardAndEmptyLists() {
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

describe("role-based navigation integration (item 596)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows campaign manager menu items for campaign workflows", async () => {
    stubDashboardAndEmptyLists();
    renderApp({ path: "/dashboard", roles: ["CAMPAIGN_MANAGER"] });

    const nav = await screen.findByLabelText("Main navigation");
    expect(within(nav).getByRole("link", { name: /Dashboard/i })).toBeInTheDocument();
    expect(within(nav).getByRole("link", { name: /Campaigns/i })).toBeInTheDocument();
    expect(within(nav).getByRole("link", { name: "Builder" })).toBeInTheDocument();
    expect(within(nav).queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(within(nav).queryByRole("link", { name: "Settings" })).not.toBeInTheDocument();
  });

  it("shows compliance and audit navigation for compliance officers", async () => {
    stubDashboardAndEmptyLists();
    renderApp({ path: "/compliance", roles: ["COMPLIANCE_OFFICER"] });

    expect(await screen.findByRole("heading", { name: "Compliance review" })).toBeInTheDocument();
    const nav = screen.getByLabelText("Main navigation");
    expect(within(nav).getByRole("link", { name: "Compliance" })).toBeInTheDocument();
    expect(within(nav).getByRole("link", { name: "Audit" })).toBeInTheDocument();
    expect(within(nav).queryByRole("link", { name: "Builder" })).not.toBeInTheDocument();
  });

  it("shows admin-only settings and users for admin role", async () => {
    stubDashboardAndEmptyLists();
    renderApp({ path: "/settings", roles: ["ADMIN"] });

    expect(await screen.findByRole("heading", { name: "System settings" })).toBeInTheDocument();
    const nav = screen.getByLabelText("Main navigation");
    expect(within(nav).getByRole("link", { name: "Users" })).toBeInTheDocument();
    expect(within(nav).getByRole("link", { name: "Settings" })).toBeInTheDocument();
  });

  it("allows BI analysts analytics routes but not user administration", async () => {
    stubDashboardAndEmptyLists();
    renderApp({ path: "/analytics", roles: ["BI_ANALYST"] });

    expect(await screen.findByRole("heading", { name: "Analytics", level: 1 })).toBeInTheDocument();
    const nav = screen.getByLabelText("Main navigation");
    expect(within(nav).getByRole("link", { name: "Analytics" })).toBeInTheDocument();
    expect(within(nav).queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
  });

  it("opens the executive dashboard for executive viewers", async () => {
    stubDashboardAndEmptyLists();
    renderApp({ path: "/executive", roles: ["EXECUTIVE_VIEWER"] });

    expect(
      await screen.findByRole("heading", { name: "Executive dashboard" }),
    ).toBeInTheDocument();
  });

  it.each(["/users", "/settings", "/audit"])(
    "prevents an unauthorized user from opening %s directly",
    async (path) => {
      stubDashboardAndEmptyLists();
      renderApp({ path, roles: ["EXECUTIVE_VIEWER"] });

      expect(
        await screen.findByRole("heading", { name: /campaign performance/i }),
      ).toBeInTheDocument();
      expect(
        screen.queryByRole("heading", { name: /users|system settings|audit log/i }),
      ).not.toBeInTheDocument();
    },
  );

  it("allows an authorized system auditor to open the audit page directly", async () => {
    stubDashboardAndEmptyLists();
    renderApp({ path: "/audit", roles: ["SYSTEM_AUDITOR"] });

    expect(await screen.findByRole("heading", { name: /audit/i })).toBeInTheDocument();
  });
});
