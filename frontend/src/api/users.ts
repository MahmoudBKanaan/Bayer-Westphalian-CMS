import { apiRequest } from "@/api/client";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

export type UserStatus = "ACTIVE" | "DISABLED" | "LOCKED";

export type UserView = {
  id: string;
  email: string;
  fullName: string;
  status: UserStatus;
  lastLoginAt: string | null;
  roles: SystemRoleName[];
};

export type CreateUserPayload = {
  email: string;
  password: string;
  fullName: string;
};

export type UpdateUserPayload = {
  fullName: string;
  status: UserStatus;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listUsers(status?: UserStatus | "ALL"): Promise<UserView[]> {
  const query = status == null || status === "ALL" ? "" : `?status=${status}`;
  const response = await apiRequest<ApiResponse<UserView[]>>(`/users${query}`);

  return response.data;
}

export async function createUser(payload: CreateUserPayload): Promise<UserView> {
  const response = await apiRequest<ApiResponse<UserView>>("/users", {
    method: "POST",
    body: JSON.stringify(payload),
  });

  return response.data;
}

export async function updateUser(id: string, payload: UpdateUserPayload): Promise<UserView> {
  const response = await apiRequest<ApiResponse<UserView>>(`/users/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

  return response.data;
}

export async function disableUser(id: string): Promise<UserView> {
  const response = await apiRequest<ApiResponse<UserView>>(`/users/${id}/disable`, {
    method: "PATCH",
  });

  return response.data;
}

export async function enableUser(id: string, fullName: string): Promise<UserView> {
  return updateUser(id, { fullName, status: "ACTIVE" });
}

export async function assignRole(
  id: string,
  roleName: SystemRoleName,
  assignedByUserId?: string,
): Promise<UserView> {
  const response = await apiRequest<ApiResponse<UserView>>(`/users/${id}/roles`, {
    method: "POST",
    body: JSON.stringify({ roleName, assignedByUserId }),
  });

  return response.data;
}

export async function resetPassword(id: string, password: string): Promise<UserView> {
  const response = await apiRequest<ApiResponse<UserView>>(`/users/${id}/password`, {
    method: "PATCH",
    body: JSON.stringify({ password }),
  });

  return response.data;
}
