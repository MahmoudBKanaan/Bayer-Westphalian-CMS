import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  getSystemSettings,
  updateSystemSettings,
  type SystemSettingsView,
  type UpdateSystemSettingsPayload,
} from "@/api/systemSettings";
import { usePermissions } from "@/features/auth/usePermissions";

/**
 * System Settings screen (KB item 534).
 *
 * Admin-only configuration of business limits: monthly marketing contact limit, send retry limit,
 * and uninterested exclusion period. Values are persisted via PUT /api/system-settings.
 * Domain services fully consume these runtime values in items 535-537.
 */
export function SystemSettingsPage() {
  const permissions = usePermissions();
  const canManage = permissions.canManageSystemSettings();

  const settingsQuery = useQuery({
    queryKey: ["system-settings"],
    queryFn: getSystemSettings,
    enabled: canManage,
  });

  if (!canManage) {
    return (
      <section className="panel" aria-labelledby="system-settings-heading">
        <div className="section-heading">
          <h2 id="system-settings-heading">System settings</h2>
          <span>Business limits and campaign configuration</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to manage system settings.
        </p>
      </section>
    );
  }

  const loadError = settingsQuery.isError
    ? isAuthorizationError(settingsQuery.error)
      ? "You are not authorized to manage system settings."
      : "System settings could not be loaded."
    : "";

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2 id="system-settings-heading">System settings</h2>
          <span>Admin configuration - contact limits and exclusion rules</span>
        </div>
        <p className="table-secondary-text">
          Configure platform business limits used for marketing contact frequency, delivery retries,
          and how long uninterested customers remain excluded from outreach. Only administrators can
          change these values. Defaults match application configuration (monthly contact 3, retry 3,
          uninterested exclusion 90 days).
        </p>
      </div>

      <div className="panel" aria-labelledby="business-limits-heading">
        <div className="section-heading">
          <h2 id="business-limits-heading">Business limits</h2>
          <span>
            {settingsQuery.isLoading
              ? "Loading settings"
              : settingsQuery.data?.updatedAt != null
                ? `Last updated ${formatDateTime(settingsQuery.data.updatedAt)}`
                : "Contact - retry - uninterested exclusion"}
          </span>
        </div>

        {settingsQuery.isLoading ? (
          <p className="table-state">Loading system settings.</p>
        ) : null}
        {loadError ? (
          <p className="form-error" role="alert">
            {loadError}
          </p>
        ) : null}
        {!settingsQuery.isLoading && !settingsQuery.isError ? (
          <SystemSettingsForm
            key={settingsQuery.data?.updatedAt ?? "defaults"}
            settings={settingsQuery.data}
          />
        ) : null}
      </div>
    </section>
  );
}

function SystemSettingsForm({ settings }: { settings?: SystemSettingsView }) {
  const queryClient = useQueryClient();
  const initialForm = settings == null ? defaultSystemSettingsForm : toForm(settings);
  const [form, setForm] = useState<UpdateSystemSettingsPayload>(initialForm);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState("");

  const updateMutation = useMutation({
    mutationFn: updateSystemSettings,
    onSuccess: async (updated) => {
      setNotice("System settings saved.");
      setForm(toForm(updated));
      setFormErrors({});
      await queryClient.invalidateQueries({ queryKey: ["system-settings"] });
    },
    onError: () => {
      setNotice("");
    },
  });

  const saveError = updateMutation.isError
    ? isAuthorizationError(updateMutation.error)
      ? "You are not authorized to manage system settings."
      : "System settings could not be saved. Check that each limit is within the allowed range."
    : "";

  return (
    <>
      {saveError ? (
        <p className="form-error" role="alert">
          {saveError}
        </p>
      ) : null}
      {notice ? (
        <p className="form-success" role="status">
          {notice}
        </p>
      ) : null}

      <form
        className="form-grid"
        aria-label="System settings form"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          setNotice("");
          const errors = validateForm(form);
          setFormErrors(errors);
          if (Object.keys(errors).length > 0) {
            return;
          }
          updateMutation.mutate(form);
        }}
      >
        <label>
          Monthly marketing contact limit
          <input
            type="number"
            name="monthlyContactLimit"
            min={1}
            max={100}
            required
            value={form.monthlyContactLimit}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                monthlyContactLimit: Number(event.target.value),
              }))
            }
            aria-describedby="monthly-contact-help"
          />
          <span id="monthly-contact-help" className="table-secondary-text">
            Maximum marketing contacts per customer in a rolling 30-day window (BR-011). Range
            1-100.
          </span>
          {formErrors.monthlyContactLimit ? (
            <span className="form-error">{formErrors.monthlyContactLimit}</span>
          ) : null}
        </label>

        <label>
          Send retry limit
          <input
            type="number"
            name="sendRetryLimit"
            min={1}
            max={20}
            required
            value={form.sendRetryLimit}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                sendRetryLimit: Number(event.target.value),
              }))
            }
            aria-describedby="retry-help"
          />
          <span id="retry-help" className="table-secondary-text">
            Maximum delivery attempts before a send is marked failed (item 536). Applied on the next
            outbound email/SMS send. Range 1-20.
          </span>
          {formErrors.sendRetryLimit ? (
            <span className="form-error">{formErrors.sendRetryLimit}</span>
          ) : null}
        </label>

        <label>
          Uninterested exclusion period (days)
          <input
            type="number"
            name="uninterestedExclusionDays"
            min={1}
            max={3650}
            required
            value={form.uninterestedExclusionDays}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                uninterestedExclusionDays: Number(event.target.value),
              }))
            }
            aria-describedby="uninterested-help"
          />
          <span id="uninterested-help" className="table-secondary-text">
            How long uninterested customers stay excluded from campaign outreach after status change
            (item 537). Applied on the next eligibility check. Range 1-3650 days.
          </span>
          {formErrors.uninterestedExclusionDays ? (
            <span className="form-error">{formErrors.uninterestedExclusionDays}</span>
          ) : null}
        </label>

        <div className="form-actions">
          <button type="submit" disabled={updateMutation.isPending}>
            {updateMutation.isPending ? "Saving..." : "Save settings"}
          </button>
          <button
            type="button"
            className="secondary-button"
            disabled={updateMutation.isPending}
            onClick={() => {
              setForm(initialForm);
              setFormErrors({});
              setNotice("");
            }}
          >
            Reset form
          </button>
        </div>
      </form>
    </>
  );
}

const defaultSystemSettingsForm: UpdateSystemSettingsPayload = {
  monthlyContactLimit: 3,
  sendRetryLimit: 3,
  uninterestedExclusionDays: 90,
};

function toForm(settings: SystemSettingsView): UpdateSystemSettingsPayload {
  return {
    monthlyContactLimit: settings.monthlyContactLimit,
    sendRetryLimit: settings.sendRetryLimit,
    uninterestedExclusionDays: settings.uninterestedExclusionDays,
  };
}

function validateForm(form: UpdateSystemSettingsPayload): Record<string, string> {
  const errors: Record<string, string> = {};
  if (
    !Number.isFinite(form.monthlyContactLimit) ||
    form.monthlyContactLimit < 1 ||
    form.monthlyContactLimit > 100
  ) {
    errors.monthlyContactLimit = "Must be between 1 and 100.";
  }
  if (
    !Number.isFinite(form.sendRetryLimit) ||
    form.sendRetryLimit < 1 ||
    form.sendRetryLimit > 20
  ) {
    errors.sendRetryLimit = "Must be between 1 and 20.";
  }
  if (
    !Number.isFinite(form.uninterestedExclusionDays) ||
    form.uninterestedExclusionDays < 1 ||
    form.uninterestedExclusionDays > 3650
  ) {
    errors.uninterestedExclusionDays = "Must be between 1 and 3650.";
  }
  return errors;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
