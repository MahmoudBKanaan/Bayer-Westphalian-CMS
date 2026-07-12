/**
 * Basic accessibility checks for main screens (KB NFR-011 / items 588–590 / **609**).
 *
 * Documents which primary routes must expose landmarks, page headings, labeled
 * tables/forms, and shell navigation — complementary to keyboard nav (item 608).
 */

import { MAIN_NAV_ARIA_LABEL } from "@/features/auth/roleBasedMenu";
import {
  KEYBOARD_FOCUS_OUTLINE_TOKEN,
  KEYBOARD_FOCUS_VISIBLE_SELECTORS,
  LOGIN_FORM_ARIA_LABEL,
  MAIN_CONTENT_ID,
  SKIP_TO_CONTENT_LABEL,
} from "@/features/a11y/keyboardNavigationFlow";
import { LOGIN_PAGE_TITLE, LOGIN_PANEL_HEADING } from "@/features/auth/loginFlow";
import { CUSTOMER_CREATE_FORM_ARIA_LABEL, CUSTOMER_LIST_TABLE_ARIA_LABEL } from "@/features/customers/customerCreationFlow";
import { PRODUCT_CREATE_FORM_ARIA_LABEL, PRODUCT_LIST_TABLE_ARIA_LABEL } from "@/features/products/productCreationFlow";
import { SEGMENT_CREATE_FORM_ARIA_LABEL, SEGMENT_LIST_TABLE_ARIA_LABEL } from "@/features/segments/segmentCreationFlow";
import { CAMPAIGN_BUILDER_FORM_ARIA_LABEL } from "@/features/campaigns/campaignCreationFlow";
import { COMPLIANCE_QUEUE_TABLE_ARIA_LABEL } from "@/features/campaigns/complianceApprovalFlow";
import { DASHBOARD_KPI_CARDS_ARIA_LABEL } from "@/features/dashboard/dashboardAnalyticsFlow";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

// Re-export keyboard a11y tokens so item 609 consumers share one import surface.
export {
  KEYBOARD_FOCUS_OUTLINE_TOKEN,
  KEYBOARD_FOCUS_VISIBLE_SELECTORS,
  MAIN_CONTENT_ID,
  SKIP_TO_CONTENT_LABEL,
  MAIN_NAV_ARIA_LABEL,
};

/** Breadcrumb nav accessible name in AppLayout. */
export const BREADCRUMB_NAV_ARIA_LABEL = "Breadcrumb";

/** WCAG AA minimum contrast for normal text (styles.test / NFR-011). */
export const WCAG_AA_NORMAL_TEXT_RATIO = 4.5;
/** WCAG non-text contrast for UI components such as focus rings. */
export const WCAG_NON_TEXT_CONTRAST_RATIO = 3;

export type MainScreenId =
  | "login"
  | "dashboard"
  | "customers"
  | "products"
  | "segments"
  | "campaigns"
  | "campaign-builder"
  | "compliance"
  | "analytics"
  | "reports"
  | "audit"
  | "users";

export type PrimaryContentExpectation =
  | { kind: "form"; name: string }
  | { kind: "table"; name: string | RegExp }
  | { kind: "region"; name: string }
  | { kind: "heading"; name: string; level?: number };

export type MainScreenDefinition = {
  id: MainScreenId;
  /** Route path under the SPA. */
  path: string;
  /**
   * Level-1 page title in the shell (AppLayout) or login h1.
   * Authenticated screens use nav item labels for the shell h1.
   */
  pageHeading: string;
  /** Roles used when integration-testing the screen. */
  roles: SystemRoleName[];
  /** Whether the authenticated application shell is expected. */
  usesAppShell: boolean;
  /** Primary labeled content that must be findable by role + name. */
  primaryContent: PrimaryContentExpectation[];
  /** Optional in-page h2 that improves screen structure. */
  contentHeading?: string;
};

/**
 * Main screens that must pass basic accessibility checks (item 609).
 * Aligns with KB “Frontend Screens” used daily by internal roles.
 */
export const MAIN_SCREENS: MainScreenDefinition[] = [
  {
    id: "login",
    path: "/login",
    pageHeading: LOGIN_PAGE_TITLE,
    roles: [],
    usesAppShell: false,
    primaryContent: [{ kind: "form", name: LOGIN_FORM_ARIA_LABEL }],
    contentHeading: LOGIN_PANEL_HEADING,
  },
  {
    id: "dashboard",
    path: "/dashboard",
    pageHeading: "Dashboard",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [{ kind: "region", name: DASHBOARD_KPI_CARDS_ARIA_LABEL }],
  },
  {
    id: "customers",
    path: "/customers",
    pageHeading: "Customers",
    roles: ["ADMIN"],
    usesAppShell: true,
    // Create form is always present for ADMIN; list table appears when rows load.
    primaryContent: [
      { kind: "form", name: CUSTOMER_CREATE_FORM_ARIA_LABEL },
      { kind: "table", name: CUSTOMER_LIST_TABLE_ARIA_LABEL },
    ],
    contentHeading: "Customers and prospects",
  },
  {
    id: "products",
    path: "/products",
    pageHeading: "Products",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [
      { kind: "form", name: PRODUCT_CREATE_FORM_ARIA_LABEL },
      { kind: "table", name: PRODUCT_LIST_TABLE_ARIA_LABEL },
    ],
  },
  {
    id: "segments",
    path: "/segments",
    pageHeading: "Segments",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [
      { kind: "form", name: SEGMENT_CREATE_FORM_ARIA_LABEL },
      { kind: "table", name: SEGMENT_LIST_TABLE_ARIA_LABEL },
    ],
  },
  {
    id: "campaigns",
    path: "/campaigns",
    pageHeading: "Campaigns",
    roles: ["ADMIN"],
    usesAppShell: true,
    // Worklist heading is always mounted; table requires at least one campaign row.
    primaryContent: [
      { kind: "heading", name: "Campaign worklist", level: 2 },
      { kind: "table", name: /Campaign worklist/i },
    ],
    contentHeading: "Campaign worklist",
  },
  {
    id: "campaign-builder",
    path: "/campaign-builder",
    pageHeading: "Builder",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [
      { kind: "form", name: CAMPAIGN_BUILDER_FORM_ARIA_LABEL },
      { kind: "region", name: "Campaign builder steps" },
    ],
    contentHeading: "Campaign Builder",
  },
  {
    id: "compliance",
    path: "/compliance",
    pageHeading: "Compliance",
    roles: ["ADMIN"],
    usesAppShell: true,
    // Checklist / summary always present; queue table when SUBMITTED campaigns exist.
    primaryContent: [
      { kind: "region", name: "Compliance review checklist" },
      { kind: "table", name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL },
    ],
  },
  {
    id: "analytics",
    path: "/analytics",
    pageHeading: "Analytics",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [
      { kind: "heading", name: "Analytics", level: 2 },
      // Charts use role="img" with aria-label (ChartFrame).
      { kind: "region", name: "Campaign open click and conversion rate comparison chart" },
    ],
  },
  {
    id: "reports",
    path: "/reports",
    pageHeading: "Reports",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [{ kind: "heading", name: "Campaign export", level: 2 }],
  },
  {
    id: "audit",
    path: "/audit",
    pageHeading: "Audit",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [
      { kind: "region", name: "Audit log filters" },
      { kind: "table", name: "Audit log table" },
    ],
  },
  {
    id: "users",
    path: "/users",
    pageHeading: "Users",
    roles: ["ADMIN"],
    usesAppShell: true,
    primaryContent: [{ kind: "table", name: /Employee accounts/i }],
  },
];

export type BasicA11yCheckId =
  | "page-heading"
  | "main-landmark"
  | "skip-link"
  | "main-navigation"
  | "breadcrumb"
  | "primary-content-labels"
  | "focus-visible-styles"
  | "color-contrast-tokens"
  | "form-control-labels";

export type BasicA11yCheckDefinition = {
  id: BasicA11yCheckId;
  index: number;
  title: string;
  description: string;
};

/** Ordered acceptance checks for “Main screens pass basic accessibility checks”. */
export const BASIC_A11Y_CHECKS: BasicA11yCheckDefinition[] = [
  {
    id: "page-heading",
    index: 0,
    title: "Page heading",
    description: "Each main screen exposes a single primary heading (shell h1 or login h1).",
  },
  {
    id: "main-landmark",
    index: 1,
    title: "Main landmark",
    description: "Authenticated screens expose main#main-content; login uses a main landmark.",
  },
  {
    id: "skip-link",
    index: 2,
    title: "Skip link",
    description: "Authenticated shell provides Skip to content targeting main content.",
  },
  {
    id: "main-navigation",
    index: 3,
    title: "Main navigation",
    description: "Authenticated shell exposes labeled Main navigation with role-filtered links.",
  },
  {
    id: "breadcrumb",
    index: 4,
    title: "Breadcrumb",
    description: "Authenticated shell exposes a Breadcrumb navigation trail.",
  },
  {
    id: "primary-content-labels",
    index: 5,
    title: "Primary content labels",
    description: "Tables, forms, and key regions have accessible names or labelled headings.",
  },
  {
    id: "focus-visible-styles",
    index: 6,
    title: "Focus-visible styles",
    description: "Interactive controls define a visible focus-visible outline (item 608 CSS).",
  },
  {
    id: "color-contrast-tokens",
    index: 7,
    title: "Color contrast tokens",
    description: "Shared UI color pairs meet WCAG AA normal text contrast (4.5:1).",
  },
  {
    id: "form-control-labels",
    index: 8,
    title: "Form control labels",
    description: "Core forms associate labels with controls (label text or aria-label).",
  },
];

export function basicA11yCheckIdsInOrder(): BasicA11yCheckId[] {
  return BASIC_A11Y_CHECKS.map((check) => check.id);
}

export function formatBasicA11yJourney(): string {
  return BASIC_A11Y_CHECKS.map((check) => check.title).join(" → ");
}

export function isValidBasicA11yCheckOrder(ids: string[]): boolean {
  const expected = basicA11yCheckIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, index) => id === expected[index]);
}

export function getMainScreenById(id: MainScreenId): MainScreenDefinition {
  const screen = MAIN_SCREENS.find((candidate) => candidate.id === id);
  if (screen == null) {
    throw new Error(`Unknown main screen id: ${id}`);
  }
  return screen;
}

export function mainScreenIds(): MainScreenId[] {
  return MAIN_SCREENS.map((screen) => screen.id);
}

export function mainScreenPaths(): string[] {
  return MAIN_SCREENS.map((screen) => screen.path);
}

export function shellMainScreens(): MainScreenDefinition[] {
  return MAIN_SCREENS.filter((screen) => screen.usesAppShell);
}

export function loginMainScreen(): MainScreenDefinition {
  return getMainScreenById("login");
}

/**
 * Shell landmark expectations for authenticated main screens.
 */
export function shellLandmarkExpectations(screen: MainScreenDefinition) {
  if (!screen.usesAppShell) {
    return {
      skipLink: false as const,
      mainNav: false as const,
      breadcrumb: false as const,
      mainId: null as string | null,
    };
  }
  return {
    skipLink: true as const,
    mainNav: true as const,
    breadcrumb: true as const,
    mainId: MAIN_CONTENT_ID,
  };
}

/** Shared text/background pairs that must stay AA-compliant (mirrored in styles.test). */
export const ACCESSIBLE_COLOR_PAIRS: ReadonlyArray<{
  label: string;
  foreground: string;
  background: string;
  minRatio: number;
}> = [
  { label: "Primary button", foreground: "#ffffff", background: "#1d4ed8", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Disabled button", foreground: "#ffffff", background: "#64748b", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Secondary button", foreground: "#075985", background: "#e0f2fe", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Table headers", foreground: "#64748b", background: "#ffffff", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Success badge", foreground: "#166534", background: "#dcfce7", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Warning badge", foreground: "#92400e", background: "#fef3c7", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Danger badge", foreground: "#991b1b", background: "#fee2e2", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  { label: "Info badge", foreground: "#1e40af", background: "#dbeafe", minRatio: WCAG_AA_NORMAL_TEXT_RATIO },
  {
    label: "Focus ring on light surface",
    foreground: "#2563eb",
    background: "#ffffff",
    minRatio: WCAG_NON_TEXT_CONTRAST_RATIO,
  },
];

export function relativeLuminance(hexColor: string): number {
  const channels = hexColor
    .replace("#", "")
    .match(/.{2}/g)
    ?.map((channel) => Number.parseInt(channel, 16) / 255);

  if (channels == null || channels.length !== 3) {
    throw new Error(`Unsupported color format: ${hexColor}`);
  }

  const [red, green, blue] = channels.map((channel) =>
    channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4,
  ) as [number, number, number];

  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

export function contrastRatio(foreground: string, background: string): number {
  const foregroundLuminance = relativeLuminance(foreground);
  const backgroundLuminance = relativeLuminance(background);
  const lighter = Math.max(foregroundLuminance, backgroundLuminance);
  const darker = Math.min(foregroundLuminance, backgroundLuminance);
  return (lighter + 0.05) / (darker + 0.05);
}

export function colorPairMeetsContrast(pair: (typeof ACCESSIBLE_COLOR_PAIRS)[number]): boolean {
  return contrastRatio(pair.foreground, pair.background) >= pair.minRatio;
}

export function allAccessibleColorPairsPass(): boolean {
  return ACCESSIBLE_COLOR_PAIRS.every(colorPairMeetsContrast);
}

/**
 * Snapshot-style evaluation used by unit tests (no DOM).
 * Integration/Playwright assert the same rules against rendered UI.
 */
export type BasicA11yScreenSnapshot = {
  screenId: MainScreenId;
  hasPageHeading: boolean;
  hasMainLandmark: boolean;
  hasSkipLink: boolean;
  hasMainNavigation: boolean;
  hasBreadcrumb: boolean;
  primaryContentLabeled: boolean;
  formControlsLabeled: boolean;
};

export function evaluateBasicA11ySnapshot(snapshot: BasicA11yScreenSnapshot): {
  passed: boolean;
  failedChecks: BasicA11yCheckId[];
} {
  const screen = getMainScreenById(snapshot.screenId);
  const landmarks = shellLandmarkExpectations(screen);
  const failedChecks: BasicA11yCheckId[] = [];

  if (!snapshot.hasPageHeading) {
    failedChecks.push("page-heading");
  }
  if (!snapshot.hasMainLandmark) {
    failedChecks.push("main-landmark");
  }
  if (landmarks.skipLink && !snapshot.hasSkipLink) {
    failedChecks.push("skip-link");
  }
  if (landmarks.mainNav && !snapshot.hasMainNavigation) {
    failedChecks.push("main-navigation");
  }
  if (landmarks.breadcrumb && !snapshot.hasBreadcrumb) {
    failedChecks.push("breadcrumb");
  }
  if (!snapshot.primaryContentLabeled) {
    failedChecks.push("primary-content-labels");
  }
  if (screen.primaryContent.some((item) => item.kind === "form") && !snapshot.formControlsLabeled) {
    failedChecks.push("form-control-labels");
  }

  return { passed: failedChecks.length === 0, failedChecks };
}

