import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_FR,
  CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_ITEM,
  CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_RULES,
  CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_STATEMENT,
  ELIGIBILITY_RULES_DOC_PATH,
  MONTHLY_CONTACT_COUNTED_EVENT_TYPES,
  MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE,
  MONTHLY_CONTACT_LIMIT_EXPLANATION,
  MONTHLY_CONTACT_ROLLING_WINDOW_DAYS,
  SYSTEM_SETTINGS_DOC_PATH,
  customerExceedsMonthlyContactLimit,
  isMonthlyContactLimitExclusion,
  monthlyContactLimitPresentation,
  monthlyLimitIsKnownExclusionCode,
} from "@/features/customers/customerCannotExceedMonthlyContactLimit";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("customerCannotExceedMonthlyContactLimit (item 652)", () => {
  it("locks the critical KB rule identity", () => {
    expect(CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_ITEM).toBe(652);
    expect(CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_STATEMENT).toBe(
      "Customer cannot exceed monthly contact limit",
    );
    expect(CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_RULES).toEqual(["BR-011"]);
    expect(CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_FR).toEqual(["FR-092", "FR-056"]);
    expect(MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE).toBe("MONTHLY_CONTACT_LIMIT");
    expect(MONTHLY_CONTACT_LIMIT_EXPLANATION).toContain("monthly marketing contact limit");
    expect(MONTHLY_CONTACT_ROLLING_WINDOW_DAYS).toBe(30);
    expect(MONTHLY_CONTACT_COUNTED_EVENT_TYPES).toEqual(["SENT", "CALLED"]);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "CustomerCannotExceedMonthlyContactLimitTests",
    );
  });

  it("excludes when contacts reach or exceed the configured monthly limit", () => {
    expect(
      customerExceedsMonthlyContactLimit({
        contactsInRollingWindow: 3,
        monthlyContactLimit: 3,
      }),
    ).toBe(true);
    expect(
      customerExceedsMonthlyContactLimit({
        contactsInRollingWindow: 4,
        monthlyContactLimit: 3,
      }),
    ).toBe(true);
    expect(
      customerExceedsMonthlyContactLimit({
        contactsInRollingWindow: 2,
        monthlyContactLimit: 3,
      }),
    ).toBe(false);
    expect(
      customerExceedsMonthlyContactLimit({
        contactsInRollingWindow: 0,
        monthlyContactLimit: 3,
        exclusionReasonCode: "MONTHLY_CONTACT_LIMIT",
      }),
    ).toBe(true);
    expect(isMonthlyContactLimitExclusion("MONTHLY_CONTACT_LIMIT")).toBe(true);
    expect(isMonthlyContactLimitExclusion("DUPLICATE_CAMPAIGN_RECIPIENT")).toBe(false);
    expect(monthlyLimitIsKnownExclusionCode()).toBe(true);
  });

  it("presents monthly limit exclusion with BR-011 hint", () => {
    const presentation = monthlyContactLimitPresentation(5);
    expect(presentation.code).toBe("MONTHLY_CONTACT_LIMIT");
    expect(presentation.title).toMatch(/monthly|contact/i);
    expect(presentation.severity).toBe("warning");
    expect(presentation.ruleHint).toMatch(/BR-011|frequency|contact/i);
    expect(presentation.count).toBe(5);
  });

  it("documents BR-011 / MONTHLY_CONTACT_LIMIT in eligibility and system settings docs", () => {
    const eligibility = readRepoFile(ELIGIBILITY_RULES_DOC_PATH);
    expect(existsSync(path.join(repoRoot, ELIGIBILITY_RULES_DOC_PATH))).toBe(true);
    expect(eligibility).toContain("652");
    expect(eligibility).toContain("CustomerCannotExceedMonthlyContactLimitTests");
    expect(eligibility).toContain("MONTHLY_CONTACT_LIMIT");
    expect(eligibility).toContain("BR-011");
    expect(eligibility).toMatch(/monthly|30|SystemSettings/i);

    const settings = readRepoFile(SYSTEM_SETTINGS_DOC_PATH);
    expect(existsSync(path.join(repoRoot, SYSTEM_SETTINGS_DOC_PATH))).toBe(true);
    expect(settings).toMatch(/monthlyContactLimit|monthly contact|BR-011/i);
  });
});
