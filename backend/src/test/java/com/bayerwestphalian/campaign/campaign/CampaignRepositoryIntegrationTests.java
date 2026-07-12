package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** KB item 213: CampaignRepository query methods against PostgreSQL (status, owner, active). */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CampaignRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private CampaignRepository campaignRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void findsCampaignsByStatusOrderedByName() {
        User owner = persistUser("status-owner");
        Campaign betaDraft = persistCampaign("Beta draft", owner, CampaignStatus.DRAFT);
        Campaign alphaDraft = persistCampaign("Alpha draft", owner, CampaignStatus.DRAFT);
        Campaign submitted = persistCampaign("Submitted campaign", owner, CampaignStatus.SUBMITTED);

        List<Campaign> drafts = campaignRepository.findByStatus(CampaignStatus.DRAFT);

        assertThat(drafts)
                .extracting(Campaign::getId)
                .containsExactly(alphaDraft.getId(), betaDraft.getId())
                .doesNotContain(submitted.getId());
    }

    @Test
    void findsCampaignsByOwnerUserIdOrderedByName() {
        User owner = persistUser("owner-a");
        User other = persistUser("owner-b");
        Campaign betaOwned = persistCampaign("Beta owned", owner, CampaignStatus.DRAFT);
        Campaign alphaOwned = persistCampaign("Alpha owned", owner, CampaignStatus.ACTIVE);
        persistCampaign("Other owner campaign", other, CampaignStatus.DRAFT);

        List<Campaign> owned = campaignRepository.findByOwnerUserId(owner.getId());

        assertThat(owned)
                .extracting(Campaign::getId)
                .containsExactly(alphaOwned.getId(), betaOwned.getId());
        assertThat(owned).allMatch(campaign -> campaign.isOwnedBy(owner.getId()));
    }

    @Test
    void findsActiveCampaignsOnly() {
        User owner = persistUser("active-owner");
        Campaign activeAlpha = persistCampaign("Alpha active", owner, CampaignStatus.ACTIVE);
        Campaign activeBeta = persistCampaign("Beta active", owner, CampaignStatus.ACTIVE);
        persistCampaign("Paused campaign", owner, CampaignStatus.PAUSED);
        persistCampaign("Draft campaign", owner, CampaignStatus.DRAFT);
        persistCampaign("Approved not launched", owner, CampaignStatus.APPROVED);

        List<Campaign> active = campaignRepository.findActiveCampaigns();

        assertThat(active)
                .extracting(Campaign::getId)
                .containsExactly(activeAlpha.getId(), activeBeta.getId());
        assertThat(active).allMatch(Campaign::isActive);
    }

    @Test
    void findsSubmittedCampaignsForComplianceQueue() {
        User owner = persistUser("submit-owner");
        Campaign first = persistCampaign("Alpha submitted", owner, CampaignStatus.SUBMITTED);
        Campaign second = persistCampaign("Beta submitted", owner, CampaignStatus.SUBMITTED);
        persistCampaign("Draft", owner, CampaignStatus.DRAFT);
        persistCampaign("Approved", owner, CampaignStatus.APPROVED);

        assertThat(campaignRepository.findSubmittedCampaigns())
                .extracting(Campaign::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void findsCampaignsBySegment() {
        User owner = persistUser("segment-owner");
        Segment segment =
                entityManager.persistAndFlush(
                        Segment.create("Target segment", null, owner, SegmentVisibility.TEAM));
        Segment otherSegment =
                entityManager.persistAndFlush(
                        Segment.create("Other segment", null, owner, SegmentVisibility.PRIVATE));

        Campaign linked =
                persistCampaignWithSegment("Linked campaign", owner, segment, CampaignStatus.DRAFT);
        persistCampaignWithSegment(
                "Other segment campaign", owner, otherSegment, CampaignStatus.DRAFT);
        persistCampaign("No segment campaign", owner, CampaignStatus.DRAFT);

        assertThat(campaignRepository.findBySegment_IdOrderByNameAsc(segment.getId()))
                .extracting(Campaign::getId)
                .containsExactly(linked.getId());
    }

    @Test
    void findsCampaignsByMultipleStatuses() {
        User owner = persistUser("multi-status-owner");
        Campaign approved = persistCampaign("Approved", owner, CampaignStatus.APPROVED);
        Campaign active = persistCampaign("Active", owner, CampaignStatus.ACTIVE);
        persistCampaign("Draft", owner, CampaignStatus.DRAFT);

        List<Campaign> launchableOrRunning =
                campaignRepository.findByStatusInOrderByNameAsc(
                        List.of(CampaignStatus.APPROVED, CampaignStatus.ACTIVE));

        assertThat(launchableOrRunning)
                .extracting(Campaign::getId)
                .containsExactlyInAnyOrder(approved.getId(), active.getId());
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-repository-integration.test",
                        "{noop}password",
                        "Campaign Repository Integration User");
        return entityManager.persistAndFlush(user);
    }

    private Campaign persistCampaign(String name, User owner, CampaignStatus status) {
        return persistCampaignWithSegment(name, owner, null, status);
    }

    private Campaign persistCampaignWithSegment(
            String name, User owner, Segment segment, CampaignStatus status) {
        Campaign campaign =
                Campaign.create(
                        name, "Objective for " + name, owner, segment, CampaignChannel.EMAIL);
        campaign = entityManager.persistAndFlush(campaign);
        applyStatus(campaign, status);
        return entityManager.persistAndFlush(campaign);
    }

    /**
     * Moves a freshly created DRAFT campaign into the requested status using domain transitions
     * where possible (so integration data matches real lifecycle rules).
     */
    private void applyStatus(Campaign campaign, CampaignStatus target) {
        if (target == CampaignStatus.DRAFT) {
            return;
        }
        User compliance =
                entityManager.persistAndFlush(
                        User.create(
                                "compliance-"
                                        + campaign.getId()
                                        + "@campaign-repository-integration.test",
                                "{noop}password",
                                "Compliance Officer"));

        switch (target) {
            case SUBMITTED -> campaign.submit();
            case APPROVED -> {
                campaign.submit();
                campaign.approve(compliance);
            }
            case REJECTED -> {
                campaign.submit();
                campaign.reject("Rejected in test setup");
            }
            case ACTIVE -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
            }
            case PAUSED -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                campaign.pause();
            }
            case COMPLETED -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                campaign.complete();
            }
            case ARCHIVED -> {
                campaign.submit();
                campaign.approve(compliance);
                campaign.launch();
                campaign.complete();
                campaign.archive();
            }
            default -> throw new IllegalArgumentException("Unsupported status " + target);
        }
    }
}
