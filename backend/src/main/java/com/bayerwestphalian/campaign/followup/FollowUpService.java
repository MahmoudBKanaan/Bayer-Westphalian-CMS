package com.bayerwestphalian.campaign.followup;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import com.bayerwestphalian.campaign.user.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Follow-up task workflows (KB E17 / FR-093+).
 *
 * <p><strong>Assignment ownership (KB):</strong>
 *
 * <ul>
 *   <li>Only managers ({@code ADMIN}, {@code CAMPAIGN_MANAGER}) may assign tasks to Customer
 *       Service Agent accounts.
 *   <li>When a non-manager creates a task, it is always assigned to the creator.
 *   <li>Non-managers only see their <em>Assigned Worklist</em> (tasks where {@code assigned_to} is
 *       themselves).
 * </ul>
 */
@Service
@Transactional
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final AuthorizationExpressions authorizationExpressions;

    public FollowUpService(
            FollowUpRepository followUpRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            AuthorizationExpressions authorizationExpressions) {
        this.followUpRepository = followUpRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.authorizationExpressions = authorizationExpressions;
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

        // Managers may assign to a Customer Service Agent (or leave unassigned).
        // Non-managers are always auto-assigned to themselves (KB agent worklist ownership).
        User assignedTo;
        if (isFollowUpManager()) {
            assignedTo =
                    command.assignedTo() == null
                            ? null
                            : requireActiveCustomerServiceAgent(command.assignedTo());
        } else {
            assignedTo = requireCurrentActiveUser();
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
     * Creates a follow-up and maps the view inside the open transaction (lazy associations).
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'CAMPAIGN_MANAGER')")
    public FollowUpTaskView createTaskView(CreateFollowUpTaskCommand command) {
        return FollowUpTaskView.from(createTask(command));
    }

    /**
     * Assigns a follow-up task to an active Customer Service Agent (KB E17 / Follow-up management).
     *
     * <p>Only managers ({@code ADMIN}, {@code CAMPAIGN_MANAGER}) may assign tasks to Customer
     * Service Agents. Agents receive work via manager assignment or self-assignment on create.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
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

        User user = requireActiveCustomerServiceAgent(userId);

        task.assignTo(user);
        return followUpRepository.save(task);
    }

    /**
     * Assigns and maps the view inside the open transaction so customer/campaign/assignee lazy
     * associations remain available (avoids LazyInitializationException after the service returns).
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public FollowUpTaskView assignTaskView(UUID taskId, UUID userId) {
        return FollowUpTaskView.from(assignTask(taskId, userId));
    }

    /**
     * Active Customer Service Agent accounts for manager assignee selectors (not the admin-only
     * /users directory).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public List<FollowUpAssigneeOption> listCustomerServiceAssigneeOptions() {
        return userRepository
                .findActiveUsersWithRole(SystemRoleName.CUSTOMER_SERVICE_AGENT, UserStatus.ACTIVE)
                .stream()
                .map(FollowUpAssigneeOption::from)
                .toList();
    }

    private boolean isFollowUpManager() {
        return authorizationExpressions.hasAnyRole(
                SystemRoleName.ADMIN.name(), SystemRoleName.CAMPAIGN_MANAGER.name());
    }

    private User requireCurrentActiveUser() {
        UUID currentUserId = authorizationExpressions.currentUserId();
        User user =
                userRepository
                        .findById(currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
        if (!user.isActive()) {
            throw new ValidationException(
                    "Follow-up task creation failed",
                    List.of("creator: must be an active user to own the task"));
        }
        return user;
    }

    /**
     * Assignees for manager assignment must be active employees with the Customer Service Agent
     * role (KB: managers assign to customer agent accounts).
     */
    private User requireActiveCustomerServiceAgent(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!user.isActive()) {
            throw new ValidationException(
                    "Follow-up task assignment failed",
                    List.of("assignedTo: must be an active Customer Service Agent"));
        }
        boolean isCustomerServiceAgent =
                userRepository.isActiveUserWithRole(
                        userId, SystemRoleName.CUSTOMER_SERVICE_AGENT, UserStatus.ACTIVE);
        if (!isCustomerServiceAgent) {
            throw new ValidationException(
                    "Follow-up task assignment failed",
                    List.of("assignedTo: must be an active Customer Service Agent"));
        }
        return user;
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public FollowUpTask updateTask(UUID taskId, String description, FollowUpTaskPriority priority) {
        FollowUpTask task =
                followUpRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("FollowUpTask", taskId));
        requireAssigneeOrManagerAccess(task);

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
     * <p>The <strong>assignee</strong> or a <strong>manager</strong> ({@code ADMIN}, {@code
     * CAMPAIGN_MANAGER}) may mark a task complete. Completion sets status to {@code COMPLETED} and
     * records {@code completed_at}.
     */
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public FollowUpTask completeTask(UUID taskId) {
        if (taskId == null) {
            throw new ValidationException(
                    "Follow-up task completion failed", List.of("taskId: is required"));
        }

        FollowUpTask task =
                followUpRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("FollowUpTask", taskId));
        requireAssigneeOrManagerAccess(task);

        task.complete();
        return followUpRepository.save(task);
    }

    /**
     * Completes and maps the view inside the open transaction (lazy associations).
     */
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public FollowUpTaskView completeTaskView(UUID taskId) {
        return FollowUpTaskView.from(completeTask(taskId));
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
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
        requireAssigneeOrManagerAccess(task);

        task.updateStatus(status);
        return followUpRepository.save(task);
    }

    /**
     * Agents may mutate only tasks on their Assigned Worklist. Managers ({@code ADMIN}, {@code
     * CAMPAIGN_MANAGER}) may complete/update any task.
     */
    private void requireAssigneeOrManagerAccess(FollowUpTask task) {
        if (isFollowUpManager()) {
            return;
        }
        UUID currentUserId = authorizationExpressions.currentUserId();
        User assignee = task.getAssignedTo();
        if (assignee == null || !currentUserId.equals(assignee.getId())) {
            throw new ValidationException(
                    "Follow-up task update failed",
                    List.of("task: you may only update tasks assigned to you"));
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')")
    public List<FollowUpTask> listAssignedTasks(UUID userId) {
        UUID effectiveAssignee = resolveAssigneeFilter(userId);
        return followUpRepository.findByAssignedTo(effectiveAssignee);
    }

    @Transactional(readOnly = true)
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')")
    public List<FollowUpTask> searchTasks(FollowUpTaskSearchCriteria criteria) {
        return followUpRepository.search(restrictToAssignedWorklistIfNeeded(criteria));
    }

    /**
     * Search and map to views inside one read-only transaction so lazy customer/campaign/assignee
     * associations are available when building {@link FollowUpTaskView}.
     *
     * <p>Non-managers only receive their <strong>Assigned Worklist</strong> (tasks where {@code
     * assigned_to} is the current user). Managers may search the full operational worklist.
     */
    @Transactional(readOnly = true)
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')")
    public List<FollowUpTaskView> searchTaskViews(FollowUpTaskSearchCriteria criteria) {
        FollowUpTaskSearchCriteria effective = restrictToAssignedWorklistIfNeeded(criteria);
        return followUpRepository.search(effective).stream().map(FollowUpTaskView::from).toList();
    }

    /**
     * Managers may list any assignee (or null for repository semantics). Non-managers are forced to
     * their own user id so the Assigned Worklist cannot be expanded by query parameter.
     */
    private UUID resolveAssigneeFilter(UUID requestedAssignee) {
        if (isFollowUpManager()) {
            return requestedAssignee;
        }
        return authorizationExpressions.currentUserId();
    }

    private FollowUpTaskSearchCriteria restrictToAssignedWorklistIfNeeded(
            FollowUpTaskSearchCriteria criteria) {
        if (isFollowUpManager()) {
            return criteria == null
                    ? new FollowUpTaskSearchCriteria(null, null, null, null, null, null)
                    : criteria;
        }
        UUID currentUserId = authorizationExpressions.currentUserId();
        if (criteria == null) {
            return new FollowUpTaskSearchCriteria(null, currentUserId, null, null, null, null);
        }
        return new FollowUpTaskSearchCriteria(
                criteria.customerId(),
                currentUserId,
                criteria.priority(),
                criteria.status(),
                criteria.dueDateFrom(),
                criteria.dueDateTo());
    }
}
