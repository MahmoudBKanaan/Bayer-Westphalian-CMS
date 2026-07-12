package com.bayerwestphalian.campaign.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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

/**
 * KB item 470: AiRecommendationRepository persistence against PostgreSQL — save recommendations and
 * query by target entity / recommendation type (newest first).
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class AiRecommendationRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_ai_recommendation_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private AiRecommendationRepository aiRecommendationRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void savesAndFindsRecommendationsByTargetEntity() {
        UUID customerA = UUID.fromString("20000000-0000-0000-0000-000000000470");
        UUID customerB = UUID.fromString("20000000-0000-0000-0000-000000000471");

        AiRecommendation forA =
                AiRecommendation.create(
                        AiRecommendationType.PRODUCT,
                        "customer",
                        customerA,
                        "Owns life policy",
                        "Recommend investment fund",
                        "Profile matches investment segment",
                        new BigDecimal("87.50"));
        AiRecommendation alsoForA =
                AiRecommendation.create(
                        AiRecommendationType.RISK,
                        "customer",
                        customerA,
                        "Overdue payment reminders",
                        "Review outreach timing",
                        "Risk protects compliant communication",
                        new BigDecimal("62.25"));
        AiRecommendation forB =
                AiRecommendation.create(
                        AiRecommendationType.SEGMENT,
                        "customer",
                        customerB,
                        "Munich location",
                        "Suggest Munich segment",
                        "Location density supports segment reuse");

        aiRecommendationRepository.saveAndFlush(forA);
        aiRecommendationRepository.saveAndFlush(alsoForA);
        aiRecommendationRepository.saveAndFlush(forB);
        entityManager.clear();

        List<AiRecommendation> byTarget =
                aiRecommendationRepository.findByTargetEntity("customer", customerA);
        List<AiRecommendation> byPropertyPath =
                aiRecommendationRepository
                        .findByTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
                                "customer", customerA);

        assertThat(byTarget).hasSize(2);
        assertThat(byPropertyPath).hasSize(2);
        assertThat(byTarget)
                .extracting(AiRecommendation::getRecommendationType)
                .containsExactlyInAnyOrder(
                        AiRecommendationType.PRODUCT, AiRecommendationType.RISK);
        assertThat(byTarget)
                .allSatisfy(row -> assertThat(row.getTargetEntityId()).isEqualTo(customerA));
        assertThat(aiRecommendationRepository.findByTargetEntity("customer", customerB))
                .hasSize(1)
                .first()
                .extracting(AiRecommendation::getRecommendationType)
                .isEqualTo(AiRecommendationType.SEGMENT);
    }

    @Test
    void findsRecommendationsByRecommendationTypeNewestFirst() throws InterruptedException {
        UUID target = UUID.fromString("20000000-0000-0000-0000-000000000472");

        AiRecommendation olderProduct =
                AiRecommendation.create(
                        AiRecommendationType.PRODUCT,
                        "customer",
                        target,
                        "Older product input",
                        "Older product recommendation",
                        "First product suggestion");
        aiRecommendationRepository.saveAndFlush(olderProduct);
        // Ensure distinct created_at ordering under real clock resolution.
        Thread.sleep(15);

        AiRecommendation newerProduct =
                AiRecommendation.create(
                        AiRecommendationType.PRODUCT,
                        "customer",
                        target,
                        "Newer product input",
                        "Newer product recommendation",
                        "Second product suggestion");
        AiRecommendation risk =
                AiRecommendation.create(
                        AiRecommendationType.RISK,
                        "customer",
                        target,
                        "Risk input",
                        "Risk recommendation",
                        "Risk explanation");
        aiRecommendationRepository.saveAndFlush(newerProduct);
        aiRecommendationRepository.saveAndFlush(risk);
        entityManager.clear();

        List<AiRecommendation> products =
                aiRecommendationRepository.findByRecommendationType(AiRecommendationType.PRODUCT);
        List<AiRecommendation> risks =
                aiRecommendationRepository.findByRecommendationType(AiRecommendationType.RISK);

        assertThat(products).hasSize(2);
        assertThat(products.get(0).getRecommendation())
                .isEqualTo("Newer product recommendation");
        assertThat(products.get(1).getRecommendation())
                .isEqualTo("Older product recommendation");
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0).getRecommendationType()).isEqualTo(AiRecommendationType.RISK);
    }

    @Test
    void findsApprovedRecommendationsByApprover() {
        User approver = persistUser("ai-repo-approver");
        UUID campaignId = UUID.fromString("50000000-0000-0000-0000-000000000470");

        AiRecommendation copy =
                AiRecommendation.create(
                        AiRecommendationType.COPY,
                        "campaign",
                        campaignId,
                        "Objective: life cross-sell",
                        "Subject: Protect what matters",
                        "Suggested copy for human review (COMP-005)");
        copy.approve(approver);

        AiRecommendation unapproved =
                AiRecommendation.create(
                        AiRecommendationType.DUPLICATE_WARNING,
                        "customer",
                        UUID.fromString("20000000-0000-0000-0000-000000000473"),
                        "3 contacts this month",
                        "Duplicate contact risk",
                        "Monthly contact limit approaching");

        aiRecommendationRepository.saveAndFlush(copy);
        aiRecommendationRepository.saveAndFlush(unapproved);
        entityManager.clear();

        List<AiRecommendation> approved =
                aiRecommendationRepository.findByApprovedBy_IdOrderByCreatedAtDesc(approver.getId());

        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).isApproved()).isTrue();
        assertThat(approved.get(0).getRecommendationType()).isEqualTo(AiRecommendationType.COPY);
        assertThat(approved.get(0).getApprovedByUserId()).isEqualTo(approver.getId());
        assertThat(approved.get(0).getExplanation()).contains("COMP-005");
    }

    @Test
    void persistsConfidenceScoreAndExplanationForHumanReview() {
        UUID customerId = UUID.fromString("20000000-0000-0000-0000-000000000474");
        AiRecommendation recommendation =
                AiRecommendation.create(
                        AiRecommendationType.DUPLICATE_WARNING,
                        "customer",
                        customerId,
                        "Same campaign contacted twice",
                        "Warn before additional send",
                        "Duplicate-contact rule at risk",
                        new BigDecimal("91.00"));

        AiRecommendation saved = aiRecommendationRepository.saveAndFlush(recommendation);
        entityManager.clear();

        AiRecommendation reloaded =
                aiRecommendationRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getConfidenceScore()).isEqualByComparingTo("91.00");
        assertThat(reloaded.getExplanation()).isEqualTo("Duplicate-contact rule at risk");
        assertThat(reloaded.getInputSummary()).isEqualTo("Same campaign contacted twice");
        assertThat(reloaded.isApproved()).isFalse();
    }

    private User persistUser(String key) {
        User user =
                User.create(
                        key + "@bayer-westphalian.test",
                        "$2a$10$examplehashforairepositorytestsxx",
                        "AI Repo " + key);
        return entityManager.persistFlushFind(user);
    }
}
