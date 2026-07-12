import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { SystemSettingsPage } from "@/pages/SystemSettingsPage";

const adminUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["ADMIN"],
};

const campaignManagerUser = {
  id: "10000000-0000-0000-0000-000000000101",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["CAMPAIGN_MANAGER"],
};

const sampleSettings = {
  id: "a1000000-0000-0000-0000-000000000001",
  monthlyContactLimit: 3,
  sendRetryLimit: 3,
  uninterestedExclusionDays: 90,
  updatedByUserId: null,
  updatedAt: "2026-07-01T00:00:00Z",
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

function renderSettingsPage(user: typeof adminUser) {
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
          <SystemSettingsPage />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createSettingsFetchMock(
  getBody: unknown = sampleSettings,
  putBody: unknown = {
    ...sampleSettings,
    monthlyContactLimit: 5,
    sendRetryLimit: 4,
    uninterestedExclusionDays: 120,
  },
) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (!url.startsWith(`${API_BASE_URL}/system-settings`)) {
      return jsonResponse({ success: false, message: "Not found", data: null }, 404);
    }
    if ((init?.method ?? "GET").toUpperCase() === "PUT") {
      return jsonResponse({
        success: true,
        message: "System settings updated",
        data: putBody,
      });
    }
    return jsonResponse({
      success: true,
      message: "System settings loaded",
      data: getBody,
    });
  });
}

describe("SystemSettingsPage (item 534)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("blocks non-admin roles from the System Settings screen", async () => {
    const fetchMock = createSettingsFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    renderSettingsPage(campaignManagerUser);

    expect(
      await screen.findByText("You are not authorized to manage system settings."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("form", { name: "System settings form" })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("loads and displays business limit fields for admins", async () => {
    vi.stubGlobal("fetch", createSettingsFetchMock());
    renderSettingsPage(adminUser);

    expect(await screen.findByRole("form", { name: "System settings form" })).toBeInTheDocument();
    expect(screen.getByText(/Configure platform business limits/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Monthly marketing contact limit/i)).toHaveValue(3);
    expect(screen.getByLabelText(/Send retry limit/i)).toHaveValue(3);
    expect(screen.getByLabelText(/Uninterested exclusion period/i)).toHaveValue(90);

    expect(fetch).toHaveBeenCalledWith(
      `${API_BASE_URL}/system-settings`,
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: expect.stringContaining("Bearer"),
        }),
      }),
    );
  });

  it("saves updated system settings", async () => {
    const user = userEvent.setup();
    const fetchMock = createSettingsFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    renderSettingsPage(adminUser);

    await screen.findByRole("form", { name: "System settings form" });

    const monthly = screen.getByLabelText(/Monthly marketing contact limit/i);
    await user.clear(monthly);
    await user.type(monthly, "5");
    const retry = screen.getByLabelText(/Send retry limit/i);
    await user.clear(retry);
    await user.type(retry, "4");
    const uninterested = screen.getByLabelText(/Uninterested exclusion period/i);
    await user.clear(uninterested);
    await user.type(uninterested, "120");

    await user.click(screen.getByRole("button", { name: "Save settings" }));

    await waitFor(() => {
      const putCall = fetchMock.mock.calls.find(
        (call) =>
          String(call[0]).includes("/system-settings") &&
          String(call[1]?.method ?? "").toUpperCase() === "PUT",
      );
      expect(putCall).toBeDefined();
      expect(putCall?.[1]?.body).toBe(
        JSON.stringify({
          monthlyContactLimit: 5,
          sendRetryLimit: 4,
          uninterestedExclusionDays: 120,
        }),
      );
    });

    expect(await screen.findByText("System settings saved.")).toBeInTheDocument();
  });

  it("shows client-side validation errors for out-of-range limits", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", createSettingsFetchMock());
    renderSettingsPage(adminUser);

    await screen.findByRole("form", { name: "System settings form" });

    const monthly = screen.getByLabelText(/Monthly marketing contact limit/i);
    await user.clear(monthly);
    await user.type(monthly, "0");
    await user.click(screen.getByRole("button", { name: "Save settings" }));

    expect(await screen.findByText("Must be between 1 and 100.")).toBeInTheDocument();
  });
});
