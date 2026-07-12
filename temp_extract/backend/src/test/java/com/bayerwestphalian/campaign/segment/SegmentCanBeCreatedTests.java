package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class SegmentCanBeCreatedTests {

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
    void campaignManagerCanCreateReusableSegment() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.createSegment(any(CreateSegmentCommand.class))).thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment created"))
                .andExpect(jsonPath("$.data.id").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                .andExpect(jsonPath("$.data.criteria[0].operator").value("EQUALS"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"))
                .andExpect(jsonPath("$.data.criteria[0].joinOperator").value("AND"));

        ArgumentCaptor<CreateSegmentCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateSegmentCommand.class);
        verify(segmentService).createSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().name()).isEqualTo("Munich prospects");
        assertThat(commandCaptor.getValue().description()).isEqualTo("Customers located in Munich");
        assertThat(commandCaptor.getValue().visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(commandCaptor.getValue().criteria()).hasSize(1);
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName()).isEqualTo("city");
        assertThat(commandCaptor.getValue().criteria().getFirst().operator())
                .isEqualTo(SegmentOperator.EQUALS);
        assertThat(commandCaptor.getValue().criteria().getFirst().value()).isEqualTo("Munich");
    }

    @Test
    void adminCanCreateReusableSegment() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(segmentService.createSegment(any(CreateSegmentCommand.class))).thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Segment created"))
                .andExpect(jsonPath("$.data.ownerFullName").value("Campaign Manager"))
                .andExpect(jsonPath("$.data.criteria[0].logicalGroup").value("location"));

        verify(segmentService).createSegment(any(CreateSegmentCommand.class));
    }

    @Test
    void biAnalystCannotCreateSegment() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment created"))));
    }

    @Test
    void complianceOfficerCannotCreateSegment() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceOfficerClaims());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer compliance-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment created"))));
    }

    @Test
    void productManagerCannotCreateSegment() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer product-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment created"))));
    }

    @Test
    void unauthenticatedRequestCannotCreateSegment() throws Exception {
        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Segment created"))));
    }

    @Test
    void rejectsInvalidCreateSegmentPayload() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": " ",
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments"));
    }

    @Test
    void createdSegmentCanBeLoadedAfterCreation() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.createSegment(any(CreateSegmentCommand.class))).thenReturn(segmentView());
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(SEGMENT_ID.toString()));

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"));

        verify(segmentService).createSegment(any(CreateSegmentCommand.class));
        verify(segmentService).findById(SEGMENT_ID);
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

    private static String createSegmentPayload() {
        return """
                {
                  "name": "Munich prospects",
                  "description": "Customers located in Munich",
                  "visibility": "TEAM",
                  "criteria": [
                    {
                      "fieldName": "city",
                      "operator": "EQUALS",
                      "value": "Munich",
                      "logicalGroup": "location",
                      "joinOperator": "AND"
                    }
                  ]
                }
                """;
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