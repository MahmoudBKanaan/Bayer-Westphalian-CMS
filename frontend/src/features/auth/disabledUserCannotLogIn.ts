/**
 * Sprint 16 critical test item **659**: Disabled user cannot log in.
 *
 * KB: FR-001 (login), FR-005 (Admin disables users), NFR-001 (security).
 * Backend AuthService rejects non-ACTIVE status after credential match; no JWT session.
 */

export const DISABLED_USER_CANNOT_LOG_IN_ITEM = 659;

export const DISABLED_USER_CANNOT_LOG_IN_STATEMENT = "Disabled user cannot log in";

export const DISABLED_USER_CANNOT_LOG_IN_FR = ["FR-001", "FR-005"] as const;

export const DISABLED_USER_CANNOT_LOG_IN_NFR = ["NFR-001"] as const;

/** Backend message when status is not ACTIVE (DISABLED / LOCKED). */
export const ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE = "User account is not active" as const;

/**
 * Safe UI copy for failed login (includes disabled/locked account cases without
 * distinguishing them from bad passwords — avoids account-status enumeration).
 * Aligns with `loginFlow.LOGIN_INVALID_CREDENTIALS_MESSAGE`.
 */
export const DISABLED_LOGIN_UI_MESSAGE =
  "Login failed. Check your credentials or account status." as const;

export const ACTIVE_USER_STATUS = "ACTIVE" as const;
export const DISABLED_USER_STATUS = "DISABLED" as const;
export const LOCKED_USER_STATUS = "LOCKED" as const;

export type EmployeeAccountStatus =
  | typeof ACTIVE_USER_STATUS
  | typeof DISABLED_USER_STATUS
  | typeof LOCKED_USER_STATUS;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.auth.DisabledUserCannotLogInTests";

export const COMPANION_AUTH_SERVICE_TEST_CLASS =
  "com.bayerwestphalian.campaign.auth.AuthServiceTests";

export const COMPANION_AUTH_CONTROLLER_TEST_CLASS =
  "com.bayerwestphalian.campaign.auth.AuthControllerTests";

export const AUTHENTICATION_DESIGN_DOC_PATH = "docs/architecture/authentication-design.md";

export const USER_MANAGEMENT_GUIDE_DOC_PATH = "docs/admin/user-management-guide.md";

/**
 * True when an employee account status is allowed to authenticate.
 */
export function canLogInWithStatus(
  status: EmployeeAccountStatus | string | null | undefined,
): boolean {
  return status === ACTIVE_USER_STATUS;
}

/**
 * True when login must be denied for account status reasons.
 */
export function isLoginBlockedByAccountStatus(
  status: EmployeeAccountStatus | string | null | undefined,
): boolean {
  return status === DISABLED_USER_STATUS || status === LOCKED_USER_STATUS;
}

/**
 * Backend-aligned: successful login requires ACTIVE; DISABLED/LOCKED map to not-active.
 */
export function loginDeniedReasonForStatus(
  status: EmployeeAccountStatus | string | null | undefined,
): typeof ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE | null {
  if (status == null || status === ACTIVE_USER_STATUS) {
    return null;
  }
  if (isLoginBlockedByAccountStatus(status) || status !== ACTIVE_USER_STATUS) {
    return ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE;
  }
  return null;
}
