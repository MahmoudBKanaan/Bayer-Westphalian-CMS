package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemConsentEvidenceStorageTests {

    @TempDir Path storageRoot;

    @Test
    void storesAndReadsEvidenceUnderGeneratedCustomerPath() throws Exception {
        FileSystemConsentEvidenceStorage storage =
                new FileSystemConsentEvidenceStorage(storageRoot.toString(), 1024);
        UUID customerId = UUID.randomUUID();
        byte[] content = "%PDF-1.7 evidence".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        StoredConsentEvidence stored =
                storage.store(customerId, "../../customer-consent.pdf", "application/pdf", content);

        assertThat(stored.storageReference())
                .startsWith("consent-evidence/" + customerId + "/")
                .endsWith(".pdf")
                .doesNotContain("customer-consent.pdf")
                .doesNotContain("..");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.sizeBytes()).isEqualTo(content.length);
        assertThat(storage.read(stored.storageReference())).isEqualTo(content);
        assertThat(Files.isRegularFile(storage.resolveReference(stored.storageReference()))).isTrue();
    }

    @Test
    void acceptsOnlyApprovedTypesAndConfiguredSize() {
        FileSystemConsentEvidenceStorage storage =
                new FileSystemConsentEvidenceStorage(storageRoot.toString(), 4);

        assertThatThrownBy(
                        () ->
                                storage.store(
                                        UUID.randomUUID(),
                                        "evidence.txt",
                                        "text/plain",
                                        new byte[] {1}))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PDF, PNG, or JPEG");
        assertThatThrownBy(
                        () ->
                                storage.store(
                                        UUID.randomUUID(),
                                        "evidence.pdf",
                                        "application/pdf",
                                        new byte[] {1, 2, 3, 4, 5}))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("size limit");
    }

    @Test
    void rejectsReferencesThatEscapeStorageRoot() {
        FileSystemConsentEvidenceStorage storage =
                new FileSystemConsentEvidenceStorage(storageRoot.toString(), 1024);

        assertThatThrownBy(() -> storage.read("../../outside.pdf"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reference is invalid");
    }

    @Test
    void rejectsContentWhoseSignatureDoesNotMatchDeclaredType() {
        FileSystemConsentEvidenceStorage storage =
                new FileSystemConsentEvidenceStorage(storageRoot.toString(), 1024);

        assertThatThrownBy(
                        () ->
                                storage.store(
                                        UUID.randomUUID(),
                                        "renamed.pdf",
                                        "application/pdf",
                                        "not a pdf".getBytes(
                                                java.nio.charset.StandardCharsets.UTF_8)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not match its declared type");
    }

    @Test
    void rejectsInvalidStorageConfiguration() {
        assertThatThrownBy(() -> new FileSystemConsentEvidenceStorage(" ", 1024))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage root is required");
        assertThatThrownBy(
                        () ->
                                new FileSystemConsentEvidenceStorage(
                                        storageRoot.toString(), 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum size must be positive");
    }
}
