import { expect, test } from "@playwright/test";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Lightweight shell smoke (companion to item 597 full happy-path).
 * Confirms authenticated navigation lands on the operational dashboard.
 */
test("dashboard loads after login", async ({ page }) => {
  await installHappyPathApiMock(page);
  await loginAsHappyPathAdmin(page);

  await expect(page.getByText("Bayer-Westphalian Campaign Management Platform")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Dashboard", level: 2 })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Campaign performance" })).toBeVisible();
});
