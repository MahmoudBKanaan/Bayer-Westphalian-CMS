import { apiRequest } from "@/api/client";

export type ProductType =
  | "HOMEOWNER_INSURANCE"
  | "LIFE_INSURANCE"
  | "INVESTMENT_FUND"
  | "HEALTH_INSURANCE"
  | "AUTO_INSURANCE"
  | "OTHER";

export type OwnershipStatus = "ACTIVE" | "EXPIRED" | "CANCELLED";

export type ProductOwnershipView = {
  id: string;
  customerId: string | null;
  customerFullName: string | null;
  productId: string | null;
  productName: string | null;
  productType: ProductType | null;
  policyNumber: string | null;
  startDate: string | null;
  expirationDate: string | null;
  status: OwnershipStatus;
  active: boolean;
  createdAt: string | null;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export type AssignProductOwnershipPayload = {
  customerId: string;
  productId: string;
  startDate: string;
  expirationDate: string;
  policyNumber: string;
};

export type UpdateProductOwnershipPayload = {
  expirationDate: string;
  policyNumber: string;
};

export async function listCustomerProductOwnerships(
  customerId: string,
): Promise<ProductOwnershipView[]> {
  const trimmedCustomerId = customerId.trim();
  const response = await apiRequest<ApiResponse<ProductOwnershipView[]>>(
    `/product-ownerships?customerId=${encodeURIComponent(trimmedCustomerId)}`,
  );

  return response.data;
}

export async function assignProductOwnership(
  payload: AssignProductOwnershipPayload,
): Promise<ProductOwnershipView> {
  const response = await apiRequest<ApiResponse<ProductOwnershipView>>("/product-ownerships", {
    method: "POST",
    body: JSON.stringify({
      customerId: payload.customerId.trim(),
      productId: payload.productId.trim(),
      startDate: payload.startDate.trim(),
      expirationDate: optionalString(payload.expirationDate),
      policyNumber: optionalString(payload.policyNumber),
    }),
  });

  return response.data;
}

export async function updateProductOwnership(
  ownershipId: string,
  payload: UpdateProductOwnershipPayload,
): Promise<ProductOwnershipView> {
  const response = await apiRequest<ApiResponse<ProductOwnershipView>>(
    `/product-ownerships/${encodeURIComponent(ownershipId.trim())}`,
    {
      method: "PUT",
      body: JSON.stringify({
        expirationDate: optionalString(payload.expirationDate),
        policyNumber: optionalString(payload.policyNumber),
      }),
    },
  );

  return response.data;
}

function optionalString(value: string) {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}
