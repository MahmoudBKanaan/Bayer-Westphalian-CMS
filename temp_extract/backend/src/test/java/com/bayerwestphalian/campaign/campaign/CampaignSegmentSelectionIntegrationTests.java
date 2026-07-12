package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
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

/**
 * KB item 222 / FR-053: campaign segment selection assigns or clears {@code campaigns.segment_id}
 * for draft/rejected campaigns only.
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
class CampaignSegmentSelectionIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_segment_selection_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;
    @Autowired private CampaignRepository campaignRepository;

    @MockBean private AuthorizationExpressions authorizationExpressions;

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
        owner = persistUser("segment-selection-owner");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void selectsReusableSegmentAndReadsItBack() {
        Segment segment = persistSegment("Munich prospects");
        CampaignView campaign = createDraft("Segment selection campaign");

        CampaignView updated =
                campaignService.selectSegment(
                        campaign.id(),
                        new SelectCampaignSegmentRequest(segment.getId()).toCommand());

        assertThat(updated.segmentId()).isEqualTo(segment.getId());
        assertThat(updated.segmentName()).isEqualTo("Munich prospects");
        assertThat(campaignService.getSelectedSegmentId(campaign.id())).isEqualTo(segment.getId());

        entityManager.flush();
        entityManager.clear();
        Campaign reloaded = campaignRepository.findById(campaign.id()).orElseThrow();
        assertThat(reloaded.getSegmentId()).isEqualTo(segment.getId());
        assertThat(reloaded.getSegment().getName()).isEqualTo("Munich prospects");
    }

    @Test
    void createCampaignAcceptsInitialSegmentSelection() {
        Segment segment = persistSegment("Create-time segment");

        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Created with segment",
                                "Objective",
                                segment.getId(),
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        assertThat(created.segmentId()).isEqualTo(segment.getId());
        assertThat(created.segmentName()).isEqualTo("Create-time segment");
    }

    @Test
    void updateCampaignChangesSegmentSelection() {
        Segment first = persistSegment("First segment");
        Segment second = persistSegment("Second segment");
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Update segment",
                                "Objective",
                                first.getId(),
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));

        CampaignView updated =
                campaignService.updateCampaign(
                        created.id(),
                        new UpdateCampaignCommand(
                                "Update segment",
                                "Objective",
                                second.getId(),
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                null));

        assertThat(updated.segmentId()).isEqualTo(second.getId());
        assertThat(updated.segmentName()).isEqualTo("Second segment");
    }

    @Test
    void selectSegmentReplacesPreviousAssignment() {
        Segment first = persistSegment("Original");
        Segment second = persistSegment("Replacement");
        CampaignView campaign = createDraft("Replace segment");
        campaignService.selectSegment(
                campaign.id(), new SelectCampaignSegmentCommand(first.getId()));

        CampaignView updated =
                campaignService.selectSegment(
                        campaign.id(), new SelectCampaignSegmentCommand(second.getId()));

        assertThat(updated.segmentId()).isEqualTo(second.getId());
        assertThat(campaignService.getSelectedSegmentId(campaign.id())).isEqualTo(second.getId());
    }

    @Test
    void selectSegmentClearsAssignmentWhenNull() {
        Segment segment = persistSegment("Clearable");
        CampaignView campaign = createDraft("Clear segment");
        campaignService.selectSegment(
                campaign.id(), new SelectCampaignSegmentCommand(segment.getId()));

        CampaignView cleared =
                campaignService.selectSegment(
                        campaign.id(), new SelectCampaignSegmentCommand(null));

        assertThat(cleared.segmentId()).isNull();
        assertThat(cleared.segmentName()).isNull();
        assertThat(campaignService.getSelectedSegmentId(campaign.id())).isNull();
        assertThat(campaignRepository.findById(campaign.id()).orElseThrow().getSegment()).isNull();
    }

    @Test
    void rejectsUnknownSegmentId() {
        CampaignView campaign = createDraft("Unknown segment");
        UUID missing = UUID.fromString("42000000-0000-0000-0000-00000000dead");

        assertThatThrownBy(
                        () ->
                                campaignService.selectSegment(
                                        campaign.id(),
                                        new SelectCampaignSegmentCommand(missing)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Segment");
    }

    @Test
    void rejectsSegmentSelectionOnSubmittedCampaign() {
        Segment segment = persistSegment("Too late");
        CampaignView campaign = createDraft("Submitted segment blocked");
        campaignService.submitCampaign(campaign.id());

        assertThatThrownBy(
                        () ->
                                campaignService.selectSegment(
                                        campaign.id(),
                                        new SelectCampaignSegmentCommand(segment.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be changed");
    }

    @Test
    void allowsSegmentSelectionOnRejectedCampaign() {
        Segment segment = persistSegment("After reject");
        CampaignView campaign = createDraft("Rejected segment allowed");
        campaignService.submitCampaign(campaign.id());

        User compliance = persistUser("segment-selection-compliance");
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                campaign.id(), new RejectCampaignCommand("Fix segment"));

        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView updated =
                campaignService.selectSegment(
                        campaign.id(), new SelectCampaignSegmentCommand(segment.getId()));

        assertThat(updated.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(updated.segmentId()).isEqualTo(segment.getId());
    }

    private CampaignView createDraft(String name) {
        return campaignService.createCampaign(
                new CreateCampaignCommand(
                        name,
                        "Objective for " + name,
                        null,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        null,
                        null,
                        List.of()));
    }

    private Segment persistSegment(String name) {
        return entityManager.persistAndFlush(
                Segment.create(name, "Reusable audience", owner, SegmentVisibility.TEAM));
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-segment-selection-integration.test",
                        "{noop}password",
                        "Campaign Segment Selection User");
        return entityManager.persistAndFlush(user);
    }
}
