package com.bayerwestphalian.campaign.schedule;

import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment-due and product-expiration reminder scheduling (KB E18).
 *
 * <p><strong>Production gate (item 409):</strong> reminder logic must respect:
 *
 * <ul>
 *   <li><strong>Consent</strong> — marketing consent / opt-out via {@link
 *       EligibilityService#evaluateForReminder} (item 401 / FR-034)
 *   <li><strong>Payment status</strong> — no payment-due schedule or send when payment is paid
 *       (BR-024)
 *   <li><strong>Expiration dates</strong> — product-expiration generation only for active
 *       ownerships with expiration in the configured 3/6/12-month window (BR-023)
 *   <li><strong>Contact frequency limits</strong> — monthly marketing contact limit (BR-011 /
 *       FR-092)
 * </ul>
 *
 * <p>Ineligible recipients are rejected on manual create, skipped on bulk generate, and cancelled
 * (not sent) on due processing.
 */
@Service
@Transactional
public class ReminderService {

    /** Marketing consent type required for reminder contact eligibility. */
    private static final ConsentType REMINDER_CONSENT_TYPE = ConsentType.MARKETING_EMAIL;

    /** Business-rule code when a reminder recipient fails consent or contact-limit checks. */
    static final String REMINDER_RECIPIENT_INELIGIBLE = "REMINDER_RECIPIENT_INELIGIBLE";

    /**
     * Business-rule code when product-expiration scheduling lacks active ownership with an
     * expiration date.
     */
    static final String EXPIRATION_REMINDER_REQUIRES_ACTIVE_OWNERSHIP =
            "EXPIRATION_REMINDER_REQUIRES_ACTIVE_OWNERSHIP";

    private final ReminderRepository reminderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final EligibilityService eligibilityService;

    public ReminderService(
            ReminderRepository reminderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            PaymentRecordRepository paymentRecordRepository,
            ProductOwnershipRepository productOwnershipRepository,
            EligibilityService eligibilityService) {
        this.reminderRepository = reminderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.productOwnershipRepository = productOwnershipRepository;
        this.eligibilityService = eligibilityService;
    }

    /**
     * Schedules a payment-due reminder for a specific customer and product (KB E18 / BR-024).
     *
     * <p>BR-024: a payment reminder must not be scheduled when the related payment is already
     * completed (paid).
     *
     * <p>KB item 401: recipient must pass consent and monthly contact-limit eligibility or
     * scheduling fails with {@code REMINDER_RECIPIENT_INELIGIBLE}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ReminderScheduleView createPaymentReminders(ReminderScheduleCommand command) {
        validateCommand(command, ReminderType.PAYMENT_DUE);
        Customer customer = findCustomer(command.customerId());
        Product product = findProduct(command.productId());

        if (isPaymentCompletedForReminder(command.customerId(), command.productId())) {
            throw new BusinessRuleException(
                    "PAYMENT_REMINDER_PAYMENT_COMPLETED",
                    "Payment reminder must not be scheduled after the payment is completed");
        }
        ensureReminderEligibility(command.customerId());

        return saveReminder(customer, product, command);
    }

    /**
     * Generates payment-due reminders for unpaid payments that are due on or before today (KB E18 /
     * BR-020–BR-022, BR-024, item 401).
     *
     * <p>Candidates are unpaid {@code DUE}, {@code OVERDUE}, and {@code DEFAULT_RISK} payment
     * records for contactable, eligibility-approved customers (valid consent and under the monthly
     * contact limit). Completed (paid) payments are never included. Reminder level escalates Green
     * → Yellow → Red from {@code reminder_count}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generatePaymentDueReminders() {
        return generatePaymentDueReminders(LocalDate.now());
    }

    /**
     * Generates payment-due reminders evaluated as of the given date (or today when {@code null}).
     *
     * <p>Authorized roles: Admin, Campaign Manager, and Customer Service Agent. Duplicate
     * generation for the same customer, product, level, and scheduled date is skipped.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generatePaymentDueReminders(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;

        return Stream.concat(
                        paymentRecordRepository.findDuePayments().stream(),
                        paymentRecordRepository.findOverduePayments().stream())
                .filter(payment -> isPaymentReminderCandidate(payment, effectiveDate))
                .filter(payment -> !hasExistingGeneratedPaymentReminder(payment, effectiveDate))
                .map(payment -> createGeneratedPaymentReminder(payment, effectiveDate))
                .peek(this::markLikelyDefaultRiskAfterRedReminder)
                .map(reminderRepository::save)
                .map(ReminderScheduleView::from)
                .toList();
    }

    /**
     * Schedules a product-expiration reminder (KB E18 / BR-023 / item 409).
     *
     * <p>Requires active ownership with a non-null expiration date for the customer and product.
     * Recipient must also pass consent and monthly contact-limit eligibility or scheduling fails
     * with {@code REMINDER_RECIPIENT_INELIGIBLE}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ReminderScheduleView createExpirationReminders(ReminderScheduleCommand command) {
        validateCommand(command, ReminderType.PRODUCT_EXPIRATION);
        Customer customer = findCustomer(command.customerId());
        Product product = findProduct(command.productId());

        if (!hasActiveOwnershipWithExpirationDate(command.customerId(), command.productId())) {
            throw new BusinessRuleException(
                    EXPIRATION_REMINDER_REQUIRES_ACTIVE_OWNERSHIP,
                    "Product-expiration reminder requires active ownership with an expiration date"
                            + " for the customer and product");
        }
        ensureReminderEligibility(command.customerId());

        return saveReminder(customer, product, command);
    }

    /**
     * Generates product-expiration reminders for ownerships expiring within 3 months (KB BR-023 /
     * item 398).
     *
     * <p>Uses the 3-month window end {@code asOfDate.plusMonths(3)} and schedules {@code
     * PRODUCT_EXPIRATION} reminders at {@link ReminderLevel#RED}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generateThreeMonthExpirationReminders() {
        return generateThreeMonthExpirationReminders(LocalDate.now());
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generateThreeMonthExpirationReminders(LocalDate asOfDate) {
        return generateProductExpirationReminders(
                asOfDate,
                ProductExpirationReminderRules.THREE_MONTH_WINDOW,
                ProductExpirationReminderRules.threeMonthReminderLevel());
    }

    /**
     * Generates product-expiration reminders for ownerships expiring within 6 months (KB BR-023 /
     * item 399).
     *
     * <p>Uses the 6-month window end {@code asOfDate.plusMonths(6)} and schedules {@code
     * PRODUCT_EXPIRATION} reminders at {@link ReminderLevel#YELLOW}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generateSixMonthExpirationReminders() {
        return generateSixMonthExpirationReminders(LocalDate.now());
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generateSixMonthExpirationReminders(LocalDate asOfDate) {
        return generateProductExpirationReminders(
                asOfDate,
                ProductExpirationReminderRules.SIX_MONTH_WINDOW,
                ProductExpirationReminderRules.sixMonthReminderLevel());
    }

    /**
     * Generates product-expiration reminders for ownerships expiring within 12 months (KB BR-023 /
     * item 400).
     *
     * <p>Uses the 12-month window end {@code asOfDate.plusMonths(12)} and schedules {@code
     * PRODUCT_EXPIRATION} reminders at {@link ReminderLevel#GREEN}.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generateTwelveMonthExpirationReminders() {
        return generateTwelveMonthExpirationReminders(LocalDate.now());
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')")
    public List<ReminderScheduleView> generateTwelveMonthExpirationReminders(LocalDate asOfDate) {
        return generateProductExpirationReminders(
                asOfDate,
                ProductExpirationReminderRules.TWELVE_MONTH_WINDOW,
                ProductExpirationReminderRules.twelveMonthReminderLevel());
    }

    /**
     * Processes due reminders for send (KB E18 / BR-024 / item 401).
     *
     * <p>BR-024: payment-due reminders are cancelled (not sent) when the related payment is already
     * completed.
     *
     * <p>Item 401: reminders are cancelled (not sent) when the recipient fails consent or monthly
     * contact-limit eligibility at send time.
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public List<ReminderScheduleView> sendDueReminders() {
        return sendDueReminders(LocalDate.now());
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public List<ReminderScheduleView> sendDueReminders(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        return reminderRepository.findDueReminders(effectiveDate).stream()
                .map(this::sendOrCancel)
                .map(reminderRepository::save)
                .map(ReminderScheduleView::from)
                .toList();
    }

    /**
     * Attempts to mark a reminder sent. Payment-due reminders for completed payments are cancelled
     * instead (KB BR-024).
     */
    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ReminderScheduleView markSent(UUID reminderId) {
        ReminderSchedule reminder = findReminder(reminderId);
        return ReminderScheduleView.from(reminderRepository.save(sendOrCancel(reminder)));
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ReminderScheduleView cancelReminder(UUID reminderId) {
        ReminderSchedule reminder = findReminder(reminderId);
        reminder.cancel();
        return ReminderScheduleView.from(reminderRepository.save(reminder));
    }

    @Transactional(readOnly = true)
    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'COMPLIANCE_OFFICER')")
    public List<ReminderScheduleView> searchReminders(ReminderScheduleSearchCriteria criteria) {
        ReminderScheduleSearchCriteria effectiveCriteria =
                criteria == null ? new ReminderScheduleSearchCriteria(null, null, null) : criteria;

        return selectReminderCandidates(effectiveCriteria).stream()
                .filter(reminder -> matchesCriteria(reminder, effectiveCriteria))
                .map(ReminderScheduleView::from)
                .toList();
    }

    private ReminderScheduleView saveReminder(
            Customer customer, Product product, ReminderScheduleCommand command) {
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        command.reminderType(),
                        command.reminderLevel(),
                        command.scheduledDate());
        return ReminderScheduleView.from(reminderRepository.save(reminder));
    }

    private ReminderSchedule createGeneratedPaymentReminder(
            PaymentRecord payment, LocalDate scheduledDate) {
        Product product = payment.getProductOwnership().getProduct();
        // BR-020–BR-022: Green first, Yellow second, Red third / default risk.
        ReminderLevel level = PaymentReminderLevelRules.resolve(payment);
        return new ReminderSchedule(
                payment.getCustomer(),
                product,
                ReminderType.PAYMENT_DUE,
                level,
                scheduledDate);
    }

    private List<ReminderScheduleView> generateProductExpirationReminders(
            LocalDate asOfDate, int expirationWindowMonths, ReminderLevel reminderLevel) {
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        LocalDate expirationWindowEnd =
                ProductExpirationReminderRules.windowEnd(effectiveDate, expirationWindowMonths);

        return productOwnershipRepository
                .findExpiringBetween(effectiveDate, expirationWindowEnd)
                .stream()
                .filter(ownership -> isExpirationReminderCandidate(ownership, effectiveDate))
                .filter(ownership -> !hasExistingExpirationReminder(ownership, reminderLevel))
                .map(
                        ownership ->
                                createGeneratedExpirationReminder(
                                        ownership, effectiveDate, reminderLevel))
                .map(reminderRepository::save)
                .map(ReminderScheduleView::from)
                .toList();
    }

    private static ReminderSchedule createGeneratedExpirationReminder(
            ProductOwnership ownership, LocalDate scheduledDate, ReminderLevel reminderLevel) {
        return new ReminderSchedule(
                ownership.getCustomer(),
                ownership.getProduct(),
                ReminderType.PRODUCT_EXPIRATION,
                reminderLevel,
                scheduledDate);
    }

    private void markLikelyDefaultRiskAfterRedReminder(ReminderSchedule reminder) {
        if (reminder.getReminderLevel() != ReminderLevel.RED) {
            return;
        }

        paymentRecordRepository.findByCustomerId(reminder.getCustomerId()).stream()
                .filter(payment -> paymentRecordMatchesProduct(payment, reminder.getProductId()))
                .filter(payment -> payment.getStatus() != PaymentStatus.DEFAULT_RISK)
                .findFirst()
                .ifPresent(
                        payment -> {
                            payment.incrementReminder();
                            paymentRecordRepository.save(payment);
                        });
    }

    /**
     * Sends a due reminder or cancels it when send rules fail.
     *
     * <p>KB BR-024 / Sprint 16 critical item 660: payment reminder is not sent if payment is
     * completed — the schedule is cancelled and {@code sent_at} remains unset.
     *
     * <p>KB item 401: reminder is not sent if consent or monthly contact-limit eligibility fails —
     * the schedule is cancelled and {@code sent_at} remains unset.
     */
    private ReminderSchedule sendOrCancel(ReminderSchedule reminder) {
        if (reminder.getReminderType() == ReminderType.PAYMENT_DUE
                && isPaymentCompletedForReminder(
                        reminder.getCustomerId(), reminder.getProductId())) {
            reminder.cancel();
            return reminder;
        }
        if (!isReminderEligible(reminder.getCustomerId())) {
            reminder.cancel();
            return reminder;
        }

        reminder.markSent();
        return reminder;
    }

    private List<ReminderSchedule> selectReminderCandidates(
            ReminderScheduleSearchCriteria criteria) {
        if (criteria.dueOnOrBefore() != null) {
            return reminderRepository.findDueReminders(criteria.dueOnOrBefore());
        }
        if (criteria.customerId() != null) {
            return reminderRepository.findByCustomerId(criteria.customerId());
        }
        if (criteria.status() != null) {
            return reminderRepository.findByStatus(criteria.status());
        }
        return reminderRepository.findAll();
    }

    private static boolean matchesCriteria(
            ReminderSchedule reminder, ReminderScheduleSearchCriteria criteria) {
        return matchesCustomer(reminder, criteria.customerId())
                && matchesStatus(reminder, criteria.status())
                && matchesDueDate(reminder, criteria.dueOnOrBefore());
    }

    private static boolean matchesCustomer(ReminderSchedule reminder, UUID customerId) {
        return customerId == null || Objects.equals(reminder.getCustomerId(), customerId);
    }

    private static boolean matchesStatus(ReminderSchedule reminder, ReminderStatus status) {
        return status == null || reminder.getStatus() == status;
    }

    private static boolean matchesDueDate(ReminderSchedule reminder, LocalDate dueOnOrBefore) {
        return dueOnOrBefore == null || !reminder.getScheduledDate().isAfter(dueOnOrBefore);
    }

    /**
     * KB BR-024 helper: true when all payment records for the customer+product are completed
     * (paid), so payment reminders must not be scheduled or sent.
     */
    private boolean isPaymentCompletedForReminder(UUID customerId, UUID productId) {
        return hasOnlyCompletedPaymentRecords(customerId, productId);
    }

    private boolean hasOnlyCompletedPaymentRecords(UUID customerId, UUID productId) {
        List<PaymentRecord> matchingRecords =
                paymentRecordRepository.findByCustomerId(customerId).stream()
                        .filter(record -> paymentRecordMatchesProduct(record, productId))
                        .toList();

        return !matchingRecords.isEmpty()
                && matchingRecords.stream()
                        .allMatch(record -> record.getStatus() == PaymentStatus.PAID);
    }

    private static boolean paymentRecordMatchesProduct(PaymentRecord record, UUID productId) {
        ProductOwnership ownership = record.getProductOwnership();
        Product product = ownership == null ? null : ownership.getProduct();
        return product != null && Objects.equals(product.getId(), productId);
    }

    /**
     * Payment-due candidate guardrails (item 409): unpaid status, due date on/before evaluation
     * date, contactable customer, consent/contact-limit eligibility, and product linkage.
     */
    private boolean isPaymentReminderCandidate(PaymentRecord payment, LocalDate asOfDate) {
        return payment.getStatus() != PaymentStatus.PAID
                && payment.getDueDate() != null
                && !payment.getDueDate().isAfter(asOfDate)
                && payment.getCustomer() != null
                && payment.getCustomer().canBeContacted()
                && isReminderEligible(payment.getCustomer().getId())
                && payment.getProductOwnership() != null
                && payment.getProductOwnership().getProduct() != null;
    }

    private boolean hasExistingGeneratedPaymentReminder(PaymentRecord payment, LocalDate scheduledDate) {
        UUID customerId = payment.getCustomer().getId();
        UUID productId = payment.getProductOwnership().getProduct().getId();
        ReminderLevel level = PaymentReminderLevelRules.resolve(payment);

        return reminderRepository.findByCustomerId(customerId).stream()
                .anyMatch(
                        reminder ->
                                reminder.getReminderType() == ReminderType.PAYMENT_DUE
                                        && reminder.getReminderLevel() == level
                                        && Objects.equals(reminder.getProductId(), productId)
                                        && Objects.equals(
                                                reminder.getScheduledDate(), scheduledDate)
                                        && reminder.getStatus() != ReminderStatus.CANCELLED);
    }

    /**
     * Product-expiration candidate guardrails (item 409 / BR-023): active ownership, non-null
     * expiration on or after evaluation date (window upper bound is applied by {@code
     * findExpiringBetween}), contactable customer, consent/contact-limit eligibility, and product
     * linkage.
     */
    private boolean isExpirationReminderCandidate(
            ProductOwnership ownership, LocalDate evaluationDate) {
        Customer customer = ownership.getCustomer();
        return ownership.isActive()
                && ownership.getExpirationDate() != null
                && !ownership.getExpirationDate().isBefore(evaluationDate)
                && customer != null
                && customer.canBeContacted()
                && isReminderEligible(customer.getId())
                && ownership.getProduct() != null;
    }

    /**
     * Enforces consent and contact-limit rules before manual reminder scheduling (KB item 401).
     *
     * <p>Delegates to {@link EligibilityService#evaluateForReminder} with marketing-email consent
     * so invalid/missing consent, marketing opt-out, guardian requirements, do-not-contact, and
     * monthly contact limits all block scheduling.
     */
    private void ensureReminderEligibility(UUID customerId) {
        EligibilityDecision decision =
                eligibilityService.evaluateForReminder(customerId, REMINDER_CONSENT_TYPE);
        if (!decision.eligible()) {
            throw new BusinessRuleException(
                    REMINDER_RECIPIENT_INELIGIBLE,
                    "Reminder recipient is not eligible for contact: "
                            + decision.eligibilityExplanation());
        }
    }

    /**
     * Returns whether the customer may receive a reminder under consent and contact-limit rules
     * (KB item 401 / BR-011 / FR-092).
     */
    private boolean isReminderEligible(UUID customerId) {
        return eligibilityService.evaluateForReminder(customerId, REMINDER_CONSENT_TYPE).eligible();
    }

    private boolean hasExistingExpirationReminder(
            ProductOwnership ownership, ReminderLevel reminderLevel) {
        UUID customerId = ownership.getCustomer().getId();
        UUID productId = ownership.getProduct().getId();

        return reminderRepository.findByCustomerId(customerId).stream()
                .anyMatch(
                        reminder ->
                                reminder.getReminderType() == ReminderType.PRODUCT_EXPIRATION
                                        && reminder.getReminderLevel() == reminderLevel
                                        && Objects.equals(reminder.getProductId(), productId)
                                        && reminder.getStatus() != ReminderStatus.CANCELLED);
    }

    /**
     * Active ownership with a recorded expiration date is required for product-expiration
     * reminders (item 409 / BR-023).
     */
    private boolean hasActiveOwnershipWithExpirationDate(UUID customerId, UUID productId) {
        return productOwnershipRepository.findByCustomerId(customerId).stream()
                .anyMatch(
                        ownership ->
                                ownership.isActive()
                                        && ownership.getExpirationDate() != null
                                        && ownership.getProduct() != null
                                        && Objects.equals(
                                                ownership.getProduct().getId(), productId));
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private Product findProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private ReminderSchedule findReminder(UUID reminderId) {
        return reminderRepository
                .findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("ReminderSchedule", reminderId));
    }

    private static void validateCommand(ReminderScheduleCommand command, ReminderType expectedType) {
        if (command == null) {
            throw new ValidationException("Reminder command is required", List.of());
        }

        List<String> details =
                java.util.stream.Stream.of(
                                command.customerId() == null ? "customerId is required" : null,
                                command.productId() == null ? "productId is required" : null,
                                command.reminderLevel() == null
                                        ? "reminderLevel is required"
                                        : null,
                                command.scheduledDate() == null
                                        ? "scheduledDate is required"
                                        : null,
                                command.reminderType() != expectedType
                                        ? "reminderType must be " + expectedType
                                        : null)
                        .filter(Objects::nonNull)
                        .toList();

        if (!details.isEmpty()) {
            throw new ValidationException("Reminder command is invalid", details);
        }
    }
}
