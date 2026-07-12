import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ExclusionReasonSummaryPanel } from "@/components/ExclusionReasonSummaryPanel";

describe("ExclusionReasonSummaryPanel", () => {
  it("shows an empty state when nothing was excluded", () => {
    render(<ExclusionReasonSummaryPanel reasons={[]} excludedCount={0} />);

    expect(screen.getByRole("heading", { name: "Exclusion reason summary" })).toBeInTheDocument();
    expect(screen.getByText("No exclusions")).toBeInTheDocument();
    expect(screen.getByText("No customers excluded")).toBeInTheDocument();
    expect(screen.getByText(/No customers were excluded for this preview/i)).toBeInTheDocument();
  });

  it("shows a compact empty state when requested", () => {
    render(<ExclusionReasonSummaryPanel reasons={[]} excludedCount={0} compactEmpty />);

    expect(screen.getByText(/All matched customers remain eligible/i)).toBeInTheDocument();
  });

  it("shows a fallback when exclusions exist without reason rows", () => {
    render(<ExclusionReasonSummaryPanel reasons={[]} excludedCount={4} />);

    expect(screen.getByText("Reasons unavailable")).toBeInTheDocument();
    expect(screen.getByText(/no detailed reason breakdown was returned/i)).toBeInTheDocument();
  });

  it("renders reason cards with counts, share meters, and a detail table", () => {
    render(
      <ExclusionReasonSummaryPanel
        excludedCount={5}
        reasons={[
          {
            code: "DO_NOT_CONTACT",
            message: "Customer has do-not-contact enabled",
            count: 3,
          },
          {
            code: "MARKETING_OPT_OUT",
            message: "Customer has withdrawn or rejected marketing consent",
            count: 2,
          },
        ]}
      />,
    );

    expect(screen.getByText(/5 excluded · 2 reason groups/i)).toBeInTheDocument();

    const cards = screen.getByLabelText("Exclusion reason cards");
    expect(within(cards).getByText("Do not contact")).toBeInTheDocument();
    expect(within(cards).getByText("Marketing opt-out")).toBeInTheDocument();
    expect(within(cards).getByText("Customer has do-not-contact enabled")).toBeInTheDocument();
    expect(within(cards).getByText(/BR-001/i)).toBeInTheDocument();
    expect(within(cards).getByText(/60.0% of excluded audience/i)).toBeInTheDocument();
    expect(within(cards).getByText(/40.0% of excluded audience/i)).toBeInTheDocument();

    const dncMeter = screen.getByRole("meter", {
      name: "Share of exclusions for Do not contact",
    });
    expect(dncMeter).toHaveAttribute("aria-valuenow", "60");

    const table = screen.getByRole("table", { name: "Exclusion reason summary table" });
    expect(within(table).getByText("DO_NOT_CONTACT")).toBeInTheDocument();
    expect(within(table).getByText("MARKETING_OPT_OUT")).toBeInTheDocument();
    expect(within(table).getByText("60.0%")).toBeInTheDocument();
    expect(within(table).getByText("40.0%")).toBeInTheDocument();
  });

  it("orders cards by descending exclusion count", () => {
    render(
      <ExclusionReasonSummaryPanel
        excludedCount={4}
        reasons={[
          {
            code: "INVALID_CONSENT",
            message: "Customer does not have valid required consent",
            count: 1,
          },
          {
            code: "MONTHLY_CONTACT_LIMIT",
            message: "Customer has reached the monthly marketing contact limit",
            count: 3,
          },
        ]}
      />,
    );

    const cards = screen.getByLabelText("Exclusion reason cards");
    const titles = within(cards)
      .getAllByRole("listitem")
      .map(
        (item) =>
          within(item).getByText(/Monthly contact limit|Invalid or missing consent/i).textContent,
      );

    expect(titles[0]).toMatch(/Monthly contact limit/i);
    expect(titles[1]).toMatch(/Invalid or missing consent/i);
  });
});
