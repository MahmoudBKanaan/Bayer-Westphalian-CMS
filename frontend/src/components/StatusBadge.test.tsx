import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusBadge } from "@/components/StatusBadge";

describe("StatusBadge (item 595)", () => {
  it("renders the provided status value", () => {
    render(<StatusBadge value="Eligible" />);

    expect(screen.getByText("Eligible")).toBeInTheDocument();
    expect(screen.getByText("Eligible")).toHaveClass("status-badge");
  });

  it("maps multi-word values to lowercase hyphenated tone classes", () => {
    render(<StatusBadge value="Do Not Contact" />);

    expect(screen.getByText("Do Not Contact")).toHaveClass("status-badge", "do-not-contact");
  });

  it("supports single-token lifecycle-style values used in recipient tables", () => {
    const { rerender } = render(<StatusBadge value="Sent" />);
    expect(screen.getByText("Sent")).toHaveClass("sent");

    rerender(<StatusBadge value="EXCLUDED" />);
    expect(screen.getByText("EXCLUDED")).toHaveClass("excluded");
  });
});
