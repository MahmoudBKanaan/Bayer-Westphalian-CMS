import { apiRequest } from "@/api/client";

export type FollowUpTaskStatus = "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type FollowUpTaskPriority = "LOW" | "MEDIUM" | "HIGH";

export type FollowUpTaskView = {
  id: string;
  customerId: string;
  customerFullName: string;
  campaignId: string | null;
  campaignName: string | null;
  assignedToUserId: string | null;
  assignedToFullName: string | null;
  title: string;
  description: string | null;
  dueDate: string | null;
  status: FollowUpTaskStatus;
  priority: FollowUpTaskPriority;
  completedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type FollowUpTaskFilters = {
  customerId: string;
  assignedTo: string;
  priority: FollowUpTaskPriority | "ALL";
  status: FollowUpTaskStatus | "ALL";
  dueDateFrom: string;
  dueDateTo: string;
};

export type CreateFollowUpTaskInput = {
  customerId: string;
  campaignId: string;
  assignedTo: string;
  title: string;
  description: string;
  dueDate: string;
  priority: FollowUpTaskPriority;
};

export type AssignFollowUpTaskInput = {
  taskId: string;
  assignedTo: string;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export const followUpTaskPriorities: FollowUpTaskPriority[] = ["LOW", "MEDIUM", "HIGH"];

export const followUpTaskStatuses: FollowUpTaskStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELLED",
];

export const emptyFollowUpTaskFilters: FollowUpTaskFilters = {
  customerId: "",
  assignedTo: "",
  priority: "ALL",
  status: "ALL",
  dueDateFrom: "",
  dueDateTo: "",
};

export async function listFollowUpTasks(
  filters: FollowUpTaskFilters = emptyFollowUpTaskFilters,
): Promise<FollowUpTaskView[]> {
  const response = await apiRequest<ApiResponse<FollowUpTaskView[]>>(
    `/follow-up-tasks${followUpTaskQuery(filters)}`,
  );
  return response.data;
}

export async function createFollowUpTask(input: CreateFollowUpTaskInput): Promise<FollowUpTaskView> {
  const payload = {
    customerId: input.customerId.trim(),
    campaignId: optionalString(input.campaignId),
    assignedTo: optionalString(input.assignedTo),
    title: input.title.trim(),
    description: optionalString(input.description),
    dueDate: optionalString(input.dueDate),
    priority: input.priority,
  };
  const response = await apiRequest<ApiResponse<FollowUpTaskView>>("/follow-up-tasks", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return response.data;
}

export async function assignFollowUpTask(input: AssignFollowUpTaskInput): Promise<FollowUpTaskView> {
  const response = await apiRequest<ApiResponse<FollowUpTaskView>>(
    `/follow-up-tasks/${input.taskId}/assign`,
    {
      method: "PUT",
      body: JSON.stringify({ assignedTo: input.assignedTo.trim() }),
    },
  );
  return response.data;
}

export function followUpTaskQuery(filters: FollowUpTaskFilters) {
  const params = new URLSearchParams();
  appendOptionalParam(params, "customerId", filters.customerId);
  appendOptionalParam(params, "assignedTo", filters.assignedTo);
  appendEnumParam(params, "priority", filters.priority);
  appendEnumParam(params, "status", filters.status);
  appendOptionalParam(params, "dueDateFrom", filters.dueDateFrom);
  appendOptionalParam(params, "dueDateTo", filters.dueDateTo);

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

export function formatFollowUpEnum(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function appendEnumParam(
  params: URLSearchParams,
  key: string,
  value: FollowUpTaskPriority | FollowUpTaskStatus | "ALL",
) {
  if (value !== "ALL") {
    params.set(key, value);
  }
}

function appendOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
