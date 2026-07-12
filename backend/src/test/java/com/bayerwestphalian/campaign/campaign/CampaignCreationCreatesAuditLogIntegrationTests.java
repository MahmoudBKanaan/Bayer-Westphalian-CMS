package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
 * KB item 233: campaign creation persists a CREATE audit log row for entity type {@code campaigns}.
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
class CampaignCreationCreatesAuditLogIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_creation_audit_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User owner;
    private Segment segment;
    private Product product;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("creation-audit-owner");
        segment =
                entityManager.persistAndFlush(
                        Segment.create(
                                "Creation audit segment",
                                "Audience for create audit",
                                owner,
                                SegmentVisibility.TEAM));
        product =
                entityManager.persistAndFlush(
                        Product.create(
                                "Creation audit product",
                                ProductType.LIFE_INSURANCE,
                                new BigDecimal("120.00"),
                                12));
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void createCampaignPersistsCreateAuditLogInSameTransaction() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Life renewal outreach",
                                "Promote life insurance renewals",
                                segment.getId(),
                                CampaignChannel.EMAIL,
                                "Renew your cover",
                                "Dear customer, ...",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                List.of(product.getId())));

        entityManager.flush();
        entityManager.clear();

        assertThat(campaignRepository.findById(created.id())).isPresent();

        List<AuditLog> logs = findCampaignAuditLogs(created.id());
        assertThat(logs).hasSize(1);

        AuditLog createLog = logs.getFirst();
        assertThat(createLog.getAction()).isEqualTo("CREATE");
        assertThat(createLog.getEntityType()).isEqualTo(CampaignService.AUDIT_ENTITY_TYPE);
        assertThat(createLog.getEntityId()).isEqualTo(created.id());
        assertThat(createLog.getActorUserId()).isEqualTo(owner.getId());
        assertThat(createLog.getOldValue()).isNull();
        assertThat(createLog.getCreatedAt()).isNotNull();

        Map<String, Object> payload = createLog.getNewValue();
        assertThat(payload)
                .containsEntry("id", created.id().toString())
                .containsEntry("name", "Life renewal outreach")
                .containsEntry("objective", "Promote life insurance renewals")
                .containsEntry("status", "DRAFT")
                .containsEntry("ownerUserId", owner.getId().toString())
                .containsEntry("segmentId", segment.getId().toString())
                .containsEntry("channel", "EMAIL")
                .containsEntry("messageSubject", "Renew your cover")
                .containsEntry("messageBody", "Dear customer, ...")
                .containsEntry("startDate", "2026-09-01")
                .containsEntry("endDate", "2026-09-30");
        assertThat(payload.get("productIds")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> productIds = (List<String>) payload.get("productIds");
        assertThat(productIds).containsExactly(product.getId().toString());
    }

    @Test
    void createMinimalDraftStillWritesCreateAuditLog() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Minimal audit draft",
                                "Objective only",
                                null,
                                CampaignChannel.SMS,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        entityManager.flush();
        entityManager.clear();

        List<AuditLog> logs = findCampaignAuditLogs(created.id());
        assertThat(logs).hasSize(1);
        AuditLog createLog = logs.getFirst();
        assertThat(createLog.getAction()).isEqualTo("CREATE");
        assertThat(createLog.getEntityType()).isEqualTo("campaigns");
        assertThat(createLog.getNewValue())
                .containsEntry("name", "Minimal audit draft")
                .containsEntry("status", "DRAFT")
                .containsEntry("channel", "SMS")
                .containsEntry("segmentId", null);
    }

    private List<AuditLog> findCampaignAuditLogs(java.util.UUID campaignId) {
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
                        emailPrefix + "@campaign-creation-audit-integration.test",
                        "{noop}password",
                        "Campaign Creation Audit User");
        return entityManager.persistAndFlush(user);
    }
}
