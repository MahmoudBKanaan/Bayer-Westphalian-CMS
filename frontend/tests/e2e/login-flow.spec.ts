import { expect, test } from "@playwright/test";
import {
  LOGIN_AUTH_REQUIRED_NOTICE,
  LOGIN_INVALID_CREDENTIALS_MESSAGE,
  LOGIN_PAGE_TITLE,
  LOGIN_PANEL_HEADING,
  loginFormValidationMessages,
} from "../../src/features/auth/loginFlow";
import { HAPPY_PATH_ADMIN } from "../../src/features/e2e/happyPathFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";

/**
 * Item 598 — Login flow works through UI (Playwright).
 *
 * Complements the multi-step happy path (597) with focused sign-in acceptance:
 * form, validation, failure copy, auth-required redirect, and successful landing.
 */
test.describe("Login flow through UI (item 598)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
  });

  test("signs in with valid credentials and opens the dashboard", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: LOGIN_PAGE_TITLE })).toBeVisible();
    await expect(page.getByRole("heading", { name: LOGIN_PANEL_HEADING })).toBeVisible();
    await expect(page.getByRole("form", { name: "Employee sign-in" })).toBeVisible();

    await page.getByLabel("Email").fill(HAPPY_PATH_ADMIN.email);
    await page.getByLabel("Password").fill(HAPPY_PATH_ADMIN.password);
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
    await expect(page.getByLabel("Main navigation")).toBeVisible();
    await expect(page.getByText("Bayer-Westphalian Campaign Management Platform")).toBeVisible();
  });

  test("shows field validation without calling login when credentials are incomplete", async ({
    page,
  }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill("not-an-email");
    await page.getByLabel("Password").fill("short");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByText(loginFormValidationMessages.emailInvalid)).toBeVisible();
    await expect(page.getByText(loginFormValidationMessages.passwordMinLength)).toBeVisible();
    await expect(page.getByRole("heading", { name: LOGIN_PANEL_HEADING })).toBeVisible();
  });

  test("shows a safe error for invalid credentials", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill(HAPPY_PATH_ADMIN.email);
    await page.getByLabel("Password").fill("WrongPassword!999");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByTestId("login-error")).toHaveText(LOGIN_INVALID_CREDENTIALS_MESSAGE);
    await expect(page.getByRole("heading", { name: LOGIN_PANEL_HEADING })).toBeVisible();
    await expect(page.getByLabel("Main navigation")).toHaveCount(0);
  });

  test("redirects protected routes to login and returns after successful sign-in", async ({
    page,
  }) => {
    await page.goto("/campaigns");
    await expect(page.getByRole("heading", { name: LOGIN_PAGE_TITLE })).toBeVisible();
    await expect(page.getByTestId("login-auth-required-notice")).toHaveText(
      LOGIN_AUTH_REQUIRED_NOTICE,
    );

    await page.getByLabel("Email").fill(HAPPY_PATH_ADMIN.email);
    await page.getByLabel("Password").fill(HAPPY_PATH_ADMIN.password);
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByRole("heading", { name: "Campaigns", level: 1 })).toBeVisible();
    await expect(page.getByLabel("Main navigation")).toBeVisible();
  });
});
