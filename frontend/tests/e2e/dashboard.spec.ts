import { expect, test } from "@playwright/test";

test("dashboard loads", async ({ page }) => {
  await page.addInitScript(() => {
    const payload = btoa(JSON.stringify({ roles: ["ADMIN"] }))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/g, "");

    sessionStorage.setItem("bwc.accessToken", `header.${payload}.signature`);
    sessionStorage.setItem("bwc.refreshToken", "refresh-token");
    sessionStorage.setItem(
      "bwc.currentUser",
      JSON.stringify({
        id: "10000000-0000-0000-0000-000000009901",
        email: "admin@bayer-westphalian.test",
        fullName: "Admin User",
        status: "ACTIVE",
        lastLoginAt: "2026-07-04T12:00:00Z",
      }),
    );
  });

  await page.goto("/dashboard");
  await expect(
    page.getByRole("heading", { name: "Bayer-Westphalian Campaign Management Platform" }),
  ).toBeVisible();
  await expect(page.getByRole("heading", { name: "Campaign performance" })).toBeVisible();
});
