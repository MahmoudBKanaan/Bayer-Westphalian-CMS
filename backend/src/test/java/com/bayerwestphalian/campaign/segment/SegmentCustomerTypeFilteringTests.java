package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import org.junit.jupiter.api.Test;

class SegmentCustomerTypeFilteringTests {

    @Test
    void recognizesKbCustomerTypeFieldNames() {
        assertThat(SegmentCustomerTypeSupport.isCustomerTypeField("customer_type")).isTrue();
        assertThat(SegmentCustomerTypeSupport.isCustomerTypeField("CUSTOMER_TYPE")).isTrue();
        assertThat(SegmentCustomerTypeSupport.isCustomerTypeField("customertype")).isTrue();
        assertThat(SegmentCustomerTypeSupport.isCustomerTypeField("type")).isTrue();
        assertThat(SegmentCustomerTypeSupport.isCustomerTypeField("city")).isFalse();
    }

    @Test
    void canonicalizesCustomerTypeAliasesToKbFieldName() {
        assertThat(SegmentCustomerTypeSupport.canonicalizeFieldName("type"))
                .isEqualTo("customer_type");
        assertThat(SegmentCustomerTypeSupport.canonicalizeFieldName("customertype"))
                .isEqualTo("customer_type");
        assertThat(SegmentCustomerTypeSupport.canonicalizeFieldName("customer_type"))
                .isEqualTo("customer_type");
    }

    @Test
    void resolvesCustomerTypeValue() {
        Customer prospect = Customer.create(CustomerType.PROSPECT, "Lena", "Mueller");

        assertThat(SegmentCustomerTypeSupport.resolveCustomerValue(prospect)).isEqualTo("PROSPECT");
    }

    @Test
    void normalizesCustomerTypeFilterValues() {
        assertThat(
                        SegmentCustomerTypeSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "prospect"))
                .isEqualTo("PROSPECT");
        assertThat(
                        SegmentCustomerTypeSupport.normalizeFilterValue(
                                SegmentOperator.IN, "customer, PROSPECT"))
                .isEqualTo("CUSTOMER,PROSPECT");
    }

    @Test
    void matchesCustomerTypeValuesUsingKbOperators() {
        String normalizedValue =
                SegmentCustomerTypeSupport.normalizeFilterValue(SegmentOperator.EQUALS, "prospect");

        assertThat(
                        SegmentCriteria.matchesValue(
                                SegmentOperator.EQUALS, normalizedValue, "PROSPECT"))
                .isTrue();
        assertThat(
                        SegmentCriteria.matchesValue(
                                SegmentOperator.IN, "CUSTOMER,PROSPECT", "CUSTOMER"))
                .isTrue();
        assertThat(
                        SegmentCriteria.matchesValue(
                                SegmentOperator.NOT_EQUALS, "BENEFICIARY", "PROSPECT"))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedCustomerTypeFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentCustomerTypeSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "LEAD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of CUSTOMER, PROSPECT, or BENEFICIARY");
    }
}
