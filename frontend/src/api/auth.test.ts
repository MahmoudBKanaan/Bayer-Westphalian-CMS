import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { login } from "@/api/auth";

const session = {
  user: {
    id: "10000000-0000-0000-0000-000000009901",
    email: "admin@bayer-westphalian.test",
    fullName: "Admin User",
    status: "ACTIVE" as const,
    lastLoginAt: "2026-07-04T12:00:00Z",
  },
  tokens: {
    accessToken: "access-token",
    accessTokenExpiresAt: "2026-07-04T12:15:00Z",
    refreshToken: "refresh-token",
    refreshTokenExpiresAt: "2026-07-11T12:00:00Z",
  },
};

describe("auth api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("posts login credentials to the KB auth endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Login successful",
        data: session,
      }),
    });

    vi.stubGlobal("fetch", fetchMock);

    await expect(login("admin@bayer-westphalian.test", "StrongPassword!2026")).resolves.toEqual(
      session,
    );
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/auth/login`, {
      body: JSON.stringify({
        email: "admin@bayer-westphalian.test",
        password: "StrongPassword!2026",
      }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
  });
});
