import { describe, expect, it } from "vitest";
import {
  formatHappyPathJourney,
  getHappyPathStep,
  HAPPY_PATH_ADMIN,
  HAPPY_PATH_FIXTURES,
  HAPPY_PATH_STEPS,
  happyPathStepIdsInOrder,
  isValidHappyPathOrder,
  nextHappyPathStep,
  previousHappyPathStep,
  type HappyPathStepId,
} from "@/features/e2e/happyPathFlow";

describe("happyPathFlow (item 597)", () => {
  it("defines the KB E2E chain login → customer → consent → campaign → approval → launch", () => {
    expect(happyPathStepIdsInOrder()).toEqual([
      "login",
      "create-customer",
      "consent",
      "campaign",
      "approval",
      "launch",
    ]);
    expect(formatHappyPathJourney()).toBe(
      "Login → Create customer → Record consent → Create and submit campaign → Compliance approval → Launch campaign",
    );
  });

  it("keeps step indexes contiguous and unique", () => {
    const indexes = HAPPY_PATH_STEPS.map((step) => step.index);
    expect(indexes).toEqual([0, 1, 2, 3, 4, 5]);
    const ids = new Set(HAPPY_PATH_STEPS.map((step) => step.id));
    expect(ids.size).toBe(HAPPY_PATH_STEPS.length);
  });

  it("accepts only the full ordered happy path", () => {
    expect(isValidHappyPathOrder(happyPathStepIdsInOrder())).toBe(true);
    expect(isValidHappyPathOrder(["login", "campaign"] as HappyPathStepId[])).toBe(false);
    expect(
      isValidHappyPathOrder([
        "login",
        "consent",
        "create-customer",
        "campaign",
        "approval",
        "launch",
      ]),
    ).toBe(false);
  });

  it("resolves adjacent steps for the workflow graph", () => {
    expect(previousHappyPathStep("login")).toBeNull();
    expect(nextHappyPathStep("login")?.id).toBe("create-customer");
    expect(nextHappyPathStep("launch")).toBeNull();
    expect(previousHappyPathStep("launch")?.id).toBe("approval");
    expect(getHappyPathStep("approval").route).toBe("/compliance");
  });

  it("pins demo admin credentials and deterministic fixture ids for Playwright mocks", () => {
    expect(HAPPY_PATH_ADMIN.email).toBe("admin@bayer-westphalian.test");
    expect(HAPPY_PATH_ADMIN.password.length).toBeGreaterThanOrEqual(8);
    expect(HAPPY_PATH_FIXTURES.campaignName).toContain("Happy Path");
    expect(HAPPY_PATH_FIXTURES.campaignId).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    );
  });
});
