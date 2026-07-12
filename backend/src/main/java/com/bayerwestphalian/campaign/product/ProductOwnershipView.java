package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.customer.Customer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProductOwnershipView(
        UUID id,
        UUID customerId,
        String customerFullName,
        UUID productId,
        String productName,
        ProductType productType,
        String policyNumber,
        LocalDate startDate,
        LocalDate expirationDate,
        OwnershipStatus status,
        boolean active,
        Instant createdAt) {

    public static ProductOwnershipView from(ProductOwnership ownership) {
        Customer customer = ownership.getCustomer();
        Product product = ownership.getProduct();

        return new ProductOwnershipView(
                ownership.getId(),
                customerId(customer),
                fullName(customer),
                productId(product),
                productName(product),
                productType(product),
                ownership.getPolicyNumber(),
                ownership.getStartDate(),
                ownership.getExpirationDate(),
                ownership.getStatus(),
                ownership.isActive(),
                ownership.getCreatedAt());
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String fullName(Customer customer) {
        return customer == null ? null : customer.getFullName();
    }

    private static UUID productId(Product product) {
        return product == null ? null : product.getId();
    }

    private static String productName(Product product) {
        return product == null ? null : product.getName();
    }

    private static ProductType productType(Product product) {
        return product == null ? null : product.getProductType();
    }
}
