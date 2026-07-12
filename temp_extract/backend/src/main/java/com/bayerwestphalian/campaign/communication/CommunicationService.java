package com.bayerwestphalian.campaign.communication;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CommunicationChannel;
import com.bayerwestphalian.campaign.campaign.ContactEvent;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.ContactEventSearchCriteria;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.campaign.ContactEventView;
import com.bayerwestphalian.campaign.campaign.RecordClickedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordContactEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordFailedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordOpenedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordRepliedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordSentEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordUnsubscribeEventCommand;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for recording and reading KB contact history events. */
@Service
public class CommunicationService {

    private static final String CONTACT_WRITE_EXPRESSION =
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')";

    private final ContactEventRepository contactEventRepository;
    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;
    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;

    public CommunicationService(
            ContactEventRepository contactEventRepository,
            CustomerRepository customerRepository,
            CampaignRepository campaignRepository,
            ConsentRepository consentRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions) {
        this.contactEventRepository = contactEventRepository;
        this.customerRepository = customerRepository;
        this.campaignRepository = campaignRepository;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordContactEvent(RecordContactEventCommand command) {
        validateRecordCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        ContactEvent event =
                ContactEvent.record(
                        customer,
                        campaign,
                        command.channel(),
                        command.eventType(),
                        command.occurredAt(),
                        actor,
                        command.outcome(),
                        command.notes());

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordSentEvent(RecordSentEventCommand command) {
        validateSentCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        ContactEvent event =
                ContactEvent.sent(
                        customer,
                        campaign,
                        command.channel(),
                        command.occurredAt(),
                        actor,
                        providerMessageNote(command.providerMessageId()));

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordFailedEvent(RecordFailedEventCommand command) {
        validateFailedCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        ContactEvent event =
                ContactEvent.failed(
                        customer,
                        campaign,
                        command.channel(),
                        command.occurredAt(),
                        actor,
                        failureNote(
                                command.providerMessageId(),
                                command.failureCode(),
                                command.failureMessage()));

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordOpenedEvent(RecordOpenedEventCommand command) {
        validateOpenedCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        ContactEvent event =
                ContactEvent.opened(
                        customer,
                        campaign,
                        command.channel(),
                        command.occurredAt(),
                        actor,
                        openedNote(command.providerMessageId(), command.trackingReference()));

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordClickedEvent(RecordClickedEventCommand command) {
        validateClickedCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        ContactEvent event =
                ContactEvent.clicked(
                        customer,
                        campaign,
                        command.channel(),
                        command.occurredAt(),
                        actor,
                        clickedNote(
                                command.providerMessageId(),
                                command.trackingReference(),
                                command.clickedUrl()));

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordRepliedEvent(RecordRepliedEventCommand command) {
        validateRepliedCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        ContactEvent event =
                ContactEvent.replied(
                        customer,
                        campaign,
                        command.channel(),
                        command.occurredAt(),
                        actor,
                        repliedNote(
                                command.providerMessageId(),
                                command.inboundMessageId(),
                                command.replyText()));

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize(CONTACT_WRITE_EXPRESSION)
    @Transactional
    public ContactEventView recordUnsubscribeEvent(RecordUnsubscribeEventCommand command) {
        validateUnsubscribeCommand(command);
        Customer customer = findCustomer(command.customerId());
        Campaign campaign = command.campaignId() == null ? null : findCampaign(command.campaignId());
        User actor = findUser(authorizationExpressions.currentUserId());

        customer.markDoNotContact();
        recordMarketingOptOut(customer, command.channel(), command.unsubscribeSource());
        ContactEvent event =
                ContactEvent.unsubscribed(
                        customer,
                        campaign,
                        command.channel(),
                        command.occurredAt(),
                        actor,
                        unsubscribeNote(
                                command.providerMessageId(),
                                command.unsubscribeSource(),
                                command.reason()));

        return ContactEventView.from(contactEventRepository.save(event));
    }

    @PreAuthorize("@authz.canReadCustomers()")
    @Transactional(readOnly = true)
    public List<ContactEventView> listCustomerContactEvents(UUID customerId) {
        validateId(customerId, "Customer id is required");
        return contactEventRepository.findByCustomerId(customerId).stream()
                .map(ContactEventView::from)
                .toList();
    }

    @PreAuthorize("@authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public List<ContactEventView> listCampaignContactEvents(UUID campaignId) {
        validateId(campaignId, "Campaign id is required");
        return contactEventRepository.findByCampaignId(campaignId).stream()
                .map(ContactEventView::from)
                .toList();
    }

    @PreAuthorize("@authz.canReadCustomers() || @authz.canReadCampaigns()")
    @Transactional(readOnly = true)
    public List<ContactEventView> searchContactEvents(ContactEventSearchCriteria criteria) {
        ContactEventSearchCriteria safeCriteria =
                criteria == null ? new ContactEventSearchCriteria(null, null, null) : criteria;

        return baseSearchResults(safeCriteria).stream()
                .filter(event -> matchesCustomer(event, safeCriteria.customerId()))
                .filter(event -> matchesCampaign(event, safeCriteria.campaignId()))
                .filter(event -> matchesEventType(event, safeCriteria.eventType()))
                .map(ContactEventView::from)
                .toList();
    }

    @PreAuthorize("@authz.canReadCustomers()")
    @Transactional(readOnly = true)
    public long countRecentCustomerMarketingContacts(UUID customerId, Instant windowStart) {
        validateId(customerId, "Customer id is required");
        if (windowStart == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Window start is required"));
        }
        return contactEventRepository.countRecentCustomerMarketingContacts(customerId, windowStart);
    }

    private List<ContactEvent> baseSearchResults(ContactEventSearchCriteria criteria) {
        if (criteria.campaignId() != null && criteria.eventType() != null) {
            return contactEventRepository.findByCampaignIdAndEventType(
                    criteria.campaignId(), criteria.eventType());
        }
        if (criteria.customerId() != null && criteria.eventType() != null) {
            return contactEventRepository.findByCustomerIdAndEventType(
                    criteria.customerId(), criteria.eventType());
        }
        if (criteria.campaignId() != null) {
            return contactEventRepository.findByCampaignId(criteria.campaignId());
        }
        if (criteria.customerId() != null) {
            return contactEventRepository.findByCustomerId(criteria.customerId());
        }
        return contactEventRepository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt"));
    }

    private static boolean matchesCustomer(ContactEvent event, UUID customerId) {
        return customerId == null || Objects.equals(event.getCustomerId(), customerId);
    }

    private static boolean matchesCampaign(ContactEvent event, UUID campaignId) {
        return campaignId == null || Objects.equals(event.getCampaignId(), campaignId);
    }

    private static boolean matchesEventType(ContactEvent event, ContactEventType eventType) {
        return eventType == null || event.getEventType() == eventType;
    }

    private void validateRecordCommand(RecordContactEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Contact event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.eventType() == null) {
            errors.add("Contact event type is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private void validateSentCommand(RecordSentEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Sent event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private void validateFailedCommand(RecordFailedEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Failed event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private void validateOpenedCommand(RecordOpenedEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Opened event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private void validateClickedCommand(RecordClickedEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Clicked event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private void validateRepliedCommand(RecordRepliedEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed", List.of("Replied event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private void validateUnsubscribeCommand(RecordUnsubscribeEventCommand command) {
        List<String> errors = new ArrayList<>();
        if (command == null) {
            throw new ValidationException(
                    "Contact event validation failed",
                    List.of("Unsubscribe event command is required"));
        }
        if (command.customerId() == null) {
            errors.add("Customer id is required");
        }
        if (command.channel() == null) {
            errors.add("Communication channel is required");
        }
        if (command.occurredAt() == null) {
            errors.add("Occurred at is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Contact event validation failed", errors);
        }
    }

    private static String providerMessageNote(String providerMessageId) {
        return providerMessageId == null || providerMessageId.isBlank()
                ? null
                : "providerMessageId=" + providerMessageId.trim();
    }

    private static String failureNote(
            String providerMessageId, String failureCode, String failureMessage) {
        List<String> parts = new ArrayList<>();
        addNotePart(parts, providerMessageNote(providerMessageId));
        addNotePart(parts, notePart("failureCode", failureCode));
        addNotePart(parts, notePart("failureMessage", failureMessage));
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static String openedNote(String providerMessageId, String trackingReference) {
        List<String> parts = new ArrayList<>();
        addNotePart(parts, providerMessageNote(providerMessageId));
        addNotePart(parts, notePart("trackingReference", trackingReference));
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static String clickedNote(
            String providerMessageId, String trackingReference, String clickedUrl) {
        List<String> parts = new ArrayList<>();
        addNotePart(parts, providerMessageNote(providerMessageId));
        addNotePart(parts, notePart("trackingReference", trackingReference));
        addNotePart(parts, notePart("clickedUrl", clickedUrl));
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static String repliedNote(
            String providerMessageId, String inboundMessageId, String replyText) {
        List<String> parts = new ArrayList<>();
        addNotePart(parts, providerMessageNote(providerMessageId));
        addNotePart(parts, notePart("inboundMessageId", inboundMessageId));
        addNotePart(parts, notePart("replyText", replyText));
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static String unsubscribeNote(
            String providerMessageId, String unsubscribeSource, String reason) {
        List<String> parts = new ArrayList<>();
        addNotePart(parts, providerMessageNote(providerMessageId));
        addNotePart(parts, notePart("unsubscribeSource", unsubscribeSource));
        addNotePart(parts, notePart("reason", reason));
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private void recordMarketingOptOut(
            Customer customer, CommunicationChannel channel, String unsubscribeSource) {
        ConsentType consentType = marketingConsentType(channel);
        if (consentType == null) {
            return;
        }
        ConsentRecord optOut =
                ConsentRecord.create(
                        customer,
                        consentType,
                        ConsentStatus.REJECTED,
                        "Marketing unsubscribe",
                        noteValue(unsubscribeSource, "UNSUBSCRIBE"));
        consentRepository.save(optOut);
    }

    private static ConsentType marketingConsentType(CommunicationChannel channel) {
        return switch (channel) {
            case EMAIL -> ConsentType.MARKETING_EMAIL;
            case SMS -> ConsentType.MARKETING_SMS;
            case PHONE -> ConsentType.MARKETING_PHONE;
            case IN_APP -> null;
        };
    }

    private static String notePart(String key, String value) {
        String normalizedValue = noteValue(value, null);
        return normalizedValue == null ? null : key + "=" + normalizedValue;
    }

    private static String noteValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static void addNotePart(List<String> parts, String value) {
        if (value != null) {
            parts.add(value);
        }
    }

    private static void validateId(UUID id, String message) {
        if (id == null) {
            throw new ValidationException("Contact event validation failed", List.of(message));
        }
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private Campaign findCampaign(UUID campaignId) {
        return campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
