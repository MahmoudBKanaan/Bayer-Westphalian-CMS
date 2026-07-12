/**
 * Product creation UI integration (KB item 601 / product CRUD).
 *
 * Full route tree: authorized user opens Products, fills create form, posts,
 * sees success notice + catalog row; validation blocks bad input; read-only
 * roles do not see the create panel.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  PRODUCT_CREATE_FORM_ARIA_LABEL,
  PRODUCT_CREATE_SECTION_HEADING,
  PRODUCT_CREATE_SUBMIT_LABEL,
  PRODUCT_CREATED_NOTICE,
  PRODUCT_CREATION_FIXTURES,
  PRODUCT_LIST_TABLE_ARIA_LABEL,
  PRODUCT_PAGE_HEADING,
  productFormValidationMessages,
} from "@/features/products/productCreationFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const existingProduct = {
  id: "30000000-0000-0000-0000-000000000201",
  name: "Life Protection",
  productType: "LIFE_INSURANCE",
  description: "Existing catalog product",
  price: 129.99,
  durationMonths: 12,
  expirationPolicy: "Annual renewal",
  active: true,
  deleted: false,
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

function productCreationHandlers() {
  let products = [existingProduct];

  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url, method) => {
        if (method !== "POST") {
          return false;
        }
        try {
          const pathname = new URL(url).pathname.replace(/\/+$/, "");
          return pathname.endsWith("/products");
        } catch {
          return url.endsWith("/products");
        }
      },
      response: () => {
        const created = {
          ...existingProduct,
          id: PRODUCT_CREATION_FIXTURES.id,
          name: PRODUCT_CREATION_FIXTURES.name,
          productType: PRODUCT_CREATION_FIXTURES.productType,
          description: PRODUCT_CREATION_FIXTURES.description,
          price: Number(PRODUCT_CREATION_FIXTURES.price),
          durationMonths: Number(PRODUCT_CREATION_FIXTURES.durationMonths),
          expirationPolicy: PRODUCT_CREATION_FIXTURES.expirationPolicy,
        };
        products = [created, ...products];
        return jsonOk(created, "Product created");
      },
    },
    {
      match: (url, method) => url.includes("/products") && method === "GET",
      response: () => jsonOk(products, "Products loaded"),
    },
  ]);
}

describe("product creation UI integration (item 601)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows the create product panel for authorized roles", async () => {
    vi.stubGlobal("fetch", productCreationHandlers());
    renderApp({ path: "/products", roles: ["PRODUCT_MANAGER"] });

    expect(await screen.findByRole("heading", { name: "Products", level: 1 })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: PRODUCT_PAGE_HEADING, level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).toBeInTheDocument();
    expect(screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: PRODUCT_LIST_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
  });

  it("hides create controls for campaign managers", async () => {
    vi.stubGlobal("fetch", productCreationHandlers());
    renderApp({ path: "/products", roles: ["CAMPAIGN_MANAGER"] });

    expect(
      await screen.findByRole("table", { name: PRODUCT_LIST_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: PRODUCT_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL }),
    ).not.toBeInTheDocument();
  });

  it(
    "creates a product through the UI and shows success plus catalog row",
    async () => {
      const user = userEvent.setup();
      const fetchMock = productCreationHandlers();
      vi.stubGlobal("fetch", fetchMock);

      renderApp({ path: "/products", roles: ["ADMIN"] });
      await screen.findByRole("table", { name: PRODUCT_LIST_TABLE_ARIA_LABEL });

      const form = screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
      await user.type(within(form).getByLabelText("Product name"), PRODUCT_CREATION_FIXTURES.name);
      await user.selectOptions(
        within(form).getByLabelText("Product type"),
        PRODUCT_CREATION_FIXTURES.productType,
      );
      await user.type(
        within(form).getByLabelText("Product description"),
        PRODUCT_CREATION_FIXTURES.description,
      );
      await user.type(within(form).getByLabelText("Product price"), PRODUCT_CREATION_FIXTURES.price);
      await user.type(
        within(form).getByLabelText("Product duration in months"),
        PRODUCT_CREATION_FIXTURES.durationMonths,
      );
      await user.type(
        within(form).getByLabelText("Product expiration policy"),
        PRODUCT_CREATION_FIXTURES.expirationPolicy,
      );
      await user.click(within(form).getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }));

      expect(await screen.findByText(PRODUCT_CREATED_NOTICE)).toBeInTheDocument();
      const table = screen.getByRole("table", { name: PRODUCT_LIST_TABLE_ARIA_LABEL });
      expect(await within(table).findByText(PRODUCT_CREATION_FIXTURES.name)).toBeInTheDocument();

      await waitFor(() => {
        const createCall = fetchMock.mock.calls.find(([url, init]) => {
          return (
            (String(url).endsWith("/products") || String(url).includes("/products?")) &&
            (init as RequestInit | undefined)?.method === "POST"
          );
        });
        expect(createCall).toBeDefined();
        expect(JSON.parse(String((createCall?.[1] as RequestInit).body))).toMatchObject({
          name: PRODUCT_CREATION_FIXTURES.name,
          productType: PRODUCT_CREATION_FIXTURES.productType,
          description: PRODUCT_CREATION_FIXTURES.description,
          price: Number(PRODUCT_CREATION_FIXTURES.price),
          durationMonths: Number(PRODUCT_CREATION_FIXTURES.durationMonths),
          expirationPolicy: PRODUCT_CREATION_FIXTURES.expirationPolicy,
        });
      });
    },
    10_000,
  );

  it("validates the create form before calling the API", async () => {
    const user = userEvent.setup();
    const fetchMock = productCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/products", roles: ["PRODUCT_MANAGER"] });
    await screen.findByRole("table", { name: PRODUCT_LIST_TABLE_ARIA_LABEL });

    const form = screen.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
    await user.type(within(form).getByLabelText("Product price"), "-1");
    await user.type(within(form).getByLabelText("Product duration in months"), "abc");
    await user.click(within(form).getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL }));

    expect(screen.getByText(productFormValidationMessages.nameRequired)).toBeInTheDocument();
    expect(screen.getByText(productFormValidationMessages.priceInvalid)).toBeInTheDocument();
    expect(screen.getByText(productFormValidationMessages.durationInvalid)).toBeInTheDocument();

    const postCalls = fetchMock.mock.calls.filter(([url, init]) => {
      return (
        String(url).includes("/products") &&
        (init as RequestInit | undefined)?.method === "POST"
      );
    });
    expect(postCalls).toHaveLength(0);
  });
});
