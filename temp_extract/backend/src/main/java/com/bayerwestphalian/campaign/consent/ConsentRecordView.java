package com.bayerwestphalian.campaign.consent;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.util.UUID;

public record ConsentRecordView(
        UUID id,
        UUID customerId,
        String customerFullName,
        ConsentType consentType,
        ConsentStatus status,
        String purpose,
        String source,
        Instant grantedAt,
        Instant withdrawnAt,
        Instant expiresAt,
        String evidenceFileUrl,
        UUID createdBy,
        String createdByFullName,
        Instant createdAt,
        boolean valid,
        boolean requiresAction) {

    public static ConsentRecordView from(ConsentRecord consentRecord, Instant now) {
        Customer customer = consentRecord.getCustomer();
        User createdBy = consentRecord.getCreatedBy();

        return new ConsentRecordView(
                consentRecord.getId(),
                customerId(customer),
                customerFullName(customer),
                consentRecord.getConsentType(),
                consentRecord.getStatus(),
                consentRecord.getPurpose(),
                consentRecord.getSource(),
                consentRecord.getGrantedAt(),
                consentRecord.getWithdrawnAt(),
                consentRecord.getExpiresAt(),
                consentRecord.getEvidenceFileUrl(),
                userId(createdBy),
                userFullName(createdBy),
                consentRecord.getCreatedAt(),
                consentRecord.isValid(now),
                consentRecord.requiresAction(now));
    }

    private static UUID customerId(Customer customer) {
        return customer == null ? null : customer.getId();
    }

    private static String customerFullName(Customer customer) {
        return customer == null ? null : customer.getFullName();
    }

    private static UUID userId(User user) {
        return user == null ? null : user.getId();
    }

    private static String userFullName(User user) {
        return user == null ? null : user.getFullName();
    }
}
