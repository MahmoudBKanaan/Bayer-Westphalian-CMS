package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** KB AI endpoints, including AI-001 search and AI-005 campaign copy (items 485-488). */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiSearchService aiSearchService;
    private final AiRecommendationService aiRecommendationService;
    private final CampaignCopyService campaignCopyService;

    public AiController(
            AiSearchService aiSearchService,
            AiRecommendationService aiRecommendationService,
            CampaignCopyService campaignCopyService) {
        this.aiSearchService = aiSearchService;
        this.aiRecommendationService = aiRecommendationService;
        this.campaignCopyService = campaignCopyService;
    }

    @GetMapping("/customer-search")
    @PreAuthorize("@authz.canReadCustomers()")
    public ResponseEntity<ApiResponse<AiCustomerSearchView>> customerSearch(
            @RequestParam("q") String query,
            @RequestParam(name = "limit", required = false) Integer limit) {
        AiCustomerSearchRequest request = new AiCustomerSearchRequest(query, limit);
        AiCustomerSearchView results =
                aiSearchService.weightedSearch(request.query(), request.effectiveLimit());
        return ResponseEntity.ok(ApiResponse.success("AI customer search completed", results));
    }

    @PostMapping("/segment-suggestions")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<SegmentSuggestionView.ListResponse>> segmentSuggestions(
            @Valid @RequestBody(required = false) SegmentSuggestionRequest request) {
        SegmentSuggestionView.ListResponse suggestions =
                aiRecommendationService.suggestSegments(request);
        return ResponseEntity.ok(
                ApiResponse.success("AI segment suggestions generated", suggestions));
    }

    @PostMapping("/product-recommendations")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<ProductRecommendationView.ListResponse>>
            productRecommendations(@Valid @RequestBody ProductRecommendationRequest request) {
        ProductRecommendationView.ListResponse recommendations =
                aiRecommendationService.recommendProducts(request);
        return ResponseEntity.ok(
                ApiResponse.success("AI product recommendations generated", recommendations));
    }

    /**
     * Default-risk score (KB AI-004) — advisory only; does not block marketing by itself.
     */
    @PostMapping("/default-risk-score")
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public ResponseEntity<ApiResponse<DefaultRiskScoreView>> defaultRiskScore(
            @Valid @RequestBody DefaultRiskScoreRequest request) {
        DefaultRiskScoreView score = aiRecommendationService.calculateDefaultRisk(request);
        return ResponseEntity.ok(
                ApiResponse.success("AI default-risk score generated", score));
    }

    @PostMapping("/duplicate-contact-warning")
    @PreAuthorize("@authz.canReadCustomers()")
    public ResponseEntity<ApiResponse<DuplicateContactRiskView>> duplicateContactWarning(
            @Valid @RequestBody DuplicateContactRiskRequest request) {
        DuplicateContactRiskView warning = aiRecommendationService.detectDuplicateRisk(request);
        return ResponseEntity.ok(
                ApiResponse.success("AI duplicate-contact warning generated", warning));
    }

    @PostMapping("/campaign-copy")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<CampaignCopySuggestionView>> campaignCopy(
            @Valid @RequestBody CampaignCopyRequest request) {
        CampaignCopySuggestionView suggestion = campaignCopyService.generateCopySuggestion(request);
        return ResponseEntity.ok(
                ApiResponse.success("AI campaign copy suggestion generated", suggestion));
    }

    @PostMapping("/campaign-copy/{recommendationId}/approve")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<AiRecommendationView>> approveCampaignCopy(
            @PathVariable UUID recommendationId,
            @Valid @RequestBody(required = false) ApproveAiRecommendationRequest request) {
        AiRecommendationView approved =
                campaignCopyService.approveCampaignCopy(recommendationId, request);
        return ResponseEntity.ok(ApiResponse.success("AI campaign copy approved", approved));
    }
}
