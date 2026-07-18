package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 471: AI DTOs for decision-support APIs (E21 / AI-001–AI-006 / COMP-005).
 *
 * <p>Covers stored recommendation views, fuzzy search hits with explainScore, product/segment
 * suggestions, default-risk and duplicate-contact warnings, and campaign copy payloads that always
 * require human approval.
 */
@DisplayName("471 Implement AI DTOs")
class AiDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000471");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000471");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000471");
    private static final UUID RECOMMENDATION_ID =
            UUID.fromString("57000000-0000-0000-0000-000000000471");
    private static final UUID APPROVER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000471");

    @Nested
    @DisplayName("AiRecommendationView")
    class RecommendationView {

        @Test
        void fromMapsEntityFieldsIncludingApprover() {
            User approver =
                    User.create(
                            "ai.dto@bayer-westphalian.test", "{noop}x", "AI DTO Approver");
            ReflectionTestUtils.setField(approver, "id", APPROVER_ID);

            AiRecommendation entity =
                    AiRecommendation.create(
                            AiRecommendationType.PRODUCT,
                            "customer",
                            CUSTOMER_ID,
                            "Owns life policy",
                            "Recommend homeowner insurance",
                            "Coverage gap suggests cross-sell",
                            new BigDecimal("81.25"));
            ReflectionTestUtils.setField(entity, "id", RECOMMENDATION_ID);
            ReflectionTestUtils.setField(
                    entity, "createdAt", Instant.parse("2026-07-11T12:00:00Z"));
            entity.approve(approver, "Reviewed for audit trail");

            AiRecommendationView view = AiRecommendationView.from(entity);

            assertThat(view.id()).isEqualTo(RECOMMENDATION_ID);
            assertThat(view.recommendationType()).isEqualTo(AiRecommendationType.PRODUCT);
            assertThat(view.targetEntityType()).isEqualTo("customer");
            assertThat(view.targetEntityId()).isEqualTo(CUSTOMER_ID);
            assertThat(view.inputSummary()).isEqualTo("Owns life policy");
            assertThat(view.recommendation()).isEqualTo("Recommend homeowner insurance");
            assertThat(view.explanation()).isEqualTo("Coverage gap suggests cross-sell");
            assertThat(view.confidenceScore()).isEqualByComparingTo("81.25");
            assertThat(view.approvedByUserId()).isEqualTo(APPROVER_ID);
            assertThat(view.approvedByFullName()).isEqualTo("AI DTO Approver");
            assertThat(view.reviewNotes()).isEqualTo("Reviewed for audit trail");
            assertThat(view.approved()).isTrue();
            assertThat(view.createdAt()).isEqualTo(Instant.parse("2026-07-11T12:00:00Z"));
        }

        @Test
        void fromRequiresEntity() {
            assertThatThrownBy(() -> AiRecommendationView.from(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("recommendation is required");
        }
    }

    @Nested
    @DisplayName("Search DTOs (AI-001 / explainScore)")
    class SearchDtos {

        @Test
        void scoreExplanationViewRequiresFactor() {
            ScoreExplanationView factor =
                    ScoreExplanationView.of(
                            "email",
                            new BigDecimal("0.40"),
                            new BigDecimal("0.40"),
                            "Exact email match");

            assertThat(factor.factor()).isEqualTo("email");
            assertThat(factor.weight()).isEqualByComparingTo("0.40");
            assertThat(factor.contribution()).isEqualByComparingTo("0.40");
            assertThat(factor.detail()).isEqualTo("Exact email match");

            assertThatThrownBy(
                            () ->
                                    ScoreExplanationView.of(
                                            "  ", BigDecimal.ONE, BigDecimal.ONE, "x"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("factor");
        }

        @Test
        void customerSearchHitAndViewCopyExplainScoreDefensively() {
            List<ScoreExplanationView> mutable =
                    new ArrayList<>(
                            List.of(
                                    ScoreExplanationView.of(
                                            "name",
                                            new BigDecimal("0.50"),
                                            new BigDecimal("0.50"),
                                            "Last name match")));
            AiCustomerSearchHitView hit =
                    new AiCustomerSearchHitView(
                            CUSTOMER_ID,
                            "Ada",
                            "Lovelace",
                            "Ada Lovelace",
                            "ada@example.test",
                            "Munich",
                            "DE",
                            CustomerType.CUSTOMER,
                            CustomerStatus.ACTIVE,
                            false,
                            new BigDecimal("0.85"),
                            mutable);

            mutable.clear();

            assertThat(hit.explainScore()).hasSize(1);
            assertThat(hit.explainScore().get(0).factor()).isEqualTo("name");
            assertThat(hit.score()).isEqualByComparingTo("0.85");
            assertThat(hit.doNotContact()).isFalse();

            AiCustomerSearchView view = AiCustomerSearchView.of("ada munich", List.of(hit));
            assertThat(view.query()).isEqualTo("ada munich");
            assertThat(view.totalHits()).isEqualTo(1);
            assertThat(view.results()).containsExactly(hit);
        }

        @Test
        void customerSearchRequestTrimsQueryAndAppliesDefaultLimit() {
            AiCustomerSearchRequest request = new AiCustomerSearchRequest("  life munich  ", null);

            assertThat(request.query()).isEqualTo("life munich");
            assertThat(request.effectiveLimit()).isEqualTo(AiCustomerSearchRequest.DEFAULT_LIMIT);
            assertThat(invalidFields(request)).isEmpty();
        }

        @Test
        void customerSearchRequestValidatesQueryAndLimit() {
            assertThat(invalidFields(new AiCustomerSearchRequest(" ", 10))).contains("query");
            assertThat(invalidFields(new AiCustomerSearchRequest(null, 10))).contains("query");

            assertThatThrownBy(() -> new AiCustomerSearchRequest("ok", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 1");
            assertThatThrownBy(() -> new AiCustomerSearchRequest("ok", 101))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }
    }

    @Nested
    @DisplayName("Product and segment recommendation DTOs (AI-002 / AI-003)")
    class ProductAndSegmentDtos {

        @Test
        void productRecommendationRequestRequiresCustomerId() {
            assertThat(invalidFields(new ProductRecommendationRequest(CUSTOMER_ID))).isEmpty();
            assertThat(invalidFields(new ProductRecommendationRequest(null)))
                    .containsExactly("customerId");
        }

        @Test
        void productRecommendationViewAndListResponse() {
            ProductRecommendationView row =
                    new ProductRecommendationView(
                            PRODUCT_ID,
                            "Home Protect",
                            ProductType.HOMEOWNER_INSURANCE,
                            "Recommend homeowner product",
                            "Customer has life cover but no homeowner policy",
                            new BigDecimal("77.00"),
                            RECOMMENDATION_ID);

            ProductRecommendationView.ListResponse response =
                    new ProductRecommendationView.ListResponse(CUSTOMER_ID, List.of(row));

            assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
            assertThat(response.recommendations()).containsExactly(row);
            assertThat(row.productType()).isEqualTo(ProductType.HOMEOWNER_INSURANCE);
            assertThat(row.explanation()).contains("life cover");
        }

        @Test
        void segmentSuggestionViewCopiesCriteriaSummary() {
            List<SuggestedSegmentCriterion> criteria =
                    new ArrayList<>(
                            List.of(
                                    SuggestedSegmentCriterion.equals("city", "Munich"),
                                    SuggestedSegmentCriterion.equals(
                                            "expiring_within_months", "6")));
            SegmentSuggestionView suggestion =
                    new SegmentSuggestionView(
                            "Munich expiring policies",
                            "Location + expiration audience",
                            criteria,
                            null,
                            "High density of expiring products in Munich",
                            new BigDecimal("70.00"),
                            null);
            criteria.clear();

            assertThat(suggestion.suggestedCriteria()).hasSize(2);
            assertThat(suggestion.suggestedCriteriaSummary()).hasSize(2);
            assertThat(suggestion.suggestedName()).isEqualTo("Munich expiring policies");

            SegmentSuggestionView.ListResponse list =
                    new SegmentSuggestionView.ListResponse(List.of(suggestion));
            assertThat(list.suggestions()).hasSize(1);
        }

        @Test
        void segmentSuggestionRequestAllowsOptionalSeeds() {
            SegmentSuggestionRequest request =
                    new SegmentSuggestionRequest(CUSTOMER_ID, "Munich", "DE", "LIFE_INSURANCE", 6);

            assertThat(request.city()).isEqualTo("Munich");
            assertThat(request.expirationWithinMonths()).isEqualTo(6);
            assertThat(invalidFields(request)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Risk DTOs (AI-004 / AI-006)")
    class RiskDtos {

        @Test
        void defaultRiskScoreRequestRequiresCustomerId() {
            assertThat(invalidFields(new DefaultRiskScoreRequest(CUSTOMER_ID))).isEmpty();
            assertThat(invalidFields(new DefaultRiskScoreRequest(null)))
                    .containsExactly("customerId");
        }

        @Test
        void defaultRiskScoreViewHoldsFactors() {
            DefaultRiskScoreView view =
                    new DefaultRiskScoreView(
                            CUSTOMER_ID,
                            new BigDecimal("68.50"),
                            "MEDIUM",
                            "Missed payments and a yellow reminder elevate risk",
                            List.of(
                                    ScoreExplanationView.of(
                                            "missedPayments",
                                            new BigDecimal("0.40"),
                                            new BigDecimal("0.30"),
                                            "2 missed payments")),
                            RECOMMENDATION_ID);

            assertThat(view.riskLevel()).isEqualTo("MEDIUM");
            assertThat(view.factors()).hasSize(1);
            assertThat(view.riskScore()).isEqualByComparingTo("68.50");
        }

        @Test
        void duplicateContactRiskRequestAndView() {
            assertThat(
                            invalidFields(
                                    new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID)))
                    .isEmpty();
            assertThat(invalidFields(new DuplicateContactRiskRequest(null, CAMPAIGN_ID)))
                    .containsExactly("customerId");

            DuplicateContactRiskView view =
                    new DuplicateContactRiskView(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            true,
                            "Customer already contacted for this campaign",
                            "Same-campaign duplicate prevention (BR-010)",
                            2,
                            3,
                            true,
                            RECOMMENDATION_ID);

            assertThat(view.riskDetected()).isTrue();
            assertThat(view.sameCampaignAlreadyContacted()).isTrue();
            assertThat(view.contactsInCurrentMonth()).isEqualTo(2);
            assertThat(view.monthlyContactLimit()).isEqualTo(3);
        }

        @Test
        void duplicateContactRiskViewRejectsNegativeContactCount() {
            assertThatThrownBy(
                            () ->
                                    new DuplicateContactRiskView(
                                            CUSTOMER_ID,
                                            null,
                                            false,
                                            null,
                                            "No risk",
                                            -1,
                                            3,
                                            false,
                                            null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contactsInCurrentMonth");
        }
    }

    @Nested
    @DisplayName("Campaign copy DTOs (AI-005 / COMP-005)")
    class CampaignCopyDtos {

        @Test
        void campaignCopyRequestRequiresObjective() {
            CampaignCopyRequest valid =
                    new CampaignCopyRequest(
                            CAMPAIGN_ID,
                            "Cross-sell life protection",
                            "Life Protect",
                            CampaignChannel.EMAIL,
                            "Munich prospects");
            assertThat(invalidFields(valid)).isEmpty();

            CampaignCopyRequest missingObjective =
                    new CampaignCopyRequest(CAMPAIGN_ID, "  ", null, null, null);
            assertThat(invalidFields(missingObjective)).contains("objective");
        }

        @Test
        void campaignCopySuggestionAlwaysRequiresHumanApproval() {
            CampaignCopySuggestionView pending =
                    CampaignCopySuggestionView.pending(
                            CAMPAIGN_ID,
                            "Protect what matters",
                            "Body copy for review",
                            "Talk to an advisor",
                            "Generated for human review only",
                            RECOMMENDATION_ID);

            assertThat(pending.requiresHumanApproval()).isTrue();
            assertThat(pending.humanApproved()).isFalse();
            assertThat(pending.approvedByUserId()).isNull();
            assertThat(pending.subject()).isEqualTo("Protect what matters");
            assertThat(pending.callToAction()).isEqualTo("Talk to an advisor");
            assertThat(pending.confidenceScore()).isNull();

            // Even if a caller tries to pass requiresHumanApproval=false, compact ctor forces true.
            CampaignCopySuggestionView forced =
                    new CampaignCopySuggestionView(
                            CAMPAIGN_ID,
                            "Subject",
                            "Body",
                            "CTA",
                            "Explanation",
                            new BigDecimal("76.5"),
                            false,
                            true,
                            APPROVER_ID,
                            RECOMMENDATION_ID);
            assertThat(forced.requiresHumanApproval()).isTrue();
            assertThat(forced.humanApproved()).isTrue();
            assertThat(forced.confidenceScore()).isEqualByComparingTo("76.5");
        }

        @Test
        void approveRequestNotesOptional() {
            assertThat(invalidFields(new ApproveAiRecommendationRequest(null))).isEmpty();
            assertThat(invalidFields(new ApproveAiRecommendationRequest("Looks good"))).isEmpty();
            String tooLong = "n".repeat(1001);
            assertThat(invalidFields(new ApproveAiRecommendationRequest(tooLong)))
                    .contains("reviewNotes");
        }
    }

    private static Set<String> invalidFields(Object target) {
        return VALIDATOR.validate(target).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
