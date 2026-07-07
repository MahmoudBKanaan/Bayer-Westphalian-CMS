package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.common.domain.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import org.hibernate.annotations.ColumnTransformer;
import org.junit.jupiter.api.Test;

class CustomerTests {

    @Test
    void mapsKbCustomersTableAsJpaEntity() {
        assertThat(Customer.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Customer.class.getAnnotation(Table.class).name()).isEqualTo("customers");
        assertThat(SoftDeletableEntity.class).isAssignableFrom(Customer.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<Customer> constructor = Customer.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbCustomerColumnsAndValidationRules() throws Exception {
        assertColumn("customerType", "customer_type", false, 255);
        assertColumn("firstName", "first_name", false, 100);
        assertColumn("lastName", "last_name", false, 100);
        assertColumn("email", "email", true, 255);
        assertColumn("phone", "phone", true, 50);
        assertColumn("addressLine", "address_line", true, 255);
        assertColumn("city", "city", true, 100);
        assertColumn("country", "country", true, 100);
        assertColumn("dateOfBirth", "date_of_birth", true, 255);
        assertColumn("ageGroup", "age_group", true, 255);
        assertColumn("status", "status", false, 255);
        assertColumn("doNotContact", "do_not_contact", false, 255);
        assertColumn("source", "source", true, 100);

        assertThat(field("customerType").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("firstName").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("firstName").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("lastName").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("lastName").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("email").isAnnotationPresent(Email.class)).isTrue();
        assertThat(field("email").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("phone").getAnnotation(Size.class).max()).isEqualTo(50);
        assertThat(field("phone").isAnnotationPresent(Pattern.class)).isTrue();
        assertThat(field("addressLine").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("city").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("country").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("source").getAnnotation(Size.class).max()).isEqualTo(100);
    }

    @Test
    void mapsTypeAndStatusToKbPostgreSqlEnums() throws Exception {
        assertNativeEnumColumn("customerType", "customer_type");
        assertNativeEnumColumn("status", "customer_status");
    }

    @Test
    void mapsAgeGroupToKbPostgreSqlEnumValues() throws Exception {
        Field ageGroup = field("ageGroup");
        Column column = ageGroup.getAnnotation(Column.class);
        Convert convert = ageGroup.getAnnotation(Convert.class);
        ColumnTransformer columnTransformer = ageGroup.getAnnotation(ColumnTransformer.class);
        CustomerAgeGroupConverter converter = new CustomerAgeGroupConverter();

        assertThat(column.columnDefinition()).isEqualTo("customer_age_group");
        assertThat(convert.converter()).isEqualTo(CustomerAgeGroupConverter.class);
        assertThat(columnTransformer.write()).isEqualTo("?::customer_age_group");
        assertThat(CustomerAgeGroup.AGE_18_25.getDatabaseValue()).isEqualTo("18_25");
        assertThat(CustomerAgeGroup.AGE_60_PLUS.getDatabaseValue()).isEqualTo("60_PLUS");
        assertThat(converter.convertToDatabaseColumn(CustomerAgeGroup.AGE_26_40))
                .isEqualTo("26_40");
        assertThat(converter.convertToEntityAttribute("41_60"))
                .isEqualTo(CustomerAgeGroup.AGE_41_60);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convertToEntityAttribute("UNKNOWN"));
    }

    @Test
    void createsActiveContactableCustomerByDefault() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");

        assertThat(customer.getCustomerType()).isEqualTo(CustomerType.CUSTOMER);
        assertThat(customer.getFirstName()).isEqualTo("Ada");
        assertThat(customer.getLastName()).isEqualTo("Policyholder");
        assertThat(customer.getFullName()).isEqualTo("Ada Policyholder");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.isDoNotContact()).isFalse();
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.canBeContacted()).isTrue();
    }

    @Test
    void supportsKbProfileContactAddressAndDemographicUpdates() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "Ben", "Beneficiary");
        LocalDate dateOfBirth = LocalDate.parse("2001-02-03");

        customer.rename("Benjamin", "Beneficiary");
        customer.updateContactDetails("ben.beneficiary@bayer-westphalian.test", "+49-555-0100");
        customer.updateAddress("Insurance Street 1", "Berlin", "Germany");
        customer.updateDemographics(dateOfBirth, CustomerAgeGroup.AGE_18_25);
        customer.recordSource("CSV_IMPORT");

        assertThat(customer.getFullName()).isEqualTo("Benjamin Beneficiary");
        assertThat(customer.getEmail()).isEqualTo("ben.beneficiary@bayer-westphalian.test");
        assertThat(customer.getPhone()).isEqualTo("+49-555-0100");
        assertThat(customer.getAddressLine()).isEqualTo("Insurance Street 1");
        assertThat(customer.getCity()).isEqualTo("Berlin");
        assertThat(customer.getCountry()).isEqualTo("Germany");
        assertThat(customer.getDateOfBirth()).isEqualTo(dateOfBirth);
        assertThat(customer.getAgeGroup()).isEqualTo(CustomerAgeGroup.AGE_18_25);
        assertThat(customer.getSource()).isEqualTo("CSV_IMPORT");
    }

    @Test
    void supportsCustomerStatusContactPreferenceAndSoftDeleteLifecycle() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Clara", "Client");

        customer.changeStatus(CustomerStatus.INTERESTED);
        customer.markDoNotContact();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INTERESTED);
        assertThat(customer.canBeContacted()).isFalse();

        customer.allowContact();
        customer.markDeleted();

        assertThat(customer.isDoNotContact()).isFalse();
        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.isActive()).isFalse();
        assertThat(customer.canBeContacted()).isFalse();
    }

    @Test
    void customerWithDoNotContactTrueIsExcludedFromContactability() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Dana", "NoContact");

        customer.markDoNotContact();

        assertThat(customer.isDoNotContact()).isTrue();
        assertThat(customer.canBeContacted()).isFalse();
    }

    private static void assertNativeEnumColumn(String fieldName, String columnDefinition)
            throws Exception {
        Field enumField = field(fieldName);
        Column column = enumField.getAnnotation(Column.class);
        Enumerated enumerated = enumField.getAnnotation(Enumerated.class);

        assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return Customer.class.getDeclaredField(fieldName);
    }
}
