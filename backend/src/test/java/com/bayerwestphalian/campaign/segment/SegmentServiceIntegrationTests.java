package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
@Import({
    SegmentService.class,
    EligibilityService.class,
    SystemSettingsService.class,
    AuditService.class
})
class SegmentServiceIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_segment_service_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private SegmentService segmentService;

    @Autowired private SegmentRepository segmentRepository;

    @Autowired private CustomerRepository customerRepository;

    @Autowired private UserRepository userRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    @MockBean private ConsentService consentService;

    private User owner;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("segment-service-owner");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
        // Default consent/eligibility path: customers are communication-eligible for preview.
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), any(ConsentType.class), anyBoolean()))
                .thenReturn(true);
        when(consentService.isCommunicationEligible(any(Customer.class), any(ConsentType.class)))
                .thenReturn(true);
    }

    @Test
    void persistsSegmentWithCriteriaThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Munich prospects",
                                "Customers located in Munich",
                                SegmentVisibility.TEAM,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND))));

        entityManager.flush();
        entityManager.clear();

        Segment reloaded = segmentRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Munich prospects");
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(reloaded.getCriteria()).hasSize(1);
        assertThat(reloaded.getCriteria().getFirst().getFieldName()).isEqualTo("city");
        assertThat(reloaded.getCriteria().getFirst().getValue()).isEqualTo("Munich");
        assertThat(created.criteria()).hasSize(1);
    }

    /**
     * KB item 201 / FR-077: Campaign Manager can create a reusable segment that persists with
     * visibility and criteria and can be reloaded and listed for later campaign targeting.
     */
    @Test
    void campaignManagerCanCreateReusableSegmentThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Expiring life policies",
                                "Reusable CM audience for renewals",
                                SegmentVisibility.TEAM,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "CUSTOMER",
                                                "type",
                                                SegmentJoinOperator.AND))));

        entityManager.flush();
        entityManager.clear();

        SegmentView reloaded = segmentService.findById(created.id());
        assertThat(reloaded.id()).isEqualTo(created.id());
        assertThat(reloaded.name()).isEqualTo("Expiring life policies");
        assertThat(reloaded.description()).isEqualTo("Reusable CM audience for renewals");
        assertThat(reloaded.ownerUserId()).isEqualTo(owner.getId());
        assertThat(reloaded.visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(reloaded.criteria()).hasSize(2);
        assertThat(reloaded.criteria().getFirst().fieldName()).isEqualTo("city");
        assertThat(reloaded.criteria().getFirst().value()).isEqualTo("Munich");
        assertThat(reloaded.criteria().get(1).fieldName()).isEqualTo("customer_type");

        List<SegmentView> searchable =
                segmentService.searchSegments(
                        new SegmentSearchCriteria("Expiring", owner.getId(), null));
        assertThat(searchable).extracting(SegmentView::id).contains(created.id());

        List<Segment> byOwner = segmentRepository.findByOwner(owner.getId());
        assertThat(byOwner).extracting(Segment::getName).contains("Expiring life policies");

        List<Segment> byVisibility = segmentRepository.findByVisibility(SegmentVisibility.TEAM);
        assertThat(byVisibility).extracting(Segment::getId).contains(created.id());
    }

    @Test
    void campaignManagerCanCreateGlobalReusableSegmentThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Global consenting prospects",
                                "Shared reusable segment",
                                SegmentVisibility.GLOBAL,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                SegmentJoinOperator.AND))));

        entityManager.flush();
        entityManager.clear();

        Segment reloaded = segmentRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getVisibility()).isEqualTo(SegmentVisibility.GLOBAL);
        assertThat(reloaded.isGlobal()).isTrue();
        assertThat(segmentRepository.findGlobal())
                .extracting(Segment::getId)
                .contains(created.id());
    }

    @Test
    void createSegmentWritesAuditLogThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Audit create segment",
                                "Segment create audit",
                                SegmentVisibility.PRIVATE,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                null,
                                                SegmentJoinOperator.AND))));

        entityManager.flush();
        entityManager.clear();

        List<AuditLog> logs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId order by a.createdAt desc",
                                AuditLog.class)
                        .setParameter("entityType", "segments")
                        .setParameter("entityId", created.id())
                        .getResultList();

        assertThat(logs).isNotEmpty();
        AuditLog createLog = logs.getFirst();
        assertThat(createLog.getAction()).isEqualTo("CREATE");
        assertThat(createLog.getActorUserId()).isEqualTo(owner.getId());
        assertThat(createLog.getNewValue()).containsEntry("name", "Audit create segment");
        assertThat(createLog.getNewValue()).containsEntry("visibility", "PRIVATE");
        assertThat(createLog.getNewValue()).containsEntry("criteriaCount", 1);
        assertThat(createLog.getOldValue()).isNull();
    }

    @Test
    void updateSegmentWritesAuditLogThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Before update",
                                "Original",
                                SegmentVisibility.TEAM,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        segmentService.updateSegment(
                created.id(),
                new UpdateSegmentCommand(
                        "After update",
                        "Changed",
                        SegmentVisibility.GLOBAL,
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Hamburg",
                                        null,
                                        SegmentJoinOperator.AND))));

        entityManager.flush();
        entityManager.clear();

        List<AuditLog> updateLogs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action "
                                        + "order by a.createdAt desc",
                                AuditLog.class)
                        .setParameter("entityType", "segments")
                        .setParameter("entityId", created.id())
                        .setParameter("action", "UPDATE")
                        .getResultList();

        assertThat(updateLogs).isNotEmpty();
        AuditLog updateLog = updateLogs.getFirst();
        assertThat(updateLog.getActorUserId()).isEqualTo(owner.getId());
        assertThat(updateLog.getOldValue()).containsEntry("name", "Before update");
        assertThat(updateLog.getNewValue()).containsEntry("name", "After update");
        assertThat(updateLog.getNewValue()).containsEntry("visibility", "GLOBAL");
    }

    @Test
    void deleteSegmentWritesAuditLogThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "To delete",
                                "Will be removed",
                                SegmentVisibility.PRIVATE,
                                List.of()));

        segmentService.deleteSegment(created.id());
        entityManager.flush();
        entityManager.clear();

        List<AuditLog> deleteLogs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action",
                                AuditLog.class)
                        .setParameter("entityType", "segments")
                        .setParameter("entityId", created.id())
                        .setParameter("action", "DELETE")
                        .getResultList();

        assertThat(deleteLogs).hasSize(1);
        assertThat(deleteLogs.getFirst().getOldValue()).containsEntry("name", "To delete");
        assertThat(deleteLogs.getFirst().getActorUserId()).isEqualTo(owner.getId());
        assertThat(segmentRepository.findById(created.id())).isEmpty();
    }

    @Test
    void filtersCustomersByOwnedProductTypeThroughService() {
        Customer lifeCustomer =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer prospectWithoutHome =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        persistOwnership(lifeCustomer, lifeProduct);
        persistOwnership(prospectWithoutHome, homeProduct);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "product_type",
                                                SegmentOperator.EQUALS,
                                                "LIFE_INSURANCE",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void filtersCustomersWithoutOwnedProductTypeThroughService() {
        Customer withHomeInsurance =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer withoutHomeInsurance =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        persistOwnership(withHomeInsurance, homeProduct);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "product_ownership",
                                                SegmentOperator.NOT_EQUALS,
                                                "HOMEOWNER_INSURANCE",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void filtersCustomersByProductIdAndOwnershipStatusThroughService() {
        Customer lifeCustomer =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer fundCustomer =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Hamburg", "Germany", CustomerType.CUSTOMER);
        Customer expiredCustomer =
                persistCustomerWithLocation(
                        "Kai", "Fischer", "Cologne", "Germany", CustomerType.CUSTOMER);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product fundProduct = persistProduct("Growth Fund", ProductType.INVESTMENT_FUND);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        persistOwnership(lifeCustomer, lifeProduct);
        persistOwnership(fundCustomer, fundProduct);
        ProductOwnership expiredOwnership = persistOwnership(expiredCustomer, autoProduct);
        expiredOwnership.expire();
        entityManager.persistAndFlush(expiredOwnership);

        SegmentPreviewView byProductId =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "product_id",
                                                SegmentOperator.EQUALS,
                                                lifeProduct.getId().toString(),
                                                "ownership",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byStatusActive =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "ownership_status",
                                                SegmentOperator.EQUALS,
                                                "ACTIVE",
                                                "ownership",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byStatusExpired =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "ownership_status",
                                                SegmentOperator.EQUALS,
                                                "EXPIRED",
                                                "ownership",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byTypeIn =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "owned_product_type",
                                                SegmentOperator.IN,
                                                "LIFE_INSURANCE,INVESTMENT_FUND",
                                                "ownership",
                                                SegmentJoinOperator.AND))));

        assertThat(byProductId.totalAudienceCount()).isEqualTo(1);
        assertThat(byProductId.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Schmidt");
        assertThat(byStatusActive.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
        assertThat(byStatusExpired.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactly("Kai Fischer");
        assertThat(byTypeIn.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
    }

    @Test
    void filtersCustomersByPaymentStatusThroughService() {
        Customer overdueCustomer =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer paidCustomer =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        ProductOwnership overdueOwnership = persistOwnership(overdueCustomer, lifeProduct);
        ProductOwnership paidOwnership = persistOwnership(paidCustomer, autoProduct);
        persistOverduePayment(overdueCustomer, overdueOwnership);
        persistPaidPayment(paidCustomer, paidOwnership);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "payment_status",
                                                SegmentOperator.EQUALS,
                                                "OVERDUE",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void filtersCustomersByPaymentHistoryAliasAndDefaultRiskThroughService() {
        Customer defaultRiskCustomer =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer paidCustomer =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        Product lifeProduct = persistProduct("Life Cover", ProductType.LIFE_INSURANCE);
        ProductOwnership riskOwnership = persistOwnership(defaultRiskCustomer, homeProduct);
        ProductOwnership paidOwnership = persistOwnership(paidCustomer, lifeProduct);
        persistDefaultRiskPayment(defaultRiskCustomer, riskOwnership);
        persistPaidPayment(paidCustomer, paidOwnership);

        SegmentPreviewView withoutDefaultRisk =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "payment_history",
                                                SegmentOperator.NOT_EQUALS,
                                                "DEFAULT_RISK",
                                                null,
                                                SegmentJoinOperator.AND))));
        SegmentPreviewView defaultRiskOnly =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "default_risk",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(withoutDefaultRisk.totalAudienceCount()).isEqualTo(1);
        assertThat(withoutDefaultRisk.matchingCustomers().getFirst().fullName())
                .isEqualTo("Lena Mueller");
        assertThat(defaultRiskOnly.totalAudienceCount()).isEqualTo(1);
        assertThat(defaultRiskOnly.matchingCustomers().getFirst().fullName())
                .isEqualTo("Tom Schmidt");
    }

    @Test
    void filtersCustomersByReminderCountDaysOverdueAndPaymentStatusInThroughService() {
        Customer overdueCustomer =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer riskCustomer =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Hamburg", "Germany", CustomerType.CUSTOMER);
        Customer paidCustomer =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        ProductOwnership overdueOwnership = persistOwnership(overdueCustomer, lifeProduct);
        ProductOwnership riskOwnership = persistOwnership(riskCustomer, homeProduct);
        ProductOwnership paidOwnership = persistOwnership(paidCustomer, autoProduct);
        persistOverduePayment(overdueCustomer, overdueOwnership);
        persistDefaultRiskPayment(riskCustomer, riskOwnership);
        persistPaidPayment(paidCustomer, paidOwnership);

        SegmentPreviewView byReminderAfter =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "reminder_count",
                                                SegmentOperator.AFTER,
                                                "2",
                                                "payment",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byDaysOverdue =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "days_overdue",
                                                SegmentOperator.AFTER,
                                                "0",
                                                "payment",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byStatusIn =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "payment_status",
                                                SegmentOperator.IN,
                                                "OVERDUE,DEFAULT_RISK",
                                                "payment",
                                                SegmentJoinOperator.AND))));

        assertThat(byReminderAfter.totalAudienceCount()).isEqualTo(1);
        assertThat(byReminderAfter.matchingCustomers().getFirst().fullName())
                .isEqualTo("Anna Weber");
        assertThat(byDaysOverdue.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
        assertThat(byStatusIn.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
    }

    @Test
    void filtersCustomersByCustomerTypeThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation(
                "Anna", "Weber", "Hamburg", "Germany", CustomerType.BENEFICIARY);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.IN,
                                                "PROSPECT,BENEFICIARY",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers()).hasSize(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::customerType)
                .containsExactlyInAnyOrder(CustomerType.PROSPECT, CustomerType.BENEFICIARY);
    }

    @Test
    void filtersCustomersByBehaviorStatusThroughService() {
        Customer interested =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        interested.changeStatus(CustomerStatus.INTERESTED);
        interested.recordSource("LIFE_INSURANCE_BENEFICIARY");
        entityManager.persistAndFlush(interested);

        Customer uninterested =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        uninterested.changeStatus(CustomerStatus.UNINTERESTED);
        entityManager.persistAndFlush(uninterested);

        SegmentPreviewView byStatus =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "status",
                                                SegmentOperator.EQUALS,
                                                "INTERESTED",
                                                null,
                                                SegmentJoinOperator.AND))));
        SegmentPreviewView byBehaviorAndSource =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "behavior",
                                                SegmentOperator.IN,
                                                "INTERESTED,CONVERTED",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "interest",
                                                SegmentOperator.CONTAINS,
                                                "LIFE_INSURANCE",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(byStatus.totalAudienceCount()).isEqualTo(1);
        assertThat(byStatus.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(byBehaviorAndSource.totalAudienceCount()).isEqualTo(1);
        assertThat(byBehaviorAndSource.matchingCustomers().getFirst().status())
                .isEqualTo(CustomerStatus.INTERESTED);
    }

    @Test
    void filtersCustomersByDoNotContactThroughService() {
        Customer contactable =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer blocked =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        entityManager.persistAndFlush(blocked);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "do_not_contact",
                                                SegmentOperator.EQUALS,
                                                "false",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers().getFirst().doNotContact()).isFalse();
    }

    @Test
    void previewReturnsTotalAudienceCountForCriteriaMatchesThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Anna", "Weber", "Munich", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        // FR-079: total audience count is criteria match size
        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).hasSize(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::city)
                .containsOnly("Munich");
    }

    @Test
    void previewTotalAudienceCountIncludesDoNotContactCriteriaMatchesThroughService() {
        Customer contactable =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer blocked =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Munich", "Germany", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        entityManager.persistAndFlush(blocked);

        // Empty criteria: both active profiles match; DNC is eligibility-excluded from listed
        // matches
        SegmentPreviewView preview =
                segmentService.previewSegment(new SegmentPreviewCommand(List.of()));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers().getFirst().id()).isEqualTo(contactable.getId());
    }

    @Test
    void previewReturnsEligibleCountThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer blocked =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Munich", "Germany", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        entityManager.persistAndFlush(blocked);
        persistCustomerWithLocation("Anna", "Weber", "Berlin", "Germany", CustomerType.CUSTOMER);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void previewReturnsExcludedCountThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer blockedOne =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Munich", "Germany", CustomerType.CUSTOMER);
        blockedOne.markDoNotContact();
        entityManager.persistAndFlush(blockedOne);
        Customer blockedTwo =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Munich", "Germany", CustomerType.CUSTOMER);
        blockedTwo.markDoNotContact();
        entityManager.persistAndFlush(blockedTwo);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.exclusionReasonSummary()).hasSize(1);
        assertThat(preview.exclusionReasonSummary().getFirst().code()).isEqualTo("DO_NOT_CONTACT");
        assertThat(preview.exclusionReasonSummary().getFirst().count()).isEqualTo(2);
    }

    @Test
    void previewAppliesEligibilityServiceGateThroughService() {
        // KB item 198: real EligibilityService path — DNC criteria matches are excluded from
        // contactable list.
        Customer contactable =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer blocked =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Munich", "Germany", CustomerType.CUSTOMER);
        blocked.markDoNotContact();
        entityManager.persistAndFlush(blocked);
        persistCustomerWithLocation("Anna", "Weber", "Berlin", "Germany", CustomerType.CUSTOMER);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND))));

        // Criteria match both Munich profiles; EligibilityService drops DNC from matchingCustomers.
        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().id()).isEqualTo(contactable.getId());
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers())
                .noneMatch(view -> view.fullName().equals("Tom Schmidt"));
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactly("DO_NOT_CONTACT");
    }

    @Test
    void previewReturnsEligibleAndExcludedCountsThroughService() {
        // KB item 199 / FR-079: total / eligible / excluded always populated with count identity.
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Sara", "Klein", "Munich", "Germany", CustomerType.CUSTOMER);
        Customer blockedOne =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Munich", "Germany", CustomerType.CUSTOMER);
        blockedOne.markDoNotContact();
        entityManager.persistAndFlush(blockedOne);
        Customer blockedTwo =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Munich", "Germany", CustomerType.CUSTOMER);
        blockedTwo.markDoNotContact();
        entityManager.persistAndFlush(blockedTwo);
        persistCustomerWithLocation("Max", "Bauer", "Berlin", "Germany", CustomerType.CUSTOMER);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(4);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.matchingCustomers()).hasSize(preview.eligibleCount());
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Sara Klein");
        int summaryTotal =
                preview.exclusionReasonSummary().stream()
                        .mapToInt(SegmentExclusionReasonSummary::count)
                        .sum();
        assertThat(summaryTotal).isEqualTo(preview.excludedCount());
    }

    @Test
    void previewReturnsExclusionReasonSummaryThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer dnc =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Munich", "Germany", CustomerType.CUSTOMER);
        dnc.markDoNotContact();
        entityManager.persistAndFlush(dnc);
        // Consent service mock returns ineligible for marketing communication for one profile
        Customer noConsent =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Munich", "Germany", CustomerType.CUSTOMER);
        when(consentService.isCommunicationEligible(
                        org.mockito.ArgumentMatchers.<Customer>argThat(
                                customer ->
                                        customer != null
                                                && noConsent.getId().equals(customer.getId())),
                        any(ConsentType.class),
                        anyBoolean()))
                .thenReturn(false);
        when(consentService.isCommunicationEligible(
                        org.mockito.ArgumentMatchers.<Customer>argThat(
                                customer ->
                                        customer != null
                                                && noConsent.getId().equals(customer.getId())),
                        any(ConsentType.class)))
                .thenReturn(false);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.exclusionReasonSummary())
                .extracting(SegmentExclusionReasonSummary::code)
                .contains("DO_NOT_CONTACT");
        assertThat(
                        preview.exclusionReasonSummary().stream()
                                .mapToInt(SegmentExclusionReasonSummary::count)
                                .sum())
                .isEqualTo(2);
    }

    @Test
    void filtersCustomersByProductExpirationThroughService() {
        Customer expiringSoon =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer farFuture =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        persistOwnershipExpiringInMonths(expiringSoon, lifeProduct, 2);
        persistOwnershipExpiringInMonths(farFuture, autoProduct, 18);

        SegmentPreviewView withinThreeMonths =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "expiring_within_months",
                                                SegmentOperator.EQUALS,
                                                "3",
                                                null,
                                                SegmentJoinOperator.AND))));
        SegmentPreviewView isExpiring =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "product_expiration",
                                                SegmentOperator.EQUALS,
                                                "6",
                                                null,
                                                SegmentJoinOperator.AND))));
        SegmentPreviewView byFlag =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "is_expiring",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(withinThreeMonths.totalAudienceCount()).isEqualTo(1);
        assertThat(withinThreeMonths.matchingCustomers().getFirst().fullName())
                .isEqualTo("Tom Schmidt");
        assertThat(isExpiring.totalAudienceCount()).isEqualTo(1);
        assertThat(isExpiring.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Schmidt");
        assertThat(byFlag.totalAudienceCount()).isEqualTo(1);
        assertThat(byFlag.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void filtersCustomersByExpirationDateAndKbMonthWindowInThroughService() {
        Customer expiringInFive =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        Customer expiringInTen =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Hamburg", "Germany", CustomerType.CUSTOMER);
        Customer farFuture =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Product lifeProduct = persistProduct("Life Protection", ProductType.LIFE_INSURANCE);
        Product homeProduct = persistProduct("Home Cover", ProductType.HOMEOWNER_INSURANCE);
        Product autoProduct = persistProduct("Auto Cover", ProductType.AUTO_INSURANCE);
        LocalDate fiveMonths = LocalDate.now().plusMonths(5);
        LocalDate tenMonths = LocalDate.now().plusMonths(10);
        persistOwnershipExpiringOn(expiringInFive, lifeProduct, fiveMonths);
        persistOwnershipExpiringOn(expiringInTen, homeProduct, tenMonths);
        persistOwnershipExpiringInMonths(farFuture, autoProduct, 18);

        SegmentPreviewView byDateBefore =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "expiration_date",
                                                SegmentOperator.BEFORE,
                                                tenMonths.toString(),
                                                "expiration",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byMonthsIn =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "expiring_within_months",
                                                SegmentOperator.IN,
                                                "3,6,12",
                                                "expiration",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byDateEquals =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "expiration_date",
                                                SegmentOperator.EQUALS,
                                                fiveMonths.toString(),
                                                "expiration",
                                                SegmentJoinOperator.AND))));

        assertThat(byDateBefore.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactly("Tom Schmidt");
        assertThat(byMonthsIn.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Schmidt", "Anna Weber");
        assertThat(byDateEquals.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Schmidt");
    }

    @Test
    void filtersCustomersByConsentStatusThroughService() {
        Customer consented =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer optedOut =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistGivenMarketingConsent(consented);
        persistWithdrawnMarketingConsent(optedOut);

        SegmentPreviewView byStatus =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "consent_status",
                                                SegmentOperator.EQUALS,
                                                "GIVEN",
                                                null,
                                                SegmentJoinOperator.AND))));
        SegmentPreviewView byValidConsent =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "has_valid_marketing_consent",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                null,
                                                SegmentJoinOperator.AND))));
        SegmentPreviewView withoutOptOut =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "opt_out",
                                                SegmentOperator.EQUALS,
                                                "false",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(byStatus.totalAudienceCount()).isEqualTo(1);
        assertThat(byStatus.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(byValidConsent.totalAudienceCount()).isEqualTo(1);
        assertThat(byValidConsent.matchingCustomers().getFirst().fullName())
                .isEqualTo("Lena Mueller");
        assertThat(withoutOptOut.totalAudienceCount()).isEqualTo(1);
        assertThat(withoutOptOut.matchingCustomers().getFirst().fullName())
                .isEqualTo("Lena Mueller");
    }

    @Test
    void filtersCustomersByConsentTypeGuardianAndStatusInThroughService() {
        Customer marketing =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        Customer guardian =
                persistCustomerWithLocation(
                        "Anna", "Weber", "Hamburg", "Germany", CustomerType.BENEFICIARY);
        Customer rejected =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistGivenMarketingConsent(marketing);
        persistGivenGuardianConsent(guardian);
        ConsentRecord rejectedConsent =
                ConsentRecord.create(
                        rejected,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        rejectedConsent.reject();
        entityManager.persistAndFlush(rejectedConsent);

        SegmentPreviewView byGuardian =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "guardian_consent",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                "consent",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byTypeGuardian =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "consent_type",
                                                SegmentOperator.EQUALS,
                                                "GUARDIAN",
                                                "consent",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byStatusIn =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "consent_status",
                                                SegmentOperator.IN,
                                                "GIVEN,REJECTED",
                                                "consent",
                                                SegmentJoinOperator.AND))));

        assertThat(byGuardian.totalAudienceCount()).isEqualTo(1);
        assertThat(byGuardian.matchingCustomers().getFirst().fullName()).isEqualTo("Anna Weber");
        assertThat(byTypeGuardian.matchingCustomers().getFirst().fullName())
                .isEqualTo("Anna Weber");
        assertThat(byStatusIn.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber", "Tom Schmidt");
    }

    @Test
    void filtersCustomersByPureAndLogicThroughService() {
        // KB item 196 / FR-078: AND returns intersection only — partial matches are excluded.
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Anna", "Weber", "Munich", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Kai", "Fischer", "Munich", "Austria", CustomerType.PROSPECT);

        SegmentPreviewView cityAndType =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                "audience",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView cityAndTypeAndCountry =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                null),
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                null),
                                        new CreateSegmentCriteriaCommand(
                                                "country",
                                                SegmentOperator.EQUALS,
                                                "Germany",
                                                null,
                                                null))));

        SegmentPreviewView emptyIntersection =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "BENEFICIARY",
                                                "audience",
                                                SegmentJoinOperator.AND))));

        assertThat(cityAndType.totalAudienceCount()).isEqualTo(2);
        assertThat(cityAndType.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Kai Fischer");
        assertThat(cityAndTypeAndCountry.totalAudienceCount()).isEqualTo(1);
        assertThat(cityAndTypeAndCountry.matchingCustomers().getFirst().fullName())
                .isEqualTo("Lena Mueller");
        assertThat(emptyIntersection.totalAudienceCount()).isEqualTo(0);
        assertThat(emptyIntersection.matchingCustomers()).isEmpty();
    }

    @Test
    void filtersCustomersByOrCriteriaThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Anna", "Weber", "Hamburg", "Germany", CustomerType.CUSTOMER);

        SegmentPreviewView munichOrBerlin =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                "location",
                                                SegmentJoinOperator.OR))));

        assertThat(munichOrBerlin.totalAudienceCount()).isEqualTo(2);
        assertThat(munichOrBerlin.matchingCustomers())
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Berlin");
    }

    @Test
    void filtersCustomersByPureOrLogicUnionThroughService() {
        // KB item 197 / FR-078: OR returns union of matching branches; non-matching cities
        // excluded.
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Anna", "Weber", "Hamburg", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Kai", "Fischer", "Cologne", "Germany", CustomerType.PROSPECT);

        SegmentPreviewView threeWayCityOr =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                "location",
                                                SegmentJoinOperator.OR),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Hamburg",
                                                "location",
                                                SegmentJoinOperator.OR))));

        SegmentPreviewView emptyUnion =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Vienna",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Zurich",
                                                "location",
                                                SegmentJoinOperator.OR))));

        assertThat(threeWayCityOr.totalAudienceCount()).isEqualTo(3);
        assertThat(threeWayCityOr.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Tom Schmidt", "Anna Weber");
        assertThat(threeWayCityOr.matchingCustomers())
                .noneMatch(view -> view.fullName().equals("Kai Fischer"));
        assertThat(emptyUnion.totalAudienceCount()).isEqualTo(0);
        assertThat(emptyUnion.matchingCustomers()).isEmpty();
    }

    @Test
    void filtersCustomersByCrossFieldOrAndMixedAndOrThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Anna", "Weber", "Munich", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        persistCustomerWithLocation("Kai", "Fischer", "Hamburg", "Germany", CustomerType.CUSTOMER);

        SegmentPreviewView prospectOrMunich =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.OR))));

        // (PROSPECT AND Munich) OR Berlin => Lena + Tom
        SegmentPreviewView mixedAndOr =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Berlin",
                                                null,
                                                SegmentJoinOperator.OR))));

        assertThat(prospectOrMunich.totalAudienceCount()).isEqualTo(2);
        assertThat(prospectOrMunich.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
        assertThat(mixedAndOr.totalAudienceCount()).isEqualTo(2);
        assertThat(mixedAndOr.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Tom Schmidt");
    }

    @Test
    void filtersCustomersByLocationThroughService() {
        persistCustomerWithLocation("Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        persistCustomerWithLocation("Anna", "Weber", "Vienna", "Austria", CustomerType.CUSTOMER);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "country",
                                                SegmentOperator.EQUALS,
                                                "Germany",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.IN,
                                                "Munich,Berlin",
                                                "location",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().city()).isEqualTo("Munich");
        assertThat(preview.matchingCustomers().getFirst().country()).isEqualTo("Germany");
    }

    @Test
    void filtersCustomersByLocationAliasContainsAndAddressLineThroughService() {
        Customer lena =
                persistCustomerWithLocation(
                        "Lena", "Mueller", "Munich", "Germany", CustomerType.PROSPECT);
        lena.updateAddress("Main Street 1", "Munich", "Germany");
        entityManager.persistAndFlush(lena);
        Customer tom =
                persistCustomerWithLocation(
                        "Tom", "Schmidt", "Berlin", "Germany", CustomerType.CUSTOMER);
        tom.updateAddress("Alexanderplatz 2", "Berlin", "Germany");
        entityManager.persistAndFlush(tom);

        SegmentPreviewView byLocationAlias =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "location",
                                                SegmentOperator.CONTAINS,
                                                "mun",
                                                "location",
                                                SegmentJoinOperator.AND))));

        SegmentPreviewView byAddressLine =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "address_line",
                                                SegmentOperator.CONTAINS,
                                                "Alexander",
                                                "location",
                                                SegmentJoinOperator.AND))));

        assertThat(byLocationAlias.totalAudienceCount()).isEqualTo(1);
        assertThat(byLocationAlias.matchingCustomers().getFirst().city()).isEqualTo("Munich");
        assertThat(byAddressLine.totalAudienceCount()).isEqualTo(1);
        assertThat(byAddressLine.matchingCustomers().getFirst().fullName())
                .isEqualTo("Tom Schmidt");
        assertThat(byAddressLine.matchingCustomers().getFirst().addressLine())
                .contains("Alexander");
    }

    @Test
    void filtersCustomersByAgeGroupThroughService() {
        Customer youngProspect =
                persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        youngProspect.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_18_25);
        entityManager.persistAndFlush(youngProspect);

        Customer matureCustomer =
                persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        matureCustomer.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_41_60);
        entityManager.persistAndFlush(matureCustomer);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "age_group",
                                                SegmentOperator.EQUALS,
                                                "18_25",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers().getFirst().ageGroup())
                .isEqualTo(com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_18_25);
    }

    @Test
    void filtersCustomersByAgeGroupInOperatorThroughService() {
        // KB item 190: age group IN filter works end-to-end through preview.
        Customer young = persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        young.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_18_25);
        entityManager.persistAndFlush(young);

        Customer mid = persistCustomer("Anna", "Weber", "Hamburg", CustomerType.CUSTOMER);
        mid.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_26_40);
        entityManager.persistAndFlush(mid);

        Customer senior = persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);
        senior.updateDemographics(
                null, com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_60_PLUS);
        entityManager.persistAndFlush(senior);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "age_group",
                                                SegmentOperator.IN,
                                                "AGE_18_25,60_PLUS",
                                                "demographics",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::ageGroup)
                .containsExactlyInAnyOrder(
                        com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_18_25,
                        com.bayerwestphalian.campaign.customer.CustomerAgeGroup.AGE_60_PLUS);
    }

    @Test
    void previewsSegmentAudienceThroughService() {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers().getFirst().customerType())
                .isEqualTo(CustomerType.PROSPECT);
    }

    @Test
    void findMatchingCustomersAndPreviewUsePersistedCustomerProfiles() {
        persistCustomer("Lena", "Mueller", "Munich", CustomerType.PROSPECT);
        persistCustomer("Tom", "Schmidt", "Berlin", CustomerType.CUSTOMER);

        List<com.bayerwestphalian.campaign.customer.CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        SegmentJoinOperator.AND)));

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void updatesSegmentMetadataWithoutReplacingCriteriaThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Initial audience",
                                "Original description",
                                SegmentVisibility.PRIVATE,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        SegmentView updated =
                segmentService.updateSegment(
                        created.id(),
                        new UpdateSegmentCommand(
                                "Renamed audience",
                                "Updated description only",
                                SegmentVisibility.TEAM,
                                null));

        entityManager.flush();
        entityManager.clear();

        Segment reloaded = segmentRepository.findById(created.id()).orElseThrow();
        assertThat(updated.name()).isEqualTo("Renamed audience");
        assertThat(updated.description()).isEqualTo("Updated description only");
        assertThat(updated.visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(updated.criteria()).hasSize(1);
        assertThat(updated.criteria().getFirst().fieldName()).isEqualTo("city");
        assertThat(reloaded.getName()).isEqualTo("Renamed audience");
        assertThat(reloaded.getVisibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(reloaded.getCriteria()).hasSize(1);
        assertThat(reloaded.getCriteria().getFirst().getValue()).isEqualTo("Munich");
    }

    @Test
    void deletesSegmentThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Disposable audience",
                                "Temporary targeting rules",
                                SegmentVisibility.GLOBAL,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        segmentService.deleteSegment(created.id());

        entityManager.flush();
        entityManager.clear();

        assertThat(segmentRepository.findById(created.id())).isEmpty();
    }

    @Test
    void rejectsDeleteForNonOwnerThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Owned audience", null, SegmentVisibility.PRIVATE, null));

        UUID otherUserId = UUID.fromString("10000000-0000-0000-0000-000000009999");
        when(authorizationExpressions.currentUserId()).thenReturn(otherUserId);
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);

        assertThatThrownBy(() -> segmentService.deleteSegment(created.id()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Segment is not owned by the current user");

        assertThat(segmentRepository.findById(created.id())).isPresent();
    }

    @Test
    void saveCriteriaReplacesStoredRulesForOwnedSegment() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Initial audience",
                                null,
                                SegmentVisibility.PRIVATE,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.EQUALS,
                                                "Munich",
                                                null,
                                                SegmentJoinOperator.AND))));

        SegmentView updated =
                segmentService.saveCriteria(
                        created.id(),
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        SegmentJoinOperator.AND)));

        entityManager.flush();
        entityManager.clear();

        Segment reloaded = segmentRepository.findById(created.id()).orElseThrow();
        assertThat(updated.criteria()).hasSize(1);
        assertThat(updated.criteria().getFirst().fieldName()).isEqualTo("customer_type");
        assertThat(reloaded.getCriteria()).hasSize(1);
        assertThat(reloaded.getCriteria().getFirst().getFieldName()).isEqualTo("customer_type");
    }

    @Test
    void loadsSegmentDetailsThroughService() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Detail audience",
                                "Loaded through findById",
                                SegmentVisibility.TEAM,
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "customer_type",
                                                SegmentOperator.EQUALS,
                                                "PROSPECT",
                                                null,
                                                SegmentJoinOperator.AND))));

        entityManager.flush();
        entityManager.clear();

        SegmentView loaded = segmentService.findById(created.id());

        assertThat(loaded.id()).isEqualTo(created.id());
        assertThat(loaded.name()).isEqualTo("Detail audience");
        assertThat(loaded.description()).isEqualTo("Loaded through findById");
        assertThat(loaded.ownerUserId()).isEqualTo(owner.getId());
        assertThat(loaded.visibility()).isEqualTo(SegmentVisibility.TEAM);
        assertThat(loaded.criteria()).hasSize(1);
        assertThat(loaded.criteria().getFirst().fieldName()).isEqualTo("customer_type");
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.updatedAt()).isNotNull();
    }

    @Test
    void blocksPrivateSegmentReadForNonOwner() {
        SegmentView created =
                segmentService.createSegment(
                        new CreateSegmentCommand(
                                "Private audience", null, SegmentVisibility.PRIVATE, null));

        UUID otherUserId = UUID.fromString("10000000-0000-0000-0000-000000009999");
        when(authorizationExpressions.currentUserId()).thenReturn(otherUserId);

        assertThatThrownBy(() -> segmentService.findById(created.id()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Private segment is not accessible");
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@segment-service-integration.test",
                        "{noop}password",
                        "Segment Service Integration User");
        return entityManager.persistAndFlush(user);
    }

    private Customer persistCustomer(
            String firstName, String lastName, String city, CustomerType customerType) {
        return persistCustomerWithLocation(firstName, lastName, city, "Germany", customerType);
    }

    private Customer persistCustomerWithLocation(
            String firstName,
            String lastName,
            String city,
            String country,
            CustomerType customerType) {
        Customer customer = Customer.create(customerType, firstName, lastName);
        customer.updateAddress(null, city, country);
        customer.changeStatus(CustomerStatus.ACTIVE);
        return entityManager.persistAndFlush(customer);
    }

    private Product persistProduct(String name, ProductType productType) {
        Product product = Product.create(name, productType, new BigDecimal("99.00"), 12);
        return entityManager.persistAndFlush(product);
    }

    private ProductOwnership persistOwnership(Customer customer, Product product) {
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.now().minusMonths(6),
                        LocalDate.now().plusYears(1));
        return entityManager.persistAndFlush(ownership);
    }

    private ProductOwnership persistOwnershipExpiringInMonths(
            Customer customer, Product product, int months) {
        return persistOwnershipExpiringOn(customer, product, LocalDate.now().plusMonths(months));
    }

    private ProductOwnership persistOwnershipExpiringOn(
            Customer customer, Product product, LocalDate expirationDate) {
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer, product, LocalDate.now().minusMonths(6), expirationDate);
        return entityManager.persistAndFlush(ownership);
    }

    private PaymentRecord persistOverduePayment(Customer customer, ProductOwnership ownership) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership,
                        LocalDate.now().minusDays(10),
                        new BigDecimal("120.00"));
        payment.markOverdue();
        return entityManager.persistAndFlush(payment);
    }

    private PaymentRecord persistPaidPayment(Customer customer, ProductOwnership ownership) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.now().minusDays(5), new BigDecimal("80.00"));
        payment.markPaid(new BigDecimal("80.00"), Instant.now());
        return entityManager.persistAndFlush(payment);
    }

    private PaymentRecord persistDefaultRiskPayment(Customer customer, ProductOwnership ownership) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership,
                        LocalDate.now().minusDays(30),
                        new BigDecimal("200.00"));
        payment.incrementReminder();
        payment.incrementReminder();
        payment.incrementReminder();
        return entityManager.persistAndFlush(payment);
    }

    private ConsentRecord persistGivenMarketingConsent(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        return entityManager.persistAndFlush(consent);
    }

    private ConsentRecord persistWithdrawnMarketingConsent(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        consent.withdraw(Instant.now());
        return entityManager.persistAndFlush(consent);
    }

    private ConsentRecord persistGivenGuardianConsent(Customer customer) {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer,
                        ConsentType.GUARDIAN,
                        ConsentStatus.GIVEN,
                        "Guardian consent for minor beneficiary",
                        "letter");
        return entityManager.persistAndFlush(consent);
    }
}
