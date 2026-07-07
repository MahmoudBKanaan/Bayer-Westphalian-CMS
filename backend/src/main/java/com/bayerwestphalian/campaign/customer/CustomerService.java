package com.bayerwestphalian.campaign.customer;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.common.api.PageResponse;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CustomerService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9 ()-]{7,50}$");

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public CustomerView createCustomer(CreateCustomerCommand command) {
        validateCreateCommand(command);

        Customer customer = createCustomerEntity(command);
        Customer savedCustomer = customerRepository.save(customer);
        auditService.logCreate(
                null, "customers", savedCustomer.getId(), customerAuditPayload(savedCustomer));

        return CustomerView.from(savedCustomer);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')")
    @Transactional
    public CustomerImportResult importCustomers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(
                    "Customer validation failed", List.of("file: must not be empty"));
        }

        List<CustomerImportRow> rows = parseCustomerCsv(file);
        if (rows.isEmpty()) {
            throw new ValidationException(
                    "Customer validation failed", List.of("file: must contain at least one row"));
        }

        List<CustomerImportError> errors =
                rows.stream().flatMap(row -> row.errors().stream()).toList();
        List<CustomerView> importedCustomers =
                rows.stream()
                        .filter(CustomerImportRow::valid)
                        .map(CustomerImportRow::command)
                        .map(this::createCustomerEntity)
                        .map(customerRepository::save)
                        .map(CustomerView::from)
                        .toList();
        int failedCount = (int) rows.stream().filter(row -> !row.valid()).count();

        return new CustomerImportResult(
                importedCustomers.size(), failedCount, importedCustomers, errors);
    }

    @PreAuthorize("@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT', 'COMPLIANCE_OFFICER')")
    @Transactional
    public CustomerView updateCustomer(UUID customerId, UpdateCustomerCommand command) {
        validateCustomerId(customerId);
        validateUpdateCommand(command);
        Customer customer = findCustomer(customerId);
        Map<String, ?> oldValue = customerAuditPayload(customer);
        boolean oldDoNotContact = customer.isDoNotContact();

        customer.rename(command.firstName().trim(), command.lastName().trim());
        customer.updateContactDetails(normalize(command.email()), normalize(command.phone()));
        customer.updateAddress(
                normalize(command.addressLine()),
                normalize(command.city()),
                normalize(command.country()));
        customer.updateDemographics(command.dateOfBirth(), command.ageGroup());
        if (command.status() != null) {
            customer.changeStatus(command.status());
        }
        if (command.doNotContact() != null && command.doNotContact()) {
            customer.markDoNotContact();
        } else if (command.doNotContact() != null) {
            customer.allowContact();
        }
        customer.recordSource(normalize(command.source()));
        Customer savedCustomer = customerRepository.save(customer);
        Map<String, ?> newValue = customerAuditPayload(savedCustomer);
        auditService.logUpdate(
                null,
                "customers",
                savedCustomer.getId(),
                oldValue,
                newValue);
        if (oldDoNotContact != savedCustomer.isDoNotContact()) {
            auditService.logDoNotContactUpdate(
                    null,
                    savedCustomer.getId(),
                    doNotContactAuditPayload(oldDoNotContact),
                    doNotContactAuditPayload(savedCustomer.isDoNotContact()));
        }

        return CustomerView.from(savedCustomer);
    }

    @PreAuthorize("@authz.hasRole('ADMIN')")
    @Transactional
    public CustomerView softDeleteCustomer(UUID customerId) {
        validateCustomerId(customerId);
        Customer customer = findCustomer(customerId);
        Map<String, ?> oldValue = customerAuditPayload(customer);

        customer.markDeleted();
        Customer savedCustomer = customerRepository.save(customer);
        auditService.logDelete(
                null,
                "customers",
                savedCustomer.getId(),
                oldValue,
                customerAuditPayload(savedCustomer));

        return CustomerView.from(savedCustomer);
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    @Transactional(readOnly = true)
    public CustomerView findById(UUID customerId) {
        validateCustomerId(customerId);
        return CustomerView.from(findCustomer(customerId));
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    @Transactional(readOnly = true)
    public List<CustomerView> searchCustomers(CustomerSearchCriteria criteria) {
        CustomerSearchCriteria normalized = normalize(criteria);

        return loadCandidates(normalized).stream()
                .filter(customer -> !customer.isDeleted())
                .filter(customer -> matches(customer, normalized))
                .map(CustomerView::from)
                .toList();
    }

    @PreAuthorize(
            "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'BI_ANALYST', 'COMPLIANCE_OFFICER', "
                    + "'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT')")
    @Transactional(readOnly = true)
    public PageResponse<CustomerView> searchCustomers(
            CustomerSearchCriteria criteria, int page, int size) {
        validatePageRequest(page, size);
        List<CustomerView> customers = searchCustomers(criteria);
        int fromIndex = Math.min(page * size, customers.size());
        int toIndex = Math.min(fromIndex + size, customers.size());
        int totalPages =
                customers.isEmpty() ? 0 : (int) Math.ceil((double) customers.size() / size);

        return PageResponse.of(
                customers.subList(fromIndex, toIndex), page, size, customers.size(), totalPages);
    }

    private void applyProfile(Customer customer, CreateCustomerCommand command) {
        customer.updateContactDetails(normalize(command.email()), normalize(command.phone()));
        customer.updateAddress(
                normalize(command.addressLine()),
                normalize(command.city()),
                normalize(command.country()));
        customer.updateDemographics(command.dateOfBirth(), command.ageGroup());
        customer.changeStatus(command.status() == null ? CustomerStatus.ACTIVE : command.status());
        if (command.doNotContact()) {
            customer.markDoNotContact();
        }
        customer.recordSource(normalize(command.source()));
    }

    private Customer createCustomerEntity(CreateCustomerCommand command) {
        validateCreateCommand(command);
        Customer customer =
                Customer.create(
                        command.customerType(),
                        command.firstName().trim(),
                        command.lastName().trim());
        applyProfile(customer, command);
        return customer;
    }

    private List<CustomerImportRow> parseCustomerCsv(MultipartFile file) {
        String content = csvContent(file);
        List<String> lines = content.lines().toList();
        int headerIndex = firstContentLineIndex(lines);
        if (headerIndex < 0) {
            return List.of();
        }

        List<String> headers =
                parseCsvLine(lines.get(headerIndex).trim()).stream()
                        .map(this::normalizeHeader)
                        .toList();
        validateImportHeaders(headers);

        List<CustomerImportRow> rows = new ArrayList<>();
        for (int index = headerIndex + 1; index < lines.size(); index++) {
            if (!StringUtils.hasText(lines.get(index))) {
                continue;
            }
            List<String> values = parseCsvLine(lines.get(index).trim());
            List<CustomerImportError> rowErrors = new ArrayList<>();
            if (values.size() > headers.size()) {
                rowErrors.add(
                        new CustomerImportError(
                                index + 1, "row", "has more values than the header row", null));
            }
            rows.add(importRow(row(headers, values), index + 1, rowErrors));
        }
        return rows;
    }

    private int firstContentLineIndex(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            if (StringUtils.hasText(lines.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private String csvContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ValidationException(
                    "Customer validation failed", List.of("file: could not be read"));
        }
    }

    private void validateImportHeaders(List<String> headers) {
        List<String> missingHeaders =
                List.of("customer_type", "first_name", "last_name").stream()
                        .filter(requiredHeader -> !headers.contains(requiredHeader))
                        .toList();
        if (!missingHeaders.isEmpty()) {
            throw new ValidationException(
                    "Customer validation failed",
                    missingHeaders.stream()
                            .map(header -> "file: missing required header " + header)
                            .toList());
        }
    }

    private CustomerImportRow importRow(
            Map<String, String> row, int lineNumber, List<CustomerImportError> rowErrors) {
        List<CustomerImportError> errors = new ArrayList<>(rowErrors);
        CustomerType customerType =
                enumValue(
                        CustomerType.class,
                        row.get("customer_type"),
                        "customer_type",
                        lineNumber,
                        errors);
        String firstName = requiredValue(row.get("first_name"), "first_name", lineNumber, errors);
        String lastName = requiredValue(row.get("last_name"), "last_name", lineNumber, errors);
        String email = row.get("email");
        String phone = row.get("phone");

        validateLength(firstName, "first_name", 100, lineNumber, errors);
        validateLength(lastName, "last_name", 100, lineNumber, errors);
        validateEmail(email, lineNumber, errors);
        validatePhone(phone, lineNumber, errors);
        validateLength(email, "email", 255, lineNumber, errors);
        validateLength(phone, "phone", 50, lineNumber, errors);
        validateLength(row.get("address_line"), "address_line", 255, lineNumber, errors);
        validateLength(row.get("city"), "city", 100, lineNumber, errors);
        validateLength(row.get("country"), "country", 100, lineNumber, errors);
        validateLength(row.get("source"), "source", 100, lineNumber, errors);

        LocalDate dateOfBirth = dateValue(row.get("date_of_birth"), lineNumber, errors);
        CustomerAgeGroup ageGroup = ageGroupValue(row.get("age_group"), lineNumber, errors);
        CustomerStatus status = defaultStatus(row.get("status"), lineNumber, errors);
        Boolean doNotContact = booleanValue(row.get("do_not_contact"), lineNumber, errors);

        if (!errors.isEmpty()) {
            return new CustomerImportRow(lineNumber, null, errors);
        }
        return new CustomerImportRow(
                lineNumber,
                new CreateCustomerCommand(
                        customerType,
                        firstName,
                        lastName,
                        email,
                        phone,
                        row.get("address_line"),
                        row.get("city"),
                        row.get("country"),
                        dateOfBirth,
                        ageGroup,
                        status,
                        Boolean.TRUE.equals(doNotContact),
                        row.get("source")),
                errors);
    }

    private Map<String, String> row(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < values.size() ? normalize(values.get(index)) : null;
            row.put(headers.get(index), value);
        }
        return row;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        values.add(value.toString());
        return values;
    }

    private <T extends Enum<T>> T enumValue(Class<T> enumType, String value) {
        return StringUtils.hasText(value)
                ? Enum.valueOf(enumType, value.trim().toUpperCase())
                : null;
    }

    private <T extends Enum<T>> T enumValue(
            Class<T> enumType,
            String value,
            String field,
            int lineNumber,
            List<CustomerImportError> errors) {
        if (!StringUtils.hasText(value)) {
            errors.add(new CustomerImportError(lineNumber, field, "must not be blank", value));
            return null;
        }
        try {
            return enumValue(enumType, value);
        } catch (IllegalArgumentException exception) {
            errors.add(new CustomerImportError(lineNumber, field, "has unsupported value", value));
            return null;
        }
    }

    private CustomerStatus defaultStatus(
            String value, int lineNumber, List<CustomerImportError> errors) {
        CustomerStatus status =
                optionalEnumValue(CustomerStatus.class, value, "status", lineNumber, errors);
        return status == null ? CustomerStatus.ACTIVE : status;
    }

    private CustomerAgeGroup ageGroupValue(
            String value, int lineNumber, List<CustomerImportError> errors) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            String normalized = value.trim().toUpperCase();
            if (normalized.matches("\\d{2}_\\d{2}") || normalized.equals("60_PLUS")) {
                return CustomerAgeGroup.fromDatabaseValue(normalized);
            }
            return CustomerAgeGroup.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            errors.add(
                    new CustomerImportError(
                            lineNumber, "age_group", "has unsupported value", value));
            return null;
        }
    }

    private LocalDate dateValue(String value, int lineNumber, List<CustomerImportError> errors) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.isAfter(LocalDate.now())) {
                errors.add(
                        new CustomerImportError(
                                lineNumber,
                                "date_of_birth",
                                "must be in the past or present",
                                value));
            }
            return date;
        } catch (RuntimeException exception) {
            errors.add(
                    new CustomerImportError(
                            lineNumber,
                            "date_of_birth",
                            "must use ISO date format yyyy-MM-dd",
                            value));
            return null;
        }
    }

    private Boolean booleanValue(String value, int lineNumber, List<CustomerImportError> errors) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("true") || normalized.equals("false")) {
            return Boolean.parseBoolean(normalized);
        }
        errors.add(
                new CustomerImportError(
                        lineNumber, "do_not_contact", "must be true or false", value));
        return null;
    }

    private <T extends Enum<T>> T optionalEnumValue(
            Class<T> enumType,
            String value,
            String field,
            int lineNumber,
            List<CustomerImportError> errors) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return enumValue(enumType, value);
        } catch (IllegalArgumentException exception) {
            errors.add(new CustomerImportError(lineNumber, field, "has unsupported value", value));
            return null;
        }
    }

    private String requiredValue(
            String value, String field, int lineNumber, List<CustomerImportError> errors) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        errors.add(new CustomerImportError(lineNumber, field, "must not be blank", value));
        return value;
    }

    private void validateLength(
            String value,
            String field,
            int maxLength,
            int lineNumber,
            List<CustomerImportError> errors) {
        if (value != null && value.length() > maxLength) {
            errors.add(
                    new CustomerImportError(
                            lineNumber,
                            field,
                            "must be at most " + maxLength + " characters",
                            value));
        }
    }

    private void validateEmail(String value, int lineNumber, List<CustomerImportError> errors) {
        if (StringUtils.hasText(value) && !EMAIL_PATTERN.matcher(value).matches()) {
            errors.add(
                    new CustomerImportError(lineNumber, "email", "must be a valid email", value));
        }
    }

    private void validatePhone(String value, int lineNumber, List<CustomerImportError> errors) {
        if (StringUtils.hasText(value) && !PHONE_PATTERN.matcher(value).matches()) {
            errors.add(
                    new CustomerImportError(
                            lineNumber, "phone", "must be a valid phone number", value));
        }
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private List<Customer> loadCandidates(CustomerSearchCriteria criteria) {
        if (StringUtils.hasText(criteria.term())) {
            return customerRepository.search(criteria.term());
        }
        if (criteria.status() != null) {
            return customerRepository.findByStatus(criteria.status());
        }
        if (StringUtils.hasText(criteria.city())) {
            return customerRepository.findByCity(criteria.city());
        }
        if (StringUtils.hasText(criteria.country())) {
            return customerRepository.findByCountry(criteria.country());
        }
        if (criteria.customerType() != null) {
            return customerRepository.findByCustomerType(criteria.customerType());
        }
        if (Boolean.TRUE.equals(criteria.contactable())) {
            return customerRepository.findByDoNotContactFalse();
        }
        return customerRepository.findActiveProfiles();
    }

    private boolean matches(Customer customer, CustomerSearchCriteria criteria) {
        return matchesCustomerType(customer, criteria.customerType())
                && matchesStatus(customer, criteria.status())
                && matchesCity(customer, criteria.city())
                && matchesCountry(customer, criteria.country())
                && matchesContactable(customer, criteria.contactable());
    }

    private boolean matchesCustomerType(Customer customer, CustomerType customerType) {
        return customerType == null || customer.getCustomerType() == customerType;
    }

    private boolean matchesStatus(Customer customer, CustomerStatus status) {
        return status == null || customer.getStatus() == status;
    }

    private boolean matchesCity(Customer customer, String city) {
        return !StringUtils.hasText(city)
                || (customer.getCity() != null && customer.getCity().equalsIgnoreCase(city));
    }

    private boolean matchesCountry(Customer customer, String country) {
        return !StringUtils.hasText(country)
                || (customer.getCountry() != null
                        && customer.getCountry().equalsIgnoreCase(country));
    }

    private boolean matchesContactable(Customer customer, Boolean contactable) {
        return contactable == null || customer.canBeContacted() == contactable;
    }

    private CustomerSearchCriteria normalize(CustomerSearchCriteria criteria) {
        if (criteria == null) {
            return new CustomerSearchCriteria(null, null, null, null, null, null);
        }
        return new CustomerSearchCriteria(
                normalize(criteria.term()),
                criteria.customerType(),
                criteria.status(),
                normalize(criteria.city()),
                normalize(criteria.country()),
                criteria.contactable());
    }

    private Customer findCustomer(UUID customerId) {
        return customerRepository
                .findById(customerId)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    private void validateCreateCommand(CreateCustomerCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Customer validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("customerType", command.customerType()),
                                required("firstName", command.firstName()),
                                required("lastName", command.lastName()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Customer validation failed", errors);
        }
    }

    private void validateUpdateCommand(UpdateCustomerCommand command) {
        if (command == null) {
            throw new ValidationException(
                    "Customer validation failed", List.of("command: is required"));
        }
        List<String> errors =
                List.of(
                                required("firstName", command.firstName()),
                                required("lastName", command.lastName()))
                        .stream()
                        .filter(StringUtils::hasText)
                        .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException("Customer validation failed", errors);
        }
    }

    private void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new ValidationException(
                    "Customer validation failed", List.of("customerId: is required"));
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new ValidationException(
                    "Customer validation failed",
                    List.of("page: must be greater than or equal to 0"));
        }
        if (size < 1 || size > 100) {
            throw new ValidationException(
                    "Customer validation failed", List.of("size: must be between 1 and 100"));
        }
    }

    private String required(String fieldName, String value) {
        return StringUtils.hasText(value) ? "" : fieldName + ": must not be blank";
    }

    private String required(String fieldName, Object value) {
        return value == null ? fieldName + ": must not be null" : "";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, ?> customerAuditPayload(Customer customer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerType", customer.getCustomerType().name());
        payload.put("firstName", customer.getFirstName());
        payload.put("lastName", customer.getLastName());
        putIfPresent(payload, "email", customer.getEmail());
        putIfPresent(payload, "phone", customer.getPhone());
        putIfPresent(payload, "city", customer.getCity());
        putIfPresent(payload, "country", customer.getCountry());
        if (customer.getAgeGroup() != null) {
            payload.put("ageGroup", customer.getAgeGroup().name());
        }
        payload.put("status", customer.getStatus().name());
        payload.put("doNotContact", customer.isDoNotContact());
        payload.put("active", customer.isActive());
        payload.put("deleted", customer.isDeleted());
        putIfPresent(payload, "source", customer.getSource());
        return payload;
    }

    private Map<String, ?> doNotContactAuditPayload(boolean doNotContact) {
        return Map.of("doNotContact", doNotContact);
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private record CustomerImportRow(
            int lineNumber, CreateCustomerCommand command, List<CustomerImportError> errors) {

        private CustomerImportRow {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }

        private boolean valid() {
            return errors.isEmpty();
        }
    }
}
