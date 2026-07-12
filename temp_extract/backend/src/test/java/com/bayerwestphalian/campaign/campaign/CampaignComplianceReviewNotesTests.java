package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.JwtAuthenticationFilter;
import com.bayerwestphalian.campaign.auth.JwtService;
import com.bayerwestphalian.campaign.auth.JwtTokenClaims;
import com.bayerwestphalian.campaign.auth.JwtTokenType;
import com.bayerwestphalian.campaign.auth.SecurityConfiguration;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
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
 * KB item 231: compliance review notes on campaign approve/reject and dedicated notes update.
 */
class CampaignComplianceReviewNotesTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000106");

    @Nested
    class Domain {

        @Test
        void approveStoresOptionalComplianceReviewNotes() throws Exception {
            User owner = user(OWNER_ID, "CM");
            User compliance = user(COMPLIANCE_ID, "Compliance Officer");
            Campaign campaign =
                    Campaign.create(
                            "Review notes approve",
                            "Objective",
                            owner,
                            null,
                            CampaignChannel.EMAIL);
            campaign.submit();
            campaign.approve(compliance, "  Audience eligibility verified.  ");

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
            assertThat(campaign.getComplianceReviewNotes())
                    .isEqualTo("Audience eligibility verified.");
            assertThat(campaign.getRejectionReason()).isNull();
        }

        @Test
        void rejectStoresReasonAndOptionalComplianceReviewNotes() throws Exception {
            User owner = user(OWNER_ID, "CM");
            Campaign campaign =
                    Campaign.create(
                            "Review notes reject",
                            "Objective",
                            owner,
                            null,
                            CampaignChannel.EMAIL);
            campaign.submit();
            campaign.reject("Missing guardian consent evidence", "Please attach guardian form.");

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.REJECTED);
            assertThat(campaign.getRejectionReason())
                    .isEqualTo("Missing guardian consent evidence");
            assertThat(campaign.getComplianceReviewNotes())
                    .isEqualTo("Please attach guardian form.");
        }

        @Test
        void recordComplianceReviewNotesAllowedOnSubmitted() throws Exception {
            User owner = user(OWNER_ID, "CM");
            Campaign campaign =
                    Campaign.create(
                            "Notes while reviewing",
                            "Objective",
                            owner,
                            null,
                            CampaignChannel.EMAIL);
            campaign.submit();
            campaign.recordComplianceReviewNotes("Pending legal review of message body.");

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            assertThat(campaign.getComplianceReviewNotes())
                    .isEqualTo("Pending legal review of message body.");
        }

        @Test
        void recordComplianceReviewNotesRejectedOnDraft() throws Exception {
            User owner = user(OWNER_ID, "CM");
            Campaign campaign =
                    Campaign.create(
                            "Draft notes blocked",
                            "Objective",
                            owner,
                            null,
                            CampaignChannel.EMAIL);

            assertThatThrownBy(() -> campaign.recordComplianceReviewNotes("Too early"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SUBMITTED, APPROVED, or REJECTED");
        }

        @Test
        void submitClearsPriorComplianceReviewNotes() throws Exception {
            User owner = user(OWNER_ID, "CM");
            User compliance = user(COMPLIANCE_ID, "CO");
            Campaign campaign =
                    Campaign.create(
                            "Resubmit clears notes",
                            "Objective",
                            owner,
                            null,
                            CampaignChannel.EMAIL);
            campaign.submit();
            campaign.reject("Fix messaging", "Notes on first review");
            assertThat(campaign.getComplianceReviewNotes()).isEqualTo("Notes on first review");

            campaign.updateName("Resubmit clears notes");
            campaign.submit();
            assertThat(campaign.getComplianceReviewNotes()).isNull();
            assertThat(campaign.getRejectionReason()).isNull();
        }

        private User user(UUID id, String name) throws Exception {
            User user =
                    User.create(
                            name.toLowerCase().replace(' ', '.') + "@test",
                            "$2a$10$hash",
                            name);
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
            return user;
        }
    }

    @Nested
    @WebMvcTest(controllers = CampaignController.class)
    @Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class
    })
    class Http {

        @Autowired private MockMvc mockMvc;

        @MockBean private CampaignService campaignService;

        @MockBean private JwtService jwtService;

        @MockBean(name = "jpaMappingContext")
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Test
        void approveEndpointAcceptsComplianceReviewNotesBody() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());
            when(campaignService.approveCampaign(
                            eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                    .thenReturn(viewWithNotes(CampaignStatus.APPROVED, null, "Looks compliant."));

            mockMvc.perform(
                            post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "complianceReviewNotes": "Looks compliant."
                                            }
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Campaign approved"))
                    .andExpect(jsonPath("$.data.complianceReviewNotes").value("Looks compliant."));

            ArgumentCaptor<ApproveCampaignCommand> captor =
                    ArgumentCaptor.forClass(ApproveCampaignCommand.class);
            verify(campaignService).approveCampaign(eq(CAMPAIGN_ID), captor.capture());
            assertThat(captor.getValue().complianceReviewNotes()).isEqualTo("Looks compliant.");
        }

        @Test
        void approveEndpointAllowsEmptyBodyWithoutNotes() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());
            when(campaignService.approveCampaign(
                            eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class)))
                    .thenReturn(viewWithNotes(CampaignStatus.APPROVED, null, null));

            mockMvc.perform(
                            post("/api/campaigns/{id}/approve", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Campaign approved"));

            verify(campaignService)
                    .approveCampaign(eq(CAMPAIGN_ID), any(ApproveCampaignCommand.class));
        }

        @Test
        void rejectEndpointAcceptsReasonAndComplianceReviewNotes() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());
            when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                    .thenReturn(
                            viewWithNotes(
                                    CampaignStatus.REJECTED,
                                    "Missing consent evidence",
                                    "Please attach guardian form."));

            mockMvc.perform(
                            post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "rejectionReason": "Missing consent evidence",
                                              "complianceReviewNotes": "Please attach guardian form."
                                            }
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Campaign rejected"))
                    .andExpect(
                            jsonPath("$.data.rejectionReason").value("Missing consent evidence"))
                    .andExpect(
                            jsonPath("$.data.complianceReviewNotes")
                                    .value("Please attach guardian form."));

            ArgumentCaptor<RejectCampaignCommand> captor =
                    ArgumentCaptor.forClass(RejectCampaignCommand.class);
            verify(campaignService).rejectCampaign(eq(CAMPAIGN_ID), captor.capture());
            assertThat(captor.getValue().rejectionReason()).isEqualTo("Missing consent evidence");
            assertThat(captor.getValue().complianceReviewNotes())
                    .isEqualTo("Please attach guardian form.");
        }

        @Test
        void recordsComplianceReviewNotesViaDedicatedEndpoint() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());
            when(campaignService.recordComplianceReviewNotes(
                            eq(CAMPAIGN_ID), eq("Pending legal review")))
                    .thenReturn(
                            viewWithNotes(
                                    CampaignStatus.SUBMITTED, null, "Pending legal review"));

            mockMvc.perform(
                            put("/api/campaigns/{id}/compliance-review-notes", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "complianceReviewNotes": "Pending legal review"
                                            }
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Compliance review notes recorded"))
                    .andExpect(
                            jsonPath("$.data.complianceReviewNotes")
                                    .value("Pending legal review"));

            verify(campaignService)
                    .recordComplianceReviewNotes(CAMPAIGN_ID, "Pending legal review");
        }

        private JwtTokenClaims complianceClaims() {
            return new JwtTokenClaims(
                    COMPLIANCE_ID,
                    "compliance.officer@bayer-westphalian.test",
                    List.of(SystemRoleName.COMPLIANCE_OFFICER));
        }

        private CampaignView viewWithNotes(
                CampaignStatus status, String rejectionReason, String notes) {
            return new CampaignView(
                    CAMPAIGN_ID,
                    "Life renewal outreach",
                    "Promote life insurance renewals",
                    status,
                    OWNER_ID,
                    "Campaign Manager",
                    null,
                    null,
                    CampaignChannel.EMAIL,
                    "Subject",
                    "Body",
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    status == CampaignStatus.APPROVED ? COMPLIANCE_ID : null,
                    status == CampaignStatus.APPROVED ? "Compliance Officer" : null,
                    status == CampaignStatus.APPROVED
                            ? Instant.parse("2026-07-09T12:00:00Z")
                            : null,
                    rejectionReason,
                    notes,
                    List.of(),
                    Instant.parse("2026-07-09T10:15:00Z"),
                    Instant.parse("2026-07-09T11:00:00Z"));
        }
    }
}
