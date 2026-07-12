import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL, ApiError } from "@/api/client";
import {
  createProduct,
  deleteProduct,
  disableProduct,
  getProduct,
  listProducts,
  updateProduct,
} from "@/api/products";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";

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

const productForm = {
  name: "Life Protection",
  productType: "LIFE_INSURANCE" as const,
  description: "Comprehensive life insurance coverage",
  price: "129.99",
  durationMonths: "12",
  expirationPolicy: "Annual renewal",
  active: true,
};

describe("products api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads products from the search endpoint", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Products loaded",
        data: [product],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(listProducts()).resolves.toEqual([product]);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("loads products with KB search and filter query parameters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Products loaded",
        data: [product],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listProducts({
        term: "life",
        productType: "LIFE_INSURANCE",
        active: "true",
      }),
    ).resolves.toEqual([product]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/products?term=life&productType=LIFE_INSURANCE&active=true`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("loads products with a search term filter only", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Products loaded",
        data: [product],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await listProducts({ term: "protection", productType: "ALL", active: "ALL" });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products?term=protection`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("loads products with an inactive status filter only", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Products loaded",
        data: [product],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await listProducts({ term: "", productType: "ALL", active: "false" });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products?active=false`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("loads a product by id", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product loaded",
        data: product,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(getProduct(product.id)).resolves.toEqual(product);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("rejects unauthorized product creation without authentication", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({
        code: "UNAUTHORIZED",
        message: "Bearer access token is required",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createProduct(productForm)).rejects.toBeInstanceOf(ApiError);
    await expect(createProduct(productForm)).rejects.toMatchObject({ status: 401 });
  });

  it("rejects unauthorized product creation for read-only roles", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(["BI_ANALYST"]));
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({
        code: "FORBIDDEN",
        message: "Access is denied",
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createProduct(productForm)).rejects.toBeInstanceOf(ApiError);
    await expect(createProduct(productForm)).rejects.toMatchObject({ status: 403 });
  });

  it("allows a product manager to create a product", async () => {
    const productManagerToken = createAccessToken(["PRODUCT_MANAGER"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, productManagerToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product created",
        data: {
          ...product,
          name: "Investment Growth Fund",
          productType: "INVESTMENT_FUND",
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createProduct({
        name: "Investment Growth Fund",
        productType: "INVESTMENT_FUND",
        description: "Balanced investment portfolio for beneficiaries",
        price: "500",
        durationMonths: "24",
        expirationPolicy: "Biennial review",
        active: true,
      }),
    ).resolves.toEqual({
      ...product,
      name: "Investment Growth Fund",
      productType: "INVESTMENT_FUND",
    });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${productManagerToken}`,
      },
      body: JSON.stringify({
        name: "Investment Growth Fund",
        productType: "INVESTMENT_FUND",
        description: "Balanced investment portfolio for beneficiaries",
        price: 500,
        durationMonths: 24,
        expirationPolicy: "Biennial review",
      }),
    });
  });

  it("creates a product", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product created",
        data: product,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createProduct(productForm)).resolves.toEqual(product);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        name: "Life Protection",
        productType: "LIFE_INSURANCE",
        description: "Comprehensive life insurance coverage",
        price: 129.99,
        durationMonths: 12,
        expirationPolicy: "Annual renewal",
      }),
    });
  });

  it("allows a product manager to update a product", async () => {
    const productManagerToken = createAccessToken(["PRODUCT_MANAGER"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, productManagerToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product updated",
        data: { ...product, name: "Life Protection Plus", price: 149.5 },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateProduct(product.id, {
        ...productForm,
        name: "Life Protection Plus",
        price: "149.50",
        expirationPolicy: "AUTO_RENEW",
      }),
    ).resolves.toEqual({ ...product, name: "Life Protection Plus", price: 149.5 });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${productManagerToken}`,
      },
      body: JSON.stringify({
        name: "Life Protection Plus",
        productType: "LIFE_INSURANCE",
        description: "Comprehensive life insurance coverage",
        price: 149.5,
        durationMonths: 12,
        expirationPolicy: "AUTO_RENEW",
        active: true,
      }),
    });
  });

  it("updates a product", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product updated",
        data: { ...product, name: "Life Protection Plus" },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateProduct(product.id, {
        ...productForm,
        name: "Life Protection Plus",
        active: false,
      }),
    ).resolves.toEqual({ ...product, name: "Life Protection Plus" });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        name: "Life Protection Plus",
        productType: "LIFE_INSURANCE",
        description: "Comprehensive life insurance coverage",
        price: 129.99,
        durationMonths: 12,
        expirationPolicy: "Annual renewal",
        active: false,
      }),
    });
  });

  it("allows a product manager to disable a product", async () => {
    const productManagerToken = createAccessToken(["PRODUCT_MANAGER"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, productManagerToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product disabled",
        data: { ...product, active: false },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(disableProduct(product.id)).resolves.toEqual({ ...product, active: false });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}/disable`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${productManagerToken}`,
      },
    });
  });

  it("disables a product", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product disabled",
        data: { ...product, active: false },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(disableProduct(product.id)).resolves.toEqual({ ...product, active: false });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}/disable`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("soft-deletes a product", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product deleted",
        data: { ...product, deleted: true, deletedAt: "2026-07-05T12:00:00Z" },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(deleteProduct(product.id)).resolves.toEqual({
      ...product,
      deleted: true,
      deletedAt: "2026-07-05T12:00:00Z",
    });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/products/${product.id}`, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });
});

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
