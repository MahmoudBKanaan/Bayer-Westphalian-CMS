/**
 * Login UI flow helpers (KB FR-001 / NFR-001 / authentication design / item 598).
 *
 * Pure rules for employee sign-in through the React UI: validation messages,
 * post-login navigation, auth-required notice, and safe error copy.
 */

import { z } from "zod";
import { ApiError, isAuthorizationError } from "@/api/client";

/** Default landing page after a successful sign-in with no deep-link return path. */
export const LOGIN_DEFAULT_LANDING_PATH = "/dashboard";

export const LOGIN_PAGE_TITLE = "Bayer-Westphalian Campaign Management";
export const LOGIN_PANEL_HEADING = "Sign in";
export const LOGIN_EMPLOYEE_HINT = "Use your employee account";
export const LOGIN_AUTH_REQUIRED_NOTICE =
  "Sign in with an authorized employee account to continue.";
export const LOGIN_INVALID_CREDENTIALS_MESSAGE =
  "Login failed. Check your credentials or account status.";
export const LOGIN_RATE_LIMITED_MESSAGE =
  "Too many failed sign-in attempts. Wait and try again, or contact an administrator.";
export const LOGIN_GENERIC_FAILURE_MESSAGE =
  "Login failed. Try again or contact an administrator.";

export const loginFormValidationMessages = {
  emailRequired: "Email is required.",
  emailInvalid: "Enter a valid internal email address.",
  passwordRequired: "Password is required.",
  passwordMinLength: "Password must be at least 8 characters.",
} as const;

export const loginSchema = z.object({
  email: z.string().trim().email(loginFormValidationMessages.emailInvalid),
  password: z.string().min(8, loginFormValidationMessages.passwordMinLength),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export type LoginFlowStepId = "open-login" | "enter-credentials" | "submit" | "land-protected";

export type LoginFlowStepDefinition = {
  id: LoginFlowStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Login flow works through UI” (item 598). */
export const LOGIN_FLOW_STEPS: LoginFlowStepDefinition[] = [
  {
    id: "open-login",
    index: 0,
    title: "Open sign-in",
    description: "Employee reaches /login (direct or redirected from a protected route).",
  },
  {
    id: "enter-credentials",
    index: 1,
    title: "Enter credentials",
    description: "Email and password fields accept internal employee account details.",
  },
  {
    id: "submit",
    index: 2,
    title: "Submit sign-in",
    description: "POST /api/auth/login; session is stored in sessionStorage on success.",
  },
  {
    id: "land-protected",
    index: 3,
    title: "Land on protected UI",
    description: "Navigate to the intended path or the default dashboard shell.",
  },
];

/**
 * Resolves the path after successful authentication.
 * Preserves deep links when ProtectedRoute redirected with {@code state.from.pathname}.
 */
export function getPostLoginPath(state: unknown): string {
  if (
    typeof state === "object" &&
    state != null &&
    "from" in state &&
    typeof state.from === "object" &&
    state.from != null &&
    "pathname" in state.from &&
    typeof state.from.pathname === "string" &&
    state.from.pathname.trim().length > 0 &&
    state.from.pathname.startsWith("/") &&
    !state.from.pathname.startsWith("//")
  ) {
    return state.from.pathname;
  }

  return LOGIN_DEFAULT_LANDING_PATH;
}

/**
 * Notice shown when the user was sent to login because a protected route required auth.
 */
export function getLoginNotice(state: unknown): string {
  if (
    typeof state === "object" &&
    state != null &&
    "reason" in state &&
    state.reason === "auth-required"
  ) {
    return LOGIN_AUTH_REQUIRED_NOTICE;
  }

  return "";
}

/**
 * Safe user-facing error for login failures (no stack traces or raw server dumps).
 * Aligns with authentication design + login rate limiting (item 544).
 */
export function loginErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 429) {
    return LOGIN_RATE_LIMITED_MESSAGE;
  }
  if (isAuthorizationError(error)) {
    return LOGIN_INVALID_CREDENTIALS_MESSAGE;
  }
  return LOGIN_GENERIC_FAILURE_MESSAGE;
}

/**
 * Validates login form values with the shared Zod schema (client-side gate before API call).
 */
export function validateLoginForm(
  values: LoginFormValues,
): Partial<Record<keyof LoginFormValues, string>> {
  const parsed = loginSchema.safeParse(values);
  if (parsed.success) {
    return {};
  }

  const fieldErrors: Partial<Record<keyof LoginFormValues, string>> = {};
  for (const issue of parsed.error.issues) {
    const fieldName = issue.path[0];
    if (fieldName === "email" || fieldName === "password") {
      fieldErrors[fieldName] = issue.message;
    }
  }
  return fieldErrors;
}

export function loginFlowStepIdsInOrder(): LoginFlowStepId[] {
  return [...LOGIN_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatLoginFlowJourney(
  steps: readonly LoginFlowStepDefinition[] = LOGIN_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidLoginFlowOrder(observed: readonly LoginFlowStepId[]): boolean {
  const expected = loginFlowStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}
