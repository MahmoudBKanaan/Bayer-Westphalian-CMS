package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class ProductOwnershipServiceTests {

    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final UUID ACTOR_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");

    @Mock private ProductOwnershipRepository productOwnershipRepository;

    @Mock private ProductRepository productRepository;

    @Mock private CustomerRepository customerRepository;

    @Mock private AuthorizationExpressions authorizationExpressions;

    @Mock private AuditService auditService;

    private ProductOwnershipService productOwnershipService;

    @BeforeEach
    void setUp() {
        productOwnershipService =
                new ProductOwnershipService(
                        productOwnershipRepository,
                        productRepository,
                        customerRepository,
                        authorizationExpressions,
                        auditService);
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "assignProduct",
                new Class<?>[] {CreateProductOwnershipCommand.class},
                "@authz.canManageProducts()");
        assertPreAuthorize("updateOwnership", UUID.class, UpdateProductOwnershipCommand.class);
        assertPreAuthorize("findExpiringWithinMonths", int.class);
        assertPreAuthorize("listCustomerProducts", UUID.class);
    }

    @Test
    void productCanBeAssignedToCustomer() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Owner");
        Product product = activeProduct();
        LocalDate startDate = LocalDate.parse("2026-01-15");
        LocalDate expirationDate = LocalDate.parse("2027-01-15");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(productOwnershipRepository.save(any(ProductOwnership.class)))
                .thenAnswer(
                        invocation -> {
                            ProductOwnership ownership = invocation.getArgument(0);
                            setOwnershipId(ownership, OWNERSHIP_ID);
                            return ownership;
                        });

        ProductOwnershipView view =
                productOwnershipService.assignProduct(
                        new CreateProductOwnershipCommand(
                                CUSTOMER_ID,
                                PRODUCT_ID,
                                startDate,
                                expirationDate,
                                " POL-1000 "));

        ArgumentCaptor<ProductOwnership> ownershipCaptor =
                ArgumentCaptor.forClass(ProductOwnership.class);
        verify(productOwnershipRepository).save(ownershipCaptor.capture());
        ProductOwnership saved = ownershipCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getStartDate()).isEqualTo(startDate);
        assertThat(saved.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(saved.getPolicyNumber()).isEqualTo("POL-1000");
        assertThat(saved.getStatus()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Owner");
        assertThat(view.productId()).isEqualTo(PRODUCT_ID);
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.policyNumber()).isEqualTo("POL-1000");
        assertThat(view.startDate()).isEqualTo(startDate);
        assertThat(view.expirationDate()).isEqualTo(expirationDate);
        assertThat(view.active()).isTrue();
        verify(auditService)
                .logCreate(
                        eq(ACTOR_ID),
                        eq("product_ownerships"),
                        eq(OWNERSHIP_ID),
                        eq(
                                Map.of(
                                        "customerId",
                                        CUSTOMER_ID,
                                        "productId",
                                        PRODUCT_ID,
                                        "policyNumber",
                                        "POL-1000",
                                        "startDate",
                                        startDate,
                                        "expirationDate",
                                        expirationDate,
                                        "status",
                                        "ACTIVE")));
    }

    @Test
    void rejectsInvalidAssignCommandsAndInactiveProducts() throws Exception {
        Product inactiveProduct = activeProduct();
        inactiveProduct.deactivate();

        assertThatThrownBy(() -> productOwnershipService.assignProduct(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product ownership validation failed");

        assertThatThrownBy(
                        () ->
                                productOwnershipService.assignProduct(
                                        new CreateProductOwnershipCommand(
                                                null,
                                                null,
                                                null,
                                                LocalDate.parse("2026-01-01"),
                                                "X".repeat(101))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product ownership validation failed");

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(inactiveProduct));
        assertThatThrownBy(
                        () ->
                                productOwnershipService.assignProduct(
                                        new CreateProductOwnershipCommand(
                                                CUSTOMER_ID,
                                                PRODUCT_ID,
                                                LocalDate.parse("2026-01-15"),
                                                LocalDate.parse("2027-01-15"),
                                                null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product ownership validation failed");
    }

    @Test
    void rejectsSoftDeletedCustomersWhenAssigningProduct() throws Exception {
        Customer deletedCustomer = customer(CUSTOMER_ID, "Ada", "Owner");
        deletedCustomer.markDeleted();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct()));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(deletedCustomer));

        assertThatThrownBy(
                        () ->
                                productOwnershipService.assignProduct(
                                        new CreateProductOwnershipCommand(
                                                CUSTOMER_ID,
                                                PRODUCT_ID,
                                                LocalDate.parse("2026-01-15"),
                                                LocalDate.parse("2027-01-15"),
                                                null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer was not found: " + CUSTOMER_ID);
    }

    @Test
    void updatesOwnershipExpirationAndPolicyNumber() throws Exception {
        ProductOwnership ownership = ownership();
        when(productOwnershipRepository.findById(OWNERSHIP_ID)).thenReturn(Optional.of(ownership));
        when(productOwnershipRepository.save(ownership)).thenReturn(ownership);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);

        ProductOwnershipView view =
                productOwnershipService.updateOwnership(
                        OWNERSHIP_ID,
                        new UpdateProductOwnershipCommand(
                                LocalDate.parse("2028-06-30"), " POL-1000-REVISED "));

        assertThat(view.expirationDate()).isEqualTo(LocalDate.parse("2028-06-30"));
        assertThat(view.policyNumber()).isEqualTo("POL-1000-REVISED");
        verify(productOwnershipRepository).save(ownership);
        verify(auditService)
                .logUpdate(
                        eq(ACTOR_ID),
                        eq("product_ownerships"),
                        eq(OWNERSHIP_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void rejectsUpdateWithoutOwnershipFields() {
        assertThatThrownBy(
                        () ->
                                productOwnershipService.updateOwnership(
                                        OWNERSHIP_ID, new UpdateProductOwnershipCommand(null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product ownership validation failed");
    }

    @Test
    void findsOwnershipsExpiringWithinMonths() throws Exception {
        ProductOwnership expiringSoon = ownership();
        ProductOwnership outsideWindow = ownershipOutsideWindow();
        LocalDate today = LocalDate.now();
        when(productOwnershipRepository.findExpiringBetween(today, today.plusMonths(3)))
                .thenReturn(List.of(expiringSoon, outsideWindow));

        List<ProductOwnershipView> views =
                productOwnershipService.findExpiringWithinMonths(3);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).customerFullName()).isEqualTo("Ada Owner");
        assertThat(views.get(0).productName()).isEqualTo("Life Protection");
        verify(productOwnershipRepository).findExpiringBetween(today, today.plusMonths(3));
    }

    @Test
    void listsCustomerProductsInRepositoryOrder() throws Exception {
        ProductOwnership first = ownership();
        ProductOwnership second = ownershipOutsideWindow();
        when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(first, second));

        List<ProductOwnershipView> views =
                productOwnershipService.listCustomerProducts(CUSTOMER_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(views.get(1).customerId()).isEqualTo(CUSTOMER_ID);
        verify(productOwnershipRepository).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void rejectsMissingOwnershipOrInvalidMonthInputs() throws Exception {
        UUID missingId = UUID.fromString("41000000-0000-0000-0000-000000000099");
        when(productOwnershipRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productOwnershipService.listCustomerProducts(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product ownership validation failed");

        assertThatThrownBy(() -> productOwnershipService.findExpiringWithinMonths(-1))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Product ownership validation failed");

        assertThatThrownBy(
                        () ->
                                productOwnershipService.updateOwnership(
                                        missingId,
                                        new UpdateProductOwnershipCommand(
                                                LocalDate.parse("2028-01-01"), "POL-2000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product ownership was not found: " + missingId);
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = ProductOwnershipService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = ProductOwnershipService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expectedExpression);
    }

    private static Customer customer(UUID id, String firstName, String lastName) throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, firstName, lastName);
        setId(customer, id);
        return customer;
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

    private static ProductOwnership ownership() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Owner");
        Product product = activeProduct();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, startDate, startDate.plusMonths(2));
        ownership.recordPolicyNumber("POL-ACTIVE-001");
        setOwnershipId(ownership, OWNERSHIP_ID);
        return ownership;
    }

    private static ProductOwnership ownershipOutsideWindow() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Owner");
        Product product = activeProduct();
        LocalDate startDate = LocalDate.now().minusMonths(1);
        return ProductOwnership.create(customer, product, startDate, startDate.plusMonths(6));
    }

    private static void setId(Customer customer, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
    }

    private static void setId(Product product, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product, id);
    }

    private static void setOwnershipId(ProductOwnership ownership, UUID id) throws Exception {
        Field idField = ProductOwnership.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(ownership, id);
    }
}