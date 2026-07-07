package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class ConsentRecordTests {

    @Test
    void mapsKbConsentRecordsTableAsJpaEntity() {
        assertThat(ConsentRecord.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ConsentRecord.class.getAnnotation(Table.class).name())
                .isEqualTo("consent_records");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<ConsentRecord> constructor = ConsentRecord.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbConsentRecordColumnsAndValidationRules() throws Exception {
        assertColumn("id", "id", false, 255);
        assertColumn("consentType", "consent_type", false, 255);
        assertColumn("status", "status", false, 255);
        assertColumn("purpose", "purpose", false, 255);
        assertColumn("source", "source", true, 100);
        assertColumn("grantedAt", "granted_at", true, 255);
        assertColumn("withdrawnAt", "withdrawn_at", true, 255);
        assertColumn("expiresAt", "expires_at", true, 255);
        assertColumn("evidenceFileUrl", "evidence_file_url", true, 255);
        assertColumn("createdAt", "created_at", false, 255);

        assertThat(field("id").isAnnotationPresent(Id.class)).isTrue();
        assertThat(field("customer").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("consentType").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("status").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("purpose").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("source").getAnnotation(Size.class).max()).isEqualTo(100);
    }

    @Test
    void mapsCustomerAndCreatorRelationshipsToKbForeignKeys() throws Exception {
        assertManyToOne("customer", "customer_id", false);
        assertManyToOne("createdBy", "created_by", true);
    }

    @Test
    void mapsTypeAndStatusToKbPostgreSqlEnums() throws Exception {
        assertNativeEnumColumn("consentType", "consent_type");
        assertNativeEnumColumn("status", "consent_status");
        assertThat(field("consentType").getAnnotation(JdbcTypeCode.class).value())
                .isEqualTo(SqlTypes.NAMED_ENUM);
        assertThat(field("status").getAnnotation(JdbcTypeCode.class).value())
                .isEqualTo(SqlTypes.NAMED_ENUM);
    }

    @Test
    void exposesKbConsentEnumValues() {
        assertThat(ConsentType.values())
                .containsExactly(
                        ConsentType.MARKETING_EMAIL,
                        ConsentType.MARKETING_PHONE,
                        ConsentType.MARKETING_SMS,
                        ConsentType.GUARDIAN,
                        ConsentType.DATA_PROCESSING);
        assertThat(ConsentStatus.values())
                .containsExactly(
                        ConsentStatus.GIVEN,
                        ConsentStatus.WITHDRAWN,
                        ConsentStatus.REQUIRED,
                        ConsentStatus.EXPIRED,
                        ConsentStatus.REJECTED);
    }

    @Test
    void createsConsentRecordWithKbRequiredFields() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");

        ConsentRecord consentRecord =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.REQUIRED,
                        "Marketing communication consent",
                        "PHONE");

        assertThat(consentRecord.getCustomer()).isSameAs(customer);
        assertThat(consentRecord.getConsentType()).isEqualTo(ConsentType.MARKETING_EMAIL);
        assertThat(consentRecord.getStatus()).isEqualTo(ConsentStatus.REQUIRED);
        assertThat(consentRecord.getPurpose()).isEqualTo("Marketing communication consent");
        assertThat(consentRecord.getSource()).isEqualTo("PHONE");
        assertThat(consentRecord.requiresAction(Instant.parse("2026-07-06T12:00:00Z"))).isTrue();
    }

    @Test
    void supportsKbGrantWithdrawExpireAndRejectWorkflow() {
        Instant grantedAt = Instant.parse("2026-07-01T12:00:00Z");
        Instant expiresAt = Instant.parse("2026-08-01T12:00:00Z");
        Instant withdrawnAt = Instant.parse("2026-07-10T12:00:00Z");
        User createdBy =
                User.create(
                        "agent@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Customer Service Agent");
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder"),
                        ConsentType.MARKETING_SMS,
                        ConsentStatus.REQUIRED,
                        "SMS campaign consent",
                        "WEB_FORM");

        consentRecord.grant(grantedAt, expiresAt, "s3://evidence/consent.pdf", createdBy);

        assertThat(consentRecord.getStatus()).isEqualTo(ConsentStatus.GIVEN);
        assertThat(consentRecord.getGrantedAt()).isEqualTo(grantedAt);
        assertThat(consentRecord.getWithdrawnAt()).isNull();
        assertThat(consentRecord.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(consentRecord.getEvidenceFileUrl()).isEqualTo("s3://evidence/consent.pdf");
        assertThat(consentRecord.getCreatedBy()).isSameAs(createdBy);
        assertThat(consentRecord.isValid(Instant.parse("2026-07-15T12:00:00Z"))).isTrue();
        assertThat(consentRecord.requiresAction(Instant.parse("2026-08-02T12:00:00Z"))).isTrue();

        consentRecord.withdraw(withdrawnAt);

        assertThat(consentRecord.getStatus()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(consentRecord.getWithdrawnAt()).isEqualTo(withdrawnAt);
        assertThat(consentRecord.isValid(Instant.parse("2026-07-15T12:00:00Z"))).isFalse();

        consentRecord.expire();
        assertThat(consentRecord.getStatus()).isEqualTo(ConsentStatus.EXPIRED);

        consentRecord.reject();
        assertThat(consentRecord.getStatus()).isEqualTo(ConsentStatus.REJECTED);
    }

    @Test
    void prePersistCreatesIdAndCreatedAtForKbColumns() throws Exception {
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder"),
                        ConsentType.DATA_PROCESSING,
                        ConsentStatus.GIVEN,
                        "Data processing consent",
                        "IMPORT");
        Method onCreate = ConsentRecord.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        onCreate.invoke(consentRecord);

        assertThat(consentRecord.getId()).isNotNull();
        assertThat(consentRecord.getCreatedAt()).isNotNull();
    }

    private static void assertManyToOne(String fieldName, String columnName, boolean optional)
            throws Exception {
        Field relationshipField = field(fieldName);
        ManyToOne manyToOne = relationshipField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = relationshipField.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isEqualTo(optional);
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isEqualTo(optional);
    }

    private static void assertNativeEnumColumn(String fieldName, String columnDefinition)
            throws Exception {
        Field enumField = field(fieldName);
        Column column = enumField.getAnnotation(Column.class);
        Enumerated enumerated = enumField.getAnnotation(Enumerated.class);

        assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return ConsentRecord.class.getDeclaredField(fieldName);
    }
}
