import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import {
  CAMPAIGN_LAUNCHED_NOTICE,
  CAMPAIGN_LAUNCH_BUTTON_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_TITLE,
  CAMPAIGN_LAUNCH_RESULT_HEADING,
  RECIPIENT_PREVIEW_PAGE_TITLE,
} from "@/features/campaigns/campaignLaunchFlow";
import { CampaignRecipientPreviewPage } from "@/pages/CampaignRecipientPreviewPage";

const campaignManagerUser = {
  id: "10000000-0000-0000-0000-000000000101",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["CAMPAIGN_MANAGER"],
};

const marketingAnalystUser = {
  ...campaignManagerUser,
  id: "10000000-0000-0000-0000-000000000109",
  email: "marketing.analyst@bayer-westphalian.test",
  fullName: "Marketing Analyst",
  roles: ["MARKETING_ANALYST"],
};

const complianceUser = {
  ...campaignManagerUser,
  id: "10000000-0000-0000-0000-000000000106",
  email: "compliance.officer@bayer-westphalian.test",
  fullName: "Compliance Officer",
  roles: ["COMPLIANCE_OFFICER"],
};

const campaign = {
  id: "50000000-0000-0000-0000-000000000285",
  name: "Life renewal outreach",
  objective: "Promote life insurance renewals",
  status: "APPROVED",
  ownerUserId: campaignManagerUser.id,
  ownerFullName: "Campaign Manager",
  segmentId: "42000000-0000-0000-0000-000000000285",
  segmentName: "Munich prospects",
  channel: "EMAIL",
  messageSubject: "Renew your cover",
  messageBody: "Dear customer, ...",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  approvedByUserId: "10000000-0000-0000-0000-000000000106",
  approvedByFullName: "Compliance Officer",
  approvedAt: "2026-07-09T12:00:00Z",
  rejectionReason: null,
  complianceReviewNotes: null,
  productIds: [],
  createdAt: "2026-07-09T10:15:00Z",
  updatedAt: "2026-07-09T12:00:00Z",
};

const preview = {
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
};

const eligibleRecipients = [
  {
    id: "62000000-0000-0000-0000-000000000286",
    campaignId: campaign.id,
    campaignName: campaign.name,
    customerId: "20000000-0000-0000-0000-000000000286",
    customerFullName: "Ada Eligible",
    eligibilityStatus: "ELIGIBLE",
    exclusionReason: null,
    eligibilityExplanation: "Customer is eligible for campaign contact",
    sentAt: null,
    openedAt: null,
    clickedAt: null,
    convertedAt: null,
    createdAt: "2026-07-09T10:15:30Z",
  },
  {
    id: "62000000-0000-0000-0000-000000000287",
    campaignId: campaign.id,
    campaignName: campaign.name,
    customerId: "20000000-0000-0000-0000-000000000287",
    customerFullName: "Lin Sent",
    eligibilityStatus: "SENT",
    exclusionReason: null,
    eligibilityExplanation: "Customer is eligible for campaign contact",
    sentAt: "2026-07-09T11:00:00Z",
    openedAt: null,
    clickedAt: null,
    convertedAt: null,
    createdAt: "2026-07-09T10:45:30Z",
  },
];

const excludedRecipients = [
  {
    id: "62000000-0000-0000-0000-000000000288",
    campaignId: campaign.id,
    campaignName: campaign.name,
    customerId: "20000000-0000-0000-0000-000000000288",
    customerFullName: "Grace Excluded",
    eligibilityStatus: "EXCLUDED",
    exclusionReason: "INVALID_CONSENT",
    eligibilityExplanation: "Customer does not have valid required consent",
    sentAt: null,
    openedAt: null,
    clickedAt: null,
    convertedAt: null,
    createdAt: "2026-07-09T10:45:30Z",
  },
  {
    id: "62000000-0000-0000-0000-000000000289",
    campaignId: campaign.id,
    campaignName: campaign.name,
    customerId: "20000000-0000-0000-0000-000000000289",
    customerFullName: "Max Limited",
    eligibilityStatus: "EXCLUDED",
    exclusionReason: "MONTHLY_CONTACT_LIMIT",
    eligibilityExplanation: "Customer has reached the monthly marketing contact limit",
    sentAt: null,
    openedAt: null,
    clickedAt: null,
    convertedAt: null,
    createdAt: "2026-07-09T10:50:30Z",
  },
];

const launchSummary = {
  campaignId: campaign.id,
  eligible: 2,
  excluded: 2,
  sent: 2,
  failed: 0,
};

describe("CampaignRecipientPreviewPage (item 594)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("renders campaign recipient preview metrics and eligible customers", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderPreviewPage(campaignManagerUser);

    expect(await screen.findByRole("heading", { name: "Recipient Preview" })).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Life renewal outreach" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Munich prospects")).toBeInTheDocument();
    expect(screen.getByText("Approved")).toBeInTheDocument();

    const metrics = screen.getByLabelText("Audience size metrics");
    expect(within(metrics).getByText("Total audience")).toBeInTheDocument();
    expect(within(metrics).getByText("3")).toBeInTheDocument();
    expect(within(metrics).getByText("Eligible")).toBeInTheDocument();
    expect(within(metrics).getByText("2")).toBeInTheDocument();
    expect(screen.getByText("Ada Eligible")).toBeInTheDocument();
    expect(screen.getAllByText("INVALID_CONSENT")).not.toHaveLength(0);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}`,
      expect.any(Object),
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/preview`,
      expect.any(Object),
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/eligible`,
      expect.any(Object),
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/excluded`,
      expect.any(Object),
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/recipients/summary`,
      expect.any(Object),
    );
  });

  it("improves recipient preview clarity with guide, snapshot, and launch readiness (item 594)", async () => {
    vi.stubGlobal("fetch", createFetchMock());
    renderPreviewPage(campaignManagerUser);

    expect(
      await screen.findByText(/Review who is eligible for this campaign/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/Launch is allowed only for APPROVED campaigns/i)).toBeInTheDocument();
    expect(screen.getByLabelText("Recipient preview guide")).toBeInTheDocument();
    expect(screen.getByText("How to read this preview")).toBeInTheDocument();
    expect(
      await screen.findByText(/APPROVED and ready to launch/i),
    ).toBeInTheDocument();

    const snapshot = await screen.findByLabelText("Audience snapshot metrics");
    expect(within(snapshot).getByText("Total matched")).toBeInTheDocument();
    expect(within(snapshot).getByText("Eligible")).toBeInTheDocument();
    expect(within(snapshot).getByText("Excluded")).toBeInTheDocument();
    expect(
      screen.getByText(/of the matched audience is eligible for contact/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Prefer eligible counts over total matched audience/i),
    ).toBeInTheDocument();
  });

  it("shows stored eligible campaign recipients in the eligible recipients tab", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPreviewPage(campaignManagerUser);

    await screen.findByRole("heading", { name: "Recipient Preview" });
    await userEvent.click(screen.getByRole("tab", { name: /Eligible recipients/i }));

    const table = await screen.findByRole("table", {
      name: "Eligible campaign recipients table",
    });
    expect(within(table).getByText("Ada Eligible")).toBeInTheDocument();
    expect(within(table).getByText("Lin Sent")).toBeInTheDocument();
    expect(within(table).getByText("Eligible", { selector: ".status-badge" })).toBeInTheDocument();
    expect(within(table).getByText("Sent", { selector: ".status-badge" })).toBeInTheDocument();
    expect(within(table).getAllByText("Customer is eligible for campaign contact")).toHaveLength(2);
  });

  it("supports keyboard navigation across recipient review tabs", async () => {
    vi.stubGlobal("fetch", createFetchMock());
    const user = userEvent.setup();

    renderPreviewPage(campaignManagerUser);

    await screen.findByRole("heading", { name: "Recipient Preview" });
    const previewTab = screen.getByRole("tab", { name: "Audience preview" });
    const eligibleTab = screen.getByRole("tab", { name: /Eligible recipients/i });

    previewTab.focus();
    await user.keyboard("{ArrowRight}");

    await waitFor(() => {
      expect(eligibleTab).toHaveFocus();
    });
    expect(eligibleTab).toHaveAttribute("aria-selected", "true");
    expect(previewTab).toHaveAttribute("tabindex", "-1");
    expect(
      await screen.findByRole("table", {
        name: "Eligible campaign recipients table",
      }),
    ).toBeInTheDocument();
  });

  it("shows stored excluded campaign recipients in the excluded recipients tab", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPreviewPage(campaignManagerUser);

    await screen.findByRole("heading", { name: "Recipient Preview" });
    await userEvent.click(screen.getByRole("tab", { name: /Excluded recipients/i }));

    const table = await screen.findByRole("table", {
      name: "Excluded campaign recipients table",
    });
    expect(within(table).getByText("Grace Excluded")).toBeInTheDocument();
    expect(within(table).getByText("Max Limited")).toBeInTheDocument();
    expect(within(table).getAllByText("Excluded", { selector: ".status-badge" })).toHaveLength(2);
    expect(within(table).getByText("INVALID_CONSENT")).toBeInTheDocument();
    expect(within(table).getByText("MONTHLY_CONTACT_LIMIT")).toBeInTheDocument();
    expect(within(table).getByText("Invalid or missing consent")).toBeInTheDocument();
    expect(within(table).getByText("Monthly contact limit")).toBeInTheDocument();
    expect(
      within(table).getByText("Customer does not have valid required consent"),
    ).toBeInTheDocument();

    expect(screen.getByRole("heading", { name: "Exclusion reason summary" })).toBeInTheDocument();
    const cards = screen.getByLabelText("Exclusion reason cards");
    expect(within(cards).getByText("Invalid or missing consent")).toBeInTheDocument();
    expect(within(cards).getByText("Monthly contact limit")).toBeInTheDocument();
    expect(
      screen.getByRole("table", { name: "Exclusion reason summary table" }),
    ).toBeInTheDocument();
  });

  it("requires confirmation before launching an approved campaign", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderPreviewPage(campaignManagerUser);

    await screen.findByRole("heading", { name: RECIPIENT_PREVIEW_PAGE_TITLE });
    await userEvent.click(screen.getByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL }));

    const dialog = await screen.findByRole("dialog", { name: CAMPAIGN_LAUNCH_CONFIRM_TITLE });
    expect(within(dialog).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(dialog).getByText(/contact/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/eligible/i)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/launch`,
      expect.any(Object),
    );

    await userEvent.click(within(dialog).getByRole("button", { name: CAMPAIGN_LAUNCH_CONFIRM_LABEL }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/campaigns/${campaign.id}/launch`,
        expect.objectContaining({ method: "POST" }),
      );
    });
    expect(await screen.findByTestId("campaign-launch-notice")).toHaveTextContent(
      CAMPAIGN_LAUNCHED_NOTICE,
    );
    const context = screen.getByLabelText("Campaign preview context");
    expect(within(context).getByText("Active")).toBeInTheDocument();

    expect(
      await screen.findByRole("heading", { name: CAMPAIGN_LAUNCH_RESULT_HEADING }),
    ).toBeInTheDocument();
    const resultMetrics = screen.getByLabelText("Launch result metrics");
    expect(within(resultMetrics).getByText("Eligible")).toBeInTheDocument();
    expect(within(resultMetrics).getByText("Excluded")).toBeInTheDocument();
    expect(within(resultMetrics).getByText("Sent")).toBeInTheDocument();
    expect(within(resultMetrics).getByText("Failed")).toBeInTheDocument();
    expect(within(resultMetrics).getAllByText("2")).toHaveLength(3);
    expect(within(resultMetrics).getByText("0")).toBeInTheDocument();
  });

  it("explains why launch is blocked when campaign is not approved", async () => {
    vi.stubGlobal("fetch", createFetchMock({ ...campaign, status: "SUBMITTED" }));

    renderPreviewPage(campaignManagerUser);

    const launchButton = await screen.findByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL });
    expect(launchButton).toBeDisabled();
    expect(await screen.findByText(/status is SUBMITTED/i)).toBeInTheDocument();
    expect(screen.getByLabelText("Launch readiness")).toHaveTextContent(/Only APPROVED/);
  });

  it("cancels launch confirmation without calling the launch endpoint", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderPreviewPage(campaignManagerUser);

    await screen.findByRole("heading", { name: RECIPIENT_PREVIEW_PAGE_TITLE });
    await userEvent.click(screen.getByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL }));
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(
      screen.queryByRole("dialog", { name: CAMPAIGN_LAUNCH_CONFIRM_TITLE }),
    ).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(
      `${API_BASE_URL}/campaigns/${campaign.id}/launch`,
      expect.any(Object),
    );
  });

  it("disables launch until campaign status is approved", async () => {
    vi.stubGlobal("fetch", createFetchMock({ ...campaign, status: "SUBMITTED" }));

    renderPreviewPage(campaignManagerUser);

    const launchButton = await screen.findByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL });
    expect(launchButton).toBeDisabled();
  });

  it("hides launch for users without campaign management access", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPreviewPage(complianceUser, ["COMPLIANCE_OFFICER"]);

    await screen.findByRole("heading", { name: RECIPIENT_PREVIEW_PAGE_TITLE });
    expect(
      screen.queryByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Launch readiness")).toHaveTextContent(
      /cannot launch/i,
    );
  });

  it("blocks users without campaign read access", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderPreviewPage(marketingAnalystUser, ["MARKETING_ANALYST"]);

    expect(
      await screen.findByText("You are not authorized to view campaign recipient previews."),
    ).toBeInTheDocument();
  });
});

function renderPreviewPage(user: typeof campaignManagerUser, roles: string[] = user.roles) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(user));
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <MemoryRouter initialEntries={[`/campaigns/${campaign.id}/recipients/preview`]}>
          <Routes>
            <Route
              path="/campaigns/:campaignId/recipients/preview"
              element={<CampaignRecipientPreviewPage />}
            />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createFetchMock(campaignResponse = campaign) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/launch") && init?.method === "POST") {
      return jsonResponse({ ...campaignResponse, status: "ACTIVE" });
    }
    if (url.endsWith("/recipients/preview")) {
      return jsonResponse(preview);
    }
    if (url.endsWith("/recipients/eligible")) {
      return jsonResponse(eligibleRecipients);
    }
    if (url.endsWith("/recipients/excluded")) {
      return jsonResponse(excludedRecipients);
    }
    if (url.endsWith("/recipients/summary")) {
      return jsonResponse(launchSummary);
    }
    return jsonResponse(campaignResponse);
  });
}

function jsonResponse(data: unknown) {
  return {
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
      data,
    }),
  };
}

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
