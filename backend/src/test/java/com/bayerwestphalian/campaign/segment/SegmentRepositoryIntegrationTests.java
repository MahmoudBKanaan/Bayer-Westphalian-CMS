package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
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

@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class SegmentRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_segment_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private SegmentRepository segmentRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void segmentRepositoryFindsSegmentsByOwnerOrderedByName() {
        User owner = persistUser("segment-owner");
        User otherOwner = persistUser("other-owner");
        Segment betaOwned = persistSegment("Beta owned audience", owner, SegmentVisibility.PRIVATE);
        Segment alphaOwned = persistSegment("Alpha owned audience", owner, SegmentVisibility.TEAM);
        persistSegment("Other owner audience", otherOwner, SegmentVisibility.PRIVATE);
        persistSegment("Global baseline audience", null, SegmentVisibility.GLOBAL);

        assertThat(segmentRepository.findByOwner(owner.getId()))
                .extracting(Segment::getId)
                .containsExactly(alphaOwned.getId(), betaOwned.getId());
    }

    @Test
    void segmentRepositoryFindsGlobalSegmentsOrderedByName() {
        User owner = persistUser("global-owner");
        Segment alphaGlobal =
                persistSegment("Alpha global audience", null, SegmentVisibility.GLOBAL);
        Segment betaGlobal = persistSegment("Beta global audience", null, SegmentVisibility.GLOBAL);
        persistSegment("Private audience", owner, SegmentVisibility.PRIVATE);
        persistSegment("Team audience", owner, SegmentVisibility.TEAM);

        assertThat(segmentRepository.findGlobal())
                .extracting(Segment::getId)
                .containsExactly(alphaGlobal.getId(), betaGlobal.getId());
    }

    @Test
    void segmentRepositoryFindsSegmentsByVisibilityOrderedByName() {
        User owner = persistUser("visibility-owner");
        Segment alphaTeam = persistSegment("Alpha team audience", owner, SegmentVisibility.TEAM);
        Segment betaTeam = persistSegment("Beta team audience", owner, SegmentVisibility.TEAM);
        persistSegment("Private audience", owner, SegmentVisibility.PRIVATE);
        persistSegment("Global audience", null, SegmentVisibility.GLOBAL);

        assertThat(segmentRepository.findByVisibility(SegmentVisibility.TEAM))
                .extracting(Segment::getId)
                .containsExactly(alphaTeam.getId(), betaTeam.getId());
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@segment-repository-integration.test",
                        "{noop}password",
                        "Segment Repository Integration User");
        return entityManager.persistAndFlush(user);
    }

    private Segment persistSegment(String name, User owner, SegmentVisibility visibility) {
        Segment segment = Segment.create(name, null, owner, visibility);
        return entityManager.persistAndFlush(segment);
    }
}
