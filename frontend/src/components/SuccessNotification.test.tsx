import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SuccessNotification } from "@/components/SuccessNotification";

describe("SuccessNotification", () => {
  it("announces successful operations with optional follow-up actions", () => {
    render(
      <SuccessNotification
        message="Campaign updated."
        action={<button type="button">View campaign</button>}
      />,
    );

    expect(screen.getByRole("status")).toHaveTextContent("Campaign updated.");
    expect(screen.getByRole("status")).toHaveAttribute("aria-live", "polite");
    expect(screen.getByRole("button", { name: "View campaign" })).toBeInTheDocument();
  });

  it("supports compact success notifications for toolbar surfaces", () => {
    render(<SuccessNotification compact message="User created." />);

    expect(screen.getByRole("status")).toHaveClass("success-notification--compact");
  });
});
