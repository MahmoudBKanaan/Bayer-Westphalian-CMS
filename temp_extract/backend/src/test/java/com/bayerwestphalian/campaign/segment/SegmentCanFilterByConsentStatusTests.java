package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SegmentCanFilterByConsentStatusTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000201");
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
    void campaignManagerCanPreviewAudienceFilteredByConsentStatus() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(1, List.of(customerView())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(consentStatusPreviewPayload("given")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].fullName").value("Lena Mueller"));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(1);
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName())
                .isEqualTo("consent_status");
        assertThat(commandCaptor.getValue().criteria().getFirst().value()).isEqualTo("given");
    }

    @Test
    void biAnalystCanPreviewAudienceWithValidMarketingConsent() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(1, List.of(customerView())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "marketing_consent",
                                              "operator": "EQUALS",
                                              "value": "true",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1));

        verify(segmentService).previewSegment(any(SegmentPreviewCommand.class));
    }

    @Test
    void campaignManagerCanPreviewAudienceExcludingOptOuts() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(1, List.of(customerView())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "opt_out",
                                              "operator": "EQUALS",
                                              "value": "false",
                                              "logicalGroup": "consent",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "consent_type",
                                              "operator": "EQUALS",
                                              "value": "MARKETING_EMAIL",
                                              "logicalGroup": "consent",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchingCustomers[0].fullName").value("Lena Mueller"));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(2);
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName()).isEqualTo("opt_out");
        assertThat(commandCaptor.getValue().criteria().get(1).fieldName())
                .isEqualTo("consent_type");
    }

    @Test
    void biAnalystCanPreviewAudienceFilteredByConsentStatusInList() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(2, List.of(customerView(), customerView())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "consent_status",
                                              "operator": "IN",
                                              "value": "WITHDRAWN,REJECTED",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria().getFirst().operator())
                .isEqualTo(SegmentOperator.IN);
        assertThat(commandCaptor.getValue().criteria().getFirst().value())
                .isEqualTo("WITHDRAWN,REJECTED");
    }

    @Test
    void campaignManagerCanPreviewAudienceFilteredByGuardianConsent() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(1, List.of(customerView())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "guardian_consent",
                                              "operator": "EQUALS",
                                              "value": "true",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName())
                .isEqualTo("guardian_consent");
        assertThat(commandCaptor.getValue().criteria().getFirst().value()).isEqualTo("true");
    }

    @Test
    void campaignManagerCanCreateSegmentWithConsentCriteria() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                .thenReturn(
                        new SegmentView(
                                UUID.fromString("42000000-0000-0000-0000-000000000041"),
                                "Marketing consented",
                                "GIVEN marketing email, not opted out",
                                OWNER_ID,
                                "Campaign Manager",
                                SegmentVisibility.TEAM,
                                List.of(),
                                CREATED_AT,
                                UPDATED_AT));

        mockMvc.perform(
                        post("/api/segments")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Marketing consented",
                                          "description": "GIVEN marketing email, not opted out",
                                          "visibility": "TEAM",
                                          "criteria": [
                                            {
                                              "fieldName": "consent_status",
                                              "operator": "EQUALS",
                                              "value": "GIVEN",
                                              "logicalGroup": "consent",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "opt_out",
                                              "operator": "EQUALS",
                                              "value": "false",
                                              "logicalGroup": "consent",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Segment created"));

        ArgumentCaptor<CreateSegmentCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateSegmentCommand.class);
        verify(segmentService).createSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(2);
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName())
                .isEqualTo("consent_status");
        assertThat(commandCaptor.getValue().criteria().getFirst().value()).isEqualTo("GIVEN");
        assertThat(commandCaptor.getValue().criteria().get(1).fieldName()).isEqualTo("opt_out");
        assertThat(commandCaptor.getValue().criteria().get(1).value()).isEqualTo("false");
    }

    private static JwtTokenClaims campaignManagerClaims() {
        return new JwtTokenClaims(
                OWNER_ID,
                "campaign.manager@bayer-westphalian.test",
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }

    private static JwtTokenClaims biAnalystClaims() {
        return new JwtTokenClaims(
                UUID.fromString("10000000-0000-0000-0000-000000009903"),
                "bi.analyst@bayer-westphalian.test",
                List.of(SystemRoleName.BI_ANALYST));
    }

    private static String consentStatusPreviewPayload(String consentStatusValue) {
        return """
                {
                  "criteria": [
                    {
                      "fieldName": "consent_status",
                      "operator": "EQUALS",
                      "value": "%s",
                      "joinOperator": "AND"
                    }
                  ]
                }
                """
                .formatted(consentStatusValue);
    }

    private static CustomerView customerView() {
        return new CustomerView(
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
                null);
    }
}
