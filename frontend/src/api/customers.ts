import { apiRequest } from "@/api/client";

export type CustomerType = "CUSTOMER" | "PROSPECT" | "BENEFICIARY";
export type CustomerAgeGroup = "MINOR" | "AGE_18_25" | "AGE_26_40" | "AGE_41_60" | "AGE_60_PLUS";
export type CustomerStatus = "ACTIVE" | "INACTIVE" | "INTERESTED" | "UNINTERESTED" | "CONVERTED";

export type CustomerView = {
  id: string;
  customerType: CustomerType;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  addressLine: string | null;
  city: string | null;
  country: string | null;
  dateOfBirth: string | null;
  ageGroup: CustomerAgeGroup | null;
  status: CustomerStatus;
  doNotContact: boolean;
  active: boolean;
  contactable: boolean;
  source: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  deletedAt: string | null;
};

export type CustomerFormPayload = {
  customerType?: CustomerType;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  addressLine: string;
  city: string;
  country: string;
  dateOfBirth: string;
  ageGroup: CustomerAgeGroup | "";
  status: CustomerStatus;
  doNotContact: boolean;
  source: string;
};

export type CustomerSearchFilters = {
  term: string;
  customerType: CustomerType | "ALL";
  status: CustomerStatus | "ALL";
  city: string;
  country: string;
  contactable: "ALL" | "true" | "false";
};

export type CustomerImportError = {
  lineNumber: number;
  field: string;
  message: string;
  value: string | null;
};

export type CustomerImportResult = {
  importedCount: number;
  failedCount: number;
  customers: CustomerView[];
  errors: CustomerImportError[];
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};

export async function listCustomers(filters?: CustomerSearchFilters): Promise<CustomerView[]> {
  const response = await apiRequest<ApiResponse<PageResponse<CustomerView>>>(
    `/customers${customerSearchQuery(filters)}`,
  );

  return response.data.content;
}

export async function getCustomer(id: string): Promise<CustomerView> {
  const response = await apiRequest<ApiResponse<CustomerView>>(`/customers/${id}`);

  return response.data;
}

export async function createCustomer(payload: CustomerFormPayload): Promise<CustomerView> {
  const response = await apiRequest<ApiResponse<CustomerView>>("/customers", {
    method: "POST",
    body: JSON.stringify(toApiPayload(payload)),
  });

  return response.data;
}

export async function updateCustomer(
  id: string,
  payload: CustomerFormPayload,
): Promise<CustomerView> {
  const response = await apiRequest<ApiResponse<CustomerView>>(`/customers/${id}`, {
    method: "PUT",
    body: JSON.stringify(toApiPayload(payload, false)),
  });

  return response.data;
}

export async function deleteCustomer(id: string): Promise<CustomerView> {
  const response = await apiRequest<ApiResponse<CustomerView>>(`/customers/${id}`, {
    method: "DELETE",
  });

  return response.data;
}

export async function importCustomersCsv(file: File): Promise<CustomerImportResult> {
  const formData = new FormData();
  formData.set("file", file);

  const response = await apiRequest<ApiResponse<CustomerImportResult>>("/customers/import", {
    method: "POST",
    body: formData,
  });

  return response.data;
}

function customerSearchQuery(filters?: CustomerSearchFilters) {
  const params = new URLSearchParams({ page: "0", size: "50" });
  if (filters == null) {
    return `?${params.toString()}`;
  }
  appendOptionalParam(params, "term", filters.term);
  appendOptionalParam(params, "city", filters.city);
  appendOptionalParam(params, "country", filters.country);
  if (filters.customerType !== "ALL") {
    params.set("customerType", filters.customerType);
  }
  if (filters.status !== "ALL") {
    params.set("status", filters.status);
  }
  if (filters.contactable !== "ALL") {
    params.set("contactable", filters.contactable);
  }
  return `?${params.toString()}`;
}

function toApiPayload(payload: CustomerFormPayload, includeType = true) {
  return {
    ...(includeType ? { customerType: payload.customerType } : {}),
    firstName: payload.firstName.trim(),
    lastName: payload.lastName.trim(),
    email: optionalString(payload.email),
    phone: optionalString(payload.phone),
    addressLine: optionalString(payload.addressLine),
    city: optionalString(payload.city),
    country: optionalString(payload.country),
    dateOfBirth: optionalString(payload.dateOfBirth),
    ageGroup: payload.ageGroup === "" ? null : payload.ageGroup,
    status: payload.status,
    doNotContact: payload.doNotContact,
    source: optionalString(payload.source),
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
