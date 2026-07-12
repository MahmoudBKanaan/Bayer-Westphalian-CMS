import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SegmentPreviewResults } from "@/components/SegmentPreviewResults";
import type { SegmentPreviewView } from "@/api/segments";

const preview: SegmentPreviewView = {
  totalAudienceCount: 5,
  eligibleCount: 3,
  excludedCount: 2,
  matchingCustomers: [
    {
      id: "20000000-0000-0000-0000-000000000201",
      customerType: "PROSPECT",
      firstName: "Lena",
      lastName: "Mueller",
      fullName: "Lena Mueller",
      email: "lena.mueller@bayer-westphalian.test",
      city: "Munich",
      country: "Germany",
      status: "ACTIVE",
      doNotContact: false,
    },
    {
      id: "20000000-0000-0000-0000-000000000202",
      customerType: "CUSTOMER",
      firstName: "Tom",
      lastName: "Schmidt",
      fullName: "Tom Schmidt",
      email: null,
      city: "Berlin",
      country: "Germany",
      status: "INTERESTED",
      doNotContact: false,
    },
  ],
  exclusionReasonSummary: [
    {
      code: "DO_NOT_CONTACT",
      message: "Customer has do-not-contact enabled",
      count: 2,
    },
  ],
};

describe("SegmentPreviewResults", () => {
  it("shows empty state when no preview is available", () => {
    render(<SegmentPreviewResults preview={null} />);

    expect(screen.getByRole("heading", { name: "Audience preview" })).toBeInTheDocument();
    expect(screen.getByText("No preview yet")).toBeInTheDocument();
    expect(
      screen.getByText(/Run preview on draft criteria or a saved segment/i),
    ).toBeInTheDocument();
  });

  it("shows loading state while preview runs", () => {
    render(<SegmentPreviewResults preview={null} isLoading />);

    expect(screen.getByText("Loading audience preview…")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Audience preview" })).toBeInTheDocument();
  });

  it("shows an error message when preview fails", () => {
    render(
      <SegmentPreviewResults
        preview={null}
        errorMessage="You are not authorized to preview segments."
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "You are not authorized to preview segments.",
    );
  });

  it("labels total as pre-eligibility and eligible as contactable (item 208)", () => {
    render(<SegmentPreviewResults preview={preview} />);

    expect(screen.getByText("Criteria matches before eligibility")).toBeInTheDocument();
    expect(screen.getByText("Contactable after eligibility checks")).toBeInTheDocument();
    expect(screen.getByText("Criteria matches filtered by EligibilityService")).toBeInTheDocument();
  });

  it("renders audience metrics, eligible customers, and exclusion reasons", () => {
    render(<SegmentPreviewResults preview={preview} sourceLabel="Draft criteria" />);

    expect(screen.getByText("Source: Draft criteria")).toBeInTheDocument();
    const metrics = screen.getByLabelText("Audience size metrics");
    expect(within(metrics).getByText("Total audience")).toBeInTheDocument();
    expect(within(metrics).getByText("5")).toBeInTheDocument();
    expect(within(metrics).getByText("Eligible")).toBeInTheDocument();
    expect(within(metrics).getByText("3")).toBeInTheDocument();
    expect(within(metrics).getByText("Excluded")).toBeInTheDocument();
    expect(within(metrics).getByText("2")).toBeInTheDocument();
    expect(within(metrics).getByText("60%")).toBeInTheDocument();

    const customersTable = screen.getByRole("table", {
      name: "Eligible customers preview table",
    });
    expect(within(customersTable).getByText("Lena Mueller")).toBeInTheDocument();
    expect(within(customersTable).getByText("Tom Schmidt")).toBeInTheDocument();
    expect(within(customersTable).getByText("Munich, Germany")).toBeInTheDocument();
    expect(within(customersTable).getByText("No email on file")).toBeInTheDocument();

    expect(screen.getByRole("heading", { name: "Exclusion reason summary" })).toBeInTheDocument();
    expect(screen.getByLabelText("Exclusion reason cards")).toBeInTheDocument();
    // Title appears on the reason card and in the summary table row.
    expect(screen.getAllByText("Do not contact").length).toBeGreaterThan(0);
    const exclusionTable = screen.getByRole("table", {
      name: "Exclusion reason summary table",
    });
    expect(within(exclusionTable).getByText("DO_NOT_CONTACT")).toBeInTheDocument();
    expect(
      within(exclusionTable).getByText("Customer has do-not-contact enabled"),
    ).toBeInTheDocument();
  });

  it("renders eligible and excluded counts that sum to total audience (KB item 199)", () => {
    render(<SegmentPreviewResults preview={preview} sourceLabel="Count metrics" />);

    const metrics = screen.getByLabelText("Audience size metrics");
    expect(within(metrics).getByText("Total audience")).toBeInTheDocument();
    expect(within(metrics).getByText("5")).toBeInTheDocument();
    expect(within(metrics).getByText("Eligible")).toBeInTheDocument();
    expect(within(metrics).getByText("3")).toBeInTheDocument();
    expect(within(metrics).getByText("Excluded")).toBeInTheDocument();
    expect(within(metrics).getByText("2")).toBeInTheDocument();
    expect(preview.eligibleCount + preview.excludedCount).toBe(preview.totalAudienceCount);
    expect(screen.getByText("Source: Count metrics")).toBeInTheDocument();
  });

  it("shows eligibility-gated metrics where total can exceed eligible (KB item 198)", () => {
    render(
      <SegmentPreviewResults
        preview={{
          totalAudienceCount: 10,
          eligibleCount: 4,
          excludedCount: 6,
          matchingCustomers: preview.matchingCustomers.slice(0, 1),
          exclusionReasonSummary: [
            {
              code: "DO_NOT_CONTACT",
              message: "Customer has do-not-contact enabled",
              count: 3,
            },
            {
              code: "MARKETING_OPT_OUT",
              message: "Customer has withdrawn or rejected marketing consent",
              count: 3,
            },
          ],
        }}
        sourceLabel="Eligibility-aware preview"
      />,
    );

    const metrics = screen.getByLabelText("Audience size metrics");
    expect(within(metrics).getByText("Total audience")).toBeInTheDocument();
    expect(within(metrics).getByText("10")).toBeInTheDocument();
    expect(within(metrics).getByText("Eligible")).toBeInTheDocument();
    expect(within(metrics).getByText("4")).toBeInTheDocument();
    expect(within(metrics).getByText("Excluded")).toBeInTheDocument();
    expect(within(metrics).getByText("6")).toBeInTheDocument();
    expect(screen.getByText("Source: Eligibility-aware preview")).toBeInTheDocument();
    expect(screen.getAllByText("Do not contact").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Marketing opt-out").length).toBeGreaterThan(0);
  });

  it("explains when criteria match but all customers are excluded", () => {
    render(
      <SegmentPreviewResults
        preview={{
          totalAudienceCount: 4,
          eligibleCount: 0,
          excludedCount: 4,
          matchingCustomers: [],
          exclusionReasonSummary: [
            {
              code: "INVALID_CONSENT",
              message: "Customer does not have valid required consent",
              count: 4,
            },
          ],
        }}
      />,
    );

    expect(
      screen.getByText("All matched customers were excluded by eligibility rules."),
    ).toBeInTheDocument();
  });

  it("explains when no customers match criteria", () => {
    render(
      <SegmentPreviewResults
        preview={{
          totalAudienceCount: 0,
          eligibleCount: 0,
          excludedCount: 0,
          matchingCustomers: [],
          exclusionReasonSummary: [],
        }}
      />,
    );

    expect(screen.getByText("No customers matched the current criteria.")).toBeInTheDocument();
    expect(screen.getByText("No customers excluded")).toBeInTheDocument();
    expect(
      screen.getByText(/All matched customers remain eligible for contact/i),
    ).toBeInTheDocument();
  });
});
