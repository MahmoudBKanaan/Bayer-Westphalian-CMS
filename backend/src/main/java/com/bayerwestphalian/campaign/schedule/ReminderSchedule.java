package com.bayerwestphalian.campaign.schedule;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.product.Product;
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
@Table(name = "reminder_schedules")
public class ReminderSchedule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reminder_type", nullable = false, columnDefinition = "reminder_type")
    private ReminderType reminderType;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reminder_level", nullable = false, columnDefinition = "reminder_level")
    private ReminderLevel reminderLevel;

    @NotNull @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "reminder_status")
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected ReminderSchedule() {}

    public ReminderSchedule(
            Customer customer,
            Product product,
            ReminderType reminderType,
            ReminderLevel reminderLevel,
            LocalDate scheduledDate) {
        this.customer = Objects.requireNonNull(customer, "Customer is required");
        this.product = Objects.requireNonNull(product, "Product is required");
        this.reminderType = Objects.requireNonNull(reminderType, "Reminder type is required");
        this.reminderLevel = Objects.requireNonNull(reminderLevel, "Reminder level is required");
        this.scheduledDate = Objects.requireNonNull(scheduledDate, "Scheduled date is required");
        this.status = ReminderStatus.PENDING;
    }

    public void markSent() {
        status = ReminderStatus.SENT;
        sentAt = Instant.now();
    }

    public void markFailed() {
        status = ReminderStatus.FAILED;
        sentAt = null;
    }

    public void cancel() {
        status = ReminderStatus.CANCELLED;
        sentAt = null;
    }

    public boolean isDue() {
        return status == ReminderStatus.PENDING && !scheduledDate.isAfter(LocalDate.now());
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public UUID getCustomerId() {
        return customer == null ? null : customer.getId();
    }

    public Product getProduct() {
        return product;
    }

    public UUID getProductId() {
        return product == null ? null : product.getId();
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public ReminderLevel getReminderLevel() {
        return reminderLevel;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public ReminderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
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
