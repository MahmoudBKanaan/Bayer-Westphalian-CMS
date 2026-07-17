import type { Page } from "@playwright/test";
import {
  createHappyPathMockState,
  E2E_API_ROUTE_PATTERN,
  handleHappyPathApiRequest,
  type MockHttpMethod,
} from "../../../src/features/e2e/happyPathApiMock";

/**
 * Installs the item 597 happy-path REST mock on the browser context.
 * Intercepts API routes so the E2E journey does not require a live backend.
 */
export async function installHappyPathApiMock(page: Page) {
  const state = createHappyPathMockState();

  await page.route(E2E_API_ROUTE_PATTERN, async (route) => {
    const request = route.request();
    const method = request.method().toUpperCase() as MockHttpMethod;
    const url = request.url();
    const bodyText = request.postData() ?? undefined;

    const response = handleHappyPathApiRequest(state, {
      method,
      url,
      bodyText,
    });

    await route.fulfill({
      status: response.status,
      contentType: "application/json",
      body: JSON.stringify(response.body),
    });
  });

  return state;
}
