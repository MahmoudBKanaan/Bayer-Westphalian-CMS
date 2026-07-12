import { apiRequest } from "@/api/client";

export type SegmentVisibility = "PRIVATE" | "TEAM" | "GLOBAL";

export type SegmentOperator =
  "EQUALS" | "NOT_EQUALS" | "CONTAINS" | "IN" | "BETWEEN" | "BEFORE" | "AFTER";

export type SegmentJoinOperator = "AND" | "OR";

export type SegmentCriteriaView = {
  id: string;
  segmentId: string;
  fieldName: string;
  operator: SegmentOperator;
  value: string;
  logicalGroup: string | null;
  joinOperator: SegmentJoinOperator;
};

export type SegmentView = {
  id: string;
  name: string;
  description: string | null;
  ownerUserId: string | null;
  ownerFullName: string | null;
  visibility: SegmentVisibility;
  criteria: SegmentCriteriaView[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type SegmentCriteriaPayload = {
  fieldName: string;
  operator: SegmentOperator;
  value: string;
  logicalGroup?: string;
  joinOperator?: SegmentJoinOperator;
};

export type SegmentFormPayload = {
  name: string;
  description: string;
  visibility: SegmentVisibility;
  criteria: SegmentCriteriaPayload[];
};

export type SegmentSearchFilters = {
  term: string;
  visibility: SegmentVisibility | "ALL";
};

export type SegmentExclusionReasonSummary = {
  code: string;
  message: string;
  count: number;
};

export type CustomerPreviewView = {
  id: string;
  customerType: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  city: string | null;
  country: string | null;
  status: string;
  doNotContact: boolean;
};

export type SegmentPreviewView = {
  totalAudienceCount: number;
  eligibleCount: number;
  excludedCount: number;
  matchingCustomers: CustomerPreviewView[];
  exclusionReasonSummary: SegmentExclusionReasonSummary[];
};

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export async function listSegments(filters?: SegmentSearchFilters): Promise<SegmentView[]> {
  const response = await apiRequest<ApiResponse<SegmentView[]>>(
    `/segments${segmentSearchQuery(filters)}`,
  );
  return response.data;
}

export async function getSegment(id: string): Promise<SegmentView> {
  const response = await apiRequest<ApiResponse<SegmentView>>(`/segments/${id}`);
  return response.data;
}

export async function createSegment(payload: SegmentFormPayload): Promise<SegmentView> {
  const response = await apiRequest<ApiResponse<SegmentView>>("/segments", {
    method: "POST",
    body: JSON.stringify(toSegmentWritePayload(payload)),
  });
  return response.data;
}

export async function updateSegment(id: string, payload: SegmentFormPayload): Promise<SegmentView> {
  const response = await apiRequest<ApiResponse<SegmentView>>(`/segments/${id}`, {
    method: "PUT",
    body: JSON.stringify(toSegmentWritePayload(payload)),
  });
  return response.data;
}

export async function deleteSegment(id: string): Promise<void> {
  await apiRequest<ApiResponse<null>>(`/segments/${id}`, {
    method: "DELETE",
  });
}

/**
 * Audience preview with server-side eligibility (item 208 production gate).
 * There is no client API for criteria-only matching as a final campaign audience.
 */
export async function previewSegment(
  criteria: SegmentCriteriaPayload[],
): Promise<SegmentPreviewView> {
  const response = await apiRequest<ApiResponse<SegmentPreviewView>>("/segments/preview", {
    method: "POST",
    body: JSON.stringify({
      criteria: criteria.map((criterion) => ({
        fieldName: criterion.fieldName,
        operator: criterion.operator,
        value: criterion.value,
        logicalGroup: criterion.logicalGroup || null,
        joinOperator: criterion.joinOperator ?? "AND",
      })),
    }),
  });
  return response.data;
}

export function emptySegmentForm(): SegmentFormPayload {
  return {
    name: "",
    description: "",
    visibility: "PRIVATE",
    criteria: [],
  };
}

export function segmentToForm(segment: SegmentView): SegmentFormPayload {
  return {
    name: segment.name,
    description: segment.description ?? "",
    visibility: segment.visibility,
    criteria: segment.criteria.map((criterion) => ({
      fieldName: criterion.fieldName,
      operator: criterion.operator,
      value: criterion.value,
      logicalGroup: criterion.logicalGroup ?? "",
      joinOperator: criterion.joinOperator,
    })),
  };
}

function toSegmentWritePayload(payload: SegmentFormPayload) {
  return {
    name: payload.name.trim(),
    description: payload.description.trim() || null,
    visibility: payload.visibility,
    criteria: payload.criteria
      .filter((criterion) => criterion.fieldName.trim() !== "" && criterion.value.trim() !== "")
      .map((criterion) => ({
        fieldName: criterion.fieldName.trim(),
        operator: criterion.operator,
        value: criterion.value.trim(),
        logicalGroup: criterion.logicalGroup?.trim() || null,
        joinOperator: criterion.joinOperator ?? "AND",
      })),
  };
}

function segmentSearchQuery(filters?: SegmentSearchFilters) {
  if (filters == null) {
    return "";
  }

  const params = new URLSearchParams();
  if (filters.term.trim() !== "") {
    params.set("term", filters.term.trim());
  }
  if (filters.visibility !== "ALL") {
    params.set("visibility", filters.visibility);
  }

  const query = params.toString();
  return query === "" ? "" : `?${query}`;
}
