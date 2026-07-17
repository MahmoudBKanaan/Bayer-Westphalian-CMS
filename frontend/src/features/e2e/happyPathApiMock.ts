/**
 * Deterministic REST mock for Playwright happy-path E2E (item 597).
 *
 * Pure request → response mapping so Vitest can cover routing without a browser,
 * while Playwright installs the same logic via page.route.
 */

import {
  HAPPY_PATH_ADMIN,
  HAPPY_PATH_FIXTURES,
} from "@/features/e2e/happyPathFlow";

export type MockHttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

/** Matches backend API paths without intercepting Vite modules such as /src/api/client.ts. */
export const E2E_API_ROUTE_PATTERN = /^https?:\/\/[^/]+\/api(?:\/|$)/;

export type MockApiRequest = {
  method: MockHttpMethod;
  /** Full request URL. */
  url: string;
  /** Raw JSON body when present. */
  bodyText?: string;
};

export type MockApiResponse = {
  status: number;
  body: unknown;
};

export type HappyPathMockState = {
  customers: CustomerRecord[];
  consents: ConsentRecord[];
  products: ProductRecord[];
  segments: SegmentRecord[];
  campaign: CampaignRecord | null;
};

type SegmentRecord = {
  id: string;
  name: string;
  description: string | null;
  ownerUserId: string | null;
  ownerFullName: string | null;
  visibility: string;
  criteria: Array<{
    id: string;
    segmentId: string;
    fieldName: string;
    operator: string;
    value: string;
    logicalGroup: string | null;
    joinOperator: string;
  }>;
  createdAt: string | null;
  updatedAt: string | null;
};

type ProductRecord = {
  id: string;
  name: string;
  productType: string;
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

type CustomerRecord = {
  id: string;
  customerType: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  addressLine: string | null;
  city: string | null;
  country: string | null;
  dateOfBirth: string | null;
  ageGroup: string | null;
  status: string;
  doNotContact: boolean;
  active: boolean;
  contactable: boolean;
  source: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  deletedAt: string | null;
};

type ConsentRecord = {
  id: string;
  customerId: string | null;
  customerFullName: string | null;
  consentType: string;
  status: string;
  purpose: string;
  source: string | null;
  grantedAt: string | null;
  withdrawnAt: string | null;
  expiresAt: string | null;
  evidenceFileUrl: string | null;
  createdBy: string | null;
  createdByFullName: string | null;
  createdAt: string | null;
  valid: boolean;
  requiresAction: boolean;
};

type CampaignRecord = {
  id: string;
  name: string;
  objective: string;
  status: string;
  ownerUserId: string | null;
  ownerFullName: string | null;
  segmentId: string | null;
  segmentName: string | null;
  channel: string;
  messageSubject: string | null;
  messageBody: string | null;
  startDate: string | null;
  endDate: string | null;
  approvedByUserId: string | null;
  approvedByFullName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  complianceReviewNotes: string | null;
  productIds: string[];
  createdAt: string | null;
  updatedAt: string | null;
};

const NOW = "2026-07-12T12:00:00Z";

export function createHappyPathMockState(): HappyPathMockState {
  return {
    customers: [],
    consents: [],
    products: [demoProduct()],
    segments: [demoSegment()],
    campaign: null,
  };
}

export function createAccessTokenPayload(roles: string[] = ["ADMIN"]): string {
  const payload = toBase64Url(JSON.stringify({ roles }));
  return `e2e-header.${payload}.e2e-signature`;
}

/**
 * Handles a single API call against the shared mock state.
 * Mutates {@code state} for write operations.
 */
export function handleHappyPathApiRequest(
  state: HappyPathMockState,
  request: MockApiRequest,
): MockApiResponse {
  const method = request.method.toUpperCase() as MockHttpMethod;
  const url = new URL(request.url);
  const path = stripApiPrefix(url.pathname);
  const body = parseJsonBody(request.bodyText);

  if (method === "GET" && path === "/health") {
    // AppLayout health probe uses raw actuator-style JSON (not ApiResponse envelope).
    return {
      status: 200,
      body: { status: "UP", service: "bwc-campaign-api" },
    };
  }

  if (method === "POST" && path === "/auth/login") {
    return handleLogin(body);
  }

  if (method === "GET" && path === "/analytics/dashboard") {
    // Prefer a non-empty fixture so item 606-style KPI smoke paths stay meaningful.
    return ok(demoDashboard());
  }

  // Item 609: main-screen a11y smoke paths (analytics detail, audit, users, reports).
  if (method === "GET" && path === "/analytics/products/performance") {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/analytics/campaigns/")) {
    return ok({
      campaignId: path.split("/").pop(),
      campaignName: "Demo campaign",
      metrics: null,
    });
  }

  if (method === "GET" && path.startsWith("/audit-logs")) {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/users")) {
    return ok([
      {
        id: HAPPY_PATH_ADMIN.userId,
        email: HAPPY_PATH_ADMIN.email,
        fullName: HAPPY_PATH_ADMIN.fullName,
        status: "ACTIVE",
        roles: ["ADMIN"],
        lastLoginAt: NOW,
      },
    ]);
  }

  if (method === "GET" && path.startsWith("/reports/exports")) {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/customers")) {
    return handleCustomersGet(state, path);
  }

  if (method === "POST" && path === "/customers") {
    return handleCustomerCreate(state, body);
  }

  if (method === "GET" && path.startsWith("/consents")) {
    return handleConsentsGet(state, url);
  }

  if (method === "POST" && path === "/consents/withdraw") {
    return handleConsentWithdraw(state, body);
  }

  if (method === "POST" && path === "/consents") {
    return handleConsentCreate(state, body);
  }

  if (method === "GET" && path.startsWith("/beneficiaries")) {
    return ok([]);
  }

  if (method === "GET" && path.includes("/product-ownerships")) {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/products")) {
    return ok(state.products);
  }

  if (method === "POST" && path === "/products") {
    return handleProductCreate(state, body);
  }

  if (method === "GET" && path.startsWith("/payment-records")) {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/contact-events")) {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/follow-up-tasks")) {
    return ok([]);
  }

  if (method === "GET" && path.startsWith("/segments")) {
    return ok(state.segments);
  }

  if (method === "POST" && path === "/segments") {
    return handleSegmentCreate(state, body);
  }

  if (method === "GET" && path.startsWith("/campaigns")) {
    return handleCampaignsGet(state, path, url);
  }

  if (method === "POST" && path === "/campaigns") {
    return handleCampaignCreate(state, body);
  }

  if (method === "PUT" && path.match(/^\/campaigns\/[^/]+\/products$/)) {
    return handleCampaignProducts(state, path, body);
  }

  if (method === "POST" && path.match(/^\/campaigns\/[^/]+\/submit$/)) {
    return handleCampaignStatus(state, path, "SUBMITTED");
  }

  if (method === "POST" && path.match(/^\/campaigns\/[^/]+\/approve$/)) {
    return handleCampaignApprove(state, path, body);
  }

  if (method === "POST" && path.match(/^\/campaigns\/[^/]+\/launch$/)) {
    return handleCampaignStatus(state, path, "ACTIVE");
  }

  return {
    status: 404,
    body: {
      success: false,
      message: `Happy-path mock has no handler for ${method} ${path}`,
      data: null,
    },
  };
}

function handleLogin(body: Record<string, unknown> | null): MockApiResponse {
  const email = stringField(body, "email");
  const password = stringField(body, "password");
  if (email !== HAPPY_PATH_ADMIN.email || password !== HAPPY_PATH_ADMIN.password) {
    return {
      status: 401,
      body: {
        success: false,
        message: "Invalid credentials",
        data: null,
      },
    };
  }

  return ok({
    user: {
      id: HAPPY_PATH_ADMIN.userId,
      email: HAPPY_PATH_ADMIN.email,
      fullName: HAPPY_PATH_ADMIN.fullName,
      status: "ACTIVE",
      lastLoginAt: NOW,
    },
    tokens: {
      accessToken: createAccessTokenPayload(["ADMIN"]),
      accessTokenExpiresAt: "2099-01-01T00:00:00Z",
      refreshToken: "e2e-refresh-token",
      refreshTokenExpiresAt: "2099-01-01T00:00:00Z",
    },
  });
}

function handleCustomersGet(state: HappyPathMockState, path: string): MockApiResponse {
  const detailMatch = path.match(/^\/customers\/([^/]+)$/);
  if (detailMatch != null) {
    const customer = state.customers.find((row) => row.id === detailMatch[1]);
    if (customer == null) {
      return notFound("Customer not found");
    }
    return ok(customer);
  }

  return ok({
    content: state.customers,
    page: 0,
    size: 50,
    totalElements: state.customers.length,
    totalPages: 1,
    first: true,
    last: true,
    empty: state.customers.length === 0,
  });
}

function handleCustomerCreate(
  state: HappyPathMockState,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const firstName = stringField(body, "firstName") || HAPPY_PATH_FIXTURES.customerFirstName;
  const lastName = stringField(body, "lastName") || HAPPY_PATH_FIXTURES.customerLastName;
  const customer: CustomerRecord = {
    id: HAPPY_PATH_FIXTURES.customerId,
    customerType: stringField(body, "customerType") || "CUSTOMER",
    firstName,
    lastName,
    fullName: `${firstName} ${lastName}`,
    email: stringField(body, "email") || HAPPY_PATH_FIXTURES.customerEmail,
    phone: stringField(body, "phone") || null,
    addressLine: stringField(body, "addressLine") || null,
    city: stringField(body, "city") || "Munich",
    country: stringField(body, "country") || "Germany",
    dateOfBirth: stringField(body, "dateOfBirth") || null,
    ageGroup: stringField(body, "ageGroup") || null,
    status: stringField(body, "status") || "ACTIVE",
    doNotContact: Boolean(body?.doNotContact),
    active: true,
    contactable: !body?.doNotContact,
    source: stringField(body, "source") || "E2E",
    createdAt: NOW,
    updatedAt: NOW,
    deletedAt: null,
  };
  state.customers = [customer, ...state.customers.filter((row) => row.id !== customer.id)];
  return ok(customer, "Customer created");
}

function handleConsentsGet(state: HappyPathMockState, url: URL): MockApiResponse {
  const customerId = url.searchParams.get("customerId");
  const rows =
    customerId == null
      ? state.consents
      : state.consents.filter((row) => row.customerId === customerId);
  return ok(rows);
}

function handleConsentCreate(
  state: HappyPathMockState,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const customerId = stringField(body, "customerId") || HAPPY_PATH_FIXTURES.customerId;
  const customer = state.customers.find((row) => row.id === customerId);
  const status = stringField(body, "status") || "GIVEN";
  const consent: ConsentRecord = {
    id: `61000000-0000-0000-0000-00000000e5${String(state.consents.length + 1).padStart(2, "0")}`,
    customerId,
    customerFullName: customer?.fullName ?? null,
    consentType: stringField(body, "consentType") || "MARKETING_EMAIL",
    status,
    purpose: stringField(body, "purpose") || HAPPY_PATH_FIXTURES.consentPurpose,
    source: stringField(body, "source") || HAPPY_PATH_FIXTURES.consentSource,
    grantedAt: NOW,
    withdrawnAt: status === "WITHDRAWN" ? NOW : null,
    expiresAt: null,
    evidenceFileUrl: stringField(body, "evidenceFileUrl") || null,
    createdBy: HAPPY_PATH_ADMIN.userId,
    createdByFullName: HAPPY_PATH_ADMIN.fullName,
    createdAt: NOW,
    valid: status === "GIVEN",
    requiresAction: status === "REQUIRED",
  };
  state.consents = [consent, ...state.consents];
  return ok(consent, "Consent recorded");
}

function handleConsentWithdraw(
  state: HappyPathMockState,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const consentRecordId = stringField(body, "consentRecordId");
  const index = state.consents.findIndex((row) => row.id === consentRecordId);
  if (index < 0) {
    return notFound("Consent record not found");
  }
  const existing = state.consents[index];
  const updated: ConsentRecord = {
    ...existing,
    status: "WITHDRAWN",
    valid: false,
    requiresAction: false,
    withdrawnAt: NOW,
  };
  state.consents = [
    ...state.consents.slice(0, index),
    updated,
    ...state.consents.slice(index + 1),
  ];
  return ok(updated, "Consent withdrawn");
}

function handleCampaignsGet(
  state: HappyPathMockState,
  path: string,
  url: URL,
): MockApiResponse {
  if (path.match(/^\/campaigns\/[^/]+\/recipients\/preview$/)) {
    return ok(demoPreview());
  }
  if (path.match(/^\/campaigns\/[^/]+\/recipients\/eligible$/)) {
    return ok(demoEligibleRecipients(state));
  }
  if (path.match(/^\/campaigns\/[^/]+\/recipients\/excluded$/)) {
    return ok([]);
  }
  if (path.match(/^\/campaigns\/[^/]+\/recipients\/summary$/)) {
    return ok({
      campaignId: state.campaign?.id ?? HAPPY_PATH_FIXTURES.campaignId,
      eligible: 1,
      excluded: 0,
      sent: state.campaign?.status === "ACTIVE" ? 1 : 0,
      failed: 0,
    });
  }

  const detailMatch = path.match(/^\/campaigns\/([^/]+)$/);
  if (detailMatch != null) {
    if (state.campaign == null || state.campaign.id !== detailMatch[1]) {
      return notFound("Campaign not found");
    }
    return ok(state.campaign);
  }

  const statusFilter = url.searchParams.get("status");
  const campaigns = state.campaign == null ? [] : [state.campaign];
  const filtered =
    statusFilter == null || statusFilter === "" || statusFilter === "ALL"
      ? campaigns
      : campaigns.filter((campaign) => campaign.status === statusFilter);
  return ok(filtered);
}

function handleCampaignCreate(
  state: HappyPathMockState,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const segmentId = stringField(body, "segmentId") || HAPPY_PATH_FIXTURES.segmentId;
  const campaign: CampaignRecord = {
    id: HAPPY_PATH_FIXTURES.campaignId,
    name: stringField(body, "name") || HAPPY_PATH_FIXTURES.campaignName,
    objective: stringField(body, "objective") || HAPPY_PATH_FIXTURES.campaignObjective,
    status: "DRAFT",
    ownerUserId: HAPPY_PATH_ADMIN.userId,
    ownerFullName: HAPPY_PATH_ADMIN.fullName,
    segmentId,
    segmentName: HAPPY_PATH_FIXTURES.segmentName,
    channel: stringField(body, "channel") || "EMAIL",
    messageSubject: stringField(body, "messageSubject") || HAPPY_PATH_FIXTURES.campaignSubject,
    messageBody: stringField(body, "messageBody") || HAPPY_PATH_FIXTURES.campaignBody,
    startDate: stringField(body, "startDate") || "2026-08-01",
    endDate: stringField(body, "endDate") || "2026-08-31",
    approvedByUserId: null,
    approvedByFullName: null,
    approvedAt: null,
    rejectionReason: null,
    complianceReviewNotes: null,
    productIds: [],
    createdAt: NOW,
    updatedAt: NOW,
  };
  state.campaign = campaign;
  return ok(campaign, "Campaign created");
}

function handleCampaignProducts(
  state: HappyPathMockState,
  path: string,
  body: Record<string, unknown> | null,
): MockApiResponse {
  if (state.campaign == null) {
    return notFound("Campaign not found");
  }
  const id = path.split("/")[2];
  if (state.campaign.id !== id) {
    return notFound("Campaign not found");
  }
  const productIds = Array.isArray(body?.productIds)
    ? (body?.productIds as string[])
    : [HAPPY_PATH_FIXTURES.productId];
  state.campaign = {
    ...state.campaign,
    productIds,
    updatedAt: NOW,
  };
  return ok(state.campaign);
}

function handleCampaignStatus(
  state: HappyPathMockState,
  path: string,
  status: string,
): MockApiResponse {
  if (state.campaign == null) {
    return notFound("Campaign not found");
  }
  const id = path.split("/")[2];
  if (state.campaign.id !== id) {
    return notFound("Campaign not found");
  }
  state.campaign = {
    ...state.campaign,
    status,
    updatedAt: NOW,
  };
  return ok(state.campaign);
}

function handleCampaignApprove(
  state: HappyPathMockState,
  path: string,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const response = handleCampaignStatus(state, path, "APPROVED");
  if (response.status !== 200 || state.campaign == null) {
    return response;
  }
  state.campaign = {
    ...state.campaign,
    approvedByUserId: HAPPY_PATH_ADMIN.userId,
    approvedByFullName: HAPPY_PATH_ADMIN.fullName,
    approvedAt: NOW,
    complianceReviewNotes: stringField(body, "complianceReviewNotes") || null,
  };
  return ok(state.campaign, "Campaign approved");
}

function demoSegment(): SegmentRecord {
  return {
    id: HAPPY_PATH_FIXTURES.segmentId,
    name: HAPPY_PATH_FIXTURES.segmentName,
    description: "Deterministic segment for Playwright happy-path",
    ownerUserId: HAPPY_PATH_ADMIN.userId,
    ownerFullName: HAPPY_PATH_ADMIN.fullName,
    visibility: "TEAM",
    criteria: [],
    createdAt: NOW,
    updatedAt: NOW,
  };
}

function handleSegmentCreate(
  state: HappyPathMockState,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const name = stringField(body, "name") || "E2E Segment";
  const id = `40000000-0000-0000-0000-00000000s${String(state.segments.length + 1).padStart(3, "0")}`;
  const rawCriteria = Array.isArray(body?.criteria) ? body.criteria : [];
  const criteria = rawCriteria.map((row, index) => {
    const criterion = (row ?? {}) as Record<string, unknown>;
    return {
      id: `${id}-c${index + 1}`,
      segmentId: id,
      fieldName: typeof criterion.fieldName === "string" ? criterion.fieldName : "",
      operator: typeof criterion.operator === "string" ? criterion.operator : "EQUALS",
      value: typeof criterion.value === "string" ? criterion.value : "",
      logicalGroup: typeof criterion.logicalGroup === "string" ? criterion.logicalGroup : null,
      joinOperator: typeof criterion.joinOperator === "string" ? criterion.joinOperator : "AND",
    };
  });
  const segment: SegmentRecord = {
    id,
    name,
    description: stringField(body, "description") || null,
    ownerUserId: HAPPY_PATH_ADMIN.userId,
    ownerFullName: HAPPY_PATH_ADMIN.fullName,
    visibility: stringField(body, "visibility") || "PRIVATE",
    criteria,
    createdAt: NOW,
    updatedAt: NOW,
  };
  state.segments = [segment, ...state.segments];
  return ok(segment, "Segment created");
}

function demoProduct(): ProductRecord {
  return {
    id: HAPPY_PATH_FIXTURES.productId,
    name: HAPPY_PATH_FIXTURES.productName,
    productType: "INVESTMENT_FUND",
    description: "Deterministic product for Playwright happy-path",
    price: 99.0,
    durationMonths: 12,
    expirationPolicy: "Annual",
    active: true,
    deleted: false,
    createdAt: NOW,
    updatedAt: NOW,
    deletedAt: null,
  };
}

function handleProductCreate(
  state: HappyPathMockState,
  body: Record<string, unknown> | null,
): MockApiResponse {
  const name = stringField(body, "name") || "E2E Product";
  const product: ProductRecord = {
    id: `30000000-0000-0000-0000-00000000p${String(state.products.length + 1).padStart(3, "0")}`,
    name,
    productType: stringField(body, "productType") || "LIFE_INSURANCE",
    description: stringField(body, "description") || null,
    price: typeof body?.price === "number" ? body.price : null,
    durationMonths: typeof body?.durationMonths === "number" ? body.durationMonths : null,
    expirationPolicy: stringField(body, "expirationPolicy") || null,
    active: true,
    deleted: false,
    createdAt: NOW,
    updatedAt: NOW,
    deletedAt: null,
  };
  state.products = [product, ...state.products];
  return ok(product, "Product created");
}

function demoPreview() {
  return {
    totalAudienceCount: 1,
    eligibleCount: 1,
    excludedCount: 0,
    matchingCustomers: [
      {
        id: HAPPY_PATH_FIXTURES.customerId,
        customerType: "CUSTOMER",
        firstName: HAPPY_PATH_FIXTURES.customerFirstName,
        lastName: HAPPY_PATH_FIXTURES.customerLastName,
        fullName: `${HAPPY_PATH_FIXTURES.customerFirstName} ${HAPPY_PATH_FIXTURES.customerLastName}`,
        email: HAPPY_PATH_FIXTURES.customerEmail,
        city: "Munich",
        country: "Germany",
        status: "ACTIVE",
        doNotContact: false,
      },
    ],
    exclusionReasonSummary: [],
  };
}

function demoEligibleRecipients(state: HappyPathMockState) {
  const customer = state.customers[0];
  return [
    {
      id: "62000000-0000-0000-0000-00000000e601",
      campaignId: state.campaign?.id ?? HAPPY_PATH_FIXTURES.campaignId,
      campaignName: state.campaign?.name ?? HAPPY_PATH_FIXTURES.campaignName,
      customerId: customer?.id ?? HAPPY_PATH_FIXTURES.customerId,
      customerFullName:
        customer?.fullName ??
        `${HAPPY_PATH_FIXTURES.customerFirstName} ${HAPPY_PATH_FIXTURES.customerLastName}`,
      eligibilityStatus: state.campaign?.status === "ACTIVE" ? "SENT" : "ELIGIBLE",
      exclusionReason: null,
      eligibilityExplanation: "Customer has valid marketing consent and is contactable",
      sentAt: state.campaign?.status === "ACTIVE" ? NOW : null,
      openedAt: null,
      clickedAt: null,
      convertedAt: null,
      createdAt: NOW,
    },
  ];
}

function demoDashboard() {
  return {
    campaignTotal: 3,
    activeCampaigns: 1,
    audienceSize: 50,
    messagesSent: 30,
    eligibleCount: 35,
    excludedCount: 15,
    openedCount: 12,
    clickedCount: 5,
    repliedCount: 2,
    convertedCount: 1,
    openRate: 0.4,
    clickRate: 0.17,
    conversionRate: 0.03,
    estimatedCost: 100,
    estimatedRevenue: 250,
    estimatedRoi: 1.5,
    recentCampaignMetrics: [
      {
        metricsId: "55000000-0000-0000-0000-00000000e701",
        campaignId: HAPPY_PATH_FIXTURES.campaignId,
        campaignName: HAPPY_PATH_FIXTURES.campaignName,
        campaignStatus: "ACTIVE",
        audienceSize: 20,
        eligibleCount: 9,
        excludedCount: 11,
        sentCount: 9,
        openedCount: 3,
        clickedCount: 1,
        repliedCount: 0,
        convertedCount: 0,
        openRate: 0.33,
        clickRate: 0.11,
        conversionRate: 0,
        estimatedCost: 12.5,
        estimatedRevenue: 0,
        estimatedRoi: 0,
        updatedAt: NOW,
      },
    ],
  };
}

function stripApiPrefix(pathname: string): string {
  const normalized = pathname.replace(/\/+$/, "") || "/";
  const apiIndex = normalized.indexOf("/api/");
  if (apiIndex >= 0) {
    return normalized.slice(apiIndex + 4);
  }
  if (normalized === "/api") {
    return "/";
  }
  return normalized.startsWith("/") ? normalized : `/${normalized}`;
}

function parseJsonBody(bodyText?: string): Record<string, unknown> | null {
  if (bodyText == null || bodyText.trim() === "") {
    return null;
  }
  try {
    return JSON.parse(bodyText) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function stringField(body: Record<string, unknown> | null, key: string): string {
  const value = body?.[key];
  return typeof value === "string" ? value : "";
}

function ok(data: unknown, message = "OK"): MockApiResponse {
  return {
    status: 200,
    body: {
      success: true,
      message,
      data,
    },
  };
}

function notFound(message: string): MockApiResponse {
  return {
    status: 404,
    body: {
      success: false,
      message,
      data: null,
    },
  };
}

function toBase64Url(value: string): string {
  if (typeof globalThis.btoa === "function") {
    return globalThis
      .btoa(value)
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/g, "");
  }
  // Node (Playwright helper / Vitest without btoa)
  const encoded = typeof Buffer === "undefined" ? value : Buffer.from(value, "utf8").toString("base64");
  return encoded.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}
