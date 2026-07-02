import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App", () => {
  it("renders the project title and health placeholder", async () => {
    render(<App />);

    expect(
      await screen.findByText("Bayer-Westphalian Campaign Management Platform"),
    ).toBeInTheDocument();
    expect(screen.getByText("API health: backend not connected yet")).toBeInTheDocument();
  });
});
