import type { AuthenticatedSession, AuthenticatedUser } from "@/api/auth";

/**
 * Frontend auth persistence strategy.
 *
 * Tokens are stored in {@link localStorage} so the same-origin session is shared across
 * browser tabs/windows. Opening an in-app link in a new tab must not force re-login.
 *
 * Historically this module used {@link sessionStorage} (tab-scoped). On load, a complete
 * legacy sessionStorage payload is migrated once into localStorage and then removed.
 *
 * Module name retained for import stability across the codebase.
 */

export type SystemRoleName =
  | "ADMIN"
  | "CAMPAIGN_MANAGER"
  | "BI_ANALYST"
  | "PRODUCT_MANAGER"
  | "COMPLIANCE_OFFICER"
  | "CUSTOMER_SERVICE_AGENT"
  | "SALES_AGENT"
  | "MARKETING_ANALYST"
  | "EXECUTIVE_VIEWER"
  | "SYSTEM_AUDITOR";

export const AUTH_STORAGE_KEYS = {
  accessToken: "bwc.accessToken",
  refreshToken: "bwc.refreshToken",
  currentUser: "bwc.currentUser",
} as const;

export const AUTH_SESSION_CHANGED_EVENT = "bwc-auth-session-changed";

export type StoredAuthSession = {
  accessToken: string;
  refreshToken: string;
  user: AuthenticatedUser;
  roles: SystemRoleName[];
};

export function saveAuthSession(session: AuthenticatedSession) {
  writeAuthPayload(localStorage, {
    accessToken: session.tokens.accessToken,
    refreshToken: session.tokens.refreshToken,
    currentUserJson: JSON.stringify(session.user),
  });
  // Avoid dual sources of truth after login/refresh.
  removeAuthKeys(sessionStorage);
  notifyAuthSessionChanged();
}

export function loadAuthSession(): StoredAuthSession | null {
  const fromLocal = readAuthPayload(localStorage);
  if (isCompleteAuthPayload(fromLocal)) {
    removeAuthKeys(sessionStorage);
    return toStoredSession(fromLocal);
  }

  // One-time migration: tab-scoped sessions written by older builds.
  const fromSession = readAuthPayload(sessionStorage);
  if (isCompleteAuthPayload(fromSession)) {
    writeAuthPayload(localStorage, fromSession);
    removeAuthKeys(sessionStorage);
    return toStoredSession(fromSession);
  }

  clearStoredAuthSession();
  return null;
}

export function getStoredAccessToken(): string | null {
  // Prefer localStorage; fall back to legacy sessionStorage for partial test seeds / migration.
  return (
    localStorage.getItem(AUTH_STORAGE_KEYS.accessToken) ??
    sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)
  );
}

export function getStoredRefreshToken(): string | null {
  return (
    localStorage.getItem(AUTH_STORAGE_KEYS.refreshToken) ??
    sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)
  );
}

export function extractRolesFromAccessToken(accessToken: string): SystemRoleName[] {
  const payload = parseJwtPayload(accessToken);
  if (!isTokenPayloadWithRoles(payload)) {
    return [];
  }

  return payload.roles.filter(isSystemRoleName);
}

export function clearAuthSession() {
  clearStoredAuthSession();
  notifyAuthSessionChanged();
}

function clearStoredAuthSession() {
  removeAuthKeys(localStorage);
  removeAuthKeys(sessionStorage);
}

export function notifyAuthSessionChanged() {
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
}

type AuthPayload = {
  accessToken: string | null;
  refreshToken: string | null;
  currentUserJson: string | null;
};

function readAuthPayload(storage: Storage): AuthPayload {
  return {
    accessToken: storage.getItem(AUTH_STORAGE_KEYS.accessToken),
    refreshToken: storage.getItem(AUTH_STORAGE_KEYS.refreshToken),
    currentUserJson: storage.getItem(AUTH_STORAGE_KEYS.currentUser),
  };
}

function writeAuthPayload(
  storage: Storage,
  payload: { accessToken: string; refreshToken: string; currentUserJson: string },
) {
  storage.setItem(AUTH_STORAGE_KEYS.accessToken, payload.accessToken);
  storage.setItem(AUTH_STORAGE_KEYS.refreshToken, payload.refreshToken);
  storage.setItem(AUTH_STORAGE_KEYS.currentUser, payload.currentUserJson);
}

function removeAuthKeys(storage: Storage) {
  storage.removeItem(AUTH_STORAGE_KEYS.accessToken);
  storage.removeItem(AUTH_STORAGE_KEYS.refreshToken);
  storage.removeItem(AUTH_STORAGE_KEYS.currentUser);
}

function isCompleteAuthPayload(
  payload: AuthPayload,
): payload is { accessToken: string; refreshToken: string; currentUserJson: string } {
  return (
    typeof payload.accessToken === "string" &&
    payload.accessToken.length > 0 &&
    typeof payload.refreshToken === "string" &&
    payload.refreshToken.length > 0 &&
    typeof payload.currentUserJson === "string" &&
    payload.currentUserJson.length > 0 &&
    parseStoredUser(payload.currentUserJson) != null
  );
}

function toStoredSession(payload: {
  accessToken: string;
  refreshToken: string;
  currentUserJson: string;
}): StoredAuthSession {
  const user = parseStoredUser(payload.currentUserJson);
  if (user == null) {
    clearStoredAuthSession();
    throw new Error("Auth session user payload was invalid after completeness check");
  }

  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    user,
    roles: extractRolesFromAccessToken(payload.accessToken),
  };
}

function parseStoredUser(value: string | null): AuthenticatedUser | null {
  if (value == null) {
    return null;
  }

  try {
    const parsed = JSON.parse(value) as unknown;
    return isStoredUser(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function isStoredUser(value: unknown): value is AuthenticatedUser {
  if (typeof value !== "object" || value == null) {
    return false;
  }

  const user = value as Partial<AuthenticatedUser>;

  return (
    typeof user.id === "string" &&
    typeof user.email === "string" &&
    typeof user.fullName === "string" &&
    (user.status === "ACTIVE" || user.status === "DISABLED" || user.status === "LOCKED") &&
    (typeof user.lastLoginAt === "string" || user.lastLoginAt === null)
  );
}

function parseJwtPayload(accessToken: string): unknown {
  const payloadPart = accessToken.split(".")[1];
  if (payloadPart == null) {
    return null;
  }

  try {
    return JSON.parse(decodeBase64Url(payloadPart)) as unknown;
  } catch {
    return null;
  }
}

function decodeBase64Url(value: string): string {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");

  return atob(padded);
}

function isTokenPayloadWithRoles(value: unknown): value is { roles: unknown[] } {
  return (
    typeof value === "object" && value != null && "roles" in value && Array.isArray(value.roles)
  );
}

function isSystemRoleName(value: unknown): value is SystemRoleName {
  return (
    value === "ADMIN" ||
    value === "CAMPAIGN_MANAGER" ||
    value === "BI_ANALYST" ||
    value === "PRODUCT_MANAGER" ||
    value === "COMPLIANCE_OFFICER" ||
    value === "CUSTOMER_SERVICE_AGENT" ||
    value === "SALES_AGENT" ||
    value === "MARKETING_ANALYST" ||
    value === "EXECUTIVE_VIEWER" ||
    value === "SYSTEM_AUDITOR"
  );
}
