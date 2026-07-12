package com.bayerwestphalian.campaign.followup;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class FollowUpDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID ASSIGNEE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void createRequestToCommandMapsAllProperties() {
        CreateFollowUpTaskRequest request =
                new CreateFollowUpTaskRequest(
                        CUSTOMER_ID,
                        CAMPAIGN_ID,
                        ASSIGNEE_ID,
                        "Call prospect back",
                        "Detailed description",
                        LocalDate.now().plusDays(2),
                        FollowUpTaskPriority.HIGH);

        CreateFollowUpTaskCommand command = request.toCommand();

        assertThat(command.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(command.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(command.assignedTo()).isEqualTo(ASSIGNEE_ID);
        assertThat(command.title()).isEqualTo("Call prospect back");
        assertThat(command.description()).isEqualTo("Detailed description");
        assertThat(command.dueDate()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(command.priority()).isEqualTo(FollowUpTaskPriority.HIGH);
    }

    @Test
    void createRequestDefaultsPriorityToMedium() {
        CreateFollowUpTaskRequest request =
                new CreateFollowUpTaskRequest(
                        CUSTOMER_ID, null, null, "Quick task", null, null, null);

        CreateFollowUpTaskCommand command = request.toCommand();
        assertThat(command.priority()).isEqualTo(FollowUpTaskPriority.MEDIUM);
    }

    @Test
    void createRequestValidationRules() {
        // Valid
        CreateFollowUpTaskRequest valid =
                new CreateFollowUpTaskRequest(
                        CUSTOMER_ID, null, null, "Follow up", null, null, null);
        assertThat(invalidFields(valid)).isEmpty();

        // Missing customerId
        CreateFollowUpTaskRequest missingCustomer =
                new CreateFollowUpTaskRequest(null, null, null, "Follow up", null, null, null);
        assertThat(invalidFields(missingCustomer)).containsExactly("customerId");

        // Missing title
        CreateFollowUpTaskRequest missingTitle =
                new CreateFollowUpTaskRequest(CUSTOMER_ID, null, null, " ", null, null, null);
        assertThat(invalidFields(missingTitle)).containsExactly("title");

        // Title too long
        CreateFollowUpTaskRequest longTitle =
                new CreateFollowUpTaskRequest(
                        CUSTOMER_ID, null, null, "a".repeat(256), null, null, null);
        assertThat(invalidFields(longTitle)).containsExactly("title");
    }

    @Test
    void updateStatusRequestRequiresStatus() {
        UpdateFollowUpStatusRequest valid =
                new UpdateFollowUpStatusRequest(FollowUpTaskStatus.IN_PROGRESS);
        UpdateFollowUpStatusRequest missingStatus = new UpdateFollowUpStatusRequest(null);

        assertThat(invalidFields(valid)).isEmpty();
        assertThat(invalidFields(missingStatus)).containsExactly("status");
    }

    @Test
    void assignRequestRequiresAssignedTo() {
        AssignFollowUpTaskRequest valid = new AssignFollowUpTaskRequest(ASSIGNEE_ID);
        AssignFollowUpTaskRequest missingAssignee = new AssignFollowUpTaskRequest(null);

        assertThat(invalidFields(valid)).isEmpty();
        assertThat(invalidFields(missingAssignee)).containsExactly("assignedTo");
    }

    @Test
    void viewFromMapsEntityProperties() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ada", "Lovelace");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);

        Campaign campaign = Mockito.mock(Campaign.class);
        Mockito.when(campaign.getId()).thenReturn(CAMPAIGN_ID);
        Mockito.when(campaign.getName()).thenReturn("Life renewal outreach");

        User user = User.create("sales@test.example", "{noop}password", "Sales Agent");
        ReflectionTestUtils.setField(user, "id", ASSIGNEE_ID);

        FollowUpTask task =
                new FollowUpTask(customer, user, "Check details", LocalDate.of(2026, 8, 1));
        ReflectionTestUtils.setField(task, "id", UUID.randomUUID());
        task.setCampaign(campaign);
        task.setDescription("Notes from call");
        task.start();

        Instant now = Instant.now();
        ReflectionTestUtils.setField(task, "createdAt", now);
        // FollowUpTask has no separate updatedAt column; view maps updatedAt from createdAt.

        FollowUpTaskView view = FollowUpTaskView.from(task);

        assertThat(view.id()).isEqualTo(task.getId());
        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Lovelace");
        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.campaignName()).isEqualTo("Life renewal outreach");
        assertThat(view.assignedToUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(view.assignedToFullName()).isEqualTo("Sales Agent");
        assertThat(view.title()).isEqualTo("Check details");
        assertThat(view.description()).isEqualTo("Notes from call");
        assertThat(view.dueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(view.status()).isEqualTo(FollowUpTaskStatus.IN_PROGRESS);
        assertThat(view.priority()).isEqualTo(FollowUpTaskPriority.MEDIUM);
        assertThat(view.createdAt()).isEqualTo(now);
        assertThat(view.updatedAt()).isEqualTo(now);
    }

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
