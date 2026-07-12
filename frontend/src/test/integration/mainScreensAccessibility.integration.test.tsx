/**
 * Main screens basic accessibility integration (KB item 609 / NFR-011).
 *
 * Loads primary routes through the real shell and asserts landmarks, headings,
 * and labeled primary content for each catalogued main screen.
 */
import { screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  BREADCRUMB_NAV_ARIA_LABEL,
  getMainScreenById,
  MAIN_CONTENT_ID,
  MAIN_NAV_ARIA_LABEL,
  MAIN_SCREENS,
  shellMainScreens,
  SKIP_TO_CONTENT_LABEL,
  type MainScreenDefinition,
  type PrimaryContentExpectation,
} from "@/features/a11y/mainScreensAccessibility";
import { LOGIN_FORM_ARIA_LABEL } from "@/features/a11y/keyboardNavigationFlow";
import { LOGIN_PAGE_TITLE } from "@/features/auth/loginFlow";
import { createDashboardAnalyticsFixture } from "@/features/dashboard/dashboardAnalyticsFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const sampleCustomer = {
  id: "20000000-0000-0000-0000-000000000609",
  customerType: "CUSTOMER",
  firstName: "A11y",
  lastName: "Customer",
  fullName: "A11y Customer",
  email: "a11y.customer@bayer-westphalian.test",
  phone: null,
  addressLine: null,
  city: "Munich",
  country: "Germany",
  dateOfBirth: null,
  ageGroup: null,
  status: "ACTIVE",
  doNotContact: false,
  active: true,
  contactable: true,
  source: null,
  createdAt: "2026-07-12T12:00:00Z",
  updatedAt: "2026-07-12T12:00:00Z",
  deletedAt: null,
};

const sampleProduct = {
  id: "30000000-0000-0000-0000-000000000609",
  name: "A11y Home Cover",
  productType: "HOMEOWNER_INSURANCE",
  description: "Accessibility catalog product",
  price: 120,
  durationMonths: 12,
  expirationPolicy: "ANNUAL",
  active: true,
  deleted: false,
  createdAt: "2026-07-12T12:00:00Z",
  updatedAt: "2026-07-12T12:00:00Z",
  deletedAt: null,
};

const sampleSegment = {
  id: "42000000-0000-4000-8000-000000000609",
  name: "A11y segment",
  description: "Segment for a11y checks",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Admin User",
  visibility: "TEAM",
  criteria: [],
  createdAt: "2026-07-12T12:00:00Z",
  updatedAt: "2026-07-12T12:00:00Z",
};

const sampleCampaign = {
  id: "50000000-0000-0000-0000-000000000609",
  name: "A11y campaign",
  objective: "Validate accessibility labels",
  status: "SUBMITTED",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  segmentId: sampleSegment.id,
  segmentName: sampleSegment.name,
  channel: "EMAIL",
  messageSubject: "Hello",
  messageBody: "Body",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  approvedByUserId: null,
  approvedByFullName: null,
  approvedAt: null,
  rejectionReason: null,
  complianceReviewNotes: null,
  productIds: [sampleProduct.id],
  createdAt: "2026-07-12T12:00:00Z",
  updatedAt: "2026-07-12T12:00:00Z",
};

const sampleAudit = {
  id: "70000000-0000-0000-0000-000000000609",
  actorUserId: "10000000-0000-0000-0000-000000000101",
  actorEmail: "admin@bayer-westphalian.test",
  actorFullName: "Admin User",
  action: "CREATE",
  entityType: "CUSTOMER",
  entityId: sampleCustomer.id,
  oldValue: null,
  newValue: { firstName: "A11y" },
  ipAddress: "127.0.0.1",
  createdAt: "2026-07-12T12:00:00Z",
};

const sampleUser = {
  id: "10000000-0000-0000-0000-000000000609",
  email: "a11y.user@bayer-westphalian.test",
  fullName: "A11y User",
  status: "ACTIVE",
  roles: ["CAMPAIGN_MANAGER"],
  lastLoginAt: "2026-07-12T12:00:00Z",
};

function pageOf<T>(items: T[]) {
  return {
    content: items,
    page: 0,
    size: 50,
    totalElements: items.length,
    totalPages: 1,
    first: true,
    last: true,
    empty: items.length === 0,
  };
}

function a11yApiHandlers() {
  const dashboard = createDashboardAnalyticsFixture();
  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(dashboard, "Analytics dashboard loaded"),
    },
    {
      match: (url) => url.includes("/analytics/") && url.includes("product"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/analytics/campaigns"),
      response: () => jsonOk(null),
    },
    {
      match: (url) => url.includes("/customers") && !url.includes("/consents"),
      response: () => jsonOk(pageOf([sampleCustomer])),
    },
    {
      match: (url) => url.includes("/products") && !url.includes("change"),
      response: () => jsonOk([sampleProduct]),
    },
    {
      match: (url) => url.includes("/segments"),
      response: () => jsonOk([sampleSegment]),
    },
    {
      match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
      response: () => jsonOk([sampleCampaign]),
    },
    {
      match: (url) => url.includes("/audit-logs"),
      response: () => jsonOk([sampleAudit]),
    },
    {
      match: (url) => url.includes("/users"),
      response: () => jsonOk([sampleUser]),
    },
    {
      match: (url) => url.includes("/roles"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/reports/exports"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/system-settings") || url.includes("/settings"),
      response: () =>
        jsonOk({
          id: "00000000-0000-0000-0000-000000000001",
          monthlyContactLimit: 3,
          sendRetryLimit: 3,
          uninterestedExclusionDays: 90,
          updatedByUserId: null,
          updatedAt: null,
        }),
    },
  ]);
}

async function expectPrimaryContent(expectation: PrimaryContentExpectation) {
  if (expectation.kind === "form") {
    expect(await screen.findByRole("form", { name: expectation.name })).toBeInTheDocument();
    return;
  }
  if (expectation.kind === "table") {
    expect(await screen.findByRole("table", { name: expectation.name })).toBeInTheDocument();
    return;
  }
  if (expectation.kind === "region") {
    expect(await screen.findByLabelText(expectation.name)).toBeInTheDocument();
    return;
  }
  expect(
    await screen.findByRole("heading", {
      name: expectation.name,
      level: expectation.level,
    }),
  ).toBeInTheDocument();
}

function assertShellLandmarks(screenDef: MainScreenDefinition) {
  expect(screen.getByRole("heading", { name: screenDef.pageHeading, level: 1 })).toBeInTheDocument();
  expect(screen.getByRole("main")).toHaveAttribute("id", MAIN_CONTENT_ID);
  expect(screen.getByRole("link", { name: SKIP_TO_CONTENT_LABEL })).toBeInTheDocument();
  expect(screen.getByLabelText(MAIN_NAV_ARIA_LABEL)).toBeInTheDocument();
  expect(screen.getByLabelText(BREADCRUMB_NAV_ARIA_LABEL)).toBeInTheDocument();
}

describe("main screens basic accessibility integration (item 609)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("login exposes main landmark, page heading, and labeled form controls", async () => {
    vi.stubGlobal("fetch", a11yApiHandlers());
    renderApp({ path: "/login", authenticated: false });

    expect(screen.getByRole("heading", { name: LOGIN_PAGE_TITLE, level: 1 })).toBeInTheDocument();
    expect(screen.getByRole("main")).toBeInTheDocument();
    const form = screen.getByRole("form", { name: LOGIN_FORM_ARIA_LABEL });
    expect(within(form).getByLabelText("Email")).toBeInTheDocument();
    expect(within(form).getByLabelText("Password")).toBeInTheDocument();
    expect(within(form).getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });

  it.each(shellMainScreens().map((screen) => [screen.id, screen] as const))(
    "shell screen %s exposes landmarks and primary labeled content",
    async (_id, screenDef) => {
      vi.stubGlobal("fetch", a11yApiHandlers());
      renderApp({ path: screenDef.path, roles: screenDef.roles });

      await waitFor(() => {
        assertShellLandmarks(screenDef);
      });

      // At least the first always-present primary content item must resolve.
      await expectPrimaryContent(screenDef.primaryContent[0]!);

      // When seeded, also assert remaining labeled primary content (tables, etc.).
      for (const expectation of screenDef.primaryContent.slice(1)) {
        await expectPrimaryContent(expectation);
      }
    },
  );

  it("covers every MAIN_SCREENS catalog entry in the a11y suite", () => {
    expect(MAIN_SCREENS.map((s) => s.id)).toEqual(
      expect.arrayContaining(shellMainScreens().map((s) => s.id).concat(["login"])),
    );
    expect(getMainScreenById("dashboard").path).toBe("/dashboard");
    expect(emptyDashboardPayload.campaignTotal).toBe(0);
  });
});
