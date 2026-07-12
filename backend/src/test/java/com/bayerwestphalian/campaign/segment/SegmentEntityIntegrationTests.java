package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.EntityManager;
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
class SegmentEntityIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_segment_entity_tests")
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
    void persistsSegmentWithOwnerVisibilityAndAuditingTimestamps() {
        User owner = persistUser("segment-owner");
        Segment segment =
                Segment.create(
                        "Expiring homeowner policies",
                        "Customers with homeowner insurance expiring within six months",
                        owner,
                        SegmentVisibility.TEAM);

        persistAndFlush(segment);
        entityManager.clear();

        Segment reloaded = entityManager.find(Segment.class, segment.getId());

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getName()).isEqualTo("Expiring homeowner policies");
        assertThat(reloaded.getDescription())
                .isEqualTo("Customers with homeowner insurance expiring within six months");
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getVisibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.isOwnedBy(owner.getId())).isTrue();
        assertThat(reloaded.isGlobal()).isFalse();
    }

    @Test
    void persistsGlobalSegmentWithoutOwnerUsingKbDefaults() {
        Segment segment =
                Segment.create("Shared baseline audience", null, null, SegmentVisibility.GLOBAL);

        persistAndFlush(segment);
        entityManager.clear();

        Segment reloaded = entityManager.find(Segment.class, segment.getId());

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getOwner()).isNull();
        assertThat(reloaded.getVisibility()).isEqualTo(SegmentVisibility.GLOBAL);
        assertThat(reloaded.isGlobal()).isTrue();
    }

    @Test
    void persistsPrivateSegmentWhenVisibilityIsOmitted() {
        Segment segment = Segment.create("Private draft audience", "Work in progress", null, null);

        persistAndFlush(segment);
        entityManager.clear();

        Segment reloaded = entityManager.find(Segment.class, segment.getId());

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getVisibility()).isEqualTo(SegmentVisibility.PRIVATE);
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@segment-entity-integration.test",
                        "{noop}password",
                        "Segment Entity Integration User");
        persistAndFlush(user);
        return user;
    }

    private <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
