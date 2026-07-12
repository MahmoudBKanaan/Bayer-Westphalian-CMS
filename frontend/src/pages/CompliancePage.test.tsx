import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  COMPLIANCE_APPROVE_BUTTON_LABEL,
  COMPLIANCE_APPROVED_NOTICE,
  COMPLIANCE_QUEUE_TABLE_ARIA_LABEL,
  COMPLIANCE_REJECT_BUTTON_LABEL,
  COMPLIANCE_REJECTED_NOTICE,
  COMPLIANCE_REVIEW_NOTES_LABEL,
  COMPLIANCE_REVIEW_PAGE_TITLE,
  complianceApprovalConfirmCopy,
} from "@/features/campaigns/complianceApprovalFlow";
import { campaignFormValidationMessages } from "@/features/campaigns/campaignFormValidation";
import { CompliancePage } from "@/pages/CompliancePage";

const submittedCampaign = {
  id: "50000000-0000-0000-0000-000000000002",
  name: "Submitted outreach",
  objective: "Promote compliant renewals",
  status: "SUBMITTED",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  segmentId: "42000000-0000-4000-8000-000000000201",
  segmentName: "Munich prospects",
  channel: "EMAIL",
  messageSubject: "Renew your cover",
  messageBody: "Dear customer, confirm your consent before renewal.",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  approvedByUserId: null,
  approvedByFullName: null,
  approvedAt: null,
  rejectionReason: null,
  complianceReviewNotes: null,
  productIds: ["41000000-0000-0000-0000-000000000201"],
  createdAt: "2026-07-09T10:15:00Z",
  updatedAt: "2026-07-09T10:30:00Z",
};

const recipientSummary = {
  campaignId: submittedCampaign.id,
  eligible: 12,
  excluded: 4,
  sent: 0,
  failed: 0,
};

function renderCompliancePage(roles: SystemRoleName[] = ["COMPLIANCE_OFFICER"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000000106",
      email: "compliance.officer@bayer-westphalian.test",
      fullName: "Compliance Officer",
      status: "ACTIVE",
      lastLoginAt: "2026-07-09T12:00:00Z",
    }),
  );

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/compliance"]}>
        <AuthProvider>
          <CompliancePage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("CompliancePage (item 593)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads submitted campaigns with checklist and structured review clarity", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCompliancePage(["COMPLIANCE_OFFICER"]);

    expect(
      await screen.findByRole("heading", { name: COMPLIANCE_REVIEW_PAGE_TITLE }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Review submitted campaigns before they can launch/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/cannot launch until a Compliance Officer/i)).toBeInTheDocument();
    expect(screen.getByLabelText("Compliance review checklist")).toBeInTheDocument();
    expect(screen.getAllByText("Message content").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Eligibility & exclusions")).toBeInTheDocument();
    expect(screen.getByText("Decision record")).toBeInTheDocument();

    expect(
      await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
    const table = screen.getByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });
    expect(within(table).getByText("Submitted outreach")).toBeInTheDocument();
    expect(within(table).getByText("Campaign Manager")).toBeInTheDocument();
    expect(screen.getByText("Renew your cover")).toBeInTheDocument();
    expect(screen.getByLabelText("Compliance review summary")).toHaveTextContent(
      "Pending campaign approvals",
    );
    expect(screen.getByLabelText("Compliance review summary")).toHaveTextContent(
      "1 campaign waiting for a compliance decision",
    );

    expect(await screen.findByText(/Eligible 12/)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Open recipient preview" }),
    ).toHaveAttribute(
      "href",
      `/campaigns/${submittedCampaign.id}/recipients/preview`,
    );
    expect(screen.getByRole("heading", { name: "Approve", level: 3 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Reject", level: 3 })).toBeInTheDocument();
  });

  it("approves a submitted campaign with confirmation and review notes", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCompliancePage(["COMPLIANCE_OFFICER"]);
    await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });

    await user.type(screen.getByLabelText(COMPLIANCE_REVIEW_NOTES_LABEL), "Approved wording");
    await user.click(screen.getByRole("button", { name: COMPLIANCE_APPROVE_BUTTON_LABEL }));

    const dialog = await screen.findByRole("dialog");
    expect(dialog).toBeInTheDocument();
    const approveCopy = complianceApprovalConfirmCopy("approve");
    expect(screen.getByText(approveCopy.title)).toBeInTheDocument();
    expect(within(dialog).getByText(/Status becomes APPROVED/i)).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: approveCopy.label }));

    await waitFor(() => {
      const approveCall = fetchMock.mock.calls.find(([url]) =>
        String(url).endsWith(`/campaigns/${submittedCampaign.id}/approve`),
      );
      expect(approveCall).toBeDefined();
      expect(JSON.parse((approveCall?.[1] as RequestInit).body as string)).toEqual({
        complianceReviewNotes: "Approved wording",
      });
    });
    expect(await screen.findByTestId("compliance-decision-notice")).toHaveTextContent(
      COMPLIANCE_APPROVED_NOTICE,
    );
  });

  it("requires rejection reason before rejecting a campaign", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCompliancePage(["COMPLIANCE_OFFICER"]);
    await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });

    await user.click(screen.getByRole("button", { name: COMPLIANCE_REJECT_BUTTON_LABEL }));

    expect(
      await screen.findByText(campaignFormValidationMessages.rejectionReasonRequired),
    ).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes("/reject"))).toBe(false);
  });

  it("rejects a submitted campaign with reason, notes, and confirmation", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCompliancePage(["COMPLIANCE_OFFICER"]);
    await screen.findByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });

    await user.type(screen.getByLabelText("Rejection reason"), "Missing consent language");
    await user.type(screen.getByLabelText("Rejection compliance notes"), "Add opt-out copy");
    await user.click(screen.getByRole("button", { name: COMPLIANCE_REJECT_BUTTON_LABEL }));

    expect(await screen.findByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Confirm campaign rejection")).toBeInTheDocument();
    expect(screen.getByText("Missing consent language")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Confirm rejection" }));

    await waitFor(() => {
      const rejectCall = fetchMock.mock.calls.find(([url]) =>
        String(url).endsWith(`/campaigns/${submittedCampaign.id}/reject`),
      );
      expect(rejectCall).toBeDefined();
      expect(JSON.parse((rejectCall?.[1] as RequestInit).body as string)).toEqual({
        rejectionReason: "Missing consent language",
        complianceReviewNotes: "Add opt-out copy",
      });
    });
    expect(await screen.findByTestId("compliance-decision-notice")).toHaveTextContent(
      COMPLIANCE_REJECTED_NOTICE,
    );
  });

  it("blocks users without campaign review permission", () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCompliancePage(["BI_ANALYST"]);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "You are not authorized to review campaigns.",
    );
    expect(
      screen.queryByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL }),
    ).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Compliance review checklist")).not.toBeInTheDocument();
  });
});

function createFetchMock() {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";

    if (url.includes("/approve") && method === "POST") {
      const body = JSON.parse(init?.body as string) as Record<string, unknown>;
      return jsonResponse({ ...submittedCampaign, status: "APPROVED", ...body });
    }
    if (url.includes("/reject") && method === "POST") {
      const body = JSON.parse(init?.body as string) as Record<string, unknown>;
      return jsonResponse({ ...submittedCampaign, status: "REJECTED", ...body });
    }
    if (url.includes("/recipients/summary")) {
      return jsonResponse(recipientSummary);
    }
    if (url.includes("/campaigns")) {
      return jsonResponse([submittedCampaign]);
    }
    return jsonResponse(null, 404);
  });
}

function jsonResponse(data: unknown, status = 200) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: async () => ({
      success: status >= 200 && status < 300,
      message: "OK",
      data,
    }),
  });
}

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
