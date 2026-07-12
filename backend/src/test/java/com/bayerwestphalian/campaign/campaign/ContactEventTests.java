package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 280: contact events are created when eligible recipients are contacted on launch. */
class ContactEventTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000280");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000280");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000280");

    @Test
    void mapsKbContactEventsTableAsJpaEntity() throws Exception {
        assertThat(ContactEvent.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ContactEvent.class.getAnnotation(Table.class).name())
                .isEqualTo("contact_events");
        assertThat(
                        Arrays.stream(ContactEvent.class.getAnnotation(Table.class).indexes())
                                .map(Index::name))
                .contains(
                        "idx_contact_events_campaign",
                        "idx_contact_events_created_by",
                        "idx_contact_events_campaign_occurred");
        assertThat(field("channel").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("communication_channel");
        assertThat(field("eventType").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("contact_event_type");
        assertThat(field("outcome").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("contact_outcome");
        assertThat(field("channel").getAnnotation(Enumerated.class).value())
                .isEqualTo(EnumType.STRING);
        assertThat(field("eventType").getAnnotation(Enumerated.class).value())
                .isEqualTo(EnumType.STRING);
        assertThat(field("outcome").getAnnotation(Enumerated.class).value())
                .isEqualTo(EnumType.STRING);
        assertThat(field("channel").getAnnotation(JdbcTypeCode.class).value())
                .isEqualTo(SqlTypes.NAMED_ENUM);
        assertThat(field("eventType").getAnnotation(JdbcTypeCode.class).value())
                .isEqualTo(SqlTypes.NAMED_ENUM);
        assertThat(field("outcome").getAnnotation(JdbcTypeCode.class).value())
                .isEqualTo(SqlTypes.NAMED_ENUM);
    }

    @Test
    void mapsCampaignCustomerAndCreatorRelationships() throws Exception {
        assertManyToOne("customer", "customer_id", false);
        assertManyToOne("campaign", "campaign_id", true);
        assertManyToOne("createdBy", "created_by", true);
    }

    @Test
    void createsSentContactEventForLaunch() {
        Campaign campaign = campaign();
        Customer customer = customer();
        User creator = user();
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.sent(
                        customer, campaign, CommunicationChannel.EMAIL, occurredAt, creator);

        assertThat(event.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(event.getEventType()).isEqualTo(ContactEventType.SENT);
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getCreatedByUserId()).isEqualTo(USER_ID);
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getNotes()).isNull();
    }

    @Test
    void createsSentContactEventWithProviderMessageNotes() {
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.sent(
                        customer(),
                        campaign(),
                        CommunicationChannel.SMS,
                        occurredAt,
                        user(),
                        "providerMessageId=mock-sms-1");

        assertThat(event.getEventType()).isEqualTo(ContactEventType.SENT);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getNotes()).isEqualTo("providerMessageId=mock-sms-1");
    }

    @Test
    void createsFailedContactEventWithFailureOutcomeAndNotes() {
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.failed(
                        customer(),
                        campaign(),
                        CommunicationChannel.EMAIL,
                        occurredAt,
                        user(),
                        "providerMessageId=mock-email-1; failureCode=BOUNCE");

        assertThat(event.getEventType()).isEqualTo(ContactEventType.FAILED);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(event.getOutcome()).isEqualTo(ContactOutcome.FAILED);
        assertThat(event.getNotes())
                .isEqualTo("providerMessageId=mock-email-1; failureCode=BOUNCE");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void createsOpenedContactEventPlaceholderWithTrackingNotes() {
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.opened(
                        customer(),
                        campaign(),
                        CommunicationChannel.EMAIL,
                        occurredAt,
                        user(),
                        "providerMessageId=mock-email-1; trackingReference=open-pixel-1");

        assertThat(event.getEventType()).isEqualTo(ContactEventType.OPENED);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getNotes())
                .isEqualTo("providerMessageId=mock-email-1; trackingReference=open-pixel-1");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void createsClickedContactEventPlaceholderWithTrackingNotes() {
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.clicked(
                        customer(),
                        campaign(),
                        CommunicationChannel.EMAIL,
                        occurredAt,
                        user(),
                        "providerMessageId=mock-email-1; trackingReference=click-1; "
                                + "clickedUrl=https://example.test/offer");

        assertThat(event.getEventType()).isEqualTo(ContactEventType.CLICKED);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getNotes())
                .isEqualTo(
                        "providerMessageId=mock-email-1; trackingReference=click-1; "
                                + "clickedUrl=https://example.test/offer");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void createsRepliedContactEventPlaceholderWithMessageNotes() {
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.replied(
                        customer(),
                        campaign(),
                        CommunicationChannel.SMS,
                        occurredAt,
                        user(),
                        "providerMessageId=mock-sms-1; inboundMessageId=inbound-1; "
                                + "replyText=Please call me");

        assertThat(event.getEventType()).isEqualTo(ContactEventType.REPLIED);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getNotes())
                .isEqualTo(
                        "providerMessageId=mock-sms-1; inboundMessageId=inbound-1; "
                                + "replyText=Please call me");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void createsUnsubscribedContactEventWithOptOutNotes() {
        Instant occurredAt = Instant.parse("2026-07-09T10:15:30Z");

        ContactEvent event =
                ContactEvent.unsubscribed(
                        customer(),
                        campaign(),
                        CommunicationChannel.EMAIL,
                        occurredAt,
                        user(),
                        "providerMessageId=mock-email-1; unsubscribeSource=footer; "
                                + "reason=Customer opted out");

        assertThat(event.getEventType()).isEqualTo(ContactEventType.UNSUBSCRIBED);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(event.getOutcome()).isNull();
        assertThat(event.getNotes())
                .isEqualTo(
                        "providerMessageId=mock-email-1; unsubscribeSource=footer; "
                                + "reason=Customer opted out");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void createsGeneralContactEventWithOutcomeAndNotes() {
        Campaign campaign = campaign();
        Customer customer = customer();
        User creator = user();
        Instant occurredAt = Instant.parse("2026-07-10T11:00:00Z");

        ContactEvent event =
                ContactEvent.record(
                        customer,
                        campaign,
                        CommunicationChannel.PHONE,
                        ContactEventType.CALLED,
                        occurredAt,
                        creator,
                        ContactOutcome.INTERESTED,
                        "Customer requested a follow-up call");

        assertThat(event.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(event.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.PHONE);
        assertThat(event.getEventType()).isEqualTo(ContactEventType.CALLED);
        assertThat(event.getOutcome()).isEqualTo(ContactOutcome.INTERESTED);
        assertThat(event.getNotes()).isEqualTo("Customer requested a follow-up call");
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getCreatedByUserId()).isEqualTo(USER_ID);
    }

    @Test
    void contactOutcomeEnumMatchesKbContactOutcomeType() {
        assertThat(ContactOutcome.values())
                .containsExactly(
                        ContactOutcome.INTERESTED,
                        ContactOutcome.NOT_INTERESTED,
                        ContactOutcome.CONVERTED,
                        ContactOutcome.NO_RESPONSE,
                        ContactOutcome.FAILED);
    }

    @Test
    void assignsIdentifierBeforePersist() throws Exception {
        ContactEvent event =
                ContactEvent.sent(
                        customer(),
                        campaign(),
                        CommunicationChannel.EMAIL,
                        Instant.parse("2026-07-10T12:00:00Z"),
                        user());

        Method onCreate = ContactEvent.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(event);

        assertThat(event.getId()).isNotNull();
        assertThat(onCreate.getAnnotation(PrePersist.class)).isNotNull();
    }

    private static void assertManyToOne(String fieldName, String joinColumnName, boolean optional)
            throws Exception {
        Field relationship = field(fieldName);
        ManyToOne manyToOne = relationship.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = relationship.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isEqualTo(optional);
        assertThat(joinColumn.name()).isEqualTo(joinColumnName);
    }

    private static Field field(String name) throws Exception {
        Field field = ContactEvent.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Campaign campaign() {
        Campaign campaign =
                Campaign.create(
                        "Launch campaign",
                        "Contact eligible recipients",
                        user(),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Contacted");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        return customer;
    }

    private static User user() {
        User user =
                User.create(
                        "launch-contact-event@test.example",
                        "{noop}password",
                        "Launch Contact Event User");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
