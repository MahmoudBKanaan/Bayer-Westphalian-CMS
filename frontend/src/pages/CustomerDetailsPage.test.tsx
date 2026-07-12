import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import type { ConsentRecordView } from "@/api/consents";
import {
  CONSENT_EMPTY_STATE,
  CONSENT_OPT_OUT_NOTICE,
  CONSENT_RECORD_FORM_ARIA_LABEL,
  CONSENT_RECORDED_NOTICE,
  CONSENT_RECORDS_TABLE_ARIA_LABEL,
  CONSENT_SECTION_HEADING,
  CONSENT_WITHDRAWN_NOTICE,
  consentFormValidationMessages,
} from "@/features/customers/consentUpdateFlow";
import type { ContactEventView } from "@/api/contactEvents";
import type { FollowUpTaskView } from "@/api/followUpTasks";
import type { PaymentRecordView } from "@/api/paymentRecords";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
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

const productOwnership = {
  id: "41000000-0000-0000-0000-000000000001",
  customerId: customer.id,
  customerFullName: customer.fullName,
  productId: "41000000-0000-0000-0000-000000000201",
  productName: "Life Protection",
  productType: "LIFE_INSURANCE",
  policyNumber: "POL-1000",
  startDate: "2026-01-15",
  expirationDate: "2027-01-15",
  status: "ACTIVE",
  active: true,
  createdAt: "2026-07-03T12:00:00Z",
};

const catalogProduct = {
  id: "41000000-0000-0000-0000-000000000201",
  name: "Life Protection",
  productType: "LIFE_INSURANCE",
  description: "Comprehensive life insurance coverage",
  price: 129.99,
  durationMonths: 12,
  expirationPolicy: "Annual renewal",
  active: true,
  deleted: false,
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

const paymentRecord: PaymentRecordView = {
  id: "43000000-0000-0000-0000-000000000001",
  customerId: customer.id,
  customerFullName: customer.fullName,
  productOwnershipId: productOwnership.id,
  productId: catalogProduct.id,
  productName: "Life Protection",
  productType: "LIFE_INSURANCE",
  dueDate: "2026-07-15",
  paidAt: null,
  amountDue: 129.99,
  amountPaid: null,
  status: "DUE",
  reminderCount: 0,
  daysOverdue: 0,
  defaultRisk: false,
};

const overduePaymentRecord: PaymentRecordView = {
  ...paymentRecord,
  id: "43000000-0000-0000-0000-000000000002",
  status: "OVERDUE",
  reminderCount: 2,
  daysOverdue: 14,
};

const expiredProductOwnership = {
  id: "41000000-0000-0000-0000-000000000002",
  customerId: customer.id,
  customerFullName: customer.fullName,
  productId: "41000000-0000-0000-0000-000000000202",
  productName: "Home Protection",
  productType: "HOMEOWNER_INSURANCE",
  policyNumber: "POL-2000",
  startDate: "2025-02-01",
  expirationDate: "2026-02-01",
  status: "EXPIRED",
  active: false,
  createdAt: "2026-07-04T12:00:00Z",
};

const consent: ConsentRecordView = {
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

const contactEvent: ContactEventView = {
  id: "60000000-0000-0000-0000-000000000001",
  customerId: customer.id,
  customerFullName: customer.fullName,
  campaignId: "50000000-0000-0000-0000-000000000001",
  campaignName: "Life renewal outreach",
  channel: "EMAIL",
  eventType: "SENT",
  outcome: null,
  notes: "Automated email sent",
  occurredAt: "2026-07-10T10:00:00Z",
  createdByUserId: "10000000-0000-0000-0000-000000000101",
  createdByFullName: "Campaign Manager",
};

const followUpTask: FollowUpTaskView = {
  id: "70000000-0000-0000-0000-000000000369",
  customerId: customer.id,
  customerFullName: customer.fullName,
  campaignId: "50000000-0000-0000-0000-000000000369",
  campaignName: "Life renewal outreach",
  assignedToUserId: "10000000-0000-0000-0000-000000000369",
  assignedToFullName: "Sales Agent",
  title: "Call Ada about renewal",
  description: "Discuss renewal options and payment concerns",
  dueDate: "2026-09-15",
  status: "OPEN",
  priority: "HIGH",
  completedAt: null,
  createdAt: "2026-07-10T12:00:00Z",
  updatedAt: "2026-07-10T12:00:00Z",
};

const doNotContactCustomer = {
  ...customer,
  doNotContact: true,
  contactable: false,
};

function renderDetailsPage(roles: SystemRoleName[] = ["CAMPAIGN_MANAGER"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000009901",
      email: "viewer@bayer-westphalian.test",
      fullName: "Viewer User",
      status: "ACTIVE",
      lastLoginAt: "2026-07-04T12:00:00Z",
    }),
  );
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/customers/${customer.id}`]}>
        <AuthProvider>
          <Routes>
            <Route path="/customers/:customerId" element={<CustomerDetailsPage />} />
          </Routes>
        </AuthProvider>
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
    expect(screen.getByRole("heading", { name: "Payments" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Beneficiaries" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contact history" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Follow-up tasks" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Notes" })).toBeInTheDocument();
    expect(screen.getByText("ada@bayer-westphalian.test")).toBeInTheDocument();
    expect(screen.getByText("Insurance Street 1")).toBeInTheDocument();

    const accessToken = createAccessToken(["CAMPAIGN_MANAGER"]);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
    });
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/beneficiaries?policyholderCustomerId=${customer.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/consents?customerId=${customer.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
    });
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/product-ownerships?customerId=${customer.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/payment-records?customerId=${customer.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/contact-events/timeline?customerId=${customer.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/follow-up-tasks?customerId=${customer.id}`,
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
  });

  it("renders the KB consent tab with status, validity, evidence, and recorder fields", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], [consent]));

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: CONSENT_RECORDS_TABLE_ARIA_LABEL });
    expect(screen.getByText("1 consent record")).toBeInTheDocument();
    expect(within(table).getByText("Marketing Email")).toBeInTheDocument();
    expect(within(table).getByText("Marketing email consent")).toBeInTheDocument();
    expect(within(table).getByLabelText("Consent status: Given")).toHaveClass(
      "consent-status-given",
    );
    expect(within(table).getByText("WEB_FORM")).toBeInTheDocument();
    expect(within(table).getByText("Valid")).toBeInTheDocument();
    expect(within(table).getByText("No action required")).toBeInTheDocument();
    expect(within(table).getByText("s3://evidence/email.pdf")).toBeInTheDocument();
    expect(within(table).getByText("Customer Service Agent")).toBeInTheDocument();
    expect(table).toBeInTheDocument();
  });

  it("displays the current consent status for each consent record correctly", async () => {
    const withdrawnConsent: ConsentRecordView = {
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
    const requiredGuardianConsent: ConsentRecordView = {
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
      createProfileFetchMock([beneficiary], [consent, withdrawnConsent, requiredGuardianConsent]),
    );

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: CONSENT_RECORDS_TABLE_ARIA_LABEL });
    expect(screen.getByText("3 consent records")).toBeInTheDocument();
    expect(within(table).getByText("Marketing Email")).toBeInTheDocument();
    expect(within(table).getByLabelText("Consent status: Given")).toHaveClass(
      "consent-status-given",
    );
    expect(within(table).getByText("Marketing Sms")).toBeInTheDocument();
    expect(within(table).getByLabelText("Consent status: Withdrawn")).toHaveClass(
      "consent-status-withdrawn",
    );
    expect(within(table).getByText("Guardian")).toBeInTheDocument();
    expect(within(table).getByLabelText("Consent status: Required")).toHaveClass(
      "consent-status-required",
    );
    expect(within(table).getAllByText("Invalid")).toHaveLength(2);
    expect(within(table).getAllByText("Action required")).toHaveLength(2);
    expect(within(table).getByText("s3://evidence/sms-withdrawal.pdf")).toBeInTheDocument();
    expect(within(table).getByText("Compliance Officer")).toBeInTheDocument();
  });

  it("shows an empty consent tab state when no consent records exist", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], []));

    renderDetailsPage();

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(
      screen.getByText(CONSENT_EMPTY_STATE),
    ).toBeInTheDocument();
  });

  it("records a new consent from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: CONSENT_SECTION_HEADING });
    const form = screen.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL });
    fireEvent.change(within(form).getByLabelText("Consent type"), {
      target: { value: "GUARDIAN" },
    });
    fireEvent.change(within(form).getByLabelText("Consent status"), {
      target: { value: "REQUIRED" },
    });
    fireEvent.change(within(form).getByLabelText("Purpose"), {
      target: { value: "Guardian consent required" },
    });
    fireEvent.change(within(form).getByLabelText("Source"), { target: { value: "PHONE" } });
    fireEvent.change(within(form).getByLabelText("Evidence URL"), {
      target: { value: "s3://evidence/guardian.pdf" },
    });
    await userEvent.click(within(form).getByRole("button", { name: "Record consent" }));

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
    expect(await screen.findByTestId("consent-update-notice")).toHaveTextContent(
      CONSENT_RECORDED_NOTICE,
    );
  });

  it("requires purpose before recording consent through the UI", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();
    await screen.findByRole("heading", { name: CONSENT_SECTION_HEADING });
    const form = screen.getByRole("form", { name: CONSENT_RECORD_FORM_ARIA_LABEL });
    await userEvent.click(within(form).getByRole("button", { name: "Record consent" }));

    expect(screen.getByTestId("consent-form-error")).toHaveTextContent(
      consentFormValidationMessages.purposeRequired,
    );
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) => url === `${API_BASE_URL}/consents` && init?.method === "POST",
      ),
    ).toBe(false);
  });

  it("withdraws consent from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("table", { name: CONSENT_RECORDS_TABLE_ARIA_LABEL });
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
    expect(await screen.findByTestId("consent-update-notice")).toHaveTextContent(
      CONSENT_WITHDRAWN_NOTICE,
    );
  });

  it("marks a marketing opt-out from the customer details consent tab", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    await screen.findByRole("heading", { name: CONSENT_SECTION_HEADING });
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
    expect(await screen.findByTestId("consent-update-notice")).toHaveTextContent(
      CONSENT_OPT_OUT_NOTICE,
    );
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
    expect(
      within(table).getByText("Grace Guardian / grace@bayer-westphalian.test"),
    ).toBeInTheDocument();
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
    fireEvent.change(screen.getByLabelText("Guardian name"), {
      target: { value: "Updated Guardian" },
    });
    fireEvent.change(screen.getByLabelText("Guardian email"), {
      target: { value: "updated.guardian@bayer-westphalian.test" },
    });
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

  it("renders the KB product ownership tab with policy, expiration, and status fields", async () => {
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock([beneficiary], [consent], customer, [
        productOwnership,
        expiredProductOwnership,
      ]),
    );

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: "Product ownership table" });
    expect(screen.getByText("2 owned products")).toBeInTheDocument();
    expect(within(table).getByText("Life Protection")).toBeInTheDocument();
    expect(within(table).getByText("Life Insurance")).toBeInTheDocument();
    expect(within(table).getByText("POL-1000")).toBeInTheDocument();
    expect(within(table).getByText("Home Protection")).toBeInTheDocument();
    expect(within(table).getByText("POL-2000")).toBeInTheDocument();
    expect(within(table).getByText("Active")).toBeInTheDocument();
    expect(within(table).getByText("Active coverage")).toBeInTheDocument();
    expect(within(table).getByText("Expired")).toBeInTheDocument();
    expect(within(table).getByText("Inactive coverage")).toBeInTheDocument();
    expect(within(table).getByText(/Expires: .+2027/)).toBeInTheDocument();
    expect(within(table).getByText(/Expires: .+2026/)).toBeInTheDocument();
    expect(table).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Life Protection" })).toHaveAttribute(
      "href",
      `/products/${catalogProduct.id}`,
    );
  });

  it("hides consent, payment, and beneficiary tabs for product managers", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], [consent]));

    renderDetailsPage(["PRODUCT_MANAGER"]);
    await screen.findByRole("heading", { name: "Products" });

    expect(screen.queryByRole("heading", { name: "Consent" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Payment records" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Beneficiaries" })).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Customer details" })).toBeInTheDocument();
  });

  it("displays the saved expiration date after assigning a product to the customer", async () => {
    const assignedOwnership = {
      id: "41000000-0000-0000-0000-000000000003",
      customerId: customer.id,
      customerFullName: customer.fullName,
      productId: catalogProduct.id,
      productName: catalogProduct.name,
      productType: catalogProduct.productType,
      policyNumber: "POL-4000",
      startDate: "2026-04-01",
      expirationDate: "2027-04-01",
      status: "ACTIVE",
      active: true,
      createdAt: "2026-07-05T12:00:00Z",
    };
    let savedOwnerships: (typeof assignedOwnership)[] = [];
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.endsWith("/product-ownerships") && init?.method === "POST") {
        savedOwnerships = [assignedOwnership];
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Product ownership assigned",
            data: assignedOwnership,
          }),
        });
      }
      if (url.includes("/product-ownerships")) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Product ownerships loaded",
            data: savedOwnerships,
          }),
        });
      }

      return createProfileFetchMock([beneficiary], [consent])(url, init);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["ADMIN"]);
    await screen.findByRole("heading", { name: "Products" });
    expect(screen.getByText("No products are assigned to this customer.")).toBeInTheDocument();

    const assignForm = screen.getByRole("form", { name: "Assign product ownership" });
    await userEvent.selectOptions(
      within(assignForm).getByLabelText("Product to assign"),
      catalogProduct.id,
    );
    await userEvent.type(within(assignForm).getByLabelText("Policy number"), "POL-4000");
    await userEvent.type(within(assignForm).getByLabelText("Coverage start date"), "2026-04-01");
    await userEvent.type(
      within(assignForm).getByLabelText("Coverage expiration date"),
      "2027-04-01",
    );
    await userEvent.click(within(assignForm).getByRole("button", { name: "Assign product" }));

    const table = await screen.findByRole("table", { name: "Product ownership table" });
    await waitFor(() => {
      expect(within(table).getByText(/Expires: .+2027/)).toBeInTheDocument();
    });
    expect(screen.getByText("Product assigned to customer.")).toBeInTheDocument();
  });

  it("allows an admin to assign a product to the customer", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["ADMIN"]);
    await screen.findByRole("heading", { name: "Products" });

    const assignForm = screen.getByRole("form", { name: "Assign product ownership" });
    await userEvent.selectOptions(
      within(assignForm).getByLabelText("Product to assign"),
      catalogProduct.id,
    );
    await userEvent.type(within(assignForm).getByLabelText("Policy number"), "POL-4000");
    await userEvent.type(within(assignForm).getByLabelText("Coverage start date"), "2026-04-01");
    await userEvent.type(
      within(assignForm).getByLabelText("Coverage expiration date"),
      "2027-04-01",
    );
    await userEvent.click(within(assignForm).getByRole("button", { name: "Assign product" }));

    await waitFor(() => {
      const assignCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/product-ownerships` && init?.method === "POST",
      );

      expect(assignCall).toBeDefined();
      expect(JSON.parse(assignCall?.[1]?.body as string)).toEqual({
        customerId: customer.id,
        productId: catalogProduct.id,
        startDate: "2026-04-01",
        expirationDate: "2027-04-01",
        policyNumber: "POL-4000",
      });
    });
    expect(screen.getByText("Product assigned to customer.")).toBeInTheDocument();
  });

  it("allows a product manager to assign a product to the customer", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["PRODUCT_MANAGER"]);
    await screen.findByRole("heading", { name: "Products" });

    const assignForm = screen.getByRole("form", { name: "Assign product ownership" });
    await userEvent.selectOptions(
      within(assignForm).getByLabelText("Product to assign"),
      catalogProduct.id,
    );
    await userEvent.type(within(assignForm).getByLabelText("Policy number"), "POL-3000");
    await userEvent.type(within(assignForm).getByLabelText("Coverage start date"), "2026-03-01");
    await userEvent.type(
      within(assignForm).getByLabelText("Coverage expiration date"),
      "2027-03-01",
    );
    await userEvent.click(within(assignForm).getByRole("button", { name: "Assign product" }));

    await waitFor(() => {
      const assignCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/product-ownerships` && init?.method === "POST",
      );

      expect(assignCall).toBeDefined();
      expect(JSON.parse(assignCall?.[1]?.body as string)).toEqual({
        customerId: customer.id,
        productId: catalogProduct.id,
        startDate: "2026-03-01",
        expirationDate: "2027-03-01",
        policyNumber: "POL-3000",
      });
    });
    expect(screen.getByText("Product assigned to customer.")).toBeInTheDocument();
  });

  it("allows a product manager to update a product ownership record", async () => {
    const fetchMock = createProfileFetchMock([beneficiary], [consent], customer, [
      productOwnership,
    ]);
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["PRODUCT_MANAGER"]);
    const table = await screen.findByRole("table", { name: "Product ownership table" });
    const updateForm = within(table).getByRole("form", { name: "Update ownership POL-1000" });

    await userEvent.clear(within(updateForm).getByLabelText("Policy number for Life Protection"));
    await userEvent.type(
      within(updateForm).getByLabelText("Policy number for Life Protection"),
      "POL-9000",
    );
    await userEvent.clear(within(updateForm).getByLabelText("Expiration date for Life Protection"));
    await userEvent.type(
      within(updateForm).getByLabelText("Expiration date for Life Protection"),
      "2028-01-15",
    );
    await userEvent.click(within(updateForm).getByRole("button", { name: "Save ownership" }));

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/product-ownerships/${productOwnership.id}` &&
          init?.method === "PUT",
      );

      expect(updateCall).toBeDefined();
      expect(JSON.parse(updateCall?.[1]?.body as string)).toEqual({
        expirationDate: "2028-01-15",
        policyNumber: "POL-9000",
      });
    });
    expect(screen.getByText("Product ownership updated.")).toBeInTheDocument();
  });

  it("renders the KB payment records tab with due amounts, reminders, and default-risk fields", async () => {
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock(
        [beneficiary],
        [consent],
        customer,
        [productOwnership],
        [paymentRecord, overduePaymentRecord],
      ),
    );

    renderDetailsPage(["CAMPAIGN_MANAGER"]);
    const table = await screen.findByRole("table", { name: "Payment records table" });

    expect(screen.getByText("2 payment records")).toBeInTheDocument();
    expect(within(table).getAllByText("Due: €129.99").length).toBeGreaterThan(0);
    expect(within(table).getAllByText("Due").length).toBeGreaterThan(0);
    expect(within(table).getByText("Overdue")).toBeInTheDocument();
    expect(within(table).getByText("0 days overdue")).toBeInTheDocument();
    expect(within(table).getByText("14 days overdue")).toBeInTheDocument();
    expect(within(table).getByText("2")).toBeInTheDocument();
  });

  it("displays the saved payment record after creating one for the customer", async () => {
    const createdPaymentRecord = {
      id: "43000000-0000-0000-0000-000000000003",
      customerId: customer.id,
      customerFullName: customer.fullName,
      productOwnershipId: productOwnership.id,
      productId: catalogProduct.id,
      productName: catalogProduct.name,
      productType: catalogProduct.productType,
      dueDate: "2026-08-01",
      paidAt: null,
      amountDue: 89.5,
      amountPaid: null,
      status: "DUE",
      reminderCount: 0,
      daysOverdue: 0,
      defaultRisk: false,
    };
    let savedPaymentRecords: (typeof createdPaymentRecord)[] = [];
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.endsWith("/payment-records") && init?.method === "POST") {
        savedPaymentRecords = [createdPaymentRecord];
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Payment record created",
            data: createdPaymentRecord,
          }),
        });
      }
      if (url.includes("/payment-records")) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Payment records loaded",
            data: savedPaymentRecords,
          }),
        });
      }

      return createProfileFetchMock([beneficiary], [consent], customer, [productOwnership])(
        url,
        init,
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findByRole("heading", { name: "Payments" });
    expect(screen.getByText("No payment records exist for this customer.")).toBeInTheDocument();

    const createForm = screen.getByRole("form", { name: "Create payment record" });
    await userEvent.selectOptions(
      within(createForm).getByLabelText("Owned product for payment record"),
      productOwnership.id,
    );
    await userEvent.type(within(createForm).getByLabelText("Payment due date"), "2026-08-01");
    await userEvent.type(within(createForm).getByLabelText("Payment amount due"), "89.50");
    await userEvent.click(
      within(createForm).getByRole("button", { name: "Create payment record" }),
    );

    const table = await screen.findByRole("table", { name: "Payment records table" });
    await waitFor(() => {
      expect(within(table).getByText("Due: €89.50")).toBeInTheDocument();
    });
    expect(within(table).getByText("Due")).toBeInTheDocument();
    expect(screen.getByText("Payment record created.")).toBeInTheDocument();
  });

  it("allows a customer service agent to update a payment record", async () => {
    const fetchMock = createProfileFetchMock(
      [beneficiary],
      [consent],
      customer,
      [productOwnership],
      [paymentRecord],
    );
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findByRole("table", { name: "Payment records table" });

    await userEvent.clear(screen.getByLabelText("Selected payment due date"));
    await userEvent.type(screen.getByLabelText("Selected payment due date"), "2026-08-01");
    await userEvent.clear(screen.getByLabelText("Selected payment amount due"));
    await userEvent.type(screen.getByLabelText("Selected payment amount due"), "150.25");
    await userEvent.click(screen.getByRole("button", { name: "Save payment" }));

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/payment-records/${paymentRecord.id}` && init?.method === "PUT",
      );

      expect(updateCall).toBeDefined();
      expect(JSON.parse(updateCall?.[1]?.body as string)).toEqual({
        dueDate: "2026-08-01",
        amountDue: 150.25,
      });
    });
    expect(screen.getByText("Payment record updated.")).toBeInTheDocument();
  });

  it("marks a due payment record paid and shows paid status in the table", async () => {
    const savedPaymentRecords = [{ ...paymentRecord }];
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (
        url.includes(`/payment-records/${paymentRecord.id}/mark-paid`) &&
        init?.method === "PATCH"
      ) {
        const payload = JSON.parse(init.body as string);
        savedPaymentRecords[0] = {
          ...paymentRecord,
          status: "PAID",
          amountPaid: payload.amountPaid,
          paidAt: "2026-07-10T09:30:00Z",
        };

        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Payment record marked paid",
            data: savedPaymentRecords[0],
          }),
        });
      }
      if (url.includes("/payment-records")) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            success: true,
            message: "Payment records loaded",
            data: savedPaymentRecords,
          }),
        });
      }

      return createProfileFetchMock([beneficiary], [consent], customer, [productOwnership])(
        url,
        init,
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findByRole("heading", { name: "Payments" });

    const table = screen.getByRole("table", { name: "Payment records table" });
    expect(within(table).getByText("Due", { selector: ".status-badge" })).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Mark paid" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/payment-records/${paymentRecord.id}/mark-paid`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${createAccessToken(["CUSTOMER_SERVICE_AGENT"])}`,
          },
          body: JSON.stringify({
            amountPaid: 129.99,
            paidAt: null,
          }),
        },
      );
    });
    expect(screen.getByText("Payment marked paid.")).toBeInTheDocument();

    await waitFor(() => {
      expect(within(table).getByText("Paid", { selector: ".status-badge" })).toBeInTheDocument();
      expect(within(table).getByText("Paid: €129.99")).toBeInTheDocument();
    });
  });

  it("allows a customer service agent to create and mark a payment record paid", async () => {
    const fetchMock = createProfileFetchMock(
      [beneficiary],
      [consent],
      customer,
      [productOwnership],
      [paymentRecord],
    );
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findByRole("heading", { name: "Payments" });

    const createForm = screen.getByRole("form", { name: "Create payment record" });
    await userEvent.selectOptions(
      within(createForm).getByLabelText("Owned product for payment record"),
      productOwnership.id,
    );
    await userEvent.type(within(createForm).getByLabelText("Payment due date"), "2026-08-01");
    await userEvent.type(within(createForm).getByLabelText("Payment amount due"), "89.50");
    await userEvent.click(
      within(createForm).getByRole("button", { name: "Create payment record" }),
    );

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/payment-records` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toEqual({
        customerId: customer.id,
        productOwnershipId: productOwnership.id,
        dueDate: "2026-08-01",
        amountDue: 89.5,
      });
    });
    expect(screen.getByText("Payment record created.")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Mark paid" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/payment-records/${paymentRecord.id}/mark-paid`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${createAccessToken(["CUSTOMER_SERVICE_AGENT"])}`,
          },
          body: JSON.stringify({
            amountPaid: 129.99,
            paidAt: null,
          }),
        },
      );
    });
    expect(screen.getByText("Payment marked paid.")).toBeInTheDocument();
  });

  it("hides payment management controls for read-only customer profile viewers", async () => {
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock(
        [beneficiary],
        [consent],
        customer,
        [productOwnership],
        [paymentRecord],
      ),
    );

    renderDetailsPage(["CAMPAIGN_MANAGER"]);
    await screen.findByRole("heading", { name: "Payments" });

    expect(screen.queryByRole("form", { name: "Create payment record" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Mark paid" })).not.toBeInTheDocument();
  });

  it("shows a recoverable error state when payment records cannot be loaded", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes("/payment-records")) {
        return Promise.resolve({
          ok: false,
          status: 500,
          json: async () => ({
            code: "INTERNAL_ERROR",
            message: "Payment records could not be loaded",
          }),
        });
      }

      return createProfileFetchMock([beneficiary], [consent])(url, init);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Payments" })).toBeInTheDocument();
    expect(screen.getByText("Payment records could not be loaded.")).toBeInTheDocument();
  });

  it("hides product assignment controls for read-only customer profile viewers", async () => {
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock([beneficiary], [consent], customer, [productOwnership]),
    );

    renderDetailsPage(["CAMPAIGN_MANAGER"]);
    await screen.findByRole("heading", { name: "Products" });

    expect(
      screen.queryByRole("form", { name: "Assign product ownership" }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Assign product" })).not.toBeInTheDocument();
  });

  it("shows an empty product ownership tab state when no products are assigned", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], [consent]));

    renderDetailsPage();

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(screen.getByText("No products are assigned to this customer.")).toBeInTheDocument();
  });

  it("shows a recoverable error state when product ownership records cannot be loaded", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes("/product-ownerships")) {
        return Promise.resolve({
          ok: false,
          status: 500,
          json: async () => ({
            code: "INTERNAL_ERROR",
            message: "Product ownership records could not be loaded",
          }),
        });
      }

      return createProfileFetchMock([beneficiary], [consent])(url, init);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage();

    expect(await screen.findByRole("heading", { name: "Customer details" })).toBeInTheDocument();
    expect(screen.getByText("Product ownership records could not be loaded.")).toBeInTheDocument();
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

  it("renders the KB contact history tab with events and outcomes", async () => {
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock([beneficiary], [consent], customer, [], [], [contactEvent]),
    );

    renderDetailsPage();

    const table = await screen.findByRole("table", { name: "Contact history table" });
    expect(screen.getByText("1 contact event")).toBeInTheDocument();
    expect(within(table).getByText("Sent")).toBeInTheDocument();
    expect(within(table).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(table).getByText("Automated email sent")).toBeInTheDocument();
    expect(table).toBeInTheDocument();
  });

  it("allows a customer service agent to record a contact event", async () => {
    const fetchMock = createProfileFetchMock(
      [beneficiary],
      [consent],
      customer,
      [],
      [],
      [contactEvent],
    );
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findByRole("heading", { name: "Contact history" });

    const createForm = screen.getByRole("form", { name: "Record contact event" });
    await userEvent.selectOptions(within(createForm).getByLabelText("Contact channel"), "PHONE");
    await userEvent.selectOptions(within(createForm).getByLabelText("Event type"), "CALLED");
    await userEvent.selectOptions(
      within(createForm).getByLabelText("Contact outcome"),
      "INTERESTED",
    );
    await userEvent.type(
      within(createForm).getByLabelText("Contact notes"),
      "Called the customer and they are interested.",
    );
    await userEvent.click(within(createForm).getByRole("button", { name: "Record event" }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/contact-events` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      const payload = JSON.parse(createCall?.[1]?.body as string);
      expect(payload).toEqual(
        expect.objectContaining({
          channel: "PHONE",
          eventType: "CALLED",
          outcome: "INTERESTED",
          notes: "Called the customer and they are interested.",
          customerId: customer.id,
        }),
      );
      expect(new Date(payload.occurredAt).toISOString()).toBe(payload.occurredAt);
    });
  });

  it("allows a user to filter the contact history timeline by event type", async () => {
    const fetchMock = createProfileFetchMock(
      [beneficiary],
      [consent],
      customer,
      [],
      [],
      [contactEvent],
    );
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CAMPAIGN_MANAGER"]);
    await screen.findByRole("heading", { name: "Contact history" });
    const eventTypeFilter = screen.getByLabelText("Filter contact history by event type");
    const eventTypeLabel = document.querySelector(
      'label[for="contact-history-event-type-filter"]',
    );

    expect(eventTypeFilter).toHaveAttribute("id", "contact-history-event-type-filter");
    expect(eventTypeLabel).toHaveClass("sr-only");

    await userEvent.selectOptions(eventTypeFilter, "SENT");

    await waitFor(() => {
      const urlString = fetchMock.mock.calls.find(
        ([url]) =>
          String(url).includes("/contact-events/timeline") &&
          String(url).includes("eventType=SENT"),
      )?.[0];
      expect(urlString).toBeDefined();
    });
  });

  it("renders the KB follow-up task tab with assignments priority status and due date", async () => {
    vi.stubGlobal(
      "fetch",
      createProfileFetchMock(
        [beneficiary],
        [consent],
        customer,
        [],
        [],
        [contactEvent],
        [followUpTask],
      ),
    );

    renderDetailsPage(["CAMPAIGN_MANAGER"]);

    const table = await screen.findByRole("table", {
      name: "Customer follow-up tasks table",
    });
    expect(screen.getByText("1 follow-up task")).toBeInTheDocument();
    expect(within(table).getByText("Call Ada about renewal")).toBeInTheDocument();
    expect(
      within(table).getByText("Discuss renewal options and payment concerns"),
    ).toBeInTheDocument();
    expect(within(table).getByText("Life renewal outreach")).toBeInTheDocument();
    expect(within(table).getByText("Sales Agent")).toBeInTheDocument();
    expect(within(table).getByText("High")).toBeInTheDocument();
    expect(within(table).getByText("Open")).toBeInTheDocument();
  });

  it("loads follow-up tasks using the current customer id", async () => {
    const fetchMock = createProfileFetchMock(
      [beneficiary],
      [consent],
      customer,
      [],
      [],
      [],
      [followUpTask],
    );
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["SALES_AGENT"]);

    await screen.findByRole("heading", { name: "Follow-up tasks" });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/follow-up-tasks?customerId=${customer.id}`,
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${createAccessToken(["SALES_AGENT"])}`,
          },
        },
      );
    });
  });

  it("shows an empty follow-up task state when no customer tasks exist", async () => {
    vi.stubGlobal("fetch", createProfileFetchMock([beneficiary], [consent]));

    renderDetailsPage(["CAMPAIGN_MANAGER"]);

    expect(
      await screen.findByText("No follow-up tasks are linked to this customer."),
    ).toBeInTheDocument();
  });

  it("shows a recoverable error state when customer follow-up tasks cannot be loaded", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if (url.includes("/follow-up-tasks")) {
        return Promise.resolve({
          ok: false,
          status: 500,
          json: async () => ({
            code: "INTERNAL_ERROR",
            message: "Follow-up tasks could not be loaded",
          }),
        });
      }

      return createProfileFetchMock([beneficiary], [consent])(url, init);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderDetailsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Follow-up tasks" })).toBeInTheDocument();
    expect(screen.getByText("Follow-up tasks could not be loaded.")).toBeInTheDocument();
  });
});

function createProfileFetchMock(
  beneficiaries: (typeof beneficiary)[],
  consents: (typeof consent)[],
  profile = customer,
  productOwnerships: (typeof productOwnership)[] = [],
  paymentRecords: (typeof paymentRecord)[] = [],
  contactEvents: (typeof contactEvent)[] = [],
  followUpTasks: FollowUpTaskView[] = [],
) {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url.includes("/follow-up-tasks")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Follow-up tasks loaded",
          data: followUpTasks,
        }),
      });
    }
    if (url === `${API_BASE_URL}/contact-events` && init?.method === "POST") {
      const payload = JSON.parse(init.body as string);
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Contact event recorded",
          data: {
            ...contactEvent,
            ...payload,
            id: "60000000-0000-0000-0000-000000000002",
          },
        }),
      });
    }
    if (url.includes("/contact-events/timeline")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Contact timeline loaded",
          data: contactEvents,
        }),
      });
    }
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
    if (url.endsWith("/product-ownerships") && init?.method === "POST") {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Product ownership assigned",
          data: {
            ...productOwnership,
            ...payload,
            id: "41000000-0000-0000-0000-000000000003",
            productName: catalogProduct.name,
            productType: catalogProduct.productType,
            status: "ACTIVE",
            active: true,
            customerFullName: profile.fullName,
            createdAt: "2026-07-05T12:00:00Z",
          },
        }),
      });
    }
    if (url.includes("/product-ownerships/") && init?.method === "PUT") {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Product ownership updated",
          data: {
            ...productOwnership,
            ...payload,
          },
        }),
      });
    }
    if (url.includes("/product-ownerships")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Product ownerships loaded",
          data: productOwnerships,
        }),
      });
    }
    if (url.endsWith("/payment-records") && init?.method === "POST") {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Payment record created",
          data: {
            ...paymentRecord,
            ...payload,
            id: "43000000-0000-0000-0000-000000000003",
            productName: catalogProduct.name,
            productType: catalogProduct.productType,
            productId: catalogProduct.id,
            status: "DUE",
            reminderCount: 0,
            daysOverdue: 0,
            defaultRisk: false,
          },
        }),
      });
    }
    if (
      url.includes(`/payment-records/${paymentRecord.id}/mark-paid`) &&
      init?.method === "PATCH"
    ) {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Payment record marked paid",
          data: {
            ...paymentRecord,
            status: "PAID",
            amountPaid: payload.amountPaid,
            paidAt: "2026-07-10T09:30:00Z",
          },
        }),
      });
    }
    if (url.includes(`/payment-records/${paymentRecord.id}`) && init?.method === "PUT") {
      const payload = JSON.parse(init.body as string);

      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Payment record updated",
          data: {
            ...paymentRecord,
            ...payload,
          },
        }),
      });
    }
    if (url.includes("/payment-records")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Payment records loaded",
          data: paymentRecords,
        }),
      });
    }
    if (url.endsWith("/products")) {
      return Promise.resolve({
        ok: true,
        json: async () => ({
          success: true,
          message: "Products loaded",
          data: [catalogProduct],
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

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
