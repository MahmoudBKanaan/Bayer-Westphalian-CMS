package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * KB item 211: Campaign entity persists against the {@code campaigns} table and PostgreSQL enums.
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CampaignEntityIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_entity_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private EntityManager entityManager;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void persistsDraftCampaignWithOwnerSegmentChannelAndAuditingTimestamps() {
        User owner = persistUser("campaign-owner");
        Segment segment =
                Segment.create(
                        "Munich prospects", "Location audience", owner, SegmentVisibility.TEAM);
        persistAndFlush(segment);

        Campaign campaign =
                Campaign.create(
                        "Life renewal outreach",
                        "Promote life insurance renewals",
                        owner,
                        segment,
                        CampaignChannel.EMAIL);
        campaign.updateMessage("Renew your life cover", "Dear customer, ...");
        campaign.updateSchedule(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        persistAndFlush(campaign);
        entityManager.clear();

        Campaign reloaded = entityManager.find(Campaign.class, campaign.getId());

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getName()).isEqualTo("Life renewal outreach");
        assertThat(reloaded.getObjective()).isEqualTo("Promote life insurance renewals");
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getSegment().getId()).isEqualTo(segment.getId());
        assertThat(reloaded.getChannel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(reloaded.getMessageSubject()).isEqualTo("Renew your life cover");
        assertThat(reloaded.getMessageBody()).isEqualTo("Dear customer, ...");
        assertThat(reloaded.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(reloaded.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.isOwnedBy(owner.getId())).isTrue();
        assertThat(reloaded.canEdit()).isTrue();
        assertThat(reloaded.canLaunch()).isFalse();
    }

    @Test
    void persistsLifecycleTransitionsThroughSubmitApproveLaunch() {
        User owner = persistUser("campaign-lifecycle-owner");
        User compliance = persistUser("campaign-lifecycle-compliance");

        Campaign campaign =
                Campaign.create(
                        "Lifecycle campaign",
                        "Exercise status transitions",
                        owner,
                        null,
                        CampaignChannel.MIXED);
        persistAndFlush(campaign);

        campaign.submit();
        persistAndFlush(campaign);
        entityManager.clear();

        Campaign submitted = entityManager.find(Campaign.class, campaign.getId());
        assertThat(submitted.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);

        submitted.approve(compliance);
        persistAndFlush(submitted);
        entityManager.clear();

        Campaign approved = entityManager.find(Campaign.class, campaign.getId());
        assertThat(approved.getStatus()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.getApprovedBy().getId()).isEqualTo(compliance.getId());
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.canLaunch()).isTrue();

        approved.launch();
        persistAndFlush(approved);
        entityManager.clear();

        Campaign active = entityManager.find(Campaign.class, campaign.getId());
        assertThat(active.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(active.isActive()).isTrue();
    }

    @Test
    void persistsRejectionReasonAndAllowsResubmitFromRejected() {
        User owner = persistUser("campaign-reject-owner");
        Campaign campaign =
                Campaign.create(
                        "Rejected path",
                        "Will be rejected then fixed",
                        owner,
                        null,
                        CampaignChannel.PHONE);
        persistAndFlush(campaign);

        campaign.submit();
        campaign.reject("Message body incomplete");
        persistAndFlush(campaign);
        entityManager.clear();

        Campaign rejected = entityManager.find(Campaign.class, campaign.getId());
        assertThat(rejected.getStatus()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Message body incomplete");
        assertThat(rejected.canEdit()).isTrue();

        rejected.updateMessage(null, "Corrected phone script");
        rejected.submit();
        persistAndFlush(rejected);
        entityManager.clear();

        Campaign resubmitted = entityManager.find(Campaign.class, campaign.getId());
        assertThat(resubmitted.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(resubmitted.getRejectionReason()).isNull();
        assertThat(resubmitted.getMessageBody()).isEqualTo("Corrected phone script");
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-entity-integration.test",
                        "{noop}password",
                        "Campaign Entity Integration User");
        persistAndFlush(user);
        return user;
    }

    private <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
