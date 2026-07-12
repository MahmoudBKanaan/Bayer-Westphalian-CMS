package com.bayerwestphalian.campaign.campaign;

import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibilityService {

    private final ConsentService consentService;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final int monthlyContactLimit;

    @Autowired
    public EligibilityService(
            ConsentService consentService,
            CustomerRepository customerRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${app.contact.monthly-limit:3}") int monthlyContactLimit) {
        this(
                consentService,
                customerRepository,
                jdbcTemplate,
                Clock.systemUTC(),
                monthlyContactLimit);
    }

    EligibilityService(
            ConsentService consentService,
            CustomerRepository customerRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock,
            int monthlyContactLimit) {
        this.consentService = consentService;
        this.customerRepository = customerRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.monthlyContactLimit = monthlyContactLimit;
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public EligibilityDecision evaluateCustomer(
            UUID customerId,
            UUID campaignId,
            ConsentType consentType,
            boolean guardianConsentRequired) {
        validateCustomerId(customerId);
        validateConsentType(consentType);
        Customer customer = findCustomer(customerId);

        if (customer.isDoNotContact()) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT);
        }
        if (customer.getStatus() == CustomerStatus.UNINTERESTED) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.UNINTERESTED);
        }
        if (customer.getStatus() == CustomerStatus.CONVERTED) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.CONVERTED);
        }
        if (excludeOptOuts(customerId)) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT);
        }
        if (excludeInvalidConsent(customerId, consentType, guardianConsentRequired)) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT);
        }
        if (isDuplicateCampaignRecipient(campaignId, customerId)) {
            return EligibilityDecision.excluded(
                    EligibilityExclusionReason.DUPLICATE_CAMPAIGN_RECIPIENT);
        }
        if (hasReachedMonthlyContactLimit(customerId, monthlyContactLimit)) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT);
        }
        return EligibilityDecision.included();
    }

    /**
     * Evaluates KB audience eligibility for segment preview (FR-054, FR-055, BR-001–BR-003,
     * BR-011). Invoked by {@code SegmentService.previewSegment} for every criteria match so
     * previews never treat unfiltered criteria results as contactable audiences.
     *
     * <p>Uses default marketing-email consent, skips campaign-duplicate checks, and is authorized
     * for segment preview roles including BI Analyst.
     */
    @PreAuthorize("@authz.canPreviewSegments()")
    @Transactional(readOnly = true)
    public EligibilityDecision evaluateForSegmentPreview(UUID customerId) {
        return evaluateForSegmentPreview(customerId, ConsentType.MARKETING_EMAIL);
    }

    /**
     * Segment-preview eligibility with an explicit required consent type (for example SMS or phone
     * marketing audiences). Same rule set as campaign eligibility except campaign-duplicate
     * recipient checks are omitted.
     */
    @PreAuthorize("@authz.canPreviewSegments()")
    @Transactional(readOnly = true)
    public EligibilityDecision evaluateForSegmentPreview(
            UUID customerId, ConsentType consentType) {
        validateCustomerId(customerId);
        validateConsentType(consentType);
        Customer customer = findCustomer(customerId);
        boolean guardianConsentRequired = requiresGuardianConsent(customerId);
        return evaluateSegmentAudienceRules(
                customer, customerId, consentType, guardianConsentRequired);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public boolean isCommunicationEligible(UUID customerId, UUID campaignId) {
        validateCustomerId(customerId);
        validateCampaignId(campaignId);

        ConsentType consentType = resolveCampaignConsentType(campaignId);
        boolean guardianConsentRequired = requiresGuardianConsent(customerId);

        return evaluateCustomer(customerId, campaignId, consentType, guardianConsentRequired)
                .eligible();
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public EligibilityDecision evaluateForCampaignPreview(UUID customerId, UUID campaignId) {
        validateCustomerId(customerId);
        validateCampaignId(campaignId);

        ConsentType consentType = resolveCampaignConsentType(campaignId);
        boolean guardianConsentRequired = requiresGuardianConsent(customerId);

        return evaluateCustomer(customerId, campaignId, consentType, guardianConsentRequired);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public boolean excludeInvalidConsent(
            UUID customerId, ConsentType consentType, boolean guardianConsentRequired) {
        validateCustomerId(customerId);
        validateConsentType(consentType);
        return !consentService.isCommunicationEligible(
                customerId, consentType, guardianConsentRequired);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public boolean excludeOptOuts(UUID customerId) {
        validateCustomerId(customerId);
        return consentService.hasMarketingOptOut(customerId);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public boolean excludeDuplicateContacts(UUID campaignId, UUID customerId) {
        validateCustomerId(customerId);
        return isDuplicateCampaignRecipient(campaignId, customerId);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public boolean checkMonthlyLimit(UUID customerId) {
        return checkMonthlyLimit(customerId, monthlyContactLimit);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER')")
    @Transactional(readOnly = true)
    public boolean checkMonthlyLimit(UUID customerId, int limit) {
        validateCustomerId(customerId);
        if (limit < 1) {
            throw new ValidationException(
                    "Eligibility validation failed", List.of("limit: must be at least 1"));
        }
        return hasReachedMonthlyContactLimit(customerId, limit);
    }

    /**
     * Segment preview rules: DNC, marketing opt-out, invalid/missing consent (including guardian),
     * and monthly contact limit. Campaign-duplicate checks are omitted because segment preview is
     * not campaign-scoped.
     */
    private EligibilityDecision evaluateSegmentAudienceRules(
            Customer customer,
            UUID customerId,
            ConsentType consentType,
            boolean guardianConsentRequired) {
        if (customer.isDoNotContact()) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT);
        }
        if (customer.getStatus() == CustomerStatus.UNINTERESTED) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.UNINTERESTED);
        }
        if (customer.getStatus() == CustomerStatus.CONVERTED) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.CONVERTED);
        }
        if (isMarketingConsentType(consentType) && consentService.hasMarketingOptOut(customer)) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT);
        }
        if (!consentService.isCommunicationEligible(
                customer, consentType, guardianConsentRequired)) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT);
        }
        if (hasReachedMonthlyContactLimit(customerId, monthlyContactLimit)) {
            return EligibilityDecision.excluded(EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT);
        }
        return EligibilityDecision.included();
    }

    private boolean isDuplicateCampaignRecipient(UUID campaignId, UUID customerId) {
        if (campaignId == null) {
            return false;
        }
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        select count(*)
                        from campaign_recipients
                        where campaign_id = ?
                          and customer_id = ?
                        """,
                        Integer.class,
                        campaignId,
                        customerId);
        return count != null && count > 0;
    }

    private boolean hasReachedMonthlyContactLimit(UUID customerId, int limit) {
        Instant windowStart = Instant.now(clock).minus(30, ChronoUnit.DAYS);
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        select count(*)
                        from contact_events
                        where customer_id = ?
                          and event_type in ('SENT', 'CALLED')
                          and occurred_at >= ?
                        """,
                        Integer.class,
                        customerId,
                        Timestamp.from(windowStart));
        return count != null && count >= limit;
    }

    private static boolean isMarketingConsentType(ConsentType consentType) {
        return consentType == ConsentType.MARKETING_EMAIL
                || consentType == ConsentType.MARKETING_SMS
                || consentType == ConsentType.MARKETING_PHONE;
    }

    private ConsentType resolveCampaignConsentType(UUID campaignId) {
        try {
            String channel =
                    jdbcTemplate.queryForObject(
                            """
                            select channel
                            from campaigns
                            where id = ?
                            """,
                            String.class,
                            campaignId);
            return toConsentType(channel);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("Campaign", campaignId);
        }
    }

    private ConsentType toConsentType(String channel) {
        return switch (channel) {
            case "EMAIL" -> ConsentType.MARKETING_EMAIL;
            case "SMS" -> ConsentType.MARKETING_SMS;
            case "PHONE" -> ConsentType.MARKETING_PHONE;
            case "IN_APP" -> ConsentType.DATA_PROCESSING;
            default ->
                    throw new ValidationException(
                            "Eligibility validation failed",
                            List.of("campaign.channel: unsupported communication channel"));
        };
    }

    private boolean requiresGuardianConsent(UUID customerId) {
        Boolean required =
                jdbcTemplate.queryForObject(
                        """
                        select exists (
                            select 1
                            from beneficiaries
                            where beneficiary_customer_id = ?
                              and guardian_consent_required = true
                        )
                        """,
                        Boolean.class,
                        customerId);
        return Boolean.TRUE.equals(required);
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException(
                    "Eligibility validation failed", List.of("customerId: must not be null"));
        }
    }

    private void validateCampaignId(UUID campaignId) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Eligibility validation failed", List.of("campaignId: must not be null"));
        }
    }

    private void validateConsentType(ConsentType consentType) {
        if (consentType == null) {
            throw new ValidationException(
                    "Eligibility validation failed", List.of("consentType: must not be null"));
        }
    }
}
