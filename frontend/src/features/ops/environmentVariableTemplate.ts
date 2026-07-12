/**
 * Sprint 17 item **688**: Add environment variable template.
 * Sprint 17 item **709**: Environment variable documentation.
 *
 * KB: deployment configuration is driven by environment variables. Catalog locks template paths
 * and required keys (pipeline / runtime secrets are not loaded by these unit tests).
 */

export const ENVIRONMENT_VARIABLE_TEMPLATE_ITEM = 688;

export const ENVIRONMENT_VARIABLE_TEMPLATE_STATEMENT = "Add environment variable template";

export const ENVIRONMENT_VARIABLE_DOCUMENTATION_ITEM = 709;

export const ENVIRONMENT_VARIABLE_DOCUMENTATION_STATEMENT =
  "Environment variable documentation";

export const ENVIRONMENT_VARIABLES_DOC_PATH = "docs/deployment/environment-variables.md";

export const ROOT_ENV_EXAMPLE_PATH = ".env.example";

export const BACKEND_ENV_EXAMPLE_PATH = "backend/.env.example";

export const FRONTEND_ENV_EXAMPLE_PATH = "frontend/.env.example";

export const BACKEND_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.EnvironmentVariableTemplateDocumentationTests";

export const ENVIRONMENT_VARIABLE_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.EnvironmentVariableDocumentationTests";

/** Keys that must appear in the root full-stack template. */
export const ROOT_ENV_TEMPLATE_REQUIRED_KEYS = [
  "SPRING_PROFILES_ACTIVE",
  "SERVER_PORT",
  "DB_URL",
  "DB_USERNAME",
  "DB_PASSWORD",
  "JWT_SECRET",
  "CORS_ALLOWED_ORIGINS",
  "POSTGRES_DB",
  "POSTGRES_USER",
  "POSTGRES_PASSWORD",
  "VITE_API_BASE_URL",
  "VITE_APP_ENV",
  "HTTPS_REQUIRED",
  "PROVIDER_REAL_SENDING_ENABLED",
] as const;

/** Keys that must appear in backend/.env.example. */
export const BACKEND_ENV_TEMPLATE_REQUIRED_KEYS = [
  "SPRING_PROFILES_ACTIVE",
  "DB_URL",
  "DB_USERNAME",
  "DB_PASSWORD",
  "JWT_SECRET",
  "CORS_ALLOWED_ORIGINS",
  "EMAIL_PROVIDER_MODE",
  "SMS_PROVIDER_MODE",
  "FILE_STORAGE_MODE",
] as const;

/** Keys that must appear in frontend/.env.example. */
export const FRONTEND_ENV_TEMPLATE_REQUIRED_KEYS = [
  "VITE_API_BASE_URL",
  "VITE_APP_ENV",
] as const;

export const ENVIRONMENT_VARIABLE_DOC_REQUIRED_MARKERS = [
  "709",
  "Environment variable documentation",
  "Variable catalog",
  "Classification",
  "Required production variables",
  "Secret variables",
  "Validation and startup behavior",
  "Change management",
  "Rotation notes",
  "Troubleshooting",
  "EnvironmentVariableDocumentationTests",
] as const;

/**
 * True when env-example text defines `KEY=` for every required key.
 */
export function envTemplateDefinesKeys(
  content: string,
  keys: readonly string[],
): boolean {
  if (content == null || content.trim() === "") {
    return false;
  }
  return keys.every((key) => content.includes(`${key}=`));
}

/**
 * True when template content looks like a safe example (no PEM blobs).
 */
export function envTemplateLooksLikeSafeExample(content: string): boolean {
  if (content == null || content.trim() === "") {
    return false;
  }
  if (content.includes("-----BEGIN")) {
    return false;
  }
  return content.includes("688") || content.toLowerCase().includes("example");
}

export function environmentVariableDocDefinesRequiredMarkers(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return ENVIRONMENT_VARIABLE_DOC_REQUIRED_MARKERS.every((marker) =>
    markdown.includes(marker),
  );
}
