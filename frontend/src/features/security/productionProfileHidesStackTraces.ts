/**
 * Sprint 16 critical test item **664**: Production profile hides stack traces.
 *
 * KB: NFR-001 / NFR-014 — clients never receive stack traces or internal exception dumps under
 * the `prod` profile (`application-prod.yml` + ProductionErrorSafetyConfiguration + GlobalExceptionHandler).
 */

export const PRODUCTION_PROFILE_HIDES_STACK_TRACES_ITEM = 664;

export const PRODUCTION_PROFILE_HIDES_STACK_TRACES_STATEMENT =
  "Production profile hides stack traces";

export const PRODUCTION_PROFILE_HIDES_STACK_TRACES_NFR = ["NFR-001", "NFR-014"] as const;

/** Spring profile that must hide client stack traces. */
export const PRODUCTION_SPRING_PROFILE = "prod" as const;

/** Required `server.error` settings in application-prod.yml. */
export const PRODUCTION_ERROR_YAML_SETTINGS = {
  includeStacktrace: "never",
  includeMessage: "never",
  includeBindingErrors: "never",
  includeException: false,
} as const;

/** Keys that must never appear in client error JSON. */
export const FORBIDDEN_CLIENT_ERROR_KEYS = [
  "trace",
  "stackTrace",
  "exception",
  "message",
] as const;

export type ForbiddenClientErrorKey = (typeof FORBIDDEN_CLIENT_ERROR_KEYS)[number];

/** Safe unexpected-error API contract. */
export const SAFE_INTERNAL_ERROR_CODE = "INTERNAL_ERROR" as const;
export const SAFE_INTERNAL_ERROR_MESSAGE = "Unexpected server error" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.api.ProductionProfileHidesStackTracesTests";

export const COMPANION_STACK_TRACE_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.api.ProductionStackTraceHiddenTests";

export const SECURITY_HARDENING_DOC_PATH = "docs/architecture/security-hardening.md";

export const PRODUCTION_SECURITY_CHECKLIST_DOC_PATH =
  "docs/deployment/production-security-checklist.md";

export type ApiErrorBodyLike = Record<string, unknown> | null | undefined;

/**
 * True when a key is forbidden on client-facing production error payloads.
 */
export function isForbiddenClientErrorKey(key: string | null | undefined): boolean {
  if (key == null || key === "") {
    return false;
  }
  return (FORBIDDEN_CLIENT_ERROR_KEYS as readonly string[]).includes(key);
}

/**
 * True when an API error body is free of stack-trace / exception leakage markers.
 */
export function clientErrorBodyHidesStackTraces(body: ApiErrorBodyLike): boolean {
  if (body == null || typeof body !== "object") {
    return false;
  }
  for (const key of FORBIDDEN_CLIENT_ERROR_KEYS) {
    if (key in body && key !== "message") {
      // `message` is allowed only when it is the safe generic text.
      return false;
    }
  }
  if ("message" in body && body.message !== SAFE_INTERNAL_ERROR_MESSAGE) {
    // Application errors may have other safe messages; for unexpected 500s only the generic
    // text is allowed. Non-500 contract is out of scope for this helper.
    const status = body.status;
    if (status === 500 || body.code === SAFE_INTERNAL_ERROR_CODE) {
      return false;
    }
  }
  if ("trace" in body || "stackTrace" in body || "exception" in body) {
    return false;
  }
  const serialized = JSON.stringify(body);
  if (serialized.includes("\tat ") || serialized.includes("java.lang.")) {
    return false;
  }
  return true;
}

/**
 * True when production YAML settings match the KB hide-stack-trace policy.
 */
export function productionErrorYamlHidesStackTraces(settings: {
  includeStacktrace?: string;
  includeMessage?: string;
  includeBindingErrors?: string;
  includeException?: boolean;
}): boolean {
  return (
    settings.includeStacktrace === PRODUCTION_ERROR_YAML_SETTINGS.includeStacktrace &&
    settings.includeMessage === PRODUCTION_ERROR_YAML_SETTINGS.includeMessage &&
    settings.includeBindingErrors === PRODUCTION_ERROR_YAML_SETTINGS.includeBindingErrors &&
    settings.includeException === PRODUCTION_ERROR_YAML_SETTINGS.includeException
  );
}
