import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  COMPANION_SECRET_PRESENCE_TEST_CLASS,
  FORBIDDEN_SECRET_PLACEHOLDERS,
  MIN_DB_PASSWORD_LENGTH,
  MIN_JWT_SECRET_LENGTH,
  MISSING_SECRETS_ARE_DETECTED_ITEM,
  MISSING_SECRETS_ARE_DETECTED_NFR,
  MISSING_SECRETS_ARE_DETECTED_STATEMENT,
  PRODUCTION_SECURITY_CHECKLIST_DOC_PATH,
  PRODUCTION_SPRING_PROFILE,
  REQUIRED_PRODUCTION_SECRETS,
  SECURITY_HARDENING_DOC_PATH,
  checkRequiredSecretPresence,
  configurationErrorDoesNotLeakSecretValues,
  isForbiddenSecretPlaceholder,
  looksLikeUnresolvedPlaceholder,
  safeMissingSecretErrorMessage,
} from "@/features/security/missingSecretsAreDetected";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("missingSecretsAreDetected (item 665)", () => {
  it("locks the critical KB rule identity", () => {
    expect(MISSING_SECRETS_ARE_DETECTED_ITEM).toBe(665);
    expect(MISSING_SECRETS_ARE_DETECTED_STATEMENT).toBe("Missing secrets are detected");
    expect(MISSING_SECRETS_ARE_DETECTED_NFR).toEqual(["NFR-001"]);
    expect(PRODUCTION_SPRING_PROFILE).toBe("prod");
    expect(REQUIRED_PRODUCTION_SECRETS).toEqual(["JWT_SECRET", "DB_PASSWORD"]);
    expect(MIN_JWT_SECRET_LENGTH).toBe(32);
    expect(MIN_DB_PASSWORD_LENGTH).toBe(8);
    expect(FORBIDDEN_SECRET_PLACEHOLDERS).toContain("dev-only-change-me");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("MissingSecretsAreDetectedTests");
    expect(COMPANION_SECRET_PRESENCE_TEST_CLASS).toContain("SecretPresenceValidatorTests");
  });

  it("detects missing, placeholder, short, and unresolved secrets without echoing values", () => {
    expect(isForbiddenSecretPlaceholder("changeme")).toBe(true);
    expect(isForbiddenSecretPlaceholder("production-jwt-secret-32chars-min!!")).toBe(false);
    expect(looksLikeUnresolvedPlaceholder("${JWT_SECRET}")).toBe(true);
    expect(looksLikeUnresolvedPlaceholder("real-value")).toBe(false);

    expect(
      checkRequiredSecretPresence({
        secretName: "JWT_SECRET",
        value: "",
        minLength: MIN_JWT_SECRET_LENGTH,
      }),
    ).toEqual({ ok: false, secretName: "JWT_SECRET", reason: "missing" });

    expect(
      checkRequiredSecretPresence({
        secretName: "JWT_SECRET",
        value: "dev-only-change-me",
        minLength: MIN_JWT_SECRET_LENGTH,
      }),
    ).toEqual({ ok: false, secretName: "JWT_SECRET", reason: "placeholder" });

    expect(
      checkRequiredSecretPresence({
        secretName: "JWT_SECRET",
        value: "sixteen-char-sec!",
        minLength: MIN_JWT_SECRET_LENGTH,
      }),
    ).toEqual({ ok: false, secretName: "JWT_SECRET", reason: "too-short" });

    expect(
      checkRequiredSecretPresence({
        secretName: "JWT_SECRET",
        value: "production-jwt-secret-32chars-min!!",
        minLength: MIN_JWT_SECRET_LENGTH,
      }),
    ).toEqual({ ok: true });

    expect(safeMissingSecretErrorMessage("JWT_SECRET")).toBe("JWT_SECRET is required");
    expect(
      configurationErrorDoesNotLeakSecretValues(
        "Production secret presence validation failed: JWT_SECRET is required",
        ["super-secret-value-should-not-appear"],
      ),
    ).toBe(true);
    expect(
      configurationErrorDoesNotLeakSecretValues(
        "Production secret presence validation failed: super-secret-value-should-not-appear",
        ["super-secret-value-should-not-appear"],
      ),
    ).toBe(false);
  });

  it("documents secret presence validation in hardening and checklist docs", () => {
    const hardeningPath = path.join(repoRoot, SECURITY_HARDENING_DOC_PATH);
    const checklistPath = path.join(repoRoot, PRODUCTION_SECURITY_CHECKLIST_DOC_PATH);
    expect(existsSync(hardeningPath)).toBe(true);
    expect(existsSync(checklistPath)).toBe(true);

    const hardening = readRepoFile(SECURITY_HARDENING_DOC_PATH);
    expect(hardening).toContain("665");
    expect(hardening).toContain("MissingSecretsAreDetectedTests");
    expect(hardening).toContain("SecretPresenceValidator");
    expect(hardening).toMatch(/JWT_SECRET/);

    const checklist = readRepoFile(PRODUCTION_SECURITY_CHECKLIST_DOC_PATH);
    expect(checklist).toContain("SecretPresenceValidator");
    expect(checklist).toMatch(/JWT_SECRET|DB_PASSWORD/);
  });
});
