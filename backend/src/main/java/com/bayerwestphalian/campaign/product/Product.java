package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.common.domain.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product extends SoftDeletableEntity {

    @NotBlank @Size(max = 255) @Column(name = "name", nullable = false)
    private String name;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "product_type", nullable = false, columnDefinition = "product_type")
    private ProductType productType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Positive @Column(name = "duration_months")
    private Integer durationMonths;

    @Size(max = 100) @Column(name = "expiration_policy", length = 100)
    private String expirationPolicy;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Product() {}

    private Product(
            String name, ProductType productType, BigDecimal price, Integer durationMonths) {
        this.name = name;
        this.productType = productType;
        this.price = price;
        this.durationMonths = durationMonths;
    }

    public static Product create(
            String name, ProductType productType, BigDecimal price, Integer durationMonths) {
        return new Product(name, productType, price, durationMonths);
    }

    public String getName() {
        return name;
    }

    public ProductType getProductType() {
        return productType;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public String getExpirationPolicy() {
        return expirationPolicy;
    }

    public boolean getActive() {
        return active;
    }

    public boolean isActive() {
        return active && !isDeleted();
    }

    public void updateDetails(
            String name,
            ProductType productType,
            String description,
            Integer durationMonths,
            String expirationPolicy) {
        this.name = name;
        this.productType = productType;
        this.description = description;
        this.durationMonths = durationMonths;
        this.expirationPolicy = expirationPolicy;
    }

    public void updatePricing(BigDecimal price) {
        this.price = price;
    }

    public void deactivate() {
        active = false;
    }

    public void activate() {
        active = true;
    }

    public void softDelete() {
        markDeleted();
    }
}
