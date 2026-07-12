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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "product_ownerships",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "product_ownerships_policy_number_unique",
                        columnNames = "policy_number"))
public class ProductOwnership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Size(max = 100) @Column(name = "policy_number", length = 100, unique = true)
    private String policyNumber;

    @NotNull @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ownership_status")
    private OwnershipStatus status = OwnershipStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductOwnership() {}

    private ProductOwnership(
            Customer customer, Product product, LocalDate startDate, LocalDate expirationDate) {
        ensureExpirationIsNotBeforeStart(startDate, expirationDate);
        this.customer = customer;
        this.product = product;
        this.startDate = startDate;
        this.expirationDate = expirationDate;
    }

    public static ProductOwnership create(
            Customer customer, Product product, LocalDate startDate, LocalDate expirationDate) {
        return new ProductOwnership(customer, product, startDate, expirationDate);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Product getProduct() {
        return product;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public OwnershipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void recordPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public void updateExpirationDate(LocalDate expirationDate) {
        ensureExpirationIsNotBeforeStart(startDate, expirationDate);
        this.expirationDate = expirationDate;
    }

    public void expire() {
        status = OwnershipStatus.EXPIRED;
    }

    public void cancel() {
        status = OwnershipStatus.CANCELLED;
    }

    public boolean isExpiringWithinMonths(int months) {
        if (months < 0 || expirationDate == null || !isActive()) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate latestExpiration = today.plusMonths(months);
        return !expirationDate.isBefore(today) && !expirationDate.isAfter(latestExpiration);
    }

    public boolean isActive() {
        return status == OwnershipStatus.ACTIVE
                && (expirationDate == null || !expirationDate.isBefore(LocalDate.now()));
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

    private static void ensureExpirationIsNotBeforeStart(
            LocalDate startDate, LocalDate expirationDate) {
        if (startDate != null && expirationDate != null && expirationDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Expiration date must be on or after start date");
        }
    }
}
