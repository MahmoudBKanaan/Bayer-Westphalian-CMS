import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  contactEventTypes,
  emptyContactTimelineFilters,
  formatContactEnum,
  listContactTimeline,
  type ContactEventView,
  type ContactTimelineFilters,
} from "@/api/contactEvents";
import { StatusBadge } from "@/components/StatusBadge";

export function ContactHistoryPage() {
  const [draftFilters, setDraftFilters] = useState<ContactTimelineFilters>(
    emptyContactTimelineFilters,
  );
  const [appliedFilters, setAppliedFilters] = useState<ContactTimelineFilters>(
    emptyContactTimelineFilters,
  );

  const timelineQuery = useQuery({
    queryKey: ["contact-timeline", appliedFilters],
    queryFn: () => listContactTimeline(appliedFilters),
  });
  const events = timelineQuery.data ?? [];
  const errorMessage = timelineQuery.isError
    ? isAuthorizationError(timelineQuery.error)
      ? "You are not authorized to view contact history."
      : "Contact history could not be loaded."
    : "";

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Contact history</h2>
          <span>
            {timelineQuery.isLoading
              ? "Loading contact timeline"
              : formatCount(events.length, "contact event")}
          </span>
        </div>
        <ContactTimelineFiltersPanel
          draftFilters={draftFilters}
          onDraftChange={setDraftFilters}
          onApply={() => setAppliedFilters(normalizeFilters(draftFilters))}
          onReset={() => {
            setDraftFilters(emptyContactTimelineFilters);
            setAppliedFilters(emptyContactTimelineFilters);
          }}
        />
      </div>

      <div className="panel">
        <div className="section-heading">
          <h2>Timeline</h2>
          <span>Customer, campaign, provider, and outcome events</span>
        </div>
        {timelineQuery.isLoading ? (
          <p className="table-state">Loading contact history entries.</p>
        ) : null}
        {errorMessage ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
        {!timelineQuery.isLoading && !timelineQuery.isError && events.length === 0 ? (
          <p className="table-state">No contact history entries match the current filters.</p>
        ) : null}
        {!timelineQuery.isLoading && !timelineQuery.isError && events.length > 0 ? (
          <table aria-label="Contact history table">
            <thead>
              <tr>
                <th>Event</th>
                <th>Customer</th>
                <th>Campaign</th>
                <th>Channel</th>
                <th>Occurred</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <ContactTimelineRow key={event.id} event={event} />
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}

function ContactTimelineFiltersPanel({
  draftFilters,
  onDraftChange,
  onApply,
  onReset,
}: {
  draftFilters: ContactTimelineFilters;
  onDraftChange: (filters: ContactTimelineFilters) => void;
  onApply: () => void;
  onReset: () => void;
}) {
  return (
    <div className="form-grid">
      <label>
        Customer ID
        <input
          value={draftFilters.customerId}
          onChange={(event) => onDraftChange({ ...draftFilters, customerId: event.target.value })}
          placeholder="Filter by customer UUID"
        />
      </label>
      <label>
        Campaign ID
        <input
          value={draftFilters.campaignId}
          onChange={(event) => onDraftChange({ ...draftFilters, campaignId: event.target.value })}
          placeholder="Filter by campaign UUID"
        />
      </label>
      <label>
        Event type
        <select
          value={draftFilters.eventType}
          onChange={(event) =>
            onDraftChange({
              ...draftFilters,
              eventType: event.target.value as ContactTimelineFilters["eventType"],
            })
          }
        >
          <option value="ALL">All event types</option>
          {contactEventTypes.map((eventType) => (
            <option key={eventType} value={eventType}>
              {formatContactEnum(eventType)}
            </option>
          ))}
        </select>
      </label>
      <div className="form-actions">
        <button type="button" onClick={onApply}>
          Apply filters
        </button>
        <button type="button" className="secondary-button" onClick={onReset}>
          Reset
        </button>
      </div>
    </div>
  );
}

function ContactTimelineRow({ event }: { event: ContactEventView }) {
  return (
    <tr>
      <td>
        <StatusBadge value={formatContactEnum(event.eventType)} />
        {event.outcome == null ? null : (
          <span className="table-secondary-text">{formatContactEnum(event.outcome)}</span>
        )}
      </td>
      <td>
        <span className="table-primary-text">{event.customerFullName ?? "Unknown customer"}</span>
        <span className="table-secondary-text">{event.customerId ?? "No customer id"}</span>
      </td>
      <td>
        <span className="table-primary-text">{event.campaignName ?? "No campaign"}</span>
        <span className="table-secondary-text">{event.campaignId ?? "No campaign id"}</span>
      </td>
      <td>{formatContactEnum(event.channel)}</td>
      <td>{formatDateTime(event.occurredAt)}</td>
      <td>
        <span className="table-primary-text">
          {event.notes == null || event.notes.trim().length === 0 ? "No notes" : event.notes}
        </span>
        <span className="table-secondary-text">
          {event.createdByFullName ?? event.createdByUserId ?? "System or provider"}
        </span>
      </td>
    </tr>
  );
}

function normalizeFilters(filters: ContactTimelineFilters): ContactTimelineFilters {
  return {
    customerId: filters.customerId.trim(),
    campaignId: filters.campaignId.trim(),
    eventType: filters.eventType,
  };
}

function formatDateTime(value: string | null) {
  if (value == null) {
    return "Not available";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatCount(count: number, noun: string) {
  return `${count} ${count === 1 ? noun : `${noun}s`}`;
}
