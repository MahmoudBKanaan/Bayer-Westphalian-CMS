package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SegmentController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SegmentCanBePreviewedTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000201");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-08T10:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private SegmentService segmentService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanPreviewSegmentAudience() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(previewView());

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(0))
                .andExpect(jsonPath("$.data.exclusionReasonSummary").isEmpty())
                .andExpect(jsonPath("$.data.matchingCustomers[0].id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.matchingCustomers[0].firstName").value("Lena"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].lastName").value("Mueller"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].city").value("Munich"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].customerType").value("PROSPECT"));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(1);
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName()).isEqualTo("city");
        assertThat(commandCaptor.getValue().criteria().getFirst().operator())
                .isEqualTo(SegmentOperator.EQUALS);
        assertThat(commandCaptor.getValue().criteria().getFirst().value()).isEqualTo("Munich");
    }

    @Test
    void adminCanPreviewSegmentAudience() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(adminClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(previewView());

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void biAnalystCanPreviewSegmentAudience() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(previewView());

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.matchingCustomers[0].city").value("Munich"));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void campaignManagerPreviewResponseIncludesTotalAudienceCount() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        // FR-079: total audience can exceed listed eligible matches when some are excluded
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(5, 1, 4, previewView().matchingCustomers()));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(5))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(4))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(1));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void campaignManagerPreviewResponseIncludesEligibleCount() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(3, 2, 1, previewView().matchingCustomers()));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3))
                .andExpect(jsonPath("$.data.eligibleCount").value(2))
                .andExpect(jsonPath("$.data.excludedCount").value(1));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void campaignManagerPreviewResponseIncludesExcludedCount() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(4, 1, 3, previewView().matchingCustomers()));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(4))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(3));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void previewResponseReturnsEligibleAndExcludedCountsTogetherWithTotal() throws Exception {
        // KB item 199 / FR-079: preview payload always exposes total, eligible, and excluded
        // counts.
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(
                        SegmentPreviewView.of(
                                10,
                                4,
                                6,
                                previewView().matchingCustomers(),
                                List.of(
                                        SegmentExclusionReasonSummary.of(
                                                "DO_NOT_CONTACT",
                                                "Customer has do-not-contact enabled",
                                                4),
                                        SegmentExclusionReasonSummary.of(
                                                "INVALID_CONSENT",
                                                "Customer does not have valid required consent",
                                                2))));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(10))
                .andExpect(jsonPath("$.data.eligibleCount").value(4))
                .andExpect(jsonPath("$.data.excludedCount").value(6))
                .andExpect(jsonPath("$.data.matchingCustomers").isArray())
                .andExpect(jsonPath("$.data.exclusionReasonSummary").isArray());

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isNotNull();
    }

    @Test
    void biAnalystPreviewResponseIncludesZeroEligibleAndPositiveExcludedCounts() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(
                        SegmentPreviewView.of(
                                3,
                                0,
                                3,
                                List.of(),
                                List.of(
                                        SegmentExclusionReasonSummary.of(
                                                "MARKETING_OPT_OUT",
                                                "Customer has withdrawn or rejected marketing consent",
                                                3))));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3))
                .andExpect(jsonPath("$.data.eligibleCount").value(0))
                .andExpect(jsonPath("$.data.excludedCount").value(3))
                .andExpect(jsonPath("$.data.matchingCustomers").isEmpty());

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void campaignManagerPreviewResponseIncludesExclusionReasonSummary() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(
                        SegmentPreviewView.of(
                                4,
                                1,
                                3,
                                previewView().matchingCustomers(),
                                List.of(
                                        SegmentExclusionReasonSummary.of(
                                                "DO_NOT_CONTACT",
                                                "Customer has do-not-contact enabled",
                                                2),
                                        SegmentExclusionReasonSummary.of(
                                                "MARKETING_OPT_OUT",
                                                "Customer has withdrawn or rejected marketing consent",
                                                1))));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excludedCount").value(3))
                .andExpect(jsonPath("$.data.exclusionReasonSummary.length()").value(2))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].code").value("DO_NOT_CONTACT"))
                .andExpect(jsonPath("$.data.exclusionReasonSummary[0].count").value(2))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].message")
                                .value("Customer has do-not-contact enabled"))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[1].code")
                                .value("MARKETING_OPT_OUT"))
                .andExpect(jsonPath("$.data.exclusionReasonSummary[1].count").value(1));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void previewEndpointExposesEligibilityServiceAwareAudienceFields() throws Exception {
        // KB items 178 / 198: preview API carries total / eligible / excluded from
        // EligibilityService gate.
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(
                        SegmentPreviewView.of(
                                3,
                                1,
                                2,
                                previewView().matchingCustomers(),
                                List.of(
                                        SegmentExclusionReasonSummary.of(
                                                "DO_NOT_CONTACT",
                                                "Customer has do-not-contact enabled",
                                                1),
                                        SegmentExclusionReasonSummary.of(
                                                "INVALID_CONSENT",
                                                "Customer does not have valid required consent",
                                                1))));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3))
                .andExpect(jsonPath("$.data.eligibleCount").value(1))
                .andExpect(jsonPath("$.data.excludedCount").value(2))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(1))
                .andExpect(jsonPath("$.data.exclusionReasonSummary.length()").value(2))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].code").value("DO_NOT_CONTACT"))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[1].code").value("INVALID_CONSENT"));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void biAnalystPreviewExposesEligibilityGateFieldsFromService() throws Exception {
        // KB item 198: BI preview path also surfaces EligibilityService-derived audience fields.
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(
                        SegmentPreviewView.of(
                                4,
                                2,
                                2,
                                List.of(previewView().matchingCustomers().getFirst()),
                                List.of(
                                        SegmentExclusionReasonSummary.of(
                                                "MARKETING_OPT_OUT",
                                                "Customer has withdrawn or rejected marketing consent",
                                                1),
                                        SegmentExclusionReasonSummary.of(
                                                "MONTHLY_CONTACT_LIMIT",
                                                "Customer has reached the monthly marketing contact limit",
                                                1))));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(4))
                .andExpect(jsonPath("$.data.eligibleCount").value(2))
                .andExpect(jsonPath("$.data.excludedCount").value(2))
                .andExpect(jsonPath("$.data.matchingCustomers.length()").value(1))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[0].code")
                                .value("MARKETING_OPT_OUT"))
                .andExpect(
                        jsonPath("$.data.exclusionReasonSummary[1].code")
                                .value("MONTHLY_CONTACT_LIMIT"));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void complianceOfficerCannotPreviewSegmentAudience() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceOfficerClaims());

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment preview loaded"))));
    }

    @Test
    void productManagerCannotPreviewSegmentAudience() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment preview loaded"))));
    }

    @Test
    void unauthenticatedRequestCannotPreviewSegmentAudience() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Segment preview loaded"))));
    }

    @Test
    void rejectsInvalidPreviewCriteriaPayload() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": " ",
                                              "operator": "EQUALS",
                                              "value": "Munich"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/preview"));
    }

    @Test
    void previewReturnsZeroAudienceWhenNoCustomersMatch() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(0, List.of()));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(previewPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(0))
                .andExpect(jsonPath("$.data.eligibleCount").value(0))
                .andExpect(jsonPath("$.data.excludedCount").value(0))
                .andExpect(jsonPath("$.data.exclusionReasonSummary").isEmpty())
                .andExpect(jsonPath("$.data.matchingCustomers").isEmpty());
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                OWNER_ID,
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static JwtTokenClaims adminClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                "admin@bayer-westphalian.test",
                List.of(SystemRoleName.ADMIN));
    }

    private static JwtTokenClaims biAnalystClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                "bi.analyst@bayer-westphalian.test",
                List.of(SystemRoleName.BI_ANALYST));
    }

    private static JwtTokenClaims complianceOfficerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009906"),
                "compliance.officer@bayer-westphalian.test",
                List.of(SystemRoleName.COMPLIANCE_OFFICER));
    }

    private static JwtTokenClaims productManagerClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009904"),
                "product.manager@bayer-westphalian.test",
                List.of(SystemRoleName.PRODUCT_MANAGER));
    }

    private static String previewPayload() {
        return """
                {
                  "criteria": [
                    {
                      "fieldName": "city",
                      "operator": "EQUALS",
                      "value": "Munich",
                      "joinOperator": "AND"
                    }
                  ]
                }
                """;
    }

    private static SegmentPreviewView previewView() {
        return SegmentPreviewView.of(
                1,
                List.of(
                        new CustomerView(
                                CUSTOMER_ID,
                                CustomerType.PROSPECT,
                                "Lena",
                                "Mueller",
                                "Lena Mueller",
                                "lena.mueller@bayer-westphalian.test",
                                null,
                                null,
                                "Munich",
                                "Germany",
                                null,
                                CustomerAgeGroup.AGE_26_40,
                                CustomerStatus.ACTIVE,
                                false,
                                true,
                                true,
                                null,
                                CREATED_AT,
                                UPDATED_AT,
                                null)));
    }
}
