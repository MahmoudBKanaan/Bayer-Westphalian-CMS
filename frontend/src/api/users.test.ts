import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  assignRole,
  createUser,
  disableUser,
  enableUser,
  listUsers,
  resetPassword,
  updateUser,
} from "@/api/users";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const user = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE",
  lastLoginAt: null,
  roles: ["ADMIN"],
};

describe("users api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads users with optional status filtering", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Users loaded",
        data: [user],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(listUsers("ACTIVE")).resolves.toEqual([user]);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users?status=ACTIVE`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("sends create, update, disable, enable, role, and password requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "User changed",
        data: user,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await createUser({
      email: "new.user@bayer-westphalian.test",
      fullName: "New User",
      password: "StrongPass!2026",
    });
    await updateUser(user.id, { fullName: "Admin Updated", status: "ACTIVE" });
    await disableUser(user.id);
    await enableUser(user.id, user.fullName);
    await assignRole(user.id, "CAMPAIGN_MANAGER", user.id);
    await resetPassword(user.id, "NewPass!2026");

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users`, {
      body: JSON.stringify({
        email: "new.user@bayer-westphalian.test",
        fullName: "New User",
        password: "StrongPass!2026",
      }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${user.id}`, {
      body: JSON.stringify({ fullName: "Admin Updated", status: "ACTIVE" }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "PUT",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${user.id}/disable`, {
      headers: {
        "Content-Type": "application/json",
      },
      method: "PATCH",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${user.id}`, {
      body: JSON.stringify({ fullName: user.fullName, status: "ACTIVE" }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "PUT",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${user.id}/roles`, {
      body: JSON.stringify({
        roleName: "CAMPAIGN_MANAGER",
        assignedByUserId: user.id,
      }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/users/${user.id}/password`, {
      body: JSON.stringify({ password: "NewPass!2026" }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "PATCH",
    });
  });
});
