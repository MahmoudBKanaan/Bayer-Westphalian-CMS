import { apiRequest } from "@/api/client";

export type ProductType =
  | "HOMEOWNER_INSURANCE"
  | "LIFE_INSURANCE"
  | "INVESTMENT_FUND"
  | "HEALTH_INSURANCE"
  | "AUTO_INSURANCE"
  | "OTHER";

export type ProductView = {
  id: string;
  name: string;
  productType: ProductType;
  description: string | null;
  price: number | null;
  durationMonths: number | null;
  expirationPolicy: string | null;
  active: boolean;
  deleted: boolean;
  createdAt: string | null;
  updatedAt: string | null;
  deletedAt: string | null;
};

export type ProductFormPayload = {
  name: string;
  productType: ProductType;
  description: string;
  price: string;
  durationMonths: string;
  expirationPolicy: string;
  active: boolean;
};

export type ProductSearchFilters = {
  term: string;
  productType: ProductType | "ALL";
  active: "ALL" | "true" | "false";
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listProducts(filters?: ProductSearchFilters): Promise<ProductView[]> {
  const response = await apiRequest<ApiResponse<ProductView[]>>(
    `/products${productSearchQuery(filters)}`,
  );

  return response.data;
}

export async function getProduct(id: string): Promise<ProductView> {
  const response = await apiRequest<ApiResponse<ProductView>>(`/products/${id}`);

  return response.data;
}

export async function createProduct(payload: ProductFormPayload): Promise<ProductView> {
  const response = await apiRequest<ApiResponse<ProductView>>("/products", {
    method: "POST",
    body: JSON.stringify(toCreatePayload(payload)),
  });

  return response.data;
}

export async function updateProduct(id: string, payload: ProductFormPayload): Promise<ProductView> {
  const response = await apiRequest<ApiResponse<ProductView>>(`/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(toUpdatePayload(payload)),
  });

  return response.data;
}

export async function disableProduct(id: string): Promise<ProductView> {
  const response = await apiRequest<ApiResponse<ProductView>>(`/products/${id}/disable`, {
    method: "PATCH",
  });

  return response.data;
}

export async function deleteProduct(id: string): Promise<ProductView> {
  const response = await apiRequest<ApiResponse<ProductView>>(`/products/${id}`, {
    method: "DELETE",
  });

  return response.data;
}

function productSearchQuery(filters?: ProductSearchFilters) {
  if (filters == null) {
    return "";
  }

  const params = new URLSearchParams();
  appendOptionalParam(params, "term", filters.term);
  if (filters.productType !== "ALL") {
    params.set("productType", filters.productType);
  }
  if (filters.active !== "ALL") {
    params.set("active", filters.active);
  }

  const query = params.toString();
  return query.length === 0 ? "" : `?${query}`;
}

function toCreatePayload(payload: ProductFormPayload) {
  return {
    name: payload.name.trim(),
    productType: payload.productType,
    description: optionalString(payload.description),
    price: optionalPrice(payload.price),
    durationMonths: optionalPositiveInteger(payload.durationMonths),
    expirationPolicy: optionalString(payload.expirationPolicy),
  };
}

function toUpdatePayload(payload: ProductFormPayload) {
  return {
    ...toCreatePayload(payload),
    active: payload.active,
  };
}

function appendOptionalParam(params: URLSearchParams, key: string, value: string) {
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function optionalPrice(value: string) {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return null;
  }

  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function optionalPositiveInteger(value: string) {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return null;
  }

  const parsed = Number.parseInt(trimmed, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}
