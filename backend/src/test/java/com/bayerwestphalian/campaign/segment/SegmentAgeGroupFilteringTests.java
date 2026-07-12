package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerType;
import org.junit.jupiter.api.Test;

class SegmentAgeGroupFilteringTests {

    @Test
    void recognizesKbAgeGroupFieldNames() {
        assertThat(SegmentAgeGroupSupport.isAgeGroupField("age_group")).isTrue();
        assertThat(SegmentAgeGroupSupport.isAgeGroupField("AGE_GROUP")).isTrue();
        assertThat(SegmentAgeGroupSupport.isAgeGroupField("agegroup")).isTrue();
        assertThat(SegmentAgeGroupSupport.isAgeGroupField("city")).isFalse();
    }

    @Test
    void resolvesCustomerAgeGroupUsingKbDatabaseValues() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Lena", "Mueller");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);

        assertThat(SegmentAgeGroupSupport.resolveCustomerValue(customer)).isEqualTo("26_40");
    }

    @Test
    void normalizesEnumNameFilterValuesToDatabaseValues() {
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "AGE_18_25"))
                .isEqualTo("18_25");
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "26_40"))
                .isEqualTo("26_40");
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "MINOR"))
                .isEqualTo("MINOR");
    }

    @Test
    void normalizesInOperatorAgeGroupValues() {
        assertThat(
                        SegmentAgeGroupSupport.normalizeFilterValue(
                                SegmentOperator.IN, "AGE_18_25, 26_40, MINOR"))
                .isEqualTo("18_25,26_40,MINOR");
    }

    @Test
    void matchesNormalizedAgeGroupValuesForKbOperators() {
        String normalizedValue =
                SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "AGE_26_40");
        String normalizedInValue =
                SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.IN, "18_25,AGE_41_60");

        assertThat(SegmentCriteria.matchesValue(SegmentOperator.EQUALS, normalizedValue, "26_40"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.IN, normalizedInValue, "41_60"))
                .isTrue();
        assertThat(
                        SegmentCriteria.matchesValue(
                                SegmentOperator.NOT_EQUALS,
                                SegmentAgeGroupSupport.normalizeFilterValue(
                                        SegmentOperator.EQUALS, "AGE_60_PLUS"),
                                "26_40"))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedAgeGroupFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentAgeGroupSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "TEENAGER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of");
    }

    @Test
    void resolvesNullAgeGroupAsNullCandidate() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "No", "Age");
        assertThat(SegmentAgeGroupSupport.resolveCustomerValue(customer)).isNull();
        assertThat(SegmentAgeGroupSupport.resolveCustomerValue(null)).isNull();
    }

    @Test
    void supportsAllKbAgeGroupDatabaseCodes() {
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "MINOR"))
                .isEqualTo("MINOR");
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "18_25"))
                .isEqualTo("18_25");
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "26_40"))
                .isEqualTo("26_40");
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "41_60"))
                .isEqualTo("41_60");
        assertThat(SegmentAgeGroupSupport.normalizeFilterValue(SegmentOperator.EQUALS, "60_PLUS"))
                .isEqualTo("60_PLUS");
    }
}
