package com.bayerwestphalian.campaign.campaign;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for campaign contact history events (KB {@code contact_events}). */
public interface ContactEventRepository extends JpaRepository<ContactEvent, UUID> {

    List<ContactEvent> findByCampaign_IdOrderByOccurredAtDesc(UUID campaignId);

    List<ContactEvent> findByCustomer_IdOrderByOccurredAtDesc(UUID customerId);

    List<ContactEvent> findByCampaign_IdAndEventTypeOrderByOccurredAtDesc(
            UUID campaignId, ContactEventType eventType);

    List<ContactEvent> findByCustomer_IdAndEventTypeOrderByOccurredAtDesc(
            UUID customerId, ContactEventType eventType);

    long countByCustomer_IdAndEventTypeInAndOccurredAtGreaterThanEqual(
            UUID customerId, Collection<ContactEventType> eventTypes, Instant windowStart);

    default List<ContactEvent> findByCampaignId(UUID campaignId) {
        return findByCampaign_IdOrderByOccurredAtDesc(campaignId);
    }

    default List<ContactEvent> findByCustomerId(UUID customerId) {
        return findByCustomer_IdOrderByOccurredAtDesc(customerId);
    }

    default List<ContactEvent> findByCampaignIdAndEventType(
            UUID campaignId, ContactEventType eventType) {
        return findByCampaign_IdAndEventTypeOrderByOccurredAtDesc(campaignId, eventType);
    }

    default List<ContactEvent> findByCustomerIdAndEventType(
            UUID customerId, ContactEventType eventType) {
        return findByCustomer_IdAndEventTypeOrderByOccurredAtDesc(customerId, eventType);
    }

    default long countRecentCustomerMarketingContacts(UUID customerId, Instant windowStart) {
        return countByCustomer_IdAndEventTypeInAndOccurredAtGreaterThanEqual(
                customerId, List.of(ContactEventType.SENT, ContactEventType.CALLED), windowStart);
    }
}
