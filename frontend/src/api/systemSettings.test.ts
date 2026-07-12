import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { getSystemSettings, updateSystemSettings } from "@/api/systemSettings";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const sampleSettings = {
  id: "a1000000-0000-0000-0000-000000000001",
  monthlyContactLimit: 3,
  sendRetryLimit: 3,
  uninterestedExclusionDays: 90,
  updatedByUserId: null,
  updatedAt: "2026-07-01T00:00:00Z",
};

describe("systemSettings api (item 534)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("loads system settings from the admin endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "System settings loaded",
        data: sampleSettings,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");

    await expect(getSystemSettings()).resolves.toEqual(sampleSettings);
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/system-settings`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("updates system settings via PUT", async () => {
    const updated = {
      ...sampleSettings,
      monthlyContactLimit: 5,
      sendRetryLimit: 4,
      uninterestedExclusionDays: 120,
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "System settings updated",
        data: updated,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");

    const payload = {
      monthlyContactLimit: 5,
      sendRetryLimit: 4,
      uninterestedExclusionDays: 120,
    };
    await expect(updateSystemSettings(payload)).resolves.toEqual(updated);
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/system-settings`, {
      method: "PUT",
      body: JSON.stringify(payload),
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });
});
