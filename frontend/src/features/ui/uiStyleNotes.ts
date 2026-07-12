/**
 * UI style notes catalog (KB NFR-005 / NFR-011 / Sprint 15 item **610**).
 *
 * Single source of truth for documented design tokens used by unit tests and
 * docs/development/ui-style-notes.md. Values must stay aligned with styles.css.
 */

/** Relative path from repo root (documentation evidence). */
export const UI_STYLE_NOTES_DOC_PATH = "docs/development/ui-style-notes.md";

/** Path to the global stylesheet relative to the frontend package. */
export const UI_STYLESHEET_PATH = "src/app/styles.css";

export const UI_STYLE_NOTES_TITLE = "UI Style Notes";

export type UiStyleNotesSectionId =
  | "goals"
  | "typography"
  | "color-system"
  | "spacing-and-radius"
  | "application-shell"
  | "forms"
  | "tables-and-worklists"
  | "feedback-components"
  | "status-and-domain-badges"
  | "dashboard-and-analytics"
  | "campaign-builder-and-compliance"
  | "login"
  | "do-and-dont"
  | "evidence"
  | "acceptance";

export type UiStyleNotesSection = {
  id: UiStyleNotesSectionId;
  index: number;
  title: string;
  /** Substring expected in the markdown document. */
  docHeading: string;
};

/** Ordered documentation sections for item 610. */
export const UI_STYLE_NOTES_SECTIONS: UiStyleNotesSection[] = [
  { id: "goals", index: 0, title: "Goals", docHeading: "## Goals" },
  { id: "typography", index: 1, title: "Typography", docHeading: "## Typography" },
  { id: "color-system", index: 2, title: "Color system", docHeading: "## Color system" },
  {
    id: "spacing-and-radius",
    index: 3,
    title: "Spacing and radius",
    docHeading: "## Spacing and radius",
  },
  {
    id: "application-shell",
    index: 4,
    title: "Application shell",
    docHeading: "## Application shell",
  },
  { id: "forms", index: 5, title: "Forms", docHeading: "## Forms" },
  {
    id: "tables-and-worklists",
    index: 6,
    title: "Tables and worklists",
    docHeading: "## Tables and worklists",
  },
  {
    id: "feedback-components",
    index: 7,
    title: "Feedback components",
    docHeading: "## Feedback components",
  },
  {
    id: "status-and-domain-badges",
    index: 8,
    title: "Status and domain badges",
    docHeading: "## Status and domain badges",
  },
  {
    id: "dashboard-and-analytics",
    index: 9,
    title: "Dashboard and analytics",
    docHeading: "## Dashboard and analytics",
  },
  {
    id: "campaign-builder-and-compliance",
    index: 10,
    title: "Campaign builder and compliance",
    docHeading: "## Campaign builder and compliance clarity",
  },
  { id: "login", index: 11, title: "Login", docHeading: "## Login" },
  { id: "do-and-dont", index: 12, title: "Do / don’t", docHeading: "## Do / don’t" },
  { id: "evidence", index: 13, title: "Evidence", docHeading: "## Evidence for reports" },
  { id: "acceptance", index: 14, title: "Acceptance", docHeading: "## Acceptance (item 610)" },
];

export function uiStyleNotesSectionIdsInOrder(): UiStyleNotesSectionId[] {
  return UI_STYLE_NOTES_SECTIONS.map((section) => section.id);
}

export function formatUiStyleNotesOutline(): string {
  return UI_STYLE_NOTES_SECTIONS.map((section) => section.title).join(" → ");
}

export function isValidUiStyleNotesSectionOrder(ids: string[]): boolean {
  const expected = uiStyleNotesSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, index) => id === expected[index]);
}

/** Core surface and text colors documented for the professional UI. */
export const UI_COLOR_TOKENS = {
  pageBackground: "#f4f7fb",
  primaryText: "#1f2937",
  strongText: "#0f172a",
  secondaryText: "#475569",
  mutedText: "#64748b",
  primaryButton: "#1d4ed8",
  primaryButtonHover: "#1e40af",
  primaryButtonText: "#ffffff",
  secondaryButton: "#e0f2fe",
  secondaryButtonText: "#075985",
  dangerButton: "#b91c1c",
  disabledButton: "#64748b",
  focusRing: "#2563eb",
  tableHeaderBackground: "#f8fafc",
  emptyStateBorder: "#cbd5e1",
} as const;

/** Status badge pairs (foreground on background) used in style notes and a11y checks. */
export const UI_STATUS_BADGE_COLORS = {
  success: { foreground: "#166534", background: "#dcfce7" },
  warning: { foreground: "#92400e", background: "#fef3c7" },
  danger: { foreground: "#991b1b", background: "#fee2e2" },
  info: { foreground: "#1e40af", background: "#dbeafe" },
} as const;

/** Layout / shell class names that define the professional chrome. */
export const UI_SHELL_CLASS_NAMES = [
  "app-shell",
  "sidebar",
  "sidebar-nav",
  "nav-link",
  "main-panel",
  "topbar",
  "breadcrumb",
  "eyebrow",
  "user-menu",
  "skip-link",
  "loading-indicator",
  "health-pill",
] as const;

/** Shared content / component class names. */
export const UI_COMPONENT_CLASS_NAMES = [
  "panel",
  "page-stack",
  "form-grid",
  "form-error",
  "form-success",
  "button-row",
  "secondary-button",
  "danger-button",
  "status-badge",
  "metric-grid",
  "metric-card",
  "empty-state",
  "state-panel",
  "split-grid",
  "table-scroll",
  "sr-only",
] as const;

/** Responsive breakpoints documented in style notes and styles.css. */
export const UI_BREAKPOINTS = {
  desktopMin: "1281px",
  tabletMax: "1280px",
  tabletMin: "961px",
  contentCollapseMax: "960px",
  mobileMax: "640px",
  minTouchTarget: "44px",
} as const;

/** Typography tokens. */
export const UI_TYPOGRAPHY = {
  fontFamily: "Roboto, Arial, sans-serif",
  fontWeightDefault: "400",
  lineHeight: "1.5",
  tableHeaderSize: "0.78rem",
} as const;

/** Sprint 15 backlog items that the style notes explicitly support. */
export const UI_STYLE_NOTES_RELATED_ITEMS = [
  569, // application shell layout
  570, // sidebar navigation
  571, // top bar and user menu
  574, // loading states
  575, // empty states
  576, // error states
  577, // success notifications
  578, // form validation messages
  579, // confirmation dialogs
  580, // campaign status badges
  585, // responsive desktop/tablet
  586, // mobile minimum usability
  591, // dashboard readability
  592, // campaign builder flow
  593, // compliance review clarity
  608, // keyboard navigation
  609, // main screens a11y
  610, // UI style notes (this item)
] as const;

/**
 * CSS substrings that must appear in styles.css for the documented professional look.
 */
export const UI_STYLE_CSS_REQUIRED_SNIPPETS = [
  UI_COLOR_TOKENS.pageBackground,
  UI_COLOR_TOKENS.primaryButton,
  UI_COLOR_TOKENS.focusRing,
  UI_TYPOGRAPHY.fontFamily,
  `.${UI_SHELL_CLASS_NAMES[0]}`,
  `.${UI_COMPONENT_CLASS_NAMES[0]}`,
  `@media (min-width: ${UI_BREAKPOINTS.desktopMin})`,
  `@media (max-width: ${UI_BREAKPOINTS.mobileMax})`,
  "button:focus-visible",
  "outline: 3px solid #2563eb",
  "min-height: 44px",
] as const;

/**
 * Markdown substrings required in docs/development/ui-style-notes.md.
 */
export const UI_STYLE_NOTES_DOC_REQUIRED_SNIPPETS = [
  UI_STYLE_NOTES_TITLE,
  "item **610**",
  "NFR-005",
  "NFR-011",
  "styles.css",
  "AppLayout",
  ...UI_STYLE_NOTES_SECTIONS.map((section) => section.docHeading),
  UI_COLOR_TOKENS.pageBackground,
  UI_COLOR_TOKENS.primaryButton,
  UI_TYPOGRAPHY.fontFamily,
  UI_BREAKPOINTS.desktopMin,
  UI_BREAKPOINTS.mobileMax,
  "WCAG AA",
  "status-badge",
  "ConfirmationDialog",
  "Skip link",
] as const;

export function docsIndexMustLinkUiStyleNotes(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("development/ui-style-notes.md") ||
    indexMarkdown.includes("ui-style-notes.md")
  );
}

export function stylesheetContainsDocumentedTokens(stylesheetCss: string): boolean {
  return UI_STYLE_CSS_REQUIRED_SNIPPETS.every((snippet) => stylesheetCss.includes(snippet));
}

export function documentationContainsRequiredSnippets(documentationMarkdown: string): boolean {
  return UI_STYLE_NOTES_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentationMarkdown.includes(snippet),
  );
}
