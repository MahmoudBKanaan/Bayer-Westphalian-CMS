import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_DOCUMENTATION_TEST_CLASS,
  DOCUMENTED_SECRET_ENV_NAMES,
  SECRETS_DOCUMENTATION_ITEM,
  SECRETS_DOCUMENTATION_EXPANSION_ITEM,
  SECRETS_DOCUMENTATION_EXPANSION_REQUIRED_MARKERS,
  SECRETS_DOCUMENTATION_EXPANSION_STATEMENT,
  SECRETS_DOCUMENTATION_EXPANSION_TEST_CLASS,
  SECRETS_DOCUMENTATION_STATEMENT,
  SECRETS_DOC_PATH,
  SECRETS_DOC_REQUIRED_MARKERS,
  secretsDocDefinesExpansionMarkers,
  secretsDocDefinesRequiredMarkers,
  secretsDocLooksSafe,
  secretsDocNamesAllSecrets,
} from "@/features/ops/secretsDocumentation";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("secretsDocumentation (items 689, 710)", () => {
  it("locks the secrets documentation identity", () => {
    expect(SECRETS_DOCUMENTATION_ITEM).toBe(689);
    expect(SECRETS_DOCUMENTATION_STATEMENT).toBe("Add secrets documentation");
    expect(SECRETS_DOC_PATH).toBe("docs/deployment/secrets.md");
    expect(BACKEND_DOCUMENTATION_TEST_CLASS).toContain("SecretsDocumentationTests");
    expect(SECRETS_DOCUMENTATION_EXPANSION_ITEM).toBe(710);
    expect(SECRETS_DOCUMENTATION_EXPANSION_STATEMENT).toBe("Secrets documentation");
    expect(SECRETS_DOCUMENTATION_EXPANSION_TEST_CLASS).toContain(
      "SecretsDocumentationExpansionTests",
    );
    expect(DOCUMENTED_SECRET_ENV_NAMES).toContain("JWT_SECRET");
    expect(SECRETS_DOC_REQUIRED_MARKERS).toContain("SecretPresenceValidator");
    expect(SECRETS_DOCUMENTATION_EXPANSION_REQUIRED_MARKERS).toContain(
      "Leak response runbook",
    );
  });

  it("requires secrets.md and cross-links without embedding secret material", () => {
    expect(existsSync(path.join(repoRoot, SECRETS_DOC_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, "docs/deployment/environment-variables.md"))).toBe(
      true,
    );
    expect(existsSync(path.join(repoRoot, ".gitignore"))).toBe(true);

    const doc = readRepoFile(SECRETS_DOC_PATH);
    expect(secretsDocDefinesRequiredMarkers(doc)).toBe(true);
    expect(secretsDocDefinesExpansionMarkers(doc)).toBe(true);
    expect(secretsDocNamesAllSecrets(doc)).toBe(true);
    expect(secretsDocLooksSafe(doc)).toBe(true);

    const envDoc = readRepoFile("docs/deployment/environment-variables.md");
    expect(envDoc).toContain("689");

    const index = readRepoFile("docs/README.md");
    expect(index).toContain("deployment/secrets.md");

    const gitignore = readRepoFile(".gitignore");
    expect(gitignore).toContain(".env");
    expect(gitignore).toContain("!.env.example");
    expect(gitignore).toContain("secrets/");

    const workflow = readRepoFile(".github/workflows/ci.yml");
    expect(workflow).not.toMatch(/JWT_SECRET:\s*['"]?[^$\s\n]+/);
    expect(workflow).not.toContain("DB_PASSWORD:");
  });

  it("rejects empty or PEM-like secrets docs", () => {
    expect(secretsDocDefinesRequiredMarkers("")).toBe(false);
    expect(secretsDocLooksSafe("-----BEGIN PRIVATE KEY-----\nabc")).toBe(false);
    expect(secretsDocNamesAllSecrets("JWT_SECRET only", ["JWT_SECRET"])).toBe(true);
  });
});
