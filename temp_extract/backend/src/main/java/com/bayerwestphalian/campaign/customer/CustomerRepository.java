package com.bayerwestphalian.campaign.customer;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query(
            """
            select customer
            from Customer customer
            where customer.deletedAt is null
              and (
                lower(customer.firstName) like lower(concat('%', :term, '%'))
                or lower(customer.lastName) like lower(concat('%', :term, '%'))
                or lower(concat(customer.firstName, ' ', customer.lastName))
                    like lower(concat('%', :term, '%'))
                or lower(customer.email) like lower(concat('%', :term, '%'))
                or lower(customer.city) like lower(concat('%', :term, '%'))
                or lower(customer.country) like lower(concat('%', :term, '%'))
                or lower(customer.phone) like lower(concat('%', :term, '%'))
                or lower(customer.source) like lower(concat('%', :term, '%'))
              )
            order by customer.lastName asc, customer.firstName asc
            """)
    List<Customer> search(@Param("term") String term);

    List<Customer> findByStatusAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
            CustomerStatus status);

    List<Customer> findByCityIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
            String city);

    List<Customer> findByCountryIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
            String country);

    List<Customer> findByCustomerTypeAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
            CustomerType customerType);

    List<Customer> findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();

    List<Customer> findByStatusAndDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
            CustomerStatus status);

    List<Customer> findByDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc();

    default List<Customer> findByStatus(CustomerStatus status) {
        return findByStatusAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(status);
    }

    default List<Customer> findByCity(String city) {
        return findByCityIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(city);
    }

    default List<Customer> findByCountry(String country) {
        return findByCountryIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(country);
    }

    default List<Customer> findByCustomerType(CustomerType customerType) {
        return findByCustomerTypeAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(customerType);
    }

    default List<Customer> findActiveProfiles() {
        return findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();
    }

    default List<Customer> findActive() {
        return findByStatusAndDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
                CustomerStatus.ACTIVE);
    }

    default List<Customer> findByDoNotContactFalse() {
        return findByDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc();
    }
}
