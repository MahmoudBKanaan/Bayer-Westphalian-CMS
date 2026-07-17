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
  // Auth session is stored in localStorage (legacy sessionStorage is still migrated on load).
  localStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(user.roles));
  localStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  localStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(user));
  sessionStorage.clear();

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

const mockCsaAssignee = {
  id: "10000000-0000-0000-0000-000000000006",
  fullName: "Test Customer Service Agent",
  email: "customer.service@bayer-westphalian.test",
};

function createFollowUpTasksFetchMock(tasks: unknown[] = [mockTask]) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.startsWith(`${API_BASE_URL}/follow-up-tasks`)) {
      if (url.includes("/assignee-options")) {
        return jsonResponse({
          success: true,
          message: "Follow-up assignee options loaded",
          data: [mockCsaAssignee],
        });
      }
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
      if (init?.method === "PUT" && url.endsWith(`/follow-up-tasks/${mockTask.id}/complete`)) {
        return jsonResponse({
          success: true,
          message: "Follow-up task completed",
          data: {
            ...mockTask,
            status: "COMPLETED",
            completedAt: "2026-07-11T09:30:00Z",
          },
        });
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
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads follow-up tasks and displays assigned worklist for agents", async () => {
    vi.stubGlobal("fetch", createFollowUpTasksFetchMock([mockTask]));

    renderFollowUpTasksPage(authorizedUser);

    expect(await screen.findByRole("heading", { name: "Assigned worklist" })).toBeInTheDocument();
    expect(await screen.findByRole("table", { name: "Follow-up tasks table" })).toBeInTheDocument();
    expect(screen.getByText("Call Ada")).toBeInTheDocument();
    expect(screen.getByText("Ada Followup")).toBeInTheDocument();
    expect(screen.getByText("Renewal campaign")).toBeInTheDocument();
    expect(screen.getByText("Sales Agent")).toBeInTheDocument();
    // Assignee can mark own open task complete
    expect(
      screen.getByRole("button", { name: "Mark follow-up Call Ada complete" }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("High").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Open").length).toBeGreaterThanOrEqual(1);
  });

  it("lets campaign managers create follow-up tasks and refreshes the worklist", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(createAuthorizedUser);
    await screen.findByText("Call Ada");

    const expandCreate = await screen.findByRole("button", { name: "Expand create task" });
    await userEvent.click(expandCreate);
    fireEvent.change(screen.getByLabelText("Follow-up customer ID"), {
      target: { value: mockTask.customerId },
    });
    fireEvent.change(screen.getByLabelText("Follow-up task title"), {
      target: { value: "Call Ada" },
    });
    fireEvent.change(screen.getByLabelText("Follow-up assigned Customer Service Agent"), {
      target: { value: mockCsaAssignee.id },
    });
    fireEvent.change(screen.getByLabelText("Follow-up campaign ID"), {
      target: { value: mockTask.campaignId },
    });
    fireEvent.change(screen.getByLabelText("Create follow-up priority"), {
      target: { value: "HIGH" },
    });
    fireEvent.change(screen.getByLabelText("Create follow-up due date"), {
      target: { value: "2026-09-15" },
    });
    fireEvent.change(screen.getByLabelText("Follow-up description"), {
      target: { value: "Discuss renewal options" },
    });
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
      assignedTo: mockCsaAssignee.id,
      title: "Call Ada",
      description: "Discuss renewal options",
      dueDate: "2026-09-15",
      priority: "HIGH",
    });
  }, 15_000);

  it("lets campaign managers assign follow-up tasks and refreshes the worklist", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(createAuthorizedUser);
    await screen.findByText("Call Ada");

    const assignSelect = await screen.findByLabelText("Assign follow-up Call Ada");
    await userEvent.selectOptions(assignSelect, mockCsaAssignee.id);
    await userEvent.click(screen.getByRole("button", { name: "Assign" }));

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          (call) =>
            String(call[0]).endsWith(`/follow-up-tasks/${mockTask.id}/assign`) &&
            (call[1] as RequestInit | undefined)?.method === "PUT",
        ),
      ).toBe(true);
    });
    const putCall = fetchMock.mock.calls.find((call) =>
      String(call[0]).endsWith(`/follow-up-tasks/${mockTask.id}/assign`),
    );
    expect(JSON.parse((putCall?.[1] as RequestInit).body as string)).toEqual({
      assignedTo: mockCsaAssignee.id,
    });
  });

  it("lets the assignee mark a follow-up task complete", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(authorizedUser);
    await screen.findByText("Call Ada");

    await userEvent.click(
      screen.getByRole("button", { name: "Mark follow-up Call Ada complete" }),
    );

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          (call) =>
            String(call[0]).endsWith(`/follow-up-tasks/${mockTask.id}/complete`) &&
            (call[1] as RequestInit | undefined)?.method === "PUT",
        ),
      ).toBe(true);
    });
  });

  it("lets managers mark any follow-up task complete", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(createAuthorizedUser);
    await screen.findByText("Call Ada");

    await userEvent.click(
      screen.getByRole("button", { name: "Mark follow-up Call Ada complete" }),
    );

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          (call) =>
            String(call[0]).endsWith(`/follow-up-tasks/${mockTask.id}/complete`) &&
            (call[1] as RequestInit | undefined)?.method === "PUT",
        ),
      ).toBe(true);
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

  it("applies priority status and due-date filters for assigned worklist", async () => {
    const fetchMock = createFollowUpTasksFetchMock([mockTask]);
    vi.stubGlobal("fetch", fetchMock);

    renderFollowUpTasksPage(authorizedUser);
    await screen.findByText("Call Ada");

    await userEvent.click(screen.getByRole("button", { name: "Expand filters" }));
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
      const listCalls = fetchMock.mock.calls.filter(
        (call) =>
          String(call[0]).includes("/follow-up-tasks") &&
          !String(call[0]).includes("assignee-options") &&
          !(call[1] as RequestInit | undefined)?.method,
      );
      const url = new URL(listCalls[listCalls.length - 1][0] as string);
      expect(url.pathname).toBe("/api/follow-up-tasks");
      // Agent worklist always forces own assignee
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
