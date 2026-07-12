import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ErrorState } from "@/components/ErrorState";

describe("ErrorState", () => {
  it("renders accessible error copy and optional recovery action", () => {
    render(
      <ErrorState
        title="Records could not be loaded"
        description="Check the connection and try again."
        action={<button type="button">Retry</button>}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("Records could not be loaded");
    expect(screen.getByText("Check the connection and try again.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("supports compact error states for shell and inline surfaces", () => {
    render(
      <ErrorState
        compact
        title="Backend unavailable"
        description="Application data could not be refreshed."
      />,
    );

    expect(screen.getByRole("alert")).toHaveClass("error-state--compact");
  });
});
