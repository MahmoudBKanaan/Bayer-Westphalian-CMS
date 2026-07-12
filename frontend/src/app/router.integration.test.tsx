import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { routes } from "@/app/router";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";

const authenticatedUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-04T12:00:00Z",
};

function seedAuthenticatedSession() {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(["CAMPAIGN_MANAGER"]));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(authenticatedUser));
}

function renderRoute(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  const router = createMemoryRouter(routes, {
    initialEntries: [path],
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

/**
 * Application routing integration cases (expanded under item 596 in
 * {@code src/test/integration/*}).
 */
describe("application routing", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("redirects unauthenticated protected routes to login", async () => {
    renderRoute("/campaigns");

    expect(
      await screen.findByRole("heading", {
        name: "Bayer-Westphalian Campaign Management",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Sign in with an authorized employee account to continue.",
    );
    expect(screen.queryByLabelText("Main navigation")).not.toBeInTheDocument();
  });

  it("redirects authenticated root route to the dashboard", async () => {
    seedAuthenticatedSession();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.includes("/analytics/dashboard")) {
          return {
            ok: true,
            status: 200,
            json: async () => ({
              success: true,
              message: "Analytics dashboard loaded",
              data: {
                campaignTotal: 12,
                activeCampaigns: 4,
                audienceSize: 2310,
                messagesSent: 1800,
                eligibleCount: 2000,
                excludedCount: 310,
                openedCount: 900,
                clickedCount: 400,
                repliedCount: 120,
                convertedCount: 80,
                openRate: 0.5,
                clickRate: 0.222,
                conversionRate: 0.044,
                estimatedCost: 1000,
                estimatedRevenue: 1855,
                estimatedRoi: 0.855,
                recentCampaignMetrics: [],
              },
            }),
          };
        }
        return { ok: false, status: 404, json: async () => ({ message: `Unexpected ${url}` }) };
      }),
    );
    renderRoute("/");

    expect(
      await screen.findByRole("heading", { name: /campaign performance/i }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Audience")).toBeInTheDocument();
    expect(screen.getByText("85.5%")).toBeInTheDocument();
  });

  it("renders campaign work from the API on the campaigns route", async () => {
    seedAuthenticatedSession();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          success: true,
          message: "Campaigns loaded",
          data: [
            {
              id: "50000000-0000-0000-0000-000000000001",
              name: "Life renewal outreach",
              objective: "Promote life insurance renewals",
              status: "DRAFT",
              ownerUserId: "10000000-0000-0000-0000-000000000101",
              ownerFullName: "Campaign Manager",
              segmentId: null,
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
            },
          ],
        }),
      }),
    );
    renderRoute("/campaigns");

    expect(await screen.findByRole("heading", { name: "Campaigns", level: 1 })).toBeInTheDocument();

    const campaignsTable = await screen.findByRole("table", { name: "Campaign worklist" });
    const campaignRows = within(campaignsTable).getAllByRole("row");
    expect(campaignRows).toHaveLength(2);
    expect(within(campaignRows[1]).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(campaignRows[1]).getByText("Campaign Manager")).toBeInTheDocument();
    expect(within(campaignRows[1]).getByText("Draft")).toBeInTheDocument();
  });

  it("registers the protected campaign recipient preview route", async () => {
    seedAuthenticatedSession();
    const campaignId = "50000000-0000-0000-0000-000000000285";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL) => {
        const url = String(input);
        if (url === `${API_BASE_URL}/campaigns/${campaignId}/recipients/preview`) {
          return Promise.resolve({
            ok: true,
            json: async () => ({
              success: true,
              message: "Campaign recipient preview loaded",
              data: {
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
              },
            }),
          });
        }

        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Campaign loaded",
            data: {
              id: campaignId,
              name: "Life renewal outreach",
              objective: "Promote life insurance renewals",
              status: "APPROVED",
              ownerUserId: "10000000-0000-0000-0000-000000000101",
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
            },
          }),
        });
      }),
    );

    renderRoute(`/campaigns/${campaignId}/recipients/preview`);

    expect(
      await screen.findByRole("heading", { name: "Recipient Preview", level: 1 }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Life renewal outreach" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("Ada Eligible")).toBeInTheDocument();
    expect(await screen.findAllByText("INVALID_CONSENT")).not.toHaveLength(0);
  });

  it("registers the protected customer details route", async () => {
    seedAuthenticatedSession();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((url: string) => {
        if (url.includes("/consents")) {
          return Promise.resolve({
            ok: true,
            json: async () => ({
              success: true,
              message: "Consents loaded",
              data: [],
            }),
          });
        }

        if (url.includes("/beneficiaries")) {
          return Promise.resolve({
            ok: true,
            json: async () => ({
              success: true,
              message: "Beneficiaries loaded",
              data: [],
            }),
          });
        }

        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Customer loaded",
            data: {
              id: "20000000-0000-0000-0000-000000000001",
              customerType: "CUSTOMER",
              firstName: "Ada",
              lastName: "Policyholder",
              fullName: "Ada Policyholder",
              email: "ada@bayer-westphalian.test",
              phone: "+49-555-0100",
              addressLine: "Insurance Street 1",
              city: "Berlin",
              country: "Germany",
              dateOfBirth: "1984-08-21",
              ageGroup: "AGE_41_60",
              status: "ACTIVE",
              doNotContact: false,
              active: true,
              contactable: true,
              source: "LIFE_INSURANCE_BENEFICIARY",
              createdAt: "2026-07-03T12:00:00Z",
              updatedAt: "2026-07-03T12:00:00Z",
              deletedAt: null,
            },
          }),
        });
      }),
    );

    renderRoute("/customers/20000000-0000-0000-0000-000000000001");

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(await screen.findByText("ada@bayer-westphalian.test")).toBeInTheDocument();
  });

  it("registers the protected follow-up tasks route", async () => {
    seedAuthenticatedSession();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL) => {
        const url = String(input);
        if (url === `${API_BASE_URL}/health`) {
          return Promise.resolve({
            ok: true,
            json: async () => ({ status: "UP" }),
          });
        }

        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Follow-up tasks loaded",
            data: [
              {
                id: "70000000-0000-0000-0000-000000000368",
                customerId: "20000000-0000-0000-0000-000000000368",
                customerFullName: "Ada Followup",
                campaignId: "50000000-0000-0000-0000-000000000368",
                campaignName: "Renewal campaign",
                assignedToUserId: "10000000-0000-0000-0000-000000000101",
                assignedToFullName: "Campaign Manager",
                title: "Call Ada",
                description: "Discuss renewal options",
                dueDate: "2026-09-15",
                status: "OPEN",
                priority: "HIGH",
                completedAt: null,
                createdAt: "2026-07-10T12:00:00Z",
                updatedAt: "2026-07-10T12:00:00Z",
              },
            ],
          }),
        });
      }),
    );

    renderRoute("/follow-up-tasks");

    expect(await screen.findByRole("heading", { name: "Follow-up tasks" })).toBeInTheDocument();
    expect(await screen.findByText("Call Ada")).toBeInTheDocument();
    expect(screen.getByText("Ada Followup")).toBeInTheDocument();
  });

  it("registers the protected reminders route", async () => {
    seedAuthenticatedSession();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL) => {
        const url = String(input);
        if (url === `${API_BASE_URL}/health`) {
          return Promise.resolve({
            ok: true,
            json: async () => ({ status: "UP" }),
          });
        }

        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Reminders loaded",
            data: [
              {
                id: "90000000-0000-0000-0000-000000000001",
                customerId: "20000000-0000-0000-0000-000000000001",
                customerFullName: "Ada Lovelace",
                productId: "30000000-0000-0000-0000-000000000001",
                productName: "Car Insurance",
                productType: "AUTO_INSURANCE",
                reminderType: "PAYMENT_DUE",
                reminderLevel: "GREEN",
                scheduledDate: "2026-08-01",
                status: "PENDING",
                createdAt: "2026-07-11T10:00:00Z",
                sentAt: null,
                due: true,
              },
            ],
          }),
        });
      }),
    );

    renderRoute("/reminders");

    expect(await screen.findByRole("heading", { name: "Reminders", level: 1 })).toBeInTheDocument();
    expect(await screen.findByText("Ada Lovelace")).toBeInTheDocument();
    expect(screen.getByText("Car Insurance")).toBeInTheDocument();
  });

  it("renders the login route outside the main application shell", async () => {
    renderRoute("/login");

    expect(
      await screen.findByRole("heading", {
        name: "Bayer-Westphalian Campaign Management",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Main navigation")).not.toBeInTheDocument();
  });
});

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
