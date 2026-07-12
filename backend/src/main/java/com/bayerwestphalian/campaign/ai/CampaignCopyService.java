package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KB AI-005 campaign copy decision support.
 *
 * <p>Generated subject/body/call-to-action text is stored for audit and always remains pending
 * human approval (COMP-005 / Sprint 16 critical item 662). This service does not apply copy to live
 * campaigns.
 */
@Service
@Transactional(readOnly = true)
public class CampaignCopyService {

    private static final String TARGET_ENTITY_TYPE = "campaign";
    private static final BigDecimal COPY_CONFIDENCE = new BigDecimal("72.00");

    private final ProductRepository productRepository;
    private final SegmentRepository segmentRepository;
    private final CampaignRepository campaignRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;

    public CampaignCopyService(
            ProductRepository productRepository,
            SegmentRepository segmentRepository,
            CampaignRepository campaignRepository,
            AiRecommendationRepository aiRecommendationRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions) {
        this.productRepository = productRepository;
        this.segmentRepository = segmentRepository;
        this.campaignRepository = campaignRepository;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
    }

    @PreAuthorize("@authz.hasAnyRole('CAMPAIGN_MANAGER')")
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
        return CampaignCopySuggestionView.pending(
                context.campaignId(),
                subject,
                body,
                callToAction,
                explanation,
                COPY_CONFIDENCE,
                saved.getId());
    }

    @PreAuthorize("@authz.hasAnyRole('CAMPAIGN_MANAGER')")
    public boolean requireHumanApproval(CampaignCopySuggestionView suggestion) {
        if (suggestion == null) {
            throw new ValidationException(
                    "Campaign copy suggestion is required",
                    List.of("suggestion: is required for human approval review"));
        }
        return suggestion.requiresHumanApproval();
    }

    @PreAuthorize("@authz.hasAnyRole('CAMPAIGN_MANAGER')")
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

    @PreAuthorize("@authz.hasAnyRole('CAMPAIGN_MANAGER')")
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

        User approver = findUser(authorizationExpressions.currentUserId());
        recommendation.approve(approver, request == null ? null : request.reviewNotes());
        return AiRecommendationView.from(aiRecommendationRepository.save(recommendation));
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
            segment = campaign.getSegment();
        }

        Product product = productFor(request.productName());
        if (segment == null && request.audienceHint() != null) {
            segment = segmentFor(request.audienceHint());
        }

        String productName =
                firstNonBlank(
                        product == null ? null : product.getName(),
                        request.productName(),
                        "the selected product");
        String audience =
                firstNonBlank(
                        segment == null ? null : segment.getName(),
                        request.audienceHint(),
                        "the selected audience");
        CampaignChannel channel =
                request.channel() != null
                        ? request.channel()
                        : campaign == null ? null : campaign.getChannel();
        return new CampaignCopyContext(
                request.campaignId(),
                normalize(request.objective()),
                productName,
                audience,
                channel);
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
        String subject =
                switch (context.channel() == null ? CampaignChannel.EMAIL : context.channel()) {
                    case SMS -> context.productName() + ": quick protection update";
                    case PHONE -> "A timely conversation about " + context.productName();
                    case MIXED -> "A coordinated " + context.productName() + " update";
                    case EMAIL -> "A tailored " + context.productName() + " option for you";
                };
        return truncate(subject, 255);
    }

    private static String bodyFor(CampaignCopyContext context) {
        return "Share a clear, review-ready message with "
                + context.audience()
                + " about "
                + context.productName()
                + ". Focus on "
                + context.objective()
                + " and invite the customer to confirm whether the offer fits their needs. This "
                + "copy is a draft for human review before use.";
    }

    private static String callToActionFor(CampaignCopyContext context) {
        return switch (context.channel() == null ? CampaignChannel.EMAIL : context.channel()) {
            case SMS -> "Reply to review your options";
            case PHONE -> "Schedule a review call";
            case MIXED -> "Choose your preferred next step";
            case EMAIL -> "Review the offer";
        };
    }

    private static String explanationFor(CampaignCopyContext context) {
        return "Rule-based AI-005 draft using campaign objective, product context, audience "
                + "context, and channel. COMP-005 requires human approval before applying or "
                + "sending this copy.";
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
}
