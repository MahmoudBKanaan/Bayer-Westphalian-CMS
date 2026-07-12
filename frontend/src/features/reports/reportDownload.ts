import type { ReportExportStatus, ReportExportType, ReportExportView } from "@/api/reports";

/**
 * Report download helpers (KB item 445 / FR-109–FR-110).
 */

export type CampaignDownloadOption = {
  id: string;
  name: string;
  label: string;
};

export type RedownloadTarget = {
  campaignId: string;
  exportType: ReportExportType;
  campaignName: string;
};

export type LastDownloadSummary = {
  filename: string;
  exportType: ReportExportType;
  campaignLabel: string;
  completedAt: string;
};

const CAMPAIGN_CSV_PREFIX = "Campaign CSV:";
const CAMPAIGN_PDF_PREFIX = "Campaign PDF:";

/**
 * Whether a history row is eligible for UI re-download (completed campaign export).
 */
export function isRedownloadableExport(exportRow: ReportExportView): boolean {
  return (
    exportRow.status === "COMPLETED" &&
    (exportRow.exportType === "CSV" || exportRow.exportType === "PDF") &&
    parseCampaignReportName(exportRow.reportName) != null
  );
}

/**
 * Parses {@code Campaign CSV: Name} / {@code Campaign PDF: Name} report labels.
 */
export function parseCampaignReportName(
  reportName: string | null | undefined,
): { exportType: ReportExportType; campaignName: string } | null {
  if (reportName == null || reportName.trim() === "") {
    return null;
  }
  const trimmed = reportName.trim();
  if (trimmed.startsWith(CAMPAIGN_CSV_PREFIX)) {
    const campaignName = trimmed.slice(CAMPAIGN_CSV_PREFIX.length).trim();
    return campaignName === "" ? null : { exportType: "CSV", campaignName };
  }
  if (trimmed.startsWith(CAMPAIGN_PDF_PREFIX)) {
    const campaignName = trimmed.slice(CAMPAIGN_PDF_PREFIX.length).trim();
    return campaignName === "" ? null : { exportType: "PDF", campaignName };
  }
  return null;
}

/**
 * Resolves a completed history row to a campaign re-download target using known campaigns.
 */
export function resolveRedownloadTarget(
  exportRow: ReportExportView,
  campaigns: CampaignDownloadOption[],
): RedownloadTarget | null {
  if (exportRow.status !== "COMPLETED") {
    return null;
  }
  const parsed = parseCampaignReportName(exportRow.reportName);
  if (parsed == null) {
    return null;
  }
  // Prefer exportType from the history row when it disagrees with the name prefix.
  const exportType = exportRow.exportType ?? parsed.exportType;
  const campaign = campaigns.find(
    (option) => option.name.trim().toLowerCase() === parsed.campaignName.toLowerCase(),
  );
  if (campaign == null) {
    return null;
  }
  return {
    campaignId: campaign.id,
    exportType,
    campaignName: campaign.name,
  };
}

/**
 * Extracts a display filename from a stored {@code local://reports/.../file.csv} URL.
 */
export function filenameFromFileUrl(fileUrl: string | null | undefined): string | null {
  if (fileUrl == null || fileUrl.trim() === "") {
    return null;
  }
  const trimmed = fileUrl.trim();
  const segment = trimmed.split("/").filter(Boolean).at(-1);
  return segment == null || segment === "" ? null : segment;
}

/**
 * Human-readable status labels for export history.
 */
export function formatExportStatus(status: ReportExportStatus): string {
  switch (status) {
    case "REQUESTED":
      return "Requested";
    case "COMPLETED":
      return "Completed";
    case "FAILED":
      return "Failed";
    default:
      return status;
  }
}

/**
 * Builds a success notice for a finished download.
 */
export function buildDownloadSuccessMessage(summary: LastDownloadSummary): string {
  return `Downloaded ${summary.exportType} report for ${summary.campaignLabel}: ${summary.filename}`;
}

/**
 * Primary download button label for a format (KB item 445 download UI).
 */
export function downloadButtonLabel(
  exportType: ReportExportType,
  isPending: boolean,
): string {
  if (isPending) {
    return exportType === "PDF" ? "Downloading PDF…" : "Downloading CSV…";
  }
  return exportType === "PDF" ? "Download PDF" : "Download CSV";
}
