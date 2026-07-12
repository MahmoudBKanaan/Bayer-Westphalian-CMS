/**
 * Keyboard navigation for core forms (KB NFR-011 / item 587 / item 608).
 *
 * Acceptance contract for Tab / Shift+Tab order, Enter-to-submit, visible
 * focus indicators, and skip-to-content — without requiring a mouse.
 */

import { CUSTOMER_CREATE_FORM_ARIA_LABEL } from "@/features/customers/customerCreationFlow";
import { PRODUCT_CREATE_FORM_ARIA_LABEL } from "@/features/products/productCreationFlow";
import { SEGMENT_CREATE_FORM_ARIA_LABEL } from "@/features/segments/segmentCreationFlow";
import { CAMPAIGN_BUILDER_FORM_ARIA_LABEL } from "@/features/campaigns/campaignCreationFlow";
import { CONSENT_RECORD_FORM_ARIA_LABEL } from "@/features/customers/consentUpdateFlow";

/** Skip link visible on keyboard focus in the authenticated shell. */
export const SKIP_TO_CONTENT_LABEL = "Skip to content";
export const MAIN_CONTENT_ID = "main-content";
export const MAIN_CONTENT_HREF = `#${MAIN_CONTENT_ID}`;

/** Login form accessible name (LoginPage). */
export const LOGIN_FORM_ARIA_LABEL = "Employee sign-in";
export const LOGIN_EMAIL_LABEL = "Email";
export const LOGIN_PASSWORD_LABEL = "Password";
export const LOGIN_SUBMIT_LABEL = "Sign in";

/** CSS selectors that must receive a visible focus ring (styles.css). */
export const KEYBOARD_FOCUS_VISIBLE_SELECTORS = [
  "a:focus-visible",
  "button:focus-visible",
  "input:focus-visible",
  "select:focus-visible",
  "textarea:focus-visible",
  '[role="tab"]:focus-visible',
  '[role="menuitem"]:focus-visible',
] as const;

export const KEYBOARD_FOCUS_OUTLINE_TOKEN = "outline: 3px solid #2563eb";

export type CoreFormId =
  | "login"
  | "customer-create"
  | "consent-record"
  | "product-create"
  | "segment-create"
  | "campaign-builder";

export type CoreFormFieldDefinition = {
  /** Accessible name (label text or control name). */
  accessibleName: string;
  /** Control kind used for keyboard expectations. */
  kind: "text" | "email" | "password" | "select" | "textarea" | "checkbox" | "submit";
};

export type CoreFormDefinition = {
  id: CoreFormId;
  /** Route where the form is mounted. */
  path: string;
  /** Accessible form name (role=form). */
  formAriaLabel: string;
  /** Human title for docs / evidence. */
  title: string;
  /** Expected sequential focus order of primary controls (Tab). */
  tabOrder: CoreFormFieldDefinition[];
  /** Whether Enter from a text field should activate the form submit control. */
  enterSubmits: boolean;
};

/**
 * Core business forms that must be operable with keyboard only (item 608).
 * Order follows the KB happy-path domain progression.
 */
export const CORE_FORMS: CoreFormDefinition[] = [
  {
    id: "login",
    path: "/login",
    formAriaLabel: LOGIN_FORM_ARIA_LABEL,
    title: "Employee sign-in",
    enterSubmits: true,
    tabOrder: [
      { accessibleName: LOGIN_EMAIL_LABEL, kind: "email" },
      { accessibleName: LOGIN_PASSWORD_LABEL, kind: "password" },
      { accessibleName: LOGIN_SUBMIT_LABEL, kind: "submit" },
    ],
  },
  {
    id: "customer-create",
    path: "/customers",
    formAriaLabel: CUSTOMER_CREATE_FORM_ARIA_LABEL,
    title: "Create customer",
    enterSubmits: true,
    tabOrder: [
      { accessibleName: "Customer type", kind: "select" },
      { accessibleName: "First name", kind: "text" },
      { accessibleName: "Last name", kind: "text" },
      { accessibleName: "Email", kind: "email" },
      { accessibleName: "Create customer", kind: "submit" },
    ],
  },
  {
    id: "consent-record",
    path: "/customers/:id",
    formAriaLabel: CONSENT_RECORD_FORM_ARIA_LABEL,
    title: "Record consent",
    enterSubmits: true,
    tabOrder: [
      { accessibleName: "Consent type", kind: "select" },
      { accessibleName: "Consent status", kind: "select" },
      { accessibleName: "Purpose", kind: "text" },
      { accessibleName: "Source", kind: "text" },
      { accessibleName: "Record consent", kind: "submit" },
    ],
  },
  {
    id: "product-create",
    path: "/products",
    formAriaLabel: PRODUCT_CREATE_FORM_ARIA_LABEL,
    title: "Create product",
    enterSubmits: true,
    tabOrder: [
      { accessibleName: "Product name", kind: "text" },
      { accessibleName: "Product type", kind: "select" },
      { accessibleName: "Create product", kind: "submit" },
    ],
  },
  {
    id: "segment-create",
    path: "/segments",
    formAriaLabel: SEGMENT_CREATE_FORM_ARIA_LABEL,
    title: "Create segment",
    enterSubmits: true,
    tabOrder: [
      { accessibleName: "Name", kind: "text" },
      { accessibleName: "Visibility", kind: "select" },
      { accessibleName: "Create segment", kind: "submit" },
    ],
  },
  {
    id: "campaign-builder",
    path: "/campaign-builder",
    formAriaLabel: CAMPAIGN_BUILDER_FORM_ARIA_LABEL,
    title: "Campaign builder",
    enterSubmits: true,
    tabOrder: [
      { accessibleName: "Campaign name", kind: "text" },
      { accessibleName: "Campaign objective", kind: "textarea" },
      { accessibleName: "Campaign channel", kind: "select" },
    ],
  },
];

export type KeyboardNavigationStepId =
  | "open-core-form"
  | "tab-forward"
  | "tab-backward"
  | "submit-with-enter"
  | "observe-focus-indicator";

export type KeyboardNavigationStepDefinition = {
  id: KeyboardNavigationStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Keyboard navigation works for core forms” (item 608). */
export const KEYBOARD_NAVIGATION_FLOW_STEPS: KeyboardNavigationStepDefinition[] = [
  {
    id: "open-core-form",
    index: 0,
    title: "Open a core form",
    description: "Reach login or an authenticated core create form with keyboard-accessible controls.",
  },
  {
    id: "tab-forward",
    index: 1,
    title: "Tab forward",
    description: "Tab moves focus through labeled fields then the primary submit control in document order.",
  },
  {
    id: "tab-backward",
    index: 2,
    title: "Shift+Tab backward",
    description: "Shift+Tab reverses focus order without trapping focus inside the form.",
  },
  {
    id: "submit-with-enter",
    index: 3,
    title: "Submit with Enter",
    description: "Pressing Enter on a text field activates the form’s submit control when the form is valid or surfaces validation.",
  },
  {
    id: "observe-focus-indicator",
    index: 4,
    title: "Visible focus",
    description: "Focused interactive controls show a focus-visible outline (styles.css focus ring).",
  },
];

export function keyboardNavigationStepIdsInOrder(): KeyboardNavigationStepId[] {
  return KEYBOARD_NAVIGATION_FLOW_STEPS.map((step) => step.id);
}

export function formatKeyboardNavigationJourney(): string {
  return KEYBOARD_NAVIGATION_FLOW_STEPS.map((step) => step.title).join(" → ");
}

export function isValidKeyboardNavigationOrder(stepIds: string[]): boolean {
  const expected = keyboardNavigationStepIdsInOrder();
  if (stepIds.length !== expected.length) {
    return false;
  }
  return stepIds.every((id, index) => id === expected[index]);
}

export function getCoreFormById(id: CoreFormId): CoreFormDefinition {
  const form = CORE_FORMS.find((candidate) => candidate.id === id);
  if (form == null) {
    throw new Error(`Unknown core form id: ${id}`);
  }
  return form;
}

export function coreFormIds(): CoreFormId[] {
  return CORE_FORMS.map((form) => form.id);
}

export function coreFormAriaLabels(): string[] {
  return CORE_FORMS.map((form) => form.formAriaLabel);
}

/**
 * Expected Tab sequence of accessible names for a core form.
 * Primary path only — intermediate optional fields may exist in the DOM after these.
 */
export function expectedPrimaryTabNames(formId: CoreFormId): string[] {
  return getCoreFormById(formId).tabOrder.map((field) => field.accessibleName);
}

/**
 * Returns true when a tabindex value would create a positive-tabindex keyboard trap
 * (KB/a11y: prefer natural document order; avoid tabindex &gt; 0).
 */
export function isPositiveTabIndexTrap(tabIndex: number | string | null | undefined): boolean {
  if (tabIndex == null || tabIndex === "") {
    return false;
  }
  const numeric = typeof tabIndex === "number" ? tabIndex : Number.parseInt(String(tabIndex), 10);
  if (Number.isNaN(numeric)) {
    return false;
  }
  return numeric > 0;
}

/** Whether a core form expects Enter to activate submit. */
export function formSupportsEnterSubmit(formId: CoreFormId): boolean {
  return getCoreFormById(formId).enterSubmits;
}

/**
 * Minimal keyboard sequence for login-only acceptance (Playwright / integration).
 * Does not include optional shell chrome.
 */
export const LOGIN_KEYBOARD_SEQUENCE = {
  focusFirst: LOGIN_EMAIL_LABEL,
  afterTab: LOGIN_PASSWORD_LABEL,
  afterSecondTab: LOGIN_SUBMIT_LABEL,
  submitKey: "Enter",
} as const;

export type KeyboardKeyExpectation = "Tab" | "Shift+Tab" | "Enter" | "Escape";

export const REQUIRED_KEYBOARD_KEYS: KeyboardKeyExpectation[] = [
  "Tab",
  "Shift+Tab",
  "Enter",
  "Escape",
];

/**
 * Documents shell keyboard affordances that complement form Tab order.
 */
export const SHELL_KEYBOARD_AFFORDANCES = {
  skipLinkLabel: SKIP_TO_CONTENT_LABEL,
  skipLinkHref: MAIN_CONTENT_HREF,
  mainContentId: MAIN_CONTENT_ID,
  mainContentTabIndex: -1,
  userMenuEscape: "Escape",
} as const;
