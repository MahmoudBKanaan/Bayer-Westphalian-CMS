import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import {
  AUDIT_ACTION_OPTIONS,
  AUDIT_ENTITY_TYPE_OPTIONS,
  emptyAuditLogSearchFilters,
  formatAuditAction,
  formatAuditDateTime,
  formatAuditEntityType,
  getEntityHistory,
  hasActiveAuditFilters,
  listAuditLogs,
  summarizeAuditValue,
  type AuditLogSearchFilters,
  type AuditLogView,
} from "@/api/auditLogs";
import { isAuthorizationError } from "@/api/client";
import { AuditActionBadge } from "@/components/AuditActionBadge";
import { usePermissions } from "@/features/auth/usePermissions";

/**
 * Audit Log screen (KB items 532–533 / E22).
 *
 * Read-only sensitive-action history for Admin, Compliance Officer, and System Auditor.
 * Item 533: server-side filters by actor, action, entity type/id, and created-at range.
 * Rows are immutable at the application level (COMP-008).
 */
export function AuditPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const canViewAuditLogs = permissions.canViewAuditLogs();
  const [selectedAuditLogId, setSelectedAuditLogId] = useState("");
  const [draftFilters, setDraftFilters] = useState<AuditLogSearchFilters>(
    emptyAuditLogSearchFilters,
  );
  const [appliedFilters, setAppliedFilters] = useState<AuditLogSearchFilters>(
    emptyAuditLogSearchFilters,
  );

  const auditLogsQuery = useQuery({
    queryKey: ["audit-logs", appliedFilters],
    queryFn: () => listAuditLogs(appliedFilters),
    enabled: canViewAuditLogs,
  });

  const auditLogs = useMemo(() => auditLogsQuery.data ?? [], [auditLogsQuery.data]);
  const filtersActive = hasActiveAuditFilters(appliedFilters);

  const selectedAuditLog = useMemo(() => {
    if (auditLogs.length === 0) {
      return undefined;
    }
    if (selectedAuditLogId === "") {
      return auditLogs[0];
    }
    return auditLogs.find((entry) => entry.id === selectedAuditLogId) ?? auditLogs[0];
  }, [auditLogs, selectedAuditLogId]);

  const entityHistoryEnabled =
    canViewAuditLogs &&
    selectedAuditLog != null &&
    selectedAuditLog.entityType.trim().length > 0 &&
    selectedAuditLog.entityId != null &&
    selectedAuditLog.entityId.trim().length > 0;

  const entityHistoryQuery = useQuery({
    queryKey: [
      "audit-logs",
      "entity-history",
      selectedAuditLog?.entityType ?? "",
      selectedAuditLog?.entityId ?? "",
    ],
    queryFn: () =>
      getEntityHistory({
        entityType: selectedAuditLog!.entityType,
        entityId: selectedAuditLog!.entityId!,
      }),
    enabled: entityHistoryEnabled,
  });

  if (!canViewAuditLogs) {
    return (
      <section className="panel" aria-labelledby="audit-log-heading">
        <div className="section-heading">
          <h2 id="audit-log-heading">Audit log</h2>
          <span>Sensitive system actions</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view audit logs.
        </p>
      </section>
    );
  }

  const errorMessage = auditLogsQuery.isError
    ? isAuthorizationError(auditLogsQuery.error)
      ? "You are not authorized to view audit logs."
      : "Audit logs could not be loaded."
    : "";

  const entityHistoryError =
    entityHistoryQuery.isError && !isAuthorizationError(entityHistoryQuery.error)
      ? "Entity history could not be loaded."
      : entityHistoryQuery.isError && isAuthorizationError(entityHistoryQuery.error)
        ? "You are not authorized to view entity audit history."
        : "";

  const emptyListMessage = filtersActive
    ? "No audit log entries match the current filters."
    : "No audit log entries have been recorded yet.";

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2 id="audit-log-heading">Audit log</h2>
          <span>
            {auditLogsQuery.isLoading
              ? "Loading sensitive actions"
              : formatCount(auditLogs.length, "audit event")}
          </span>
        </div>
        <p className="table-secondary-text">
          Immutable history of sensitive actions for accountability and compliance review. Entries
          cannot be edited or deleted from this screen. Covers consent and opt-out changes, user and
          role activity, product catalog changes, campaign submission/approval/rejection/launch, and
          report exports.
        </p>
        <div className="form-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              void queryClient.invalidateQueries({ queryKey: ["audit-logs"] });
            }}
            disabled={auditLogsQuery.isFetching}
          >
            {auditLogsQuery.isFetching ? "Refreshing…" : "Refresh"}
          </button>
        </div>
      </div>

      <div className="panel" aria-labelledby="audit-filters-heading">
        <div className="section-heading">
          <h2 id="audit-filters-heading">Filters</h2>
          <span>Actor · action · entity · date range</span>
        </div>
        <AuditFiltersPanel
          draftFilters={draftFilters}
          onDraftChange={setDraftFilters}
          onApply={() => {
            setSelectedAuditLogId("");
            setAppliedFilters(normalizeAuditFilters(draftFilters));
          }}
          onReset={() => {
            setDraftFilters(emptyAuditLogSearchFilters);
            setAppliedFilters(emptyAuditLogSearchFilters);
            setSelectedAuditLogId("");
          }}
        />
      </div>

      <div className="split-grid user-management-grid">
        <div className="panel">
          <div className="section-heading">
            <h2>Sensitive actions</h2>
            <span>
              {filtersActive
                ? "Filtered results · newest first · select a row for details"
                : "Newest first · select a row for details"}
            </span>
          </div>
          {auditLogsQuery.isLoading ? (
            <p className="table-state">Loading audit log entries.</p>
          ) : null}
          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}
          {!auditLogsQuery.isLoading && !auditLogsQuery.isError && auditLogs.length === 0 ? (
            <p className="table-state">{emptyListMessage}</p>
          ) : null}
          {!auditLogsQuery.isLoading && !auditLogsQuery.isError && auditLogs.length > 0 ? (
            <div className="table-scroll">
              <table aria-label="Audit log table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Entity</th>
                    <th>Actor</th>
                    <th>Recorded</th>
                    <th>IP address</th>
                    <th>Summary</th>
                  </tr>
                </thead>
                <tbody>
                  {auditLogs.map((auditLog) => {
                    const isSelected = selectedAuditLog?.id === auditLog.id;
                    return (
                      <tr
                        key={auditLog.id}
                        className={isSelected ? "selected-table-row" : undefined}
                        onClick={() => setSelectedAuditLogId(auditLog.id)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault();
                            setSelectedAuditLogId(auditLog.id);
                          }
                        }}
                        tabIndex={0}
                        aria-selected={isSelected}
                        data-testid={`audit-row-${auditLog.id}`}
                      >
                        <td>
                          <AuditActionBadge action={auditLog.action} />
                        </td>
                        <td>
                          <span className="table-primary-text">
                            {formatAuditEntityType(auditLog.entityType)}
                          </span>
                          <span className="table-secondary-text">
                            {auditLog.entityId ?? "No entity id"}
                          </span>
                        </td>
                        <td>
                          <span className="table-primary-text">
                            {auditLog.actorUserId ?? "System or unavailable"}
                          </span>
                        </td>
                        <td>{formatAuditDateTime(auditLog.createdAt)}</td>
                        <td>{auditLog.ipAddress ?? "Not recorded"}</td>
                        <td>
                          <AuditSummary auditLog={auditLog} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>

        <div className="panel" aria-labelledby="audit-detail-heading">
          <div className="section-heading">
            <h2 id="audit-detail-heading">Selected entry</h2>
            <span>Read-only detail · COMP-008 immutable trail</span>
          </div>
          {selectedAuditLog == null ? (
            <p className="table-state">Select an audit log entry to inspect before/after values.</p>
          ) : (
            <AuditLogDetailPanel auditLog={selectedAuditLog} />
          )}
        </div>
      </div>

      {selectedAuditLog != null && entityHistoryEnabled ? (
        <div className="panel" aria-labelledby="entity-history-heading">
          <div className="section-heading">
            <h2 id="entity-history-heading">Entity history</h2>
            <span>
              {formatAuditEntityType(selectedAuditLog.entityType)} · {selectedAuditLog.entityId}
            </span>
          </div>
          {entityHistoryQuery.isLoading ? (
            <p className="table-state">Loading entity history.</p>
          ) : null}
          {entityHistoryError ? (
            <p className="form-error" role="alert">
              {entityHistoryError}
            </p>
          ) : null}
          {!entityHistoryQuery.isLoading &&
          !entityHistoryQuery.isError &&
          (entityHistoryQuery.data?.length ?? 0) === 0 ? (
            <p className="table-state">No history rows for this entity.</p>
          ) : null}
          {!entityHistoryQuery.isLoading &&
          !entityHistoryQuery.isError &&
          (entityHistoryQuery.data?.length ?? 0) > 0 ? (
            <div className="table-scroll">
              <table aria-label="Entity audit history table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Actor</th>
                    <th>Recorded</th>
                    <th>Summary</th>
                  </tr>
                </thead>
                <tbody>
                  {(entityHistoryQuery.data ?? []).map((entry) => (
                    <tr key={entry.id}>
                      <td>
                        <AuditActionBadge action={entry.action} />
                      </td>
                      <td>{entry.actorUserId ?? "System or unavailable"}</td>
                      <td>{formatAuditDateTime(entry.createdAt)}</td>
                      <td>
                        <AuditSummary auditLog={entry} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function AuditFiltersPanel({
  draftFilters,
  onDraftChange,
  onApply,
  onReset,
}: {
  draftFilters: AuditLogSearchFilters;
  onDraftChange: (filters: AuditLogSearchFilters) => void;
  onApply: () => void;
  onReset: () => void;
}) {
  return (
    <form
      className="form-grid"
      aria-label="Audit log filters"
      onSubmit={(event) => {
        event.preventDefault();
        onApply();
      }}
    >
      <label>
        Actor user id
        <input
          value={draftFilters.actorUserId}
          onChange={(event) =>
            onDraftChange({ ...draftFilters, actorUserId: event.target.value })
          }
          placeholder="Filter by actor UUID"
          autoComplete="off"
          name="actorUserId"
        />
      </label>
      <label>
        Action
        <select
          value={draftFilters.action}
          onChange={(event) => onDraftChange({ ...draftFilters, action: event.target.value })}
          name="action"
          aria-label="Action filter"
        >
          <option value="">All actions</option>
          {AUDIT_ACTION_OPTIONS.map((action) => (
            <option key={action} value={action}>
              {formatAuditAction(action)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Entity type
        <select
          value={draftFilters.entityType}
          onChange={(event) =>
            onDraftChange({ ...draftFilters, entityType: event.target.value })
          }
          name="entityType"
          aria-label="Entity type filter"
        >
          <option value="">All entity types</option>
          {AUDIT_ENTITY_TYPE_OPTIONS.map((entityType) => (
            <option key={entityType} value={entityType}>
              {formatAuditEntityType(entityType)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Entity id
        <input
          value={draftFilters.entityId}
          onChange={(event) => onDraftChange({ ...draftFilters, entityId: event.target.value })}
          placeholder="Filter by entity UUID"
          autoComplete="off"
          name="entityId"
        />
      </label>
      <label>
        From (recorded)
        <input
          type="datetime-local"
          value={draftFilters.createdFrom}
          onChange={(event) =>
            onDraftChange({ ...draftFilters, createdFrom: event.target.value })
          }
          name="createdFrom"
          aria-label="Created from filter"
        />
      </label>
      <label>
        To (recorded)
        <input
          type="datetime-local"
          value={draftFilters.createdTo}
          onChange={(event) => onDraftChange({ ...draftFilters, createdTo: event.target.value })}
          name="createdTo"
          aria-label="Created to filter"
        />
      </label>
      <div className="form-actions">
        <button type="submit">Apply filters</button>
        <button type="button" className="secondary-button" onClick={onReset}>
          Reset
        </button>
      </div>
    </form>
  );
}

function normalizeAuditFilters(filters: AuditLogSearchFilters): AuditLogSearchFilters {
  return {
    actorUserId: filters.actorUserId.trim(),
    action: filters.action.trim(),
    entityType: filters.entityType.trim(),
    entityId: filters.entityId.trim(),
    createdFrom: filters.createdFrom.trim(),
    createdTo: filters.createdTo.trim(),
  };
}

function AuditSummary({ auditLog }: { auditLog: AuditLogView }) {
  const summary =
    summarizeAuditValue(auditLog.newValue) || summarizeAuditValue(auditLog.oldValue);

  if (summary.length === 0) {
    return <span>Not provided</span>;
  }

  return <span className="table-secondary-text">{summary}</span>;
}

function AuditLogDetailPanel({ auditLog }: { auditLog: AuditLogView }) {
  return (
    <div className="page-stack">
      <dl className="detail-list" aria-label="Selected audit log fields">
        <div>
          <dt>Action</dt>
          <dd>
            <AuditActionBadge action={auditLog.action} />
          </dd>
        </div>
        <div>
          <dt>Entity type</dt>
          <dd>{formatAuditEntityType(auditLog.entityType)}</dd>
        </div>
        <div>
          <dt>Entity id</dt>
          <dd>{auditLog.entityId ?? "Not provided"}</dd>
        </div>
        <div>
          <dt>Actor user id</dt>
          <dd>{auditLog.actorUserId ?? "System or unavailable"}</dd>
        </div>
        <div>
          <dt>Recorded at</dt>
          <dd>{formatAuditDateTime(auditLog.createdAt)}</dd>
        </div>
        <div>
          <dt>IP address</dt>
          <dd>{auditLog.ipAddress ?? "Not recorded"}</dd>
        </div>
        <div>
          <dt>Audit id</dt>
          <dd>{auditLog.id}</dd>
        </div>
      </dl>

      <section aria-labelledby="audit-old-value-heading">
        <h3 id="audit-old-value-heading" className="table-primary-text">
          Previous value
        </h3>
        <AuditValueBlock value={auditLog.oldValue} emptyLabel="No previous value recorded." />
      </section>

      <section aria-labelledby="audit-new-value-heading">
        <h3 id="audit-new-value-heading" className="table-primary-text">
          New value
        </h3>
        <AuditValueBlock value={auditLog.newValue} emptyLabel="No new value recorded." />
      </section>
    </div>
  );
}

function AuditValueBlock({
  value,
  emptyLabel,
}: {
  value: Record<string, unknown> | null;
  emptyLabel: string;
}) {
  if (value == null || Object.keys(value).length === 0) {
    return <p className="table-state">{emptyLabel}</p>;
  }

  return (
    <pre className="state-panel" aria-label="Audit JSON value">
      {JSON.stringify(value, null, 2)}
    </pre>
  );
}

function formatCount(count: number, noun: string) {
  return `${count} ${count === 1 ? noun : `${noun}s`}`;
}
