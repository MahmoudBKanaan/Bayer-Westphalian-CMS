import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EmptyState } from "@/components/EmptyState";

describe("EmptyState", () => {
  it("renders accessible empty-state copy and optional action", () => {
    render(
      <EmptyState
        title="No records yet"
        description="Create the first record to start tracking this workflow."
        action={<button type="button">Create record</button>}
      />,
    );

    expect(screen.getByRole("status")).toHaveTextContent("No records yet");
    expect(
      screen.getByText("Create the first record to start tracking this workflow."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create record" })).toBeInTheDocument();
  });

  it("supports compact empty states for constrained surfaces", () => {
    render(
      <EmptyState
        compact
        title="No navigation available"
        description="Your account has no assigned application roles."
      />,
    );

    expect(screen.getByRole("status")).toHaveClass("empty-state--compact");
  });
});
