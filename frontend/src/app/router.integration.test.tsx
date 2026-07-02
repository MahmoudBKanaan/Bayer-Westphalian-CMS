import { render, screen, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { routes } from "@/app/router";

function renderRoute(path: string) {
  const router = createMemoryRouter(routes, {
    initialEntries: [path],
  });

  return render(<RouterProvider router={router} />);
}

describe("application routing", () => {
  it("redirects the root route to the dashboard", async () => {
    renderRoute("/");

    expect(
      await screen.findByRole("heading", { name: /campaign performance/i }),
    ).toBeInTheDocument();
    expect(screen.getByText("2,310")).toBeInTheDocument();
    expect(screen.getByText("85.5%")).toBeInTheDocument();
  });

  it("renders campaign work from the dashboard data on the campaigns route", async () => {
    renderRoute("/campaigns");

    expect(await screen.findByRole("heading", { name: "Campaigns" })).toBeInTheDocument();

    const campaignRows = screen.getAllByRole("row");
    expect(campaignRows).toHaveLength(4);
    expect(within(campaignRows[1]).getByText("CMP-001")).toBeInTheDocument();
    expect(within(campaignRows[1]).getByText("Grandchild Education Plan")).toBeInTheDocument();
    expect(within(campaignRows[1]).getByText("982")).toBeInTheDocument();
  });

  it("renders the login route outside the main application shell", async () => {
    renderRoute("/login");

    expect(
      await screen.findByRole("heading", {
        name: "Bayer-Westphalian Campaign Management",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Main navigation")).not.toBeInTheDocument();
  });
});
