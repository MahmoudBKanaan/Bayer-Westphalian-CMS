package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
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
 * KB item 232: formal rejection reason persists on reject and clears on resubmit/approve.
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
class CampaignRejectionReasonIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_rejection_reason_tests")
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
        owner = persistUser("rejection-reason-owner");
        compliance = persistUser("rejection-reason-officer");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void rejectPersistsRequiredRejectionReason() {
        CampaignView created = createAndSubmit("Reject with reason");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView rejected =
                campaignService.rejectCampaign(
                        created.id(),
                        new RejectCampaignCommand("Missing guardian consent evidence"));

        assertThat(rejected.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Missing guardian consent evidence");

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(reloaded.getRejectionReason()).isEqualTo("Missing guardian consent evidence");
    }

    @Test
    void rejectWithoutReasonFailsValidationAndLeavesCampaignSubmitted() {
        CampaignView created = createAndSubmit("Reject without reason");

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        assertThatThrownBy(
                        () ->
                                campaignService.rejectCampaign(
                                        created.id(), new RejectCampaignCommand("  ")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("rejection");

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(reloaded.getRejectionReason()).isNull();
    }

    @Test
    void resubmitClearsPersistedRejectionReason() {
        CampaignView created = createAndSubmit("Resubmit clears reason");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                created.id(), new RejectCampaignCommand("Needs clearer objective"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        campaignService.updateCampaign(
                created.id(),
                new UpdateCampaignCommand(
                        "Resubmit clears reason",
                        "Clearer objective for compliance",
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        null));
        CampaignView resubmitted = campaignService.submitCampaign(created.id());

        assertThat(resubmitted.status()).isEqualTo(CampaignStatus.SUBMITTED);
        assertThat(resubmitted.rejectionReason()).isNull();

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(created.id()).orElseThrow();
        assertThat(reloaded.getRejectionReason()).isNull();
    }

    @Test
    void approveAfterResubmitKeepsRejectionReasonCleared() {
        CampaignView created = createAndSubmit("Approve after reject");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                created.id(), new RejectCampaignCommand("First-pass rejection"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        campaignService.updateCampaign(
                created.id(),
                new UpdateCampaignCommand(
                        "Approve after reject",
                        "Addressed first-pass feedback",
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        null));
        campaignService.submitCampaign(created.id());

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        CampaignView approved =
                campaignService.approveCampaign(created.id(), new ApproveCampaignCommand(null));

        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.rejectionReason()).isNull();
    }

    @Test
    void findByIdExposesRejectionReasonOnRejectedCampaign() {
        CampaignView created = createAndSubmit("Details show reason");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                created.id(), new RejectCampaignCommand("Audience segment incomplete"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView details = campaignService.findById(created.id());

        assertThat(details.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(details.rejectionReason()).isEqualTo("Audience segment incomplete");
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
                        emailPrefix + "@campaign-rejection-reason-integration.test",
                        "{noop}password",
                        "Campaign Rejection Reason User");
        return entityManager.persistAndFlush(user);
    }
}
