package com.bayerwestphalian.campaign.segment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class SegmentCanBeDeletedTests {

    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");

    @Autowired private MockMvc mockMvc;

    @MockBean private SegmentService segmentService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanDeleteOwnedSegment() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment deleted"))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(segmentService).deleteSegment(SEGMENT_ID);
    }

    @Test
    void adminCanDeleteSegment() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment deleted"));

        verify(segmentService).deleteSegment(SEGMENT_ID);
    }

    @Test
    void biAnalystCannotDeleteSegment() throws Exception {
        // KB item 200: BI Analyst cannot edit/delete unless also granted a manage role.
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment deleted"))));
    }

    @Test
    void complianceOfficerCannotDeleteSegment() throws Exception {
        when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                .thenReturn(complianceOfficerClaims());

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer compliance-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment deleted"))));
    }

    @Test
    void productManagerCannotDeleteSegment() throws Exception {
        when(jwtService.validateToken("product-manager-token", JwtTokenType.ACCESS))
                .thenReturn(productManagerClaims());

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer product-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment deleted"))));
    }

    @Test
    void unauthenticatedRequestCannotDeleteSegment() throws Exception {
        mockMvc.perform(delete("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Segment deleted"))));
    }

    @Test
    void mapsMissingSegmentToNotFoundOnDelete() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        doThrow(new ResourceNotFoundException("Segment", SEGMENT_ID))
                .when(segmentService)
                .deleteSegment(SEGMENT_ID);

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
    }

    @Test
    void mapsForbiddenDeleteForNonOwner() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        doThrow(new ForbiddenException("Segment is not owned by the current user"))
                .when(segmentService)
                .deleteSegment(SEGMENT_ID);

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
    }

    @Test
    void deletedSegmentCannotBeLoadedAfterDelete() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.findById(SEGMENT_ID))
                .thenThrow(new ResourceNotFoundException("Segment", SEGMENT_ID));

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment deleted"));

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer campaign-manager-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));

        verify(segmentService).deleteSegment(SEGMENT_ID);
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
}