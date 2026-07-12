package com.bayerwestphalian.campaign.segment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SegmentController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SegmentCanBeLoadedTests {

    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID CRITERION_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000101");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-08T10:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private SegmentService segmentService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanLoadSegmentDetails() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.id").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                .andExpect(jsonPath("$.data.description").value("Customers located in Munich"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.ownerFullName").value("Campaign Manager"))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                .andExpect(jsonPath("$.data.criteria[0].id").value(CRITERION_ID.toString()))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                .andExpect(jsonPath("$.data.criteria[0].operator").value("EQUALS"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"))
                .andExpect(jsonPath("$.data.criteria[0].logicalGroup").value("location"))
                .andExpect(jsonPath("$.data.criteria[0].joinOperator").value("AND"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());

        verify(segmentService).findById(SEGMENT_ID);
    }

    @Test
    void adminCanLoadSegmentDetails() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"));

        verify(segmentService).findById(SEGMENT_ID);
    }

    @Test
    void biAnalystCanLoadSegmentDetails() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"));

        verify(segmentService).findById(SEGMENT_ID);
    }

    @Test
    void complianceOfficerCanLoadSegmentDetails() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceOfficerClaims());
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"));

        verify(segmentService).findById(SEGMENT_ID);
    }

    @Test
    void productManagerCannotLoadSegmentDetails() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment loaded"))));
    }

    @Test
    void unauthenticatedRequestCannotLoadSegmentDetails() throws Exception {
        mockMvc.perform(get("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Segment loaded"))));
    }

    @Test
    void mapsMissingSegmentToNotFoundOnLoad() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.findById(SEGMENT_ID))
                .thenThrow(new ResourceNotFoundException("Segment", SEGMENT_ID));

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));

        verify(segmentService).findById(SEGMENT_ID);
    }

    @Test
    void mapsForbiddenPrivateSegmentAccessOnLoad() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.findById(SEGMENT_ID))
                .thenThrow(new ForbiddenException("Private segment is not accessible"));

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));

        verify(segmentService).findById(eq(SEGMENT_ID));
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

    private static SegmentView segmentView() {
        return new SegmentView(
                SEGMENT_ID,
                "Munich prospects",
                "Customers located in Munich",
                OWNER_ID,
                "Campaign Manager",
                SegmentVisibility.TEAM,
                List.of(
                        new SegmentCriteriaView(
                                CRITERION_ID,
                                SEGMENT_ID,
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND)),
                CREATED_AT,
                UPDATED_AT);
    }
}