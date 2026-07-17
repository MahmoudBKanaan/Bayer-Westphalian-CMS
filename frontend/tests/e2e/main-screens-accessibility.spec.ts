import { expect, test } from "@playwright/test";
import {
  BREADCRUMB_NAV_ARIA_LABEL,
  MAIN_CONTENT_ID,
  MAIN_NAV_ARIA_LABEL,
  MAIN_SCREENS,
  SKIP_TO_CONTENT_LABEL,
  type MainScreenDefinition,
} from "../../src/features/a11y/mainScreensAccessibility";
import { LOGIN_FORM_ARIA_LABEL } from "../../src/features/a11y/keyboardNavigationFlow";
import { LOGIN_PAGE_TITLE } from "../../src/features/auth/loginFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 609 — Main screens pass basic accessibility checks (Playwright).
 *
 * Verifies landmarks, page headings, skip link, and labeled primary content
 * on the main business screens without a full axe suite (covered later by 638).
 */

const shellScreens: MainScreenDefinition[] = MAIN_SCREENS.filter((screen) => screen.usesAppShell);

async function assertShellA11y(page: import("@playwright/test").Page, screenDef: MainScreenDefinition) {
  await expect(page.getByRole("heading", { name: screenDef.pageHeading, level: 1 })).toBeVisible();
  await expect(page.locator(`#${MAIN_CONTENT_ID}`)).toBeVisible();
  await expect(page.getByRole("link", { name: SKIP_TO_CONTENT_LABEL })).toHaveAttribute(
    "href",
    `#${MAIN_CONTENT_ID}`,
  );
  await expect(page.getByLabel(MAIN_NAV_ARIA_LABEL)).toBeVisible();
  await expect(page.getByLabel(BREADCRUMB_NAV_ARIA_LABEL)).toBeVisible();
}

async function assertPrimaryContent(
  page: import("@playwright/test").Page,
  screenDef: MainScreenDefinition,
) {
  const primary = screenDef.primaryContent[0];
  if (primary == null) {
    return;
  }
  if (primary.kind === "form") {
    await expect(page.getByRole("form", { name: primary.name })).toBeVisible();
    return;
  }
  if (primary.kind === "table") {
    await expect(page.getByRole("table", { name: primary.name })).toBeVisible();
    return;
  }
  if (primary.kind === "region") {
    await expect(page.getByLabel(primary.name)).toBeVisible();
    return;
  }
  await expect(
    page.getByRole("heading", {
      name: primary.name,
      level: primary.level ?? 2,
      exact: true,
    }),
  ).toBeVisible();
}

test.describe("Main screens basic accessibility (item 609)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
  });

  test("login screen has main landmark, heading, and labeled credentials form", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: LOGIN_PAGE_TITLE, level: 1 })).toBeVisible();
    await expect(page.getByRole("main")).toBeVisible();
    const form = page.getByRole("form", { name: LOGIN_FORM_ARIA_LABEL });
    await expect(form).toBeVisible();
    await expect(form.getByLabel("Email")).toBeVisible();
    await expect(form.getByLabel("Password")).toBeVisible();
    await expect(form.getByRole("button", { name: "Sign in" })).toBeVisible();
  });

  test("dashboard shell landmarks and KPI region load for admin", async ({ page }) => {
    await loginAsHappyPathAdmin(page);
    const dashboard = MAIN_SCREENS.find((screen) => screen.id === "dashboard")!;
    await page.goto(dashboard.path);
    await assertShellA11y(page, dashboard);
    await assertPrimaryContent(page, dashboard);
  });

  test("core workflow screens keep labeled primary content", async ({ page }) => {
    await loginAsHappyPathAdmin(page);

    for (const screenId of ["customers", "products", "segments", "campaigns", "compliance"] as const) {
      const screenDef = shellScreens.find((screen) => screen.id === screenId)!;
      await page.goto(screenDef.path);
      await assertShellA11y(page, screenDef);
      await assertPrimaryContent(page, screenDef);
    }
  });

  test("analytics, reports, audit, and users pass shell landmark checks", async ({ page }) => {
    await loginAsHappyPathAdmin(page);

    for (const screenId of ["analytics", "reports", "audit", "users", "campaign-builder"] as const) {
      const screenDef = shellScreens.find((screen) => screen.id === screenId)!;
      await page.goto(screenDef.path);
      await assertShellA11y(page, screenDef);
      await assertPrimaryContent(page, screenDef);
    }
  });
});
