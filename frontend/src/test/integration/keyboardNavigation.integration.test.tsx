/**
 * Keyboard navigation for core forms (KB item 608 / NFR-011).
 *
 * Exercises Tab / Shift+Tab / Enter on login and authenticated create forms
 * through the real route tree without a pointer device.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  LOGIN_EMAIL_LABEL,
  LOGIN_FORM_ARIA_LABEL,
  LOGIN_PASSWORD_LABEL,
  LOGIN_SUBMIT_LABEL,
  MAIN_CONTENT_ID,
  SKIP_TO_CONTENT_LABEL,
  expectedPrimaryTabNames,
} from "@/features/a11y/keyboardNavigationFlow";
import {
  CUSTOMER_CREATE_FORM_ARIA_LABEL,
  CUSTOMER_CREATE_SUBMIT_LABEL,
} from "@/features/customers/customerCreationFlow";
import {
  PRODUCT_CREATE_FORM_ARIA_LABEL,
  PRODUCT_CREATE_SUBMIT_LABEL,
} from "@/features/products/productCreationFlow";
import {
  createAccessToken,
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const loginUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE" as const,
  lastLoginAt: "2026-07-12T12:00:00Z",
};

function shellHandlers() {
  return createFetchRouter([
    {
      match: (url, method) => url.endsWith("/auth/login") && method === "POST",
      response: () =>
        jsonOk(
          {
            user: loginUser,
            tokens: {
              accessToken: createAccessToken(["ADMIN"]),
              accessTokenExpiresAt: "2026-07-12T12:15:00Z",
              refreshToken: "refresh-token",
              refreshTokenExpiresAt: "2026-07-19T12:00:00Z",
            },
          },
          "Login successful",
        ),
    },
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/products") && !url.includes("change"),
      response: () => jsonOk([], "Products loaded"),
    },
    {
      match: (url) => url.includes("/customers"),
      response: () =>
        jsonOk({
          content: [],
          page: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
          empty: true,
        }),
    },
    {
      match: (url) => url.includes("/actuator/health") || url.endsWith("/health"),
      response: () =>
        Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ status: "UP" }),
        }),
    },
  ]);
}

describe("keyboard navigation core forms UI integration (item 608)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("moves focus Email → Password → Sign in with Tab on the login form", async () => {
    vi.stubGlobal("fetch", shellHandlers());
    const user = userEvent.setup();
    renderApp({ path: "/login", authenticated: false });

    const form = screen.getByRole("form", { name: LOGIN_FORM_ARIA_LABEL });
    const email = within(form).getByLabelText(LOGIN_EMAIL_LABEL);
    const password = within(form).getByLabelText(LOGIN_PASSWORD_LABEL);
    const submit = within(form).getByRole("button", { name: LOGIN_SUBMIT_LABEL });

    email.focus();
    expect(email).toHaveFocus();

    await user.tab();
    expect(password).toHaveFocus();

    await user.tab();
    expect(submit).toHaveFocus();

    await user.tab({ shift: true });
    expect(password).toHaveFocus();

    expect(expectedPrimaryTabNames("login")).toEqual([
      LOGIN_EMAIL_LABEL,
      LOGIN_PASSWORD_LABEL,
      LOGIN_SUBMIT_LABEL,
    ]);
  });

  it("submits the login form with Enter from the password field", async () => {
    vi.stubGlobal("fetch", shellHandlers());
    const user = userEvent.setup();
    renderApp({ path: "/login", authenticated: false });

    const form = screen.getByRole("form", { name: LOGIN_FORM_ARIA_LABEL });
    await user.type(within(form).getByLabelText(LOGIN_EMAIL_LABEL), loginUser.email);
    await user.type(within(form).getByLabelText(LOGIN_PASSWORD_LABEL), "AdminPass!234");
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Dashboard", level: 1 })).toBeInTheDocument();
    });
  });

  it("exposes skip-to-content and main landmark for keyboard shell navigation", () => {
    vi.stubGlobal("fetch", shellHandlers());
    renderApp({ path: "/dashboard", roles: ["ADMIN"] });

    const skip = screen.getByRole("link", { name: SKIP_TO_CONTENT_LABEL });
    expect(skip).toHaveAttribute("href", `#${MAIN_CONTENT_ID}`);
    expect(screen.getByRole("main")).toHaveAttribute("id", MAIN_CONTENT_ID);
  });

  it("tabs through primary customer create fields without positive tabindex traps", async () => {
    vi.stubGlobal("fetch", shellHandlers());
    const user = userEvent.setup();
    renderApp({ path: "/customers", roles: ["ADMIN"] });

    await waitFor(() => {
      expect(screen.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL })).toBeInTheDocument();
    });

    const form = screen.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL });
    const customerType = within(form).getByLabelText("Customer type");
    const firstName = within(form).getByLabelText("First name");
    const lastName = within(form).getByLabelText("Last name");

    customerType.focus();
    expect(customerType).toHaveFocus();
    expect(customerType).not.toHaveAttribute("tabindex", "1");

    await user.tab();
    expect(firstName).toHaveFocus();

    await user.tab();
    expect(lastName).toHaveFocus();

    await user.tab({ shift: true });
    expect(firstName).toHaveFocus();

    expect(within(form).getByRole("button", { name: CUSTOMER_CREATE_SUBMIT_LABEL })).toBeInTheDocument();
  });

  it("tabs into the product create form and reaches the submit control", async () => {
    vi.stubGlobal("fetch", shellHandlers());
    const user = userEvent.setup();
    renderApp({ path: "/products", roles: ["ADMIN"] });

    await waitFor(() => {
      expect(screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL })).toBeInTheDocument();
    });

    const form = screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
    const name = within(form).getByLabelText("Product name");
    name.focus();
    expect(name).toHaveFocus();

    await user.tab();
    expect(within(form).getByLabelText("Product type")).toHaveFocus();

    // Remaining fields: description, price, duration, expiration policy, then submit.
    await user.tab();
    await user.tab();
    await user.tab();
    await user.tab();
    await user.tab();
    expect(within(form).getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL })).toHaveFocus();
  });
});
