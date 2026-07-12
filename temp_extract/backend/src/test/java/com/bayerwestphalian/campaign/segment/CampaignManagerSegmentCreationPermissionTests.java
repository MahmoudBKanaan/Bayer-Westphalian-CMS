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

import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.auth.method.SegmentCreateAccess;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
 * KB item: Campaign Manager segment creation permissions (FR-077, role matrix CRUD on segments).
 *
 * <p>Proves Admin and Campaign Manager may create reusable segments at expression, HTTP filter, and
 * service method layers; other roles are denied.
 */
@WebMvcTest(controllers = SegmentController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignManagerSegmentCreationPermissionTests {

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

    private final AuthorizationExpressions authorizationExpressions =
            new AuthorizationExpressions();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canCreateSegmentsExpressionAllowsCampaignManagerAndAdmin() {
        authenticate(SystemRoleName.CAMPAIGN_MANAGER);
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();

        authenticate(SystemRoleName.ADMIN);
        assertThat(authorizationExpressions.canCreateSegments()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = SystemRoleName.class,
            names = {
                "BI_ANALYST",
                "PRODUCT_MANAGER",
                "COMPLIANCE_OFFICER",
                "CUSTOMER_SERVICE_AGENT",
                "SALES_AGENT",
                "MARKETING_ANALYST",
                "EXECUTIVE_VIEWER",
                "SYSTEM_AUDITOR"
            })
    void canCreateSegmentsExpressionDeniesNonCampaignRoles(SystemRoleName role) {
        authenticate(role);
        assertThat(authorizationExpressions.canCreateSegments()).isFalse();
    }

    @Test
    void createSegmentServiceMethodUsesSegmentCreateAccessMetaAnnotation() throws Exception {
        Method createSegment =
                SegmentService.class.getMethod("createSegment", CreateSegmentCommand.class);
        SegmentCreateAccess createAccess = createSegment.getAnnotation(SegmentCreateAccess.class);

        assertThat(createAccess).isNotNull();
        PreAuthorize preAuthorize = SegmentCreateAccess.class.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canCreateSegments()");
    }

    @Test
    void segmentCreateAccessAnnotationUsesCanCreateSegmentsExpression() {
        PreAuthorize preAuthorize = SegmentCreateAccess.class.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("@authz.canCreateSegments()");
    }

    @Test
    void securityConfigurationSegmentCreateRolesAreAdminAndCampaignManager() {
        assertThat(SecurityConfiguration.SEGMENT_CREATE_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    @Test
    void campaignManagerCanCreateSegmentViaPostApi() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(claims(SystemRoleName.CAMPAIGN_MANAGER, "campaign-manager-token"));
        when(segmentService.createSegment(any(CreateSegmentCommand.class))).thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment created"))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"));

        verify(segmentService).createSegment(any(CreateSegmentCommand.class));
    }

    @Test
    void adminCanCreateSegmentViaPostApi() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS))
                .thenReturn(claims(SystemRoleName.ADMIN, "admin-token"));
        when(segmentService.createSegment(any(CreateSegmentCommand.class))).thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Segment created"));

        verify(segmentService).createSegment(any(CreateSegmentCommand.class));
    }

    @ParameterizedTest
    @MethodSource("rolesDeniedSegmentCreation")
    void deniedRolesCannotCreateSegmentViaPostApi(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(claims(role, token));

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Segment created"))));
    }

    @Test
    void unauthenticatedCallerCannotCreateSegment() throws Exception {
        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createSegmentPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Segment created"))));
    }

    private static Stream<SystemRoleName> rolesDeniedSegmentCreation() {
        return Stream.of(
                SystemRoleName.BI_ANALYST,
                SystemRoleName.PRODUCT_MANAGER,
                SystemRoleName.COMPLIANCE_OFFICER,
                SystemRoleName.CUSTOMER_SERVICE_AGENT,
                SystemRoleName.SALES_AGENT,
                SystemRoleName.MARKETING_ANALYST,
                SystemRoleName.EXECUTIVE_VIEWER,
                SystemRoleName.SYSTEM_AUDITOR);
    }

    private void authenticate(SystemRoleName role) {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(
                        OWNER_ID,
                        role.name().toLowerCase() + "@bayer-westphalian.test",
                        List.of(role));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                "access-token",
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private static JwtTokenClaims claims(SystemRoleName role, String tokenId) {
        return new JwtTokenClaims(
                OWNER_ID,
                role.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                List.of(role),
                JwtTokenType.ACCESS,
                "bayer-westphalian-campaign-platform-test",
                Instant.parse("2026-07-08T10:00:00Z"),
                Instant.parse("2026-07-08T10:15:00Z"),
                tokenId);
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
