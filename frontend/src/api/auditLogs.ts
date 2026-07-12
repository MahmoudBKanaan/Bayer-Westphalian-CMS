import { apiRequest } from "@/api/client";

/**
 * Audit log list/detail API client (KB items 532–533 / E22).
 *
 * Backed by {@code GET /api/audit-logs} (optional filters) and
 * {@code GET /api/audit-logs/entity-history}.
 * Writes are intentionally not exposed — audit rows are immutable (COMP-008).
 */

export type AuditLogView = {
  id: string;
  actorUserId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  oldValue: Record<string, unknown> | null;
  newValue: Record<string, unknown> | null;
  ipAddress: string | null;
  createdAt: string | null;
};

/**
 * Server-side list filters (item 533) matching {@code AuditLogSearchCriteria} /
 * {@code GET /api/audit-logs} query params.
 */
export type AuditLogSearchFilters = {
  actorUserId: string;
  action: string;
  entityType: string;
  entityId: string;
  /** {@code datetime-local} value or empty */
  createdFrom: string;
  /** {@code datetime-local} value or empty */
  createdTo: string;
};

export type EntityHistoryQuery = {
  entityType: string;
  entityId: string;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

/** Common audit actions for filter presets (items 520–531). */
export const AUDIT_ACTION_OPTIONS = [
  "CREATE",
  "UPDATE",
  "DELETE",
  "ASSIGN_ROLE",
  "DISABLE_USER",
  "WITHDRAW_CONSENT",
  "OPT_OUT",
  "UPDATE_DO_NOT_CONTACT",
  "SUBMIT",
  "APPROVE",
  "REJECT",
  "LAUNCH",
  "EXPORT_REPORT",
] as const;

/** Common entity types recorded in audit_logs. */
export const AUDIT_ENTITY_TYPE_OPTIONS = [
  "users",
  "customers",
  "consent_records",
  "products",
  "campaigns",
  "report_exports",
  "segments",
] as const;

export const emptyAuditLogSearchFilters: AuditLogSearchFilters = {
  actorUserId: "",
  action: "",
  entityType: "",
  entityId: "",
  createdFrom: "",
  createdTo: "",
};

/**
 * Lists audit log entries newest first, optionally filtered (item 533).
 */
export async function listAuditLogs(
  filters: AuditLogSearchFilters = emptyAuditLogSearchFilters,
): Promise<AuditLogView[]> {
  const response = await apiRequest<ApiResponse<AuditLogView[]>>(
    `/audit-logs${auditLogsQuery(filters)}`,
  );
  return response.data;
}

/**
 * Loads immutable history for a single entity (KB {@code getEntityHistory}).
 */
export async function getEntityHistory(
  query: EntityHistoryQuery,
): Promise<AuditLogView[]> {
  const params = new URLSearchParams({
    entityType: query.entityType.trim(),
    entityId: query.entityId.trim(),
  });
  const response = await apiRequest<ApiResponse<AuditLogView[]>>(
    `/audit-logs/entity-history?${params.toString()}`,
  );
  return response.data;
}

/**
 * Builds the query string for {@code GET /api/audit-logs} filters.
 * Empty filters yield an empty string (unfiltered recent list).
 */
export function auditLogsQuery(filters: AuditLogSearchFilters): string {
  const params = new URLSearchParams();
  appendOptionalParam(params, "actorUserId", filters.actorUserId);
  appendOptionalParam(params, "action", filters.action);
  appendOptionalParam(params, "entityType", filters.entityType);
  appendOptionalParam(params, "entityId", filters.entityId);

  const createdFrom = toInstantQueryValue(filters.createdFrom);
  if (createdFrom != null) {
    params.set("createdFrom", createdFrom);
  }
  const createdTo = toInstantQueryValue(filters.createdTo);
  if (createdTo != null) {
    params.set("createdTo", createdTo);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

/** True when any list filter dimension is set (item 533). */
export function hasActiveAuditFilters(filters: AuditLogSearchFilters): boolean {
  return (
    filters.actorUserId.trim().length > 0 ||
    filters.action.trim().length > 0 ||
    filters.entityType.trim().length > 0 ||
    filters.entityId.trim().length > 0 ||
    filters.createdFrom.trim().length > 0 ||
    filters.createdTo.trim().length > 0
  );
}

/** Human-readable action label for badges and table cells. */
export function formatAuditAction(value: string) {
  return formatAuditToken(value);
}

/** Human-readable entity type label. */
export function formatAuditEntityType(value: string) {
  return formatAuditToken(value);
}

export function formatAuditDateTime(value: string | null) {
  if (value == null || value.trim().length === 0) {
    return "Not available";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function summarizeAuditValue(value: Record<string, unknown> | null, maxEntries = 4) {
  if (value == null) {
    return "";
  }

  return Object.entries(value)
    .slice(0, maxEntries)
    .map(([key, entryValue]) => `${formatAuditKey(key)}: ${formatAuditScalar(entryValue)}`)
    .join(", ");
}

/**
 * Converts a {@code datetime-local} control value to an ISO-8601 instant for the API.
 * Returns null when blank or unparseable.
 */
export function toInstantQueryValue(localDatetime: string): string | null {
  const trimmed = localDatetime.trim();
  if (trimmed.length === 0) {
    return null;
  }
  const parsed = new Date(trimmed);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  return parsed.toISOString();
}

function appendOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}

function formatAuditToken(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatAuditKey(value: string) {
  return value.replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
}

function formatAuditScalar(value: unknown) {
  if (value == null) {
    return "none";
  }
  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  return JSON.stringify(value);
}
