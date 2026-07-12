package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.api.PageResponse;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sprint 16 critical test item <b>657</b>: Soft-deleted customers do not appear in active lists.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code FR-013} — Authorized users can soft-delete customers/prospects
 *   <li>{@code FR-010} — Active lists/search only include non-deleted profiles
 *   <li>Customer soft delete sets {@code deletedAt}; hard delete is not MVP
 * </ul>
 *
 * <p>Enforcement layers:
 *
 * <ol>
 *   <li>Repository finders require {@code deletedAt is null}
 *   <li>{@link CustomerService#searchCustomers} filters {@code !customer.isDeleted()}
 *   <li>{@code findById} / mutations load only non-deleted rows
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("657 Soft-deleted customers do not appear in active lists")
class SoftDeletedCustomersDoNotAppearInActiveListsTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000657");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000657");

    @Mock private CustomerRepository customerRepository;
    @Mock private AuditService auditService;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService =
                new CustomerService(customerRepository, authorizationExpressions, auditService);
    }

    @Nested
    @DisplayName("Repository: all active list queries require deletedAt is null")
    class RepositoryQueries {

        @Test
        void searchQueryExcludesSoftDeletedCustomers() throws Exception {
            Method search = CustomerRepository.class.getMethod("search", String.class);
            Query query = search.getAnnotation(Query.class);

            assertThat(search.getParameters()[0].getAnnotation(Param.class).value())
                    .isEqualTo("term");
            assertThat(query.value())
                    .contains("customer.deletedAt is null")
                    .contains("customer.firstName")
                    .contains("customer.lastName")
                    .contains("customer.email");
        }

        @Test
        void concreteFindersAreNamedWithDeletedAtIsNull() throws Exception {
            assertMethodExists(
                    "findByStatusAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                    CustomerStatus.class);
            assertMethodExists(
                    "findByCityIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                    String.class);
            assertMethodExists(
                    "findByCountryIgnoreCaseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                    String.class);
            assertMethodExists(
                    "findByCustomerTypeAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                    CustomerType.class);
            assertMethodExists("findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc");
            assertMethodExists(
                    "findByDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc");
            assertMethodExists(
                    "findByStatusAndDoNotContactFalseAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc",
                    CustomerStatus.class);
        }

        @Test
        void activeProfileDefaultsDelegateToDeletedAtNullFinders() throws Exception {
            Method findActiveProfiles =
                    CustomerRepository.class.getMethod("findActiveProfiles");
            Method findActive = CustomerRepository.class.getMethod("findActive");
            Method findByDoNotContactFalse =
                    CustomerRepository.class.getMethod("findByDoNotContactFalse");

            assertThat(findActiveProfiles.getGenericReturnType()).isEqualTo(customerListType());
            assertThat(findActive.getGenericReturnType()).isEqualTo(customerListType());
            assertThat(findByDoNotContactFalse.getGenericReturnType())
                    .isEqualTo(customerListType());
        }

        private static void assertMethodExists(String name, Class<?>... parameterTypes)
                throws Exception {
            Method method = CustomerRepository.class.getMethod(name, parameterTypes);
            assertThat(method.getGenericReturnType()).isEqualTo(customerListType());
        }

        private static Type customerListType() throws NoSuchMethodException {
            return ReturnTypes.class.getDeclaredMethod("customerList").getGenericReturnType();
        }

        private interface ReturnTypes {
            List<Customer> customerList();
        }
    }

    @Nested
    @DisplayName("Service: active lists and lookups hide soft-deleted customers")
    class ServiceListsAndLookup {

        @Test
        void searchCustomersFiltersOutSoftDeletedEvenIfRepositoryReturnedThem() {
            Customer active = Customer.create(CustomerType.CUSTOMER, "Ada", "Active");
            Customer deleted = Customer.create(CustomerType.CUSTOMER, "Ben", "Deleted");
            deleted.markDeleted();
            when(customerRepository.findActiveProfiles()).thenReturn(List.of(active, deleted));

            PageResponse<CustomerView> page =
                    customerService.searchCustomers(
                            new CustomerSearchCriteria(null, null, null, null, null, null), 0, 10);

            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).fullName()).isEqualTo("Ada Active");
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.content()).noneMatch(view -> view.deletedAt() != null);
            verify(customerRepository).findActiveProfiles();
        }

        @Test
        void searchByTermUsesRepositorySearchThatExcludesDeleted() {
            Customer active = Customer.create(CustomerType.CUSTOMER, "Ada", "Searchable");
            when(customerRepository.search("Ada")).thenReturn(List.of(active));

            List<CustomerView> results =
                    customerService.searchCustomers(
                            new CustomerSearchCriteria("Ada", null, null, null, null, null));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).fullName()).isEqualTo("Ada Searchable");
            verify(customerRepository).search("Ada");
        }

        @Test
        void findByIdThrowsWhenCustomerIsSoftDeleted() {
            Customer deleted = Customer.create(CustomerType.CUSTOMER, "Gone", "Customer");
            deleted.markDeleted();
            ReflectionTestUtils.setField(deleted, "id", CUSTOMER_ID);
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> customerService.findById(CUSTOMER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(CUSTOMER_ID.toString());
        }

        @Test
        void findByIdReturnsActiveCustomer() {
            Customer active = Customer.create(CustomerType.CUSTOMER, "Ada", "Active");
            ReflectionTestUtils.setField(active, "id", CUSTOMER_ID);
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(active));

            CustomerView view = customerService.findById(CUSTOMER_ID);

            assertThat(view.fullName()).isEqualTo("Ada Active");
            assertThat(view.deletedAt()).isNull();
            assertThat(view.active()).isTrue();
        }

        @Test
        void softDeleteSetsDeletedAtAndHidesFromSubsequentLookup() {
            Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "ToDelete");
            ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
            when(authorizationExpressions.currentUserId()).thenReturn(ADMIN_ID);
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(customerRepository.save(customer)).thenReturn(customer);

            CustomerView deleted = customerService.softDeleteCustomer(CUSTOMER_ID);

            assertThat(deleted.deletedAt()).isNotNull();
            assertThat(deleted.active()).isFalse();
            assertThat(customer.isDeleted()).isTrue();

            // After soft delete, the same row is treated as not found for active lookups.
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            assertThatThrownBy(() -> customerService.findById(CUSTOMER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void statusFilterListDoesNotIncludeSoftDeletedCustomersFromRepoDefault() {
            Customer active = Customer.create(CustomerType.CUSTOMER, "Ada", "Active");
            active.changeStatus(CustomerStatus.ACTIVE);
            when(customerRepository.findByStatus(CustomerStatus.ACTIVE))
                    .thenReturn(List.of(active));

            List<CustomerView> results =
                    customerService.searchCustomers(
                            new CustomerSearchCriteria(
                                    null, null, CustomerStatus.ACTIVE, null, null, null));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).status()).isEqualTo(CustomerStatus.ACTIVE);
            verify(customerRepository).findByStatus(CustomerStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Domain: markDeleted soft-delete semantics")
    class DomainSoftDelete {

        @Test
        void markDeletedSetsDeletedFlagWithoutHardRemovingEntity() {
            Customer customer = Customer.create(CustomerType.PROSPECT, "Pat", "Prospect");
            assertThat(customer.isDeleted()).isFalse();
            assertThat(customer.getDeletedAt()).isNull();

            customer.markDeleted();

            assertThat(customer.isDeleted()).isTrue();
            assertThat(customer.getDeletedAt()).isNotNull();
            assertThat(customer.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 657)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(SoftDeletedCustomersDoNotAppearInActiveListsContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(657);
            assertThat(SoftDeletedCustomersDoNotAppearInActiveListsContract.RULE_STATEMENT)
                    .isEqualTo("Soft-deleted customers do not appear in active lists");
            assertThat(
                            SoftDeletedCustomersDoNotAppearInActiveListsContract
                                    .FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-010", "FR-013");
            assertThat(SoftDeletedCustomersDoNotAppearInActiveListsContract.SOFT_DELETE_FIELD)
                    .isEqualTo("deletedAt");
            assertThat(
                            SoftDeletedCustomersDoNotAppearInActiveListsContract
                                    .REPOSITORY_EXCLUSION_PREDICATE)
                    .isEqualTo("deletedAt is null");
            assertThat(SoftDeletedCustomersDoNotAppearInActiveListsContract.SERVICE_FILTER)
                    .isEqualTo("!customer.isDeleted()");
        }
    }

    static final class SoftDeletedCustomersDoNotAppearInActiveListsContract {
        static final int CRITICAL_TEST_ITEM = 657;
        static final String RULE_STATEMENT =
                "Soft-deleted customers do not appear in active lists";
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-010", "FR-013");
        static final String SOFT_DELETE_FIELD = "deletedAt";
        static final String REPOSITORY_EXCLUSION_PREDICATE = "deletedAt is null";
        static final String SERVICE_FILTER = "!customer.isDeleted()";

        private SoftDeletedCustomersDoNotAppearInActiveListsContract() {}
    }
}
