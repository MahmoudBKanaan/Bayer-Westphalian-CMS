package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CommunicationChannel;
import com.bayerwestphalian.campaign.campaign.ContactEvent;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.ContactEventSearchCriteria;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.campaign.ContactEventView;
import com.bayerwestphalian.campaign.campaign.ContactOutcome;
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
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 318: CommunicationService records and reads contact history events. */
@ExtendWith(MockitoExtension.class)
class CommunicationServiceTests {

    private static final UUID EVENT_ID =
            UUID.fromString("63000000-0000-0000-0000-000000000318");
    private static final UUID OTHER_EVENT_ID =
            UUID.fromString("63000000-0000-0000-0000-000000000319");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000318");
    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000319");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000318");
    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000318");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-10T15:00:00Z");

    @Mock private ContactEventRepository contactEventRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private CommunicationService communicationService;

    @BeforeEach
    void setUp() {
        communicationService =
                new CommunicationService(
                        contactEventRepository,
                        customerRepository,
                        campaignRepository,
                        consentRepository,
                        userRepository,
                        authorizationExpressions);
    }

    @Test
    void serviceMethodsDeclareKbAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "recordContactEvent",
                new Class<?>[] {RecordContactEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "recordSentEvent",
                new Class<?>[] {RecordSentEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "recordFailedEvent",
                new Class<?>[] {RecordFailedEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "recordOpenedEvent",
                new Class<?>[] {RecordOpenedEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "recordClickedEvent",
                new Class<?>[] {RecordClickedEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "recordRepliedEvent",
                new Class<?>[] {RecordRepliedEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "recordUnsubscribeEvent",
                new Class<?>[] {RecordUnsubscribeEventCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");
        assertPreAuthorizeWithExpression(
                "listCustomerContactEvents",
                new Class<?>[] {UUID.class},
                "@authz.canReadCustomers()");
        assertPreAuthorizeWithExpression(
                "listCampaignContactEvents",
                new Class<?>[] {UUID.class},
                "@authz.canReadCampaigns()");
        assertPreAuthorizeWithExpression(
                "searchContactEvents",
                new Class<?>[] {ContactEventSearchCriteria.class},
                "@authz.canReadCustomers() || @authz.canReadCampaigns()");
        assertPreAuthorizeWithExpression(
                "countRecentCustomerMarketingContacts",
                new Class<?>[] {UUID.class, Instant.class},
                "@authz.canReadCustomers()");
    }

    @Test
    void recordsContactEventFromKbCommandWithCurrentActor() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordContactEvent(
                        new RecordContactEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.PHONE,
                                ContactEventType.CALLED,
                                ContactOutcome.INTERESTED,
                                "Customer asked for a quote",
                                OCCURRED_AT));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.PHONE);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.CALLED);
        assertThat(saved.getOutcome()).isEqualTo(ContactOutcome.INTERESTED);
        assertThat(saved.getNotes()).isEqualTo("Customer asked for a quote");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.id()).isEqualTo(EVENT_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Contact");
        assertThat(view.campaignName()).isEqualTo("Communication service campaign");
        assertThat(view.createdByFullName()).isEqualTo("Contact Event User");
    }

    @Test
    void recordsSentEventWithProviderMessageReference() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordSentEvent(
                        new RecordSentEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.EMAIL,
                                OCCURRED_AT,
                                " provider-message-326 "));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.SENT);
        assertThat(saved.getOutcome()).isNull();
        assertThat(saved.getNotes()).isEqualTo("providerMessageId=provider-message-326");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.eventType()).isEqualTo(ContactEventType.SENT);
        assertThat(view.notes()).isEqualTo("providerMessageId=provider-message-326");
    }

    @Test
    void recordsFailedEventWithProviderFailureDetails() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordFailedEvent(
                        new RecordFailedEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.SMS,
                                OCCURRED_AT,
                                " provider-message-327 ",
                                " BOUNCE ",
                                " Mailbox unavailable "));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.FAILED);
        assertThat(saved.getOutcome()).isEqualTo(ContactOutcome.FAILED);
        assertThat(saved.getNotes())
                .isEqualTo(
                        "providerMessageId=provider-message-327; failureCode=BOUNCE; "
                                + "failureMessage=Mailbox unavailable");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.eventType()).isEqualTo(ContactEventType.FAILED);
        assertThat(view.outcome()).isEqualTo(ContactOutcome.FAILED);
    }

    @Test
    void recordsOpenedEventPlaceholderWithTrackingReference() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordOpenedEvent(
                        new RecordOpenedEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.EMAIL,
                                OCCURRED_AT,
                                " provider-message-328 ",
                                " open-pixel-328 "));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.OPENED);
        assertThat(saved.getOutcome()).isNull();
        assertThat(saved.getNotes())
                .isEqualTo("providerMessageId=provider-message-328; trackingReference=open-pixel-328");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.eventType()).isEqualTo(ContactEventType.OPENED);
        assertThat(view.notes())
                .isEqualTo("providerMessageId=provider-message-328; trackingReference=open-pixel-328");
    }

    @Test
    void recordsClickedEventPlaceholderWithTrackingReferenceAndUrl() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordClickedEvent(
                        new RecordClickedEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.EMAIL,
                                OCCURRED_AT,
                                " provider-message-329 ",
                                " click-link-329 ",
                                " https://example.test/offer "));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.CLICKED);
        assertThat(saved.getOutcome()).isNull();
        assertThat(saved.getNotes())
                .isEqualTo(
                        "providerMessageId=provider-message-329; trackingReference=click-link-329; "
                                + "clickedUrl=https://example.test/offer");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.eventType()).isEqualTo(ContactEventType.CLICKED);
        assertThat(view.notes())
                .isEqualTo(
                        "providerMessageId=provider-message-329; trackingReference=click-link-329; "
                                + "clickedUrl=https://example.test/offer");
    }

    @Test
    void recordsRepliedEventPlaceholderWithInboundMessageDetails() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordRepliedEvent(
                        new RecordRepliedEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.SMS,
                                OCCURRED_AT,
                                " provider-message-330 ",
                                " inbound-message-330 ",
                                " Please call me tomorrow "));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.SMS);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.REPLIED);
        assertThat(saved.getOutcome()).isNull();
        assertThat(saved.getNotes())
                .isEqualTo(
                        "providerMessageId=provider-message-330; "
                                + "inboundMessageId=inbound-message-330; "
                                + "replyText=Please call me tomorrow");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.eventType()).isEqualTo(ContactEventType.REPLIED);
        assertThat(view.notes())
                .isEqualTo(
                        "providerMessageId=provider-message-330; "
                                + "inboundMessageId=inbound-message-330; "
                                + "replyText=Please call me tomorrow");
    }

    @Test
    void recordsUnsubscribeEventAndMarksCustomerDoNotContact() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        Campaign campaign = campaign();
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });

        ContactEventView view =
                communicationService.recordUnsubscribeEvent(
                        new RecordUnsubscribeEventCommand(
                                CUSTOMER_ID,
                                CAMPAIGN_ID,
                                CommunicationChannel.EMAIL,
                                OCCURRED_AT,
                                " provider-message-331 ",
                                " footer-link ",
                                " Do not email again "));

        ArgumentCaptor<ContactEvent> eventCaptor = ArgumentCaptor.forClass(ContactEvent.class);
        verify(contactEventRepository).save(eventCaptor.capture());
        ArgumentCaptor<ConsentRecord> consentCaptor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(consentRepository).save(consentCaptor.capture());
        ContactEvent saved = eventCaptor.getValue();
        ConsentRecord optOut = consentCaptor.getValue();
        assertThat(customer.isDoNotContact()).isTrue();
        assertThat(optOut.getCustomer()).isSameAs(customer);
        assertThat(optOut.getConsentType()).isEqualTo(ConsentType.MARKETING_EMAIL);
        assertThat(optOut.getStatus()).isEqualTo(ConsentStatus.REJECTED);
        assertThat(optOut.getPurpose()).isEqualTo("Marketing unsubscribe");
        assertThat(optOut.getSource()).isEqualTo("footer-link");
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getCampaign()).isSameAs(campaign);
        assertThat(saved.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(saved.getEventType()).isEqualTo(ContactEventType.UNSUBSCRIBED);
        assertThat(saved.getOutcome()).isNull();
        assertThat(saved.getNotes())
                .isEqualTo(
                        "providerMessageId=provider-message-331; "
                                + "unsubscribeSource=footer-link; reason=Do not email again");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getCreatedBy()).isSameAs(actor);
        assertThat(view.eventType()).isEqualTo(ContactEventType.UNSUBSCRIBED);
        assertThat(view.notes())
                .isEqualTo(
                        "providerMessageId=provider-message-331; "
                                + "unsubscribeSource=footer-link; reason=Do not email again");
    }

    @Test
    void unsubscribeCreatesChannelSpecificMarketingSmsOptOut() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        communicationService.recordUnsubscribeEvent(
                new RecordUnsubscribeEventCommand(
                        CUSTOMER_ID,
                        null,
                        CommunicationChannel.SMS,
                        OCCURRED_AT,
                        null,
                        " sms-stop ",
                        null));

        ArgumentCaptor<ConsentRecord> consentCaptor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(consentRepository).save(consentCaptor.capture());
        ConsentRecord optOut = consentCaptor.getValue();
        assertThat(optOut.getConsentType()).isEqualTo(ConsentType.MARKETING_SMS);
        assertThat(optOut.getStatus()).isEqualTo(ConsentStatus.REJECTED);
        assertThat(optOut.getSource()).isEqualTo("sms-stop");
    }

    @Test
    void unsubscribeWithoutSourceStillCreatesMarketingOptOutWithDefaultSource() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        communicationService.recordUnsubscribeEvent(
                new RecordUnsubscribeEventCommand(
                        CUSTOMER_ID,
                        null,
                        CommunicationChannel.PHONE,
                        OCCURRED_AT,
                        null,
                        " ",
                        null));

        ArgumentCaptor<ConsentRecord> consentCaptor = ArgumentCaptor.forClass(ConsentRecord.class);
        verify(consentRepository).save(consentCaptor.capture());
        ConsentRecord optOut = consentCaptor.getValue();
        assertThat(optOut.getConsentType()).isEqualTo(ConsentType.MARKETING_PHONE);
        assertThat(optOut.getStatus()).isEqualTo(ConsentStatus.REJECTED);
        assertThat(optOut.getSource()).isEqualTo("UNSUBSCRIBE");
    }

    @Test
    void recordsContactEventWithoutCampaignWhenCampaignIdIsOmitted() {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Contact");
        User actor = user(USER_ID, "Contact Event User");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContactEventView view =
                communicationService.recordContactEvent(
                        new RecordContactEventCommand(
                                CUSTOMER_ID,
                                null,
                                CommunicationChannel.EMAIL,
                                ContactEventType.NOTE,
                                null,
                                "General contact note",
                                OCCURRED_AT));

        assertThat(view.campaignId()).isNull();
        verifyNoInteractions(campaignRepository);
    }

    @Test
    void rejectsRecordContactEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordContactEvent(
                                        new RecordContactEventCommand(
                                                null, null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void rejectsFailedEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordFailedEvent(
                                        new RecordFailedEventCommand(
                                                null, null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void rejectsOpenedEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordOpenedEvent(
                                        new RecordOpenedEventCommand(
                                                null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void rejectsClickedEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordClickedEvent(
                                        new RecordClickedEventCommand(
                                                null, null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void rejectsRepliedEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordRepliedEvent(
                                        new RecordRepliedEventCommand(
                                                null, null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void rejectsUnsubscribeEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordUnsubscribeEvent(
                                        new RecordUnsubscribeEventCommand(
                                                null, null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void rejectsSentEventWithoutRequiredFields() {
        assertThatThrownBy(
                        () ->
                                communicationService.recordSentEvent(
                                        new RecordSentEventCommand(
                                                null, null, null, null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Contact event validation failed");

        verifyNoInteractions(
                customerRepository, campaignRepository, userRepository, contactEventRepository);
    }

    @Test
    void throwsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                communicationService.recordContactEvent(
                                        new RecordContactEventCommand(
                                                CUSTOMER_ID,
                                                null,
                                                CommunicationChannel.EMAIL,
                                                ContactEventType.SENT,
                                                null,
                                                null,
                                                OCCURRED_AT)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer was not found");
    }

    @Test
    void listsCustomerAndCampaignContactHistories() {
        ContactEvent event = contactEvent(EVENT_ID, customer(CUSTOMER_ID, "Ada", "Contact"));
        when(contactEventRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(event));
        when(contactEventRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(event));

        assertThat(communicationService.listCustomerContactEvents(CUSTOMER_ID))
                .extracting(ContactEventView::id)
                .containsExactly(EVENT_ID);
        assertThat(communicationService.listCampaignContactEvents(CAMPAIGN_ID))
                .extracting(ContactEventView::id)
                .containsExactly(EVENT_ID);
    }

    @Test
    void searchContactEventsUsesRepositoryLookupThenAppliesRemainingFilters() {
        ContactEvent matchingEvent =
                contactEvent(EVENT_ID, customer(CUSTOMER_ID, "Ada", "Contact"));
        ContactEvent otherCustomerEvent =
                contactEvent(OTHER_EVENT_ID, customer(OTHER_CUSTOMER_ID, "Ben", "Other"));
        when(contactEventRepository.findByCampaignIdAndEventType(CAMPAIGN_ID, ContactEventType.SENT))
                .thenReturn(List.of(matchingEvent, otherCustomerEvent));

        List<ContactEventView> results =
                communicationService.searchContactEvents(
                        new ContactEventSearchCriteria(
                                CUSTOMER_ID, CAMPAIGN_ID, ContactEventType.SENT));

        assertThat(results).extracting(ContactEventView::id).containsExactly(EVENT_ID);
    }

    @Test
    void countRecentCustomerMarketingContactsDelegatesToRepository() {
        Instant windowStart = Instant.parse("2026-06-10T00:00:00Z");
        when(contactEventRepository.countRecentCustomerMarketingContacts(CUSTOMER_ID, windowStart))
                .thenReturn(3L);

        long count =
                communicationService.countRecentCustomerMarketingContacts(CUSTOMER_ID, windowStart);

        assertThat(count).isEqualTo(3L);
        verify(contactEventRepository)
                .countRecentCustomerMarketingContacts(CUSTOMER_ID, windowStart);
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = CommunicationService.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expectedExpression);
    }

    private static ContactEvent contactEvent(UUID eventId, Customer customer) {
        ContactEvent event =
                ContactEvent.sent(
                        customer,
                        campaign(),
                        CommunicationChannel.EMAIL,
                        OCCURRED_AT,
                        user(USER_ID, "Contact Event User"));
        ReflectionTestUtils.setField(event, "id", eventId);
        return event;
    }

    private static Customer customer(UUID id, String firstName, String lastName) {
        Customer customer = Customer.create(CustomerType.INDIVIDUAL, firstName, lastName);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private static Campaign campaign() {
        Campaign campaign =
                Campaign.create(
                        "Communication service campaign",
                        "Record contact events",
                        user(USER_ID, "Contact Event User"),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static User user(UUID id, String fullName) {
        User user =
                User.create(
                        fullName.toLowerCase().replace(' ', '.') + "@test.example",
                        "{noop}password",
                        fullName);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
