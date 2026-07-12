package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.customer.Customer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRecordView(
        UUID id,
        UUID customerId,
        String customerFullName,
        UUID productOwnershipId,
        UUID productId,
        String productName,
        ProductType productType,
        LocalDate dueDate,
        Instant paidAt,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        PaymentStatus status,
        int reminderCount,
        long daysOverdue,
        boolean defaultRisk) {

    public static PaymentRecordView from(PaymentRecord paymentRecord) {
        Customer customer = paymentRecord.getCustomer();
        ProductOwnership ownership = paymentRecord.getProductOwnership();
        Product product = ownership == null ? null : ownership.getProduct();

        return new PaymentRecordView(
                paymentRecord.getId(),
                customerId(customer),
                fullName(customer),
                ownershipId(ownership),
                productId(product),
                productName(product),
                productType(product),
                paymentRecord.getDueDate(),
                paymentRecord.getPaidAt(),
                paymentRecord.getAmountDue(),
                paymentRecord.getAmountPaid(),
                paymentRecord.getStatus(),
                paymentRecord.getReminderCount(),
                paymentRecord.calculateDaysOverdue(),
                paymentRecord.isDefaultRisk());
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String fullName(Customer customer) {
        return customer == null ? null : customer.getFullName();
    }

    private static UUID ownershipId(ProductOwnership ownership) {
        return ownership == null ? null : ownership.getId();
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