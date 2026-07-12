import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
  seedAuthenticatedSession,
} from "@/test/integration/renderApp";

describe("auth and routing integration (item 596)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("redirects unauthenticated users from protected routes to login", async () => {
    renderApp({ path: "/campaigns", authenticated: false });

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

  it("redirects authenticated root to the dashboard shell", async () => {
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/analytics/dashboard"),
          response: () =>
            jsonOk(
              {
                ...emptyDashboardPayload,
                campaignTotal: 3,
                activeCampaigns: 1,
                audienceSize: 100,
                messagesSent: 80,
                openRate: 0.5,
                estimatedRoi: 0.25,
              },
              "Analytics dashboard loaded",
            ),
        },
      ]),
    );

    renderApp({ path: "/", roles: ["BI_ANALYST"] });

    expect(await screen.findByRole("heading", { name: "Dashboard", level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
  });

  it("logs in through the full router and lands on the dashboard", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url, method) => url.endsWith("/auth/login") && method === "POST",
          response: () =>
            jsonOk(
              {
                user: {
                  id: "10000000-0000-0000-0000-000000009901",
                  email: "campaign.manager@bayer-westphalian.test",
                  fullName: "Campaign Manager",
                  status: "ACTIVE",
                  lastLoginAt: "2026-07-12T12:00:00Z",
                  roles: ["CAMPAIGN_MANAGER"],
                },
                tokens: {
                  accessToken: createAccessTokenForLogin(["CAMPAIGN_MANAGER"]),
                  accessTokenExpiresAt: "2026-07-12T12:15:00Z",
                  refreshToken: "refresh-token",
                  refreshTokenExpiresAt: "2026-07-19T12:00:00Z",
                },
              },
              "Login successful",
            ),
        },
        {
          match: (url) => url.includes("/analytics/dashboard"),
          response: () => jsonOk(emptyDashboardPayload, "Analytics dashboard loaded"),
        },
      ]),
    );

    renderApp({ path: "/login", authenticated: false });

    await user.type(screen.getByLabelText("Email"), "campaign.manager@bayer-westphalian.test");
    await user.type(screen.getByLabelText("Password"), "Neoarel@7368");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Dashboard", level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
  });

  it("shows not-found for unknown paths outside the protected shell", async () => {
    renderApp({ path: "/this-route-does-not-exist", authenticated: false });

    expect(await screen.findByRole("heading", { name: "Page not found" })).toBeInTheDocument();
  });

  it("keeps an authenticated session when navigating between protected routes", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url) => url.includes("/analytics/dashboard"),
          response: () => jsonOk(emptyDashboardPayload),
        },
        {
          match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
          response: () => jsonOk([]),
        },
      ]),
    );

    seedAuthenticatedSession(["CAMPAIGN_MANAGER"]);
    renderApp({ path: "/dashboard", roles: ["CAMPAIGN_MANAGER"] });

    expect(await screen.findByRole("heading", { name: "Dashboard", level: 1 })).toBeInTheDocument();
    await user.click(screen.getByRole("link", { name: "Campaigns" }));
    expect(await screen.findByRole("heading", { name: "Campaigns", level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
  });
});

function createAccessTokenForLogin(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
  return `header.${payload}.signature`;
}
