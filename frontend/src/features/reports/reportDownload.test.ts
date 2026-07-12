import { describe, expect, it } from "vitest";
import type { ReportExportView } from "@/api/reports";
import {
  buildDownloadSuccessMessage,
  downloadButtonLabel,
  filenameFromFileUrl,
  formatExportStatus,
  isRedownloadableExport,
  parseCampaignReportName,
  resolveRedownloadTarget,
} from "@/features/reports/reportDownload";

const campaigns = [
  {
    id: "50000000-0000-0000-0000-000000000445",
    name: "Spring Life Drive",
    label: "Spring Life Drive (ACTIVE)",
  },
];

const completedCsv: ReportExportView = {
  id: "56000000-0000-0000-0000-000000000445",
  requestedByUserId: "10000000-0000-0000-0000-000000000445",
  reportName: "Campaign CSV: Spring Life Drive",
  exportType: "CSV",
  status: "COMPLETED",
  fileUrl: "local://reports/56000000-0000-0000-0000-000000000445/Spring-Life-Drive.csv",
  requestedAt: "2026-07-11T10:00:00Z",
  completedAt: "2026-07-11T10:00:01Z",
};

describe("reportDownload helpers (item 445)", () => {
  it("parses campaign report name prefixes from export history", () => {
    expect(parseCampaignReportName("Campaign CSV: Spring Life Drive")).toEqual({
      exportType: "CSV",
      campaignName: "Spring Life Drive",
    });
    expect(parseCampaignReportName("Campaign PDF: Auto Renewal")).toEqual({
      exportType: "PDF",
      campaignName: "Auto Renewal",
    });
    expect(parseCampaignReportName("Audit history export")).toBeNull();
    expect(parseCampaignReportName("")).toBeNull();
  });

  it("resolves re-download targets for completed campaign exports", () => {
    expect(isRedownloadableExport(completedCsv)).toBe(true);
    expect(resolveRedownloadTarget(completedCsv, campaigns)).toEqual({
      campaignId: "50000000-0000-0000-0000-000000000445",
      exportType: "CSV",
      campaignName: "Spring Life Drive",
    });
  });

  it("does not re-download failed or unmatched history rows", () => {
    expect(
      resolveRedownloadTarget({ ...completedCsv, status: "FAILED" }, campaigns),
    ).toBeNull();
    expect(
      resolveRedownloadTarget(
        { ...completedCsv, reportName: "Campaign CSV: Unknown Campaign" },
        campaigns,
      ),
    ).toBeNull();
    expect(isRedownloadableExport({ ...completedCsv, status: "REQUESTED" })).toBe(false);
  });

  it("extracts filenames from local report file URLs", () => {
    expect(
      filenameFromFileUrl(
        "local://reports/56000000-0000-0000-0000-000000000445/Spring-Life-Drive.csv",
      ),
    ).toBe("Spring-Life-Drive.csv");
    expect(filenameFromFileUrl(null)).toBeNull();
  });

  it("formats export statuses and download notices", () => {
    expect(formatExportStatus("COMPLETED")).toBe("Completed");
    expect(formatExportStatus("FAILED")).toBe("Failed");
    expect(downloadButtonLabel("CSV", false)).toBe("Download CSV");
    expect(downloadButtonLabel("PDF", true)).toBe("Downloading PDF…");
    expect(
      buildDownloadSuccessMessage({
        filename: "campaign.csv",
        exportType: "CSV",
        campaignLabel: "Spring Life Drive (ACTIVE)",
        completedAt: "2026-07-11T10:00:00Z",
      }),
    ).toBe("Downloaded CSV report for Spring Life Drive (ACTIVE): campaign.csv");
  });
});
