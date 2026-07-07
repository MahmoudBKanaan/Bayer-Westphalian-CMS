import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { CustomerDetailsPage } from "@/pages/CustomerDetailsPage";

const customer = {
  id: "20000000-0000-0000-0000-000000000001",
  customerType: "CUSTOMER",
  firstName: "Ada",
  lastName: "Policyholder",
  fullName: "Ada Policyholder",
  email: "ada@bayer-westphalian.test",
  phone: "+49-555-0100",
  addressLine: "Insurance Street 1",
  city: "Berlin",
  country: "Germany",
  dateOfBirth: "1984-08-21",
  ageGroup: "AGE_41_60",
  status: "ACTIVE",
  doNotContact: false,
  active: true,
  contactable: true,
  source: "LIFE_INSURANCE_BENEFICIARY",
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

const beneficiary = {
  id: "30000000-0000-0000-0000-000000000001",
  policyholderCustomerId: customer.id,
  policyholderFullName: customer.fullName,
  beneficiaryCustomerId: "20000000-0000-0000-0000-000000000002",
  beneficiaryFullName: "Ben Grandchild",
  relationship: "Grandchild",
  guardianName: "Grace Guardian",
  guardianEmail: "grace@bayer-westphalian.test",
  guardianConsentRequired: true,
  hasGuardianRequirement: true,
  createdAt: "2026-07-03T12:00:00Z",
};

const consent = {
  id: "22000000-0000-0000-0000-000000000001",
  customerId: customer.id,
  customerFullName: customer.fullName,
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

const doNotContactCustomer = {
  ...customer,
  doNotContact: true,
  contactable: false,
};

function renderDetailsPage() {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/customers/${customer.id}`]}>
        <Routes>
          <Route path="/customers/:customerId" element={<CustomerDetailsPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("CustomerDetailsPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads and renders the customer profile sections required by the KB", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(screen.getAllByText("Ada Policyholder")).not.toHaveLength(0);
    expect(screen.getByRole("heading", { name: "Profile" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Activity" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Consent" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Products" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Beneficiaries" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contact history" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Follow-up tasks" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Notes" })).toBeInTheDocument();
    expect(screen.getByText("ada@bayer-westphalian.test")).toBeInTheDocument();
    expect(screen.getByText("Insurance Street 1")).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/beneficiaries?policyholderCustomerId=${customer.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer access-token",
        },
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/consents?customerId=${customer.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("renders the KB consent tab with status, validity, evidence, and recorder fields", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], [consent]));

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: "Consent records table" });
    expect(screen.getByText("1 consent record")).toBeInTheDocument();
    expect(within(table).getByText("Marketing Email")).toBeInTheDocument();
    expect(within(table).getByText("Marketing email consent")).toBeInTheDocument();
    expect(within(table).getByText("Given")).toBeInTheDocument();
    expect(within(table).getByText("WEB_FORM")).toBeInTheDocument();
    expect(within(table).getByText("Valid")).toBeInTheDocument();
    expect(within(table).getByText("No action required")).toBeInTheDocument();
    expect(within(table).getByText("s3://evidence/email.pdf")).toBeInTheDocument();
    expect(within(table).getByText("Customer Service Agent")).toBeInTheDocument();
    expect(table).toBeInTheDocument();
  });

  it("displays the current consent status for each consent record correctly", async () => {
    const withdrawnConsent = {
      ...consent,
      id: "22000000-0000-0000-0000-000000000002",
      consentType: "MARKETING_SMS",
      status: "WITHDRAWN",
      purpose: "Withdrawn SMS marketing consent",
      source: "PHONE",
      grantedAt: "2026-06-01T12:00:00Z",
      withdrawnAt: "2026-07-02T12:00:00Z",
      expiresAt: null,
      evidenceFileUrl: "s3://evidence/sms-withdrawal.pdf",
      valid: false,
      requiresAction: true,
    };
    const requiredGuardianConsent = {
      ...consent,
      id: "22000000-0000-0000-0000-000000000003",
      consentType: "GUARDIAN",
      status: "REQUIRED",
      purpose: "Guardian consent pending",
      source: "CUSTOMER_SERVICE",
      grantedAt: null,
      withdrawnAt: null,
      expiresAt: null,
      evidenceFileUrl: null,
      valid: false,
      requiresAction: true,
      createdByFullName: "Compliance Officer",
    };
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock([beneficiary], [
        consent,
        withdrawnConsent,
        requiredGuardianConsent,
      ]),
    );

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: "Consent records table" });
    expect(screen.getByText("3 consent records")).toBeInTheDocument();
    expect(within(table).getByText("Marketing Email")).toBeInTheDocument();
    expect(within(table).getByText("Given")).toBeInTheDocument();
    expect(within(table).getByText("Marketing Sms")).toBeInTheDocument();
    expect(within(table).getByText("Withdrawn")).toBeInTheDocument();
    expect(within(table).getByText("Guardian")).toBeInTheDocument();
    expect(within(table).getByText("Required")).toBeInTheDocument();
    expect(within(table).getAllByText("Invalid")).toHaveLength(2);
    expect(within(table).getAllByText("Action required")).toHaveLength(2);
    expect(within(table).getByText("s3://evidence/sms-withdrawal.pdf")).toBeInTheDocument();
    expect(within(table).getByText("Compliance Officer")).toBeInTheDocument();
  });

  it("shows an empty consent tab state when no consent records exist", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], []));

    renderDetailsPage();

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(screen.getByText("No consent records are available for this customer.")).toBeInTheDocument();
  });

  it("records a new consent from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: "Consent" });
    fireEvent.change(screen.getByLabelText("Consent type"), { target: { value: "GUARDIAN" } });
    fireEvent.change(screen.getByLabelText("Consent status"), { target: { value: "REQUIRED" } });
    fireEvent.change(screen.getByLabelText("Purpose"), {
      target: { value: "Guardian consent required" },
    });
    fireEvent.change(screen.getByLabelText("Source"), { target: { value: "PHONE" } });
    fireEvent.change(screen.getByLabelText("Evidence URL"), {
      target: { value: "s3://evidence/guardian.pdf" },
    });
    await userEvent.click(screen.getByRole("button", { name: "Record consent" }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/consents` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toMatchObject({
        customerId: customer.id,
        consentType: "GUARDIAN",
        status: "REQUIRED",
        purpose: "Guardian consent required",
        source: "PHONE",
        evidenceFileUrl: "s3://evidence/guardian.pdf",
      });
    });
  });

  it("withdraws consent from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("table", { name: "Consent records table" });
    await userEvent.click(screen.getByRole("button", { name: "Withdraw" }));

    await waitFor(() => {
      const withdrawCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/consents/withdraw` && init?.method === "POST",
      );

      expect(withdrawCall).toBeDefined();
      expect(JSON.parse(withdrawCall?.[1]?.body as string)).toEqual({
        consentRecordId: consent.id,
      });
    });
  });

  it("marks a marketing opt-out from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: "Consent" });
    await userEvent.selectOptions(screen.getByLabelText("Opt-out channel"), "MARKETING_SMS");
    await userEvent.type(screen.getByLabelText("Opt-out source"), "PHONE");
    await userEvent.type(
      screen.getByLabelText("Opt-out evidence URL"),
      "s3://evidence/sms-opt-out.pdf",
    );
    await userEvent.click(screen.getByRole("button", { name: "Mark opt-out" }));

    await waitFor(() => {
      const optOutCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/consents` && init?.method === "POST",
      );

      expect(optOutCall).toBeDefined();
      expect(JSON.parse(optOutCall?.[1]?.body as string)).toMatchObject({
        customerId: customer.id,
        consentType: "MARKETING_SMS",
        status: "WITHDRAWN",
        purpose: "Marketing opt-out",
        source: "PHONE",
        evidenceFileUrl: "s3://evidence/sms-opt-out.pdf",
      });
    });
  });

  it("marks a customer do-not-contact from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: "Consent" });
    await userEvent.click(screen.getByRole("button", { name: "Mark do not contact" }));

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/customers/${customer.id}` && init?.method === "PUT",
      );

      expect(updateCall).toBeDefined();
      expect(JSON.parse(updateCall?.[1]?.body as string)).toMatchObject({
        firstName: "Ada",
        lastName: "Policyholder",
        email: "ada@bayer-westphalian.test",
        phone: "+49-555-0100",
        addressLine: "Insurance Street 1",
        city: "Berlin",
        country: "Germany",
        dateOfBirth: "1984-08-21",
        ageGroup: "AGE_41_60",
        status: "ACTIVE",
        doNotContact: true,
        source: "LIFE_INSURANCE_BENEFICIARY",
      });
    });
  });

  it("clears a customer do-not-contact override from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent], doNotContactCustomer);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: "Consent" });
    await userEvent.click(screen.getByRole("button", { name: "Allow contact" }));

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/customers/${customer.id}` && init?.method === "PUT",
      );

      expect(updateCall).toBeDefined();
      expect(JSON.parse(updateCall?.[1]?.body as string)).toMatchObject({
        status: "ACTIVE",
        doNotContact: false,
      });
    });
  });

  it("validates consent purpose before recording consent", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: "Consent" });
    await userEvent.click(screen.getByRole("button", { name: "Record consent" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Purpose is required.");
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) => url === `${API_BASE_URL}/consents` && init?.method === "POST",
      ),
    ).toBe(false);
  });

  it("renders the KB beneficiary tab with relationship and guardian consent fields", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], [consent]));

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: "Beneficiaries table" });
    expect(screen.getByText("1 linked records")).toBeInTheDocument();
    expect(within(table).getByText("Ben Grandchild")).toBeInTheDocument();
    expect(within(table).getByText("Grandchild")).toBeInTheDocument();
    expect(within(table).getByText("Guardian consent required")).toBeInTheDocument();
    expect(within(table).getByText("Grace Guardian / grace@bayer-westphalian.test")).toBeInTheDocument();
    expect(table).toBeInTheDocument();
  });

  it("updates guardian consent from the customer details beneficiaries tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("table", { name: "Beneficiaries table" });
    await userEvent.selectOptions(
      screen.getByLabelText("Guardian consent beneficiary"),
      beneficiary.id,
    );
    await userEvent.clear(screen.getByLabelText("Guardian name"));
    await userEvent.type(screen.getByLabelText("Guardian name"), "Updated Guardian");
    await userEvent.clear(screen.getByLabelText("Guardian email"));
    await userEvent.type(
      screen.getByLabelText("Guardian email"),
      "updated.guardian@bayer-westphalian.test",
    );
    await userEvent.selectOptions(
      screen.getByLabelText("Guardian consent requirement"),
      "not-required",
    );
    await userEvent.click(screen.getByRole("button", { name: "Save guardian consent" }));

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/beneficiaries/${beneficiary.id}` && init?.method === "PUT",
      );

      expect(updateCall).toBeDefined();
      expect(JSON.parse(updateCall?.[1]?.body as string)).toEqual({
        relationship: "Grandchild",
        guardianName: "Updated Guardian",
        guardianEmail: "updated.guardian@bayer-westphalian.test",
        guardianConsentRequired: false,
      });
    });
  });

  it("shows an empty beneficiary tab state when no links exist", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([], [consent]));

    renderDetailsPage();

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(screen.getByText("No beneficiaries are linked to this customer.")).toBeInTheDocument();
  });

  it("shows a recoverable error state when the profile cannot be loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: async () => ({
          code: "RESOURCE_NOT_FOUND",
          message: "Customer was not found",
        }),
      }),
    );

    renderDetailsPage();

    expect(
      await screen.findByRole("heading", { name: "Customer profile unavailable" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Customer profile could not be loaded.");
    expect(screen.getByRole("link", { name: "Back to customers" })).toHaveAttribute(
      "href",
      "/customers",
    );
  });
});

function createProfileFetchMock(
  beneficiaries: (typeof beneficiary)[],
  consents: (typeof consent)[],
  profile = customer,
) {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url === `${API_BASE_URL}/consents` && init?.method === "POST") {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Consent recorded",
          data: consent,
        }),
      });
    }
    if (url === `${API_BASE_URL}/consents/withdraw` && init?.method === "POST") {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Consent withdrawn",
          data: { ...consent, status: "WITHDRAWN", valid: false, requiresAction: true },
        }),
      });
    }
    if (url === `${API_BASE_URL}/customers/${customer.id}` && init?.method === "PUT") {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Customer updated",
          data: {
            ...profile,
            ...payload,
            doNotContact: payload.doNotContact,
            contactable: !payload.doNotContact,
          },
        }),
      });
    }
    if (url === `${API_BASE_URL}/beneficiaries/${beneficiary.id}` && init?.method === "PUT") {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Beneficiary updated",
          data: {
            ...beneficiary,
            ...payload,
          },
        }),
      });
    }
    if (url.includes("/beneficiaries")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Beneficiaries loaded",
          data: beneficiaries,
        }),
      });
    }
    if (url.includes("/consents")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Consents loaded",
          data: consents,
        }),
      });
    }

    return Promise.resolve({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customer loaded",
        data: profile,
      }),
    });
  });
}
