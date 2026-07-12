import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ProductSearchFiltersPanel } from "@/components/ProductSearchFilters";
import { emptyProductSearchFilters } from "@/features/products/productSearch";

describe("ProductSearchFiltersPanel", () => {
  it("renders KB product search fields and reports active filter count", () => {
    render(
      <ProductSearchFiltersPanel
        draftFilters={emptyProductSearchFilters}
        appliedFilters={{
          term: "life",
          productType: "LIFE_INSURANCE",
          active: "ALL",
        }}
        onDraftChange={vi.fn()}
        onApply={vi.fn()}
        onReset={vi.fn()}
        onRemoveAppliedFilter={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("Search products")).toBeInTheDocument();
    expect(screen.getByLabelText("Product type filter")).toBeInTheDocument();
    expect(screen.getByLabelText("Product active filter")).toBeInTheDocument();
    expect(screen.getByText("2 active filters")).toBeInTheDocument();
    expect(screen.getByLabelText("Applied product filters")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Search: life" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Type: Life Insurance" })).toBeInTheDocument();
  });

  it("calls apply and reset handlers from the filter toolbar", async () => {
    const onApply = vi.fn();
    const onReset = vi.fn();

    render(
      <ProductSearchFiltersPanel
        draftFilters={{ term: "fund", productType: "INVESTMENT_FUND", active: "false" }}
        appliedFilters={emptyProductSearchFilters}
        onDraftChange={vi.fn()}
        onApply={onApply}
        onReset={onReset}
        onRemoveAppliedFilter={vi.fn()}
      />,
    );

    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));
    await userEvent.click(screen.getByRole("button", { name: "Reset filters" }));

    expect(onApply).toHaveBeenCalledTimes(1);
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it("updates draft filters when search fields change", async () => {
    const onDraftChange = vi.fn();

    render(
      <ProductSearchFiltersPanel
        draftFilters={emptyProductSearchFilters}
        appliedFilters={emptyProductSearchFilters}
        onDraftChange={onDraftChange}
        onApply={vi.fn()}
        onReset={vi.fn()}
        onRemoveAppliedFilter={vi.fn()}
      />,
    );

    await userEvent.type(screen.getByLabelText("Search products"), "p");
    expect(onDraftChange).toHaveBeenLastCalledWith({
      term: "p",
      productType: "ALL",
      active: "ALL",
    });

    await userEvent.selectOptions(screen.getByLabelText("Product type filter"), "AUTO_INSURANCE");
    expect(onDraftChange).toHaveBeenLastCalledWith({
      term: "",
      productType: "AUTO_INSURANCE",
      active: "ALL",
    });

    await userEvent.selectOptions(screen.getByLabelText("Product active filter"), "false");
    expect(onDraftChange).toHaveBeenLastCalledWith({
      term: "",
      productType: "ALL",
      active: "false",
    });
  });

  it("shows no active filters and surfaces notice or error messages", () => {
    render(
      <ProductSearchFiltersPanel
        draftFilters={emptyProductSearchFilters}
        appliedFilters={emptyProductSearchFilters}
        onDraftChange={vi.fn()}
        onApply={vi.fn()}
        onReset={vi.fn()}
        onRemoveAppliedFilter={vi.fn()}
        notice="Filters applied."
        errorMessage="Product records could not be loaded."
      />,
    );

    expect(screen.getByText("No active filters")).toBeInTheDocument();
    expect(screen.getByText("Filters applied.")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Product records could not be loaded.");
    expect(screen.queryByLabelText("Applied product filters")).not.toBeInTheDocument();
  });

  it("removes an applied filter chip when selected", async () => {
    const onRemoveAppliedFilter = vi.fn();

    render(
      <ProductSearchFiltersPanel
        draftFilters={emptyProductSearchFilters}
        appliedFilters={{
          term: "",
          productType: "AUTO_INSURANCE",
          active: "true",
        }}
        onDraftChange={vi.fn()}
        onApply={vi.fn()}
        onReset={vi.fn()}
        onRemoveAppliedFilter={onRemoveAppliedFilter}
      />,
    );

    await userEvent.click(screen.getByRole("button", { name: "Remove Status: Active" }));

    expect(onRemoveAppliedFilter).toHaveBeenCalledWith("active");
  });
});
