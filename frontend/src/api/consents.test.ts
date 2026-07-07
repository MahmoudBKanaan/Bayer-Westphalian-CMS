import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { listConsents, recordConsent, recordOptOut, withdrawConsent } from "@/api/consents";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const consent = {
  id: "22000000-0000-0000-0000-000000000001",
  customerId: "20000000-0000-0000-0000-000000000001",
  customerFullName: "Ada Policyholder",
  consentType: "MARKETING_EMAIL",
  status: "GIVEN",
  purpose: "Marketing email consent",
  source: "WEB_FORM",
  grantedAt: "2026-07-01T12:00:00Z",
  withdrawnAt: null,
  expiresAt: "2027-07-01T12:00:00Z",
  evidenceFileUrl: "s3://evidence/email.pdf",
  createdBy: "10000000-0000-0000-0000-000000000101",
  createdByFullName: "Customer Service Agent",
  createdAt: "2026-07-01T12:00:00Z",
  valid: true,
  requiresAction: false,
};

describe("consents api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads customer consent history with KB consent filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Consents loaded",
        data: [consent],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listConsents({
        customerId: consent.customerId,
        consentType: "MARKETING_EMAIL",
        status: "GIVEN",
        validOnly: true,
      }),
    ).resolves.toEqual([consent]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/consents?customerId=${consent.customerId}` +
        "&consentType=MARKETING_EMAIL&status=GIVEN&validOnly=true",
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("records and withdraws consent through the KB consent endpoints", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Consent changed",
        data: consent,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await recordConsent({
      customerId: consent.customerId,
      consentType: "MARKETING_EMAIL",
      status: "GIVEN",
      purpose: " Marketing email consent ",
      source: "WEB_FORM",
      evidenceFileUrl: "s3://evidence/email.pdf",
    });
    await withdrawConsent(consent.id);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/consents`, {
      body: JSON.stringify({
        customerId: consent.customerId,
        consentType: "MARKETING_EMAIL",
        status: "GIVEN",
        purpose: " Marketing email consent ",
        source: "WEB_FORM",
        evidenceFileUrl: "s3://evidence/email.pdf",
      }),
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      method: "POST",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/consents/withdraw`, {
      body: JSON.stringify({ consentRecordId: consent.id }),
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      method: "POST",
    });
  });

  it("records marketing opt-out as withdrawn marketing consent", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Consent recorded",
        data: { ...consent, status: "WITHDRAWN", valid: false, requiresAction: true },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await recordOptOut({
      customerId: consent.customerId,
      consentType: "MARKETING_SMS",
      source: "PHONE",
      evidenceFileUrl: "s3://evidence/sms-opt-out.pdf",
    });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/consents`, {
      body: JSON.stringify({
        customerId: consent.customerId,
        consentType: "MARKETING_SMS",
        status: "WITHDRAWN",
        purpose: "Marketing opt-out",
        source: "PHONE",
        evidenceFileUrl: "s3://evidence/sms-opt-out.pdf",
      }),
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      method: "POST",
    });
  });
});
