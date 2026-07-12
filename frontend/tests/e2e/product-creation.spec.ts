import { expect, test } from "@playwright/test";
import {
  PRODUCT_CREATE_FORM_ARIA_LABEL,
  PRODUCT_CREATE_SECTION_HEADING,
  PRODUCT_CREATE_SUBMIT_LABEL,
  PRODUCT_CREATED_NOTICE,
  PRODUCT_CREATION_FIXTURES,
  PRODUCT_LIST_TABLE_ARIA_LABEL,
  productFormValidationMessages,
} from "../../src/features/products/productCreationFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 601 — Product creation works through UI (Playwright).
 */
test.describe("Product creation through UI (item 601)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
    await loginAsHappyPathAdmin(page);
  });

  test("creates a product from the Products page", async ({ page }) => {
    await page.getByRole("link", { name: "Products" }).click();
    await expect(page.getByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING })).toBeVisible();

    const form = page.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
    await form.getByLabel("Product name").fill(PRODUCT_CREATION_FIXTURES.name);
    await form.getByLabel("Product type").selectOption(PRODUCT_CREATION_FIXTURES.productType);
    await form.getByLabel("Product description").fill(PRODUCT_CREATION_FIXTURES.description);
    await form.getByLabel("Product price").fill(PRODUCT_CREATION_FIXTURES.price);
    await form
      .getByLabel("Product duration in months")
      .fill(PRODUCT_CREATION_FIXTURES.durationMonths);
    await form
      .getByLabel("Product expiration policy")
      .fill(PRODUCT_CREATION_FIXTURES.expirationPolicy);
    await form.getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }).click();

    await expect(page.getByText(PRODUCT_CREATED_NOTICE)).toBeVisible();
    await expect(
      page.getByRole("table", { name: PRODUCT_LIST_TABLE_ARIA_LABEL }).getByText(
        PRODUCT_CREATION_FIXTURES.name,
      ),
    ).toBeVisible();
  });

  test("shows create validation without posting incomplete data", async ({ page }) => {
    await page.getByRole("link", { name: "Products" }).click();
    const form = page.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
    await form.getByLabel("Product price").fill("-9");
    await form.getByLabel("Product duration in months").fill("0");
    await form.getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }).click();

    await expect(page.getByText(productFormValidationMessages.nameRequired)).toBeVisible();
    await expect(page.getByText(productFormValidationMessages.priceInvalid)).toBeVisible();
    await expect(page.getByText(productFormValidationMessages.durationInvalid)).toBeVisible();
    await expect(page.getByText(PRODUCT_CREATED_NOTICE)).toHaveCount(0);
  });
});
