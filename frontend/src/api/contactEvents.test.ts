import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  contactTimelineQuery,
  formatContactEnum,
  listContactTimeline,
  recordContactEvent,
} from "@/api/contactEvents";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

const contactEvent = {
  id: "63000000-0000-0000-0000-000000000337",
  customerId: "20000000-0000-0000-0000-000000000337",
  customerFullName: "Ada Contact",
  campaignId: "50000000-0000-0000-0000-000000000337",
  campaignName: "Renewal outreach",
  channel: "EMAIL",
  eventType: "SENT",
  outcome: null,
  notes: "providerMessageId=provider-337",
  occurredAt: "2026-07-10T15:00:00Z",
  createdByUserId: "10000000-0000-0000-0000-000000000337",
  createdByFullName: "Contact Event User",
};

describe("contactEvents api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads the contact timeline with KB filters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Contact timeline loaded",
        data: [contactEvent],
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      listContactTimeline({
        customerId: contactEvent.customerId,
        campaignId: contactEvent.campaignId,
        eventType: "SENT",
      }),
    ).resolves.toEqual([contactEvent]);

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/contact-events/timeline?customerId=${contactEvent.customerId}&campaignId=${contactEvent.campaignId}&eventType=SENT`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
  });

  it("omits blank timeline filters and ALL event type", () => {
    expect(
      contactTimelineQuery({
        customerId: " ",
        campaignId: "50000000-0000-0000-0000-000000000337",
        eventType: "ALL",
      }),
    ).toBe("?campaignId=50000000-0000-0000-0000-000000000337");
  });

  it("records a manual contact outcome", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Contact event recorded",
        data: contactEvent,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      recordContactEvent({
        customerId: contactEvent.customerId,
        channel: "PHONE",
        eventType: "CALLED",
        outcome: "INTERESTED",
        notes: "Customer asked for a quote",
        occurredAt: "2026-07-10T15:05:00.000Z",
      }),
    ).resolves.toEqual(contactEvent);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/contact-events`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      body: JSON.stringify({
        customerId: contactEvent.customerId,
        channel: "PHONE",
        eventType: "CALLED",
        outcome: "INTERESTED",
        notes: "Customer asked for a quote",
        occurredAt: "2026-07-10T15:05:00.000Z",
      }),
    });
  });

  it("formats contact enum display text", () => {
    expect(formatContactEnum("NOT_INTERESTED")).toBe("Not Interested");
  });
});
