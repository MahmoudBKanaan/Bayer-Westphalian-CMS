import {
  downloadButtonLabel,
  type CampaignDownloadOption,
  type LastDownloadSummary,
} from "@/features/reports/reportDownload";

export type ReportDownloadPanelProps = {
  campaignOptions: CampaignDownloadOption[];
  selectedCampaignId: string;
  onCampaignChange: (campaignId: string) => void;
  isCsvPending: boolean;
  isPdfPending: boolean;
  onDownloadCsv: () => void;
  onDownloadPdf: () => void;
  lastDownload: LastDownloadSummary | null;
  disabledReason?: string;
};

/**
 * Dedicated campaign report download UI (KB item 445 / FR-109–FR-110).
 *
 * Campaign selection, explicit CSV/PDF download actions, and last-download summary.
 */
export function ReportDownloadPanel({
  campaignOptions,
  selectedCampaignId,
  onCampaignChange,
  isCsvPending,
  isPdfPending,
  onDownloadCsv,
  onDownloadPdf,
  lastDownload,
  disabledReason,
}: ReportDownloadPanelProps) {
  const busy = isCsvPending || isPdfPending;
  const canDownload =
    selectedCampaignId !== "" && !busy && (disabledReason == null || disabledReason === "");

  return (
    <section className="panel" aria-labelledby="report-download-heading">
      <div className="section-heading">
        <h2 id="report-download-heading">Report download</h2>
        <span>Download campaign performance as CSV (FR-109) or PDF (FR-110)</span>
      </div>

      <div className="form-grid">
        <label>
          Campaign
          <select
            aria-label="Select campaign for report download"
            value={selectedCampaignId}
            onChange={(event) => onCampaignChange(event.target.value)}
            disabled={campaignOptions.length === 0 || busy}
          >
            {campaignOptions.length === 0 ? (
              <option value="">No campaigns available</option>
            ) : (
              campaignOptions.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label}
                </option>
              ))
            )}
          </select>
        </label>
      </div>

      <div className="button-row" role="group" aria-label="Campaign report download actions">
        <button
          type="button"
          aria-label="Download campaign CSV report"
          disabled={!canDownload}
          onClick={onDownloadCsv}
        >
          {downloadButtonLabel("CSV", isCsvPending)}
        </button>
        <button
          type="button"
          aria-label="Download campaign PDF report"
          disabled={!canDownload}
          onClick={onDownloadPdf}
        >
          {downloadButtonLabel("PDF", isPdfPending)}
        </button>
      </div>

      {disabledReason != null && disabledReason !== "" ? (
        <p className="field-caption" role="status">
          {disabledReason}
        </p>
      ) : null}

      {selectedCampaignId === "" ? (
        <p className="field-caption">Select a campaign to enable report download.</p>
      ) : (
        <p className="field-caption">
          Downloads attach the report file in the browser and store a row in export history.
        </p>
      )}

      {lastDownload != null ? (
        <div className="download-summary" role="status" aria-label="Last report download summary">
          <strong>Last download</strong>
          <dl className="detail-list">
            <div>
              <dt>Campaign</dt>
              <dd>{lastDownload.campaignLabel}</dd>
            </div>
            <div>
              <dt>Type</dt>
              <dd>{lastDownload.exportType}</dd>
            </div>
            <div>
              <dt>File</dt>
              <dd>{lastDownload.filename}</dd>
            </div>
            <div>
              <dt>Completed</dt>
              <dd>{formatCompletedAt(lastDownload.completedAt)}</dd>
            </div>
          </dl>
        </div>
      ) : null}
    </section>
  );
}

/**
 * Compact download actions for embedding on campaign detail screens (KB item 445).
 */
export type CampaignReportDownloadActionsProps = {
  campaignId: string;
  campaignName: string;
  canDownload: boolean;
  isCsvPending: boolean;
  isPdfPending: boolean;
  onDownloadCsv: () => void;
  onDownloadPdf: () => void;
  notice?: string;
  error?: string;
};

export function CampaignReportDownloadActions({
  campaignId,
  campaignName,
  canDownload,
  isCsvPending,
  isPdfPending,
  onDownloadCsv,
  onDownloadPdf,
  notice,
  error,
}: CampaignReportDownloadActionsProps) {
  const busy = isCsvPending || isPdfPending;

  return (
    <div className="report-download-actions" aria-label={`Report downloads for ${campaignName}`}>
      <div className="section-heading">
        <h3>Report download</h3>
        <span>CSV (FR-109) and PDF (FR-110)</span>
      </div>
      <div className="button-row">
        <button
          type="button"
          aria-label={`Download CSV report for ${campaignName}`}
          disabled={!canDownload || busy || campaignId === ""}
          onClick={onDownloadCsv}
        >
          {downloadButtonLabel("CSV", isCsvPending)}
        </button>
        <button
          type="button"
          aria-label={`Download PDF report for ${campaignName}`}
          disabled={!canDownload || busy || campaignId === ""}
          onClick={onDownloadPdf}
        >
          {downloadButtonLabel("PDF", isPdfPending)}
        </button>
      </div>
      {notice != null && notice !== "" ? (
        <p className="form-success" role="status">
          {notice}
        </p>
      ) : null}
      {error != null && error !== "" ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}

function formatCompletedAt(value: string): string {
  if (value.trim() === "") {
    return "—";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
