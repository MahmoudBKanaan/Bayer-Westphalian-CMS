import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { AuditPage } from "@/pages/AuditPage";

const auditorUser = {
  id: "10000000-0000-0000-0000-000000000201",
  email: "system.auditor@bayer-westphalian.test",
  fullName: "System Auditor",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["SYSTEM_AUDITOR"],
};

const unauthorizedUser = {
  id: "10000000-0000-0000-0000-000000000105",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["CAMPAIGN_MANAGER"],
};

const productAudit = {
  id: "53000000-0000-0000-0000-000000000001",
  actorUserId: "10000000-0000-0000-0000-000000000001",
  action: "CREATE",
  entityType: "products",
  entityId: "41000000-0000-0000-0000-000000000201",
  oldValue: null,
  newValue: { name: "Life Protection" },
  ipAddress: "10.0.0.8",
  createdAt: "2026-07-07T15:02:46Z",
};

const consentAudit = {
  id: "53000000-0000-0000-0000-000000000002",
  actorUserId: null,
  action: "WITHDRAW_CONSENT",
  entityType: "consent_records",
  entityId: "53000000-0000-0000-0000-000000000101",
  oldValue: { status: "GIVEN" },
  newValue: { status: "WITHDRAWN" },
  ipAddress: null,
  createdAt: "2026-07-07T15:01:46Z",
};

const approvalAudit = {
  id: "53000000-0000-0000-0000-000000000003",
  actorUserId: "10000000-0000-0000-0000-000000000003",
  action: "APPROVE",
  entityType: "campaigns",
  entityId: "50000000-0000-0000-0000-000000000001",
  oldValue: { status: "SUBMITTED" },
  newValue: { status: "APPROVED" },
  ipAddress: "10.0.0.9",
  createdAt: "2026-07-07T14:00:00Z",
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

function renderAuditPage(user: typeof auditorUser) {
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
          <AuditPage />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createAuditFetchMock(logs: unknown[] = [productAudit, consentAudit, approvalAudit]) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.startsWith(`${API_BASE_URL}/audit-logs/entity-history`)) {
      return jsonResponse({
        success: true,
        message: "Entity audit history loaded",
        data: [productAudit],
      });
    }
    if (url.startsWith(`${API_BASE_URL}/audit-logs`)) {
      const queryIndex = url.indexOf("?");
      let result = logs;
      if (queryIndex >= 0) {
        const params = new URLSearchParams(url.slice(queryIndex + 1));
        const action = params.get("action");
        const entityType = params.get("entityType");
        const actorUserId = params.get("actorUserId");
        result = (logs as Array<typeof productAudit>).filter((entry) => {
          if (action != null && entry.action !== action) {
            return false;
          }
          if (entityType != null && entry.entityType !== entityType) {
            return false;
          }
          if (actorUserId != null && entry.actorUserId !== actorUserId) {
            return false;
          }
          return true;
        });
      }
      return jsonResponse({
        success: true,
        message: "Audit logs loaded",
        data: result,
      });
    }
    return jsonResponse({ success: false, message: "Not found", data: null }, 404);
  });
}

describe("AuditPage (items 532–533)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("blocks unauthorized roles from viewing the Audit Log screen", async () => {
    const fetchMock = createAuditFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    renderAuditPage(unauthorizedUser);

    expect(
      await screen.findByText("You are not authorized to view audit logs."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Audit log table" })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("renders the Audit Log screen with sensitive action rows and detail panel", async () => {
    vi.stubGlobal("fetch", createAuditFetchMock());
    renderAuditPage(auditorUser);

    const auditTable = await screen.findByRole("table", { name: "Audit log table" });
    expect(auditTable).toBeInTheDocument();
    expect(screen.getByText(/Immutable history of sensitive actions/i)).toBeInTheDocument();
    expect(within(auditTable).getByText("Products")).toBeInTheDocument();
    expect(within(auditTable).getByText("Consent Records")).toBeInTheDocument();
    expect(within(auditTable).getByText("Campaigns")).toBeInTheDocument();
    expect(within(auditTable).getByText("name: Life Protection")).toBeInTheDocument();
    expect(within(auditTable).getByText("status: WITHDRAWN")).toBeInTheDocument();
    expect(within(auditTable).getByText("10.0.0.8")).toBeInTheDocument();
    expect(
      screen
        .getAllByLabelText("Audit action: Create")
        .some((badge) => badge.classList.contains("audit-action-create")),
    ).toBe(true);
    expect(screen.getByLabelText("Audit action: Withdraw Consent")).toHaveClass(
      "audit-action-consent",
    );
    expect(screen.getByLabelText("Audit action: Approve")).toHaveClass("audit-action-approval");

    const detail = screen.getByLabelText("Selected audit log fields");
    expect(within(detail).getByText(productAudit.id)).toBeInTheDocument();
    expect(within(detail).getByLabelText("Audit action: Create")).toHaveClass(
      "audit-action-create",
    );
    // Value headings sit outside the definition-list summary region.
    expect(screen.getByRole("heading", { name: "Previous value" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "New value" })).toBeInTheDocument();
    expect(screen.getByText("No previous value recorded.")).toBeInTheDocument();
    expect(screen.getByText(/"name": "Life Protection"/)).toBeInTheDocument();

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(`${API_BASE_URL}/audit-logs`),
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: expect.stringContaining("Bearer"),
        }),
      }),
    );
  });

  it("selects another row and loads entity history for that entity", async () => {
    const user = userEvent.setup();
    const fetchMock = createAuditFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    renderAuditPage(auditorUser);

    await screen.findByRole("table", { name: "Audit log table" });

    await user.click(screen.getByTestId(`audit-row-${consentAudit.id}`));

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some((call) =>
          String(call[0]).includes(
            `/audit-logs/entity-history?entityType=consent_records&entityId=${consentAudit.entityId}`,
          ),
        ),
      ).toBe(true);
    });

    expect(await screen.findByRole("table", { name: "Entity audit history table" })).toBeInTheDocument();
    expect(screen.getByText("Entity history")).toBeInTheDocument();
  });

  it("shows empty state when no audit events exist", async () => {
    vi.stubGlobal("fetch", createAuditFetchMock([]));
    renderAuditPage(auditorUser);

    expect(
      await screen.findByText("No audit log entries have been recorded yet."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Audit log table" })).not.toBeInTheDocument();
  });

  it("shows API authorization failure message when the list endpoint forbids access", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: async () => ({
          success: false,
          message: "Forbidden",
          data: null,
        }),
      }),
    );
    renderAuditPage(auditorUser);

    expect(
      await screen.findByText("You are not authorized to view audit logs."),
    ).toBeInTheDocument();
  });

  it(
    "applies actor, action, entity, and date filters to the list request (item 533)",
    async () => {
      const user = userEvent.setup({ delay: null });
      const fetchMock = createAuditFetchMock();
      vi.stubGlobal("fetch", fetchMock);
      renderAuditPage(auditorUser);

      await screen.findByRole("table", { name: "Audit log table" });
      expect(screen.getByRole("form", { name: "Audit log filters" })).toBeInTheDocument();

      await user.type(
        screen.getByPlaceholderText("Filter by actor UUID"),
        approvalAudit.actorUserId!,
      );
      await user.selectOptions(screen.getByLabelText("Action filter"), "APPROVE");
      await user.selectOptions(screen.getByLabelText("Entity type filter"), "campaigns");
      await user.type(
        screen.getByPlaceholderText("Filter by entity UUID"),
        approvalAudit.entityId!,
      );
      await user.type(screen.getByLabelText("Created from filter"), "2026-07-01T00:00");
      await user.type(screen.getByLabelText("Created to filter"), "2026-07-31T23:59");
      await user.click(screen.getByRole("button", { name: "Apply filters" }));

      await waitFor(() => {
        const filteredCall = fetchMock.mock.calls
          .map((call) => String(call[0]))
          .find(
            (url) =>
              url.includes("/audit-logs?") &&
              url.includes(`actorUserId=${approvalAudit.actorUserId}`) &&
              url.includes("action=APPROVE") &&
              url.includes("entityType=campaigns") &&
              url.includes(`entityId=${approvalAudit.entityId}`) &&
              url.includes("createdFrom=") &&
              url.includes("createdTo="),
          );
        expect(filteredCall).toBeDefined();
      });

      expect(await screen.findByRole("table", { name: "Audit log table" })).toBeInTheDocument();
      expect(screen.getByTestId(`audit-row-${approvalAudit.id}`)).toBeInTheDocument();
      expect(screen.queryByTestId(`audit-row-${productAudit.id}`)).not.toBeInTheDocument();
      expect(
        within(screen.getByRole("table", { name: "Audit log table" })).queryByText(
          "name: Life Protection",
        ),
      ).not.toBeInTheDocument();
    },
    15_000,
  );

  it("shows filtered empty state and resets filters (item 533)", async () => {
    const user = userEvent.setup();
    const fetchMock = createAuditFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    renderAuditPage(auditorUser);

    await screen.findByRole("table", { name: "Audit log table" });

    await user.selectOptions(screen.getByLabelText("Action filter"), "EXPORT_REPORT");
    await user.click(screen.getByRole("button", { name: "Apply filters" }));

    expect(
      await screen.findByText("No audit log entries match the current filters."),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Reset" }));

    await waitFor(() => {
      const unfiltered = fetchMock.mock.calls
        .map((call) => String(call[0]))
        .filter((url) => url.includes("/audit-logs") && !url.includes("entity-history"));
      expect(unfiltered.some((url) => !url.includes("?"))).toBe(true);
    });

    expect(await screen.findByRole("table", { name: "Audit log table" })).toBeInTheDocument();
    expect(screen.getByLabelText("Action filter")).toHaveValue("");
  });
});
