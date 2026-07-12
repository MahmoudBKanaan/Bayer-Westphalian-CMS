import { expect, test } from "@playwright/test";
import {
  COMPLIANCE_APPROVE_BUTTON_LABEL,
  COMPLIANCE_APPROVAL_FIXTURES,
  COMPLIANCE_APPROVED_NOTICE,
  COMPLIANCE_QUEUE_TABLE_ARIA_LABEL,
  COMPLIANCE_REVIEW_NOTES_LABEL,
  COMPLIANCE_REVIEW_PAGE_TITLE,
  complianceApprovalConfirmCopy,
} from "../../src/features/campaigns/complianceApprovalFlow";
import {
  createHappyPathMockState,
  handleHappyPathApiRequest,
} from "../../src/features/e2e/happyPathApiMock";
import { HAPPY_PATH_ADMIN, HAPPY_PATH_FIXTURES } from "../../src/features/e2e/happyPathFlow";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 604 — Compliance approval works through UI (Playwright).
 *
 * Seeds a SUBMITTED campaign in the happy-path API mock, then approves it.
 */
async function installComplianceApprovalApiMock(
  page: import("@playwright/test").Page,
) {
  const state = createHappyPathMockState();
  state.campaign = {
    id: COMPLIANCE_APPROVAL_FIXTURES.campaignId,
    name: COMPLIANCE_APPROVAL_FIXTURES.campaignName,
    objective: COMPLIANCE_APPROVAL_FIXTURES.objective,
    status: "SUBMITTED",
    ownerUserId: HAPPY_PATH_ADMIN.userId,
    ownerFullName: COMPLIANCE_APPROVAL_FIXTURES.ownerFullName,
    segmentId: HAPPY_PATH_FIXTURES.segmentId,
    segmentName: COMPLIANCE_APPROVAL_FIXTURES.segmentName,
    channel: COMPLIANCE_APPROVAL_FIXTURES.channel,
    messageSubject: COMPLIANCE_APPROVAL_FIXTURES.messageSubject,
    messageBody: COMPLIANCE_APPROVAL_FIXTURES.messageBody,
    startDate: "2026-09-01",
    endDate: "2026-09-30",
    approvedByUserId: null,
    approvedByFullName: null,
    approvedAt: null,
    rejectionReason: null,
    complianceReviewNotes: null,
    productIds: [HAPPY_PATH_FIXTURES.productId],
    createdAt: "2026-07-12T12:00:00Z",
    updatedAt: "2026-07-12T12:05:00Z",
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const response = handleHappyPathApiRequest(state, {
      method: request.method().toUpperCase() as "GET" | "POST" | "PUT" | "PATCH" | "DELETE",
      url: request.url(),
      bodyText: request.postData() ?? undefined,
    });
    await route.fulfill({
      status: response.status,
      contentType: "application/json",
      body: JSON.stringify(response.body),
    });
  });

  return state;
}

test.describe("Compliance approval through UI (item 604)", () => {
  test("approves a submitted campaign from Compliance review", async ({ page }) => {
    await installComplianceApprovalApiMock(page);
    await loginAsHappyPathAdmin(page);

    await page.getByRole("link", { name: "Compliance" }).click();
    await expect(
      page.getByRole("heading", { name: COMPLIANCE_REVIEW_PAGE_TITLE, level: 2 }),
    ).toBeVisible();

    const queue = page.getByRole("table", { name: COMPLIANCE_QUEUE_TABLE_ARIA_LABEL });
    await expect(queue.getByText(COMPLIANCE_APPROVAL_FIXTURES.campaignName)).toBeVisible();
    await queue.getByRole("button", { name: "Review" }).first().click();

    await page
      .getByLabel(COMPLIANCE_REVIEW_NOTES_LABEL)
      .fill(COMPLIANCE_APPROVAL_FIXTURES.reviewNotes);
    await page.getByRole("button", { name: COMPLIANCE_APPROVE_BUTTON_LABEL }).click();

    const copy = complianceApprovalConfirmCopy("approve");
    await expect(page.getByRole("dialog")).toBeVisible();
    await expect(page.getByText(copy.title)).toBeVisible();
    await page.getByRole("button", { name: copy.label }).click();

    await expect(page.getByTestId("compliance-decision-notice")).toHaveText(
      COMPLIANCE_APPROVED_NOTICE,
    );
  });

  test("shows unauthorized message for non-reviewer roles after login as empty nav path", async ({
    page,
  }) => {
    // Seed session as campaign manager via mock login is admin-only; use storage inject.
    await installComplianceApprovalApiMock(page);
    await page.addInitScript(() => {
      const payload = btoa(JSON.stringify({ roles: ["CAMPAIGN_MANAGER"] }))
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/g, "");
      sessionStorage.setItem("bwc.accessToken", `header.${payload}.signature`);
      sessionStorage.setItem("bwc.refreshToken", "refresh-token");
      sessionStorage.setItem(
        "bwc.currentUser",
        JSON.stringify({
          id: "10000000-0000-0000-0000-000000000101",
          email: "campaign.manager@bayer-westphalian.test",
          fullName: "Campaign Manager",
          status: "ACTIVE",
          lastLoginAt: "2026-07-12T12:00:00Z",
        }),
      );
    });

    await page.goto("/compliance");
    // Campaign managers may open compliance read-only menu entry; review actions require officer/admin.
    // If menu hides Compliance, direct navigation still hits the page.
    await expect(page.getByText(/not authorized to review campaigns/i)).toBeVisible({
      timeout: 15_000,
    });
  });
});
