package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * KB item 217 / item 243 / FR-050 / FR-057: create campaign endpoint {@code POST /api/campaigns}
 * creates a draft campaign for Campaign Manager (and Admin).
 */
@WebMvcTest(controllers = CampaignController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class CampaignCanBeCreatedTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000201");
    private static final UUID PRODUCT_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-07-09T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-09T10:30:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;

    @MockBean private JwtService jwtService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void campaignManagerCanCreateDraftCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.createCampaign(any(CreateCampaignCommand.class)))
                .thenReturn(draftCampaignView());

        mockMvc.perform(
                        post("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCampaignPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign created"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data.objective").value("Promote life insurance renewals"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.ownerUserId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.data.segmentId").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.messageSubject").value("Renew your cover"))
                .andExpect(jsonPath("$.data.messageBody").value("Dear customer, ..."))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-30"))
                .andExpect(jsonPath("$.data.productIds[0]").value(PRODUCT_ID.toString()));

        ArgumentCaptor<CreateCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateCampaignCommand.class);
        verify(campaignService).createCampaign(commandCaptor.capture());
        CreateCampaignCommand command = commandCaptor.getValue();
        assertThat(command.name()).isEqualTo("Life renewal outreach");
        assertThat(command.objective()).isEqualTo("Promote life insurance renewals");
        assertThat(command.segmentId()).isEqualTo(SEGMENT_ID);
        assertThat(command.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(command.messageSubject()).isEqualTo("Renew your cover");
        assertThat(command.messageBody()).isEqualTo("Dear customer, ...");
        assertThat(command.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(command.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(command.productIds()).containsExactly(PRODUCT_ID);
    }

    @Test
    void adminCanCreateDraftCampaignViaPostApi() throws Exception {
        when(jwtService.validateToken("admin-token", JwtTokenType.ACCESS)).thenReturn(adminClaims());
        when(campaignService.createCampaign(any(CreateCampaignCommand.class)))
                .thenReturn(draftCampaignView());

        mockMvc.perform(
                        post("/api/campaigns")
                                .header("Authorization", "Bearer admin-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCampaignPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Campaign created"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(campaignService).createCampaign(any(CreateCampaignCommand.class));
    }

    @Test
    void createsMinimalDraftCampaignWithRequiredFieldsOnly() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.createCampaign(any(CreateCampaignCommand.class)))
                .thenReturn(
                        new CampaignView(
                CAMPAIGN_ID,
                "Minimal draft",
                "Minimal objective",
                CampaignStatus.DRAFT,
                OWNER_ID,
                "Campaign Manager",
                null,
                null,
                CampaignChannel.SMS,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                CREATED_AT,
                UPDATED_AT));

        mockMvc.perform(
                        post("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Minimal draft",
                                          "objective": "Minimal objective",
                                          "channel": "SMS"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Minimal draft"))
                .andExpect(jsonPath("$.data.channel").value("SMS"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        ArgumentCaptor<CreateCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateCampaignCommand.class);
        verify(campaignService).createCampaign(commandCaptor.capture());
        assertThat(commandCaptor.getValue().segmentId()).isNull();
        assertThat(commandCaptor.getValue().productIds()).isEmpty();
        assertThat(commandCaptor.getValue().channel()).isEqualTo(CampaignChannel.SMS);
    }

    @ParameterizedTest
    @MethodSource("rolesDeniedCampaignCreation")
    void deniedRolesCannotCreateCampaignViaPostApi(SystemRoleName role) throws Exception {
        String token = role.name().toLowerCase() + "-token";
        when(jwtService.validateToken(token, JwtTokenType.ACCESS)).thenReturn(roleClaims(role));

        mockMvc.perform(
                        post("/api/campaigns")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCampaignPayload()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("Campaign created"))));

        verify(campaignService, never()).createCampaign(any(CreateCampaignCommand.class));
    }

    @Test
    void unauthenticatedCallerCannotCreateCampaign() throws Exception {
        mockMvc.perform(
                        post("/api/campaigns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCampaignPayload()))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(not(containsString("Campaign created"))));

        verify(campaignService, never()).createCampaign(any(CreateCampaignCommand.class));
    }

    @Test
    void rejectsInvalidCreateCampaignPayload() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());

        mockMvc.perform(
                        post("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": " ",
                                          "objective": " ",
                                          "channel": null
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns"));

        verify(campaignService, never()).createCampaign(any(CreateCampaignCommand.class));
    }

    @Test
    void securityConfigurationAllowsAdminAndCampaignManagerToPostCampaigns() {
        assertThat(SecurityConfiguration.CAMPAIGN_MANAGER_ROLES)
                .containsExactly(
                        SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    @Test
    void createCampaignEndpointIsPostOnCampaignsCollection() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(campaignService.createCampaign(any(CreateCampaignCommand.class)))
                .thenReturn(draftCampaignView());

        mockMvc.perform(
                        post("/api/campaigns")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCampaignPayload()))
                .andExpect(status().isCreated());

        // Collection create (not nested resource path)
        verify(campaignService).createCampaign(any(CreateCampaignCommand.class));
    }

    private static Stream<SystemRoleName> rolesDeniedCampaignCreation() {
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

    private static JwtTokenClaims roleClaims(SystemRoleName role) {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009902"),
                role.name().toLowerCase().replace('_', '.') + "@bayer-westphalian.test",
                List.of(role));
    }

    private static String createCampaignPayload() {
        return """
                {
                  "name": "Life renewal outreach",
                  "objective": "Promote life insurance renewals",
                  "segmentId": "%s",
                  "channel": "EMAIL",
                  "messageSubject": "Renew your cover",
                  "messageBody": "Dear customer, ...",
                  "startDate": "2026-09-01",
                  "endDate": "2026-09-30",
                  "productIds": ["%s"]
                }
                """
                .formatted(SEGMENT_ID, PRODUCT_ID);
    }

    private static CampaignView draftCampaignView() {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                CampaignStatus.DRAFT,
                OWNER_ID,
                "Campaign Manager",
                SEGMENT_ID,
                "Munich prospects",
                CampaignChannel.EMAIL,
                "Renew your cover",
                "Dear customer, ...",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null,
                null,
                null,
                null,
                List.of(PRODUCT_ID),
                CREATED_AT,
                UPDATED_AT);
    }
}
