package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.util.List;
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
 * KB item 243 / FR-050 / FR-057: Campaign Manager create draft campaign persists DRAFT ownership.
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
class CampaignManagerCanCreateDraftCampaignIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_manager_create_draft_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User campaignManager;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        campaignManager = persistUser("campaign-manager-draft");
        when(authorizationExpressions.currentUserId()).thenReturn(campaignManager.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void campaignManagerCreatesDraftCampaignOwnedBySelf() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Life renewal outreach",
                                "Promote life insurance renewals",
                                null,
                                CampaignChannel.EMAIL,
                                "Renew your cover",
                                "Dear customer, ...",
                                null,
                                null,
                                List.of()));

        entityManager.flush();
        entityManager.clear();

        assertThat(created.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(created.ownerUserId()).isEqualTo(campaignManager.getId());
        assertThat(created.ownerFullName()).isEqualTo("Campaign Manager Draft User");
        assertThat(created.name()).isEqualTo("Life renewal outreach");

        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(reloaded.isDraft()).isTrue();
        assertThat(reloaded.canEdit()).isTrue();
        assertThat(reloaded.canLaunch()).isFalse();
        assertThat(reloaded.getOwnerUserId()).isEqualTo(campaignManager.getId());
        assertThat(reloaded.getApprovedBy()).isNull();
        assertThat(reloaded.getRejectionReason()).isNull();

        List<AuditLog> createLogs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :type and a.entityId = :id and a.action = :action",
                                AuditLog.class)
                        .setParameter("type", CampaignService.AUDIT_ENTITY_TYPE)
                        .setParameter("id", created.id())
                        .setParameter("action", "CREATE")
                        .getResultList();
        assertThat(createLogs).hasSize(1);
        assertThat(createLogs.getFirst().getActorUserId()).isEqualTo(campaignManager.getId());
        assertThat(createLogs.getFirst().getNewValue()).containsEntry("status", "DRAFT");
    }

    @Test
    void campaignManagerCanCreateMinimalDraftWithRequiredFieldsOnly() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Minimal draft",
                                "Minimal objective",
                                null,
                                CampaignChannel.SMS,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        assertThat(created.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(created.channel()).isEqualTo(CampaignChannel.SMS);
        assertThat(created.segmentId()).isNull();
        assertThat(created.productIds()).isEmpty();
        assertThat(created.ownerUserId()).isEqualTo(campaignManager.getId());
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-manager-create-draft-integration.test",
                        "{noop}password",
                        "Campaign Manager Draft User");
        return entityManager.persistAndFlush(user);
    }
}
