import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { ReportsPage } from "@/pages/ReportsPage";

const campaignId = "50000000-0000-0000-0000-000000000442";

const campaignsPayload = [
  {
    id: campaignId,
    name: "Spring Life Drive",
    objective: "Promote life products",
    status: "ACTIVE",
    ownerUserId: "10000000-0000-0000-0000-000000000442",
    ownerFullName: "Campaign Owner",
    segmentId: null,
    segmentName: null,
    channel: "EMAIL",
    messageSubject: "Hello",
    messageBody: "Body",
    startDate: "2026-07-01",
    endDate: "2026-07-31",
    approvedByUserId: null,
    approvedByFullName: null,
    approvedAt: null,
    rejectionReason: null,
    complianceReviewNotes: null,
    productIds: [],
    createdAt: "2026-07-01T00:00:00Z",
    updatedAt: "2026-07-11T00:00:00Z",
  },
];

const exportHistoryPayload = [
  {
    id: "56000000-0000-0000-0000-000000000442",
    requestedByUserId: "10000000-0000-0000-0000-000000000442",
    reportName: "Campaign CSV: Spring Life Drive",
    exportType: "CSV",
    status: "COMPLETED",
    fileUrl: "local://reports/56000000-0000-0000-0000-000000000442/Spring-Life-Drive.csv",
    requestedAt: "2026-07-11T10:00:00Z",
    completedAt: "2026-07-11T10:00:01Z",
  },
  {
    id: "56000000-0000-0000-0000-000000000443",
    requestedByUserId: "10000000-0000-0000-0000-000000000442",
    reportName: "Campaign PDF: Spring Life Drive",
    exportType: "PDF",
    status: "COMPLETED",
    fileUrl: "local://reports/56000000-0000-0000-0000-000000000443/Spring-Life-Drive.pdf",
    requestedAt: "2026-07-11T11:00:00Z",
    completedAt: "2026-07-11T11:00:01Z",
  },
];

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
  return `header.${payload}.signature`;
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  });
}

function blobResponse(
  body: string,
  contentType: string,
  filename: string,
  status = 200,
) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({
      "Content-Type": contentType,
      "Content-Disposition": `attachment; filename="${filename}"`,
    }),
    blob: async () => new Blob([body], { type: contentType }),
  });
}

function renderReportsPage(roles: SystemRoleName[] = ["BI_ANALYST"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000000442",
      email: "bi@bayer-westphalian.test",
      fullName: "BI Analyst",
      status: "ACTIVE",
      lastLoginAt: "2026-07-11T10:00:00Z",
      roles,
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
      <MemoryRouter initialEntries={["/reports"]}>
        <AuthProvider>
          <ReportsPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function createReportsFetchMock(options?: {
  campaigns?: unknown;
  history?: unknown;
}) {
  const campaigns = options?.campaigns ?? campaignsPayload;
  const history = options?.history ?? exportHistoryPayload;

  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.startsWith(`${API_BASE_URL}/campaigns`)) {
      return jsonResponse({
        success: true,
        message: "Campaigns loaded",
        data: campaigns,
      });
    }
    if (url.startsWith(`${API_BASE_URL}/reports/exports`)) {
      return jsonResponse({
        success: true,
        message: "Report export history loaded",
        data: history,
      });
    }
    if (url.endsWith("/csv")) {
      return blobResponse("campaign,sent\nA,10\n", "text/csv", "campaign-report.csv");
    }
    if (url.endsWith("/pdf")) {
      return blobResponse("%PDF-1.4", "application/pdf", "campaign-report.pdf");
    }
    if (url.startsWith(`${API_BASE_URL}/analytics/dashboard`)) {
      return jsonResponse({
        success: true,
        message: "Analytics dashboard loaded",
        data: {
          campaignTotal: 1,
          activeCampaigns: 1,
          audienceSize: 0,
          messagesSent: 0,
          eligibleCount: 0,
          excludedCount: 0,
          openedCount: 0,
          clickedCount: 0,
          repliedCount: 0,
          convertedCount: 0,
          openRate: 0,
          clickRate: 0,
          conversionRate: 0,
          estimatedCost: null,
          estimatedRevenue: null,
          estimatedRoi: null,
          recentCampaignMetrics: [
            {
              metricsId: null,
              campaignId,
              campaignName: "Spring Life Drive",
              campaignStatus: "ACTIVE",
              audienceSize: 10,
              eligibleCount: 8,
              excludedCount: 2,
              sentCount: 8,
              openedCount: 4,
              clickedCount: 2,
              repliedCount: 1,
              convertedCount: 1,
              openRate: 0.5,
              clickRate: 0.25,
              conversionRate: 0.125,
              estimatedCost: 10,
              estimatedRevenue: 20,
              estimatedRoi: 1,
              updatedAt: "2026-07-11T10:00:00Z",
            },
          ],
        },
      });
    }
    return jsonResponse({ message: `Unexpected URL ${url}` }, 404);
  });
}

describe("ReportsPage (items 442 / 445)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("loads export history and enables campaign CSV/PDF export for BI analysts", async () => {
    const fetchMock = createReportsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderReportsPage(["BI_ANALYST"]);

    expect(await screen.findByRole("heading", { name: "Reports" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Report download" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Campaign export" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Export history" })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: "Report export history table" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Campaign CSV: Spring Life Drive")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Campaign CSV" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Campaign PDF" })).toBeEnabled();
    expect(screen.getByLabelText("Download campaign CSV report")).toBeEnabled();
    expect(screen.getByLabelText("Download campaign PDF report")).toBeEnabled();

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/exports`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/campaigns`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
  });

  it("downloads a campaign CSV and refreshes export history", async () => {
    const user = userEvent.setup();
    const fetchMock = createReportsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    const createObjectURL = vi.fn(() => "blob:csv");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", { ...URL, createObjectURL, revokeObjectURL });
    const click = vi.fn();
    const originalAppendChild = document.body.appendChild.bind(document.body);
    vi.spyOn(document.body, "appendChild").mockImplementation((node) => {
      if (node instanceof HTMLAnchorElement) {
        Object.defineProperty(node, "click", { value: click });
      }
      return originalAppendChild(node as Node);
    });
    vi.spyOn(HTMLElement.prototype, "remove").mockImplementation(function (this: HTMLElement) {
      this.parentNode?.removeChild(this);
    });

    renderReportsPage(["BI_ANALYST"]);
    expect(await screen.findByLabelText("Download campaign CSV report")).toBeInTheDocument();

    await user.click(screen.getByLabelText("Download campaign CSV report"));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/campaigns/${campaignId}/csv`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
    expect(createObjectURL).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(
      await screen.findByText(
        "Downloaded CSV report for Spring Life Drive (ACTIVE): campaign-report.csv",
      ),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Last report download summary")).toBeInTheDocument();
  });

  it("downloads a campaign PDF attachment", async () => {
    const user = userEvent.setup();
    const fetchMock = createReportsFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL: vi.fn(() => "blob:pdf"),
      revokeObjectURL: vi.fn(),
    });
    const originalAppendChild = document.body.appendChild.bind(document.body);
    vi.spyOn(document.body, "appendChild").mockImplementation((node) => {
      if (node instanceof HTMLAnchorElement) {
        Object.defineProperty(node, "click", { value: vi.fn() });
      }
      return originalAppendChild(node as Node);
    });
    vi.spyOn(HTMLElement.prototype, "remove").mockImplementation(function (this: HTMLElement) {
      this.parentNode?.removeChild(this);
    });

    renderReportsPage(["CAMPAIGN_MANAGER"]);
    expect(await screen.findByLabelText("Download campaign PDF report")).toBeInTheDocument();
    await user.click(screen.getByLabelText("Download campaign PDF report"));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/campaigns/${campaignId}/pdf`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
    expect(
      await screen.findByText(
        "Downloaded PDF report for Spring Life Drive (ACTIVE): campaign-report.pdf",
      ),
    ).toBeInTheDocument();
  });

  it("denies reports for unauthorized roles without calling report APIs", async () => {
    const fetchMock = createReportsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderReportsPage(["PRODUCT_MANAGER"]);

    expect(
      await screen.findByText("You are not authorized to view or export reports."),
    ).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows empty export history messaging", async () => {
    vi.stubGlobal("fetch", createReportsFetchMock({ history: [] }));

    renderReportsPage(["ADMIN"]);

    expect(await screen.findByText("No report exports have been recorded yet.")).toBeInTheDocument();
  });

  it("filters export history by mine and status", async () => {
    const user = userEvent.setup();
    const fetchMock = createReportsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderReportsPage(["BI_ANALYST"]);
    expect(await screen.findByRole("table", { name: "Report export history table" })).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Filter export history by status"), "FAILED");
    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/exports?status=FAILED`,
        expect.anything(),
      );
    });

    await user.click(screen.getByLabelText("Show only my exports"));
    await waitFor(() => {
      // Both mine and status filters remain applied together.
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/exports?mine=true&status=FAILED`,
        expect.anything(),
      );
    });
  });

  it("uses analytics recent metrics for campaign options when campaigns list is unavailable", async () => {
    const fetchMock = createReportsFetchMock();
    // MARKETING_ANALYST can export reports but cannot list campaigns.
    vi.stubGlobal("fetch", fetchMock);

    renderReportsPage(["MARKETING_ANALYST"]);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/analytics/dashboard`,
        expect.anything(),
      );
    });
    expect(await screen.findByRole("button", { name: "Campaign CSV" })).toBeEnabled();
    expect(screen.getByLabelText("Select campaign for report download")).toHaveDisplayValue(
      /Spring Life Drive/,
    );
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).startsWith(`${API_BASE_URL}/campaigns`)),
    ).toBe(false);
  });

  it("re-downloads a completed export from history (item 445)", async () => {
    const user = userEvent.setup();
    const fetchMock = createReportsFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL: vi.fn(() => "blob:redownload"),
      revokeObjectURL: vi.fn(),
    });
    const originalAppendChild = document.body.appendChild.bind(document.body);
    vi.spyOn(document.body, "appendChild").mockImplementation((node) => {
      if (node instanceof HTMLAnchorElement) {
        Object.defineProperty(node, "click", { value: vi.fn() });
      }
      return originalAppendChild(node as Node);
    });
    vi.spyOn(HTMLElement.prototype, "remove").mockImplementation(function (this: HTMLElement) {
      this.parentNode?.removeChild(this);
    });

    renderReportsPage(["BI_ANALYST"]);

    expect(
      await screen.findByRole("button", {
        name: "Re-download CSV report Campaign CSV: Spring Life Drive",
      }),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", {
        name: "Re-download CSV report Campaign CSV: Spring Life Drive",
      }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/campaigns/${campaignId}/csv`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
    expect(
      await screen.findByText(/Downloaded CSV report for Spring Life Drive/),
    ).toBeInTheDocument();
  });
});
