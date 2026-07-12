import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  cancelReminder,
  createExpirationReminder,
  createPaymentReminder,
  emptyReminderFilters,
  emptyReminderForm,
  formatReminderEnum,
  listReminders,
  manuallyTriggerReminderProcessing,
  markReminderSent,
  reminderLevels,
  reminderStatuses,
  sendDueReminders,
  type ReminderFilters,
  type ReminderFormPayload,
  type ReminderScheduleView,
} from "@/api/reminders";
import { ReminderLevelBadge } from "@/components/ReminderLevelBadge";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

export function RemindersPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const canReadReminders = permissions.canReadReminders();
  const canManageReminders = permissions.canManageReminders();
  const canManuallyTriggerReminderProcessing =
    permissions.canManuallyTriggerReminderProcessing();
  const [draftFilters, setDraftFilters] = useState<ReminderFilters>(emptyReminderFilters);
  const [appliedFilters, setAppliedFilters] = useState<ReminderFilters>(emptyReminderFilters);
  const [paymentForm, setPaymentForm] = useState<ReminderFormPayload>(emptyReminderForm);
  const [expirationForm, setExpirationForm] = useState<ReminderFormPayload>({
    ...emptyReminderForm,
    reminderLevel: "YELLOW",
  });
  const [asOfDate, setAsOfDate] = useState("");
  const [notice, setNotice] = useState("");

  const remindersQuery = useQuery({
    queryKey: ["reminders", appliedFilters],
    queryFn: () => listReminders(appliedFilters),
    enabled: canReadReminders,
  });
  const reminders = remindersQuery.data ?? [];
  const summary = useMemo(() => summarizeReminders(reminders), [reminders]);

  const refreshReminders = async () => {
    await queryClient.invalidateQueries({ queryKey: ["reminders"] });
  };

  const createPaymentMutation = useMutation({
    mutationFn: createPaymentReminder,
    onSuccess: async () => {
      setPaymentForm(emptyReminderForm);
      setNotice("Payment reminder scheduled.");
      await refreshReminders();
    },
  });
  const createExpirationMutation = useMutation({
    mutationFn: createExpirationReminder,
    onSuccess: async () => {
      setExpirationForm({ ...emptyReminderForm, reminderLevel: "YELLOW" });
      setNotice("Product-expiration reminder scheduled.");
      await refreshReminders();
    },
  });
  const sendDueMutation = useMutation({
    mutationFn: sendDueReminders,
    onSuccess: async (processedReminders) => {
      setNotice(`${processedReminders.length} due reminders processed.`);
      await refreshReminders();
    },
  });
  const manualTriggerMutation = useMutation({
    mutationFn: manuallyTriggerReminderProcessing,
    onSuccess: async (processedReminders) => {
      setNotice(`${processedReminders.length} reminders processed by manual trigger.`);
      await refreshReminders();
    },
  });
  const markSentMutation = useMutation({
    mutationFn: markReminderSent,
    onSuccess: async () => {
      setNotice("Reminder marked sent.");
      await refreshReminders();
    },
  });
  const cancelMutation = useMutation({
    mutationFn: cancelReminder,
    onSuccess: async () => {
      setNotice("Reminder cancelled.");
      await refreshReminders();
    },
  });

  const errorMessage = reminderErrorMessage(
    remindersQuery.error,
    createPaymentMutation.error,
    createExpirationMutation.error,
    sendDueMutation.error,
    manualTriggerMutation.error,
    markSentMutation.error,
    cancelMutation.error,
  );
  const isBusy =
    createPaymentMutation.isPending ||
    createExpirationMutation.isPending ||
    sendDueMutation.isPending ||
    manualTriggerMutation.isPending ||
    markSentMutation.isPending ||
    cancelMutation.isPending;

  if (!canReadReminders) {
    return (
      <section className="panel">
        <div className="section-heading">
          <h2>Reminders</h2>
          <span>Payment and product-expiration reminder schedules</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view reminders.
        </p>
      </section>
    );
  }

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Reminders</h2>
          <span>
            {remindersQuery.isLoading
              ? "Loading reminder schedules"
              : formatCount(reminders.length, "reminder")}
          </span>
        </div>
        <ReminderFiltersPanel
          draftFilters={draftFilters}
          onDraftChange={setDraftFilters}
          onApply={() => setAppliedFilters(normalizeFilters(draftFilters))}
          onReset={() => {
            setDraftFilters(emptyReminderFilters);
            setAppliedFilters(emptyReminderFilters);
          }}
        />
        {notice ? <p className="table-state">{notice}</p> : null}
        {errorMessage ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
      </div>

      <div className="split-grid">
        <section className="panel">
          <div className="section-heading">
            <h2>Reminder summary</h2>
            <span>Operational counts for the current filter</span>
          </div>
          <div className="metric-grid">
            <ReminderMetric label="Pending" value={summary.pending} />
            <ReminderMetric label="Due" value={summary.due} />
            <ReminderMetric label="Sent" value={summary.sent} />
            <ReminderMetric label="Cancelled" value={summary.cancelled} />
          </div>
        </section>

        <section className="panel">
          <div className="section-heading">
            <h2>Processing</h2>
            <span>Send due reminders and run admin test triggers</span>
          </div>
          {canManageReminders ? (
            <div className="form-grid">
              <label>
                Due on or before
                <input
                  aria-label="Process due reminders as of date"
                  type="date"
                  value={asOfDate}
                  onChange={(event) => setAsOfDate(event.target.value)}
                />
              </label>
              <div className="button-row">
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => {
                    setNotice("");
                    sendDueMutation.mutate(asOfDate);
                  }}
                >
                  Process due reminders
                </button>
                {canManuallyTriggerReminderProcessing ? (
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={isBusy}
                    onClick={() => {
                      setNotice("");
                      manualTriggerMutation.mutate();
                    }}
                  >
                    Manual admin trigger
                  </button>
                ) : null}
              </div>
            </div>
          ) : (
            <p className="table-state">Reminder processing is restricted to campaign managers.</p>
          )}
        </section>
      </div>

      {canManageReminders ? (
        <div className="split-grid">
          <section className="panel" aria-labelledby="payment-reminder-heading">
            <div className="section-heading">
              <h2 id="payment-reminder-heading">Create payment reminder</h2>
              <span>Green, Yellow, and Red payment reminder schedules</span>
            </div>
            <ReminderForm
              formId="payment-reminder-form"
              submitLabel="Schedule payment reminder"
              values={paymentForm}
              disabled={isBusy}
              onChange={setPaymentForm}
              onSubmit={() => {
                setNotice("");
                createPaymentMutation.mutate(paymentForm);
              }}
            />
          </section>

          <section className="panel" aria-labelledby="expiration-reminder-heading">
            <div className="section-heading">
              <h2 id="expiration-reminder-heading">Create product-expiration reminder</h2>
              <span>Reminder schedules for products expiring in campaign windows</span>
            </div>
            <ReminderForm
              formId="expiration-reminder-form"
              submitLabel="Schedule expiration reminder"
              values={expirationForm}
              disabled={isBusy}
              onChange={setExpirationForm}
              onSubmit={() => {
                setNotice("");
                createExpirationMutation.mutate(expirationForm);
              }}
            />
          </section>
        </div>
      ) : null}

      <div className="panel">
        <div className="section-heading">
          <h2>Reminder schedules</h2>
          <span>Customer, product, reminder level, due date, and send status</span>
        </div>
        {remindersQuery.isLoading ? (
          <p className="table-state">Loading reminder schedule records.</p>
        ) : null}
        {!remindersQuery.isLoading && !remindersQuery.isError && reminders.length === 0 ? (
          <p className="table-state">No reminders match the current filters.</p>
        ) : null}
        {!remindersQuery.isLoading && !remindersQuery.isError && reminders.length > 0 ? (
          <table aria-label="Reminders table">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Product</th>
                <th>Type</th>
                <th>Level</th>
                <th>Status</th>
                <th>Scheduled</th>
                <th>Sent</th>
                {canManageReminders ? <th>Actions</th> : null}
              </tr>
            </thead>
            <tbody>
              {reminders.map((reminder) => (
                <ReminderRow
                  key={reminder.id}
                  reminder={reminder}
                  canManageReminders={canManageReminders}
                  isBusy={isBusy}
                  onMarkSent={(id) => {
                    setNotice("");
                    markSentMutation.mutate(id);
                  }}
                  onCancel={(id) => {
                    setNotice("");
                    cancelMutation.mutate(id);
                  }}
                />
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}

function ReminderFiltersPanel({
  draftFilters,
  onDraftChange,
  onApply,
  onReset,
}: {
  draftFilters: ReminderFilters;
  onDraftChange: (filters: ReminderFilters) => void;
  onApply: () => void;
  onReset: () => void;
}) {
  return (
    <div className="form-grid">
      <label>
        Customer ID
        <input
          aria-label="Reminder customer ID filter"
          value={draftFilters.customerId}
          onChange={(event) => onDraftChange({ ...draftFilters, customerId: event.target.value })}
          placeholder="Filter by customer UUID"
        />
      </label>
      <label>
        Status
        <select
          aria-label="Reminder status filter"
          value={draftFilters.status}
          onChange={(event) =>
            onDraftChange({
              ...draftFilters,
              status: event.target.value as ReminderFilters["status"],
            })
          }
        >
          <option value="ALL">All statuses</option>
          {reminderStatuses.map((status) => (
            <option key={status} value={status}>
              {formatReminderEnum(status)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Due on or before
        <input
          aria-label="Reminder due on or before filter"
          type="date"
          value={draftFilters.dueOnOrBefore}
          onChange={(event) =>
            onDraftChange({ ...draftFilters, dueOnOrBefore: event.target.value })
          }
        />
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

function ReminderForm({
  formId,
  submitLabel,
  values,
  disabled,
  onChange,
  onSubmit,
}: {
  formId: string;
  submitLabel: string;
  values: ReminderFormPayload;
  disabled: boolean;
  onChange: (values: ReminderFormPayload) => void;
  onSubmit: () => void;
}) {
  return (
    <form
      id={formId}
      className="form-grid"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
    >
      <label>
        Customer ID
        <input
          aria-label={`${submitLabel} customer ID`}
          value={values.customerId}
          onChange={(event) => onChange({ ...values, customerId: event.target.value })}
          placeholder="Customer UUID"
          required
        />
      </label>
      <label>
        Product ID
        <input
          aria-label={`${submitLabel} product ID`}
          value={values.productId}
          onChange={(event) => onChange({ ...values, productId: event.target.value })}
          placeholder="Product UUID"
          required
        />
      </label>
      <label>
        Level
        <select
          aria-label={`${submitLabel} level`}
          value={values.reminderLevel}
          onChange={(event) =>
            onChange({
              ...values,
              reminderLevel: event.target.value as ReminderFormPayload["reminderLevel"],
            })
          }
        >
          {reminderLevels.map((level) => (
            <option key={level} value={level}>
              {formatReminderEnum(level)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Scheduled date
        <input
          aria-label={`${submitLabel} scheduled date`}
          type="date"
          value={values.scheduledDate}
          onChange={(event) => onChange({ ...values, scheduledDate: event.target.value })}
          required
        />
      </label>
      <div className="form-actions">
        <button type="submit" disabled={disabled}>
          {submitLabel}
        </button>
      </div>
    </form>
  );
}

function ReminderRow({
  reminder,
  canManageReminders,
  isBusy,
  onMarkSent,
  onCancel,
}: {
  reminder: ReminderScheduleView;
  canManageReminders: boolean;
  isBusy: boolean;
  onMarkSent: (id: string) => void;
  onCancel: (id: string) => void;
}) {
  return (
    <tr>
      <td>
        <span className="table-primary-text">{reminder.customerFullName}</span>
        <span className="table-secondary-text">{reminder.customerId}</span>
      </td>
      <td>
        <span className="table-primary-text">{reminder.productName}</span>
        <span className="table-secondary-text">{formatReminderEnum(reminder.productType)}</span>
      </td>
      <td>{formatReminderEnum(reminder.reminderType)}</td>
      <td>
        <ReminderLevelBadge level={reminder.reminderLevel} />
      </td>
      <td>
        <StatusBadge value={formatReminderEnum(reminder.status)} />
        {reminder.due ? <span className="table-secondary-text">Due now</span> : null}
      </td>
      <td>{formatDate(reminder.scheduledDate)}</td>
      <td>{formatDateTime(reminder.sentAt)}</td>
      {canManageReminders ? (
        <td>
          <div className="button-row">
            <button
              type="button"
              disabled={isBusy || reminder.status !== "PENDING"}
              onClick={() => onMarkSent(reminder.id)}
            >
              Mark sent
            </button>
            <button
              type="button"
              className="secondary-button"
              disabled={isBusy || reminder.status === "CANCELLED"}
              onClick={() => onCancel(reminder.id)}
            >
              Cancel
            </button>
          </div>
        </td>
      ) : null}
    </tr>
  );
}

function ReminderMetric({ label, value }: { label: string; value: number }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value.toLocaleString()}</strong>
    </div>
  );
}

function normalizeFilters(filters: ReminderFilters): ReminderFilters {
  return {
    customerId: filters.customerId.trim(),
    status: filters.status,
    dueOnOrBefore: filters.dueOnOrBefore,
  };
}

function summarizeReminders(reminders: ReminderScheduleView[]) {
  return reminders.reduce(
    (summary, reminder) => ({
      pending: summary.pending + (reminder.status === "PENDING" ? 1 : 0),
      due: summary.due + (reminder.due ? 1 : 0),
      sent: summary.sent + (reminder.status === "SENT" ? 1 : 0),
      cancelled: summary.cancelled + (reminder.status === "CANCELLED" ? 1 : 0),
    }),
    { pending: 0, due: 0, sent: 0, cancelled: 0 },
  );
}

function formatDate(value: string | null) {
  if (value == null || value.trim().length === 0) {
    return "No date";
  }
  return new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(value));
}

function formatDateTime(value: string | null) {
  if (value == null || value.trim().length === 0) {
    return "Not sent";
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatCount(count: number, singular: string) {
  return `${count.toLocaleString()} ${count === 1 ? singular : `${singular}s`}`;
}

function reminderErrorMessage(...errors: unknown[]) {
  const error = errors.find(Boolean);
  if (error == null) {
    return "";
  }
  if (isAuthorizationError(error)) {
    return "You are not authorized to perform this reminder action.";
  }
  return "Reminder action could not be completed.";
}
