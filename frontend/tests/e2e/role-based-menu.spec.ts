import { expect, test } from "@playwright/test";
import {
  ADMIN_ONLY_MENU_LABELS,
  CAMPAIGN_BUILDER_MENU_LABEL,
  EXPECTED_MENU_LABELS_BY_ROLE,
  MAIN_NAV_ARIA_LABEL,
} from "../../src/features/auth/roleBasedMenu";
import {
  createHappyPathMockState,
  handleHappyPathApiRequest,
  type MockHttpMethod,
} from "../../src/features/e2e/happyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 607 — Role-based menu hides unauthorized features (Playwright).
 */

async function installShellApiMock(page: import("@playwright/test").Page) {
  const state = createHappyPathMockState();
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const response = handleHappyPathApiRequest(state, {
      method: request.method().toUpperCase() as MockHttpMethod,
      url: request.url(),
      bodyText: request.postData() ?? undefined,
    });
    await route.fulfill({
      status: response.status,
      contentType: "application/json",
      body: JSON.stringify(response.body),
    });
  });
}

async function seedSession(
  page: import("@playwright/test").Page,
  roles: string[],
  email: string,
  fullName: string,
) {
  await page.addInitScript(
    ({ roles: roleList, email: userEmail, fullName: name }) => {
      const payload = btoa(JSON.stringify({ roles: roleList }))
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/g, "");
      sessionStorage.setItem("bwc.accessToken", `header.${payload}.signature`);
      sessionStorage.setItem("bwc.refreshToken", "refresh-token");
      sessionStorage.setItem(
        "bwc.currentUser",
        JSON.stringify({
          id: "10000000-0000-0000-0000-000000009907",
          email: userEmail,
          fullName: name,
          status: "ACTIVE",
          lastLoginAt: "2026-07-12T12:00:00Z",
        }),
      );
    },
    { roles, email, fullName },
  );
}

async function navLabels(page: import("@playwright/test").Page): Promise<string[]> {
  const nav = page.getByLabel(MAIN_NAV_ARIA_LABEL);
  const links = nav.getByRole("link");
  const count = await links.count();
  const labels: string[] = [];
  for (let index = 0; index < count; index += 1) {
    labels.push(((await links.nth(index).textContent()) ?? "").trim());
  }
  return labels.filter((label) => label.length > 0);
}

test.describe("Role-based menu hides unauthorized features (item 607)", () => {
  test("admin sees administration links including Users and Settings", async ({ page }) => {
    await installShellApiMock(page);
    await loginAsHappyPathAdmin(page);

    const labels = await navLabels(page);
    expect(labels).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.ADMIN);
    await expect(page.getByRole("link", { name: "Users" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Settings" })).toBeVisible();
    await expect(page.getByRole("link", { name: CAMPAIGN_BUILDER_MENU_LABEL })).toBeVisible();
  });

  test("campaign manager sees builder but not admin-only links", async ({ page }) => {
    await installShellApiMock(page);
    await seedSession(
      page,
      ["CAMPAIGN_MANAGER"],
      "campaign.manager@bayer-westphalian.test",
      "Campaign Manager",
    );
    await page.goto("/dashboard");

    const labels = await navLabels(page);
    expect(labels).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.CAMPAIGN_MANAGER);
    await expect(page.getByRole("link", { name: CAMPAIGN_BUILDER_MENU_LABEL })).toBeVisible();
    for (const label of ADMIN_ONLY_MENU_LABELS) {
      await expect(page.getByRole("link", { name: label })).toHaveCount(0);
    }
  });

  test("BI analyst does not see Builder, Users, or Settings", async ({ page }) => {
    await installShellApiMock(page);
    await seedSession(page, ["BI_ANALYST"], "bi.analyst@bayer-westphalian.test", "BI Analyst");
    await page.goto("/dashboard");

    const labels = await navLabels(page);
    expect(labels).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.BI_ANALYST);
    await expect(page.getByRole("link", { name: "Analytics" })).toBeVisible();
    await expect(page.getByRole("link", { name: CAMPAIGN_BUILDER_MENU_LABEL })).toHaveCount(0);
    for (const label of ADMIN_ONLY_MENU_LABELS) {
      await expect(page.getByRole("link", { name: label })).toHaveCount(0);
    }
  });

  test("marketing analyst only sees insights navigation", async ({ page }) => {
    await installShellApiMock(page);
    await seedSession(
      page,
      ["MARKETING_ANALYST"],
      "marketing.analyst@bayer-westphalian.test",
      "Marketing Analyst",
    );
    await page.goto("/dashboard");

    expect(await navLabels(page)).toEqual(EXPECTED_MENU_LABELS_BY_ROLE.MARKETING_ANALYST);
    await expect(page.getByRole("link", { name: "Campaigns" })).toHaveCount(0);
    await expect(page.getByRole("link", { name: "Customers" })).toHaveCount(0);
  });
});
