import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { ProductChangeRequestsPage } from "@/pages/ProductChangeRequestsPage";

const ADMIN_ACCESS_TOKEN = createAccessToken(["ADMIN"]);

const product = {
  id: "41000000-0000-0000-0000-000000000201",
  name: "Life Protection",
  productType: "LIFE_INSURANCE",
  description: "Comprehensive life insurance coverage",
  price: 129.99,
  durationMonths: 12,
  expirationPolicy: "Annual renewal",
  active: true,
  deleted: false,
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

const openRequest = {
  id: "42000000-0000-0000-0000-000000000001",
  productId: product.id,
  productName: product.name,
  productType: "LIFE_INSURANCE",
  requestedByUserId: "10000000-0000-0000-0000-000000009901",
  requestedByFullName: "Product Manager",
  requestType: "PRICE_CHANGE",
  description: "Increase annual premium to reflect market rates.",
  status: "OPEN",
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
};

const approvedRequest = {
  ...openRequest,
  id: "42000000-0000-0000-0000-000000000002",
  requestType: "DURATION_CHANGE",
  description: "Extend standard contract duration to 24 months.",
  status: "APPROVED",
};

function renderProductChangeRequestsPage(roles: SystemRoleName[] = ["ADMIN"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000009901",
      email: "admin@bayer-westphalian.test",
      fullName: "Admin User",
      status: "ACTIVE",
      lastLoginAt: "2026-07-04T12:00:00Z",
    }),
  );

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/product-change-requests"]}>
        <AuthProvider>
          <ProductChangeRequestsPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ProductChangeRequestsPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads change requests and renders workflow panels for admins", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["ADMIN"]);

    expect(
      await screen.findByRole("heading", { name: "Product change requests" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Create request" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Selected request" })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: "Product change requests table" }),
    ).toBeInTheDocument();
    expect(screen.getAllByText("Life Protection")).not.toHaveLength(0);
    expect(
      screen.getAllByText("Increase annual premium to reflect market rates."),
    ).not.toHaveLength(0);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
      },
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/product-change-requests`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
      },
    });
  });

  it("applies product and status filters", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    await userEvent.selectOptions(screen.getByLabelText("Product filter"), product.id);
    await userEvent.selectOptions(screen.getByLabelText("Change request status filter"), "OPEN");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/product-change-requests?productId=${product.id}&status=OPEN`,
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
          }),
        }),
      );
    });
    expect(screen.getByText("2 active filters")).toBeInTheDocument();
  });

  it("creates a product change request and shows it in the tracker table", async () => {
    const savedRequests: (typeof openRequest)[] = [];
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.endsWith("/product-change-requests") && init?.method === "POST") {
        const payload = JSON.parse(init.body as string) as Record<string, unknown>;
        const createdRequest = {
          ...openRequest,
          id: "42000000-0000-0000-0000-000000000003",
          requestType: payload.requestType as string,
          description: payload.description as string,
        };
        savedRequests.push(createdRequest);

        return jsonResponse(createdRequest, "Product change request created");
      }
      if (url.endsWith("/product-change-requests")) {
        return jsonResponse(
          savedRequests.length > 0 ? savedRequests : [openRequest, approvedRequest],
        );
      }
      if (url.endsWith("/products")) {
        return jsonResponse([product]);
      }

      return Promise.reject(new Error(`Unexpected fetch call: ${url}`));
    });
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["PRODUCT_MANAGER"]);
    await screen.findAllByText("Life Protection");

    const createPanel = screen.getByRole("heading", { name: "Create request" }).closest("section");
    expect(createPanel).not.toBeNull();

    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Product for change request"),
      product.id,
    );
    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Product change request type"),
      "STATUS_CHANGE",
    );
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Product change request description"),
      "Deactivate legacy tariff for new customers.",
    );
    await userEvent.click(
      within(createPanel as HTMLElement).getByRole("button", { name: "Create request" }),
    );

    const table = await screen.findByRole("table", { name: "Product change requests table" });
    await waitFor(() => {
      expect(
        within(table).getByText("Deactivate legacy tariff for new customers."),
      ).toBeInTheDocument();
    });
    expect(within(table).getByText("Open", { selector: ".status-badge" })).toBeInTheDocument();
    expect(screen.getByText("Product change request created.")).toBeInTheDocument();
  });

  it("tracks approved status in the table after approving a request", async () => {
    const savedRequests = [{ ...openRequest }];
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (
        url.includes(`/product-change-requests/${openRequest.id}/approve`) &&
        init?.method === "PATCH"
      ) {
        savedRequests[0] = { ...openRequest, status: "APPROVED" };
        return jsonResponse(savedRequests[0], "Product change request approved");
      }
      if (url.endsWith("/product-change-requests")) {
        return jsonResponse(savedRequests);
      }
      if (url.endsWith("/products")) {
        return jsonResponse([product]);
      }

      return Promise.reject(new Error(`Unexpected fetch call: ${url}`));
    });
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const table = screen.getByRole("table", { name: "Product change requests table" });
    expect(within(table).getByText("Open", { selector: ".status-badge" })).toBeInTheDocument();

    const selectedPanel = screen
      .getByRole("heading", { name: "Selected request" })
      .closest("section");
    expect(selectedPanel).not.toBeNull();

    await userEvent.click(
      within(selectedPanel as HTMLElement).getByRole("button", { name: "Approve request" }),
    );

    await waitFor(() => {
      expect(
        within(table).getByText("Approved", { selector: ".status-badge" }),
      ).toBeInTheDocument();
    });
    expect(screen.getByText("Product change request approved.")).toBeInTheDocument();
  });

  it("allows an admin to create a product change request", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const createPanel = screen.getByRole("heading", { name: "Create request" }).closest("section");
    expect(createPanel).not.toBeNull();

    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Product for change request"),
      product.id,
    );
    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Product change request type"),
      "EXPIRATION_RULE_CHANGE",
    );
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Product change request description"),
      "Add grace period to annual renewal policy.",
    );
    await userEvent.click(
      within(createPanel as HTMLElement).getByRole("button", { name: "Create request" }),
    );

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/product-change-requests` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        productId: product.id,
        requestType: "EXPIRATION_RULE_CHANGE",
        description: "Add grace period to annual renewal policy.",
      });
    });
    expect(screen.getByText("Product change request created.")).toBeInTheDocument();
  });

  it("allows an admin to approve and mark a request implemented", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const selectedPanel = screen
      .getByRole("heading", { name: "Selected request" })
      .closest("section");
    expect(selectedPanel).not.toBeNull();

    await userEvent.click(
      within(selectedPanel as HTMLElement).getByRole("button", { name: "Approve request" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/product-change-requests/${openRequest.id}/approve`,
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
          },
          method: "PATCH",
        },
      );
    });
    expect(screen.getByText("Product change request approved.")).toBeInTheDocument();

    await userEvent.selectOptions(
      within(selectedPanel as HTMLElement).getByLabelText("Selected change request"),
      approvedRequest.id,
    );
    await userEvent.click(
      within(selectedPanel as HTMLElement).getByRole("button", { name: "Mark implemented" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/product-change-requests/${approvedRequest.id}/mark-implemented`,
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
          },
          method: "PATCH",
        },
      );
    });
    expect(screen.getByText("Product change request marked implemented.")).toBeInTheDocument();
  });

  it("allows a product manager to update an open request description", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductChangeRequestsPage(["PRODUCT_MANAGER"]);
    await screen.findAllByText("Life Protection");

    const selectedPanel = screen
      .getByRole("heading", { name: "Selected request" })
      .closest("section");
    expect(selectedPanel).not.toBeNull();

    const descriptionInput = within(selectedPanel as HTMLElement).getByLabelText(
      "Selected change request description",
    );
    await userEvent.clear(descriptionInput);
    await userEvent.type(descriptionInput, "Increase annual premium and update renewal terms.");
    await userEvent.click(
      within(selectedPanel as HTMLElement).getByRole("button", { name: "Save description" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/product-change-requests/${openRequest.id}`,
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${createAccessToken(["PRODUCT_MANAGER"])}`,
          },
          method: "PUT",
          body: JSON.stringify({
            description: "Increase annual premium and update renewal terms.",
          }),
        },
      );
    });
    expect(screen.getByText("Product change request updated.")).toBeInTheDocument();
  });

  it("renders a read-only tracker for BI analysts", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderProductChangeRequestsPage(["BI_ANALYST"]);

    expect(await screen.findByRole("heading", { name: "Request tracker" })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: "Product change requests table" }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Create request" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Selected request" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Approve request" })).not.toBeInTheDocument();
  });
});

function createFetchMock(requests = [openRequest, approvedRequest]) {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url.endsWith("/product-change-requests") && init?.method === "POST") {
      return jsonResponse({
        ...openRequest,
        id: "42000000-0000-0000-0000-000000000003",
        requestType: "EXPIRATION_RULE_CHANGE",
        description: "Add grace period to annual renewal policy.",
      });
    }

    if (
      url.includes(`/product-change-requests/${openRequest.id}/approve`) &&
      init?.method === "PATCH"
    ) {
      return jsonResponse({ ...openRequest, status: "APPROVED" });
    }

    if (
      url.includes(`/product-change-requests/${approvedRequest.id}/mark-implemented`) &&
      init?.method === "PATCH"
    ) {
      return jsonResponse({ ...approvedRequest, status: "IMPLEMENTED" });
    }

    if (url.includes(`/product-change-requests/${openRequest.id}`) && init?.method === "PUT") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        ...openRequest,
        ...body,
        updatedAt: "2026-07-05T12:00:00Z",
      });
    }

    if (url.endsWith("/product-change-requests")) {
      return jsonResponse(requests);
    }

    if (url.endsWith("/products")) {
      return jsonResponse([product]);
    }

    return Promise.reject(new Error(`Unexpected fetch call: ${url}`));
  });
}

function jsonResponse(data: unknown, message = "OK") {
  return Promise.resolve({
    ok: true,
    json: async () => ({
      success: true,
      message,
      data,
    }),
  });
}

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
