package com.bayerwestphalian.campaign.product;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public PaymentRecordService(
            PaymentRecordRepository paymentRecordRepository,
            ProductOwnershipRepository productOwnershipRepository,
            CustomerRepository customerRepository,
            AuditService auditService) {
        this(
                paymentRecordRepository,
                productOwnershipRepository,
                customerRepository,
                auditService,
                Clock.systemUTC());
    }

    PaymentRecordService(
            PaymentRecordRepository paymentRecordRepository,
            ProductOwnershipRepository productOwnershipRepository,
            CustomerRepository customerRepository,
            AuditService auditService,
            Clock clock) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.productOwnershipRepository = productOwnershipRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public PaymentRecordView createPaymentRecord(CreatePaymentRecordCommand command) {
        validateCreateCommand(command);
        Customer customer = findCustomer(command.customerId());
        ProductOwnership ownership =
                findOwnershipForCustomer(command.productOwnershipId(), customer);

        PaymentRecord payment =
                PaymentRecord.create(customer, ownership, command.dueDate(), command.amountDue());
        PaymentRecord savedPayment = paymentRecordRepository.save(payment);
        auditService.logCreate(
                null, "payment_records", savedPayment.getId(), paymentAuditPayload(savedPayment));

        return PaymentRecordView.from(savedPayment);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public PaymentRecordView updatePaymentRecord(
            UUID paymentId, UpdatePaymentRecordCommand command) {
        validatePaymentId(paymentId);
        validateUpdateCommand(command);
        PaymentRecord payment = findPayment(paymentId);
        ensureNotPaid(payment, "update");

        Map<String, ?> oldValue = paymentAuditPayload(payment);
        payment.updateDetails(command.dueDate(), command.amountDue());

        PaymentRecord savedPayment = paymentRecordRepository.save(payment);
        auditService.logUpdate(
                null,
                "payment_records",
                savedPayment.getId(),
                oldValue,
                paymentAuditPayload(savedPayment));

        return PaymentRecordView.from(savedPayment);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public PaymentRecordView markPaid(UUID paymentId, MarkPaymentPaidCommand command) {
        validatePaymentId(paymentId);
        validateMarkPaidCommand(command);
        PaymentRecord payment = findPayment(paymentId);
        ensureNotPaid(payment, "mark paid");

        Map<String, ?> oldValue = paymentAuditPayload(payment);
        Instant paidAt = command.paidAt() == null ? clock.instant() : command.paidAt();
        payment.markPaid(command.amountPaid(), paidAt);

        PaymentRecord savedPayment = paymentRecordRepository.save(payment);
        auditService.logUpdate(
                null,
                "payment_records",
                savedPayment.getId(),
                oldValue,
                paymentAuditPayload(savedPayment));

        return PaymentRecordView.from(savedPayment);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public PaymentRecordView markOverdue(UUID paymentId) {
        validatePaymentId(paymentId);
        PaymentRecord payment = findPayment(paymentId);
        ensureNotPaid(payment, "mark overdue");

        Map<String, ?> oldValue = paymentAuditPayload(payment);
        payment.markOverdue();

        PaymentRecord savedPayment = paymentRecordRepository.save(payment);
        auditService.logUpdate(
                null,
                "payment_records",
                savedPayment.getId(),
                oldValue,
                paymentAuditPayload(savedPayment));

        return PaymentRecordView.from(savedPayment);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public PaymentRecordView incrementReminder(UUID paymentId) {
        validatePaymentId(paymentId);
        PaymentRecord payment = findPayment(paymentId);
        ensureNotPaid(payment, "increment reminder");

        Map<String, ?> oldValue = paymentAuditPayload(payment);
        payment.incrementReminder();

        PaymentRecord savedPayment = paymentRecordRepository.save(payment);
        auditService.logUpdate(
                null,
                "payment_records",
                savedPayment.getId(),
                oldValue,
                paymentAuditPayload(savedPayment));

        return PaymentRecordView.from(savedPayment);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'EXECUTIVE_VIEWER', "
                    + "'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public List<PaymentRecordView> findDuePayments() {
        return paymentRecordRepository.findDuePayments().stream()
                .map(PaymentRecordView::from)
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'EXECUTIVE_VIEWER', "
                    + "'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public List<PaymentRecordView> findOverduePayments() {
        return paymentRecordRepository.findOverduePayments().stream()
                .map(PaymentRecordView::from)
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'EXECUTIVE_VIEWER', "
                    + "'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public List<PaymentRecordView> listCustomerPayments(UUID customerId) {
        validateCustomerId(customerId);

        return paymentRecordRepository.findByCustomerId(customerId).stream()
                .map(PaymentRecordView::from)
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'EXECUTIVE_VIEWER', "
                    + "'SYSTEM_AUDITOR')")
    @Transactional(readOnly = true)
    public List<PaymentRecordView> searchPayments(PaymentRecordSearchCriteria criteria) {
        PaymentRecordSearchCriteria normalized = normalize(criteria);

        return loadCandidates(normalized).stream()
                .filter(payment -> matches(payment, normalized))
                .map(PaymentRecordView::from)
                .toList();
    }

    private List<PaymentRecord> loadCandidates(PaymentRecordSearchCriteria criteria) {
        if (criteria.customerId() != null) {
            return paymentRecordRepository.findByCustomerId(criteria.customerId());
        }
        if (criteria.status() == PaymentStatus.DUE) {
            return paymentRecordRepository.findDuePayments();
        }
        if (criteria.status() == PaymentStatus.OVERDUE
                || criteria.status() == PaymentStatus.DEFAULT_RISK) {
            return paymentRecordRepository.findOverduePayments();
        }
        if (criteria.status() != null) {
            return paymentRecordRepository.findByStatusOrderByDueDateAsc(criteria.status());
        }
        return paymentRecordRepository.findAll();
    }

    private boolean matches(PaymentRecord payment, PaymentRecordSearchCriteria criteria) {
        return matchesCustomer(payment, criteria.customerId())
                && matchesStatus(payment, criteria.status());
    }

    private boolean matchesCustomer(PaymentRecord payment, UUID customerId) {
        return customerId == null || Objects.equals(payment.getCustomer().getId(), customerId);
    }

    private boolean matchesStatus(PaymentRecord payment, PaymentStatus status) {
        return status == null || payment.getStatus() == status;
    }

    private PaymentRecordSearchCriteria normalize(PaymentRecordSearchCriteria criteria) {
        if (criteria == null) {
            return new PaymentRecordSearchCriteria(null, null);
        }
        return criteria;
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private ProductOwnership findOwnershipForCustomer(UUID ownershipId, Customer customer) {
        ProductOwnership ownership =
                productOwnershipRepository
                        .findById(ownershipId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product ownership", ownershipId));
        if (!Objects.equals(ownership.getCustomer().getId(), customer.getId())) {
            throw new ValidationException(
                    "Payment record validation failed",
                    List.of("productOwnershipId: must belong to the specified customer"));
        }
        return ownership;
    }

    private PaymentRecord findPayment(UUID paymentId) {
        return paymentRecordRepository
                .findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record", paymentId));
    }

    private void ensureNotPaid(PaymentRecord payment, String action) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ValidationException(
                    "Payment record validation failed",
                    List.of("status: payment is already PAID and cannot " + action));
        }
    }

    private void validateCreateCommand(CreatePaymentRecordCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Payment record validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("customerId", command.customerId()),
                                required("productOwnershipId", command.productOwnershipId()),
                                required("dueDate", command.dueDate()),
                                validateAmount("amountDue", command.amountDue()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Payment record validation failed", errors);
        }
    }

    private void validateMarkPaidCommand(MarkPaymentPaidCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Payment record validation failed", List.of("command: is required"));
        }
        if (validateAmount("amountPaid", command.amountPaid()).isBlank()) {
            return;
        }
        throw new ValidationException(
                "Payment record validation failed",
                List.of(validateAmount("amountPaid", command.amountPaid())));
    }

    private void validateUpdateCommand(UpdatePaymentRecordCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Payment record validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("dueDate", command.dueDate()),
                                validateAmount("amountDue", command.amountDue()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Payment record validation failed", errors);
        }
    }

    private void validatePaymentId(UUID paymentId) {
        if (paymentId == null) {
            throw new ValidationException(
                    "Payment record validation failed", List.of("paymentId: is required"));
        }
    }

    private void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException(
                    "Payment record validation failed", List.of("customerId: is required"));
        }
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String validateAmount(String fieldName, BigDecimal amount) {
        if (amount == null) {
            return fieldName + ": must not be null";
        }
        if (amount.signum() < 0) {
            return fieldName + ": must be greater than or equal to 0.00";
        }
        return "";
    }

    private Map<String, ?> paymentAuditPayload(PaymentRecord payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", payment.getCustomer().getId());
        payload.put("productOwnershipId", payment.getProductOwnership().getId());
        payload.put("dueDate", payment.getDueDate().toString());
        payload.put("amountDue", payment.getAmountDue());
        if (payment.getAmountPaid() != null) {
            payload.put("amountPaid", payment.getAmountPaid());
        }
        if (payment.getPaidAt() != null) {
            payload.put("paidAt", payment.getPaidAt().toString());
        }
        payload.put("status", payment.getStatus().name());
        payload.put("reminderCount", payment.getReminderCount());
        payload.put("defaultRisk", payment.isDefaultRisk());
        return payload;
    }
}
