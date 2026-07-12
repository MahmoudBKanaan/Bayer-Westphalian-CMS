package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsCalculations;
import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.analytics.DashboardView;
import com.bayerwestphalian.campaign.analytics.ExecutiveDashboardView;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
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
 * KB item 449 acceptance: Sent count updates after launch.
 *
 * <p>Item 420 / FR-103 / item 282: launching an approved campaign creates SENT contact events for
 * stored eligible recipients and refreshes {@code campaign_metrics.sent_count} to that event total
 * (with eligible/excluded audience counters). Dashboard {@code messagesSent} and executive {@code
 * totalSent} sum those per-campaign values.
 *
 * <p>Companion coverage: {@code CalculateSentCountTests} (item 420) and launch tests in {@code
 * CampaignServiceTests}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("449 Sent count updates after launch")
class SentCountUpdatesAfterLaunchTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000449");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000449");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000450");
    private static final UUID CUSTOMER_A = UUID.fromString("20000000-0000-0000-0000-000000000449");
    private static final UUID CUSTOMER_B = UUID.fromString("20000000-0000-0000-0000-000000000450");
    private static final UUID CUSTOMER_C = UUID.fromString("20000000-0000-0000-0000-000000000451");

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignProductRepository campaignProductRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;
    @Mock private AuditService auditService;
    @Mock private CampaignRecipientRepository campaignRecipientRepository;
    @Mock private ContactEventRepository contactEventRepository;
    @Mock private CampaignMetricsRepository campaignMetricsRepository;
    @Mock private CampaignProductRepository analyticsProductRepository;

    private CampaignService campaignService;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        campaignService =
                new CampaignService(
                        campaignRepository,
                        campaignProductRepository,
                        segmentRepository,
                        productRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
        ReflectionTestUtils.setField(
                campaignService, "campaignRecipientRepository", campaignRecipientRepository);
        ReflectionTestUtils.setField(
                campaignService, "contactEventRepository", contactEventRepository);
        ReflectionTestUtils.setField(
                campaignService, "campaignMetricsRepository", campaignMetricsRepository);

        analyticsService =
                new AnalyticsService(
                        campaignRepository, campaignMetricsRepository, analyticsProductRepository);
    }

    @Nested
    @DisplayName("Formula and launch metrics helpers")
    class Formula {

        @Test
        void sentCountFormulaAcceptsNonNegativeTotals() {
            assertThat(CampaignMetrics.calculateSentCount(0)).isZero();
            assertThat(CampaignMetrics.calculateSentCount(1)).isEqualTo(1);
            assertThat(CampaignMetrics.calculateSentCount(10L)).isEqualTo(10);
            assertThat(AnalyticsCalculations.calculateSentCount(7)).isEqualTo(7);
        }

        @Test
        void sentCountFormulaRejectsNegatives() {
            assertThatThrownBy(() -> CampaignMetrics.calculateSentCount(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sent count must not be negative");
            assertThatThrownBy(() -> CampaignMetrics.calculateSentCount(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sent count must not be negative");
        }

        @Test
        void recordLaunchCountsStoresSentCountFromLaunchEventTotal() {
            Campaign campaign = approvedCampaign();
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);

            // Launch-style refresh: sent equals number of SENT contact events created.
            metrics.recordLaunchCounts(3, 1, 3);

            assertThat(metrics.getSentCount()).isEqualTo(3);
            assertThat(metrics.calculateSentCount()).isEqualTo(3);
            assertThat(metrics.getEligibleCount()).isEqualTo(3);
            assertThat(metrics.getExcludedCount()).isEqualTo(1);
            assertThat(metrics.getAudienceSize()).isEqualTo(4);
        }

        @Test
        void recordLaunchCountsRejectsNegativeSentInput() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(approvedCampaign());

            assertThatThrownBy(() -> metrics.recordLaunchCounts(1, 0, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sent count must not be negative");
        }
    }

    @Nested
    @DisplayName("CampaignService.launchCampaign updates sent_count")
    class LaunchFlow {

        @Test
        void launchSetsSentCountToNumberOfEligibleContactEventsCreated() {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = approvedCampaign(owner);
            CampaignRecipient first = CampaignRecipient.eligible(campaign, customer(CUSTOMER_A));
            CampaignRecipient second = CampaignRecipient.eligible(campaign, customer(CUSTOMER_B));

            stubLaunchAuth(owner);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(List.of(first, second));
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(2L);
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                    .thenReturn(1L);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.empty());
            when(campaignRepository.save(any(Campaign.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
            when(contactEventRepository.saveAll(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CampaignView launched = campaignService.launchCampaign(CAMPAIGN_ID);

            ArgumentCaptor<CampaignMetrics> metricsCaptor =
                    ArgumentCaptor.forClass(CampaignMetrics.class);
            verify(campaignMetricsRepository).save(metricsCaptor.capture());
            CampaignMetrics metrics = metricsCaptor.getValue();

            assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);
            // Item 449: sent_count updates after launch to SENT contact-event total.
            assertThat(metrics.getSentCount()).isEqualTo(2);
            assertThat(metrics.calculateSentCount()).isEqualTo(2);
            assertThat(metrics.getEligibleCount()).isEqualTo(2);
            assertThat(metrics.getExcludedCount()).isEqualTo(1);
            assertThat(metrics.getAudienceSize()).isEqualTo(3);
            assertThat(first.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.SENT);
            assertThat(second.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.SENT);
        }

        @Test
        void launchRefreshesExistingMetricsSentCountFromNewLaunchEvents() {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = approvedCampaign(owner);
            CampaignRecipient first = CampaignRecipient.eligible(campaign, customer(CUSTOMER_A));
            CampaignRecipient second = CampaignRecipient.eligible(campaign, customer(CUSTOMER_B));
            CampaignRecipient third = CampaignRecipient.eligible(campaign, customer(CUSTOMER_C));
            CampaignMetrics existing = CampaignMetrics.forCampaign(campaign);
            // Stale pre-launch snapshot (e.g. prior partial refresh).
            existing.recordLaunchCounts(5, 2, 5);

            stubLaunchAuth(owner);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(List.of(first, second, third));
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(3L);
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                    .thenReturn(2L);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.of(existing));
            when(campaignRepository.save(any(Campaign.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
            when(contactEventRepository.saveAll(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            campaignService.launchCampaign(CAMPAIGN_ID);

            verify(campaignMetricsRepository).save(existing);
            assertThat(existing.getSentCount()).isEqualTo(3);
            assertThat(existing.getEligibleCount()).isEqualTo(3);
            assertThat(existing.getExcludedCount()).isEqualTo(2);
            assertThat(existing.getAudienceSize()).isEqualTo(5);
        }

        @Test
        void launchWithNoEligibleRecipientsSetsSentCountToZero() {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = approvedCampaign(owner);

            stubLaunchAuth(owner);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(List.of());
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(0L);
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                    .thenReturn(4L);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.empty());
            when(campaignRepository.save(any(Campaign.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

            campaignService.launchCampaign(CAMPAIGN_ID);

            ArgumentCaptor<CampaignMetrics> metricsCaptor =
                    ArgumentCaptor.forClass(CampaignMetrics.class);
            verify(campaignMetricsRepository).save(metricsCaptor.capture());
            CampaignMetrics metrics = metricsCaptor.getValue();

            verify(contactEventRepository, never()).saveAll(any());
            assertThat(metrics.getSentCount()).isZero();
            assertThat(metrics.getEligibleCount()).isZero();
            assertThat(metrics.getExcludedCount()).isEqualTo(4);
            assertThat(metrics.getAudienceSize()).isEqualTo(4);
        }

        @Test
        void launchSentCountMatchesSavedSentContactEventCount() {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = approvedCampaign(owner);
            List<CampaignRecipient> recipients =
                    List.of(
                            CampaignRecipient.eligible(campaign, customer(CUSTOMER_A)),
                            CampaignRecipient.eligible(campaign, customer(CUSTOMER_B)));

            stubLaunchAuth(owner);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(recipients);
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.ELIGIBLE))
                    .thenReturn(2L);
            when(campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                            CAMPAIGN_ID, CampaignRecipientStatus.EXCLUDED))
                    .thenReturn(0L);
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.empty());
            when(campaignRepository.save(any(Campaign.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());
            when(contactEventRepository.saveAll(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            campaignService.launchCampaign(CAMPAIGN_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ContactEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<CampaignMetrics> metricsCaptor =
                    ArgumentCaptor.forClass(CampaignMetrics.class);
            verify(contactEventRepository).saveAll(eventsCaptor.capture());
            verify(campaignMetricsRepository).save(metricsCaptor.capture());

            List<ContactEvent> events = eventsCaptor.getValue();
            CampaignMetrics metrics = metricsCaptor.getValue();
            assertThat(events).hasSize(2);
            assertThat(events)
                    .allMatch(event -> event.getEventType() == ContactEventType.SENT);
            assertThat(metrics.getSentCount()).isEqualTo(events.size());
        }
    }

    @Nested
    @DisplayName("Analytics surfaces after launch metrics refresh")
    class AnalyticsSurfaces {

        @Test
        void dashboardMessagesSentReflectsLaunchUpdatedSentCounts() {
            Campaign campaign = approvedCampaign();
            ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            // Values as if just written by updateMetricsForLaunch after 2 SENT events.
            metrics.recordLaunchCounts(2, 1, 2);

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));

            DashboardView dashboard = analyticsService.getDashboard();

            // FR-103: dashboard messages sent is the sum of launch-updated sent counts.
            assertThat(dashboard.messagesSent()).isEqualTo(2L);
            assertThat(dashboard.recentCampaignMetrics())
                    .extracting(CampaignMetricsView::sentCount)
                    .containsExactly(2);
        }

        @Test
        void executiveTotalSentReflectsLaunchUpdatedSentCounts() {
            Campaign campaign = approvedCampaign();
            ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(5, 2, 5);

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
            when(analyticsProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(executive.totalSent()).isEqualTo(5L);
            assertThat(executive.totalEligible()).isEqualTo(5L);
        }

        @Test
        void campaignMetricsViewExposesLaunchUpdatedSentCount() {
            Campaign campaign = approvedCampaign();
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(4, 1, 4);

            CampaignMetricsView view = CampaignMetricsView.from(metrics);

            assertThat(view.sentCount()).isEqualTo(4);
            assertThat(view.eligibleCount()).isEqualTo(4);
            assertThat(view.excludedCount()).isEqualTo(1);
        }
    }

    private void stubLaunchAuth(User owner) {
        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    }

    private Campaign approvedCampaign() {
        return approvedCampaign(user(OWNER_ID, "Campaign Manager"));
    }

    private Campaign approvedCampaign(User owner) {
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign =
                Campaign.create(
                        "Launch sent campaign",
                        "Verify sent_count after launch",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        campaign.submit();
        campaign.approve(compliance);
        return campaign;
    }

    private static User user(UUID id, String name) {
        User user =
                User.create(
                        name.toLowerCase().replace(' ', '.') + "@test.example",
                        "{noop}x",
                        name);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Customer customer(UUID id) {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Customer", "449");
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }
}
