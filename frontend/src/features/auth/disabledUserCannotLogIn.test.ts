import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE,
  ACTIVE_USER_STATUS,
  AUTHENTICATION_DESIGN_DOC_PATH,
  BACKEND_CRITICAL_TEST_CLASS,
  COMPANION_AUTH_CONTROLLER_TEST_CLASS,
  COMPANION_AUTH_SERVICE_TEST_CLASS,
  DISABLED_LOGIN_UI_MESSAGE,
  DISABLED_USER_CANNOT_LOG_IN_FR,
  DISABLED_USER_CANNOT_LOG_IN_ITEM,
  DISABLED_USER_CANNOT_LOG_IN_NFR,
  DISABLED_USER_CANNOT_LOG_IN_STATEMENT,
  DISABLED_USER_STATUS,
  LOCKED_USER_STATUS,
  USER_MANAGEMENT_GUIDE_DOC_PATH,
  canLogInWithStatus,
  isLoginBlockedByAccountStatus,
  loginDeniedReasonForStatus,
} from "@/features/auth/disabledUserCannotLogIn";
import { LOGIN_INVALID_CREDENTIALS_MESSAGE } from "@/features/auth/loginFlow";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("disabledUserCannotLogIn (item 659)", () => {
  it("locks the critical KB rule identity", () => {
    expect(DISABLED_USER_CANNOT_LOG_IN_ITEM).toBe(659);
    expect(DISABLED_USER_CANNOT_LOG_IN_STATEMENT).toBe("Disabled user cannot log in");
    expect(DISABLED_USER_CANNOT_LOG_IN_FR).toEqual(["FR-001", "FR-005"]);
    expect(DISABLED_USER_CANNOT_LOG_IN_NFR).toEqual(["NFR-001"]);
    expect(ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE).toBe("User account is not active");
    expect(DISABLED_LOGIN_UI_MESSAGE).toBe(LOGIN_INVALID_CREDENTIALS_MESSAGE);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("DisabledUserCannotLogInTests");
    expect(COMPANION_AUTH_SERVICE_TEST_CLASS).toContain("AuthServiceTests");
    expect(COMPANION_AUTH_CONTROLLER_TEST_CLASS).toContain("AuthControllerTests");
  });

  it("allows only ACTIVE accounts to log in", () => {
    expect(canLogInWithStatus(ACTIVE_USER_STATUS)).toBe(true);
    expect(canLogInWithStatus(DISABLED_USER_STATUS)).toBe(false);
    expect(canLogInWithStatus(LOCKED_USER_STATUS)).toBe(false);
    expect(canLogInWithStatus(null)).toBe(false);

    expect(isLoginBlockedByAccountStatus(DISABLED_USER_STATUS)).toBe(true);
    expect(isLoginBlockedByAccountStatus(LOCKED_USER_STATUS)).toBe(true);
    expect(isLoginBlockedByAccountStatus(ACTIVE_USER_STATUS)).toBe(false);

    expect(loginDeniedReasonForStatus(DISABLED_USER_STATUS)).toBe(
      ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE,
    );
    expect(loginDeniedReasonForStatus(LOCKED_USER_STATUS)).toBe(
      ACCOUNT_NOT_ACTIVE_BACKEND_MESSAGE,
    );
    expect(loginDeniedReasonForStatus(ACTIVE_USER_STATUS)).toBeNull();
  });

  it("documents disabled login rejection in authentication and user-management docs", () => {
    const authDocPath = path.join(repoRoot, AUTHENTICATION_DESIGN_DOC_PATH);
    const userDocPath = path.join(repoRoot, USER_MANAGEMENT_GUIDE_DOC_PATH);
    expect(existsSync(authDocPath)).toBe(true);
    expect(existsSync(userDocPath)).toBe(true);

    const authDoc = readRepoFile(AUTHENTICATION_DESIGN_DOC_PATH);
    expect(authDoc).toContain("659");
    expect(authDoc).toContain("DisabledUserCannotLogInTests");
    expect(authDoc).toMatch(/Disabled or locked users/i);

    const userDoc = readRepoFile(USER_MANAGEMENT_GUIDE_DOC_PATH);
    expect(userDoc).toMatch(/Disabled or locked users cannot log in/i);
    expect(userDoc).toMatch(/DISABLED/);
  });
});
