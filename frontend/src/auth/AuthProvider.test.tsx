import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import type { AuthenticatedUser } from "@/api/auth";
import { AuthProvider, useAuth } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const user: AuthenticatedUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "admin@bayer-westphalian.test",
  fullName: "Admin User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-04T12:00:00Z",
};

const accessToken = createAccessToken(["ADMIN", "CAMPAIGN_MANAGER"]);

const sessionResponse = {
  success: true,
  message: "Login successful",
  data: {
    user,
    tokens: {
      accessToken,
      accessTokenExpiresAt: "2026-07-04T12:15:00Z",
      refreshToken: "refresh-token",
      refreshTokenExpiresAt: "2026-07-11T12:00:00Z",
    },
  },
};

function AuthProbe() {
  const auth = useAuth();

  return (
    <section>
      <div>Authenticated: {auth.isAuthenticated ? "yes" : "no"}</div>
      <div>User: {auth.user?.fullName ?? "none"}</div>
      <div>Roles: {auth.roles.join(",") || "none"}</div>
      <div>Can manage users: {auth.hasAnyRole(["ADMIN"]) ? "yes" : "no"}</div>
      <button
        type="button"
        onClick={() => void auth.signIn("admin@bayer-westphalian.test", "StrongPassword!2026")}
      >
        Sign in probe
      </button>
      <button type="button" onClick={auth.signOut}>
        Sign out probe
      </button>
    </section>
  );
}

function renderProvider() {
  return render(
    <AuthProvider>
      <AuthProbe />
    </AuthProvider>,
  );
}

describe("AuthProvider", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("hydrates authenticated user state from session storage", () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, accessToken);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(user));

    renderProvider();

    expect(screen.getByText("Authenticated: yes")).toBeInTheDocument();
    expect(screen.getByText("User: Admin User")).toBeInTheDocument();
    expect(screen.getByText("Roles: ADMIN,CAMPAIGN_MANAGER")).toBeInTheDocument();
    expect(screen.getByText("Can manage users: yes")).toBeInTheDocument();
  });

  it("clears incomplete or invalid stored auth state", () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, "not-json");

    renderProvider();

    expect(screen.getByText("Authenticated: no")).toBeInTheDocument();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.refreshToken)).toBeNull();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.currentUser)).toBeNull();
  });

  it("signs in through the backend and stores auth state", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => sessionResponse,
    });
    vi.stubGlobal("fetch", fetchMock);

    renderProvider();
    await userEvent.click(screen.getByRole("button", { name: "Sign in probe" }));

    expect(await screen.findByText("Authenticated: yes")).toBeInTheDocument();
    expect(screen.getByText("User: Admin User")).toBeInTheDocument();
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
    expect(screen.getByText("Roles: ADMIN,CAMPAIGN_MANAGER")).toBeInTheDocument();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBe(accessToken);
  });

  it("signs out and removes stored auth state", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
    sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(user));

    renderProvider();
    await userEvent.click(screen.getByRole("button", { name: "Sign out probe" }));

    expect(screen.getByText("Authenticated: no")).toBeInTheDocument();
    expect(screen.getByText("User: none")).toBeInTheDocument();
    expect(sessionStorage.getItem(AUTH_STORAGE_KEYS.accessToken)).toBeNull();
  });
});

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
