package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Item 537: status changes record {@code status_changed_at} for exclusion period anchoring. */
@DisplayName("Customer status_changed_at (item 537)")
class CustomerStatusChangedAtTests {

    @Test
    void changeStatusRecordsStatusChangedAtWhenStatusDiffers() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Status", "Change");
        assertThat(customer.getStatusChangedAt()).isNull();

        customer.changeStatus(CustomerStatus.UNINTERESTED);

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.UNINTERESTED);
        assertThat(customer.getStatusChangedAt()).isNotNull();
    }

    @Test
    void changeStatusDoesNotRefreshTimestampWhenStatusUnchanged() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Same", "Status");
        customer.changeStatus(CustomerStatus.UNINTERESTED);
        var first = customer.getStatusChangedAt();

        customer.changeStatus(CustomerStatus.UNINTERESTED);

        assertThat(customer.getStatusChangedAt()).isEqualTo(first);
    }
}
