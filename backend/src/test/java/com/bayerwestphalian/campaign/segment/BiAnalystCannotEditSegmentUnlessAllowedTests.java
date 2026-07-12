package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
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
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 200 acceptance: BI Analyst cannot edit segment unless allowed.
 *
 * <p>Proves {@code BI_ANALYST} alone cannot update, delete, or manage segment definitions (HTTP
 * filter + {@code canManageSegments} / {@code canCreateSegments}), while still reading and
 * previewing. Dual-role BI + Campaign Manager (or Admin) is the “unless allowed” path.
 */
@WebMvcTest(controllers = SegmentController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class BiAnalystCannotEditSegmentUnlessAllowedTests {

    private static final UUID SEGMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID BI_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009903");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-08T10:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private SegmentService segmentService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private final AuthorizationExpressions authorizationExpressions =
            new AuthorizationExpressions();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void biAnalystAloneCannotManageOrCreateSegmentsViaExpression() {
        authenticate(SystemRoleName.BI_ANALYST);

        assertThat(authorizationExpressions.canManageSegments()).isFalse();
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();
        assertThat(authorizationExpressions.canReadSegments()).isTrue();
        assertThat(authorizationExpressions.canPreviewSegments()).isTrue();
    }

    @Test
    void biAnalystWithCampaignManagerRoleIsAllowedToEdit() {
        // “Unless allowed” — dual-role accounts holding a manage role may edit.
        authenticate(SystemRoleName.BI_ANALYST, SystemRoleName.CAMPAIGN_MANAGER);

        assertThat(authorizationExpressions.canManageSegments()).isTrue();
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
        assertThat(authorizationExpressions.canReadSegments()).isTrue();
        assertThat(authorizationExpressions.canPreviewSegments()).isTrue();
    }

    @Test
    void biAnalystWithAdminRoleIsAllowedToEdit() {
        authenticate(SystemRoleName.BI_ANALYST, SystemRoleName.ADMIN);

        assertThat(authorizationExpressions.canManageSegments()).isTrue();
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
    }

    @Test
    void segmentManageRolesDoNotIncludeBiAnalyst() {
        assertThat(SecurityConfiguration.SEGMENT_MANAGE_ROLES)
                .containsExactlyInAnyOrder(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
        assertThat(SecurityConfiguration.SEGMENT_MANAGE_ROLES)
                .doesNotContain(SystemRoleName.BI_ANALYST.name());
        assertThat(SecurityConfiguration.SEGMENT_CREATE_ROLES)
                .doesNotContain(SystemRoleName.BI_ANALYST.name());
        assertThat(SecurityConfiguration.SEGMENT_READ_ROLES)
                .contains(SystemRoleName.BI_ANALYST.name());
        assertThat(SecurityConfiguration.SEGMENT_PREVIEW_ROLES)
                .contains(SystemRoleName.BI_ANALYST.name());
    }

    @Test
    void updateDeleteAndSaveCriteriaMethodsRequireCanManageSegments() throws Exception {
        assertMethodUsesCanManageSegments("updateSegment", UUID.class, UpdateSegmentCommand.class);
        assertMethodUsesCanManageSegments("deleteSegment", UUID.class);
        assertMethodUsesCanManageSegments("saveCriteria", UUID.class, List.class);
    }

    @Test
    void biAnalystCannotUpdateSegmentViaPutApi() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());

        mockMvc.perform(
                        put("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatePayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment updated"))));

        verify(segmentService, never())
                .updateSegment(any(UUID.class), any(UpdateSegmentCommand.class));
    }

    @Test
    void biAnalystCannotDeleteSegmentViaDeleteApi() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());

        mockMvc.perform(
                        delete("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment deleted"))));

        verify(segmentService, never()).deleteSegment(any(UUID.class));
    }

    @Test
    void biAnalystCannotCreateSegmentViaPostApi() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment created"))));

        verify(segmentService, never()).createSegment(any(CreateSegmentCommand.class));
    }

    @Test
    void biAnalystCanStillReadAndPreviewSegments() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(1, List.of(customerView())));

        mockMvc.perform(
                        get("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer bi-analyst-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
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
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1));

        verify(segmentService).findById(SEGMENT_ID);
        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void dualRoleBiAndCampaignManagerCanUpdateSegmentViaPutApi() throws Exception {
        when(jwtService.validateToken("bi-cm-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystAndCampaignManagerClaims());
        when(segmentService.updateSegment(eq(SEGMENT_ID), any(UpdateSegmentCommand.class)))
                .thenReturn(segmentView());

        mockMvc.perform(
                        put("/api/segments/{id}", SEGMENT_ID)
                                .header("Authorization", "Bearer bi-cm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatePayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment updated"));

        verify(segmentService).updateSegment(eq(SEGMENT_ID), any(UpdateSegmentCommand.class));
    }

    @Test
    void dualRoleBiAndCampaignManagerCanCreateSegmentViaPostApi() throws Exception {
        when(jwtService.validateToken("bi-cm-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystAndCampaignManagerClaims());
        when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                .thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer bi-cm-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Segment created"));

        verify(segmentService).createSegment(any(CreateSegmentCommand.class));
    }

    private static void assertMethodUsesCanManageSegments(
            String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = SegmentService.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canManageSegments()");
    }

    private void authenticate(SystemRoleName... roles) {
        List<SystemRoleName> roleList = List.of(roles);
        List<SimpleGrantedAuthority> authorities =
                roleList.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList();
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(
                        BI_USER_ID, "bi.analyst@bayer-westphalian.test", roleList);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private static JwtTokenClaims biAnalystClaims() {
        return new JwtTokenClaims(
                BI_USER_ID,
                "bi.analyst@bayer-westphalian.test",
                List.of(SystemRoleName.BI_ANALYST));
    }

    private static JwtTokenClaims biAnalystAndCampaignManagerClaims() {
        return new JwtTokenClaims(
                BI_USER_ID,
                "bi.cm@bayer-westphalian.test",
                List.of(SystemRoleName.BI_ANALYST, SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static String updatePayload() {
        return """
                {
                  "name": "Updated Munich prospects",
                  "description": "BI must not edit",
                  "visibility": "PRIVATE",
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

    private static String createPayload() {
        return """
                {
                  "name": "BI draft",
                  "description": "Should be denied for BI alone",
                  "visibility": "PRIVATE",
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

    private static SegmentView segmentView() {
        return new SegmentView(
                SEGMENT_ID,
                "Munich prospects",
                "City Munich",
                OWNER_ID,
                "Campaign Manager",
                SegmentVisibility.TEAM,
                List.of(),
                CREATED_AT,
                UPDATED_AT);
    }

    private static CustomerView customerView() {
        return new CustomerView(
                UUID.fromString("20000000-0000-0000-0000-000000000201"),
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
                null);
    }
}
