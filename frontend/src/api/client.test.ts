import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL, apiRequest } from "@/api/client";

describe("apiRequest", () => {
  afterEach(() => {
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
    ).rejects.toThrow("API request failed with 403");

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/secure`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer token",
      },
    });
  });
});
