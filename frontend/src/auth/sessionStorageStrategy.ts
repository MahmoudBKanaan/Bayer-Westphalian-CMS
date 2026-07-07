import type { AuthenticatedSession, AuthenticatedUser } from "@/api/auth";

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

export type StoredAuthSession = {
  accessToken: string;
  refreshToken: string;
  user: AuthenticatedUser;
  roles: SystemRoleName[];
};

export function saveAuthSession(session: AuthenticatedSession) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, session.tokens.accessToken);
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, session.tokens.refreshToken);
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(session.user));
}

export function loadAuthSession(): StoredAuthSession | null {
  const accessToken = sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken);
  const refreshToken = sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken);
  const user = parseStoredUser(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser));

  if (!accessToken || !refreshToken || user == null) {
    clearAuthSession();
    return null;
  }

  return { accessToken, refreshToken, user, roles: extractRolesFromAccessToken(accessToken) };
}

export function getStoredAccessToken(): string | null {
  return sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken);
}

export function extractRolesFromAccessToken(accessToken: string): SystemRoleName[] {
  const payload = parseJwtPayload(accessToken);
  if (!isTokenPayloadWithRoles(payload)) {
    return [];
  }

  return payload.roles.filter(isSystemRoleName);
}

export function clearAuthSession() {
  sessionStorage.removeItem(AUTH_STORAGE_KEYS.accessToken);
  sessionStorage.removeItem(AUTH_STORAGE_KEYS.refreshToken);
  sessionStorage.removeItem(AUTH_STORAGE_KEYS.currentUser);
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
