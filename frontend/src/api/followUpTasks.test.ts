import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  assignFollowUpTask,
  createFollowUpTask,
  followUpTaskQuery,
  formatFollowUpEnum,
  listFollowUpTasks,
} from "@/api/followUpTasks";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const followUpTask = {
  id: "70000000-0000-0000-0000-000000000368",
  customerId: "20000000-0000-0000-0000-000000000368",
  customerFullName: "Ada Followup",
  campaignId: "50000000-0000-0000-0000-000000000368",
  campaignName: "Renewal campaign",
  assignedToUserId: "10000000-0000-0000-0000-000000000368",
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

describe("followUpTasks api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads follow-up tasks with KB filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Follow-up tasks loaded",
        data: [followUpTask],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listFollowUpTasks({
        customerId: followUpTask.customerId,
        assignedTo: followUpTask.assignedToUserId,
        priority: "HIGH",
        status: "OPEN",
        dueDateFrom: "2026-09-01",
        dueDateTo: "2026-09-30",
      }),
    ).resolves.toEqual([followUpTask]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/follow-up-tasks?customerId=${followUpTask.customerId}&assignedTo=${followUpTask.assignedToUserId}&priority=HIGH&status=OPEN&dueDateFrom=2026-09-01&dueDateTo=2026-09-30`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("omits blank filters and ALL enum values", () => {
    expect(
      followUpTaskQuery({
        customerId: "",
        assignedTo: " ",
        priority: "ALL",
        status: "COMPLETED",
        dueDateFrom: "",
        dueDateTo: "2026-09-30",
      }),
    ).toBe("?status=COMPLETED&dueDateTo=2026-09-30");
  });

  it("creates follow-up tasks with optional campaign assignee description and due date", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Follow-up task created",
        data: followUpTask,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createFollowUpTask({
        customerId: ` ${followUpTask.customerId} `,
        campaignId: followUpTask.campaignId,
        assignedTo: followUpTask.assignedToUserId,
        title: " Call Ada ",
        description: " Discuss renewal options ",
        dueDate: "2026-09-15",
        priority: "HIGH",
      }),
    ).resolves.toEqual(followUpTask);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/follow-up-tasks`, {
      method: "POST",
      body: JSON.stringify({
        customerId: followUpTask.customerId,
        campaignId: followUpTask.campaignId,
        assignedTo: followUpTask.assignedToUserId,
        title: "Call Ada",
        description: "Discuss renewal options",
        dueDate: "2026-09-15",
        priority: "HIGH",
      }),
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("assigns follow-up tasks to a user", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Follow-up task assigned",
        data: followUpTask,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      assignFollowUpTask({
        taskId: followUpTask.id,
        assignedTo: ` ${followUpTask.assignedToUserId} `,
      }),
    ).resolves.toEqual(followUpTask);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/follow-up-tasks/${followUpTask.id}/assign`,
      {
        method: "PUT",
        body: JSON.stringify({ assignedTo: followUpTask.assignedToUserId }),
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("formats follow-up enum display text", () => {
    expect(formatFollowUpEnum("IN_PROGRESS")).toBe("In Progress");
  });
});
