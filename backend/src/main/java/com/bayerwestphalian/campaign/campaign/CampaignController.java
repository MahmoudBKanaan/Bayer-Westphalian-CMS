package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import com.bayerwestphalian.campaign.segment.SegmentPreviewView;
import com.bayerwestphalian.campaign.segment.SegmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for campaign lifecycle (KB CampaignController: list, create, update, submit, approve,
 * reject, launch, pause, complete, archive).
 */
@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    @Autowired(required = false)
    private SegmentService segmentService;

    @Autowired(required = false)
    private CampaignRecipientService campaignRecipientService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /**
     * Campaign list endpoint (KB item 219, FR-050 list/read).
     *
     * <p>{@code GET /api/campaigns} returns campaigns for roles with {@code canReadCampaigns}.
     * Optional query filters: {@code term} (name/objective), {@code ownerUserId}, {@code status},
     * {@code segmentId}.
     */
    @GetMapping
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<List<CampaignView>>> listCampaigns(
            @Valid @ModelAttribute CampaignSearchRequest searchRequest) {
        List<CampaignView> campaigns = campaignService.searchCampaigns(searchRequest.toCriteria());

        return ResponseEntity.ok(ApiResponse.success("Campaigns loaded", campaigns));
    }

    /**
     * Campaign details endpoint (KB item 220).
     *
     * <p>{@code GET /api/campaigns/{id}} returns the full campaign definition for roles with {@code
     * canReadCampaigns}: status, owner, segment, channel, message, schedule, approval fields,
     * rejection reason, and promoted product ids.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> getCampaign(@PathVariable UUID id) {
        CampaignView campaign = campaignService.findById(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign loaded", campaign));
    }

    /**
     * Create draft campaign endpoint (KB FR-050 / FR-057, item 217 / item 243).
     *
     * <p>{@code POST /api/campaigns} accepts name, objective, optional segment and promoted
     * products, channel, message fields, and schedule. Campaign Manager (or Admin) creates the
     * campaign in {@code DRAFT} status owned by the authenticated user. Creation writes a {@code
     * CREATE} audit log for entity type {@code campaigns} (item 233).
     */
    @PostMapping
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request) {
        CampaignView campaign = campaignService.createCampaign(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created", campaign));
    }

    /**
     * Update draft campaign endpoint (KB FR-057 edit draft, item 218).
     *
     * <p>{@code PUT /api/campaigns/{id}} updates name, objective, segment, channel, message,
     * schedule, and optional product set. Only {@code DRAFT} or {@code REJECTED} campaigns can be
     * edited (domain {@code canEdit}); submitted/approved/active campaigns require workflow actions
     * instead of draft updates.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> updateCampaign(
            @PathVariable UUID id, @Valid @RequestBody UpdateCampaignRequest request) {
        CampaignView campaign = campaignService.updateCampaign(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Campaign updated", campaign));
    }

    /**
     * Campaign product selection endpoint (KB FR-052 / item 221).
     *
     * <p>{@code PUT /api/campaigns/{id}/products} replaces the promoted product set for a draft or
     * rejected campaign. Empty {@code productIds} clears all selections.
     */
    @PutMapping("/{id}/products")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> selectCampaignProducts(
            @PathVariable UUID id, @Valid @RequestBody SelectCampaignProductsRequest request) {
        CampaignView campaign = campaignService.selectProducts(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Campaign products updated", campaign));
    }

    /** Lists selected promoted product ids for a campaign (FR-052). */
    @GetMapping("/{id}/products")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<List<UUID>>> listCampaignProducts(@PathVariable UUID id) {
        List<UUID> productIds = campaignService.listSelectedProductIds(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign products loaded", productIds));
    }

    /**
     * Campaign segment selection endpoint (KB FR-053 / item 222).
     *
     * <p>{@code PUT /api/campaigns/{id}/segment} assigns a reusable segment as the campaign target
     * audience, or clears it when {@code segmentId} is null. Only draft/rejected campaigns may
     * change segment selection.
     */
    @PutMapping("/{id}/segment")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> selectCampaignSegment(
            @PathVariable UUID id, @Valid @RequestBody SelectCampaignSegmentRequest request) {
        CampaignView campaign = campaignService.selectSegment(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Campaign segment updated", campaign));
    }

    /** Returns the selected segment id for a campaign (FR-053), or null when none is linked. */
    @GetMapping("/{id}/segment")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<UUID>> getCampaignSegment(@PathVariable UUID id) {
        UUID segmentId = campaignService.getSelectedSegmentId(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign segment loaded", segmentId));
    }

    /**
     * Campaign recipient preview endpoint (KB item 268 / FR-054 / FR-055 / BR-006).
     *
     * <p>{@code GET /api/campaigns/{id}/recipients/preview} returns the campaign-scoped audience
     * preview for the selected segment, with eligibility and exclusion reasons applied before
     * launch.
     */
    @GetMapping("/{id}/recipients/preview")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<SegmentPreviewView>> previewCampaignRecipients(
            @PathVariable UUID id) {
        UUID segmentId = campaignService.getSelectedSegmentId(id);
        SegmentPreviewView preview = segmentService.previewCampaignRecipients(id, segmentId);

        return ResponseEntity.ok(ApiResponse.success("Campaign recipient preview loaded", preview));
    }

    /**
     * Eligible campaign recipient list endpoint (KB item 269).
     *
     * <p>{@code GET /api/campaigns/{id}/recipients/eligible} returns only recipients persisted with
     * {@code ELIGIBLE} status after recipient generation.
     */
    @GetMapping("/{id}/recipients/eligible")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<List<CampaignRecipientView>>> listEligibleRecipients(
            @PathVariable UUID id) {
        List<CampaignRecipientView> recipients =
                campaignRecipientService.listEligibleRecipients(id);

        return ResponseEntity.ok(
                ApiResponse.success("Eligible campaign recipients loaded", recipients));
    }

    /**
     * Excluded campaign recipient list endpoint (KB item 270).
     *
     * <p>{@code GET /api/campaigns/{id}/recipients/excluded} returns only recipients persisted with
     * {@code EXCLUDED} status, including exclusion reasons and explanations.
     */
    @GetMapping("/{id}/recipients/excluded")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<List<CampaignRecipientView>>> listExcludedRecipients(
            @PathVariable UUID id) {
        List<CampaignRecipientView> recipients =
                campaignRecipientService.listExcludedRecipients(id);

        return ResponseEntity.ok(
                ApiResponse.success("Excluded campaign recipients loaded", recipients));
    }

    /**
     * Campaign recipient response summary endpoint (KB item 284).
     *
     * <p>{@code GET /api/campaigns/{id}/recipients/summary} returns persisted counts for eligible,
     * excluded, sent, and failed recipient rows.
     */
    @GetMapping("/{id}/recipients/summary")
    @PreAuthorize("@authz.canReadCampaigns()")
    public ResponseEntity<ApiResponse<CampaignRecipientSummaryView>> summarizeCampaignRecipients(
            @PathVariable UUID id) {
        CampaignRecipientSummaryView summary = campaignRecipientService.summarizeRecipients(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign recipient summary loaded", summary));
    }

    /**
     * Submit campaign endpoint (KB item 228 / FR-058).
     *
     * <p>{@code POST /api/campaigns/{id}/submit} moves a draft or rejected campaign into {@code
     * SUBMITTED} status for compliance review.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> submitCampaign(@PathVariable UUID id) {
        CampaignView campaign = campaignService.submitCampaign(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign submitted", campaign));
    }

    /**
     * Approve campaign endpoint (KB item 229 / FR-059).
     *
     * <p>{@code POST /api/campaigns/{id}/approve} moves a submitted campaign into {@code APPROVED}
     * status. Optional body may include compliance review notes (item 231). Approval writes an
     * {@code APPROVE} audit log for entity type {@code campaigns} (item 235).
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.canReviewCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> approveCampaign(
            @PathVariable UUID id, @RequestBody(required = false) ApproveCampaignRequest request) {
        ApproveCampaignCommand command =
                request == null ? new ApproveCampaignCommand(null) : request.toCommand();
        CampaignView campaign = campaignService.approveCampaign(id, command);

        return ResponseEntity.ok(ApiResponse.success("Campaign approved", campaign));
    }

    /**
     * Reject campaign endpoint (KB item 230 / FR-059).
     *
     * <p>{@code POST /api/campaigns/{id}/reject} moves a submitted campaign into {@code REJECTED}
     * status with a required {@code rejectionReason} (item 232 / KB field). Optional body may
     * include compliance review notes (item 231).
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("@authz.canReviewCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> rejectCampaign(
            @PathVariable UUID id, @Valid @RequestBody RejectCampaignRequest request) {
        CampaignView campaign = campaignService.rejectCampaign(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Campaign rejected", campaign));
    }

    /** Records compliance review notes without changing status (item 231). */
    @PutMapping("/{id}/compliance-review-notes")
    @PreAuthorize("@authz.canReviewCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> recordComplianceReviewNotes(
            @PathVariable UUID id,
            @RequestBody(required = false) RecordComplianceReviewNotesRequest request) {
        String notes = request == null ? null : request.complianceReviewNotes();
        CampaignView campaign = campaignService.recordComplianceReviewNotes(id, notes);

        return ResponseEntity.ok(ApiResponse.success("Compliance review notes recorded", campaign));
    }

    /**
     * Launch campaign endpoint (KB item 278).
     *
     * <p>{@code POST /api/campaigns/{id}/launch} moves an approved campaign into {@code ACTIVE}
     * status through the controlled lifecycle service.
     */
    @PostMapping("/{id}/launch")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> launchCampaign(@PathVariable UUID id) {
        CampaignView campaign = campaignService.launchCampaign(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign launched", campaign));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> pauseCampaign(@PathVariable UUID id) {
        CampaignView campaign = campaignService.pauseCampaign(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign paused", campaign));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> completeCampaign(@PathVariable UUID id) {
        CampaignView campaign = campaignService.completeCampaign(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign completed", campaign));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("@authz.canManageCampaigns()")
    public ResponseEntity<ApiResponse<CampaignView>> archiveCampaign(@PathVariable UUID id) {
        CampaignView campaign = campaignService.archiveCampaign(id);

        return ResponseEntity.ok(ApiResponse.success("Campaign archived", campaign));
    }
}
