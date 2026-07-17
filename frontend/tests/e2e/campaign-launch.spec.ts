import { expect, test } from "@playwright/test";
import {
  CAMPAIGN_LAUNCHED_NOTICE,
  CAMPAIGN_LAUNCH_BUTTON_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_LABEL,
  CAMPAIGN_LAUNCH_CONFIRM_TITLE,
  CAMPAIGN_LAUNCH_FIXTURES,
  CAMPAIGN_LAUNCH_RESULT_HEADING,
  RECIPIENT_PREVIEW_PAGE_TITLE,
  recipientPreviewPath,
} from "../../src/features/campaigns/campaignLaunchFlow";
import {
  createHappyPathMockState,
  E2E_API_ROUTE_PATTERN,
  handleHappyPathApiRequest,
} from "../../src/features/e2e/happyPathApiMock";
import { HAPPY_PATH_ADMIN, HAPPY_PATH_FIXTURES } from "../../src/features/e2e/happyPathFlow";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 605 — Campaign launch works through UI (Playwright).
 *
 * Seeds an APPROVED campaign in the API mock, then launches from recipient preview.
 */
async function installCampaignLaunchApiMock(page: import("@playwright/test").Page) {
  const state = createHappyPathMockState();
  state.campaign = {
    id: CAMPAIGN_LAUNCH_FIXTURES.campaignId,
    name: CAMPAIGN_LAUNCH_FIXTURES.campaignName,
    objective: CAMPAIGN_LAUNCH_FIXTURES.objective,
    status: "APPROVED",
    ownerUserId: HAPPY_PATH_ADMIN.userId,
    ownerFullName: HAPPY_PATH_ADMIN.fullName,
    segmentId: HAPPY_PATH_FIXTURES.segmentId,
    segmentName: CAMPAIGN_LAUNCH_FIXTURES.segmentName,
    channel: CAMPAIGN_LAUNCH_FIXTURES.channel,
    messageSubject: CAMPAIGN_LAUNCH_FIXTURES.messageSubject,
    messageBody: CAMPAIGN_LAUNCH_FIXTURES.messageBody,
    startDate: CAMPAIGN_LAUNCH_FIXTURES.startDate,
    endDate: CAMPAIGN_LAUNCH_FIXTURES.endDate,
    approvedByUserId: HAPPY_PATH_ADMIN.userId,
    approvedByFullName: HAPPY_PATH_ADMIN.fullName,
    approvedAt: "2026-07-12T12:00:00Z",
    rejectionReason: null,
    complianceReviewNotes: "Ready for launch UI",
    productIds: [HAPPY_PATH_FIXTURES.productId],
    createdAt: "2026-07-12T10:00:00Z",
    updatedAt: "2026-07-12T12:00:00Z",
  };

  await page.route(E2E_API_ROUTE_PATTERN, async (route) => {
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

test.describe("Campaign launch through UI (item 605)", () => {
  test("launches an approved campaign from recipient preview", async ({ page }) => {
    await installCampaignLaunchApiMock(page);
    await loginAsHappyPathAdmin(page);

    await page.goto(recipientPreviewPath(CAMPAIGN_LAUNCH_FIXTURES.campaignId));
    await expect(
      page.getByRole("heading", { name: RECIPIENT_PREVIEW_PAGE_TITLE, level: 2 }),
    ).toBeVisible();
    await expect(page.getByLabel("Launch readiness")).toContainText(/ready|approved/i);

    await page.getByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL }).click();
    await expect(page.getByRole("dialog", { name: CAMPAIGN_LAUNCH_CONFIRM_TITLE })).toBeVisible();
    await page.getByRole("button", { name: CAMPAIGN_LAUNCH_CONFIRM_LABEL }).click();

    await expect(page.getByTestId("campaign-launch-notice")).toHaveText(CAMPAIGN_LAUNCHED_NOTICE);
    await expect(
      page.getByRole("heading", { name: CAMPAIGN_LAUNCH_RESULT_HEADING }),
    ).toBeVisible();
  });

  test("keeps launch disabled for non-approved campaign status", async ({ page }) => {
    const state = await installCampaignLaunchApiMock(page);
    if (state.campaign != null) {
      state.campaign.status = "SUBMITTED";
    }
    await loginAsHappyPathAdmin(page);

    await page.goto(recipientPreviewPath(CAMPAIGN_LAUNCH_FIXTURES.campaignId));
    await expect(page.getByRole("button", { name: CAMPAIGN_LAUNCH_BUTTON_LABEL })).toBeDisabled();
    await expect(page.getByLabel("Launch readiness")).toContainText(/SUBMITTED|APPROVED/);
  });
});
