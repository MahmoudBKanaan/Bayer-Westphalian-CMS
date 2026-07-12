package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Item 535: monthly marketing contact limit is configurable via {@link SystemSettingsService}
 * (Admin System Settings) and applied at eligibility evaluation time (BR-011).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("535 Configurable monthly contact limit")
class ConfigurableMonthlyContactLimitTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000535");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000535");
    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");

    @Mock private ConsentService consentService;
    @Mock private CustomerRepository customerRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SystemSettingsService systemSettingsService;

    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService =
                new EligibilityService(
                        consentService,
                        customerRepository,
                        jdbcTemplate,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        systemSettingsService);
    }

    @Test
    void usesConfiguredLimitFromSystemSettingsWhenEvaluatingEligibility() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(5);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        // 4 contacts is under a configured limit of 5
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(4);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isTrue();
        verify(systemSettingsService).monthlyContactLimit();
    }

    @Test
    void excludesWhenContactsReachAdminConfiguredLimit() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(2);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(2);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        verify(systemSettingsService).monthlyContactLimit();
    }

    @Test
    void checkMonthlyLimitWithoutOverrideUsesSystemSettings() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(4);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(4);

        assertThat(eligibilityService.checkMonthlyLimit(CUSTOMER_ID)).isTrue();
        verify(systemSettingsService).monthlyContactLimit();
    }

    @Test
    void raisedConfiguredLimitAllowsPreviouslyBlockedContactCount() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(10);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        // Default legacy limit of 3 would exclude; configured 10 includes
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(3);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isTrue();
        verify(systemSettingsService).monthlyContactLimit();
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Item", "FiveThirtyFive");
        try {
            Field id = BaseEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(customer, CUSTOMER_ID);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return customer;
    }
}
