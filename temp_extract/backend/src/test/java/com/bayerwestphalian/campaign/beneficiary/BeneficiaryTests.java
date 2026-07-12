package com.bayerwestphalian.campaign.beneficiary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class BeneficiaryTests {

    @Test
    void mapsKbBeneficiariesTableAsJpaEntity() {
        Table table = Beneficiary.class.getAnnotation(Table.class);

        assertThat(Beneficiary.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(table.name()).isEqualTo("beneficiaries");
        assertThat(table.uniqueConstraints())
                .extracting(UniqueConstraint::name)
                .contains("beneficiaries_unique_link");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<Beneficiary> constructor = Beneficiary.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbBeneficiaryColumnsAndValidationRules() throws Exception {
        assertColumn("id", "id", false, 255);
        assertColumn("relationship", "relationship", false, 100);
        assertColumn("guardianName", "guardian_name", true, 255);
        assertColumn("guardianEmail", "guardian_email", true, 255);
        assertColumn("guardianConsentRequired", "guardian_consent_required", false, 255);
        assertColumn("createdAt", "created_at", false, 255);

        assertThat(field("policyholderCustomer").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("beneficiaryCustomer").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("relationship").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("relationship").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("guardianName").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("guardianEmail").isAnnotationPresent(Email.class)).isTrue();
        assertThat(field("guardianEmail").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("id").getAnnotation(Column.class).updatable()).isFalse();
        assertThat(field("createdAt").getAnnotation(Column.class).updatable()).isFalse();
    }

    @Test
    void mapsPolicyholderAndBeneficiaryCustomerRelationships() throws Exception {
        assertCustomerJoin(
                "policyholderCustomer", "policyholder_customer_id", "Policyholder customer");
        assertCustomerJoin(
                "beneficiaryCustomer", "beneficiary_customer_id", "Beneficiary customer");
    }

    @Test
    void createsBeneficiaryLinkWithGuardianConsentOffByDefault() {
        Customer policyholder = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        Customer beneficiaryCustomer =
                Customer.create(CustomerType.BENEFICIARY, "Ben", "Beneficiary");

        Beneficiary beneficiary =
                Beneficiary.create(policyholder, beneficiaryCustomer, "Grandchild");

        assertThat(beneficiary.getPolicyholderCustomer()).isSameAs(policyholder);
        assertThat(beneficiary.getBeneficiaryCustomer()).isSameAs(beneficiaryCustomer);
        assertThat(beneficiary.getRelationship()).isEqualTo("Grandchild");
        assertThat(beneficiary.isGuardianConsentRequired()).isFalse();
        assertThat(beneficiary.hasGuardianRequirement()).isFalse();
        assertThat(beneficiary.getGuardianName()).isNull();
        assertThat(beneficiary.getGuardianEmail()).isNull();
    }

    @Test
    void rejectsSelfLinkToMatchKbDatabaseConstraint() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Beneficiary.create(customer, customer, "Self"))
                .withMessageContaining("must be different");
    }

    @Test
    void supportsRelationshipAndGuardianConsentWorkflow() {
        Beneficiary beneficiary =
                Beneficiary.create(
                        Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder"),
                        Customer.create(CustomerType.BENEFICIARY, "Minor", "Beneficiary"),
                        "Grandchild");

        beneficiary.updateRelationship("Grandchild - minor");
        beneficiary.requireGuardianConsent("Guardian User", "guardian@bayer-westphalian.test");

        assertThat(beneficiary.getRelationship()).isEqualTo("Grandchild - minor");
        assertThat(beneficiary.isGuardianConsentRequired()).isTrue();
        assertThat(beneficiary.hasGuardianRequirement()).isTrue();
        assertThat(beneficiary.getGuardianName()).isEqualTo("Guardian User");
        assertThat(beneficiary.getGuardianEmail()).isEqualTo("guardian@bayer-westphalian.test");

        beneficiary.updateGuardian("Updated Guardian", "updated.guardian@bayer-westphalian.test");
        beneficiary.clearGuardianConsentRequirement();

        assertThat(beneficiary.getGuardianName()).isEqualTo("Updated Guardian");
        assertThat(beneficiary.getGuardianEmail())
                .isEqualTo("updated.guardian@bayer-westphalian.test");
        assertThat(beneficiary.isGuardianConsentRequired()).isFalse();
    }

    @Test
    void minorBeneficiaryWithoutGuardianConsentRequiresExclusionUntilGuardianConsentExists() {
        Beneficiary beneficiary =
                Beneficiary.create(
                        Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder"),
                        Customer.create(CustomerType.BENEFICIARY, "Minor", "Beneficiary"),
                        "Grandchild - minor");

        beneficiary.requireGuardianConsent("Guardian User", "guardian@bayer-westphalian.test");

        assertThat(beneficiary.isGuardianConsentRequired()).isTrue();
        assertThat(beneficiary.hasGuardianRequirement()).isTrue();
    }

    @Test
    void prePersistCreatesIdAndCreatedAtForKbCreatedAtColumn() throws Exception {
        Beneficiary beneficiary =
                Beneficiary.create(
                        Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder"),
                        Customer.create(CustomerType.BENEFICIARY, "Ben", "Beneficiary"),
                        "Grandchild");
        Method onCreate = Beneficiary.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        onCreate.invoke(beneficiary);

        assertThat(beneficiary.getId()).isNotNull();
        assertThat(beneficiary.getCreatedAt()).isNotNull();
    }

    private static void assertCustomerJoin(
            String fieldName, String columnName, String relationshipDescription) throws Exception {
        Field field = field(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).as(relationshipDescription).isEqualTo(Customer.class);
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isFalse();
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return Beneficiary.class.getDeclaredField(fieldName);
    }
}
