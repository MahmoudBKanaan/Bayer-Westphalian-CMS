import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import {
  createSegment,
  deleteSegment,
  emptySegmentForm,
  getSegment,
  listSegments,
  previewSegment,
  segmentToForm,
  updateSegment,
  type SegmentView,
} from "@/api/segments";

const segment: SegmentView = {
  id: "42000000-0000-0000-0000-000000000001",
  name: "Munich prospects",
  description: "Customers in Munich",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  visibility: "TEAM",
  criteria: [
    {
      id: "43000000-0000-0000-0000-000000000001",
      segmentId: "42000000-0000-0000-0000-000000000001",
      fieldName: "city",
      operator: "EQUALS",
      value: "Munich",
      logicalGroup: "location",
      joinOperator: "AND",
    },
  ],
  createdAt: "2026-07-09T10:00:00Z",
  updatedAt: "2026-07-09T10:05:00Z",
};

describe("segments API", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("lists segments with optional search filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, message: "Segments loaded", data: [segment] }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const result = await listSegments({ term: "Munich", visibility: "TEAM" });

    expect(result).toEqual([segment]);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/segments?term=Munich&visibility=TEAM`,
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer access-token",
        }),
      }),
    );
  });

  it("loads a single segment by id", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, message: "Segment loaded", data: segment }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(getSegment(segment.id)).resolves.toEqual(segment);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/segments/${segment.id}`,
      expect.any(Object),
    );
  });

  it("creates and updates segments with normalized write payloads", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true, message: "Segment created", data: segment }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          message: "Segment updated",
          data: { ...segment, name: "Updated" },
        }),
      });
    vi.stubGlobal("fetch", fetchMock);

    const form = {
      name: "  Munich prospects  ",
      description: "  Customers in Munich  ",
      visibility: "TEAM" as const,
      criteria: [
        {
          fieldName: " city ",
          operator: "EQUALS" as const,
          value: " Munich ",
          logicalGroup: " location ",
          joinOperator: "AND" as const,
        },
        {
          fieldName: "   ",
          operator: "EQUALS" as const,
          value: "ignored",
        },
      ],
    };

    await createSegment(form);
    await updateSegment(segment.id, { ...form, name: "Updated" });

    const createBody = JSON.parse(String(fetchMock.mock.calls[0][1].body));
    expect(createBody).toEqual({
      name: "Munich prospects",
      description: "Customers in Munich",
      visibility: "TEAM",
      criteria: [
        {
          fieldName: "city",
          operator: "EQUALS",
          value: "Munich",
          logicalGroup: "location",
          joinOperator: "AND",
        },
      ],
    });
    expect(fetchMock.mock.calls[1][0]).toBe(`${API_BASE_URL}/segments/${segment.id}`);
    expect(fetchMock.mock.calls[1][1].method).toBe("PUT");
  });

  it("deletes a segment", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, message: "Segment deleted", data: null }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await deleteSegment(segment.id);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/segments/${segment.id}`,
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("previews audience for criteria", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const preview = {
      totalAudienceCount: 2,
      eligibleCount: 1,
      excludedCount: 1,
      matchingCustomers: [],
      exclusionReasonSummary: [
        { code: "DO_NOT_CONTACT", message: "Customer has do-not-contact enabled", count: 1 },
      ],
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, message: "Segment preview loaded", data: preview }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const result = await previewSegment([
      { fieldName: "city", operator: "EQUALS", value: "Munich", joinOperator: "AND" },
    ]);

    expect(result).toEqual(preview);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/segments/preview`,
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("uses only eligibility-gated preview endpoint as contactable audience API (item 208)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Segment preview loaded",
        data: {
          totalAudienceCount: 1,
          eligibleCount: 1,
          excludedCount: 0,
          matchingCustomers: [],
          exclusionReasonSummary: [],
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await previewSegment([
      { fieldName: "city", operator: "EQUALS", value: "Munich", joinOperator: "AND" },
    ]);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toBe(`${API_BASE_URL}/segments/preview`);
    expect(String(url)).not.toMatch(/matching/i);
  });

  it("preview response includes FR-079 eligible and excluded counts (item 199)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const preview = {
      totalAudienceCount: 10,
      eligibleCount: 4,
      excludedCount: 6,
      matchingCustomers: [
        {
          id: "20000000-0000-0000-0000-000000000201",
          customerType: "PROSPECT",
          firstName: "Lena",
          lastName: "Mueller",
          fullName: "Lena Mueller",
          email: "lena@example.com",
          city: "Munich",
          country: "Germany",
          status: "ACTIVE",
          doNotContact: false,
        },
      ],
      exclusionReasonSummary: [
        { code: "DO_NOT_CONTACT", message: "Customer has do-not-contact enabled", count: 4 },
        {
          code: "MARKETING_OPT_OUT",
          message: "Customer has withdrawn or rejected marketing consent",
          count: 2,
        },
      ],
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ success: true, message: "Segment preview loaded", data: preview }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const result = await previewSegment([
      { fieldName: "city", operator: "EQUALS", value: "Munich", joinOperator: "AND" },
    ]);

    expect(result.totalAudienceCount).toBe(10);
    expect(result.eligibleCount).toBe(4);
    expect(result.excludedCount).toBe(6);
    expect(result.eligibleCount + result.excludedCount).toBe(result.totalAudienceCount);
    expect(result.exclusionReasonSummary.reduce((sum, row) => sum + row.count, 0)).toBe(
      result.excludedCount,
    );
  });

  it("maps segment views to forms and provides empty form defaults", () => {
    expect(emptySegmentForm()).toEqual({
      name: "",
      description: "",
      visibility: "PRIVATE",
      criteria: [],
    });
    expect(segmentToForm(segment)).toEqual({
      name: "Munich prospects",
      description: "Customers in Munich",
      visibility: "TEAM",
      criteria: [
        {
          fieldName: "city",
          operator: "EQUALS",
          value: "Munich",
          logicalGroup: "location",
          joinOperator: "AND",
        },
      ],
    });
  });
});
