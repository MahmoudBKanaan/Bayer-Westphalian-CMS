import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { isAuthorizationError } from "@/api/client";
import {
  approveCampaign,
  archiveCampaign,
  campaignChannels,
  campaignStatuses,
  campaignToForm,
  completeCampaign,
  createCampaign,
  emptyCampaignFilters,
  emptyCampaignForm,
  formatCampaignEnum,
  launchCampaign,
  listCampaigns,
  pauseCampaign,
  rejectCampaign,
  selectCampaignProducts,
  submitCampaign,
  updateCampaign,
  type CampaignFormPayload,
  type CampaignSearchFilters,
  type CampaignStatus,
  type CampaignView,
  type RejectCampaignPayload,
} from "@/api/campaigns";
import { listProducts, type ProductView } from "@/api/products";
import { exportCampaignCsv, exportCampaignPdf } from "@/api/reports";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import { CampaignReportDownloadActions } from "@/components/ReportDownloadPanel";
import { usePermissions } from "@/features/auth/usePermissions";
import { buildDownloadSuccessMessage } from "@/features/reports/reportDownload";
import {
  hasCampaignFormErrors,
  validateCampaignForm,
  validateRejectCampaignForm,
  type CampaignFormErrors,
  type RejectCampaignFormErrors,
} from "@/features/campaigns/campaignFormValidation";

export function CampaignsPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const [draftFilters, setDraftFilters] = useState<CampaignSearchFilters>(emptyCampaignFilters);
  const [appliedFilters, setAppliedFilters] = useState<CampaignSearchFilters>(emptyCampaignFilters);
  const [selectedCampaignId, setSelectedCampaignId] = useState("");
  const [createForm, setCreateForm] = useState<CampaignFormPayload>(emptyCampaignForm);
  const [createProductId, setCreateProductId] = useState("");
  const [editForm, setEditForm] = useState<CampaignFormPayload | null>(null);
  const [editProductSelection, setEditProductSelection] = useState<{
    campaignId: string;
    productId: string;
  } | null>(null);
  const [reviewNotes, setReviewNotes] = useState("");
  const [rejectForm, setRejectForm] = useState<RejectCampaignPayload>({
    rejectionReason: "",
    complianceReviewNotes: "",
  });
  const [rejectErrors, setRejectErrors] = useState<RejectCampaignFormErrors>({});
  const [notice, setNotice] = useState("");
  const [reportDownloadNotice, setReportDownloadNotice] = useState("");
  const [reportDownloadError, setReportDownloadError] = useState("");
  const canReadCampaigns = permissions.canReadCampaigns();
  const canManageCampaigns = permissions.canManageCampaigns();
  const canReviewCampaigns = permissions.canReviewCampaigns();
  const canDownloadReports = permissions.canViewReports();

  const campaignsQuery = useQuery({
    queryKey: ["campaigns", appliedFilters],
    queryFn: () => listCampaigns(appliedFilters),
    enabled: canReadCampaigns,
  });
  const productsQuery = useQuery({
    queryKey: ["products", "campaign-create"],
    queryFn: () => listProducts({ term: "", productType: "ALL", active: "true" }),
    enabled: canManageCampaigns,
  });

  const campaigns = useMemo(() => campaignsQuery.data ?? [], [campaignsQuery.data]);
  const products = useMemo(() => productsQuery.data ?? [], [productsQuery.data]);
  const selectedCampaign = useMemo(() => {
    if (selectedCampaignId === "") {
      return campaigns[0];
    }
    return campaigns.find((campaign) => campaign.id === selectedCampaignId);
  }, [campaigns, selectedCampaignId]);
  const selectedCampaignForm =
    selectedCampaign == null
      ? null
      : editForm == null
        ? campaignToForm(selectedCampaign)
        : editForm;
  const selectedCampaignProductId = selectedCampaign?.productIds[0] ?? "";
  const editProductId =
    selectedCampaign != null && editProductSelection?.campaignId === selectedCampaign.id
      ? editProductSelection.productId
      : selectedCampaignProductId;

  const filtersAreActive = hasActiveCampaignFilters(appliedFilters);

  const refreshCampaigns = async () => {
    await queryClient.invalidateQueries({ queryKey: ["campaigns"] });
  };

  const createMutation = useMutation({
    mutationFn: async (payload: CampaignFormPayload) => {
      const createdCampaign = await createCampaign(payload);
      return selectCampaignProducts(createdCampaign.id, [createProductId]);
    },
    onSuccess: async (createdCampaign) => {
      setCreateForm(emptyCampaignForm);
      setCreateProductId("");
      setSelectedCampaignId(createdCampaign.id);
      setEditForm(campaignToForm(createdCampaign));
      setNotice("Campaign created.");
      await refreshCampaigns();
    },
  });

  const updateMutation = useMutation({
    mutationFn: async () => {
      const updatedCampaign = await updateCampaign(
        selectedCampaign?.id ?? "",
        selectedCampaignForm ?? emptyCampaignForm,
      );
      return selectCampaignProducts(updatedCampaign.id, [editProductId]);
    },
    onSuccess: async (updatedCampaign) => {
      setSelectedCampaignId(updatedCampaign.id);
      setEditForm(campaignToForm(updatedCampaign));
      setEditProductSelection({
        campaignId: updatedCampaign.id,
        productId: updatedCampaign.productIds[0] ?? "",
      });
      setNotice("Campaign updated.");
      await refreshCampaigns();
    },
  });

  const lifecycleMutation = useMutation({
    mutationFn: (action: CampaignAction) =>
      runCampaignAction(action, selectedCampaign, {
        reviewNotes,
        rejectForm,
      }),
    onSuccess: async (updatedCampaign, action) => {
      setSelectedCampaignId(updatedCampaign.id);
      setEditForm(campaignToForm(updatedCampaign));
      setReviewNotes("");
      setRejectForm({ rejectionReason: "", complianceReviewNotes: "" });
      setRejectErrors({});
      setNotice(campaignActionNotice(action));
      await refreshCampaigns();
    },
  });

  const csvReportMutation = useMutation({
    mutationFn: (campaignId: string) => exportCampaignCsv(campaignId),
    onSuccess: (file) => {
      setReportDownloadError("");
      setReportDownloadNotice(
        buildDownloadSuccessMessage({
          filename: file.filename,
          exportType: "CSV",
          campaignLabel: selectedCampaign?.name ?? "campaign",
          completedAt: new Date().toISOString(),
        }),
      );
    },
    onError: (error) => {
      setReportDownloadNotice("");
      setReportDownloadError(
        isAuthorizationError(error)
          ? "You are not authorized to download campaign reports."
          : error instanceof Error
            ? error.message
            : "Unable to download CSV report.",
      );
    },
  });

  const pdfReportMutation = useMutation({
    mutationFn: (campaignId: string) => exportCampaignPdf(campaignId),
    onSuccess: (file) => {
      setReportDownloadError("");
      setReportDownloadNotice(
        buildDownloadSuccessMessage({
          filename: file.filename,
          exportType: "PDF",
          campaignLabel: selectedCampaign?.name ?? "campaign",
          completedAt: new Date().toISOString(),
        }),
      );
    },
    onError: (error) => {
      setReportDownloadNotice("");
      setReportDownloadError(
        isAuthorizationError(error)
          ? "You are not authorized to download campaign reports."
          : error instanceof Error
            ? error.message
            : "Unable to download PDF report.",
      );
    },
  });

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    lifecycleMutation.isPending ||
    csvReportMutation.isPending ||
    pdfReportMutation.isPending;
  const errorMessage =
    campaignListAuthorizationErrorMessage(campaignsQuery.error) ||
    campaignManagementAuthorizationErrorMessage(
      productsQuery.error,
      createMutation.error,
      updateMutation.error,
      lifecycleMutation.error,
    ) ||
    generalErrorMessage(
      campaignsQuery.error,
      productsQuery.error,
      createMutation.error,
      updateMutation.error,
      lifecycleMutation.error,
    );

  if (!canReadCampaigns) {
    return (
      <section className="panel">
        <div className="section-heading">
          <h2>Campaigns</h2>
          <span>Campaign definitions, compliance review, and lifecycle control</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view campaigns.
        </p>
      </section>
    );
  }

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Campaigns</h2>
          <span>Campaign definitions, compliance review, and lifecycle control</span>
        </div>
        <CampaignFiltersPanel
          draftFilters={draftFilters}
          appliedFilters={appliedFilters}
          notice={notice}
          errorMessage={errorMessage}
          onDraftChange={setDraftFilters}
          onApply={() => {
            setSelectedCampaignId("");
            setEditForm(null);
            setAppliedFilters(normalizeCampaignFilters(draftFilters));
          }}
          onReset={() => {
            setSelectedCampaignId("");
            setEditForm(null);
            setDraftFilters(emptyCampaignFilters);
            setAppliedFilters(emptyCampaignFilters);
          }}
        />
      </div>

      {canManageCampaigns ? (
        <div className="split-grid user-management-grid">
          <section className="panel" aria-labelledby="create-campaign-heading">
            <div className="section-heading">
              <h2 id="create-campaign-heading">Create campaign</h2>
              <span>Draft a campaign for segment targeting and compliance review</span>
            </div>
            <CampaignForm
              formId="create-campaign-form"
              submitLabel="Create campaign"
              values={createForm}
              products={products}
              productId={createProductId}
              productRequired
              disabled={isBusy}
              onChange={setCreateForm}
              onProductChange={setCreateProductId}
              onSubmit={() => {
                setNotice("");
                createMutation.mutate(createForm);
              }}
            />
          </section>

          <section className="panel" aria-labelledby="edit-campaign-heading">
            <div className="section-heading">
              <h2 id="edit-campaign-heading">Edit campaign</h2>
              <span>Draft and rejected campaigns can be revised before submission</span>
            </div>
            {selectedCampaign == null || selectedCampaignForm == null ? (
              <p>No campaigns match the selected filters.</p>
            ) : (
              <div className="form-grid">
                <label>
                  Campaign
                  <select
                    aria-label="Selected campaign"
                    value={selectedCampaign.id}
                    onChange={(event) => {
                      const nextCampaign = campaigns.find(
                        (campaign) => campaign.id === event.target.value,
                      );
                      setSelectedCampaignId(event.target.value);
                      setEditForm(nextCampaign == null ? null : campaignToForm(nextCampaign));
                      setEditProductSelection(null);
                    }}
                  >
                    {campaigns.map((campaign) => (
                      <option key={campaign.id} value={campaign.id}>
                        {campaign.name}
                      </option>
                    ))}
                  </select>
                </label>
                <CampaignForm
                  formId="edit-campaign-form"
                  submitLabel="Save campaign"
                  values={selectedCampaignForm}
                  products={products}
                  productId={editProductId}
                  productRequired
                  productAriaLabel="Edit campaign product"
                  disabled={isBusy || !canEditCampaign(selectedCampaign.status)}
                  onChange={(nextValues) =>
                    setEditForm((current) => ({
                      ...(current ?? selectedCampaignForm),
                      ...nextValues,
                    }))
                  }
                  onProductChange={(productId) => {
                    if (selectedCampaign != null) {
                      setEditProductSelection({ campaignId: selectedCampaign.id, productId });
                    }
                  }}
                  onSubmit={() => {
                    setNotice("");
                    updateMutation.mutate();
                  }}
                />
              </div>
            )}
          </section>
        </div>
      ) : null}

      <div className="panel">
        <div className="section-heading">
          <h2 id="campaign-worklist-heading">Campaign worklist</h2>
          <span>
            {campaignsQuery.isLoading
              ? "Loading campaigns"
              : formatCampaignSummary(campaigns.length, filtersAreActive)}
          </span>
        </div>
        {campaignsQuery.isLoading ? <p className="table-state">Loading campaign records.</p> : null}
        {campaignsQuery.isError ? (
          <p className="form-error" role="alert">
            Campaign records could not be loaded.
          </p>
        ) : null}
        {!campaignsQuery.isLoading && !campaignsQuery.isError && campaigns.length === 0 ? (
          <p className="table-state">No campaigns match the current filters.</p>
        ) : null}
        {!campaignsQuery.isLoading && !campaignsQuery.isError && campaigns.length > 0 ? (
          <table aria-labelledby="campaign-worklist-heading">
            <caption className="sr-only">
              Campaign worklist table with owner, segment, channel, status, schedule, update time,
              and recipient counts.
            </caption>
            <thead>
              <tr>
                <th scope="col">Campaign</th>
                <th scope="col">Owner</th>
                <th scope="col">Segment</th>
                <th scope="col">Channel</th>
                <th scope="col">Status</th>
                <th scope="col">Schedule</th>
                <th scope="col">Updated</th>
                <th scope="col">Recipients</th>
              </tr>
            </thead>
            <tbody>
              {campaigns.map((campaign) => (
                <tr
                  key={campaign.id}
                  className={selectedCampaign?.id === campaign.id ? "selected-row" : undefined}
                  onClick={() => {
                    setSelectedCampaignId(campaign.id);
                    setEditForm(campaignToForm(campaign));
                    setEditProductSelection(null);
                  }}
                >
                  <td>
                    <span className="table-primary-text">{campaign.name}</span>
                    <span className="table-secondary-text">{campaign.objective}</span>
                  </td>
                  <td>{campaign.ownerFullName ?? "Unassigned"}</td>
                  <td>{campaign.segmentName ?? "No segment"}</td>
                  <td>{formatCampaignEnum(campaign.channel)}</td>
                  <td>
                    <CampaignStatusBadge status={campaign.status} />
                  </td>
                  <td>{formatSchedule(campaign)}</td>
                  <td>{formatDateTime(campaign.updatedAt)}</td>
                  <td>
                    <Link
                      className="table-action-link"
                      to={`/campaigns/${campaign.id}/recipients/preview`}
                      onClick={(event) => event.stopPropagation()}
                    >
                      Preview
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>

      {selectedCampaign != null ? (
        <section className="panel" aria-labelledby="campaign-actions-heading">
          <div className="section-heading">
            <h2 id="campaign-actions-heading">Campaign actions</h2>
            <span>{selectedCampaign.name}</span>
          </div>
          <div className="button-row">
            {canManageCampaigns ? (
              <>
                <button
                  type="button"
                  disabled={isBusy || !canSubmitCampaign(selectedCampaign.status)}
                  onClick={() => lifecycleMutation.mutate("submit")}
                >
                  Submit
                </button>
                <button
                  type="button"
                  disabled={isBusy || selectedCampaign.status !== "APPROVED"}
                  onClick={() => lifecycleMutation.mutate("launch")}
                >
                  Launch
                </button>
                <button
                  type="button"
                  disabled={isBusy || selectedCampaign.status !== "ACTIVE"}
                  onClick={() => lifecycleMutation.mutate("pause")}
                >
                  Pause
                </button>
                <button
                  type="button"
                  disabled={
                    isBusy ||
                    (selectedCampaign.status !== "ACTIVE" && selectedCampaign.status !== "PAUSED")
                  }
                  onClick={() => lifecycleMutation.mutate("complete")}
                >
                  Complete
                </button>
                <button
                  type="button"
                  disabled={isBusy || !canArchiveCampaign(selectedCampaign.status)}
                  onClick={() => lifecycleMutation.mutate("archive")}
                >
                  Archive
                </button>
              </>
            ) : null}
          </div>

          {canDownloadReports ? (
            <CampaignReportDownloadActions
              campaignId={selectedCampaign.id}
              campaignName={selectedCampaign.name}
              canDownload={canDownloadReports}
              isCsvPending={csvReportMutation.isPending}
              isPdfPending={pdfReportMutation.isPending}
              onDownloadCsv={() => {
                setReportDownloadNotice("");
                setReportDownloadError("");
                csvReportMutation.mutate(selectedCampaign.id);
              }}
              onDownloadPdf={() => {
                setReportDownloadNotice("");
                setReportDownloadError("");
                pdfReportMutation.mutate(selectedCampaign.id);
              }}
              notice={reportDownloadNotice}
              error={reportDownloadError}
            />
          ) : null}

          {canReviewCampaigns ? (
            <div className="form-grid">
              <label>
                Compliance review notes
                <textarea
                  aria-label="Compliance review notes"
                  value={reviewNotes}
                  onChange={(event) => setReviewNotes(event.target.value)}
                />
              </label>
              <div className="button-row">
                <button
                  type="button"
                  disabled={isBusy || selectedCampaign.status !== "SUBMITTED"}
                  onClick={() => lifecycleMutation.mutate("approve")}
                >
                  Approve
                </button>
              </div>
              <label>
                Rejection reason
                <input
                  aria-label="Rejection reason"
                  value={rejectForm.rejectionReason}
                  aria-invalid={Boolean(rejectErrors.rejectionReason)}
                  onChange={(event) => {
                    setRejectErrors((current) => ({
                      ...current,
                      rejectionReason: undefined,
                    }));
                    setRejectForm({ ...rejectForm, rejectionReason: event.target.value });
                  }}
                />
                <FieldError message={rejectErrors.rejectionReason} />
              </label>
              <label>
                Rejection notes
                <textarea
                  aria-label="Rejection compliance notes"
                  value={rejectForm.complianceReviewNotes}
                  onChange={(event) =>
                    setRejectForm({ ...rejectForm, complianceReviewNotes: event.target.value })
                  }
                />
              </label>
              <button
                type="button"
                disabled={isBusy || selectedCampaign.status !== "SUBMITTED"}
                onClick={() => {
                  setNotice("");
                  const nextErrors = validateRejectCampaignForm(rejectForm);
                  setRejectErrors(nextErrors);
                  if (hasCampaignFormErrors(nextErrors)) {
                    return;
                  }
                  lifecycleMutation.mutate("reject");
                }}
              >
                Reject
              </button>
            </div>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

function CampaignFiltersPanel({
  draftFilters,
  appliedFilters,
  notice,
  errorMessage,
  onDraftChange,
  onApply,
  onReset,
}: {
  draftFilters: CampaignSearchFilters;
  appliedFilters: CampaignSearchFilters;
  notice: string;
  errorMessage: string;
  onDraftChange: (filters: CampaignSearchFilters) => void;
  onApply: () => void;
  onReset: () => void;
}) {
  const activeFilters = activeCampaignFilters(appliedFilters);

  return (
    <form
      className="form-grid"
      aria-label="Campaign search filters"
      onSubmit={(event) => {
        event.preventDefault();
        onApply();
      }}
    >
      <label>
        Search
        <input
          aria-label="Search campaigns"
          value={draftFilters.term}
          onChange={(event) => onDraftChange({ ...draftFilters, term: event.target.value })}
        />
      </label>
      <label>
        Status
        <select
          aria-label="Campaign status filter"
          value={draftFilters.status}
          onChange={(event) =>
            onDraftChange({
              ...draftFilters,
              status: event.target.value as CampaignStatus | "ALL",
            })
          }
        >
          <option value="ALL">All statuses</option>
          {campaignStatuses.map((status) => (
            <option key={status} value={status}>
              {formatCampaignEnum(status)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Owner user id
        <input
          aria-label="Campaign owner user id filter"
          value={draftFilters.ownerUserId}
          onChange={(event) => onDraftChange({ ...draftFilters, ownerUserId: event.target.value })}
        />
      </label>
      <label>
        Segment id
        <input
          aria-label="Campaign segment id filter"
          value={draftFilters.segmentId}
          onChange={(event) => onDraftChange({ ...draftFilters, segmentId: event.target.value })}
        />
      </label>
      <div className="button-row">
        <button type="submit">Apply filters</button>
        <button type="button" onClick={onReset}>
          Reset filters
        </button>
      </div>
      <p className="table-state">
        {activeFilters.length === 0
          ? "No active filters"
          : `${activeFilters.length} active ${activeFilters.length === 1 ? "filter" : "filters"}`}
      </p>
      {notice ? <p className="form-success">{notice}</p> : null}
      {errorMessage ? (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
    </form>
  );
}

function CampaignForm({
  formId,
  submitLabel,
  values,
  products,
  productId,
  productRequired = false,
  productAriaLabel = "Campaign product",
  disabled,
  onChange,
  onProductChange,
  onSubmit,
}: {
  formId: string;
  submitLabel: string;
  values: CampaignFormPayload;
  products?: ProductView[];
  productId?: string;
  productRequired?: boolean;
  productAriaLabel?: string;
  disabled: boolean;
  onChange: (values: CampaignFormPayload) => void;
  onProductChange?: (productId: string) => void;
  onSubmit: () => void;
}) {
  const [errors, setErrors] = useState<CampaignFormErrors>({});
  const [productError, setProductError] = useState("");

  const updateField = <TKey extends keyof CampaignFormPayload>(
    field: TKey,
    fieldValue: CampaignFormPayload[TKey],
  ) => {
    setErrors((current) => ({ ...current, [field]: undefined }));
    onChange({ ...values, [field]: fieldValue });
  };

  return (
    <form
      id={formId}
      className="form-grid"
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        const nextErrors = validateCampaignForm(values);
        const nextProductError =
          productRequired && (productId == null || productId.trim().length === 0)
            ? "Product is required."
            : "";
        setErrors(nextErrors);
        setProductError(nextProductError);
        if (hasCampaignFormErrors(nextErrors) || nextProductError) {
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
          aria-label="Campaign name"
          value={values.name}
          aria-invalid={Boolean(errors.name)}
          onChange={(event) => updateField("name", event.target.value)}
        />
        <FieldError message={errors.name} />
      </label>
      <label>
        Objective
        <textarea
          required
          aria-label="Campaign objective"
          value={values.objective}
          aria-invalid={Boolean(errors.objective)}
          onChange={(event) => updateField("objective", event.target.value)}
        />
        <FieldError message={errors.objective} />
      </label>
      <label>
        Channel
        <select
          aria-label="Campaign channel"
          value={values.channel}
          aria-invalid={Boolean(errors.channel)}
          onChange={(event) =>
            updateField("channel", event.target.value as CampaignFormPayload["channel"])
          }
        >
          {campaignChannels.map((channel) => (
            <option key={channel} value={channel}>
              {formatCampaignEnum(channel)}
            </option>
          ))}
        </select>
        <FieldError message={errors.channel} />
      </label>
      {products != null && onProductChange != null ? (
        <label>
          Product
          <select
            required={productRequired}
            aria-label={productAriaLabel}
            value={productId ?? ""}
            aria-invalid={Boolean(productError)}
            onChange={(event) => {
              setProductError("");
              onProductChange(event.target.value);
            }}
          >
            <option value="">Select product</option>
            {products.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
          <FieldError message={productError} />
        </label>
      ) : null}
      <label>
        Segment id
        <input
          required
          aria-label="Campaign segment id"
          value={values.segmentId}
          aria-invalid={Boolean(errors.segmentId)}
          onChange={(event) => updateField("segmentId", event.target.value)}
        />
        <FieldError message={errors.segmentId} />
      </label>
      <label>
        Message subject
        <input
          required
          maxLength={255}
          aria-label="Campaign message subject"
          value={values.messageSubject}
          aria-invalid={Boolean(errors.messageSubject)}
          onChange={(event) => updateField("messageSubject", event.target.value)}
        />
        <FieldError message={errors.messageSubject} />
      </label>
      <label>
        Message body
        <textarea
          required
          aria-label="Campaign message body"
          value={values.messageBody}
          aria-invalid={Boolean(errors.messageBody)}
          onChange={(event) => updateField("messageBody", event.target.value)}
        />
        <FieldError message={errors.messageBody} />
      </label>
      <label>
        Schedule date
        <input
          required
          type="date"
          aria-label="Campaign schedule date"
          value={values.startDate}
          aria-invalid={Boolean(errors.startDate)}
          onChange={(event) => updateField("startDate", event.target.value)}
        />
        <FieldError message={errors.startDate} />
      </label>
      <label>
        End date
        <input
          required
          type="date"
          aria-label="Campaign end date"
          value={values.endDate}
          aria-invalid={Boolean(errors.endDate)}
          onChange={(event) => updateField("endDate", event.target.value)}
        />
        <FieldError message={errors.endDate} />
      </label>
      <button type="submit" disabled={disabled}>
        {submitLabel}
      </button>
    </form>
  );
}

function FieldError({ message }: { message?: string }) {
  return message == null ? null : <span className="field-error">{message}</span>;
}

type CampaignAction = "submit" | "approve" | "reject" | "launch" | "pause" | "complete" | "archive";

function runCampaignAction(
  action: CampaignAction,
  campaign: CampaignView | undefined,
  values: { reviewNotes: string; rejectForm: RejectCampaignPayload },
) {
  const id = campaign?.id ?? "";
  switch (action) {
    case "submit":
      return submitCampaign(id);
    case "approve":
      return approveCampaign(id, values.reviewNotes);
    case "reject":
      return rejectCampaign(id, values.rejectForm);
    case "launch":
      return launchCampaign(id);
    case "pause":
      return pauseCampaign(id);
    case "complete":
      return completeCampaign(id);
    case "archive":
      return archiveCampaign(id);
  }
}

function canEditCampaign(status: CampaignStatus) {
  return status === "DRAFT" || status === "REJECTED";
}

function canSubmitCampaign(status: CampaignStatus) {
  return status === "DRAFT" || status === "REJECTED";
}

function canArchiveCampaign(status: CampaignStatus) {
  return status === "COMPLETED" || status === "REJECTED";
}

function normalizeCampaignFilters(filters: CampaignSearchFilters): CampaignSearchFilters {
  return {
    term: filters.term.trim(),
    status: filters.status,
    ownerUserId: filters.ownerUserId.trim(),
    segmentId: filters.segmentId.trim(),
  };
}

function hasActiveCampaignFilters(filters: CampaignSearchFilters) {
  return activeCampaignFilters(filters).length > 0;
}

function activeCampaignFilters(filters: CampaignSearchFilters) {
  return [
    filters.term.trim() ? "term" : "",
    filters.status !== "ALL" ? "status" : "",
    filters.ownerUserId.trim() ? "ownerUserId" : "",
    filters.segmentId.trim() ? "segmentId" : "",
  ].filter(Boolean);
}

function formatCampaignSummary(count: number, filtered: boolean) {
  const noun = count === 1 ? "campaign" : "campaigns";
  return filtered ? `${count} matching ${noun}` : `${count} ${noun}`;
}

function campaignActionNotice(action: CampaignAction) {
  const labels: Record<CampaignAction, string> = {
    submit: "Campaign submitted.",
    approve: "Campaign approved.",
    reject: "Campaign rejected.",
    launch: "Campaign launched.",
    pause: "Campaign paused.",
    complete: "Campaign completed.",
    archive: "Campaign archived.",
  };
  return labels[action];
}

function formatSchedule(campaign: CampaignView) {
  if (campaign.startDate == null && campaign.endDate == null) {
    return "Not scheduled";
  }
  return `${campaign.startDate ?? "Open"} - ${campaign.endDate ?? "Open"}`;
}

function formatDateTime(value: string | null) {
  if (value == null) {
    return "Not available";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function campaignListAuthorizationErrorMessage(error: unknown) {
  return isAuthorizationError(error) ? "You are not authorized to view campaigns." : "";
}

function campaignManagementAuthorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError) ? "You are not authorized to manage campaigns." : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Campaign action failed." : "";
}
