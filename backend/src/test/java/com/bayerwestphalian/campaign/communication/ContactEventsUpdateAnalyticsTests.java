package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsCalculations;
import com.bayerwestphalian.campaign.analytics.AnalyticsService;
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
 * Sprint 16 critical test item <b>656</b>: Contact events update analytics.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code BR-034} — Campaign metrics update after contact events
 *   <li>{@code FR-103}–{@code FR-106} — Dashboard sent / open / click / conversion rates from
 *       metrics
 *   <li>Items 421–424 / 450 — open/click/reply/conversion counters from contact events
 * </ul>
 *
 * <p>Pipeline under test:
 *
 * <pre>
 * ContactEvent recorded → CommunicationService.applyCampaignMetricsFromContactEvent
 *   → campaign_metrics counters → AnalyticsService dashboard / KPI aggregates
 * </pre>
 *
 * <p>Companion suite: {@link EngagementCountsUpdateFromContactEventsTests} (item 450).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("656 Contact events update analytics")
class ContactEventsUpdateAnalyticsTests {

    private static final UUID EVENT_ID = UUID.fromString("63000000-0000-0000-0000-000000000656");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000656");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000656");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000656");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-12T18:00:00Z");

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
    @DisplayName("Contact events increment campaign_metrics (BR-034)")
    class MetricsFromContactEvents {

        @Test
        void openedEventIncrementsOpenedCountUsedByAnalytics() {
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
                            "provider-open",
                            "pixel"));

            ArgumentCaptor<CampaignMetrics> captor = ArgumentCaptor.forClass(CampaignMetrics.class);
            verify(campaignMetricsRepository).save(captor.capture());
            assertThat(captor.getValue().getOpenedCount()).isEqualTo(1);
        }

        @Test
        void clickedEventIncrementsClickedCount() {
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
                            "provider-click",
                            "link-1",
                            "https://example.test/offer"));

            verify(campaignMetricsRepository).save(metrics);
            assertThat(metrics.getClickedCount()).isEqualTo(1);
        }

        @Test
        void repliedEventIncrementsRepliedCount() {
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
                            CommunicationChannel.SMS,
                            OCCURRED_AT,
                            "provider-reply",
                            "inbound",
                            "Interested"));

            verify(campaignMetricsRepository).save(metrics);
            assertThat(metrics.getRepliedCount()).isEqualTo(1);
        }

        @Test
        void sentEventIncrementsSentCountBeyondLaunchBaseline() {
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
            assertThat(metrics.getSentCount()).isEqualTo(11);
        }

        @Test
        void convertedOutcomeIncrementsConvertedCountForAnalytics() {
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
        }

        @Test
        void successiveEventsAccumulateAndFeedRateCalculators() {
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
            communicationService.recordContactEvent(
                    new RecordContactEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            ContactEventType.NOTE,
                            ContactOutcome.CONVERTED,
                            "Sold",
                            OCCURRED_AT.plusSeconds(3)));

            assertThat(metrics.getOpenedCount()).isEqualTo(2);
            assertThat(metrics.getClickedCount()).isEqualTo(1);
            assertThat(metrics.getConvertedCount()).isEqualTo(1);
            // FR-104/105/106 rate formulas from sent baseline 10.
            assertThat(metrics.calculateOpenRate()).isEqualByComparingTo("0.2000");
            assertThat(metrics.calculateClickRate()).isEqualByComparingTo("0.1000");
            assertThat(metrics.calculateConversionRate()).isEqualByComparingTo("0.1000");
        }

        @Test
        void eventWithoutCampaignDoesNotUpdateMetrics() {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
            stubContactEventSave();

            communicationService.recordOpenedEvent(
                    new RecordOpenedEventCommand(
                            CUSTOMER_ID,
                            null,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT,
                            null,
                            null));

            verify(campaignMetricsRepository, never()).findByCampaignId(any());
            verify(campaignMetricsRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Analytics dashboard reflects contact-event updated metrics")
    class AnalyticsDashboard {

        @Test
        void dashboardTotalsReflectMetricsAfterContactEventRecording() {
            Campaign campaign = campaign();
            ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
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
            communicationService.recordClickedEvent(
                    new RecordClickedEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            OCCURRED_AT.plusSeconds(1),
                            null,
                            null,
                            "https://example.test"));
            communicationService.recordContactEvent(
                    new RecordContactEventCommand(
                            CUSTOMER_ID,
                            CAMPAIGN_ID,
                            CommunicationChannel.EMAIL,
                            ContactEventType.NOTE,
                            ContactOutcome.CONVERTED,
                            "Sold",
                            OCCURRED_AT.plusSeconds(2)));

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));

            DashboardView dashboard = analyticsService.getDashboard();

            assertThat(dashboard.messagesSent()).isEqualTo(10L);
            assertThat(dashboard.openedCount()).isEqualTo(1L);
            assertThat(dashboard.clickedCount()).isEqualTo(1L);
            assertThat(dashboard.convertedCount()).isEqualTo(1L);
            assertThat(dashboard.openRate())
                    .isEqualByComparingTo(
                            AnalyticsCalculations.calculateOpenRate(1L, 10L));
            assertThat(dashboard.clickRate())
                    .isEqualByComparingTo(
                            AnalyticsCalculations.calculateClickRate(1L, 10L));
            assertThat(dashboard.conversionRate())
                    .isEqualByComparingTo(
                            AnalyticsCalculations.calculateConversionRate(1L, 10L));
        }

        @Test
        void platformTotalsSumPerCampaignMetricsFedByContactEvents() {
            Campaign a = campaign();
            Campaign b =
                    Campaign.create(
                            "Second campaign",
                            "More engagement",
                            user(),
                            null,
                            CampaignChannel.EMAIL);
            ReflectionTestUtils.setField(b, "id", UUID.fromString("50000000-0000-0000-0000-000000000657"));
            ReflectionTestUtils.setField(a, "status", CampaignStatus.ACTIVE);
            ReflectionTestUtils.setField(b, "status", CampaignStatus.ACTIVE);

            CampaignMetrics metricsA = launchedMetrics(a);
            metricsA.recordEngagementCounts(4, 2, 1, 1);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(b);
            metricsB.recordLaunchCounts(5, 0, 5);
            metricsB.recordEngagementCounts(3, 1, 0, 0);

            when(campaignRepository.findAll()).thenReturn(List.of(a, b));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            DashboardView dashboard = analyticsService.getDashboard();

            assertThat(dashboard.openedCount()).isEqualTo(7L);
            assertThat(dashboard.clickedCount()).isEqualTo(3L);
            assertThat(dashboard.messagesSent()).isEqualTo(15L);
            assertThat(dashboard.convertedCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 656)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(ContactEventsUpdateAnalyticsContract.CRITICAL_TEST_ITEM).isEqualTo(656);
            assertThat(ContactEventsUpdateAnalyticsContract.RULE_STATEMENT)
                    .isEqualTo("Contact events update analytics");
            assertThat(ContactEventsUpdateAnalyticsContract.BUSINESS_RULE_IDS)
                    .containsExactly("BR-034");
            assertThat(ContactEventsUpdateAnalyticsContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-103", "FR-104", "FR-105", "FR-106");
            assertThat(ContactEventsUpdateAnalyticsContract.METRIC_UPDATING_EVENT_TYPES)
                    .containsExactlyInAnyOrder(
                            ContactEventType.SENT,
                            ContactEventType.OPENED,
                            ContactEventType.CLICKED,
                            ContactEventType.REPLIED);
            assertThat(ContactEventsUpdateAnalyticsContract.CONVERSION_OUTCOME)
                    .isEqualTo(ContactOutcome.CONVERTED);
            assertThat(ContactEventsUpdateAnalyticsContract.PIPELINE_STEPS)
                    .containsExactly(
                            "record-contact-event",
                            "update-campaign-metrics",
                            "aggregate-analytics-dashboard");
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
                        "Analytics contact campaign",
                        "BR-034 contact events update analytics",
                        user(),
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Eve", "Engaged");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        return customer;
    }

    private static User user() {
        User user = User.create("agent656@test.example", "{noop}x", "Contact Agent");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    static final class ContactEventsUpdateAnalyticsContract {
        static final int CRITICAL_TEST_ITEM = 656;
        static final String RULE_STATEMENT = "Contact events update analytics";
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-034");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-103", "FR-104", "FR-105", "FR-106");
        static final java.util.List<ContactEventType> METRIC_UPDATING_EVENT_TYPES =
                java.util.List.of(
                        ContactEventType.SENT,
                        ContactEventType.OPENED,
                        ContactEventType.CLICKED,
                        ContactEventType.REPLIED);
        static final ContactOutcome CONVERSION_OUTCOME = ContactOutcome.CONVERTED;
        static final java.util.List<String> PIPELINE_STEPS =
                java.util.List.of(
                        "record-contact-event",
                        "update-campaign-metrics",
                        "aggregate-analytics-dashboard");

        private ContactEventsUpdateAnalyticsContract() {}
    }
}
