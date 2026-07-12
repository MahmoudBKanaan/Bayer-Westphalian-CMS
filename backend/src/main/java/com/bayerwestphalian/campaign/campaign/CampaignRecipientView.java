package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.customer.Customer;
import java.time.Instant;
import java.util.UUID;

/** API view for a stored campaign recipient row. */
public record CampaignRecipientView(
        UUID id,
        UUID campaignId,
        String campaignName,
        UUID customerId,
        String customerFullName,
        CampaignRecipientStatus eligibilityStatus,
        String exclusionReason,
        String eligibilityExplanation,
        Instant sentAt,
        Instant openedAt,
        Instant clickedAt,
        Instant convertedAt,
        Instant createdAt) {

    public static CampaignRecipientView from(CampaignRecipient recipient) {
        Campaign campaign = recipient.getCampaign();
        Customer customer = recipient.getCustomer();

        return new CampaignRecipientView(
                recipient.getId(),
                campaignId(campaign),
                campaignName(campaign),
                customerId(customer),
                fullName(customer),
                recipient.getEligibilityStatus(),
                recipient.getExclusionReason(),
                recipient.getEligibilityExplanation(),
                recipient.getSentAt(),
                recipient.getOpenedAt(),
                recipient.getClickedAt(),
                recipient.getConvertedAt(),
                recipient.getCreatedAt());
    }

    private static UUID campaignId(Campaign campaign) {
        return campaign == null ? null : campaign.getId();
    }

    private static String campaignName(Campaign campaign) {
        return campaign == null ? null : campaign.getName();
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String fullName(Customer customer) {
        return customer == null ? null : customer.getFullName();
    }
}
