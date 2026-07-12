package com.bayerwestphalian.campaign.product;

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
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Product-domain audit evidence (product catalog, ownership, payment, change-request).
 *
 * <p>Item 527 focuses on product catalog mutations ({@code products} entity type). Related domain
 * services also leave audit trails for ownership, payments, and product-change requests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("527 Log product changes")
class ProductChangesCreateAuditLogTests {

    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID REQUEST_ID = UUID.fromString("42000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000527");
    private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-07-15");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductOwnershipRepository productOwnershipRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private ProductChangeRequestRepository productChangeRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private AuditService auditService;
    private ProductService productService;
    private ProductOwnershipService productOwnershipService;
    private PaymentRecordService paymentRecordService;
    private ProductChangeRequestService productChangeRequestService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService =
                new ProductService(productRepository, authorizationExpressions, auditService);
        productOwnershipService =
                new ProductOwnershipService(
                        productOwnershipRepository,
                        productRepository,
                        customerRepository,
                        authorizationExpressions,
                        auditService);
        paymentRecordService =
                new PaymentRecordService(
                        paymentRecordRepository,
                        productOwnershipRepository,
                        customerRepository,
                        auditService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        productChangeRequestService =
                new ProductChangeRequestService(
                        productChangeRequestRepository,
                        productRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Nested
    @DisplayName("Product catalog (item 527)")
    class ProductCatalogAudits {

        @Test
        void productCreationPersistsAuditLog() throws Exception {
            when(authorizationExpressions.isAuthenticated()).thenReturn(true);
            when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
            when(productRepository.save(any(Product.class)))
                    .thenAnswer(
                            invocation -> {
                                Product product = invocation.getArgument(0);
                                setId(product, PRODUCT_ID);
                                return product;
                            });

            productService.createProduct(
                    new CreateProductCommand(
                            "Life Protection",
                            ProductType.LIFE_INSURANCE,
                            "Coverage for beneficiaries",
                            new BigDecimal("129.99"),
                            24,
                            "EXPIRES_AT_TERM_END"));

            AuditLog auditLog = captureSavedAuditLog();
            assertThat(auditLog.getAction()).isEqualTo("CREATE");
            assertThat(auditLog.getEntityType()).isEqualTo(ProductService.AUDIT_ENTITY_TYPE);
            assertThat(auditLog.getEntityType()).isEqualTo("products");
            assertThat(auditLog.getEntityId()).isEqualTo(PRODUCT_ID);
            assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(auditLog.getNewValue())
                    .containsEntry("id", PRODUCT_ID.toString())
                    .containsEntry("name", "Life Protection")
                    .containsEntry("productType", "LIFE_INSURANCE")
                    .containsEntry("description", "Coverage for beneficiaries")
                    .containsEntry("price", new BigDecimal("129.99"))
                    .containsEntry("durationMonths", 24)
                    .containsEntry("expirationPolicy", "EXPIRES_AT_TERM_END")
                    .containsEntry("active", true)
                    .containsEntry("deleted", false);
            assertThat(auditLog.getOldValue()).isNull();
        }

        @Test
        void productUpdatePersistsAuditLogWithOldAndNewValues() throws Exception {
            Product product = product();
            when(authorizationExpressions.isAuthenticated()).thenReturn(true);
            when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            productService.updateProduct(
                    PRODUCT_ID,
                    new UpdateProductCommand(
                            "Life Protection Plus",
                            ProductType.LIFE_INSURANCE,
                            "Expanded coverage",
                            new BigDecimal("149.50"),
                            36,
                            "AUTO_RENEW",
                            false));

            AuditLog auditLog = captureSavedAuditLog();
            assertThat(auditLog.getAction()).isEqualTo("UPDATE");
            assertThat(auditLog.getEntityType()).isEqualTo("products");
            assertThat(auditLog.getEntityId()).isEqualTo(PRODUCT_ID);
            assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(auditLog.getOldValue())
                    .containsEntry("name", "Life Protection")
                    .containsEntry("active", true)
                    .containsEntry("deleted", false);
            assertThat(auditLog.getNewValue())
                    .containsEntry("id", PRODUCT_ID.toString())
                    .containsEntry("name", "Life Protection Plus")
                    .containsEntry("description", "Expanded coverage")
                    .containsEntry("price", new BigDecimal("149.50"))
                    .containsEntry("durationMonths", 36)
                    .containsEntry("expirationPolicy", "AUTO_RENEW")
                    .containsEntry("active", false);
        }

        @Test
        void productDeactivationPersistsAuditLogWithOldAndNewValues() throws Exception {
            Product product = product();
            when(authorizationExpressions.isAuthenticated()).thenReturn(true);
            when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            productService.deactivateProduct(PRODUCT_ID);

            AuditLog auditLog = captureSavedAuditLog();
            assertThat(auditLog.getAction()).isEqualTo("UPDATE");
            assertThat(auditLog.getEntityType()).isEqualTo("products");
            assertThat(auditLog.getEntityId()).isEqualTo(PRODUCT_ID);
            assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(auditLog.getOldValue()).containsEntry("active", true);
            assertThat(auditLog.getNewValue()).containsEntry("active", false);
        }

        @Test
        void productSoftDeletePersistsAuditLog() throws Exception {
            Product product = product();
            when(authorizationExpressions.isAuthenticated()).thenReturn(true);
            when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            productService.softDeleteProduct(PRODUCT_ID);

            AuditLog auditLog = captureSavedAuditLog();
            assertThat(auditLog.getAction()).isEqualTo("DELETE");
            assertThat(auditLog.getEntityType()).isEqualTo("products");
            assertThat(auditLog.getEntityId()).isEqualTo(PRODUCT_ID);
            assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(auditLog.getOldValue()).containsEntry("deleted", false);
            assertThat(auditLog.getNewValue())
                    .containsEntry("deleted", true)
                    .containsEntry("id", PRODUCT_ID.toString());
        }

        @Test
        void productCreateDoesNotWriteAuditWhenValidationFails() {
            assertThatThrownBy(() -> productService.createProduct(null))
                    .isInstanceOf(ValidationException.class);

            verify(productRepository, never()).save(any(Product.class));
            verify(auditLogRepository, never()).save(any(AuditLog.class));
        }

        @Test
        void productUpdateDoesNotWriteAuditWhenProductMissing() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    productService.updateProduct(
                                            PRODUCT_ID,
                                            new UpdateProductCommand(
                                                    "Name",
                                                    ProductType.LIFE_INSURANCE,
                                                    null,
                                                    new BigDecimal("10.00"),
                                                    12,
                                                    null,
                                                    true)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).save(any(Product.class));
            verify(auditLogRepository, never()).save(any(AuditLog.class));
        }
    }

    @Test
    void productOwnershipAssignmentPersistsAuditLog() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Owner");
        Product product = activeProduct();
        LocalDate startDate = LocalDate.parse("2026-01-15");
        LocalDate expirationDate = LocalDate.parse("2027-01-15");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(productOwnershipRepository.save(any(ProductOwnership.class)))
                .thenAnswer(
                        invocation -> {
                            ProductOwnership ownership = invocation.getArgument(0);
                            setOwnershipId(ownership, OWNERSHIP_ID);
                            return ownership;
                        });

        productOwnershipService.assignProduct(
                new CreateProductOwnershipCommand(
                        CUSTOMER_ID, PRODUCT_ID, startDate, expirationDate, "POL-1000"));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("product_ownerships");
        assertThat(auditLog.getEntityId()).isEqualTo(OWNERSHIP_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(USER_ID);
        assertThat(auditLog.getNewValue())
                .containsEntry("customerId", CUSTOMER_ID)
                .containsEntry("productId", PRODUCT_ID)
                .containsEntry("policyNumber", "POL-1000")
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void paymentRecordCreationPersistsAuditLog() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Payer");
        ProductOwnership ownership = ownership(customer);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productOwnershipRepository.findById(OWNERSHIP_ID)).thenReturn(Optional.of(ownership));
        when(paymentRecordRepository.save(any(PaymentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            PaymentRecord payment = invocation.getArgument(0);
                            setPaymentId(payment, PAYMENT_ID);
                            return payment;
                        });

        paymentRecordService.createPaymentRecord(
                new CreatePaymentRecordCommand(
                        CUSTOMER_ID, OWNERSHIP_ID, DUE_DATE, new BigDecimal("129.99")));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("payment_records");
        assertThat(auditLog.getEntityId()).isEqualTo(PAYMENT_ID);
        assertThat(auditLog.getNewValue())
                .containsEntry("customerId", CUSTOMER_ID)
                .containsEntry("productOwnershipId", OWNERSHIP_ID)
                .containsEntry("status", "DUE");
    }

    @Test
    void paymentRecordMarkPaidPersistsAuditLog() throws Exception {
        PaymentRecord payment = duePayment();
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        paymentRecordService.markPaid(
                PAYMENT_ID, new MarkPaymentPaidCommand(new BigDecimal("129.99"), NOW));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("UPDATE");
        assertThat(auditLog.getEntityType()).isEqualTo("payment_records");
        assertThat(auditLog.getEntityId()).isEqualTo(PAYMENT_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "DUE");
        assertThat(auditLog.getNewValue())
                .containsEntry("status", "PAID")
                .containsEntry("amountPaid", new BigDecimal("129.99"));
    }

    @Test
    void productChangeRequestCreationPersistsAuditLog() throws Exception {
        Product product = activeProduct();
        User requester = requester();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(requester));
        when(productChangeRequestRepository.save(any(ProductChangeRequest.class)))
                .thenAnswer(
                        invocation -> {
                            ProductChangeRequest request = invocation.getArgument(0);
                            setId(request, REQUEST_ID);
                            return request;
                        });

        productChangeRequestService.createRequest(
                new CreateProductChangeRequestCommand(
                        PRODUCT_ID,
                        ProductChangeType.PRICE_CHANGE,
                        "Adjust monthly price for the new tariff."));

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("product_change_requests");
        assertThat(auditLog.getEntityId()).isEqualTo(REQUEST_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(USER_ID);
        assertThat(auditLog.getNewValue())
                .containsEntry("productId", PRODUCT_ID)
                .containsEntry("requestType", "PRICE_CHANGE")
                .containsEntry("status", "OPEN")
                .containsEntry("requestedByUserId", USER_ID);
    }

    @Test
    void productChangeRequestApprovalPersistsAuditLog() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        productChangeRequestService.approveRequest(REQUEST_ID);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("APPROVE");
        assertThat(auditLog.getEntityType()).isEqualTo("product_change_requests");
        assertThat(auditLog.getEntityId()).isEqualTo(REQUEST_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "OPEN");
        assertThat(auditLog.getNewValue()).containsEntry("status", "APPROVED");
    }

    @Test
    void productChangeRequestRejectionPersistsAuditLog() throws Exception {
        ProductChangeRequest request = openRequest();
        when(productChangeRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(productChangeRequestRepository.save(request)).thenReturn(request);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(USER_ID);

        productChangeRequestService.rejectRequest(REQUEST_ID);

        AuditLog auditLog = captureSavedAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("REJECT");
        assertThat(auditLog.getEntityType()).isEqualTo("product_change_requests");
        assertThat(auditLog.getEntityId()).isEqualTo(REQUEST_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "OPEN");
        assertThat(auditLog.getNewValue()).containsEntry("status", "REJECTED");
    }

    private AuditLog captureSavedAuditLog() {
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        return auditLogCaptor.getValue();
    }

    private static Product product() throws Exception {
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        product.updateDetails(
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                "Coverage for beneficiaries",
                24,
                "EXPIRES_AT_TERM_END");
        setId(product, PRODUCT_ID);
        return product;
    }

    private static Product activeProduct() throws Exception {
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        setId(product, PRODUCT_ID);
        return product;
    }

    private static Customer customer(UUID id, String firstName, String lastName) throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, firstName, lastName);
        setId(customer, id);
        return customer;
    }

    private static User requester() throws Exception {
        User user =
                User.create("product.manager@bayer-westphalian.test", "hash", "Product Manager");
        setId(user, USER_ID);
        return user;
    }

    private static ProductOwnership ownership(Customer customer) throws Exception {
        Product product = activeProduct();
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.parse("2026-01-15"),
                        LocalDate.parse("2027-01-15"));
        setOwnershipId(ownership, OWNERSHIP_ID);
        return ownership;
    }

    private PaymentRecord duePayment() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Payer");
        ProductOwnership ownership = ownership(customer);
        PaymentRecord payment =
                PaymentRecord.create(customer, ownership, DUE_DATE, new BigDecimal("129.99"));
        setPaymentId(payment, PAYMENT_ID);
        return payment;
    }

    private static ProductChangeRequest openRequest() throws Exception {
        ProductChangeRequest request =
                ProductChangeRequest.create(
                        activeProduct(),
                        requester(),
                        ProductChangeType.PRICE_CHANGE,
                        "Adjust monthly price for the new tariff.");
        setId(request, REQUEST_ID);
        return request;
    }

    private static void setId(Product product, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product, id);
    }

    private static void setId(Customer customer, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
    }

    private static void setId(User user, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }

    private static void setId(ProductChangeRequest request, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(request, id);
    }

    private static void setOwnershipId(ProductOwnership ownership, UUID id) throws Exception {
        Field idField = ProductOwnership.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(ownership, id);
    }

    private static void setPaymentId(PaymentRecord payment, UUID id) throws Exception {
        Field idField = PaymentRecord.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(payment, id);
    }
}
