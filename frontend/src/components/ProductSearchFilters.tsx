import type { ProductSearchFilters } from "@/api/products";
import {
  PRODUCT_ACTIVE_FILTERS,
  PRODUCT_TYPE_FILTERS,
  countActiveProductFilters,
  describeAppliedProductFilters,
  formatProductActiveFilter,
  formatProductEnum,
} from "@/features/products/productSearch";

type ProductSearchFiltersPanelProps = {
  draftFilters: ProductSearchFilters;
  appliedFilters: ProductSearchFilters;
  onDraftChange: (filters: ProductSearchFilters) => void;
  onApply: () => void;
  onReset: () => void;
  onRemoveAppliedFilter: (filterKey: string) => void;
  notice?: string;
  errorMessage?: string;
};

export function ProductSearchFiltersPanel({
  draftFilters,
  appliedFilters,
  onDraftChange,
  onApply,
  onReset,
  onRemoveAppliedFilter,
  notice,
  errorMessage,
}: ProductSearchFiltersPanelProps) {
  const activeFilterCount = countActiveProductFilters(appliedFilters);
  const appliedChips = describeAppliedProductFilters(appliedFilters);

  return (
    <>
      <form
        className="toolbar-row"
        aria-label="Product search filters"
        onSubmit={(event) => {
          event.preventDefault();
          onApply();
        }}
      >
        <label>
          Search
          <input
            aria-label="Search products"
            placeholder="Name or description"
            value={draftFilters.term}
            onChange={(event) => onDraftChange({ ...draftFilters, term: event.target.value })}
          />
        </label>
        <label>
          Type
          <select
            aria-label="Product type filter"
            value={draftFilters.productType}
            onChange={(event) =>
              onDraftChange({
                ...draftFilters,
                productType: event.target.value as ProductSearchFilters["productType"],
              })
            }
          >
            {PRODUCT_TYPE_FILTERS.map((type) => (
              <option key={type} value={type}>
                {formatProductEnum(type)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select
            aria-label="Product active filter"
            value={draftFilters.active}
            onChange={(event) =>
              onDraftChange({
                ...draftFilters,
                active: event.target.value as ProductSearchFilters["active"],
              })
            }
          >
            {PRODUCT_ACTIVE_FILTERS.map((active) => (
              <option key={active} value={active}>
                {formatProductActiveFilter(active)}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Apply filters</button>
        <button type="button" className="secondary-button" onClick={onReset}>
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
      {appliedChips.length > 0 ? (
        <div className="role-chip-list" aria-label="Applied product filters">
          {appliedChips.map((chip) => (
            <button
              key={chip.key}
              type="button"
              className="status-badge"
              aria-label={`Remove ${chip.label}`}
              onClick={() => onRemoveAppliedFilter(chip.key)}
            >
              {chip.label} ×
            </button>
          ))}
        </div>
      ) : null}
    </>
  );
}
