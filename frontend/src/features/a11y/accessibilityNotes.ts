/**
 * Accessibility notes catalog (KB NFR-011 / Sprint 15 item **611**).
 *
 * Locks documentation structure and cross-references for accessibility guidance
 * in docs/development/accessibility-notes.md.
 */

import {
  KEYBOARD_FOCUS_OUTLINE_TOKEN,
  KEYBOARD_FOCUS_VISIBLE_SELECTORS,
  MAIN_CONTENT_ID,
  SKIP_TO_CONTENT_LABEL,
} from "@/features/a11y/keyboardNavigationFlow";
import {
  BREADCRUMB_NAV_ARIA_LABEL,
  MAIN_NAV_ARIA_LABEL,
  WCAG_AA_NORMAL_TEXT_RATIO,
  WCAG_NON_TEXT_CONTRAST_RATIO,
} from "@/features/a11y/mainScreensAccessibility";

/** Relative path from repo root (documentation evidence). */
export const ACCESSIBILITY_NOTES_DOC_PATH = "docs/development/accessibility-notes.md";

export const ACCESSIBILITY_NOTES_TITLE = "Accessibility Notes";

export type AccessibilityNotesSectionId =
  | "goals"
  | "scope"
  | "principles"
  | "landmarks-and-page-structure"
  | "keyboard-support"
  | "form-labels-and-validation"
  | "tables-and-lists"
  | "focus-and-contrast"
  | "live-regions-and-dialogs"
  | "role-aware-navigation"
  | "charts-and-non-text"
  | "testing-map"
  | "known-limitations"
  | "do-and-dont"
  | "evidence"
  | "acceptance";

export type AccessibilityNotesSection = {
  id: AccessibilityNotesSectionId;
  index: number;
  title: string;
  docHeading: string;
};

/** Ordered documentation sections for item 611. */
export const ACCESSIBILITY_NOTES_SECTIONS: AccessibilityNotesSection[] = [
  { id: "goals", index: 0, title: "Goals", docHeading: "## Goals" },
  { id: "scope", index: 1, title: "Scope", docHeading: "## Scope" },
  { id: "principles", index: 2, title: "Principles", docHeading: "## Principles" },
  {
    id: "landmarks-and-page-structure",
    index: 3,
    title: "Landmarks and page structure",
    docHeading: "## Landmarks and page structure",
  },
  {
    id: "keyboard-support",
    index: 4,
    title: "Keyboard support",
    docHeading: "## Keyboard support",
  },
  {
    id: "form-labels-and-validation",
    index: 5,
    title: "Form labels and validation",
    docHeading: "## Form labels and validation",
  },
  {
    id: "tables-and-lists",
    index: 6,
    title: "Tables and lists",
    docHeading: "## Tables and lists",
  },
  {
    id: "focus-and-contrast",
    index: 7,
    title: "Focus and contrast",
    docHeading: "## Focus and contrast",
  },
  {
    id: "live-regions-and-dialogs",
    index: 8,
    title: "Live regions and dialogs",
    docHeading: "## Live regions and dialogs",
  },
  {
    id: "role-aware-navigation",
    index: 9,
    title: "Role-aware navigation",
    docHeading: "## Role-aware navigation",
  },
  {
    id: "charts-and-non-text",
    index: 10,
    title: "Charts and non-text content",
    docHeading: "## Charts and non-text content",
  },
  { id: "testing-map", index: 11, title: "Testing map", docHeading: "## Testing map" },
  {
    id: "known-limitations",
    index: 12,
    title: "Known limitations",
    docHeading: "## Known limitations",
  },
  { id: "do-and-dont", index: 13, title: "Do / don’t", docHeading: "## Do / don’t" },
  {
    id: "evidence",
    index: 14,
    title: "Evidence for reports",
    docHeading: "## Evidence for reports",
  },
  {
    id: "acceptance",
    index: 15,
    title: "Acceptance",
    docHeading: "## Acceptance (item 611)",
  },
];

export function accessibilityNotesSectionIdsInOrder(): AccessibilityNotesSectionId[] {
  return ACCESSIBILITY_NOTES_SECTIONS.map((section) => section.id);
}

export function formatAccessibilityNotesOutline(): string {
  return ACCESSIBILITY_NOTES_SECTIONS.map((section) => section.title).join(" → ");
}

export function isValidAccessibilityNotesSectionOrder(ids: string[]): boolean {
  const expected = accessibilityNotesSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, index) => id === expected[index]);
}

/** Landmark labels documented for the authenticated shell. */
export const ACCESSIBILITY_LANDMARK_LABELS = {
  skipLink: SKIP_TO_CONTENT_LABEL,
  mainContentId: MAIN_CONTENT_ID,
  mainNavigation: MAIN_NAV_ARIA_LABEL,
  breadcrumb: BREADCRUMB_NAV_ARIA_LABEL,
} as const;

/** WCAG targets restated for accessibility notes. */
export const ACCESSIBILITY_CONTRAST_TARGETS = {
  normalTextRatio: WCAG_AA_NORMAL_TEXT_RATIO,
  nonTextRatio: WCAG_NON_TEXT_CONTRAST_RATIO,
  focusOutlineToken: KEYBOARD_FOCUS_OUTLINE_TOKEN,
  focusVisibleSelectors: KEYBOARD_FOCUS_VISIBLE_SELECTORS,
} as const;

/** Live region / dialog roles expected in the UI. */
export const ACCESSIBILITY_LIVE_REGION_ROLES = ["alert", "status", "dialog", "note"] as const;

/** Core keyboard keys documented for operability. */
export const ACCESSIBILITY_KEYBOARD_KEYS = ["Tab", "Shift+Tab", "Enter", "Escape"] as const;

/** Implementation modules referenced by the notes. */
export const ACCESSIBILITY_IMPLEMENTATION_MODULES = [
  "frontend/src/features/a11y/keyboardNavigationFlow.ts",
  "frontend/src/features/a11y/mainScreensAccessibility.ts",
  "frontend/src/features/a11y/accessibilityNotes.ts",
  "frontend/src/features/auth/roleBasedMenu.ts",
  "frontend/src/components/AppLayout.tsx",
  "frontend/src/components/ConfirmationDialog.tsx",
  "frontend/src/app/styles.css",
] as const;

/** Related testing doc paths (repo-relative). */
export const ACCESSIBILITY_RELATED_TEST_DOCS = [
  "docs/testing/ui-keyboard-navigation.md",
  "docs/testing/ui-main-screens-accessibility.md",
  "docs/development/ui-style-notes.md",
] as const;

/** Sprint / backlog items the accessibility notes explicitly support. */
export const ACCESSIBILITY_NOTES_RELATED_ITEMS = [
  587, // keyboard navigation support
  588, // labels for form controls
  589, // color contrast
  590, // accessible table labels
  607, // role-based menu
  608, // keyboard navigation core forms
  609, // main screens basic a11y
  610, // UI style notes
  611, // accessibility notes (this item)
  638, // run accessibility checks (later)
] as const;

/**
 * Substrings required in docs/development/accessibility-notes.md.
 */
export const ACCESSIBILITY_NOTES_DOC_REQUIRED_SNIPPETS = [
  ACCESSIBILITY_NOTES_TITLE,
  "item **611**",
  "NFR-011",
  "NFR-005",
  "SEC-003",
  ...ACCESSIBILITY_NOTES_SECTIONS.map((section) => section.docHeading),
  ACCESSIBILITY_LANDMARK_LABELS.skipLink,
  ACCESSIBILITY_LANDMARK_LABELS.mainContentId,
  ACCESSIBILITY_LANDMARK_LABELS.mainNavigation,
  ACCESSIBILITY_LANDMARK_LABELS.breadcrumb,
  "WCAG AA",
  "4.5:1",
  "3:1",
  "tabindex",
  "role=\"alert\"",
  "role=\"status\"",
  "ConfirmationDialog",
  "keyboardNavigationFlow",
  "mainScreensAccessibility",
  "item **608**",
  "item **609**",
  "item **638**",
  "Backend authorization is authoritative",
] as const;

export function docsIndexMustLinkAccessibilityNotes(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("development/accessibility-notes.md") ||
    indexMarkdown.includes("accessibility-notes.md")
  );
}

export function documentationContainsRequiredSnippets(documentationMarkdown: string): boolean {
  return ACCESSIBILITY_NOTES_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentationMarkdown.includes(snippet),
  );
}

/**
 * True when CSS still exposes the focus-visible and skip-link patterns referenced by the notes.
 */
export function stylesheetSupportsAccessibilityNotes(stylesheetCss: string): boolean {
  return (
    stylesheetCss.includes("button:focus-visible") &&
    stylesheetCss.includes("input:focus-visible") &&
    stylesheetCss.includes(KEYBOARD_FOCUS_OUTLINE_TOKEN) &&
    stylesheetCss.includes(".skip-link") &&
    stylesheetCss.includes(".sr-only")
  );
}
