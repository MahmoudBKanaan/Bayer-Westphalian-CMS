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
  type RecordOptOutPayload,
  type RecordConsentPayload,
} from "@/api/consents";
import {
  buildOptOutPayload,
  buildRecordConsentPayload,
  CONSENT_EMPTY_STATE,
  CONSENT_OPT_OUT_FORM_ARIA_LABEL,
  CONSENT_OPT_OUT_NOTICE,
  CONSENT_OPT_OUT_SUBMIT_LABEL,
  CONSENT_RECORD_FORM_ARIA_LABEL,
  CONSENT_RECORD_SUBMIT_LABEL,
  CONSENT_RECORDED_NOTICE,
  CONSENT_RECORDS_TABLE_ARIA_LABEL,
  CONSENT_SECTION_HEADING,
  CONSENT_STATUSES,
  CONSENT_TYPES,
  CONSENT_WITHDRAW_SUBMIT_LABEL,
  CONSENT_WITHDRAWN_NOTICE,
  emptyConsentOptOutForm,
  emptyConsentRecordForm,
  hasConsentFormErrors,
  MARKETING_CONSENT_TYPES,
  validateConsentRecordForm,
  type ConsentOptOutFormValues,
  type ConsentRecordFormValues,
  type MarketingConsentType,
} from "@/features/customers/consentUpdateFlow";
import {
  contactEventTypes,
  listContactTimeline,
  recordContactEvent,
  emptyRecordContactEventForm,
  type ContactEventView,
  type ContactEventType,
  type ContactOutcome,
  type RecordContactEventPayload,
} from "@/api/contactEvents";
import {
  formatFollowUpEnum,
  listFollowUpTasks,
  type FollowUpTaskView,
} from "@/api/followUpTasks";
import { Link, useParams } from "react-router-dom";
import {
  getCustomer,
  updateCustomer,
  type CustomerFormPayload,
  type CustomerView,
} from "@/api/customers";
import { listProducts } from "@/api/products";
import {
  createPaymentRecord,
  incrementPaymentReminder,
  listCustomerPaymentRecords,
  markPaymentOverdue,
  markPaymentPaid,
  updatePaymentRecord,
  type CreatePaymentRecordPayload,
  type PaymentRecordView,
  type UpdatePaymentRecordPayload,
} from "@/api/paymentRecords";
import {
  assignProductOwnership,
  listCustomerProductOwnerships,
  updateProductOwnership,
  type AssignProductOwnershipPayload,
  type ProductOwnershipView,
  type UpdateProductOwnershipPayload,
} from "@/api/productOwnerships";
import { CustomerStatusBadge } from "@/components/CustomerStatusBadge";
import { ConsentStatusBadge } from "@/components/ConsentStatusBadge";
import { DefaultRiskScorePanel } from "@/components/DefaultRiskScorePanel";
import { ProductRecommendationPanel } from "@/components/ProductRecommendationPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

const emptyAssignOwnershipForm: AssignProductOwnershipPayload = {
  customerId: "",
  productId: "",
  startDate: "",
  expirationDate: "",
  policyNumber: "",
};
const emptyCreatePaymentForm: CreatePaymentRecordPayload = {
  customerId: "",
  productOwnershipId: "",
  dueDate: "",
  amountDue: "",
};

export function CustomerDetailsPage() {
  const { customerId } = useParams();
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const canAssignProducts = permissions.canManageProductOwnership();
  const canManagePayments = permissions.canManagePaymentRecords();
  const canManageContactHistory = permissions.canManageContactHistory();
  const canReadConsentRecords = permissions.canReadConsentRecords();
  const canReadPaymentRecords = permissions.canReadPaymentRecords();
  const canReadBeneficiaries = permissions.canReadBeneficiaries();
  const canReadFollowUpTasks = permissions.canReadFollowUpTasks();
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
    enabled: hasCustomerId && canReadBeneficiaries,
  });
  const consentsQuery = useQuery({
    queryKey: ["consents", "customer", customerId],
    queryFn: () => listConsents({ customerId: customerId ?? "" }),
    enabled: hasCustomerId && canReadConsentRecords,
  });
  const productOwnershipsQuery = useQuery({
    queryKey: ["product-ownerships", "customer", customerId],
    queryFn: () => listCustomerProductOwnerships(customerId ?? ""),
    enabled: hasCustomerId,
  });
  const [contactEventTypeFilter, setContactEventTypeFilter] = useState<ContactEventType | "ALL">(
    "ALL",
  );

  const contactHistoryQuery = useQuery({
    queryKey: ["contact-timeline", customerId, contactEventTypeFilter],
    queryFn: () =>
      listContactTimeline({
        customerId: customerId ?? "",
        campaignId: "",
        eventType: contactEventTypeFilter,
      }),
    enabled: hasCustomerId,
  });
  const followUpTasksQuery = useQuery({
    queryKey: ["follow-up-tasks", "customer", customerId],
    queryFn: () =>
      listFollowUpTasks({
        customerId: customerId ?? "",
        assignedTo: "",
        priority: "ALL",
        status: "ALL",
        dueDateFrom: "",
        dueDateTo: "",
      }),
    enabled: hasCustomerId && canReadFollowUpTasks,
  });
  const recordContactEventMutation = useMutation({
    mutationFn: recordContactEvent,
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["contact-timeline", customerId],
      });
    },
  });

  const productsQuery = useQuery({
    queryKey: ["products", "catalog"],
    queryFn: () => listProducts(),
    enabled: hasCustomerId && canAssignProducts,
  });
  const refreshProductOwnerships = async () => {
    await queryClient.invalidateQueries({
      queryKey: ["product-ownerships", "customer", customerId],
    });
  };
  const paymentRecordsQuery = useQuery({
    queryKey: ["payment-records", "customer", customerId],
    queryFn: () => listCustomerPaymentRecords(customerId ?? ""),
    enabled: hasCustomerId && canReadPaymentRecords,
  });
  const refreshPaymentRecords = async () => {
    await queryClient.invalidateQueries({
      queryKey: ["payment-records", "customer", customerId],
    });
  };
  const [consentNotice, setConsentNotice] = useState("");
  const recordConsentMutation = useMutation({
    mutationFn: recordConsent,
    onSuccess: async () => {
      setConsentNotice(CONSENT_RECORDED_NOTICE);
      await refreshConsent();
    },
  });
  const recordOptOutMutation = useMutation({
    mutationFn: recordOptOut,
    onSuccess: async () => {
      setConsentNotice(CONSENT_OPT_OUT_NOTICE);
      await refreshConsent();
    },
  });
  const withdrawConsentMutation = useMutation({
    mutationFn: withdrawConsent,
    onSuccess: async () => {
      setConsentNotice(CONSENT_WITHDRAWN_NOTICE);
      await refreshConsent();
    },
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
  const [productOwnershipNotice, setProductOwnershipNotice] = useState("");
  const [paymentRecordNotice, setPaymentRecordNotice] = useState("");
  const assignProductOwnershipMutation = useMutation({
    mutationFn: assignProductOwnership,
    onSuccess: async () => {
      setProductOwnershipNotice("Product assigned to customer.");
      await refreshProductOwnerships();
    },
  });
  const updateProductOwnershipMutation = useMutation({
    mutationFn: ({
      ownershipId,
      payload,
    }: {
      ownershipId: string;
      payload: UpdateProductOwnershipPayload;
    }) => updateProductOwnership(ownershipId, payload),
    onSuccess: async () => {
      setProductOwnershipNotice("Product ownership updated.");
      await refreshProductOwnerships();
    },
  });
  const createPaymentRecordMutation = useMutation({
    mutationFn: createPaymentRecord,
    onSuccess: async () => {
      setPaymentRecordNotice("Payment record created.");
      await refreshPaymentRecords();
    },
  });
  const updatePaymentRecordMutation = useMutation({
    mutationFn: ({
      paymentId,
      payload,
    }: {
      paymentId: string;
      payload: UpdatePaymentRecordPayload;
    }) => updatePaymentRecord(paymentId, payload),
    onSuccess: async () => {
      setPaymentRecordNotice("Payment record updated.");
      await refreshPaymentRecords();
    },
  });
  const markPaymentPaidMutation = useMutation({
    mutationFn: ({ paymentId, amountPaid }: { paymentId: string; amountPaid: string }) =>
      markPaymentPaid(paymentId, { amountPaid }),
    onSuccess: async () => {
      setPaymentRecordNotice("Payment marked paid.");
      await refreshPaymentRecords();
    },
  });
  const markPaymentOverdueMutation = useMutation({
    mutationFn: markPaymentOverdue,
    onSuccess: async () => {
      setPaymentRecordNotice("Payment marked overdue.");
      await refreshPaymentRecords();
    },
  });
  const incrementPaymentReminderMutation = useMutation({
    mutationFn: incrementPaymentReminder,
    onSuccess: async () => {
      setPaymentRecordNotice("Payment reminder incremented.");
      await refreshPaymentRecords();
    },
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
            <CustomerStatusBadge status={customer.status} />
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
        {canReadConsentRecords ? (
          <ConsentTab
            customer={customer}
            consents={consentsQuery.data ?? []}
            loading={consentsQuery.isLoading}
            error={consentsQuery.isError}
            notice={consentNotice}
            recordConsent={(payload) => {
              setConsentNotice("");
              recordConsentMutation.mutate(payload);
            }}
            recordOptOut={(payload) => {
              setConsentNotice("");
              recordOptOutMutation.mutate(payload);
            }}
            withdrawConsent={(consentRecordId) => {
              setConsentNotice("");
              withdrawConsentMutation.mutate(consentRecordId);
            }}
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
        ) : null}
        <ProductOwnershipTab
          customerId={customer.id}
          ownerships={productOwnershipsQuery.data ?? []}
          products={productsQuery.data ?? []}
          loading={productOwnershipsQuery.isLoading}
          error={productOwnershipsQuery.isError}
          canAssign={canAssignProducts}
          assignOwnership={(payload) => {
            setProductOwnershipNotice("");
            assignProductOwnershipMutation.mutate(payload);
          }}
          updateOwnership={(ownershipId, payload) => {
            setProductOwnershipNotice("");
            updateProductOwnershipMutation.mutate({ ownershipId, payload });
          }}
          assignNotice={productOwnershipNotice}
          busy={
            assignProductOwnershipMutation.isPending || updateProductOwnershipMutation.isPending
          }
          mutationError={
            assignProductOwnershipMutation.error ?? updateProductOwnershipMutation.error
          }
        />
        <ProductRecommendationPanel
          customerId={customer.id}
          customerName={customer.fullName}
        />
        <DefaultRiskScorePanel customerId={customer.id} customerName={customer.fullName} />
        {canReadPaymentRecords ? (
          <PaymentRecordsTab
            customerId={customer.id}
            ownerships={productOwnershipsQuery.data ?? []}
            payments={paymentRecordsQuery.data ?? []}
            loading={paymentRecordsQuery.isLoading}
            error={paymentRecordsQuery.isError}
            canManage={canManagePayments}
            createPayment={(payload) => {
              setPaymentRecordNotice("");
              createPaymentRecordMutation.mutate(payload);
            }}
            updatePayment={(paymentId, payload) => {
              setPaymentRecordNotice("");
              updatePaymentRecordMutation.mutate({ paymentId, payload });
            }}
            markPaid={(paymentId, amountPaid) => {
              setPaymentRecordNotice("");
              markPaymentPaidMutation.mutate({ paymentId, amountPaid });
            }}
            markOverdue={(paymentId) => {
              setPaymentRecordNotice("");
              markPaymentOverdueMutation.mutate(paymentId);
            }}
            incrementReminder={(paymentId) => {
              setPaymentRecordNotice("");
              incrementPaymentReminderMutation.mutate(paymentId);
            }}
            paymentNotice={paymentRecordNotice}
            busy={
              createPaymentRecordMutation.isPending ||
              updatePaymentRecordMutation.isPending ||
              markPaymentPaidMutation.isPending ||
              markPaymentOverdueMutation.isPending ||
              incrementPaymentReminderMutation.isPending
            }
            mutationError={
              createPaymentRecordMutation.error ??
              updatePaymentRecordMutation.error ??
              markPaymentPaidMutation.error ??
              markPaymentOverdueMutation.error ??
              incrementPaymentReminderMutation.error
            }
          />
        ) : null}
        {canReadBeneficiaries ? (
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
        ) : null}
        <ContactHistoryTab
          events={contactHistoryQuery.data ?? []}
          loading={contactHistoryQuery.isLoading}
          error={contactHistoryQuery.isError}
          customerId={customer.id}
          canManage={canManageContactHistory}
          recordEvent={(payload) => recordContactEventMutation.mutate(payload)}
          busy={recordContactEventMutation.isPending}
          mutationError={recordContactEventMutation.error}
          eventTypeFilter={contactEventTypeFilter}
          onEventTypeFilterChange={setContactEventTypeFilter}
        />
        {canReadFollowUpTasks ? (
          <FollowUpTasksTab
            tasks={followUpTasksQuery.data ?? []}
            loading={followUpTasksQuery.isLoading}
            error={followUpTasksQuery.isError}
          />
        ) : null}
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

function FollowUpTasksTab({
  tasks,
  loading,
  error,
}: {
  tasks: FollowUpTaskView[];
  loading: boolean;
  error: boolean;
}) {
  return (
    <section className="panel" role="tabpanel" aria-labelledby="follow-up-tasks-heading">
      <div className="section-heading">
        <h2 id="follow-up-tasks-heading">Follow-up tasks</h2>
        <span>
          {loading ? "Loading follow-up tasks" : formatCount(tasks.length, "follow-up task")}
        </span>
      </div>
      {loading ? <p className="table-state">Loading follow-up tasks.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Follow-up tasks could not be loaded.
        </p>
      ) : null}
      {!loading && !error && tasks.length === 0 ? (
        <p className="table-state">No follow-up tasks are linked to this customer.</p>
      ) : null}
      {!loading && !error && tasks.length > 0 ? (
        <div className="table-scroll">
          <table aria-label="Customer follow-up tasks table">
            <thead>
              <tr>
                <th scope="col">Task</th>
                <th scope="col">Campaign</th>
                <th scope="col">Assignee</th>
                <th scope="col">Priority</th>
                <th scope="col">Status</th>
                <th scope="col">Due</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task) => (
                <tr key={task.id}>
                  <th scope="row">
                    <span className="table-primary-text">{task.title}</span>
                    <span className="table-secondary-text">
                      {task.description == null || task.description.trim().length === 0
                        ? "No description"
                        : task.description}
                    </span>
                  </th>
                  <td>
                    <span className="table-primary-text">
                      {task.campaignName ?? "No campaign"}
                    </span>
                    <span className="table-secondary-text">
                      {task.campaignId ?? "No campaign id"}
                    </span>
                  </td>
                  <td>{task.assignedToFullName ?? task.assignedToUserId ?? "Unassigned"}</td>
                  <td>
                    <StatusBadge value={formatFollowUpEnum(task.priority)} />
                  </td>
                  <td>
                    <StatusBadge value={formatFollowUpEnum(task.status)} />
                    {task.completedAt == null ? null : (
                      <span className="table-secondary-text">
                        Completed: {formatDateTime(task.completedAt)}
                      </span>
                    )}
                  </td>
                  <td>{formatDate(task.dueDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
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
  notice,
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
  notice: string;
  recordConsent: (payload: RecordConsentPayload) => void;
  recordOptOut: (payload: RecordOptOutPayload) => void;
  withdrawConsent: (consentRecordId: string) => void;
  setDoNotContact: (doNotContact: boolean) => void;
  busy: boolean;
  mutationError: unknown;
}) {
  const [form, setForm] = useState<ConsentRecordFormValues>(() => emptyConsentRecordForm());
  const [optOutForm, setOptOutForm] = useState<ConsentOptOutFormValues>(() =>
    emptyConsentOptOutForm(),
  );
  const [formError, setFormError] = useState("");

  function updateField<Key extends keyof ConsentRecordFormValues>(
    field: Key,
    value: ConsentRecordFormValues[Key],
  ) {
    setForm((current) => ({ ...current, [field]: value }));
    setFormError("");
  }

  function updateOptOutField<Key extends keyof ConsentOptOutFormValues>(
    field: Key,
    value: ConsentOptOutFormValues[Key],
  ) {
    setOptOutForm((current) => ({ ...current, [field]: value }));
  }

  function submitConsent() {
    const fieldErrors = validateConsentRecordForm(form, customer.id);
    if (hasConsentFormErrors(fieldErrors)) {
      setFormError(fieldErrors.purpose ?? fieldErrors.customerId ?? "Consent form is invalid.");
      return;
    }

    recordConsent(buildRecordConsentPayload(customer.id, form));
    setForm(emptyConsentRecordForm());
  }

  function submitOptOut() {
    recordOptOut(buildOptOutPayload(customer.id, optOutForm));
    setOptOutForm(emptyConsentOptOutForm());
  }

  return (
    <section className="panel" role="tabpanel" aria-labelledby="consent-heading">
      <div className="section-heading">
        <h2 id="consent-heading">{CONSENT_SECTION_HEADING}</h2>
        <span>{loading ? "Loading consent" : formatCount(consents.length, "consent record")}</span>
      </div>
      {notice ? (
        <p className="form-success" role="status" data-testid="consent-update-notice">
          {notice}
        </p>
      ) : null}
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
        aria-label={CONSENT_RECORD_FORM_ARIA_LABEL}
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
            onChange={(event) =>
              updateField("consentType", event.target.value as ConsentRecordFormValues["consentType"])
            }
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
            onChange={(event) =>
              updateField("status", event.target.value as ConsentRecordFormValues["status"])
            }
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
          {CONSENT_RECORD_SUBMIT_LABEL}
        </button>
      </form>
      {formError ? (
        <p className="form-error" role="alert" data-testid="consent-form-error">
          {formError}
        </p>
      ) : null}
      <form
        className="form-grid consent-form opt-out-form"
        aria-label={CONSENT_OPT_OUT_FORM_ARIA_LABEL}
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
              updateOptOutField("consentType", event.target.value as MarketingConsentType)
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
          {CONSENT_OPT_OUT_SUBMIT_LABEL}
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
        <p className="table-state">{CONSENT_EMPTY_STATE}</p>
      ) : null}
      {!loading && !error && consents.length > 0 ? (
        <div className="table-scroll">
          <table aria-label={CONSENT_RECORDS_TABLE_ARIA_LABEL}>
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
                    <ConsentStatusBadge status={consent.status} />
                    <span className="table-secondary-text">
                      {consent.source ?? "Source not provided"}
                    </span>
                  </td>
                  <td>
                    <StatusBadge value={consent.valid ? "Valid" : "Invalid"} />
                    <span className="table-secondary-text">
                      {consent.requiresAction ? "Action required" : "No action required"}
                    </span>
                  </td>
                  <td>
                    <span className="table-primary-text">
                      Granted: {formatDateTime(consent.grantedAt)}
                    </span>
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
                      {CONSENT_WITHDRAW_SUBMIT_LABEL}
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

function ProductOwnershipTab({
  customerId,
  ownerships,
  products,
  loading,
  error,
  canAssign,
  assignOwnership,
  updateOwnership,
  assignNotice,
  busy,
  mutationError,
}: {
  customerId: string;
  ownerships: ProductOwnershipView[];
  products: Array<{ id: string; name: string }>;
  loading: boolean;
  error: boolean;
  canAssign: boolean;
  assignOwnership: (payload: AssignProductOwnershipPayload) => void;
  updateOwnership: (ownershipId: string, payload: UpdateProductOwnershipPayload) => void;
  assignNotice: string;
  busy: boolean;
  mutationError: unknown;
}) {
  const [assignForm, setAssignForm] = useState<AssignProductOwnershipPayload>({
    ...emptyAssignOwnershipForm,
    customerId,
  });

  return (
    <section className="panel" role="tabpanel" aria-labelledby="products-heading">
      <div className="section-heading">
        <h2 id="products-heading">Products</h2>
        <span>
          {loading ? "Loading product ownership" : formatCount(ownerships.length, "owned product")}
        </span>
      </div>
      <p className="table-state">
        Owned products, policy references, expiration dates, and payment context.
      </p>
      {canAssign ? (
        <form
          className="form-grid"
          aria-label="Assign product ownership"
          onSubmit={(event) => {
            event.preventDefault();
            assignOwnership({
              ...assignForm,
              customerId,
            });
            setAssignForm({
              ...emptyAssignOwnershipForm,
              customerId,
            });
          }}
        >
          <label>
            Product
            <select
              required
              aria-label="Product to assign"
              value={assignForm.productId}
              onChange={(event) =>
                setAssignForm((current) => ({
                  ...current,
                  productId: event.target.value,
                }))
              }
            >
              <option value="">Select product</option>
              {products.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Policy number
            <input
              aria-label="Policy number"
              maxLength={100}
              value={assignForm.policyNumber}
              onChange={(event) =>
                setAssignForm((current) => ({
                  ...current,
                  policyNumber: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Start date
            <input
              required
              type="date"
              aria-label="Coverage start date"
              value={assignForm.startDate}
              onChange={(event) =>
                setAssignForm((current) => ({
                  ...current,
                  startDate: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Expiration date
            <input
              type="date"
              aria-label="Coverage expiration date"
              value={assignForm.expirationDate}
              onChange={(event) =>
                setAssignForm((current) => ({
                  ...current,
                  expirationDate: event.target.value,
                }))
              }
            />
          </label>
          <button type="submit" disabled={busy}>
            Assign product
          </button>
          {assignNotice ? <p className="form-success">{assignNotice}</p> : null}
          {mutationError ? (
            <p className="form-error" role="alert">
              Product assignment failed.
            </p>
          ) : null}
        </form>
      ) : null}
      {loading ? <p className="table-state">Loading product ownership records.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Product ownership records could not be loaded.
        </p>
      ) : null}
      {!loading && !error && ownerships.length === 0 ? (
        <p className="table-state">No products are assigned to this customer.</p>
      ) : null}
      {!loading && !error && ownerships.length > 0 ? (
        <div className="table-scroll">
          <table aria-label="Product ownership table">
            <thead>
              <tr>
                <th scope="col">Product</th>
                <th scope="col">Policy</th>
                <th scope="col">Coverage period</th>
                <th scope="col">Status</th>
                <th scope="col">Assigned</th>
                {canAssign ? <th scope="col">Actions</th> : null}
              </tr>
            </thead>
            <tbody>
              {ownerships.map((ownership) => (
                <tr key={ownership.id}>
                  <th scope="row">
                    {ownership.productId == null ? (
                      <span className="table-primary-text">
                        {ownership.productName ?? "Unnamed product"}
                      </span>
                    ) : (
                      <Link className="table-primary-text" to={`/products/${ownership.productId}`}>
                        {ownership.productName ?? "Unnamed product"}
                      </Link>
                    )}
                    <span className="table-secondary-text">
                      {ownership.productType == null
                        ? "Product type not provided"
                        : formatEnum(ownership.productType)}
                    </span>
                  </th>
                  <td>{formatPolicyNumber(ownership.policyNumber)}</td>
                  <td>
                    <span className="table-primary-text">
                      Start: {formatDate(ownership.startDate) ?? "Not provided"}
                    </span>
                    <span className="table-secondary-text">
                      Expires: {formatDate(ownership.expirationDate) ?? "Not provided"}
                    </span>
                  </td>
                  <td>
                    <StatusBadge value={formatEnum(ownership.status)} />
                    <span className="table-secondary-text">
                      {ownership.active ? "Active coverage" : "Inactive coverage"}
                    </span>
                  </td>
                  <td>{formatDateTime(ownership.createdAt)}</td>
                  {canAssign ? (
                    <td>
                      <ProductOwnershipUpdateForm
                        ownership={ownership}
                        busy={busy}
                        updateOwnership={updateOwnership}
                      />
                    </td>
                  ) : null}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

function ProductOwnershipUpdateForm({
  ownership,
  busy,
  updateOwnership,
}: {
  ownership: ProductOwnershipView;
  busy: boolean;
  updateOwnership: (ownershipId: string, payload: UpdateProductOwnershipPayload) => void;
}) {
  const [form, setForm] = useState<UpdateProductOwnershipPayload>({
    policyNumber: ownership.policyNumber ?? "",
    expirationDate: ownership.expirationDate ?? "",
  });

  return (
    <form
      className="inline-form"
      aria-label={`Update ownership ${ownership.policyNumber ?? ownership.id}`}
      onSubmit={(event) => {
        event.preventDefault();
        updateOwnership(ownership.id, form);
      }}
    >
      <label>
        Policy number
        <input
          aria-label={`Policy number for ${ownership.productName ?? "owned product"}`}
          maxLength={100}
          value={form.policyNumber}
          onChange={(event) =>
            setForm((current) => ({
              ...current,
              policyNumber: event.target.value,
            }))
          }
        />
      </label>
      <label>
        Expiration date
        <input
          type="date"
          aria-label={`Expiration date for ${ownership.productName ?? "owned product"}`}
          value={form.expirationDate}
          onChange={(event) =>
            setForm((current) => ({
              ...current,
              expirationDate: event.target.value,
            }))
          }
        />
      </label>
      <button type="submit" disabled={busy}>
        Save ownership
      </button>
    </form>
  );
}

function PaymentRecordsTab({
  customerId,
  ownerships,
  payments,
  loading,
  error,
  canManage,
  createPayment,
  updatePayment,
  markPaid,
  markOverdue,
  incrementReminder,
  paymentNotice,
  busy,
  mutationError,
}: {
  customerId: string;
  ownerships: ProductOwnershipView[];
  payments: PaymentRecordView[];
  loading: boolean;
  error: boolean;
  canManage: boolean;
  createPayment: (payload: CreatePaymentRecordPayload) => void;
  updatePayment: (paymentId: string, payload: UpdatePaymentRecordPayload) => void;
  markPaid: (paymentId: string, amountPaid: string) => void;
  markOverdue: (paymentId: string) => void;
  incrementReminder: (paymentId: string) => void;
  paymentNotice: string;
  busy: boolean;
  mutationError: unknown;
}) {
  const [createForm, setCreateForm] = useState<CreatePaymentRecordPayload>({
    ...emptyCreatePaymentForm,
    customerId,
  });
  const [selectedPaymentId, setSelectedPaymentId] = useState("");
  const [paidAmount, setPaidAmount] = useState("");
  const [updateForm, setUpdateForm] = useState<{
    paymentId: string;
    payload: UpdatePaymentRecordPayload;
  }>({
    paymentId: "",
    payload: { dueDate: "", amountDue: "" },
  });

  const selectedPayment =
    selectedPaymentId === ""
      ? payments[0]
      : payments.find((payment) => payment.id === selectedPaymentId);
  const selectedPaymentUpdateForm =
    selectedPayment != null && updateForm.paymentId === selectedPayment.id
      ? updateForm.payload
      : selectedPayment == null
        ? { dueDate: "", amountDue: "" }
        : paymentToUpdateForm(selectedPayment);
  const effectivePaidAmount =
    paidAmount.trim() === "" && selectedPayment?.amountDue != null
      ? String(selectedPayment.amountDue)
      : paidAmount;

  return (
    <section className="panel" role="tabpanel" aria-labelledby="payments-heading">
      <div className="section-heading">
        <h2 id="payments-heading">Payments</h2>
        <span>
          {loading ? "Loading payment records" : formatCount(payments.length, "payment record")}
        </span>
      </div>
      <p className="table-state">
        Due dates, paid amounts, reminder counts, overdue days, and default-risk status.
      </p>
      {canManage ? (
        <form
          className="form-grid"
          aria-label="Create payment record"
          onSubmit={(event) => {
            event.preventDefault();
            createPayment({
              ...createForm,
              customerId,
            });
            setCreateForm({
              ...emptyCreatePaymentForm,
              customerId,
            });
          }}
        >
          <label>
            Owned product
            <select
              required
              aria-label="Owned product for payment record"
              value={createForm.productOwnershipId}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  productOwnershipId: event.target.value,
                }))
              }
            >
              <option value="">Select owned product</option>
              {ownerships.map((ownership) => (
                <option key={ownership.id} value={ownership.id}>
                  {ownership.productName ?? "Unnamed product"}
                  {ownership.policyNumber ? ` (${ownership.policyNumber})` : ""}
                </option>
              ))}
            </select>
          </label>
          <label>
            Due date
            <input
              required
              type="date"
              aria-label="Payment due date"
              value={createForm.dueDate}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  dueDate: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Amount due
            <input
              required
              inputMode="decimal"
              aria-label="Payment amount due"
              placeholder="0.00"
              value={createForm.amountDue}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  amountDue: event.target.value,
                }))
              }
            />
          </label>
          <button type="submit" disabled={busy || ownerships.length === 0}>
            Create payment record
          </button>
        </form>
      ) : null}
      {paymentNotice ? <p className="form-success">{paymentNotice}</p> : null}
      {mutationError ? (
        <p className="form-error" role="alert">
          Payment record action failed.
        </p>
      ) : null}
      {loading ? <p className="table-state">Loading payment records.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Payment records could not be loaded.
        </p>
      ) : null}
      {!loading && !error && payments.length === 0 ? (
        <p className="table-state">No payment records exist for this customer.</p>
      ) : null}
      {!loading && !error && payments.length > 0 ? (
        <div className="table-scroll">
          <table aria-label="Payment records table">
            <thead>
              <tr>
                <th scope="col">Product</th>
                <th scope="col">Due date</th>
                <th scope="col">Amounts</th>
                <th scope="col">Status</th>
                <th scope="col">Reminders</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr
                  key={payment.id}
                  className={selectedPayment?.id === payment.id ? "selected-row" : undefined}
                  onClick={() => {
                    setSelectedPaymentId(payment.id);
                    setPaidAmount(payment.amountDue == null ? "" : String(payment.amountDue));
                    setUpdateForm({
                      paymentId: payment.id,
                      payload: paymentToUpdateForm(payment),
                    });
                  }}
                >
                  <th scope="row">
                    {payment.productId == null ? (
                      <span className="table-primary-text">
                        {payment.productName ?? "Unnamed product"}
                      </span>
                    ) : (
                      <Link className="table-primary-text" to={`/products/${payment.productId}`}>
                        {payment.productName ?? "Unnamed product"}
                      </Link>
                    )}
                    <span className="table-secondary-text">
                      {payment.productType == null
                        ? "Product type not provided"
                        : formatEnum(payment.productType)}
                    </span>
                  </th>
                  <td>
                    <span className="table-primary-text">
                      Due: {formatDate(payment.dueDate) ?? "Not provided"}
                    </span>
                    <span className="table-secondary-text">
                      Paid: {formatDateTime(payment.paidAt)}
                    </span>
                  </td>
                  <td>
                    <span className="table-primary-text">
                      Due: {formatMoney(payment.amountDue)}
                    </span>
                    <span className="table-secondary-text">
                      Paid: {formatMoney(payment.amountPaid)}
                    </span>
                  </td>
                  <td>
                    <StatusBadge value={formatEnum(payment.status)} />
                    <span className="table-secondary-text">
                      {payment.defaultRisk ? "Default risk" : `${payment.daysOverdue} days overdue`}
                    </span>
                  </td>
                  <td>{payment.reminderCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {canManage && selectedPayment != null ? (
        <div className="form-grid">
          <label>
            Selected payment
            <select
              aria-label="Selected payment record"
              value={selectedPayment.id}
              onChange={(event) => {
                const nextPayment = payments.find((payment) => payment.id === event.target.value);
                setSelectedPaymentId(event.target.value);
                setPaidAmount(nextPayment?.amountDue == null ? "" : String(nextPayment.amountDue));
                setUpdateForm({
                  paymentId: nextPayment?.id ?? "",
                  payload:
                    nextPayment == null
                      ? { dueDate: "", amountDue: "" }
                      : paymentToUpdateForm(nextPayment),
                });
              }}
            >
              {payments.map((payment) => (
                <option key={payment.id} value={payment.id}>
                  {payment.productName ?? "Unnamed product"} — {formatEnum(payment.status)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Due date
            <input
              type="date"
              aria-label="Selected payment due date"
              value={selectedPaymentUpdateForm.dueDate}
              disabled={selectedPayment.status === "PAID"}
              onChange={(event) =>
                setUpdateForm((current) => ({
                  paymentId: selectedPayment.id,
                  payload: {
                    ...(current.paymentId === selectedPayment.id
                      ? current.payload
                      : selectedPaymentUpdateForm),
                    dueDate: event.target.value,
                  },
                }))
              }
            />
          </label>
          <label>
            Amount due
            <input
              inputMode="decimal"
              aria-label="Selected payment amount due"
              value={selectedPaymentUpdateForm.amountDue}
              disabled={selectedPayment.status === "PAID"}
              onChange={(event) =>
                setUpdateForm((current) => ({
                  paymentId: selectedPayment.id,
                  payload: {
                    ...(current.paymentId === selectedPayment.id
                      ? current.payload
                      : selectedPaymentUpdateForm),
                    amountDue: event.target.value,
                  },
                }))
              }
            />
          </label>
          <label>
            Amount paid
            <input
              inputMode="decimal"
              aria-label="Amount paid"
              value={paidAmount}
              disabled={selectedPayment.status === "PAID"}
              onChange={(event) => setPaidAmount(event.target.value)}
            />
          </label>
          <div className="button-row">
            <button
              type="button"
              disabled={busy || selectedPayment.status === "PAID"}
              onClick={() =>
                updatePayment(selectedPayment.id, {
                  dueDate: selectedPaymentUpdateForm.dueDate || selectedPayment.dueDate || "",
                  amountDue:
                    selectedPaymentUpdateForm.amountDue || String(selectedPayment.amountDue ?? ""),
                })
              }
            >
              Save payment
            </button>
            <button
              type="button"
              disabled={busy || selectedPayment.status === "PAID"}
              onClick={() => markPaid(selectedPayment.id, effectivePaidAmount)}
            >
              Mark paid
            </button>
            <button
              type="button"
              disabled={busy || selectedPayment.status === "PAID"}
              onClick={() => markOverdue(selectedPayment.id)}
            >
              Mark overdue
            </button>
            <button
              type="button"
              disabled={busy || selectedPayment.status === "PAID"}
              onClick={() => incrementReminder(selectedPayment.id)}
            >
              Increment reminder
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function formatMoney(value: number | null) {
  if (value == null) {
    return "Not set";
  }
  return new Intl.NumberFormat("en", {
    style: "currency",
    currency: "EUR",
    minimumFractionDigits: 2,
  }).format(value);
}

function paymentToUpdateForm(payment: PaymentRecordView): UpdatePaymentRecordPayload {
  return {
    dueDate: payment.dueDate ?? "",
    amountDue: payment.amountDue == null ? "" : String(payment.amountDue),
  };
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
  const date = new Date(value);
  return `${String(date.getDate()).padStart(2, "0")}.${String(date.getMonth() + 1).padStart(2, "0")}.${date.getFullYear()}`;
}

function ContactHistoryTab({
  events,
  loading,
  error,
  customerId,
  canManage,
  recordEvent,
  busy,
  mutationError,
  eventTypeFilter,
  onEventTypeFilterChange,
}: {
  events: ContactEventView[];
  loading: boolean;
  error: boolean;
  customerId: string;
  canManage: boolean;
  recordEvent: (payload: RecordContactEventPayload) => void;
  busy: boolean;
  mutationError: unknown;
  eventTypeFilter: ContactEventType | "ALL";
  onEventTypeFilterChange: (eventType: ContactEventType | "ALL") => void;
}) {
  const [form, setForm] = useState<RecordContactEventPayload>({
    ...emptyRecordContactEventForm,
  });

  function handleSubmit() {
    recordEvent({
      ...form,
      customerId,
      occurredAt: new Date().toISOString(),
    });
    setForm({ ...emptyRecordContactEventForm });
  }

  return (
    <section className="panel" role="tabpanel" aria-labelledby="contact-history-heading">
      <div className="section-heading">
        <h2 id="contact-history-heading">Contact history</h2>
        <div className="heading-actions">
          <label className="sr-only" htmlFor="contact-history-event-type-filter">
            Filter contact history by event type
          </label>
          <select
            id="contact-history-event-type-filter"
            value={eventTypeFilter}
            onChange={(event) =>
              onEventTypeFilterChange(event.target.value as ContactEventType | "ALL")
            }
          >
            <option value="ALL">All event types</option>
            {contactEventTypes.map((eventType) => (
              <option key={eventType} value={eventType}>
                {formatEnum(eventType)}
              </option>
            ))}
          </select>
          <span>
            {loading ? "Loading contact history" : formatCount(events.length, "contact event")}
          </span>
        </div>
      </div>
      <p className="table-state">
        Calls, emails, SMS, outcomes, replies, conversions, and communication notes.
      </p>
      {canManage ? (
        <form
          className="form-grid"
          aria-label="Record contact event"
          onSubmit={(event) => {
            event.preventDefault();
            handleSubmit();
          }}
        >
          <label>
            Channel
            <select
              required
              aria-label="Contact channel"
              value={form.channel}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  channel: event.target.value as RecordContactEventPayload["channel"],
                }))
              }
            >
              <option value="">Select channel</option>
              <option value="EMAIL">Email</option>
              <option value="SMS">SMS</option>
              <option value="PHONE">Phone</option>
              <option value="IN_APP">In-App</option>
            </select>
          </label>
          <label>
            Event type
            <select
              required
              aria-label="Event type"
              value={form.eventType}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  eventType: event.target.value as RecordContactEventPayload["eventType"],
                }))
              }
            >
              <option value="">Select event</option>
              <option value="SENT">Sent</option>
              <option value="OPENED">Opened</option>
              <option value="CLICKED">Clicked</option>
              <option value="REPLIED">Replied</option>
              <option value="FAILED">Failed</option>
              <option value="UNSUBSCRIBED">Unsubscribed</option>
              <option value="CALLED">Called</option>
              <option value="NOTE">Note</option>
            </select>
          </label>
          <label>
            Outcome (optional)
            <select
              aria-label="Contact outcome"
              value={form.outcome ?? ""}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  outcome: (event.target.value as ContactOutcome | "") || undefined,
                }))
              }
            >
              <option value="">No outcome</option>
              <option value="INTERESTED">Interested</option>
              <option value="NOT_INTERESTED">Not interested</option>
              <option value="CONVERTED">Converted</option>
              <option value="NO_RESPONSE">No response</option>
              <option value="FAILED">Failed</option>
            </select>
          </label>
          <label>
            Notes (optional)
            <input
              aria-label="Contact notes"
              maxLength={1000}
              value={form.notes ?? ""}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  notes: event.target.value,
                }))
              }
            />
          </label>
          <button type="submit" disabled={busy}>
            Record event
          </button>
          {mutationError ? (
            <p className="form-error" role="alert">
              Contact event could not be recorded.
            </p>
          ) : null}
        </form>
      ) : null}
      {loading ? <p className="table-state">Loading contact events.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Contact history could not be loaded.
        </p>
      ) : null}
      {!loading && !error && events.length === 0 ? (
        <p className="table-state">No contact history entries are available for this customer.</p>
      ) : null}
      {!loading && !error && events.length > 0 ? (
        <div className="table-scroll">
          <table aria-label="Contact history table">
            <thead>
              <tr>
                <th scope="col">Event</th>
                <th scope="col">Campaign</th>
                <th scope="col">Channel</th>
                <th scope="col">Occurred</th>
                <th scope="col">Details</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.id}>
                  <td>
                    <StatusBadge value={formatEnum(event.eventType)} />
                    {event.outcome == null ? null : (
                      <span className="table-secondary-text">{formatEnum(event.outcome)}</span>
                    )}
                  </td>
                  <td>
                    <span className="table-primary-text">
                      {event.campaignName ?? "No campaign"}
                    </span>
                    <span className="table-secondary-text">
                      {event.campaignId ?? "No campaign id"}
                    </span>
                  </td>
                  <td>{formatEnum(event.channel)}</td>
                  <td>{formatDateTime(event.occurredAt)}</td>
                  <td>
                    <span className="table-primary-text">
                      {event.notes == null || event.notes.trim().length === 0
                        ? "No notes"
                        : event.notes}
                    </span>
                    <span className="table-secondary-text">
                      {event.createdByFullName ?? event.createdByUserId ?? "System or provider"}
                    </span>
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

function formatDateTime(value: CustomerView["updatedAt"]) {
  if (value == null) {
    return null;
  }
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatPolicyNumber(value: string | null) {
  return value == null || value.length === 0 ? "Not provided" : value;
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
