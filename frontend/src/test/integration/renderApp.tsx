/**
 * Shared harness for frontend integration tests (KB item 596 / NFR-010).
 *
 * Mounts the real app route tree with AuthProvider + React Query and stubs fetch.
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderResult } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { vi } from "vitest";
import { routes } from "@/app/router";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";

export type IntegrationUser = {
  id: string;
  email: string;
  fullName: string;
  status: string;
  lastLoginAt: string;
};

export const defaultIntegrationUser: IntegrationUser = {
  id: "10000000-0000-0000-0000-000000009901",
  email: "integration@bayer-westphalian.test",
  fullName: "Integration User",
  status: "ACTIVE",
  lastLoginAt: "2026-07-12T12:00:00Z",
};

export function createAccessToken(roles: SystemRoleName[]): string {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
  return `header.${payload}.signature`;
}

export function seedAuthenticatedSession(
  roles: SystemRoleName[] = ["CAMPAIGN_MANAGER"],
  user: IntegrationUser = defaultIntegrationUser,
): void {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify({ ...user, roles }));
}

export function jsonOk<T>(data: T, message = "OK") {
  return {
    ok: true,
    status: 200,
    json: async () => ({
      success: true,
      message,
      data,
    }),
  };
}

export function jsonError(status: number, message: string, code = "ERROR") {
  return {
    ok: false,
    status,
    json: async () => ({
      success: false,
      message,
      code,
      data: null,
    }),
  };
}

export type RenderAppOptions = {
  path: string;
  roles?: SystemRoleName[];
  user?: IntegrationUser;
  authenticated?: boolean;
};

/**
 * Renders the full protected route tree at {@code path}.
 */
export function renderApp(options: RenderAppOptions): RenderResult {
  const {
    path,
    roles = ["CAMPAIGN_MANAGER"],
    user = defaultIntegrationUser,
    authenticated = true,
  } = options;

  if (authenticated) {
    seedAuthenticatedSession(roles, user);
  } else {
    sessionStorage.clear();
  }

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const router = createMemoryRouter(routes, {
    initialEntries: [path],
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

/** Minimal dashboard payload so DashboardPage can render without unrelated network noise. */
export const emptyDashboardPayload = {
  campaignTotal: 0,
  activeCampaigns: 0,
  audienceSize: 0,
  messagesSent: 0,
  eligibleCount: 0,
  excludedCount: 0,
  openedCount: 0,
  clickedCount: 0,
  repliedCount: 0,
  convertedCount: 0,
  openRate: 0,
  clickRate: 0,
  conversionRate: 0,
  estimatedCost: 0,
  estimatedRevenue: 0,
  estimatedRoi: 0,
  recentCampaignMetrics: [],
};

export const defaultSystemSettings = {
  id: "00000000-0000-0000-0000-000000000001",
  monthlyContactLimit: 3,
  sendRetryLimit: 3,
  uninterestedExclusionDays: 90,
  updatedByUserId: null,
  updatedAt: null,
};

export function createFetchRouter(
  handlers: Array<{
    match: (url: string, method: string) => boolean;
    response: () => ReturnType<typeof jsonOk> | ReturnType<typeof jsonError> | Promise<unknown>;
  }>,
) {
  const withHealth: Array<{
    match: (url: string, method: string) => boolean;
    response: () => ReturnType<typeof jsonOk> | ReturnType<typeof jsonError> | Promise<unknown>;
  }> = [
    {
      match: (url: string) => url.includes("/health"),
      response: () =>
        // Health client reads the full JSON body (not ApiResponse.data).
        Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ status: "UP", service: "campaign-platform" }),
        }),
    },
    ...handlers,
  ];

  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    for (const handler of withHealth) {
      if (handler.match(url, method)) {
        return handler.response();
      }
    }
    return jsonError(404, `Unexpected URL ${url}`, "NOT_FOUND");
  });
}
