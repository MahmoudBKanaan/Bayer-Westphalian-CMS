/**
 * Campaign launch UI integration (KB item 605 / FR-054–055 / BR-005).
 *
 * Full route tree: authorized manager opens recipient preview for an APPROVED
 * campaign, confirms launch, sees success notice and ACTIVE status.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CAMPAIGN_LAUNCHED_NOTICE,
  CAMPAIGN_LAUNCH_BUTTON_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_TITLE,
  CAMPAIGN_LAUNCH_FIXTURES,
  CAMPAIGN_LAUNCH_RESULT_HEADING,
  RECIPIENT_PREVIEW_PAGE_TITLE,
  recipientPreviewPath,
} from "@/features/campaigns/campaignLaunchFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const approvedCampaign = {
  id: CAMPAIGN_LAUNCH_FIXTURES.campaignId,
  name: CAMPAIGN_LAUNCH_FIXTURES.campaignName,
  objective: CAMPAIGN_LAUNCH_FIXTURES.objective,
  status: "APPROVED",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  segmentId: "40000000-0000-0000-0000-00000000e201",
  segmentName: CAMPAIGN_LAUNCH_FIXTURES.segmentName,
  channel: CAMPAIGN_LAUNCH_FIXTURES.channel,
  messageSubject: CAMPAIGN_LAUNCH_FIXTURES.messageSubject,
  messageBody: CAMPAIGN_LAUNCH_FIXTURES.messageBody,
  startDate: CAMPAIGN_LAUNCH_FIXTURES.startDate,
  endDate: CAMPAIGN_LAUNCH_FIXTURES.endDate,
  approvedByUserId: "10000000-0000-0000-0000-000000000106",
  approvedByFullName: "Compliance Officer",
  approvedAt: "2026-07-12T12:00:00Z",
  rejectionReason: null,
  complianceReviewNotes: "Approved for launch UI test",
  productIds: ["30000000-0000-0000-0000-00000000e301"],
  createdAt: "2026-07-12T10:00:00Z",
  updatedAt: "2026-07-12T12:00:00Z",
};

const preview = {
  totalAudienceCount: CAMPAIGN_LAUNCH_FIXTURES.eligible + CAMPAIGN_LAUNCH_FIXTURES.excluded,
  eligibleCount: CAMPAIGN_LAUNCH_FIXTURES.eligible,
  excludedCount: CAMPAIGN_LAUNCH_FIXTURES.excluded,
  matchingCustomers: [],
  exclusionReasonSummary: [],
};

const summaryBefore = {
  campaignId: approvedCampaign.id,
  eligible: CAMPAIGN_LAUNCH_FIXTURES.eligible,
  excluded: CAMPAIGN_LAUNCH_FIXTURES.excluded,
  sent: 0,
  failed: 0,
};

function campaignLaunchHandlers() {
  let campaign = { ...approvedCampaign };
  let summary = { ...summaryBefore };

  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url, method) =>
        url.includes(`/campaigns/${approvedCampaign.id}`) &&
        !url.includes("/recipients") &&
        method === "GET",
      response: () => jsonOk(campaign),
    },
    {
      match: (url) => url.includes("/recipients/preview"),
      response: () => jsonOk(preview),
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
      response: () => jsonOk(summary),
    },
    {
      match: (url, method) =>
        url.includes(`/campaigns/${approvedCampaign.id}/launch`) && method === "POST",
      response: () => {
        campaign = { ...campaign, status: "ACTIVE", updatedAt: "2026-07-12T13:00:00Z" };
        summary = {
          ...summary,
          sent: CAMPAIGN_LAUNCH_FIXTURES.eligible,
        };
        return jsonOk(campaign, "Campaign launched");
      },
    },
    {
      match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
      response: () => jsonOk([campaign]),
    },
  ]);
}

describe("campaign launch UI integration (item 605)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("opens recipient preview with launch enabled for approved campaigns", async () => {
    vi.stubGlobal("fetch", campaignLaunchHandlers());
    renderApp({
      path: recipientPreviewPath(approvedCampaign.id),
      roles: ["CAMPAIGN_MANAGER"],
    });

    expect(
      await screen.findByRole("heading", { name: RECIPIENT_PREVIEW_PAGE_TITLE, level: 2 }),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByLabelText("Launch readiness")).toHaveTextContent(/ready to launch/i);
    });
    expect(screen.getByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL })).toBeEnabled();
  });

  it("disables launch when campaign is not approved", async () => {
    const handlers = createFetchRouter([
      {
        match: (url) => url.includes("/analytics/dashboard"),
        response: () => jsonOk(emptyDashboardPayload),
      },
      {
        match: (url, method) =>
          url.includes(`/campaigns/${approvedCampaign.id}`) &&
          !url.includes("/recipients") &&
          method === "GET",
        response: () => jsonOk({ ...approvedCampaign, status: "SUBMITTED" }),
      },
      {
        match: (url) => url.includes("/recipients/preview"),
        response: () => jsonOk(preview),
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
        response: () => jsonOk(summaryBefore),
      },
    ]);
    vi.stubGlobal("fetch", handlers);

    renderApp({
      path: recipientPreviewPath(approvedCampaign.id),
      roles: ["CAMPAIGN_MANAGER"],
    });

    const launchButton = await screen.findByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL });
    expect(launchButton).toBeDisabled();
    await waitFor(() => {
      expect(screen.getByLabelText("Launch readiness")).toHaveTextContent(/SUBMITTED/);
    });
  });

  it("launches an approved campaign after confirmation", async () => {
    const user = userEvent.setup();
    const fetchMock = campaignLaunchHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({
      path: recipientPreviewPath(approvedCampaign.id),
      roles: ["ADMIN"],
    });

    await screen.findByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL });
    await user.click(screen.getByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL }));

    const dialog = await screen.findByRole("dialog", { name: CAMPAIGN_LAUNCH_CONFIRM_TITLE });
    expect(within(dialog).getByText(CAMPAIGN_LAUNCH_FIXTURES.campaignName)).toBeInTheDocument();
    await user.click(within(dialog).getByRole("button", { name: CAMPAIGN_LAUNCH_CONFIRM_LABEL }));

    expect(await screen.findByTestId("campaign-launch-notice")).toHaveTextContent(
      CAMPAIGN_LAUNCHED_NOTICE,
    );
    expect(
      await screen.findByRole("heading", { name: CAMPAIGN_LAUNCH_RESULT_HEADING }),
    ).toBeInTheDocument();

    await waitFor(() => {
      const launchCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).includes(`/campaigns/${approvedCampaign.id}/launch`) &&
          (init as RequestInit | undefined)?.method === "POST"
        );
      });
      expect(launchCall).toBeDefined();
    });

    const context = screen.getByLabelText("Campaign preview context");
    expect(within(context).getByText("Active")).toBeInTheDocument();
  });

  it("hides launch for compliance officers", async () => {
    vi.stubGlobal("fetch", campaignLaunchHandlers());
    renderApp({
      path: recipientPreviewPath(approvedCampaign.id),
      roles: ["COMPLIANCE_OFFICER"],
    });

    await screen.findByRole("heading", { name: RECIPIENT_PREVIEW_PAGE_TITLE, level: 2 });
    expect(
      screen.queryByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Launch readiness")).toHaveTextContent(/cannot launch/i);
  });
});
