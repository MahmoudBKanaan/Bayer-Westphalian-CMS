package com.bayerwestphalian.campaign.followup;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "follow_up_tasks")
public class FollowUpTask {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @NotNull @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "follow_up_status")
    private FollowUpTaskStatus status = FollowUpTaskStatus.OPEN;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "priority", nullable = false, columnDefinition = "work_priority")
    private FollowUpTaskPriority priority = FollowUpTaskPriority.MEDIUM;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FollowUpTask() {}

    public FollowUpTask(Customer customer, User assignedTo, String title, LocalDate dueDate) {
        this.customer = Objects.requireNonNull(customer, "Customer is required");
        this.assignedTo = assignedTo;
        this.title = Objects.requireNonNull(title, "Title is required");
        this.dueDate = dueDate;
        this.status = FollowUpTaskStatus.OPEN;
        this.priority = FollowUpTaskPriority.MEDIUM;
    }

    public void assignTo(User user) {
        this.assignedTo = user;
    }

    public void start() {
        this.status = FollowUpTaskStatus.IN_PROGRESS;
        this.completedAt = null;
    }

    public void complete() {
        this.status = FollowUpTaskStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        this.status = FollowUpTaskStatus.CANCELLED;
        this.completedAt = null;
    }

    public void reopen() {
        this.status = FollowUpTaskStatus.OPEN;
        this.completedAt = null;
    }

    public void updateStatus(FollowUpTaskStatus status) {
        Objects.requireNonNull(status, "Status is required");
        switch (status) {
            case OPEN -> reopen();
            case IN_PROGRESS -> start();
            case COMPLETED -> complete();
            case CANCELLED -> cancel();
        }
    }

    public void updatePriority(FollowUpTaskPriority priority) {
        this.priority = Objects.requireNonNull(priority, "Priority is required");
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Customer getCustomer() {
        return customer;
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public FollowUpTaskStatus getStatus() {
        return status;
    }

    public FollowUpTaskPriority getPriority() {
        return priority;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
