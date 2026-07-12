package com.bayerwestphalian.campaign.followup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FollowUpTaskTests {

    @Test
    void initializesWithCorrectValuesAndDefaults() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        User assignedTo = Mockito.mock(User.class);
        LocalDate dueDate = LocalDate.now().plusDays(7);

        FollowUpTask task =
                new FollowUpTask(customer, assignedTo, "Follow up on product interest", dueDate);

        assertThat(task.getCustomer()).isSameAs(customer);
        assertThat(task.getAssignedTo()).isSameAs(assignedTo);
        assertThat(task.getTitle()).isEqualTo("Follow up on product interest");
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.OPEN);
        assertThat(task.getPriority()).isEqualTo(FollowUpTaskPriority.MEDIUM);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void throwsExceptionWhenCustomerOrTitleIsNull() {
        User assignedTo = Mockito.mock(User.class);
        LocalDate dueDate = LocalDate.now().plusDays(7);

        assertThatThrownBy(() -> new FollowUpTask(null, assignedTo, "Title", dueDate))
                .isInstanceOf(NullPointerException.class);

        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        assertThatThrownBy(() -> new FollowUpTask(customer, assignedTo, null, dueDate))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void followUpTaskCanBeAssignedAtEntityLevel() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());
        User firstAssignee = Mockito.mock(User.class);
        User secondAssignee = Mockito.mock(User.class);

        task.assignTo(firstAssignee);
        assertThat(task.getAssignedTo()).isSameAs(firstAssignee);

        task.assignTo(secondAssignee);
        assertThat(task.getAssignedTo()).isSameAs(secondAssignee);
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.OPEN);
    }

    @Test
    void startChangesStatusToInProgress() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());

        task.start();

        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.IN_PROGRESS);
    }

    @Test
    void followUpTaskCanBeCompletedAtEntityLevel() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());

        task.complete();

        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();

        task.start();
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.IN_PROGRESS);
        assertThat(task.getCompletedAt()).isNull();

        task.complete();
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void cancelChangesStatusToCancelled() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());

        task.cancel();

        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.CANCELLED);
    }

    @Test
    void updateStatusAppliesAllKbStatusTransitions() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());

        task.updateStatus(FollowUpTaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.IN_PROGRESS);
        assertThat(task.getCompletedAt()).isNull();

        task.updateStatus(FollowUpTaskStatus.COMPLETED);
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();

        task.updateStatus(FollowUpTaskStatus.OPEN);
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.OPEN);
        assertThat(task.getCompletedAt()).isNull();

        task.updateStatus(FollowUpTaskStatus.CANCELLED);
        assertThat(task.getStatus()).isEqualTo(FollowUpTaskStatus.CANCELLED);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void updatePriorityUpdatesPriorityAndRejectsNull() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        FollowUpTask task = new FollowUpTask(customer, null, "Follow up", LocalDate.now());

        task.updatePriority(FollowUpTaskPriority.HIGH);
        assertThat(task.getPriority()).isEqualTo(FollowUpTaskPriority.HIGH);

        assertThatThrownBy(() -> task.updatePriority(null))
                .isInstanceOf(NullPointerException.class);
    }
}
