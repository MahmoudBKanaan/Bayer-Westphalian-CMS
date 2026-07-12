import { afterEach, describe, expect, it, vi } from "vitest";
import {
  API_BASE_URL,
  ApiError,
  apiDownload,
  apiRequest,
  isAuthorizationError,
  parseContentDispositionFilename,
} from "@/api/client";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

describe("apiRequest", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("sends JSON requests to the configured API base URL", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ status: "UP" }),
    });

    vi.stubGlobal("fetch", fetchMock);

    await expect(apiRequest<{ status: string }>("/health")).resolves.toEqual({ status: "UP" });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/health`, {
      headers: {
        "Content-Type": "application/json",
      },
    });
  });

  it("preserves caller headers and reports failed requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({ message: "Forbidden" }),
    });

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest("/secure", {
        headers: {
          Authorization: "Bearer token",
        },
      }),
    ).rejects.toThrow("Forbidden");

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/secure`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer token",
      },
    });
  });

  it("exposes authorization failures for user-facing error handling", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ message: "Bearer access token is required" }),
    });

    vi.stubGlobal("fetch", fetchMock);

    await expect(apiRequest("/users")).rejects.toMatchObject({
      status: 401,
      message: "Bearer access token is required",
    });
    expect(isAuthorizationError(new ApiError(403, "Forbidden"))).toBe(true);
    expect(isAuthorizationError(new ApiError(500, "Server error"))).toBe(false);
  });

  it("adds the stored access token to API requests", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "stored-access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true }),
    });

    vi.stubGlobal("fetch", fetchMock);

    await expect(apiRequest<{ success: boolean }>("/customers")).resolves.toEqual({
      success: true,
    });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer stored-access-token",
      },
    });
  });

  it("refreshes an expired access token and retries the original request", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "expired-access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "stored-refresh-token");
    sessionStorage.setItem(
      AUTH_STORAGE_KEYS.currentUser,
      JSON.stringify({
        id: "10000000-0000-0000-0000-000000000101",
        email: "campaign.manager@bayer-westphalian.test",
        fullName: "Campaign Manager",
        status: "ACTIVE",
        lastLoginAt: "2026-07-10T12:00:00Z",
      }),
    );
    const refreshedSession = {
      user: {
        id: "10000000-0000-0000-0000-000000000101",
        email: "campaign.manager@bayer-westphalian.test",
        fullName: "Campaign Manager",
        status: "ACTIVE",
        lastLoginAt: "2026-07-10T12:00:00Z",
      },
      tokens: {
        accessToken: "fresh-access-token",
        accessTokenExpiresAt: "2026-07-10T12:15:00Z",
        refreshToken: "fresh-refresh-token",
        refreshTokenExpiresAt: "2026-07-17T12:00:00Z",
      },
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: "Access token expired" }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          message: "Token refreshed successfully",
          data: refreshedSession,
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true, data: ["campaign"] }),
      });

    vi.stubGlobal("fetch", fetchMock);

    await expect(apiRequest<{ success: boolean; data: string[] }>("/campaigns")).resolves.toEqual({
      success: true,
      data: ["campaign"],
    });

    expect(fetchMock).toHaveBeenNthCalledWith(1, `${API_BASE_URL}/campaigns`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer expired-access-token",
      },
    });
    expect(fetchMock).toHaveBeenNthCalledWith(2, `${API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken: "stored-refresh-token" }),
    });
    expect(fetchMock).toHaveBeenNthCalledWith(3, `${API_BASE_URL}/campaigns`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer fresh-access-token",
      },
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBe("fresh-access-token");
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBe("fresh-refresh-token");
  });

  it("clears the stored session when refresh token recovery fails", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "expired-access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "expired-refresh-token");
    sessionStorage.setItem(
      AUTH_STORAGE_KEYS.currentUser,
      JSON.stringify({
        id: "10000000-0000-0000-0000-000000000101",
        email: "campaign.manager@bayer-westphalian.test",
        fullName: "Campaign Manager",
        status: "ACTIVE",
        lastLoginAt: null,
      }),
    );
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: "Access token expired" }),
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: "Refresh token expired" }),
      });

    vi.stubGlobal("fetch", fetchMock);

    await expect(apiRequest("/campaigns")).rejects.toMatchObject({
      status: 401,
      message: "Access token expired",
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("can skip stored access token attachment for public auth requests", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "stale-access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true }),
    });

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest<{ success: boolean }>("/auth/login", {
        authenticated: false,
        method: "POST",
      }),
    ).resolves.toEqual({ success: true });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/auth/login`, {
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
  });

  it("does not force a JSON content type for multipart form uploads", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "stored-access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true }),
    });
    const formData = new FormData();
    formData.set("file", new File(["firstName,lastName"], "customers.csv", { type: "text/csv" }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest<{ success: boolean }>("/customers/import", {
        method: "POST",
        body: formData,
      }),
    ).resolves.toEqual({ success: true });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/import`, {
      body: formData,
      headers: {
        Authorization: "Bearer stored-access-token",
      },
      method: "POST",
    });
  });

  it("downloads binary attachments with content-disposition filenames", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "stored-access-token");
    const blob = new Blob(["a,b\n1,2\n"], { type: "text/csv" });
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({
        "Content-Type": "text/csv; charset=UTF-8",
        "Content-Disposition": 'attachment; filename="report.csv"',
      }),
      blob: async () => blob,
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiDownload("/reports/campaigns/1/csv")).resolves.toEqual({
      filename: "report.csv",
      contentType: "text/csv; charset=UTF-8",
      blob,
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/reports/campaigns/1/csv`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer stored-access-token",
      },
    });
  });

  it("parses content-disposition filenames for report downloads", () => {
    expect(parseContentDispositionFilename('attachment; filename="campaign.pdf"')).toBe(
      "campaign.pdf",
    );
    expect(parseContentDispositionFilename("attachment; filename*=UTF-8''life%20report.csv")).toBe(
      "life report.csv",
    );
    expect(parseContentDispositionFilename(null)).toBeNull();
  });
});
