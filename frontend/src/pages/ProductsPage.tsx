import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { isAuthorizationError } from "@/api/client";
import {
  createProduct,
  deleteProduct,
  disableProduct,
  listProducts,
  updateProduct,
  type ProductFormPayload,
  type ProductSearchFilters,
  type ProductType,
  type ProductView,
} from "@/api/products";
import { ProductSearchFiltersPanel } from "@/components/ProductSearchFilters";
import { usePermissions } from "@/features/auth/usePermissions";
import { StatusBadge } from "@/components/StatusBadge";
import {
  emptyProductSearchFilters,
  formatProductCatalogSummary,
  formatProductEnum,
  hasActiveProductFilters,
  normalizeProductSearchFilters,
} from "@/features/products/productSearch";
import {
  PRODUCT_CREATE_FORM_ARIA_LABEL,
  PRODUCT_CREATE_SECTION_HEADING,
  PRODUCT_CREATE_SECTION_HINT,
  PRODUCT_CREATE_SUBMIT_LABEL,
  PRODUCT_CREATE_TYPES,
  PRODUCT_CREATED_NOTICE,
  PRODUCT_LIST_TABLE_ARIA_LABEL,
  PRODUCT_PAGE_HEADING,
  emptyProductForm,
  hasProductFormErrors,
  productViewToForm,
  validateProductForm,
  type ProductFormErrors,
} from "@/features/products/productCreationFlow";

export function ProductsPage() {
  const permissions = usePermissions();
  const queryClient = useQueryClient();
  const [selectedProductId, setSelectedProductId] = useState("");
  const [createForm, setCreateForm] = useState<ProductFormPayload>(() => emptyProductForm());
  const [editForm, setEditForm] = useState<ProductFormPayload | null>(null);
  const [draftFilters, setDraftFilters] = useState<ProductSearchFilters>(emptyProductSearchFilters);
  const [appliedFilters, setAppliedFilters] =
    useState<ProductSearchFilters>(emptyProductSearchFilters);
  const [notice, setNotice] = useState("");

  const productsQuery = useQuery({
    queryKey: ["products", appliedFilters],
    queryFn: () => listProducts(appliedFilters),
  });
  const canManageProducts = permissions.canManageProducts();
  const filtersAreActive = hasActiveProductFilters(appliedFilters);

  const products = useMemo(() => productsQuery.data ?? [], [productsQuery.data]);
  const selectedProduct = useMemo(() => {
    if (selectedProductId === "") {
      return products[0];
    }

    return products.find((candidate) => candidate.id === selectedProductId);
  }, [products, selectedProductId]);

  const selectedProductForm =
    selectedProduct == null
      ? null
      : editForm == null
        ? productViewToForm(selectedProduct)
        : editForm;

  const refreshProducts = async () => {
    await queryClient.invalidateQueries({ queryKey: ["products"] });
  };

  const createMutation = useMutation({
    mutationFn: createProduct,
    onSuccess: async (createdProduct) => {
      setCreateForm(emptyProductForm());
      setSelectedProductId(createdProduct.id);
      setEditForm(productViewToForm(createdProduct));
      setNotice(PRODUCT_CREATED_NOTICE);
      await refreshProducts();
    },
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      updateProduct(selectedProduct?.id ?? "", selectedProductForm ?? emptyProductForm()),
    onSuccess: async (updatedProduct) => {
      setSelectedProductId(updatedProduct.id);
      setEditForm(productViewToForm(updatedProduct));
      setNotice("Product updated.");
      await refreshProducts();
    },
  });

  const disableMutation = useMutation({
    mutationFn: () => disableProduct(selectedProduct?.id ?? ""),
    onSuccess: async (disabledProduct) => {
      setSelectedProductId(disabledProduct.id);
      setEditForm(productViewToForm(disabledProduct));
      setNotice("Product disabled.");
      await refreshProducts();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteProduct(selectedProduct?.id ?? ""),
    onSuccess: async () => {
      setSelectedProductId("");
      setEditForm(null);
      setNotice("Product deleted.");
      await refreshProducts();
    },
  });

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    disableMutation.isPending ||
    deleteMutation.isPending;
  const errorMessage =
    authorizationErrorMessage(
      productsQuery.error,
      createMutation.error,
      updateMutation.error,
      disableMutation.error,
      deleteMutation.error,
    ) ||
    generalErrorMessage(
      productsQuery.error,
      createMutation.error,
      updateMutation.error,
      disableMutation.error,
      deleteMutation.error,
    );

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>{PRODUCT_PAGE_HEADING}</h2>
          <span>Insurance and investment products used by campaigns and reminders</span>
        </div>
        <ProductSearchFiltersPanel
          draftFilters={draftFilters}
          appliedFilters={appliedFilters}
          notice={notice}
          errorMessage={errorMessage}
          onDraftChange={setDraftFilters}
          onApply={() => {
            setSelectedProductId("");
            setEditForm(null);
            setAppliedFilters(normalizeProductSearchFilters(draftFilters));
          }}
          onReset={() => {
            setSelectedProductId("");
            setEditForm(null);
            setDraftFilters(emptyProductSearchFilters);
            setAppliedFilters(emptyProductSearchFilters);
          }}
          onRemoveAppliedFilter={(filterKey) => {
            const clearFilter = (filters: ProductSearchFilters): ProductSearchFilters => {
              if (filterKey === "term") {
                return { ...filters, term: "" };
              }
              if (filterKey === "productType") {
                return { ...filters, productType: "ALL" };
              }
              if (filterKey === "active") {
                return { ...filters, active: "ALL" };
              }
              return filters;
            };

            setSelectedProductId("");
            setEditForm(null);
            const nextFilters = clearFilter(appliedFilters);
            setDraftFilters(nextFilters);
            setAppliedFilters(nextFilters);
          }}
        />
      </div>

      {canManageProducts ? (
        <div className="split-grid user-management-grid">
          <section className="panel" aria-labelledby="create-product-heading">
            <div className="section-heading">
              <h2 id="create-product-heading">{PRODUCT_CREATE_SECTION_HEADING}</h2>
              <span>{PRODUCT_CREATE_SECTION_HINT}</span>
            </div>
            <ProductForm
              formId="create-product-form"
              formAriaLabel={PRODUCT_CREATE_FORM_ARIA_LABEL}
              submitLabel={PRODUCT_CREATE_SUBMIT_LABEL}
              values={createForm}
              disabled={isBusy}
              onChange={setCreateForm}
              onSubmit={() => {
                setNotice("");
                createMutation.mutate(createForm);
              }}
            />
          </section>

          <section className="panel" aria-labelledby="edit-product-heading">
            <div className="section-heading">
              <h2 id="edit-product-heading">Edit product</h2>
              <span>Update pricing, duration, expiration rules, and status</span>
            </div>
            {selectedProduct == null || selectedProductForm == null ? (
              <p>No products match the selected filter.</p>
            ) : (
              <div className="form-grid">
                <label>
                  Product
                  <select
                    aria-label="Selected product"
                    value={selectedProduct.id}
                    onChange={(event) => {
                      const nextProduct = products.find(
                        (candidate) => candidate.id === event.target.value,
                      );
                      setSelectedProductId(event.target.value);
                      setEditForm(nextProduct == null ? null : productViewToForm(nextProduct));
                    }}
                  >
                    {products.map((candidate) => (
                      <option key={candidate.id} value={candidate.id}>
                        {candidate.name}
                      </option>
                    ))}
                  </select>
                </label>
                <ProductForm
                  formId="edit-product-form"
                  submitLabel="Save changes"
                  values={selectedProductForm}
                  disabled={isBusy}
                  includeActive
                  onChange={(nextValues) =>
                    setEditForm((current) => ({
                      ...(current ?? selectedProductForm),
                      ...nextValues,
                    }))
                  }
                  onSubmit={() => {
                    setNotice("");
                    updateMutation.mutate();
                  }}
                />
                <div className="button-row">
                  <button
                    type="button"
                    disabled={isBusy || !selectedProduct.active || selectedProduct.deleted}
                    onClick={() => {
                      setNotice("");
                      disableMutation.mutate();
                    }}
                  >
                    Disable product
                  </button>
                  <button
                    type="button"
                    disabled={isBusy || selectedProduct.deleted}
                    onClick={() => {
                      setNotice("");
                      deleteMutation.mutate();
                    }}
                  >
                    Delete product
                  </button>
                </div>
              </div>
            )}
          </section>
        </div>
      ) : null}

      <div className="panel">
        <div className="section-heading">
          <h2>Product catalog</h2>
          <span>
            {productsQuery.isLoading
              ? "Loading products"
              : formatProductCatalogSummary(products.length, filtersAreActive)}
          </span>
        </div>
        {productsQuery.isLoading ? <p className="table-state">Loading product records.</p> : null}
        {productsQuery.isError ? (
          <p className="form-error" role="alert">
            Product records could not be loaded.
          </p>
        ) : null}
        {!productsQuery.isLoading && !productsQuery.isError && products.length === 0 ? (
          <>
            <p className="table-state">No products match the current filters.</p>
            {filtersAreActive ? (
              <p className="table-state">
                Adjust the search fields or reset filters to broaden the product list.
              </p>
            ) : null}
          </>
        ) : null}
        {!productsQuery.isLoading && !productsQuery.isError && products.length > 0 ? (
          <table aria-label={PRODUCT_LIST_TABLE_ARIA_LABEL}>
            <thead>
              <tr>
                <th>Product</th>
                <th>Type</th>
                <th>Price</th>
                <th>Duration</th>
                <th>Expiration policy</th>
                <th>Status</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr
                  key={product.id}
                  className={selectedProduct?.id === product.id ? "selected-row" : undefined}
                  onClick={() => {
                    setSelectedProductId(product.id);
                    setEditForm(productViewToForm(product));
                  }}
                >
                  <td>
                    <Link className="table-primary-text" to={`/products/${product.id}`}>
                      {product.name}
                    </Link>
                    {product.description ? (
                      <span className="table-secondary-text">{product.description}</span>
                    ) : null}
                  </td>
                  <td>{formatProductEnum(product.productType)}</td>
                  <td>{formatPrice(product.price)}</td>
                  <td>{formatDuration(product.durationMonths)}</td>
                  <td>{product.expirationPolicy ?? "Not provided"}</td>
                  <td>
                    <StatusBadge value={productStatusLabel(product)} />
                  </td>
                  <td>{formatDateTime(product.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}

function ProductForm({
  formId,
  formAriaLabel,
  submitLabel,
  values,
  disabled,
  includeActive = false,
  onChange,
  onSubmit,
}: {
  formId: string;
  formAriaLabel?: string;
  submitLabel: string;
  values: ProductFormPayload;
  disabled: boolean;
  includeActive?: boolean;
  onChange: (values: ProductFormPayload) => void;
  onSubmit: () => void;
}) {
  const [errors, setErrors] = useState<ProductFormErrors>({});

  function updateField<TKey extends keyof ProductFormPayload>(
    field: TKey,
    fieldValue: ProductFormPayload[TKey],
  ) {
    setErrors((current) => ({ ...current, [field]: undefined }));
    onChange({ ...values, [field]: fieldValue });
  }

  return (
    <form
      id={formId}
      className="form-grid"
      noValidate
      aria-label={formAriaLabel}
      onSubmit={(event) => {
        event.preventDefault();
        const nextErrors = validateProductForm(values);
        setErrors(nextErrors);
        if (hasProductFormErrors(nextErrors)) {
          return;
        }
        onSubmit();
      }}
    >
      <label>
        Name
        <input
          required
          maxLength={255}
          aria-label="Product name"
          value={values.name}
          aria-invalid={Boolean(errors.name)}
          onChange={(event) => updateField("name", event.target.value)}
        />
        <ProductFieldError message={errors.name} />
      </label>
      <label>
        Type
        <select
          aria-label="Product type"
          value={values.productType}
          aria-invalid={Boolean(errors.productType)}
          onChange={(event) =>
            updateField("productType", event.target.value as ProductType)
          }
        >
          {PRODUCT_CREATE_TYPES.map((type) => (
            <option key={type} value={type}>
              {formatProductEnum(type)}
            </option>
          ))}
        </select>
        <ProductFieldError message={errors.productType} />
      </label>
      <label>
        Description
        <textarea
          aria-label="Product description"
          value={values.description}
          onChange={(event) => updateField("description", event.target.value)}
        />
      </label>
      <label>
        Price
        <input
          aria-label="Product price"
          inputMode="decimal"
          placeholder="0.00"
          value={values.price}
          aria-invalid={Boolean(errors.price)}
          onChange={(event) => updateField("price", event.target.value)}
        />
        <ProductFieldError message={errors.price} />
      </label>
      <label>
        Duration (months)
        <input
          aria-label="Product duration in months"
          inputMode="numeric"
          value={values.durationMonths}
          aria-invalid={Boolean(errors.durationMonths)}
          onChange={(event) => updateField("durationMonths", event.target.value)}
        />
        <ProductFieldError message={errors.durationMonths} />
      </label>
      <label>
        Expiration policy
        <input
          maxLength={100}
          aria-label="Product expiration policy"
          value={values.expirationPolicy}
          aria-invalid={Boolean(errors.expirationPolicy)}
          onChange={(event) => updateField("expirationPolicy", event.target.value)}
        />
        <ProductFieldError message={errors.expirationPolicy} />
      </label>
      {includeActive ? (
        <label>
          Active
          <select
            aria-label="Product active status"
            value={values.active ? "true" : "false"}
            onChange={(event) => updateField("active", event.target.value === "true")}
          >
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </label>
      ) : null}
      <button type="submit" disabled={disabled}>
        {submitLabel}
      </button>
    </form>
  );
}

function ProductFieldError({ message }: { message?: string }) {
  return message == null ? null : <span className="field-error">{message}</span>;
}

function productStatusLabel(product: ProductView) {
  if (product.deleted) {
    return "Deleted";
  }
  return product.active ? "Active" : "Inactive";
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

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError) ? "You are not authorized to manage products." : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "Product action failed." : "";
}
