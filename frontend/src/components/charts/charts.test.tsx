import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  ChartFrame,
  EngagementMixPieChart,
  FinancialLineChart,
  MultiSeriesBarChart,
  NamedCountBarChart,
  hasNonZeroValues,
} from "@/components/charts";

describe("ChartFrame and helpers (item 444)", () => {
  it("shows loading and empty states", () => {
    const { rerender } = render(
      <ChartFrame ariaLabel="Demo chart" isLoading loadingMessage="Loading demo chart…">
        <div>chart-body</div>
      </ChartFrame>,
    );
    expect(screen.getByText("Loading demo chart…")).toBeInTheDocument();
    expect(screen.queryByText("chart-body")).not.toBeInTheDocument();

    rerender(
      <ChartFrame ariaLabel="Demo chart" isEmpty emptyMessage="No demo data.">
        <div>chart-body</div>
      </ChartFrame>,
    );
    expect(screen.getByText("No demo data.")).toBeInTheDocument();

    rerender(
      <ChartFrame ariaLabel="Demo chart">
        <div>chart-body</div>
      </ChartFrame>,
    );
    expect(screen.getByLabelText("Demo chart")).toBeInTheDocument();
    expect(screen.getByText("chart-body")).toBeInTheDocument();
    expect(screen.getByTestId("chart-frame")).toBeInTheDocument();
  });

  it("detects non-zero chart values", () => {
    expect(hasNonZeroValues([0, 0, 0])).toBe(false);
    expect(hasNonZeroValues([0, 2, 0])).toBe(true);
    expect(hasNonZeroValues([Number.NaN])).toBe(false);
  });
});

describe("MultiSeriesBarChart (item 444 / FR-108)", () => {
  it("renders a labeled multi-series bar chart when data is present", () => {
    render(
      <MultiSeriesBarChart
        ariaLabel="Sent messages versus conversions chart"
        data={[
          { name: "Spring", sent: 80, conversions: 4 },
          { name: "Summer", sent: 40, conversions: 2 },
        ]}
        series={[
          { dataKey: "sent", name: "Sent" },
          { dataKey: "conversions", name: "Conversions" },
        ]}
      />,
    );

    expect(
      screen.getByLabelText("Sent messages versus conversions chart"),
    ).toBeInTheDocument();
  });

  it("shows empty messaging when series values are all zero", () => {
    render(
      <MultiSeriesBarChart
        ariaLabel="Empty bars"
        data={[{ name: "A", sent: 0, conversions: 0 }]}
        series={[
          { dataKey: "sent", name: "Sent" },
          { dataKey: "conversions", name: "Conversions" },
        ]}
        emptyMessage="No product performance data is available yet."
      />,
    );

    expect(
      screen.getByText("No product performance data is available yet."),
    ).toBeInTheDocument();
  });

  it("shows loading messaging", () => {
    render(
      <MultiSeriesBarChart
        ariaLabel="Loading bars"
        data={[]}
        series={[{ dataKey: "sent", name: "Sent" }]}
        isLoading
        loadingMessage="Loading performance chart…"
      />,
    );
    expect(screen.getByText("Loading performance chart…")).toBeInTheDocument();
  });
});

describe("NamedCountBarChart (item 444 / item 595)", () => {
  it("renders named inventory bars", () => {
    render(
      <NamedCountBarChart
        ariaLabel="Executive campaign inventory chart"
        data={[
          { name: "Total", value: 5 },
          { name: "Active", value: 2 },
        ]}
      />,
    );
    expect(screen.getByLabelText("Executive campaign inventory chart")).toBeInTheDocument();
  });

  it("shows empty messaging when counts are zero", () => {
    render(
      <NamedCountBarChart
        ariaLabel="Empty inventory"
        data={[
          { name: "Total", value: 0 },
          { name: "Active", value: 0 },
        ]}
        emptyMessage="No campaign inventory aggregates are available yet."
      />,
    );
    expect(
      screen.getByText("No campaign inventory aggregates are available yet."),
    ).toBeInTheDocument();
  });

  it("shows loading messaging", () => {
    render(
      <NamedCountBarChart
        ariaLabel="Loading inventory"
        data={[]}
        isLoading
        loadingMessage="Loading inventory chart…"
      />,
    );
    expect(screen.getByText("Loading inventory chart…")).toBeInTheDocument();
  });
});

describe("EngagementMixPieChart (item 444 / FR-108 / item 595)", () => {
  it("renders a pie chart for engagement mix slices", () => {
    render(
      <EngagementMixPieChart
        ariaLabel="Dashboard engagement mix pie chart"
        data={[
          { name: "Opened", value: 40 },
          { name: "Clicked", value: 16 },
          { name: "Replied", value: 8 },
          { name: "Converted", value: 4 },
        ]}
      />,
    );
    expect(screen.getByLabelText("Dashboard engagement mix pie chart")).toBeInTheDocument();
  });

  it("shows empty messaging when engagement counts are zero", () => {
    render(
      <EngagementMixPieChart
        ariaLabel="Empty mix"
        data={[
          { name: "Opened", value: 0 },
          { name: "Clicked", value: 0 },
        ]}
        emptyMessage="No engagement mix data is available yet."
      />,
    );
    expect(screen.getByText("No engagement mix data is available yet.")).toBeInTheDocument();
  });

  it("shows loading messaging", () => {
    render(
      <EngagementMixPieChart
        ariaLabel="Loading mix"
        data={[]}
        isLoading
        loadingMessage="Loading engagement mix chart…"
      />,
    );
    expect(screen.getByText("Loading engagement mix chart…")).toBeInTheDocument();
  });
});

describe("FinancialLineChart (item 444 / FR-107 / item 595)", () => {
  it("renders financial cost revenue and ROI lines", () => {
    render(
      <FinancialLineChart
        ariaLabel="Product cost revenue and ROI line chart"
        data={[
          { name: "Life", cost: 200, revenue: 300, roiPercent: 50 },
          { name: "Auto", cost: 100, revenue: 180, roiPercent: 80 },
        ]}
      />,
    );
    expect(
      screen.getByLabelText("Product cost revenue and ROI line chart"),
    ).toBeInTheDocument();
  });

  it("shows empty messaging when financial values are missing", () => {
    render(
      <FinancialLineChart
        ariaLabel="Empty financials"
        data={[{ name: "Life", cost: 0, revenue: 0, roiPercent: 0 }]}
        emptyMessage="No product financial chart data is available yet."
      />,
    );
    expect(
      screen.getByText("No product financial chart data is available yet."),
    ).toBeInTheDocument();
  });

  it("shows loading messaging", () => {
    render(
      <FinancialLineChart
        ariaLabel="Loading financials"
        data={[]}
        isLoading
        loadingMessage="Loading financial chart…"
      />,
    );
    expect(screen.getByText("Loading financial chart…")).toBeInTheDocument();
  });
});
