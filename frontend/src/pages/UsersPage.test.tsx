import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { UsersPage } from "@/pages/UsersPage";

const adminUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-04T12:00:00Z",
  roles: ["ADMIN"],
};

const campaignManager = {
  id: "10000000-0000-0000-0000-000000009902",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: null,
  roles: ["CAMPAIGN_MANAGER"],
};

function renderUsersPage() {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(["ADMIN"]));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(adminUser));

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <UsersPage />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

describe("UsersPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads and displays admin user management data", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderUsersPage();

    expect(await screen.findByRole("heading", { name: "User management" })).toBeInTheDocument();
    expect(
      screen.getByText("Admin account control, role assignment, and access status"),
    ).toBeInTheDocument();
    expect(await screen.findAllByText("Admin User")).not.toHaveLength(0);
    expect(screen.getAllByText("Campaign Manager")).not.toHaveLength(0);
    expect(screen.getByText("admin@bayer-westphalian.test")).toBeInTheDocument();
  });

  it("creates a user through the admin endpoint", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderUsersPage();
    await screen.findAllByText("Admin User");

    const createPanel = screen.getByRole("heading", { name: "Create user" }).closest("section");
    expect(createPanel).not.toBeNull();

    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Full name"),
      "New Analyst",
    );
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Email"),
      "new.analyst@bayer-westphalian.test",
    );
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Temporary password"),
      "StrongPass!2026",
    );
    await userEvent.click(
      within(createPanel as HTMLElement).getByRole("button", {
        name: "Create user",
      }),
    );

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/users` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        fullName: "New Analyst",
        email: "new.analyst@bayer-westphalian.test",
        password: "StrongPass!2026",
      });
      expect(createCall?.[1]).toMatchObject({
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + createAccessToken(["ADMIN"]),
        },
        method: "POST",
      });
    });
  });

  it("edits the selected user's name and status", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderUsersPage();
    await screen.findAllByText("Campaign Manager");

    await userEvent.selectOptions(screen.getByLabelText("User"), campaignManager.id);

    const selectedPanel = screen.getByRole("heading", { name: "Selected user" }).closest("section");
    expect(selectedPanel).not.toBeNull();

    const fullNameInput = within(selectedPanel as HTMLElement).getByLabelText("Full name");
    await userEvent.clear(fullNameInput);
    await userEvent.type(fullNameInput, "Campaign Manager Updated");
    await userEvent.selectOptions(
      within(selectedPanel as HTMLElement).getByLabelText("Status"),
      "LOCKED",
    );
    await userEvent.click(
      within(selectedPanel as HTMLElement).getByRole("button", { name: "Save changes" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${campaignManager.id}`, {
        body: JSON.stringify({
          fullName: "Campaign Manager Updated",
          status: "LOCKED",
        }),
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + createAccessToken(["ADMIN"]),
        },
        method: "PUT",
      });
    });
  });

  it("assigns roles and disables selected users", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderUsersPage();
    await screen.findAllByText("Campaign Manager");

    await userEvent.selectOptions(screen.getByLabelText("User"), campaignManager.id);
    const selectedPanel = screen.getByRole("heading", { name: "Selected user" }).closest("section");
    expect(selectedPanel).not.toBeNull();
    expect(within(selectedPanel as HTMLElement).getByLabelText("Current roles")).toHaveTextContent(
      "Campaign Manager",
    );
    const roleSelect = within(selectedPanel as HTMLElement).getByLabelText("Assign role");
    expect(within(roleSelect).getByRole("option", { name: "Campaign Manager" })).toBeDisabled();

    await userEvent.selectOptions(roleSelect, "BI_ANALYST");
    await userEvent.click(screen.getByRole("button", { name: "Assign role" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${campaignManager.id}/roles`, {
        body: JSON.stringify({
          roleName: "BI_ANALYST",
          assignedByUserId: adminUser.id,
        }),
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + createAccessToken(["ADMIN"]),
        },
        method: "POST",
      });
    });

    await userEvent.click(screen.getByRole("button", { name: "Disable user" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/users/${campaignManager.id}/disable`,
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: "Bearer " + createAccessToken(["ADMIN"]),
          },
          method: "PATCH",
        },
      );
    });
  });

  it("filters users by status", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderUsersPage();
    await screen.findAllByText("Admin User");

    const toolbar = screen.getByRole("heading", { name: "User management" }).closest(".panel");
    expect(toolbar).not.toBeNull();
    await userEvent.selectOptions(
      within(toolbar as HTMLElement).getByLabelText("Status"),
      "ACTIVE",
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users?status=ACTIVE`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + createAccessToken(["ADMIN"]),
        },
      });
    });
  });

  it("shows an authorization error when admin APIs reject access", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({
        message: "Forbidden",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderUsersPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "You are not authorized to manage users or roles.",
    );
  });
});

function createFetchMock() {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url.endsWith("/users") && init?.method === "POST") {
      return jsonResponse({
        ...JSON.parse(init.body as string),
        id: "10000000-0000-0000-0000-000000009903",
        status: "ACTIVE",
        lastLoginAt: null,
        roles: [],
      });
    }

    if (url.includes("/roles")) {
      return jsonResponse({ ...campaignManager, roles: ["BI_ANALYST", "CAMPAIGN_MANAGER"] });
    }

    if (url.endsWith("/disable")) {
      return jsonResponse({ ...campaignManager, status: "DISABLED" });
    }

    return jsonResponse([adminUser, campaignManager]);
  });
}

function jsonResponse(data: unknown) {
  return Promise.resolve({
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
      data,
    }),
  });
}

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
