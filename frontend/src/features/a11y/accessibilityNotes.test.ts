import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ACCESSIBILITY_CONTRAST_TARGETS,
  ACCESSIBILITY_IMPLEMENTATION_MODULES,
  ACCESSIBILITY_KEYBOARD_KEYS,
  ACCESSIBILITY_LANDMARK_LABELS,
  ACCESSIBILITY_LIVE_REGION_ROLES,
  ACCESSIBILITY_NOTES_DOC_PATH,
  ACCESSIBILITY_NOTES_DOC_REQUIRED_SNIPPETS,
  ACCESSIBILITY_NOTES_RELATED_ITEMS,
  ACCESSIBILITY_NOTES_SECTIONS,
  ACCESSIBILITY_NOTES_TITLE,
  ACCESSIBILITY_RELATED_TEST_DOCS,
  accessibilityNotesSectionIdsInOrder,
  documentationContainsRequiredSnippets,
  docsIndexMustLinkAccessibilityNotes,
  formatAccessibilityNotesOutline,
  isValidAccessibilityNotesSectionOrder,
  stylesheetSupportsAccessibilityNotes,
} from "@/features/a11y/accessibilityNotes";
import {
  KEYBOARD_FOCUS_OUTLINE_TOKEN,
  SKIP_TO_CONTENT_LABEL,
} from "@/features/a11y/keyboardNavigationFlow";
import {
  BREADCRUMB_NAV_ARIA_LABEL,
  MAIN_NAV_ARIA_LABEL,
  WCAG_AA_NORMAL_TEXT_RATIO,
} from "@/features/a11y/mainScreensAccessibility";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");
const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("accessibilityNotes (item 611)", () => {
  it("documents the ordered accessibility notes outline", () => {
    expect(accessibilityNotesSectionIdsInOrder()).toEqual([
      "goals",
      "scope",
      "principles",
      "landmarks-and-page-structure",
      "keyboard-support",
      "form-labels-and-validation",
      "tables-and-lists",
      "focus-and-contrast",
      "live-regions-and-dialogs",
      "role-aware-navigation",
      "charts-and-non-text",
      "testing-map",
      "known-limitations",
      "do-and-dont",
      "evidence",
      "acceptance",
    ]);
    expect(formatAccessibilityNotesOutline()).toContain("Keyboard support");
    expect(formatAccessibilityNotesOutline()).toContain("Acceptance");
    expect(isValidAccessibilityNotesSectionOrder(accessibilityNotesSectionIdsInOrder())).toBe(
      true,
    );
    expect(isValidAccessibilityNotesSectionOrder(["goals", "scope"])).toBe(false);
    expect(ACCESSIBILITY_NOTES_SECTIONS).toHaveLength(16);
    expect(ACCESSIBILITY_NOTES_TITLE).toBe("Accessibility Notes");
  });

  it("aligns landmark and contrast tokens with items 608 and 609", () => {
    expect(ACCESSIBILITY_LANDMARK_LABELS.skipLink).toBe(SKIP_TO_CONTENT_LABEL);
    expect(ACCESSIBILITY_LANDMARK_LABELS.mainNavigation).toBe(MAIN_NAV_ARIA_LABEL);
    expect(ACCESSIBILITY_LANDMARK_LABELS.breadcrumb).toBe(BREADCRUMB_NAV_ARIA_LABEL);
    expect(ACCESSIBILITY_LANDMARK_LABELS.mainContentId).toBe("main-content");
    expect(ACCESSIBILITY_CONTRAST_TARGETS.normalTextRatio).toBe(WCAG_AA_NORMAL_TEXT_RATIO);
    expect(ACCESSIBILITY_CONTRAST_TARGETS.nonTextRatio).toBe(3);
    expect(ACCESSIBILITY_CONTRAST_TARGETS.focusOutlineToken).toBe(KEYBOARD_FOCUS_OUTLINE_TOKEN);
    expect(ACCESSIBILITY_CONTRAST_TARGETS.focusVisibleSelectors).toContain("input:focus-visible");
    expect(ACCESSIBILITY_KEYBOARD_KEYS).toEqual(["Tab", "Shift+Tab", "Enter", "Escape"]);
    expect(ACCESSIBILITY_LIVE_REGION_ROLES).toContain("alert");
    expect(ACCESSIBILITY_LIVE_REGION_ROLES).toContain("status");
  });

  it("lists implementation modules and related backlog items", () => {
    expect(ACCESSIBILITY_IMPLEMENTATION_MODULES).toContain(
      "frontend/src/features/a11y/keyboardNavigationFlow.ts",
    );
    expect(ACCESSIBILITY_IMPLEMENTATION_MODULES).toContain(
      "frontend/src/features/a11y/mainScreensAccessibility.ts",
    );
    expect(ACCESSIBILITY_RELATED_TEST_DOCS).toContain("docs/testing/ui-keyboard-navigation.md");
    expect(ACCESSIBILITY_NOTES_RELATED_ITEMS).toContain(611);
    expect(ACCESSIBILITY_NOTES_RELATED_ITEMS).toContain(608);
    expect(ACCESSIBILITY_NOTES_RELATED_ITEMS).toContain(609);
    expect(ACCESSIBILITY_NOTES_RELATED_ITEMS).toContain(638);
    expect(ACCESSIBILITY_NOTES_DOC_REQUIRED_SNIPPETS).toContain("item **611**");
  });

  it("keeps the accessibility notes markdown file as delivery evidence", () => {
    const docPath = path.join(repoRoot, ACCESSIBILITY_NOTES_DOC_PATH);
    expect(existsSync(docPath), `Missing ${ACCESSIBILITY_NOTES_DOC_PATH}`).toBe(true);

    const documentation = readRepoFile(ACCESSIBILITY_NOTES_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Landmarks and page structure");
    expect(documentation).toContain("## Keyboard support");
    expect(documentation).toContain("## Acceptance (item 611)");
    expect(documentation).toContain("accessibilityNotes.ts");
    expect(documentation).toContain("Backend authorization is authoritative");
  });

  it("links accessibility notes from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkAccessibilityNotes(index)).toBe(true);
    expect(index).toContain("development/accessibility-notes.md");
  });

  it("requires related testing docs referenced by the notes to exist", () => {
    for (const relative of ACCESSIBILITY_RELATED_TEST_DOCS) {
      expect(existsSync(path.join(repoRoot, relative)), relative).toBe(true);
    }
  });

  it("keeps styles.css supporting focus-visible and skip-link patterns", () => {
    const styles = readFileSync(path.join(frontendRoot, "src/app/styles.css"), "utf8");
    expect(stylesheetSupportsAccessibilityNotes(styles)).toBe(true);
  });

  it("rejects incomplete documentation or stylesheet snapshots", () => {
    expect(documentationContainsRequiredSnippets("# incomplete")).toBe(false);
    expect(stylesheetSupportsAccessibilityNotes("body {}")).toBe(false);
    expect(docsIndexMustLinkAccessibilityNotes("# Documentation\n")).toBe(false);
  });
});
