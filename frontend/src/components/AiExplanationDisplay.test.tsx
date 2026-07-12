import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";

describe("AiExplanationDisplay (item 491)", () => {
  it("renders explanation, confidence, and stored recommendation id", () => {
    render(
      <AiExplanationDisplay
        explanation="Suggested from product ownership and payment history."
        confidenceScore={86}
        storedRecommendationId="70000000-0000-0000-0000-000000000491"
      />,
    );

    expect(screen.getByLabelText("AI explanation")).toBeInTheDocument();
    expect(
      screen.getByText("Suggested from product ownership and payment history."),
    ).toBeInTheDocument();
    expect(screen.getByText("Confidence")).toBeInTheDocument();
    expect(screen.getByText("86%")).toBeInTheDocument();
    expect(screen.getByText("Recommendation ID")).toBeInTheDocument();
    expect(screen.getByText("70000000-0000-0000-0000-000000000491")).toBeInTheDocument();
  });

  it("renders weighted score factors for AI search and risk explanations", () => {
    render(
      <AiExplanationDisplay
        explanation="Default-risk score uses payment history."
        factors={[
          {
            fieldName: "missed payments",
            weight: 25,
            contribution: 12,
            reason: "1 unpaid overdue payment",
          },
          {
            fieldName: "reminder escalation",
            weight: 20,
            contribution: 10,
            reason: "Red reminder level",
          },
        ]}
      />,
    );

    expect(screen.getByLabelText("AI score factors")).toBeInTheDocument();
    expect(screen.getByText("missed payments")).toBeInTheDocument();
    expect(screen.getByText("12 / 25")).toBeInTheDocument();
    expect(screen.getByText("Red reminder level")).toBeInTheDocument();
  });

  it("renders nothing for blank explanations", () => {
    const { container } = render(<AiExplanationDisplay explanation="   " />);

    expect(container).toBeEmptyDOMElement();
  });
});
