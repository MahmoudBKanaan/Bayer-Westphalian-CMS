package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** KB item 265: CampaignRecipientRepository queries against PostgreSQL recipient rows. */
@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CampaignRecipientRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_campaign_recipient_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;

    @Autowired private CampaignRecipientRepository campaignRecipientRepository;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Test
    void findsCampaignRecipientsByCampaignAndEligibilityStatus() {
        User owner = persistUser("recipient-owner");
        Campaign campaign = persistCampaign("Recipient campaign", owner);
        Campaign otherCampaign = persistCampaign("Other recipient campaign", owner);
        Customer eligibleCustomer = persistCustomer("Ada", "Lovelace");
        Customer excludedCustomer = persistCustomer("Grace", "Hopper");
        Customer otherCustomer = persistCustomer("Katherine", "Johnson");
        CampaignRecipient eligible =
                persistRecipient(CampaignRecipient.eligible(campaign, eligibleCustomer));
        CampaignRecipient excluded =
                persistRecipient(
                        CampaignRecipient.excluded(
                                campaign,
                                excludedCustomer,
                                "DO_NOT_CONTACT",
                                "Customer opted out"));
        persistRecipient(CampaignRecipient.eligible(otherCampaign, otherCustomer));
        entityManager.clear();

        List<CampaignRecipient> campaignRecipients =
                campaignRecipientRepository.findByCampaignId(campaign.getId());
        List<CampaignRecipient> eligibleRecipients =
                campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                        campaign.getId(), CampaignRecipientStatus.ELIGIBLE);

        assertThat(campaignRecipients)
                .extracting(CampaignRecipient::getId)
                .containsExactlyInAnyOrder(eligible.getId(), excluded.getId());
        assertThat(eligibleRecipients)
                .extracting(CampaignRecipient::getId)
                .containsExactly(eligible.getId());
        assertThat(
                        campaignRecipientRepository.countByCampaignIdAndEligibilityStatus(
                                campaign.getId(), CampaignRecipientStatus.EXCLUDED))
                .isEqualTo(1L);
    }

    @Test
    void findsRecipientByCampaignCustomerPairAndCustomerHistory() {
        User owner = persistUser("recipient-history-owner");
        Customer customer = persistCustomer("Alan", "Turing");
        Campaign firstCampaign = persistCampaign("First recipient campaign", owner);
        Campaign secondCampaign = persistCampaign("Second recipient campaign", owner);
        CampaignRecipient first =
                persistRecipient(CampaignRecipient.eligible(firstCampaign, customer));
        CampaignRecipient second =
                persistRecipient(
                        CampaignRecipient.excluded(
                                secondCampaign, customer, "NO_MATCH", "Not in target segment"));
        entityManager.clear();

        assertThat(
                        campaignRecipientRepository.findByCampaignIdAndCustomerId(
                                firstCampaign.getId(), customer.getId()))
                .hasValueSatisfying(
                        recipient -> assertThat(recipient.getId()).isEqualTo(first.getId()));
        assertThat(
                        campaignRecipientRepository.existsByCampaignIdAndCustomerId(
                                secondCampaign.getId(), customer.getId()))
                .isTrue();
        assertThat(campaignRecipientRepository.findByCustomerId(customer.getId()))
                .extracting(CampaignRecipient::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void storesExclusionReasonAndEligibilityExplanationForExcludedRecipient() {
        User owner = persistUser("recipient-exclusion-reason-owner");
        Campaign campaign = persistCampaign("Exclusion reason campaign", owner);
        Customer customer = persistCustomer("Grace", "Excluded");
        CampaignRecipient excluded =
                persistRecipient(
                        CampaignRecipient.excluded(
                                campaign,
                                customer,
                                "INVALID_CONSENT",
                                "Customer does not have valid required consent"));
        entityManager.clear();

        CampaignRecipient reloaded = entityManager.find(CampaignRecipient.class, excluded.getId());

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(reloaded.getExclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(reloaded.getEligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        assertThat(
                        campaignRecipientRepository.findByCampaignIdAndEligibilityStatus(
                                campaign.getId(), CampaignRecipientStatus.EXCLUDED))
                .extracting(CampaignRecipient::getExclusionReason)
                .containsExactly("INVALID_CONSENT");
    }

    @Test
    void storesEligibilityExplanationForEligibleRecipient() {
        User owner = persistUser("recipient-eligibility-explanation-owner");
        Campaign campaign = persistCampaign("Eligibility explanation campaign", owner);
        Customer customer = persistCustomer("Ada", "Eligible");
        CampaignRecipient eligible =
                persistRecipient(
                        CampaignRecipient.eligible(
                                campaign, customer, "Customer is eligible for campaign contact"));
        entityManager.clear();

        CampaignRecipient reloaded = entityManager.find(CampaignRecipient.class, eligible.getId());

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.ELIGIBLE);
        assertThat(reloaded.getExclusionReason()).isNull();
        assertThat(reloaded.getEligibilityExplanation())
                .isEqualTo("Customer is eligible for campaign contact");
    }

    @Test
    void rejectsDuplicateRecipientRowsForSameCampaignAndCustomer() {
        User owner = persistUser("recipient-duplicate-owner");
        Campaign campaign = persistCampaign("Duplicate recipient campaign", owner);
        Customer customer = persistCustomer("Ada", "Duplicate");
        persistRecipient(CampaignRecipient.eligible(campaign, customer));

        assertThatThrownBy(
                        () ->
                                persistRecipient(
                                        CampaignRecipient.excluded(
                                                campaign,
                                                customer,
                                                "DUPLICATE_CAMPAIGN_RECIPIENT",
                                                "Customer is already assigned to this campaign")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("campaign_recipients_campaign_customer_unique");
    }

    private User persistUser(String emailPrefix) {
        User user =
                User.create(
                        emailPrefix + "@campaign-recipient-repository-integration.test",
                        "{noop}password",
                        "Campaign Recipient Repository User");
        return persistAndFlush(user);
    }

    private Campaign persistCampaign(String name, User owner) {
        Campaign campaign =
                Campaign.create(name, "Audience for " + name, owner, null, CampaignChannel.EMAIL);
        return persistAndFlush(campaign);
    }

    private Customer persistCustomer(String firstName, String lastName) {
        Customer customer = Customer.create(CustomerType.PROSPECT, firstName, lastName);
        return persistAndFlush(customer);
    }

    private CampaignRecipient persistRecipient(CampaignRecipient recipient) {
        return persistAndFlush(recipient);
    }

    private <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
