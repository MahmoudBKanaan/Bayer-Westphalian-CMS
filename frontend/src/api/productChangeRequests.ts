import { apiRequest } from "@/api/client";
import type { ProductType } from "@/api/products";

export type ProductChangeType =
  "PRICE_CHANGE" | "DURATION_CHANGE" | "EXPIRATION_RULE_CHANGE" | "STATUS_CHANGE";

export type ProductChangeStatus = "OPEN" | "APPROVED" | "REJECTED" | "IMPLEMENTED";

export type ProductChangeRequestView = {
  id: string;
  productId: string | null;
  productName: string | null;
  productType: ProductType | null;
  requestedByUserId: string | null;
  requestedByFullName: string | null;
  requestType: ProductChangeType;
  description: string;
  status: ProductChangeStatus;
  createdAt: string | null;
  updatedAt: string | null;
};

export type ProductChangeRequestSearchFilters = {
  productId?: string;
  status?: ProductChangeStatus | "ALL";
};

export type CreateProductChangeRequestPayload = {
  productId: string;
  requestType: ProductChangeType;
  description: string;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listProductChangeRequests(
  filters?: ProductChangeRequestSearchFilters,
): Promise<ProductChangeRequestView[]> {
  const response = await apiRequest<ApiResponse<ProductChangeRequestView[]>>(
    `/product-change-requests${changeRequestSearchQuery(filters)}`,
  );

  return response.data;
}

export async function createProductChangeRequest(
  payload: CreateProductChangeRequestPayload,
): Promise<ProductChangeRequestView> {
  const response = await apiRequest<ApiResponse<ProductChangeRequestView>>(
    "/product-change-requests",
    {
      method: "POST",
      body: JSON.stringify({
        productId: payload.productId,
        requestType: payload.requestType,
        description: payload.description.trim(),
      }),
    },
  );

  return response.data;
}

export async function updateProductChangeRequest(
  id: string,
  description: string,
): Promise<ProductChangeRequestView> {
  const response = await apiRequest<ApiResponse<ProductChangeRequestView>>(
    `/product-change-requests/${id}`,
    {
      method: "PUT",
      body: JSON.stringify({ description: description.trim() }),
    },
  );

  return response.data;
}

export async function approveProductChangeRequest(id: string): Promise<ProductChangeRequestView> {
  const response = await apiRequest<ApiResponse<ProductChangeRequestView>>(
    `/product-change-requests/${id}/approve`,
    { method: "PATCH" },
  );

  return response.data;
}

export async function rejectProductChangeRequest(id: string): Promise<ProductChangeRequestView> {
  const response = await apiRequest<ApiResponse<ProductChangeRequestView>>(
    `/product-change-requests/${id}/reject`,
    { method: "PATCH" },
  );

  return response.data;
}

export async function markProductChangeRequestImplemented(
  id: string,
): Promise<ProductChangeRequestView> {
  const response = await apiRequest<ApiResponse<ProductChangeRequestView>>(
    `/product-change-requests/${id}/mark-implemented`,
    { method: "PATCH" },
  );

  return response.data;
}

function changeRequestSearchQuery(filters?: ProductChangeRequestSearchFilters) {
  if (filters == null) {
    return "";
  }

  const params = new URLSearchParams();
  if (filters.productId != null && filters.productId.trim().length > 0) {
    params.set("productId", filters.productId.trim());
  }
  if (filters.status != null && filters.status !== "ALL") {
    params.set("status", filters.status);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}
