import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  assignProductOwnership,
  listCustomerProductOwnerships,
  updateProductOwnership,
} from "@/api/productOwnerships";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";

const productOwnership = {
  id: "41000000-0000-0000-0000-000000000001",
  customerId: "20000000-0000-0000-0000-000000000001",
  customerFullName: "Ada Policyholder",
  productId: "41000000-0000-0000-0000-000000000201",
  productName: "Life Protection",
  productType: "LIFE_INSURANCE",
  policyNumber: "POL-1000",
  startDate: "2026-01-15",
  expirationDate: "2027-01-15",
  status: "ACTIVE",
  active: true,
  createdAt: "2026-07-03T12:00:00Z",
};

describe("productOwnerships api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("returns saved expiration dates when listing customer product ownerships", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product ownerships loaded",
        data: [
          productOwnership,
          {
            ...productOwnership,
            id: "41000000-0000-0000-0000-000000000002",
            productName: "Home Protection",
            expirationDate: "2027-02-01",
          },
        ],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const ownerships = await listCustomerProductOwnerships(productOwnership.customerId as string);

    expect(ownerships).toEqual([
      productOwnership,
      {
        ...productOwnership,
        id: "41000000-0000-0000-0000-000000000002",
        productName: "Home Protection",
        expirationDate: "2027-02-01",
      },
    ]);
    expect(ownerships[0]?.expirationDate).toBe("2027-01-15");
    expect(ownerships[1]?.expirationDate).toBe("2027-02-01");
  });

  it("loads product ownership records for a customer profile", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product ownerships loaded",
        data: [productOwnership],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listCustomerProductOwnerships(productOwnership.customerId as string),
    ).resolves.toEqual([productOwnership]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-ownerships?customerId=${productOwnership.customerId}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("allows a product manager to assign a product to a customer", async () => {
    const productManagerToken = createAccessToken(["PRODUCT_MANAGER"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, productManagerToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product ownership assigned",
        data: productOwnership,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const assignedOwnership = await assignProductOwnership({
      customerId: productOwnership.customerId as string,
      productId: productOwnership.productId as string,
      startDate: "2026-03-01",
      expirationDate: "2027-03-01",
      policyNumber: "POL-3000",
    });

    expect(assignedOwnership).toEqual(productOwnership);
    expect(assignedOwnership.expirationDate).toBe("2027-01-15");

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/product-ownerships`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${productManagerToken}`,
      },
      body: JSON.stringify({
        customerId: productOwnership.customerId,
        productId: productOwnership.productId,
        startDate: "2026-03-01",
        expirationDate: "2027-03-01",
        policyNumber: "POL-3000",
      }),
    });
  });

  it("assigns a product to a customer", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product ownership assigned",
        data: productOwnership,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      assignProductOwnership({
        customerId: productOwnership.customerId as string,
        productId: productOwnership.productId as string,
        startDate: "2026-01-15",
        expirationDate: "2027-01-15",
        policyNumber: "POL-1000",
      }),
    ).resolves.toEqual(productOwnership);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/product-ownerships`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        customerId: productOwnership.customerId,
        productId: productOwnership.productId,
        startDate: "2026-01-15",
        expirationDate: "2027-01-15",
        policyNumber: "POL-1000",
      }),
    });
  });

  it("updates a product ownership policy number and expiration date", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const updatedOwnership = {
      ...productOwnership,
      policyNumber: "POL-9000",
      expirationDate: "2028-01-15",
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product ownership updated",
        data: updatedOwnership,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateProductOwnership(productOwnership.id, {
        policyNumber: " POL-9000 ",
        expirationDate: "2028-01-15",
      }),
    ).resolves.toEqual(updatedOwnership);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-ownerships/${productOwnership.id}`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
        body: JSON.stringify({
          expirationDate: "2028-01-15",
          policyNumber: "POL-9000",
        }),
      },
    );
  });
});

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
