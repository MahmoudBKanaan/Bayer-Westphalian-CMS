import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { routes } from "@/app/router";
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
    renderRoute("/");

    expect(
      await screen.findByRole("heading", { name: /campaign performance/i }),
    ).toBeInTheDocument();
    expect(screen.getByText("2,310")).toBeInTheDocument();
    expect(screen.getByText("85.5%")).toBeInTheDocument();
  });

  it("renders campaign work from the dashboard data on the campaigns route", async () => {
    seedAuthenticatedSession();
    renderRoute("/campaigns");

    expect(await screen.findByRole("heading", { name: "Campaigns" })).toBeInTheDocument();

    const campaignRows = screen.getAllByRole("row");
    expect(campaignRows).toHaveLength(4);
    expect(within(campaignRows[1]).getByText("CMP-001")).toBeInTheDocument();
    expect(within(campaignRows[1]).getByText("Grandchild Education Plan")).toBeInTheDocument();
    expect(within(campaignRows[1]).getByText("982")).toBeInTheDocument();
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
    expect(screen.getAllByText("Ada Policyholder")).not.toHaveLength(0);
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
