package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.api.PageResponse;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock private CustomerService customerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new CustomerController(customerService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void exposesCustomerApiRoute() {
        assertThat(CustomerController.class.isAnnotationPresent(RestController.class)).isTrue();
        assertThat(CustomerController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/customers");
    }

    @Test
    void searchesCustomersWithKbFilters() throws Exception {
        when(customerService.searchCustomers(any(CustomerSearchCriteria.class), eq(1), eq(10)))
                .thenReturn(PageResponse.of(List.of(customerView()), 1, 10, 21, 3));

        mockMvc.perform(
                        get("/api/customers")
                                .param("term", "ada")
                                .param("customerType", "CUSTOMER")
                                .param("status", "ACTIVE")
                                .param("city", "Berlin")
                                .param("country", "Germany")
                                .param("contactable", "true")
                                .param("page", "1")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customers loaded"))
                .andExpect(jsonPath("$.data.content[0].id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Ada Policyholder"))
                .andExpect(jsonPath("$.data.content[0].contactable").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(21))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        ArgumentCaptor<CustomerSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CustomerSearchCriteria.class);
        verify(customerService).searchCustomers(criteriaCaptor.capture(), eq(1), eq(10));
        assertThat(criteriaCaptor.getValue().term()).isEqualTo("ada");
        assertThat(criteriaCaptor.getValue().customerType()).isEqualTo(CustomerType.CUSTOMER);
        assertThat(criteriaCaptor.getValue().status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(criteriaCaptor.getValue().city()).isEqualTo("Berlin");
        assertThat(criteriaCaptor.getValue().country()).isEqualTo("Germany");
        assertThat(criteriaCaptor.getValue().contactable()).isTrue();
    }

    @Test
    void returnsDefaultPaginatedCustomerList() throws Exception {
        when(customerService.searchCustomers(any(CustomerSearchCriteria.class), eq(0), eq(20)))
                .thenReturn(PageResponse.of(List.of(customerView()), 0, 20, 41, 3));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customers loaded"))
                .andExpect(jsonPath("$.data.content[0].id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(41))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.empty").value(false));

        ArgumentCaptor<CustomerSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(CustomerSearchCriteria.class);
        verify(customerService).searchCustomers(criteriaCaptor.capture(), eq(0), eq(20));
        assertThat(criteriaCaptor.getValue().term()).isNull();
        assertThat(criteriaCaptor.getValue().customerType()).isNull();
        assertThat(criteriaCaptor.getValue().status()).isNull();
        assertThat(criteriaCaptor.getValue().city()).isNull();
        assertThat(criteriaCaptor.getValue().country()).isNull();
        assertThat(criteriaCaptor.getValue().contactable()).isNull();
    }

    @Test
    void getsCustomerById() throws Exception {
        when(customerService.findById(CUSTOMER_ID)).thenReturn(customerView());

        mockMvc.perform(get("/api/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer loaded"))
                .andExpect(jsonPath("$.data.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.email").value("ada@bayer-westphalian.test"));

        verify(customerService).findById(CUSTOMER_ID);
    }

    @Test
    void createsCustomer() throws Exception {
        when(customerService.createCustomer(any(CreateCustomerCommand.class)))
                .thenReturn(customerView());

        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerType": "CUSTOMER",
                                          "firstName": "Ada",
                                          "lastName": "Policyholder",
                                          "email": "ada@bayer-westphalian.test",
                                          "phone": "+49-555-0100",
                                          "addressLine": "Insurance Street 1",
                                          "city": "Berlin",
                                          "country": "Germany",
                                          "dateOfBirth": "1984-08-21",
                                          "ageGroup": "AGE_41_60",
                                          "status": "ACTIVE",
                                          "doNotContact": false,
                                          "source": "LIFE_INSURANCE_BENEFICIARY"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer created"))
                .andExpect(jsonPath("$.data.fullName").value("Ada Policyholder"));

        ArgumentCaptor<CreateCustomerCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateCustomerCommand.class);
        verify(customerService).createCustomer(commandCaptor.capture());
        assertThat(commandCaptor.getValue().customerType()).isEqualTo(CustomerType.CUSTOMER);
        assertThat(commandCaptor.getValue().firstName()).isEqualTo("Ada");
        assertThat(commandCaptor.getValue().ageGroup()).isEqualTo(CustomerAgeGroup.AGE_41_60);
        assertThat(commandCaptor.getValue().source()).isEqualTo("LIFE_INSURANCE_BENEFICIARY");
    }

    @Test
    void importsCustomersFromCsv() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "customers.csv",
                        "text/csv",
                        "customer_type,first_name,last_name\nCUSTOMER,Ada,Policyholder"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(customerService.importCustomers(any()))
                .thenReturn(new CustomerImportResult(1, 0, List.of(customerView()), List.of()));

        mockMvc.perform(multipart("/api/customers/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customers imported"))
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.errors").isEmpty())
                .andExpect(jsonPath("$.data.customers[0].fullName").value("Ada Policyholder"));

        verify(customerService).importCustomers(any());
    }

    @Test
    void returnsCustomerCsvRowLevelImportErrors() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "customers.csv",
                        "text/csv",
                        "customer_type,first_name,last_name\nCUSTOMER,,Policyholder"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(customerService.importCustomers(any()))
                .thenReturn(
                        new CustomerImportResult(
                                0,
                                1,
                                List.of(),
                                List.of(
                                        new CustomerImportError(
                                                2, "first_name", "must not be blank", ""))));

        mockMvc.perform(multipart("/api/customers/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.errors[0].lineNumber").value(2))
                .andExpect(jsonPath("$.data.errors[0].field").value("first_name"))
                .andExpect(jsonPath("$.data.errors[0].message").value("must not be blank"));

        verify(customerService).importCustomers(any());
    }

    @Test
    void rejectsInvalidCreateCustomerRequest() throws Exception {
        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/customers"));
    }

    @Test
    void rejectsInvalidCustomerFormBeforeCreatingCustomer() throws Exception {
        mockMvc.perform(
                        post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "customerType": "CUSTOMER",
                                          "firstName": " ",
                                          "lastName": " ",
                                          "email": "not-an-email",
                                          "phone": "CALLME"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/customers"))
                .andExpect(
                        jsonPath(
                                "$.validationErrors[*].field",
                                hasItems("firstName", "lastName", "email", "phone")));

        verifyNoInteractions(customerService);
    }

    @Test
    void updatesCustomer() throws Exception {
        when(customerService.updateCustomer(any(UUID.class), any(UpdateCustomerCommand.class)))
                .thenReturn(convertedCustomerView());

        mockMvc.perform(
                        put("/api/customers/{id}", CUSTOMER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "firstName": "Ada",
                                          "lastName": "Client",
                                          "email": "ada.client@bayer-westphalian.test",
                                          "phone": "+49-555-0101",
                                          "addressLine": "Customer Street 2",
                                          "city": "Berlin",
                                          "country": "Germany",
                                          "dateOfBirth": "1984-08-21",
                                          "ageGroup": "AGE_41_60",
                                          "status": "CONVERTED",
                                          "doNotContact": true,
                                          "source": "CUSTOMER_SERVICE_UPDATE"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer updated"))
                .andExpect(jsonPath("$.data.lastName").value("Client"))
                .andExpect(jsonPath("$.data.status").value("CONVERTED"))
                .andExpect(jsonPath("$.data.doNotContact").value(true));

        ArgumentCaptor<UpdateCustomerCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateCustomerCommand.class);
        verify(customerService).updateCustomer(eq(CUSTOMER_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().lastName()).isEqualTo("Client");
        assertThat(commandCaptor.getValue().status()).isEqualTo(CustomerStatus.CONVERTED);
        assertThat(commandCaptor.getValue().doNotContact()).isTrue();
    }

    @Test
    void rejectsInvalidUpdateCustomerRequest() throws Exception {
        mockMvc.perform(
                        put("/api/customers/{id}", CUSTOMER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "firstName": " ",
                                          "lastName": "Policyholder"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/customers/" + CUSTOMER_ID))
                .andExpect(jsonPath("$.validationErrors[0].field").value("firstName"));
    }

    @Test
    void softDeletesCustomer() throws Exception {
        when(customerService.softDeleteCustomer(CUSTOMER_ID)).thenReturn(deletedCustomerView());

        mockMvc.perform(delete("/api/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer deleted"))
                .andExpect(jsonPath("$.data.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.deletedAt").exists());

        verify(customerService).softDeleteCustomer(CUSTOMER_ID);
    }

    @Test
    void mapsMissingCustomerToNotFoundResponse() throws Exception {
        when(customerService.findById(CUSTOMER_ID))
                .thenThrow(new ResourceNotFoundException("Customer", CUSTOMER_ID));

        mockMvc.perform(get("/api/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/customers/" + CUSTOMER_ID));
    }

    private static CustomerView customerView() {
        return new CustomerView(
                CUSTOMER_ID,
                CustomerType.CUSTOMER,
                "Ada",
                "Policyholder",
                "Ada Policyholder",
                "ada@bayer-westphalian.test",
                "+49-555-0100",
                "Insurance Street 1",
                "Berlin",
                "Germany",
                LocalDate.parse("1984-08-21"),
                CustomerAgeGroup.AGE_41_60,
                CustomerStatus.ACTIVE,
                false,
                true,
                true,
                "LIFE_INSURANCE_BENEFICIARY",
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-03T12:00:00Z"),
                null);
    }

    private static CustomerView convertedCustomerView() {
        return new CustomerView(
                CUSTOMER_ID,
                CustomerType.CUSTOMER,
                "Ada",
                "Client",
                "Ada Client",
                "ada.client@bayer-westphalian.test",
                "+49-555-0101",
                "Customer Street 2",
                "Berlin",
                "Germany",
                LocalDate.parse("1984-08-21"),
                CustomerAgeGroup.AGE_41_60,
                CustomerStatus.CONVERTED,
                true,
                false,
                false,
                "CUSTOMER_SERVICE_UPDATE",
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-03T12:00:00Z"),
                null);
    }

    private static CustomerView deletedCustomerView() {
        return new CustomerView(
                CUSTOMER_ID,
                CustomerType.CUSTOMER,
                "Ada",
                "Policyholder",
                "Ada Policyholder",
                "ada@bayer-westphalian.test",
                "+49-555-0100",
                "Insurance Street 1",
                "Berlin",
                "Germany",
                LocalDate.parse("1984-08-21"),
                CustomerAgeGroup.AGE_41_60,
                CustomerStatus.ACTIVE,
                false,
                false,
                false,
                "LIFE_INSURANCE_BENEFICIARY",
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-03T12:00:00Z"),
                Instant.parse("2026-07-04T12:00:00Z"));
    }
}
