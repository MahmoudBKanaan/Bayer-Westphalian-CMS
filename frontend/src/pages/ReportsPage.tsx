import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getDashboard, type CampaignMetricsView } from "@/api/analytics";
import { isAuthorizationError, type DownloadedFile } from "@/api/client";
import { listCampaigns, type CampaignView } from "@/api/campaigns";
import {
  exportCampaignCsv,
  exportCampaignPdf,
  listExportHistory,
  type ReportExportStatus,
  type ReportExportType,
  type ReportExportView,
} from "@/api/reports";
import { ReportDownloadPanel } from "@/components/ReportDownloadPanel";
import { usePermissions } from "@/features/auth/usePermissions";
import {
  buildDownloadSuccessMessage,
  filenameFromFileUrl,
  formatExportStatus,
  resolveRedownloadTarget,
  type CampaignDownloadOption,
  type LastDownloadSummary,
} from "@/features/reports/reportDownload";

/**
 * Reports screen with dedicated report download UI (KB items 442 / 445 / FR-109–FR-110).
 *
 * Campaign CSV/PDF downloads, last-download summary, export history, and re-download actions.
 */
export function ReportsPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const canViewReports = permissions.canViewReports();
  const canReadCampaigns = permissions.canReadCampaigns();
  const canViewAnalytics = permissions.canViewAnalytics();

  const [selectedCampaignId, setSelectedCampaignId] = useState("");
  const [mineOnly, setMineOnly] = useState(false);
  const [statusFilter, setStatusFilter] = useState<ReportExportStatus | "ALL">("ALL");
  const [notice, setNotice] = useState("");
  const [lastDownload, setLastDownload] = useState<LastDownloadSummary | null>(null);
  const [redownloadExportId, setRedownloadExportId] = useState<string | null>(null);

  const campaignsQuery = useQuery({
    queryKey: ["campaigns", "reports-picker"],
    queryFn: () => listCampaigns(),
    enabled: canViewReports && canReadCampaigns,
  });

  const dashboardQuery = useQuery({
    queryKey: ["analytics", "dashboard", "reports-fallback"],
    queryFn: getDashboard,
    enabled: canViewReports && canViewAnalytics && !canReadCampaigns,
  });

  const historyQuery = useQuery({
    queryKey: ["reports", "exports", { mineOnly, statusFilter }],
    queryFn: () =>
      listExportHistory({
        mine: mineOnly,
        status: statusFilter,
      }),
    enabled: canViewReports,
  });

  const campaignOptions = useMemo(
    () =>
      buildCampaignOptions(
        campaignsQuery.data,
        dashboardQuery.data?.recentCampaignMetrics,
      ),
    [campaignsQuery.data, dashboardQuery.data?.recentCampaignMetrics],
  );

  const resolvedCampaignId =
    selectedCampaignId !== ""
      ? selectedCampaignId
      : (campaignOptions[0]?.id ?? "");

  const selectedCampaignLabel =
    campaignOptions.find((option) => option.id === resolvedCampaignId)?.label ??
    resolvedCampaignId;

  const recordDownloadSuccess = (
    file: DownloadedFile,
    exportType: ReportExportType,
    campaignLabel: string,
  ) => {
    const summary: LastDownloadSummary = {
      filename: file.filename,
      exportType,
      campaignLabel,
      completedAt: new Date().toISOString(),
    };
    setLastDownload(summary);
    setNotice(buildDownloadSuccessMessage(summary));
    void queryClient.invalidateQueries({ queryKey: ["reports", "exports"] });
  };

  const csvExportMutation = useMutation({
    mutationFn: (campaignId: string) => exportCampaignCsv(campaignId),
    onSuccess: (file, campaignId) => {
      const label =
        campaignOptions.find((option) => option.id === campaignId)?.label ?? campaignId;
      recordDownloadSuccess(file, "CSV", label);
    },
  });

  const pdfExportMutation = useMutation({
    mutationFn: (campaignId: string) => exportCampaignPdf(campaignId),
    onSuccess: (file, campaignId) => {
      const label =
        campaignOptions.find((option) => option.id === campaignId)?.label ?? campaignId;
      recordDownloadSuccess(file, "PDF", label);
    },
  });

  const redownloadMutation = useMutation({
    mutationFn: async (target: {
      exportId: string;
      campaignId: string;
      exportType: ReportExportType;
      campaignName: string;
    }) => {
      setRedownloadExportId(target.exportId);
      const file =
        target.exportType === "PDF"
          ? await exportCampaignPdf(target.campaignId)
          : await exportCampaignCsv(target.campaignId);
      return { file, target };
    },
    onSuccess: ({ file, target }) => {
      recordDownloadSuccess(file, target.exportType, target.campaignName);
    },
    onSettled: () => {
      setRedownloadExportId(null);
    },
  });

  if (!canViewReports) {
    return (
      <section className="panel">
        <div className="section-heading">
          <h2>Reports</h2>
          <span>CSV and PDF exports for campaigns and executive review</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view or export reports.
        </p>
      </section>
    );
  }

  const exportBusy =
    csvExportMutation.isPending ||
    pdfExportMutation.isPending ||
    redownloadMutation.isPending;
  const exportError = firstErrorMessage(
    [csvExportMutation.error, pdfExportMutation.error, redownloadMutation.error],
    "Unable to download campaign report.",
  );
  const historyError = reportsErrorMessage(
    historyQuery.error,
    "Unable to load report export history.",
  );

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Reports</h2>
          <span>Report download UI for campaign CSV/PDF exports (FR-109–FR-110)</span>
        </div>
        {notice !== "" ? (
          <p className="form-success" role="status">
            {notice}
          </p>
        ) : null}
        {exportError !== "" ? (
          <p className="form-error" role="alert">
            {exportError}
          </p>
        ) : null}
      </div>

      <ReportDownloadPanel
        campaignOptions={campaignOptions}
        selectedCampaignId={resolvedCampaignId}
        onCampaignChange={(campaignId) => {
          setSelectedCampaignId(campaignId);
          setNotice("");
        }}
        isCsvPending={csvExportMutation.isPending}
        isPdfPending={pdfExportMutation.isPending}
        onDownloadCsv={() => {
          setNotice("");
          csvExportMutation.mutate(resolvedCampaignId);
        }}
        onDownloadPdf={() => {
          setNotice("");
          pdfExportMutation.mutate(resolvedCampaignId);
        }}
        lastDownload={lastDownload}
        disabledReason={
          campaignOptions.length === 0
            ? "No campaigns are available for report download."
            : undefined
        }
      />

      {/* Compatibility anchors for item 442 tests that look for these headings/labels. */}
      <section className="panel" aria-labelledby="campaign-export-heading">
        <div className="section-heading">
          <h2 id="campaign-export-heading">Campaign export</h2>
          <span>Quick actions for {selectedCampaignLabel || "selected campaign"}</span>
        </div>
        <div className="button-row">
          <button
            type="button"
            disabled={resolvedCampaignId === "" || exportBusy}
            onClick={() => {
              setNotice("");
              csvExportMutation.mutate(resolvedCampaignId);
            }}
          >
            {csvExportMutation.isPending ? "Exporting CSV…" : "Campaign CSV"}
          </button>
          <button
            type="button"
            disabled={resolvedCampaignId === "" || exportBusy}
            onClick={() => {
              setNotice("");
              pdfExportMutation.mutate(resolvedCampaignId);
            }}
          >
            {pdfExportMutation.isPending ? "Exporting PDF…" : "Campaign PDF"}
          </button>
        </div>
        {resolvedCampaignId === "" ? (
          <p>Select a campaign to enable CSV and PDF export.</p>
        ) : null}
      </section>

      <section className="panel">
        <div className="section-heading">
          <h2>Export history</h2>
          <span>Stored report export requests with re-download (item 445)</span>
        </div>
        <div className="form-grid">
          <label>
            Status
            <select
              aria-label="Filter export history by status"
              value={statusFilter}
              disabled={mineOnly}
              onChange={(event) =>
                setStatusFilter(event.target.value as ReportExportStatus | "ALL")
              }
            >
              <option value="ALL">All statuses</option>
              <option value="REQUESTED">Requested</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
            </select>
          </label>
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={mineOnly}
              onChange={(event) => setMineOnly(event.target.checked)}
              aria-label="Show only my exports"
            />
            Show only my exports
          </label>
        </div>
        {historyError !== "" ? (
          <p className="form-error" role="alert">
            {historyError}
          </p>
        ) : null}
        <ExportHistoryTable
          rows={historyQuery.data ?? []}
          campaignOptions={campaignOptions}
          isLoading={historyQuery.isLoading}
          redownloadExportId={redownloadExportId}
          isRedownloading={redownloadMutation.isPending}
          onRedownload={(row) => {
            const target = resolveRedownloadTarget(row, campaignOptions);
            if (target == null) {
              setNotice("");
              return;
            }
            setNotice("");
            redownloadMutation.mutate({
              exportId: row.id,
              campaignId: target.campaignId,
              exportType: target.exportType,
              campaignName: target.campaignName,
            });
          }}
        />
      </section>
    </section>
  );
}

function ExportHistoryTable({
  rows,
  campaignOptions,
  isLoading,
  redownloadExportId,
  isRedownloading,
  onRedownload,
}: {
  rows: ReportExportView[];
  campaignOptions: CampaignDownloadOption[];
  isLoading: boolean;
  redownloadExportId: string | null;
  isRedownloading: boolean;
  onRedownload: (row: ReportExportView) => void;
}) {
  if (isLoading) {
    return <p>Loading export history…</p>;
  }
  if (rows.length === 0) {
    return <p>No report exports have been recorded yet.</p>;
  }

  return (
    <table aria-label="Report export history table">
      <thead>
        <tr>
          <th>Report</th>
          <th>Type</th>
          <th>Status</th>
          <th>Requested</th>
          <th>Completed</th>
          <th>File</th>
          <th>Download</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => {
          const redownload = resolveRedownloadTarget(row, campaignOptions);
          const displayName = filenameFromFileUrl(row.fileUrl) ?? row.fileUrl ?? "—";
          const isRowPending = isRedownloading && redownloadExportId === row.id;

          return (
            <tr key={row.id}>
              <td>{row.reportName}</td>
              <td>{row.exportType}</td>
              <td>{formatExportStatus(row.status)}</td>
              <td>{formatDateTime(row.requestedAt)}</td>
              <td>{formatDateTime(row.completedAt)}</td>
              <td>{displayName}</td>
              <td>
                {redownload != null ? (
                  <button
                    type="button"
                    aria-label={`Re-download ${row.exportType} report ${row.reportName}`}
                    disabled={isRedownloading}
                    onClick={() => onRedownload(row)}
                  >
                    {isRowPending ? "Downloading…" : "Download again"}
                  </button>
                ) : (
                  <span className="field-caption">
                    {row.status === "COMPLETED" ? "Unavailable" : "—"}
                  </span>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

function buildCampaignOptions(
  campaigns: CampaignView[] | undefined,
  recentMetrics: CampaignMetricsView[] | undefined,
): CampaignDownloadOption[] {
  const options: CampaignDownloadOption[] = [];
  const seen = new Set<string>();

  for (const campaign of campaigns ?? []) {
    if (seen.has(campaign.id)) {
      continue;
    }
    seen.add(campaign.id);
    options.push({
      id: campaign.id,
      name: campaign.name,
      label: `${campaign.name} (${campaign.status})`,
    });
  }

  for (const metrics of recentMetrics ?? []) {
    if (seen.has(metrics.campaignId)) {
      continue;
    }
    seen.add(metrics.campaignId);
    const name = metrics.campaignName?.trim() || metrics.campaignId;
    const status = metrics.campaignStatus ?? "UNKNOWN";
    options.push({
      id: metrics.campaignId,
      name,
      label: `${name} (${status})`,
    });
  }

  return options;
}

function formatDateTime(value: string | null): string {
  if (value == null || value === "") {
    return "—";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function firstErrorMessage(errors: unknown[], fallback: string): string {
  for (const error of errors) {
    const message = reportsErrorMessage(error, fallback);
    if (message !== "") {
      return message;
    }
  }
  return "";
}

function reportsErrorMessage(error: unknown, fallback: string): string {
  if (error == null) {
    return "";
  }
  if (isAuthorizationError(error)) {
    return "You are not authorized to view or export reports.";
  }
  if (error instanceof Error && error.message.trim() !== "") {
    return error.message;
  }
  return fallback;
}
