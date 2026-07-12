import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const draftCampaign = {
  id: "50000000-0000-0000-0000-000000000001",
  name: "Life renewal outreach",
  objective: "Promote life insurance renewals",
  status: "DRAFT",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  segmentId: "42000000-0000-4000-8000-000000000201",
  segmentName: "Munich prospects",
  channel: "EMAIL",
  messageSubject: "Renew your cover",
  messageBody: "Dear customer, ...",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  approvedByUserId: null,
  approvedByFullName: null,
  approvedAt: null,
  rejectionReason: null,
  complianceReviewNotes: null,
  productIds: [],
  createdAt: "2026-07-09T10:15:00Z",
  updatedAt: "2026-07-09T10:30:00Z",
};

const submittedCampaign = {
  ...draftCampaign,
  id: "50000000-0000-0000-0000-000000000002",
  name: "Submitted outreach",
  status: "SUBMITTED",
};

describe("workflow route integration (item 596)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads campaigns list from the API inside the app shell", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
          response: () => jsonOk([draftCampaign], "Campaigns loaded"),
        },
      ]),
    );

    renderApp({ path: "/campaigns", roles: ["CAMPAIGN_MANAGER"] });

    expect(await screen.findByRole("heading", { name: "Campaigns", level: 1 })).toBeInTheDocument();
    const table = await screen.findByRole("table", { name: /Campaign worklist/i });
    expect(within(table).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(table).getByText("Draft")).toBeInTheDocument();
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
  });

  it("opens campaign builder route with shell navigation available", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/segments"),
          response: () => jsonOk([]),
        },
        {
          match: (url) => url.includes("/products"),
          response: () => jsonOk([]),
        },
      ]),
    );

    renderApp({ path: "/campaign-builder", roles: ["CAMPAIGN_MANAGER"] });

    // Shell top bar uses nav label "Builder"; page content uses "Campaign Builder".
    expect(await screen.findByRole("heading", { name: "Builder", level: 1 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Campaign Builder", level: 2 })).toBeInTheDocument();
    expect(screen.getByLabelText("Campaign builder steps")).toBeInTheDocument();
    expect(screen.getByText(/Follow the steps to define the campaign/i)).toBeInTheDocument();
  });

  it("navigates from campaigns to campaign builder through the shell", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
          response: () => jsonOk([draftCampaign]),
        },
        {
          match: (url) => url.includes("/segments"),
          response: () => jsonOk([]),
        },
        {
          match: (url) => url.includes("/products"),
          response: () => jsonOk([]),
        },
        {
          match: (url) => url.includes("/analytics/dashboard"),
          response: () => jsonOk(emptyDashboardPayload),
        },
      ]),
    );

    renderApp({ path: "/campaigns", roles: ["CAMPAIGN_MANAGER"] });
    await screen.findByRole("heading", { name: "Campaigns", level: 1 });

    await user.click(screen.getByRole("link", { name: "Builder" }));

    expect(await screen.findByRole("heading", { name: "Builder", level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText("Campaign builder steps")).toBeInTheDocument();
  });

  it("loads compliance review queue for compliance officers", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/campaigns"),
          response: () => jsonOk([submittedCampaign], "Campaigns loaded"),
        },
        {
          match: (url) => url.includes("/recipients/summary"),
          response: () =>
            jsonOk({
              campaignId: submittedCampaign.id,
              eligible: 5,
              excluded: 2,
              sent: 0,
              failed: 0,
            }),
        },
      ]),
    );

    renderApp({ path: "/compliance", roles: ["COMPLIANCE_OFFICER"] });

    expect(await screen.findByRole("heading", { name: "Compliance review" })).toBeInTheDocument();
    expect(screen.getByLabelText("Compliance review checklist")).toBeInTheDocument();
    const table = await screen.findByRole("table", { name: "Submitted campaigns table" });
    expect(within(table).getByText("Submitted outreach")).toBeInTheDocument();
  });

  it("loads recipient preview for a campaign id route", async () => {
    const campaignId = "50000000-0000-0000-0000-000000000285";
    const approved = {
      ...draftCampaign,
      id: campaignId,
      status: "APPROVED",
      name: "Life renewal outreach",
    };

    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url === `${API_BASE_URL}/campaigns/${campaignId}`,
          response: () => jsonOk(approved),
        },
        {
          match: (url) => url.includes("/recipients/preview"),
          response: () =>
            jsonOk({
              totalAudienceCount: 3,
              eligibleCount: 2,
              excludedCount: 1,
              matchingCustomers: [
                {
                  id: "20000000-0000-0000-0000-000000000285",
                  customerType: "CUSTOMER",
                  firstName: "Ada",
                  lastName: "Eligible",
                  fullName: "Ada Eligible",
                  email: "ada@bayer-westphalian.test",
                  city: "Munich",
                  country: "Germany",
                  status: "ACTIVE",
                  doNotContact: false,
                },
              ],
              exclusionReasonSummary: [
                {
                  code: "INVALID_CONSENT",
                  message: "Customer does not have valid required consent",
                  count: 1,
                },
              ],
            }),
        },
        {
          match: (url) => url.includes("/recipients/eligible"),
          response: () => jsonOk([]),
        },
        {
          match: (url) => url.includes("/recipients/excluded"),
          response: () => jsonOk([]),
        },
        {
          match: (url) => url.includes("/recipients/summary"),
          response: () =>
            jsonOk({
              campaignId,
              eligible: 2,
              excluded: 1,
              sent: 0,
              failed: 0,
            }),
        },
      ]),
    );

    renderApp({
      path: `/campaigns/${campaignId}/recipients/preview`,
      roles: ["CAMPAIGN_MANAGER"],
    });

    expect(
      await screen.findByRole("heading", { name: "Recipient Preview", level: 1 }),
    ).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "Life renewal outreach" })).toBeInTheDocument();
    expect(screen.getByLabelText("Launch readiness")).toBeInTheDocument();
    expect(await screen.findByText("Ada Eligible")).toBeInTheDocument();
  });

  it("loads audit log route for auditors", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/audit-logs"),
          response: () => jsonOk([]),
        },
      ]),
    );

    renderApp({ path: "/audit", roles: ["SYSTEM_AUDITOR"] });

    expect(await screen.findByRole("heading", { name: "Audit log" })).toBeInTheDocument();
  });
});
