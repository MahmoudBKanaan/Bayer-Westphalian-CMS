import { describe, expect, it } from "vitest";
import { resolveApiBaseUrl } from "@/api/client";

describe("production frontend environment (item 719)", () => {
  it("accepts same-origin and HTTPS production API URLs", () => {
    expect(resolveApiBaseUrl("/api", "prod", true)).toBe("/api");
    expect(resolveApiBaseUrl("https://campaign.example.com/api/", "prod", true)).toBe(
      "https://campaign.example.com/api",
    );
  });

  it("rejects localhost and insecure HTTP URLs in production", () => {
    expect(() => resolveApiBaseUrl("http://localhost:8080/api", "prod", true)).toThrow(
      "Production frontend API URL",
    );
    expect(() => resolveApiBaseUrl("http://api.example.com/api", "prod", true)).toThrow(
      "Production frontend API URL",
    );
  });

  it("keeps the localhost fallback available outside production", () => {
    expect(resolveApiBaseUrl(undefined, "dev", false)).toBe("http://localhost:8080/api");
  });
});
