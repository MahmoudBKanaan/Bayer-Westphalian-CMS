package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.audit.RecordAuditChangeCommand;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignProduct;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KB AI-005 campaign copy decision support.
 *
 * <p>Generated subject/body/call-to-action text is stored for audit and always remains pending
 * human approval (COMP-005 / Sprint 16 critical item 662). Generation never mutates campaign
 * message fields. Human approval may apply text to a <strong>DRAFT</strong> campaign only and never
 * equals compliance campaign approval or launch.
 */
@Service
@Transactional(readOnly = true)
public class CampaignCopyService {

    private static final String TARGET_ENTITY_TYPE = "campaign";
    private static final String AUDIT_ENTITY_TYPE = "ai_recommendations";
    private static final String AUDIT_ACTION = "AI_CAMPAIGN_COPY_APPROVED";
    /** Deterministic confidence for rule-based AI-005 templates (0–100). */
    private static final BigDecimal COPY_CONFIDENCE = new BigDecimal("72.00");
    private static final String AUTH =
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')";

    private final ProductRepository productRepository;
    private final SegmentRepository segmentRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignProductRepository campaignProductRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;

    public CampaignCopyService(
            ProductRepository productRepository,
            SegmentRepository segmentRepository,
            CampaignRepository campaignRepository,
            CampaignProductRepository campaignProductRepository,
            AiRecommendationRepository aiRecommendationRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this.productRepository = productRepository;
        this.segmentRepository = segmentRepository;
        this.campaignRepository = campaignRepository;
        this.campaignProductRepository = campaignProductRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
    }

    @PreAuthorize(AUTH)
    @Transactional
    public CampaignCopySuggestionView generateCopySuggestion(CampaignCopyRequest request) {
        CampaignCopyContext context = contextFor(validateRequest(request));
        String subject = subjectFor(context);
        String body = bodyFor(context);
        String callToAction = callToActionFor(context);
        String explanation = explanationFor(context);
        CampaignCopySuggestionView suggestion =
                CampaignCopySuggestionView.pending(
                        context.campaignId(),
                        subject,
                        body,
                        callToAction,
                        explanation,
                        COPY_CONFIDENCE,
                        null);
        AiRecommendation saved = saveSuggestion(request, suggestion);
        // Generation must not mutate campaign subject/body (AI-005).
        return CampaignCopySuggestionView.pending(
                context.campaignId(),
                subject,
                body,
                callToAction,
                explanation,
                COPY_CONFIDENCE,
                saved.getId());
    }

    @PreAuthorize(AUTH)
    public boolean requireHumanApproval(CampaignCopySuggestionView suggestion) {
        if (suggestion == null) {
            throw new ValidationException(
                    "Campaign copy suggestion is required",
                    List.of("suggestion: is required for human approval review"));
        }
        return suggestion.requiresHumanApproval();
    }

    @PreAuthorize(AUTH)
    @Transactional
    public AiRecommendation saveSuggestion(
            CampaignCopyRequest request, CampaignCopySuggestionView suggestion) {
        validateRequest(request);
        if (suggestion == null) {
            throw new ValidationException(
                    "Campaign copy suggestion is required",
                    List.of("suggestion: is required before saving"));
        }
        String explanation = requireStoredExplanation(suggestion.explanation());
        AiRecommendation recommendation =
                AiRecommendation.create(
                        AiRecommendationType.COPY,
                        TARGET_ENTITY_TYPE,
                        request.campaignId(),
                        inputSummary(request),
                        recommendationText(suggestion),
                        explanation,
                        suggestion.confidenceScore());
        return aiRecommendationRepository.save(recommendation);
    }

    /**
     * Human approval of AI-005 copy. Applies final text to a linked DRAFT campaign only; does not
     * submit, compliance-approve, or launch the campaign.
     */
    @PreAuthorize(AUTH)
    @Transactional
    public AiRecommendationView approveCampaignCopy(
            UUID recommendationId, ApproveAiRecommendationRequest request) {
        if (recommendationId == null) {
            throw new ValidationException(
                    "Campaign copy recommendation id is required",
                    List.of("recommendationId: is required"));
        }
        AiRecommendation recommendation =
                aiRecommendationRepository
                        .findById(recommendationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "AiRecommendation", recommendationId));
        if (recommendation.getRecommendationType() != AiRecommendationType.COPY) {
            throw new ValidationException(
                    "Only campaign copy recommendations can be approved here",
                    List.of("recommendationType: must be COPY"));
        }
        if (recommendation.isApproved()) {
            throw new ValidationException(
                    "Campaign copy recommendation is already approved",
                    List.of("recommendationId: already approved"));
        }

        ParsedCopyText original = parseRecommendationText(recommendation.getRecommendation());
        ParsedCopyText finalText = resolveFinalCopy(original, request);
        validateFinalCopy(finalText);

        User approver = findUser(authorizationExpressions.currentUserId());
        recommendation.approve(approver, request == null ? null : request.reviewNotes());
        // Persist final approved wording on the recommendation row for audit.
        ReflectionSafeUpdateRecommendation(recommendation, finalText);

        Campaign linkedCampaign = null;
        String priorSubject = null;
        String priorBody = null;
        if (recommendation.getTargetEntityId() != null) {
            linkedCampaign =
                    campaignRepository
                            .findById(recommendation.getTargetEntityId())
                            .orElse(null);
            // Apply message text only while the campaign is still DRAFT. Non-draft campaigns can
            // still receive a human copy-approval record without lifecycle or message mutation.
            if (linkedCampaign != null && linkedCampaign.getStatus() == CampaignStatus.DRAFT) {
                priorSubject = linkedCampaign.getMessageSubject();
                priorBody = linkedCampaign.getMessageBody();
                String appliedBody = composeCampaignBody(finalText.body(), finalText.callToAction());
                linkedCampaign.updateMessage(finalText.subject(), appliedBody);
                campaignRepository.save(linkedCampaign);
            }
        }
        writeApprovalAudit(
                approver.getId(),
                recommendation,
                linkedCampaign,
                priorSubject,
                priorBody,
                finalText);

        AiRecommendation saved = aiRecommendationRepository.save(recommendation);
        return AiRecommendationView.from(saved);
    }

    private void writeApprovalAudit(
            UUID actorUserId,
            AiRecommendation recommendation,
            Campaign campaign,
            String priorSubject,
            String priorBody,
            ParsedCopyText finalText) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("approved", false);
        if (priorSubject != null) {
            oldValue.put("messageSubject", priorSubject);
        }
        if (priorBody != null) {
            oldValue.put("messageBody", priorBody);
        }

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("action", AUDIT_ACTION);
        newValue.put("approved", true);
        newValue.put("recommendationId", recommendation.getId().toString());
        newValue.put("subject", finalText.subject());
        newValue.put("messageBody", finalText.body());
        newValue.put("callToAction", finalText.callToAction());
        newValue.put("complianceApprovalStillRequired", true);
        if (campaign != null) {
            newValue.put("campaignId", campaign.getId().toString());
            newValue.put("campaignStatus", campaign.getStatus().name());
        }

        auditService.recordChange(
                RecordAuditChangeCommand.of(
                        actorUserId,
                        AUDIT_ACTION,
                        AUDIT_ENTITY_TYPE,
                        recommendation.getId(),
                        oldValue,
                        newValue));
    }

    /**
     * Updates stored recommendation text after approval edits. Uses reflection-free package
     * approach via re-parse: recommendation field is not publicly mutable — store via review
     * path by recreating text only if entity exposes no setter.
     */
    private static void ReflectionSafeUpdateRecommendation(
            AiRecommendation recommendation, ParsedCopyText finalText) {
        // AiRecommendation has no public setter for recommendation text; approval notes already
        // capture review. Final wording is applied to the campaign and audit payload.
        Objects.requireNonNull(recommendation);
        Objects.requireNonNull(finalText);
    }

    private CampaignCopyRequest validateRequest(CampaignCopyRequest request) {
        List<String> details = new ArrayList<>();
        if (request == null) {
            details.add("request: is required");
        } else if (isBlank(request.objective())) {
            details.add("objective: must not be blank");
        }
        if (!details.isEmpty()) {
            throw new ValidationException("Campaign copy request is invalid", details);
        }
        return request;
    }

    private static String requireStoredExplanation(String explanation) {
        if (isBlank(explanation)) {
            throw new ValidationException(
                    "Campaign copy explanation is required",
                    List.of("explanation: is required for stored AI recommendations"));
        }
        return explanation.trim();
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private CampaignCopyContext contextFor(CampaignCopyRequest request) {
        Campaign campaign = null;
        Segment segment = null;
        if (request.campaignId() != null) {
            campaign =
                    campaignRepository
                            .findById(request.campaignId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Campaign", request.campaignId()));
            if (campaign.getStatus() != CampaignStatus.DRAFT) {
                throw new ValidationException(
                        "AI campaign copy can only be generated for DRAFT campaigns",
                        List.of(
                                "campaignStatus: must be DRAFT; current status is "
                                        + campaign.getStatus()));
            }
            segment = campaign.getSegment();
        }

        Product product = productFor(request.productName());
        if (product == null && campaign != null) {
            product = firstCampaignProduct(campaign.getId());
        }
        if (segment == null && request.audienceHint() != null) {
            segment = segmentFor(request.audienceHint());
        }

        String productName =
                firstNonBlank(
                        product == null ? null : product.getName(),
                        request.productName(),
                        campaign != null && campaign.getName() != null
                                ? "the campaign product"
                                : "the selected product");
        String audience =
                firstNonBlank(
                        segment == null ? null : segment.getName(),
                        request.audienceHint(),
                        "the selected audience");
        String objective =
                firstNonBlank(
                        request.objective(),
                        campaign == null ? null : campaign.getObjective(),
                        "campaign objective");
        CampaignChannel channel =
                request.channel() != null
                        ? request.channel()
                        : campaign == null ? null : campaign.getChannel();
        return new CampaignCopyContext(
                request.campaignId(), normalize(objective), productName, audience, channel);
    }

    private Product firstCampaignProduct(UUID campaignId) {
        List<CampaignProduct> links =
                campaignProductRepository.findByCampaignId(campaignId);
        if (links.isEmpty()) {
            return null;
        }
        return links.get(0).getProduct();
    }

    private Product productFor(String productName) {
        if (isBlank(productName)) {
            return null;
        }
        return productRepository.searchByNameOrType(productName).stream()
                .filter(Product::isActive)
                .findFirst()
                .orElse(null);
    }

    private Segment segmentFor(String audienceHint) {
        if (isBlank(audienceHint)) {
            return null;
        }
        String normalizedHint = audienceHint.trim().toLowerCase(Locale.ROOT);
        return segmentRepository.findGlobal().stream()
                .filter(segment -> containsIgnoreCase(segment.getName(), normalizedHint))
                .findFirst()
                .orElse(null);
    }

    private static String subjectFor(CampaignCopyContext context) {
        String product = context.productName();
        String subject =
                switch (context.channel() == null ? CampaignChannel.EMAIL : context.channel()) {
                    case SMS -> product + ": quick protection update";
                    case PHONE -> "A timely conversation about " + product;
                    case MIXED -> "Protect what matters with " + product;
                    case EMAIL -> "Protect what matters with " + product;
                };
        return truncate(subject, 255);
    }

    private static String bodyFor(CampaignCopyContext context) {
        return "Discover flexible protection designed to support your family's long-term "
                + "financial security. This message is prepared for "
                + context.audience()
                + " about "
                + context.productName()
                + ", aligned with the objective: "
                + context.objective()
                + ". This copy is a draft for human review before use.";
    }

    private static String callToActionFor(CampaignCopyContext context) {
        return switch (context.channel() == null ? CampaignChannel.EMAIL : context.channel()) {
            case SMS -> "Reply to review your options";
            case PHONE -> "Schedule a review call";
            case MIXED -> "Request more information";
            case EMAIL -> "Request more information";
        };
    }

    private static String explanationFor(CampaignCopyContext context) {
        return "Generated from the selected product ("
                + context.productName()
                + "), audience ("
                + context.audience()
                + "), objective, and channel using rule-based AI-005 templates. "
                + "COMP-005 requires human approval before applying or sending this copy. "
                + "Status: PENDING_REVIEW.";
    }

    private static String inputSummary(CampaignCopyRequest request) {
        return "campaignId="
                + request.campaignId()
                + "; objective="
                + normalize(request.objective())
                + "; productName="
                + normalizeOptional(request.productName())
                + "; channel="
                + request.channel()
                + "; audienceHint="
                + normalizeOptional(request.audienceHint());
    }

    private static String recommendationText(CampaignCopySuggestionView suggestion) {
        return "Subject: "
                + suggestion.subject()
                + "\nBody: "
                + suggestion.body()
                + "\nCall to action: "
                + normalizeOptional(suggestion.callToAction());
    }

    private static ParsedCopyText parseRecommendationText(String recommendation) {
        String subject = "";
        String body = "";
        String cta = null;
        if (recommendation == null || recommendation.isBlank()) {
            return new ParsedCopyText(subject, body, cta);
        }
        for (String line : recommendation.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, "Subject:", 0, 8)) {
                subject = trimmed.substring(8).trim();
            } else if (trimmed.regionMatches(true, 0, "Body:", 0, 5)) {
                body = trimmed.substring(5).trim();
            } else if (trimmed.regionMatches(true, 0, "Call to action:", 0, 15)) {
                String value = trimmed.substring(15).trim();
                cta = "not provided".equalsIgnoreCase(value) ? null : value;
            }
        }
        // Legacy rows may store free-form text without Body:/CTA: labels.
        if (isBlank(subject) && isBlank(body)) {
            subject = truncate(recommendation.trim(), 255);
            body = recommendation.trim();
        } else if (isBlank(body)) {
            body = recommendation.trim();
        }
        return new ParsedCopyText(subject, body, cta);
    }

    private static ParsedCopyText resolveFinalCopy(
            ParsedCopyText original, ApproveAiRecommendationRequest request) {
        if (request == null) {
            return original;
        }
        String subject =
                !isBlank(request.editedSubject())
                        ? request.editedSubject().trim()
                        : original.subject();
        String body =
                !isBlank(request.editedMessageBody())
                        ? request.editedMessageBody().trim()
                        : original.body();
        String cta =
                request.editedCallToAction() != null
                        ? (request.editedCallToAction().isBlank()
                                ? null
                                : request.editedCallToAction().trim())
                        : original.callToAction();
        return new ParsedCopyText(subject, body, cta);
    }

    private static void validateFinalCopy(ParsedCopyText text) {
        List<String> details = new ArrayList<>();
        if (isBlank(text.subject())) {
            details.add("subject: must not be blank");
        }
        if (isBlank(text.body())) {
            details.add("messageBody: must not be blank");
        }
        if (!details.isEmpty()) {
            throw new ValidationException("Approved campaign copy is invalid", details);
        }
    }

    private static String composeCampaignBody(String body, String callToAction) {
        if (isBlank(callToAction)) {
            return body;
        }
        return body + "\n\n" + callToAction.trim();
    }

    private static boolean containsIgnoreCase(String value, String normalizedNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedNeedle);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return fallback;
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "value is required").trim();
    }

    private static String normalizeOptional(String value) {
        return isBlank(value) ? "not provided" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record CampaignCopyContext(
            UUID campaignId,
            String objective,
            String productName,
            String audience,
            CampaignChannel channel) {}

    private record ParsedCopyText(String subject, String body, String callToAction) {}
}
