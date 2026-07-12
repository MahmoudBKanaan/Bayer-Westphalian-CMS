package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ProductRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_product_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private ProductRepository productRepository;

    @Autowired private ProductOwnershipRepository ownershipRepository;

    @Autowired private ProductChangeRequestRepository changeRequestRepository;

    @Autowired private PaymentRecordRepository paymentRecordRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void productRepositoryFindsOnlyActiveProductsOrderedByName() {
        Product inactive = persistProduct("Dormant Health Cover", ProductType.HEALTH_INSURANCE);
        inactive.deactivate();
        entityManager.persistAndFlush(inactive);

        Product deleted = persistProduct("Archived Auto Cover", ProductType.AUTO_INSURANCE);
        deleted.softDelete();
        entityManager.persistAndFlush(deleted);

        Product beta = persistProduct("Beta Life Cover", ProductType.LIFE_INSURANCE);
        Product alpha = persistProduct("Alpha Home Cover", ProductType.HOMEOWNER_INSURANCE);

        assertThat(productRepository.findActive()).extracting(Product::getId)
                .containsExactly(alpha.getId(), beta.getId());
    }

    @Test
    void productRepositoryFindsProductsByTypeAndExcludesSoftDeletedRows() {
        Product included = persistProduct("Family Life Cover", ProductType.LIFE_INSURANCE);
        Product deleted = persistProduct("Retired Life Cover", ProductType.LIFE_INSURANCE);
        deleted.softDelete();
        entityManager.persistAndFlush(deleted);
        persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);

        assertThat(productRepository.findByType(ProductType.LIFE_INSURANCE))
                .extracting(Product::getId)
                .containsExactly(included.getId());
    }

    @Test
    void productRepositorySearchesNameAndDescriptionAndExcludesSoftDeletedRows() {
        Product byName = persistProduct("Flex Life Policy", ProductType.LIFE_INSURANCE);
        Product byDescription =
                persistProductWithDescription(
                        "Education Builder",
                        ProductType.INVESTMENT_FUND,
                        "Long-term life planning product");
        Product deleted =
                persistProductWithDescription(
                        "Archived Life Policy",
                        ProductType.LIFE_INSURANCE,
                        "Should stay out of search results");
        deleted.softDelete();
        entityManager.persistAndFlush(deleted);

        assertThat(productRepository.searchByNameOrType("life"))
                .extracting(Product::getId)
                .containsExactly(byDescription.getId(), byName.getId());
    }

    @Test
    void productRepositorySearchesByParsedProductType() {
        Product investment = persistProduct("Growth Builder", ProductType.INVESTMENT_FUND);
        persistProduct("Family Life Cover", ProductType.LIFE_INSURANCE);

        assertThat(productRepository.searchByNameOrType("investment fund"))
                .extracting(Product::getId)
                .containsExactly(investment.getId());
    }

    @Test
    void ownershipRepositoryFindsCustomerOwnershipsOrderedByNewestStartDate() {
        Customer customer = persistCustomer("Ownership", "Customer");
        Customer otherCustomer = persistCustomer("Other", "Customer");
        Product product = persistProduct("Customer Product", ProductType.OTHER);

        ProductOwnership older =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2025, 1, 1));
        ProductOwnership newer =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 1, 1));
        persistOwnership(
                otherCustomer, product, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        assertThat(ownershipRepository.findByCustomerId(customer.getId()))
                .extracting(ProductOwnership::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void ownershipRepositoryFindsActiveOwnershipsByProductOrderedByNewestStartDate() {
        Customer customer = persistCustomer("Product", "Owner");
        Product product = persistProduct("Active Ownership Product", ProductType.OTHER);
        Product otherProduct = persistProduct("Other Product", ProductType.OTHER);

        ProductOwnership older =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2027, 1, 1));
        ProductOwnership newer =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2028, 1, 1));
        ProductOwnership cancelled =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2029, 1, 1));
        cancelled.cancel();
        entityManager.persistAndFlush(cancelled);
        persistOwnership(
                customer, otherProduct, LocalDate.of(2027, 1, 1), LocalDate.of(2030, 1, 1));

        assertThat(ownershipRepository.findActiveByProduct(product.getId()))
                .extracting(ProductOwnership::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void ownershipRepositoryFindsActiveOwnershipsExpiringInDateRange() {
        Customer customer = persistCustomer("Expiring", "Owner");
        Product product = persistProduct("Expiring Product", ProductType.OTHER);
        LocalDate rangeStart = LocalDate.of(2026, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2026, 3, 31);

        ProductOwnership first =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 1, 15));
        ProductOwnership second =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2025, 2, 1),
                        LocalDate.of(2026, 3, 1));
        ProductOwnership expired =
                persistOwnership(
                        customer,
                        product,
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2026, 2, 1));
        expired.expire();
        entityManager.persistAndFlush(expired);
        persistOwnership(customer, product, LocalDate.of(2025, 4, 1), null);
        persistOwnership(
                customer, product, LocalDate.of(2025, 5, 1), LocalDate.of(2026, 6, 1));

        assertThat(ownershipRepository.findExpiringBetween(rangeStart, rangeEnd))
                .extracting(ProductOwnership::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void changeRequestRepositoryFindsRequestsByStatusOrderedByNewestCreationTime() {
        Product product = persistProduct("Change Product", ProductType.OTHER);
        User requester = persistUser("status-requester");
        ProductChangeRequest older =
                persistChangeRequest(
                        product,
                        requester,
                        ProductChangeType.PRICE_CHANGE,
                        "Increase renewal price",
                        Instant.parse("2026-01-01T10:00:00Z"));
        ProductChangeRequest newer =
                persistChangeRequest(
                        product,
                        requester,
                        ProductChangeType.STATUS_CHANGE,
                        "Deactivate retired product",
                        Instant.parse("2026-01-02T10:00:00Z"));
        ProductChangeRequest approved =
                persistChangeRequest(
                        product,
                        requester,
                        ProductChangeType.DURATION_CHANGE,
                        "Extend duration",
                        Instant.parse("2026-01-03T10:00:00Z"));
        approved.approve();
        entityManager.persistAndFlush(approved);

        assertThat(changeRequestRepository.findByStatus(ProductChangeStatus.OPEN))
                .extracting(ProductChangeRequest::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void changeRequestRepositoryFindsRequestsByProduct() {
        Product product = persistProduct("Primary Change Product", ProductType.OTHER);
        Product otherProduct = persistProduct("Secondary Change Product", ProductType.OTHER);
        User requester = persistUser("product-requester");
        ProductChangeRequest older =
                persistChangeRequest(
                        product,
                        requester,
                        ProductChangeType.PRICE_CHANGE,
                        "Change product price",
                        Instant.parse("2026-02-01T10:00:00Z"));
        ProductChangeRequest newer =
                persistChangeRequest(
                        product,
                        requester,
                        ProductChangeType.EXPIRATION_RULE_CHANGE,
                        "Change expiration policy",
                        Instant.parse("2026-02-02T10:00:00Z"));
        persistChangeRequest(
                otherProduct,
                requester,
                ProductChangeType.STATUS_CHANGE,
                "Change another product",
                Instant.parse("2026-02-03T10:00:00Z"));

        assertThat(changeRequestRepository.findByProductId(product.getId()))
                .extracting(ProductChangeRequest::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void paymentRecordRepositoryFindsDuePaymentsOrderedByDueDate() {
        Customer customer = persistCustomer("Due", "Customer");
        ProductOwnership ownership = persistOwnershipForPayments(customer);
        PaymentRecord later =
                persistPayment(customer, ownership, LocalDate.of(2026, 3, 1), "100.00");
        PaymentRecord earlier =
                persistPayment(customer, ownership, LocalDate.of(2026, 1, 1), "100.00");
        PaymentRecord paid =
                persistPayment(customer, ownership, LocalDate.of(2026, 2, 1), "100.00");
        paid.markPaid(new BigDecimal("100.00"), Instant.parse("2026-02-02T10:00:00Z"));
        entityManager.persistAndFlush(paid);

        assertThat(paymentRecordRepository.findDuePayments())
                .extracting(PaymentRecord::getId)
                .containsExactly(earlier.getId(), later.getId());
    }

    @Test
    void paymentRecordRepositoryFindsOverdueAndDefaultRiskPayments() {
        Customer customer = persistCustomer("Overdue", "Customer");
        ProductOwnership ownership = persistOwnershipForPayments(customer);
        PaymentRecord overdue =
                persistPayment(customer, ownership, LocalDate.of(2026, 1, 1), "100.00");
        overdue.markOverdue();
        entityManager.persistAndFlush(overdue);
        PaymentRecord defaultRisk =
                persistPayment(customer, ownership, LocalDate.of(2026, 1, 1), "100.00");
        defaultRisk.incrementReminder();
        defaultRisk.incrementReminder();
        defaultRisk.incrementReminder();
        entityManager.persistAndFlush(defaultRisk);
        persistPayment(customer, ownership, LocalDate.of(2026, 2, 1), "100.00");

        assertThat(paymentRecordRepository.findOverduePayments())
                .extracting(PaymentRecord::getId)
                .containsExactly(defaultRisk.getId(), overdue.getId());
    }

    @Test
    void paymentRecordRepositoryFindsPaymentsByCustomerOrderedByDueDate() {
        Customer customer = persistCustomer("Payment", "Customer");
        Customer otherCustomer = persistCustomer("Other", "Payer");
        ProductOwnership ownership = persistOwnershipForPayments(customer);
        ProductOwnership otherOwnership = persistOwnershipForPayments(otherCustomer);
        PaymentRecord later =
                persistPayment(customer, ownership, LocalDate.of(2026, 5, 1), "100.00");
        PaymentRecord earlier =
                persistPayment(customer, ownership, LocalDate.of(2026, 4, 1), "100.00");
        persistPayment(otherCustomer, otherOwnership, LocalDate.of(2026, 3, 1), "100.00");

        assertThat(paymentRecordRepository.findByCustomerId(customer.getId()))
                .extracting(PaymentRecord::getId)
                .containsExactly(earlier.getId(), later.getId());
    }

    private Product persistProduct(String name, ProductType type) {
        return persistProductWithDescription(name, type, name + " description");
    }

    private Product persistProductWithDescription(
            String name, ProductType type, String description) {
        Product product = Product.create(name, type, new BigDecimal("100.00"), 12);
        product.updateDetails(name, type, description, 12, "Annual renewal");
        return entityManager.persistAndFlush(product);
    }

    private Customer persistCustomer(String firstName, String lastName) {
        Customer customer = Customer.create(CustomerType.CUSTOMER, firstName, lastName);
        return entityManager.persistAndFlush(customer);
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@repository-integration.test",
                        "{noop}password",
                        "Repository Integration User");
        return entityManager.persistAndFlush(user);
    }

    private ProductOwnership persistOwnership(
            Customer customer, Product product, LocalDate startDate, LocalDate expirationDate) {
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, startDate, expirationDate);
        ownership.recordPolicyNumber(
                "POL-" + customer.getId().toString().substring(0, 8) + "-" + startDate);
        return entityManager.persistAndFlush(ownership);
    }

    private ProductOwnership persistOwnershipForPayments(Customer customer) {
        Product product = persistProduct("Payment Product " + customer.getId(), ProductType.OTHER);
        return persistOwnership(
                customer, product, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
    }

    private ProductChangeRequest persistChangeRequest(
            Product product,
            User requester,
            ProductChangeType type,
            String description,
            Instant createdAt) {
        ProductChangeRequest request =
                ProductChangeRequest.create(product, requester, type, description);
        ReflectionTestUtils.setField(request, "createdAt", createdAt);
        return entityManager.persistAndFlush(request);
    }

    private PaymentRecord persistPayment(
            Customer customer, ProductOwnership ownership, LocalDate dueDate, String amountDue) {
        PaymentRecord payment =
                PaymentRecord.create(customer, ownership, dueDate, new BigDecimal(amountDue));
        return entityManager.persistAndFlush(payment);
    }
}
