package com.bayerwestphalian.campaign.consent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<ConsentRecord> findAllByOrderByCreatedAtDesc();

    @Query(
            """
            select consentRecord
            from ConsentRecord consentRecord
            where consentRecord.customer.id = :customerId
              and consentRecord.consentType = :consentType
              and consentRecord.status = 'GIVEN'
              and consentRecord.withdrawnAt is null
              and (consentRecord.expiresAt is null or consentRecord.expiresAt > :now)
            order by consentRecord.grantedAt desc nulls last, consentRecord.createdAt desc
            """)
    List<ConsentRecord> findValidConsents(
            @Param("customerId") UUID customerId,
            @Param("consentType") ConsentType consentType,
            @Param("now") Instant now,
            Pageable pageable);

    Optional<ConsentRecord> findFirstByCustomerIdAndConsentTypeOrderByCreatedAtDesc(
            UUID customerId, ConsentType consentType);

    List<ConsentRecord> findByCustomerIdAndStatusInOrderByCreatedAtDesc(
            UUID customerId, List<ConsentStatus> statuses);

    default List<ConsentRecord> findByCustomerId(UUID customerId) {
        return findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    default Optional<ConsentRecord> findValidConsent(
            UUID customerId, ConsentType consentType, Instant now) {
        return findValidConsents(customerId, consentType, now, PageRequest.of(0, 1)).stream()
                .findFirst();
    }

    default Optional<ConsentRecord> findLatestByType(UUID customerId, ConsentType consentType) {
        return findFirstByCustomerIdAndConsentTypeOrderByCreatedAtDesc(customerId, consentType);
    }

    default List<ConsentRecord> findOptOuts(UUID customerId) {
        return findByCustomerIdAndStatusInOrderByCreatedAtDesc(
                customerId, List.of(ConsentStatus.WITHDRAWN, ConsentStatus.REJECTED));
    }
}
