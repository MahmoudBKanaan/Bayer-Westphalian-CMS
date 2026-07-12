import { describe, expect, it } from "vitest";
import {
  ACCESSIBLE_COLOR_PAIRS,
  allAccessibleColorPairsPass,
  BASIC_A11Y_CHECKS,
  basicA11yCheckIdsInOrder,
  BREADCRUMB_NAV_ARIA_LABEL,
  colorPairMeetsContrast,
  contrastRatio,
  evaluateBasicA11ySnapshot,
  formatBasicA11yJourney,
  getMainScreenById,
  isValidBasicA11yCheckOrder,
  KEYBOARD_FOCUS_OUTLINE_TOKEN,
  KEYBOARD_FOCUS_VISIBLE_SELECTORS,
  loginMainScreen,
  MAIN_CONTENT_ID,
  MAIN_NAV_ARIA_LABEL,
  MAIN_SCREENS,
  mainScreenIds,
  mainScreenPaths,
  shellLandmarkExpectations,
  shellMainScreens,
  SKIP_TO_CONTENT_LABEL,
  WCAG_AA_NORMAL_TEXT_RATIO,
} from "@/features/a11y/mainScreensAccessibility";
import { LOGIN_FORM_ARIA_LABEL } from "@/features/a11y/keyboardNavigationFlow";
import { LOGIN_PAGE_TITLE } from "@/features/auth/loginFlow";

describe("mainScreensAccessibility (item 609)", () => {
  it("documents the ordered basic accessibility check journey", () => {
    expect(basicA11yCheckIdsInOrder()).toEqual([
      "page-heading",
      "main-landmark",
      "skip-link",
      "main-navigation",
      "breadcrumb",
      "primary-content-labels",
      "focus-visible-styles",
      "color-contrast-tokens",
      "form-control-labels",
    ]);
    expect(formatBasicA11yJourney()).toContain("Page heading");
    expect(formatBasicA11yJourney()).toContain("Form control labels");
    expect(isValidBasicA11yCheckOrder(basicA11yCheckIdsInOrder())).toBe(true);
    expect(isValidBasicA11yCheckOrder(["page-heading"])).toBe(false);
    expect(BASIC_A11Y_CHECKS).toHaveLength(9);
  });

  it("catalogues the main screens used by internal business users", () => {
    expect(mainScreenIds()).toEqual([
      "login",
      "dashboard",
      "customers",
      "products",
      "segments",
      "campaigns",
      "campaign-builder",
      "compliance",
      "analytics",
      "reports",
      "audit",
      "users",
    ]);
    expect(mainScreenPaths()).toContain("/login");
    expect(mainScreenPaths()).toContain("/dashboard");
    expect(mainScreenPaths()).toContain("/campaign-builder");
    expect(shellMainScreens().every((screen) => screen.usesAppShell)).toBe(true);
    expect(shellMainScreens()).toHaveLength(MAIN_SCREENS.length - 1);
  });

  it("defines login without shell landmarks and with labeled sign-in form", () => {
    const login = loginMainScreen();
    expect(login.pageHeading).toBe(LOGIN_PAGE_TITLE);
    expect(login.usesAppShell).toBe(false);
    expect(login.primaryContent).toEqual([{ kind: "form", name: LOGIN_FORM_ARIA_LABEL }]);
    expect(shellLandmarkExpectations(login)).toEqual({
      skipLink: false,
      mainNav: false,
      breadcrumb: false,
      mainId: null,
    });
  });

  it("requires authenticated shell landmarks for workspace screens", () => {
    const dashboard = getMainScreenById("dashboard");
    expect(shellLandmarkExpectations(dashboard)).toEqual({
      skipLink: true,
      mainNav: true,
      breadcrumb: true,
      mainId: MAIN_CONTENT_ID,
    });
    expect(SKIP_TO_CONTENT_LABEL).toBe("Skip to content");
    expect(MAIN_NAV_ARIA_LABEL).toBe("Main navigation");
    expect(BREADCRUMB_NAV_ARIA_LABEL).toBe("Breadcrumb");
  });

  it("requires primary labeled content on each main screen", () => {
    for (const screen of MAIN_SCREENS) {
      expect(screen.primaryContent.length).toBeGreaterThan(0);
      expect(screen.path.startsWith("/")).toBe(true);
      expect(screen.pageHeading.length).toBeGreaterThan(0);
    }
    expect(getMainScreenById("customers").primaryContent.some((c) => c.kind === "form")).toBe(
      true,
    );
    expect(getMainScreenById("campaigns").contentHeading).toBe("Campaign worklist");
  });

  it("evaluates basic a11y snapshots for shell vs login rules", () => {
    const loginPass = evaluateBasicA11ySnapshot({
      screenId: "login",
      hasPageHeading: true,
      hasMainLandmark: true,
      hasSkipLink: false,
      hasMainNavigation: false,
      hasBreadcrumb: false,
      primaryContentLabeled: true,
      formControlsLabeled: true,
    });
    expect(loginPass.passed).toBe(true);

    const shellFail = evaluateBasicA11ySnapshot({
      screenId: "dashboard",
      hasPageHeading: true,
      hasMainLandmark: true,
      hasSkipLink: false,
      hasMainNavigation: true,
      hasBreadcrumb: true,
      primaryContentLabeled: true,
      formControlsLabeled: true,
    });
    expect(shellFail.passed).toBe(false);
    expect(shellFail.failedChecks).toContain("skip-link");

    const unlabeledForm = evaluateBasicA11ySnapshot({
      screenId: "products",
      hasPageHeading: true,
      hasMainLandmark: true,
      hasSkipLink: true,
      hasMainNavigation: true,
      hasBreadcrumb: true,
      primaryContentLabeled: true,
      formControlsLabeled: false,
    });
    expect(unlabeledForm.failedChecks).toContain("form-control-labels");
  });

  it("keeps accessible color pairs at WCAG AA thresholds", () => {
    expect(allAccessibleColorPairsPass()).toBe(true);
    for (const pair of ACCESSIBLE_COLOR_PAIRS) {
      expect(colorPairMeetsContrast(pair), pair.label).toBe(true);
      expect(contrastRatio(pair.foreground, pair.background)).toBeGreaterThanOrEqual(
        pair.minRatio,
      );
    }
    expect(WCAG_AA_NORMAL_TEXT_RATIO).toBe(4.5);
  });

  it("requires focus-visible outline tokens for interactive controls", () => {
    expect(KEYBOARD_FOCUS_VISIBLE_SELECTORS).toContain("input:focus-visible");
    expect(KEYBOARD_FOCUS_VISIBLE_SELECTORS).toContain("button:focus-visible");
    expect(KEYBOARD_FOCUS_OUTLINE_TOKEN).toContain("3px solid");
  });
});
