package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.campaign.CommunicationChannel;
import com.bayerwestphalian.campaign.campaign.ContactEventSearchCriteria;
import com.bayerwestphalian.campaign.campaign.ContactEventSearchRequest;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.campaign.ContactEventView;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** KB item 333: contact timeline endpoint. */
@ExtendWith(MockitoExtension.class)
class CommunicationControllerTests {

    private static final UUID EVENT_ID =
            UUID.fromString("63000000-0000-0000-0000-000000000333");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000333");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000333");
    private static final UUID CREATED_BY_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000333");

    @Mock private CommunicationService communicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new CommunicationController(communicationService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesContactEventApiRoute() {
        assertThat(CommunicationController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(CommunicationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/contact-events");
    }

    @Test
    void exposesContactTimelineGetEndpointWithReadAuthorization() throws Exception {
        Method method =
                CommunicationController.class.getMethod(
                        "getContactTimeline", ContactEventSearchRequest.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/timeline");
        assertThat(preAuthorize.value())
                .isEqualTo("@authz.canReadCustomers() || @authz.canReadCampaigns()");
    }

    @Test
    void loadsContactTimelineWithKbFilters() throws Exception {
        when(communicationService.searchContactEvents(any(ContactEventSearchCriteria.class)))
                .thenReturn(List.of(contactEventView()));

        mockMvc.perform(
                        get("/api/contact-events/timeline")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("campaignId", CAMPAIGN_ID.toString())
                                .param("eventType", "CLICKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contact timeline loaded"))
                .andExpect(jsonPath("$.data[0].id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data[0].campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data[0].eventType").value("CLICKED"));

        ArgumentCaptor<ContactEventSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ContactEventSearchCriteria.class);
        verify(communicationService).searchContactEvents(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(criteriaCaptor.getValue().campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(criteriaCaptor.getValue().eventType()).isEqualTo(ContactEventType.CLICKED);
    }

    @Test
    void loadsUnfilteredContactTimeline() throws Exception {
        when(communicationService.searchContactEvents(any(ContactEventSearchCriteria.class)))
                .thenReturn(List.of(contactEventView()));

        mockMvc.perform(get("/api/contact-events/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact timeline loaded"))
                .andExpect(jsonPath("$.data[0].channel").value("EMAIL"));

        ArgumentCaptor<ContactEventSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ContactEventSearchCriteria.class);
        verify(communicationService).searchContactEvents(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().customerId()).isNull();
        assertThat(criteriaCaptor.getValue().campaignId()).isNull();
        assertThat(criteriaCaptor.getValue().eventType()).isNull();
    }

    private static ContactEventView contactEventView() {
        return new ContactEventView(
                EVENT_ID,
                CUSTOMER_ID,
                "Ada Contact",
                CAMPAIGN_ID,
                "Renewal campaign",
                CommunicationChannel.EMAIL,
                ContactEventType.CLICKED,
                null,
                "clickedUrl=https://example.test/offer",
                Instant.parse("2026-07-10T15:00:00Z"),
                CREATED_BY_ID,
                "Contact Event User");
    }
}
