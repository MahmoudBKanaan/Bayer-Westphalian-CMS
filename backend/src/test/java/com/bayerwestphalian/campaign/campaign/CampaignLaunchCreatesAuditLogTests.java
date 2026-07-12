package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 530 (FR-060 / BR-005; also items 279–281): campaign launch writes an immutable {@code
 * LAUNCH} audit log on entity type {@code campaigns}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("530 Log campaign launch")
class CampaignLaunchCreatesAuditLogTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000530");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID OTHER_ID = UUID.fromString("10000000-0000-0000-0000-000000000199");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000106");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignProductRepository campaignProductRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        AuditService auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        campaignService =
                new CampaignService(
                        campaignRepository,
                        campaignProductRepository,
                        segmentRepository,
                        productRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Test
    @DisplayName("551 Campaign launch creates audit log")
    void launchApprovedCampaignPersistsLaunchAuditLogWithStatusTransition() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        lenient()
                .when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name()))
                .thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient()
                .when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID))
                .thenReturn(List.of());

        CampaignView view = campaignService.launchCampaign(CAMPAIGN_ID);

        assertThat(view.status()).isEqualTo(CampaignStatus.ACTIVE);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("LAUNCH");
        assertThat(auditLog.getEntityType()).isEqualTo(CampaignService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getEntityId()).isEqualTo(CAMPAIGN_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(OWNER_ID);
        assertThat(auditLog.getOldValue())
                .containsEntry("id", CAMPAIGN_ID.toString())
                .containsEntry("status", "APPROVED")
                .containsEntry("name", "Life renewal outreach")
                .containsEntry("channel", "EMAIL")
                .containsEntry("ownerUserId", OWNER_ID.toString())
                .containsEntry("approvedByUserId", COMPLIANCE_ID.toString());
        assertThat(auditLog.getNewValue())
                .containsEntry("id", CAMPAIGN_ID.toString())
                .containsEntry("status", "ACTIVE")
                .containsEntry("name", "Life renewal outreach")
                .containsEntry("approvedByUserId", COMPLIANCE_ID.toString())
                .containsEntry("productIds", List.of());
        assertThat(auditLog.getNewValue().get("approvedAt")).isNotNull();
    }

    @Test
    void launchDraftCampaignDoesNotWriteAuditLog() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        lenient()
                .when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name()))
                .thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only APPROVED");

        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void launchSubmittedButUnapprovedCampaignDoesNotWriteAuditLog() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        lenient()
                .when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name()))
                .thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only APPROVED");

        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void launchByNonOwnerDoesNotWriteAuditLog() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        campaign.approve(compliance);
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(OTHER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void launchDoesNotWriteAuditWhenCampaignMissing() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.launchCampaign(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        return auditLogCaptor.getValue();
    }

    private Campaign draftCampaign(User owner) {
        return Campaign.create(
                "Life renewal outreach",
                "Promote life insurance renewals",
                owner,
                null,
                CampaignChannel.EMAIL);
    }

    private User user(UUID id, String name) throws Exception {
        User user =
                User.create(name.toLowerCase().replace(' ', '.') + "@test", "$2a$10$hash", name);
        setId(user, id);
        return user;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
