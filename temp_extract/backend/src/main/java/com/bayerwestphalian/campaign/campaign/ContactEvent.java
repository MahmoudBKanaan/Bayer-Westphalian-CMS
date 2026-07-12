package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Contact history event for campaign communication (KB {@code contact_events}). */
@Entity
@Table(
        name = "contact_events",
        indexes = {
            @Index(name = "idx_contact_events_campaign", columnList = "campaign_id"),
            @Index(name = "idx_contact_events_created_by", columnList = "created_by"),
            @Index(
                    name = "idx_contact_events_campaign_occurred",
                    columnList = "campaign_id, occurred_at")
        })
public class ContactEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel", nullable = false, columnDefinition = "communication_channel")
    private CommunicationChannel channel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false, columnDefinition = "contact_event_type")
    private ContactEventType eventType;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "outcome", columnDefinition = "contact_outcome")
    private ContactOutcome outcome;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    protected ContactEvent() {}

    private ContactEvent(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            ContactEventType eventType,
            Instant occurredAt,
            User createdBy,
            ContactOutcome outcome,
            String notes) {
        this.customer = Objects.requireNonNull(customer, "Customer is required");
        this.campaign = campaign;
        this.channel = Objects.requireNonNull(channel, "Communication channel is required");
        this.eventType = Objects.requireNonNull(eventType, "Contact event type is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at is required");
        this.createdBy = createdBy;
        this.outcome = outcome;
        this.notes = notes;
    }

    public static ContactEvent sent(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy) {
        return sent(customer, campaign, channel, occurredAt, createdBy, null);
    }

    public static ContactEvent sent(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy,
            String notes) {
        return new ContactEvent(
                customer, campaign, channel, ContactEventType.SENT, occurredAt, createdBy, null, notes);
    }

    public static ContactEvent failed(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy,
            String notes) {
        return new ContactEvent(
                customer,
                campaign,
                channel,
                ContactEventType.FAILED,
                occurredAt,
                createdBy,
                ContactOutcome.FAILED,
                notes);
    }

    public static ContactEvent opened(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy,
            String notes) {
        return new ContactEvent(
                customer, campaign, channel, ContactEventType.OPENED, occurredAt, createdBy, null, notes);
    }

    public static ContactEvent clicked(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy,
            String notes) {
        return new ContactEvent(
                customer, campaign, channel, ContactEventType.CLICKED, occurredAt, createdBy, null, notes);
    }

    public static ContactEvent replied(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy,
            String notes) {
        return new ContactEvent(
                customer, campaign, channel, ContactEventType.REPLIED, occurredAt, createdBy, null, notes);
    }

    public static ContactEvent unsubscribed(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            Instant occurredAt,
            User createdBy,
            String notes) {
        return new ContactEvent(
                customer,
                campaign,
                channel,
                ContactEventType.UNSUBSCRIBED,
                occurredAt,
                createdBy,
                null,
                notes);
    }

    public static ContactEvent record(
            Customer customer,
            Campaign campaign,
            CommunicationChannel channel,
            ContactEventType eventType,
            Instant occurredAt,
            User createdBy,
            ContactOutcome outcome,
            String notes) {
        return new ContactEvent(
                customer, campaign, channel, eventType, occurredAt, createdBy, outcome, notes);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public UUID getCustomerId() {
        return customer == null ? null : customer.getId();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public UUID getCampaignId() {
        return campaign == null ? null : campaign.getId();
    }

    public CommunicationChannel getChannel() {
        return channel;
    }

    public ContactEventType getEventType() {
        return eventType;
    }

    public String getNotes() {
        return notes;
    }

    public ContactOutcome getOutcome() {
        return outcome;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public UUID getCreatedByUserId() {
        return createdBy == null ? null : createdBy.getId();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
