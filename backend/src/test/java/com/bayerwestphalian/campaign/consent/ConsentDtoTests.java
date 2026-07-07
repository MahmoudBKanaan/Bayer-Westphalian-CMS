package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ConsentDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000101");
    private static final UUID CONSENT_ID =
            UUID.fromString("22000000-0000-0000-0000-000000000101");
    private static final UUID CREATED_BY_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");

    @Test
    void validatesRecordConsentRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(RecordConsentRequest.class, "customerId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(RecordConsentRequest.class, "consentType")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(field(RecordConsentRequest.class, "status").isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(RecordConsentRequest.class, "purpose")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(RecordConsentRequest.class, "source").getAnnotation(Size.class).max())
                .isEqualTo(100);
    }

    @Test
    void validatesWithdrawConsentRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(WithdrawConsentRequest.class, "consentRecordId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
    }

    @Test
    void mapsRecordAndWithdrawRequestsToCommands() {
        Instant grantedAt = Instant.parse("2026-07-01T12:00:00Z");
        Instant expiresAt = Instant.parse("2027-07-01T12:00:00Z");
        Instant withdrawnAt = Instant.parse("2026-08-01T12:00:00Z");

        RecordConsentCommand recordCommand =
                new RecordConsentRequest(
                                CUSTOMER_ID,
                                ConsentType.MARKETING_EMAIL,
                                ConsentStatus.GIVEN,
                                "Marketing email consent",
                                "WEB_FORM",
                                grantedAt,
                                expiresAt,
                                "s3://evidence/consent.pdf",
                                CREATED_BY_ID)
                        .toCommand();
        WithdrawConsentCommand withdrawCommand =
                new WithdrawConsentRequest(CONSENT_ID, withdrawnAt).toCommand();

        assertThat(recordCommand.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(recordCommand.consentType()).isEqualTo(ConsentType.MARKETING_EMAIL);
        assertThat(recordCommand.status()).isEqualTo(ConsentStatus.GIVEN);
        assertThat(recordCommand.purpose()).isEqualTo("Marketing email consent");
        assertThat(recordCommand.source()).isEqualTo("WEB_FORM");
        assertThat(recordCommand.grantedAt()).isEqualTo(grantedAt);
        assertThat(recordCommand.expiresAt()).isEqualTo(expiresAt);
        assertThat(recordCommand.evidenceFileUrl()).isEqualTo("s3://evidence/consent.pdf");
        assertThat(recordCommand.createdBy()).isEqualTo(CREATED_BY_ID);
        assertThat(withdrawCommand.consentRecordId()).isEqualTo(CONSENT_ID);
        assertThat(withdrawCommand.withdrawnAt()).isEqualTo(withdrawnAt);
    }

    @Test
    void rejectsRecordConsentRequestWithoutRequiredKbFields() {
        RecordConsentRequest request =
                new RecordConsentRequest(
                        null, null, null, " ", "PHONE", null, null, null, null);

        assertThat(invalidFields(request))
                .contains("customerId", "consentType", "status", "purpose");
    }

    @Test
    void rejectsWithdrawConsentRequestWithoutConsentRecordId() {
        WithdrawConsentRequest request = new WithdrawConsentRequest(null, null);

        assertThat(invalidFields(request)).contains("consentRecordId");
    }

    @Test
    void mapsConsentSearchRequestToCriteria() {
        ConsentSearchCriteria criteria =
                new ConsentSearchRequest(
                                CUSTOMER_ID,
                                ConsentType.GUARDIAN,
                                ConsentStatus.REQUIRED,
                                true)
                        .toCriteria();

        assertThat(criteria.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(criteria.consentType()).isEqualTo(ConsentType.GUARDIAN);
        assertThat(criteria.status()).isEqualTo(ConsentStatus.REQUIRED);
        assertThat(criteria.validOnly()).isTrue();
    }

    @Test
    void mapsConsentRecordEntityToKbView() {
        Instant now = Instant.parse("2026-07-06T12:00:00Z");
        Instant grantedAt = Instant.parse("2026-07-01T12:00:00Z");
        Instant expiresAt = Instant.parse("2027-07-01T12:00:00Z");
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Anna", "Keller");
        User createdBy =
                User.create(
                        "agent@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Customer Service Agent");
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_SMS,
                        ConsentStatus.REQUIRED,
                        "SMS marketing consent",
                        "PHONE");
        consentRecord.grant(grantedAt, expiresAt, "s3://evidence/sms.pdf", createdBy);

        ConsentRecordView view = ConsentRecordView.from(consentRecord, now);

        assertThat(view.customerFullName()).isEqualTo("Anna Keller");
        assertThat(view.consentType()).isEqualTo(ConsentType.MARKETING_SMS);
        assertThat(view.status()).isEqualTo(ConsentStatus.GIVEN);
        assertThat(view.purpose()).isEqualTo("SMS marketing consent");
        assertThat(view.source()).isEqualTo("PHONE");
        assertThat(view.grantedAt()).isEqualTo(grantedAt);
        assertThat(view.expiresAt()).isEqualTo(expiresAt);
        assertThat(view.evidenceFileUrl()).isEqualTo("s3://evidence/sms.pdf");
        assertThat(view.createdByFullName()).isEqualTo("Customer Service Agent");
        assertThat(view.valid()).isTrue();
        assertThat(view.requiresAction()).isFalse();
    }

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }
}
