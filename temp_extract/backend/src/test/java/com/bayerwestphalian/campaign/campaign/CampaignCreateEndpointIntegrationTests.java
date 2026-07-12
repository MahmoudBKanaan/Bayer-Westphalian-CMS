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
 * KB item 217: create campaign endpoint path through {@link CampaignService#createCampaign} persists
 * a DRAFT campaign with optional segment, products, message, and schedule (FR-050 / FR-057). Create
 * also writes a CREATE audit log (item 233).
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
class CampaignCreateEndpointIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_create_endpoint_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignProductRepository campaignProductRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User owner;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("create-endpoint-owner");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void createCampaignEndpointPersistsDraftWithSegmentProductsMessageAndSchedule() {
        Segment segment =
                entityManager.persistAndFlush(
                        Segment.create(
                                "Munich prospects",
                                "Location audience",
                                owner,
                                SegmentVisibility.TEAM));
        Product product =
                entityManager.persistAndFlush(
                        Product.create(
                                "Life Plan",
                                ProductType.LIFE_INSURANCE,
                                new BigDecimal("55.00"),
                                12));

        // Mirrors POST /api/campaigns body → CreateCampaignCommand mapping used by the controller.
        CreateCampaignCommand command =
                new CreateCampaignRequest(
                                "Life renewal outreach",
                                "Promote life insurance renewals",
                                segment.getId(),
                                CampaignChannel.EMAIL,
                                "Renew your cover",
                                "Dear customer, ...",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                List.of(product.getId()))
                        .toCommand();

        CampaignView created = campaignService.createCampaign(command);

        entityManager.flush();
        entityManager.clear();

        assertThat(created.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(created.name()).isEqualTo("Life renewal outreach");
        assertThat(created.ownerUserId()).isEqualTo(owner.getId());
        assertThat(created.segmentId()).isEqualTo(segment.getId());
        assertThat(created.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(created.messageSubject()).isEqualTo("Renew your cover");
        assertThat(created.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(created.productIds()).containsExactly(product.getId());

        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(reloaded.isDraft()).isTrue();
        assertThat(reloaded.canEdit()).isTrue();
        assertThat(reloaded.canLaunch()).isFalse();
        assertThat(campaignProductRepository.findByCampaignId(created.id())).hasSize(1);

        List<AuditLog> logs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :type and a.entityId = :id",
                                AuditLog.class)
                        .setParameter("type", CampaignService.AUDIT_ENTITY_TYPE)
                        .setParameter("id", created.id())
                        .getResultList();
        assertThat(logs).isNotEmpty();
        AuditLog createLog = logs.getFirst();
        assertThat(createLog.getAction()).isEqualTo("CREATE");
        assertThat(createLog.getEntityType()).isEqualTo("campaigns");
        assertThat(createLog.getActorUserId()).isEqualTo(owner.getId());
        assertThat(createLog.getOldValue()).isNull();
        assertThat(createLog.getNewValue())
                .containsEntry("name", "Life renewal outreach")
                .containsEntry("status", "DRAFT")
                .containsEntry("channel", "EMAIL");
    }

    @Test
    void createCampaignEndpointPersistsMinimalDraftWithRequiredFieldsOnly() {
        CreateCampaignCommand command =
                new CreateCampaignRequest(
                                "Minimal draft",
                                "Minimal objective",
                                null,
                                CampaignChannel.PHONE,
                                null,
                                null,
                                null,
                                null,
                                null)
                        .toCommand();

        CampaignView created = campaignService.createCampaign(command);

        entityManager.flush();
        entityManager.clear();

        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Minimal draft");
        assertThat(reloaded.getChannel()).isEqualTo(CampaignChannel.PHONE);
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(reloaded.getSegment()).isNull();
        assertThat(campaignProductRepository.findByCampaignId(created.id())).isEmpty();
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-create-endpoint-integration.test",
                        "{noop}password",
                        "Campaign Create Endpoint User");
        return entityManager.persistAndFlush(user);
    }
}
