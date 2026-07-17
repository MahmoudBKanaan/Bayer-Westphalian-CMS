import { describe, expect, it } from "vitest";
import {
  createHappyPathMockState,
  E2E_API_ROUTE_PATTERN,
  handleHappyPathApiRequest,
} from "@/features/e2e/happyPathApiMock";
import { HAPPY_PATH_ADMIN, HAPPY_PATH_FIXTURES } from "@/features/e2e/happyPathFlow";

const API = "http://localhost:8080/api";

describe("happyPathApiMock (item 597)", () => {
  it("intercepts backend API requests without replacing Vite source modules", () => {
    expect(E2E_API_ROUTE_PATTERN.test(`${API}/auth/login`)).toBe(true);
    expect(E2E_API_ROUTE_PATTERN.test("http://127.0.0.1:5173/src/api/client.ts")).toBe(false);
  });

  it("serves a11y main-screen read endpoints used by item 609", () => {
    const state = createHappyPathMockState();
    expect(
      handleHappyPathApiRequest(state, {
        method: "GET",
        url: `${API}/users`,
      }).status,
    ).toBe(200);
    expect(
      handleHappyPathApiRequest(state, {
        method: "GET",
        url: `${API}/audit-logs`,
      }).status,
    ).toBe(200);
    expect(
      handleHappyPathApiRequest(state, {
        method: "GET",
        url: `${API}/analytics/products/performance`,
      }).status,
    ).toBe(200);
    expect(
      handleHappyPathApiRequest(state, {
        method: "GET",
        url: `${API}/reports/exports`,
      }).status,
    ).toBe(200);
  });

  it("authenticates the happy-path admin and rejects bad passwords", () => {
    const state = createHappyPathMockState();
    const okLogin = handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/auth/login`,
      bodyText: JSON.stringify({
        email: HAPPY_PATH_ADMIN.email,
        password: HAPPY_PATH_ADMIN.password,
      }),
    });
    expect(okLogin.status).toBe(200);
    const session = (okLogin.body as { data: { tokens: { accessToken: string } } }).data;
    expect(session.tokens.accessToken).toContain(".");

    const badLogin = handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/auth/login`,
      bodyText: JSON.stringify({
        email: HAPPY_PATH_ADMIN.email,
        password: "wrong-password",
      }),
    });
    expect(badLogin.status).toBe(401);
  });

  it("walks customer → consent → campaign → approve → launch against mock state", () => {
    const state = createHappyPathMockState();

    const createdCustomer = handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/customers`,
      bodyText: JSON.stringify({
        customerType: "CUSTOMER",
        firstName: HAPPY_PATH_FIXTURES.customerFirstName,
        lastName: HAPPY_PATH_FIXTURES.customerLastName,
        email: HAPPY_PATH_FIXTURES.customerEmail,
        phone: "",
        addressLine: "",
        city: "Munich",
        country: "Germany",
        dateOfBirth: "",
        ageGroup: null,
        status: "ACTIVE",
        doNotContact: false,
        source: "E2E",
      }),
    });
    expect(createdCustomer.status).toBe(200);
    expect(state.customers).toHaveLength(1);

    const consent = handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/consents`,
      bodyText: JSON.stringify({
        customerId: HAPPY_PATH_FIXTURES.customerId,
        consentType: "MARKETING_EMAIL",
        status: "GIVEN",
        purpose: HAPPY_PATH_FIXTURES.consentPurpose,
        source: HAPPY_PATH_FIXTURES.consentSource,
        evidenceFileUrl: null,
      }),
    });
    expect(consent.status).toBe(200);
    expect(state.consents[0]?.valid).toBe(true);

    const draft = handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/campaigns`,
      bodyText: JSON.stringify({
        name: HAPPY_PATH_FIXTURES.campaignName,
        objective: HAPPY_PATH_FIXTURES.campaignObjective,
        segmentId: HAPPY_PATH_FIXTURES.segmentId,
        channel: "EMAIL",
        messageSubject: HAPPY_PATH_FIXTURES.campaignSubject,
        messageBody: HAPPY_PATH_FIXTURES.campaignBody,
        startDate: "2026-08-01",
        endDate: "2026-08-31",
      }),
    });
    expect(draft.status).toBe(200);
    expect(state.campaign?.status).toBe("DRAFT");

    handleHappyPathApiRequest(state, {
      method: "PUT",
      url: `${API}/campaigns/${HAPPY_PATH_FIXTURES.campaignId}/products`,
      bodyText: JSON.stringify({ productIds: [HAPPY_PATH_FIXTURES.productId] }),
    });
    handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/campaigns/${HAPPY_PATH_FIXTURES.campaignId}/submit`,
    });
    expect(state.campaign?.status).toBe("SUBMITTED");

    const submittedList = handleHappyPathApiRequest(state, {
      method: "GET",
      url: `${API}/campaigns?status=SUBMITTED`,
    });
    expect(
      (submittedList.body as { data: Array<{ status: string }> }).data[0]?.status,
    ).toBe("SUBMITTED");

    handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/campaigns/${HAPPY_PATH_FIXTURES.campaignId}/approve`,
      bodyText: JSON.stringify({ complianceReviewNotes: "E2E approved" }),
    });
    expect(state.campaign?.status).toBe("APPROVED");

    handleHappyPathApiRequest(state, {
      method: "POST",
      url: `${API}/campaigns/${HAPPY_PATH_FIXTURES.campaignId}/launch`,
    });
    expect(state.campaign?.status).toBe("ACTIVE");
  });

  it("returns health and catalog fixtures for shell and builder loads", () => {
    const state = createHappyPathMockState();
    const health = handleHappyPathApiRequest(state, {
      method: "GET",
      url: `${API}/health`,
    });
    expect(health.status).toBe(200);
    expect(health.body).toEqual(
      expect.objectContaining({ status: "UP" }),
    );
    const segments = handleHappyPathApiRequest(state, {
      method: "GET",
      url: `${API}/segments?term=&visibility=ALL`,
    });
    expect((segments.body as { data: unknown[] }).data).toHaveLength(1);
    const products = handleHappyPathApiRequest(state, {
      method: "GET",
      url: `${API}/products?term=&productType=ALL&active=true`,
    });
    expect((products.body as { data: unknown[] }).data).toHaveLength(1);
  });
});
