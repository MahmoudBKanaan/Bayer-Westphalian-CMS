/**
 * Consent update UI integration (KB item 600 / FR-018 / BR-004).
 *
 * Full route tree: authorized user opens customer details, records consent,
 * sees success notice and list refresh; purpose validation blocks empty submit;
 * opt-out and withdraw paths call the consents API.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CONSENT_EMPTY_STATE,
  CONSENT_OPT_OUT_FORM_ARIA_LABEL,
  CONSENT_OPT_OUT_NOTICE,
  CONSENT_OPT_OUT_SUBMIT_LABEL,
  CONSENT_RECORD_FORM_ARIA_LABEL,
  CONSENT_RECORD_SUBMIT_LABEL,
  CONSENT_RECORDED_NOTICE,
  CONSENT_RECORDS_TABLE_ARIA_LABEL,
  CONSENT_SECTION_HEADING,
  CONSENT_UPDATE_FIXTURES,
  CONSENT_WITHDRAW_SUBMIT_LABEL,
  CONSENT_WITHDRAWN_NOTICE,
  consentFormValidationMessages,
  MARKETING_OPT_OUT_PURPOSE,
} from "@/features/customers/consentUpdateFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const customerId = CONSENT_UPDATE_FIXTURES.customerId;

const customer = {
  id: customerId,
  customerType: "CUSTOMER",
  firstName: "UI",
  lastName: "Consent",
  fullName: "UI Consent",
  email: "ui.consent@bayer-westphalian.test",
  phone: null,
  addressLine: null,
  city: "Munich",
  country: "Germany",
  dateOfBirth: null,
  ageGroup: null,
  status: "ACTIVE",
  doNotContact: false,
  active: true,
  contactable: true,
  source: "UI",
  createdAt: "2026-07-12T12:00:00Z",
  updatedAt: "2026-07-12T12:00:00Z",
  deletedAt: null,
};

const seedConsent = {
  id: "61000000-0000-0000-0000-00000000c601",
  customerId,
  customerFullName: customer.fullName,
  consentType: "DATA_PROCESSING",
  status: "GIVEN",
  purpose: "Seed processing consent",
  source: "SYSTEM",
  grantedAt: "2026-07-12T10:00:00Z",
  withdrawnAt: null,
  expiresAt: null,
  evidenceFileUrl: null,
  createdBy: "10000000-0000-0000-0000-000000009901",
  createdByFullName: "Admin User",
  createdAt: "2026-07-12T10:00:00Z",
  valid: true,
  requiresAction: false,
};

function consentUpdateHandlers(options?: { emptyConsents?: boolean }) {
  let consents: unknown[] = options?.emptyConsents ? [] : [seedConsent];

  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url, method) =>
        url.includes(`/customers/${customerId}`) && method === "GET" && !url.includes("?"),
      response: () => jsonOk(customer),
    },
    {
      match: (url, method) => url.includes("/customers") && method === "GET",
      response: () =>
        jsonOk({
          content: [customer],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
          empty: false,
        }),
    },
    {
      match: (url, method) => url.includes("/consents") && method === "GET",
      response: () => jsonOk(consents),
    },
    {
      match: (url, method) => url.endsWith("/consents/withdraw") && method === "POST",
      response: () => {
        const updated = {
          ...seedConsent,
          status: "WITHDRAWN",
          valid: false,
          withdrawnAt: "2026-07-12T13:00:00Z",
        };
        consents = [updated];
        return jsonOk(updated, "Consent withdrawn");
      },
    },
    {
      match: (url, method) => url.endsWith("/consents") && method === "POST",
      response: () => {
        // Body is not available in this handler factory; tests assert request body via fetch mock.
        // Response mirrors the last successful UI submit fields from fixtures / opt-out defaults.
        const created = {
          id: `${CONSENT_UPDATE_FIXTURES.consentRecordId}-${consents.length}`,
          customerId,
          customerFullName: customer.fullName,
          consentType: CONSENT_UPDATE_FIXTURES.consentType,
          status: CONSENT_UPDATE_FIXTURES.status,
          purpose: CONSENT_UPDATE_FIXTURES.purpose,
          source: CONSENT_UPDATE_FIXTURES.source,
          grantedAt: "2026-07-12T13:00:00Z",
          withdrawnAt: null,
          expiresAt: null,
          evidenceFileUrl: CONSENT_UPDATE_FIXTURES.evidenceFileUrl,
          createdBy: "10000000-0000-0000-0000-000000009901",
          createdByFullName: "Admin User",
          createdAt: "2026-07-12T13:00:00Z",
          valid: true,
          requiresAction: false,
        };
        consents = [created, ...consents];
        return jsonOk(created, "Consent recorded");
      },
    },
    {
      match: (url) => url.includes("/beneficiaries"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/product-ownerships"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/products"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/payment-records"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/contact-events"),
      response: () => jsonOk([]),
    },
    {
      match: (url) => url.includes("/follow-up-tasks"),
      response: () => jsonOk([]),
    },
  ]);
}

describe("consent update UI integration (item 600)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows the Consent panel on customer details for authorized roles", async () => {
    vi.stubGlobal("fetch", consentUpdateHandlers());
    renderApp({ path: `/customers/${customerId}`, roles: ["ADMIN"] });

    expect(await screen.findByRole("heading", { name: CONSENT_SECTION_HEADING })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: CONSENT_OPT_OUT_FORM_ARIA_LABEL })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: CONSENT_RECORDS_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
  });

  it("shows empty consent state when no records exist", async () => {
    vi.stubGlobal("fetch", consentUpdateHandlers({ emptyConsents: true }));
    renderApp({ path: `/customers/${customerId}`, roles: ["CUSTOMER_SERVICE_AGENT"] });

    expect(await screen.findByText(CONSENT_EMPTY_STATE)).toBeInTheDocument();
  });

  it("records consent through the UI and shows success notice", async () => {
    const user = userEvent.setup();
    const fetchMock = consentUpdateHandlers({ emptyConsents: true });
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: `/customers/${customerId}`, roles: ["ADMIN"] });
    await screen.findByText(CONSENT_EMPTY_STATE);

    const form = screen.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL });
    await user.type(within(form).getByLabelText("Purpose"), CONSENT_UPDATE_FIXTURES.purpose);
    await user.type(within(form).getByLabelText("Source"), CONSENT_UPDATE_FIXTURES.source);
    await user.type(
      within(form).getByLabelText("Evidence URL"),
      CONSENT_UPDATE_FIXTURES.evidenceFileUrl,
    );
    await user.click(within(form).getByRole("button", { name: CONSENT_RECORD_SUBMIT_LABEL }));

    expect(await screen.findByTestId("consent-update-notice")).toHaveTextContent(
      CONSENT_RECORDED_NOTICE,
    );
    expect(
      await screen.findByText(CONSENT_UPDATE_FIXTURES.purpose),
    ).toBeInTheDocument();

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).endsWith("/consents") && (init as RequestInit | undefined)?.method === "POST"
        );
      });
      expect(createCall).toBeDefined();
      expect(JSON.parse(String((createCall?.[1] as RequestInit).body))).toMatchObject({
        customerId,
        purpose: CONSENT_UPDATE_FIXTURES.purpose,
        source: CONSENT_UPDATE_FIXTURES.source,
        evidenceFileUrl: CONSENT_UPDATE_FIXTURES.evidenceFileUrl,
        status: "GIVEN",
      });
    });
  });

  it("blocks record consent without purpose", async () => {
    const user = userEvent.setup();
    const fetchMock = consentUpdateHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: `/customers/${customerId}`, roles: ["ADMIN"] });
    await screen.findByRole("heading", { name: CONSENT_SECTION_HEADING });

    const form = screen.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL });
    await user.click(within(form).getByRole("button", { name: CONSENT_RECORD_SUBMIT_LABEL }));

    expect(screen.getByTestId("consent-form-error")).toHaveTextContent(
      consentFormValidationMessages.purposeRequired,
    );
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url).endsWith("/consents") && (init as RequestInit | undefined)?.method === "POST",
      ),
    ).toBe(false);
  });

  it("withdraws consent through the UI", async () => {
    const user = userEvent.setup();
    const fetchMock = consentUpdateHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: `/customers/${customerId}`, roles: ["COMPLIANCE_OFFICER"] });
    await screen.findByRole("table", { name: CONSENT_RECORDS_TABLE_ARIA_LABEL });
    await user.click(screen.getByRole("button", { name: CONSENT_WITHDRAW_SUBMIT_LABEL }));

    expect(await screen.findByTestId("consent-update-notice")).toHaveTextContent(
      CONSENT_WITHDRAWN_NOTICE,
    );
    await waitFor(() => {
      const withdrawCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url).includes("/consents/withdraw") &&
          (init as RequestInit | undefined)?.method === "POST",
      );
      expect(withdrawCall).toBeDefined();
    });
  });

  it("marks marketing opt-out through the UI", async () => {
    const user = userEvent.setup();
    const fetchMock = consentUpdateHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: `/customers/${customerId}`, roles: ["CUSTOMER_SERVICE_AGENT"] });
    await screen.findByRole("heading", { name: CONSENT_SECTION_HEADING });

    const form = screen.getByRole("form", { name: CONSENT_OPT_OUT_FORM_ARIA_LABEL });
    await user.selectOptions(within(form).getByLabelText("Opt-out channel"), "MARKETING_SMS");
    await user.type(within(form).getByLabelText("Opt-out source"), "PHONE");
    await user.click(within(form).getByRole("button", { name: CONSENT_OPT_OUT_SUBMIT_LABEL }));

    expect(await screen.findByTestId("consent-update-notice")).toHaveTextContent(
      CONSENT_OPT_OUT_NOTICE,
    );
    await waitFor(() => {
      const optOutCall = fetchMock.mock.calls.find(([url, init]) => {
        if (
          !String(url).endsWith("/consents") ||
          (init as RequestInit | undefined)?.method !== "POST"
        ) {
          return false;
        }
        const body = JSON.parse(String((init as RequestInit).body)) as { purpose?: string };
        return body.purpose === MARKETING_OPT_OUT_PURPOSE;
      });
      expect(optOutCall).toBeDefined();
    });
  });
});
