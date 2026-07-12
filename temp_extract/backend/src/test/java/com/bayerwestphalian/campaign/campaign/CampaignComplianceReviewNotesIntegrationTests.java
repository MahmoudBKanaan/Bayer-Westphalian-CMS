package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
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

/**
 * KB item 231: compliance review notes persist on approve/reject and dedicated notes update.
 */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({CampaignService.class, AuditService.class})
class CampaignComplianceReviewNotesIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_compliance_notes_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;

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
        owner = persistUser("compliance-notes-owner");
        compliance = persistUser("compliance-notes-officer");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void approvePersistsComplianceReviewNotes() {
        CampaignView created = createAndSubmit("Approve with notes");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView approved =
                campaignService.approveCampaign(
                        created.id(),
                        new ApproveCampaignCommand("Consent and segment eligibility verified."));

        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.complianceReviewNotes())
                .isEqualTo("Consent and segment eligibility verified.");

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getComplianceReviewNotes())
                .isEqualTo("Consent and segment eligibility verified.");
    }

    @Test
    void rejectPersistsReasonAndComplianceReviewNotes() {
        CampaignView created = createAndSubmit("Reject with notes");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView rejected =
                campaignService.rejectCampaign(
                        created.id(),
                        new RejectCampaignCommand(
                                "Message incomplete",
                                "Add unsubscribe language and guardian consent proof."));

        assertThat(rejected.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Message incomplete");
        assertThat(rejected.complianceReviewNotes())
                .isEqualTo("Add unsubscribe language and guardian consent proof.");

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getRejectionReason()).isEqualTo("Message incomplete");
        assertThat(reloaded.getComplianceReviewNotes())
                .isEqualTo("Add unsubscribe language and guardian consent proof.");
    }

    @Test
    void recordsNotesOnSubmittedCampaignWithoutStatusChange() {
        CampaignView created = createAndSubmit("Notes while submitted");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView noted =
                campaignService.recordComplianceReviewNotes(
                        created.id(), "Waiting on legal for SMS wording.");

        assertThat(noted.status()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(noted.complianceReviewNotes()).isEqualTo("Waiting on legal for SMS wording.");
    }

    @Test
    void resubmitClearsComplianceReviewNotesAndRejectionReason() {
        CampaignView created = createAndSubmit("Resubmit clears notes");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                created.id(),
                new RejectCampaignCommand("Needs work", "First review notes"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        campaignService.updateCampaign(
                created.id(),
                new UpdateCampaignCommand(
                        "Resubmit clears notes",
                        "Fixed objective",
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        null));
        CampaignView resubmitted = campaignService.submitCampaign(created.id());

        assertThat(resubmitted.status()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(resubmitted.complianceReviewNotes()).isNull();
        assertThat(resubmitted.rejectionReason()).isNull();
    }

    private CampaignView createAndSubmit(String name) {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                name,
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        return campaignService.submitCampaign(created.id());
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-compliance-notes-integration.test",
                        "{noop}password",
                        "Campaign Compliance Notes User");
        return entityManager.persistAndFlush(user);
    }
}
