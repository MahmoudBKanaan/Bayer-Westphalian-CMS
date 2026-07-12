import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { AppLayout } from "@/components/AppLayout";

const authenticatedUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-04T12:00:00Z",
};

function renderLayoutForRoles(roles: SystemRoleName[], initialPath = "/dashboard") {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(authenticatedUser));
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <AuthProvider>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<h2>Dashboard content</h2>} />
              <Route path="/reports" element={<h2>Reports content</h2>} />
              <Route
                path="/campaigns/:campaignId/recipients/preview"
                element={<h2>Recipient preview content</h2>}
              />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function visibleMenuLabels() {
  const navigation = screen.getByLabelText("Main navigation");

  return within(navigation)
    .getAllByRole("link")
    .map((link) => link.textContent);
}

describe("AppLayout role-based navigation", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("implements the authenticated application shell layout", () => {
    stubHealthyApi();
    renderLayoutForRoles(["ADMIN"]);

    expect(screen.getByRole("link", { name: "Skip to content" })).toHaveAttribute(
      "href",
      "#main-content",
    );
    // Item 608: shell skip target remains the main landmark id.
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
    expect(
      screen.getByRole("main", {
        name: "Dashboard",
      }),
    ).toHaveAttribute("id", "main-content");
    expect(
      screen.getByRole("heading", {
        name: "Dashboard",
      }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Breadcrumb")).toHaveTextContent("Workspace/Dashboard");
    expect(screen.getByText("Dashboard content")).toBeInTheDocument();
    expect(screen.getByText("Admin User")).toBeInTheDocument();
  });

  it("uses route breadcrumbs and page headings for nested workflow pages", () => {
    stubHealthyApi();
    renderLayoutForRoles(
      ["ADMIN", "CAMPAIGN_MANAGER"],
      "/campaigns/50000000-0000-0000-0000-000000000001/recipients/preview",
    );

    expect(
      screen.getByRole("main", {
        name: "Recipient Preview",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Recipient Preview" })).toBeInTheDocument();
    expect(screen.getByLabelText("Breadcrumb")).toHaveTextContent(
      "Campaign Operations/Campaigns/Recipient Preview",
    );
    expect(screen.getByText("Recipient preview content")).toBeInTheDocument();
  });

  it("shows a shared loading state while application data is loading", async () => {
    vi.stubGlobal("fetch", vi.fn(() => new Promise(() => {})));

    renderLayoutForRoles(["ADMIN"]);

    expect(await screen.findByRole("status")).toHaveTextContent("Loading application data");
    expect(screen.getByRole("main", { name: "Dashboard" })).toHaveAttribute(
      "aria-busy",
      "true",
    );
    expect(screen.getByText("API health: checking")).toBeInTheDocument();
  });

  it("shows an empty navigation state when no menu items are available", () => {
    stubHealthyApi();
    renderLayoutForRoles([]);

    const navigation = screen.getByLabelText("Main navigation");

    expect(within(navigation).getByRole("status")).toHaveTextContent("No navigation available");
    expect(
      within(navigation).getByText(
        "Your account has no assigned application roles. Contact an administrator to update access.",
      ),
    ).toBeInTheDocument();
    expect(within(navigation).queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  it("opens the top bar user menu with identity, roles, and sign out", async () => {
    stubHealthyApi();
    renderLayoutForRoles(["ADMIN", "CAMPAIGN_MANAGER"]);

    const trigger = screen.getByRole("button", { name: /Admin User/i });
    expect(trigger).toHaveAttribute("aria-haspopup", "menu");
    expect(trigger).toHaveAttribute("aria-expanded", "false");

    await userEvent.click(trigger);

    expect(trigger).toHaveAttribute("aria-expanded", "true");
    const menu = screen.getByRole("menu", { name: "User menu" });
    expect(within(menu).getByText("Admin User")).toBeInTheDocument();
    expect(within(menu).getByText("admin@bayer-westphalian.test")).toBeInTheDocument();
    expect(within(menu).getByText("Admin")).toBeInTheDocument();
    expect(within(menu).getByText("Campaign Manager")).toBeInTheDocument();

    await userEvent.click(within(menu).getByRole("menuitem", { name: "Sign out" }));

    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("supports keyboard navigation for the top bar user menu", async () => {
    stubHealthyApi();
    renderLayoutForRoles(["ADMIN"]);
    const user = userEvent.setup();

    const trigger = screen.getByRole("button", { name: /Admin User/i });
    trigger.focus();

    await user.keyboard("{ArrowDown}");

    const menu = screen.getByRole("menu", { name: "User menu" });
    const signOutButton = within(menu).getByRole("menuitem", { name: "Sign out" });

    await waitFor(() => {
      expect(signOutButton).toHaveFocus();
    });
    expect(trigger).toHaveAttribute("aria-expanded", "true");

    await user.keyboard("{Escape}");

    expect(screen.queryByRole("menu", { name: "User menu" })).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
    expect(trigger).toHaveAttribute("aria-expanded", "false");
  });

  it("shows administrative navigation for admins", () => {
    stubHealthyApi();
    renderLayoutForRoles(["ADMIN"]);

    expect(
      screen.getByRole("heading", {
        name: "Workspace",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Campaign Operations",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Insights",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Administration",
      }),
    ).toBeInTheDocument();
    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Change requests",
      "Segments",
      "Campaigns",
      "Builder",
      "Compliance",
      "Contact history",
      "Follow-ups",
      "Reminders",
      "Analytics",
      "Executive",
      "Reports",
      "Users",
      "Settings",
      "Audit",
    ]);
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/dashboard");
    expect(screen.getByRole("link", { name: "Campaigns" })).toHaveAttribute("href", "/campaigns");
    expect(screen.getByRole("link", { name: "Audit" })).toHaveAttribute("href", "/audit");
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveClass("active");
  });

  it("shows campaign work without admin-only links for campaign managers", () => {
    stubHealthyApi();
    renderLayoutForRoles(["CAMPAIGN_MANAGER"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Segments",
      "Campaigns",
      "Builder",
      "Compliance",
      "Contact history",
      "Follow-ups",
      "Reminders",
      "Analytics",
      "Executive",
      "Reports",
    ]);
  });

  it("shows analytics and reports without operational links for BI analysts", () => {
    stubHealthyApi();
    renderLayoutForRoles(["BI_ANALYST"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Change requests",
      "Segments",
      "Contact history",
      "Analytics",
      "Executive",
      "Reports",
    ]);
  });

  it("shows product workflows with read-only customer access for product managers", () => {
    stubHealthyApi();
    renderLayoutForRoles(["PRODUCT_MANAGER"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Change requests",
      "Campaigns",
      "Contact history",
      "Analytics",
    ]);
  });

  it("shows compliance menus without user administration for compliance officers", () => {
    stubHealthyApi();
    renderLayoutForRoles(["COMPLIANCE_OFFICER"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Segments",
      "Campaigns",
      "Compliance",
      "Contact history",
      "Reminders",
      "Audit",
    ]);
  });

  it.each([
    {
      role: "CUSTOMER_SERVICE_AGENT" as SystemRoleName,
      labels: ["Dashboard", "Customers", "Contact history", "Follow-ups", "Reminders"],
    },
    {
      role: "SALES_AGENT" as SystemRoleName,
      labels: ["Dashboard", "Customers", "Contact history", "Follow-ups", "Reminders"],
    },
    {
      role: "MARKETING_ANALYST" as SystemRoleName,
      labels: ["Dashboard", "Analytics", "Executive", "Reports"],
    },
    {
      role: "EXECUTIVE_VIEWER" as SystemRoleName,
      labels: ["Dashboard", "Contact history", "Analytics", "Executive", "Reports"],
    },
    {
      role: "SYSTEM_AUDITOR" as SystemRoleName,
      labels: ["Dashboard", "Customers", "Contact history", "Audit"],
    },
  ])("shows KB role-based menu visibility for $role", ({ role, labels }) => {
    stubHealthyApi();
    renderLayoutForRoles([role]);

    expect(visibleMenuLabels()).toEqual(labels);
    expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Settings" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Builder" })).not.toBeInTheDocument();
  });

  it("combines menu visibility when a user has multiple roles", () => {
    stubHealthyApi();
    renderLayoutForRoles(["PRODUCT_MANAGER", "COMPLIANCE_OFFICER"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Change requests",
      "Segments",
      "Campaigns",
      "Compliance",
      "Contact history",
      "Reminders",
      "Analytics",
      "Audit",
    ]);
    expect(screen.queryByRole("link", { name: "Users" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Settings" })).not.toBeInTheDocument();
  });

  it("shows connected health when the backend health endpoint responds", async () => {
    const fetchMock = stubHealthyApi();

    renderLayoutForRoles(["ADMIN"]);

    expect(await screen.findByText("API health: connected")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/health`, {
      headers: {
        "Content-Type": "application/json",
      },
    });
  });

  it("shows the not connected message only when the health endpoint fails", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    renderLayoutForRoles(["ADMIN"]);

    await waitFor(() => {
      expect(screen.getByText("API health: backend not connected yet")).toBeInTheDocument();
    });
    expect(screen.getByRole("alert")).toHaveTextContent("Backend unavailable");
    expect(
      screen.getByText(
        "Application data could not be refreshed. Check the API service before continuing operational work.",
      ),
    ).toBeInTheDocument();
  });
});

function stubHealthyApi() {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    json: async () => ({
      status: "UP",
      service: "bayer-westphalian-campaign-platform",
    }),
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
