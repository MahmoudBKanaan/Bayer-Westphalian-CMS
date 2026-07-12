import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  createSegment,
  deleteSegment,
  emptySegmentForm,
  listSegments,
  previewSegment,
  segmentToForm,
  updateSegment,
  type SegmentCriteriaPayload,
  type SegmentFormPayload,
  type SegmentPreviewView,
  type SegmentSearchFilters,
  type SegmentView,
  type SegmentVisibility,
} from "@/api/segments";
import { SegmentCriteriaBuilder } from "@/components/SegmentCriteriaBuilder";
import { SegmentInsightPanel } from "@/components/SegmentInsightPanel";
import { SegmentPreviewResults } from "@/components/SegmentPreviewResults";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";
import { countCriteriaRows } from "@/features/segments/criteriaFields";
import {
  SEGMENT_CREATE_FORM_ARIA_LABEL,
  SEGMENT_CREATE_SECTION_HEADING,
  SEGMENT_CREATE_SECTION_HINT,
  SEGMENT_CREATE_SUBMIT_LABEL,
  SEGMENT_CREATED_NOTICE,
  SEGMENT_LIST_TABLE_ARIA_LABEL,
  SEGMENT_VISIBILITIES,
  hasSegmentFormErrors,
  validateSegmentForm,
  type SegmentFormErrors,
} from "@/features/segments/segmentCreationFlow";

const VISIBILITIES = SEGMENT_VISIBILITIES;
const VISIBILITY_FILTERS: Array<SegmentVisibility | "ALL"> = ["ALL", ...VISIBILITIES];

const emptySearchFilters: SegmentSearchFilters = {
  term: "",
  visibility: "ALL",
};

export function SegmentsPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const [selectedSegmentId, setSelectedSegmentId] = useState("");
  const [createForm, setCreateForm] = useState<SegmentFormPayload>(emptySegmentForm());
  const [editForm, setEditForm] = useState<SegmentFormPayload | null>(null);
  const [draftFilters, setDraftFilters] = useState<SegmentSearchFilters>(emptySearchFilters);
  const [appliedFilters, setAppliedFilters] = useState<SegmentSearchFilters>(emptySearchFilters);
  const [notice, setNotice] = useState("");
  const [preview, setPreview] = useState<SegmentPreviewView | null>(null);
  const [previewSourceLabel, setPreviewSourceLabel] = useState("");

  const canManageSegments = permissions.canManageSegments();
  const canCreateSegments = permissions.canCreateSegments();
  const canReadSegments = permissions.canReadSegments();
  const canPreviewSegments = permissions.canPreviewSegments();
  /** BI Analyst (and similar): read + preview without create/edit. */
  const isBiAnalystInsightView =
    canReadSegments && canPreviewSegments && !canManageSegments && !canCreateSegments;

  const segmentsQuery = useQuery({
    queryKey: ["segments", appliedFilters],
    queryFn: () => listSegments(appliedFilters),
    enabled: canReadSegments,
  });

  const segments = useMemo(() => segmentsQuery.data ?? [], [segmentsQuery.data]);
  const selectedSegment = useMemo(() => {
    if (selectedSegmentId === "") {
      return segments[0];
    }
    return segments.find((candidate) => candidate.id === selectedSegmentId);
  }, [segments, selectedSegmentId]);

  const selectedSegmentForm =
    selectedSegment == null ? null : editForm == null ? segmentToForm(selectedSegment) : editForm;

  const refreshSegments = async () => {
    await queryClient.invalidateQueries({ queryKey: ["segments"] });
  };

  const createMutation = useMutation({
    mutationFn: createSegment,
    onSuccess: async (created) => {
      setCreateForm(emptySegmentForm());
      setSelectedSegmentId(created.id);
      setEditForm(segmentToForm(created));
      setNotice(SEGMENT_CREATED_NOTICE);
      await refreshSegments();
    },
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      updateSegment(selectedSegment?.id ?? "", selectedSegmentForm ?? emptySegmentForm()),
    onSuccess: async (updated) => {
      setSelectedSegmentId(updated.id);
      setEditForm(segmentToForm(updated));
      setNotice("Segment updated.");
      await refreshSegments();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteSegment(selectedSegment?.id ?? ""),
    onSuccess: async () => {
      setSelectedSegmentId("");
      setEditForm(null);
      setPreview(null);
      setPreviewSourceLabel("");
      setNotice("Segment deleted.");
      await refreshSegments();
    },
  });

  const previewMutation = useMutation({
    mutationFn: (input: { criteria: SegmentCriteriaPayload[]; sourceLabel: string }) =>
      previewSegment(input.criteria),
    onSuccess: (result, variables) => {
      setPreview(result);
      setPreviewSourceLabel(variables.sourceLabel);
      setNotice("Audience preview loaded.");
    },
  });

  const runPreview = (criteria: SegmentCriteriaPayload[], sourceLabel: string) => {
    setNotice("");
    setPreviewSourceLabel(sourceLabel);
    previewMutation.mutate({
      criteria: completeCriteria(criteria),
      sourceLabel,
    });
  };

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending ||
    previewMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(
      segmentsQuery.error,
      createMutation.error,
      updateMutation.error,
      deleteMutation.error,
      previewMutation.error,
    ) ||
    generalErrorMessage(
      segmentsQuery.error,
      createMutation.error,
      updateMutation.error,
      deleteMutation.error,
      previewMutation.error,
    );
  const previewErrorMessage =
    authorizationErrorMessage(previewMutation.error) || generalErrorMessage(previewMutation.error);

  if (!canReadSegments) {
    return (
      <section className="page-stack">
        <section className="panel">
          <div className="section-heading">
            <h2>Segmentation</h2>
            <span>Access restricted</span>
          </div>
          <p className="form-error" role="alert">
            You do not have permission to view segments.
          </p>
        </section>
      </section>
    );
  }

  return (
    <section className="page-stack">
      <section className="panel">
        <div className="section-heading">
          <h2>Segmentation</h2>
          <span>
            {isBiAnalystInsightView
              ? "Read-only insight view for audience size, patterns, and exclusions"
              : "Reusable audience criteria with eligibility-aware preview"}
          </span>
        </div>
        <p className="table-state">
          {isBiAnalystInsightView
            ? "Analyze saved campaign audiences without changing definitions. Inspect criteria patterns, visibility mix, and eligibility outcomes for BI reporting."
            : "Define target groups for campaigns using demographics, products, consent, payment history, and product expiration. Audience previews apply eligibility rules (do-not-contact, opt-out, consent, monthly contact limits)."}
        </p>
        {!isBiAnalystInsightView ? (
          <div className="split-grid" aria-label="Segmentation capabilities">
            <div className="criteria-box">Filters: age, location, customer type, products</div>
            <div className="criteria-box">
              Filters: payment history, behavior, consent, expiration
            </div>
            <div className="criteria-box">Logic: AND / OR multi-criteria combinations</div>
            <div className="criteria-box">
              Preview: eligible, excluded, and exclusion reason summary
            </div>
          </div>
        ) : null}
      </section>

      {isBiAnalystInsightView ? (
        <SegmentInsightPanel
          segments={segments}
          selectedSegment={selectedSegment}
          preview={preview}
          isLoading={segmentsQuery.isLoading}
          previewPending={previewMutation.isPending}
          onAnalyzeSegment={(segment) =>
            runPreview(segmentToForm(segment).criteria, `Insight analysis: ${segment.name}`)
          }
        />
      ) : null}

      <section className="panel" aria-labelledby="segment-search-heading">
        <div className="section-heading">
          <h2 id="segment-search-heading">Saved segments</h2>
          <span>
            {segmentsQuery.isLoading
              ? "Loading…"
              : `${segments.length} segment${segments.length === 1 ? "" : "s"}`}
          </span>
        </div>

        <form
          className="toolbar-row"
          aria-label="Segment search filters"
          onSubmit={(event) => {
            event.preventDefault();
            setSelectedSegmentId("");
            setEditForm(null);
            setAppliedFilters({
              term: draftFilters.term.trim(),
              visibility: draftFilters.visibility,
            });
          }}
        >
          <label>
            Search segments
            <input
              type="search"
              aria-label="Search segments"
              value={draftFilters.term}
              onChange={(event) =>
                setDraftFilters((current) => ({ ...current, term: event.target.value }))
              }
              placeholder="Name or description"
            />
          </label>
          <label>
            Visibility filter
            <select
              aria-label="Visibility filter"
              value={draftFilters.visibility}
              onChange={(event) =>
                setDraftFilters((current) => ({
                  ...current,
                  visibility: event.target.value as SegmentSearchFilters["visibility"],
                }))
              }
            >
              {VISIBILITY_FILTERS.map((visibility) => (
                <option key={visibility} value={visibility}>
                  {visibility === "ALL" ? "All visibilities" : formatVisibility(visibility)}
                </option>
              ))}
            </select>
          </label>
          <div className="button-row">
            <button type="submit">Apply filters</button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => {
                setDraftFilters(emptySearchFilters);
                setAppliedFilters(emptySearchFilters);
                setSelectedSegmentId("");
                setEditForm(null);
              }}
            >
              Reset filters
            </button>
          </div>
        </form>

        {notice ? (
          <p className="form-success" role="status">
            {notice}
          </p>
        ) : null}
        {errorMessage ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}

        {segmentsQuery.isLoading ? (
          <p className="table-state">Loading segments…</p>
        ) : segments.length === 0 ? (
          <p className="table-state">No saved segments match the current filters.</p>
        ) : (
          <div className="table-scroll">
            <table aria-label={SEGMENT_LIST_TABLE_ARIA_LABEL}>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Visibility</th>
                  <th scope="col">Owner</th>
                  <th scope="col">Criteria</th>
                  <th scope="col">Updated</th>
                  <th scope="col">Action</th>
                </tr>
              </thead>
              <tbody>
                {segments.map((segment) => {
                  const isSelected = selectedSegment?.id === segment.id;
                  return (
                    <tr key={segment.id} className={isSelected ? "selected-table-row" : undefined}>
                      <th scope="row">
                        <span className="table-primary-text">{segment.name}</span>
                        <span className="table-secondary-text">
                          {segment.description || "No description"}
                        </span>
                      </th>
                      <td>
                        <StatusBadge value={formatVisibility(segment.visibility)} />
                      </td>
                      <td>{segment.ownerFullName ?? "—"}</td>
                      <td>{segment.criteria.length}</td>
                      <td>{formatTimestamp(segment.updatedAt)}</td>
                      <td>
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => {
                            setSelectedSegmentId(segment.id);
                            setEditForm(null);
                            setNotice("");
                          }}
                        >
                          Select
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {canCreateSegments || canManageSegments ? (
        <div className="split-grid user-management-grid">
          {canCreateSegments ? (
            <section className="panel" aria-labelledby="create-segment-heading">
              <div className="section-heading">
                <h2 id="create-segment-heading">{SEGMENT_CREATE_SECTION_HEADING}</h2>
                <span>{SEGMENT_CREATE_SECTION_HINT}</span>
              </div>
              <SegmentForm
                formId="create-segment-form"
                formAriaLabel={SEGMENT_CREATE_FORM_ARIA_LABEL}
                submitLabel={SEGMENT_CREATE_SUBMIT_LABEL}
                values={createForm}
                disabled={isBusy}
                canPreview={canPreviewSegments}
                previewPending={previewMutation.isPending}
                onChange={setCreateForm}
                onSubmit={() => {
                  setNotice("");
                  createMutation.mutate(createForm);
                }}
                onPreview={() => runPreview(createForm.criteria, "Create form draft")}
              />
            </section>
          ) : null}

          {canManageSegments ? (
            <section className="panel" aria-labelledby="edit-segment-heading">
              <div className="section-heading">
                <h2 id="edit-segment-heading">Edit segment</h2>
                <span>
                  {selectedSegment
                    ? `Selected: ${selectedSegment.name}`
                    : "Select a segment from the table"}
                </span>
              </div>
              {selectedSegmentForm == null || selectedSegment == null ? (
                <p className="table-state">Select a segment to edit its definition.</p>
              ) : (
                <>
                  <SegmentForm
                    formId="edit-segment-form"
                    submitLabel="Save changes"
                    values={selectedSegmentForm}
                    disabled={isBusy}
                    canPreview={canPreviewSegments}
                    previewPending={previewMutation.isPending}
                    onChange={setEditForm}
                    onSubmit={() => {
                      setNotice("");
                      updateMutation.mutate();
                    }}
                    onPreview={() =>
                      runPreview(selectedSegmentForm.criteria, `Draft: ${selectedSegment.name}`)
                    }
                  />
                  <div className="button-row">
                    <button
                      type="button"
                      className="danger-button"
                      disabled={isBusy}
                      onClick={() => {
                        if (
                          window.confirm(
                            `Delete segment “${selectedSegment.name}”? This cannot be undone.`,
                          )
                        ) {
                          setNotice("");
                          deleteMutation.mutate();
                        }
                      }}
                    >
                      Delete segment
                    </button>
                  </div>
                </>
              )}
            </section>
          ) : null}
        </div>
      ) : isBiAnalystInsightView ? null : (
        <p className="table-state">
          Segment create and edit actions are hidden for your role. You can still review saved
          audiences below.
        </p>
      )}

      <section className="panel" aria-labelledby="segment-detail-heading">
        <div className="section-heading">
          <h2 id="segment-detail-heading">Segment details</h2>
          <span>
            {canPreviewSegments
              ? "Criteria shown for eligibility-aware campaign targeting"
              : "Read-only segment definition"}
          </span>
        </div>
        {selectedSegment == null ? (
          <p className="table-state">Select a segment to view its criteria and metadata.</p>
        ) : (
          <SegmentDetails
            segment={selectedSegment}
            canPreview={canPreviewSegments}
            previewPending={previewMutation.isPending}
            onPreview={() =>
              runPreview(
                segmentToForm(selectedSegment).criteria,
                `Saved segment: ${selectedSegment.name}`,
              )
            }
          />
        )}
      </section>

      {canPreviewSegments ? (
        <SegmentPreviewResults
          preview={preview}
          sourceLabel={previewSourceLabel}
          isLoading={previewMutation.isPending}
          errorMessage={previewErrorMessage}
        />
      ) : null}
    </section>
  );
}

type SegmentFormProps = {
  formId: string;
  formAriaLabel?: string;
  submitLabel: string;
  values: SegmentFormPayload;
  disabled: boolean;
  canPreview?: boolean;
  previewPending?: boolean;
  onChange: (values: SegmentFormPayload) => void;
  onSubmit: () => void;
  onPreview?: () => void;
};

function SegmentForm({
  formId,
  formAriaLabel,
  submitLabel,
  values,
  disabled,
  canPreview = false,
  previewPending = false,
  onChange,
  onSubmit,
  onPreview,
}: SegmentFormProps) {
  const [errors, setErrors] = useState<SegmentFormErrors>({});

  function updateField(next: SegmentFormPayload) {
    setErrors({});
    onChange(next);
  }

  return (
    <form
      id={formId}
      className="form-grid"
      noValidate
      aria-label={formAriaLabel}
      onSubmit={(event) => {
        event.preventDefault();
        const nextErrors = validateSegmentForm(values);
        setErrors(nextErrors);
        if (hasSegmentFormErrors(nextErrors)) {
          return;
        }
        onSubmit();
      }}
    >
      <label>
        Name
        <input
          required
          maxLength={255}
          aria-label="Name"
          value={values.name}
          disabled={disabled}
          aria-invalid={Boolean(errors.name)}
          onChange={(event) => updateField({ ...values, name: event.target.value })}
        />
        <SegmentFieldError message={errors.name} />
      </label>
      <label>
        Description
        <textarea
          rows={3}
          aria-label="Description"
          value={values.description}
          disabled={disabled}
          onChange={(event) => updateField({ ...values, description: event.target.value })}
        />
      </label>
      <label>
        Visibility
        <select
          aria-label="Visibility"
          value={values.visibility}
          disabled={disabled}
          aria-invalid={Boolean(errors.visibility)}
          onChange={(event) =>
            updateField({
              ...values,
              visibility: event.target.value as SegmentVisibility,
            })
          }
        >
          {VISIBILITIES.map((visibility) => (
            <option key={visibility} value={visibility}>
              {formatVisibility(visibility)}
            </option>
          ))}
        </select>
        <SegmentFieldError message={errors.visibility} />
      </label>

      <SegmentCriteriaBuilder
        idPrefix={formId}
        criteria={values.criteria}
        disabled={disabled}
        onChange={(criteria) => updateField({ ...values, criteria })}
      />
      <SegmentFieldError message={errors.criteria} />

      <p className="table-state">
        {countCriteriaRows(values.criteria) === 0
          ? "No complete criteria rows yet — saving matches all active profiles until filters are added."
          : `${countCriteriaRows(values.criteria)} complete criterion row(s) will be saved with this segment.`}
      </p>
      <div className="button-row">
        <button type="submit" disabled={disabled}>
          {submitLabel}
        </button>
        {canPreview && onPreview ? (
          <button
            type="button"
            className="secondary-button"
            disabled={disabled || previewPending}
            onClick={onPreview}
          >
            {previewPending ? "Previewing…" : "Preview audience"}
          </button>
        ) : null}
      </div>
    </form>
  );
}

function SegmentFieldError({ message }: { message?: string }) {
  return message == null ? null : (
    <span className="field-error" role="alert">
      {message}
    </span>
  );
}

function SegmentDetails({
  segment,
  canPreview,
  previewPending = false,
  onPreview,
}: {
  segment: SegmentView;
  canPreview: boolean;
  previewPending?: boolean;
  onPreview?: () => void;
}) {
  return (
    <div className="page-stack">
      <dl className="detail-list">
        <div>
          <dt>Segment UUID</dt>
          <dd>
            <code>{segment.id}</code>
          </dd>
        </div>
        <div>
          <dt>Name</dt>
          <dd>{segment.name}</dd>
        </div>
        <div>
          <dt>Description</dt>
          <dd>{segment.description || "—"}</dd>
        </div>
        <div>
          <dt>Visibility</dt>
          <dd>
            <StatusBadge value={formatVisibility(segment.visibility)} />
          </dd>
        </div>
        <div>
          <dt>Owner</dt>
          <dd>{segment.ownerFullName ?? "—"}</dd>
        </div>
        <div>
          <dt>Created</dt>
          <dd>{formatTimestamp(segment.createdAt)}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatTimestamp(segment.updatedAt)}</dd>
        </div>
      </dl>

      <div className="section-heading">
        <h3>Criteria ({segment.criteria.length})</h3>
        {canPreview ? (
          <span>Run preview to estimate eligible audience size</span>
        ) : (
          <span>Preview requires campaign or BI analyst access</span>
        )}
      </div>

      {segment.criteria.length === 0 ? (
        <p className="table-state">
          No criteria defined. This segment matches all active customer profiles until filters are
          added.
        </p>
      ) : (
        <div className="table-scroll">
          <table aria-label="Segment criteria table">
            <thead>
              <tr>
                <th scope="col">Field</th>
                <th scope="col">Operator</th>
                <th scope="col">Value</th>
                <th scope="col">Group</th>
                <th scope="col">Join</th>
              </tr>
            </thead>
            <tbody>
              {segment.criteria.map((criterion) => (
                <tr key={criterion.id}>
                  <td>
                    <code>{criterion.fieldName}</code>
                  </td>
                  <td>{criterion.operator}</td>
                  <td>{criterion.value}</td>
                  <td>{criterion.logicalGroup ?? "—"}</td>
                  <td>{criterion.joinOperator}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {canPreview && onPreview ? (
        <div className="button-row">
          <button
            type="button"
            className="secondary-button"
            disabled={previewPending}
            onClick={onPreview}
          >
            {previewPending ? "Previewing…" : "Preview this segment"}
          </button>
        </div>
      ) : null}
    </div>
  );
}

function completeCriteria(criteria: SegmentCriteriaPayload[]): SegmentCriteriaPayload[] {
  return criteria.filter(
    (criterion) => criterion.fieldName.trim() !== "" && criterion.value.trim() !== "",
  );
}

function formatVisibility(visibility: SegmentVisibility) {
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

function formatTimestamp(value: string | null) {
  if (value == null || value === "") {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function authorizationErrorMessage(...errors: unknown[]) {
  for (const error of errors) {
    if (isAuthorizationError(error)) {
      return "You are not authorized to perform this segmentation action.";
    }
  }
  return "";
}

function generalErrorMessage(...errors: unknown[]) {
  for (const error of errors) {
    if (error instanceof Error && error.message.trim() !== "") {
      return error.message;
    }
  }
  return "";
}
