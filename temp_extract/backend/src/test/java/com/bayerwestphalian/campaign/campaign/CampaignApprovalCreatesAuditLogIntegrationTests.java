package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * KB item 235: campaign approval persists an APPROVE audit log row for entity type {@code
 * campaigns}.
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({CampaignService.class, AuditService.class})
class CampaignApprovalCreatesAuditLogIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_approval_audit_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User owner;
    private User compliance;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("approval-audit-owner");
        compliance = persistUser("approval-audit-officer");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void approveCampaignPersistsApproveAuditLogInSameTransaction() {
        CampaignView created = createAndSubmit("Approval audit campaign");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView approved =
                campaignService.approveCampaign(
                        created.id(),
                        new ApproveCampaignCommand("Consent and segment eligibility verified."));

        entityManager.flush();
        entityManager.clear();

        assertThat(campaignRepository.findById(created.id())).isPresent();
        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);

        List<AuditLog> approveLogs =
                findCampaignAuditLogs(created.id()).stream()
                        .filter(log -> "APPROVE".equals(log.getAction()))
                        .toList();
        assertThat(approveLogs).hasSize(1);

        AuditLog approveLog = approveLogs.getFirst();
        assertThat(approveLog.getEntityType()).isEqualTo(CampaignService.AUDIT_ENTITY_TYPE);
        assertThat(approveLog.getEntityId()).isEqualTo(created.id());
        assertThat(approveLog.getActorUserId()).isEqualTo(compliance.getId());
        assertThat(approveLog.getCreatedAt()).isNotNull();

        Map<String, Object> oldValue = approveLog.getOldValue();
        Map<String, Object> newValue = approveLog.getNewValue();
        assertThat(oldValue)
                .containsEntry("status", "SUBMITTED")
                .containsEntry("approvedByUserId", null);
        assertThat(newValue)
                .containsEntry("id", created.id().toString())
                .containsEntry("status", "APPROVED")
                .containsEntry("approvedByUserId", compliance.getId().toString())
                .containsEntry(
                        "complianceReviewNotes", "Consent and segment eligibility verified.");
        assertThat(newValue.get("approvedAt")).isNotNull();
    }

    @Test
    void approveWithoutNotesStillWritesApproveAuditLog() {
        CampaignView created = createAndSubmit("Approval audit without notes");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.approveCampaign(created.id(), new ApproveCampaignCommand(null));

        entityManager.flush();
        entityManager.clear();

        List<AuditLog> approveLogs =
                findCampaignAuditLogs(created.id()).stream()
                        .filter(log -> "APPROVE".equals(log.getAction()))
                        .toList();
        assertThat(approveLogs).hasSize(1);
        assertThat(approveLogs.getFirst().getNewValue())
                .containsEntry("status", "APPROVED")
                .containsEntry("complianceReviewNotes", null);
    }

    private CampaignView createAndSubmit(String name) {
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                name,
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        return campaignService.submitCampaign(created.id());
    }

    private List<AuditLog> findCampaignAuditLogs(UUID campaignId) {
        return entityManager
                .getEntityManager()
                .createQuery(
                        "select a from AuditLog a where a.entityType = :type and a.entityId = :id order by a.createdAt asc",
                        AuditLog.class)
                .setParameter("type", CampaignService.AUDIT_ENTITY_TYPE)
                .setParameter("id", campaignId)
                .getResultList();
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-approval-audit-integration.test",
                        "{noop}password",
                        "Campaign Approval Audit User");
        return entityManager.persistAndFlush(user);
    }
}
