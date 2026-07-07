import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { listBeneficiaries, updateBeneficiary } from "@/api/beneficiaries";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const beneficiary = {
  id: "30000000-0000-0000-0000-000000000001",
  policyholderCustomerId: "20000000-0000-0000-0000-000000000001",
  policyholderFullName: "Ada Policyholder",
  beneficiaryCustomerId: "20000000-0000-0000-0000-000000000002",
  beneficiaryFullName: "Ben Grandchild",
  relationship: "Grandchild",
  guardianName: "Grace Guardian",
  guardianEmail: "grace@bayer-westphalian.test",
  guardianConsentRequired: true,
  hasGuardianRequirement: true,
  createdAt: "2026-07-03T12:00:00Z",
};

describe("beneficiaries api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads beneficiaries for a policyholder customer", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Beneficiaries loaded",
        data: [beneficiary],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listBeneficiaries({
        policyholderCustomerId: beneficiary.policyholderCustomerId,
        guardianConsentRequired: true,
      }),
    ).resolves.toEqual([beneficiary]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/beneficiaries?policyholderCustomerId=${beneficiary.policyholderCustomerId}` +
        "&guardianConsentRequired=true",
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("updates guardian consent details for a beneficiary", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Beneficiary updated",
        data: { ...beneficiary, guardianConsentRequired: false },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updateBeneficiary(beneficiary.id, {
        relationship: "Grandchild",
        guardianName: "Grace Guardian",
        guardianEmail: "grace@bayer-westphalian.test",
        guardianConsentRequired: false,
      }),
    ).resolves.toMatchObject({
      id: beneficiary.id,
      guardianConsentRequired: false,
    });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/beneficiaries/${beneficiary.id}`, {
      body: JSON.stringify({
        relationship: "Grandchild",
        guardianName: "Grace Guardian",
        guardianEmail: "grace@bayer-westphalian.test",
        guardianConsentRequired: false,
      }),
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      method: "PUT",
    });
  });
});
