import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { FollowUpTasksPage } from "@/pages/FollowUpTasksPage";

const authorizedUser = {
  id: "10000000-0000-0000-0000-000000000368",
  email: "sales.agent@bayer-westphalian.test",
  fullName: "Sales Agent",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["SALES_AGENT"],
};

const createAuthorizedUser = {
  id: "10000000-0000-0000-0000-000000000370",
  email: "campaign.manager@bayer-westphalian.test",
  fullName: "Campaign Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["CAMPAIGN_MANAGER"],
};

const unauthorizedUser = {
  id: "10000000-0000-0000-0000-000000000369",
  email: "product.manager@bayer-westphalian.test",
  fullName: "Product Manager",
  status: "ACTIVE",
  lastLoginAt: "2026-07-09T10:00:00Z",
  roles: ["PRODUCT_MANAGER"],
};

const mockTask = {
  id: "70000000-0000-0000-0000-000000000368",
  customerId: "20000000-0000-0000-0000-000000000368",
  customerFullName: "Ada Followup",
  campaignId: "50000000-0000-0000-0000-000000000368",
  campaignName: "Renewal campaign",
  assignedToUserId: authorizedUser.id,
  assignedToFullName: "Sales Agent",
  title: "Call Ada",
  description: "Discuss renewal options",
  dueDate: "2026-09-15",
  status: "OPEN",
  priority: "HIGH",
  completedAt: null,
  createdAt: "2026-07-10T12:00:00Z",
  updatedAt: "2026-07-10T12:00:00Z",
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

function renderFollowUpTasksPage(user: typeof authorizedUser) {
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
          <FollowUpTasksPage />
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createFollowUpTasksFetchMock(tasks: unknown[] = [mockTask]) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.startsWith(`${API_BASE_URL}/follow-up-tasks`)) {
      if (init?.method === "POST") {
        return jsonResponse(
          {
            success: true,
            message: "Follow-up task created",
            data: mockTask,
          },
          201,
        );
      }
      if (init?.method === "PUT" && url.endsWith(`/follow-up-tasks/${mockTask.id}/assign`)) {
        const body = JSON.parse(String(init.body)) as { assignedTo: string };
        return jsonResponse({
          success: true,
          message: "Follow-up task assigned",
          data: {
            ...mockTask,
            assignedToUserId: body.assignedTo,
            assignedToFullName: "Assigned User",
          },
        });
      }
      return jsonResponse({
        success: true,
        message: "Follow-up tasks loaded",
        data: tasks,
      });
    }
    return jsonResponse({ success: false, message: "Not found", data: null }, 404);
  });
}

describe("FollowUpTasksPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads follow-up tasks and displays worklist context", async () => {
    vi.stubGlobal("fetch", createFollowUpTasksFetchMock([mockTask]));

    renderFollowUpTasksPage(authorizedUser);

    expect(await screen.findByRole("heading", { name: "Follow-up tasks" })).toBeInTheDocument();
    expect(
      screen.getByText("Assignee, priority, status, due date, customer, and campaign context"),
    ).toBeInTheDocument();
    expect(await screen.findByRole("table", { name: "Follow-up tasks table" })).toBeInTheDocument();
    expect(screen.getByText("Call Ada")).toBeInTheDocument();
    expect(screen.getByText("Ada Followup")).toBeInTheDocument();
    expect(screen.getByText("Renewal campaign")).toBeInTheDocument();
    expect(screen.getByText("Sales Agent")).toBeInTheDocument();
    // Priority/status labels can appear in filters and the worklist table.
    expect(screen.getAllByText("High").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Open").length).toBeGreaterThanOrEqual(1);
  });

  it("lets campaign managers create follow-up tasks and refreshes the worklist", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(createAuthorizedUser);
    await screen.findByText("Call Ada");

    await userEvent.type(screen.getByLabelText("Follow-up customer ID"), mockTask.customerId);
    await userEvent.type(screen.getByLabelText("Follow-up task title"), "Call Ada");
    await userEvent.type(screen.getByLabelText("Follow-up assigned user ID"), authorizedUser.id);
    await userEvent.type(screen.getByLabelText("Follow-up campaign ID"), mockTask.campaignId);
    await userEvent.selectOptions(screen.getByLabelText("Create follow-up priority"), "HIGH");
    fireEvent.change(screen.getByLabelText("Create follow-up due date"), {
      target: { value: "2026-09-15" },
    });
    await userEvent.type(screen.getByLabelText("Follow-up description"), "Discuss renewal options");
    await userEvent.click(screen.getByRole("button", { name: "Create task" }));

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          (call) => (call[1] as RequestInit | undefined)?.method === "POST",
        ),
      ).toBe(true);
    });
    const postCall = fetchMock.mock.calls.find(
      (call) => (call[1] as RequestInit | undefined)?.method === "POST",
    );
    expect(postCall?.[0]).toBe(`${API_BASE_URL}/follow-up-tasks`);
    expect(JSON.parse((postCall?.[1] as RequestInit).body as string)).toEqual({
      customerId: mockTask.customerId,
      campaignId: mockTask.campaignId,
      assignedTo: authorizedUser.id,
      title: "Call Ada",
      description: "Discuss renewal options",
      dueDate: "2026-09-15",
      priority: "HIGH",
    });
    await waitFor(() => {
      expect(fetchMock.mock.calls.filter((call) => String(call[0]).includes("/follow-up-tasks")))
        .toHaveLength(3);
    });
  });

  it("lets authorized users assign follow-up tasks and refreshes the worklist", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(authorizedUser);
    await screen.findByText("Call Ada");

    const assigneeId = "10000000-0000-0000-0000-000000000371";
    await userEvent.clear(screen.getByLabelText("Assign follow-up Call Ada"));
    await userEvent.type(screen.getByLabelText("Assign follow-up Call Ada"), assigneeId);
    await userEvent.click(screen.getByRole("button", { name: "Assign" }));

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          (call) => (call[1] as RequestInit | undefined)?.method === "PUT",
        ),
      ).toBe(true);
    });
    const putCall = fetchMock.mock.calls.find(
      (call) => (call[1] as RequestInit | undefined)?.method === "PUT",
    );
    expect(putCall?.[0]).toBe(`${API_BASE_URL}/follow-up-tasks/${mockTask.id}/assign`);
    expect(JSON.parse((putCall?.[1] as RequestInit).body as string)).toEqual({
      assignedTo: assigneeId,
    });
    await waitFor(() => {
      expect(fetchMock.mock.calls.filter((call) => String(call[0]).includes("/follow-up-tasks")))
        .toHaveLength(3);
    });
  });

  it("hides create controls from sales agents", async () => {
    vi.stubGlobal("fetch", createFollowUpTasksFetchMock([mockTask]));

    renderFollowUpTasksPage(authorizedUser);
    await screen.findByText("Call Ada");

    expect(
      screen.queryByRole("heading", { name: "Create follow-up task" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Create task" })).not.toBeInTheDocument();
  });

  it("applies assignee priority status and due-date filters", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(authorizedUser);
    await screen.findByText("Call Ada");

    await userEvent.type(screen.getByLabelText("Follow-up assignee ID"), authorizedUser.id);
    await userEvent.selectOptions(screen.getByLabelText("Follow-up priority"), "HIGH");
    await userEvent.selectOptions(screen.getByLabelText("Follow-up status"), "OPEN");
    fireEvent.change(screen.getByLabelText("Follow-up due date from"), {
      target: { value: "2026-09-01" },
    });
    fireEvent.change(screen.getByLabelText("Follow-up due date to"), {
      target: { value: "2026-09-30" },
    });
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      const url = new URL(fetchMock.mock.calls[fetchMock.mock.calls.length - 1][0] as string);
      expect(url.pathname).toBe("/api/follow-up-tasks");
      expect(url.searchParams.get("assignedTo")).toBe(authorizedUser.id);
      expect(url.searchParams.get("priority")).toBe("HIGH");
      expect(url.searchParams.get("status")).toBe("OPEN");
      expect(url.searchParams.get("dueDateFrom")).toBe("2026-09-01");
      expect(url.searchParams.get("dueDateTo")).toBe("2026-09-30");
    });
  });

  it("shows empty state when no tasks match", async () => {
    vi.stubGlobal("fetch", createFollowUpTasksFetchMock([]));

    renderFollowUpTasksPage(authorizedUser);

    expect(
      await screen.findByText("No follow-up tasks match the current filters."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Follow-up tasks table" })).not.toBeInTheDocument();
  });

  it("blocks unauthorized users without calling the endpoint", () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(unauthorizedUser);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "You are not authorized to view follow-up tasks.",
    );
    expect(screen.queryByRole("table", { name: "Follow-up tasks table" })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
