package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** KB AI controller endpoints, including item 485 customer search and campaign copy suggestions. */
@WebMvcTest(controllers = AiController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationFilter.class,
    AuthorizationExpressions.class,
    GlobalExceptionHandler.class
})
class AiControllerTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000473");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000487");
    private static final UUID USER_ID = UUID.fromString("90000000-0000-0000-0000-000000000473");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000481");
    private static final UUID RECOMMENDATION_ID =
            UUID.fromString("80000000-0000-0000-0000-000000000481");

    @Autowired private MockMvc mockMvc;

    @MockBean private AiSearchService aiSearchService;

    @MockBean private AiRecommendationService aiRecommendationService;

    @MockBean private CampaignCopyService campaignCopyService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void exposesCustomerSearchEndpointWithCustomerReadAuthorization() throws Exception {
        assertThat(AiController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(AiController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/ai");

        Method method = AiController.class.getMethod("customerSearch", String.class, Integer.class);
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/customer-search");
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@authz.canReadCustomers()");
    }

    @Test
    void exposesCampaignCopyEndpointWithCampaignManagerAuthorizationForItem488() throws Exception {
        Method segmentMethod =
                AiController.class.getMethod("segmentSuggestions", SegmentSuggestionRequest.class);
        assertThat(segmentMethod.getAnnotation(PostMapping.class).value())
                .containsExactly("/segment-suggestions");
        assertThat(segmentMethod.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')");

        Method productMethod =
                AiController.class.getMethod(
                        "productRecommendations", ProductRecommendationRequest.class);
        assertThat(productMethod.getAnnotation(PostMapping.class).value())
                .containsExactly("/product-recommendations");
        assertThat(productMethod.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER')");

        Method duplicateMethod =
                AiController.class.getMethod(
                        "duplicateContactWarning", DuplicateContactRiskRequest.class);
        assertThat(duplicateMethod.getAnnotation(PostMapping.class).value())
                .containsExactly("/duplicate-contact-warning");
        assertThat(duplicateMethod.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@authz.canReadCustomers()");

        Method method = AiController.class.getMethod("campaignCopy", CampaignCopyRequest.class);
        assertThat(method.getAnnotation(PostMapping.class).value())
                .containsExactly("/campaign-copy");
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");

        Method approveMethod =
                AiController.class.getMethod(
                        "approveCampaignCopy", UUID.class, ApproveAiRecommendationRequest.class);
        assertThat(approveMethod.getAnnotation(PostMapping.class).value())
                .containsExactly("/campaign-copy/{recommendationId}/approve");
        assertThat(approveMethod.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
    }

    @Test
    void campaignManagerCanRunWeightedCustomerSearch() throws Exception {
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(aiSearchService.weightedSearch("Ada Lovelce", 7))
                .thenReturn(sampleResults("Ada Lovelce"));

        mockMvc.perform(
                        get("/api/ai/customer-search")
                                .param("q", "Ada Lovelce")
                                .param("limit", "7")
                                .header("Authorization", "Bearer cm-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI customer search completed"))
                .andExpect(jsonPath("$.data.query").value("Ada Lovelce"))
                .andExpect(jsonPath("$.data.totalHits").value(1))
                .andExpect(jsonPath("$.data.results", hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.results[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.data.results[0].score").value(93))
                .andExpect(jsonPath("$.data.results[0].explainScore[0].factor").value("full name"))
                .andExpect(jsonPath("$.data.results[0].explainScore[0].weight").value(45))
                .andExpect(jsonPath("$.data.results[0].explainScore[0].contribution").value(40))
                .andExpect(
                        jsonPath("$.data.results[0].explainScore[0].detail")
                                .value("fuzzy match (full name: Ada Lovelace)"));

        verify(aiSearchService).weightedSearch("Ada Lovelce", 7);
    }

    @Test
    void customerSearchUsesDefaultLimitWhenLimitIsOmitted() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(aiSearchService.weightedSearch("ada", AiCustomerSearchRequest.DEFAULT_LIMIT))
                .thenReturn(sampleResults("ada"));

        mockMvc.perform(
                        get("/api/ai/customer-search")
                                .param("q", "ada")
                                .header("Authorization", "Bearer bi-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalHits").value(1));

        verify(aiSearchService).weightedSearch("ada", AiCustomerSearchRequest.DEFAULT_LIMIT);
    }

    @Test
    void customerServiceAgentCanRunCustomerSearchAsAuthorizedKbRole() throws Exception {
        when(jwtService.validateToken("agent-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CUSTOMER_SERVICE_AGENT));
        when(aiSearchService.weightedSearch("policy holder", 3))
                .thenReturn(sampleResults("policy holder"));

        mockMvc.perform(
                        get("/api/ai/customer-search")
                                .param("q", "policy holder")
                                .param("limit", "3")
                                .header("Authorization", "Bearer agent-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.query").value("policy holder"))
                .andExpect(jsonPath("$.data.results", hasSize(1)));

        verify(aiSearchService).weightedSearch("policy holder", 3);
    }

    @Test
    void executiveViewerCannotRunCustomerSearchWithoutCustomerReadRole() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));

        mockMvc.perform(
                        get("/api/ai/customer-search")
                                .param("q", "ada")
                                .header("Authorization", "Bearer exec-token"))
                .andExpect(status().isForbidden());

        verify(aiSearchService, never())
                .weightedSearch("ada", AiCustomerSearchRequest.DEFAULT_LIMIT);
    }

    @Test
    void biAnalystCanGenerateSegmentSuggestions() throws Exception {
        SegmentSuggestionRequest request =
                new SegmentSuggestionRequest(null, "Berlin", "Germany", "LIFE_INSURANCE", 6);
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));
        when(aiRecommendationService.suggestSegments(request))
                .thenReturn(sampleSegmentSuggestions());

        mockMvc.perform(
                        post("/api/ai/segment-suggestions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer bi-token")
                                .content(
                                        """
                                        {
                                          "city": "Berlin",
                                          "country": "Germany",
                                          "productTypeHint": "LIFE_INSURANCE",
                                          "expirationWithinMonths": 6
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI segment suggestions generated"))
                .andExpect(jsonPath("$.data.suggestions", hasSize(1)))
                .andExpect(jsonPath("$.data.suggestions[0].suggestedName").value("Berlin Life"))
                .andExpect(
                        jsonPath("$.data.suggestions[0].explanation")
                                .value(
                                        "AI-002 rule-based segment suggestion for human decision support from location and product rules"))
                .andExpect(jsonPath("$.data.suggestions[0].confidenceScore").value(88));

        verify(aiRecommendationService).suggestSegments(request);
    }

    @Test
    void executiveViewerCannotGenerateSegmentSuggestions() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));

        mockMvc.perform(
                        post("/api/ai/segment-suggestions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer exec-token")
                                .content("{}"))
                .andExpect(status().isForbidden());

        verify(aiRecommendationService, never())
                .suggestSegments(any(SegmentSuggestionRequest.class));
    }

    @Test
    void campaignManagerCanGenerateProductRecommendations() throws Exception {
        ProductRecommendationRequest request = new ProductRecommendationRequest(CUSTOMER_ID);
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(aiRecommendationService.recommendProducts(request))
                .thenReturn(sampleProductRecommendations());

        mockMvc.perform(
                        post("/api/ai/product-recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer cm-token")
                                .content(
                                        """
                                        {
                                          "customerId": "%s"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI product recommendations generated"))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.recommendations", hasSize(1)))
                .andExpect(
                        jsonPath("$.data.recommendations[0].productId")
                                .value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.data.recommendations[0].productName").value("Life Plus"))
                .andExpect(
                        jsonPath("$.data.recommendations[0].productType")
                                .value("LIFE_INSURANCE"))
                .andExpect(
                        jsonPath("$.data.recommendations[0].explanation")
                                .value(
                                        "Customer profile and existing products indicate a coverage gap"))
                .andExpect(jsonPath("$.data.recommendations[0].confidenceScore").value(91));

        verify(aiRecommendationService).recommendProducts(request);
    }

    @Test
    void executiveViewerCannotGenerateProductRecommendations() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));

        mockMvc.perform(
                        post("/api/ai/product-recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer exec-token")
                                .content(
                                        """
                                        {
                                          "customerId": "%s"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID)))
                .andExpect(status().isForbidden());

        verify(aiRecommendationService, never())
                .recommendProducts(any(ProductRecommendationRequest.class));
    }

    @Test
    void campaignManagerCanGenerateDuplicateContactWarning() throws Exception {
        DuplicateContactRiskRequest request =
                new DuplicateContactRiskRequest(CUSTOMER_ID, CAMPAIGN_ID);
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(aiRecommendationService.detectDuplicateRisk(request))
                .thenReturn(sampleDuplicateWarning());

        mockMvc.perform(
                        post("/api/ai/duplicate-contact-warning")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer cm-token")
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "campaignId": "%s"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, CAMPAIGN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI duplicate-contact warning generated"))
                .andExpect(jsonPath("$.data.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.riskDetected").value(true))
                .andExpect(jsonPath("$.data.warning").value("Duplicate contact risk"))
                .andExpect(jsonPath("$.data.contactsInCurrentMonth").value(3))
                .andExpect(jsonPath("$.data.monthlyContactLimit").value(3))
                .andExpect(jsonPath("$.data.sameCampaignAlreadyContacted").value(true))
                .andExpect(
                        jsonPath("$.data.storedRecommendationId")
                                .value(RECOMMENDATION_ID.toString()));

        verify(aiRecommendationService).detectDuplicateRisk(request);
    }

    @Test
    void executiveViewerCannotGenerateDuplicateContactWarning() throws Exception {
        when(jwtService.validateToken("exec-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.EXECUTIVE_VIEWER));

        mockMvc.perform(
                        post("/api/ai/duplicate-contact-warning")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer exec-token")
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "campaignId": "%s"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, CAMPAIGN_ID)))
                .andExpect(status().isForbidden());

        verify(aiRecommendationService, never())
                .detectDuplicateRisk(any(DuplicateContactRiskRequest.class));
    }

    @Test
    void campaignManagerCanGenerateCampaignCopySuggestion() throws Exception {
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(campaignCopyService.generateCopySuggestion(
                        new CampaignCopyRequest(
                                CAMPAIGN_ID,
                                "renew expiring policies",
                                "Life Protect",
                                CampaignChannel.EMAIL,
                                "Family guardians")))
                .thenReturn(sampleCopySuggestion());

        mockMvc.perform(
                        post("/api/ai/campaign-copy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer cm-token")
                                .content(
                                        """
                                        {
                                          "campaignId": "%s",
                                          "objective": "renew expiring policies",
                                          "productName": "Life Protect",
                                          "channel": "EMAIL",
                                          "audienceHint": "Family guardians"
                                        }
                                        """
                                                .formatted(CAMPAIGN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.message")
                                .value("AI campaign copy suggestion generated"))
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.subject").value("Protect what matters"))
                .andExpect(jsonPath("$.data.body").value("Body copy for review"))
                .andExpect(jsonPath("$.data.callToAction").value("Review the offer"))
                .andExpect(
                        jsonPath("$.data.explanation")
                                .value("Generated for human review only"))
                .andExpect(jsonPath("$.data.confidenceScore").value(72))
                .andExpect(jsonPath("$.data.requiresHumanApproval").value(true))
                .andExpect(jsonPath("$.data.humanApproved").value(false))
                .andExpect(
                        jsonPath("$.data.storedRecommendationId")
                                .value(RECOMMENDATION_ID.toString()));

        verify(campaignCopyService)
                .generateCopySuggestion(
                        new CampaignCopyRequest(
                                CAMPAIGN_ID,
                                "renew expiring policies",
                                "Life Protect",
                                CampaignChannel.EMAIL,
                                "Family guardians"));
    }

    @Test
    void nonCampaignManagerCannotGenerateCampaignCopySuggestion() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

        mockMvc.perform(
                        post("/api/ai/campaign-copy")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer bi-token")
                                .content(
                                        """
                                        {
                                          "campaignId": "%s",
                                          "objective": "renew expiring policies",
                                          "productName": "Life Protect",
                                          "channel": "EMAIL",
                                          "audienceHint": "Family guardians"
                                        }
                                        """
                                                .formatted(CAMPAIGN_ID)))
                .andExpect(status().isForbidden());

        verify(campaignCopyService, never())
                .generateCopySuggestion(
                        new CampaignCopyRequest(
                                CAMPAIGN_ID,
                                "renew expiring policies",
                                "Life Protect",
                                CampaignChannel.EMAIL,
                                "Family guardians"));
    }

    @Test
    void campaignManagerCanApproveStoredCampaignCopySuggestion() throws Exception {
        when(jwtService.validateToken("cm-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.CAMPAIGN_MANAGER));
        when(campaignCopyService.approveCampaignCopy(
                        RECOMMENDATION_ID, new ApproveAiRecommendationRequest("Approved copy")))
                .thenReturn(sampleApprovedCopyRecommendation());

        mockMvc.perform(
                        post("/api/ai/campaign-copy/{recommendationId}/approve", RECOMMENDATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer cm-token")
                                .content(
                                        """
                                        {
                                          "reviewNotes": "Approved copy"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI campaign copy approved"))
                .andExpect(jsonPath("$.data.id").value(RECOMMENDATION_ID.toString()))
                .andExpect(jsonPath("$.data.recommendationType").value("COPY"))
                .andExpect(
                        jsonPath("$.data.explanation")
                                .value("Generated for human review only"))
                .andExpect(jsonPath("$.data.confidenceScore").value(72))
                .andExpect(jsonPath("$.data.approved").value(true))
                .andExpect(jsonPath("$.data.reviewNotes").value("Approved copy"))
                .andExpect(jsonPath("$.data.approvedByUserId").value(USER_ID.toString()));

        verify(campaignCopyService)
                .approveCampaignCopy(
                        RECOMMENDATION_ID, new ApproveAiRecommendationRequest("Approved copy"));
    }

    @Test
    void nonCampaignManagerCannotApproveCampaignCopySuggestion() throws Exception {
        when(jwtService.validateToken("bi-token", JwtTokenType.ACCESS))
                .thenReturn(roleClaims(SystemRoleName.BI_ANALYST));

        mockMvc.perform(
                        post("/api/ai/campaign-copy/{recommendationId}/approve", RECOMMENDATION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer bi-token")
                                .content("{}"))
                .andExpect(status().isForbidden());

        verify(campaignCopyService, never())
                .approveCampaignCopy(any(UUID.class), any(ApproveAiRecommendationRequest.class));
    }

    private static AiCustomerSearchView sampleResults(String query) {
        AiCustomerSearchHitView hit =
                new AiCustomerSearchHitView(
                        CUSTOMER_ID,
                        "Ada",
                        "Lovelace",
                        "Ada Lovelace",
                        "ada.lovelace@example.test",
                        "London",
                        "United Kingdom",
                        CustomerType.PROSPECT,
                        CustomerStatus.ACTIVE,
                        false,
                        BigDecimal.valueOf(93),
                        List.of(
                                ScoreExplanationView.of(
                                        "full name",
                                        BigDecimal.valueOf(45),
                                        BigDecimal.valueOf(40),
                                        "fuzzy match (full name: Ada Lovelace)")));
        return AiCustomerSearchView.of(query, List.of(hit));
    }

    private static CampaignCopySuggestionView sampleCopySuggestion() {
        return CampaignCopySuggestionView.pending(
                CAMPAIGN_ID,
                "Protect what matters",
                "Body copy for review",
                "Review the offer",
                "Generated for human review only",
                BigDecimal.valueOf(72),
                RECOMMENDATION_ID);
    }

    private static SegmentSuggestionView.ListResponse sampleSegmentSuggestions() {
        SegmentSuggestionView suggestion =
                new SegmentSuggestionView(
                        "Berlin Life",
                        "Customers in Berlin with life product signals",
                        List.of(
                                SuggestedSegmentCriterion.equals("city", "Berlin"),
                                SuggestedSegmentCriterion.equals(
                                        "product_type", "LIFE_INSURANCE")),
                        null,
                        "AI-002 rule-based segment suggestion for human decision support from location and product rules",
                        BigDecimal.valueOf(88),
                        RECOMMENDATION_ID);
        return new SegmentSuggestionView.ListResponse(List.of(suggestion));
    }

    private static ProductRecommendationView.ListResponse sampleProductRecommendations() {
        ProductRecommendationView recommendation =
                new ProductRecommendationView(
                        PRODUCT_ID,
                        "Life Plus",
                        ProductType.LIFE_INSURANCE,
                        "Recommend Life Plus for protection needs",
                        "Customer profile and existing products indicate a coverage gap",
                        BigDecimal.valueOf(91),
                        RECOMMENDATION_ID);
        return new ProductRecommendationView.ListResponse(CUSTOMER_ID, List.of(recommendation));
    }

    private static DuplicateContactRiskView sampleDuplicateWarning() {
        return new DuplicateContactRiskView(
                CUSTOMER_ID,
                CAMPAIGN_ID,
                true,
                "Duplicate contact risk",
                "Same-campaign and monthly contact rules are both at risk",
                3,
                3,
                true,
                RECOMMENDATION_ID);
    }

    private static AiRecommendationView sampleApprovedCopyRecommendation() {
        return new AiRecommendationView(
                RECOMMENDATION_ID,
                AiRecommendationType.COPY,
                "campaign",
                CAMPAIGN_ID,
                "campaign copy input",
                "Subject: Protect what matters",
                "Generated for human review only",
                BigDecimal.valueOf(72),
                USER_ID,
                "Campaign Manager",
                "Approved copy",
                true,
                null);
    }

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(USER_ID, "ai.search@bayer-westphalian.test", List.of(role));
    }
}
