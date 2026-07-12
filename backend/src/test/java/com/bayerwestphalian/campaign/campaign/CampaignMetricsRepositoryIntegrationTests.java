package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * KB item 413: CampaignMetricRepository persistence against PostgreSQL — one metrics row per
 * campaign, {@code findByCampaignId}, save/update of counters and financial estimates.
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CampaignMetricsRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_metrics_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private CampaignMetricsRepository campaignMetricsRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void findsMetricsByCampaignId() {
        User owner = persistUser("metrics-find-owner");
        Campaign campaign = persistCampaign("Metrics find campaign", owner);
        Campaign other = persistCampaign("Other metrics campaign", owner);

        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(10, 2, 10);
        metrics.recordEngagementCounts(4, 2, 1, 1);
        metrics.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
        campaignMetricsRepository.saveAndFlush(metrics);

        CampaignMetrics otherMetrics = CampaignMetrics.forCampaign(other);
        otherMetrics.recordLaunchCounts(1, 0, 1);
        campaignMetricsRepository.saveAndFlush(otherMetrics);
        entityManager.clear();

        Optional<CampaignMetrics> found =
                campaignMetricsRepository.findByCampaignId(campaign.getId());
        Optional<CampaignMetrics> byPropertyPath =
                campaignMetricsRepository.findByCampaign_Id(campaign.getId());

        assertThat(found).isPresent();
        assertThat(byPropertyPath).map(CampaignMetrics::getId).isEqualTo(found.map(CampaignMetrics::getId));
        assertThat(found.get().getCampaignId()).isEqualTo(campaign.getId());
        assertThat(found.get().getAudienceSize()).isEqualTo(12);
        assertThat(found.get().getEligibleCount()).isEqualTo(10);
        assertThat(found.get().getExcludedCount()).isEqualTo(2);
        assertThat(found.get().getSentCount()).isEqualTo(10);
        assertThat(found.get().getOpenedCount()).isEqualTo(4);
        assertThat(found.get().getClickedCount()).isEqualTo(2);
        assertThat(found.get().getRepliedCount()).isEqualTo(1);
        assertThat(found.get().getConvertedCount()).isEqualTo(1);
        assertThat(found.get().getEstimatedCost()).isEqualByComparingTo("100.00");
        assertThat(found.get().getEstimatedRevenue()).isEqualByComparingTo("150.00");
        assertThat(found.get().getEstimatedRoi()).isEqualByComparingTo("0.50");
        assertThat(found.get().getUpdatedAt()).isNotNull();

        assertThat(campaignMetricsRepository.findByCampaignId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existsByCampaignIdReflectsPersistedMetrics() {
        User owner = persistUser("metrics-exists-owner");
        Campaign campaign = persistCampaign("Metrics exists campaign", owner);

        assertThat(campaignMetricsRepository.existsByCampaignId(campaign.getId())).isFalse();

        campaignMetricsRepository.saveAndFlush(CampaignMetrics.forCampaign(campaign));
        entityManager.clear();

        assertThat(campaignMetricsRepository.existsByCampaignId(campaign.getId())).isTrue();
        assertThat(campaignMetricsRepository.existsByCampaign_Id(campaign.getId())).isTrue();
    }

    @Test
    void updatesExistingMetricsForSameCampaign() {
        User owner = persistUser("metrics-update-owner");
        Campaign campaign = persistCampaign("Metrics update campaign", owner);

        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(5, 1, 5);
        metrics = campaignMetricsRepository.saveAndFlush(metrics);
        UUID metricsId = metrics.getId();
        entityManager.clear();

        CampaignMetrics reloaded =
                campaignMetricsRepository.findByCampaignId(campaign.getId()).orElseThrow();
        reloaded.recordLaunchCounts(8, 2, 8);
        reloaded.recordEngagementCounts(3, 1, 0, 0);
        reloaded.updateFinancialEstimates(new BigDecimal("50.00"), new BigDecimal("80.00"));
        campaignMetricsRepository.saveAndFlush(reloaded);
        entityManager.clear();

        CampaignMetrics updated =
                campaignMetricsRepository.findByCampaignId(campaign.getId()).orElseThrow();
        assertThat(updated.getId()).isEqualTo(metricsId);
        assertThat(updated.getAudienceSize()).isEqualTo(10);
        assertThat(updated.getEligibleCount()).isEqualTo(8);
        assertThat(updated.getSentCount()).isEqualTo(8);
        assertThat(updated.getOpenedCount()).isEqualTo(3);
        assertThat(updated.getEstimatedRoi()).isEqualByComparingTo("0.60");
    }

    @Test
    void enforcesOneMetricsRowPerCampaign() {
        User owner = persistUser("metrics-unique-owner");
        Campaign campaign = persistCampaign("Metrics unique campaign", owner);

        campaignMetricsRepository.saveAndFlush(CampaignMetrics.forCampaign(campaign));
        entityManager.clear();

        CampaignMetrics duplicate = CampaignMetrics.forCampaign(campaign);
        duplicate.recordLaunchCounts(1, 0, 1);

        assertThatThrownBy(() -> campaignMetricsRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-metrics-repository-integration.test",
                        "{noop}password",
                        "Campaign Metrics Repository Integration User");
        return entityManager.persistAndFlush(user);
    }

    private Campaign persistCampaign(String name, User owner) {
        Campaign campaign =
                Campaign.create(
                        name, "Objective for " + name, owner, null, CampaignChannel.EMAIL);
        return entityManager.persistAndFlush(campaign);
    }
}
