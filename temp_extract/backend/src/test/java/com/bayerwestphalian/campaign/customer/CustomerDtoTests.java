package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CustomerDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCreateCustomerRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(CreateCustomerRequest.class, "customerType")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateCustomerRequest.class, "firstName")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateCustomerRequest.class, "firstName").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(
                        field(CreateCustomerRequest.class, "lastName")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateCustomerRequest.class, "lastName").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(field(CreateCustomerRequest.class, "email").isAnnotationPresent(Email.class))
                .isTrue();
        assertThat(field(CreateCustomerRequest.class, "email").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(field(CreateCustomerRequest.class, "phone").getAnnotation(Size.class).max())
                .isEqualTo(50);
        assertThat(field(CreateCustomerRequest.class, "phone").isAnnotationPresent(Pattern.class))
                .isTrue();
        assertThat(
                        field(CreateCustomerRequest.class, "addressLine")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
        assertThat(field(CreateCustomerRequest.class, "city").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(field(CreateCustomerRequest.class, "country").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(
                        field(CreateCustomerRequest.class, "dateOfBirth")
                                .isAnnotationPresent(PastOrPresent.class))
                .isTrue();
        assertThat(field(CreateCustomerRequest.class, "source").getAnnotation(Size.class).max())
                .isEqualTo(100);
    }

    @Test
    void validatesUpdateAndSearchCustomerRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(UpdateCustomerRequest.class, "firstName")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(
                        field(UpdateCustomerRequest.class, "lastName")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(UpdateCustomerRequest.class, "email").isAnnotationPresent(Email.class))
                .isTrue();
        assertThat(field(UpdateCustomerRequest.class, "phone").getAnnotation(Size.class).max())
                .isEqualTo(50);
        assertThat(field(UpdateCustomerRequest.class, "phone").isAnnotationPresent(Pattern.class))
                .isTrue();
        assertThat(
                        field(UpdateCustomerRequest.class, "addressLine")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(255);
        assertThat(field(UpdateCustomerRequest.class, "city").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(field(UpdateCustomerRequest.class, "country").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(field(UpdateCustomerRequest.class, "source").getAnnotation(Size.class).max())
                .isEqualTo(100);

        assertThat(field(CustomerSearchRequest.class, "term").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(field(CustomerSearchRequest.class, "city").getAnnotation(Size.class).max())
                .isEqualTo(100);
        assertThat(field(CustomerSearchRequest.class, "country").getAnnotation(Size.class).max())
                .isEqualTo(100);
    }

    @Test
    void mapsCreateAndUpdateRequestsToCommands() {
        LocalDate dateOfBirth = LocalDate.parse("1992-05-14");

        CreateCustomerCommand createCommand =
                new CreateCustomerRequest(
                                CustomerType.PROSPECT,
                                "Lena",
                                "Mueller",
                                "lena.mueller@bayer-westphalian.test",
                                "+49-555-0200",
                                "Policy Avenue 8",
                                "Munich",
                                "Germany",
                                dateOfBirth,
                                CustomerAgeGroup.AGE_26_40,
                                null,
                                false,
                                "CSV_IMPORT")
                        .toCommand();
        UpdateCustomerCommand updateCommand =
                new UpdateCustomerRequest(
                                "Lena",
                                "Meyer",
                                "lena.meyer@bayer-westphalian.test",
                                "+49-555-0201",
                                "Customer Street 2",
                                "Berlin",
                                "Germany",
                                dateOfBirth,
                                CustomerAgeGroup.AGE_26_40,
                                CustomerStatus.INTERESTED,
                                true,
                                "LIFE_INSURANCE_BENEFICIARY")
                        .toCommand();

        assertThat(createCommand.customerType()).isEqualTo(CustomerType.PROSPECT);
        assertThat(createCommand.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(createCommand.doNotContact()).isFalse();
        assertThat(createCommand.source()).isEqualTo("CSV_IMPORT");
        assertThat(updateCommand.lastName()).isEqualTo("Meyer");
        assertThat(updateCommand.status()).isEqualTo(CustomerStatus.INTERESTED);
        assertThat(updateCommand.doNotContact()).isTrue();
        assertThat(updateCommand.source()).isEqualTo("LIFE_INSURANCE_BENEFICIARY");
    }

    @Test
    void rejectsCreateCustomerRequestWithoutRequiredKbFields() {
        CreateCustomerRequest request =
                new CreateCustomerRequest(
                        null,
                        " ",
                        null,
                        "ada@bayer-westphalian.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("customerType", "firstName", "lastName");
    }

    @Test
    void rejectsUpdateCustomerRequestWithoutRequiredKbNames() {
        UpdateCustomerRequest request =
                new UpdateCustomerRequest(
                        " ",
                        null,
                        "ada@bayer-westphalian.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("firstName", "lastName");
    }

    @Test
    void rejectsInvalidCustomerEmailAndPhoneFormats() {
        CreateCustomerRequest createRequest =
                new CreateCustomerRequest(
                        CustomerType.CUSTOMER,
                        "Ada",
                        "Policyholder",
                        "not-an-email",
                        "CALL-ME",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null);
        UpdateCustomerRequest updateRequest =
                new UpdateCustomerRequest(
                        "Ada",
                        "Policyholder",
                        "also-not-email",
                        "123",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThat(invalidFields(createRequest)).contains("email", "phone");
        assertThat(invalidFields(updateRequest)).contains("email", "phone");
    }

    @Test
    void normalizesCustomerSearchCriteriaForRepositoryFilters() {
        CustomerSearchCriteria criteria =
                new CustomerSearchRequest(
                                "  beneficiary  ",
                                CustomerType.BENEFICIARY,
                                CustomerStatus.ACTIVE,
                                "  Cologne  ",
                                "  Germany  ",
                                true)
                        .toCriteria();
        CustomerSearchCriteria blankCriteria =
                new CustomerSearchRequest("   ", null, null, " ", " ", null).toCriteria();

        assertThat(criteria.term()).isEqualTo("beneficiary");
        assertThat(criteria.customerType()).isEqualTo(CustomerType.BENEFICIARY);
        assertThat(criteria.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(criteria.city()).isEqualTo("Cologne");
        assertThat(criteria.country()).isEqualTo("Germany");
        assertThat(criteria.contactable()).isTrue();
        assertThat(blankCriteria.term()).isNull();
        assertThat(blankCriteria.city()).isNull();
        assertThat(blankCriteria.country()).isNull();
    }

    @Test
    void mapsCustomerEntityToProfileView() {
        LocalDate dateOfBirth = LocalDate.parse("1984-08-21");
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        customer.updateContactDetails("ada.policyholder@bayer-westphalian.test", "+49-555-0100");
        customer.updateAddress("Insurance Street 1", "Berlin", "Germany");
        customer.updateDemographics(dateOfBirth, CustomerAgeGroup.AGE_41_60);
        customer.changeStatus(CustomerStatus.INTERESTED);
        customer.recordSource("LIFE_INSURANCE_BENEFICIARY");
        customer.markDoNotContact();

        CustomerView view = CustomerView.from(customer);

        assertThat(view.customerType()).isEqualTo(CustomerType.CUSTOMER);
        assertThat(view.firstName()).isEqualTo("Ada");
        assertThat(view.lastName()).isEqualTo("Policyholder");
        assertThat(view.fullName()).isEqualTo("Ada Policyholder");
        assertThat(view.email()).isEqualTo("ada.policyholder@bayer-westphalian.test");
        assertThat(view.phone()).isEqualTo("+49-555-0100");
        assertThat(view.addressLine()).isEqualTo("Insurance Street 1");
        assertThat(view.city()).isEqualTo("Berlin");
        assertThat(view.country()).isEqualTo("Germany");
        assertThat(view.dateOfBirth()).isEqualTo(dateOfBirth);
        assertThat(view.ageGroup()).isEqualTo(CustomerAgeGroup.AGE_41_60);
        assertThat(view.status()).isEqualTo(CustomerStatus.INTERESTED);
        assertThat(view.doNotContact()).isTrue();
        assertThat(view.active()).isFalse();
        assertThat(view.contactable()).isFalse();
        assertThat(view.source()).isEqualTo("LIFE_INSURANCE_BENEFICIARY");
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
