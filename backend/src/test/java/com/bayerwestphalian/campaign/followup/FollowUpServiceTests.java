package com.bayerwestphalian.campaign.followup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.prepost.PreAuthorize;

class FollowUpServiceTests {

    private FollowUpRepository followUpRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private CampaignRepository campaignRepository;
    private FollowUpService followUpService;

    private Customer customer;
    private User agent;
    private Campaign campaign;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID AGENT_ID = UUID.randomUUID();
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final UUID TASK_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        followUpRepository = Mockito.mock(FollowUpRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        campaignRepository = Mockito.mock(CampaignRepository.class);

        followUpService =
                new FollowUpService(
                        followUpRepository, customerRepository, userRepository, campaignRepository);

        customer = Customer.create(CustomerType.PROSPECT, "Ada", "Lovelace");
        agent = User.create("agent@test.example", "{noop}password", "Sales Agent");
        campaign = Mockito.mock(Campaign.class);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
    }

    @Test
    void createTaskSavesAndReturnsTask() {
        CreateFollowUpTaskCommand command =
                new CreateFollowUpTaskCommand(
                        CUSTOMER_ID,
                        CAMPAIGN_ID,
                        AGENT_ID,
                        "Call Ada",
                        "Notes",
                        LocalDate.now(),
                        FollowUpTaskPriority.HIGH);

        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask created = followUpService.createTask(command);

        assertThat(created.getCustomer()).isSameAs(customer);
        assertThat(created.getCampaign()).isSameAs(campaign);
        assertThat(created.getAssignedTo()).isSameAs(agent);
        assertThat(created.getTitle()).isEqualTo("Call Ada");
        assertThat(created.getDescription()).isEqualTo("Notes");
        assertThat(created.getDueDate()).isEqualTo(command.dueDate());
        assertThat(created.getStatus()).isEqualTo(FollowUpTaskStatus.OPEN);
        assertThat(created.getCompletedAt()).isNull();
        assertThat(created.getPriority()).isEqualTo(FollowUpTaskPriority.HIGH);

        verify(followUpRepository).save(any(FollowUpTask.class));
    }

    @Test
    void createTaskThrowsExceptionIfCustomerNotFound() {
        UUID unknownId = UUID.randomUUID();
        CreateFollowUpTaskCommand command =
                new CreateFollowUpTaskCommand(
                        unknownId, null, null, "Call Ada", null, null, FollowUpTaskPriority.MEDIUM);

        when(customerRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.createTask(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    void followUpTaskCanBeAssigned() {
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated = followUpService.assignTask(TASK_ID, AGENT_ID);

        assertThat(updated.getAssignedTo()).isSameAs(agent);
        assertThat(updated.getAssignedTo().isActive()).isTrue();
        assertThat(updated.getStatus()).isEqualTo(FollowUpTaskStatus.OPEN);
        verify(followUpRepository).save(task);
    }

    @Test
    void assignTaskCanReassignToAnotherActiveUser() {
        User previousAssignee =
                User.create("previous@test.example", "{noop}password", "Previous Agent");
        User newAssignee = User.create("new@test.example", "{noop}password", "New Agent");
        UUID newAssigneeId = UUID.randomUUID();

        FollowUpTask task =
                new FollowUpTask(customer, previousAssignee, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(userRepository.findById(newAssigneeId)).thenReturn(Optional.of(newAssignee));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated = followUpService.assignTask(TASK_ID, newAssigneeId);

        assertThat(updated.getAssignedTo()).isSameAs(newAssignee);
        verify(followUpRepository).save(task);
    }

    @Test
    void assignTaskThrowsWhenTaskNotFound() {
        UUID unknownTaskId = UUID.randomUUID();
        when(followUpRepository.findById(unknownTaskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.assignTask(unknownTaskId, AGENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FollowUpTask");
    }

    @Test
    void assignTaskThrowsWhenAssigneeNotFound() {
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());
        UUID unknownUserId = UUID.randomUUID();
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.assignTask(TASK_ID, unknownUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void assignTaskRejectsNullTaskIdOrAssignee() {
        assertThatThrownBy(() -> followUpService.assignTask(null, AGENT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Follow-up task assignment failed");

        assertThatThrownBy(() -> followUpService.assignTask(TASK_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Follow-up task assignment failed");
    }

    @Test
    void assignTaskRejectsInactiveAssignee() {
        User disabledAgent =
                User.create("disabled@test.example", "{noop}password", "Disabled Agent");
        disabledAgent.disable();
        UUID disabledAgentId = UUID.randomUUID();

        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(userRepository.findById(disabledAgentId)).thenReturn(Optional.of(disabledAgent));

        assertThatThrownBy(() -> followUpService.assignTask(TASK_ID, disabledAgentId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Follow-up task assignment failed");
    }

    @Test
    void updateTaskUpdatesDescriptionAndPriority() {
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated =
                followUpService.updateTask(TASK_ID, "New description", FollowUpTaskPriority.LOW);

        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getPriority()).isEqualTo(FollowUpTaskPriority.LOW);
        verify(followUpRepository).save(task);
    }

    @Test
    void followUpTaskCanBeCompleted() {
        FollowUpTask task = new FollowUpTask(customer, agent, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated = followUpService.completeTask(TASK_ID);

        assertThat(updated.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getAssignedTo()).isSameAs(agent);
        verify(followUpRepository).save(task);
    }

    @Test
    void completeTaskWorksFromInProgressStatus() {
        FollowUpTask task = new FollowUpTask(customer, agent, "Follow up", LocalDate.now());
        task.start();
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated = followUpService.completeTask(TASK_ID);

        assertThat(updated.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        verify(followUpRepository).save(task);
    }

    @Test
    void completeTaskThrowsWhenTaskNotFound() {
        UUID unknownTaskId = UUID.randomUUID();
        when(followUpRepository.findById(unknownTaskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.completeTask(unknownTaskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("FollowUpTask");
    }

    @Test
    void completeTaskRejectsNullTaskId() {
        assertThatThrownBy(() -> followUpService.completeTask(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Follow-up task completion failed");
    }

    @Test
    void updateTaskStatusChangesStatus() {
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated =
                followUpService.updateTaskStatus(TASK_ID, FollowUpTaskStatus.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(FollowUpTaskStatus.IN_PROGRESS);
        assertThat(updated.getCompletedAt()).isNull();
        verify(followUpRepository).save(task);
    }

    @Test
    void updateTaskStatusToCompletedSetsCompletedAt() {
        FollowUpTask task = new FollowUpTask(customer, agent, "Follow up", LocalDate.now());
        when(followUpRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(followUpRepository.save(any(FollowUpTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FollowUpTask updated =
                followUpService.updateTaskStatus(TASK_ID, FollowUpTaskStatus.COMPLETED);

        assertThat(updated.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(updated.getCompletedAt()).isNotNull();
        verify(followUpRepository).save(task);
    }

    @Test
    void listAssignedTasksDelegatesToRepository() {
        FollowUpTask task = new FollowUpTask(customer, agent, "Follow up", LocalDate.now());
        when(followUpRepository.findByAssignedTo(AGENT_ID)).thenReturn(List.of(task));

        List<FollowUpTask> tasks = followUpService.listAssignedTasks(AGENT_ID);

        assertThat(tasks).containsExactly(task);
        verify(followUpRepository).findByAssignedTo(AGENT_ID);
    }

    @Test
    void searchTasksDelegatesCriteriaToRepository() {
        FollowUpTask task = new FollowUpTask(customer, agent, "Follow up", LocalDate.now());
        FollowUpTaskSearchCriteria criteria =
                new FollowUpTaskSearchCriteria(
                        CUSTOMER_ID,
                        AGENT_ID,
                        FollowUpTaskPriority.HIGH,
                        FollowUpTaskStatus.OPEN,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30));
        doReturn(List.of(task)).when(followUpRepository).search(criteria);

        List<FollowUpTask> tasks = followUpService.searchTasks(criteria);

        assertThat(tasks).containsExactly(task);
        verify(followUpRepository).search(criteria);
    }

    @Test
    void serviceMethodsDeclareKbAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "createTask",
                new Class<?>[] {CreateFollowUpTaskCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'CAMPAIGN_MANAGER')");

        assertPreAuthorizeWithExpression(
                "assignTask",
                new Class<?>[] {UUID.class, UUID.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')");

        assertPreAuthorizeWithExpression(
                "updateTask",
                new Class<?>[] {UUID.class, String.class, FollowUpTaskPriority.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");

        assertPreAuthorizeWithExpression(
                "completeTask",
                new Class<?>[] {UUID.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");

        assertPreAuthorizeWithExpression(
                "updateTaskStatus",
                new Class<?>[] {UUID.class, FollowUpTaskStatus.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')");

        assertPreAuthorizeWithExpression(
                "listAssignedTasks",
                new Class<?>[] {UUID.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')");

        assertPreAuthorizeWithExpression(
                "searchTasks",
                new Class<?>[] {FollowUpTaskSearchCriteria.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')");
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = FollowUpService.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedExpression);
    }
}
