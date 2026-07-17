import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  CAMPAIGN_BUILDER_PAGE_TITLE,
  CAMPAIGN_CREATE_DRAFT_LABEL,
  CAMPAIGN_DRAFT_CREATED_NOTICE,
  CAMPAIGN_SUBMIT_FOR_REVIEW_LABEL,
} from "@/features/campaigns/campaignCreationFlow";
import { CampaignBuilderPage } from "@/pages/CampaignBuilderPage";

const segment = {
  id: "42000000-0000-4000-8000-000000000201",
  name: "Munich prospects",
  description: "Customers in Munich",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  visibility: "TEAM",
  criteria: [],
  createdAt: "2026-07-09T10:00:00Z",
  updatedAt: "2026-07-09T10:05:00Z",
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

const createdCampaign = {
  id: "50000000-0000-0000-0000-000000000001",
  name: "Life renewal outreach",
  objective: "Promote renewals",
  status: "DRAFT",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  segmentId: segment.id,
  segmentName: segment.name,
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
  productIds: [product.id],
  createdAt: "2026-07-09T10:15:00Z",
  updatedAt: "2026-07-09T10:30:00Z",
};

const copySuggestion = {
  campaignId: null,
  subject: "Protect your next chapter",
  body: "A tailored life insurance option is ready for review.",
  callToAction: "Review the offer",
  explanation: "Rule-based AI-005 draft using campaign objective and product context.",
  confidenceScore: 72,
  requiresHumanApproval: true,
  humanApproved: false,
  approvedByUserId: null,
  storedRecommendationId: "70000000-0000-0000-0000-000000000492",
};

const copyApproval = {
  id: copySuggestion.storedRecommendationId,
  recommendationType: "COPY",
  targetEntityType: "campaign",
  targetEntityId: null,
  inputSummary: "objective=Promote renewals",
  recommendation: "Subject: Protect your next chapter",
  explanation: copySuggestion.explanation,
  confidenceScore: 72,
  approvedByUserId: "10000000-0000-0000-0000-000000000101",
  approvedByFullName: "Campaign Manager",
  reviewNotes: "Reviewed by manager",
  approved: true,
  createdAt: "2026-07-11T10:00:00Z",
};

function renderBuilder(roles: SystemRoleName[] = ["CAMPAIGN_MANAGER"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000000101",
      email: "campaign.manager@bayer-westphalian.test",
      fullName: "Campaign Manager",
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
      <MemoryRouter initialEntries={["/campaign-builder"]}>
        <AuthProvider>
          <CampaignBuilderPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function completeBasics(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Campaign name"), "Life renewal outreach");
  await user.type(screen.getByLabelText("Campaign objective"), "Promote renewals");
  await user.click(screen.getByRole("button", { name: "Continue to audience" }));
  expect(await screen.findByLabelText("Campaign audience segment")).toBeInTheDocument();
}

async function completeAudience(user: ReturnType<typeof userEvent.setup>) {
  await user.selectOptions(screen.getByLabelText("Campaign audience segment"), segment.id);
  await user.selectOptions(screen.getByLabelText("Campaign product"), product.id);
  await user.click(screen.getByRole("button", { name: "Continue to message" }));
  expect(await screen.findByLabelText("Message subject")).toBeInTheDocument();
}

async function completeMessage(
  user: ReturnType<typeof userEvent.setup>,
  subject = "Renew your cover",
  body = "Dear customer, ...",
) {
  await user.type(screen.getByLabelText("Message subject"), subject);
  await user.type(screen.getByLabelText("Campaign message body"), body);
  await user.click(screen.getByRole("button", { name: "Continue to schedule" }));
  expect(await screen.findByLabelText("Campaign schedule date")).toBeInTheDocument();
}

async function completeSchedule(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Campaign schedule date"), "2026-09-01");
  await user.type(screen.getByLabelText("Campaign end date"), "2026-09-30");
  await user.click(screen.getByRole("button", { name: "Continue to review" }));
  expect(await screen.findByLabelText("Campaign builder review")).toBeInTheDocument();
}

describe("CampaignBuilderPage (item 592)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it(
    "guides the manager through steps to create and submit a draft campaign",
    async () => {
    const user = userEvent.setup({ delay: null });
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderBuilder(["CAMPAIGN_MANAGER"]);

    expect(
      await screen.findByRole("heading", { name: CAMPAIGN_BUILDER_PAGE_TITLE }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Follow the steps to define the campaign/i),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Campaign builder steps")).toBeInTheDocument();
    expect(screen.getByText(/Step 1 of 5/i)).toBeInTheDocument();

    await completeBasics(user);
    expect(await screen.findByText("Munich prospects")).toBeInTheDocument();
    expect(await screen.findByText("Life Protection")).toBeInTheDocument();

    await completeAudience(user);
    await completeMessage(user);
    await completeSchedule(user);

    await user.click(screen.getByRole("button", { name: CAMPAIGN_CREATE_DRAFT_LABEL }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns` &&
          (init as RequestInit | undefined)?.method === "POST",
      );
      expect(createCall).toBeDefined();
      expect(JSON.parse((createCall?.[1] as RequestInit).body as string)).toMatchObject({
        name: "Life renewal outreach",
        objective: "Promote renewals",
        segmentId: segment.id,
        channel: "EMAIL",
        messageSubject: "Renew your cover",
        messageBody: "Dear customer, ...",
        startDate: "2026-09-01",
        endDate: "2026-09-30",
      });
    });

    await waitFor(() => {
      const productCall = fetchMock.mock.calls.find(([url]) =>
        String(url).endsWith(`/campaigns/${createdCampaign.id}/products`),
      );
      expect(productCall).toBeDefined();
      expect(JSON.parse((productCall?.[1] as RequestInit).body as string)).toEqual({
        productIds: [product.id],
      });
    });

    expect(await screen.findByText(CAMPAIGN_DRAFT_CREATED_NOTICE)).toBeInTheDocument();
    expect(screen.getAllByLabelText("Campaign status: Draft").length).toBeGreaterThanOrEqual(1);

    await user.click(screen.getByRole("button", { name: CAMPAIGN_SUBMIT_FOR_REVIEW_LABEL }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/campaigns/${createdCampaign.id}/submit`,
        expect.objectContaining({ method: "POST" }),
      );
    });
    },
    20_000,
  );

  it("validates the current step before allowing progress", async () => {
    const user = userEvent.setup({ delay: null });
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderBuilder(["CAMPAIGN_MANAGER"]);

    await screen.findByLabelText("Campaign name");
    await user.click(screen.getByRole("button", { name: "Continue to audience" }));

    expect(await screen.findByText("Campaign name is required.")).toBeInTheDocument();
    expect(screen.getByText("Campaign objective is required.")).toBeInTheDocument();
    expect(screen.queryByLabelText("Campaign product")).not.toBeInTheDocument();

    await completeBasics(user);
    await user.click(screen.getByRole("button", { name: "Continue to message" }));

    expect(await screen.findByText("Audience segment is required.")).toBeInTheDocument();
    expect(screen.getByText("Product is required.")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns` &&
          (init as RequestInit | undefined)?.method === "POST",
      ),
    ).toBe(false);
  });

  it("blocks create draft from review until segment, product, message, and schedule are complete", async () => {
    const user = userEvent.setup();
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderBuilder(["CAMPAIGN_MANAGER"]);
    await completeBasics(user);
    // Jump ahead via stepper without completing audience/message/schedule.
    await user.click(screen.getByRole("button", { name: /Review/i }));
    expect(await screen.findByLabelText("Campaign builder review")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: CAMPAIGN_CREATE_DRAFT_LABEL }));

    // First incomplete step after basics is audience.
    expect(await screen.findByLabelText("Campaign audience segment")).toBeInTheDocument();
    expect(screen.getByText("Audience segment is required.")).toBeInTheDocument();
    expect(screen.getByText("Product is required.")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/campaigns` &&
          (init as RequestInit | undefined)?.method === "POST",
      ),
    ).toBe(false);
  });

  it(
    "requires human approval before applying AI campaign copy on the message step",
    async () => {
    const user = userEvent.setup({ delay: null });
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderBuilder(["CAMPAIGN_MANAGER"]);

    await completeBasics(user);
    await completeAudience(user);

    expect(
      await screen.findByRole("button", { name: "Generate AI campaign copy suggestion" }),
    ).toBeInTheDocument();
    const subjectBefore = (screen.getByLabelText("Message subject") as HTMLInputElement).value;
    await user.click(
      screen.getByRole("button", { name: "Generate AI campaign copy suggestion" }),
    );

    expect(await screen.findByText("Protect your next chapter")).toBeInTheDocument();
    expect(screen.getByText(copySuggestion.explanation)).toBeInTheDocument();
    expect(screen.getByText("Awaiting human approval")).toBeInTheDocument();
    expect(screen.getByTestId("ai-copy-pending-banner")).toHaveTextContent(
      "human review required",
    );
    // Suggestion must not auto-write into campaign form fields before approval.
    expect(screen.getByLabelText("Message subject")).toHaveValue(subjectBefore);

    await waitFor(() => {
      const generateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/ai/campaign-copy` &&
          (init as RequestInit | undefined)?.method === "POST",
      );
      expect(generateCall).toBeDefined();
      expect(JSON.parse((generateCall?.[1] as RequestInit).body as string)).toMatchObject({
        objective: "Promote renewals",
        productName: "Life Protection",
        channel: "EMAIL",
        audienceHint: "Munich prospects",
      });
    });

    await user.type(screen.getByLabelText("AI copy review notes"), "Reviewed by manager");
    await user.click(screen.getByRole("button", { name: "Approve and Apply AI copy" }));

    const dialog = await screen.findByRole("dialog", { name: "Approve AI Copy Suggestion?" });
    expect(within(dialog).getByText(/does not approve the campaign for launch/i)).toBeInTheDocument();
    await user.click(within(dialog).getByRole("button", { name: "Cancel" }));
    expect(screen.getByLabelText("Message subject")).toHaveValue(subjectBefore);

    await user.click(screen.getByRole("button", { name: "Approve and Apply AI copy" }));
    await user.click(
      within(await screen.findByRole("dialog", { name: "Approve AI Copy Suggestion?" })).getByRole(
        "button",
        { name: "Approve and Apply" },
      ),
    );

    await waitFor(() => {
      const approvalCall = fetchMock.mock.calls.find(([url]) =>
        String(url).endsWith(`/ai/campaign-copy/${copySuggestion.storedRecommendationId}/approve`),
      );
      expect(approvalCall).toBeDefined();
      expect(JSON.parse((approvalCall?.[1] as RequestInit).body as string)).toMatchObject({
        reviewNotes: "Reviewed by manager",
        editedSubject: copySuggestion.subject,
        editedMessageBody: copySuggestion.body,
      });
    });

    expect(await screen.findByText("Human approved")).toBeInTheDocument();
    expect(screen.getByText(/APPROVED BY USER/i)).toBeInTheDocument();
    expect(screen.getByText(/Compliance approval/i)).toBeInTheDocument();
    expect(screen.getByText(/Still required before launch/i)).toBeInTheDocument();
    expect(screen.getByLabelText("Message subject")).toHaveValue(copySuggestion.subject);
    expect(screen.getByLabelText("Campaign message body")).toHaveValue(
      `${copySuggestion.body}\n\n${copySuggestion.callToAction}`,
    );
    expect(
      await screen.findByText(/Campaign remains DRAFT/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Approve and Apply AI copy" }),
    ).toBeDisabled();
    },
    20_000,
  );

  it("shows a live summary that updates across steps", async () => {
    const user = userEvent.setup({ delay: null });
    vi.stubGlobal("fetch", createFetchMock());
    renderBuilder(["CAMPAIGN_MANAGER"]);

    const summary = await screen.findByLabelText("Campaign builder live summary");
    expect(within(summary).getByText("Not created")).toBeInTheDocument();

    await completeBasics(user);
    await completeAudience(user);

    expect(within(summary).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(summary).getByText("Munich prospects")).toBeInTheDocument();
    expect(within(summary).getByText("Life Protection")).toBeInTheDocument();
  });

  it("blocks campaign building for users without campaign management access", () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderBuilder(["BI_ANALYST"]);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "You are not authorized to build campaigns.",
    );
    expect(
      screen.queryByRole("button", { name: CAMPAIGN_CREATE_DRAFT_LABEL }),
    ).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Campaign builder steps")).not.toBeInTheDocument();
  });
});

function createFetchMock() {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";

    if (url.includes("/segments")) {
      return jsonResponse([segment]);
    }
    if (url.endsWith("/ai/campaign-copy") && method === "POST") {
      return jsonResponse(copySuggestion);
    }
    if (
      url.endsWith(`/ai/campaign-copy/${copySuggestion.storedRecommendationId}/approve`) &&
      method === "POST"
    ) {
      return jsonResponse(copyApproval);
    }
    if (url.endsWith("/campaigns") && method === "POST") {
      return jsonResponse(createdCampaign);
    }
    if (url.endsWith(`/campaigns/${createdCampaign.id}/products`) && method === "PUT") {
      return jsonResponse(createdCampaign);
    }
    if (url.endsWith(`/campaigns/${createdCampaign.id}/submit`) && method === "POST") {
      return jsonResponse({ ...createdCampaign, status: "SUBMITTED" });
    }
    if (url.includes("/products")) {
      return jsonResponse([product]);
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
