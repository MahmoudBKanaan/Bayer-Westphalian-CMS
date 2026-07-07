import { apiRequest } from "@/api/client";

export type BeneficiaryView = {
  id: string;
  policyholderCustomerId: string | null;
  policyholderFullName: string | null;
  beneficiaryCustomerId: string | null;
  beneficiaryFullName: string | null;
  relationship: string;
  guardianName: string | null;
  guardianEmail: string | null;
  guardianConsentRequired: boolean;
  hasGuardianRequirement: boolean;
  createdAt: string | null;
};

export type BeneficiarySearchFilters = {
  policyholderCustomerId?: string;
  beneficiaryCustomerId?: string;
  guardianConsentRequired?: boolean;
};

export type UpdateBeneficiaryPayload = {
  relationship: string;
  guardianName: string | null;
  guardianEmail: string | null;
  guardianConsentRequired: boolean;
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listBeneficiaries(
  filters: BeneficiarySearchFilters = {},
): Promise<BeneficiaryView[]> {
  const response = await apiRequest<ApiResponse<BeneficiaryView[]>>(
    `/beneficiaries${beneficiarySearchQuery(filters)}`,
  );

  return response.data;
}

export async function updateBeneficiary(
  id: string,
  payload: UpdateBeneficiaryPayload,
): Promise<BeneficiaryView> {
  const response = await apiRequest<ApiResponse<BeneficiaryView>>(`/beneficiaries/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

  return response.data;
}

function beneficiarySearchQuery(filters: BeneficiarySearchFilters) {
  const params = new URLSearchParams();
  appendOptionalParam(params, "policyholderCustomerId", filters.policyholderCustomerId);
  appendOptionalParam(params, "beneficiaryCustomerId", filters.beneficiaryCustomerId);

  if (filters.guardianConsentRequired != null) {
    params.set("guardianConsentRequired", String(filters.guardianConsentRequired));
  }

  const queryString = params.toString();
  return queryString.length === 0 ? "" : `?${queryString}`;
}

function appendOptionalParam(params: URLSearchParams, key: string, value?: string) {
  const trimmed = value?.trim() ?? "";
  if (trimmed.length > 0) {
    params.set(key, trimmed);
  }
}
