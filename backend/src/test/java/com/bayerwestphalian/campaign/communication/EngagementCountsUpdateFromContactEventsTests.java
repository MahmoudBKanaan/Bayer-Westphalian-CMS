package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.analytics.DashboardView;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.campaign.CommunicationChannel;
import com.bayerwestphalian.campaign.campaign.ContactEvent;
import com.bayerwestphalian.campaign.campaign.ContactEventRepository;
import com.bayerwestphalian.campaign.campaign.ContactEventType;
import com.bayerwestphalian.campaign.campaign.ContactEventView;
import com.bayerwestphalian.campaign.campaign.ContactOutcome;
import com.bayerwestphalian.campaign.campaign.RecordClickedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordContactEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordOpenedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordRepliedEventCommand;
import com.bayerwestphalian.campaign.campaign.RecordSentEventCommand;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 450 acceptance: Open/click/reply/conversion counts update from contact events.
 *
 * <p>BR-034: campaign metrics update after contact events. Recording OPENED / CLICKED / REPLIED /
 * SENT events (and CONVERTED outcomes) increments the corresponding {@code campaign_metrics}
 * counters via {@link CommunicationService}.
 *
 * <p>Companion unit coverage for pure counters lives in {@code CalculateOpenedCountTests}, {@code
 * CalculateClickedCountTests}, {@code CalculateRepliedCountTests}, and {@code
 * CalculateConvertedCountTests} (items 421–424).
 *
 * <p>Sprint 16 critical item <b>656</b> (“Contact events update analytics”) is formalized in
 * {@link ContactEventsUpdateAnalyticsTests}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("450 Open/click/reply/conversion counts update from contact events")
class EngagementCountsUpdateFromContactEventsTests {

    private static final UUID EVENT_ID = UUID.fromString("63000000-0000-0000-0000-000000000450");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000450");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000450");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000450");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-11T15:00:00Z");

    @Mock private ContactEventRepository contactEventRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;
    @Mock private CampaignMetricsRepository campaignMetricsRepository;
    @Mock private CampaignProductRepository campaignProductRepository;

    private CommunicationService communicationService;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        communicationService =
                new CommunicationService(
                        contactEventRepository,
                        customerRepository,
                        campaignRepository,
                        consentRepository,
                        userRepository,
                        authorizationExpressions,
                        null,
                        campaignMetricsRepository);
        analyticsService =
                new AnalyticsService(
                        campaignRepository, campaignMetricsRepository, campaignProductRepository);
    }

    @Nested
    @DisplayName("CampaignMetrics increments (BR-034 domain helpers)")
    class DomainHelpers {

        @Test
        void incrementHelpersUpdateEachEngagementCounter() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
            metrics.recordLaunchCounts(10, 0, 10);

            metrics.incrementOpened();
            metrics.incrementClicked();
            metrics.incrementReplied();
            metrics.incrementConverted();

            assertThat(metrics.getOpenedCount()).isEqualTo(1);
            assertThat(metrics.getClickedCount()).isEqualTo(1);
            assertThat(metrics.getRepliedCount()).isEqualTo(1);
            assertThat(metrics.getConvertedCount()).isEqualTo(1);
        }

        @Test
        void recordEngagementCountsReplacesOpenClickReplyConversionTotals() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
            metrics.recordLaunchCounts(20, 0, 20);

            metrics.recordEngagementCounts(8, 4, 2, 1);

            assertThat(metrics.getOpenedCount()).isEqualTo(8);
            assertThat(metrics.getClickedCount()).isEqualTo(4);
            assertThat(metrics.getRepliedCount()).isEqualTo(2);
            assertThat(metrics.getConvertedCount()).isEqualTo(1);
            assertThat(metrics.calculateOpenRate()).isEqualByComparingTo("0.4000");
            assertThat(metrics.calculateClickRate()).isEqualByComparingTo("0.2000");
            assertThat(metrics.calculateConversionRate()).isEqualByComparingTo("0.0500");
        }
    }

    @Nested
    @DisplayName("CommunicationService updates metrics after contact events")
    class ServiceWiring {

        @Test
        void recordOpenedEventIncrementsOpenedCount() {
            Campaign campaign = campaign();
            CampaignMetrics metrics = launchedMetrics(campaign);
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));
            stubContactEventSave();

            communicationService.recordOpenedEvent(
                    new RecordOpenedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            "provider-1",
                            "pixel-1"));

            ArgumentCaptor<CampaignMetrics> metricsCaptor =
                    ArgumentCaptor.forClass(CampaignMetrics.class);
            verify(campaignMetricsRepository).save(metricsCaptor.capture());
            assertThat(metricsCaptor.getValue().getOpenedCount()).isEqualTo(1);
            assertThat(metricsCaptor.getValue().getClickedCount()).isZero();
            assertThat(metricsCaptor.getValue().getRepliedCount()).isZero();
            assertThat(metricsCaptor.getValue().getConvertedCount()).isZero();
        }

        @Test
        void recordClickedEventIncrementsClickedCount() {
            Campaign campaign = campaign();
            CampaignMetrics metrics = launchedMetrics(campaign);
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));
            stubContactEventSave();

            communicationService.recordClickedEvent(
                    new RecordClickedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            "provider-2",
                            "link-2",
                            "https://example.test/offer"));

            verify(campaignMetricsRepository).save(metrics);
            assertThat(metrics.getClickedCount()).isEqualTo(1);
            assertThat(metrics.getOpenedCount()).isZero();
        }

        @Test
        void recordRepliedEventIncrementsRepliedCount() {
            Campaign campaign = campaign();
            CampaignMetrics metrics = launchedMetrics(campaign);
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));
            stubContactEventSave();

            communicationService.recordRepliedEvent(
                    new RecordRepliedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            "provider-3",
                            "inbound-3",
                            "Interested"));

            verify(campaignMetricsRepository).save(metrics);
            assertThat(metrics.getRepliedCount()).isEqualTo(1);
        }

        @Test
        void recordSentEventIncrementsSentCount() {
            Campaign campaign = campaign();
            CampaignMetrics metrics = launchedMetrics(campaign);
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));
            stubContactEventSave();

            communicationService.recordSentEvent(
                    new RecordSentEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            "provider-sent"));

            verify(campaignMetricsRepository).save(metrics);
            // launch baseline sent=10 plus one additional SENT contact event.
            assertThat(metrics.getSentCount()).isEqualTo(11);
        }

        @Test
        void recordContactEventWithConvertedOutcomeIncrementsConvertedCount() {
            Campaign campaign = campaign();
            CampaignMetrics metrics = launchedMetrics(campaign);
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));
            stubContactEventSave();

            communicationService.recordContactEvent(
                    new RecordContactEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.PHONE,
                            ContactEventType.CALLED,
                            ContactOutcome.CONVERTED,
                            "Policy sold",
                            OCCURRED_AT));

            verify(campaignMetricsRepository).save(metrics);
            assertThat(metrics.getConvertedCount()).isEqualTo(1);
            // CALLED does not increment open/click/reply/sent.
            assertThat(metrics.getOpenedCount()).isZero();
            assertThat(metrics.getClickedCount()).isZero();
            assertThat(metrics.getRepliedCount()).isZero();
        }

        @Test
        void recordOpenedEventCreatesMetricsRowWhenMissing() {
            Campaign campaign = campaign();
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.empty());
            stubContactEventSave();

            communicationService.recordOpenedEvent(
                    new RecordOpenedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            null,
                            null));

            ArgumentCaptor<CampaignMetrics> metricsCaptor =
                    ArgumentCaptor.forClass(CampaignMetrics.class);
            verify(campaignMetricsRepository).save(metricsCaptor.capture());
            CampaignMetrics created = metricsCaptor.getValue();
            assertThat(created.getCampaignId()).isEqualTo(CAMPAIGN_ID);
            assertThat(created.getOpenedCount()).isEqualTo(1);
        }

        @Test
        void contactEventWithoutCampaignDoesNotTouchMetrics() {
            Customer customer = customer();
            User actor = user();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            stubContactEventSave();

            ContactEventView view =
                    communicationService.recordOpenedEvent(
                            new RecordOpenedEventCommand(
                                    CUSTOMER_ID,
                                    null,
                                    CommunicationChannel.EMAIL,
                                    OCCURRED_AT,
                                    null,
                                    null));

            assertThat(view.campaignId()).isNull();
            verify(campaignMetricsRepository, never()).findByCampaignId(any());
            verify(campaignMetricsRepository, never()).save(any());
        }

        @Test
        void successiveEngagementEventsAccumulateCounters() {
            Campaign campaign = campaign();
            CampaignMetrics metrics = launchedMetrics(campaign);
            stubContactActors(campaign);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(metrics));
            stubContactEventSave();

            communicationService.recordOpenedEvent(
                    new RecordOpenedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            null,
                            null));
            communicationService.recordOpenedEvent(
                    new RecordOpenedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT.plusSeconds(1),
                            null,
                            null));
            communicationService.recordClickedEvent(
                    new RecordClickedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT.plusSeconds(2),
                            null,
                            null,
                            "https://example.test"));
            communicationService.recordRepliedEvent(
                    new RecordRepliedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT.plusSeconds(3),
                            null,
                            null,
                            "Yes"));
            communicationService.recordContactEvent(
                    new RecordContactEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            ContactEventType.NOTE,
                            ContactOutcome.CONVERTED,
                            "Sold",
                            OCCURRED_AT.plusSeconds(4)));

            assertThat(metrics.getOpenedCount()).isEqualTo(2);
            assertThat(metrics.getClickedCount()).isEqualTo(1);
            assertThat(metrics.getRepliedCount()).isEqualTo(1);
            assertThat(metrics.getConvertedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Analytics surfaces after contact-event metric updates")
    class AnalyticsSurfaces {

        @Test
        void dashboardEngagementTotalsReflectContactEventUpdatedMetrics() {
            Campaign campaign = campaign();
            ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
            CampaignMetrics metrics = launchedMetrics(campaign);
            metrics.recordEngagementCounts(5, 2, 1, 1);

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));

            DashboardView dashboard = analyticsService.getDashboard();

            assertThat(dashboard.openedCount()).isEqualTo(5L);
            assertThat(dashboard.clickedCount()).isEqualTo(2L);
            assertThat(dashboard.repliedCount()).isEqualTo(1L);
            assertThat(dashboard.convertedCount()).isEqualTo(1L);
            assertThat(dashboard.recentCampaignMetrics())
                    .extracting(CampaignMetricsView::openedCount)
                    .containsExactly(5);
        }
    }

    private void stubContactActors(Campaign campaign) {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
    }

    private void stubContactEventSave() {
        when(contactEventRepository.save(any(ContactEvent.class)))
                .thenAnswer(
                        invocation -> {
                            ContactEvent event = invocation.getArgument(0);
                            ReflectionTestUtils.setField(event, "id", EVENT_ID);
                            return event;
                        });
    }

    private static CampaignMetrics launchedMetrics(Campaign campaign) {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(10, 0, 10);
        return metrics;
    }

    private static Campaign campaign() {
        Campaign campaign =
                Campaign.create(
                        "Engagement campaign",
                        "BR-034 contact events",
                        user(),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Contact");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        return customer;
    }

    private static User user() {
        User user = User.create("agent450@test.example", "{noop}x", "Contact Agent");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
