import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  approveProductChangeRequest,
  createProductChangeRequest,
  listProductChangeRequests,
  markProductChangeRequestImplemented,
  rejectProductChangeRequest,
  updateProductChangeRequest,
} from "@/api/productChangeRequests";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";

const changeRequest = {
  id: "42000000-0000-0000-0000-000000000001",
  productId: "41000000-0000-0000-0000-000000000201",
  productName: "Life Protection",
  productType: "LIFE_INSURANCE",
  requestedByUserId: "10000000-0000-0000-0000-000000009901",
  requestedByFullName: "Product Manager",
  requestType: "PRICE_CHANGE",
  description: "Increase annual premium to reflect market rates.",
  status: "OPEN",
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
};

describe("productChangeRequests api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads product change requests with search filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change requests loaded",
        data: [changeRequest],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listProductChangeRequests({
        productId: changeRequest.productId as string,
        status: "OPEN",
      }),
    ).resolves.toEqual([changeRequest]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-change-requests?productId=${changeRequest.productId}&status=OPEN`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("creates a product change request", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change request created",
        data: changeRequest,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const createdRequest = await createProductChangeRequest({
      productId: changeRequest.productId as string,
      requestType: "PRICE_CHANGE",
      description: "Increase annual premium to reflect market rates.",
    });

    expect(createdRequest).toEqual(changeRequest);
    expect(createdRequest.status).toBe("OPEN");
    expect(createdRequest.requestType).toBe("PRICE_CHANGE");

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/product-change-requests`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        productId: changeRequest.productId,
        requestType: "PRICE_CHANGE",
        description: "Increase annual premium to reflect market rates.",
      }),
    });
  });

  it("allows a product manager to create a product change request", async () => {
    const productManagerToken = createAccessToken(["PRODUCT_MANAGER"]);
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, productManagerToken);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change request created",
        data: changeRequest,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const createdRequest = await createProductChangeRequest({
      productId: changeRequest.productId as string,
      requestType: "DURATION_CHANGE",
      description: "Extend standard contract duration to 24 months.",
    });

    expect(createdRequest.status).toBe("OPEN");
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/product-change-requests`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${productManagerToken}`,
      },
      body: JSON.stringify({
        productId: changeRequest.productId,
        requestType: "DURATION_CHANGE",
        description: "Extend standard contract duration to 24 months.",
      }),
    });
  });

  it("returns saved product change requests when listing for tracking", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const approvedRequest = { ...changeRequest, status: "APPROVED" as const };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change requests loaded",
        data: [changeRequest, approvedRequest],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const requests = await listProductChangeRequests({
      productId: changeRequest.productId as string,
      status: "ALL",
    });

    expect(requests).toHaveLength(2);
    expect(requests[0]?.status).toBe("OPEN");
    expect(requests[1]?.status).toBe("APPROVED");
  });

  it("updates an open product change request", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change request updated",
        data: {
          ...changeRequest,
          description: "Increase annual premium and update renewal terms.",
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateProductChangeRequest(
        changeRequest.id,
        "Increase annual premium and update renewal terms.",
      ),
    ).resolves.toEqual({
      ...changeRequest,
      description: "Increase annual premium and update renewal terms.",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-change-requests/${changeRequest.id}`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
        body: JSON.stringify({
          description: "Increase annual premium and update renewal terms.",
        }),
      },
    );
  });

  it("approves a product change request", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change request approved",
        data: { ...changeRequest, status: "APPROVED" },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(approveProductChangeRequest(changeRequest.id)).resolves.toEqual({
      ...changeRequest,
      status: "APPROVED",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-change-requests/${changeRequest.id}/approve`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("rejects a product change request", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change request rejected",
        data: { ...changeRequest, status: "REJECTED" },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(rejectProductChangeRequest(changeRequest.id)).resolves.toEqual({
      ...changeRequest,
      status: "REJECTED",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-change-requests/${changeRequest.id}/reject`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("marks a product change request as implemented", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Product change request marked implemented",
        data: { ...changeRequest, status: "IMPLEMENTED" },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(markProductChangeRequestImplemented(changeRequest.id)).resolves.toEqual({
      ...changeRequest,
      status: "IMPLEMENTED",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-change-requests/${changeRequest.id}/mark-implemented`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
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
