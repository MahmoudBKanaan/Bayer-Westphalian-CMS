import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { isAuthorizationError } from "@/api/client";
import {
  createProductChangeRequest,
  listProductChangeRequests,
  type CreateProductChangeRequestPayload,
  type ProductChangeRequestView,
  type ProductChangeType,
} from "@/api/productChangeRequests";
import {
  disableProduct,
  deleteProduct,
  getProduct,
  updateProduct,
  type ProductFormPayload,
  type ProductType,
  type ProductView,
} from "@/api/products";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

const PRODUCT_TYPES: ProductType[] = [
  "HOMEOWNER_INSURANCE",
  "LIFE_INSURANCE",
  "INVESTMENT_FUND",
  "HEALTH_INSURANCE",
  "AUTO_INSURANCE",
  "OTHER",
];
const CHANGE_REQUEST_TYPES: ProductChangeType[] = [
  "PRICE_CHANGE",
  "DURATION_CHANGE",
  "EXPIRATION_RULE_CHANGE",
  "STATUS_CHANGE",
];
const emptyChangeRequestForm: CreateProductChangeRequestPayload = {
  productId: "",
  requestType: "PRICE_CHANGE",
  description: "",
};

export function ProductDetailsPage() {
  const { productId } = useParams();
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const hasProductId = productId != null && productId.trim().length > 0;
  const canManageProducts = permissions.canManageProducts();
  const [editForm, setEditForm] = useState<{
    productId: string;
    payload: ProductFormPayload;
  } | null>(null);
  const [changeRequestForm, setChangeRequestForm] =
    useState<CreateProductChangeRequestPayload>(emptyChangeRequestForm);
  const [notice, setNotice] = useState("");

  const productQuery = useQuery({
    queryKey: ["product", productId],
    queryFn: () => getProduct(productId ?? ""),
    enabled: hasProductId,
  });
  const changeRequestsQuery = useQuery({
    queryKey: ["product-change-requests", "product", productId],
    queryFn: () => listProductChangeRequests({ productId: productId ?? "" }),
    enabled: hasProductId,
  });

  const refreshProduct = async () => {
    await queryClient.invalidateQueries({ queryKey: ["product", productId] });
    await queryClient.invalidateQueries({ queryKey: ["products"] });
  };
  const refreshChangeRequests = async () => {
    await queryClient.invalidateQueries({
      queryKey: ["product-change-requests", "product", productId],
    });
  };

  const updateMutation = useMutation({
    mutationFn: (payload: ProductFormPayload) => updateProduct(productId ?? "", payload),
    onSuccess: async (updatedProduct) => {
      setEditForm({ productId: updatedProduct.id, payload: productToForm(updatedProduct) });
      setNotice("Product updated.");
      await refreshProduct();
    },
  });
  const disableMutation = useMutation({
    mutationFn: () => disableProduct(productId ?? ""),
    onSuccess: async (disabledProduct) => {
      setEditForm({ productId: disabledProduct.id, payload: productToForm(disabledProduct) });
      setNotice("Product disabled.");
      await refreshProduct();
    },
  });
  const deleteMutation = useMutation({
    mutationFn: () => deleteProduct(productId ?? ""),
    onSuccess: async () => {
      setNotice("Product deleted.");
      await refreshProduct();
    },
  });
  const createChangeRequestMutation = useMutation({
    mutationFn: createProductChangeRequest,
    onSuccess: async () => {
      setChangeRequestForm({
        ...emptyChangeRequestForm,
        productId: productId ?? "",
        requestType: "PRICE_CHANGE",
      });
      setNotice("Product change request created.");
      await refreshChangeRequests();
    },
  });

  const isBusy =
    updateMutation.isPending ||
    disableMutation.isPending ||
    deleteMutation.isPending ||
    createChangeRequestMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(
      updateMutation.error,
      disableMutation.error,
      deleteMutation.error,
      createChangeRequestMutation.error,
    ) ||
    generalErrorMessage(
      updateMutation.error,
      disableMutation.error,
      deleteMutation.error,
      createChangeRequestMutation.error,
    );

  if (!hasProductId) {
    return (
      <section className="page-stack">
        <div className="panel">
          <h2>Product profile unavailable</h2>
          <p className="form-error" role="alert">
            A product identifier is required.
          </p>
          <Link className="secondary-link-button" to="/products">
            Back to products
          </Link>
        </div>
      </section>
    );
  }

  if (productQuery.isLoading) {
    return (
      <section className="page-stack">
        <div className="panel">
          <p className="table-state">Loading product profile.</p>
        </div>
      </section>
    );
  }

  if (productQuery.isError || productQuery.data == null) {
    return (
      <section className="page-stack">
        <div className="panel">
          <h2>Product profile unavailable</h2>
          <p className="form-error" role="alert">
            Product profile could not be loaded.
          </p>
          <Link className="secondary-link-button" to="/products">
            Back to products
          </Link>
        </div>
      </section>
    );
  }

  const product = productQuery.data;
  const productForm =
    editForm?.productId === product.id ? editForm.payload : productToForm(product);

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <div>
            <h2>Product details</h2>
            <span>{product.name}</span>
          </div>
          <Link className="secondary-link-button" to="/products">
            Back to products
          </Link>
        </div>
        <div className="detail-summary">
          <div>
            <span className="eyebrow">Type</span>
            <strong>{formatEnum(product.productType)}</strong>
          </div>
          <div>
            <span className="eyebrow">Status</span>
            <StatusBadge value={productStatusLabel(product)} />
          </div>
          <div>
            <span className="eyebrow">Price</span>
            <strong>{formatPrice(product.price)}</strong>
          </div>
          <div>
            <span className="eyebrow">Duration</span>
            <strong>{formatDuration(product.durationMonths)}</strong>
          </div>
        </div>
        {notice ? <p className="form-success">{notice}</p> : null}
        {errorMessage ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
      </div>

      <div className="split-grid user-management-grid">
        <section className="panel" aria-labelledby="product-profile-heading">
          <div className="section-heading">
            <h2 id="product-profile-heading">Profile</h2>
            <span>Core product definition for campaigns and reminders</span>
          </div>
          <dl className="detail-list">
            <DetailItem label="Product ID" value={product.id} />
            <DetailItem label="Name" value={product.name} />
            <DetailItem label="Type" value={formatEnum(product.productType)} />
            <DetailItem label="Description" value={product.description} />
            <DetailItem label="Price" value={formatPrice(product.price)} />
            <DetailItem label="Duration" value={formatDuration(product.durationMonths)} />
            <DetailItem label="Expiration policy" value={product.expirationPolicy} />
          </dl>
        </section>

        <section className="panel" aria-labelledby="product-activity-heading">
          <div className="section-heading">
            <h2 id="product-activity-heading">Activity</h2>
            <span>Lifecycle timestamps</span>
          </div>
          <dl className="detail-list">
            <DetailItem label="Created" value={formatDateTime(product.createdAt)} />
            <DetailItem label="Updated" value={formatDateTime(product.updatedAt)} />
            <DetailItem label="Deleted" value={formatDateTime(product.deletedAt)} />
          </dl>
        </section>
      </div>

      {canManageProducts ? (
        <section className="panel" aria-labelledby="product-edit-heading">
          <div className="section-heading">
            <h2 id="product-edit-heading">Edit product</h2>
            <span>Update price, duration, expiration rules, and status</span>
          </div>
          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault();
              setNotice("");
              updateMutation.mutate(productForm);
            }}
          >
            <label>
              Name
              <input
                required
                maxLength={255}
                aria-label="Product name"
                value={productForm.name}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: { ...productForm, name: event.target.value },
                  })
                }
              />
            </label>
            <label>
              Type
              <select
                aria-label="Product type"
                value={productForm.productType}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: {
                      ...productForm,
                      productType: event.target.value as ProductType,
                    },
                  })
                }
              >
                {PRODUCT_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {formatEnum(type)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Description
              <textarea
                aria-label="Product description"
                value={productForm.description}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: { ...productForm, description: event.target.value },
                  })
                }
              />
            </label>
            <label>
              Price
              <input
                aria-label="Product price"
                inputMode="decimal"
                placeholder="0.00"
                value={productForm.price}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: { ...productForm, price: event.target.value },
                  })
                }
              />
            </label>
            <label>
              Duration (months)
              <input
                aria-label="Product duration in months"
                inputMode="numeric"
                value={productForm.durationMonths}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: { ...productForm, durationMonths: event.target.value },
                  })
                }
              />
            </label>
            <label>
              Expiration policy
              <input
                maxLength={100}
                aria-label="Product expiration policy"
                value={productForm.expirationPolicy}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: { ...productForm, expirationPolicy: event.target.value },
                  })
                }
              />
            </label>
            <label>
              Active
              <select
                aria-label="Product active status"
                value={productForm.active ? "true" : "false"}
                onChange={(event) =>
                  setEditForm({
                    productId: product.id,
                    payload: {
                      ...productForm,
                      active: event.target.value === "true",
                    },
                  })
                }
              >
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </label>
            <div className="button-row">
              <button type="submit" disabled={isBusy}>
                Save changes
              </button>
              <button
                type="button"
                disabled={isBusy || !product.active || product.deleted}
                onClick={() => {
                  setNotice("");
                  disableMutation.mutate();
                }}
              >
                Disable product
              </button>
              <button
                type="button"
                disabled={isBusy || product.deleted}
                onClick={() => {
                  setNotice("");
                  deleteMutation.mutate();
                }}
              >
                Delete product
              </button>
            </div>
          </form>
        </section>
      ) : null}

      <div className="split-grid user-management-grid">
        <ChangeRequestsPanel
          requests={changeRequestsQuery.data ?? []}
          loading={changeRequestsQuery.isLoading}
          error={changeRequestsQuery.isError}
        />
        {canManageProducts ? (
          <section className="panel" aria-labelledby="create-change-request-heading">
            <div className="section-heading">
              <h2 id="create-change-request-heading">Request product change</h2>
              <span>Track price, duration, expiration, or status updates</span>
            </div>
            <form
              className="form-grid"
              onSubmit={(event) => {
                event.preventDefault();
                setNotice("");
                createChangeRequestMutation.mutate({
                  ...changeRequestForm,
                  productId: product.id,
                });
              }}
            >
              <label>
                Change type
                <select
                  aria-label="Product change request type"
                  value={changeRequestForm.requestType}
                  onChange={(event) =>
                    setChangeRequestForm((current) => ({
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
                  value={changeRequestForm.description}
                  onChange={(event) =>
                    setChangeRequestForm((current) => ({
                      ...current,
                      description: event.target.value,
                    }))
                  }
                />
              </label>
              <button type="submit" disabled={isBusy}>
                Create change request
              </button>
            </form>
          </section>
        ) : null}
      </div>
    </section>
  );
}

function ChangeRequestsPanel({
  requests,
  loading,
  error,
}: {
  requests: ProductChangeRequestView[];
  loading: boolean;
  error: boolean;
}) {
  return (
    <section className="panel" aria-labelledby="change-requests-heading">
      <div className="section-heading">
        <h2 id="change-requests-heading">Change requests</h2>
        <span>{loading ? "Loading change requests" : formatCount(requests.length, "request")}</span>
      </div>
      {loading ? <p className="table-state">Loading product change requests.</p> : null}
      {error ? (
        <p className="form-error" role="alert">
          Product change requests could not be loaded.
        </p>
      ) : null}
      {!loading && !error && requests.length === 0 ? (
        <p className="table-state">No change requests recorded for this product.</p>
      ) : null}
      {!loading && !error && requests.length > 0 ? (
        <table aria-label="Product change requests table">
          <thead>
            <tr>
              <th>Request</th>
              <th>Requested by</th>
              <th>Status</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {requests.map((request) => (
              <tr key={request.id}>
                <td>
                  <span className="table-primary-text">{formatEnum(request.requestType)}</span>
                  <span className="table-secondary-text">{request.description}</span>
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

function productToForm(product: ProductView): ProductFormPayload {
  return {
    name: product.name,
    productType: product.productType,
    description: product.description ?? "",
    price: product.price == null ? "" : String(product.price),
    durationMonths: product.durationMonths == null ? "" : String(product.durationMonths),
    expirationPolicy: product.expirationPolicy ?? "",
    active: product.active,
  };
}

function productStatusLabel(product: ProductView) {
  if (product.deleted) {
    return "Deleted";
  }
  return product.active ? "Active" : "Inactive";
}

function formatEnum(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatPrice(value: number | null) {
  if (value == null) {
    return "Not set";
  }
  return new Intl.NumberFormat("en", {
    style: "currency",
    currency: "EUR",
    minimumFractionDigits: 2,
  }).format(value);
}

function formatDuration(value: number | null) {
  if (value == null) {
    return "Not set";
  }
  return `${value} ${value === 1 ? "month" : "months"}`;
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

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError) ? "You are not authorized to manage products." : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Product action failed." : "";
}
