import { expect, test } from "@playwright/test";
import {
  CAMPAIGN_BUILDER_PAGE_TITLE,
  CAMPAIGN_CREATE_DRAFT_LABEL,
  CAMPAIGN_CREATION_FIXTURES,
  CAMPAIGN_DRAFT_CREATED_NOTICE,
  campaignFormValidationMessages,
} from "../../src/features/campaigns/campaignCreationFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 603 — Campaign creation works through UI (Playwright).
 *
 * Creates a DRAFT via the multi-step Campaign Builder (FR-050 / FR-057).
 */
test.describe("Campaign creation through UI (item 603)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
    await loginAsHappyPathAdmin(page);
  });

  test("creates a draft campaign from the Campaign Builder", async ({ page }) => {
    test.setTimeout(90_000);
    await page.getByRole("link", { name: "Builder" }).click();
    await expect(
      page.getByRole("heading", { name: CAMPAIGN_BUILDER_PAGE_TITLE, level: 2 }),
    ).toBeVisible();

    await page.getByLabel("Campaign name").fill(CAMPAIGN_CREATION_FIXTURES.name);
    await page.getByLabel("Campaign objective").fill(CAMPAIGN_CREATION_FIXTURES.objective);
    await page.getByRole("button", { name: "Continue to audience" }).click();

    await page.getByLabel("Campaign audience segment").selectOption({
      label: CAMPAIGN_CREATION_FIXTURES.segmentName,
    });
    await page.getByLabel("Campaign product").selectOption({
      label: CAMPAIGN_CREATION_FIXTURES.productName,
    });
    await page.getByRole("button", { name: "Continue to message" }).click();

    await page.getByLabel("Message subject").fill(CAMPAIGN_CREATION_FIXTURES.messageSubject);
    await page.getByLabel("Campaign message body").fill(CAMPAIGN_CREATION_FIXTURES.messageBody);
    await page.getByRole("button", { name: "Continue to schedule" }).click();

    await page.getByLabel("Campaign schedule date").fill(CAMPAIGN_CREATION_FIXTURES.startDate);
    await page.getByLabel("Campaign end date").fill(CAMPAIGN_CREATION_FIXTURES.endDate);
    await page.getByRole("button", { name: "Continue to review" }).click();

    await page.getByRole("button", { name: CAMPAIGN_CREATE_DRAFT_LABEL }).click();
    await expect(page.getByText(CAMPAIGN_DRAFT_CREATED_NOTICE)).toBeVisible();
    await expect(page.getByLabel("Campaign status: Draft").first()).toBeVisible();
  });

  test("validates basics before advancing", async ({ page }) => {
    await page.getByRole("link", { name: "Builder" }).click();
    await page.getByRole("button", { name: "Continue to audience" }).click();

    await expect(page.getByText(campaignFormValidationMessages.nameRequired)).toBeVisible();
    await expect(page.getByText(campaignFormValidationMessages.objectiveRequired)).toBeVisible();
    await expect(page.getByText(CAMPAIGN_DRAFT_CREATED_NOTICE)).toHaveCount(0);
  });
});
