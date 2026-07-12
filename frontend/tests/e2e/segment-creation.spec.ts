import { expect, test } from "@playwright/test";
import {
  SEGMENT_CREATE_FORM_ARIA_LABEL,
  SEGMENT_CREATE_SECTION_HEADING,
  SEGMENT_CREATE_SUBMIT_LABEL,
  SEGMENT_CREATED_NOTICE,
  SEGMENT_CREATION_FIXTURES,
  SEGMENT_LIST_TABLE_ARIA_LABEL,
  segmentFormValidationMessages,
} from "../../src/features/segments/segmentCreationFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 602 — Segment creation works through UI (Playwright).
 */
test.describe("Segment creation through UI (item 602)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
    await loginAsHappyPathAdmin(page);
  });

  test("creates a segment from the Segments page", async ({ page }) => {
    await page.getByRole("link", { name: "Segments" }).click();
    await expect(page.getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeVisible();

    const form = page.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL });
    await form.getByLabel("Name").fill(SEGMENT_CREATION_FIXTURES.name);
    await form.getByLabel("Description").fill(SEGMENT_CREATION_FIXTURES.description);
    await form.getByLabel("Visibility").selectOption(SEGMENT_CREATION_FIXTURES.visibility);
    await form.getByRole("button", { name: "Add criterion" }).click();
    await form.getByLabel("Field for rule 1").selectOption(SEGMENT_CREATION_FIXTURES.cityCriterionField);
    await form.getByLabel("Value for rule 1").fill(SEGMENT_CREATION_FIXTURES.cityCriterionValue);
    await form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }).click();

    await expect(page.getByText(SEGMENT_CREATED_NOTICE)).toBeVisible();
    await expect(
      page.getByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL }).getByText(
        SEGMENT_CREATION_FIXTURES.name,
      ),
    ).toBeVisible();
  });

  test("shows create validation without posting incomplete data", async ({ page }) => {
    await page.getByRole("link", { name: "Segments" }).click();
    const form = page.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL });
    await form.getByRole("button", { name: "Add criterion" }).click();
    await form.getByLabel("Field for rule 1").selectOption("city");
    await form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }).click();

    await expect(page.getByText(segmentFormValidationMessages.nameRequired)).toBeVisible();
    await expect(page.getByText(segmentFormValidationMessages.criterionValueRequired)).toBeVisible();
    await expect(page.getByText(SEGMENT_CREATED_NOTICE)).toHaveCount(0);
  });
});
