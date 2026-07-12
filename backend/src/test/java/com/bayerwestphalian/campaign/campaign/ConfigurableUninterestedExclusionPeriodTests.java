package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Item 537: uninterested exclusion lasts only for the Admin-configured period (days), read from
 * {@link SystemSettingsService#uninterestedExclusionDays()}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("537 Configurable uninterested exclusion period")
class ConfigurableUninterestedExclusionPeriodTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000537");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000537");
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
    void excludesUninterestedCustomerWithinConfiguredPeriod() {
        when(systemSettingsService.uninterestedExclusionDays()).thenReturn(90);
        Customer customer = uninterestedCustomer(NOW.minus(30, ChronoUnit.DAYS));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("UNINTERESTED");
        verify(systemSettingsService).uninterestedExclusionDays();
    }

    @Test
    void includesUninterestedCustomerAfterConfiguredPeriodElapses() {
        when(systemSettingsService.uninterestedExclusionDays()).thenReturn(90);
        Customer customer = uninterestedCustomer(NOW.minus(91, ChronoUnit.DAYS));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        stubEligibleBeyondUninterested();

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isTrue();
        verify(systemSettingsService).uninterestedExclusionDays();
    }

    @Test
    void shorterConfiguredPeriodExpiresExclusionSooner() {
        when(systemSettingsService.uninterestedExclusionDays()).thenReturn(14);
        Customer customer = uninterestedCustomer(NOW.minus(20, ChronoUnit.DAYS));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        stubEligibleBeyondUninterested();

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isTrue();
        verify(systemSettingsService).uninterestedExclusionDays();
    }

    @Test
    void segmentPreviewRespectsUninterestedExclusionPeriod() {
        when(systemSettingsService.uninterestedExclusionDays()).thenReturn(90);
        Customer customer = uninterestedCustomer(NOW.minus(100, ChronoUnit.DAYS));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(systemSettingsService.monthlyContactLimit()).thenReturn(3);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(0);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isTrue();
        verify(systemSettingsService).uninterestedExclusionDays();
    }

    @Test
    void excludesWhenStatusChangedAtMissingAndStillUninterested() {
        lenient().when(systemSettingsService.uninterestedExclusionDays()).thenReturn(90);
        Customer customer = Customer.create(CustomerType.CUSTOMER, "No", "Anchor");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        ReflectionTestUtils.setField(customer, "status", CustomerStatus.UNINTERESTED);
        // no statusChangedAt / updatedAt
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("UNINTERESTED");
    }

    private void stubEligibleBeyondUninterested() {
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
                .thenReturn(0);
        lenient().when(systemSettingsService.monthlyContactLimit()).thenReturn(3);
    }

    private static Customer uninterestedCustomer(Instant statusChangedAt) {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Un", "Interested");
        try {
            Field id = BaseEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(customer, CUSTOMER_ID);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        ReflectionTestUtils.setField(customer, "status", CustomerStatus.UNINTERESTED);
        ReflectionTestUtils.setField(customer, "statusChangedAt", statusChangedAt);
        return customer;
    }
}
