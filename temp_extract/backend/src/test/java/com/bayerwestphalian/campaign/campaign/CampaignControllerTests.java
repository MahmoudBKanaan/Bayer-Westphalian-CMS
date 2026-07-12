package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.segment.SegmentExclusionReasonSummary;
import com.bayerwestphalian.campaign.segment.SegmentPreviewView;
import com.bayerwestphalian.campaign.segment.SegmentService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KB item 216: CampaignController exposes campaign lifecycle REST endpoints under {@code
 * /api/campaigns}.
 */
@ExtendWith(MockitoExtension.class)
class CampaignControllerTests {

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

    @Mock private CampaignService campaignService;
    @Mock private SegmentService segmentService;
    @Mock private CampaignRecipientService campaignRecipientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CampaignController controller = new CampaignController(campaignService);
        ReflectionTestUtils.setField(controller, "segmentService", segmentService);
        ReflectionTestUtils.setField(controller, "campaignRecipientService", campaignRecipientService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesCampaignApiRoute() {
        assertThat(CampaignController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(CampaignController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/campaigns");
    }

    @Test
    void exposesKbLaunchCampaignPostEndpoint() throws Exception {
        PostMapping postMapping =
                CampaignController.class
                        .getMethod("launchCampaign", UUID.class)
                        .getAnnotation(PostMapping.class);

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/{id}/launch");
    }

    @Test
    void exposesRoleBasedCampaignPermissionsAtControllerBoundary() throws Exception {
        assertPreAuthorize(
                "listCampaigns", "@authz.canReadCampaigns()", CampaignSearchRequest.class);
        assertPreAuthorize("getCampaign", "@authz.canReadCampaigns()", UUID.class);
        assertPreAuthorize("listCampaignProducts", "@authz.canReadCampaigns()", UUID.class);
        assertPreAuthorize("getCampaignSegment", "@authz.canReadCampaigns()", UUID.class);
        assertPreAuthorize("previewCampaignRecipients", "@authz.canReadCampaigns()", UUID.class);
        assertPreAuthorize("listEligibleRecipients", "@authz.canReadCampaigns()", UUID.class);
        assertPreAuthorize("listExcludedRecipients", "@authz.canReadCampaigns()", UUID.class);
        assertPreAuthorize(
                "summarizeCampaignRecipients", "@authz.canReadCampaigns()", UUID.class);

        assertPreAuthorize(
                "createCampaign", "@authz.canManageCampaigns()", CreateCampaignRequest.class);
        assertPreAuthorize(
                "updateCampaign",
                "@authz.canManageCampaigns()",
                UUID.class,
                UpdateCampaignRequest.class);
        assertPreAuthorize(
                "selectCampaignProducts",
                "@authz.canManageCampaigns()",
                UUID.class,
                SelectCampaignProductsRequest.class);
        assertPreAuthorize(
                "selectCampaignSegment",
                "@authz.canManageCampaigns()",
                UUID.class,
                SelectCampaignSegmentRequest.class);
        assertPreAuthorize("submitCampaign", "@authz.canManageCampaigns()", UUID.class);
        assertPreAuthorize("launchCampaign", "@authz.canManageCampaigns()", UUID.class);
        assertPreAuthorize("pauseCampaign", "@authz.canManageCampaigns()", UUID.class);
        assertPreAuthorize("completeCampaign", "@authz.canManageCampaigns()", UUID.class);
        assertPreAuthorize("archiveCampaign", "@authz.canManageCampaigns()", UUID.class);

        assertPreAuthorize(
                "approveCampaign",
                "@authz.canReviewCampaigns()",
                UUID.class,
                ApproveCampaignRequest.class);
        assertPreAuthorize(
                "rejectCampaign",
                "@authz.canReviewCampaigns()",
                UUID.class,
                RejectCampaignRequest.class);
        assertPreAuthorize(
                "recordComplianceReviewNotes",
                "@authz.canReviewCampaigns()",
                UUID.class,
                RecordComplianceReviewNotesRequest.class);
    }

    @Test
    void listsCampaignsWithoutFilters() throws Exception {
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(draftView()));

        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaigns loaded"))
                .andExpect(jsonPath("$.data[0].id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"));

        ArgumentCaptor<CampaignSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CampaignSearchCriteria.class);
        verify(campaignService).searchCampaigns(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().status()).isNull();
    }

    @Test
    void listsCampaignsWithKbFilters() throws Exception {
        when(campaignService.searchCampaigns(any(CampaignSearchCriteria.class)))
                .thenReturn(List.of(draftView()));

        mockMvc.perform(
                        get("/api/campaigns")
                                .param("term", "life")
                                .param("ownerUserId", OWNER_ID.toString())
                                .param("status", "DRAFT")
                                .param("segmentId", SEGMENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ownerUserId").value(OWNER_ID.toString()));

        ArgumentCaptor<CampaignSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CampaignSearchCriteria.class);
        verify(campaignService).searchCampaigns(criteriaCaptor.capture());
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("life");
        assertThat(criteriaCaptor.getValue().ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(criteriaCaptor.getValue().segmentId()).isEqualTo(SEGMENT_ID);
    }

    @Test
    void rejectsOversizedCampaignSearchTerm() throws Exception {
        mockMvc.perform(get("/api/campaigns").param("term", "x".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns"));
    }

    @Test
    void getsCampaignById() throws Exception {
        when(campaignService.findById(CAMPAIGN_ID)).thenReturn(draftView());

        mockMvc.perform(get("/api/campaigns/{id}", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign loaded"))
                .andExpect(jsonPath("$.data.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.messageSubject").value("Renew your cover"))
                .andExpect(jsonPath("$.data.messageBody").value("Dear customer, ..."))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-30"))
                .andExpect(jsonPath("$.data.segmentName").value("Munich prospects"))
                .andExpect(jsonPath("$.data.productIds[0]").value(PRODUCT_ID.toString()));

        verify(campaignService).findById(CAMPAIGN_ID);
    }

    @Test
    void createsCampaign() throws Exception {
        when(campaignService.createCampaign(any(CreateCampaignCommand.class)))
                .thenReturn(draftView());

        mockMvc.perform(
                        post("/api/campaigns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
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
                                                .formatted(SEGMENT_ID, PRODUCT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Campaign created"))
                .andExpect(jsonPath("$.data.name").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data.messageSubject").value("Renew your cover"))
                .andExpect(jsonPath("$.data.messageBody").value("Dear customer, ..."))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-30"));

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
        assertThat(command.productIds()).containsExactly(PRODUCT_ID);
        assertThat(command.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(command.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void rejectsCreateCampaignWithoutRequiredFields() throws Exception {
        mockMvc.perform(
                        post("/api/campaigns")
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
                .andExpect(jsonPath("$.path").value("/api/campaigns"))
                .andExpect(
                        jsonPath(
                                "$.validationErrors[*].field",
                                hasItems("name", "objective", "channel")))
                .andExpect(
                        jsonPath(
                                "$.details",
                                hasItems(
                                        "name: must not be blank",
                                        "objective: must not be blank",
                                        "channel: must not be null")));
    }

    @Test
    void updatesCampaign() throws Exception {
        when(campaignService.updateCampaign(eq(CAMPAIGN_ID), any(UpdateCampaignCommand.class)))
                .thenReturn(draftView());

        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Life renewal outreach",
                                          "objective": "Promote life insurance renewals",
                                          "channel": "EMAIL",
                                          "messageSubject": "Renew your cover",
                                          "messageBody": "Dear customer, ...",
                                          "startDate": "2026-09-01",
                                          "endDate": "2026-09-30"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign updated"))
                .andExpect(jsonPath("$.data.messageSubject").value("Renew your cover"))
                .andExpect(jsonPath("$.data.messageBody").value("Dear customer, ..."))
                .andExpect(jsonPath("$.data.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-30"));

        ArgumentCaptor<UpdateCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateCampaignCommand.class);
        verify(campaignService).updateCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().messageSubject()).isEqualTo("Renew your cover");
        assertThat(commandCaptor.getValue().messageBody()).isEqualTo("Dear customer, ...");
        assertThat(commandCaptor.getValue().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(commandCaptor.getValue().endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void rejectsUpdateCampaignWithoutRequiredFields() throws Exception {
        mockMvc.perform(
                        put("/api/campaigns/{id}", CAMPAIGN_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "",
                                          "objective": " ",
                                          "channel": null
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns/" + CAMPAIGN_ID))
                .andExpect(
                        jsonPath(
                                "$.validationErrors[*].field",
                                hasItems("name", "objective", "channel")))
                .andExpect(
                        jsonPath(
                                "$.details",
                                hasItems(
                                        "name: must not be blank",
                                        "objective: must not be blank",
                                        "channel: must not be null")));
    }

    @Test
    void submitsCampaign() throws Exception {
        when(campaignService.submitCampaign(CAMPAIGN_ID)).thenReturn(viewWithStatus(CampaignStatus.SUBMITTED));

        mockMvc.perform(post("/api/campaigns/{id}/submit", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign submitted"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        verify(campaignService).submitCampaign(CAMPAIGN_ID);
    }

    @Test
    void approvesCampaign() throws Exception {
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenReturn(viewWithStatus(CampaignStatus.APPROVED));

        mockMvc.perform(post("/api/campaigns/{id}/approve", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign approved"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(campaignService)
                .approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class));
    }

    @Test
    void rejectsCampaignWithReason() throws Exception {
        when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                .thenReturn(
                        new CampaignView(
                                CAMPAIGN_ID,
                                "Life renewal outreach",
                                "Promote life insurance renewals",
                                CampaignStatus.REJECTED,
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
                                "Missing consent language",
                                null,
                                List.of(PRODUCT_ID),
                                CREATED_AT,
                                UPDATED_AT));

        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "rejectionReason": "Missing consent language"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign rejected"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Missing consent language"));

        ArgumentCaptor<RejectCampaignCommand> commandCaptor =
                ArgumentCaptor.forClass(RejectCampaignCommand.class);
        verify(campaignService).rejectCampaign(eq(CAMPAIGN_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().rejectionReason())
                .isEqualTo("Missing consent language");
    }

    @Test
    void rejectsBlankRejectionReason() throws Exception {
        mockMvc.perform(
                        post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "rejectionReason": " "
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void launchesCampaign() throws Exception {
        when(campaignService.launchCampaign(CAMPAIGN_ID))
                .thenReturn(viewWithStatus(CampaignStatus.ACTIVE));

        mockMvc.perform(post("/api/campaigns/{id}/launch", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign launched"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(campaignService).launchCampaign(CAMPAIGN_ID);
    }

    @Test
    void rejectsLaunchBeforeComplianceApproval() throws Exception {
        when(campaignService.launchCampaign(CAMPAIGN_ID))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Only APPROVED campaigns can be launched; current status is SUBMITTED"));

        mockMvc.perform(post("/api/campaigns/{id}/launch", CAMPAIGN_ID))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Only APPROVED campaigns can be launched; current status is SUBMITTED"))
                .andExpect(jsonPath("$.path").value("/api/campaigns/" + CAMPAIGN_ID + "/launch"));

        verify(campaignService).launchCampaign(CAMPAIGN_ID);
    }

    @Test
    void previewsCampaignRecipientsForSelectedSegment() throws Exception {
        when(campaignService.getSelectedSegmentId(CAMPAIGN_ID)).thenReturn(SEGMENT_ID);
        when(segmentService.previewCampaignRecipients(CAMPAIGN_ID, SEGMENT_ID))
                .thenReturn(
                        SegmentPreviewView.of(
                                3,
                                2,
                                1,
                                List.of(),
                                List.of(
                                        SegmentExclusionReasonSummary.of(
                                                "INVALID_CONSENT",
                                                "Valid marketing consent is required",
                                                1))));

        mockMvc.perform(get("/api/campaigns/{id}/recipients/preview", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign recipient preview loaded"))
                .andExpect(jsonPath("$.data.totalAudienceCount").value(3))
                .andExpect(jsonPath("$.data.eligibleCount").value(2))
                .andExpect(jsonPath("$.data.excludedCount").value(1))
                .andExpect(jsonPath("$.data.exclusionReasonSummary[0].code")
                        .value("INVALID_CONSENT"));

        verify(campaignService).getSelectedSegmentId(CAMPAIGN_ID);
        verify(segmentService).previewCampaignRecipients(CAMPAIGN_ID, SEGMENT_ID);
    }

    @Test
    void listsEligibleCampaignRecipients() throws Exception {
        UUID recipientId = UUID.fromString("62000000-0000-0000-0000-000000000269");
        UUID customerId = UUID.fromString("20000000-0000-0000-0000-000000000269");
        when(campaignRecipientService.listEligibleRecipients(CAMPAIGN_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientView(
                                        recipientId,
                                        CAMPAIGN_ID,
                                        "Life renewal outreach",
                                        customerId,
                                        "Ada Eligible",
                                        CampaignRecipientStatus.ELIGIBLE,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Instant.parse("2026-07-09T10:15:30Z"))));

        mockMvc.perform(get("/api/campaigns/{id}/recipients/eligible", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Eligible campaign recipients loaded"))
                .andExpect(jsonPath("$.data[0].id").value(recipientId.toString()))
                .andExpect(jsonPath("$.data[0].campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data[0].campaignName").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.data[0].customerFullName").value("Ada Eligible"))
                .andExpect(jsonPath("$.data[0].eligibilityStatus").value("ELIGIBLE"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-07-09T10:15:30Z"));

        verify(campaignRecipientService).listEligibleRecipients(CAMPAIGN_ID);
    }

    @Test
    void listsExcludedCampaignRecipients() throws Exception {
        UUID recipientId = UUID.fromString("62000000-0000-0000-0000-000000000270");
        UUID customerId = UUID.fromString("20000000-0000-0000-0000-000000000270");
        when(campaignRecipientService.listExcludedRecipients(CAMPAIGN_ID))
                .thenReturn(
                        List.of(
                                new CampaignRecipientView(
                                        recipientId,
                                        CAMPAIGN_ID,
                                        "Life renewal outreach",
                                        customerId,
                                        "Grace Excluded",
                                        CampaignRecipientStatus.EXCLUDED,
                                        "INVALID_CONSENT",
                                        "Customer does not have valid required consent",
                                        null,
                                        null,
                                        null,
                                        null,
                                        Instant.parse("2026-07-09T10:45:30Z"))));

        mockMvc.perform(get("/api/campaigns/{id}/recipients/excluded", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Excluded campaign recipients loaded"))
                .andExpect(jsonPath("$.data[0].id").value(recipientId.toString()))
                .andExpect(jsonPath("$.data[0].campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data[0].campaignName").value("Life renewal outreach"))
                .andExpect(jsonPath("$.data[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.data[0].customerFullName").value("Grace Excluded"))
                .andExpect(jsonPath("$.data[0].eligibilityStatus").value("EXCLUDED"))
                .andExpect(jsonPath("$.data[0].exclusionReason").value("INVALID_CONSENT"))
                .andExpect(
                        jsonPath("$.data[0].eligibilityExplanation")
                                .value("Customer does not have valid required consent"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-07-09T10:45:30Z"));

        verify(campaignRecipientService).listExcludedRecipients(CAMPAIGN_ID);
    }

    @Test
    void summarizesCampaignRecipientResponseCounts() throws Exception {
        when(campaignRecipientService.summarizeRecipients(CAMPAIGN_ID))
                .thenReturn(new CampaignRecipientSummaryView(CAMPAIGN_ID, 8L, 2L, 7L, 1L));

        mockMvc.perform(get("/api/campaigns/{id}/recipients/summary", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign recipient summary loaded"))
                .andExpect(jsonPath("$.data.campaignId").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.data.eligible").value(8))
                .andExpect(jsonPath("$.data.excluded").value(2))
                .andExpect(jsonPath("$.data.sent").value(7))
                .andExpect(jsonPath("$.data.failed").value(1));

        verify(campaignRecipientService).summarizeRecipients(CAMPAIGN_ID);
    }

    @Test
    void pausesCompletesAndArchivesCampaign() throws Exception {
        when(campaignService.pauseCampaign(CAMPAIGN_ID))
                .thenReturn(viewWithStatus(CampaignStatus.PAUSED));
        when(campaignService.completeCampaign(CAMPAIGN_ID))
                .thenReturn(viewWithStatus(CampaignStatus.COMPLETED));
        when(campaignService.archiveCampaign(CAMPAIGN_ID))
                .thenReturn(viewWithStatus(CampaignStatus.ARCHIVED));

        mockMvc.perform(post("/api/campaigns/{id}/pause", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign paused"))
                .andExpect(jsonPath("$.data.status").value("PAUSED"));
        mockMvc.perform(post("/api/campaigns/{id}/complete", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign completed"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(post("/api/campaigns/{id}/archive", CAMPAIGN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Campaign archived"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
        verify(campaignService).pauseCampaign(CAMPAIGN_ID);
        verify(campaignService).completeCampaign(CAMPAIGN_ID);
        verify(campaignService).archiveCampaign(CAMPAIGN_ID);
    }

    @Test
    void mapsNotFoundToApiError() throws Exception {
        when(campaignService.findById(CAMPAIGN_ID))
                .thenThrow(new ResourceNotFoundException("Campaign", CAMPAIGN_ID));

        mockMvc.perform(get("/api/campaigns/{id}", CAMPAIGN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void mapsForbiddenAndBusinessRuleErrors() throws Exception {
        when(campaignService.approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                .thenThrow(
                        new ForbiddenException(
                                "Campaign owner cannot approve or reject own campaign"));
        when(campaignService.launchCampaign(CAMPAIGN_ID))
                .thenThrow(
                        new BusinessRuleException(
                                "CAMPAIGN_LIFECYCLE",
                                "Only APPROVED campaigns can be launched; current status is SUBMITTED"));

        mockMvc.perform(post("/api/campaigns/{id}/approve", CAMPAIGN_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/campaigns/{id}/launch", CAMPAIGN_ID))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAMPAIGN_LIFECYCLE"));
    }

    private static CampaignView draftView() {
        return viewWithStatus(CampaignStatus.DRAFT);
    }

    private static CampaignView viewWithStatus(CampaignStatus status) {
        return new CampaignView(
                CAMPAIGN_ID,
                "Life renewal outreach",
                "Promote life insurance renewals",
                status,
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

    private static void assertPreAuthorize(
            String methodName, String expression, Class<?>... parameterTypes) throws Exception {
        PreAuthorize preAuthorize =
                CampaignController.class
                        .getMethod(methodName, parameterTypes)
                        .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
