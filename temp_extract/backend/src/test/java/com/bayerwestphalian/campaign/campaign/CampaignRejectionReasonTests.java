package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
 * KB item 232: formal rejection reason on campaign reject (KB {@code rejectionReason} /
 * {@code campaigns.rejection_reason}).
 */
class CampaignRejectionReasonTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000106");

    @Nested
    class Domain {

        @Test
        void rejectStoresTrimmedRequiredRejectionReason() throws Exception {
            Campaign campaign = submittedCampaign();

            campaign.reject("  Missing guardian consent evidence  ");

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.REJECTED);
            assertThat(campaign.getRejectionReason())
                    .isEqualTo("Missing guardian consent evidence");
            assertThat(campaign.getApprovedBy()).isNull();
            assertThat(campaign.getApprovedAt()).isNull();
            assertThat(campaign.canEdit()).isTrue();
        }

        @Test
        void rejectRejectsBlankOrNullReason() throws Exception {
            Campaign campaign = submittedCampaign();

            assertThatThrownBy(() -> campaign.reject("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rejection reason");
            assertThatThrownBy(() -> campaign.reject(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rejection reason");
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            assertThat(campaign.getRejectionReason()).isNull();
        }

        @Test
        void rejectOnlyAllowedFromSubmittedStatus() throws Exception {
            Campaign draft = draftCampaign();

            assertThatThrownBy(() -> draft.reject("Too early"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only SUBMITTED");
        }

        @Test
        void resubmitClearsRejectionReason() throws Exception {
            Campaign campaign = submittedCampaign();
            campaign.reject("Fix messaging before launch");
            assertThat(campaign.getRejectionReason()).isEqualTo("Fix messaging before launch");

            campaign.updateName("Revised campaign");
            campaign.submit();

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            assertThat(campaign.getRejectionReason()).isNull();
        }

        @Test
        void approveClearsPriorRejectionReason() throws Exception {
            User compliance = user(COMPLIANCE_ID, "Compliance Officer");
            Campaign campaign = submittedCampaign();
            campaign.reject("Incomplete message");
            campaign.updateObjective("Complete objective for resubmit");
            campaign.submit();

            campaign.approve(compliance);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
            assertThat(campaign.getRejectionReason()).isNull();
        }

        @Test
        void mapsRejectionReasonColumnToKbField() throws Exception {
            Field field = Campaign.class.getDeclaredField("rejectionReason");
            jakarta.persistence.Column column = field.getAnnotation(jakarta.persistence.Column.class);

            assertThat(column).isNotNull();
            assertThat(column.name()).isEqualTo("rejection_reason");
            assertThat(column.columnDefinition()).isEqualTo("text");
            assertThat(column.nullable()).isTrue();
        }

        private Campaign draftCampaign() throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            return Campaign.create(
                    "Rejection reason domain",
                    "Objective",
                    owner,
                    null,
                    CampaignChannel.EMAIL);
        }

        private Campaign submittedCampaign() throws Exception {
            Campaign campaign = draftCampaign();
            campaign.submit();
            return campaign;
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
        void rejectEndpointRequiresAndReturnsRejectionReason() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());
            when(campaignService.rejectCampaign(eq(CAMPAIGN_ID), any(RejectCampaignCommand.class)))
                    .thenReturn(rejectedView("Missing consent language in message body"));

            mockMvc.perform(
                            post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "rejectionReason": "Missing consent language in message body"
                                            }
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Campaign rejected"))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(
                            jsonPath("$.data.rejectionReason")
                                    .value("Missing consent language in message body"));

            ArgumentCaptor<RejectCampaignCommand> captor =
                    ArgumentCaptor.forClass(RejectCampaignCommand.class);
            verify(campaignService).rejectCampaign(eq(CAMPAIGN_ID), captor.capture());
            assertThat(captor.getValue().rejectionReason())
                    .isEqualTo("Missing consent language in message body");
        }

        @Test
        void rejectEndpointRejectsBlankRejectionReason() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());

            mockMvc.perform(
                            post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {
                                              "rejectionReason": " "
                                            }
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verify(campaignService, never())
                    .rejectCampaign(any(UUID.class), any(RejectCampaignCommand.class));
        }

        @Test
        void rejectEndpointRejectsMissingRejectionReasonField() throws Exception {
            when(jwtService.validateToken("compliance-token", JwtTokenType.ACCESS))
                    .thenReturn(complianceClaims());

            mockMvc.perform(
                            post("/api/campaigns/{id}/reject", CAMPAIGN_ID)
                                    .header("Authorization", "Bearer compliance-token")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verify(campaignService, never())
                    .rejectCampaign(any(UUID.class), any(RejectCampaignCommand.class));
        }

        private JwtTokenClaims complianceClaims() {
            return new JwtTokenClaims(
                    COMPLIANCE_ID,
                    "compliance.officer@bayer-westphalian.test",
                    List.of(SystemRoleName.COMPLIANCE_OFFICER));
        }

        private CampaignView rejectedView(String rejectionReason) {
            return new CampaignView(
                    CAMPAIGN_ID,
                    "Life renewal outreach",
                    "Promote life insurance renewals",
                    CampaignStatus.REJECTED,
                    OWNER_ID,
                    "Campaign Manager",
                    null,
                    null,
                    CampaignChannel.EMAIL,
                    "Subject",
                    "Body",
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null,
                    null,
                    null,
                    rejectionReason,
                    null,
                    List.of(),
                    Instant.parse("2026-07-09T10:15:00Z"),
                    Instant.parse("2026-07-09T11:00:00Z"));
        }
    }
}
