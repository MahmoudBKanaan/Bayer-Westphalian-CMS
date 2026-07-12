import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_DOCUMENTATION_TEST_CLASS,
  BACKEND_ENV_EXAMPLE_PATH,
  BACKEND_ENV_TEMPLATE_REQUIRED_KEYS,
  ENVIRONMENT_VARIABLE_DOC_REQUIRED_MARKERS,
  ENVIRONMENT_VARIABLE_DOCUMENTATION_ITEM,
  ENVIRONMENT_VARIABLE_DOCUMENTATION_STATEMENT,
  ENVIRONMENT_VARIABLE_DOCUMENTATION_TEST_CLASS,
  ENVIRONMENT_VARIABLES_DOC_PATH,
  ENVIRONMENT_VARIABLE_TEMPLATE_ITEM,
  ENVIRONMENT_VARIABLE_TEMPLATE_STATEMENT,
  FRONTEND_ENV_EXAMPLE_PATH,
  FRONTEND_ENV_TEMPLATE_REQUIRED_KEYS,
  ROOT_ENV_EXAMPLE_PATH,
  ROOT_ENV_TEMPLATE_REQUIRED_KEYS,
  envTemplateDefinesKeys,
  envTemplateLooksLikeSafeExample,
  environmentVariableDocDefinesRequiredMarkers,
} from "@/features/ops/environmentVariableTemplate";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("environmentVariableTemplate (items 688, 709)", () => {
  it("locks the environment variable template identity", () => {
    expect(ENVIRONMENT_VARIABLE_TEMPLATE_ITEM).toBe(688);
    expect(ENVIRONMENT_VARIABLE_TEMPLATE_STATEMENT).toBe("Add environment variable template");
    expect(ROOT_ENV_EXAMPLE_PATH).toBe(".env.example");
    expect(BACKEND_ENV_EXAMPLE_PATH).toBe("backend/.env.example");
    expect(FRONTEND_ENV_EXAMPLE_PATH).toBe("frontend/.env.example");
    expect(ENVIRONMENT_VARIABLES_DOC_PATH).toBe("docs/deployment/environment-variables.md");
    expect(BACKEND_DOCUMENTATION_TEST_CLASS).toContain(
      "EnvironmentVariableTemplateDocumentationTests",
    );
    expect(ENVIRONMENT_VARIABLE_DOCUMENTATION_ITEM).toBe(709);
    expect(ENVIRONMENT_VARIABLE_DOCUMENTATION_STATEMENT).toBe(
      "Environment variable documentation",
    );
    expect(ENVIRONMENT_VARIABLE_DOCUMENTATION_TEST_CLASS).toContain(
      "EnvironmentVariableDocumentationTests",
    );
  });

  it("requires root, backend, and frontend env templates with required keys", () => {
    expect(existsSync(path.join(repoRoot, ROOT_ENV_EXAMPLE_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, BACKEND_ENV_EXAMPLE_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, FRONTEND_ENV_EXAMPLE_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, ENVIRONMENT_VARIABLES_DOC_PATH))).toBe(true);

    const root = readRepoFile(ROOT_ENV_EXAMPLE_PATH);
    const backend = readRepoFile(BACKEND_ENV_EXAMPLE_PATH);
    const frontend = readRepoFile(FRONTEND_ENV_EXAMPLE_PATH);
    const doc = readRepoFile(ENVIRONMENT_VARIABLES_DOC_PATH);

    expect(root).toContain("688");
    expect(backend).toContain("688");
    expect(frontend).toContain("688");
    expect(envTemplateDefinesKeys(root, ROOT_ENV_TEMPLATE_REQUIRED_KEYS)).toBe(true);
    expect(envTemplateDefinesKeys(backend, BACKEND_ENV_TEMPLATE_REQUIRED_KEYS)).toBe(true);
    expect(envTemplateDefinesKeys(frontend, FRONTEND_ENV_TEMPLATE_REQUIRED_KEYS)).toBe(true);
    expect(envTemplateLooksLikeSafeExample(root)).toBe(true);
    expect(envTemplateLooksLikeSafeExample(backend)).toBe(true);

    expect(doc).toContain("688");
    expect(doc).toContain("709");
    expect(doc).toContain("EnvironmentVariableTemplateDocumentationTests");
    expect(doc).toContain("EnvironmentVariableDocumentationTests");
    expect(doc).toContain("JWT_SECRET");
    expect(doc).toContain("VITE_API_BASE_URL");
    expect(doc).toContain(".env.example");
    expect(environmentVariableDocDefinesRequiredMarkers(doc)).toBe(true);
    expect(ENVIRONMENT_VARIABLE_DOC_REQUIRED_MARKERS).toContain("Required production variables");
  });

  it("rejects empty or PEM-like content as unsafe templates", () => {
    expect(envTemplateDefinesKeys("", ["DB_URL"])).toBe(false);
    expect(envTemplateLooksLikeSafeExample("-----BEGIN PRIVATE KEY-----\nabc")).toBe(false);
    expect(envTemplateDefinesKeys("DB_URL=jdbc:x\n", ["DB_URL"])).toBe(true);
  });
});
