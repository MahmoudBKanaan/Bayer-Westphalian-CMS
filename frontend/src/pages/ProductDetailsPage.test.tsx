import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { ProductDetailsPage } from "@/pages/ProductDetailsPage";

const ADMIN_ACCESS_TOKEN = createAccessToken(["ADMIN"]);
const PRODUCT_MANAGER_ACCESS_TOKEN = createAccessToken(["PRODUCT_MANAGER"]);

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

const changeRequest = {
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

function renderProductDetailsPage(roles: SystemRoleName[] = ["ADMIN"]) {
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
      <AuthProvider>
        <MemoryRouter initialEntries={[`/products/${product.id}`]}>
          <Routes>
            <Route path="/products/:productId" element={<ProductDetailsPage />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}

describe("ProductDetailsPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads and renders the product profile sections required by the KB", async () => {
    const fetchMock = createProfileFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductDetailsPage(["ADMIN"]);

    expect(await screen.findByRole("heading", { name: "Product details" })).toBeInTheDocument();
    expect(screen.getAllByText("Life Protection")).not.toHaveLength(0);
    expect(screen.getByRole("heading", { name: "Profile" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Activity" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Edit product" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Change requests" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Request product change" })).toBeInTheDocument();
    expect(screen.getAllByText("Comprehensive life insurance coverage")).not.toHaveLength(0);
    expect(screen.getAllByText("Annual renewal")).not.toHaveLength(0);
    expect(
      screen.getByText("Increase annual premium to reflect market rates."),
    ).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
      },
    });
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-change-requests?productId=${product.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
      },
    );
  });

  it("allows an admin to update product pricing and expiration rules", async () => {
    const fetchMock = createProfileFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductDetailsPage(["ADMIN"]);
    await screen.findByRole("heading", { name: "Edit product" });

    const editPanel = screen.getByRole("heading", { name: "Edit product" }).closest("section");
    expect(editPanel).not.toBeNull();

    const priceInput = within(editPanel as HTMLElement).getByLabelText("Product price");
    await userEvent.clear(priceInput);
    await userEvent.type(priceInput, "149.50");
    await userEvent.clear(
      within(editPanel as HTMLElement).getByLabelText("Product expiration policy"),
    );
    await userEvent.type(
      within(editPanel as HTMLElement).getByLabelText("Product expiration policy"),
      "Annual renewal with grace period",
    );
    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Save changes" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
        method: "PUT",
        body: JSON.stringify({
          name: "Life Protection",
          productType: "LIFE_INSURANCE",
          description: "Comprehensive life insurance coverage",
          price: 149.5,
          durationMonths: 12,
          expirationPolicy: "Annual renewal with grace period",
          active: true,
        }),
      });
    });
    expect(screen.getByText("Product updated.")).toBeInTheDocument();
  });

  it("allows a product manager to edit product pricing and expiration rules", async () => {
    const fetchMock = createProfileFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductDetailsPage(["PRODUCT_MANAGER"]);
    await screen.findByRole("heading", { name: "Edit product" });

    const editPanel = screen.getByRole("heading", { name: "Edit product" }).closest("section");
    expect(editPanel).not.toBeNull();

    const priceInput = within(editPanel as HTMLElement).getByLabelText("Product price");
    await userEvent.clear(priceInput);
    await userEvent.type(priceInput, "149.50");
    await userEvent.clear(
      within(editPanel as HTMLElement).getByLabelText("Product expiration policy"),
    );
    await userEvent.type(
      within(editPanel as HTMLElement).getByLabelText("Product expiration policy"),
      "Annual renewal with grace period",
    );
    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Save changes" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${PRODUCT_MANAGER_ACCESS_TOKEN}`,
        },
        method: "PUT",
        body: JSON.stringify({
          name: "Life Protection",
          productType: "LIFE_INSURANCE",
          description: "Comprehensive life insurance coverage",
          price: 149.5,
          durationMonths: 12,
          expirationPolicy: "Annual renewal with grace period",
          active: true,
        }),
      });
    });
    expect(screen.getByText("Product updated.")).toBeInTheDocument();
  });

  it("allows a product manager to disable a product", async () => {
    const fetchMock = createProfileFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductDetailsPage(["PRODUCT_MANAGER"]);
    await screen.findByRole("heading", { name: "Edit product" });

    const editPanel = screen.getByRole("heading", { name: "Edit product" }).closest("section");
    expect(editPanel).not.toBeNull();

    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Disable product" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}/disable`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${PRODUCT_MANAGER_ACCESS_TOKEN}`,
        },
        method: "PATCH",
      });
    });
    expect(screen.getByText("Product disabled.")).toBeInTheDocument();
  });

  it("allows a product manager to create a product change request", async () => {
    const fetchMock = createProfileFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductDetailsPage(["PRODUCT_MANAGER"]);
    await screen.findByRole("heading", { name: "Request product change" });

    const requestPanel = screen
      .getByRole("heading", { name: "Request product change" })
      .closest("section");
    expect(requestPanel).not.toBeNull();

    await userEvent.selectOptions(
      within(requestPanel as HTMLElement).getByLabelText("Product change request type"),
      "DURATION_CHANGE",
    );
    await userEvent.type(
      within(requestPanel as HTMLElement).getByLabelText("Product change request description"),
      "Extend standard contract duration to 24 months.",
    );
    await userEvent.click(
      within(requestPanel as HTMLElement).getByRole("button", { name: "Create change request" }),
    );

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/product-change-requests` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        productId: product.id,
        requestType: "DURATION_CHANGE",
        description: "Extend standard contract duration to 24 months.",
      });
    });
    expect(screen.getByText("Product change request created.")).toBeInTheDocument();
  });

  it("renders a read-only product profile for BI analysts", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock());

    renderProductDetailsPage(["BI_ANALYST"]);

    expect(await screen.findByRole("heading", { name: "Product details" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Profile" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Change requests" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Edit product" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Request product change" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Save changes" })).not.toBeInTheDocument();
  });
});

function createProfileFetchMock() {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url.endsWith("/product-change-requests") && init?.method === "POST") {
      return jsonResponse({
        ...changeRequest,
        requestType: "DURATION_CHANGE",
        description: "Extend standard contract duration to 24 months.",
      });
    }

    if (url.includes(`/products/${product.id}/disable`) && init?.method === "PATCH") {
      return jsonResponse({
        ...product,
        active: false,
      });
    }

    if (url.includes(`/products/${product.id}`) && init?.method === "PUT") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        ...product,
        ...body,
        updatedAt: "2026-07-05T12:00:00Z",
      });
    }

    if (url.endsWith(`/products/${product.id}`)) {
      return jsonResponse(product);
    }

    if (url.includes("/product-change-requests")) {
      return jsonResponse([changeRequest]);
    }

    return Promise.reject(new Error(`Unexpected fetch call: ${url}`));
  });
}

function jsonResponse(data: unknown) {
  return Promise.resolve({
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
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
