import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ANALYTICS_MODULE_DOC_PATH,
  BACKEND_CRITICAL_TEST_CLASS,
  COMMUNICATION_TRACKING_DOC_PATH,
  COMPANION_ENGAGEMENT_TEST_CLASS,
  CONTACT_EVENTS_UPDATE_ANALYTICS_FR,
  CONTACT_EVENTS_UPDATE_ANALYTICS_ITEM,
  CONTACT_EVENTS_UPDATE_ANALYTICS_RULES,
  CONTACT_EVENTS_UPDATE_ANALYTICS_STATEMENT,
  CONVERSION_CONTACT_OUTCOME,
  KPI_DEFINITIONS_DOC_PATH,
  METRIC_UPDATING_CONTACT_EVENT_TYPES,
  PIPELINE_STEPS,
  calculateClickRate,
  calculateConversionRate,
  calculateOpenRate,
  dashboardReflectsContactEventMetrics,
  isMetricUpdatingContactEventType,
  metricsFieldUpdatedByContactEvent,
} from "@/features/analytics/contactEventsUpdateAnalytics";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("contactEventsUpdateAnalytics (item 656)", () => {
  it("locks the critical KB rule identity", () => {
    expect(CONTACT_EVENTS_UPDATE_ANALYTICS_ITEM).toBe(656);
    expect(CONTACT_EVENTS_UPDATE_ANALYTICS_STATEMENT).toBe("Contact events update analytics");
    expect(CONTACT_EVENTS_UPDATE_ANALYTICS_RULES).toEqual(["BR-034"]);
    expect(CONTACT_EVENTS_UPDATE_ANALYTICS_FR).toEqual([
      "FR-103",
      "FR-104",
      "FR-105",
      "FR-106",
    ]);
    expect(METRIC_UPDATING_CONTACT_EVENT_TYPES).toEqual([
      "SENT",
      "OPENED",
      "CLICKED",
      "REPLIED",
    ]);
    expect(CONVERSION_CONTACT_OUTCOME).toBe("CONVERTED");
    expect(PIPELINE_STEPS).toEqual([
      "record-contact-event",
      "update-campaign-metrics",
      "aggregate-analytics-dashboard",
    ]);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("ContactEventsUpdateAnalyticsTests");
    expect(COMPANION_ENGAGEMENT_TEST_CLASS).toContain(
      "EngagementCountsUpdateFromContactEventsTests",
    );
  });

  it("maps contact event types to campaign_metrics fields", () => {
    expect(metricsFieldUpdatedByContactEvent({ eventType: "OPENED" })).toEqual(["openedCount"]);
    expect(metricsFieldUpdatedByContactEvent({ eventType: "CLICKED" })).toEqual(["clickedCount"]);
    expect(metricsFieldUpdatedByContactEvent({ eventType: "REPLIED" })).toEqual(["repliedCount"]);
    expect(metricsFieldUpdatedByContactEvent({ eventType: "SENT" })).toEqual(["sentCount"]);
    expect(
      metricsFieldUpdatedByContactEvent({
        eventType: "CALLED",
        outcome: "CONVERTED",
      }),
    ).toEqual(["convertedCount"]);
    expect(isMetricUpdatingContactEventType("OPENED")).toBe(true);
    expect(isMetricUpdatingContactEventType("FAILED")).toBe(false);
  });

  it("computes dashboard rates from contact-event-fed metrics", () => {
    expect(calculateOpenRate(2, 10)).toBeCloseTo(0.2);
    expect(calculateClickRate(1, 10)).toBeCloseTo(0.1);
    expect(calculateConversionRate(1, 10)).toBeCloseTo(0.1);
    expect(calculateOpenRate(1, 0)).toBe(0);
    expect(
      dashboardReflectsContactEventMetrics({
        messagesSent: 10,
        openedCount: 2,
        clickedCount: 1,
        convertedCount: 1,
      }),
    ).toBe(true);
  });

  it("documents BR-034 contact-event → metrics → analytics pipeline", () => {
    const communication = readRepoFile(COMMUNICATION_TRACKING_DOC_PATH);
    expect(existsSync(path.join(repoRoot, COMMUNICATION_TRACKING_DOC_PATH))).toBe(true);
    expect(communication).toContain("656");
    expect(communication).toContain("ContactEventsUpdateAnalyticsTests");
    expect(communication).toMatch(/BR-034|campaign_metrics|contact event/i);

    const analytics = readRepoFile(ANALYTICS_MODULE_DOC_PATH);
    expect(existsSync(path.join(repoRoot, ANALYTICS_MODULE_DOC_PATH))).toBe(true);
    expect(analytics).toMatch(/metric|contact|dashboard|BR-034/i);

    const kpis = readRepoFile(KPI_DEFINITIONS_DOC_PATH);
    expect(existsSync(path.join(repoRoot, KPI_DEFINITIONS_DOC_PATH))).toBe(true);
    expect(kpis).toContain("BR-034");
  });
});
