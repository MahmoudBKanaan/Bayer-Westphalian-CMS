package com.bayerwestphalian.campaign.consent;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Production filesystem storage backed by the dedicated consent-evidence volume. */
@Service
@Profile("prod")
@ConditionalOnProperty(
        prefix = "app.providers.file-storage",
        name = "mode",
        havingValue = "filesystem")
public class FileSystemConsentEvidenceStorage implements ConsentEvidenceStorage {

    private static final Map<String, String> EXTENSIONS =
            Map.of(
                    "application/pdf", ".pdf",
                    "image/png", ".png",
                    "image/jpeg", ".jpg");
    private static final Set<String> ALLOWED_CONTENT_TYPES = EXTENSIONS.keySet();

    private final Path root;
    private final long maxBytes;

    public FileSystemConsentEvidenceStorage(
            @Value("${app.providers.file-storage.local-path}") String configuredRoot,
            @Value("${app.providers.file-storage.max-bytes:10485760}") long maxBytes) {
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new IllegalStateException("Consent evidence storage root is required");
        }
        if (maxBytes < 1) {
            throw new IllegalStateException("Consent evidence maximum size must be positive");
        }
        this.root = Path.of(configuredRoot).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    @Override
    public StoredConsentEvidence store(
            UUID customerId, String originalFilename, String contentType, byte[] content) {
        if (customerId == null) {
            throw validation("Customer is required", "CONSENT_EVIDENCE_CUSTOMER_REQUIRED");
        }
        String normalizedType = normalizeContentType(contentType);
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw validation(
                    "Consent evidence must be PDF, PNG, or JPEG",
                    "CONSENT_EVIDENCE_TYPE_INVALID");
        }
        if (content == null || content.length == 0) {
            throw validation(
                    "Consent evidence file must not be empty", "CONSENT_EVIDENCE_EMPTY");
        }
        if (content.length > maxBytes) {
            throw validation(
                    "Consent evidence file exceeds the size limit", "CONSENT_EVIDENCE_TOO_LARGE");
        }
        if (!hasExpectedSignature(normalizedType, content)) {
            throw validation(
                    "Consent evidence content does not match its declared type",
                    "CONSENT_EVIDENCE_CONTENT_INVALID");
        }

        String generatedName = UUID.randomUUID() + EXTENSIONS.get(normalizedType);
        String reference = "consent-evidence/" + customerId + "/" + generatedName;
        Path target = resolveReference(reference);
        Path temporary = target.resolveSibling(generatedName + ".tmp");

        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, content);
            moveAtomically(temporary, target);
        } catch (IOException exception) {
            tryDelete(temporary);
            throw new IllegalStateException("Unable to store consent evidence", exception);
        }
        return new StoredConsentEvidence(reference, normalizedType, content.length);
    }

    @Override
    public byte[] read(String storageReference) {
        Path target = resolveReference(storageReference);
        try {
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read consent evidence", exception);
        }
    }

    Path resolveReference(String storageReference) {
        if (storageReference == null || storageReference.isBlank()) {
            throw validation(
                    "Storage reference is required", "CONSENT_EVIDENCE_REFERENCE_REQUIRED");
        }
        Path resolved = root.resolve(storageReference).normalize();
        if (!resolved.startsWith(root)) {
            throw validation(
                    "Storage reference is invalid", "CONSENT_EVIDENCE_REFERENCE_INVALID");
        }
        return resolved;
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasExpectedSignature(String contentType, byte[] content) {
        return switch (contentType) {
            case "application/pdf" ->
                    content.length >= 5
                            && content[0] == '%'
                            && content[1] == 'P'
                            && content[2] == 'D'
                            && content[3] == 'F'
                            && content[4] == '-';
            case "image/png" ->
                    content.length >= 8
                            && content[0] == (byte) 0x89
                            && content[1] == 'P'
                            && content[2] == 'N'
                            && content[3] == 'G'
                            && content[4] == 0x0D
                            && content[5] == 0x0A
                            && content[6] == 0x1A
                            && content[7] == 0x0A;
            case "image/jpeg" ->
                    content.length >= 3
                            && content[0] == (byte) 0xFF
                            && content[1] == (byte) 0xD8
                            && content[2] == (byte) 0xFF;
            default -> false;
        };
    }

    private static ValidationException validation(String message, String code) {
        return new ValidationException(message, java.util.List.of(code));
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Preserve the original storage failure.
        }
    }
}
