/**
 * Login flow UI integration (KB item 598 / FR-001 / NFR-001).
 *
 * Exercises the real route tree: unauthenticated redirect, form submit, session
 * persistence, dashboard landing, and return-to deep link after login.
 */
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import {
  LOGIN_AUTH_REQUIRED_NOTICE,
  LOGIN_INVALID_CREDENTIALS_MESSAGE,
  LOGIN_PAGE_TITLE,
  LOGIN_PANEL_HEADING,
  LOGIN_RATE_LIMITED_MESSAGE,
  loginFormValidationMessages,
} from "@/features/auth/loginFlow";
import {
  createAccessToken,
  createFetchRouter,
  emptyDashboardPayload,
  jsonError,
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

function successfulLoginHandlers() {
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
      response: () => jsonOk(emptyDashboardPayload, "Analytics dashboard loaded"),
    },
    {
      match: (url) => url.includes("/campaigns") && !url.includes("/recipients"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/products"),
      response: () => jsonOk([]),
    },
  ]);
}

describe("login flow UI integration (item 598)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("presents the employee sign-in form on /login", () => {
    renderApp({ path: "/login", authenticated: false });

    expect(screen.getByRole("heading", { name: LOGIN_PAGE_TITLE })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: LOGIN_PANEL_HEADING })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Employee sign-in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });

  it("redirects unauthenticated access to login with an auth-required notice", async () => {
    renderApp({ path: "/users", authenticated: false });

    expect(await screen.findByRole("heading", { name: LOGIN_PAGE_TITLE })).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(LOGIN_AUTH_REQUIRED_NOTICE);
    expect(screen.queryByLabelText("Main navigation")).not.toBeInTheDocument();
  });

  it("logs in through the UI, stores the session, and lands on the dashboard", async () => {
    const user = userEvent.setup();
    const fetchMock = successfulLoginHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/login", authenticated: false });

    await user.type(screen.getByLabelText("Email"), loginUser.email);
    await user.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Dashboard", level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toContain(".");
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBe("refresh-token");
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toContain(loginUser.email);

    const loginCall = fetchMock.mock.calls.find(([url, init]) => {
      return String(url).endsWith("/auth/login") && (init as RequestInit | undefined)?.method === "POST";
    });
    expect(loginCall).toBeDefined();
    expect(JSON.parse(String((loginCall?.[1] as RequestInit).body))).toEqual({
      email: loginUser.email,
      password: "StrongPassword!2026",
    });
  });

  it("returns to the originally requested protected route after login", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", successfulLoginHandlers());

    // First hit a protected route so ProtectedRoute stores state.from + reason.
    renderApp({ path: "/campaigns", authenticated: false });
    expect(await screen.findByRole("alert")).toHaveTextContent(LOGIN_AUTH_REQUIRED_NOTICE);

    await user.type(screen.getByLabelText("Email"), loginUser.email);
    await user.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Campaigns", level: 1 })).toBeInTheDocument();
    expect(screen.getByLabelText("Main navigation")).toBeInTheDocument();
  });

  it("blocks invalid credentials in the form before calling the API", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/login", authenticated: false });
    await user.type(screen.getByLabelText("Email"), "not-an-email");
    await user.type(screen.getByLabelText("Password"), "short");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText(loginFormValidationMessages.emailInvalid),
    ).toBeInTheDocument();
    expect(screen.getByText(loginFormValidationMessages.passwordMinLength)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows a safe failure message for invalid credentials", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url, method) => url.endsWith("/auth/login") && method === "POST",
          response: () => jsonError(401, "Invalid email or password", "UNAUTHORIZED"),
        },
      ]),
    );

    renderApp({ path: "/login", authenticated: false });
    await user.type(screen.getByLabelText("Email"), loginUser.email);
    await user.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByTestId("login-error")).toHaveTextContent(
        LOGIN_INVALID_CREDENTIALS_MESSAGE,
      );
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(screen.queryByLabelText("Main navigation")).not.toBeInTheDocument();
  });

  it("shows a rate-limit message when login is temporarily locked (item 544)", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      createFetchRouter([
        {
          match: (url, method) => url.endsWith("/auth/login") && method === "POST",
          response: () => jsonError(429, "LOGIN_RATE_LIMITED", "LOGIN_RATE_LIMITED"),
        },
      ]),
    );

    renderApp({ path: "/login", authenticated: false });
    await user.type(screen.getByLabelText("Email"), loginUser.email);
    await user.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByTestId("login-error")).toHaveTextContent(LOGIN_RATE_LIMITED_MESSAGE);
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
  });
});
