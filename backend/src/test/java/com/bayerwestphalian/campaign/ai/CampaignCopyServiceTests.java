package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * KB item 480: Implement CampaignCopyService for AI-005 copy suggestions.
 *
 * <p>Sprint 16 critical restatement: item <b>662</b> — {@link
 * AiGeneratedCampaignCopyRequiresHumanApprovalTests}.
 */
@DisplayName("480 Implement CampaignCopyService")
class CampaignCopyServiceTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000480");
    private static final UUID SEGMENT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000480");
    private static final UUID PRODUCT_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000480");
    private static final UUID RECOMMENDATION_ID =
            UUID.fromString("80000000-0000-0000-0000-000000000480");
    private static final UUID APPROVER_ID =
            UUID.fromString("90000000-0000-0000-0000-000000000482");

    private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);
    private final SegmentRepository segmentRepository = Mockito.mock(SegmentRepository.class);
    private final CampaignRepository campaignRepository = Mockito.mock(CampaignRepository.class);
    private final AiRecommendationRepository aiRecommendationRepository =
            Mockito.mock(AiRecommendationRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AuthorizationExpressions authorizationExpressions =
            Mockito.mock(AuthorizationExpressions.class);
    private final com.bayerwestphalian.campaign.campaign.CampaignProductRepository
            campaignProductRepository =
                    Mockito.mock(
                            com.bayerwestphalian.campaign.campaign.CampaignProductRepository.class);
    private final com.bayerwestphalian.campaign.audit.AuditService auditService =
            Mockito.mock(com.bayerwestphalian.campaign.audit.AuditService.class);
    private final CampaignCopyService service =
            new CampaignCopyService(
                    productRepository,
                    segmentRepository,
                    campaignRepository,
                    campaignProductRepository,
                    aiRecommendationRepository,
                    userRepository,
                    authorizationExpressions,
                    auditService);

    @Test
    void declaresKbServiceContractAndCampaignManagerAuthorization() throws Exception {
        assertThat(CampaignCopyService.class.getAnnotation(Service.class)).isNotNull();
        Transactional transactional = CampaignCopyService.class.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();

        assertMethodAuthorization(
                "generateCopySuggestion",
                new Class<?>[] {CampaignCopyRequest.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertMethodAuthorization(
                "requireHumanApproval",
                new Class<?>[] {CampaignCopySuggestionView.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertMethodAuthorization(
                "saveSuggestion",
                new Class<?>[] {CampaignCopyRequest.class, CampaignCopySuggestionView.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertMethodAuthorization(
                "approveCampaignCopy",
                new Class<?>[] {UUID.class, ApproveAiRecommendationRequest.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
    }

    @Test
    void generateCopySuggestionBuildsDraftAndStoresCopyRecommendation() {
        Campaign campaign = campaign(segment());
        Product product = product();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(productRepository.searchByNameOrType("Life Protect")).thenReturn(List.of(product));
        stubRecommendationSave();

        CampaignCopySuggestionView suggestion =
                service.generateCopySuggestion(
                        new CampaignCopyRequest(
                                CAMPAIGN_ID,
                                "cross-sell life cover",
                                "Life Protect",
                                CampaignChannel.EMAIL,
                                "Family guardians"));

        assertThat(suggestion.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(suggestion.subject()).contains("Life Protect");
        assertThat(suggestion.body())
                .contains("Family guardians", "Life Protect", "cross-sell life cover");
        assertThat(suggestion.callToAction()).isEqualTo("Request more information");
        assertThat(suggestion.explanation()).contains("AI-005", "human approval");
        assertThat(suggestion.confidenceScore()).isEqualByComparingTo("72.00");
        assertThat(suggestion.requiresHumanApproval()).isTrue();
        assertThat(suggestion.humanApproved()).isFalse();
        assertThat(suggestion.storedRecommendationId()).isEqualTo(RECOMMENDATION_ID);
        verify(aiRecommendationRepository)
                .save(
                        argThat(
                                recommendation ->
                                        recommendation.getRecommendationType()
                                                        == AiRecommendationType.COPY
                                                && "campaign"
                                                        .equals(
                                                                recommendation
                                                                        .getTargetEntityType())
                                                && CAMPAIGN_ID.equals(
                                                        recommendation.getTargetEntityId())
                                                && recommendation
                                                        .getRecommendation()
                                                        .contains("Subject:")
                                                && recommendation
                                                        .getInputSummary()
                                                        .contains("Life Protect")
                                                && recommendation
                                                        .getExplanation()
                                                        .contains("COMP-005")
                                                && recommendation
                                                        .getConfidenceScore()
                                                        .compareTo(new BigDecimal("72.00"))
                                                        == 0));
    }

    @Test
    void requireHumanApprovalReturnsForcedReviewFlag() {
        CampaignCopySuggestionView suggestion =
                CampaignCopySuggestionView.pending(
                        CAMPAIGN_ID,
                        "Subject",
                        "Body",
                        "Review",
                        "Generated for human approval",
                        RECOMMENDATION_ID);

        assertThat(service.requireHumanApproval(suggestion)).isTrue();

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.requireHumanApproval(null))
                .withMessageContaining("Campaign copy suggestion is required");
    }

    @Test
    @DisplayName("500 Campaign copy suggestion requires human approval")
    void generateCopySuggestionAlwaysReturnsPendingHumanApprovalState() {
        Campaign campaign = campaign(segment());
        Product product = product();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(productRepository.searchByNameOrType("Life Protect")).thenReturn(List.of(product));
        stubRecommendationSave();

        CampaignCopySuggestionView suggestion =
                service.generateCopySuggestion(
                        new CampaignCopyRequest(
                                CAMPAIGN_ID,
                                "retain families",
                                "Life Protect",
                                CampaignChannel.MIXED,
                                "Family guardians"));

        assertThat(suggestion.requiresHumanApproval()).isTrue();
        assertThat(service.requireHumanApproval(suggestion)).isTrue();
        assertThat(suggestion.humanApproved()).isFalse();
        assertThat(suggestion.approvedByUserId()).isNull();
        assertThat(suggestion.explanation()).contains("COMP-005 requires human approval");
        assertThat(suggestion.storedRecommendationId()).isEqualTo(RECOMMENDATION_ID);
        verify(aiRecommendationRepository)
                .save(
                        argThat(
                                recommendation ->
                                        recommendation.getRecommendationType()
                                                        == AiRecommendationType.COPY
                                                && !recommendation.isApproved()
                                                && recommendation.getApprovedBy() == null
                                                && recommendation.getReviewNotes() == null
                                                && recommendation
                                                        .getExplanation()
                                                        .contains("human approval")));
    }

    @Test
    void saveSuggestionStoresHumanReviewOnlyCopyRecommendation() {
        CampaignCopyRequest request =
                new CampaignCopyRequest(
                        CAMPAIGN_ID,
                        "renew expiring cover",
                        "Life Protect",
                        CampaignChannel.SMS,
                        "Renewal audience");
        CampaignCopySuggestionView suggestion =
                CampaignCopySuggestionView.pending(
                        CAMPAIGN_ID,
                        "Life Protect: quick protection update",
                        "Body draft for review",
                        "Reply to review your options",
                        "COMP-005 requires human approval",
                        new BigDecimal("88.50"),
                        null);
        stubRecommendationSave();

        AiRecommendation saved = service.saveSuggestion(request, suggestion);

        assertThat(saved.getId()).isEqualTo(RECOMMENDATION_ID);
        assertThat(saved.getRecommendationType()).isEqualTo(AiRecommendationType.COPY);
        assertThat(saved.getApprovedBy()).isNull();
        assertThat(saved.getRecommendation()).contains("Call to action:");
        assertThat(saved.getExplanation()).isEqualTo("COMP-005 requires human approval");
        assertThat(saved.getConfidenceScore()).isEqualByComparingTo("88.50");
    }

    @Test
    void saveSuggestionRequiresExplanationForStoredRecommendation() {
        CampaignCopyRequest request =
                new CampaignCopyRequest(
                        CAMPAIGN_ID,
                        "renew expiring cover",
                        "Life Protect",
                        CampaignChannel.SMS,
                        "Renewal audience");
        CampaignCopySuggestionView suggestion =
                CampaignCopySuggestionView.pending(
                        CAMPAIGN_ID,
                        "Life Protect: quick protection update",
                        "Body draft for review",
                        "Reply to review your options",
                        " ",
                        null);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.saveSuggestion(request, suggestion))
                .withMessageContaining("Campaign copy explanation is required");
    }

    @Test
    void generateCopySuggestionValidatesObjectiveAndCampaignId() {
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(
                        () ->
                                service.generateCopySuggestion(
                                        new CampaignCopyRequest(
                                                CAMPAIGN_ID,
                                                " ",
                                                null,
                                                CampaignChannel.EMAIL,
                                                null)))
                .withMessageContaining("Campaign copy request is invalid");

        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(
                        () ->
                                service.generateCopySuggestion(
                                        new CampaignCopyRequest(
                                                CAMPAIGN_ID,
                                                "retain members",
                                                null,
                                                CampaignChannel.EMAIL,
                                                null)))
                .withMessageContaining("Campaign was not found");
    }

    @Test
    void approveCampaignCopyRequiresHumanApproverAndStoresApproval() {
        AiRecommendation recommendation = copyRecommendation();
        User approver = approver();
        when(aiRecommendationRepository.findById(RECOMMENDATION_ID))
                .thenReturn(Optional.of(recommendation));
        when(authorizationExpressions.currentUserId()).thenReturn(APPROVER_ID);
        when(userRepository.findById(APPROVER_ID)).thenReturn(Optional.of(approver));
        when(aiRecommendationRepository.save(recommendation)).thenReturn(recommendation);

        AiRecommendationView view =
                service.approveCampaignCopy(
                        RECOMMENDATION_ID, new ApproveAiRecommendationRequest("Reviewed by CM"));

        assertThat(view.id()).isEqualTo(RECOMMENDATION_ID);
        assertThat(view.recommendationType()).isEqualTo(AiRecommendationType.COPY);
        assertThat(view.approved()).isTrue();
        assertThat(view.approvedByUserId()).isEqualTo(APPROVER_ID);
        assertThat(view.approvedByFullName()).isEqualTo("Campaign Copy Approver");
        assertThat(view.reviewNotes()).isEqualTo("Reviewed by CM");
        assertThat(recommendation.getReviewNotes()).isEqualTo("Reviewed by CM");
        assertThat(view.explanation()).isEqualTo("Generated for human review only");
        verify(aiRecommendationRepository).save(recommendation);
        verify(auditService).recordChange(any());
    }

    @Test
    void approveCampaignCopyAppliesMessageToDraftCampaignOnly() {
        Segment segment = segment();
        Campaign campaign = campaign(segment);
        AiRecommendation recommendation = copyRecommendation();
        User approver = approver();
        when(aiRecommendationRepository.findById(RECOMMENDATION_ID))
                .thenReturn(Optional.of(recommendation));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(APPROVER_ID);
        when(userRepository.findById(APPROVER_ID)).thenReturn(Optional.of(approver));
        when(aiRecommendationRepository.save(recommendation)).thenReturn(recommendation);
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        service.approveCampaignCopy(
                RECOMMENDATION_ID, new ApproveAiRecommendationRequest("Apply to draft"));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(campaign.getMessageSubject()).isEqualTo("Subject for review");
        assertThat(campaign.getMessageBody()).contains("Body for review");
        assertThat(campaign.getApprovedBy()).isNull();
        assertThat(campaign.getApprovedAt()).isNull();
        verify(campaignRepository).save(campaign);
        verify(auditService).recordChange(any());
    }

    @Test
    @DisplayName("501 AI cannot approve campaign")
    void approveCampaignCopyDoesNotApproveLinkedCampaignLifecycle() {
        Segment segment = segment();
        Campaign campaign = campaign(segment);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.SUBMITTED);
        AiRecommendation recommendation = copyRecommendation();
        User approver = approver();
        when(aiRecommendationRepository.findById(RECOMMENDATION_ID))
                .thenReturn(Optional.of(recommendation));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(APPROVER_ID);
        when(userRepository.findById(APPROVER_ID)).thenReturn(Optional.of(approver));
        when(aiRecommendationRepository.save(recommendation)).thenReturn(recommendation);

        AiRecommendationView view =
                service.approveCampaignCopy(
                        RECOMMENDATION_ID,
                        new ApproveAiRecommendationRequest("Human reviewed copy only"));

        assertThat(view.approved()).isTrue();
        assertThat(view.recommendationType()).isEqualTo(AiRecommendationType.COPY);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(campaign.getApprovedBy()).isNull();
        assertThat(campaign.getApprovedAt()).isNull();
        // Non-DRAFT campaigns are not mutated by copy approval.
        verify(campaignRepository, Mockito.never()).save(any(Campaign.class));
    }

    @Test
    void approveCampaignCopyRejectsMissingOrNonCopyRecommendation() {
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.approveCampaignCopy(null, null))
                .withMessageContaining("Campaign copy recommendation id is required");

        when(aiRecommendationRepository.findById(RECOMMENDATION_ID))
                .thenReturn(Optional.empty());
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.approveCampaignCopy(RECOMMENDATION_ID, null))
                .withMessageContaining("AiRecommendation was not found");

        AiRecommendation risk =
                AiRecommendation.create(
                        AiRecommendationType.RISK,
                        "customer",
                        CAMPAIGN_ID,
                        "risk input",
                        "Risk warning",
                        "Risk explanation");
        ReflectionTestUtils.setField(risk, "id", RECOMMENDATION_ID);
        ReflectionTestUtils.setField(risk, "createdAt", Instant.now());
        when(aiRecommendationRepository.findById(RECOMMENDATION_ID)).thenReturn(Optional.of(risk));
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.approveCampaignCopy(RECOMMENDATION_ID, null))
                .withMessageContaining("Only campaign copy recommendations can be approved");
    }

    private void assertMethodAuthorization(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws NoSuchMethodException {
        Method method = CampaignCopyService.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedExpression);
    }

    private void stubRecommendationSave() {
        when(aiRecommendationRepository.save(any(AiRecommendation.class)))
                .thenAnswer(
                        invocation -> {
                            AiRecommendation recommendation = invocation.getArgument(0);
                            ReflectionTestUtils.setField(recommendation, "id", RECOMMENDATION_ID);
                            return recommendation;
                        });
    }

    private static Campaign campaign(Segment segment) {
        Campaign campaign =
                Campaign.create(
                        "Renewal campaign",
                        "cross-sell life cover",
                        null,
                        segment,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Segment segment() {
        Segment segment =
                Segment.create(
                        "Family guardians",
                        "Customers with family protection needs",
                        null,
                        SegmentVisibility.GLOBAL);
        ReflectionTestUtils.setField(segment, "id", SEGMENT_ID);
        return segment;
    }

    private static Product product() {
        Product product =
                Product.create(
                        "Life Protect", ProductType.LIFE_INSURANCE, new BigDecimal("120.00"), 12);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }

    private static AiRecommendation copyRecommendation() {
        AiRecommendation recommendation =
                AiRecommendation.create(
                        AiRecommendationType.COPY,
                        "campaign",
                        CAMPAIGN_ID,
                        "campaign copy input",
                        "Subject: Subject for review\nBody: Body for review\nCall to action: Request more information",
                        "Generated for human review only");
        ReflectionTestUtils.setField(recommendation, "id", RECOMMENDATION_ID);
        ReflectionTestUtils.setField(recommendation, "createdAt", Instant.now());
        return recommendation;
    }

    private static User approver() {
        User user =
                User.create(
                        "campaign.copy.approver@bayer-westphalian.test",
                        "{noop}x",
                        "Campaign Copy Approver");
        ReflectionTestUtils.setField(user, "id", APPROVER_ID);
        return user;
    }
}
