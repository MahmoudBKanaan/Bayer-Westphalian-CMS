package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.common.api.PageResponse;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock private CustomerRepository customerRepository;

    @Mock private AuditService auditService;

    @InjectMocks private CustomerService customerService;

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorize("createCustomer", CreateCustomerCommand.class);
        assertPreAuthorize("updateCustomer", UUID.class, UpdateCustomerCommand.class);
        assertPreAuthorize("softDeleteCustomer", UUID.class);
        assertPreAuthorizeWithExpression("findById", new Class<?>[] {UUID.class}, "@authz.canReadCustomers()");
        assertPreAuthorizeWithExpression(
                "searchCustomers",
                new Class<?>[] {CustomerSearchCriteria.class},
                "@authz.canReadCustomers()");
        assertPreAuthorizeWithExpression(
                "searchCustomers",
                new Class<?>[] {CustomerSearchCriteria.class, int.class, int.class},
                "@authz.canReadCustomers()");
        assertPreAuthorize(
                "importCustomers", org.springframework.web.multipart.MultipartFile.class);
    }

    @Test
    void createsCustomerFromKbProfileCommand() {
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(
                        invocation -> {
                            Customer customer = invocation.getArgument(0);
                            setId(customer, CUSTOMER_ID);
                            return customer;
                        });

        CustomerView view =
                customerService.createCustomer(
                        new CreateCustomerCommand(
                                CustomerType.PROSPECT,
                                "  Lena  ",
                                "  Mueller  ",
                                " lena.mueller@bayer-westphalian.test ",
                                " +49-555-0200 ",
                                " Policy Avenue 8 ",
                                " Munich ",
                                " Germany ",
                                LocalDate.parse("1992-05-14"),
                                CustomerAgeGroup.AGE_26_40,
                                CustomerStatus.INTERESTED,
                                false,
                                " LIFE_INSURANCE_BENEFICIARY "));

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer saved = customerCaptor.getValue();
        assertThat(saved.getCustomerType()).isEqualTo(CustomerType.PROSPECT);
        assertThat(saved.getFullName()).isEqualTo("Lena Mueller");
        assertThat(saved.getEmail()).isEqualTo("lena.mueller@bayer-westphalian.test");
        assertThat(saved.getPhone()).isEqualTo("+49-555-0200");
        assertThat(saved.getAddressLine()).isEqualTo("Policy Avenue 8");
        assertThat(saved.getCity()).isEqualTo("Munich");
        assertThat(saved.getCountry()).isEqualTo("Germany");
        assertThat(saved.getAgeGroup()).isEqualTo(CustomerAgeGroup.AGE_26_40);
        assertThat(saved.getStatus()).isEqualTo(CustomerStatus.INTERESTED);
        assertThat(saved.getSource()).isEqualTo("LIFE_INSURANCE_BENEFICIARY");
        assertThat(view.fullName()).isEqualTo("Lena Mueller");
        verify(auditService)
                .logCreate(
                        eq((UUID) null),
                        eq("customers"),
                        eq(CUSTOMER_ID),
                        eq(
                                Map.ofEntries(
                                        Map.entry("customerType", "PROSPECT"),
                                        Map.entry("firstName", "Lena"),
                                        Map.entry("lastName", "Mueller"),
                                        Map.entry("email", "lena.mueller@bayer-westphalian.test"),
                                        Map.entry("phone", "+49-555-0200"),
                                        Map.entry("city", "Munich"),
                                        Map.entry("country", "Germany"),
                                        Map.entry("ageGroup", "AGE_26_40"),
                                        Map.entry("status", "INTERESTED"),
                                        Map.entry("doNotContact", false),
                                        Map.entry("active", false),
                                        Map.entry("deleted", false),
                                        Map.entry("source", "LIFE_INSURANCE_BENEFICIARY"))));
    }

    @Test
    void validatesCreateCustomerCommand() {
        assertThatThrownBy(() -> customerService.createCustomer(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");
        assertThatThrownBy(
                        () ->
                                customerService.createCustomer(
                                        new CreateCustomerCommand(
                                                null,
                                                " ",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                CustomerStatus.ACTIVE,
                                                false,
                                                null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");
    }

    @Test
    void updatesCustomerProfileStatusAndContactPreference() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerView view =
                customerService.updateCustomer(
                        CUSTOMER_ID,
                        new UpdateCustomerCommand(
                                "Ada",
                                "Client",
                                "ada.client@bayer-westphalian.test",
                                "+49-555-0101",
                                "Customer Street 2",
                                "Berlin",
                                "Germany",
                                LocalDate.parse("1984-08-21"),
                                CustomerAgeGroup.AGE_41_60,
                                CustomerStatus.CONVERTED,
                                true,
                                "CUSTOMER_SERVICE_UPDATE"));

        assertThat(view.fullName()).isEqualTo("Ada Client");
        assertThat(view.status()).isEqualTo(CustomerStatus.CONVERTED);
        assertThat(view.doNotContact()).isTrue();
        assertThat(view.contactable()).isFalse();
        assertThat(view.source()).isEqualTo("CUSTOMER_SERVICE_UPDATE");
        verify(customerRepository).save(customer);
        ArgumentCaptor<Map> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("customers"),
                        eq(CUSTOMER_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(oldValueCaptor.getValue())
                .containsEntry("customerType", "CUSTOMER")
                .containsEntry("status", "ACTIVE")
                .containsEntry("doNotContact", false);
        assertThat(newValueCaptor.getValue())
                .containsEntry("email", "ada.client@bayer-westphalian.test")
                .containsEntry("status", "CONVERTED")
                .containsEntry("doNotContact", true)
                .containsEntry("source", "CUSTOMER_SERVICE_UPDATE");
        verify(auditService)
                .logDoNotContactUpdate(
                        eq((UUID) null),
                        eq(CUSTOMER_ID),
                        eq(Map.of("doNotContact", false)),
                        eq(Map.of("doNotContact", true)));
    }

    @Test
    void doesNotCreateDedicatedDoNotContactAuditWhenPreferenceIsUnchanged() throws Exception {
        Customer customer = customer();
        customer.markDoNotContact();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerView view =
                customerService.updateCustomer(
                        CUSTOMER_ID,
                        new UpdateCustomerCommand(
                                "Ada",
                                "Policyholder",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                CustomerStatus.ACTIVE,
                                true,
                                null));

        assertThat(view.doNotContact()).isTrue();
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("customers"),
                        eq(CUSTOMER_ID),
                        any(Map.class),
                        any(Map.class));
        verify(auditService, never())
                .logDoNotContactUpdate(any(), any(), any(Map.class), any(Map.class));
    }

    @Test
    void softDeletesCustomerAndHidesDeletedRecordsFromLookup() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerView deleted = customerService.softDeleteCustomer(CUSTOMER_ID);

        assertThat(deleted.deletedAt()).isNotNull();
        assertThat(deleted.active()).isFalse();
        ArgumentCaptor<Map> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logDelete(
                        eq((UUID) null),
                        eq("customers"),
                        eq(CUSTOMER_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(oldValueCaptor.getValue()).containsEntry("deleted", false);
        assertThat(newValueCaptor.getValue()).containsEntry("deleted", true);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.findById(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer was not found: " + CUSTOMER_ID);
    }

    @Test
    void searchesCustomersWithKbFiltersOverActiveProfiles() {
        Customer berlinCustomer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        berlinCustomer.updateAddress(null, "Berlin", "Germany");
        berlinCustomer.changeStatus(CustomerStatus.ACTIVE);
        Customer inactiveProspect = Customer.create(CustomerType.PROSPECT, "Ben", "Prospect");
        inactiveProspect.updateAddress(null, "Munich", "Germany");
        inactiveProspect.changeStatus(CustomerStatus.INACTIVE);
        when(customerRepository.findByStatus(CustomerStatus.ACTIVE))
                .thenReturn(List.of(berlinCustomer, inactiveProspect));

        List<CustomerView> views =
                customerService.searchCustomers(
                        new CustomerSearchCriteria(
                                null,
                                CustomerType.CUSTOMER,
                                CustomerStatus.ACTIVE,
                                "berlin",
                                "germany",
                                true));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).fullName()).isEqualTo("Ada Policyholder");
        verify(customerRepository).findByStatus(CustomerStatus.ACTIVE);
    }

    @Test
    void searchAndFiltersReturnOnlyMatchingCustomers() {
        Customer matchingCustomer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        matchingCustomer.updateAddress(null, "Berlin", "Germany");
        matchingCustomer.changeStatus(CustomerStatus.ACTIVE);

        Customer wrongCity = Customer.create(CustomerType.CUSTOMER, "Ada", "Munich");
        wrongCity.updateAddress(null, "Munich", "Germany");
        wrongCity.changeStatus(CustomerStatus.ACTIVE);

        Customer wrongType = Customer.create(CustomerType.PROSPECT, "Ada", "Prospect");
        wrongType.updateAddress(null, "Berlin", "Germany");
        wrongType.changeStatus(CustomerStatus.ACTIVE);

        Customer notContactable = Customer.create(CustomerType.CUSTOMER, "Ada", "Optout");
        notContactable.updateAddress(null, "Berlin", "Germany");
        notContactable.changeStatus(CustomerStatus.ACTIVE);
        notContactable.markDoNotContact();

        when(customerRepository.search("Ada"))
                .thenReturn(List.of(matchingCustomer, wrongCity, wrongType, notContactable));

        List<CustomerView> views =
                customerService.searchCustomers(
                        new CustomerSearchCriteria(
                                " Ada ",
                                CustomerType.CUSTOMER,
                                CustomerStatus.ACTIVE,
                                " berlin ",
                                " germany ",
                                true));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).fullName()).isEqualTo("Ada Policyholder");
        assertThat(views.get(0).customerType()).isEqualTo(CustomerType.CUSTOMER);
        assertThat(views.get(0).status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(views.get(0).city()).isEqualTo("Berlin");
        assertThat(views.get(0).country()).isEqualTo("Germany");
        assertThat(views.get(0).contactable()).isTrue();
        verify(customerRepository).search("Ada");
    }

    @Test
    void returnsPaginatedCustomerSearchResults() {
        Customer first = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        Customer second = Customer.create(CustomerType.CUSTOMER, "Ben", "Policyholder");
        Customer third = Customer.create(CustomerType.CUSTOMER, "Clara", "Policyholder");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(first, second, third));

        PageResponse<CustomerView> page =
                customerService.searchCustomers(
                        new CustomerSearchCriteria(null, null, null, null, null, null), 1, 2);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).fullName()).isEqualTo("Clara Policyholder");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.first()).isFalse();
        assertThat(page.last()).isTrue();
    }

    @Test
    void excludesSoftDeletedCustomersFromActiveLists() {
        Customer activeCustomer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        Customer deletedCustomer = Customer.create(CustomerType.CUSTOMER, "Ben", "Archived");
        deletedCustomer.markDeleted();
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(activeCustomer, deletedCustomer));

        PageResponse<CustomerView> page =
                customerService.searchCustomers(
                        new CustomerSearchCriteria(null, null, null, null, null, null), 0, 10);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).fullName()).isEqualTo("Ada Policyholder");
        assertThat(page.totalElements()).isEqualTo(1);
        verify(customerRepository).findActiveProfiles();
    }

    @Test
    void validatesCustomerPageRequest() {
        assertThatThrownBy(
                        () ->
                                customerService.searchCustomers(
                                        new CustomerSearchCriteria(
                                                null, null, null, null, null, null),
                                        -1,
                                        20))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");
        assertThatThrownBy(
                        () ->
                                customerService.searchCustomers(
                                        new CustomerSearchCriteria(
                                                null, null, null, null, null, null),
                                        0,
                                        101))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");
    }

    @Test
    void importsCustomersAndProspectsFromCsv() {
        String csv =
                String.join(
                        "\n",
                        "customer_type,first_name,last_name,email,phone,address_line,city,country,"
                                + "date_of_birth,age_group,status,do_not_contact,source",
                        "CUSTOMER,Ada,Policyholder,ada@bayer-westphalian.test,+49-555-0100,"
                                + "Insurance Street 1,Berlin,Germany,1984-08-21,41_60,ACTIVE,"
                                + "false,LIFE_INSURANCE_BENEFICIARY",
                        "PROSPECT,Ben,Prospect,ben@bayer-westphalian.test,+49-555-0200,,"
                                + "Munich,Germany,,AGE_26_40,INTERESTED,true,CSV_IMPORT");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "customers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerImportResult result = customerService.importCustomers(file);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        assertThat(result.customers()).hasSize(2);
        assertThat(result.errors()).isEmpty();
        assertThat(result.customers().get(0).fullName()).isEqualTo("Ada Policyholder");
        assertThat(result.customers().get(0).ageGroup()).isEqualTo(CustomerAgeGroup.AGE_41_60);
        assertThat(result.customers().get(1).customerType()).isEqualTo(CustomerType.PROSPECT);
        assertThat(result.customers().get(1).doNotContact()).isTrue();
        verify(customerRepository, times(2)).save(any(Customer.class));
    }

    @Test
    void importsValidCsvRowsAndReturnsRowLevelErrorsForInvalidRows() {
        String csv =
                String.join(
                        "\n",
                        "customer_type,first_name,last_name,email,phone,address_line,city,country,"
                                + "date_of_birth,age_group,status,do_not_contact,source",
                        "CUSTOMER,Ada,Policyholder,ada@bayer-westphalian.test,+49-555-0100,"
                                + "Insurance Street 1,Berlin,Germany,1984-08-21,41_60,ACTIVE,"
                                + "false,LIFE_INSURANCE_BENEFICIARY",
                        "CUSTOMER,,Broken,bad-email,CALLME,,,,not-a-date,UNKNOWN,UNKNOWN,maybe,"
                                + "CSV_IMPORT");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "customers.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerImportResult result = customerService.importCustomers(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.customers()).hasSize(1);
        assertThat(result.errors()).extracting(CustomerImportError::lineNumber).containsOnly(3);
        assertThat(result.errors())
                .extracting(CustomerImportError::field)
                .contains(
                        "first_name",
                        "email",
                        "phone",
                        "date_of_birth",
                        "age_group",
                        "status",
                        "do_not_contact");
        assertThat(result.errors())
                .anySatisfy(
                        error -> {
                            assertThat(error.field()).isEqualTo("email");
                            assertThat(error.message()).isEqualTo("must be a valid email");
                            assertThat(error.value()).isEqualTo("bad-email");
                        });
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void rejectsCustomerCsvImportWithoutRequiredHeaders() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "customers.csv",
                        "text/csv",
                        "first_name,last_name\nAda,Policyholder".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> customerService.importCustomers(file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");
    }

    @Test
    void findsCustomerByIdAndRejectsMissingOrNullId() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThat(customerService.findById(CUSTOMER_ID).fullName()).isEqualTo("Ada Policyholder");

        assertThatThrownBy(() -> customerService.findById(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");

        UUID missingId = UUID.fromString("20000000-0000-0000-0000-000000000099");
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer was not found: " + missingId);
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = CustomerService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = CustomerService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expectedExpression);
    }

    private static Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        java.lang.reflect.Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
