package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import com.bayerwestphalian.campaign.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 317: contact event DTOs expose contact history request and response data. */
class ContactEventDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();
    private static final UUID EVENT_ID = UUID.fromString("63000000-0000-0000-0000-000000000317");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000317");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000317");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000317");

    @Test
    void validatesRecordContactEventRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(RecordContactEventRequest.class, "customerId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(RecordContactEventRequest.class, "channel")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(RecordContactEventRequest.class, "eventType")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(RecordContactEventRequest.class, "occurredAt")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(RecordContactEventRequest.class, "campaignId")
                                .isAnnotationPresent(NotNull.class))
                .isFalse();
        assertThat(
                        field(RecordContactEventRequest.class, "outcome")
                                .isAnnotationPresent(NotNull.class))
                .isFalse();
        assertThat(
                        field(RecordContactEventRequest.class, "notes")
                                .isAnnotationPresent(NotNull.class))
                .isFalse();
    }

    @Test
    void rejectsRecordContactEventRequestWithoutRequiredKbFields() {
        RecordContactEventRequest request =
                new RecordContactEventRequest(null, null, null, null, null, null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("customerId", "channel", "eventType", "occurredAt");
    }

    @Test
    void mapsRecordContactEventRequestToCommand() {
        Instant occurredAt = Instant.parse("2026-07-10T11:00:00Z");

        RecordContactEventCommand command =
                new RecordContactEventRequest(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.PHONE,
                                ContactEventType.CALLED,
                                ContactOutcome.INTERESTED,
                                "Customer requested advice",
                                occurredAt)
                        .toCommand();

        assertThat(command.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(command.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(command.channel()).isEqualTo(CommunicationChannel.PHONE);
        assertThat(command.eventType()).isEqualTo(ContactEventType.CALLED);
        assertThat(command.outcome()).isEqualTo(ContactOutcome.INTERESTED);
        assertThat(command.notes()).isEqualTo("Customer requested advice");
        assertThat(command.occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void mapsSearchRequestToCriteria() {
        ContactEventSearchCriteria criteria =
                new ContactEventSearchRequest(CUSTOMER_ID, CAMPAIGN_ID, ContactEventType.SENT)
                        .toCriteria();

        assertThat(criteria.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(criteria.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(criteria.eventType()).isEqualTo(ContactEventType.SENT);
    }

    @Test
    void mapsContactEventEntityToView() {
        Instant occurredAt = Instant.parse("2026-07-10T12:30:00Z");
        ContactEvent event =
                ContactEvent.record(
                        customer(),
                        campaign(),
                        CommunicationChannel.PHONE,
                        ContactEventType.CALLED,
                        occurredAt,
                        user(),
                        ContactOutcome.CONVERTED,
                        "Customer converted after call");
        ReflectionTestUtils.setField(event, "id", EVENT_ID);

        ContactEventView view = ContactEventView.from(event);

        assertThat(view.id()).isEqualTo(EVENT_ID);
        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Contact");
        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.campaignName()).isEqualTo("Contact Event DTO campaign");
        assertThat(view.channel()).isEqualTo(CommunicationChannel.PHONE);
        assertThat(view.eventType()).isEqualTo(ContactEventType.CALLED);
        assertThat(view.outcome()).isEqualTo(ContactOutcome.CONVERTED);
        assertThat(view.notes()).isEqualTo("Customer converted after call");
        assertThat(view.occurredAt()).isEqualTo(occurredAt);
        assertThat(view.createdByUserId()).isEqualTo(USER_ID);
        assertThat(view.createdByFullName()).isEqualTo("Contact Event User");
    }

    @Test
    void serializesContactEventViewWithIsoTimestampAndEnumNames() throws Exception {
        Instant occurredAt = Instant.parse("2026-07-10T13:45:00Z");
        ContactEvent event =
                ContactEvent.record(
                        customer(),
                        campaign(),
                        CommunicationChannel.EMAIL,
                        ContactEventType.REPLIED,
                        occurredAt,
                        user(),
                        ContactOutcome.NO_RESPONSE,
                        "Reply received without decision");
        ReflectionTestUtils.setField(event, "id", EVENT_ID);

        String json =
                ControllerTestSupport.apiObjectMapper()
                        .writeValueAsString(ContactEventView.from(event));
        JsonNode node = ControllerTestSupport.apiObjectMapper().readTree(json);

        assertThat(node.get("id").asText()).isEqualTo(EVENT_ID.toString());
        assertThat(node.get("customerId").asText()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(node.get("customerFullName").asText()).isEqualTo("Ada Contact");
        assertThat(node.get("campaignId").asText()).isEqualTo(CAMPAIGN_ID.toString());
        assertThat(node.get("campaignName").asText()).isEqualTo("Contact Event DTO campaign");
        assertThat(node.get("channel").asText()).isEqualTo("EMAIL");
        assertThat(node.get("eventType").asText()).isEqualTo("REPLIED");
        assertThat(node.get("outcome").asText()).isEqualTo("NO_RESPONSE");
        assertThat(node.get("notes").asText()).isEqualTo("Reply received without decision");
        assertThat(node.get("occurredAt").asText()).isEqualTo("2026-07-10T13:45:00Z");
        assertThat(node.get("createdByUserId").asText()).isEqualTo(USER_ID.toString());
        assertThat(node.get("createdByFullName").asText()).isEqualTo("Contact Event User");
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Contact");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        return customer;
    }

    private static Campaign campaign() {
        Campaign campaign =
                Campaign.create(
                        "Contact Event DTO campaign",
                        "Expose contact event DTOs",
                        user(),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static User user() {
        User user =
                User.create(
                        "contact-event-dto@test.example", "{noop}password", "Contact Event User");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
