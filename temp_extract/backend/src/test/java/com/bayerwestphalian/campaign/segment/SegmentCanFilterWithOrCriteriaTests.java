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

/**
 * KB FR-078 / items 177 and 197: HTTP preview accepts multi-criteria OR joins so disjunctive
 * audiences return the correct union (and mixed AND/OR left-to-right chains).
 */
@WebMvcTest(controllers = SegmentController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SegmentCanFilterWithOrCriteriaTests {

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
    void campaignManagerCanPreviewAudienceWithOrJoinedCityCriteria() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(2, List.of(munichCustomer(), berlinCustomer())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
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
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Berlin",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(2);
        assertThat(commandCaptor.getValue().criteria().get(0).joinOperator())
                .isEqualTo(SegmentJoinOperator.AND);
        assertThat(commandCaptor.getValue().criteria().get(1).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
        assertThat(commandCaptor.getValue().criteria().get(1).value()).isEqualTo("Berlin");
    }

    @Test
    void biAnalystCanPreviewAudienceWithCrossFieldOrCriteria() throws Exception {
        when(jwtService.validateToken("bi-analyst-token", JwtTokenType.ACCESS))
                .thenReturn(biAnalystClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(2, List.of(munichCustomer(), berlinCustomer())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer bi-analyst-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "customer_type",
                                              "operator": "EQUALS",
                                              "value": "PROSPECT",
                                              "logicalGroup": "audience",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "logicalGroup": "audience",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(2));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria().get(1).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
        assertThat(commandCaptor.getValue().criteria().get(1).fieldName()).isEqualTo("city");
    }

    @Test
    void campaignManagerCanPreviewMixedAndOrChain() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(1, List.of(munichCustomer())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "customer_type",
                                              "operator": "EQUALS",
                                              "value": "PROSPECT",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Berlin",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchingCustomers[0].city").value("Munich"));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(3);
        assertThat(commandCaptor.getValue().criteria().get(0).joinOperator())
                .isEqualTo(SegmentJoinOperator.AND);
        assertThat(commandCaptor.getValue().criteria().get(1).joinOperator())
                .isEqualTo(SegmentJoinOperator.AND);
        assertThat(commandCaptor.getValue().criteria().get(2).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
    }

    @Test
    void campaignManagerCanPreviewThreeWayCityOrChain() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(
                        SegmentPreviewView.of(
                                3, List.of(munichCustomer(), berlinCustomer(), hamburgCustomer())));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
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
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Berlin",
                                              "joinOperator": "OR"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Hamburg",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(3);
        assertThat(commandCaptor.getValue().criteria().get(1).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
        assertThat(commandCaptor.getValue().criteria().get(2).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
        assertThat(commandCaptor.getValue().criteria())
                .extracting(CreateSegmentCriteriaCommand::value)
                .containsExactly("Munich", "Berlin", "Hamburg");
    }

    @Test
    void campaignManagerCanCreateSegmentWithOrJoinedCriteria() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.createSegment(any(CreateSegmentCommand.class)))
                .thenReturn(
                        new SegmentView(
                                UUID.fromString("42000000-0000-0000-0000-000000000061"),
                                "Munich or Berlin",
                                "OR city union",
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
                                          "name": "Munich or Berlin",
                                          "description": "OR city union",
                                          "visibility": "TEAM",
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Munich",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Berlin",
                                              "joinOperator": "OR"
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
        assertThat(commandCaptor.getValue().criteria().get(0).joinOperator())
                .isEqualTo(SegmentJoinOperator.AND);
        assertThat(commandCaptor.getValue().criteria().get(1).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
    }

    @Test
    void previewWithOrCriteriaCanReturnEmptyUnion() throws Exception {
        when(jwtService.validateToken("campaign-manager-token", JwtTokenType.ACCESS))
                .thenReturn(campaignManagerClaims());
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(SegmentPreviewView.of(0, List.of()));

        mockMvc.perform(
                        post("/api/segments/preview")
                                .header("Authorization", "Bearer campaign-manager-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "criteria": [
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Vienna",
                                              "joinOperator": "AND"
                                            },
                                            {
                                              "fieldName": "city",
                                              "operator": "EQUALS",
                                              "value": "Zurich",
                                              "joinOperator": "OR"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAudienceCount").value(0))
                .andExpect(jsonPath("$.data.matchingCustomers").isEmpty());

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria().get(1).joinOperator())
                .isEqualTo(SegmentJoinOperator.OR);
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

    private static CustomerView munichCustomer() {
        return customerView(CUSTOMER_ID, "Lena", "Mueller", "Munich", CustomerType.PROSPECT);
    }

    private static CustomerView berlinCustomer() {
        return customerView(
                UUID.fromString("20000000-0000-0000-0000-000000000202"),
                "Tom",
                "Schmidt",
                "Berlin",
                CustomerType.CUSTOMER);
    }

    private static CustomerView hamburgCustomer() {
        return customerView(
                UUID.fromString("20000000-0000-0000-0000-000000000203"),
                "Max",
                "Bauer",
                "Hamburg",
                CustomerType.CUSTOMER);
    }

    private static CustomerView customerView(
            UUID id, String firstName, String lastName, String city, CustomerType type) {
        return new CustomerView(
                id,
                type,
                firstName,
                lastName,
                firstName + " " + lastName,
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@bayer-westphalian.test",
                null,
                null,
                city,
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
