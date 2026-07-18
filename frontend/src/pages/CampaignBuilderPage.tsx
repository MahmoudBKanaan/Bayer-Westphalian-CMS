import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  approveCampaignCopySuggestion,
  generateCampaignCopySuggestion,
  type AiRecommendationView,
  type CampaignCopySuggestionView,
} from "@/api/ai";
import {
  campaignChannels,
  createCampaign,
  emptyCampaignForm,
  formatCampaignEnum,
  selectCampaignProducts,
  submitCampaign,
  type CampaignFormPayload,
  type CampaignView,
} from "@/api/campaigns";
import { isAuthorizationError } from "@/api/client";
import { listProducts, type ProductView } from "@/api/products";
import { listSegments, type SegmentView } from "@/api/segments";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import { ConfirmationDialog } from "@/components/ConfirmationDialog";
import { usePermissions } from "@/features/auth/usePermissions";
import {
  CAMPAIGN_BUILDER_PAGE_LEAD,
  CAMPAIGN_BUILDER_STEPS,
  builderStepStatus,
  firstIncompleteBuilderStep,
  getCampaignBuilderStep,
  getNextBuilderStepId,
  getPreviousBuilderStepId,
  validateBuilderStep,
  type CampaignBuilderStepId,
} from "@/features/campaigns/campaignBuilderFlow";
import {
  CAMPAIGN_BUILDER_FORM_ARIA_LABEL,
  CAMPAIGN_BUILDER_PAGE_TITLE,
  CAMPAIGN_CREATE_DRAFT_LABEL,
  CAMPAIGN_DRAFT_CREATED_NOTICE,
  CAMPAIGN_SUBMITTED_NOTICE,
  CAMPAIGN_SUBMIT_FOR_REVIEW_LABEL,
} from "@/features/campaigns/campaignCreationFlow";
import {
  type CampaignFormErrors,
} from "@/features/campaigns/campaignFormValidation";

/**
 * Campaign Builder (KB FR-050 / FR-057 / item 592 flow improvement).
 *
 * Guided multi-step draft creation: basics → audience/product → message → schedule → review.
 */
export function CampaignBuilderPage() {
  const permissions = usePermissions();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const recommendedProductId = searchParams.get("productId")?.trim() ?? "";
  const [form, setForm] = useState<CampaignFormPayload>(emptyCampaignForm);
  const [selectedProductIds, setSelectedProductIds] = useState<string[]>(() =>
    recommendedProductId === "" ? [] : [recommendedProductId],
  );
  const [createdCampaign, setCreatedCampaign] = useState<CampaignView | null>(null);
  const [formErrors, setFormErrors] = useState<CampaignFormErrors>({});
  const [productSelectionError, setProductSelectionError] = useState("");
  const [copySuggestion, setCopySuggestion] = useState<CampaignCopySuggestionView | null>(null);
  const [copyApproval, setCopyApproval] = useState<AiRecommendationView | null>(null);
  const [copyReviewNotes, setCopyReviewNotes] = useState("");
  const [editedCopySubject, setEditedCopySubject] = useState("");
  const [editedCopyBody, setEditedCopyBody] = useState("");
  const [editedCopyCta, setEditedCopyCta] = useState("");
  const [copyEditing, setCopyEditing] = useState(false);
  const [approveConfirmOpen, setApproveConfirmOpen] = useState(false);
  const [copyApprovedAt, setCopyApprovedAt] = useState<string | null>(null);
  const [notice, setNotice] = useState(() =>
    recommendedProductId === ""
      ? ""
      : "AI-recommended product loaded for human review. Campaign remains a draft.",
  );
  const [currentStepId, setCurrentStepId] = useState<CampaignBuilderStepId>(() =>
    recommendedProductId === "" ? "basics" : "audience",
  );

  const segmentsQuery = useQuery({
    queryKey: ["segments", "campaign-builder"],
    queryFn: () => listSegments({ term: "", visibility: "ALL" }),
  });
  const productsQuery = useQuery({
    queryKey: ["products", "campaign-builder"],
    queryFn: () => listProducts({ term: "", productType: "ALL", active: "true" }),
  });

  const segments = useMemo(() => segmentsQuery.data ?? [], [segmentsQuery.data]);
  const products = useMemo(() => productsQuery.data ?? [], [productsQuery.data]);
  const selectedProduct = products.find((product) => product.id === selectedProductIds[0]);

  const selectedSegment = segments.find((segment) => segment.id === form.segmentId);
  const canBuildCampaigns = permissions.canManageCampaigns();
  const canUseAiCampaignCopy = permissions.canUseAiCampaignCopy();
  const currentStep = getCampaignBuilderStep(currentStepId);

  const createDraftMutation = useMutation({
    mutationFn: async () => {
      const campaign = await createCampaign(form);
      if (selectedProductIds.length > 0) {
        return selectCampaignProducts(campaign.id, selectedProductIds);
      }
      return campaign;
    },
    onSuccess: async (campaign) => {
      setCreatedCampaign(campaign);
      setNotice(CAMPAIGN_DRAFT_CREATED_NOTICE);
      setCurrentStepId("review");
      await queryClient.invalidateQueries({ queryKey: ["campaigns"] });
    },
  });

  const submitMutation = useMutation({
    mutationFn: () => submitCampaign(createdCampaign?.id ?? ""),
    onSuccess: async (campaign) => {
      setCreatedCampaign(campaign);
      setNotice(CAMPAIGN_SUBMITTED_NOTICE);
      await queryClient.invalidateQueries({ queryKey: ["campaigns"] });
    },
  });

  const generateCopyMutation = useMutation({
    mutationFn: () =>
      generateCampaignCopySuggestion({
        campaignId: createdCampaign?.id ?? null,
        objective: form.objective,
        productName: selectedProduct?.name ?? null,
        channel: form.channel,
        audienceHint: selectedSegment?.name ?? null,
      }),
    onSuccess: (suggestion) => {
      setCopySuggestion(suggestion);
      setCopyApproval(null);
      setCopyApprovedAt(null);
      setEditedCopySubject(suggestion.subject);
      setEditedCopyBody(suggestion.body);
      setEditedCopyCta(suggestion.callToAction ?? "");
      setCopyEditing(false);
      setNotice(
        "AI campaign copy suggestion generated for human review. Campaign form text was not changed.",
      );
    },
  });

  const approveCopyMutation = useMutation({
    mutationFn: () =>
      approveCampaignCopySuggestion(copySuggestion?.storedRecommendationId ?? "", {
        reviewNotes: copyReviewNotes,
        editedSubject: editedCopySubject,
        editedMessageBody: editedCopyBody,
        editedCallToAction: editedCopyCta,
      }),
    onSuccess: (approval) => {
      setCopyApproval(approval);
      setCopyApprovedAt(new Date().toISOString());
      setApproveConfirmOpen(false);
      setCopyEditing(false);
      // Apply approved text into the local draft form only after human approval.
      updateFormField("messageSubject", editedCopySubject.trim());
      const bodyWithCta =
        editedCopyCta.trim().length > 0
          ? `${editedCopyBody.trim()}\n\n${editedCopyCta.trim()}`
          : editedCopyBody.trim();
      updateFormField("messageBody", bodyWithCta);
      setNotice(
        "AI campaign copy approved and applied to the draft. Campaign remains DRAFT. Compliance approval is still required.",
      );
    },
  });

  const isBusy =
    createDraftMutation.isPending ||
    submitMutation.isPending ||
    generateCopyMutation.isPending ||
    approveCopyMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(
      segmentsQuery.error,
      productsQuery.error,
      createDraftMutation.error,
      submitMutation.error,
      generateCopyMutation.error,
      approveCopyMutation.error,
    ) ||
    generalErrorMessage(
      segmentsQuery.error,
      productsQuery.error,
      createDraftMutation.error,
      submitMutation.error,
      generateCopyMutation.error,
      approveCopyMutation.error,
    );

  if (!canBuildCampaigns) {
    return (
      <section className="panel campaign-builder-page" aria-labelledby="campaign-builder-title">
        <div className="section-heading">
          <h2 id="campaign-builder-title">{CAMPAIGN_BUILDER_PAGE_TITLE}</h2>
          <span>Draft campaign setup</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to build campaigns.
        </p>
      </section>
    );
  }

  function applyStepValidation(stepId: CampaignBuilderStepId): boolean {
    const { formErrors: nextErrors, productError } = validateBuilderStep(
      stepId,
      form,
      selectedProductIds,
    );
    setFormErrors(nextErrors);
    setProductSelectionError(productError);
    return Object.keys(nextErrors).length === 0 && productError === "";
  }

  function goToStep(stepId: CampaignBuilderStepId) {
    setFormErrors({});
    setProductSelectionError("");
    setCurrentStepId(stepId);
  }

  function handleContinue() {
    if (!applyStepValidation(currentStepId)) {
      return;
    }
    const next = getNextBuilderStepId(currentStepId);
    if (next != null) {
      goToStep(next);
    }
  }

  function handleBack() {
    const previous = getPreviousBuilderStepId(currentStepId);
    if (previous != null) {
      goToStep(previous);
    }
  }

  function handleCreateDraft() {
    if (!applyStepValidation("review")) {
      const incomplete = firstIncompleteBuilderStep(form, selectedProductIds);
      if (incomplete != null) {
        setCurrentStepId(incomplete);
      }
      return;
    }
    setNotice("");
    createDraftMutation.mutate();
  }

  return (
    <section className="page-stack campaign-builder-page" aria-labelledby="campaign-builder-title">
      <header className="panel campaign-builder-header">
        <div className="section-heading">
          <h2 id="campaign-builder-title">{CAMPAIGN_BUILDER_PAGE_TITLE}</h2>
          <span>
            Step {currentStep.index + 1} of {CAMPAIGN_BUILDER_STEPS.length}: {currentStep.title}
          </span>
        </div>
        <p className="campaign-builder-lead">{CAMPAIGN_BUILDER_PAGE_LEAD}</p>
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
      </header>

      <CampaignBuilderStepper
        currentStepId={currentStepId}
        form={form}
        selectedProductIds={selectedProductIds}
        draftCreated={createdCampaign != null}
        onSelectStep={goToStep}
      />

      <div className="split-grid user-management-grid campaign-builder-main">
        <section
          className="panel campaign-builder-step-panel"
          aria-labelledby="builder-step-heading"
        >
          <div className="section-heading">
            <h2 id="builder-step-heading">{currentStep.title}</h2>
            <span>{currentStep.description}</span>
          </div>

          {currentStepId === "basics" ? (
            <BasicsStepFields
              form={form}
              formErrors={formErrors}
              onChange={updateFormField}
            />
          ) : null}

          {currentStepId === "audience" ? (
            <AudienceStepFields
              form={form}
              formErrors={formErrors}
              productSelectionError={productSelectionError}
              segments={segments}
              products={products}
              selectedProductIds={selectedProductIds}
              segmentsLoading={segmentsQuery.isLoading}
              productsLoading={productsQuery.isLoading}
              onChange={updateFormField}
              onProductChange={(productId) => {
                setProductSelectionError("");
                setSelectedProductIds(productId ? [productId] : []);
              }}
            />
          ) : null}

          {currentStepId === "message" ? (
            <>
              <MessageStepFields
                form={form}
                formErrors={formErrors}
                onChange={updateFormField}
              />
              {canUseAiCampaignCopy ? (
                <CampaignCopyApprovalPanel
                  suggestion={copySuggestion}
                  approval={copyApproval}
                  approvedAt={copyApprovedAt}
                  reviewNotes={copyReviewNotes}
                  editedSubject={editedCopySubject}
                  editedBody={editedCopyBody}
                  editedCta={editedCopyCta}
                  editing={copyEditing}
                  productName={selectedProduct?.name ?? null}
                  segmentName={selectedSegment?.name ?? null}
                  campaignStatus={createdCampaign?.status ?? null}
                  formSubject={form.messageSubject}
                  generating={generateCopyMutation.isPending}
                  approving={approveCopyMutation.isPending}
                  canGenerate={form.objective.trim().length > 0}
                  busy={isBusy}
                  onReviewNotesChange={setCopyReviewNotes}
                  onEditedSubjectChange={setEditedCopySubject}
                  onEditedBodyChange={setEditedCopyBody}
                  onEditedCtaChange={setEditedCopyCta}
                  onToggleEdit={() => setCopyEditing((value) => !value)}
                  onDismiss={() => {
                    setCopySuggestion(null);
                    setCopyApproval(null);
                    setCopyApprovedAt(null);
                    setCopyEditing(false);
                    setCopyReviewNotes("");
                    setNotice("AI campaign copy suggestion dismissed. Campaign form was not changed.");
                  }}
                  onGenerate={() => {
                    setNotice("");
                    generateCopyMutation.mutate();
                  }}
                  onRequestApprove={() => {
                    setNotice("");
                    setApproveConfirmOpen(true);
                  }}
                />
              ) : null}
              {approveConfirmOpen ? (
                <ConfirmationDialog
                  id="approve-ai-copy-confirmation"
                  title="Approve AI Copy Suggestion?"
                  description={
                    <>
                      <p>
                        The suggested copy will be copied into the campaign draft message fields.
                      </p>
                      <p>
                        <strong>This does not approve the campaign for launch.</strong> Compliance
                        Officer approval is still required before launch.
                      </p>
                    </>
                  }
                  confirmLabel="Approve and Apply"
                  confirmVariant="primary"
                  busy={approveCopyMutation.isPending}
                  onCancel={() => setApproveConfirmOpen(false)}
                  onConfirm={() => approveCopyMutation.mutate()}
                />
              ) : null}
            </>
          ) : null}

          {currentStepId === "schedule" ? (
            <ScheduleStepFields
              form={form}
              formErrors={formErrors}
              onChange={updateFormField}
            />
          ) : null}

          {currentStepId === "review" ? (
            <ReviewStepContent
              form={form}
              campaign={createdCampaign}
              products={products}
              selectedProductIds={selectedProductIds}
              segment={selectedSegment}
              formErrors={formErrors}
              productSelectionError={productSelectionError}
              onEditStep={goToStep}
            />
          ) : null}

          <div className="button-row campaign-builder-actions">
            <button
              type="button"
              className="secondary-button"
              disabled={getPreviousBuilderStepId(currentStepId) == null || isBusy}
              onClick={handleBack}
            >
              Back
            </button>
            {currentStepId !== "review" ? (
              <button type="button" disabled={isBusy} onClick={handleContinue}>
                {currentStep.primaryActionLabel}
              </button>
            ) : (
              <>
                <button
                  type="button"
                  disabled={isBusy || createdCampaign != null}
                  onClick={handleCreateDraft}
                >
                  {CAMPAIGN_CREATE_DRAFT_LABEL}
                </button>
                <button
                  type="button"
                  disabled={
                    createdCampaign == null || isBusy || createdCampaign.status !== "DRAFT"
                  }
                  onClick={() => {
                    setNotice("");
                    submitMutation.mutate();
                  }}
                >
                  {CAMPAIGN_SUBMIT_FOR_REVIEW_LABEL}
                </button>
              </>
            )}
            <button
              type="button"
              className="secondary-button"
              onClick={() => navigate("/campaigns")}
            >
              View campaigns
            </button>
          </div>
        </section>

        <aside className="panel campaign-builder-sidebar" aria-labelledby="builder-summary-heading">
          <div className="section-heading">
            <h2 id="builder-summary-heading">Live summary</h2>
            <span>Selections update as you complete each step</span>
          </div>
          <CampaignBuilderSummary
            form={form}
            campaign={createdCampaign}
            products={products}
            selectedProductIds={selectedProductIds}
            segment={selectedSegment}
          />
          <dl className="detail-list" aria-label="Promoted product selection">
            <div>
              <dt>Selected product</dt>
              <dd>
                {products.find((product) => product.id === selectedProductIds[0])?.name ??
                  "No product selected"}
              </dd>
            </div>
            <div>
              <dt>Available products</dt>
              <dd>{productsQuery.isLoading ? "Loading…" : products.length}</dd>
            </div>
          </dl>
          {productsQuery.isLoading ? <p className="table-state">Loading active products.</p> : null}
          {!productsQuery.isLoading && products.length === 0 ? (
            <p className="table-state">No active products are available.</p>
          ) : null}
        </aside>
      </div>
    </section>
  );

  function updateFormField<TKey extends keyof CampaignFormPayload>(
    field: TKey,
    value: CampaignFormPayload[TKey],
  ) {
    setFormErrors((current) => ({ ...current, [field]: undefined }));
    setForm((current) => ({ ...current, [field]: value }));
  }
}

function CampaignBuilderStepper({
  currentStepId,
  form,
  selectedProductIds,
  draftCreated,
  onSelectStep,
}: {
  currentStepId: CampaignBuilderStepId;
  form: CampaignFormPayload;
  selectedProductIds: string[];
  draftCreated: boolean;
  onSelectStep: (stepId: CampaignBuilderStepId) => void;
}) {
  return (
    <nav className="panel campaign-builder-stepper" aria-label="Campaign builder steps">
      <ol className="campaign-builder-step-list">
        {CAMPAIGN_BUILDER_STEPS.map((step) => {
          const status = builderStepStatus(
            step.id,
            currentStepId,
            form,
            selectedProductIds,
            draftCreated,
          );
          const isCurrent = status === "current";
          return (
            <li key={step.id} className={`campaign-builder-step campaign-builder-step--${status}`}>
              <button
                type="button"
                className="campaign-builder-step-button"
                aria-current={isCurrent ? "step" : undefined}
                onClick={() => onSelectStep(step.id)}
              >
                <span className="campaign-builder-step-index" aria-hidden="true">
                  {step.index + 1}
                </span>
                <span className="campaign-builder-step-copy">
                  <span className="campaign-builder-step-title">{step.shortTitle}</span>
                  <span className="campaign-builder-step-status">
                    {status === "complete" ? "Done" : isCurrent ? "Current" : "Next"}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

function BasicsStepFields({
  form,
  formErrors,
  onChange,
}: {
  form: CampaignFormPayload;
  formErrors: CampaignFormErrors;
  onChange: <TKey extends keyof CampaignFormPayload>(
    field: TKey,
    value: CampaignFormPayload[TKey],
  ) => void;
}) {
  return (
    <form
      className="form-grid"
      aria-label={CAMPAIGN_BUILDER_FORM_ARIA_LABEL}
      noValidate
      onSubmit={(event) => event.preventDefault()}
    >
      <BuilderInput
        label="Campaign name"
        value={form.name}
        error={formErrors.name}
        required
        onChange={(value) => onChange("name", value)}
      />
      <label>
        Objective
        <textarea
          required
          aria-label="Campaign objective"
          value={form.objective}
          aria-invalid={Boolean(formErrors.objective)}
          onChange={(event) => onChange("objective", event.target.value)}
        />
        <FieldError message={formErrors.objective} />
      </label>
      <label>
        Channel
        <select
          aria-label="Campaign channel"
          value={form.channel}
          aria-invalid={Boolean(formErrors.channel)}
          onChange={(event) =>
            onChange("channel", event.target.value as CampaignFormPayload["channel"])
          }
        >
          {campaignChannels.map((channel) => (
            <option key={channel} value={channel}>
              {formatCampaignEnum(channel)}
            </option>
          ))}
        </select>
        <FieldError message={formErrors.channel} />
      </label>
    </form>
  );
}

function AudienceStepFields({
  form,
  formErrors,
  productSelectionError,
  segments,
  products,
  selectedProductIds,
  segmentsLoading,
  productsLoading,
  onChange,
  onProductChange,
}: {
  form: CampaignFormPayload;
  formErrors: CampaignFormErrors;
  productSelectionError: string;
  segments: SegmentView[];
  products: ProductView[];
  selectedProductIds: string[];
  segmentsLoading: boolean;
  productsLoading: boolean;
  onChange: <TKey extends keyof CampaignFormPayload>(
    field: TKey,
    value: CampaignFormPayload[TKey],
  ) => void;
  onProductChange: (productId: string) => void;
}) {
  return (
    <form
      className="form-grid"
      aria-label={CAMPAIGN_BUILDER_FORM_ARIA_LABEL}
      noValidate
      onSubmit={(event) => event.preventDefault()}
    >
      <label>
        Audience segment
        <select
          required
          aria-label="Campaign audience segment"
          value={form.segmentId}
          disabled={segmentsLoading}
          aria-invalid={Boolean(formErrors.segmentId)}
          onChange={(event) => onChange("segmentId", event.target.value)}
        >
          <option value="">Select audience segment</option>
          {segments.map((segment) => (
            <option key={segment.id} value={segment.id}>
              {segment.name}
            </option>
          ))}
        </select>
        <FieldError message={formErrors.segmentId} />
      </label>
      <label>
        Product
        <select
          required
          aria-label="Campaign product"
          value={selectedProductIds[0] ?? ""}
          disabled={productsLoading}
          aria-invalid={Boolean(productSelectionError)}
          onChange={(event) => onProductChange(event.target.value)}
        >
          <option value="">Select product</option>
          {products.map((product) => (
            <option key={product.id} value={product.id}>
              {product.name}
            </option>
          ))}
        </select>
        <FieldError message={productSelectionError} />
      </label>
      <p className="campaign-builder-step-hint">
        The segment drives eligibility later; the product ties the offer and analytics to the right
        product context.
      </p>
    </form>
  );
}

function MessageStepFields({
  form,
  formErrors,
  onChange,
}: {
  form: CampaignFormPayload;
  formErrors: CampaignFormErrors;
  onChange: <TKey extends keyof CampaignFormPayload>(
    field: TKey,
    value: CampaignFormPayload[TKey],
  ) => void;
}) {
  return (
    <form
      className="form-grid"
      aria-label={CAMPAIGN_BUILDER_FORM_ARIA_LABEL}
      noValidate
      onSubmit={(event) => event.preventDefault()}
    >
      <BuilderInput
        label="Message subject"
        value={form.messageSubject}
        error={formErrors.messageSubject}
        onChange={(value) => onChange("messageSubject", value)}
      />
      <label>
        Message body
        <textarea
          required
          aria-label="Campaign message body"
          value={form.messageBody}
          aria-invalid={Boolean(formErrors.messageBody)}
          onChange={(event) => onChange("messageBody", event.target.value)}
        />
        <FieldError message={formErrors.messageBody} />
      </label>
    </form>
  );
}

function ScheduleStepFields({
  form,
  formErrors,
  onChange,
}: {
  form: CampaignFormPayload;
  formErrors: CampaignFormErrors;
  onChange: <TKey extends keyof CampaignFormPayload>(
    field: TKey,
    value: CampaignFormPayload[TKey],
  ) => void;
}) {
  return (
    <form
      className="form-grid"
      aria-label={CAMPAIGN_BUILDER_FORM_ARIA_LABEL}
      noValidate
      onSubmit={(event) => event.preventDefault()}
    >
      <label>
        Schedule date
        <input
          required
          type="date"
          aria-label="Campaign schedule date"
          value={form.startDate}
          aria-invalid={Boolean(formErrors.startDate)}
          onChange={(event) => onChange("startDate", event.target.value)}
        />
        <FieldError message={formErrors.startDate} />
      </label>
      <label>
        End date
        <input
          required
          type="date"
          aria-label="Campaign end date"
          value={form.endDate}
          aria-invalid={Boolean(formErrors.endDate)}
          onChange={(event) => onChange("endDate", event.target.value)}
        />
        <FieldError message={formErrors.endDate} />
      </label>
      <p className="campaign-builder-step-hint">
        End date must be on or after the schedule date. Compliance reviews the planned window with
        the draft.
      </p>
    </form>
  );
}

function ReviewStepContent({
  form,
  campaign,
  products,
  selectedProductIds,
  segment,
  formErrors,
  productSelectionError,
  onEditStep,
}: {
  form: CampaignFormPayload;
  campaign: CampaignView | null;
  products: ProductView[];
  selectedProductIds: string[];
  segment: SegmentView | undefined;
  formErrors: CampaignFormErrors;
  productSelectionError: string;
  onEditStep: (stepId: CampaignBuilderStepId) => void;
}) {
  const hasBlockingErrors =
    Object.keys(formErrors).length > 0 || productSelectionError.length > 0;
  const selectedProducts = products.filter((product) => selectedProductIds.includes(product.id));

  return (
    <div className="campaign-builder-review" aria-label="Campaign builder review">
      {hasBlockingErrors ? (
        <p className="form-error" role="alert">
          Complete the highlighted steps before creating a draft.
        </p>
      ) : null}
      {productSelectionError ? <p className="field-error">{productSelectionError}</p> : null}
      {formErrors.name ? <p className="field-error">{formErrors.name}</p> : null}
      {formErrors.objective ? <p className="field-error">{formErrors.objective}</p> : null}
      {formErrors.segmentId ? <p className="field-error">{formErrors.segmentId}</p> : null}
      {formErrors.messageSubject ? (
        <p className="field-error">{formErrors.messageSubject}</p>
      ) : null}
      {formErrors.messageBody ? <p className="field-error">{formErrors.messageBody}</p> : null}
      {formErrors.startDate ? <p className="field-error">{formErrors.startDate}</p> : null}
      {formErrors.endDate ? <p className="field-error">{formErrors.endDate}</p> : null}

      <dl className="detail-list campaign-builder-review-list">
        <div>
          <dt>Name</dt>
          <dd>{form.name.trim() || "—"}</dd>
        </div>
        <div>
          <dt>Objective</dt>
          <dd>{form.objective.trim() || "—"}</dd>
        </div>
        <div>
          <dt>Channel</dt>
          <dd>{formatCampaignEnum(form.channel)}</dd>
        </div>
        <div>
          <dt>Audience</dt>
          <dd>{segment?.name ?? "No segment selected"}</dd>
        </div>
        <div>
          <dt>Products</dt>
          <dd>
            {selectedProducts.length === 0
              ? "No products selected"
              : selectedProducts.map((product) => product.name).join(", ")}
          </dd>
        </div>
        <div>
          <dt>Message subject</dt>
          <dd>{form.messageSubject.trim() || "—"}</dd>
        </div>
        <div>
          <dt>Message body</dt>
          <dd className="campaign-builder-message-preview">
            {form.messageBody.trim() || "—"}
          </dd>
        </div>
        <div>
          <dt>Schedule</dt>
          <dd>
            {form.startDate || "—"} → {form.endDate || "—"}
          </dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            {campaign == null ? "Not created" : <CampaignStatusBadge status={campaign.status} />}
          </dd>
        </div>
      </dl>

      <div className="button-row campaign-builder-edit-links" aria-label="Edit builder steps">
        <button type="button" className="secondary-button" onClick={() => onEditStep("basics")}>
          Edit basics
        </button>
        <button type="button" className="secondary-button" onClick={() => onEditStep("audience")}>
          Edit audience
        </button>
        <button type="button" className="secondary-button" onClick={() => onEditStep("message")}>
          Edit message
        </button>
        <button type="button" className="secondary-button" onClick={() => onEditStep("schedule")}>
          Edit schedule
        </button>
      </div>

      <p className="campaign-builder-step-hint">
        Creating a draft saves the campaign as <strong>DRAFT</strong>. Submit for review only after
        the draft exists so Compliance Officers can approve or reject it.
      </p>
    </div>
  );
}

function CampaignCopyApprovalPanel({
  suggestion,
  approval,
  approvedAt,
  reviewNotes,
  editedSubject,
  editedBody,
  editedCta,
  editing,
  productName,
  segmentName,
  campaignStatus,
  formSubject,
  generating,
  approving,
  canGenerate,
  busy,
  onReviewNotesChange,
  onEditedSubjectChange,
  onEditedBodyChange,
  onEditedCtaChange,
  onToggleEdit,
  onDismiss,
  onGenerate,
  onRequestApprove,
}: {
  suggestion: CampaignCopySuggestionView | null;
  approval: AiRecommendationView | null;
  approvedAt: string | null;
  reviewNotes: string;
  editedSubject: string;
  editedBody: string;
  editedCta: string;
  editing: boolean;
  productName: string | null;
  segmentName: string | null;
  campaignStatus: string | null;
  formSubject: string;
  generating: boolean;
  approving: boolean;
  canGenerate: boolean;
  busy: boolean;
  onReviewNotesChange: (value: string) => void;
  onEditedSubjectChange: (value: string) => void;
  onEditedBodyChange: (value: string) => void;
  onEditedCtaChange: (value: string) => void;
  onToggleEdit: () => void;
  onDismiss: () => void;
  onGenerate: () => void;
  onRequestApprove: () => void;
}) {
  const isApproved = approval?.approved === true;
  const hasStoredRecommendation = suggestion?.storedRecommendationId != null;
  const formUnchangedBySuggestion =
    suggestion != null &&
    formSubject.trim() !== suggestion.subject.trim() &&
    !isApproved;

  return (
    <section
      className="campaign-builder-ai-panel"
      aria-labelledby="ai-copy-assistant-heading"
      data-testid="ai-copy-assistant"
    >
      <div className="section-heading">
        <h3 id="ai-copy-assistant-heading">AI Copy Assistant</h3>
        <span>Rule-based decision support (AI-005) · human review required</span>
      </div>
      <p className="table-secondary-text">
        Suggestions stay separate from the campaign message form until a human clicks{" "}
        <strong>Approve and Apply</strong>. This never submits, compliance-approves, or launches
        the campaign.
      </p>
      <div className="ai-copy-panel">
        <button
          type="button"
          disabled={!canGenerate || busy}
          onClick={onGenerate}
          aria-label="Generate AI campaign copy suggestion"
        >
          {generating ? "Generating…" : "Generate AI copy"}
        </button>
        {generating ? (
          <p className="table-state" role="status" aria-live="polite">
            Generating campaign copy suggestion…
          </p>
        ) : null}
        {!generating && suggestion == null ? (
          <p className="table-state" data-testid="ai-copy-idle">
            No AI copy suggestion has been generated yet. Status: idle.
          </p>
        ) : null}
        {suggestion != null ? (
          <div className="ai-copy-suggestion" aria-label="AI campaign copy suggestion">
            <p className="form-success" role="status" data-testid="ai-copy-pending-banner">
              {isApproved
                ? "Suggestion status: APPROVED BY USER"
                : "Draft suggestion — human review required (PENDING_REVIEW)"}
            </p>
            <dl className="detail-list">
              <div>
                <dt>Product</dt>
                <dd>{productName ?? "Not selected"}</dd>
              </div>
              <div>
                <dt>Target segment</dt>
                <dd>{segmentName ?? "Not selected"}</dd>
              </div>
              <div>
                <dt>Suggested subject</dt>
                <dd>
                  {editing ? (
                    <input
                      aria-label="Edit suggested subject"
                      value={editedSubject}
                      onChange={(event) => onEditedSubjectChange(event.target.value)}
                    />
                  ) : (
                    editedSubject || suggestion.subject
                  )}
                </dd>
              </div>
              <div>
                <dt>Suggested message</dt>
                <dd>
                  {editing ? (
                    <textarea
                      aria-label="Edit suggested message"
                      value={editedBody}
                      onChange={(event) => onEditedBodyChange(event.target.value)}
                    />
                  ) : (
                    editedBody || suggestion.body
                  )}
                </dd>
              </div>
              <div>
                <dt>Suggested CTA</dt>
                <dd>
                  {editing ? (
                    <input
                      aria-label="Edit suggested call to action"
                      value={editedCta}
                      onChange={(event) => onEditedCtaChange(event.target.value)}
                    />
                  ) : (
                    editedCta || suggestion.callToAction || "Not provided"
                  )}
                </dd>
              </div>
              <div>
                <dt>Approval status</dt>
                <dd>
                  {isApproved ? "Human approved" : "Awaiting human approval"}
                </dd>
              </div>
              {isApproved ? (
                <>
                  <div>
                    <dt>Approved by</dt>
                    <dd>{approval?.approvedByFullName ?? "Campaign Manager"}</dd>
                  </div>
                  <div>
                    <dt>Approved at</dt>
                    <dd>
                      {approvedAt
                        ? new Intl.DateTimeFormat("en", {
                            dateStyle: "medium",
                            timeStyle: "short",
                          }).format(new Date(approvedAt))
                        : "Just now"}
                    </dd>
                  </div>
                  <div>
                    <dt>Campaign status</dt>
                    <dd>
                      {campaignStatus ?? "DRAFT"} — remains DRAFT after copy approval
                    </dd>
                  </div>
                  <div>
                    <dt>Compliance approval</dt>
                    <dd>Still required before launch</dd>
                  </div>
                </>
              ) : null}
              <div>
                <dt>AI audit notes</dt>
                <dd>{approval?.reviewNotes ?? "No review notes recorded"}</dd>
              </div>
            </dl>
            {formUnchangedBySuggestion ? (
              <p className="campaign-builder-step-hint" data-testid="ai-copy-form-unchanged">
                Campaign message form is unchanged until Approve and Apply.
              </p>
            ) : null}
            <AiExplanationDisplay
              explanation={suggestion.explanation}
              confidenceScore={suggestion.confidenceScore}
              storedRecommendationId={suggestion.storedRecommendationId}
            />
            <label>
              Review notes
              <textarea
                aria-label="AI copy review notes"
                value={reviewNotes}
                disabled={isApproved}
                onChange={(event) => onReviewNotesChange(event.target.value)}
              />
            </label>
            <div className="button-row">
              <button
                type="button"
                className="secondary-button"
                disabled={isApproved || busy}
                onClick={onToggleEdit}
              >
                {editing ? "Done editing" : "Edit"}
              </button>
              <button
                type="button"
                className="secondary-button"
                disabled={isApproved || busy}
                onClick={onDismiss}
              >
                Reject/Dismiss
              </button>
              <button
                type="button"
                disabled={!hasStoredRecommendation || isApproved || busy || approving}
                onClick={onRequestApprove}
                aria-label="Approve and Apply AI copy"
              >
                {approving ? "Approving…" : "Approve and Apply"}
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}

function CampaignBuilderSummary({
  form,
  campaign,
  products,
  selectedProductIds,
  segment,
}: {
  form: CampaignFormPayload;
  campaign: CampaignView | null;
  products: ProductView[];
  selectedProductIds: string[];
  segment: SegmentView | undefined;
}) {
  const selectedProducts = products.filter((product) => selectedProductIds.includes(product.id));

  return (
    <dl className="detail-list" aria-label="Campaign builder live summary">
      <div>
        <dt>Status</dt>
        <dd>
          {campaign == null ? "Not created" : <CampaignStatusBadge status={campaign.status} />}
        </dd>
      </div>
      <div>
        <dt>Name</dt>
        <dd>{form.name.trim() || "—"}</dd>
      </div>
      <div>
        <dt>Channel</dt>
        <dd>{formatCampaignEnum(form.channel)}</dd>
      </div>
      <div>
        <dt>Audience</dt>
        <dd>{segment?.name ?? "No segment selected"}</dd>
      </div>
      <div>
        <dt>Products</dt>
        <dd>
          {selectedProducts.length === 0
            ? "No products selected"
            : selectedProducts.map((product) => product.name).join(", ")}
        </dd>
      </div>
      <div>
        <dt>Schedule</dt>
        <dd>
          {form.startDate || "—"} → {form.endDate || "—"}
        </dd>
      </div>
    </dl>
  );
}

function BuilderInput({
  label,
  value,
  error,
  required = false,
  onChange,
}: {
  label: string;
  value: string;
  error?: string;
  required?: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <label>
      {label}
      <input
        required={required}
        aria-label={label}
        value={value}
        aria-invalid={Boolean(error)}
        onChange={(event) => onChange(event.target.value)}
      />
      <FieldError message={error} />
    </label>
  );
}

function FieldError({ message }: { message?: string }) {
  return message == null ? null : <span className="field-error">{message}</span>;
}

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError) ? "You are not authorized to build campaigns." : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Campaign builder action failed." : "";
}
