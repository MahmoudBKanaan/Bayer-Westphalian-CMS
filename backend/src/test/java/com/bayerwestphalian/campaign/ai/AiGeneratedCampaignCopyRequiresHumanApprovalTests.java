package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sprint 16 critical test item <b>662</b>: AI-generated campaign copy requires human approval.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code AI-005} — Campaign copy suggestion requires human approval before use
 *   <li>{@code COMP-005} — AI suggestions require human review
 *   <li>Items 482 / 500 — generated copy is always pending human approval
 *   <li>Item 501 — approving copy does not approve the campaign lifecycle
 * </ul>
 *
 * <p>Enforcement under test:
 *
 * <ol>
 *   <li>{@link CampaignCopySuggestionView} forces {@code requiresHumanApproval = true}
 *   <li>Generate stores unapproved {@link AiRecommendationType#COPY} rows
 *   <li>{@link CampaignCopyService#requireHumanApproval} always returns true for valid drafts
 *   <li>Human {@link CampaignCopyService#approveCampaignCopy} records approver; no auto-apply
 *   <li>Approval of copy ≠ campaign compliance approval
 * </ol>
 *
 * <p>Companions: {@link CampaignCopyServiceTests}, {@link AiSupportsHumanDecisionMakingOnlyTests}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("662 AI-generated campaign copy requires human approval")
class AiGeneratedCampaignCopyRequiresHumanApprovalTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000662");
    private static final UUID SEGMENT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000662");
    private static final UUID PRODUCT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000662");
    private static final UUID RECOMMENDATION_ID =
            UUID.fromString("80000000-0000-0000-0000-000000000662");
    private static final UUID APPROVER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000662");
    private static final Path AI_LIMITATIONS_DOC =
            Path.of("../docs/modules/ai-limitations-and-human-approval.md");

    @Mock private ProductRepository productRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock
    private com.bayerwestphalian.campaign.campaign.CampaignProductRepository
            campaignProductRepository;
    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;
    @Mock private com.bayerwestphalian.campaign.audit.AuditService auditService;

    private CampaignCopyService campaignCopyService;

    @BeforeEach
    void setUp() {
        campaignCopyService =
                new CampaignCopyService(
                        productRepository,
                        segmentRepository,
                        campaignRepository,
                        campaignProductRepository,
                        aiRecommendationRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Nested
    @DisplayName("DTO: requiresHumanApproval is always forced true")
    class ViewContract {

        @Test
        void campaignCopySuggestionViewForcesRequiresHumanApprovalEvenWhenConstructedFalse() {
            CampaignCopySuggestionView forcedFalse =
                    new CampaignCopySuggestionView(
                            CAMPAIGN_ID,
                            "Subject",
                            "Body",
                            "CTA",
                            "Explanation for human review",
                            new BigDecimal("50.00"),
                            false,
                            true,
                            APPROVER_ID,
                            RECOMMENDATION_ID);

            assertThat(forcedFalse.requiresHumanApproval()).isTrue();
        }

        @Test
        void pendingFactoryStartsUnapprovedWithHumanReviewRequired() {
            CampaignCopySuggestionView pending =
                    CampaignCopySuggestionView.pending(
                            CAMPAIGN_ID,
                            "Subject",
                            "Body",
                            "CTA",
                            "Pending human approval",
                            RECOMMENDATION_ID);

            assertThat(pending.requiresHumanApproval()).isTrue();
            assertThat(pending.humanApproved()).isFalse();
            assertThat(pending.approvedByUserId()).isNull();
            assertThat(pending.storedRecommendationId()).isEqualTo(RECOMMENDATION_ID);
        }
    }

    @Nested
    @DisplayName("Generate: stored COPY recommendation stays pending")
    class GeneratePending {

        @Test
        void generateCopySuggestionAlwaysReturnsPendingHumanApprovalState() {
            Campaign campaign = campaign();
            Product product = product();
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(productRepository.searchByNameOrType("Life Protect")).thenReturn(List.of(product));
            stubRecommendationSave();

            CampaignCopySuggestionView suggestion =
                    campaignCopyService.generateCopySuggestion(
                            new CampaignCopyRequest(
                                    CAMPAIGN_ID,
                                    "cross-sell life cover",
                                    "Life Protect",
                                    CampaignChannel.EMAIL,
                                    "Family guardians"));

            assertThat(suggestion.requiresHumanApproval()).isTrue();
            assertThat(campaignCopyService.requireHumanApproval(suggestion)).isTrue();
            assertThat(suggestion.humanApproved()).isFalse();
            assertThat(suggestion.approvedByUserId()).isNull();
            assertThat(suggestion.explanation()).containsIgnoringCase("human approval");
            assertThat(suggestion.storedRecommendationId()).isEqualTo(RECOMMENDATION_ID);

            verify(aiRecommendationRepository)
                    .save(
                            argThat(
                                    recommendation ->
                                            recommendation.getRecommendationType()
                                                            == AiRecommendationType.COPY
                                                    && !recommendation.isApproved()
                                                    && recommendation.getApprovedBy() == null
                                                    && recommendation
                                                            .getExplanation()
                                                            .toLowerCase()
                                                            .contains("human approval")));
        }

        @Test
        void requireHumanApprovalRejectsNullAndReturnsTrueForDrafts() {
            CampaignCopySuggestionView suggestion =
                    CampaignCopySuggestionView.pending(
                            CAMPAIGN_ID,
                            "Subject",
                            "Body",
                            "Review",
                            "Generated for human approval",
                            RECOMMENDATION_ID);

            assertThat(campaignCopyService.requireHumanApproval(suggestion)).isTrue();
            assertThatExceptionOfType(ValidationException.class)
                    .isThrownBy(() -> campaignCopyService.requireHumanApproval(null))
                    .withMessageContaining("Campaign copy suggestion is required");
        }

        @Test
        void entityApproveRequiresHumanUserPrincipal() {
            AiRecommendation recommendation =
                    AiRecommendation.create(
                            AiRecommendationType.COPY,
                            "campaign",
                            CAMPAIGN_ID,
                            "objective",
                            "Draft subject",
                            "Requires human review (COMP-005)");

            assertThatThrownBy(() -> recommendation.approve(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Approver");
            assertThat(recommendation.isApproved()).isFalse();
        }
    }

    @Nested
    @DisplayName("Human approve: records review only")
    class HumanApprove {

        @Test
        void approveCampaignCopyRequiresHumanApproverAndDoesNotApproveCampaign() {
            Campaign campaign = campaign();
            ReflectionTestUtils.setField(campaign, "status", CampaignStatus.SUBMITTED);
            AiRecommendation recommendation = copyRecommendation();
            User approver = approver();

            when(aiRecommendationRepository.findById(RECOMMENDATION_ID))
                    .thenReturn(Optional.of(recommendation));
            when(authorizationExpressions.currentUserId()).thenReturn(APPROVER_ID);
            when(userRepository.findById(APPROVER_ID)).thenReturn(Optional.of(approver));
            when(aiRecommendationRepository.save(recommendation)).thenReturn(recommendation);

            AiRecommendationView view =
                    campaignCopyService.approveCampaignCopy(
                            RECOMMENDATION_ID,
                            new ApproveAiRecommendationRequest("Copy reviewed by CM"));

            assertThat(view.approved()).isTrue();
            assertThat(view.recommendationType()).isEqualTo(AiRecommendationType.COPY);
            assertThat(view.approvedByUserId()).isEqualTo(APPROVER_ID);
            assertThat(view.reviewNotes()).isEqualTo("Copy reviewed by CM");

            // Item 501 / 662: human copy approval ≠ campaign compliance approval.
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            assertThat(campaign.getApprovedBy()).isNull();
            assertThat(campaign.getApprovedAt()).isNull();
            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        void approveCampaignCopyIsRestrictedToCampaignManagerRole() throws Exception {
            Method approve =
                    CampaignCopyService.class.getMethod(
                            "approveCampaignCopy",
                            UUID.class,
                            ApproveAiRecommendationRequest.class);
            PreAuthorize preAuthorize = approve.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).contains("CAMPAIGN_MANAGER");
            assertThat(preAuthorize.value()).contains("ADMIN");

            Method generate =
                    CampaignCopyService.class.getMethod(
                            "generateCopySuggestion", CampaignCopyRequest.class);
            assertThat(generate.getAnnotation(PreAuthorize.class).value())
                    .contains("CAMPAIGN_MANAGER")
                    .contains("ADMIN");
        }
    }

    @Nested
    @DisplayName("Policy documentation (AI-005 / COMP-005)")
    class PolicyDocs {

        @Test
        void limitationsDocStatesCampaignCopyAlwaysRequiresHumanApproval() throws Exception {
            String policy = Files.readString(AI_LIMITATIONS_DOC);
            assertThat(policy)
                    .contains("662")
                    .contains("AiGeneratedCampaignCopyRequiresHumanApprovalTests")
                    .contains("AI-005")
                    .contains("requiresHumanApproval")
                    .contains("approveCampaignCopy")
                    .containsIgnoringCase("human approval");
        }
    }

    private void stubRecommendationSave() {
        when(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .thenAnswer(
                        invocation -> {
                            AiRecommendation recommendation = invocation.getArgument(0);
                            ReflectionTestUtils.setField(
                                    recommendation, "id", RECOMMENDATION_ID);
                            ReflectionTestUtils.setField(
                                    recommendation, "createdAt", Instant.parse("2026-07-12T12:00:00Z"));
                            return recommendation;
                        });
    }

    private static AiRecommendation copyRecommendation() {
        AiRecommendation recommendation =
                AiRecommendation.create(
                        AiRecommendationType.COPY,
                        "campaign",
                        CAMPAIGN_ID,
                        "objective",
                        "Subject: Subject line\nBody: Draft body for human review\nCall to action: Request more information",
                        "Generated for human review only");
        ReflectionTestUtils.setField(recommendation, "id", RECOMMENDATION_ID);
        ReflectionTestUtils.setField(recommendation, "createdAt", Instant.now());
        return recommendation;
    }

    private static Campaign campaign() {
        Segment segment =
                Segment.create(
                        "Family guardians",
                        "Audience for copy test",
                        null,
                        SegmentVisibility.GLOBAL);
        ReflectionTestUtils.setField(segment, "id", SEGMENT_ID);
        Campaign campaign =
                Campaign.create(
                        "Life protect draft",
                        "cross-sell life cover",
                        null,
                        segment,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Product product() {
        Product product =
                Product.create(
                        "Life Protect", ProductType.LIFE_INSURANCE, new BigDecimal("99.00"), 12);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }

    private static User approver() {
        User user =
                User.create(
                        "cm.662@bayer-westphalian.test",
                        "{noop}x",
                        "Campaign Copy Approver");
        ReflectionTestUtils.setField(user, "id", APPROVER_ID);
        return user;
    }
}
