package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentRepository;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
@Import({CampaignService.class, AuditService.class})
class CampaignServiceIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_service_integration_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignProductRepository campaignProductRepository;
    @Autowired private SegmentRepository segmentRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CampaignRecipientRepository campaignRecipientRepository;
    @Autowired private ContactEventRepository contactEventRepository;
    @Autowired private CampaignMetricsRepository campaignMetricsRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

    private User owner;
    private User compliance;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        owner = persistUser("campaign-service-owner");
        compliance = persistUser("campaign-service-compliance");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void createsCampaignWithSegmentAndProductsThroughService() {
        Segment segment =
                entityManager.persistAndFlush(
                        Segment.create("Munich prospects", null, owner, SegmentVisibility.TEAM));
        Product product =
                entityManager.persistAndFlush(
                        Product.create(
                                "Life Plan",
                                ProductType.LIFE_INSURANCE,
                                new BigDecimal("55.00"),
                                12));

        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Life renewal",
                                "Renewal objective",
                                segment.getId(),
                                CampaignChannel.EMAIL,
                                "Subject",
                                "Body",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                List.of(product.getId())));

        entityManager.flush();
        entityManager.clear();

        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(reloaded.getChannel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(reloaded.getSegment().getId()).isEqualTo(segment.getId());
        assertThat(reloaded.getMessageSubject()).isEqualTo("Subject");
        assertThat(reloaded.getMessageBody()).isEqualTo("Body");
        assertThat(reloaded.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(reloaded.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(campaignProductRepository.findByCampaignId(created.id())).hasSize(1);
        assertThat(created.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(created.messageSubject()).isEqualTo("Subject");
        assertThat(created.messageBody()).isEqualTo("Body");
        assertThat(created.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(created.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(created.productIds()).containsExactly(product.getId());
    }

    @Test
    void fullLifecycleSubmitApproveLaunchThroughService() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Lifecycle campaign",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        CampaignView submitted = campaignService.submitCampaign(created.id());
        assertThat(submitted.status()).isEqualTo(CampaignStatus.SUBMITTED);

        List<AuditLog> submissionLogs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action",
                                AuditLog.class)
                        .setParameter("entityType", CampaignService.AUDIT_ENTITY_TYPE)
                        .setParameter("entityId", created.id())
                        .setParameter("action", "SUBMIT")
                        .getResultList();
        assertThat(submissionLogs).hasSize(1);
        assertThat(submissionLogs.getFirst().getActorUserId()).isEqualTo(owner.getId());
        assertThat(submissionLogs.getFirst().getOldValue()).containsEntry("status", "DRAFT");
        assertThat(submissionLogs.getFirst().getNewValue()).containsEntry("status", "SUBMITTED");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView approved = campaignService.approveCampaign(created.id());
        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.approvedByUserId()).isEqualTo(compliance.getId());

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView launched = campaignService.launchCampaign(created.id());
        assertThat(launched.status()).isEqualTo(CampaignStatus.ACTIVE);

        List<AuditLog> launchLogs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action",
                                AuditLog.class)
                        .setParameter("entityType", CampaignService.AUDIT_ENTITY_TYPE)
                        .setParameter("entityId", created.id())
                        .setParameter("action", "LAUNCH")
                        .getResultList();
        assertThat(launchLogs).hasSize(1);
        assertThat(launchLogs.getFirst().getActorUserId()).isEqualTo(owner.getId());
        assertThat(launchLogs.getFirst().getOldValue()).containsEntry("status", "APPROVED");
        assertThat(launchLogs.getFirst().getNewValue()).containsEntry("status", "ACTIVE");

        entityManager.flush();
        entityManager.clear();
        assertThat(campaignRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.ACTIVE);

        CampaignView paused = campaignService.pauseCampaign(created.id());
        assertThat(paused.status()).isEqualTo(CampaignStatus.PAUSED);

        CampaignView completed = campaignService.completeCampaign(created.id());
        assertThat(completed.status()).isEqualTo(CampaignStatus.COMPLETED);

        CampaignView archived = campaignService.archiveCampaign(created.id());
        assertThat(archived.status()).isEqualTo(CampaignStatus.ARCHIVED);

        entityManager.flush();
        entityManager.clear();
        assertThat(campaignRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.ARCHIVED);
    }

    @Test
    void cannotLaunchBeforeApprovalThroughService() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Blocked launch",
                                "Objective",
                                null,
                                CampaignChannel.SMS,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(created.id());

        assertThatThrownBy(() -> campaignService.launchCampaign(created.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only APPROVED");
    }

    @Test
    void complianceOfficerCanApproveSubmittedCampaignThroughService() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Compliance approval",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                "Approval subject",
                                "Approval body",
                                null,
                                null,
                                List.of()));
        CampaignView submitted = campaignService.submitCampaign(created.id());
        assertThat(submitted.status()).isEqualTo(CampaignStatus.SUBMITTED);

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView approved =
                campaignService.approveCampaign(
                        created.id(), new ApproveCampaignCommand("Compliance Officer approved."));

        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.approvedByUserId()).isEqualTo(compliance.getId());
        assertThat(approved.approvedByFullName()).isEqualTo("Campaign Service User");
        assertThat(approved.complianceReviewNotes()).isEqualTo("Compliance Officer approved.");

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(reloaded.getApprovedBy().getId()).isEqualTo(compliance.getId());
        assertThat(reloaded.getComplianceReviewNotes()).isEqualTo("Compliance Officer approved.");
    }

    @Test
    void complianceOfficerCanRejectSubmittedCampaignWithReasonThroughService() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Compliance rejection",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                "Rejection subject",
                                "Rejection body",
                                null,
                                null,
                                List.of()));
        CampaignView submitted = campaignService.submitCampaign(created.id());
        assertThat(submitted.status()).isEqualTo(CampaignStatus.SUBMITTED);

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView rejected =
                campaignService.rejectCampaign(
                        created.id(),
                        new RejectCampaignCommand(
                                "Missing consent language", "Add explicit opt-out wording."));

        assertThat(rejected.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Missing consent language");
        assertThat(rejected.complianceReviewNotes()).isEqualTo("Add explicit opt-out wording.");
        assertThat(rejected.approvedByUserId()).isNull();
        assertThat(rejected.approvedAt()).isNull();

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(reloaded.getRejectionReason()).isEqualTo("Missing consent language");
        assertThat(reloaded.getComplianceReviewNotes()).isEqualTo("Add explicit opt-out wording.");
        assertThat(reloaded.getApprovedBy()).isNull();
    }

    @Test
    void ownerCannotApproveOwnCampaignThroughService() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Self approve blocked",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(created.id());

        assertThatThrownBy(() -> campaignService.approveCampaign(created.id()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("cannot approve or reject own campaign");
    }

    @Test
    void rejectsSubmittedCampaignAndPersistsRejectionAuditLog() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Rejected audit",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(created.id());

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView rejected =
                campaignService.rejectCampaign(
                        created.id(),
                        new RejectCampaignCommand(
                                "Missing consent language", "Add consent wording"));

        assertThat(rejected.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Missing consent language");
        assertThat(rejected.complianceReviewNotes()).isEqualTo("Add consent wording");

        List<AuditLog> rejectionLogs =
                entityManager
                        .getEntityManager()
                        .createQuery(
                                "select a from AuditLog a where a.entityType = :entityType "
                                        + "and a.entityId = :entityId and a.action = :action",
                                AuditLog.class)
                        .setParameter("entityType", CampaignService.AUDIT_ENTITY_TYPE)
                        .setParameter("entityId", created.id())
                        .setParameter("action", "REJECT")
                        .getResultList();
        assertThat(rejectionLogs).hasSize(1);
        AuditLog rejectionLog = rejectionLogs.getFirst();
        assertThat(rejectionLog.getActorUserId()).isEqualTo(compliance.getId());
        assertThat(rejectionLog.getOldValue())
                .containsEntry("status", "SUBMITTED")
                .containsEntry("rejectionReason", null)
                .containsEntry("complianceReviewNotes", null);
        assertThat(rejectionLog.getNewValue())
                .containsEntry("status", "REJECTED")
                .containsEntry("rejectionReason", "Missing consent language")
                .containsEntry("complianceReviewNotes", "Add consent wording");
    }

    @Test
    void launchCampaignCreatesContactEventsForEligibleRecipients() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        entityManager.persistAndFlush(customer);

        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Launch Event Campaign",
                                "Launch Event Campaign Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        Campaign campaign = campaignRepository.findById(created.id()).orElseThrow();
        CampaignRecipient recipient = CampaignRecipient.eligible(campaign, customer);
        campaignRecipientRepository.saveAndFlush(recipient);

        campaignService.submitCampaign(created.id());

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.approveCampaign(created.id());

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        campaignService.launchCampaign(created.id());

        entityManager.flush();
        entityManager.clear();

        Campaign reloadedCampaign = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloadedCampaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);

        CampaignRecipient reloadedRecipient =
                campaignRecipientRepository.findById(recipient.getId()).orElseThrow();
        assertThat(reloadedRecipient.getEligibilityStatus())
                .isEqualTo(CampaignRecipientStatus.SENT);
        assertThat(reloadedRecipient.getSentAt()).isNotNull();

        List<ContactEvent> events = contactEventRepository.findByCampaignId(created.id());
        assertThat(events).hasSize(1);
        ContactEvent event = events.getFirst();
        assertThat(event.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(event.getEventType()).isEqualTo(ContactEventType.SENT);
        assertThat(event.getChannel()).isEqualTo(CommunicationChannel.EMAIL);
        assertThat(event.getCreatedBy().getId()).isEqualTo(owner.getId());
    }

    @Test
    void contactTimelineReturnsEventsInCorrectOrder() {
        Customer customer = Customer.create(CustomerType.PROSPECT, "John", "Doe");
        entityManager.persistAndFlush(customer);

        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Timeline Order Campaign",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        Campaign campaign = campaignRepository.findById(created.id()).orElseThrow();

        ContactEvent event1 =
                ContactEvent.sent(
                        customer,
                        campaign,
                        CommunicationChannel.EMAIL,
                        java.time.Instant.now().minusSeconds(60),
                        owner);
        ContactEvent event2 =
                ContactEvent.sent(
                        customer,
                        campaign,
                        CommunicationChannel.EMAIL,
                        java.time.Instant.now(),
                        owner);
        ContactEvent event3 =
                ContactEvent.sent(
                        customer,
                        campaign,
                        CommunicationChannel.EMAIL,
                        java.time.Instant.now().minusSeconds(120),
                        owner);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);
        entityManager.flush();
        entityManager.clear();

        List<ContactEvent> events = contactEventRepository.findByCustomerId(customer.getId());
        assertThat(events).hasSize(3);
        // Descending order: event2 (newest), event1, event3 (oldest)
        assertThat(events.get(0).getOccurredAt()).isEqualTo(event2.getOccurredAt());
        assertThat(events.get(1).getOccurredAt()).isEqualTo(event1.getOccurredAt());
        assertThat(events.get(2).getOccurredAt()).isEqualTo(event3.getOccurredAt());
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-service-integration.test",
                        "{noop}password",
                        "Campaign Service User");
        return entityManager.persistAndFlush(user);
    }
}
