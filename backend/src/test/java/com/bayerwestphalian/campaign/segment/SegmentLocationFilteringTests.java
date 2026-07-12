package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import org.junit.jupiter.api.Test;

/** Unit coverage for KB FR-071 location field helpers used by segment criteria matching. */
class SegmentLocationFilteringTests {

    @Test
    void recognizesKbLocationFieldNames() {
        assertThat(SegmentLocationSupport.isLocationField("city")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("COUNTRY")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("address_line")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("addressline")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("location")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("age_group")).isFalse();
        assertThat(SegmentLocationSupport.isLocationField(" ")).isFalse();
        assertThat(SegmentLocationSupport.isLocationField(null)).isFalse();
    }

    @Test
    void canonicalizesLocationAliasesToKbFieldNames() {
        assertThat(SegmentLocationSupport.canonicalizeFieldName("location")).isEqualTo("city");
        assertThat(SegmentLocationSupport.canonicalizeFieldName("LOCATION")).isEqualTo("city");
        assertThat(SegmentLocationSupport.canonicalizeFieldName("addressline"))
                .isEqualTo("address_line");
        assertThat(SegmentLocationSupport.canonicalizeFieldName("ADDRESS_LINE"))
                .isEqualTo("address_line");
        assertThat(SegmentLocationSupport.canonicalizeFieldName("city")).isEqualTo("city");
        assertThat(SegmentLocationSupport.canonicalizeFieldName("country")).isEqualTo("country");
    }

    @Test
    void resolvesCustomerLocationValues() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Lena", "Mueller");
        customer.updateAddress("Main Street 1", "Munich", "Germany");

        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "city"))
                .isEqualTo("Munich");
        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "country"))
                .isEqualTo("Germany");
        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "address_line"))
                .isEqualTo("Main Street 1");
        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "location"))
                .isEqualTo("Munich");
        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "addressline"))
                .isEqualTo("Main Street 1");
    }

    @Test
    void resolvesNullLocationWhenCustomerHasNoAddress() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "No", "Address");

        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "city")).isNull();
        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "country")).isNull();
        assertThat(SegmentLocationSupport.resolveCustomerValue(customer, "address_line")).isNull();
        assertThat(SegmentLocationSupport.resolveCustomerValue(null, "city")).isNull();
    }

    @Test
    void normalizesLocationFilterValuesForInOperator() {
        assertThat(
                        SegmentLocationSupport.normalizeFilterValue(
                                SegmentOperator.IN, "city", " Munich , Berlin "))
                .isEqualTo("Munich,Berlin");
        assertThat(
                        SegmentLocationSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "country", " Germany "))
                .isEqualTo("Germany");
    }

    @Test
    void matchesLocationValuesUsingKbOperators() {
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.EQUALS, "Munich", "Munich"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.EQUALS, "munich", "Munich"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.CONTAINS, "mun", "Munich"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.CONTAINS, "MUN", "Munich"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.IN, "Munich,Berlin", "Berlin"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.NOT_EQUALS, "Germany", "Austria"))
                .isTrue();
        assertThat(SegmentCriteria.matchesValue(SegmentOperator.EQUALS, "Munich", null)).isFalse();
    }

    @Test
    void rejectsBlankAndOverlongLocationFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentLocationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "city", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid city value");

        String overlongCity = "x".repeat(101);
        assertThatThrownBy(
                        () ->
                                SegmentLocationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "city", overlongCity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be at most 100 characters");

        String overlongCountry = "y".repeat(101);
        assertThatThrownBy(
                        () ->
                                SegmentLocationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "country", overlongCountry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be at most 100 characters");

        String overlongAddress = "z".repeat(256);
        assertThatThrownBy(
                        () ->
                                SegmentLocationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "address_line", overlongAddress))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be at most 255 characters");
    }

    @Test
    void inListNormalizationDropsBlankEntries() {
        assertThat(
                        SegmentLocationSupport.normalizeFilterValue(
                                SegmentOperator.IN, "city", "Munich,  ,Berlin"))
                .isEqualTo("Munich,Berlin");
        SegmentLocationSupport.validateFilterValue(SegmentOperator.IN, "city", "Munich,  ,Berlin");
    }
}
