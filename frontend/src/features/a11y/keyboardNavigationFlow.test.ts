import { describe, expect, it } from "vitest";
import { CAMPAIGN_BUILDER_FORM_ARIA_LABEL } from "@/features/campaigns/campaignCreationFlow";
import { CONSENT_RECORD_FORM_ARIA_LABEL } from "@/features/customers/consentUpdateFlow";
import { CUSTOMER_CREATE_FORM_ARIA_LABEL } from "@/features/customers/customerCreationFlow";
import { PRODUCT_CREATE_FORM_ARIA_LABEL } from "@/features/products/productCreationFlow";
import { SEGMENT_CREATE_FORM_ARIA_LABEL } from "@/features/segments/segmentCreationFlow";
import {
  CORE_FORMS,
  coreFormAriaLabels,
  coreFormIds,
  expectedPrimaryTabNames,
  formSupportsEnterSubmit,
  formatKeyboardNavigationJourney,
  getCoreFormById,
  isPositiveTabIndexTrap,
  isValidKeyboardNavigationOrder,
  KEYBOARD_FOCUS_OUTLINE_TOKEN,
  KEYBOARD_FOCUS_VISIBLE_SELECTORS,
  KEYBOARD_NAVIGATION_FLOW_STEPS,
  keyboardNavigationStepIdsInOrder,
  LOGIN_FORM_ARIA_LABEL,
  LOGIN_KEYBOARD_SEQUENCE,
  MAIN_CONTENT_HREF,
  MAIN_CONTENT_ID,
  REQUIRED_KEYBOARD_KEYS,
  SHELL_KEYBOARD_AFFORDANCES,
  SKIP_TO_CONTENT_LABEL,
} from "@/features/a11y/keyboardNavigationFlow";

describe("keyboardNavigationFlow (item 608)", () => {
  it("documents the keyboard navigation acceptance journey", () => {
    expect(keyboardNavigationStepIdsInOrder()).toEqual([
      "open-core-form",
      "tab-forward",
      "tab-backward",
      "submit-with-enter",
      "observe-focus-indicator",
    ]);
    expect(formatKeyboardNavigationJourney()).toBe(
      "Open a core form → Tab forward → Shift+Tab backward → Submit with Enter → Visible focus",
    );
    expect(isValidKeyboardNavigationOrder(keyboardNavigationStepIdsInOrder())).toBe(true);
    expect(isValidKeyboardNavigationOrder(["tab-forward", "open-core-form"])).toBe(false);
    expect(KEYBOARD_NAVIGATION_FLOW_STEPS).toHaveLength(5);
  });

  it("catalogues the core business forms required for keyboard operability", () => {
    expect(coreFormIds()).toEqual([
      "login",
      "customer-create",
      "consent-record",
      "product-create",
      "segment-create",
      "campaign-builder",
    ]);
    expect(coreFormAriaLabels()).toEqual([
      LOGIN_FORM_ARIA_LABEL,
      CUSTOMER_CREATE_FORM_ARIA_LABEL,
      CONSENT_RECORD_FORM_ARIA_LABEL,
      PRODUCT_CREATE_FORM_ARIA_LABEL,
      SEGMENT_CREATE_FORM_ARIA_LABEL,
      CAMPAIGN_BUILDER_FORM_ARIA_LABEL,
    ]);
    for (const form of CORE_FORMS) {
      expect(form.tabOrder.length).toBeGreaterThanOrEqual(3);
      expect(formSupportsEnterSubmit(form.id)).toBe(true);
      expect(form.path.length).toBeGreaterThan(0);
    }
  });

  it("defines login Tab order Email → Password → Sign in", () => {
    expect(expectedPrimaryTabNames("login")).toEqual(["Email", "Password", "Sign in"]);
    expect(LOGIN_KEYBOARD_SEQUENCE.focusFirst).toBe("Email");
    expect(LOGIN_KEYBOARD_SEQUENCE.afterTab).toBe("Password");
    expect(LOGIN_KEYBOARD_SEQUENCE.afterSecondTab).toBe("Sign in");
    expect(LOGIN_KEYBOARD_SEQUENCE.submitKey).toBe("Enter");
  });

  it("defines primary Tab names for create forms used in the happy path", () => {
    expect(expectedPrimaryTabNames("customer-create")[0]).toBe("Customer type");
    expect(expectedPrimaryTabNames("customer-create")).toContain("Create customer");
    expect(expectedPrimaryTabNames("product-create")[0]).toBe("Product name");
    expect(expectedPrimaryTabNames("segment-create")).toContain("Visibility");
    expect(expectedPrimaryTabNames("campaign-builder")).toEqual([
      "Campaign name",
      "Campaign objective",
      "Campaign channel",
    ]);
    expect(expectedPrimaryTabNames("consent-record")).toContain("Consent type");
  });

  it("flags positive tabindex values as keyboard traps", () => {
    expect(isPositiveTabIndexTrap(0)).toBe(false);
    expect(isPositiveTabIndexTrap(-1)).toBe(false);
    expect(isPositiveTabIndexTrap(undefined)).toBe(false);
    expect(isPositiveTabIndexTrap(null)).toBe(false);
    expect(isPositiveTabIndexTrap(1)).toBe(true);
    expect(isPositiveTabIndexTrap("2")).toBe(true);
    expect(isPositiveTabIndexTrap("not-a-number")).toBe(false);
  });

  it("requires focus-visible outline selectors and shell skip link", () => {
    expect(KEYBOARD_FOCUS_VISIBLE_SELECTORS).toContain("input:focus-visible");
    expect(KEYBOARD_FOCUS_VISIBLE_SELECTORS).toContain("button:focus-visible");
    expect(KEYBOARD_FOCUS_OUTLINE_TOKEN).toContain("3px solid");
    expect(SKIP_TO_CONTENT_LABEL).toBe("Skip to content");
    expect(MAIN_CONTENT_ID).toBe("main-content");
    expect(MAIN_CONTENT_HREF).toBe("#main-content");
    expect(SHELL_KEYBOARD_AFFORDANCES.mainContentTabIndex).toBe(-1);
    expect(SHELL_KEYBOARD_AFFORDANCES.skipLinkHref).toBe("#main-content");
    expect(REQUIRED_KEYBOARD_KEYS).toEqual(["Tab", "Shift+Tab", "Enter", "Escape"]);
  });

  it("returns form definitions by id", () => {
    expect(getCoreFormById("login").formAriaLabel).toBe(LOGIN_FORM_ARIA_LABEL);
    expect(getCoreFormById("campaign-builder").formAriaLabel).toBe(
      CAMPAIGN_BUILDER_FORM_ARIA_LABEL,
    );
  });
});
