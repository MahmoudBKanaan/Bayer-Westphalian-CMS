import { expect, test } from "@playwright/test";

test("dashboard loads", async ({ page }) => {
  await page.goto("/dashboard");
  await expect(page.getByRole("heading", { name: "Campaign operations workspace" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Campaign performance" })).toBeVisible();
});
