import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  CI_CD_DOC_PATH,
  CI_JOB_IDS,
  CI_TRIGGER_BRANCHES,
  CI_WORKFLOW_REQUIRED_MARKERS,
  GITHUB_ACTIONS_WORKFLOW_ITEM,
  GITHUB_ACTIONS_WORKFLOW_NAME,
  GITHUB_ACTIONS_WORKFLOW_PATH,
  GITHUB_ACTIONS_WORKFLOW_STATEMENT,
  PRODUCTION_CONFIG_VALIDATION_JOB,
  PRODUCTION_CONFIG_VALIDATION_JOB_DOCUMENTATION_TEST_CLASS,
  PRODUCTION_CONFIG_VALIDATION_JOB_ITEM,
  PRODUCTION_CONFIG_VALIDATION_JOB_REQUIRED_MARKERS,
  PRODUCTION_CONFIG_VALIDATION_JOB_STATEMENT,
  CI_BADGE,
  CI_BADGE_DOCUMENTATION_TEST_CLASS,
  CI_BADGE_ITEM,
  CI_BADGE_REQUIRED_MARKERS,
  CI_BADGE_STATEMENT,
  PIPELINE_FAILS_WHEN_TESTS_FAIL,
  PIPELINE_FAILS_WHEN_TESTS_FAIL_DOCUMENTATION_TEST_CLASS,
  PIPELINE_FAILS_WHEN_TESTS_FAIL_ITEM,
  PIPELINE_FAILS_WHEN_TESTS_FAIL_REQUIRED_MARKERS,
  PIPELINE_FAILS_WHEN_TESTS_FAIL_STATEMENT,
  PIPELINE_PASSES_ON_CLEAN_MAIN,
  PIPELINE_PASSES_ON_CLEAN_MAIN_DOCUMENTATION_TEST_CLASS,
  PIPELINE_PASSES_ON_CLEAN_MAIN_ITEM,
  PIPELINE_PASSES_ON_CLEAN_MAIN_REQUIRED_MARKERS,
  PIPELINE_PASSES_ON_CLEAN_MAIN_STATEMENT,
  RELEASE_ARTIFACT_GENERATION,
  RELEASE_ARTIFACT_GENERATION_DOCUMENTATION_TEST_CLASS,
  RELEASE_ARTIFACT_GENERATION_ITEM,
  RELEASE_ARTIFACT_GENERATION_REQUIRED_MARKERS,
  RELEASE_ARTIFACT_GENERATION_STATEMENT,
  ROOT_README_PATH,
  isCiTriggerBranch,
  readmeDefinesCiBadge,
  relatedBacklogBandForJob,
  workflowYamlDefinesCiPipeline,
  workflowYamlDefinesDockerComposeValidationJob,
  workflowYamlDefinesPipelineFailsWhenTestsFail,
  workflowYamlDefinesPipelinePassesOnCleanMain,
  workflowYamlDefinesProductionConfigValidationJob,
  workflowYamlDefinesReleaseArtifactGeneration,
} from "@/features/ops/githubActionsWorkflow";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("githubActionsWorkflow (items 677–687, 690–694)", () => {
  it("locks the GitHub Actions workflow identity including release artifacts", () => {
    expect(GITHUB_ACTIONS_WORKFLOW_ITEM).toBe(677);
    expect(GITHUB_ACTIONS_WORKFLOW_STATEMENT).toBe("Add GitHub Actions workflow");
    expect(GITHUB_ACTIONS_WORKFLOW_PATH).toBe(".github/workflows/ci.yml");
    expect(GITHUB_ACTIONS_WORKFLOW_NAME).toBe("CI");
    expect(CI_TRIGGER_BRANCHES).toEqual(["main", "dev"]);
    expect(CI_JOB_IDS).toContain("production-config-validate");
    expect(CI_JOB_IDS).toContain("docker-compose-validate");
    expect(CI_JOB_IDS).toContain("backend-build");
    expect(CI_JOB_IDS).toContain("frontend-build");
    expect(CI_WORKFLOW_REQUIRED_MARKERS).toContain("production-config-validate:");
    expect(CI_WORKFLOW_REQUIRED_MARKERS).toContain("actions/upload-artifact@v4");
    expect(CI_WORKFLOW_REQUIRED_MARKERS).toContain("name: bwc-backend-jar");
    expect(CI_WORKFLOW_REQUIRED_MARKERS).toContain("name: bwc-frontend-dist");
  });

  it("locks the production config validation job identity (690)", () => {
    expect(PRODUCTION_CONFIG_VALIDATION_JOB_ITEM).toBe(690);
    expect(PRODUCTION_CONFIG_VALIDATION_JOB_STATEMENT).toBe(
      "Add production config validation step",
    );
    expect(PRODUCTION_CONFIG_VALIDATION_JOB.id).toBe("production-config-validate");
    expect(PRODUCTION_CONFIG_VALIDATION_JOB.name).toBe("Production config validation");
    expect(PRODUCTION_CONFIG_VALIDATION_JOB.prodYamlPath).toBe(
      "backend/src/main/resources/application-prod.yml",
    );
    expect(PRODUCTION_CONFIG_VALIDATION_JOB.startsApplication).toBe(false);
    expect(PRODUCTION_CONFIG_VALIDATION_JOB.usesRealSecrets).toBe(false);
    expect(PRODUCTION_CONFIG_VALIDATION_JOB_DOCUMENTATION_TEST_CLASS).toContain(
      "ProductionConfigValidationStepDocumentationTests",
    );
    expect(PRODUCTION_CONFIG_VALIDATION_JOB_REQUIRED_MARKERS).toContain(
      "MIN_JWT_SECRET_LENGTH = 32",
    );
    expect(relatedBacklogBandForJob("docker-compose-validate")).toBe("687");
    expect(relatedBacklogBandForJob("production-config-validate")).toBe("690");
  });

  it("locks release artifact generation identity (691)", () => {
    expect(RELEASE_ARTIFACT_GENERATION_ITEM).toBe(691);
    expect(RELEASE_ARTIFACT_GENERATION_STATEMENT).toBe("Add release artifact generation");
    expect(RELEASE_ARTIFACT_GENERATION.action).toBe("actions/upload-artifact@v4");
    expect(RELEASE_ARTIFACT_GENERATION.backendArtifactName).toBe("bwc-backend-jar");
    expect(RELEASE_ARTIFACT_GENERATION.frontendArtifactName).toBe("bwc-frontend-dist");
    expect(RELEASE_ARTIFACT_GENERATION.backendPath).toBe("backend/target/*.jar");
    expect(RELEASE_ARTIFACT_GENERATION.frontendPath).toBe("frontend/dist");
    expect(RELEASE_ARTIFACT_GENERATION.retentionDays).toBe(14);
    expect(RELEASE_ARTIFACT_GENERATION.ifNoFilesFound).toBe("error");
    expect(RELEASE_ARTIFACT_GENERATION.attachesToJobs).toEqual([
      "backend-build",
      "frontend-build",
    ]);
    expect(RELEASE_ARTIFACT_GENERATION_DOCUMENTATION_TEST_CLASS).toContain(
      "ReleaseArtifactGenerationDocumentationTests",
    );
    expect(RELEASE_ARTIFACT_GENERATION_REQUIRED_MARKERS).toContain(
      "actions/upload-artifact@v4",
    );
    expect(relatedBacklogBandForJob("backend-build")).toBe("678+691");
    expect(relatedBacklogBandForJob("frontend-build")).toBe("684+691");
  });

  it("locks the CI badge identity (692)", () => {
    expect(CI_BADGE_ITEM).toBe(692);
    expect(CI_BADGE_STATEMENT).toBe("Add CI badge to README");
    expect(CI_BADGE.altText).toBe("CI");
    expect(CI_BADGE.workflowFile).toBe("ci.yml");
    expect(CI_BADGE.workflowName).toBe("CI");
    expect(CI_BADGE.branch).toBe("main");
    expect(CI_BADGE.repository).toBe("MahmoudBKanaan/Bayer-Westphalian-CMS");
    expect(CI_BADGE.readmePath).toBe(ROOT_README_PATH);
    expect(CI_BADGE.imageUrl).toContain("badge.svg?branch=main");
    expect(CI_BADGE.linkUrl).toContain("actions/workflows/ci.yml");
    expect(CI_BADGE_DOCUMENTATION_TEST_CLASS).toContain("CiBadgeDocumentationTests");
    expect(CI_BADGE_REQUIRED_MARKERS).toContain("branch=main");
  });

  it("locks fail-on-red pipeline verification (693)", () => {
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL_ITEM).toBe(693);
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL_STATEMENT).toBe(
      "Verify pipeline fails when tests fail",
    );
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL.qualityGateJobs).toEqual([
      "backend-test",
      "backend-integration-test",
      "frontend-lint",
      "frontend-test",
    ]);
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL.backendTestCommand).toBe("mvn -B test");
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL.frontendTestCommand).toBe("npm test");
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL.forbiddenSoftFail).toBe("continue-on-error: true");
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL.forbiddenShellSwallow).toBe("|| true");
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL_DOCUMENTATION_TEST_CLASS).toContain(
      "PipelineFailsWhenTestsFailDocumentationTests",
    );
    expect(PIPELINE_FAILS_WHEN_TESTS_FAIL_REQUIRED_MARKERS).toContain("fail-on-red");
  });

  it("locks pass-on-green clean main verification (694)", () => {
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN_ITEM).toBe(694);
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN_STATEMENT).toBe(
      "Verify pipeline passes on clean main branch",
    );
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.branch).toBe("main");
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.triggerEvents).toEqual(["push", "pull_request"]);
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.requiredJobs).toContain("backend-test");
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.requiredJobs).toContain(
      "production-config-validate",
    );
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.forbidsPathFilters).toBe(true);
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.forbidsJobIfSkips).toBe(true);
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN.badgeTracksMain).toBe(true);
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN_DOCUMENTATION_TEST_CLASS).toContain(
      "PipelinePassesOnCleanMainBranchDocumentationTests",
    );
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN_REQUIRED_MARKERS).toContain("pass-on-green");
    expect(PIPELINE_PASSES_ON_CLEAN_MAIN_REQUIRED_MARKERS).toContain("- main");
  });

  it("classifies CI trigger branches", () => {
    expect(isCiTriggerBranch("main")).toBe(true);
    expect(isCiTriggerBranch("dev")).toBe(true);
    expect(isCiTriggerBranch(null)).toBe(false);
  });

  it("requires the CI workflow contracts through pass-on-green (694)", () => {
    expect(existsSync(path.join(repoRoot, GITHUB_ACTIONS_WORKFLOW_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, CI_CD_DOC_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, ROOT_README_PATH))).toBe(true);
    expect(
      existsSync(
        path.join(repoRoot, "backend/src/main/resources/application-prod.yml"),
      ),
    ).toBe(true);

    const yaml = readRepoFile(GITHUB_ACTIONS_WORKFLOW_PATH);
    expect(yaml).toContain("690");
    expect(yaml).toContain("691");
    expect(yaml).toContain("693");
    expect(yaml).toContain("694");
    expect(workflowYamlDefinesCiPipeline(yaml)).toBe(true);
    expect(workflowYamlDefinesDockerComposeValidationJob(yaml)).toBe(true);
    expect(workflowYamlDefinesProductionConfigValidationJob(yaml)).toBe(true);
    expect(workflowYamlDefinesReleaseArtifactGeneration(yaml)).toBe(true);
    expect(workflowYamlDefinesPipelineFailsWhenTestsFail(yaml)).toBe(true);
    expect(workflowYamlDefinesPipelinePassesOnCleanMain(yaml)).toBe(true);

    const prod = readRepoFile("backend/src/main/resources/application-prod.yml");
    expect(prod).toContain("include-stacktrace: never");
    expect(prod).toContain("${JWT_SECRET}");
    expect(prod.toLowerCase()).not.toContain("localhost");

    const readme = readRepoFile(ROOT_README_PATH);
    expect(readme).toContain("692");
    expect(readmeDefinesCiBadge(readme)).toBe(true);
    expect(readme).toContain("badge.svg?branch=main");

    const doc = readRepoFile(CI_CD_DOC_PATH);
    expect(doc).toContain("690");
    expect(doc).toContain("691");
    expect(doc).toContain("692");
    expect(doc).toContain("693");
    expect(doc).toContain("694");
    expect(doc).toContain("production-config-validate");
    expect(doc).toContain("bwc-backend-jar");
    expect(doc).toContain("bwc-frontend-dist");
    expect(doc).toContain("ReleaseArtifactGenerationDocumentationTests");
    expect(doc).toContain("ProductionConfigValidationStepDocumentationTests");
    expect(doc).toContain("CiBadgeDocumentationTests");
    expect(doc).toContain("PipelineFailsWhenTestsFailDocumentationTests");
    expect(doc).toContain("PipelinePassesOnCleanMainBranchDocumentationTests");
  });
});
