import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
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

function renderLayoutForRoles(roles: SystemRoleName[]) {
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
      <MemoryRouter initialEntries={["/dashboard"]}>
        <AuthProvider>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<h2>Dashboard content</h2>} />
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

  it("shows administrative navigation for admins", () => {
    stubHealthyApi();
    renderLayoutForRoles(["ADMIN"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Segments",
      "Campaigns",
      "Compliance",
      "Analytics",
      "Reports",
      "Users",
      "Audit",
    ]);
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
      "Compliance",
      "Analytics",
    ]);
  });

  it("shows analytics and reports without operational links for BI analysts", () => {
    stubHealthyApi();
    renderLayoutForRoles(["BI_ANALYST"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Products",
      "Segments",
      "Analytics",
      "Reports",
    ]);
  });

  it("shows compliance menus without user administration for compliance officers", () => {
    stubHealthyApi();
    renderLayoutForRoles(["COMPLIANCE_OFFICER"]);

    expect(visibleMenuLabels()).toEqual([
      "Dashboard",
      "Customers",
      "Campaigns",
      "Compliance",
      "Reports",
      "Audit",
    ]);
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
