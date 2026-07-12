import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MetricCard } from "@/components/MetricCard";

describe("MetricCard (item 591)", () => {
  it("renders label, value, and detail with an accessible name", () => {
    render(
      <MetricCard label="Open rate" value="50.0%" detail="Opened ÷ sent" tone="engagement" />,
    );

    expect(screen.getByRole("heading", { name: "Open rate", level: 3 })).toBeInTheDocument();
    expect(screen.getByText("50.0%")).toBeInTheDocument();
    expect(screen.getByText("Opened ÷ sent")).toBeInTheDocument();
    expect(screen.getByLabelText("Open rate: 50.0%")).toBeInTheDocument();
  });

  it("applies tone class for visual grouping", () => {
    const { container } = render(
      <MetricCard label="Estimated ROI" value="10.0%" detail="Cost $1" tone="financial" />,
    );

    expect(container.querySelector(".metric-card--financial")).not.toBeNull();
  });
});
