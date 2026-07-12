import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  downloadCampaignCsv,
  downloadCampaignPdf,
  exportCampaignCsv,
  exportCampaignPdf,
  getExportHistory,
  listExportHistory,
} from "@/api/reports";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const exportHistory = [
  {
    id: "56000000-0000-0000-0000-000000000442",
    requestedByUserId: "10000000-0000-0000-0000-000000000442",
    reportName: "Campaign Spring Life Drive",
    exportType: "CSV",
    status: "COMPLETED",
    fileUrl: "local://reports/56000000-0000-0000-0000-000000000442/campaign.csv",
    requestedAt: "2026-07-11T10:00:00Z",
    completedAt: "2026-07-11T10:00:01Z",
  },
];

describe("reports api (item 442)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("downloads campaign CSV attachments (FR-109)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const blob = new Blob(["campaign,sent\nA,10\n"], { type: "text/csv" });
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      headers: new Headers({
        "Content-Type": "text/csv; charset=UTF-8",
        "Content-Disposition": 'attachment; filename="campaign-report.csv"',
      }),
      blob: async () => blob,
    }));
    vi.stubGlobal("fetch", fetchMock);

    const campaignId = "50000000-0000-0000-0000-000000000442";
    await expect(downloadCampaignCsv(campaignId)).resolves.toEqual({
      filename: "campaign-report.csv",
      contentType: "text/csv; charset=UTF-8",
      blob,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/reports/campaigns/${campaignId}/csv`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("downloads campaign PDF attachments (FR-110)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const blob = new Blob(["%PDF-1.4"], { type: "application/pdf" });
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      headers: new Headers({
        "Content-Type": "application/pdf",
        "Content-Disposition": "attachment; filename*=UTF-8''campaign-report.pdf",
      }),
      blob: async () => blob,
    }));
    vi.stubGlobal("fetch", fetchMock);

    const campaignId = "50000000-0000-0000-0000-000000000442";
    await expect(downloadCampaignPdf(campaignId)).resolves.toEqual({
      filename: "campaign-report.pdf",
      contentType: "application/pdf",
      blob,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/reports/campaigns/${campaignId}/pdf`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("triggers browser download for campaign CSV export", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const blob = new Blob(["csv"], { type: "text/csv" });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        headers: new Headers({
          "Content-Type": "text/csv",
          "Content-Disposition": 'attachment; filename="export.csv"',
        }),
        blob: async () => blob,
      })),
    );

    const createObjectURL = vi.fn(() => "blob:mock-url");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL,
      revokeObjectURL,
    });

    const click = vi.fn();
    const appendChild = vi.spyOn(document.body, "appendChild").mockImplementation((node) => {
      if (node instanceof HTMLAnchorElement) {
        Object.defineProperty(node, "click", { value: click });
      }
      return node;
    });
    const removeChild = vi.spyOn(HTMLElement.prototype, "remove").mockImplementation(() => undefined);

    await exportCampaignCsv("50000000-0000-0000-0000-000000000442");

    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(click).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith("blob:mock-url");

    appendChild.mockRestore();
    removeChild.mockRestore();
  });

  it("triggers browser download for campaign PDF export", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const blob = new Blob(["%PDF"], { type: "application/pdf" });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        headers: new Headers({
          "Content-Type": "application/pdf",
          "Content-Disposition": 'attachment; filename="export.pdf"',
        }),
        blob: async () => blob,
      })),
    );

    const createObjectURL = vi.fn(() => "blob:pdf-url");
    const revokeObjectURL = vi.fn();
    vi.stubGlobal("URL", {
      ...URL,
      createObjectURL,
      revokeObjectURL,
    });
    const click = vi.fn();
    vi.spyOn(document.body, "appendChild").mockImplementation((node) => {
      if (node instanceof HTMLAnchorElement) {
        Object.defineProperty(node, "click", { value: click });
      }
      return node;
    });
    vi.spyOn(HTMLElement.prototype, "remove").mockImplementation(() => undefined);

    await exportCampaignPdf("50000000-0000-0000-0000-000000000442");
    expect(createObjectURL).toHaveBeenCalledWith(blob);
    expect(click).toHaveBeenCalled();
  });

  it("lists export history with mine and status filters (item 439)", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        message: "Report export history loaded",
        data: exportHistory,
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(listExportHistory({ mine: true })).resolves.toEqual(exportHistory);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/reports/exports?mine=true`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );

    await expect(listExportHistory({ status: "COMPLETED" })).resolves.toEqual(exportHistory);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/reports/exports?status=COMPLETED`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });

  it("falls back to empty export history when data is null", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => ({
          success: true,
          message: "Report export history loaded",
          data: null,
        }),
      })),
    );

    await expect(listExportHistory()).resolves.toEqual([]);
  });

  it("loads a single export history row", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const exportId = exportHistory[0].id;
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        success: true,
        message: "Report export loaded",
        data: exportHistory[0],
      }),
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getExportHistory(exportId)).resolves.toEqual(exportHistory[0]);
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/reports/exports/${exportId}`,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
      }),
    );
  });
});
