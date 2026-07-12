package com.bayerwestphalian.campaign.followup;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class FollowUpRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_follow_up_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private FollowUpRepository followUpRepository;

    private Customer customer;
    private User agent;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        entityManager.persistAndFlush(customer);

        agent =
                User.create(
                        "sales.agent@followup-repo-test.example", "{noop}password", "Sales Agent");
        entityManager.persistAndFlush(agent);
    }

    @Test
    void findsTasksByAssignedTo() {
        FollowUpTask task1 = new FollowUpTask(customer, agent, "First task", LocalDate.now());
        FollowUpTask task2 = new FollowUpTask(customer, null, "Unassigned task", LocalDate.now());
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();

        List<FollowUpTask> assigned = followUpRepository.findByAssignedTo(agent.getId());
        assertThat(assigned).containsExactly(task1);
    }

    @Test
    void followUpTaskCanBeAssignedAndPersisted() {
        FollowUpTask unassigned =
                new FollowUpTask(customer, null, "Needs owner", LocalDate.of(2026, 9, 15));
        entityManager.persistAndFlush(unassigned);
        entityManager.clear();

        FollowUpTask loaded = followUpRepository.findById(unassigned.getId()).orElseThrow();
        assertThat(loaded.getAssignedTo()).isNull();

        loaded.assignTo(agent);
        followUpRepository.saveAndFlush(loaded);
        entityManager.clear();

        FollowUpTask reloaded = followUpRepository.findById(unassigned.getId()).orElseThrow();
        assertThat(reloaded.getAssignedTo()).isNotNull();
        assertThat(reloaded.getAssignedTo().getId()).isEqualTo(agent.getId());
        assertThat(followUpRepository.findByAssignedTo(agent.getId()))
                .extracting(FollowUpTask::getId)
                .contains(unassigned.getId());
    }

    @Test
    void findsOpenTasks() {
        FollowUpTask task1 = new FollowUpTask(customer, agent, "First task", LocalDate.now());
        FollowUpTask task2 = new FollowUpTask(customer, agent, "Second task", LocalDate.now());
        task2.complete();
        FollowUpTask task3 = new FollowUpTask(customer, agent, "Third task", LocalDate.now());
        task3.cancel();

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        List<FollowUpTask> openTasks = followUpRepository.findOpenTasks();
        assertThat(openTasks).containsExactly(task1);
    }

    @Test
    void followUpTaskCanBeCompletedAndPersisted() {
        FollowUpTask openTask =
                new FollowUpTask(customer, agent, "Close the lead", LocalDate.of(2026, 9, 20));
        entityManager.persistAndFlush(openTask);
        entityManager.clear();

        FollowUpTask loaded = followUpRepository.findById(openTask.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(FollowUpTaskStatus.OPEN);
        assertThat(loaded.getCompletedAt()).isNull();

        loaded.complete();
        followUpRepository.saveAndFlush(loaded);
        entityManager.clear();

        FollowUpTask reloaded = followUpRepository.findById(openTask.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(reloaded.getCompletedAt()).isNotNull();
        assertThat(followUpRepository.findOpenTasks())
                .extracting(FollowUpTask::getId)
                .doesNotContain(openTask.getId());
        assertThat(
                        followUpRepository.search(
                                new FollowUpTaskSearchCriteria(
                                        null,
                                        agent.getId(),
                                        null,
                                        FollowUpTaskStatus.COMPLETED,
                                        null,
                                        null)))
                .extracting(FollowUpTask::getId)
                .contains(openTask.getId());
    }

    @Test
    void findsTasksByCustomerId() {
        Customer otherCustomer = Customer.create(CustomerType.PROSPECT, "Jane", "Smith");
        entityManager.persistAndFlush(otherCustomer);

        FollowUpTask task1 = new FollowUpTask(customer, agent, "First task", LocalDate.now());
        FollowUpTask task2 =
                new FollowUpTask(otherCustomer, agent, "Other customer task", LocalDate.now());
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();

        List<FollowUpTask> customerTasks = followUpRepository.findByCustomerId(customer.getId());
        assertThat(customerTasks).containsExactly(task1);
    }

    @Test
    void filtersTasksByAssigneePriorityStatusAndDueDateRange() {
        User otherAgent =
                User.create(
                        "other.agent@followup-repo-test.example", "{noop}password", "Other Agent");
        entityManager.persistAndFlush(otherAgent);

        FollowUpTask matching =
                new FollowUpTask(customer, agent, "Matching task", LocalDate.of(2026, 9, 15));
        matching.updatePriority(FollowUpTaskPriority.HIGH);

        FollowUpTask wrongAssignee =
                new FollowUpTask(customer, otherAgent, "Wrong assignee", LocalDate.of(2026, 9, 15));
        wrongAssignee.updatePriority(FollowUpTaskPriority.HIGH);

        FollowUpTask wrongPriority =
                new FollowUpTask(customer, agent, "Wrong priority", LocalDate.of(2026, 9, 15));
        wrongPriority.updatePriority(FollowUpTaskPriority.LOW);

        FollowUpTask wrongStatus =
                new FollowUpTask(customer, agent, "Wrong status", LocalDate.of(2026, 9, 15));
        wrongStatus.updatePriority(FollowUpTaskPriority.HIGH);
        wrongStatus.complete();

        FollowUpTask outsideDueDate =
                new FollowUpTask(customer, agent, "Outside due date", LocalDate.of(2026, 10, 1));
        outsideDueDate.updatePriority(FollowUpTaskPriority.HIGH);

        entityManager.persist(matching);
        entityManager.persist(wrongAssignee);
        entityManager.persist(wrongPriority);
        entityManager.persist(wrongStatus);
        entityManager.persist(outsideDueDate);
        entityManager.flush();

        List<FollowUpTask> filtered =
                followUpRepository.search(
                        new FollowUpTaskSearchCriteria(
                                customer.getId(),
                                agent.getId(),
                                FollowUpTaskPriority.HIGH,
                                FollowUpTaskStatus.OPEN,
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30)));

        assertThat(filtered).containsExactly(matching);
    }
}
