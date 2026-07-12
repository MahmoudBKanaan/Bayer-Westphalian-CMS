import { expect, type Page } from "@playwright/test";
import {
  HAPPY_PATH_ADMIN,
  HAPPY_PATH_FIXTURES,
} from "../../../src/features/e2e/happyPathFlow";

export async function loginAsHappyPathAdmin(page: Page) {
  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
  await expect(page.getByRole("form", { name: "Employee sign-in" })).toBeVisible();
  await page.getByLabel("Email").fill(HAPPY_PATH_ADMIN.email);
  await page.getByLabel("Password").fill(HAPPY_PATH_ADMIN.password);
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("heading", { name: "Dashboard", level: 2 })).toBeVisible();
}

export async function createHappyPathCustomer(page: Page) {
  await page.getByRole("link", { name: "Customers" }).click();
  await expect(page.getByRole("heading", { name: "Create customer" })).toBeVisible();

  const form = page.getByRole("form", { name: "Create customer form" });
  await form.getByLabel("First name").fill(HAPPY_PATH_FIXTURES.customerFirstName);
  await form.getByLabel("Last name").fill(HAPPY_PATH_FIXTURES.customerLastName);
  await form.getByLabel("Email", { exact: true }).fill(HAPPY_PATH_FIXTURES.customerEmail);
  await form.getByLabel("City").fill("Munich");
  await form.getByLabel("Country").fill("Germany");
  await form.getByLabel("Source").fill("E2E");
  await form.getByRole("button", { name: "Create customer" }).click();

  await expect(page.getByText("Customer created.")).toBeVisible();
  await expect(
    page.getByRole("table", { name: "Customer list table" }).getByText(
      `${HAPPY_PATH_FIXTURES.customerFirstName} ${HAPPY_PATH_FIXTURES.customerLastName}`,
    ),
  ).toBeVisible();
}

export async function recordHappyPathConsent(page: Page) {
  await page
    .getByRole("table", { name: "Customer list table" })
    .getByRole("link", { name: "Details" })
    .first()
    .click();

  await expect(page.getByRole("heading", { name: "Customer details" })).toBeVisible();

  const consentForm = page.getByRole("form", { name: "Record consent" });
  await consentForm.getByLabel("Purpose").fill(HAPPY_PATH_FIXTURES.consentPurpose);
  await consentForm.getByLabel("Source").fill(HAPPY_PATH_FIXTURES.consentSource);
  await consentForm.getByRole("button", { name: "Record consent" }).click();

  await expect(page.getByTestId("consent-update-notice")).toHaveText("Consent recorded.");
  await expect(page.getByText(HAPPY_PATH_FIXTURES.consentPurpose)).toBeVisible();
}

export async function createAndSubmitHappyPathCampaign(page: Page) {
  await page.getByRole("link", { name: "Builder" }).click();
  await expect(page.getByRole("heading", { name: "Campaign Builder", level: 2 })).toBeVisible({
    timeout: 15_000,
  });

  await page.getByLabel("Campaign name").fill(HAPPY_PATH_FIXTURES.campaignName);
  await page.getByLabel("Campaign objective").fill(HAPPY_PATH_FIXTURES.campaignObjective);
  await page.getByRole("button", { name: "Continue to audience" }).click();

  await page.getByLabel("Campaign audience segment").selectOption({
    label: HAPPY_PATH_FIXTURES.segmentName,
  });
  await page.getByLabel("Campaign product").selectOption({
    label: HAPPY_PATH_FIXTURES.productName,
  });
  await page.getByRole("button", { name: "Continue to message" }).click();

  await page.getByLabel("Message subject").fill(HAPPY_PATH_FIXTURES.campaignSubject);
  await page.getByLabel("Campaign message body").fill(HAPPY_PATH_FIXTURES.campaignBody);
  await page.getByRole("button", { name: "Continue to schedule" }).click();

  await page.getByLabel("Campaign schedule date").fill("2026-08-01");
  await page.getByLabel("Campaign end date").fill("2026-08-31");
  await page.getByRole("button", { name: "Continue to review" }).click();

  await page.getByRole("button", { name: "Create draft" }).click();
  await expect(page.getByText("Campaign draft created.")).toBeVisible();
  await expect(page.getByLabel("Campaign status: Draft").first()).toBeVisible();

  await page.getByRole("button", { name: "Submit for review" }).click();
  await expect(page.getByText("Campaign submitted for compliance review.")).toBeVisible();
}

export async function approveHappyPathCampaign(page: Page) {
  await page.getByRole("link", { name: "Compliance" }).click();
  await expect(page.getByRole("heading", { name: /Compliance review/i })).toBeVisible();

  const queue = page.getByRole("table", { name: "Submitted campaigns table" });
  await expect(queue.getByText(HAPPY_PATH_FIXTURES.campaignName)).toBeVisible();
  await queue.getByRole("button", { name: "Review" }).first().click();

  await page.getByLabel("Compliance review notes").fill("Approved in Playwright happy-path");
  await page.getByRole("button", { name: "Approve campaign" }).click();
  await page.getByRole("button", { name: "Confirm approval" }).click();
  await expect(page.getByTestId("compliance-decision-notice")).toHaveText("Campaign approved.");
}

export async function launchHappyPathCampaign(page: Page) {
  await page.getByRole("link", { name: "Campaigns", exact: true }).click();
  await expect(
    page.getByRole("table").getByText(HAPPY_PATH_FIXTURES.campaignName),
  ).toBeVisible();

  await page
    .getByRole("row")
    .filter({ hasText: HAPPY_PATH_FIXTURES.campaignName })
    .getByRole("link", { name: "Preview" })
    .click();

  await expect(page.getByRole("heading", { name: "Recipient Preview" })).toBeVisible();
  await expect(page.getByLabel("Launch readiness")).toContainText(/ready|approved/i);

  await page.getByRole("button", { name: "Launch campaign" }).click();
  await page.getByRole("button", { name: "Confirm launch" }).click();
  await expect(page.getByTestId("campaign-launch-notice")).toHaveText("Campaign launched.");
  await expect(page.getByRole("heading", { name: "Launch result" })).toBeVisible();
}
