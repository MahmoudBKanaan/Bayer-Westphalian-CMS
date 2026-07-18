/**
 * Compliance approval UI integration (KB item 604 / FR-059 / BR-005 / COMP-006).
 *
 * Full route tree: authorized reviewer opens /compliance, selects a SUBMITTED
 * campaign, confirms approval, sees success notice; unauthorized roles blocked.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  COMPLIANCE_APPROVE_BUTTON_LABEL,
  COMPLIANCE_APPROVAL_FIXTURES,
  COMPLIANCE_APPROVED_NOTICE,
  COMPLIANCE_QUEUE_TABLE_ARIA_LABEL,
  COMPLIANCE_REJECT_BUTTON_LABEL,
  COMPLIANCE_REVIEW_NOTES_LABEL,
  COMPLIANCE_REVIEW_PAGE_TITLE,
  campaignFormValidationMessages,
  complianceApprovalConfirmCopy,
} from "@/features/campaigns/complianceApprovalFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const submittedCampaign = {
  id: COMPLIANCE_APPROVAL_FIXTURES.campaignId,
  name: COMPLIANCE_APPROVAL_FIXTURES.campaignName,
  objective: COMPLIANCE_APPROVAL_FIXTURES.objective,
  status: "SUBMITTED",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: COMPLIANCE_APPROVAL_FIXTURES.ownerFullName,
  segmentId: "40000000-0000-0000-0000-00000000e201",
  segmentName: COMPLIANCE_APPROVAL_FIXTURES.segmentName,
  channel: COMPLIANCE_APPROVAL_FIXTURES.channel,
  messageSubject: COMPLIANCE_APPROVAL_FIXTURES.messageSubject,
  messageBody: COMPLIANCE_APPROVAL_FIXTURES.messageBody,
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  approvedByUserId: null,
  approvedByFullName: null,
  approvedAt: null,
  rejectionReason: null,
  complianceReviewNotes: null,
  productIds: ["30000000-0000-0000-0000-00000000e301"],
  createdAt: "2026-07-12T12:00:00Z",
  updatedAt: "2026-07-12T12:05:00Z",
};

const recipientSummary = {
  campaignId: submittedCampaign.id,
  eligible: COMPLIANCE_APPROVAL_FIXTURES.eligible,
  excluded: COMPLIANCE_APPROVAL_FIXTURES.excluded,
  sent: 0,
  failed: 0,
};

function complianceApprovalHandlers() {
  let queue = [submittedCampaign];

  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url, method) =>
        url.includes("/campaigns") &&
        url.includes("status=SUBMITTED") &&
        method === "GET",
      response: () => jsonOk(queue, "Submitted campaigns loaded"),
    },
    {
      match: (url) => url.includes("/recipients/summary"),
      response: () => jsonOk(recipientSummary),
    },
    {
      match: (url, method) =>
        url.includes(`/campaigns/${submittedCampaign.id}/approve`) && method === "POST",
      response: () => {
        queue = [];
        return jsonOk(
          {
            ...submittedCampaign,
            status: "APPROVED",
            approvedByFullName: "Compliance Officer",
            approvedAt: "2026-07-12T13:00:00Z",
            complianceReviewNotes: COMPLIANCE_APPROVAL_FIXTURES.reviewNotes,
          },
          "Campaign approved",
        );
      },
    },
    {
      match: (url, method) =>
        url.includes("/campaigns") &&
        !url.includes("/recipients") &&
        method === "GET",
      response: () => jsonOk(queue, "Campaigns loaded"),
    },
  ]);
}

describe("compliance approval UI integration (item 604)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows the SUBMITTED queue for compliance officers", async () => {
    vi.stubGlobal("fetch", complianceApprovalHandlers());
    renderApp({ path: "/compliance", roles: ["COMPLIANCE_OFFICER"] });

    expect(await screen.findByRole("heading", { name: "Compliance", level: 1 })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: COMPLIANCE_REVIEW_PAGE_TITLE, level: 2 }),
    ).toBeInTheDocument();
    const table = await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });
    expect(within(table).getByText(COMPLIANCE_APPROVAL_FIXTURES.campaignName)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: COMPLIANCE_APPROVE_BUTTON_LABEL })).toBeInTheDocument();
  });

  it("blocks unauthorized roles from reviewing campaigns", async () => {
    vi.stubGlobal("fetch", complianceApprovalHandlers());
    renderApp({ path: "/compliance", roles: ["BI_ANALYST"] });

    expect(
      await screen.findByRole("heading", { name: /campaign performance/i }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL }),
    ).not.toBeInTheDocument();
  });

  it("approves a submitted campaign with notes and confirmation", async () => {
    const user = userEvent.setup();
    const fetchMock = complianceApprovalHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/compliance", roles: ["ADMIN"] });
    await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });

    await user.type(
      screen.getByLabelText(COMPLIANCE_REVIEW_NOTES_LABEL),
      COMPLIANCE_APPROVAL_FIXTURES.reviewNotes,
    );
    await user.click(screen.getByRole("button", { name: COMPLIANCE_APPROVE_BUTTON_LABEL }));

    const dialog = await screen.findByRole("dialog");
    const copy = complianceApprovalConfirmCopy("approve");
    expect(within(dialog).getByText(copy.title)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: copy.label }));

    expect(await screen.findByTestId("compliance-decision-notice")).toHaveTextContent(
      COMPLIANCE_APPROVED_NOTICE,
    );

    await waitFor(() => {
      const approveCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).includes(`/campaigns/${submittedCampaign.id}/approve`) &&
          (init as RequestInit | undefined)?.method === "POST"
        );
      });
      expect(approveCall).toBeDefined();
      expect(JSON.parse(String((approveCall?.[1] as RequestInit).body))).toEqual({
        complianceReviewNotes: COMPLIANCE_APPROVAL_FIXTURES.reviewNotes,
      });
    });
  });

  it("requires rejection reason before opening reject confirmation", async () => {
    const user = userEvent.setup();
    const fetchMock = complianceApprovalHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/compliance", roles: ["COMPLIANCE_OFFICER"] });
    await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });

    await user.click(screen.getByRole("button", { name: COMPLIANCE_REJECT_BUTTON_LABEL }));

    expect(
      await screen.findByText(campaignFormValidationMessages.rejectionReasonRequired),
    ).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).includes("/reject")),
    ).toBe(false);
  });
});
