/**
 * Sprint 16 critical test item **665**: Missing secrets are detected.
 *
 * KB: NFR-001 / items 543 & 555 — production startup fails when required secrets are missing,
 * unresolved, placeholders, or too short. Error messages name keys only (never secret values).
 */

export const MISSING_SECRETS_ARE_DETECTED_ITEM = 665;

export const MISSING_SECRETS_ARE_DETECTED_STATEMENT = "Missing secrets are detected";

export const MISSING_SECRETS_ARE_DETECTED_NFR = ["NFR-001"] as const;

export const PRODUCTION_SPRING_PROFILE = "prod" as const;

/** Always-required production secrets (SecretPresenceValidator.productionRequiredSecretNames). */
export const REQUIRED_PRODUCTION_SECRETS = ["JWT_SECRET", "DB_PASSWORD"] as const;

export type RequiredProductionSecret = (typeof REQUIRED_PRODUCTION_SECRETS)[number];

export const MIN_JWT_SECRET_LENGTH = 32 as const;
export const MIN_DB_PASSWORD_LENGTH = 8 as const;

/** Known insecure placeholder tokens rejected in production. */
export const FORBIDDEN_SECRET_PLACEHOLDERS = [
  "dev-only-change-me",
  "changeme",
  "change-me",
  "changeit",
  "secret",
  "password",
  "passw0rd",
  "admin",
  "default",
  "todo",
  "replace-me",
  "your-secret-here",
] as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.MissingSecretsAreDetectedTests";

export const COMPANION_SECRET_PRESENCE_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.SecretPresenceValidatorTests";

export const SECURITY_HARDENING_DOC_PATH = "docs/architecture/security-hardening.md";

export const PRODUCTION_SECURITY_CHECKLIST_DOC_PATH =
  "docs/deployment/production-security-checklist.md";

export type SecretPresenceCheckResult =
  | { ok: true }
  | { ok: false; secretName: string; reason: "missing" | "placeholder" | "too-short" | "unresolved" };

/**
 * True when a value is a known insecure placeholder (does not echo the value in callers).
 */
export function isForbiddenSecretPlaceholder(value: string | null | undefined): boolean {
  if (value == null || value.trim() === "") {
    return true;
  }
  const normalized = value.trim().toLowerCase();
  if ((FORBIDDEN_SECRET_PLACEHOLDERS as readonly string[]).includes(normalized)) {
    return true;
  }
  return (
    normalized.startsWith("changeme") ||
    normalized.startsWith("replace") ||
    normalized.includes("your-secret") ||
    normalized.includes("example-secret")
  );
}

/**
 * True when a Spring-style unresolved placeholder remains (e.g. ${JWT_SECRET}).
 */
export function looksLikeUnresolvedPlaceholder(value: string | null | undefined): boolean {
  if (value == null) {
    return false;
  }
  const trimmed = value.trim();
  return trimmed.startsWith("${") && trimmed.endsWith("}");
}

/**
 * Validates a single required production secret without returning the secret value.
 */
export function checkRequiredSecretPresence(options: {
  secretName: RequiredProductionSecret | string;
  value: string | null | undefined;
  minLength: number;
}): SecretPresenceCheckResult {
  const { secretName, value, minLength } = options;
  if (value == null || value.trim() === "") {
    return { ok: false, secretName, reason: "missing" };
  }
  if (looksLikeUnresolvedPlaceholder(value)) {
    return { ok: false, secretName, reason: "unresolved" };
  }
  if (isForbiddenSecretPlaceholder(value)) {
    return { ok: false, secretName, reason: "placeholder" };
  }
  if (value.trim().length < minLength) {
    return { ok: false, secretName, reason: "too-short" };
  }
  return { ok: true };
}

/**
 * Safe error fragment for ops (secret name only).
 */
export function safeMissingSecretErrorMessage(secretName: string): string {
  return `${secretName} is required`;
}

/**
 * True when an error message appears safe (names keys, does not embed common secret-looking dumps).
 */
export function configurationErrorDoesNotLeakSecretValues(
  message: string,
  secretValues: readonly string[],
): boolean {
  if (!message.includes("secret") && !message.includes("JWT_SECRET") && !message.includes("DB_PASSWORD")) {
    // Still reject if any known secret value appears.
  }
  return secretValues.every((secret) => secret === "" || !message.includes(secret));
}
