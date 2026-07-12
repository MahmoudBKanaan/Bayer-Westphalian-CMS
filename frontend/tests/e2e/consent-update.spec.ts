import { expect, test } from "@playwright/test";
import {
  CONSENT_OPT_OUT_FORM_ARIA_LABEL,
  CONSENT_OPT_OUT_NOTICE,
  CONSENT_OPT_OUT_SUBMIT_LABEL,
  CONSENT_RECORD_FORM_ARIA_LABEL,
  CONSENT_RECORD_SUBMIT_LABEL,
  CONSENT_RECORDED_NOTICE,
  CONSENT_SECTION_HEADING,
  CONSENT_UPDATE_FIXTURES,
  consentFormValidationMessages,
} from "../../src/features/customers/consentUpdateFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import {
  createHappyPathCustomer,
  loginAsHappyPathAdmin,
} from "./helpers/uiActions";

/**
 * Item 600 — Consent update works through UI (Playwright).
 *
 * Focused acceptance for FR-018 / BR-004 consent changes on customer details.
 */
test.describe("Consent update through UI (item 600)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
    await loginAsHappyPathAdmin(page);
    await createHappyPathCustomer(page);
  });

  test("records marketing consent from customer details", async ({ page }) => {
    await page
      .getByRole("table", { name: "Customer list table" })
      .getByRole("link", { name: "Details" })
      .first()
      .click();

    await expect(page.getByRole("heading", { name: CONSENT_SECTION_HEADING })).toBeVisible();
    const form = page.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL });
    await form.getByLabel("Purpose").fill(CONSENT_UPDATE_FIXTURES.purpose);
    await form.getByLabel("Source").fill(CONSENT_UPDATE_FIXTURES.source);
    await form.getByRole("button", { name: CONSENT_RECORD_SUBMIT_LABEL }).click();

    await expect(page.getByTestId("consent-update-notice")).toHaveText(CONSENT_RECORDED_NOTICE);
    await expect(page.getByText(CONSENT_UPDATE_FIXTURES.purpose)).toBeVisible();
  });

  test("requires purpose before recording consent", async ({ page }) => {
    await page
      .getByRole("table", { name: "Customer list table" })
      .getByRole("link", { name: "Details" })
      .first()
      .click();

    const form = page.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL });
    await form.getByRole("button", { name: CONSENT_RECORD_SUBMIT_LABEL }).click();

    await expect(page.getByTestId("consent-form-error")).toHaveText(
      consentFormValidationMessages.purposeRequired,
    );
    await expect(page.getByTestId("consent-update-notice")).toHaveCount(0);
  });

  test("marks marketing opt-out from customer details", async ({ page }) => {
    await page
      .getByRole("table", { name: "Customer list table" })
      .getByRole("link", { name: "Details" })
      .first()
      .click();

    const form = page.getByRole("form", { name: CONSENT_OPT_OUT_FORM_ARIA_LABEL });
    await form.getByLabel("Opt-out channel").selectOption("MARKETING_SMS");
    await form.getByLabel("Opt-out source").fill(CONSENT_UPDATE_FIXTURES.optOutSource);
    await form.getByRole("button", { name: CONSENT_OPT_OUT_SUBMIT_LABEL }).click();

    await expect(page.getByTestId("consent-update-notice")).toHaveText(CONSENT_OPT_OUT_NOTICE);
  });
});
