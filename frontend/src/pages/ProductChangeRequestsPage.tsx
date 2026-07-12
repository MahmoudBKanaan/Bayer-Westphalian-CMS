import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { isAuthorizationError } from "@/api/client";
import {
  approveProductChangeRequest,
  createProductChangeRequest,
  listProductChangeRequests,
  markProductChangeRequestImplemented,
  rejectProductChangeRequest,
  updateProductChangeRequest,
  type CreateProductChangeRequestPayload,
  type ProductChangeRequestSearchFilters,
  type ProductChangeStatus,
  type ProductChangeType,
} from "@/api/productChangeRequests";
import { listProducts } from "@/api/products";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

const CHANGE_REQUEST_TYPES: ProductChangeType[] = [
  "PRICE_CHANGE",
  "DURATION_CHANGE",
  "EXPIRATION_RULE_CHANGE",
  "STATUS_CHANGE",
];
const STATUS_FILTERS: Array<ProductChangeStatus | "ALL"> = [
  "ALL",
  "OPEN",
  "APPROVED",
  "REJECTED",
  "IMPLEMENTED",
];
const emptySearchFilters: ProductChangeRequestSearchFilters = {
  productId: "",
  status: "ALL",
};

const emptyCreateForm: CreateProductChangeRequestPayload = {
  productId: "",
  requestType: "PRICE_CHANGE",
  description: "",
};

export function ProductChangeRequestsPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const canManageRequests = permissions.canManageProducts();
  const [selectedRequestId, setSelectedRequestId] = useState("");
  const [createForm, setCreateForm] = useState<CreateProductChangeRequestPayload>(emptyCreateForm);
  const [editDescription, setEditDescription] = useState<string | null>(null);
  const [draftFilters, setDraftFilters] = useState(emptySearchFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptySearchFilters);
  const [notice, setNotice] = useState("");

  const productsQuery = useQuery({
    queryKey: ["products", "catalog"],
    queryFn: () => listProducts(),
  });
  const requestsQuery = useQuery({
    queryKey: ["product-change-requests", appliedFilters],
    queryFn: () => listProductChangeRequests(normalizeFilters(appliedFilters)),
  });

  const products = useMemo(() => productsQuery.data ?? [], [productsQuery.data]);
  const requests = useMemo(() => requestsQuery.data ?? [], [requestsQuery.data]);
  const selectedRequest = useMemo(() => {
    if (selectedRequestId === "") {
      return requests[0];
    }

    return requests.find((candidate) => candidate.id === selectedRequestId);
  }, [requests, selectedRequestId]);
  const selectedDescription =
    selectedRequest == null
      ? ""
      : editDescription == null
        ? selectedRequest.description
        : editDescription;
  const activeFilterCount = countActiveFilters(appliedFilters);

  const refreshRequests = async () => {
    await queryClient.invalidateQueries({ queryKey: ["product-change-requests"] });
  };

  const createMutation = useMutation({
    mutationFn: createProductChangeRequest,
    onSuccess: async (createdRequest) => {
      setCreateForm(emptyCreateForm);
      setSelectedRequestId(createdRequest.id);
      setEditDescription(createdRequest.description);
      setNotice("Product change request created.");
      await refreshRequests();
    },
  });
  const updateMutation = useMutation({
    mutationFn: () => updateProductChangeRequest(selectedRequest?.id ?? "", selectedDescription),
    onSuccess: async (updatedRequest) => {
      setSelectedRequestId(updatedRequest.id);
      setEditDescription(updatedRequest.description);
      setNotice("Product change request updated.");
      await refreshRequests();
    },
  });
  const approveMutation = useMutation({
    mutationFn: () => approveProductChangeRequest(selectedRequest?.id ?? ""),
    onSuccess: async (approvedRequest) => {
      setSelectedRequestId(approvedRequest.id);
      setEditDescription(approvedRequest.description);
      setNotice("Product change request approved.");
      await refreshRequests();
    },
  });
  const rejectMutation = useMutation({
    mutationFn: () => rejectProductChangeRequest(selectedRequest?.id ?? ""),
    onSuccess: async (rejectedRequest) => {
      setSelectedRequestId(rejectedRequest.id);
      setEditDescription(rejectedRequest.description);
      setNotice("Product change request rejected.");
      await refreshRequests();
    },
  });
  const implementMutation = useMutation({
    mutationFn: () => markProductChangeRequestImplemented(selectedRequest?.id ?? ""),
    onSuccess: async (implementedRequest) => {
      setSelectedRequestId(implementedRequest.id);
      setEditDescription(implementedRequest.description);
      setNotice("Product change request marked implemented.");
      await refreshRequests();
    },
  });

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    approveMutation.isPending ||
    rejectMutation.isPending ||
    implementMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(
      requestsQuery.error,
      createMutation.error,
      updateMutation.error,
      approveMutation.error,
      rejectMutation.error,
      implementMutation.error,
    ) ||
    generalErrorMessage(
      requestsQuery.error,
      createMutation.error,
      updateMutation.error,
      approveMutation.error,
      rejectMutation.error,
      implementMutation.error,
    );

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Product change requests</h2>
          <span>Create, update, and track product modification workflow</span>
        </div>
        <form
          className="toolbar-row"
          aria-label="Product change request filters"
          onSubmit={(event) => {
            event.preventDefault();
            setSelectedRequestId("");
            setEditDescription(null);
            setAppliedFilters(normalizeFilters(draftFilters));
          }}
        >
          <label>
            Product
            <select
              aria-label="Product filter"
              value={draftFilters.productId ?? ""}
              onChange={(event) =>
                setDraftFilters((current) => ({
                  ...current,
                  productId: event.target.value,
                }))
              }
            >
              <option value="">All products</option>
              {products.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Status
            <select
              aria-label="Change request status filter"
              value={draftFilters.status ?? "ALL"}
              onChange={(event) =>
                setDraftFilters((current) => ({
                  ...current,
                  status: event.target.value as ProductChangeRequestSearchFilters["status"],
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
          <button type="submit">Apply filters</button>
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              setSelectedRequestId("");
              setEditDescription(null);
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

      {canManageRequests ? (
        <div className="split-grid user-management-grid">
          <section className="panel" aria-labelledby="create-change-request-heading">
            <div className="section-heading">
              <h2 id="create-change-request-heading">Create request</h2>
              <span>Submit a product data change for review</span>
            </div>
            <form
              className="form-grid"
              onSubmit={(event) => {
                event.preventDefault();
                setNotice("");
                createMutation.mutate(createForm);
              }}
            >
              <label>
                Product
                <select
                  required
                  aria-label="Product for change request"
                  value={createForm.productId}
                  onChange={(event) =>
                    setCreateForm((current) => ({
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
                Change type
                <select
                  aria-label="Product change request type"
                  value={createForm.requestType}
                  onChange={(event) =>
                    setCreateForm((current) => ({
                      ...current,
                      requestType: event.target.value as ProductChangeType,
                    }))
                  }
                >
                  {CHANGE_REQUEST_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {formatEnum(type)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Description
                <textarea
                  required
                  aria-label="Product change request description"
                  value={createForm.description}
                  onChange={(event) =>
                    setCreateForm((current) => ({
                      ...current,
                      description: event.target.value,
                    }))
                  }
                />
              </label>
              <button type="submit" disabled={isBusy}>
                Create request
              </button>
            </form>
          </section>

          <section className="panel" aria-labelledby="selected-change-request-heading">
            <div className="section-heading">
              <h2 id="selected-change-request-heading">Selected request</h2>
              <span>Update, approve, reject, or mark implemented</span>
            </div>
            {selectedRequest == null ? (
              <p>No change requests match the selected filter.</p>
            ) : (
              <div className="form-grid">
                <label>
                  Request
                  <select
                    aria-label="Selected change request"
                    value={selectedRequest.id}
                    onChange={(event) => {
                      const nextRequest = requests.find(
                        (candidate) => candidate.id === event.target.value,
                      );
                      setSelectedRequestId(event.target.value);
                      setEditDescription(nextRequest?.description ?? null);
                    }}
                  >
                    {requests.map((candidate) => (
                      <option key={candidate.id} value={candidate.id}>
                        {formatEnum(candidate.requestType)} — {candidate.productName}
                      </option>
                    ))}
                  </select>
                </label>
                <dl className="detail-list">
                  <DetailItem label="Product" value={selectedRequest.productName} />
                  <DetailItem
                    label="Product link"
                    value={
                      selectedRequest.productId == null
                        ? null
                        : `/products/${selectedRequest.productId}`
                    }
                    renderValue={(value) =>
                      value == null ? (
                        "Not provided"
                      ) : (
                        <Link className="secondary-link-button" to={value}>
                          View product details
                        </Link>
                      )
                    }
                  />
                  <DetailItem label="Requested by" value={selectedRequest.requestedByFullName} />
                  <DetailItem label="Status" value={formatEnum(selectedRequest.status)} />
                  <DetailItem label="Created" value={formatDateTime(selectedRequest.createdAt)} />
                  <DetailItem label="Updated" value={formatDateTime(selectedRequest.updatedAt)} />
                </dl>
                <label>
                  Description
                  <textarea
                    aria-label="Selected change request description"
                    value={selectedDescription}
                    disabled={selectedRequest.status !== "OPEN"}
                    onChange={(event) => setEditDescription(event.target.value)}
                  />
                </label>
                <div className="button-row">
                  <button
                    type="button"
                    disabled={isBusy || selectedRequest.status !== "OPEN"}
                    onClick={() => {
                      setNotice("");
                      updateMutation.mutate();
                    }}
                  >
                    Save description
                  </button>
                  <button
                    type="button"
                    disabled={isBusy || selectedRequest.status !== "OPEN"}
                    onClick={() => {
                      setNotice("");
                      approveMutation.mutate();
                    }}
                  >
                    Approve request
                  </button>
                  <button
                    type="button"
                    disabled={isBusy || selectedRequest.status !== "OPEN"}
                    onClick={() => {
                      setNotice("");
                      rejectMutation.mutate();
                    }}
                  >
                    Reject request
                  </button>
                  <button
                    type="button"
                    disabled={isBusy || selectedRequest.status !== "APPROVED"}
                    onClick={() => {
                      setNotice("");
                      implementMutation.mutate();
                    }}
                  >
                    Mark implemented
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      ) : null}

      <div className="panel">
        <div className="section-heading">
          <h2>Request tracker</h2>
          <span>
            {requestsQuery.isLoading
              ? "Loading change requests"
              : formatCount(requests.length, "request")}
          </span>
        </div>
        {requestsQuery.isLoading ? (
          <p className="table-state">Loading product change requests.</p>
        ) : null}
        {requestsQuery.isError ? (
          <p className="form-error" role="alert">
            Product change requests could not be loaded.
          </p>
        ) : null}
        {!requestsQuery.isLoading && !requestsQuery.isError && requests.length === 0 ? (
          <p className="table-state">No change requests match the current filters.</p>
        ) : null}
        {!requestsQuery.isLoading && !requestsQuery.isError && requests.length > 0 ? (
          <table aria-label="Product change requests table">
            <thead>
              <tr>
                <th>Request</th>
                <th>Product</th>
                <th>Requested by</th>
                <th>Status</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((request) => (
                <tr
                  key={request.id}
                  className={selectedRequest?.id === request.id ? "selected-row" : undefined}
                  onClick={() => {
                    setSelectedRequestId(request.id);
                    setEditDescription(request.description);
                  }}
                >
                  <td>
                    <span className="table-primary-text">{formatEnum(request.requestType)}</span>
                    <span className="table-secondary-text">{request.description}</span>
                  </td>
                  <td>
                    {request.productId == null ? (
                      (request.productName ?? "Unknown")
                    ) : (
                      <Link to={`/products/${request.productId}`}>{request.productName}</Link>
                    )}
                  </td>
                  <td>{request.requestedByFullName ?? "Unknown"}</td>
                  <td>
                    <StatusBadge value={formatEnum(request.status)} />
                  </td>
                  <td>{formatDateTime(request.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}

function DetailItem({
  label,
  value,
  renderValue,
}: {
  label: string;
  value: string | null;
  renderValue?: (value: string | null) => ReactNode;
}) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>
        {renderValue == null
          ? value == null || value.length === 0
            ? "Not provided"
            : value
          : renderValue(value)}
      </dd>
    </div>
  );
}

function formatEnum(value: string) {
  if (value === "ALL") {
    return "All";
  }
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
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

function normalizeFilters(
  filters: ProductChangeRequestSearchFilters,
): ProductChangeRequestSearchFilters {
  return {
    productId: filters.productId?.trim() ?? "",
    status: filters.status ?? "ALL",
  };
}

function countActiveFilters(filters: ProductChangeRequestSearchFilters) {
  return [
    filters.productId?.trim() ?? "",
    filters.status === "ALL" || filters.status == null ? "" : filters.status,
  ].filter((value) => value.length > 0).length;
}

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError)
    ? "You are not authorized to manage product change requests."
    : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Product change request action failed." : "";
}
