import { expect, test } from "@playwright/test";
import {
  formatHappyPathJourney,
  HAPPY_PATH_FIXTURES,
  happyPathStepIdsInOrder,
} from "../../src/features/e2e/happyPathFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import {
  approveHappyPathCampaign,
  createAndSubmitHappyPathCampaign,
  createHappyPathCustomer,
  launchHappyPathCampaign,
  loginAsHappyPathAdmin,
  recordHappyPathConsent,
} from "./helpers/uiActions";

/**
 * KB Testing Plan / Sprint 15 item 597 — Playwright happy-path E2E.
 *
 * Journey: Login → create customer → consent → campaign → approval → launch
 *
 * Uses a deterministic API mock so the suite validates the React UI workflow
 * without requiring a multi-service backend for CI. Live-backend acceptance
 * remains available via later workflow items (598+).
 */
test.describe("Playwright happy-path E2E (item 597)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
  });

  test("documents the KB journey order", () => {
    expect(happyPathStepIdsInOrder()).toEqual([
      "login",
      "create-customer",
      "consent",
      "campaign",
      "approval",
      "launch",
    ]);
    expect(formatHappyPathJourney()).toContain("Login");
    expect(formatHappyPathJourney()).toContain("Launch campaign");
  });

  test("completes login → customer → consent → campaign → approval → launch", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    await test.step("Login", async () => {
      await loginAsHappyPathAdmin(page);
      await expect(page.getByText("Bayer-Westphalian Campaign Management Platform")).toBeVisible();
      await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
    });

    await test.step("Create customer", async () => {
      await createHappyPathCustomer(page);
    });

    await test.step("Record consent", async () => {
      await recordHappyPathConsent(page);
    });

    await test.step("Create and submit campaign", async () => {
      await createAndSubmitHappyPathCampaign(page);
    });

    await test.step("Compliance approval", async () => {
      await approveHappyPathCampaign(page);
    });

    await test.step("Launch campaign", async () => {
      await launchHappyPathCampaign(page);
      await expect(page.getByText(HAPPY_PATH_FIXTURES.campaignName).first()).toBeVisible();
    });
  });
});
