package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB items 235 / 252: campaign approval and rejection write immutable workflow audit logs for
 * entity type {@code campaigns}.
 */
@ExtendWith(MockitoExtension.class)
class CampaignApprovalCreatesAuditLogTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID COMPLIANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000106");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignProductRepository campaignProductRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private AuditService auditService;
    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
        when(auditLogRepository.save(any(AuditLog.class)))
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
    void approveCampaignPersistsApproveAuditLogWithStatusTransition() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(COMPLIANCE_ID)).thenReturn(Optional.of(compliance));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        campaignService.approveCampaign(
                CAMPAIGN_ID, new ApproveCampaignCommand("Audience eligibility verified."));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("APPROVE");
        assertThat(auditLog.getEntityType()).isEqualTo(CampaignService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getEntityId()).isEqualTo(CAMPAIGN_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(COMPLIANCE_ID);
        assertThat(auditLog.getOldValue())
                .containsEntry("status", "SUBMITTED")
                .containsEntry("approvedByUserId", null)
                .containsEntry("complianceReviewNotes", null);
        assertThat(auditLog.getNewValue())
                .containsEntry("id", CAMPAIGN_ID.toString())
                .containsEntry("status", "APPROVED")
                .containsEntry("approvedByUserId", COMPLIANCE_ID.toString())
                .containsEntry("complianceReviewNotes", "Audience eligibility verified.")
                .containsEntry("rejectionReason", null);
        assertThat(auditLog.getNewValue().get("approvedAt")).isNotNull();
    }

    @Test
    void approveCampaignWithoutNotesStillWritesApproveAuditLog() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(COMPLIANCE_ID)).thenReturn(Optional.of(compliance));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        campaignService.approveCampaign(CAMPAIGN_ID);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("APPROVE");
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getNewValue())
                .containsEntry("status", "APPROVED")
                .containsEntry("complianceReviewNotes", null);
    }

    @Test
    void rejectCampaignPersistsRejectAuditLogWithReasonAndNotes() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignProductRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of());

        campaignService.rejectCampaign(
                CAMPAIGN_ID,
                new RejectCampaignCommand(
                        "Missing consent language", "Add explicit consent wording."));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("REJECT");
        assertThat(auditLog.getEntityType()).isEqualTo(CampaignService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getEntityId()).isEqualTo(CAMPAIGN_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(COMPLIANCE_ID);
        assertThat(auditLog.getOldValue())
                .containsEntry("status", "SUBMITTED")
                .containsEntry("rejectionReason", null)
                .containsEntry("complianceReviewNotes", null);
        assertThat(auditLog.getNewValue())
                .containsEntry("id", CAMPAIGN_ID.toString())
                .containsEntry("status", "REJECTED")
                .containsEntry("rejectionReason", "Missing consent language")
                .containsEntry("complianceReviewNotes", "Add explicit consent wording.")
                .containsEntry("approvedByUserId", null);
    }

    @Test
    void failedApprovalDoesNotWriteAuditLog() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        Campaign campaign = draftCampaign(owner);
        campaign.submit();
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(OWNER_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.approveCampaign(CAMPAIGN_ID))
                .isInstanceOf(ForbiddenException.class);

        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void approveFromNonSubmittedStatusDoesNotWriteAuditLog() throws Exception {
        User owner = user(OWNER_ID, "Campaign Manager");
        User compliance = user(COMPLIANCE_ID, "Compliance Officer");
        Campaign campaign = draftCampaign(owner);
        setId(campaign, CAMPAIGN_ID);

        when(authorizationExpressions.currentUserId()).thenReturn(COMPLIANCE_ID);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(userRepository.findById(COMPLIANCE_ID)).thenReturn(Optional.of(compliance));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.approveCampaign(CAMPAIGN_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only SUBMITTED");

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
                User.create(
                        name.toLowerCase().replace(' ', '.') + "@test",
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
}
