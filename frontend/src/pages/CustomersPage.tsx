import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { isAuthorizationError } from "@/api/client";
import { useAuth } from "@/auth/AuthProvider";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  createCustomer,
  deleteCustomer,
  importCustomersCsv,
  listCustomers,
  updateCustomer,
  type CustomerAgeGroup,
  type CustomerFormPayload,
  type CustomerImportResult,
  type CustomerSearchFilters,
  type CustomerStatus,
  type CustomerType,
  type CustomerView,
} from "@/api/customers";
import { StatusBadge } from "@/components/StatusBadge";

const CUSTOMER_TYPES: CustomerType[] = ["CUSTOMER", "PROSPECT", "BENEFICIARY"];
const AGE_GROUPS: Array<CustomerAgeGroup | ""> = [
  "",
  "MINOR",
  "AGE_18_25",
  "AGE_26_40",
  "AGE_41_60",
  "AGE_60_PLUS",
];
const CUSTOMER_STATUSES: CustomerStatus[] = [
  "ACTIVE",
  "INACTIVE",
  "INTERESTED",
  "UNINTERESTED",
  "CONVERTED",
];
const STATUS_FILTERS: Array<CustomerStatus | "ALL"> = ["ALL", ...CUSTOMER_STATUSES];
const TYPE_FILTERS: Array<CustomerType | "ALL"> = ["ALL", ...CUSTOMER_TYPES];
const CONTACTABLE_FILTERS: CustomerSearchFilters["contactable"][] = ["ALL", "true", "false"];
const CUSTOMER_CREATE_ROLES: SystemRoleName[] = ["ADMIN", "CUSTOMER_SERVICE_AGENT"];
const CUSTOMER_UPDATE_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CUSTOMER_SERVICE_AGENT",
  "COMPLIANCE_OFFICER",
];
const CUSTOMER_DELETE_ROLES: SystemRoleName[] = ["ADMIN"];
const CUSTOMER_IMPORT_ROLES: SystemRoleName[] = ["ADMIN", "CUSTOMER_SERVICE_AGENT"];

const emptyCustomerForm: CustomerFormPayload = {
  customerType: "CUSTOMER",
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  addressLine: "",
  city: "",
  country: "",
  dateOfBirth: "",
  ageGroup: "",
  status: "ACTIVE",
  doNotContact: false,
  source: "",
};

const emptySearchFilters: CustomerSearchFilters = {
  term: "",
  customerType: "ALL",
  status: "ALL",
  city: "",
  country: "",
  contactable: "ALL",
};

export function CustomersPage() {
  const { hasAnyRole } = useAuth();
  const queryClient = useQueryClient();
  const [selectedCustomerId, setSelectedCustomerId] = useState("");
  const [createForm, setCreateForm] = useState<CustomerFormPayload>(emptyCustomerForm);
  const [editForm, setEditForm] = useState<CustomerFormPayload | null>(null);
  const [draftFilters, setDraftFilters] = useState<CustomerSearchFilters>(emptySearchFilters);
  const [appliedFilters, setAppliedFilters] = useState<CustomerSearchFilters>(emptySearchFilters);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<CustomerImportResult | null>(null);
  const [notice, setNotice] = useState("");

  const customersQuery = useQuery({
    queryKey: ["customers", appliedFilters],
    queryFn: () => listCustomers(appliedFilters),
  });
  const activeFilterCount = countActiveFilters(appliedFilters);

  const customers = useMemo(() => customersQuery.data ?? [], [customersQuery.data]);
  const selectedCustomer = useMemo(() => {
    if (selectedCustomerId === "") {
      return customers[0];
    }

    return customers.find((candidate) => candidate.id === selectedCustomerId);
  }, [customers, selectedCustomerId]);

  const selectedCustomerForm =
    selectedCustomer == null
      ? null
      : editForm == null
        ? customerToForm(selectedCustomer)
        : editForm;

  const refreshCustomers = async () => {
    await queryClient.invalidateQueries({ queryKey: ["customers"] });
  };
  const canCreateCustomers = hasAnyRole(CUSTOMER_CREATE_ROLES);
  const canUpdateCustomers = hasAnyRole(CUSTOMER_UPDATE_ROLES);
  const canDeleteCustomers = hasAnyRole(CUSTOMER_DELETE_ROLES);
  const canImportCustomers = hasAnyRole(CUSTOMER_IMPORT_ROLES);
  const canManageCustomers =
    canCreateCustomers || canUpdateCustomers || canDeleteCustomers || canImportCustomers;

  const createMutation = useMutation({
    mutationFn: createCustomer,
    onSuccess: async (createdCustomer) => {
      setCreateForm(emptyCustomerForm);
      setSelectedCustomerId(createdCustomer.id);
      setEditForm(customerToForm(createdCustomer));
      setNotice("Customer created.");
      await refreshCustomers();
    },
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      updateCustomer(selectedCustomer?.id ?? "", selectedCustomerForm ?? emptyCustomerForm),
    onSuccess: async (updatedCustomer) => {
      setSelectedCustomerId(updatedCustomer.id);
      setEditForm(customerToForm(updatedCustomer));
      setNotice("Customer updated.");
      await refreshCustomers();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteCustomer(selectedCustomer?.id ?? ""),
    onSuccess: async () => {
      setSelectedCustomerId("");
      setEditForm(null);
      setNotice("Customer deleted.");
      await refreshCustomers();
    },
  });

  const importMutation = useMutation({
    mutationFn: () => {
      if (importFile == null) {
        throw new Error("Choose a CSV file before importing.");
      }

      return importCustomersCsv(importFile);
    },
    onSuccess: async (result) => {
      setImportResult(result);
      setNotice("CSV import completed.");
      await refreshCustomers();
    },
  });

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    deleteMutation.isPending ||
    importMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(
      createMutation.error,
      updateMutation.error,
      deleteMutation.error,
      importMutation.error,
    ) ||
    generalErrorMessage(
      createMutation.error,
      updateMutation.error,
      deleteMutation.error,
      importMutation.error,
    );

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Customers and prospects</h2>
          <span>Profile, consent status, products, beneficiaries, and contact history</span>
        </div>
        <form
          className="toolbar-row"
          aria-label="Customer search filters"
          onSubmit={(event) => {
            event.preventDefault();
            setSelectedCustomerId("");
            setEditForm(null);
            setAppliedFilters(normalizeSearchFilters(draftFilters));
          }}
        >
          <label>
            Search
            <input
              aria-label="Search customers"
              placeholder="Name, email, or phone"
              value={draftFilters.term}
              onChange={(event) =>
                setDraftFilters((current) => ({ ...current, term: event.target.value }))
              }
            />
          </label>
          <label>
            Type
            <select
              aria-label="Customer type filter"
              value={draftFilters.customerType}
              onChange={(event) =>
                setDraftFilters((current) => ({
                  ...current,
                  customerType: event.target.value as CustomerSearchFilters["customerType"],
                }))
              }
            >
              {TYPE_FILTERS.map((type) => (
                <option key={type} value={type}>
                  {formatEnum(type)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Status
            <select
              aria-label="Customer status filter"
              value={draftFilters.status}
              onChange={(event) =>
                setDraftFilters((current) => ({
                  ...current,
                  status: event.target.value as CustomerSearchFilters["status"],
                }))
              }
            >
              {STATUS_FILTERS.map((status) => (
                <option key={status} value={status}>
                  {formatEnum(status)}
                </option>
              ))}
            </select>
          </label>
          <label>
            City
            <input
              aria-label="Customer city filter"
              value={draftFilters.city}
              onChange={(event) =>
                setDraftFilters((current) => ({ ...current, city: event.target.value }))
              }
            />
          </label>
          <label>
            Country
            <input
              aria-label="Customer country filter"
              value={draftFilters.country}
              onChange={(event) =>
                setDraftFilters((current) => ({ ...current, country: event.target.value }))
              }
            />
          </label>
          <label>
            Contactable
            <select
              aria-label="Customer contactable filter"
              value={draftFilters.contactable}
              onChange={(event) =>
                setDraftFilters((current) => ({
                  ...current,
                  contactable: event.target.value as CustomerSearchFilters["contactable"],
                }))
              }
            >
              {CONTACTABLE_FILTERS.map((contactable) => (
                <option key={contactable} value={contactable}>
                  {formatContactableFilter(contactable)}
                </option>
              ))}
            </select>
          </label>
          <button type="submit">Apply filters</button>
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              setSelectedCustomerId("");
              setEditForm(null);
              setDraftFilters(emptySearchFilters);
              setAppliedFilters(emptySearchFilters);
            }}
          >
            Reset filters
          </button>
          <span className="filter-count" aria-live="polite">
            {activeFilterCount === 0
              ? "No active filters"
              : `${activeFilterCount} active ${activeFilterCount === 1 ? "filter" : "filters"}`}
          </span>
          {notice ? <p className="form-success">{notice}</p> : null}
          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}
        </form>
      </div>

      {canManageCustomers ? (
        <div className="split-grid user-management-grid">
          {canCreateCustomers ? (
            <section className="panel" aria-labelledby="create-customer-heading">
              <div className="section-heading">
                <h2 id="create-customer-heading">Create customer</h2>
                <span>Customer, prospect, or beneficiary profile</span>
              </div>
              <CustomerForm
                submitLabel="Create customer"
                value={createForm}
                disabled={isBusy}
                includeCustomerType
                onChange={setCreateForm}
                onSubmit={() => {
                  setNotice("");
                  createMutation.mutate(createForm);
                }}
              />
            </section>
          ) : null}

          {canUpdateCustomers || canDeleteCustomers ? (
            <section className="panel" aria-labelledby="edit-customer-heading">
              <div className="section-heading">
                <h2 id="edit-customer-heading">Edit customer</h2>
                <span>Contact details, status, and communication preference</span>
              </div>
              {selectedCustomer == null || selectedCustomerForm == null ? (
                <p>No customer profiles are available.</p>
              ) : (
                <div className="form-grid">
                  <label>
                    Customer
                    <select
                      value={selectedCustomer.id}
                      onChange={(event) => {
                        const nextCustomer = customers.find(
                          (candidate) => candidate.id === event.target.value,
                        );
                        setSelectedCustomerId(event.target.value);
                        setEditForm(nextCustomer == null ? null : customerToForm(nextCustomer));
                      }}
                    >
                      {customers.map((customer) => (
                        <option key={customer.id} value={customer.id}>
                          {customer.fullName}
                        </option>
                      ))}
                    </select>
                  </label>
                  {canUpdateCustomers ? (
                    <CustomerForm
                      submitLabel="Save customer"
                      value={selectedCustomerForm}
                      disabled={isBusy}
                      onChange={setEditForm}
                      onSubmit={() => {
                        setNotice("");
                        updateMutation.mutate();
                      }}
                    />
                  ) : null}
                  {canDeleteCustomers ? (
                    <button
                      type="button"
                      className="danger-button"
                      disabled={isBusy}
                      onClick={() => {
                        setNotice("");
                        deleteMutation.mutate();
                      }}
                    >
                      Delete customer
                    </button>
                  ) : null}
                </div>
              )}
            </section>
          ) : null}
        </div>
      ) : (
        <p className="table-state">Customer management actions are hidden for your role.</p>
      )}

      {canImportCustomers ? (
        <section className="panel" aria-labelledby="csv-import-heading">
          <div className="section-heading">
            <h2 id="csv-import-heading">CSV import</h2>
            <span>Bulk customer and prospect onboarding</span>
          </div>
          <CustomerImportPanel
            file={importFile}
            result={importResult}
            disabled={isBusy}
            onFileChange={(file) => {
              setImportFile(file);
              setImportResult(null);
            }}
            onSubmit={() => {
              setNotice("");
              importMutation.mutate();
            }}
          />
        </section>
      ) : null}

      <section className="panel" aria-labelledby="customers-table-heading">
        <div className="section-heading">
          <h2 id="customers-table-heading">Customer list</h2>
          <span>
            {customersQuery.isLoading ? "Loading customers" : `${customers.length} records`}
          </span>
        </div>
        <CustomerListTable
          activeFilterCount={activeFilterCount}
          customers={customers}
          error={customersQuery.isError}
          loading={customersQuery.isLoading}
          selectedCustomerId={selectedCustomer?.id}
          canSelectForEditing={canUpdateCustomers || canDeleteCustomers}
          onSelect={(customer) => {
            setSelectedCustomerId(customer.id);
            setEditForm(customerToForm(customer));
          }}
          onRetry={() => void customersQuery.refetch()}
        />
      </section>
    </section>
  );
}

type CustomerListTableProps = {
  activeFilterCount: number;
  customers: CustomerView[];
  error: boolean;
  loading: boolean;
  selectedCustomerId?: string;
  canSelectForEditing: boolean;
  onSelect: (customer: CustomerView) => void;
  onRetry: () => void;
};

type CustomerImportPanelProps = {
  file: File | null;
  result: CustomerImportResult | null;
  disabled: boolean;
  onFileChange: (file: File | null) => void;
  onSubmit: () => void;
};

function CustomerImportPanel({
  file,
  result,
  disabled,
  onFileChange,
  onSubmit,
}: CustomerImportPanelProps) {
  return (
    <div className="page-stack">
      <form
        className="toolbar-row"
        aria-label="Customer CSV import"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit();
        }}
      >
        <label>
          CSV file
          <input
            type="file"
            accept=".csv,text/csv"
            aria-label="Customer CSV file"
            onChange={(event) => onFileChange(event.target.files?.[0] ?? null)}
          />
        </label>
        <span className="filter-count" aria-live="polite">
          {file == null ? "No CSV selected" : file.name}
        </span>
        <button type="submit" disabled={disabled || file == null}>
          Import CSV
        </button>
      </form>

      {result == null ? (
        <p className="table-state">
          CSV rows are validated individually so accepted customers can import while invalid rows
          remain visible for correction.
        </p>
      ) : (
        <div className="split-grid user-management-grid">
          <div className="detail-summary" aria-label="CSV import summary">
            <div>
              <span className="eyebrow">Imported</span>
              <strong>{result.importedCount}</strong>
            </div>
            <div>
              <span className="eyebrow">Failed</span>
              <strong>{result.failedCount}</strong>
            </div>
            <div>
              <span className="eyebrow">Created records</span>
              <strong>{result.customers.length}</strong>
            </div>
            <div>
              <span className="eyebrow">Row errors</span>
              <strong>{result.errors.length}</strong>
            </div>
          </div>
          <ImportErrorTable errors={result.errors} />
        </div>
      )}
    </div>
  );
}

function ImportErrorTable({ errors }: { errors: CustomerImportResult["errors"] }) {
  if (errors.length === 0) {
    return <p className="table-state">No row-level import errors.</p>;
  }

  return (
    <div className="table-scroll">
      <table aria-label="CSV import errors table">
        <thead>
          <tr>
            <th scope="col">Line</th>
            <th scope="col">Field</th>
            <th scope="col">Message</th>
            <th scope="col">Value</th>
          </tr>
        </thead>
        <tbody>
          {errors.map((error) => (
            <tr key={`${error.lineNumber}-${error.field}-${error.message}`}>
              <th scope="row">{error.lineNumber}</th>
              <td>{error.field}</td>
              <td>{error.message}</td>
              <td>{error.value ?? "Not provided"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CustomerListTable({
  activeFilterCount,
  customers,
  error,
  loading,
  selectedCustomerId,
  canSelectForEditing,
  onSelect,
  onRetry,
}: CustomerListTableProps) {
  if (loading) {
    return (
      <div className="state-panel" role="status" aria-live="polite">
        <strong>Loading customer records</strong>
        <p>Customer and prospect data is being loaded from the CRM service.</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="state-panel error-state" role="alert">
        <strong>Customer records could not be loaded.</strong>
        <p>Check the connection or try again before continuing customer work.</p>
        <button type="button" className="secondary-button" onClick={onRetry}>
          Retry
        </button>
      </div>
    );
  }

  if (customers.length === 0) {
    return (
      <div className="state-panel">
        <strong>
          {activeFilterCount === 0
            ? "No customer records are available."
            : "No customer records match the current filters."}
        </strong>
        <p>
          {activeFilterCount === 0
            ? "Create a customer or import prospects from CSV when your role allows it."
            : "Adjust the search fields or reset filters to broaden the customer list."}
        </p>
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table aria-label="Customer list table">
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Type</th>
            <th scope="col">Contact details</th>
            <th scope="col">Location</th>
            <th scope="col">Age group</th>
            <th scope="col">Status</th>
            <th scope="col">Marketing</th>
            <th scope="col">Source</th>
            <th scope="col">Updated</th>
            <th scope="col">Action</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr
              key={customer.id}
              className={customer.id === selectedCustomerId ? "selected-table-row" : undefined}
            >
              <th scope="row">
                <span className="table-primary-text">{customer.fullName}</span>
                <span className="table-secondary-text">{customer.id}</span>
              </th>
              <td>{formatEnum(customer.customerType)}</td>
              <td>
                <span className="table-primary-text">{customer.email ?? "Email not provided"}</span>
                <span className="table-secondary-text">
                  {customer.phone ?? "Phone not provided"}
                </span>
              </td>
              <td>{formatLocation(customer)}</td>
              <td>{customer.ageGroup == null ? "Not set" : formatEnum(customer.ageGroup)}</td>
              <td>
                <StatusBadge value={formatEnum(customer.status)} />
              </td>
              <td>
                <StatusBadge value={customer.doNotContact ? "Do not contact" : "Allowed"} />
              </td>
              <td>{customer.source ?? "Not provided"}</td>
              <td>{formatDateTime(customer.updatedAt)}</td>
              <td>
                <div className="table-action-group">
                  <Link className="secondary-link-button" to={`/customers/${customer.id}`}>
                    Details
                  </Link>
                  {canSelectForEditing ? (
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => onSelect(customer)}
                    >
                      Select
                    </button>
                  ) : null}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

type CustomerFormProps = {
  value: CustomerFormPayload;
  submitLabel: string;
  disabled: boolean;
  includeCustomerType?: boolean;
  onChange: (value: CustomerFormPayload) => void;
  onSubmit: () => void;
};

function CustomerForm({
  value,
  submitLabel,
  disabled,
  includeCustomerType = false,
  onChange,
  onSubmit,
}: CustomerFormProps) {
  const [errors, setErrors] = useState<CustomerFormErrors>({});
  const updateField = <TKey extends keyof CustomerFormPayload>(
    field: TKey,
    fieldValue: CustomerFormPayload[TKey],
  ) => {
    setErrors((current) => ({ ...current, [field]: undefined }));
    onChange({ ...value, [field]: fieldValue });
  };

  return (
    <form
      className="form-grid"
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        const nextErrors = validateCustomerForm(value, includeCustomerType);
        setErrors(nextErrors);
        if (Object.keys(nextErrors).length > 0) {
          return;
        }
        onSubmit();
      }}
    >
      {includeCustomerType ? (
        <label>
          Customer type
          <select
            required
            value={value.customerType ?? "CUSTOMER"}
            aria-invalid={Boolean(errors.customerType)}
            onChange={(event) => updateField("customerType", event.target.value as CustomerType)}
          >
            {CUSTOMER_TYPES.map((type) => (
              <option key={type} value={type}>
                {formatEnum(type)}
              </option>
            ))}
          </select>
          <FieldError message={errors.customerType} />
        </label>
      ) : null}
      <label>
        First name
        <input
          required
          maxLength={100}
          value={value.firstName}
          aria-invalid={Boolean(errors.firstName)}
          onChange={(event) => updateField("firstName", event.target.value)}
        />
        <FieldError message={errors.firstName} />
      </label>
      <label>
        Last name
        <input
          required
          maxLength={100}
          value={value.lastName}
          aria-invalid={Boolean(errors.lastName)}
          onChange={(event) => updateField("lastName", event.target.value)}
        />
        <FieldError message={errors.lastName} />
      </label>
      <label>
        Email
        <input
          type="email"
          maxLength={255}
          value={value.email}
          aria-invalid={Boolean(errors.email)}
          onChange={(event) => updateField("email", event.target.value)}
        />
        <FieldError message={errors.email} />
      </label>
      <label>
        Phone
        <input
          pattern="^\+?[0-9 ()-]{7,50}$"
          maxLength={50}
          value={value.phone}
          aria-invalid={Boolean(errors.phone)}
          onChange={(event) => updateField("phone", event.target.value)}
        />
        <FieldError message={errors.phone} />
      </label>
      <label>
        Address
        <input
          maxLength={255}
          value={value.addressLine}
          aria-invalid={Boolean(errors.addressLine)}
          onChange={(event) => updateField("addressLine", event.target.value)}
        />
        <FieldError message={errors.addressLine} />
      </label>
      <label>
        City
        <input
          maxLength={100}
          value={value.city}
          aria-invalid={Boolean(errors.city)}
          onChange={(event) => updateField("city", event.target.value)}
        />
        <FieldError message={errors.city} />
      </label>
      <label>
        Country
        <input
          maxLength={100}
          value={value.country}
          aria-invalid={Boolean(errors.country)}
          onChange={(event) => updateField("country", event.target.value)}
        />
        <FieldError message={errors.country} />
      </label>
      <label>
        Date of birth
        <input
          type="date"
          value={value.dateOfBirth}
          aria-invalid={Boolean(errors.dateOfBirth)}
          onChange={(event) => updateField("dateOfBirth", event.target.value)}
        />
        <FieldError message={errors.dateOfBirth} />
      </label>
      <label>
        Age group
        <select
          value={value.ageGroup}
          onChange={(event) => updateField("ageGroup", event.target.value as CustomerAgeGroup | "")}
        >
          {AGE_GROUPS.map((ageGroup) => (
            <option key={ageGroup || "none"} value={ageGroup}>
              {ageGroup === "" ? "Not set" : formatEnum(ageGroup)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Status
        <select
          value={value.status}
          onChange={(event) => updateField("status", event.target.value as CustomerStatus)}
        >
          {CUSTOMER_STATUSES.map((status) => (
            <option key={status} value={status}>
              {formatEnum(status)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Source
        <input
          maxLength={100}
          value={value.source}
          aria-invalid={Boolean(errors.source)}
          onChange={(event) => updateField("source", event.target.value)}
        />
        <FieldError message={errors.source} />
      </label>
      <label className="checkbox-label">
        <input
          type="checkbox"
          checked={value.doNotContact}
          onChange={(event) => updateField("doNotContact", event.target.checked)}
        />
        Do not contact
      </label>
      <button type="submit" disabled={disabled}>
        {submitLabel}
      </button>
    </form>
  );
}

type CustomerFormErrors = Partial<Record<keyof CustomerFormPayload, string>>;

function FieldError({ message }: { message?: string }) {
  return message == null ? null : <span className="field-error">{message}</span>;
}

function validateCustomerForm(
  value: CustomerFormPayload,
  includeCustomerType: boolean,
): CustomerFormErrors {
  const errors: CustomerFormErrors = {};
  if (includeCustomerType && value.customerType == null) {
    errors.customerType = "Customer type is required.";
  }
  if (value.firstName.trim().length === 0) {
    errors.firstName = "First name is required.";
  }
  if (value.lastName.trim().length === 0) {
    errors.lastName = "Last name is required.";
  }
  if (value.email.trim().length > 0 && !isValidEmail(value.email)) {
    errors.email = "Enter a valid email address.";
  }
  if (value.phone.trim().length > 0 && !isValidPhone(value.phone)) {
    errors.phone = "Use 7 to 50 digits, spaces, parentheses, hyphens, and an optional +.";
  }
  if (value.dateOfBirth !== "" && new Date(value.dateOfBirth) > startOfToday()) {
    errors.dateOfBirth = "Date of birth cannot be in the future.";
  }
  addMaxLengthError(errors, "firstName", value.firstName, 100);
  addMaxLengthError(errors, "lastName", value.lastName, 100);
  addMaxLengthError(errors, "email", value.email, 255);
  addMaxLengthError(errors, "phone", value.phone, 50);
  addMaxLengthError(errors, "addressLine", value.addressLine, 255);
  addMaxLengthError(errors, "city", value.city, 100);
  addMaxLengthError(errors, "country", value.country, 100);
  addMaxLengthError(errors, "source", value.source, 100);
  return errors;
}

function addMaxLengthError(
  errors: CustomerFormErrors,
  field: keyof CustomerFormPayload,
  value: string,
  maxLength: number,
) {
  if (value.length > maxLength) {
    errors[field] = `Must be ${maxLength} characters or fewer.`;
  }
}

function isValidEmail(value: string) {
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value.trim());
}

function isValidPhone(value: string) {
  return /^\+?[0-9 ()-]{7,50}$/.test(value.trim());
}

function startOfToday() {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today;
}

function customerToForm(customer: CustomerView): CustomerFormPayload {
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

function formatEnum(value: string) {
  if (value === "ALL") {
    return "All";
  }
  return value
    .replace(/^AGE_/, "")
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatContactableFilter(value: CustomerSearchFilters["contactable"]) {
  if (value === "ALL") {
    return "All";
  }
  return value === "true" ? "Contactable" : "Do not contact";
}

function formatLocation(customer: CustomerView) {
  const parts = [customer.city, customer.country].filter(Boolean);
  return parts.length === 0 ? "Not provided" : parts.join(", ");
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

function normalizeSearchFilters(filters: CustomerSearchFilters): CustomerSearchFilters {
  return {
    ...filters,
    term: filters.term.trim(),
    city: filters.city.trim(),
    country: filters.country.trim(),
  };
}

function countActiveFilters(filters: CustomerSearchFilters) {
  return [
    filters.term,
    filters.city,
    filters.country,
    filters.customerType === "ALL" ? "" : filters.customerType,
    filters.status === "ALL" ? "" : filters.status,
    filters.contactable === "ALL" ? "" : filters.contactable,
  ].filter((value) => value.trim().length > 0).length;
}

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError) ? "You are not authorized to manage customers." : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Customer action failed." : "";
}
