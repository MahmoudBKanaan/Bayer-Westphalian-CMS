package com.bayerwestphalian.campaign.followup;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;

    public FollowUpService(
            FollowUpRepository followUpRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository) {
        this.followUpRepository = followUpRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'CAMPAIGN_MANAGER')")
    public FollowUpTask createTask(CreateFollowUpTaskCommand command) {
        Customer customer =
                customerRepository
                        .findById(command.customerId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Customer", command.customerId()));

        User assignedTo = null;
        if (command.assignedTo() != null) {
            assignedTo =
                    userRepository
                            .findById(command.assignedTo())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "User", command.assignedTo()));
        }

        Campaign campaign = null;
        if (command.campaignId() != null) {
            campaign =
                    campaignRepository
                            .findById(command.campaignId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Campaign", command.campaignId()));
        }

        FollowUpTask task =
                new FollowUpTask(customer, assignedTo, command.title(), command.dueDate());
        task.setCampaign(campaign);
        task.setDescription(command.description());
        task.updatePriority(command.priority());

        return followUpRepository.save(task);
    }

    /**
     * Assigns a follow-up task to an active internal user (KB E17 / Follow-up management).
     *
     * <p>Authorized roles: Admin, Customer Service Agent, Sales Agent, and Campaign Manager.
     * Assignment is required for Sales Agent lead ownership and for filtering the follow-up
     * worklist by assignee.
     */
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', "
                    + "'CAMPAIGN_MANAGER')")
    public FollowUpTask assignTask(UUID taskId, UUID userId) {
        if (taskId == null) {
            throw new ValidationException(
                    "Follow-up task assignment failed", List.of("taskId: is required"));
        }
        if (userId == null) {
            throw new ValidationException(
                    "Follow-up task assignment failed", List.of("assignedTo: is required"));
        }

        FollowUpTask task =
                followUpRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("FollowUpTask", taskId));

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!user.isActive()) {
            throw new ValidationException(
                    "Follow-up task assignment failed",
                    List.of("assignedTo: must be an active user"));
        }

        task.assignTo(user);
        return followUpRepository.save(task);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public FollowUpTask updateTask(UUID taskId, String description, FollowUpTaskPriority priority) {
        FollowUpTask task =
                followUpRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("FollowUpTask", taskId));

        if (description != null) {
            task.setDescription(description);
        }
        if (priority != null) {
            task.updatePriority(priority);
        }

        return followUpRepository.save(task);
    }

    /**
     * Completes a follow-up task (KB E17 / Follow-up management).
     *
     * <p>Authorized roles: Admin, Customer Service Agent, and Sales Agent. Completion sets status
     * to {@code COMPLETED} and records {@code completed_at} so the worklist no longer treats the
     * task as open operational work.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public FollowUpTask completeTask(UUID taskId) {
        if (taskId == null) {
            throw new ValidationException(
                    "Follow-up task completion failed", List.of("taskId: is required"));
        }

        FollowUpTask task =
                followUpRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("FollowUpTask", taskId));

        task.complete();
        return followUpRepository.save(task);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public FollowUpTask updateTaskStatus(UUID taskId, FollowUpTaskStatus status) {
        if (taskId == null) {
            throw new ValidationException(
                    "Follow-up task status update failed", List.of("taskId: is required"));
        }
        if (status == null) {
            throw new ValidationException(
                    "Follow-up task status update failed", List.of("status: is required"));
        }

        FollowUpTask task =
                followUpRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("FollowUpTask", taskId));

        task.updateStatus(status);
        return followUpRepository.save(task);
    }

    @Transactional(readOnly = true)
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')")
    public List<FollowUpTask> listAssignedTasks(UUID userId) {
        return followUpRepository.findByAssignedTo(userId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')")
    public List<FollowUpTask> searchTasks(FollowUpTaskSearchCriteria criteria) {
        return followUpRepository.search(criteria);
    }
}
