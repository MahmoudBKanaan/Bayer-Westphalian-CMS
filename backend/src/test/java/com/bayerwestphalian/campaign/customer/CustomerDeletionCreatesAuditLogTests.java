package com.bayerwestphalian.campaign.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
 * KB item 523: Admin customer soft deletion writes an immutable {@code DELETE} audit log on entity
 * type {@code customers} (SEC-012 / FR-013). Permanent row removal is not part of the MVP.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("523 Log customer deletion")
class CustomerDeletionCreatesAuditLogTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000523");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

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
    void softDeleteCustomerPersistsDeleteAuditWithActorAndDeletedFlagTransition()
            throws Exception {
        Customer customer = customer();
        when(authorizationExpressions.currentUserId()).thenReturn(ADMIN_ID);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerView view = customerService.softDeleteCustomer(CUSTOMER_ID);

        assertThat(view.deletedAt()).isNotNull();
        assertThat(view.active()).isFalse();
        assertThat(customer.isDeleted()).isTrue();

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("DELETE");
        assertThat(auditLog.getEntityType()).isEqualTo(CustomerService.AUDIT_ENTITY_TYPE);
        assertThat(auditLog.getEntityType()).isEqualTo("customers");
        assertThat(auditLog.getEntityId()).isEqualTo(CUSTOMER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ADMIN_ID);
        assertThat(auditLog.getOldValue())
                .containsEntry("id", CUSTOMER_ID.toString())
                .containsEntry("firstName", "Ada")
                .containsEntry("lastName", "Policyholder")
                .containsEntry("email", "ada@bayer-westphalian.test")
                .containsEntry("customerType", "CUSTOMER")
                .containsEntry("status", "ACTIVE")
                .containsEntry("deleted", false)
                .containsEntry("active", true)
                .doesNotContainKey("password");
        assertThat(auditLog.getNewValue())
                .containsEntry("id", CUSTOMER_ID.toString())
                .containsEntry("deleted", true)
                .containsEntry("active", false)
                .containsKey("deletedAt")
                .containsEntry("email", "ada@bayer-westphalian.test");
        assertThat(auditLog.getNewValue().get("deletedAt")).isNotNull();
    }

    @Test
    void softDeleteDoesNotWriteAuditWhenCustomerAlreadyDeleted() throws Exception {
        Customer customer = customer();
        customer.markDeleted();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.softDeleteCustomer(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");

        verify(customerRepository, never()).save(any(Customer.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void softDeleteDoesNotWriteAuditWhenCustomerMissing() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.softDeleteCustomer(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customerRepository, never()).save(any(Customer.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void softDeleteDoesNotWriteAuditWhenCustomerIdNull() {
        assertThatThrownBy(() -> customerService.softDeleteCustomer(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Customer validation failed");

        verify(customerRepository, never()).save(any(Customer.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void softDeleteStillAuditsWhenActorPrincipalIsUnavailable() throws Exception {
        Customer customer = customer();
        when(authorizationExpressions.currentUserId())
                .thenThrow(new RuntimeException("no principal in unit test"));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        customerService.softDeleteCustomer(CUSTOMER_ID);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("DELETE");
        assertThat(auditLog.getEntityType()).isEqualTo("customers");
        assertThat(auditLog.getEntityId()).isEqualTo(CUSTOMER_ID);
        assertThat(auditLog.getActorUserId()).isNull();
        assertThat(auditLog.getOldValue()).containsEntry("deleted", false);
        assertThat(auditLog.getNewValue()).containsEntry("deleted", true);
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        customer.updateContactDetails("ada@bayer-westphalian.test", "+49-555-0100");
        customer.updateAddress(null, "Berlin", "Germany");
        customer.changeStatus(CustomerStatus.ACTIVE);
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
