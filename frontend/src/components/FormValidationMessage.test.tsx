import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FormValidationMessage } from "@/components/FormValidationMessage";

describe("FormValidationMessage", () => {
  it("renders field-level validation copy with a stable id", () => {
    render(<FormValidationMessage id="email-error" message="Email is required." />);

    expect(screen.getByText("Email is required.")).toHaveAttribute("id", "email-error");
    expect(screen.getByText("Email is required.")).toHaveClass("field-error");
  });

  it("renders nothing when no validation message is present", () => {
    const { container } = render(<FormValidationMessage id="name-error" />);

    expect(container).toBeEmptyDOMElement();
  });
});
