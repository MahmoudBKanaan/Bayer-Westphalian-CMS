package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

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
class SegmentCriteriaRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_segment_criteria_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private SegmentCriteriaRepository segmentCriteriaRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void segmentCriteriaRepositoryFindsCriteriaBySegmentOrderedByFieldName() {
        Segment targetSegment = persistSegment("Target audience");
        Segment otherSegment = persistSegment("Other audience");
        SegmentCriteria customerType =
                targetSegment.addCriteria("customer_type", SegmentOperator.EQUALS, "CUSTOMER");
        SegmentCriteria city =
                targetSegment.addCriteria("city", SegmentOperator.CONTAINS, "Berlin");
        SegmentCriteria age =
                targetSegment.addCriteria(
                        "age",
                        SegmentOperator.BETWEEN,
                        "30..65",
                        "retirement-readiness",
                        SegmentJoinOperator.OR);
        otherSegment.addCriteria("status", SegmentOperator.EQUALS, "ACTIVE");
        entityManager.persistAndFlush(targetSegment);
        entityManager.persistAndFlush(otherSegment);

        assertThat(segmentCriteriaRepository.findBySegmentId(targetSegment.getId()))
                .extracting(SegmentCriteria::getId)
                .containsExactly(age.getId(), city.getId(), customerType.getId());
    }

    @Test
    void segmentCriteriaRepositoryReturnsEmptyListWhenSegmentHasNoCriteria() {
        Segment segment = persistSegment("Empty criteria audience");

        assertThat(segmentCriteriaRepository.findBySegmentId(segment.getId())).isEmpty();
    }

    @Test
    void segmentCriteriaRepositoryDoesNotReturnCriteriaFromOtherSegments() {
        Segment firstSegment = persistSegment("First audience");
        Segment secondSegment = persistSegment("Second audience");
        SegmentCriteria firstCriterion =
                firstSegment.addCriteria("policy_status", SegmentOperator.IN, "ACTIVE,LAPSED");
        secondSegment.addCriteria("country", SegmentOperator.EQUALS, "DE");
        entityManager.persistAndFlush(firstSegment);
        entityManager.persistAndFlush(secondSegment);

        assertThat(segmentCriteriaRepository.findBySegmentId(firstSegment.getId()))
                .extracting(SegmentCriteria::getId)
                .containsExactly(firstCriterion.getId());
    }

    private Segment persistSegment(String name) {
        Segment segment = Segment.create(name, null, null, SegmentVisibility.TEAM);
        return entityManager.persistAndFlush(segment);
    }
}