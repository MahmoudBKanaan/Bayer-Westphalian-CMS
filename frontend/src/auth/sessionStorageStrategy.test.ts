import { afterEach, describe, expect, it } from "vitest";
import type { AuthenticatedSession } from "@/api/auth";
import {
  AUTH_STORAGE_KEYS,
  clearAuthSession,
  extractRolesFromAccessToken,
  loadAuthSession,
  saveAuthSession,
} from "@/auth/sessionStorageStrategy";

const accessToken = createAccessToken(["ADMIN", "CAMPAIGN_MANAGER"]);

const session: AuthenticatedSession = {
  user: {
    id: "10000000-0000-0000-0000-000000009901",
    email: "admin@bayer-westphalian.test",
    fullName: "Admin User",
    status: "ACTIVE",
    lastLoginAt: "2026-07-04T12:00:00Z",
  },
  tokens: {
    accessToken,
    accessTokenExpiresAt: "2026-07-04T12:15:00Z",
    refreshToken: "refresh-token",
    refreshTokenExpiresAt: "2026-07-11T12:00:00Z",
  },
};

describe("sessionStorage auth strategy", () => {
  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it("stores and loads the authenticated session from session storage", () => {
    saveAuthSession(session);

    expect(loadAuthSession()).toEqual({
      accessToken,
      refreshToken: "refresh-token",
      roles: ["ADMIN", "CAMPAIGN_MANAGER"],
      user: session.user,
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBe(
      JSON.stringify(session.user),
    );
  });

  it("does not persist auth tokens in local storage", () => {
    saveAuthSession(session);

    expect(localStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(localStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(localStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("extracts known role names from the stored access token", () => {
    expect(
      extractRolesFromAccessToken(createAccessToken(["ADMIN", "UNKNOWN", "BI_ANALYST"])),
    ).toEqual(["ADMIN", "BI_ANALYST"]);
  });

  it("returns no roles for malformed access tokens", () => {
    expect(extractRolesFromAccessToken("not-a-jwt")).toEqual([]);
  });

  it("clears stored session values on sign out", () => {
    saveAuthSession(session);

    clearAuthSession();

    expect(loadAuthSession()).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("clears incomplete stored sessions", () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(session.user));

    expect(loadAuthSession()).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("clears invalid stored user payloads", () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, "not-json");

    expect(loadAuthSession()).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("clears non-object stored user payloads", () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, "null");

    expect(loadAuthSession()).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });
});

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
