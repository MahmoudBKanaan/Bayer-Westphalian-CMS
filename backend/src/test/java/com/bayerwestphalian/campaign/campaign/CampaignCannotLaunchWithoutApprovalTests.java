package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sprint 16 critical test item <b>647</b>: Campaign cannot launch without approval.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code BR-005} — Campaigns cannot launch before Compliance Officer approval
 *   <li>{@code BR-032} — Submitted campaign cannot be launched before approval
 *   <li>{@code FR-060} / TC-001 — Only {@code APPROVED} campaigns may launch
 * </ul>
 *
 * <p>Authoritative enforcement is domain {@link Campaign#launch()} and {@link
 * CampaignService#launchCampaign(UUID)}. UI launch controls are convenience only.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("647 Campaign cannot launch without approval")
class CampaignCannotLaunchWithoutApprovalTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000647");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000647");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000648");

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

    private CampaignService campaignService;

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
    }

    @Nested
    @DisplayName("Domain: Campaign.launch / canLaunch (BR-005, BR-032, TC-001)")
    class DomainLaunchGate {

        @Test
        void onlyApprovedStatusCanLaunch() throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            User compliance = user(COMPLIANCE_ID, "Compliance Officer");
            Campaign campaign = draftCampaign(owner);
            assertThat(campaign.canLaunch()).isFalse();

            campaign.submit();
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            assertThat(campaign.canLaunch()).isFalse();

            campaign.approve(compliance);
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.APPROVED);
            assertThat(campaign.canLaunch()).isTrue();
        }

        @ParameterizedTest(name = "status {0} cannot launch without approval gate")
        @MethodSource(
                "com.bayerwestphalian.campaign.campaign.CampaignCannotLaunchWithoutApprovalTests#nonApprovedStatuses")
        void launchThrowsForEveryNonApprovedStatus(CampaignStatus status) throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = draftCampaign(owner);
            ReflectionTestUtils.setField(campaign, "status", status);

            assertThat(campaign.canLaunch()).isFalse();
            assertThatThrownBy(campaign::launch)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only APPROVED campaigns can be launched")
                    .hasMessageContaining(status.name());
            assertThat(campaign.getStatus()).isEqualTo(status);
        }

        @Test
        void submittedCampaignCannotLaunchUntilComplianceApproves() throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            User compliance = user(COMPLIANCE_ID, "Compliance Officer");
            Campaign campaign = draftCampaign(owner);
            campaign.submit();

            assertThatThrownBy(campaign::launch)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SUBMITTED");

            campaign.approve(compliance);
            campaign.launch();
            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        }

        @Test
        void rejectedCampaignCannotLaunchWithoutReapprovalWorkflow() throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = draftCampaign(owner);
            campaign.submit();
            campaign.reject("Missing consent statement");

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.REJECTED);
            assertThat(campaign.canLaunch()).isFalse();
            assertThatThrownBy(campaign::launch)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("REJECTED");
        }
    }

    @Nested
    @DisplayName("Service: CampaignService.launchCampaign (authoritative API gate)")
    class ServiceLaunchGate {

        @BeforeEach
        void authorizeOwner() {
            when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
            lenient()
                    .when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name()))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("DRAFT campaign cannot launch without approval")
        void draftCannotLaunch() throws Exception {
            Campaign campaign = draftCampaign(user(OWNER_ID, "Campaign Manager"));
            setId(campaign, CAMPAIGN_ID);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Only APPROVED")
                    .hasMessageContaining("DRAFT");

            verify(campaignRepository, never()).save(any(Campaign.class));
            verify(auditService, never()).logLaunch(any(), any(), any(), any(), any());
            verify(contactEventRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("SUBMITTED campaign cannot launch before Compliance Officer approval (BR-032)")
        void submittedCannotLaunchBeforeApproval() throws Exception {
            Campaign campaign = draftCampaign(user(OWNER_ID, "Campaign Manager"));
            campaign.submit();
            setId(campaign, CAMPAIGN_ID);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Only APPROVED")
                    .hasMessageContaining("SUBMITTED");

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
            verify(campaignRepository, never()).save(any(Campaign.class));
            verify(auditService, never())
                    .logLaunch(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("REJECTED campaign cannot launch without re-approval")
        void rejectedCannotLaunch() throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            Campaign campaign = draftCampaign(owner);
            campaign.submit();
            campaign.reject("Incomplete product selection");
            setId(campaign, CAMPAIGN_ID);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("REJECTED");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @ParameterizedTest(name = "service blocks launch when status is {0}")
        @EnumSource(
                value = CampaignStatus.class,
                names = {"DRAFT", "SUBMITTED", "REJECTED", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED"})
        void serviceBlocksAllNonApprovedStatuses(CampaignStatus status) throws Exception {
            Campaign campaign = draftCampaign(user(OWNER_ID, "Campaign Manager"));
            ReflectionTestUtils.setField(campaign, "status", status);
            setId(campaign, CAMPAIGN_ID);
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Only APPROVED");

            verify(campaignRepository, never()).save(any(Campaign.class));
        }

        @Test
        @DisplayName("APPROVED campaign can launch (positive control for BR-005 gate)")
        void approvedCanLaunchAsPositiveControl() throws Exception {
            User owner = user(OWNER_ID, "Campaign Manager");
            User compliance = user(COMPLIANCE_ID, "Compliance Officer");
            Campaign campaign = draftCampaign(owner);
            campaign.submit();
            campaign.approve(compliance);
            setId(campaign, CAMPAIGN_ID);

            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient()
                    .when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(List.of());
            lenient()
                    .when(
                            campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                                    any(), any()))
                    .thenReturn(List.of());
            lenient()
                    .when(
                            campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                                    any(), any()))
                    .thenReturn(0L);
            lenient()
                    .when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                    .thenReturn(Optional.empty());
            lenient()
                    .when(campaignMetricsRepository.save(any(CampaignMetrics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            lenient().when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));

            CampaignView view = campaignService.launchCampaign(CAMPAIGN_ID);

            assertThat(view.status()).isEqualTo(CampaignStatus.ACTIVE);
            verify(campaignRepository).save(any(Campaign.class));
            verify(auditService).logLaunch(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 647)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(CampaignCannotLaunchWithoutApprovalContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(647);
            assertThat(CampaignCannotLaunchWithoutApprovalContract.RULE_STATEMENT)
                    .isEqualTo("Campaign cannot launch without approval");
            assertThat(CampaignCannotLaunchWithoutApprovalContract.BUSINESS_RULE_IDS)
                    .containsExactly("BR-005", "BR-032");
            assertThat(CampaignCannotLaunchWithoutApprovalContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-060");
            assertThat(CampaignCannotLaunchWithoutApprovalContract.ALLOWED_LAUNCH_STATUS)
                    .isEqualTo(CampaignStatus.APPROVED);
            assertThat(CampaignCannotLaunchWithoutApprovalContract.BLOCKED_PRE_APPROVAL_STATUSES)
                    .containsExactly(CampaignStatus.DRAFT, CampaignStatus.SUBMITTED, CampaignStatus.REJECTED);
        }
    }

    static Stream<CampaignStatus> nonApprovedStatuses() {
        return Stream.of(CampaignStatus.values()).filter(status -> status != CampaignStatus.APPROVED);
    }

    private Campaign draftCampaign(User owner) {
        return Campaign.create(
                "Critical launch gate outreach",
                "Must not contact audience before compliance approval",
                owner,
                null,
                CampaignChannel.EMAIL);
    }

    private User user(UUID id, String name) throws Exception {
        User user =
                User.create(
                        name.toLowerCase().replace(' ', '.') + "@test.example",
                        "$2a$10$hash",
                        name);
        setId(user, id);
        return user;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    /**
     * Lightweight contract constants for documentation/frontend alignment (item 647).
     */
    static final class CampaignCannotLaunchWithoutApprovalContract {
        static final int CRITICAL_TEST_ITEM = 647;
        static final String RULE_STATEMENT = "Campaign cannot launch without approval";
        static final List<String> BUSINESS_RULE_IDS = List.of("BR-005", "BR-032");
        static final List<String> FUNCTIONAL_REQUIREMENT_IDS = List.of("FR-060");
        static final CampaignStatus ALLOWED_LAUNCH_STATUS = CampaignStatus.APPROVED;
        static final List<CampaignStatus> BLOCKED_PRE_APPROVAL_STATUSES =
                List.of(CampaignStatus.DRAFT, CampaignStatus.SUBMITTED, CampaignStatus.REJECTED);

        private CampaignCannotLaunchWithoutApprovalContract() {}
    }
}
