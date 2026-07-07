import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  listBeneficiaries,
  updateBeneficiary,
  type BeneficiaryView,
  type UpdateBeneficiaryPayload,
} from "@/api/beneficiaries";
import {
  listConsents,
  recordConsent,
  recordOptOut,
  withdrawConsent,
  type ConsentRecordView,
  type ConsentStatus,
  type ConsentType,
  type RecordOptOutPayload,
  type RecordConsentPayload,
} from "@/api/consents";
import { Link, useParams } from "react-router-dom";
import {
  getCustomer,
  updateCustomer,
  type CustomerFormPayload,
  type CustomerView,
} from "@/api/customers";
import { StatusBadge } from "@/components/StatusBadge";

const CONSENT_TYPES: ConsentType[] = [
  "MARKETING_EMAIL",
  "MARKETING_PHONE",
  "MARKETING_SMS",
  "GUARDIAN",
  "DATA_PROCESSING",
];
const CONSENT_STATUSES: ConsentStatus[] = ["GIVEN", "REQUIRED", "WITHDRAWN", "EXPIRED", "REJECTED"];
const MARKETING_CONSENT_TYPES = ["MARKETING_EMAIL", "MARKETING_PHONE", "MARKETING_SMS"] as const;
const emptyConsentForm = {
  consentType: "MARKETING_EMAIL" as ConsentType,
  status: "GIVEN" as ConsentStatus,
  purpose: "",
  source: "",
  evidenceFileUrl: "",
};
const emptyOptOutForm = {
  consentType: "MARKETING_EMAIL" as (typeof MARKETING_CONSENT_TYPES)[number],
  source: "",
  evidenceFileUrl: "",
};

export function CustomerDetailsPage() {
  const { customerId } = useParams();
  const queryClient = useQueryClient();
  const hasCustomerId = customerId != null && customerId.trim().length > 0;
  const refreshConsent = async () => {
    await queryClient.invalidateQueries({ queryKey: ["consents", "customer", customerId] });
  };
  const refreshBeneficiaries = async () => {
    await queryClient.invalidateQueries({
      queryKey: ["beneficiaries", "policyholder", customerId],
    });
  };
  const customerQuery = useQuery({
    queryKey: ["customer", customerId],
    queryFn: () => getCustomer(customerId ?? ""),
    enabled: hasCustomerId,
  });
  const beneficiariesQuery = useQuery({
    queryKey: ["beneficiaries", "policyholder", customerId],
    queryFn: () => listBeneficiaries({ policyholderCustomerId: customerId ?? "" }),
    enabled: hasCustomerId,
  });
  const consentsQuery = useQuery({
    queryKey: ["consents", "customer", customerId],
    queryFn: () => listConsents({ customerId: customerId ?? "" }),
    enabled: hasCustomerId,
  });
  const recordConsentMutation = useMutation({
    mutationFn: recordConsent,
    onSuccess: refreshConsent,
  });
  const recordOptOutMutation = useMutation({
    mutationFn: recordOptOut,
    onSuccess: refreshConsent,
  });
  const withdrawConsentMutation = useMutation({
    mutationFn: withdrawConsent,
    onSuccess: refreshConsent,
  });
  const updateDoNotContactMutation = useMutation({
    mutationFn: (doNotContact: boolean) => {
      const customer = customerQuery.data;
      if (customer == null) {
        throw new Error("Customer profile is required.");
      }

      return updateCustomer(customer.id, {
        ...customerToFormPayload(customer),
        doNotContact,
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["customer", customerId] });
      await refreshConsent();
    },
  });
  const updateGuardianConsentMutation = useMutation({
    mutationFn: ({
      beneficiaryId,
      payload,
    }: {
      beneficiaryId: string;
      payload: UpdateBeneficiaryPayload;
    }) => updateBeneficiary(beneficiaryId, payload),
    onSuccess: refreshBeneficiaries,
  });

  if (customerQuery.isLoading) {
    return <p className="table-state">Loading customer profile.</p>;
  }

  if (customerQuery.isError || customerQuery.data == null) {
    return (
      <section className="page-stack">
        <div className="panel">
          <h2>Customer profile unavailable</h2>
          <p className="form-error" role="alert">
            Customer profile could not be loaded.
          </p>
          <Link className="secondary-link-button" to="/customers">
            Back to customers
          </Link>
        </div>
      </section>
    );
  }

  const customer = customerQuery.data;

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <div>
            <h2>Customer details</h2>
            <span>{customer.fullName}</span>
          </div>
          <Link className="secondary-link-button" to="/customers">
            Back to customers
          </Link>
        </div>
        <div className="detail-summary">
          <div>
            <span className="eyebrow">Type</span>
            <strong>{formatEnum(customer.customerType)}</strong>
          </div>
          <div>
            <span className="eyebrow">Status</span>
            <StatusBadge value={formatEnum(customer.status)} />
          </div>
          <div>
            <span className="eyebrow">Marketing</span>
            <StatusBadge value={customer.doNotContact ? "Do not contact" : "Allowed"} />
          </div>
          <div>
            <span className="eyebrow">Contactable</span>
            <strong>{customer.contactable ? "Yes" : "No"}</strong>
          </div>
        </div>
      </div>

      <div className="split-grid user-management-grid">
        <section className="panel" aria-labelledby="profile-heading">
          <div className="section-heading">
            <h2 id="profile-heading">Profile</h2>
            <span>Core customer/prospect record</span>
          </div>
          <dl className="detail-list">
            <DetailItem label="Customer ID" value={customer.id} />
            <DetailItem label="Full name" value={customer.fullName} />
            <DetailItem label="Email" value={customer.email} />
            <DetailItem label="Phone" value={customer.phone} />
            <DetailItem label="Address" value={customer.addressLine} />
            <DetailItem label="City" value={customer.city} />
            <DetailItem label="Country" value={customer.country} />
            <DetailItem label="Date of birth" value={formatDate(customer.dateOfBirth)} />
            <DetailItem
              label="Age group"
              value={customer.ageGroup == null ? null : formatEnum(customer.ageGroup)}
            />
            <DetailItem label="Source" value={customer.source} />
          </dl>
        </section>

        <section className="panel" aria-labelledby="activity-heading">
          <div className="section-heading">
            <h2 id="activity-heading">Activity</h2>
            <span>Lifecycle timestamps</span>
          </div>
          <dl className="detail-list">
            <DetailItem label="Created" value={formatDateTime(customer.createdAt)} />
            <DetailItem label="Updated" value={formatDateTime(customer.updatedAt)} />
            <DetailItem label="Deleted" value={formatDateTime(customer.deletedAt)} />
          </dl>
        </section>
      </div>

      <div className="split-grid user-management-grid">
        <ConsentTab
          customer={customer}
          consents={consentsQuery.data ?? []}
          loading={consentsQuery.isLoading}
          error={consentsQuery.isError}
          recordConsent={(payload) => recordConsentMutation.mutate(payload)}
          recordOptOut={(payload) => recordOptOutMutation.mutate(payload)}
          withdrawConsent={(consentRecordId) => withdrawConsentMutation.mutate(consentRecordId)}
          setDoNotContact={(doNotContact) => updateDoNotContactMutation.mutate(doNotContact)}
          busy={
            recordConsentMutation.isPending ||
            recordOptOutMutation.isPending ||
            withdrawConsentMutation.isPending ||
            updateDoNotContactMutation.isPending
          }
          mutationError={
            recordConsentMutation.error ??
            recordOptOutMutation.error ??
            withdrawConsentMutation.error ??
            updateDoNotContactMutation.error
          }
        />
        <RelatedPanel
          title="Products"
          description="Owned products, policy references, expiration dates, and payment context."
        />
        <BeneficiariesTab
          beneficiaries={beneficiariesQuery.data ?? []}
          loading={beneficiariesQuery.isLoading}
          error={beneficiariesQuery.isError}
          updateGuardianConsent={(beneficiaryId, payload) =>
            updateGuardianConsentMutation.mutate({ beneficiaryId, payload })
          }
          busy={updateGuardianConsentMutation.isPending}
          mutationError={updateGuardianConsentMutation.error}
        />
        <RelatedPanel
          title="Contact history"
          description="Calls, emails, SMS, outcomes, replies, conversions, and communication notes."
        />
        <RelatedPanel
          title="Follow-up tasks"
          description="Interested-prospect tasks, assignments, due dates, priority, and completion state."
        />
        <RelatedPanel
          title="Notes"
          description="Customer-service notes, campaign context, compliance comments, and audit-sensitive remarks."
        />
      </div>
    </section>
  );
}

function DetailItem({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{value == null || value.length === 0 ? "Not provided" : value}</dd>
    </div>
  );
}

function customerToFormPayload(customer: CustomerView): CustomerFormPayload {
  return {
    customerType: customer.customerType,
    firstName: customer.firstName,
    lastName: customer.lastName,
    email: customer.email ?? "",
    phone: customer.phone ?? "",
    addressLine: customer.addressLine ?? "",
    city: customer.city ?? "",
    country: customer.country ?? "",
    dateOfBirth: customer.dateOfBirth ?? "",
    ageGroup: customer.ageGroup ?? "",
    status: customer.status,
    doNotContact: customer.doNotContact,
    source: customer.source ?? "",
  };
}

function RelatedPanel({ title, description }: { title: string; description: string }) {
  const headingId = `${title.toLowerCase().replaceAll(" ", "-")}-heading`;

  return (
    <section className="panel" aria-labelledby={headingId}>
      <div className="section-heading">
        <h2 id={headingId}>{title}</h2>
      </div>
      <p className="table-state">{description}</p>
    </section>
  );
}

function ConsentTab({
  customer,
  consents,
  loading,
  error,
  recordConsent,
  recordOptOut,
  withdrawConsent,
  setDoNotContact,
  busy,
  mutationError,
}: {
  customer: CustomerView;
  consents: ConsentRecordView[];
  loading: boolean;
  error: boolean;
  recordConsent: (payload: RecordConsentPayload) => void;
  recordOptOut: (payload: RecordOptOutPayload) => void;
  withdrawConsent: (consentRecordId: string) => void;
  setDoNotContact: (doNotContact: boolean) => void;
  busy: boolean;
  mutationError: unknown;
}) {
  const [form, setForm] = useState(emptyConsentForm);
  const [optOutForm, setOptOutForm] = useState(emptyOptOutForm);
  const [formError, setFormError] = useState("");

  function updateField<Key extends keyof typeof emptyConsentForm>(
    field: Key,
    value: (typeof emptyConsentForm)[Key],
  ) {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError("");
  }

  function updateOptOutField<Key extends keyof typeof emptyOptOutForm>(
    field: Key,
    value: (typeof emptyOptOutForm)[Key],
  ) {
    setOptOutForm((current) => ({ ...current, [field]: value }));
  }

  function submitConsent() {
    const purpose = form.purpose.trim();
    if (purpose.length === 0) {
      setFormError("Purpose is required.");
      return;
    }

    recordConsent({
      customerId: customer.id,
      consentType: form.consentType,
      status: form.status,
      purpose,
      source: optionalString(form.source),
      evidenceFileUrl: optionalString(form.evidenceFileUrl),
    });
    setForm(emptyConsentForm);
  }

  function submitOptOut() {
    recordOptOut({
      customerId: customer.id,
      consentType: optOutForm.consentType,
      source: optionalString(optOutForm.source),
      evidenceFileUrl: optionalString(optOutForm.evidenceFileUrl),
    });
    setOptOutForm(emptyOptOutForm);
  }

  return (
    <section className="panel" role="tabpanel" aria-labelledby="consent-heading">
      <div className="section-heading">
        <h2 id="consent-heading">Consent</h2>
        <span>{loading ? "Loading consent" : formatCount(consents.length, "consent record")}</span>
      </div>
      <div className="detail-summary consent-summary">
        <div>
          <span className="eyebrow">Do not contact</span>
          <StatusBadge value={customer.doNotContact ? "Do not contact" : "Allowed"} />
        </div>
        <div>
          <span className="eyebrow">Communication</span>
          <strong>{customer.contactable ? "Eligible for contact" : "Blocked"}</strong>
        </div>
        <div>
          <span className="eyebrow">Valid consents</span>
          <strong>{consents.filter((consent) => consent.valid).length}</strong>
        </div>
        <div>
          <span className="eyebrow">Needs action</span>
          <strong>{consents.filter((consent) => consent.requiresAction).length}</strong>
        </div>
      </div>
      <div className="compliance-action-row" aria-label="Do-not-contact controls">
        <div>
          <strong>Do-not-contact override</strong>
          <span>Blocks all marketing communication for this customer.</span>
        </div>
        <button
          type="button"
          className={customer.doNotContact ? "secondary-button" : "danger-button"}
          disabled={busy}
          onClick={() => setDoNotContact(!customer.doNotContact)}
        >
          {customer.doNotContact ? "Allow contact" : "Mark do not contact"}
        </button>
      </div>
      <form
        className="form-grid consent-form"
        aria-label="Record consent"
        onSubmit={(event) => {
          event.preventDefault();
          submitConsent();
        }}
      >
        <label>
          Consent type
          <select
            aria-label="Consent type"
            value={form.consentType}
            onChange={(event) => updateField("consentType", event.target.value as ConsentType)}
          >
            {CONSENT_TYPES.map((consentType) => (
              <option key={consentType} value={consentType}>
                {formatEnum(consentType)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select
            aria-label="Consent status"
            value={form.status}
            onChange={(event) => updateField("status", event.target.value as ConsentStatus)}
          >
            {CONSENT_STATUSES.map((status) => (
              <option key={status} value={status}>
                {formatEnum(status)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Purpose
          <input
            maxLength={255}
            value={form.purpose}
            aria-invalid={formError.length > 0}
            onChange={(event) => updateField("purpose", event.target.value)}
          />
        </label>
        <label>
          Source
          <input
            maxLength={100}
            placeholder="WEB_FORM, PHONE, LETTER"
            value={form.source}
            onChange={(event) => updateField("source", event.target.value)}
          />
        </label>
        <label>
          Evidence URL
          <input
            value={form.evidenceFileUrl}
            onChange={(event) => updateField("evidenceFileUrl", event.target.value)}
          />
        </label>
        <button type="submit" disabled={busy}>
          Record consent
        </button>
      </form>
      {formError ? (
        <p className="form-error" role="alert">
          {formError}
        </p>
      ) : null}
      <form
        className="form-grid consent-form opt-out-form"
        aria-label="Mark opt-out"
        onSubmit={(event) => {
          event.preventDefault();
          submitOptOut();
        }}
      >
        <label>
          Opt-out channel
          <select
            aria-label="Opt-out channel"
            value={optOutForm.consentType}
            onChange={(event) =>
              updateOptOutField(
                "consentType",
                event.target.value as (typeof MARKETING_CONSENT_TYPES)[number],
              )
            }
          >
            {MARKETING_CONSENT_TYPES.map((consentType) => (
              <option key={consentType} value={consentType}>
                {formatEnum(consentType)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Opt-out source
          <input
            maxLength={100}
            placeholder="PHONE, EMAIL, WEB_FORM"
            value={optOutForm.source}
            onChange={(event) => updateOptOutField("source", event.target.value)}
          />
        </label>
        <label>
          Opt-out evidence URL
          <input
            value={optOutForm.evidenceFileUrl}
            onChange={(event) => updateOptOutField("evidenceFileUrl", event.target.value)}
          />
        </label>
        <button type="submit" className="danger-button" disabled={busy}>
          Mark opt-out
        </button>
      </form>
      {mutationError ? (
        <p className="form-error" role="alert">
          Compliance change could not be saved.
        </p>
      ) : null}
      {loading ? <p className="table-state">Loading consent records.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Consent records could not be loaded.
        </p>
      ) : null}
      {!loading && !error && consents.length === 0 ? (
        <p className="table-state">No consent records are available for this customer.</p>
      ) : null}
      {!loading && !error && consents.length > 0 ? (
        <div className="table-scroll">
          <table aria-label="Consent records table">
            <thead>
              <tr>
                <th scope="col">Consent</th>
                <th scope="col">Status</th>
                <th scope="col">Validity</th>
                <th scope="col">Dates</th>
                <th scope="col">Evidence</th>
                <th scope="col">Recorded by</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {consents.map((consent) => (
                <tr key={consent.id}>
                  <th scope="row">
                    <span className="table-primary-text">{formatEnum(consent.consentType)}</span>
                    <span className="table-secondary-text">{consent.purpose}</span>
                  </th>
                  <td>
                    <StatusBadge value={formatEnum(consent.status)} />
                    <span className="table-secondary-text">{consent.source ?? "Source not provided"}</span>
                  </td>
                  <td>
                    <StatusBadge value={consent.valid ? "Valid" : "Invalid"} />
                    <span className="table-secondary-text">
                      {consent.requiresAction ? "Action required" : "No action required"}
                    </span>
                  </td>
                  <td>
                    <span className="table-primary-text">Granted: {formatDateTime(consent.grantedAt)}</span>
                    <span className="table-secondary-text">
                      Withdrawn: {formatDateTime(consent.withdrawnAt)}
                    </span>
                    <span className="table-secondary-text">
                      Expires: {formatDateTime(consent.expiresAt)}
                    </span>
                  </td>
                  <td>{formatEvidence(consent.evidenceFileUrl)}</td>
                  <td>
                    <span className="table-primary-text">
                      {consent.createdByFullName ?? "Recorder not provided"}
                    </span>
                    <span className="table-secondary-text">
                      Recorded: {formatDateTime(consent.createdAt)}
                    </span>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="danger-button"
                      disabled={busy || consent.status === "WITHDRAWN"}
                      onClick={() => withdrawConsent(consent.id)}
                    >
                      Withdraw
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function BeneficiariesTab({
  beneficiaries,
  loading,
  error,
  updateGuardianConsent,
  busy,
  mutationError,
}: {
  beneficiaries: BeneficiaryView[];
  loading: boolean;
  error: boolean;
  updateGuardianConsent: (beneficiaryId: string, payload: UpdateBeneficiaryPayload) => void;
  busy: boolean;
  mutationError: unknown;
}) {
  const [form, setForm] = useState({
    beneficiaryId: "",
    guardianName: "",
    guardianEmail: "",
    guardianConsentRequired: true,
  });
  const [formError, setFormError] = useState("");
  const selectedBeneficiary = beneficiaries.find(
    (beneficiary) => beneficiary.id === form.beneficiaryId,
  );

  function selectBeneficiary(beneficiaryId: string) {
    const beneficiary = beneficiaries.find((current) => current.id === beneficiaryId);
    setForm({
      beneficiaryId,
      guardianName: beneficiary?.guardianName ?? "",
      guardianEmail: beneficiary?.guardianEmail ?? "",
      guardianConsentRequired: beneficiary?.guardianConsentRequired ?? true,
    });
    setFormError("");
  }

  function submitGuardianConsent() {
    if (selectedBeneficiary == null) {
      setFormError("Select a beneficiary.");
      return;
    }

    updateGuardianConsent(selectedBeneficiary.id, {
      relationship: selectedBeneficiary.relationship,
      guardianName: optionalString(form.guardianName),
      guardianEmail: optionalString(form.guardianEmail),
      guardianConsentRequired: form.guardianConsentRequired,
    });
  }

  return (
    <section className="panel" role="tabpanel" aria-labelledby="beneficiaries-heading">
      <div className="section-heading">
        <h2 id="beneficiaries-heading">Beneficiaries</h2>
        <span>{loading ? "Loading links" : `${beneficiaries.length} linked records`}</span>
      </div>
      <form
        className="form-grid guardian-consent-form"
        aria-label="Update guardian consent"
        onSubmit={(event) => {
          event.preventDefault();
          submitGuardianConsent();
        }}
      >
        <label>
          Beneficiary
          <select
            aria-label="Guardian consent beneficiary"
            value={form.beneficiaryId}
            onChange={(event) => selectBeneficiary(event.target.value)}
            disabled={busy || beneficiaries.length === 0}
          >
            <option value="">Select beneficiary</option>
            {beneficiaries.map((beneficiary) => (
              <option key={beneficiary.id} value={beneficiary.id}>
                {beneficiary.beneficiaryFullName ?? beneficiary.id}
              </option>
            ))}
          </select>
        </label>
        <label>
          Guardian name
          <input
            maxLength={255}
            value={form.guardianName}
            onChange={(event) =>
              setForm((current) => ({ ...current, guardianName: event.target.value }))
            }
          />
        </label>
        <label>
          Guardian email
          <input
            maxLength={255}
            value={form.guardianEmail}
            onChange={(event) =>
              setForm((current) => ({ ...current, guardianEmail: event.target.value }))
            }
          />
        </label>
        <label>
          Requirement
          <select
            aria-label="Guardian consent requirement"
            value={form.guardianConsentRequired ? "required" : "not-required"}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                guardianConsentRequired: event.target.value === "required",
              }))
            }
          >
            <option value="required">Guardian consent required</option>
            <option value="not-required">Guardian consent not required</option>
          </select>
        </label>
        <button type="submit" disabled={busy || beneficiaries.length === 0}>
          Save guardian consent
        </button>
      </form>
      {formError ? (
        <p className="form-error" role="alert">
          {formError}
        </p>
      ) : null}
      {mutationError ? (
        <p className="form-error" role="alert">
          Guardian consent could not be saved.
        </p>
      ) : null}
      {loading ? <p className="table-state">Loading beneficiary records.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Beneficiary records could not be loaded.
        </p>
      ) : null}
      {!loading && !error && beneficiaries.length === 0 ? (
        <p className="table-state">No beneficiaries are linked to this customer.</p>
      ) : null}
      {!loading && !error && beneficiaries.length > 0 ? (
        <div className="table-scroll">
          <table aria-label="Beneficiaries table">
            <thead>
              <tr>
                <th scope="col">Beneficiary</th>
                <th scope="col">Relationship</th>
                <th scope="col">Guardian consent</th>
                <th scope="col">Guardian contact</th>
                <th scope="col">Created</th>
              </tr>
            </thead>
            <tbody>
              {beneficiaries.map((beneficiary) => (
                <tr key={beneficiary.id}>
                  <th scope="row">
                    <span className="table-primary-text">
                      {beneficiary.beneficiaryFullName ?? "Unnamed beneficiary"}
                    </span>
                    <span className="table-secondary-text">
                      {beneficiary.beneficiaryCustomerId ?? "Customer ID not available"}
                    </span>
                  </th>
                  <td>{beneficiary.relationship}</td>
                  <td>
                    <StatusBadge
                      value={
                        beneficiary.guardianConsentRequired
                          ? "Guardian consent required"
                          : "Guardian consent not required"
                      }
                    />
                  </td>
                  <td>{formatGuardianContact(beneficiary)}</td>
                  <td>{formatDateTime(beneficiary.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function formatEnum(value: string) {
  return value
    .replace(/^AGE_/, "")
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatDate(value: string | null) {
  if (value == null) {
    return null;
  }
  return new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(value));
}

function formatDateTime(value: CustomerView["updatedAt"]) {
  if (value == null) {
    return null;
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatGuardianContact(beneficiary: BeneficiaryView) {
  const parts = [beneficiary.guardianName, beneficiary.guardianEmail].filter(Boolean);
  return parts.length === 0 ? "Not provided" : parts.join(" / ");
}

function formatEvidence(value: string | null) {
  if (value == null || value.length === 0) {
    return "Not provided";
  }
  return value;
}

function formatCount(count: number, label: string) {
  return `${count} ${label}${count === 1 ? "" : "s"}`;
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
