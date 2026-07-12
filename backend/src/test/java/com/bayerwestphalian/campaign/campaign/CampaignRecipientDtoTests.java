package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import com.bayerwestphalian.campaign.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 266: recipient DTOs expose campaign audience recipient data. */
class CampaignRecipientDtoTests {

    private static final UUID RECIPIENT_ID =
            UUID.fromString("62000000-0000-0000-0000-000000000266");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000266");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000266");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000266");

    @Test
    void mapsEligibleCampaignRecipientEntityToView() {
        CampaignRecipient recipient = CampaignRecipient.eligible(campaign(), customer());
        ReflectionTestUtils.setField(recipient, "id", RECIPIENT_ID);
        ReflectionTestUtils.setField(recipient, "createdAt", Instant.parse("2026-07-09T10:15:30Z"));
        recipient.markSent();

        CampaignRecipientView view = CampaignRecipientView.from(recipient);

        assertThat(view.id()).isEqualTo(RECIPIENT_ID);
        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.campaignName()).isEqualTo("Recipient DTO campaign");
        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Recipient");
        assertThat(view.eligibilityStatus()).isEqualTo(CampaignRecipientStatus.SENT);
        assertThat(view.exclusionReason()).isNull();
        assertThat(view.eligibilityExplanation()).isNull();
        assertThat(view.sentAt()).isNotNull();
        assertThat(view.createdAt()).isEqualTo(Instant.parse("2026-07-09T10:15:30Z"));
    }

    @Test
    void mapsExcludedCampaignRecipientReasonAndExplanationToView() {
        CampaignRecipient recipient =
                CampaignRecipient.excluded(
                        campaign(), customer(), "DO_NOT_CONTACT", "Customer opted out");
        ReflectionTestUtils.setField(recipient, "id", RECIPIENT_ID);

        CampaignRecipientView view = CampaignRecipientView.from(recipient);

        assertThat(view.eligibilityStatus()).isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(view.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(view.eligibilityExplanation()).isEqualTo("Customer opted out");
    }

    @Test
    void serializesCampaignRecipientViewWithIsoTimestamps() throws Exception {
        CampaignRecipient recipient = CampaignRecipient.eligible(campaign(), customer());
        ReflectionTestUtils.setField(recipient, "id", RECIPIENT_ID);
        ReflectionTestUtils.setField(recipient, "createdAt", Instant.parse("2026-07-09T10:15:30Z"));
        ReflectionTestUtils.setField(recipient, "sentAt", Instant.parse("2026-07-09T11:00:00Z"));
        ReflectionTestUtils.setField(recipient, "openedAt", Instant.parse("2026-07-09T12:00:00Z"));
        ReflectionTestUtils.setField(
                recipient, "eligibilityStatus", CampaignRecipientStatus.OPENED);

        String json =
                ControllerTestSupport.apiObjectMapper()
                        .writeValueAsString(CampaignRecipientView.from(recipient));
        JsonNode node = ControllerTestSupport.apiObjectMapper().readTree(json);

        assertThat(node.get("id").asText()).isEqualTo(RECIPIENT_ID.toString());
        assertThat(node.get("campaignId").asText()).isEqualTo(CAMPAIGN_ID.toString());
        assertThat(node.get("campaignName").asText()).isEqualTo("Recipient DTO campaign");
        assertThat(node.get("customerId").asText()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(node.get("customerFullName").asText()).isEqualTo("Ada Recipient");
        assertThat(node.get("eligibilityStatus").asText()).isEqualTo("OPENED");
        assertThat(node.get("sentAt").asText()).isEqualTo("2026-07-09T11:00:00Z");
        assertThat(node.get("openedAt").asText()).isEqualTo("2026-07-09T12:00:00Z");
        assertThat(node.get("createdAt").asText()).isEqualTo("2026-07-09T10:15:30Z");
    }

    @Test
    void serializesCampaignRecipientSummaryCounts() throws Exception {
        CampaignRecipientSummaryView summary =
                new CampaignRecipientSummaryView(CAMPAIGN_ID, 8L, 2L, 7L, 1L);

        String json = ControllerTestSupport.apiObjectMapper().writeValueAsString(summary);
        JsonNode node = ControllerTestSupport.apiObjectMapper().readTree(json);

        assertThat(node.get("campaignId").asText()).isEqualTo(CAMPAIGN_ID.toString());
        assertThat(node.get("eligible").asLong()).isEqualTo(8L);
        assertThat(node.get("excluded").asLong()).isEqualTo(2L);
        assertThat(node.get("sent").asLong()).isEqualTo(7L);
        assertThat(node.get("failed").asLong()).isEqualTo(1L);
    }

    private static Campaign campaign() {
        User owner =
                User.create(
                        "campaign-recipient-dto-owner@test.example",
                        "{noop}password",
                        "Campaign Recipient DTO Owner");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Campaign campaign =
                Campaign.create(
                        "Recipient DTO campaign",
                        "Expose recipient DTOs",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Recipient");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        return customer;
    }
}
