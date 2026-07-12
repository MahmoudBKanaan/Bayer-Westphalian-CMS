package com.bayerwestphalian.campaign.product;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    List<PaymentRecord> findByStatusOrderByDueDateAsc(PaymentStatus status);

    List<PaymentRecord> findByStatusInOrderByDueDateAscReminderCountDesc(
            Collection<PaymentStatus> statuses);

    List<PaymentRecord> findByCustomerIdOrderByDueDateAsc(UUID customerId);

    default List<PaymentRecord> findDuePayments() {
        return findByStatusOrderByDueDateAsc(PaymentStatus.DUE);
    }

    default List<PaymentRecord> findOverduePayments() {
        return findByStatusInOrderByDueDateAscReminderCountDesc(
                List.of(PaymentStatus.OVERDUE, PaymentStatus.DEFAULT_RISK));
    }

    default List<PaymentRecord> findByCustomerId(UUID customerId) {
        return findByCustomerIdOrderByDueDateAsc(customerId);
    }
}