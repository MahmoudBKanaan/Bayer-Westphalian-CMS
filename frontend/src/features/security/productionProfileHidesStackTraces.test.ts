import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  COMPANION_STACK_TRACE_TEST_CLASS,
  FORBIDDEN_CLIENT_ERROR_KEYS,
  PRODUCTION_ERROR_YAML_SETTINGS,
  PRODUCTION_PROFILE_HIDES_STACK_TRACES_ITEM,
  PRODUCTION_PROFILE_HIDES_STACK_TRACES_NFR,
  PRODUCTION_PROFILE_HIDES_STACK_TRACES_STATEMENT,
  PRODUCTION_SECURITY_CHECKLIST_DOC_PATH,
  PRODUCTION_SPRING_PROFILE,
  SAFE_INTERNAL_ERROR_CODE,
  SAFE_INTERNAL_ERROR_MESSAGE,
  SECURITY_HARDENING_DOC_PATH,
  clientErrorBodyHidesStackTraces,
  isForbiddenClientErrorKey,
  productionErrorYamlHidesStackTraces,
} from "@/features/security/productionProfileHidesStackTraces";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("productionProfileHidesStackTraces (item 664)", () => {
  it("locks the critical KB rule identity", () => {
    expect(PRODUCTION_PROFILE_HIDES_STACK_TRACES_ITEM).toBe(664);
    expect(PRODUCTION_PROFILE_HIDES_STACK_TRACES_STATEMENT).toBe(
      "Production profile hides stack traces",
    );
    expect(PRODUCTION_PROFILE_HIDES_STACK_TRACES_NFR).toEqual(["NFR-001", "NFR-014"]);
    expect(PRODUCTION_SPRING_PROFILE).toBe("prod");
    expect(PRODUCTION_ERROR_YAML_SETTINGS.includeStacktrace).toBe("never");
    expect(PRODUCTION_ERROR_YAML_SETTINGS.includeException).toBe(false);
    expect(FORBIDDEN_CLIENT_ERROR_KEYS).toContain("trace");
    expect(FORBIDDEN_CLIENT_ERROR_KEYS).toContain("stackTrace");
    expect(SAFE_INTERNAL_ERROR_CODE).toBe("INTERNAL_ERROR");
    expect(SAFE_INTERNAL_ERROR_MESSAGE).toBe("Unexpected server error");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("ProductionProfileHidesStackTracesTests");
    expect(COMPANION_STACK_TRACE_TEST_CLASS).toContain("ProductionStackTraceHiddenTests");
  });

  it("rejects client error bodies that leak stack traces", () => {
    expect(isForbiddenClientErrorKey("trace")).toBe(true);
    expect(isForbiddenClientErrorKey("status")).toBe(false);

    expect(
      clientErrorBodyHidesStackTraces({
        status: 500,
        code: "INTERNAL_ERROR",
        message: "Unexpected server error",
        details: [],
      }),
    ).toBe(true);

    expect(
      clientErrorBodyHidesStackTraces({
        status: 500,
        code: "INTERNAL_ERROR",
        message: "Unexpected server error",
        trace: "java.lang.NullPointerException",
      }),
    ).toBe(false);

    expect(
      clientErrorBodyHidesStackTraces({
        status: 500,
        code: "INTERNAL_ERROR",
        message: "NullPointerException at com.example.Foo",
      }),
    ).toBe(false);

    expect(
      productionErrorYamlHidesStackTraces({
        includeStacktrace: "never",
        includeMessage: "never",
        includeBindingErrors: "never",
        includeException: false,
      }),
    ).toBe(true);
    expect(
      productionErrorYamlHidesStackTraces({
        includeStacktrace: "always",
        includeMessage: "never",
        includeBindingErrors: "never",
        includeException: false,
      }),
    ).toBe(false);
  });

  it("documents production stack-trace hiding in hardening and checklist docs", () => {
    const hardeningPath = path.join(repoRoot, SECURITY_HARDENING_DOC_PATH);
    const checklistPath = path.join(repoRoot, PRODUCTION_SECURITY_CHECKLIST_DOC_PATH);
    expect(existsSync(hardeningPath)).toBe(true);
    expect(existsSync(checklistPath)).toBe(true);

    const hardening = readRepoFile(SECURITY_HARDENING_DOC_PATH);
    expect(hardening).toContain("664");
    expect(hardening).toContain("ProductionProfileHidesStackTracesTests");
    expect(hardening).toMatch(/include-stacktrace:\s*never/i);

    const checklist = readRepoFile(PRODUCTION_SECURITY_CHECKLIST_DOC_PATH);
    expect(checklist).toMatch(/include-stacktrace:\s*never/i);
    expect(checklist).toMatch(/stack/i);
  });
});
