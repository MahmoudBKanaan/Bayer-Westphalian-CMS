package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for KB consent field helpers used by segment criteria matching.
 */
class SegmentConsentStatusFilteringTests {

    private static final Instant NOW = Instant.parse("2026-07-09T12:00:00Z");

    @Test
    void recognizesKbConsentStatusFieldNames() {
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("consent_status")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("CONSENT_STATUS")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("consent")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("consent_type")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("has_valid_marketing_consent"))
                .isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("marketing_consent")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("opt_out")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("marketing_opt_out")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("guardian_consent")).isTrue();
        assertThat(SegmentConsentStatusSupport.isConsentStatusField("city")).isFalse();
    }

    @Test
    void canonicalizesConsentStatusAliasesToKbFieldNames() {
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("consent"))
                .isEqualTo("consent_status");
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("consenttype"))
                .isEqualTo("consent_type");
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("marketing_consent"))
                .isEqualTo("has_valid_marketing_consent");
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("opted_out"))
                .isEqualTo("opt_out");
        assertThat(SegmentConsentStatusSupport.canonicalizeFieldName("guardian_consent"))
                .isEqualTo("has_valid_guardian_consent");
    }

    @Test
    void normalizesConsentStatusFilterValues() {
        assertThat(
                        SegmentConsentStatusSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "consent_status", "given"))
                .isEqualTo("GIVEN");
        assertThat(
                        SegmentConsentStatusSupport.normalizeFilterValue(
                                SegmentOperator.IN,
                                "consent_type",
                                "marketing_email, guardian"))
                .isEqualTo("MARKETING_EMAIL,GUARDIAN");
        assertThat(
                        SegmentConsentStatusSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "marketing_consent", "YES"))
                .isEqualTo("true");
    }

    @Test
    void matchesCustomersWithConsentStatusUsingKbOperators() {
        ConsentRecord given = givenMarketingConsent();
        ConsentRecord withdrawn = withdrawnMarketingConsent();

        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(given),
                                SegmentOperator.EQUALS,
                                "consent_status",
                                "GIVEN",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(withdrawn),
                                SegmentOperator.EQUALS,
                                "consent_status",
                                "WITHDRAWN",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(given),
                                SegmentOperator.NOT_EQUALS,
                                "consent",
                                "WITHDRAWN",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(given, withdrawn),
                                SegmentOperator.IN,
                                "consent_status",
                                "EXPIRED,REJECTED",
                                NOW))
                .isFalse();
    }

    @Test
    void matchesValidMarketingConsentAndOptOutFlags() {
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(givenMarketingConsent()),
                                SegmentOperator.EQUALS,
                                "has_valid_marketing_consent",
                                "true",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(withdrawnMarketingConsent()),
                                SegmentOperator.EQUALS,
                                "has_valid_marketing_consent",
                                "true",
                                NOW))
                .isFalse();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(withdrawnMarketingConsent()),
                                SegmentOperator.EQUALS,
                                "opt_out",
                                "true",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(givenMarketingConsent()),
                                SegmentOperator.EQUALS,
                                "opt_out",
                                "false",
                                NOW))
                .isTrue();
    }

    @Test
    void matchesConsentTypeAndGuardianConsent() {
        ConsentRecord guardian = givenGuardianConsent();
        ConsentRecord marketing = givenMarketingConsent();

        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(guardian),
                                SegmentOperator.EQUALS,
                                "consent_type",
                                "GUARDIAN",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(marketing),
                                SegmentOperator.EQUALS,
                                "consent_type",
                                "GUARDIAN",
                                NOW))
                .isFalse();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(guardian),
                                SegmentOperator.EQUALS,
                                "guardian_consent",
                                "true",
                                NOW))
                .isTrue();
    }

    @Test
    void treatsMissingConsentsAsNoValidConsentAndNoOptOut() {
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "consent_status",
                                "GIVEN",
                                NOW))
                .isFalse();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "has_valid_marketing_consent",
                                "false",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "opt_out",
                                "false",
                                NOW))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedConsentStatusFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentConsentStatusSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "consent_status", "PENDING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of GIVEN, WITHDRAWN");

        assertThatThrownBy(
                        () ->
                                SegmentConsentStatusSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "consent_type", "POSTAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of MARKETING_EMAIL");

        assertThatThrownBy(
                        () ->
                                SegmentConsentStatusSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "opt_out", "maybe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("true, false");
    }

    @Test
    void matchesRejectedAndExpiredStatusesAndOptOutFromRejected() {
        ConsentRecord rejected =
                ConsentRecord.create(
                        customer("Rejected", "User"),
                        ConsentType.MARKETING_SMS,
                        ConsentStatus.GIVEN,
                        "SMS marketing",
                        "form");
        rejected.reject();
        ConsentRecord expired =
                ConsentRecord.create(
                        customer("Expired", "User"),
                        ConsentType.MARKETING_PHONE,
                        ConsentStatus.GIVEN,
                        "Phone marketing",
                        "form");
        expired.expire();

        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(rejected),
                                SegmentOperator.EQUALS,
                                "consent_status",
                                "REJECTED",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(expired),
                                SegmentOperator.EQUALS,
                                "consent_status",
                                "EXPIRED",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(rejected),
                                SegmentOperator.EQUALS,
                                "opt_out",
                                "true",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(expired),
                                SegmentOperator.EQUALS,
                                "has_valid_marketing_consent",
                                "false",
                                NOW))
                .isTrue();
    }

    @Test
    void matchesRequiredStatusAndConsentTypeInList() {
        ConsentRecord required =
                ConsentRecord.create(
                        customer("Required", "User"),
                        ConsentType.DATA_PROCESSING,
                        ConsentStatus.REQUIRED,
                        "Data processing required",
                        "import");

        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(required),
                                SegmentOperator.EQUALS,
                                "consent_status",
                                "REQUIRED",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(required),
                                SegmentOperator.IN,
                                "consent_type",
                                "GUARDIAN,DATA_PROCESSING",
                                NOW))
                .isTrue();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(required),
                                SegmentOperator.IN,
                                "consent_type",
                                "MARKETING_EMAIL,MARKETING_SMS",
                                NOW))
                .isFalse();
    }

    @Test
    void withdrawnAtInvalidatesMarketingConsentEvenIfStatusWereGiven() {
        ConsentRecord withdrawn = withdrawnMarketingConsent();
        assertThat(withdrawn.getStatus()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(withdrawn.getWithdrawnAt()).isNotNull();

        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(withdrawn),
                                SegmentOperator.EQUALS,
                                "has_valid_marketing_consent",
                                "true",
                                NOW))
                .isFalse();
        assertThat(
                        SegmentConsentStatusSupport.matchesCustomerConsents(
                                List.of(withdrawn),
                                SegmentOperator.EQUALS,
                                "marketing_opt_out",
                                "true",
                                NOW))
                .isTrue();
    }

    private static ConsentRecord givenMarketingConsent() {
        return ConsentRecord.create(
                customer("Ada", "Consent"),
                ConsentType.MARKETING_EMAIL,
                ConsentStatus.GIVEN,
                "Marketing email consent",
                "phone");
    }

    private static ConsentRecord withdrawnMarketingConsent() {
        ConsentRecord consent =
                ConsentRecord.create(
                        customer("Opt", "Out"),
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Marketing email consent",
                        "phone");
        consent.withdraw(NOW);
        return consent;
    }

    private static ConsentRecord givenGuardianConsent() {
        return ConsentRecord.create(
                customer("Guardian", "Ready"),
                ConsentType.GUARDIAN,
                ConsentStatus.GIVEN,
                "Guardian consent",
                "letter");
    }

    private static Customer customer(String firstName, String lastName) {
        return Customer.create(CustomerType.PROSPECT, firstName, lastName);
    }
}
