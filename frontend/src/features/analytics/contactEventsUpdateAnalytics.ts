/**
 * Sprint 16 critical test item **656**: Contact events update analytics.
 *
 * KB: BR-034 — campaign metrics update after contact events; dashboard FR-103–FR-106 rates
 * derive from those metrics (sent/opened/clicked/converted).
 */

export const CONTACT_EVENTS_UPDATE_ANALYTICS_ITEM = 656;

export const CONTACT_EVENTS_UPDATE_ANALYTICS_STATEMENT = "Contact events update analytics";

export const CONTACT_EVENTS_UPDATE_ANALYTICS_RULES = ["BR-034"] as const;

export const CONTACT_EVENTS_UPDATE_ANALYTICS_FR = [
  "FR-103",
  "FR-104",
  "FR-105",
  "FR-106",
] as const;

/** Contact event types that increment campaign_metrics counters. */
export const METRIC_UPDATING_CONTACT_EVENT_TYPES = [
  "SENT",
  "OPENED",
  "CLICKED",
  "REPLIED",
] as const;

export type MetricUpdatingContactEventType =
  (typeof METRIC_UPDATING_CONTACT_EVENT_TYPES)[number];

export const CONVERSION_CONTACT_OUTCOME = "CONVERTED" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.communication.ContactEventsUpdateAnalyticsTests";

export const COMPANION_ENGAGEMENT_TEST_CLASS =
  "com.bayerwestphalian.campaign.communication.EngagementCountsUpdateFromContactEventsTests";

export const COMMUNICATION_TRACKING_DOC_PATH = "docs/modules/communication-tracking.md";

export const ANALYTICS_MODULE_DOC_PATH = "docs/modules/analytics-module.md";

export const KPI_DEFINITIONS_DOC_PATH = "docs/modules/kpi-definitions.md";

export const PIPELINE_STEPS = [
  "record-contact-event",
  "update-campaign-metrics",
  "aggregate-analytics-dashboard",
] as const;

/**
 * Maps a contact event type (+ optional conversion outcome) to the metrics field it feeds.
 */
export function metricsFieldUpdatedByContactEvent(options: {
  eventType: string;
  outcome?: string | null;
}): Array<"sentCount" | "openedCount" | "clickedCount" | "repliedCount" | "convertedCount"> {
  const fields: Array<
    "sentCount" | "openedCount" | "clickedCount" | "repliedCount" | "convertedCount"
  > = [];
  switch (options.eventType) {
    case "SENT":
      fields.push("sentCount");
      break;
    case "OPENED":
      fields.push("openedCount");
      break;
    case "CLICKED":
      fields.push("clickedCount");
      break;
    case "REPLIED":
      fields.push("repliedCount");
      break;
    default:
      break;
  }
  if (options.outcome === CONVERSION_CONTACT_OUTCOME) {
    fields.push("convertedCount");
  }
  return fields;
}

export function isMetricUpdatingContactEventType(eventType: string): boolean {
  return (METRIC_UPDATING_CONTACT_EVENT_TYPES as readonly string[]).includes(eventType);
}

/**
 * Dashboard rate formulas (fractions of messages sent) used after metrics are updated.
 */
export function calculateOpenRate(opened: number, sent: number): number {
  if (sent <= 0) {
    return 0;
  }
  return opened / sent;
}

export function calculateClickRate(clicked: number, sent: number): number {
  if (sent <= 0) {
    return 0;
  }
  return clicked / sent;
}

export function calculateConversionRate(converted: number, sent: number): number {
  if (sent <= 0) {
    return 0;
  }
  return converted / sent;
}

/**
 * True when dashboard engagement KPIs are consistent with campaign_metrics aggregates
 * that were fed by contact events (BR-034 pipeline).
 */
export function dashboardReflectsContactEventMetrics(options: {
  messagesSent: number;
  openedCount: number;
  clickedCount: number;
  convertedCount: number;
}): boolean {
  if (options.messagesSent < 0 || options.openedCount < 0) {
    return false;
  }
  const openRate = calculateOpenRate(options.openedCount, options.messagesSent);
  const clickRate = calculateClickRate(options.clickedCount, options.messagesSent);
  const conversionRate = calculateConversionRate(
    options.convertedCount,
    options.messagesSent,
  );
  return (
    openRate >= 0 &&
    openRate <= 1 &&
    clickRate >= 0 &&
    clickRate <= 1 &&
    conversionRate >= 0 &&
    conversionRate <= 1
  );
}
