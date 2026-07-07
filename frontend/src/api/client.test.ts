import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL, ApiError, apiRequest, isAuthorizationError } from "@/api/client";
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
});
