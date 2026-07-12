import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { ContactHistoryPage } from "@/pages/ContactHistoryPage";

const authorizedUser = {
  id: "10000000-0000-0000-0000-000000000101",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["CAMPAIGN_MANAGER"],
};

const unauthorizedUser = {
  id: "10000000-0000-0000-0000-000000000105",
  email: "unauthorized.user@bayer-westphalian.test",
  fullName: "Unauthorized User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["SOME_OTHER_ROLE"],
};

const mockEvent1 = {
  id: "60000000-0000-0000-0000-000000000001",
  customerId: "20000000-0000-0000-0000-000000000001",
  customerFullName: "John Doe",
  campaignId: "50000000-0000-0000-0000-000000000001",
  campaignName: "Life renewal outreach",
  channel: "EMAIL",
  eventType: "SENT",
  outcome: null,
  notes: "Automated email sent",
  occurredAt: "2026-07-10T10:00:00Z",
  createdByUserId: "10000000-0000-0000-0000-000000000101",
  createdByFullName: "Campaign Manager",
};

const mockEvent2 = {
  id: "60000000-0000-0000-0000-000000000002",
  customerId: "20000000-0000-0000-0000-000000000002",
  customerFullName: "Jane Smith",
  campaignId: null,
  campaignName: null,
  channel: "PHONE",
  eventType: "CALLED",
  outcome: "INTERESTED",
  notes: "Customer is interested in the new policy",
  occurredAt: "2026-07-10T11:30:00Z",
  createdByUserId: "10000000-0000-0000-0000-000000000102",
  createdByFullName: "Sales Agent",
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

function renderContactHistoryPage(user: typeof authorizedUser) {
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
          <ContactHistoryPage />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createContactEventsFetchMock(events: unknown[] = [mockEvent1, mockEvent2]) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.startsWith(`${API_BASE_URL}/contact-events/timeline`)) {
      return jsonResponse({
        success: true,
        message: "Contact timeline loaded",
        data: events,
      });
    }
    return jsonResponse({ success: false, message: "Not found", data: null }, 404);
  });
}

describe("ContactHistoryPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads contact timeline and displays events", async () => {
    vi.stubGlobal("fetch", createContactEventsFetchMock([mockEvent1, mockEvent2]));

    renderContactHistoryPage(authorizedUser);

    expect(await screen.findByRole("heading", { name: "Contact history" })).toBeInTheDocument();
    expect(
      screen.getByText("Customer, campaign, provider, and outcome events"),
    ).toBeInTheDocument();
    expect(await screen.findByRole("table", { name: "Contact history table" })).toBeInTheDocument();
    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText("Life renewal outreach")).toBeInTheDocument();
    expect(screen.getByText("Automated email sent")).toBeInTheDocument();
    expect(screen.getByText("Jane Smith")).toBeInTheDocument();
    expect(screen.getByText("Customer is interested in the new policy")).toBeInTheDocument();
  });

  it("applies timeline search filters", async () => {
    const fetchMock = createContactEventsFetchMock([mockEvent1]);
    vi.stubGlobal("fetch", fetchMock);

    renderContactHistoryPage(authorizedUser);
    await screen.findByText("John Doe");

    await userEvent.type(screen.getByLabelText("Customer ID"), mockEvent1.customerId);
    await userEvent.selectOptions(screen.getByLabelText("Event type"), "SENT");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      const url = new URL(fetchMock.mock.calls[fetchMock.mock.calls.length - 1][0] as string);
      expect(url.pathname).toBe("/api/contact-events/timeline");
      expect(url.searchParams.get("customerId")).toBe(mockEvent1.customerId);
      expect(url.searchParams.get("eventType")).toBe("SENT");
    });
  });

  it("shows empty state when no events exist", async () => {
    vi.stubGlobal("fetch", createContactEventsFetchMock([]));

    renderContactHistoryPage(authorizedUser);

    expect(
      await screen.findByText("No contact history entries match the current filters."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Contact history table" })).not.toBeInTheDocument();
  });

  it("blocks unauthorized users", async () => {
    const fetchMock = vi.fn(async () =>
      jsonResponse({ success: false, message: "Forbidden", data: null }, 403),
    );
    vi.stubGlobal("fetch", fetchMock);

    renderContactHistoryPage(unauthorizedUser);

    expect(
      await screen.findByText("You are not authorized to view contact history."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Contact history table" })).not.toBeInTheDocument();
  });
});
