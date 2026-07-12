package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.segment.Segment;
import com.bayerwestphalian.campaign.segment.SegmentVisibility;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
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

/**
 * KB item 220: campaign details endpoint path through {@link CampaignService#findById} returns full
 * definition including segment, products, and lifecycle fields.
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
class CampaignDetailsEndpointIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_details_endpoint_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private CampaignService campaignService;

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
        owner = persistUser("details-endpoint-owner");
        compliance = persistUser("details-endpoint-compliance");
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        when(authorizationExpressions.hasRole(SystemRoleName.ADMIN.name())).thenReturn(false);
        when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @Test
    void detailsEndpointReturnsFullDraftCampaignDefinition() {
        Segment segment =
                entityManager.persistAndFlush(
                        Segment.create(
                                "Munich prospects",
                                "Location audience",
                                owner,
                                SegmentVisibility.TEAM));
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
                                "Life renewal outreach",
                                "Promote life insurance renewals",
                                segment.getId(),
                                CampaignChannel.EMAIL,
                                "Renew your cover",
                                "Dear customer, ...",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                List.of(product.getId())));

        entityManager.flush();
        entityManager.clear();

        // Mirrors GET /api/campaigns/{id}
        CampaignView details = campaignService.findById(created.id());

        assertThat(details.id()).isEqualTo(created.id());
        assertThat(details.name()).isEqualTo("Life renewal outreach");
        assertThat(details.objective()).isEqualTo("Promote life insurance renewals");
        assertThat(details.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(details.ownerUserId()).isEqualTo(owner.getId());
        assertThat(details.ownerFullName()).isNotBlank();
        assertThat(details.segmentId()).isEqualTo(segment.getId());
        assertThat(details.segmentName()).isEqualTo("Munich prospects");
        assertThat(details.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(details.messageSubject()).isEqualTo("Renew your cover");
        assertThat(details.messageBody()).isEqualTo("Dear customer, ...");
        assertThat(details.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(details.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(details.productIds()).containsExactly(product.getId());
        assertThat(details.approvedByUserId()).isNull();
        assertThat(details.rejectionReason()).isNull();
        assertThat(details.createdAt()).isNotNull();
        assertThat(details.updatedAt()).isNotNull();
    }

    @Test
    void detailsEndpointReflectsLifecycleStateAfterSubmitApproveReject() {
        CampaignView created =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Lifecycle details",
                                "Objective",
                                null,
                                CampaignChannel.SMS,
                                "Subject",
                                "Body",
                                null,
                                null,
                                List.of()));

        campaignService.submitCampaign(created.id());
        CampaignView submitted = campaignService.findById(created.id());
        assertThat(submitted.status()).isEqualTo(CampaignStatus.SUBMITTED);

        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.approveCampaign(created.id());
        CampaignView approved = campaignService.findById(created.id());
        assertThat(approved.status()).isEqualTo(CampaignStatus.APPROVED);
        assertThat(approved.approvedByUserId()).isEqualTo(compliance.getId());
        assertThat(approved.approvedByFullName()).isNotBlank();
        assertThat(approved.approvedAt()).isNotNull();

        // New campaign for reject path
        when(authorizationExpressions.currentUserId()).thenReturn(owner.getId());
        CampaignView toReject =
                campaignService.createCampaign(
                        new CreateCampaignCommand(
                                "Reject details",
                                "Objective",
                                null,
                                CampaignChannel.EMAIL,
                                null,
                                null,
                                null,
                                null,
                                List.of()));
        campaignService.submitCampaign(toReject.id());
        when(authorizationExpressions.currentUserId()).thenReturn(compliance.getId());
        campaignService.rejectCampaign(
                toReject.id(), new RejectCampaignCommand("Missing consent language"));

        CampaignView rejected = campaignService.findById(toReject.id());
        assertThat(rejected.status()).isEqualTo(CampaignStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Missing consent language");
        assertThat(rejected.approvedByUserId()).isNull();
        assertThat(rejected.approvedAt()).isNull();
    }

    @Test
    void detailsEndpointThrowsNotFoundForUnknownId() {
        UUID missing = UUID.fromString("50000000-0000-0000-0000-00000000dead");

        assertThatThrownBy(() -> campaignService.findById(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign");
    }

    @Test
    void detailsEndpointRejectsNullCampaignId() {
        assertThatThrownBy(() -> campaignService.findById(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign validation failed");
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-details-endpoint-integration.test",
                        "{noop}password",
                        "Campaign Details Endpoint User");
        return entityManager.persistAndFlush(user);
    }
}
