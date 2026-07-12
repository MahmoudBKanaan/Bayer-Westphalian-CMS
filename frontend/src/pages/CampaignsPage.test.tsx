import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { campaignFormValidationMessages } from "@/features/campaigns/campaignFormValidation";
import { CampaignsPage } from "@/pages/CampaignsPage";

const campaignManagerUser = {
  id: "10000000-0000-0000-0000-000000000101",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["CAMPAIGN_MANAGER"],
};

const complianceUser = {
  id: "10000000-0000-0000-0000-000000000106",
  email: "compliance.officer@bayer-westphalian.test",
  fullName: "Compliance Officer",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["COMPLIANCE_OFFICER"],
};

const marketingAnalystUser = {
  id: "10000000-0000-0000-0000-000000000109",
  email: "marketing.analyst@bayer-westphalian.test",
  fullName: "Marketing Analyst",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["MARKETING_ANALYST"],
};

const draftCampaign = {
  id: "50000000-0000-0000-0000-000000000001",
  name: "Life renewal outreach",
  objective: "Promote life insurance renewals",
  status: "DRAFT",
  ownerUserId: campaignManagerUser.id,
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
  productIds: ["41000000-0000-0000-0000-000000000201"],
  createdAt: "2026-07-09T10:15:00Z",
  updatedAt: "2026-07-09T11:00:00Z",
};

const submittedCampaign = {
  ...draftCampaign,
  id: "50000000-0000-0000-0000-000000000002",
  name: "Submitted outreach",
  status: "SUBMITTED",
};

const editedDraftCampaign = {
  ...draftCampaign,
  name: "Edited life renewal",
  objective: "Edited renewal objective",
  channel: "SMS",
  messageSubject: "Edited subject",
  messageBody: "Edited body",
  startDate: "2026-10-01",
  endDate: "2026-10-31",
  updatedAt: "2026-07-09T12:00:00Z",
};

const product = {
  id: "41000000-0000-0000-0000-000000000201",
  name: "Life Protection",
  productType: "LIFE_INSURANCE",
  description: "Comprehensive life insurance coverage",
  price: 129.99,
  durationMonths: 12,
  expirationPolicy: "Annual renewal",
  active: true,
  deleted: false,
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

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

function renderCampaignsPage(user: typeof campaignManagerUser) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(user.roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(user));

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <MemoryRouter>
          <CampaignsPage />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createCampaignsFetchMock(campaigns: unknown[] = [draftCampaign]) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url === `${API_BASE_URL}/campaigns` && init?.method === "POST") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        success: true,
        message: "Campaign created",
        data: {
          ...draftCampaign,
          ...body,
          id: "50000000-0000-0000-0000-000000000003",
        },
      });
    }
    if (url === `${API_BASE_URL}/campaigns/${draftCampaign.id}` && init?.method === "PUT") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        success: true,
        message: "Campaign updated",
        data: {
          ...editedDraftCampaign,
          ...body,
          id: draftCampaign.id,
          status: "DRAFT",
        },
      });
    }
    if (url === `${API_BASE_URL}/campaigns/${draftCampaign.id}/products` && init?.method === "PUT") {
      const body = JSON.parse(init.body as string) as { productIds?: string[] };
      return jsonResponse({
        success: true,
        message: "Campaign products updated",
        data: {
          ...editedDraftCampaign,
          productIds: body.productIds ?? [],
        },
      });
    }
    if (
      url === `${API_BASE_URL}/campaigns/50000000-0000-0000-0000-000000000003/products` &&
      init?.method === "PUT"
    ) {
      return jsonResponse({
        success: true,
        message: "Campaign products updated",
        data: {
          ...draftCampaign,
          id: "50000000-0000-0000-0000-000000000003",
          productIds: [product.id],
        },
      });
    }
    if (url.includes("/submit") && init?.method === "POST") {
      return jsonResponse({
        success: true,
        message: "Campaign submitted",
        data: { ...draftCampaign, status: "SUBMITTED" },
      });
    }
    if (url.includes("/approve") && init?.method === "POST") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        success: true,
        message: "Campaign approved",
        data: { ...submittedCampaign, status: "APPROVED", ...body },
      });
    }
    if (url.includes("/reject") && init?.method === "POST") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        success: true,
        message: "Campaign rejected",
        data: { ...submittedCampaign, status: "REJECTED", ...body },
      });
    }
    if (url.startsWith(`${API_BASE_URL}/campaigns`)) {
      return jsonResponse({
        success: true,
        message: "Campaigns loaded",
        data: campaigns,
      });
    }
    if (url.startsWith(`${API_BASE_URL}/products`)) {
      return jsonResponse({
        success: true,
        message: "Products loaded",
        data: [product],
      });
    }
    return jsonResponse({ success: false, message: "Not found", data: null }, 404);
  });
}

async function findCampaignsTable() {
  return screen.findByRole("table", { name: "Campaign worklist" });
}

describe("CampaignsPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads campaigns and shows create and edit forms for campaign managers", async () => {
    vi.stubGlobal("fetch", createCampaignsFetchMock([draftCampaign, submittedCampaign]));

    renderCampaignsPage(campaignManagerUser);

    expect(await screen.findByRole("heading", { name: "Campaigns" })).toBeInTheDocument();
    expect(
      screen.getByText("Campaign definitions, compliance review, and lifecycle control"),
    ).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Campaign search filters" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Create campaign" })).toBeInTheDocument();
    expect(await screen.findByLabelText("Campaign product")).toHaveValue("");
    expect(screen.getByRole("heading", { name: "Edit campaign" })).toBeInTheDocument();
    expect(await screen.findByLabelText("Edit campaign product")).toHaveValue(product.id);
    const table = await findCampaignsTable();
    expect(within(table).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(table).getByText("Submitted outreach")).toBeInTheDocument();
    expect(
      within(table).getByText(
        /Campaign worklist table with owner, segment, channel, status, schedule, update time, and recipient counts./,
      ),
    ).toHaveClass("sr-only");
    expect(within(table).getByRole("columnheader", { name: "Campaign" })).toHaveAttribute(
      "scope",
      "col",
    );
    expect(within(table).getByLabelText("Campaign status: Draft")).toHaveClass(
      "campaign-status-draft",
    );
    expect(within(table).getByLabelText("Campaign status: Submitted")).toHaveClass(
      "campaign-status-submitted",
    );
  });

  it("links campaign rows to the recipient preview screen", async () => {
    vi.stubGlobal("fetch", createCampaignsFetchMock([draftCampaign]));

    renderCampaignsPage(campaignManagerUser);

    await findCampaignsTable();

    const previewLink = screen.getByRole("link", { name: "Preview" });
    expect(previewLink).toHaveAttribute(
      "href",
      `/campaigns/${draftCampaign.id}/recipients/preview`,
    );
  });

  it("allows a campaign manager to edit a draft campaign", async () => {
    const fetchMock = createCampaignsFetchMock([draftCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(campaignManagerUser);
    await findCampaignsTable();

    const editPanel = screen.getByRole("heading", { name: "Edit campaign" }).closest("section");
    expect(editPanel).not.toBeNull();
    const panel = within(editPanel as HTMLElement);

    await userEvent.clear(panel.getByLabelText("Campaign name"));
    await userEvent.type(panel.getByLabelText("Campaign name"), "Edited life renewal");
    await userEvent.clear(panel.getByLabelText("Campaign objective"));
    await userEvent.type(panel.getByLabelText("Campaign objective"), "Edited renewal objective");
    await userEvent.selectOptions(panel.getByLabelText("Campaign channel"), "SMS");
    expect(panel.getByLabelText("Edit campaign product")).toHaveValue(product.id);
    await userEvent.selectOptions(panel.getByLabelText("Edit campaign product"), product.id);
    await userEvent.clear(panel.getByLabelText("Campaign message subject"));
    await userEvent.type(panel.getByLabelText("Campaign message subject"), "Edited subject");
    await userEvent.clear(panel.getByLabelText("Campaign message body"));
    await userEvent.type(panel.getByLabelText("Campaign message body"), "Edited body");
    fireEvent.change(panel.getByLabelText("Campaign schedule date"), {
      target: { value: "2026-10-01" },
    });
    fireEvent.change(panel.getByLabelText("Campaign end date"), {
      target: { value: "2026-10-31" },
    });
    await userEvent.click(panel.getByRole("button", { name: "Save campaign" }));

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns/${draftCampaign.id}` &&
          (init as RequestInit | undefined)?.method === "PUT",
      );
      expect(updateCall).toBeDefined();
      expect(JSON.parse((updateCall?.[1] as RequestInit).body as string)).toEqual({
        name: "Edited life renewal",
        objective: "Edited renewal objective",
        segmentId: "42000000-0000-4000-8000-000000000201",
        channel: "SMS",
        messageSubject: "Edited subject",
        messageBody: "Edited body",
        startDate: "2026-10-01",
        endDate: "2026-10-31",
      });
    });
    await waitFor(() => {
      const productCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns/${draftCampaign.id}/products` &&
          (init as RequestInit | undefined)?.method === "PUT",
      );
      expect(productCall).toBeDefined();
      expect(JSON.parse((productCall?.[1] as RequestInit).body as string)).toEqual({
        productIds: [product.id],
      });
    });
    expect(await screen.findByText("Campaign updated.")).toBeInTheDocument();
  });

  it("prevents editing a submitted campaign as a draft", async () => {
    const fetchMock = createCampaignsFetchMock([draftCampaign, submittedCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(campaignManagerUser);
    await findCampaignsTable();

    await userEvent.selectOptions(screen.getByLabelText("Selected campaign"), submittedCampaign.id);

    const editPanel = screen.getByRole("heading", { name: "Edit campaign" }).closest("section");
    expect(editPanel).not.toBeNull();
    const panel = within(editPanel as HTMLElement);

    expect(panel.getByRole("button", { name: "Save campaign" })).toBeDisabled();
    await userEvent.click(panel.getByRole("button", { name: "Save campaign" }));

    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns/${submittedCampaign.id}` &&
          (init as RequestInit | undefined)?.method === "PUT",
      ),
    ).toBe(false);
  });

  it(
    "allows a campaign manager to create a draft campaign",
    async () => {
      const fetchMock = createCampaignsFetchMock([draftCampaign]);
      vi.stubGlobal("fetch", fetchMock);

      renderCampaignsPage(campaignManagerUser);
      await findCampaignsTable();

      const createPanel = screen.getByRole("heading", { name: "Create campaign" }).closest("section");
      expect(createPanel).not.toBeNull();
      const panel = within(createPanel as HTMLElement);

      fireEvent.change(panel.getByLabelText("Campaign name"), {
        target: { value: "Draft campaign" },
      });
      fireEvent.change(panel.getByLabelText("Campaign objective"), {
        target: { value: "Create a draft first" },
      });
      fireEvent.change(panel.getByLabelText("Campaign channel"), { target: { value: "SMS" } });
      expect(panel.getByLabelText("Campaign product")).toHaveValue("");
      fireEvent.change(panel.getByLabelText("Campaign product"), { target: { value: product.id } });
      fireEvent.change(panel.getByLabelText("Campaign segment id"), {
        target: { value: "42000000-0000-4000-8000-000000000201" },
      });
      fireEvent.change(panel.getByLabelText("Campaign message subject"), {
        target: { value: "Draft subject" },
      });
      fireEvent.change(panel.getByLabelText("Campaign message body"), {
        target: { value: "Draft body" },
      });
      fireEvent.change(panel.getByLabelText("Campaign schedule date"), {
        target: { value: "2026-09-01" },
      });
      fireEvent.change(panel.getByLabelText("Campaign end date"), {
        target: { value: "2026-09-30" },
      });
      fireEvent.click(panel.getByRole("button", { name: "Create campaign" }));

      await waitFor(() => {
        const createCall = fetchMock.mock.calls.find(
          ([url, init]) =>
            String(url) === `${API_BASE_URL}/campaigns` &&
            (init as RequestInit | undefined)?.method === "POST",
        );
        expect(createCall).toBeDefined();
        expect(JSON.parse((createCall?.[1] as RequestInit).body as string)).toEqual({
          name: "Draft campaign",
          objective: "Create a draft first",
          segmentId: "42000000-0000-4000-8000-000000000201",
          channel: "SMS",
          messageSubject: "Draft subject",
          messageBody: "Draft body",
          startDate: "2026-09-01",
          endDate: "2026-09-30",
        });
      });
      await waitFor(() => {
        const productCall = fetchMock.mock.calls.find(([url, init]) =>
          String(url).endsWith("/campaigns/50000000-0000-0000-0000-000000000003/products") &&
          (init as RequestInit | undefined)?.method === "PUT",
        );
        expect(productCall).toBeDefined();
        expect(JSON.parse((productCall?.[1] as RequestInit).body as string)).toEqual({
          productIds: [product.id],
        });
      });
      expect(await screen.findByText("Campaign created.")).toBeInTheDocument();
    },
    10_000,
  );

  it("allows a campaign manager to submit an existing draft campaign", async () => {
    const fetchMock = createCampaignsFetchMock([draftCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(campaignManagerUser);
    await findCampaignsTable();

    await userEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/campaigns/${draftCampaign.id}/submit`,
        expect.objectContaining({ method: "POST" }),
      );
    });
    expect(await screen.findByText("Campaign submitted.")).toBeInTheDocument();
  });

  it("applies campaign search filters", async () => {
    const fetchMock = createCampaignsFetchMock([draftCampaign, submittedCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(campaignManagerUser);
    await findCampaignsTable();

    fireEvent.change(screen.getByLabelText("Search campaigns"), { target: { value: "life" } });
    fireEvent.change(screen.getByLabelText("Campaign status filter"), {
      target: { value: "SUBMITTED" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/campaigns?term=life&status=SUBMITTED`,
        expect.any(Object),
      );
    });
    expect(screen.getByText("2 active filters")).toBeInTheDocument();
  });

  it(
    "allows a campaign manager to create and submit a campaign",
    async () => {
      const fetchMock = createCampaignsFetchMock([draftCampaign, submittedCampaign]);
      vi.stubGlobal("fetch", fetchMock);

      renderCampaignsPage(campaignManagerUser);
      await findCampaignsTable();

      fireEvent.click(screen.getByRole("button", { name: "Submit" }));

      await waitFor(() => {
        expect(fetchMock).toHaveBeenCalledWith(
          `${API_BASE_URL}/campaigns/${draftCampaign.id}/submit`,
          expect.objectContaining({ method: "POST" }),
        );
      });

      const createPanel = screen.getByRole("heading", { name: "Create campaign" }).closest("section");
      expect(createPanel).not.toBeNull();
      const panel = within(createPanel as HTMLElement);

      fireEvent.change(panel.getByLabelText("Campaign name"), { target: { value: "New renewal" } });
      fireEvent.change(panel.getByLabelText("Campaign objective"), {
        target: { value: "Retain customers" },
      });
      fireEvent.change(panel.getByLabelText("Campaign product"), { target: { value: product.id } });
      fireEvent.change(panel.getByLabelText("Campaign segment id"), {
        target: { value: "42000000-0000-4000-8000-000000000201" },
      });
      fireEvent.change(panel.getByLabelText("Campaign message subject"), {
        target: { value: "Renew now" },
      });
      fireEvent.change(panel.getByLabelText("Campaign message body"), {
        target: { value: "Renewal details" },
      });
      fireEvent.change(panel.getByLabelText("Campaign schedule date"), {
        target: { value: "2026-09-01" },
      });
      fireEvent.change(panel.getByLabelText("Campaign end date"), {
        target: { value: "2026-09-30" },
      });
      fireEvent.click(panel.getByRole("button", { name: "Create campaign" }));

      await waitFor(() => {
        const createCall = fetchMock.mock.calls.find(
          ([url, init]) =>
            String(url) === `${API_BASE_URL}/campaigns` &&
            (init as RequestInit | undefined)?.method === "POST",
        );
        expect(createCall).toBeDefined();
        expect(JSON.parse((createCall?.[1] as RequestInit).body as string)).toMatchObject({
          name: "New renewal",
          objective: "Retain customers",
          segmentId: "42000000-0000-4000-8000-000000000201",
          channel: "EMAIL",
          messageSubject: "Renew now",
          messageBody: "Renewal details",
          startDate: "2026-09-01",
          endDate: "2026-09-30",
        });
      });
    },
    10_000,
  );

  it("allows compliance users to approve submitted campaigns", async () => {
    const fetchMock = createCampaignsFetchMock([submittedCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(complianceUser);
    await findCampaignsTable();

    fireEvent.change(screen.getByLabelText("Compliance review notes"), {
      target: { value: "Approved" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Approve" }));

    await waitFor(() => {
      const approveCall = fetchMock.mock.calls.find(([url]) =>
        String(url).endsWith(`/campaigns/${submittedCampaign.id}/approve`),
      );
      expect(approveCall).toBeDefined();
      expect(JSON.parse((approveCall?.[1] as RequestInit).body as string)).toEqual({
        complianceReviewNotes: "Approved",
      });
    });
  });

  it("allows compliance users to reject submitted campaigns", async () => {
    const fetchMock = createCampaignsFetchMock([submittedCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(complianceUser);
    await findCampaignsTable();

    fireEvent.change(screen.getByLabelText("Rejection reason"), {
      target: { value: "Missing consent" },
    });
    fireEvent.change(screen.getByLabelText("Rejection compliance notes"), {
      target: { value: "Fix wording" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Reject" }));

    await waitFor(() => {
      const rejectCall = fetchMock.mock.calls.find(([url]) =>
        String(url).endsWith(`/campaigns/${submittedCampaign.id}/reject`),
      );
      expect(rejectCall).toBeDefined();
      expect(JSON.parse((rejectCall?.[1] as RequestInit).body as string)).toEqual({
        rejectionReason: "Missing consent",
        complianceReviewNotes: "Fix wording",
      });
    });
  });

  it("blocks roles outside the campaign permission matrix", () => {
    const fetchMock = createCampaignsFetchMock([draftCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(marketingAnalystUser);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "You are not authorized to view campaigns.",
    );
    expect(screen.queryByRole("table", { name: "Campaign worklist" })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("reports campaign list authorization failures as view failures, not manage failures", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.startsWith(`${API_BASE_URL}/campaigns`)) {
        return jsonResponse(
          { success: false, message: "Forbidden", data: null },
          403,
        );
      }
      return jsonResponse({ success: false, message: "Not found", data: null }, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(campaignManagerUser);

    expect(await screen.findByText("You are not authorized to view campaigns.")).toBeInTheDocument();
    expect(screen.queryByText("You are not authorized to manage campaigns.")).not.toBeInTheDocument();
    expect(screen.getByText("Campaign records could not be loaded.")).toBeInTheDocument();
  });

  it("offers campaign report download actions for authorized report roles (item 445)", async () => {
    const user = userEvent.setup();
    const baseMock = createCampaignsFetchMock([draftCampaign]);
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/csv")) {
        return {
          ok: true,
          status: 200,
          headers: new Headers({
            "Content-Type": "text/csv",
            "Content-Disposition": 'attachment; filename="life-renewal.csv"',
          }),
          blob: async () => new Blob(["csv"], { type: "text/csv" }),
        };
      }
      return baseMock(input, init);
    });
    vi.stubGlobal("fetch", fetchMock);
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL: vi.fn(() => "blob:campaign-csv"),
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

    renderCampaignsPage(campaignManagerUser);

    expect(
      await screen.findByLabelText("Report downloads for Life renewal outreach"),
    ).toBeInTheDocument();
    await user.click(
      screen.getByLabelText("Download CSV report for Life renewal outreach"),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reports/campaigns/${draftCampaign.id}/csv`,
        expect.objectContaining({
          headers: expect.objectContaining({ Authorization: expect.stringContaining("Bearer") }),
        }),
      );
    });
    expect(
      await screen.findByText(/Downloaded CSV report for Life renewal outreach/),
    ).toBeInTheDocument();
  });
});

describe("CampaignsPage form validation messages (item 242)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("shows campaign form validation messages and blocks create post", async () => {
    const fetchMock = createCampaignsFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(campaignManagerUser);
    expect(
      await screen.findByRole("heading", { name: "Create campaign" }),
    ).toBeInTheDocument();

    const createPanel = screen.getByRole("heading", { name: "Create campaign" }).closest("section");
    expect(createPanel).not.toBeNull();
    const panel = createPanel as HTMLElement;

    await userEvent.type(within(panel).getByLabelText("Campaign segment id"), "bad-segment");
    fireEvent.change(within(panel).getByLabelText("Campaign schedule date"), {
      target: { value: "2026-10-15" },
    });
    fireEvent.change(within(panel).getByLabelText("Campaign end date"), {
      target: { value: "2026-10-01" },
    });
    await userEvent.click(within(panel).getByRole("button", { name: "Create campaign" }));

    expect(screen.getByText(campaignFormValidationMessages.nameRequired)).toBeInTheDocument();
    expect(screen.getByText(campaignFormValidationMessages.objectiveRequired)).toBeInTheDocument();
    expect(screen.getByText(campaignFormValidationMessages.messageSubjectRequired)).toBeInTheDocument();
    expect(screen.getByText(campaignFormValidationMessages.messageBodyRequired)).toBeInTheDocument();
    expect(screen.getByText(campaignFormValidationMessages.segmentIdInvalid)).toBeInTheDocument();
    expect(screen.getByText(campaignFormValidationMessages.endDateBeforeStart)).toBeInTheDocument();
    expect(screen.getByText("Product is required.")).toBeInTheDocument();

    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns` &&
          (init as RequestInit | undefined)?.method === "POST",
      ),
    ).toBe(false);
  });

  it("shows rejection reason validation message and blocks reject post", async () => {
    const fetchMock = createCampaignsFetchMock([submittedCampaign]);
    vi.stubGlobal("fetch", fetchMock);

    renderCampaignsPage(complianceUser);
    await findCampaignsTable();

    await userEvent.click(screen.getByRole("button", { name: "Reject" }));

    expect(
      await screen.findByText(campaignFormValidationMessages.rejectionReasonRequired),
    ).toBeInTheDocument();

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          ([url, init]) =>
            String(url).includes("/reject") && (init as RequestInit | undefined)?.method === "POST",
        ),
      ).toBe(false);
    });
  });
});
