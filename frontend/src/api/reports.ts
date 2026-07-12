import {
  apiDownload,
  apiRequest,
  triggerBrowserDownload,
  type DownloadedFile,
} from "@/api/client";

/**
 * Reports API client (KB epic E20 / FR-109–FR-110 / items 437–439, 442).
 *
 * <ul>
 *   <li>{@code GET /api/reports/campaigns/{id}/csv} — campaign CSV (FR-109)</li>
 *   <li>{@code GET /api/reports/campaigns/{id}/pdf} — campaign PDF (FR-110)</li>
 *   <li>{@code GET /api/reports/exports} — export history</li>
 *   <li>{@code GET /api/reports/exports/{id}} — single export row</li>
 * </ul>
 */

export type ReportExportType = "CSV" | "PDF";
export type ReportExportStatus = "REQUESTED" | "COMPLETED" | "FAILED";

export type ReportExportView = {
  id: string;
  requestedByUserId: string | null;
  reportName: string;
  exportType: ReportExportType;
  status: ReportExportStatus;
  fileUrl: string | null;
  requestedAt: string | null;
  completedAt: string | null;
};

export type ListExportHistoryFilters = {
  mine?: boolean;
  status?: ReportExportStatus | "ALL";
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

/** Downloads a campaign performance CSV (KB FR-109 / item 437). */
export async function downloadCampaignCsv(campaignId: string): Promise<DownloadedFile> {
  return apiDownload(`/reports/campaigns/${encodeURIComponent(campaignId)}/csv`);
}

/** Downloads a campaign performance PDF (KB FR-110 / item 438). */
export async function downloadCampaignPdf(campaignId: string): Promise<DownloadedFile> {
  return apiDownload(`/reports/campaigns/${encodeURIComponent(campaignId)}/pdf`);
}

/**
 * Downloads a campaign CSV and triggers the browser save dialog.
 */
export async function exportCampaignCsv(campaignId: string): Promise<DownloadedFile> {
  const file = await downloadCampaignCsv(campaignId);
  triggerBrowserDownload(file);
  return file;
}

/**
 * Downloads a campaign PDF and triggers the browser save dialog.
 */
export async function exportCampaignPdf(campaignId: string): Promise<DownloadedFile> {
  const file = await downloadCampaignPdf(campaignId);
  triggerBrowserDownload(file);
  return file;
}

/** Lists stored report export history (KB item 439). */
export async function listExportHistory(
  filters?: ListExportHistoryFilters,
): Promise<ReportExportView[]> {
  const response = await apiRequest<ApiResponse<ReportExportView[]>>(
    `/reports/exports${exportHistoryQuery(filters)}`,
  );
  return response.data ?? [];
}

/** Loads a single export history row (KB item 439). */
export async function getExportHistory(exportId: string): Promise<ReportExportView> {
  const response = await apiRequest<ApiResponse<ReportExportView>>(
    `/reports/exports/${encodeURIComponent(exportId)}`,
  );
  if (response.data == null) {
    throw new Error("Report export payload was empty");
  }
  return response.data;
}

function exportHistoryQuery(filters?: ListExportHistoryFilters): string {
  if (filters == null) {
    return "";
  }
  const params = new URLSearchParams();
  if (filters.mine === true) {
    params.set("mine", "true");
  }
  if (filters.status != null && filters.status !== "ALL") {
    params.set("status", filters.status);
  }
  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}
