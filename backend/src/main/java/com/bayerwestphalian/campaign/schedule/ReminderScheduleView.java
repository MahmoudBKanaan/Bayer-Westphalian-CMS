package com.bayerwestphalian.campaign.schedule;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReminderScheduleView(
        UUID id,
        UUID customerId,
        String customerFullName,
        UUID productId,
        String productName,
        ProductType productType,
        ReminderType reminderType,
        ReminderLevel reminderLevel,
        LocalDate scheduledDate,
        ReminderStatus status,
        Instant createdAt,
        Instant sentAt,
        boolean due) {

    public static ReminderScheduleView from(ReminderSchedule reminder) {
        Customer customer = reminder.getCustomer();
        Product product = reminder.getProduct();

        return new ReminderScheduleView(
                reminder.getId(),
                customerId(customer),
                customerFullName(customer),
                productId(product),
                productName(product),
                productType(product),
                reminder.getReminderType(),
                reminder.getReminderLevel(),
                reminder.getScheduledDate(),
                reminder.getStatus(),
                reminder.getCreatedAt(),
                reminder.getSentAt(),
                reminder.isDue());
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String customerFullName(Customer customer) {
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
