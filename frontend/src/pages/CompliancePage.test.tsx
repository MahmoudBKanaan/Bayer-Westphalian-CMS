import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CompliancePage } from "@/pages/CompliancePage";

describe("CompliancePage", () => {
  it("renders the KB compliance dashboard alert placeholder", () => {
    render(<CompliancePage />);

    expect(screen.getByRole("heading", { name: "Compliance alerts" })).toBeInTheDocument();
    expect(
      screen.getByRole("status", { name: "Compliance dashboard alert placeholder" }),
    ).toHaveTextContent("pending approvals");
    expect(screen.getByText(/consent exceptions/i)).toBeInTheDocument();
    expect(screen.getByText(/opt-outs/i)).toBeInTheDocument();
    expect(screen.getByText(/guardian-consent requirements/i)).toBeInTheDocument();
    expect(screen.getByText(/do-not-contact overrides/i)).toBeInTheDocument();
  });

  it("keeps the compliance review work list visible", () => {
    render(<CompliancePage />);

    expect(screen.getByRole("heading", { name: "Compliance review" })).toBeInTheDocument();
    expect(screen.getByText("Grandchild Education Plan")).toBeInTheDocument();
    expect(screen.getByText("Consent eligibility check")).toBeInTheDocument();
    expect(screen.getByText("Guardian consent")).toBeInTheDocument();
  });
});
