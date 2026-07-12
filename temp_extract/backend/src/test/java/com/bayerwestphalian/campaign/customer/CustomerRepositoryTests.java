package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

class CustomerRepositoryTests {

    @Test
    void extendsJpaRepositoryForCustomerAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(CustomerRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(CustomerRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(Customer.class, UUID.class);
    }

    @Test
    void declaresKbCustomerSearchMethodAcrossProfileFields() throws Exception {
        Method search = CustomerRepository.class.getMethod("search", String.class);
        Query query = search.getAnnotation(Query.class);

        assertThat(search.getGenericReturnType()).isEqualTo(customerList());
        assertThat(search.getParameters()[0].getAnnotation(Param.class).value()).isEqualTo("term");
        assertThat(query.value())
                .contains("customer.deletedAt is null")
                .contains("customer.firstName")
                .contains("customer.lastName")
                .contains("customer.email")
                .contains("customer.city")
                .contains("customer.country")
                .contains("customer.phone")
                .contains("customer.source")
                .contains("order by customer.lastName asc, customer.firstName asc");
    }

    @Test
    void declaresKbStatusCityActiveAndContactableFinders() throws Exception {
        assertThat(
                        CustomerRepository.class
                                .getMethod("findByStatus", CustomerStatus.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod("findByCity", String.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod("findByCountry", String.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod("findByCustomerType", CustomerType.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(CustomerRepository.class.getMethod("findActive").getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod("findByDoNotContactFalse")
                                .getGenericReturnType())
                .isEqualTo(customerList());
    }

    @Test
    void concreteFindersExcludeSoftDeletedCustomersAndUseStableNameOrdering() throws Exception {
        assertThat(
                        CustomerRepository.class
                                .getMethod(
                                        "findByStatusAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                                        CustomerStatus.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod(
                                        "findByCityIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                                        String.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod(
                                        "findByCountryIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                                        String.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod(
                                        "findByCustomerTypeAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                                        CustomerType.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod(
                                        "findByStatusAndDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                                        CustomerStatus.class)
                                .getGenericReturnType())
                .isEqualTo(customerList());
        assertThat(
                        CustomerRepository.class
                                .getMethod(
                                        "findByDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc")
                                .getGenericReturnType())
                .isEqualTo(customerList());
    }

    private static Type customerList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("customerList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<Customer> customerList();
    }
}
