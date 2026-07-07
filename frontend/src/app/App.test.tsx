import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App", () => {
  it("renders the login screen before an employee is authenticated", async () => {
    render(<App />);

    expect(
      await screen.findByRole("heading", {
        name: "Bayer-Westphalian Campaign Management",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });
});
