package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import org.junit.jupiter.api.Test;

class SegmentBehaviorStatusFilteringTests {

    @Test
    void recognizesKbBehaviorStatusFieldNames() {
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("status")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("STATUS")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("customer_status")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("behavior")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("do_not_contact")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("dnc")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("source")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("interest")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("interests")).isTrue();
        assertThat(SegmentBehaviorStatusSupport.isBehaviorStatusField("city")).isFalse();
    }

    @Test
    void canonicalizesBehaviorStatusAliasesToKbFieldNames() {
        assertThat(SegmentBehaviorStatusSupport.canonicalizeFieldName("customer_status"))
                .isEqualTo("status");
        assertThat(SegmentBehaviorStatusSupport.canonicalizeFieldName("behavior"))
                .isEqualTo("status");
        assertThat(SegmentBehaviorStatusSupport.canonicalizeFieldName("behaviour"))
                .isEqualTo("status");
        assertThat(SegmentBehaviorStatusSupport.canonicalizeFieldName("dnc"))
                .isEqualTo("do_not_contact");
        assertThat(SegmentBehaviorStatusSupport.canonicalizeFieldName("interest"))
                .isEqualTo("source");
        assertThat(SegmentBehaviorStatusSupport.canonicalizeFieldName("interests"))
                .isEqualTo("source");
    }

    @Test
    void resolvesCustomerBehaviorStatusValues() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Lena", "Mueller");
        customer.changeStatus(CustomerStatus.INTERESTED);
        customer.markDoNotContact();
        customer.recordSource("LIFE_INSURANCE_BENEFICIARY");

        assertThat(SegmentBehaviorStatusSupport.resolveCustomerValue(customer, "status"))
                .isEqualTo("INTERESTED");
        assertThat(SegmentBehaviorStatusSupport.resolveCustomerValue(customer, "behavior"))
                .isEqualTo("INTERESTED");
        assertThat(SegmentBehaviorStatusSupport.resolveCustomerValue(customer, "do_not_contact"))
                .isEqualTo("true");
        assertThat(SegmentBehaviorStatusSupport.resolveCustomerValue(customer, "interest"))
                .isEqualTo("LIFE_INSURANCE_BENEFICIARY");
    }

    @Test
    void normalizesBehaviorStatusFilterValues() {
        assertThat(
                        SegmentBehaviorStatusSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "status", "interested"))
                .isEqualTo("INTERESTED");
        assertThat(
                        SegmentBehaviorStatusSupport.normalizeFilterValue(
                                SegmentOperator.IN, "behavior", "interested, converted"))
                .isEqualTo("INTERESTED,CONVERTED");
        assertThat(
                        SegmentBehaviorStatusSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "dnc", "YES"))
                .isEqualTo("true");
        assertThat(
                        SegmentBehaviorStatusSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS,
                                "interests",
                                "  LIFE_INSURANCE_BENEFICIARY  "))
                .isEqualTo("LIFE_INSURANCE_BENEFICIARY");
    }

    @Test
    void matchesBehaviorStatusValuesUsingKbOperators() {
        String interested =
                SegmentBehaviorStatusSupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, "status", "INTERESTED");
        String statuses =
                SegmentBehaviorStatusSupport.normalizeFilterValue(
                        SegmentOperator.IN, "behavior", "INTERESTED,CONVERTED");

        assertThat(SegmentCriteria.matchesValue(SegmentOperator.EQUALS, interested, "INTERESTED"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.IN, statuses, "CONVERTED"))
                .isTrue();
        assertThat(
                        SegmentCriteria.matchesValue(
                                SegmentOperator.NOT_EQUALS, "UNINTERESTED", "INTERESTED"))
                .isTrue();
        assertThat(
                        SegmentCriteria.matchesValue(
                                SegmentOperator.CONTAINS, "LIFE", "LIFE_INSURANCE_BENEFICIARY"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.EQUALS, "true", "true")).isTrue();
    }

    @Test
    void rejectsUnsupportedBehaviorStatusFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentBehaviorStatusSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "status", "WARM_LEAD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of ACTIVE, INACTIVE, INTERESTED");

        assertThatThrownBy(
                        () ->
                                SegmentBehaviorStatusSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "do_not_contact", "maybe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("true, false");

        assertThatThrownBy(
                        () ->
                                SegmentBehaviorStatusSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "source", "x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be at most 100 characters");
    }
}
