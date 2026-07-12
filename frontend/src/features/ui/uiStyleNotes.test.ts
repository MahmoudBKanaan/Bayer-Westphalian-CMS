import { readFileSync, existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  documentationContainsRequiredSnippets,
  docsIndexMustLinkUiStyleNotes,
  formatUiStyleNotesOutline,
  isValidUiStyleNotesSectionOrder,
  stylesheetContainsDocumentedTokens,
  UI_BREAKPOINTS,
  UI_COLOR_TOKENS,
  UI_COMPONENT_CLASS_NAMES,
  UI_SHELL_CLASS_NAMES,
  UI_STATUS_BADGE_COLORS,
  UI_STYLE_CSS_REQUIRED_SNIPPETS,
  UI_STYLE_NOTES_DOC_PATH,
  UI_STYLE_NOTES_DOC_REQUIRED_SNIPPETS,
  UI_STYLE_NOTES_RELATED_ITEMS,
  UI_STYLE_NOTES_SECTIONS,
  UI_STYLE_NOTES_TITLE,
  UI_TYPOGRAPHY,
  uiStyleNotesSectionIdsInOrder,
} from "@/features/ui/uiStyleNotes";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");
const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("uiStyleNotes (item 610)", () => {
  it("documents the ordered UI style notes outline", () => {
    expect(uiStyleNotesSectionIdsInOrder()).toEqual([
      "goals",
      "typography",
      "color-system",
      "spacing-and-radius",
      "application-shell",
      "forms",
      "tables-and-worklists",
      "feedback-components",
      "status-and-domain-badges",
      "dashboard-and-analytics",
      "campaign-builder-and-compliance",
      "login",
      "do-and-dont",
      "evidence",
      "acceptance",
    ]);
    expect(formatUiStyleNotesOutline()).toContain("Typography");
    expect(formatUiStyleNotesOutline()).toContain("Acceptance");
    expect(isValidUiStyleNotesSectionOrder(uiStyleNotesSectionIdsInOrder())).toBe(true);
    expect(isValidUiStyleNotesSectionOrder(["goals"])).toBe(false);
    expect(UI_STYLE_NOTES_SECTIONS).toHaveLength(15);
    expect(UI_STYLE_NOTES_TITLE).toBe("UI Style Notes");
  });

  it("catalogues shell, component, color, and breakpoint tokens", () => {
    expect(UI_SHELL_CLASS_NAMES).toContain("app-shell");
    expect(UI_SHELL_CLASS_NAMES).toContain("skip-link");
    expect(UI_COMPONENT_CLASS_NAMES).toContain("status-badge");
    expect(UI_COMPONENT_CLASS_NAMES).toContain("form-error");
    expect(UI_COLOR_TOKENS.pageBackground).toBe("#f4f7fb");
    expect(UI_COLOR_TOKENS.primaryButton).toBe("#1d4ed8");
    expect(UI_COLOR_TOKENS.focusRing).toBe("#2563eb");
    expect(UI_STATUS_BADGE_COLORS.success.foreground).toBe("#166534");
    expect(UI_STATUS_BADGE_COLORS.danger.background).toBe("#fee2e2");
    expect(UI_BREAKPOINTS.desktopMin).toBe("1281px");
    expect(UI_BREAKPOINTS.mobileMax).toBe("640px");
    expect(UI_BREAKPOINTS.minTouchTarget).toBe("44px");
    expect(UI_TYPOGRAPHY.fontFamily).toContain("Roboto");
    expect(UI_STYLE_NOTES_RELATED_ITEMS).toContain(610);
    expect(UI_STYLE_NOTES_RELATED_ITEMS).toContain(569);
    expect(UI_STYLE_CSS_REQUIRED_SNIPPETS.length).toBeGreaterThan(5);
    expect(UI_STYLE_NOTES_DOC_REQUIRED_SNIPPETS).toContain("item **610**");
  });

  it("keeps the UI style notes markdown file as delivery evidence", () => {
    const docPath = path.join(repoRoot, UI_STYLE_NOTES_DOC_PATH);
    expect(existsSync(docPath), `Missing ${UI_STYLE_NOTES_DOC_PATH}`).toBe(true);

    const documentation = readRepoFile(UI_STYLE_NOTES_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Typography");
    expect(documentation).toContain("## Color system");
    expect(documentation).toContain("## Application shell");
    expect(documentation).toContain("## Acceptance (item 610)");
    expect(documentation).toContain("uiStyleNotes.ts");
    expect(documentation).toContain("styles.css");
  });

  it("links UI style notes from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkUiStyleNotes(index)).toBe(true);
    expect(index).toContain("development/ui-style-notes.md");
  });

  it("keeps styles.css aligned with documented professional tokens", () => {
    const stylesPath = path.join(frontendRoot, "src/app/styles.css");
    expect(existsSync(stylesPath)).toBe(true);
    const styles = readFileSync(stylesPath, "utf8");
    expect(stylesheetContainsDocumentedTokens(styles)).toBe(true);

    for (const className of UI_SHELL_CLASS_NAMES) {
      expect(styles, `missing .${className}`).toContain(`.${className}`);
    }
    for (const className of UI_COMPONENT_CLASS_NAMES) {
      expect(styles, `missing .${className}`).toContain(`.${className}`);
    }
    expect(styles).toContain(UI_TYPOGRAPHY.fontFamily);
    expect(styles).toContain(`@media (min-width: ${UI_BREAKPOINTS.desktopMin})`);
    expect(styles).toContain(`@media (max-width: ${UI_BREAKPOINTS.mobileMax})`);
  });

  it("rejects incomplete documentation or stylesheet snapshots", () => {
    expect(documentationContainsRequiredSnippets("# incomplete")).toBe(false);
    expect(stylesheetContainsDocumentedTokens("body { color: red; }")).toBe(false);
    expect(docsIndexMustLinkUiStyleNotes("# Documentation\n")).toBe(false);
  });
});
