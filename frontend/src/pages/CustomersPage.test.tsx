import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import { CustomersPage } from "@/pages/CustomersPage";

const ADMIN_ACCESS_TOKEN = createAccessToken(["ADMIN"]);
const CUSTOMER_SERVICE_ACCESS_TOKEN = createAccessToken(["CUSTOMER_SERVICE_AGENT"]);

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

const prospect = {
  id: "20000000-0000-0000-0000-000000000002",
  customerType: "PROSPECT",
  firstName: "Ben",
  lastName: "Prospect",
  fullName: "Ben Prospect",
  email: null,
  phone: "+49-555-0200",
  addressLine: null,
  city: "Munich",
  country: "Germany",
  dateOfBirth: null,
  ageGroup: null,
  status: "INTERESTED",
  doNotContact: true,
  active: false,
  contactable: false,
  source: "CSV_IMPORT",
  createdAt: "2026-07-04T12:00:00Z",
  updatedAt: "2026-07-04T12:00:00Z",
  deletedAt: null,
};

function renderCustomersPage(roles: SystemRoleName[] = ["ADMIN"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000009901",
      email: "admin@bayer-westphalian.test",
      fullName: "Admin User",
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
      <MemoryRouter>
        <AuthProvider>
          <CustomersPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("CustomersPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads customers and renders create and edit forms", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCustomersPage();

    expect(
      await screen.findByRole("heading", { name: "Customers and prospects" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Create customer" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Edit customer" })).toBeInTheDocument();
    expect(await screen.findAllByText("Ada Policyholder")).not.toHaveLength(0);
    expect(screen.getByText("ada@bayer-westphalian.test")).toBeInTheDocument();
  });

  it("hides customer management controls for KB read-only customer roles", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCustomersPage(["CAMPAIGN_MANAGER"]);

    const table = await screen.findByRole("table", { name: "Customer list table" });
    expect(screen.queryByRole("heading", { name: "Create customer" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Edit customer" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "CSV import" })).not.toBeInTheDocument();
    expect(
      screen.getByText("Customer management actions are hidden for your role."),
    ).toBeInTheDocument();
    const customerRow = within(table).getByRole("row", { name: /Ada Policyholder/ });
    expect(within(customerRow).getByRole("link", { name: "Details" })).toHaveAttribute(
      "href",
      `/customers/${customer.id}`,
    );
    expect(within(table).queryByRole("button", { name: "Select" })).not.toBeInTheDocument();
  });

  it("limits customer service users to create, update, and import actions", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCustomersPage(["CUSTOMER_SERVICE_AGENT"]);

    expect(await screen.findByRole("heading", { name: "Create customer" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Edit customer" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "CSV import" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Delete customer" })).not.toBeInTheDocument();
  });

  it("renders the customer list table with KB profile columns", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCustomersPage();

    const table = await screen.findByRole("table", { name: "Customer list table" });
    expect(
      within(table).getByRole("columnheader", { name: "Contact details" }),
    ).toBeInTheDocument();
    expect(within(table).getByRole("columnheader", { name: "Marketing" })).toBeInTheDocument();
    expect(within(table).getByRole("columnheader", { name: "Source" })).toBeInTheDocument();
    expect(within(table).getByText("Ada Policyholder")).toBeInTheDocument();
    expect(within(table).getByText("Berlin, Germany")).toBeInTheDocument();
    expect(within(table).getByText("41 60")).toBeInTheDocument();
    expect(within(table).getByText("LIFE_INSURANCE_BENEFICIARY")).toBeInTheDocument();
    expect(within(table).getByText("Ben Prospect")).toBeInTheDocument();
    expect(within(table).getByText("Email not provided")).toBeInTheDocument();
    expect(within(table).getByText("Do not contact")).toBeInTheDocument();
    const customerRow = within(table).getByRole("row", { name: /Ada Policyholder/ });
    expect(within(customerRow).getByRole("link", { name: "Details" })).toHaveAttribute(
      "href",
      `/customers/${customer.id}`,
    );
  });

  it("selects a customer from the list table for editing", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderCustomersPage();

    const table = await screen.findByRole("table", { name: "Customer list table" });
    const prospectRow = within(table).getByRole("row", { name: /Ben Prospect/ });
    await userEvent.click(within(prospectRow).getByRole("button", { name: "Select" }));

    const editPanel = screen.getByRole("heading", { name: "Edit customer" }).closest("section");
    expect(editPanel).not.toBeNull();
    expect(within(editPanel as HTMLElement).getByLabelText("Last name")).toHaveValue("Prospect");
    expect(within(editPanel as HTMLElement).getByLabelText("Phone")).toHaveValue("+49-555-0200");
    expect(within(editPanel as HTMLElement).getByLabelText("Do not contact")).toBeChecked();
  });

  it("shows an empty table state when no customers match filters", async () => {
    vi.stubGlobal("fetch", createFetchMock([]));

    renderCustomersPage();

    expect(await screen.findByText("No customer records are available.")).toBeInTheDocument();
    expect(
      screen.getByText("Create a customer or import prospects from CSV when your role allows it."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: "Customer list table" })).not.toBeInTheDocument();
  });

  it("shows a filtered empty state when active filters return no customer records", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");
    fetchMock.mockImplementation(createFetchMock([]));

    await userEvent.type(screen.getByLabelText("Search customers"), "No Match");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    expect(
      await screen.findByText("No customer records match the current filters."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Adjust the search fields or reset filters to broaden the customer list."),
    ).toBeInTheDocument();
  });

  it("shows a loading state while customer records are being loaded", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => undefined)),
    );

    renderCustomersPage();

    expect(screen.getByRole("status")).toHaveTextContent("Loading customer records");
    expect(
      screen.getByText("Customer and prospect data is being loaded from the CRM service."),
    ).toBeInTheDocument();
  });

  it("shows an error state with retry when customer records cannot load", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: async () => ({ message: "CRM service unavailable" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Customer records could not be loaded.",
    );
    await userEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.length).toBeGreaterThan(1);
    });
  });

  it("filters customers using KB search fields", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const expectedQuery =
      "/customers?page=0&size=50&term=Ada&city=Berlin&country=Germany" +
      "&customerType=CUSTOMER&status=ACTIVE&contactable=true";

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    await userEvent.type(screen.getByLabelText("Search customers"), "Ada");
    await userEvent.selectOptions(screen.getByLabelText("Customer type filter"), "CUSTOMER");
    await userEvent.selectOptions(screen.getByLabelText("Customer status filter"), "ACTIVE");
    await userEvent.type(screen.getByLabelText("Customer city filter"), "Berlin");
    await userEvent.type(screen.getByLabelText("Customer country filter"), "Germany");
    await userEvent.selectOptions(screen.getByLabelText("Customer contactable filter"), "true");
    expect(screen.getByText("No active filters")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([url]) => String(url).includes(expectedQuery))).toBe(true);
    });
    expect(screen.getByText("6 active filters")).toBeInTheDocument();
  });

  it("resets customer search and filter controls", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    await userEvent.type(screen.getByLabelText("Search customers"), "Ada");
    await userEvent.selectOptions(screen.getByLabelText("Customer status filter"), "ACTIVE");
    await userEvent.click(screen.getByRole("button", { name: "Apply filters" }));
    await screen.findByText("2 active filters");

    await userEvent.click(screen.getByRole("button", { name: "Reset filters" }));

    expect(screen.getByLabelText("Search customers")).toHaveValue("");
    expect(screen.getByLabelText("Customer status filter")).toHaveValue("ALL");
    expect(screen.getByText("No active filters")).toBeInTheDocument();
    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([url]) => url === `${API_BASE_URL}/customers?page=0&size=50`),
      ).toBe(true);
    });
  });

  it("creates a customer through the backend endpoint", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    const createPanel = screen.getByRole("heading", { name: "Create customer" }).closest("section");
    expect(createPanel).not.toBeNull();

    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Customer type"),
      "PROSPECT",
    );
    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("First name"), "Lena");
    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("Last name"), "Mueller");
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Email"),
      "lena.mueller@bayer-westphalian.test",
    );
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Phone"),
      "+49-555-0200",
    );
    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("City"), "Munich");
    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Status"),
      "INTERESTED",
    );
    await userEvent.click(
      within(createPanel as HTMLElement).getByRole("button", { name: "Create customer" }),
    );

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/customers` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toMatchObject({
        customerType: "PROSPECT",
        firstName: "Lena",
        lastName: "Mueller",
        email: "lena.mueller@bayer-westphalian.test",
        phone: "+49-555-0200",
        city: "Munich",
        status: "INTERESTED",
      });
      expect(createCall?.[1]).toMatchObject({
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
        method: "POST",
      });
    });
  });

  it("allows an authorized customer service user to create a customer", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findAllByText("Ada Policyholder");

    const createPanel = screen.getByRole("heading", { name: "Create customer" }).closest("section");
    expect(createPanel).not.toBeNull();
    expect(screen.queryByRole("button", { name: "Delete customer" })).not.toBeInTheDocument();

    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("First name"), "Clara");
    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("Last name"), "Service");
    await userEvent.type(
      within(createPanel as HTMLElement).getByLabelText("Email"),
      "clara.service@bayer-westphalian.test",
    );
    await userEvent.selectOptions(
      within(createPanel as HTMLElement).getByLabelText("Status"),
      "ACTIVE",
    );
    await userEvent.click(
      within(createPanel as HTMLElement).getByRole("button", { name: "Create customer" }),
    );

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/customers` && init?.method === "POST",
      );

      expect(createCall).toBeDefined();
      expect(JSON.parse(createCall?.[1]?.body as string)).toMatchObject({
        customerType: "CUSTOMER",
        firstName: "Clara",
        lastName: "Service",
        email: "clara.service@bayer-westphalian.test",
        status: "ACTIVE",
      });
      expect(createCall?.[1]).toMatchObject({
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${CUSTOMER_SERVICE_ACCESS_TOKEN}`,
        },
        method: "POST",
      });
    });
  });

  it("validates the create customer form before posting", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    const createPanel = screen.getByRole("heading", { name: "Create customer" }).closest("section");
    expect(createPanel).not.toBeNull();

    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("Email"), "bad-email");
    await userEvent.type(within(createPanel as HTMLElement).getByLabelText("Phone"), "CALLME");
    await userEvent.click(
      within(createPanel as HTMLElement).getByRole("button", { name: "Create customer" }),
    );

    expect(screen.getByText("First name is required.")).toBeInTheDocument();
    expect(screen.getByText("Last name is required.")).toBeInTheDocument();
    expect(screen.getByText("Enter a valid email address.")).toBeInTheDocument();
    expect(
      screen.getByText("Use 7 to 50 digits, spaces, parentheses, hyphens, and an optional +."),
    ).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) => url === `${API_BASE_URL}/customers` && init?.method === "POST",
      ),
    ).toBe(false);
  });

  it("imports customers from CSV and renders row-level validation errors", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    const importPanel = screen.getByRole("heading", { name: "CSV import" }).closest("section");
    expect(importPanel).not.toBeNull();

    const file = new File(
      ["firstName,lastName,email\nAda,Policyholder,bad-email"],
      "customers.csv",
      {
        type: "text/csv",
      },
    );
    await userEvent.upload(
      within(importPanel as HTMLElement).getByLabelText("Customer CSV file"),
      file,
    );
    expect(within(importPanel as HTMLElement).getByText("customers.csv")).toBeInTheDocument();

    await userEvent.click(
      within(importPanel as HTMLElement).getByRole("button", { name: "Import CSV" }),
    );

    await waitFor(() => {
      const importCall = fetchMock.mock.calls.find(
        ([url, init]) => url === `${API_BASE_URL}/customers/import` && init?.method === "POST",
      );

      expect(importCall).toBeDefined();
      expect(importCall?.[1]?.body).toBeInstanceOf(FormData);
      expect(importCall?.[1]?.headers).toMatchObject({
        Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
      });
      expect(importCall?.[1]?.headers).not.toHaveProperty("Content-Type");
    });
    expect(screen.getByText("CSV import completed.")).toBeInTheDocument();
    expect(screen.getByText("Imported")).toBeInTheDocument();
    expect(screen.getByText("Failed")).toBeInTheDocument();

    const errorsTable = screen.getByRole("table", { name: "CSV import errors table" });
    expect(within(errorsTable).getByText("email")).toBeInTheDocument();
    expect(within(errorsTable).getByText("Enter a valid email address.")).toBeInTheDocument();
    expect(within(errorsTable).getByText("bad-email")).toBeInTheDocument();
  });

  it("edits the selected customer profile", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    const editPanel = screen.getByRole("heading", { name: "Edit customer" }).closest("section");
    expect(editPanel).not.toBeNull();

    const lastNameInput = within(editPanel as HTMLElement).getByLabelText("Last name");
    await userEvent.clear(lastNameInput);
    await userEvent.type(lastNameInput, "Client");
    await userEvent.selectOptions(
      within(editPanel as HTMLElement).getByLabelText("Status"),
      "CONVERTED",
    );
    await userEvent.click(within(editPanel as HTMLElement).getByLabelText("Do not contact"));
    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Save customer" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
        body: expect.stringContaining('"lastName":"Client"'),
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
        method: "PUT",
      });
    });

    const updateCall = fetchMock.mock.calls.find(
      ([url, init]) => url === `${API_BASE_URL}/customers/${customer.id}` && init?.method === "PUT",
    );
    expect(JSON.parse(updateCall?.[1]?.body as string)).toMatchObject({
      firstName: "Ada",
      lastName: "Client",
      status: "CONVERTED",
      doNotContact: true,
    });
  });

  it("allows an authorized customer service user to update a customer", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage(["CUSTOMER_SERVICE_AGENT"]);
    await screen.findAllByText("Ada Policyholder");

    const editPanel = screen.getByRole("heading", { name: "Edit customer" }).closest("section");
    expect(editPanel).not.toBeNull();
    expect(screen.queryByRole("button", { name: "Delete customer" })).not.toBeInTheDocument();

    const phoneInput = within(editPanel as HTMLElement).getByLabelText("Phone");
    await userEvent.clear(phoneInput);
    await userEvent.type(phoneInput, "+49-555-0199");
    await userEvent.selectOptions(
      within(editPanel as HTMLElement).getByLabelText("Status"),
      "INTERESTED",
    );
    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Save customer" }),
    );

    await waitFor(() => {
      const updateCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          url === `${API_BASE_URL}/customers/${customer.id}` && init?.method === "PUT",
      );

      expect(updateCall).toBeDefined();
      expect(JSON.parse(updateCall?.[1]?.body as string)).toMatchObject({
        firstName: "Ada",
        lastName: "Policyholder",
        phone: "+49-555-0199",
        status: "INTERESTED",
      });
      expect(updateCall?.[1]).toMatchObject({
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${CUSTOMER_SERVICE_ACCESS_TOKEN}`,
        },
        method: "PUT",
      });
    });
  });

  it("validates the edit customer form before saving", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage();
    await screen.findAllByText("Ada Policyholder");

    const editPanel = screen.getByRole("heading", { name: "Edit customer" }).closest("section");
    expect(editPanel).not.toBeNull();

    const lastNameInput = within(editPanel as HTMLElement).getByLabelText("Last name");
    await userEvent.clear(lastNameInput);
    await userEvent.clear(within(editPanel as HTMLElement).getByLabelText("Date of birth"));
    await userEvent.type(
      within(editPanel as HTMLElement).getByLabelText("Date of birth"),
      "2999-01-01",
    );
    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Save customer" }),
    );

    expect(screen.getByText("Last name is required.")).toBeInTheDocument();
    expect(screen.getByText("Date of birth cannot be in the future.")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          url === `${API_BASE_URL}/customers/${customer.id}` && init?.method === "PUT",
      ),
    ).toBe(false);
  });

  it("allows an admin to soft-delete a customer", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);

    renderCustomersPage(["ADMIN"]);
    await screen.findAllByText("Ada Policyholder");

    const editPanel = screen.getByRole("heading", { name: "Edit customer" }).closest("section");
    expect(editPanel).not.toBeNull();
    expect(
      within(editPanel as HTMLElement).getByRole("button", { name: "Delete customer" }),
    ).toBeInTheDocument();

    await userEvent.click(
      within(editPanel as HTMLElement).getByRole("button", { name: "Delete customer" }),
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${ADMIN_ACCESS_TOKEN}`,
        },
        method: "DELETE",
      });
    });
    expect(screen.getByText("Customer deleted.")).toBeInTheDocument();
  });
});

function createFetchMock(customers = [customer, prospect]) {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    if (url.endsWith("/customers/import") && init?.method === "POST") {
      return jsonResponse({
        importedCount: 1,
        failedCount: 1,
        customers: [customer],
        errors: [
          {
            lineNumber: 2,
            field: "email",
            message: "Enter a valid email address.",
            value: "bad-email",
          },
        ],
      });
    }

    if (url.endsWith("/customers") && init?.method === "POST") {
      return jsonResponse({
        ...customer,
        ...JSON.parse(init.body as string),
        id: "20000000-0000-0000-0000-000000000002",
        fullName: "Lena Mueller",
      });
    }

    if (url.includes(`/customers/${customer.id}`) && init?.method === "PUT") {
      const body = JSON.parse(init.body as string) as Record<string, unknown>;
      return jsonResponse({
        ...customer,
        ...body,
        fullName: `${body.firstName} ${body.lastName}`,
      });
    }

    if (url.includes(`/customers/${customer.id}`) && init?.method === "DELETE") {
      return jsonResponse({
        ...customer,
        active: false,
        contactable: false,
        deletedAt: "2026-07-05T12:00:00Z",
      });
    }

    return Promise.resolve({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customers loaded",
        data: {
          content: customers,
          page: 0,
          size: 50,
          totalElements: customers.length,
          totalPages: 1,
          first: true,
          last: true,
          empty: false,
        },
      }),
    });
  });
}

function jsonResponse(data: unknown) {
  return Promise.resolve({
    ok: true,
    json: async () => ({
      success: true,
      message: "OK",
      data,
    }),
  });
}

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
