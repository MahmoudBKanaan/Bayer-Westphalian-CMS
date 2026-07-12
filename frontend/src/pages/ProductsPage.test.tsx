import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  PRODUCT_CREATE_FORM_ARIA_LABEL,
  PRODUCT_CREATE_SECTION_HEADING,
  PRODUCT_CREATE_SUBMIT_LABEL,
  PRODUCT_CREATED_NOTICE,
  productFormValidationMessages,
} from "@/features/products/productCreationFlow";
import { ProductsPage } from "@/pages/ProductsPage";

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

const investmentProduct = {
  id: "41000000-0000-0000-0000-000000000202",
  name: "Growth Fund",
  productType: "INVESTMENT_FUND",
  description: "Balanced investment portfolio",
  price: 500,
  durationMonths: 24,
  expirationPolicy: "Biennial review",
  active: false,
  deleted: false,
  createdAt: "2026-07-04T12:00:00Z",
  updatedAt: "2026-07-04T12:00:00Z",
  deletedAt: null,
};

function renderProductsPage(roles: SystemRoleName[] = ["ADMIN"]) {
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
      <MemoryRouter initialEntries={["/products"]}>
        <AuthProvider>
          <ProductsPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("ProductsPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads products and renders create and edit forms for admins", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderProductsPage(["ADMIN"]);

    expect(await screen.findByRole("heading", { name: "Products" })).toBeInTheDocument();
    expect(
      screen.getByText("Insurance and investment products used by campaigns and reminders"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).toBeInTheDocument();
    expect(screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Edit product" })).toBeInTheDocument();
    expect(await screen.findAllByText("Life Protection")).not.toHaveLength(0);
    expect(screen.getAllByText("Growth Fund")).not.toHaveLength(0);
    expect(screen.getByRole("table", { name: "Products table" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Life Protection" })).toHaveAttribute(
      "href",
      `/products/${product.id}`,
    );
  });

  it("renders the KB product search and filter UI on the catalog page", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderProductsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Product catalog" })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Product search filters" })).toBeInTheDocument();
    expect(screen.getByLabelText("Search products")).toBeInTheDocument();
    expect(screen.getByLabelText("Product type filter")).toBeInTheDocument();
    expect(screen.getByLabelText("Product active filter")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Apply filters" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reset filters" })).toBeInTheDocument();
    expect(screen.getByText("No active filters")).toBeInTheDocument();
    expect(await screen.findByText("2 products")).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
  });

  it("applies product search and filter query parameters", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    await userEvent.type(screen.getByLabelText("Search products"), "life");
    await userEvent.selectOptions(screen.getByLabelText("Product type filter"), "LIFE_INSURANCE");
    await userEvent.selectOptions(screen.getByLabelText("Product active filter"), "true");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/products?term=life&productType=LIFE_INSURANCE&active=true`,
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
          }),
        }),
      );
    });
    expect(screen.getByText("3 active filters")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Search: life" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Type: Life Insurance" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Status: Active" })).toBeInTheDocument();
    expect(screen.getByText("2 matching products")).toBeInTheDocument();
  });

  it("resets product search and filter controls", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    await userEvent.type(screen.getByLabelText("Search products"), "life");
    await userEvent.selectOptions(screen.getByLabelText("Product type filter"), "LIFE_INSURANCE");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));
    await screen.findByText("2 active filters");

    await userEvent.click(screen.getByRole("button", { name: "Reset filters" }));

    expect(screen.getByLabelText("Search products")).toHaveValue("");
    expect(screen.getByLabelText("Product type filter")).toHaveValue("ALL");
    expect(screen.getByLabelText("Product active filter")).toHaveValue("ALL");
    expect(screen.getByText("No active filters")).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([url]) => url === `${API_BASE_URL}/products`)).toBe(true);
    });
  });

  it("shows an empty catalog state when filters return no products", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");
    fetchMock.mockImplementation(createFetchMock([]));

    await userEvent.type(screen.getByLabelText("Search products"), "No Match");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    expect(await screen.findByText("No products match the current filters.")).toBeInTheDocument();
    expect(
      screen.getByText("Adjust the search fields or reset filters to broaden the product list."),
    ).toBeInTheDocument();
    expect(screen.getByText("0 matching products")).toBeInTheDocument();
  });

  it("allows a product manager to filter products by type only", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["PRODUCT_MANAGER"]);
    await screen.findAllByText("Life Protection");

    await userEvent.selectOptions(screen.getByLabelText("Product type filter"), "INVESTMENT_FUND");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/products?productType=INVESTMENT_FUND`,
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: `Bearer ${PRODUCT_MANAGER_ACCESS_TOKEN}`,
          }),
        }),
      );
    });
    expect(screen.getByText("1 active filter")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Remove Type: Investment Fund" }),
    ).toBeInTheDocument();
  });

  it("allows an admin to create a product", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const form = screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });

    await userEvent.type(within(form).getByLabelText("Product name"), "New Home Cover");
    await userEvent.selectOptions(within(form).getByLabelText("Product type"), "HOMEOWNER_INSURANCE");
    await userEvent.type(within(form).getByLabelText("Product description"), "Homeowner protection plan");
    await userEvent.type(within(form).getByLabelText("Product price"), "89.50");
    await userEvent.type(within(form).getByLabelText("Product duration in months"), "12");
    await userEvent.type(within(form).getByLabelText("Product expiration policy"), "Annual renewal");
    await userEvent.click(within(form).getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/products` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        name: "New Home Cover",
        productType: "HOMEOWNER_INSURANCE",
        description: "Homeowner protection plan",
        price: 89.5,
        durationMonths: 12,
        expirationPolicy: "Annual renewal",
      });
    });
    expect(screen.getByText(PRODUCT_CREATED_NOTICE)).toBeInTheDocument();
  }, 15000);

  it("validates the create product form before posting", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const form = screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
    await userEvent.type(within(form).getByLabelText("Product price"), "-5");
    await userEvent.type(within(form).getByLabelText("Product duration in months"), "0");
    await userEvent.click(within(form).getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }));

    expect(screen.getByText(productFormValidationMessages.nameRequired)).toBeInTheDocument();
    expect(screen.getByText(productFormValidationMessages.priceInvalid)).toBeInTheDocument();
    expect(screen.getByText(productFormValidationMessages.durationInvalid)).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) => url === `${API_BASE_URL}/products` && init?.method === "POST",
      ),
    ).toBe(false);
  });

  it("allows a product manager to edit a product", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["PRODUCT_MANAGER"]);
    await screen.findAllByText("Life Protection");

    const editPanel = screen.getByRole("heading", { name: "Edit product" }).closest("section");
    expect(editPanel).not.toBeNull();

    const nameInput = within(editPanel as HTMLElement).getByLabelText("Product name");
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "Life Protection Plus");
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
          name: "Life Protection Plus",
          productType: "LIFE_INSURANCE",
          description: "Comprehensive life insurance coverage",
          price: 129.99,
          durationMonths: 12,
          expirationPolicy: "Annual renewal",
          active: true,
        }),
      });
    });
    expect(screen.getByText("Product updated.")).toBeInTheDocument();
  });

  it("allows an admin to update the selected product", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const editPanel = screen.getByRole("heading", { name: "Edit product" }).closest("section");
    expect(editPanel).not.toBeNull();

    const nameInput = within(editPanel as HTMLElement).getByLabelText("Product name");
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, "Life Protection Plus");
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
          name: "Life Protection Plus",
          productType: "LIFE_INSURANCE",
          description: "Comprehensive life insurance coverage",
          price: 129.99,
          durationMonths: 12,
          expirationPolicy: "Annual renewal",
          active: true,
        }),
      });
    });
    expect(screen.getByText("Product updated.")).toBeInTheDocument();
  });

  it("allows a product manager to disable a product", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["PRODUCT_MANAGER"]);
    await screen.findAllByText("Life Protection");

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

  it("allows an admin to disable and delete a product", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    const editPanel = screen.getByRole("heading", { name: "Edit product" }).closest("section");
    expect(editPanel).not.toBeNull();

    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Disable product" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}/disable`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
        method: "PATCH",
      });
    });
    expect(screen.getByText("Product disabled.")).toBeInTheDocument();

    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Delete product" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
        method: "DELETE",
      });
    });
    expect(screen.getByText("Product deleted.")).toBeInTheDocument();
  });

  it("allows a product manager to create a product", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["PRODUCT_MANAGER"]);
    await screen.findAllByText("Life Protection");

    const form = screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });

    await userEvent.type(within(form).getByLabelText("Product name"), "Investment Growth Fund");
    await userEvent.selectOptions(within(form).getByLabelText("Product type"), "INVESTMENT_FUND");
    await userEvent.type(
      within(form).getByLabelText("Product description"),
      "Balanced investment portfolio for beneficiaries",
    );
    await userEvent.type(within(form).getByLabelText("Product price"), "500");
    await userEvent.type(within(form).getByLabelText("Product duration in months"), "24");
    await userEvent.type(within(form).getByLabelText("Product expiration policy"), "Biennial review");
    await userEvent.click(within(form).getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/products` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(createCall?.[1]?.headers).toMatchObject({
        Authorization: `Bearer ${PRODUCT_MANAGER_ACCESS_TOKEN}`,
      });
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        name: "Investment Growth Fund",
        productType: "INVESTMENT_FUND",
        description: "Balanced investment portfolio for beneficiaries",
        price: 500,
        durationMonths: 24,
        expirationPolicy: "Biennial review",
      });
    });
    expect(screen.getByText(PRODUCT_CREATED_NOTICE)).toBeInTheDocument();
  });

  it("does not allow unauthorized roles to create products", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderProductsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Product catalog" })).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Edit product" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }),
    ).not.toBeInTheDocument();
  });

  it("renders a read-only catalog for BI analysts", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderProductsPage(["BI_ANALYST"]);

    expect(await screen.findByRole("heading", { name: "Product catalog" })).toBeInTheDocument();
    expect(await screen.findAllByText("Life Protection")).not.toHaveLength(0);
    expect(
      screen.queryByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Edit product" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }),
    ).not.toBeInTheDocument();
  });

  it("allows a BI analyst to search and filter the product catalog", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["BI_ANALYST"]);
    await screen.findAllByText("Life Protection");

    await userEvent.type(screen.getByLabelText("Search products"), "growth");
    await userEvent.selectOptions(screen.getByLabelText("Product active filter"), "false");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/products?term=growth&active=false`,
        expect.any(Object),
      );
    });
    expect(screen.getByText("2 active filters")).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
  });

  it("removes an applied filter chip and reloads the catalog", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    await userEvent.type(screen.getByLabelText("Search products"), "life");
    await userEvent.selectOptions(screen.getByLabelText("Product active filter"), "true");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));
    await screen.findByText("2 active filters");

    await userEvent.click(screen.getByRole("button", { name: "Remove Status: Active" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/products?term=life`,
        expect.any(Object),
      );
    });
    expect(screen.getByText("1 active filter")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Remove Status: Active" })).not.toBeInTheDocument();
  });

  it("removes a search term chip and reloads the catalog", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderProductsPage(["ADMIN"]);
    await screen.findAllByText("Life Protection");

    await userEvent.type(screen.getByLabelText("Search products"), "life");
    await userEvent.selectOptions(screen.getByLabelText("Product type filter"), "LIFE_INSURANCE");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));
    await screen.findByText("2 active filters");

    await userEvent.click(screen.getByRole("button", { name: "Remove Search: life" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/products?productType=LIFE_INSURANCE`,
        expect.any(Object),
      );
    });
    expect(screen.getByText("1 active filter")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Remove Search: life" })).not.toBeInTheDocument();
  });
});

function createFetchMock(products = [product, investmentProduct]) {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url.endsWith("/products") && init?.method === "POST") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        ...product,
        ...body,
        id: "41000000-0000-0000-0000-000000000203",
        active: true,
        deleted: false,
        createdAt: "2026-07-05T12:00:00Z",
        updatedAt: "2026-07-05T12:00:00Z",
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

    if (url.includes(`/products/${product.id}`) && init?.method === "DELETE") {
      return jsonResponse({
        ...product,
        deleted: true,
        deletedAt: "2026-07-05T12:00:00Z",
      });
    }

    return Promise.resolve({
      ok: true,
      json: async () => ({
        success: true,
        message: "Products loaded",
        data: products,
      }),
    });
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
