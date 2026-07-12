package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.Query;

@ExtendWith(MockitoExtension.class)
class ProductAndOwnershipDownstreamSupportTests {

    private static final Path PRODUCT_MODULE_DOC = Path.of("../docs/modules/product-module.md");
    private static final Path PRODUCT_OWNERSHIP_DOC =
            Path.of("../docs/modules/product-ownership.md");
    private static final Path PAYMENT_RECORD_DOC = Path.of("../docs/modules/payment-records.md");

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
    void productModuleDocumentationDescribesDownstreamCampaignSegmentationReminderAndAnalyticsUse()
            throws Exception {
        String documentation = Files.readString(PRODUCT_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("Product and ownership data support later segmentation")
                .contains("product-expiration campaigns")
                .contains("payment reminders")
                .contains("analytics")
                .contains("BR-023")
                .contains("3, 6, or 12 months")
                .contains("Production gate")
                .contains("FR-073")
                .contains("FR-074")
                .contains("FR-076")
                .contains("FR-080");
    }

    @Test
    void relatedModuleDocumentationDescribesDownstreamSupportForProductionGate() throws Exception {
        String ownershipDocumentation =
                Files.readString(PRODUCT_OWNERSHIP_DOC, StandardCharsets.UTF_8);
        String paymentDocumentation = Files.readString(PAYMENT_RECORD_DOC, StandardCharsets.UTF_8);

        assertThat(ownershipDocumentation)
                .contains("segmentation")
                .contains("reminder scheduling")
                .contains("analytics")
                .contains("BR-023")
                .contains("FR-073")
                .contains("FR-076");
        assertThat(paymentDocumentation)
                .contains("segmentation")
                .contains("payment reminders")
                .contains("analytics")
                .contains("FR-074")
                .contains("product_ownership_id");
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 6, 12})
    void ownershipEntitySupportsKbProductExpirationCampaignWindows(int months) {
        LocalDate startDate = LocalDate.now();
        ProductOwnership ownership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Campaign", "Target"),
                        Product.create(
                                "Renewal Offer",
                                ProductType.HOMEOWNER_INSURANCE,
                                new BigDecimal("99.00"),
                                12),
                        startDate,
                        startDate.plusMonths(months));

        assertThat(ownership.isExpiringWithinMonths(months)).isTrue();
        assertThat(ownership.isExpiringWithinMonths(months - 1)).isFalse();
        assertThat(ownership.isActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 6, 12})
    void ownershipServiceSupportsKbExpirationCampaignLookupWindows(int months) {
        ProductOwnership ownership = expiringOwnership();
        LocalDate today = LocalDate.now();
        when(productOwnershipRepository.findExpiringBetween(today, today.plusMonths(months)))
                .thenReturn(List.of(ownership));

        List<ProductOwnershipView> views = productOwnershipService.findExpiringWithinMonths(months);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).expirationDate()).isEqualTo(ownership.getExpirationDate());
        assertThat(views.get(0).productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(views.get(0).customerFullName()).isEqualTo("Ada Owner");
    }

    @Test
    void ownershipRepositoryDeclaresActiveExpirationRangeQueryForCampaignGeneration()
            throws Exception {
        Method method =
                ProductOwnershipRepository.class.getMethod(
                        "findExpiringBetween", LocalDate.class, LocalDate.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query.value())
                .contains("ownership.status = 'ACTIVE'")
                .contains("ownership.expirationDate is not null")
                .contains("ownership.expirationDate between :startDate and :endDate");
    }

    @Test
    void ownershipSearchCriteriaExposesSegmentationFiltersForOwnershipAndExpiration() {
        LocalDate expiringFrom = LocalDate.parse("2026-07-01");
        LocalDate expiringTo = LocalDate.parse("2026-10-01");

        ProductOwnershipSearchCriteria criteria =
                new ProductOwnershipSearchCriteria(
                        expiringOwnership().getCustomer().getId(),
                        expiringOwnership().getProduct().getId(),
                        OwnershipStatus.ACTIVE,
                        expiringFrom,
                        expiringTo);

        assertThat(criteria.customerId()).isNotNull();
        assertThat(criteria.productId()).isNotNull();
        assertThat(criteria.status()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(criteria.expiringFrom()).isEqualTo(expiringFrom);
        assertThat(criteria.expiringTo()).isEqualTo(expiringTo);
    }

    @Test
    void ownershipViewExposesAnalyticsJoinFieldsForCustomerProductAndExpiration() {
        ProductOwnershipView view = ProductOwnershipView.from(expiringOwnership());

        assertThat(view.customerId()).isNotNull();
        assertThat(view.customerFullName()).isEqualTo("Ada Owner");
        assertThat(view.productId()).isNotNull();
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.expirationDate()).isNotNull();
        assertThat(view.status()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(view.active()).isTrue();
    }

    @Test
    void paymentRecordRepositoryExposesDueAndOverdueLookupsForReminderScheduling()
            throws Exception {
        assertThat(PaymentRecordRepository.class.getMethod("findDuePayments")).isNotNull();
        assertThat(PaymentRecordRepository.class.getMethod("findOverduePayments")).isNotNull();
        assertThat(
                        PaymentRecordRepository.class.getMethod(
                                "findByCustomerId", java.util.UUID.class))
                .isNotNull();
    }

    @Test
    void paymentRecordSupportsReminderEscalationAndPaymentCompleteExclusion() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Pay", "Customer");
        ProductOwnership ownership = expiringOwnership();
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership,
                        LocalDate.now().minusDays(10),
                        new BigDecimal("120.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DUE);
        assertThat(payment.calculateDaysOverdue()).isGreaterThanOrEqualTo(10);

        payment.incrementReminder();
        payment.incrementReminder();
        assertThat(payment.getReminderCount()).isEqualTo(2);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.OVERDUE);

        payment.incrementReminder();
        assertThat(payment.getReminderCount()).isEqualTo(3);
        assertThat(payment.isDefaultRisk()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DEFAULT_RISK);

        payment.markPaid(new BigDecimal("120.00"), java.time.Instant.now());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);

        payment.incrementReminder();
        assertThat(payment.getReminderCount()).isEqualTo(3);
    }

    @Test
    void paymentRecordLinksCustomerAndOwnershipForProductPerformanceAnalytics() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ana", "Lytics");
        ProductOwnership ownership = expiringOwnership();
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.now().plusDays(14), new BigDecimal("45.50"));

        assertThat(payment.getCustomer()).isSameAs(customer);
        assertThat(payment.getProductOwnership()).isSameAs(ownership);
        assertThat(payment.getAmountDue()).isEqualByComparingTo("45.50");
        assertThat(PaymentStatus.values())
                .containsExactly(
                        PaymentStatus.DUE,
                        PaymentStatus.PAID,
                        PaymentStatus.OVERDUE,
                        PaymentStatus.DEFAULT_RISK);
    }

    @Test
    void productRepositoryExposesTypeAndActiveLookupsForSegmentationAndAnalytics()
            throws Exception {
        assertThat(ProductRepository.class.getMethod("findByType", ProductType.class)).isNotNull();
        assertThat(ProductRepository.class.getMethod("findActive")).isNotNull();
        assertThat(ProductRepository.class.getMethod("searchByNameOrType", String.class))
                .isNotNull();
    }

    @Test
    void paymentRecordViewExposesReminderAndAnalyticsFields() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ana", "Lytics");
        ProductOwnership ownership = expiringOwnership();
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.now().minusDays(5), new BigDecimal("88.00"));

        PaymentRecordView view = PaymentRecordView.from(payment);

        assertThat(view.customerId()).isEqualTo(customer.getId());
        assertThat(view.productOwnershipId()).isEqualTo(ownership.getId());
        assertThat(view.productId()).isEqualTo(ownership.getProduct().getId());
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.reminderCount()).isZero();
        assertThat(view.daysOverdue()).isGreaterThanOrEqualTo(5);
        assertThat(view.defaultRisk()).isFalse();
    }

    @Test
    void flywayMigrationsDefineDownstreamIndexesForExpirationAndPaymentQueries() throws Exception {
        String ownershipMigration =
                new ClassPathResource("db/migration/V8__enhance_product_ownerships_table.sql")
                        .getContentAsString(StandardCharsets.UTF_8);
        String paymentMigration =
                new ClassPathResource("db/migration/V10__enhance_payment_records_table.sql")
                        .getContentAsString(StandardCharsets.UTF_8);
        String initialSchemaMigration =
                new ClassPathResource("db/migration/V1__create_initial_schema.sql")
                        .getContentAsString(StandardCharsets.UTF_8);
        String searchIndexMigration =
                new ClassPathResource("db/migration/V15__add_kb_search_filter_indexes.sql")
                        .getContentAsString(StandardCharsets.UTF_8);

        assertThat(ownershipMigration)
                .contains("idx_product_ownership_expiration")
                .contains("idx_product_ownerships_customer_status");
        assertThat(searchIndexMigration)
                .contains("idx_product_ownerships_status_expiration")
                .contains("idx_product_ownerships_product_expiration");
        assertThat(paymentMigration).contains("idx_payment_records_customer_status");
        assertThat(initialSchemaMigration).contains("payment_records_due_status_idx");
    }

    @Test
    void productCatalogStoresExpirationPolicyForReminderAndCampaignRulePropagation() {
        Product product =
                Product.create(
                        "Investment Growth",
                        ProductType.INVESTMENT_FUND,
                        new BigDecimal("250.00"),
                        36);
        product.updateDetails(
                "Investment Growth",
                ProductType.INVESTMENT_FUND,
                "Long-term fund",
                36,
                "RENEWAL_12_MONTH_NOTICE");

        assertThat(product.getExpirationPolicy()).isEqualTo("RENEWAL_12_MONTH_NOTICE");
        assertThat(product.getProductType()).isEqualTo(ProductType.INVESTMENT_FUND);
        assertThat(product.getDurationMonths()).isEqualTo(36);
    }

    private static ProductOwnership expiringOwnership() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Owner");
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        setEntityId(customer, UUID.fromString("41000000-0000-0000-0000-000000000101"));
        setEntityId(product, UUID.fromString("41000000-0000-0000-0000-000000000201"));
        return ProductOwnership.create(
                customer, product, LocalDate.now(), LocalDate.now().plusMonths(3));
    }

    private static void setEntityId(BaseEntity entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to assign entity id for test fixture", exception);
        }
    }
}
