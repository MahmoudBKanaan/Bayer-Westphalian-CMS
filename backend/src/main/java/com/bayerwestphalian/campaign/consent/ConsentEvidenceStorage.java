package com.bayerwestphalian.campaign.consent;

import java.util.UUID;

/** Secure storage boundary for consent evidence files. */
public interface ConsentEvidenceStorage {

    StoredConsentEvidence store(
            UUID customerId, String originalFilename, String contentType, byte[] content);

    byte[] read(String storageReference);
}
