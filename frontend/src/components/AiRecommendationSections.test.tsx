import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  AiRecommendationSections,
  buildAiRecommendationSections,
} from "@/components/AiRecommendationSections";
import { createPermissionChecks } from "@/features/auth/permissions";

function permissionsFor(roles: SystemRoleName[]) {
  return createPermissionChecks((requiredRoles) =>
    requiredRoles.some((role) => roles.includes(role)),
  );
}

describe("AiRecommendationSections (item 490)", () => {
  it("builds all AI recommendation sections for campaign managers", () => {
    const sections = buildAiRecommendationSections(permissionsFor(["CAMPAIGN_MANAGER"]));

    expect(sections.map((section) => section.id)).toEqual([
      "customer-search",
      "duplicate-contact",
      "default-risk",
      "segment-suggestions",
      "product-recommendations",
      "campaign-copy",
    ]);
  });

  it("limits BI analysts to customer, duplicate, segment, and product sections", () => {
    const sections = buildAiRecommendationSections(permissionsFor(["BI_ANALYST"]));

    expect(sections.map((section) => section.id)).toEqual([
      "customer-search",
      "duplicate-contact",
      "default-risk",
      "segment-suggestions",
      "product-recommendations",
    ]);
  });

  it("renders links for available AI recommendation sections", () => {
    const sections = buildAiRecommendationSections(permissionsFor(["CAMPAIGN_MANAGER"]));

    render(
      <MemoryRouter>
        <AiRecommendationSections sections={sections} />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("heading", { name: "AI recommendations" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("list", { name: "AI recommendation sections" })).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Open customers" })[0]).toHaveAttribute(
      "href",
      "/customers",
    );
    expect(screen.getByRole("link", { name: "Open builder" })).toHaveAttribute(
      "href",
      "/campaign-builder",
    );
    expect(screen.getAllByLabelText("AI explanation")).toHaveLength(sections.length);
    expect(
      screen.getByText(/Drafts campaign copy from objective, product, audience, and channel/i),
    ).toBeInTheDocument();
  });

  it("renders nothing when no AI sections are available", () => {
    const { container } = render(
      <MemoryRouter>
        <AiRecommendationSections sections={[]} />
      </MemoryRouter>,
    );

    expect(container).toBeEmptyDOMElement();
  });
});
