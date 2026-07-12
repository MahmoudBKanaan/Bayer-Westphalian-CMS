package com.bayerwestphalian.campaign.consent;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Consent, opt-out, guardian consent, and eligibility service (KB E09 / FR-033–FR-034 / COMP-001).
 *
 * <p>Item 524: every successful consent record or withdrawal writes an immutable audit row on
 * entity type {@code consent_records} via {@link AuditService#logConsentCreation}, {@link
 * AuditService#logConsentChange}, or {@link AuditService#logConsentWithdrawal}.
 */
@Service
public class ConsentService {

    /** KB audit entity type for consent history rows. */
    public static final String AUDIT_ENTITY_TYPE = "consent_records";

    private static final List<ConsentType> MARKETING_CONSENT_TYPES =
            List.of(
                    ConsentType.MARKETING_EMAIL,
                    ConsentType.MARKETING_PHONE,
                    ConsentType.MARKETING_SMS);

    private final ConsentRepository consentRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AuthorizationExpressions authorizationExpressions;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public ConsentService(
            ConsentRepository consentRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService) {
        this(
                consentRepository,
                customerRepository,
                userRepository,
                authorizationExpressions,
                auditService,
                Clock.systemUTC());
    }

    ConsentService(
            ConsentRepository consentRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            AuthorizationExpressions authorizationExpressions,
            AuditService auditService,
            Clock clock) {
        this.consentRepository = consentRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.authorizationExpressions = authorizationExpressions;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Records a new consent / opt-out / guardian / data-processing row (FR-033).
     *
     * <p>Item 524: persists a {@code CREATE} audit entry with actor and full consent payload.
     *
     * <p>Item 525 / COMP-002: when the recorded row is a marketing opt-out ({@code REJECTED} or
     * {@code WITHDRAWN} on {@code MARKETING_EMAIL}/{@code MARKETING_PHONE}/{@code MARKETING_SMS}),
     * also writes an {@code OPT_OUT} audit row via {@link AuditService#logOptOutChange}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')")
    @Transactional
    public ConsentRecordView recordConsent(RecordConsentCommand command) {
        validateRecordCommand(command);
        Customer customer = findCustomer(command.customerId());
        UUID actorUserId = resolveConsentActor(command.createdBy());
        User createdBy = findOptionalUser(command.createdBy());
        if (createdBy == null && actorUserId != null) {
            createdBy = userRepository.findById(actorUserId).orElse(null);
        }
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        customer,
                        command.consentType(),
                        command.status(),
                        command.purpose().trim(),
                        normalize(command.source()));

        applyInitialStatus(consentRecord, command, createdBy);
        ConsentRecord savedConsent = consentRepository.save(consentRecord);

        // Item 524: log consent recording with actor and full payload (never secrets).
        Map<String, Object> newValue = consentAuditPayload(savedConsent);
        auditService.logConsentCreation(actorUserId, savedConsent.getId(), newValue);

        // Item 525: dedicated OPT_OUT trail for marketing opt-out recordings.
        if (isMarketingOptOutRecord(savedConsent)) {
            auditService.logOptOutChange(
                    actorUserId,
                    savedConsent.getId(),
                    null,
                    marketingOptOutAuditPayload(savedConsent));
        }

        return ConsentRecordView.from(savedConsent, now());
    }

    /**
     * Withdraws an existing consent record.
     *
     * <p>Item 524: writes {@code WITHDRAW_CONSENT} with before/after payloads and the acting
     * principal as actor.
     *
     * <p>Item 525 / COMP-002: when the withdrawn consent is a marketing channel type, also writes
     * {@code OPT_OUT} so opt-outs are filterable separately from general consent history.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')")
    @Transactional
    public ConsentRecordView withdrawConsent(WithdrawConsentCommand command) {
        validateWithdrawCommand(command);
        ConsentRecord consentRecord = findConsentRecord(command.consentRecordId());
        Map<String, Object> oldValue = consentAuditPayload(consentRecord);
        boolean marketingChannel = isMarketingConsentType(consentRecord.getConsentType());

        consentRecord.withdraw(command.withdrawnAt() == null ? now() : command.withdrawnAt());
        ConsentRecord savedConsent = consentRepository.save(consentRecord);
        UUID actorUserId = currentActorUserId();
        Map<String, Object> newValue = consentAuditPayload(savedConsent);

        auditService.logConsentWithdrawal(
                actorUserId, savedConsent.getId(), oldValue, newValue);

        if (marketingChannel) {
            auditService.logOptOutChange(
                    actorUserId,
                    savedConsent.getId(),
                    marketingOptOutAuditPayload(oldValue, false),
                    marketingOptOutAuditPayload(savedConsent));
        }

        return ConsentRecordView.from(savedConsent, now());
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public List<ConsentRecordView> listConsents(ConsentSearchCriteria criteria) {
        ConsentSearchCriteria normalized = normalize(criteria);

        return loadCandidates(normalized).stream()
                .filter(consentRecord -> matches(consentRecord, normalized))
                .map(consentRecord -> ConsentRecordView.from(consentRecord, now()))
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public Optional<ConsentRecordView> getConsentStatus(UUID customerId, ConsentType consentType) {
        validateCustomerId(customerId);
        validateConsentType(consentType);

        return consentRepository
                .findLatestByType(customerId, consentType)
                .map(consentRecord -> ConsentRecordView.from(consentRecord, now()));
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean hasValidMarketingConsent(UUID customerId) {
        validateCustomerId(customerId);
        if (hasMarketingOptOut(customerId)) {
            return false;
        }
        return MARKETING_CONSENT_TYPES.stream()
                .anyMatch(consentType -> hasValidConsent(customerId, consentType));
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean hasValidMarketingConsent(UUID customerId, ConsentType consentType) {
        validateCustomerId(customerId);
        validateMarketingConsentType(consentType);
        if (hasMarketingOptOut(customerId)) {
            return false;
        }
        return hasValidConsent(customerId, consentType);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean hasValidGuardianConsent(UUID customerId) {
        validateCustomerId(customerId);
        return hasValidConsent(customerId, ConsentType.GUARDIAN);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean hasMarketingOptOut(UUID customerId) {
        validateCustomerId(customerId);
        return consentRepository.findOptOuts(customerId).stream()
                .map(ConsentRecord::getConsentType)
                .anyMatch(this::isMarketingConsentType);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean isGuardianConsentSatisfied(UUID customerId, boolean guardianConsentRequired) {
        validateCustomerId(customerId);
        return !guardianConsentRequired || hasValidGuardianConsent(customerId);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public void validateGuardianConsent(UUID customerId, boolean guardianConsentRequired) {
        if (!isGuardianConsentSatisfied(customerId, guardianConsentRequired)) {
            throw new ValidationException(
                    "Consent validation failed",
                    List.of("guardianConsent: valid guardian consent is required"));
        }
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean isCommunicationEligible(UUID customerId, ConsentType consentType) {
        validateCustomerId(customerId);
        return isCommunicationEligible(findCustomer(customerId), consentType);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public boolean isCommunicationEligible(
            UUID customerId, ConsentType consentType, boolean guardianConsentRequired) {
        validateCustomerId(customerId);
        return isCommunicationEligible(findCustomer(customerId), consentType)
                && isGuardianConsentSatisfied(customerId, guardianConsentRequired);
    }

    public boolean isCommunicationEligible(Customer customer, ConsentType consentType) {
        if (customer == null || customer.isDeleted() || customer.isDoNotContact()) {
            return false;
        }
        validateConsentType(consentType);
        if (isMarketingConsentType(consentType) && hasMarketingOptOut(customer)) {
            return false;
        }
        return hasValidConsent(customer.getId(), consentType);
    }

    /**
     * Unsecured customer-entity overload for internal campaign/segment eligibility evaluation.
     * Callers (for example {@code EligibilityService} / {@code SegmentService}) must already be
     * authorized.
     */
    public boolean isCommunicationEligible(
            Customer customer, ConsentType consentType, boolean guardianConsentRequired) {
        if (!isCommunicationEligible(customer, consentType)) {
            return false;
        }
        return !guardianConsentRequired || hasValidConsent(customer.getId(), ConsentType.GUARDIAN);
    }

    /**
     * Unsecured customer-entity opt-out check for internal eligibility evaluation. Callers must
     * already be authorized.
     */
    public boolean hasMarketingOptOut(Customer customer) {
        if (customer == null || customer.getId() == null) {
            return false;
        }
        return consentRepository.findOptOuts(customer.getId()).stream()
                .map(ConsentRecord::getConsentType)
                .anyMatch(this::isMarketingConsentType);
    }

    private void applyInitialStatus(
            ConsentRecord consentRecord, RecordConsentCommand command, User createdBy) {
        if (command.status() == ConsentStatus.GIVEN) {
            consentRecord.grant(
                    command.grantedAt() == null ? now() : command.grantedAt(),
                    command.expiresAt(),
                    normalize(command.evidenceFileUrl()),
                    createdBy);
        } else if (command.status() == ConsentStatus.WITHDRAWN) {
            consentRecord.withdraw(now());
        } else if (command.status() == ConsentStatus.EXPIRED) {
            consentRecord.expire();
        } else if (command.status() == ConsentStatus.REJECTED) {
            consentRecord.reject();
        }
    }

    private boolean hasValidConsent(UUID customerId, ConsentType consentType) {
        return consentRepository.findValidConsent(customerId, consentType, now()).isPresent();
    }

    private boolean isMarketingConsentType(ConsentType consentType) {
        return MARKETING_CONSENT_TYPES.contains(consentType);
    }

    /**
     * Marketing opt-out: marketing channel consent with status that excludes the customer from
     * marketing (KB: WITHDRAWN or REJECTED).
     */
    private boolean isMarketingOptOutRecord(ConsentRecord consentRecord) {
        if (consentRecord == null || !isMarketingConsentType(consentRecord.getConsentType())) {
            return false;
        }
        ConsentStatus status = consentRecord.getStatus();
        return status == ConsentStatus.REJECTED || status == ConsentStatus.WITHDRAWN;
    }

    private Map<String, Object> marketingOptOutAuditPayload(ConsentRecord consentRecord) {
        return marketingOptOutAuditPayload(consentAuditPayload(consentRecord), true);
    }

    private Map<String, Object> marketingOptOutAuditPayload(
            Map<String, Object> basePayload, boolean optedOut) {
        Map<String, Object> payload = new LinkedHashMap<>(basePayload);
        payload.put("optOut", optedOut);
        payload.put("marketingConsent", true);
        return payload;
    }

    private List<ConsentRecord> loadCandidates(ConsentSearchCriteria criteria) {
        if (criteria.customerId() != null) {
            return consentRepository.findByCustomerId(criteria.customerId());
        }
        return consentRepository.findAllByOrderByCreatedAtDesc();
    }

    private boolean matches(ConsentRecord consentRecord, ConsentSearchCriteria criteria) {
        return matchesCustomer(consentRecord, criteria.customerId())
                && matchesType(consentRecord, criteria.consentType())
                && matchesStatus(consentRecord, criteria.status())
                && matchesValidOnly(consentRecord, criteria.validOnly());
    }

    private boolean matchesCustomer(ConsentRecord consentRecord, UUID customerId) {
        return customerId == null
                || (consentRecord.getCustomer() != null
                        && Objects.equals(consentRecord.getCustomer().getId(), customerId));
    }

    private boolean matchesType(ConsentRecord consentRecord, ConsentType consentType) {
        return consentType == null || consentRecord.getConsentType() == consentType;
    }

    private boolean matchesStatus(ConsentRecord consentRecord, ConsentStatus status) {
        return status == null || consentRecord.getStatus() == status;
    }

    private boolean matchesValidOnly(ConsentRecord consentRecord, Boolean validOnly) {
        return !Boolean.TRUE.equals(validOnly) || consentRecord.isValid(now());
    }

    private ConsentSearchCriteria normalize(ConsentSearchCriteria criteria) {
        if (criteria == null) {
            return new ConsentSearchCriteria(null, null, null, null);
        }
        return criteria;
    }

    private ConsentRecord findConsentRecord(UUID consentRecordId) {
        return consentRepository
                .findById(consentRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsentRecord", consentRecordId));
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private User findOptionalUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void validateRecordCommand(RecordConsentCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Consent validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("customerId", command.customerId()),
                                required("consentType", command.consentType()),
                                required("status", command.status()),
                                required("purpose", command.purpose()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Consent validation failed", errors);
        }
        if (command.expiresAt() != null
                && command.grantedAt() != null
                && !command.expiresAt().isAfter(command.grantedAt())) {
            throw new ValidationException(
                    "Consent validation failed", List.of("expiresAt: must be after grantedAt"));
        }
    }

    private void validateWithdrawCommand(WithdrawConsentCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Consent validation failed", List.of("command: is required"));
        }
        if (command.consentRecordId() == null) {
            throw new ValidationException(
                    "Consent validation failed", List.of("consentRecordId: must not be null"));
        }
    }

    private void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException(
                    "Consent validation failed", List.of("customerId: must not be null"));
        }
    }

    private void validateConsentType(ConsentType consentType) {
        if (consentType == null) {
            throw new ValidationException(
                    "Consent validation failed", List.of("consentType: must not be null"));
        }
    }

    private void validateMarketingConsentType(ConsentType consentType) {
        validateConsentType(consentType);
        if (!MARKETING_CONSENT_TYPES.contains(consentType)) {
            throw new ValidationException(
                    "Consent validation failed",
                    List.of("consentType: must be a marketing consent type"));
        }
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private Map<String, Object> consentAuditPayload(ConsentRecord consentRecord) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (consentRecord.getId() != null) {
            payload.put("id", consentRecord.getId().toString());
        }
        if (consentRecord.getCustomer() != null && consentRecord.getCustomer().getId() != null) {
            payload.put("customerId", consentRecord.getCustomer().getId());
        }
        payload.put("consentType", consentRecord.getConsentType().name());
        payload.put("status", consentRecord.getStatus().name());
        payload.put("purpose", consentRecord.getPurpose());
        putIfPresent(payload, "source", consentRecord.getSource());
        putIfPresent(payload, "grantedAt", consentRecord.getGrantedAt());
        putIfPresent(payload, "withdrawnAt", consentRecord.getWithdrawnAt());
        putIfPresent(payload, "expiresAt", consentRecord.getExpiresAt());
        putIfPresent(payload, "evidenceFileUrl", consentRecord.getEvidenceFileUrl());
        if (consentRecord.getCreatedBy() != null) {
            payload.put("createdBy", consentRecord.getCreatedBy().getId());
        }
        return payload;
    }

    /** Prefer explicit recorder from the request; otherwise the authenticated principal. */
    private UUID resolveConsentActor(UUID createdByUserId) {
        if (createdByUserId != null) {
            return createdByUserId;
        }
        return currentActorUserId();
    }

    private UUID currentActorUserId() {
        try {
            return authorizationExpressions.currentUserId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }
}
