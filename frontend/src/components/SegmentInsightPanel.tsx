import type { SegmentPreviewView, SegmentView } from "@/api/segments";
import { MetricCard } from "@/components/MetricCard";
import { StatusBadge } from "@/components/StatusBadge";
import {
  buildCatalogInsights,
  buildSelectedSegmentInsight,
} from "@/features/segments/segmentInsights";
import { formatNumber, formatPercent } from "@/utils/format";

export type SegmentInsightPanelProps = {
  segments: SegmentView[];
  selectedSegment?: SegmentView;
  preview: SegmentPreviewView | null;
  isLoading?: boolean;
  previewPending?: boolean;
  onAnalyzeSegment?: (segment: SegmentView) => void;
};

/**
 * Read-only BI Analyst segmentation insight view (KB): catalog patterns, selected-segment
 * structure, and eligibility preview context without create/edit controls.
 */
export function SegmentInsightPanel({
  segments,
  selectedSegment,
  preview,
  isLoading = false,
  previewPending = false,
  onAnalyzeSegment,
}: SegmentInsightPanelProps) {
  const catalog = buildCatalogInsights(segments);
  const selected = buildSelectedSegmentInsight(selectedSegment);

  return (
    <section className="panel" aria-labelledby="segment-insight-heading">
      <div className="section-heading">
        <h2 id="segment-insight-heading">Segmentation insights</h2>
        <span>Read-only BI Analyst view — size, patterns, and exclusions</span>
      </div>
      <p className="table-state">
        Review saved audience definitions, criteria patterns, and eligibility-aware previews. Create
        and edit actions are reserved for Campaign Managers and Admins.
      </p>

      {isLoading ? (
        <p className="table-state">Loading segment catalog insights…</p>
      ) : (
        <>
          <div className="metric-grid" aria-label="Segment catalog metrics">
            <MetricCard
              label="Saved segments"
              value={formatNumber(catalog.totalSegments)}
              detail={`${formatNumber(catalog.ownerCount)} owner(s) in catalog`}
            />
            <MetricCard
              label="Avg criteria"
              value={formatNumber(catalog.averageCriteriaPerSegment)}
              detail={`${formatNumber(catalog.totalCriteria)} total filter rows`}
            />
            <MetricCard
              label="Multi-criteria"
              value={formatNumber(catalog.multiCriteriaCount)}
              detail="Segments with 2+ rules"
            />
            <MetricCard
              label="Open audiences"
              value={formatNumber(catalog.emptyCriteriaCount)}
              detail="Segments with no criteria"
            />
          </div>

          <div className="split-grid segment-insight-grid">
            <div>
              <div className="section-heading">
                <h3>Visibility mix</h3>
                <span>How shared the catalog is</span>
              </div>
              {catalog.totalSegments === 0 ? (
                <p className="table-state">No segments available for insight analysis.</p>
              ) : (
                <ul className="insight-stat-list" aria-label="Visibility mix">
                  {catalog.visibilityBreakdown.map((entry) => (
                    <li key={entry.visibility}>
                      <div className="insight-stat-row">
                        <StatusBadge value={formatVisibility(entry.visibility)} />
                        <strong>
                          {formatNumber(entry.count)} · {formatPercent(entry.share)}
                        </strong>
                      </div>
                      <div
                        className="exclusion-reason-bar"
                        role="meter"
                        aria-label={`${formatVisibility(entry.visibility)} share of catalog`}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={Math.round(entry.share)}
                      >
                        <div
                          className="exclusion-reason-bar-fill"
                          style={{ width: `${Math.min(100, Math.max(0, entry.share))}%` }}
                        />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div>
              <div className="section-heading">
                <h3>Most used fields</h3>
                <span>Criteria patterns across segments</span>
              </div>
              {catalog.topFields.length === 0 ? (
                <p className="table-state">No criteria fields recorded yet.</p>
              ) : (
                <ul className="insight-stat-list" aria-label="Most used segment fields">
                  {catalog.topFields.map((field) => (
                    <li key={field.fieldName}>
                      <div className="insight-stat-row">
                        <span>
                          <strong>{field.label}</strong>
                          <span className="table-secondary-text">
                            <code>{field.fieldName}</code>
                          </span>
                        </span>
                        <strong>
                          {formatNumber(field.count)} · {formatPercent(field.share)}
                        </strong>
                      </div>
                      <div
                        className="exclusion-reason-bar"
                        role="meter"
                        aria-label={`${field.label} share of criteria`}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={Math.round(field.share)}
                      >
                        <div
                          className="exclusion-reason-bar-fill"
                          style={{ width: `${Math.min(100, Math.max(0, field.share))}%` }}
                        />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div className="section-heading">
            <h3>Selected segment analysis</h3>
            <span>
              {selected ? selected.name : "Select a segment from the catalog to inspect structure"}
            </span>
          </div>

          {selected == null ? (
            <div className="state-panel" role="status">
              <strong>No segment selected</strong>
              <p>
                Choose a saved segment to review criteria structure and run an eligibility preview.
              </p>
            </div>
          ) : (
            <div className="insight-selected-card">
              <div className="insight-stat-row">
                <div>
                  <span className="eyebrow">Selected audience</span>
                  <strong className="insight-selected-name">{selected.name}</strong>
                </div>
                <StatusBadge value={formatVisibility(selected.visibility)} />
              </div>
              <dl className="detail-list">
                <div>
                  <dt>Criteria count</dt>
                  <dd>{formatNumber(selected.criteriaCount)}</dd>
                </div>
                <div>
                  <dt>Fields</dt>
                  <dd>
                    {selected.fieldLabels.length === 0 ? "None" : selected.fieldLabels.join(", ")}
                  </dd>
                </div>
                <div>
                  <dt>Join logic</dt>
                  <dd>
                    {selected.criteriaCount <= 1
                      ? "Single rule"
                      : selected.hasOrLogic
                        ? "Includes OR"
                        : "AND only"}
                  </dd>
                </div>
                <div>
                  <dt>Logical groups</dt>
                  <dd>{selected.groups.length === 0 ? "None" : selected.groups.join(", ")}</dd>
                </div>
              </dl>
              <ul className="insight-notes" aria-label="Selected segment insight notes">
                {selected.insightNotes.map((note) => (
                  <li key={note}>{note}</li>
                ))}
              </ul>
              {onAnalyzeSegment && selectedSegment ? (
                <div className="button-row">
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={previewPending}
                    onClick={() => onAnalyzeSegment(selectedSegment)}
                  >
                    {previewPending ? "Analyzing audience…" : "Analyze audience eligibility"}
                  </button>
                </div>
              ) : null}
            </div>
          )}

          {preview != null ? (
            <div className="insight-preview-callout" aria-label="Latest eligibility insight">
              <span className="eyebrow">Latest eligibility snapshot</span>
              <p>
                Total {formatNumber(preview.totalAudienceCount)} · Eligible{" "}
                {formatNumber(preview.eligibleCount)} · Excluded{" "}
                {formatNumber(preview.excludedCount)}
                {preview.totalAudienceCount > 0
                  ? ` (${formatPercent(
                      (preview.eligibleCount / preview.totalAudienceCount) * 100,
                    )} eligibility rate)`
                  : ""}
              </p>
              <p className="table-secondary-text">
                Full eligible customer list and exclusion reason cards appear in the audience
                preview panel below.
              </p>
            </div>
          ) : null}
        </>
      )}
    </section>
  );
}

function formatVisibility(visibility: SegmentView["visibility"]) {
  switch (visibility) {
    case "PRIVATE":
      return "Private";
    case "TEAM":
      return "Team";
    case "GLOBAL":
      return "Global";
    default:
      return visibility;
  }
}
