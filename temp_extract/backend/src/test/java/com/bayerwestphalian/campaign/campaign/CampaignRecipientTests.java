package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 264/265: CampaignRecipient maps recipient rows for campaign audiences. */
class CampaignRecipientTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000265");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000265");
    private static final UUID OWNER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000265");

    @Test
    void mapsKbCampaignRecipientsTableAsJpaEntity() throws Exception {
        assertThat(CampaignRecipient.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(CampaignRecipient.class.getAnnotation(Table.class).name())
                .isEqualTo("campaign_recipients");
        assertThat(field("id").getAnnotation(Column.class).name()).isEqualTo("id");
    }

    @Test
    void mapsCampaignCustomerUniqueConstraintToPreventDuplicateRecipientRows() {
        Table table = CampaignRecipient.class.getAnnotation(Table.class);

        assertThat(table.uniqueConstraints())
                .extracting(UniqueConstraint::name)
                .contains("campaign_recipients_campaign_customer_unique");
        assertThat(
                        Arrays.stream(table.uniqueConstraints())
                                .filter(
                                        constraint ->
                                                constraint.name()
                                                        .equals(
                                                                "campaign_recipients_campaign_customer_unique"))
                                .findFirst()
                                .orElseThrow()
                                .columnNames())
                .containsExactly("campaign_id", "customer_id");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<CampaignRecipient> constructor =
                CampaignRecipient.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsCampaignAndCustomerRelationships() throws Exception {
        assertManyToOne("campaign", "campaign_id");
        assertManyToOne("customer", "customer_id");
        assertThat(field("campaign").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("customer").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void mapsNamedEnumStatusAndRecipientDetailColumns() throws Exception {
        Field status = field("eligibilityStatus");

        assertThat(status.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        assertThat(status.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.NAMED_ENUM);
        assertThat(status.getAnnotation(Column.class).columnDefinition())
                .isEqualTo("campaign_recipient_status");
        assertThat(field("exclusionReason").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
        assertThat(field("eligibilityExplanation").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
    }

    @Test
    void statusEnumMatchesKbDatabaseValues() {
        assertThat(Arrays.stream(CampaignRecipientStatus.values()).map(Enum::name))
                .containsExactly(
                        "ELIGIBLE",
                        "EXCLUDED",
                        "SENT",
                        "OPENED",
                        "CLICKED",
                        "REPLIED",
                        "CONVERTED",
                        "FAILED");
    }

    @Test
    void createsEligibleRecipientForCampaignAndCustomer() {
        Campaign campaign = campaign();
        Customer customer = customer();

        CampaignRecipient recipient =
                CampaignRecipient.eligible(
                        campaign, customer, "  Customer is eligible for campaign contact  ");

        assertThat(recipient.getCampaign()).isSameAs(campaign);
        assertThat(recipient.getCustomer()).isSameAs(customer);
        assertThat(recipient.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(recipient.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.ELIGIBLE);
        assertThat(recipient.getEligibilityExplanation())
                .isEqualTo("Customer is eligible for campaign contact");
        assertThat(recipient.isEligible()).isTrue();
        assertThat(recipient.isExcluded()).isFalse();
    }

    @Test
    void createsExcludedRecipientWithReasonAndExplanation() {
        CampaignRecipient recipient =
                CampaignRecipient.excluded(
                        campaign(), customer(), "  DO_NOT_CONTACT  ", "  Consent missing  ");

        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.EXCLUDED);
        assertThat(recipient.getExclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(recipient.getEligibilityExplanation()).isEqualTo("Consent missing");
        assertThat(recipient.isExcluded()).isTrue();
    }

    @Test
    void rejectsMissingRequiredReferencesOrExclusionReason() {
        Campaign campaign = campaign();
        Customer customer = customer();

        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> CampaignRecipient.eligible(null, customer))
                .withMessageContaining("Campaign is required");
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> CampaignRecipient.eligible(campaign, null))
                .withMessageContaining("Customer is required");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> CampaignRecipient.excluded(campaign, customer, " ", null))
                .withMessageContaining("Exclusion reason is required");
    }

    @Test
    void tracksDeliveryStatusMilestones() {
        CampaignRecipient recipient = CampaignRecipient.eligible(campaign(), customer());

        recipient.markSent();
        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.SENT);
        assertThat(recipient.getSentAt()).isNotNull();

        recipient.markOpened();
        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.OPENED);
        assertThat(recipient.getOpenedAt()).isNotNull();

        recipient.markClicked();
        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.CLICKED);
        assertThat(recipient.getClickedAt()).isNotNull();

        recipient.markConverted();
        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.CONVERTED);
        assertThat(recipient.getConvertedAt()).isNotNull();

        recipient.markFailed("  Delivery bounced  ");
        assertThat(recipient.getEligibilityStatus()).isEqualTo(CampaignRecipientStatus.FAILED);
        assertThat(recipient.getEligibilityExplanation()).isEqualTo("Delivery bounced");
    }

    @Test
    void initializesIdAndCreatedAtBeforePersist() {
        CampaignRecipient recipient = CampaignRecipient.eligible(campaign(), customer());

        recipient.onCreate();

        assertThat(recipient.getId()).isNotNull();
        assertThat(recipient.getCreatedAt()).isNotNull();
    }

    private static void assertManyToOne(String fieldName, String joinColumnName) throws Exception {
        Field relationship = field(fieldName);
        ManyToOne manyToOne = relationship.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = relationship.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo(joinColumnName);
        assertThat(joinColumn.nullable()).isFalse();
    }

    private static Field field(String name) throws Exception {
        Field field = CampaignRecipient.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Campaign campaign() {
        User owner =
                User.create(
                        "campaign-recipient-owner@test.example",
                        "{noop}password",
                        "Campaign Recipient Owner");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Campaign campaign =
                Campaign.create(
                        "Recipient campaign",
                        "Build a recipient audience",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.INDIVIDUAL, "Ada", "Lovelace");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        return customer;
    }
}
