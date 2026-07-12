package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class SegmentControllerTests {

    private static final UUID SEGMENT_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID CRITERION_ID =
            UUID.fromString("42000000-0000-0000-0000-000000000101");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000201");
    private static final Instant CREATED_AT = Instant.parse("2026-07-08T10:15:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-08T10:30:00Z");

    @Mock private SegmentService segmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new SegmentController(segmentService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesSegmentApiRoute() {
        assertThat(SegmentController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(SegmentController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/segments");
    }

    @Test
    void listsSegmentsWithoutFilters() throws Exception {
        when(segmentService.searchSegments(any(SegmentSearchCriteria.class)))
                .thenReturn(List.of(segmentView()));

        mockMvc.perform(get("/api/segments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segments loaded"))
                .andExpect(jsonPath("$.data[0].id").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Munich prospects"))
                .andExpect(jsonPath("$.data[0].visibility").value("TEAM"));

        ArgumentCaptor<SegmentSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(SegmentSearchCriteria.class);
        verify(segmentService).searchSegments(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().ownerUserId()).isNull();
        assertThat(criteriaCaptor.getValue().visibility()).isNull();
    }

    @Test
    void listsSegmentsWithKbFilters() throws Exception {
        when(segmentService.searchSegments(any(SegmentSearchCriteria.class)))
                .thenReturn(List.of(segmentView()));

        mockMvc.perform(
                        get("/api/segments")
                                .param("term", "munich")
                                .param("ownerUserId", OWNER_ID.toString())
                                .param("visibility", "TEAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ownerUserId").value(OWNER_ID.toString()));

        ArgumentCaptor<SegmentSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(SegmentSearchCriteria.class);
        verify(segmentService).searchSegments(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("munich");
        assertThat(criteriaCaptor.getValue().ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(criteriaCaptor.getValue().visibility()).isEqualTo(SegmentVisibility.TEAM);
    }

    @Test
    void rejectsOversizedSegmentSearchTerm() throws Exception {
        mockMvc.perform(get("/api/segments").param("term", "x".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments"));
    }

    @Test
    void getsSegmentById() throws Exception {
        when(segmentService.findById(SEGMENT_ID)).thenReturn(segmentView());

        mockMvc.perform(get("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment loaded"))
                .andExpect(jsonPath("$.data.id").value(SEGMENT_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                .andExpect(jsonPath("$.data.ownerFullName").value("Campaign Manager"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"))
                .andExpect(jsonPath("$.data.criteria[0].operator").value("EQUALS"))
                .andExpect(jsonPath("$.data.criteria[0].value").value("Munich"));

        verify(segmentService).findById(SEGMENT_ID);
    }

    @Test
    void createsSegment() throws Exception {
        when(segmentService.createSegment(any(CreateSegmentCommand.class))).thenReturn(segmentView());

        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
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
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment created"))
                .andExpect(jsonPath("$.data.name").value("Munich prospects"))
                .andExpect(jsonPath("$.data.visibility").value("TEAM"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("city"));

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
    void rejectsInvalidCreateSegmentRequest() throws Exception {
        mockMvc.perform(
                        post("/api/segments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments"));
    }

    @Test
    void updatesSegment() throws Exception {
        when(segmentService.updateSegment(any(UUID.class), any(UpdateSegmentCommand.class)))
                .thenReturn(updatedSegmentView());

        mockMvc.perform(
                        put("/api/segments/{id}", SEGMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Updated Munich prospects",
                                          "description": "Refined Munich audience",
                                          "visibility": "PRIVATE",
                                          "criteria": [
                                            {
                                              "fieldName": "customer_type",
                                              "operator": "EQUALS",
                                              "value": "PROSPECT",
                                              "joinOperator": "AND"
                                            }
                                          ]
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Segment updated"))
                .andExpect(jsonPath("$.data.name").value("Updated Munich prospects"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.criteria[0].fieldName").value("customer_type"));

        ArgumentCaptor<UpdateSegmentCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateSegmentCommand.class);
        verify(segmentService).updateSegment(eq(SEGMENT_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().name()).isEqualTo("Updated Munich prospects");
        assertThat(commandCaptor.getValue().visibility()).isEqualTo(SegmentVisibility.PRIVATE);
        assertThat(commandCaptor.getValue().criteria()).hasSize(1);
    }

    @Test
    void rejectsInvalidUpdateSegmentRequest() throws Exception {
        mockMvc.perform(
                        put("/api/segments/{id}", SEGMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": " "
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID))
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
    }

    @Test
    void deletesSegment() throws Exception {
        mockMvc.perform(delete("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Segment deleted"));

        verify(segmentService).deleteSegment(SEGMENT_ID);
    }

    @Test
    void previewsSegmentAudience() throws Exception {
        when(segmentService.previewSegment(any(SegmentPreviewCommand.class)))
                .thenReturn(previewView());

        mockMvc.perform(
                        post("/api/segments/preview")
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
                .andExpect(jsonPath("$.data.totalAudienceCount").value(1))
                .andExpect(jsonPath("$.data.matchingCustomers[0].id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.matchingCustomers[0].city").value("Munich"));

        ArgumentCaptor<SegmentPreviewCommand> commandCaptor =
                ArgumentCaptor.forClass(SegmentPreviewCommand.class);
        verify(segmentService).previewSegment(commandCaptor.capture());
        assertThat(commandCaptor.getValue().criteria()).hasSize(1);
        assertThat(commandCaptor.getValue().criteria().getFirst().fieldName()).isEqualTo("city");
    }

    @Test
    void mapsMissingSegmentToNotFoundResponse() throws Exception {
        when(segmentService.findById(SEGMENT_ID))
                .thenThrow(new ResourceNotFoundException("Segment", SEGMENT_ID));

        mockMvc.perform(get("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
    }

    @Test
    void mapsForbiddenSegmentAccessToForbiddenResponse() throws Exception {
        when(segmentService.findById(SEGMENT_ID))
                .thenThrow(new ForbiddenException("Private segment is not accessible"));

        mockMvc.perform(get("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
    }

    @Test
    void rejectsInvalidPreviewCriteriaRequest() throws Exception {
        mockMvc.perform(
                        post("/api/segments/preview")
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
    void mapsForbiddenSegmentOnUpdateToForbiddenResponse() throws Exception {
        when(segmentService.updateSegment(any(UUID.class), any(UpdateSegmentCommand.class)))
                .thenThrow(new ForbiddenException("Segment is not owned by the current user"));

        mockMvc.perform(
                        put("/api/segments/{id}", SEGMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Updated Munich prospects"
                                        }
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
    }

    @Test
    void mapsForbiddenSegmentOnDeleteToForbiddenResponse() throws Exception {
        doThrow(new ForbiddenException("Segment is not owned by the current user"))
                .when(segmentService)
                .deleteSegment(SEGMENT_ID);

        mockMvc.perform(delete("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
    }

    @Test
    void mapsMissingSegmentOnDeleteToNotFoundResponse() throws Exception {
        doThrow(new ResourceNotFoundException("Segment", SEGMENT_ID))
                .when(segmentService)
                .deleteSegment(SEGMENT_ID);

        mockMvc.perform(delete("/api/segments/{id}", SEGMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/segments/" + SEGMENT_ID));
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

    private static SegmentView updatedSegmentView() {
        return new SegmentView(
                SEGMENT_ID,
                "Updated Munich prospects",
                "Refined Munich audience",
                OWNER_ID,
                "Campaign Manager",
                SegmentVisibility.PRIVATE,
                List.of(
                        new SegmentCriteriaView(
                                CRITERION_ID,
                                SEGMENT_ID,
                                "customer_type",
                                SegmentOperator.EQUALS,
                                "PROSPECT",
                                null,
                                SegmentJoinOperator.AND)),
                CREATED_AT,
                UPDATED_AT);
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