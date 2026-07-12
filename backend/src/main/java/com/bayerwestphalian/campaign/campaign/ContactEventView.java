package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.util.UUID;

/** API view for a stored contact history event. */
public record ContactEventView(
        UUID id,
        UUID customerId,
        String customerFullName,
        UUID campaignId,
        String campaignName,
        CommunicationChannel channel,
        ContactEventType eventType,
        ContactOutcome outcome,
        String notes,
        Instant occurredAt,
        UUID createdByUserId,
        String createdByFullName) {

    public static ContactEventView from(ContactEvent event) {
        Customer customer = event.getCustomer();
        Campaign campaign = event.getCampaign();
        User createdBy = event.getCreatedBy();

        return new ContactEventView(
                event.getId(),
                customerId(customer),
                fullName(customer),
                campaignId(campaign),
                campaignName(campaign),
                event.getChannel(),
                event.getEventType(),
                event.getOutcome(),
                event.getNotes(),
                event.getOccurredAt(),
                userId(createdBy),
                fullName(createdBy));
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String fullName(Customer customer) {
        return customer == null ? null : customer.getFullName();
    }

    private static UUID campaignId(Campaign campaign) {
        return campaign == null ? null : campaign.getId();
    }

    private static String campaignName(Campaign campaign) {
        return campaign == null ? null : campaign.getName();
    }

    private static UUID userId(User user) {
        return user == null ? null : user.getId();
    }

    private static String fullName(User user) {
        return user == null ? null : user.getFullName();
    }
}
