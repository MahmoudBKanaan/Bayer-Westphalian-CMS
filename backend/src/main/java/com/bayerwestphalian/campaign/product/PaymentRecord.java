package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.customer.Customer;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    private static final int DEFAULT_RISK_REMINDER_THRESHOLD = 3;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_ownership_id", nullable = false)
    private ProductOwnership productOwnership;

    @NotNull @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "payment_status")
    private PaymentStatus status = PaymentStatus.DUE;

    @PositiveOrZero @Column(name = "reminder_count", nullable = false)
    private int reminderCount;

    protected PaymentRecord() {}

    private PaymentRecord(
            Customer customer,
            ProductOwnership productOwnership,
            LocalDate dueDate,
            BigDecimal amountDue) {
        this.customer = customer;
        this.productOwnership = productOwnership;
        this.dueDate = dueDate;
        this.amountDue = amountDue;
    }

    public static PaymentRecord create(
            Customer customer,
            ProductOwnership productOwnership,
            LocalDate dueDate,
            BigDecimal amountDue) {
        return new PaymentRecord(customer, productOwnership, dueDate, amountDue);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public ProductOwnership getProductOwnership() {
        return productOwnership;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public void markPaid(BigDecimal amountPaid, Instant paidAt) {
        this.amountPaid = amountPaid;
        this.paidAt = paidAt;
        status = PaymentStatus.PAID;
    }

    public void updateDetails(LocalDate dueDate, BigDecimal amountDue) {
        this.dueDate = dueDate;
        this.amountDue = amountDue;
    }

    public void markOverdue() {
        if (status == PaymentStatus.DUE) {
            status = PaymentStatus.OVERDUE;
        }
    }

    public void incrementReminder() {
        if (status == PaymentStatus.PAID) {
            return;
        }

        reminderCount++;
        if (reminderCount >= DEFAULT_RISK_REMINDER_THRESHOLD) {
            status = PaymentStatus.DEFAULT_RISK;
        } else if (reminderCount >= 2) {
            status = PaymentStatus.OVERDUE;
        }
    }

    public long calculateDaysOverdue() {
        if (dueDate == null || status == PaymentStatus.PAID) {
            return 0;
        }

        return Math.max(0, ChronoUnit.DAYS.between(dueDate, LocalDate.now()));
    }

    public boolean isDefaultRisk() {
        return status == PaymentStatus.DEFAULT_RISK;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
