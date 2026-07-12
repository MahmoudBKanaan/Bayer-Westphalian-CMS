package com.bayerwestphalian.campaign.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
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
class BeneficiaryControllerTests {

    private static final UUID BENEFICIARY_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID POLICYHOLDER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000101");
    private static final UUID BENEFICIARY_CUSTOMER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000102");

    @Mock private BeneficiaryService beneficiaryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new BeneficiaryController(beneficiaryService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesBeneficiaryApiRoute() {
        assertThat(BeneficiaryController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(BeneficiaryController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/beneficiaries");
    }

    @Test
    void searchesBeneficiariesWithKbFilters() throws Exception {
        when(beneficiaryService.searchBeneficiaries(any(BeneficiarySearchCriteria.class)))
                .thenReturn(List.of(beneficiaryView()));

        mockMvc.perform(
                        get("/api/beneficiaries")
                                .param("policyholderCustomerId", POLICYHOLDER_ID.toString())
                                .param("beneficiaryCustomerId", BENEFICIARY_CUSTOMER_ID.toString())
                                .param("guardianConsentRequired", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Beneficiaries loaded"))
                .andExpect(jsonPath("$.data[0].id").value(BENEFICIARY_ID.toString()))
                .andExpect(jsonPath("$.data[0].relationship").value("Grandchild"))
                .andExpect(jsonPath("$.data[0].guardianConsentRequired").value(true));

        ArgumentCaptor<BeneficiarySearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(BeneficiarySearchCriteria.class);
        verify(beneficiaryService).searchBeneficiaries(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().policyholderCustomerId()).isEqualTo(POLICYHOLDER_ID);
        assertThat(criteriaCaptor.getValue().beneficiaryCustomerId())
                .isEqualTo(BENEFICIARY_CUSTOMER_ID);
        assertThat(criteriaCaptor.getValue().guardianConsentRequired()).isTrue();
    }

    @Test
    void getsBeneficiaryById() throws Exception {
        when(beneficiaryService.findById(BENEFICIARY_ID)).thenReturn(beneficiaryView());

        mockMvc.perform(get("/api/beneficiaries/{id}", BENEFICIARY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Beneficiary loaded"))
                .andExpect(jsonPath("$.data.id").value(BENEFICIARY_ID.toString()))
                .andExpect(jsonPath("$.data.policyholderFullName").value("Ada Policyholder"));

        verify(beneficiaryService).findById(BENEFICIARY_ID);
    }

    @Test
    void createsBeneficiary() throws Exception {
        when(beneficiaryService.createBeneficiary(any(CreateBeneficiaryCommand.class)))
                .thenReturn(beneficiaryView());

        mockMvc.perform(
                        post("/api/beneficiaries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "policyholderCustomerId": "%s",
                                          "beneficiaryCustomerId": "%s",
                                          "relationship": "Grandchild",
                                          "guardianName": "Guardian User",
                                          "guardianEmail": "guardian@bayer-westphalian.test",
                                          "guardianConsentRequired": true
                                        }
                                        """
                                                .formatted(
                                                        POLICYHOLDER_ID, BENEFICIARY_CUSTOMER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Beneficiary created"))
                .andExpect(
                        jsonPath("$.data.policyholderCustomerId").value(POLICYHOLDER_ID.toString()))
                .andExpect(jsonPath("$.data.policyholderFullName").value("Ada Policyholder"))
                .andExpect(
                        jsonPath("$.data.beneficiaryCustomerId")
                                .value(BENEFICIARY_CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.beneficiaryFullName").value("Ben Beneficiary"))
                .andExpect(jsonPath("$.data.guardianConsentRequired").value(true))
                .andExpect(jsonPath("$.data.hasGuardianRequirement").value(true));

        ArgumentCaptor<CreateBeneficiaryCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateBeneficiaryCommand.class);
        verify(beneficiaryService).createBeneficiary(commandCaptor.capture());
        assertThat(commandCaptor.getValue().policyholderCustomerId()).isEqualTo(POLICYHOLDER_ID);
        assertThat(commandCaptor.getValue().beneficiaryCustomerId())
                .isEqualTo(BENEFICIARY_CUSTOMER_ID);
        assertThat(commandCaptor.getValue().relationship()).isEqualTo("Grandchild");
        assertThat(commandCaptor.getValue().guardianConsentRequired()).isTrue();
    }

    @Test
    void rejectsInvalidCreateBeneficiaryRequest() throws Exception {
        mockMvc.perform(
                        post("/api/beneficiaries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/beneficiaries"));
    }

    @Test
    void mapsDuplicateBeneficiaryLinkToConflictResponse() throws Exception {
        when(beneficiaryService.createBeneficiary(any(CreateBeneficiaryCommand.class)))
                .thenThrow(
                        new ConflictException(
                                "BENEFICIARY_LINK_EXISTS", "Beneficiary link already exists"));

        mockMvc.perform(
                        post("/api/beneficiaries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "policyholderCustomerId": "%s",
                                          "beneficiaryCustomerId": "%s",
                                          "relationship": "Grandchild",
                                          "guardianConsentRequired": false
                                        }
                                        """
                                                .formatted(
                                                        POLICYHOLDER_ID, BENEFICIARY_CUSTOMER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BENEFICIARY_LINK_EXISTS"))
                .andExpect(jsonPath("$.message").value("Beneficiary link already exists"));
    }

    @Test
    void updatesBeneficiary() throws Exception {
        when(beneficiaryService.updateBeneficiary(
                        any(UUID.class), any(UpdateBeneficiaryCommand.class)))
                .thenReturn(updatedBeneficiaryView());

        mockMvc.perform(
                        put("/api/beneficiaries/{id}", BENEFICIARY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "relationship": "Grandchild - minor",
                                          "guardianName": "Updated Guardian",
                                          "guardianEmail": "updated.guardian@bayer-westphalian.test",
                                          "guardianConsentRequired": true
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Beneficiary updated"))
                .andExpect(jsonPath("$.data.relationship").value("Grandchild - minor"))
                .andExpect(jsonPath("$.data.guardianName").value("Updated Guardian"))
                .andExpect(jsonPath("$.data.guardianConsentRequired").value(true));

        ArgumentCaptor<UpdateBeneficiaryCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateBeneficiaryCommand.class);
        verify(beneficiaryService).updateBeneficiary(eq(BENEFICIARY_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().relationship()).isEqualTo("Grandchild - minor");
        assertThat(commandCaptor.getValue().guardianName()).isEqualTo("Updated Guardian");
        assertThat(commandCaptor.getValue().guardianConsentRequired()).isTrue();
    }

    @Test
    void rejectsInvalidUpdateBeneficiaryRequest() throws Exception {
        mockMvc.perform(
                        put("/api/beneficiaries/{id}", BENEFICIARY_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "relationship": " "
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/beneficiaries/" + BENEFICIARY_ID))
                .andExpect(jsonPath("$.validationErrors[0].field").value("relationship"));
    }

    @Test
    void deletesBeneficiary() throws Exception {
        mockMvc.perform(delete("/api/beneficiaries/{id}", BENEFICIARY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Beneficiary deleted"));

        verify(beneficiaryService).deleteBeneficiary(BENEFICIARY_ID);
    }

    @Test
    void mapsMissingBeneficiaryToNotFoundResponse() throws Exception {
        when(beneficiaryService.findById(BENEFICIARY_ID))
                .thenThrow(new ResourceNotFoundException("Beneficiary", BENEFICIARY_ID));

        mockMvc.perform(get("/api/beneficiaries/{id}", BENEFICIARY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/beneficiaries/" + BENEFICIARY_ID));
    }

    private static BeneficiaryView beneficiaryView() {
        return new BeneficiaryView(
                BENEFICIARY_ID,
                POLICYHOLDER_ID,
                "Ada Policyholder",
                BENEFICIARY_CUSTOMER_ID,
                "Ben Beneficiary",
                "Grandchild",
                "Guardian User",
                "guardian@bayer-westphalian.test",
                true,
                true,
                Instant.parse("2026-07-03T12:00:00Z"));
    }

    private static BeneficiaryView updatedBeneficiaryView() {
        return new BeneficiaryView(
                BENEFICIARY_ID,
                POLICYHOLDER_ID,
                "Ada Policyholder",
                BENEFICIARY_CUSTOMER_ID,
                "Ben Beneficiary",
                "Grandchild - minor",
                "Updated Guardian",
                "updated.guardian@bayer-westphalian.test",
                true,
                true,
                Instant.parse("2026-07-03T12:00:00Z"));
    }
}
