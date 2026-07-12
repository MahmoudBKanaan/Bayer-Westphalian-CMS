/**
 * Campaign creation UI integration (KB item 603 / FR-050 / FR-057).
 *
 * Full route tree: authorized user opens Campaign Builder, walks steps, creates
 * DRAFT via POST /campaigns + product selection; unauthorized roles are blocked.
 */
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CAMPAIGN_BUILDER_PAGE_TITLE,
  CAMPAIGN_CREATE_DRAFT_LABEL,
  CAMPAIGN_CREATION_FIXTURES,
  CAMPAIGN_DRAFT_CREATED_NOTICE,
  campaignFormValidationMessages,
} from "@/features/campaigns/campaignCreationFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const segment = {
  id: CAMPAIGN_CREATION_FIXTURES.segmentId,
  name: CAMPAIGN_CREATION_FIXTURES.segmentName,
  description: "Integration segment",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  visibility: "TEAM",
  criteria: [],
  createdAt: "2026-07-09T10:00:00Z",
  updatedAt: "2026-07-09T10:05:00Z",
};

const product = {
  id: CAMPAIGN_CREATION_FIXTURES.productId,
  name: CAMPAIGN_CREATION_FIXTURES.productName,
  productType: "INVESTMENT_FUND",
  description: "Integration product",
  price: 99,
  durationMonths: 12,
  expirationPolicy: "Annual",
  active: true,
  deleted: false,
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

function campaignCreationHandlers() {
  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url) => url.includes("/segments"),
      response: () => jsonOk([segment]),
    },
    {
      match: (url, method) => url.includes("/products") && method === "GET",
      response: () => jsonOk([product]),
    },
    {
      match: (url, method) => {
        if (method !== "POST") {
          return false;
        }
        try {
          const pathname = new URL(url).pathname.replace(/\/+$/, "");
          return pathname.endsWith("/campaigns");
        } catch {
          return url.endsWith("/campaigns");
        }
      },
      response: () =>
        jsonOk(
          {
            id: CAMPAIGN_CREATION_FIXTURES.campaignId,
            name: CAMPAIGN_CREATION_FIXTURES.name,
            objective: CAMPAIGN_CREATION_FIXTURES.objective,
            status: "DRAFT",
            ownerUserId: "10000000-0000-0000-0000-000000000101",
            ownerFullName: "Campaign Manager",
            segmentId: segment.id,
            segmentName: segment.name,
            channel: CAMPAIGN_CREATION_FIXTURES.channel,
            messageSubject: CAMPAIGN_CREATION_FIXTURES.messageSubject,
            messageBody: CAMPAIGN_CREATION_FIXTURES.messageBody,
            startDate: CAMPAIGN_CREATION_FIXTURES.startDate,
            endDate: CAMPAIGN_CREATION_FIXTURES.endDate,
            approvedByUserId: null,
            approvedByFullName: null,
            approvedAt: null,
            rejectionReason: null,
            complianceReviewNotes: null,
            productIds: [],
            createdAt: "2026-07-12T12:00:00Z",
            updatedAt: "2026-07-12T12:00:00Z",
          },
          "Campaign created",
        ),
    },
    {
      match: (url, method) =>
        url.includes(`/campaigns/${CAMPAIGN_CREATION_FIXTURES.campaignId}/products`) &&
        method === "PUT",
      response: () =>
        jsonOk(
          {
            id: CAMPAIGN_CREATION_FIXTURES.campaignId,
            name: CAMPAIGN_CREATION_FIXTURES.name,
            objective: CAMPAIGN_CREATION_FIXTURES.objective,
            status: "DRAFT",
            ownerUserId: "10000000-0000-0000-0000-000000000101",
            ownerFullName: "Campaign Manager",
            segmentId: segment.id,
            segmentName: segment.name,
            channel: CAMPAIGN_CREATION_FIXTURES.channel,
            messageSubject: CAMPAIGN_CREATION_FIXTURES.messageSubject,
            messageBody: CAMPAIGN_CREATION_FIXTURES.messageBody,
            startDate: CAMPAIGN_CREATION_FIXTURES.startDate,
            endDate: CAMPAIGN_CREATION_FIXTURES.endDate,
            approvedByUserId: null,
            approvedByFullName: null,
            approvedAt: null,
            rejectionReason: null,
            complianceReviewNotes: null,
            productIds: [product.id],
            createdAt: "2026-07-12T12:00:00Z",
            updatedAt: "2026-07-12T12:00:00Z",
          },
          "Products selected",
        ),
    },
  ]);
}

async function completeBuilderToReview(user: ReturnType<typeof userEvent.setup>) {
  await user.type(
    screen.getByLabelText("Campaign name"),
    CAMPAIGN_CREATION_FIXTURES.name,
  );
  await user.type(
    screen.getByLabelText("Campaign objective"),
    CAMPAIGN_CREATION_FIXTURES.objective,
  );
  await user.click(screen.getByRole("button", { name: "Continue to audience" }));

  await screen.findByLabelText("Campaign audience segment");
  await user.selectOptions(
    screen.getByLabelText("Campaign audience segment"),
    segment.id,
  );
  await user.selectOptions(screen.getByLabelText("Campaign product"), product.id);
  await user.click(screen.getByRole("button", { name: "Continue to message" }));

  await user.type(
    screen.getByLabelText("Message subject"),
    CAMPAIGN_CREATION_FIXTURES.messageSubject,
  );
  await user.type(
    screen.getByLabelText("Campaign message body"),
    CAMPAIGN_CREATION_FIXTURES.messageBody,
  );
  await user.click(screen.getByRole("button", { name: "Continue to schedule" }));

  await user.type(
    screen.getByLabelText("Campaign schedule date"),
    CAMPAIGN_CREATION_FIXTURES.startDate,
  );
  await user.type(
    screen.getByLabelText("Campaign end date"),
    CAMPAIGN_CREATION_FIXTURES.endDate,
  );
  await user.click(screen.getByRole("button", { name: "Continue to review" }));
}

describe("campaign creation UI integration (item 603)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("opens Campaign Builder for campaign managers", async () => {
    vi.stubGlobal("fetch", campaignCreationHandlers());
    renderApp({ path: "/campaign-builder", roles: ["CAMPAIGN_MANAGER"] });

    expect(await screen.findByRole("heading", { name: "Builder", level: 1 })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: CAMPAIGN_BUILDER_PAGE_TITLE, level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Campaign builder steps")).toBeInTheDocument();
  });

  it("blocks unauthorized roles from building campaigns", async () => {
    vi.stubGlobal("fetch", campaignCreationHandlers());
    renderApp({ path: "/campaign-builder", roles: ["BI_ANALYST"] });

    expect(
      await screen.findByText(/not authorized to build campaigns/i),
    ).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: CAMPAIGN_CREATE_DRAFT_LABEL })).not.toBeInTheDocument();
  });

  it("creates a draft campaign through the multi-step builder UI", async () => {
    const user = userEvent.setup({ delay: null });
    const fetchMock = campaignCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/campaign-builder", roles: ["ADMIN"] });
    await screen.findByLabelText("Campaign name");

    await completeBuilderToReview(user);
    await user.click(screen.getByRole("button", { name: CAMPAIGN_CREATE_DRAFT_LABEL }));

    expect(await screen.findByText(CAMPAIGN_DRAFT_CREATED_NOTICE)).toBeInTheDocument();
    expect(screen.getAllByLabelText("Campaign status: Draft").length).toBeGreaterThanOrEqual(1);

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).includes("/campaigns") &&
          !String(url).includes("/products") &&
          (init as RequestInit | undefined)?.method === "POST"
        );
      });
      expect(createCall).toBeDefined();
      expect(JSON.parse(String((createCall?.[1] as RequestInit).body))).toMatchObject({
        name: CAMPAIGN_CREATION_FIXTURES.name,
        objective: CAMPAIGN_CREATION_FIXTURES.objective,
        segmentId: segment.id,
        channel: CAMPAIGN_CREATION_FIXTURES.channel,
      });
    });

    await waitFor(() => {
      const productCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).includes(`/campaigns/${CAMPAIGN_CREATION_FIXTURES.campaignId}/products`) &&
          (init as RequestInit | undefined)?.method === "PUT"
        );
      });
      expect(productCall).toBeDefined();
    });
  }, 25_000);

  it("validates the first step before allowing progress", async () => {
    const user = userEvent.setup({ delay: null });
    const fetchMock = campaignCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/campaign-builder", roles: ["CAMPAIGN_MANAGER"] });
    await screen.findByLabelText("Campaign name");
    await user.click(screen.getByRole("button", { name: "Continue to audience" }));

    expect(
      await screen.findByText(campaignFormValidationMessages.nameRequired),
    ).toBeInTheDocument();
    expect(
      screen.getByText(campaignFormValidationMessages.objectiveRequired),
    ).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url).includes("/campaigns") &&
          (init as RequestInit | undefined)?.method === "POST",
      ),
    ).toBe(false);
  });
});
