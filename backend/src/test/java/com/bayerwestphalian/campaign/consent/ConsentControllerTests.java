package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class ConsentControllerTests {

    private static final UUID CONSENT_ID = UUID.fromString("22000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000101");
    private static final UUID CREATED_BY_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");

    @Mock private ConsentService consentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ConsentController(consentService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesConsentApiRoute() {
        assertThat(ConsentController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(ConsentController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/consents");
    }

    @Test
    void listsConsentsWithKbFilters() throws Exception {
        when(consentService.listConsents(any(ConsentSearchCriteria.class)))
                .thenReturn(List.of(consentView()));

        mockMvc.perform(
                        get("/api/consents")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("consentType", "MARKETING_EMAIL")
                                .param("status", "GIVEN")
                                .param("validOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Consents loaded"))
                .andExpect(jsonPath("$.data[0].id").value(CONSENT_ID.toString()))
                .andExpect(jsonPath("$.data[0].consentType").value("MARKETING_EMAIL"))
                .andExpect(jsonPath("$.data[0].status").value("GIVEN"))
                .andExpect(jsonPath("$.data[0].valid").value(true));

        ArgumentCaptor<ConsentSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ConsentSearchCriteria.class);
        verify(consentService).listConsents(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(criteriaCaptor.getValue().consentType()).isEqualTo(ConsentType.MARKETING_EMAIL);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(ConsentStatus.GIVEN);
        assertThat(criteriaCaptor.getValue().validOnly()).isTrue();
    }

    @Test
    void getsLatestConsentStatus() throws Exception {
        when(consentService.getConsentStatus(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(Optional.of(consentView()));

        mockMvc.perform(
                        get("/api/consents/status")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("consentType", "MARKETING_EMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consent status loaded"))
                .andExpect(jsonPath("$.data.id").value(CONSENT_ID.toString()))
                .andExpect(jsonPath("$.data.customerFullName").value("Anna Keller"));

        verify(consentService).getConsentStatus(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @Test
    void checksCommunicationEligibility() throws Exception {
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .thenReturn(true);

        mockMvc.perform(
                        get("/api/consents/eligibility")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("consentType", "MARKETING_SMS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consent eligibility checked"))
                .andExpect(jsonPath("$.data").value(true));

        verify(consentService).isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS);
    }

    @Test
    void recordsConsent() throws Exception {
        when(consentService.recordConsent(any(RecordConsentCommand.class)))
                .thenReturn(consentView());

        mockMvc.perform(
                        post("/api/consents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerId": "%s",
                                          "consentType": "MARKETING_EMAIL",
                                          "status": "GIVEN",
                                          "purpose": "Marketing email consent",
                                          "source": "WEB_FORM",
                                          "grantedAt": "2026-07-01T12:00:00Z",
                                          "expiresAt": "2027-07-01T12:00:00Z",
                                          "evidenceFileUrl": "s3://evidence/email.pdf",
                                          "createdBy": "%s"
                                        }
                                        """
                                                .formatted(CUSTOMER_ID, CREATED_BY_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Consent recorded"))
                .andExpect(jsonPath("$.data.consentType").value("MARKETING_EMAIL"))
                .andExpect(jsonPath("$.data.valid").value(true));

        ArgumentCaptor<RecordConsentCommand> commandCaptor =
                ArgumentCaptor.forClass(RecordConsentCommand.class);
        verify(consentService).recordConsent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(commandCaptor.getValue().consentType()).isEqualTo(ConsentType.MARKETING_EMAIL);
        assertThat(commandCaptor.getValue().status()).isEqualTo(ConsentStatus.GIVEN);
        assertThat(commandCaptor.getValue().createdBy()).isEqualTo(CREATED_BY_ID);
    }

    @Test
    void rejectsInvalidRecordConsentRequest() throws Exception {
        mockMvc.perform(post("/api/consents").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/consents"));
    }

    @Test
    void withdrawsConsent() throws Exception {
        when(consentService.withdrawConsent(any(WithdrawConsentCommand.class)))
                .thenReturn(withdrawnConsentView());

        mockMvc.perform(
                        post("/api/consents/withdraw")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "consentRecordId": "%s",
                                          "withdrawnAt": "2026-08-01T12:00:00Z"
                                        }
                                        """
                                                .formatted(CONSENT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consent withdrawn"))
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.data.requiresAction").value(true));

        ArgumentCaptor<WithdrawConsentCommand> commandCaptor =
                ArgumentCaptor.forClass(WithdrawConsentCommand.class);
        verify(consentService).withdrawConsent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().consentRecordId()).isEqualTo(CONSENT_ID);
        assertThat(commandCaptor.getValue().withdrawnAt())
                .isEqualTo(Instant.parse("2026-08-01T12:00:00Z"));
    }

    @Test
    void mapsMissingConsentStatusToNotFoundResponse() throws Exception {
        when(consentService.getConsentStatus(CUSTOMER_ID, ConsentType.GUARDIAN))
                .thenThrow(new ResourceNotFoundException("ConsentRecord", CONSENT_ID));

        mockMvc.perform(
                        get("/api/consents/status")
                                .param("customerId", CUSTOMER_ID.toString())
                                .param("consentType", "GUARDIAN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/consents/status"));
    }

    private static ConsentRecordView consentView() {
        return new ConsentRecordView(
                CONSENT_ID,
                CUSTOMER_ID,
                "Anna Keller",
                ConsentType.MARKETING_EMAIL,
                ConsentStatus.GIVEN,
                "Marketing email consent",
                "WEB_FORM",
                Instant.parse("2026-07-01T12:00:00Z"),
                null,
                Instant.parse("2027-07-01T12:00:00Z"),
                "s3://evidence/email.pdf",
                CREATED_BY_ID,
                "Customer Service Agent",
                Instant.parse("2026-07-01T12:00:00Z"),
                true,
                false);
    }

    private static ConsentRecordView withdrawnConsentView() {
        return new ConsentRecordView(
                CONSENT_ID,
                CUSTOMER_ID,
                "Anna Keller",
                ConsentType.MARKETING_EMAIL,
                ConsentStatus.WITHDRAWN,
                "Marketing email consent",
                "WEB_FORM",
                Instant.parse("2026-07-01T12:00:00Z"),
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2027-07-01T12:00:00Z"),
                "s3://evidence/email.pdf",
                CREATED_BY_ID,
                "Customer Service Agent",
                Instant.parse("2026-07-01T12:00:00Z"),
                false,
                true);
    }
}
