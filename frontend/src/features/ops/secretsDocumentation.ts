/**
 * Sprint 17 item **689**: Add secrets documentation.
 * Sprint 17 item **710**: Secrets documentation.
 *
 * KB: production secrets must live outside Git and must not appear in CI YAML or images.
 * Catalog locks doc paths and secret *names* only (no secret values).
 */

export const SECRETS_DOCUMENTATION_ITEM = 689;

export const SECRETS_DOCUMENTATION_STATEMENT = "Add secrets documentation";

export const SECRETS_DOCUMENTATION_EXPANSION_ITEM = 710;

export const SECRETS_DOCUMENTATION_EXPANSION_STATEMENT = "Secrets documentation";

export const SECRETS_DOC_PATH = "docs/deployment/secrets.md";

export const ENVIRONMENT_VARIABLES_DOC_PATH = "docs/deployment/environment-variables.md";

export const SECURITY_HARDENING_DOC_PATH = "docs/architecture/security-hardening.md";

export const BACKEND_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.SecretsDocumentationTests";

export const SECRETS_DOCUMENTATION_EXPANSION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.SecretsDocumentationExpansionTests";

/** Secret env *names* that the secrets guide must document. */
export const DOCUMENTED_SECRET_ENV_NAMES = [
  "JWT_SECRET",
  "DB_PASSWORD",
  "SMTP_PASSWORD",
  "SMS_API_KEY",
] as const;

/** Substrings required in docs/deployment/secrets.md. */
export const SECRETS_DOC_REQUIRED_MARKERS = [
  "689",
  "JWT_SECRET",
  "DB_PASSWORD",
  "SecretPresenceValidator",
  "MissingSecretsAreDetectedTests",
  "secret manager",
  "Never commit",
  "Docker",
  "GitHub Actions",
  "SecretsDocumentationTests",
] as const;

export const SECRETS_DOCUMENTATION_EXPANSION_REQUIRED_MARKERS = [
  "710",
  "Secrets documentation",
  "Secret ownership and access",
  "Provisioning checklist",
  "Rotation schedule",
  "Leak response runbook",
  "Backup and restore handling",
  "Audit evidence",
  "SecretsDocumentationExpansionTests",
] as const;

/**
 * True when secrets markdown includes all required markers.
 */
export function secretsDocDefinesRequiredMarkers(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return SECRETS_DOC_REQUIRED_MARKERS.every((marker) => markdown.includes(marker));
}

/**
 * True when text documents all listed secret env names (names only).
 */
export function secretsDocNamesAllSecrets(
  markdown: string,
  names: readonly string[] = DOCUMENTED_SECRET_ENV_NAMES,
): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return names.every((name) => markdown.includes(name));
}

/**
 * True when content does not look like embedded secret material.
 */
export function secretsDocLooksSafe(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  if (markdown.includes("-----BEGIN")) {
    return false;
  }
  return true;
}

export function secretsDocDefinesExpansionMarkers(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return SECRETS_DOCUMENTATION_EXPANSION_REQUIRED_MARKERS.every((marker) =>
    markdown.includes(marker),
  );
}
