import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import {
  LOGIN_GENERIC_FAILURE_MESSAGE,
  LOGIN_INVALID_CREDENTIALS_MESSAGE,
  LOGIN_PAGE_TITLE,
  LOGIN_PANEL_HEADING,
  LOGIN_RATE_LIMITED_MESSAGE,
  loginFormValidationMessages,
} from "@/features/auth/loginFlow";
import { LoginPage } from "@/pages/LoginPage";

type LoginInitialEntry =
  | string
  | {
      pathname: string;
      state?: unknown;
    };

function renderLoginPage(initialEntries: LoginInitialEntry[] = ["/login"]) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/dashboard" element={<h1>Campaign Performance</h1>} />
          <Route path="/campaigns" element={<h1>Campaigns</h1>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

const sessionResponse = {
  success: true,
  message: "Login successful",
  data: {
    user: {
      id: "10000000-0000-0000-0000-000000009901",
      email: "admin@bayer-westphalian.test",
      fullName: "Admin User",
      status: "ACTIVE",
      lastLoginAt: "2026-07-04T12:00:00Z",
    },
    tokens: {
      accessToken: "access-token",
      accessTokenExpiresAt: "2026-07-04T12:15:00Z",
      refreshToken: "refresh-token",
      refreshTokenExpiresAt: "2026-07-11T12:00:00Z",
    },
  },
};

describe("LoginPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("renders the internal employee login form", () => {
    renderLoginPage();

    expect(screen.getByRole("heading", { name: LOGIN_PAGE_TITLE })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: LOGIN_PANEL_HEADING })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Employee sign-in" })).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });

  it("logs in with valid credentials, stores the session, and opens the dashboard", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => sessionResponse,
    });
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email"), "admin@bayer-westphalian.test");
    await userEvent.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByRole("heading", { name: "Campaign Performance" }),
    ).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/auth/login`, {
      body: JSON.stringify({
        email: "admin@bayer-westphalian.test",
        password: "StrongPassword!2026",
      }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBe("access-token");
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBe("refresh-token");
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBe(
      JSON.stringify(sessionResponse.data.user),
    );
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("returns employees to the protected route they requested after login", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => sessionResponse,
    });
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage([
      {
        pathname: "/login",
        state: {
          from: {
            pathname: "/campaigns",
          },
        },
      },
    ]);
    await userEvent.type(screen.getByLabelText("Email"), "admin@bayer-westphalian.test");
    await userEvent.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("heading", { name: "Campaigns" })).toBeInTheDocument();
  });

  it("explains when a protected route requires login", () => {
    renderLoginPage([
      {
        pathname: "/login",
        state: {
          reason: "auth-required",
          from: {
            pathname: "/users",
          },
        },
      },
    ]);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Sign in with an authorized employee account to continue.",
    );
  });

  it("shows validation feedback before calling the backend", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email"), "not-an-email");
    await userEvent.type(screen.getByLabelText("Password"), "short");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText(loginFormValidationMessages.emailInvalid),
    ).toBeInTheDocument();
    expect(screen.getByText(loginFormValidationMessages.passwordMinLength)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("fails login with invalid credentials without storing a session", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({
        code: "UNAUTHORIZED",
        message: "Invalid email or password",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email"), "admin@bayer-westphalian.test");
    await userEvent.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByTestId("login-error")).toHaveTextContent(
        LOGIN_INVALID_CREDENTIALS_MESSAGE,
      );
    });
    expect(screen.queryByRole("heading", { name: "Campaign Performance" })).not.toBeInTheDocument();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("does not log in a disabled user account", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({
        code: "UNAUTHORIZED",
        message: "User account is not active",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email"), "disabled@bayer-westphalian.test");
    await userEvent.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByTestId("login-error")).toHaveTextContent(
        LOGIN_INVALID_CREDENTIALS_MESSAGE,
      );
    });
    expect(screen.queryByRole("heading", { name: "Campaign Performance" })).not.toBeInTheDocument();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("shows a rate-limit message when login is temporarily locked", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 429,
      json: async () => ({
        code: "LOGIN_RATE_LIMITED",
        message: "Too many failed attempts",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email"), "admin@bayer-westphalian.test");
    await userEvent.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByTestId("login-error")).toHaveTextContent(LOGIN_RATE_LIMITED_MESSAGE);
    });
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
  });

  it("shows a generic login error when the backend is unavailable", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({
        code: "SERVER_ERROR",
        message: "Unexpected error",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email"), "admin@bayer-westphalian.test");
    await userEvent.type(screen.getByLabelText("Password"), "StrongPassword!2026");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(screen.getByTestId("login-error")).toHaveTextContent(LOGIN_GENERIC_FAILURE_MESSAGE);
    });
  });
});
