import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  auditLogsQuery,
  emptyAuditLogSearchFilters,
  formatAuditAction,
  formatAuditEntityType,
  getEntityHistory,
  hasActiveAuditFilters,
  listAuditLogs,
  summarizeAuditValue,
  toInstantQueryValue,
} from "@/api/auditLogs";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const sampleLog = {
  id: "53000000-0000-0000-0000-000000000001",
  actorUserId: "10000000-0000-0000-0000-000000000001",
  action: "CREATE",
  entityType: "products",
  entityId: "41000000-0000-0000-0000-000000000201",
  oldValue: null,
  newValue: { name: "Life Protection" },
  ipAddress: "127.0.0.1",
  createdAt: "2026-07-07T15:02:46Z",
};

describe("auditLogs api", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("loads audit logs from the backend audit endpoint without filters", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Audit logs loaded",
        data: [sampleLog],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");

    await expect(listAuditLogs()).resolves.toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/audit-logs`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("loads filtered audit logs with actor, action, entity, and date query params (item 533)", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Audit logs loaded",
        data: [sampleLog],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");

    const actorUserId = "10000000-0000-0000-0000-000000000001";
    const entityId = "41000000-0000-0000-0000-000000000201";
    const filters = {
      actorUserId,
      action: "CREATE",
      entityType: "products",
      entityId,
      createdFrom: "2026-07-01T00:00",
      createdTo: "2026-07-31T23:59",
    };

    await expect(listAuditLogs(filters)).resolves.toHaveLength(1);

    const calledUrl = String(fetchMock.mock.calls[0]?.[0]);
    expect(calledUrl.startsWith(`${API_BASE_URL}/audit-logs?`)).toBe(true);
    expect(calledUrl).toContain(`actorUserId=${actorUserId}`);
    expect(calledUrl).toContain("action=CREATE");
    expect(calledUrl).toContain("entityType=products");
    expect(calledUrl).toContain(`entityId=${entityId}`);
    expect(calledUrl).toContain("createdFrom=");
    expect(calledUrl).toContain("createdTo=");
  });

  it("loads entity history from the backend entity-history endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Entity audit history loaded",
        data: [sampleLog],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");

    const entityId = "41000000-0000-0000-0000-000000000201";
    await expect(
      getEntityHistory({ entityType: "products", entityId }),
    ).resolves.toHaveLength(1);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/audit-logs/entity-history?entityType=products&entityId=${entityId}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("builds empty query for blank filters and detects active filters", () => {
    expect(auditLogsQuery(emptyAuditLogSearchFilters)).toBe("");
    expect(hasActiveAuditFilters(emptyAuditLogSearchFilters)).toBe(false);
    expect(
      hasActiveAuditFilters({
        ...emptyAuditLogSearchFilters,
        action: "APPROVE",
      }),
    ).toBe(true);
    expect(auditLogsQuery({ ...emptyAuditLogSearchFilters, action: "APPROVE" })).toBe(
      "?action=APPROVE",
    );
  });

  it("converts datetime-local values to ISO instants for the API", () => {
    expect(toInstantQueryValue("")).toBeNull();
    expect(toInstantQueryValue("not-a-date")).toBeNull();
    const iso = toInstantQueryValue("2026-07-07T15:02");
    expect(iso).not.toBeNull();
    expect(iso).toMatch(/2026-07-07T/);
  });

  it("formats action and entity tokens for the Audit Log screen", () => {
    expect(formatAuditAction("EXPORT_REPORT")).toBe("Export Report");
    expect(formatAuditEntityType("consent_records")).toBe("Consent Records");
    expect(summarizeAuditValue({ status: "WITHDRAWN", channel: "EMAIL" })).toContain(
      "status: WITHDRAWN",
    );
  });
});
