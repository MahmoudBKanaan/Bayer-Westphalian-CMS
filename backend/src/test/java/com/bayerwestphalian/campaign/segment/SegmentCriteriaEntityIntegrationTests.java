package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class SegmentCriteriaEntityIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_segment_criteria_entity_tests")
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
    void persistsSegmentCriteriaWithKbDefaultsAndGrouping() {
        Segment segment = persistSegment("Segment criteria audience");
        SegmentCriteria defaultCriterion =
                segment.addCriteria("customer_type", SegmentOperator.EQUALS, "CUSTOMER");
        SegmentCriteria groupedCriterion =
                segment.addCriteria(
                        "age",
                        SegmentOperator.BETWEEN,
                        "30..65",
                        "retirement-readiness",
                        SegmentJoinOperator.OR);

        persistAndFlush(segment);
        entityManager.clear();

        SegmentCriteria reloadedDefault =
                entityManager.find(SegmentCriteria.class, defaultCriterion.getId());
        SegmentCriteria reloadedGrouped =
                entityManager.find(SegmentCriteria.class, groupedCriterion.getId());

        assertThat(reloadedDefault).isNotNull();
        assertThat(reloadedDefault.getSegment().getId()).isEqualTo(segment.getId());
        assertThat(reloadedDefault.getFieldName()).isEqualTo("customer_type");
        assertThat(reloadedDefault.getOperator()).isEqualTo(SegmentOperator.EQUALS);
        assertThat(reloadedDefault.getValue()).isEqualTo("CUSTOMER");
        assertThat(reloadedDefault.getJoinOperator()).isEqualTo(SegmentJoinOperator.AND);
        assertThat(reloadedDefault.getLogicalGroup()).isNull();

        assertThat(reloadedGrouped).isNotNull();
        assertThat(reloadedGrouped.getFieldName()).isEqualTo("age");
        assertThat(reloadedGrouped.getOperator()).isEqualTo(SegmentOperator.BETWEEN);
        assertThat(reloadedGrouped.getValue()).isEqualTo("30..65");
        assertThat(reloadedGrouped.getLogicalGroup()).isEqualTo("retirement-readiness");
        assertThat(reloadedGrouped.getJoinOperator()).isEqualTo(SegmentJoinOperator.OR);
    }

    @Test
    void cascadesCriteriaDeletionWhenSegmentIsRemoved() {
        Segment segment = persistSegment("Cascade criteria segment");
        SegmentCriteria criterion =
                segment.addCriteria("policy_status", SegmentOperator.IN, "ACTIVE,LAPSED");
        persistAndFlush(segment);
        UUID criterionId = criterion.getId();

        entityManager.remove(segment);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(SegmentCriteria.class, criterionId)).isNull();
    }

    @Test
    void segmentAggregateManagesCriteriaCollection() {
        Segment segment = persistSegment("Managed criteria segment");
        SegmentCriteria first = segment.addCriteria("city", SegmentOperator.EQUALS, "Berlin");
        SegmentCriteria second =
                SegmentCriteria.create(
                        segment,
                        "status",
                        SegmentOperator.NOT_EQUALS,
                        "INACTIVE",
                        "engagement",
                        SegmentJoinOperator.OR);
        segment.addCriteria(second);

        persistAndFlush(segment);
        entityManager.clear();

        Segment reloaded = entityManager.find(Segment.class, segment.getId());

        assertThat(reloaded.getCriteria()).hasSize(2);
        assertThat(reloaded.getCriteria())
                .extracting(SegmentCriteria::getFieldName)
                .containsExactly("city", "status");

        SegmentCriteria removed =
                reloaded.getCriteria().stream()
                        .filter(item -> "city".equals(item.getFieldName()))
                        .findFirst()
                        .orElseThrow();
        reloaded.removeCriteria(removed);
        persistAndFlush(reloaded);
        entityManager.clear();

        Segment updated = entityManager.find(Segment.class, segment.getId());
        assertThat(updated.getCriteria()).hasSize(1);
        assertThat(updated.getCriteria().getFirst().getFieldName()).isEqualTo("status");
    }

    private Segment persistSegment(String name) {
        Segment segment = Segment.create(name, null, null, SegmentVisibility.TEAM);
        persistAndFlush(segment);
        return segment;
    }

    private <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
