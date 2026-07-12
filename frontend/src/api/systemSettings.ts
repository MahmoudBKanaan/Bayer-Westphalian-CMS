import { apiRequest } from "@/api/client";

/**
 * System Settings API client (KB item 534).
 *
 * Admin-only {@code GET/PUT /api/system-settings} for contact limits, send retry, and
 * uninterested exclusion period.
 */

export type SystemSettingsView = {
  id: string;
  monthlyContactLimit: number;
  sendRetryLimit: number;
  uninterestedExclusionDays: number;
  updatedByUserId: string | null;
  updatedAt: string | null;
};

export type UpdateSystemSettingsPayload = {
  monthlyContactLimit: number;
  sendRetryLimit: number;
  uninterestedExclusionDays: number;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function getSystemSettings(): Promise<SystemSettingsView> {
  const response = await apiRequest<ApiResponse<SystemSettingsView>>("/system-settings");
  return response.data;
}

export async function updateSystemSettings(
  payload: UpdateSystemSettingsPayload,
): Promise<SystemSettingsView> {
  const response = await apiRequest<ApiResponse<SystemSettingsView>>("/system-settings", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  return response.data;
}
