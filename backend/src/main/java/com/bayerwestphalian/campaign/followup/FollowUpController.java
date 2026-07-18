package com.bayerwestphalian.campaign.followup;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follow-up-tasks")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @PostMapping
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<FollowUpTaskView>> createTask(
            @Valid @RequestBody CreateFollowUpTaskRequest request) {
        FollowUpTaskView task = followUpService.createTaskView(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Follow-up task created", task));
    }

    /**
     * Active Customer Service Agent options for manager assignment selectors (not admin-only
     * /users). Mapped before {@code /{id}/assign} so {@code assignee-options} is not treated as an
     * id.
     */
    @GetMapping("/assignee-options")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<List<FollowUpAssigneeOption>>> listAssigneeOptions() {
        List<FollowUpAssigneeOption> options =
                followUpService.listCustomerServiceAssigneeOptions();
        return ResponseEntity.ok(
                ApiResponse.success("Follow-up assignee options loaded", options));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<FollowUpTaskView>> assignTask(
            @PathVariable UUID id, @Valid @RequestBody AssignFollowUpTaskRequest request) {
        FollowUpTaskView task = followUpService.assignTaskView(id, request.assignedTo());
        return ResponseEntity.ok(ApiResponse.success("Follow-up task assigned", task));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public ResponseEntity<ApiResponse<FollowUpTaskView>> completeTask(@PathVariable UUID id) {
        FollowUpTaskView task = followUpService.completeTaskView(id);
        return ResponseEntity.ok(ApiResponse.success("Follow-up task completed", task));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public ResponseEntity<ApiResponse<FollowUpTaskView>> updateTaskStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateFollowUpStatusRequest request) {
        FollowUpTask task = followUpService.updateTaskStatus(id, request.status());
        return ResponseEntity.ok(
                ApiResponse.success("Follow-up task status updated", FollowUpTaskView.from(task)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    public ResponseEntity<ApiResponse<FollowUpTaskView>> updateTask(
            @PathVariable UUID id, @Valid @RequestBody UpdateFollowUpTaskRequest request) {
        FollowUpTask task =
                followUpService.updateTask(id, request.description(), request.priority());
        return ResponseEntity.ok(
                ApiResponse.success("Follow-up task updated", FollowUpTaskView.from(task)));
    }

    @GetMapping
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<ApiResponse<List<FollowUpTaskView>>> listTasks(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) FollowUpTaskPriority priority,
            @RequestParam(required = false) FollowUpTaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dueDateTo) {
        List<FollowUpTaskView> taskViews =
                followUpService.searchTaskViews(
                        new FollowUpTaskSearchCriteria(
                                customerId, assignedTo, priority, status, dueDateFrom, dueDateTo));
        return ResponseEntity.ok(ApiResponse.success("Follow-up tasks loaded", taskViews));
    }
}
