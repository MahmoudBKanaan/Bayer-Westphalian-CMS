package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 526: do-not-contact preference changes write an immutable {@code
 * UPDATE_DO_NOT_CONTACT} audit row on entity type {@code customers} (BR-001 / COMP-003 / SEC-012).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("526 Log do-not-contact changes")
class DoNotContactChangeCreatesAuditLogTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000526");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000526");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        AuditService auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        customerService =
                new CustomerService(customerRepository, authorizationExpressions, auditService);
    }

    @Test
    void updateCustomerEnablingDoNotContactWritesUpdateAndDncAudits() throws Exception {
        Customer customer = customer(false);
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerView view =
                customerService.updateCustomer(
                        CUSTOMER_ID,
                        new UpdateCustomerCommand(
                                "Ada",
                                "Policyholder",
                                "ada@bayer-westphalian.test",
                                "+49-555-0100",
                                null,
                                "Berlin",
                                "Germany",
                                LocalDate.parse("1984-08-21"),
                                CustomerAgeGroup.AGE_41_60,
                                CustomerStatus.ACTIVE,
                                true,
                                "CUSTOMER_REQUEST"));

        assertThat(view.doNotContact()).isTrue();
        assertThat(view.contactable()).isFalse();

        List<AuditLog> auditLogs = captureSavedAuditLogs(2);
        AuditLog profileUpdate =
                auditLogs.stream()
                        .filter(log -> "UPDATE".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();
        AuditLog dncUpdate =
                auditLogs.stream()
                        .filter(log -> "UPDATE_DO_NOT_CONTACT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();

        assertThat(profileUpdate.getEntityType()).isEqualTo(CustomerService.AUDIT_ENTITY_TYPE);
        assertThat(profileUpdate.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(profileUpdate.getNewValue()).containsEntry("doNotContact", true);

        assertThat(dncUpdate.getEntityType()).isEqualTo("customers");
        assertThat(dncUpdate.getEntityId()).isEqualTo(CUSTOMER_ID);
        assertThat(dncUpdate.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(dncUpdate.getOldValue())
                .containsEntry("id", CUSTOMER_ID.toString())
                .containsEntry("firstName", "Ada")
                .containsEntry("lastName", "Policyholder")
                .containsEntry("email", "ada@bayer-westphalian.test")
                .containsEntry("doNotContact", false);
        assertThat(dncUpdate.getNewValue())
                .containsEntry("doNotContact", true)
                .containsEntry("email", "ada@bayer-westphalian.test")
                .doesNotContainKey("password");
    }

    @Test
    void updateCustomerClearingDoNotContactWritesDncAudit() throws Exception {
        Customer customer = customer(true);
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        customerService.updateCustomer(
                CUSTOMER_ID,
                new UpdateCustomerCommand(
                        "Ada",
                        "Policyholder",
                        "ada@bayer-westphalian.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        CustomerStatus.ACTIVE,
                        false,
                        null));

        List<AuditLog> auditLogs = captureSavedAuditLogs(2);
        AuditLog dncUpdate =
                auditLogs.stream()
                        .filter(log -> "UPDATE_DO_NOT_CONTACT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();
        assertThat(dncUpdate.getOldValue()).containsEntry("doNotContact", true);
        assertThat(dncUpdate.getNewValue()).containsEntry("doNotContact", false);
    }

    @Test
    void updateCustomerUnchangedDoNotContactDoesNotWriteDncAudit() throws Exception {
        Customer customer = customer(true);
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        customerService.updateCustomer(
                CUSTOMER_ID,
                new UpdateCustomerCommand(
                        "Ada",
                        "Policyholder",
                        "ada@bayer-westphalian.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        CustomerStatus.ACTIVE,
                        true,
                        null));

        List<AuditLog> auditLogs = captureSavedAuditLogs(1);
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getAction()).isEqualTo("UPDATE");
        assertThat(auditLogs).noneMatch(log -> "UPDATE_DO_NOT_CONTACT".equals(log.getAction()));
    }

    @Test
    void createCustomerWithDoNotContactWritesCreateAndDncAudits() {
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(
                        invocation -> {
                            Customer customer = invocation.getArgument(0);
                            try {
                                setId(customer, CUSTOMER_ID);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return customer;
                        });

        CustomerView view =
                customerService.createCustomer(
                        new CreateCustomerCommand(
                                CustomerType.PROSPECT,
                                "Lena",
                                "Mueller",
                                "lena@bayer-westphalian.test",
                                null,
                                null,
                                "Munich",
                                "Germany",
                                null,
                                null,
                                CustomerStatus.ACTIVE,
                                true,
                                "WEB"));

        assertThat(view.doNotContact()).isTrue();

        List<AuditLog> auditLogs = captureSavedAuditLogs(2);
        assertThat(auditLogs)
                .extracting(AuditLog::getAction)
                .containsExactlyInAnyOrder("CREATE", "UPDATE_DO_NOT_CONTACT");
        AuditLog dnc =
                auditLogs.stream()
                        .filter(log -> "UPDATE_DO_NOT_CONTACT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();
        assertThat(dnc.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(dnc.getEntityId()).isEqualTo(CUSTOMER_ID);
        assertThat(dnc.getOldValue()).containsEntry("doNotContact", false);
        assertThat(dnc.getNewValue()).containsEntry("doNotContact", true);
    }

    @Test
    void createCustomerWithoutDoNotContactDoesNotWriteDncAudit() {
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(
                        invocation -> {
                            Customer customer = invocation.getArgument(0);
                            try {
                                setId(customer, CUSTOMER_ID);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return customer;
                        });

        customerService.createCustomer(
                new CreateCustomerCommand(
                        CustomerType.CUSTOMER,
                        "Ada",
                        "Policyholder",
                        "ada@bayer-westphalian.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        CustomerStatus.ACTIVE,
                        false,
                        null));

        List<AuditLog> auditLogs = captureSavedAuditLogs(1);
        assertThat(auditLogs.getFirst().getAction()).isEqualTo("CREATE");
        assertThat(auditLogs).noneMatch(log -> "UPDATE_DO_NOT_CONTACT".equals(log.getAction()));
    }

    @Test
    void updateCustomerDoesNotWriteDncAuditWhenCustomerMissing() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
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
                                                null,
                                                true,
                                                null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void updateCustomerDoesNotWriteDncAuditWhenCommandInvalid() {
        assertThatThrownBy(() -> customerService.updateCustomer(CUSTOMER_ID, null))
                .isInstanceOf(ValidationException.class);

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    private List<AuditLog> captureSavedAuditLogs(int expectedCount) {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private static Customer customer(boolean doNotContact) throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        customer.updateContactDetails("ada@bayer-westphalian.test", "+49-555-0100");
        customer.updateAddress(null, "Berlin", "Germany");
        customer.changeStatus(CustomerStatus.ACTIVE);
        if (doNotContact) {
            customer.markDoNotContact();
        }
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
