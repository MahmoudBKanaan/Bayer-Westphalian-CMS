import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { RemindersPage } from "@/pages/RemindersPage";

const MANAGER_ACCESS_TOKEN = createAccessToken(["CAMPAIGN_MANAGER"]);

const pendingReminder = {
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
};

const sentReminder = {
  ...pendingReminder,
  id: "90000000-0000-0000-0000-000000000002",
  customerFullName: "Grace Hopper",
  productName: "Life Insurance",
  productType: "LIFE_INSURANCE",
  reminderType: "PRODUCT_EXPIRATION",
  reminderLevel: "YELLOW",
  scheduledDate: "2026-10-01",
  status: "SENT",
  sentAt: "2026-10-01T09:15:00Z",
  due: false,
};

function renderRemindersPage(roles: SystemRoleName[] = ["ADMIN"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000009901",
      email: "admin@bayer-westphalian.test",
      fullName: "Admin User",
      status: "ACTIVE",
      lastLoginAt: "2026-07-04T12:00:00Z",
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
      <MemoryRouter initialEntries={["/reminders"]}>
        <AuthProvider>
          <RemindersPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("RemindersPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads reminders and renders operational panels for admins", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderRemindersPage(["ADMIN"]);

    expect(await screen.findByRole("heading", { name: "Reminders" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Reminder summary" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Processing" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Create payment reminder" })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Create product-expiration reminder" }),
    ).toBeInTheDocument();
    expect(await screen.findByRole("table", { name: "Reminders table" })).toBeInTheDocument();
    expect(screen.getByText("Ada Lovelace")).toBeInTheDocument();
    expect(screen.getByText("Grace Hopper")).toBeInTheDocument();
    const remindersTable = screen.getByRole("table", { name: "Reminders table" });
    expect(within(remindersTable).getByLabelText("Reminder level: Green")).toHaveClass(
      "reminder-level-green",
    );
    expect(within(remindersTable).getByLabelText("Reminder level: Yellow")).toHaveClass(
      "reminder-level-yellow",
    );
    expect(screen.getByRole("button", { name: "Manual admin trigger" })).toBeInTheDocument();
  });

  it("applies reminder filters", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderRemindersPage(["CAMPAIGN_MANAGER"]);
    await screen.findByText("Ada Lovelace");

    await userEvent.type(
      screen.getByLabelText("Reminder customer ID filter"),
      pendingReminder.customerId,
    );
    await userEvent.selectOptions(screen.getByLabelText("Reminder status filter"), "PENDING");
    await userEvent.type(screen.getByLabelText("Reminder due on or before filter"), "2026-08-31");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/reminders?customerId=${pendingReminder.customerId}&status=PENDING&dueOnOrBefore=2026-08-31`,
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${MANAGER_ACCESS_TOKEN}`,
          }),
        }),
      );
    });
  });

  it("lets a campaign manager schedule payment reminders and process due reminders", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderRemindersPage(["CAMPAIGN_MANAGER"]);
    await screen.findByText("Ada Lovelace");

    await userEvent.type(
      screen.getByLabelText("Schedule payment reminder customer ID"),
      pendingReminder.customerId,
    );
    await userEvent.type(
      screen.getByLabelText("Schedule payment reminder product ID"),
      pendingReminder.productId,
    );
    await userEvent.selectOptions(screen.getByLabelText("Schedule payment reminder level"), "RED");
    await userEvent.type(
      screen.getByLabelText("Schedule payment reminder scheduled date"),
      "2026-08-01",
    );
    await userEvent.click(screen.getByRole("button", { name: "Schedule payment reminder" }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url]) => url === `${API_BASE_URL}/reminders/payment`,
      );
      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        customerId: pendingReminder.customerId,
        productId: pendingReminder.productId,
        reminderLevel: "RED",
        scheduledDate: "2026-08-01",
      });
    });

    await userEvent.type(screen.getByLabelText("Process due reminders as of date"), "2026-08-01");
    await userEvent.click(screen.getByRole("button", { name: "Process due reminders" }));

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          ([url, init]) =>
            url === `${API_BASE_URL}/reminders/due/send?asOfDate=2026-08-01` &&
            init?.method === "POST",
        ),
      ).toBe(true);
    });
  });

  it("lets an admin use the manual test trigger and row actions", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderRemindersPage(["ADMIN"]);
    await screen.findByText("Ada Lovelace");

    await userEvent.click(screen.getByRole("button", { name: "Manual admin trigger" }));
    await userEvent.click(screen.getAllByRole("button", { name: "Mark sent" })[0]);
    await userEvent.click(screen.getAllByRole("button", { name: "Cancel" })[0]);

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          ([url, init]) =>
            url === `${API_BASE_URL}/reminders/due/manual-trigger` && init?.method === "POST",
        ),
      ).toBe(true);
      expect(
        fetchMock.mock.calls.some(
          ([url, init]) =>
            url === `${API_BASE_URL}/reminders/${pendingReminder.id}/sent` &&
            init?.method === "PUT",
        ),
      ).toBe(true);
      expect(
        fetchMock.mock.calls.some(
          ([url, init]) =>
            url === `${API_BASE_URL}/reminders/${pendingReminder.id}/cancel` &&
            init?.method === "PUT",
        ),
      ).toBe(true);
    });
  });

  it("renders a read-only reminders worklist for customer service agents", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderRemindersPage(["CUSTOMER_SERVICE_AGENT"]);

    const remindersTable = await screen.findByRole("table", { name: "Reminders table" });
    expect(within(remindersTable).getByText("Ada Lovelace")).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Create payment reminder" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Process due reminders" })).not.toBeInTheDocument();
  });

  it("hides the manual admin trigger from campaign managers", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderRemindersPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByText("Ada Lovelace")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Process due reminders" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Manual admin trigger" })).not.toBeInTheDocument();
  });
});

function createFetchMock(reminders = [pendingReminder, sentReminder]) {
  return vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (
      url.startsWith(`${API_BASE_URL}/reminders`) &&
      (init?.method == null || init.method === "GET")
    ) {
      return Promise.resolve(apiResponse(reminders));
    }
    if (url.includes("/sent") || url.includes("/cancel") || url.includes("/payment")) {
      return Promise.resolve(apiResponse(pendingReminder));
    }
    return Promise.resolve(apiResponse([pendingReminder]));
  });
}

function apiResponse(data: unknown) {
  return {
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
      data,
    }),
  };
}

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
