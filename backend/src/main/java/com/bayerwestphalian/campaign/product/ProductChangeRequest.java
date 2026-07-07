package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_change_requests")
public class ProductChangeRequest extends BaseEntity {

    @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "request_type", nullable = false, columnDefinition = "product_change_type")
    private ProductChangeType requestType;

    @NotBlank @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "product_change_status")
    private ProductChangeStatus status = ProductChangeStatus.OPEN;

    protected ProductChangeRequest() {}

    private ProductChangeRequest(
            Product product,
            User requestedBy,
            ProductChangeType requestType,
            String description) {
        this.product = product;
        this.requestedBy = requestedBy;
        this.requestType = requestType;
        this.description = description;
    }

    public static ProductChangeRequest create(
            Product product,
            User requestedBy,
            ProductChangeType requestType,
            String description) {
        return new ProductChangeRequest(product, requestedBy, requestType, description);
    }

    public Product getProduct() {
        return product;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public ProductChangeType getRequestType() {
        return requestType;
    }

    public String getDescription() {
        return description;
    }

    public ProductChangeStatus getStatus() {
        return status;
    }

    public void approve() {
        status = ProductChangeStatus.APPROVED;
    }

    public void reject() {
        status = ProductChangeStatus.REJECTED;
    }

    public void markImplemented() {
        status = ProductChangeStatus.IMPLEMENTED;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
