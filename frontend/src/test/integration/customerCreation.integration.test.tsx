/**
 * Customer creation UI integration (KB item 599 / FR-011).
 *
 * Full route tree: authorized user opens Customers, fills create form, posts,
 * sees success notice + list row; validation blocks bad input; read-only roles
 * do not see the create panel.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  CUSTOMER_CREATE_FORM_ARIA_LABEL,
  CUSTOMER_CREATE_PAGE_HEADING,
  CUSTOMER_CREATE_SECTION_HEADING,
  CUSTOMER_CREATE_SUBMIT_LABEL,
  CUSTOMER_CREATED_NOTICE,
  CUSTOMER_CREATION_FIXTURES,
  CUSTOMER_LIST_TABLE_ARIA_LABEL,
  customerFormValidationMessages,
} from "@/features/customers/customerCreationFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const existingCustomer = {
  id: "20000000-0000-0000-0000-000000000001",
  customerType: "CUSTOMER",
  firstName: "Ada",
  lastName: "Policyholder",
  fullName: "Ada Policyholder",
  email: "ada@bayer-westphalian.test",
  phone: "+49-555-0100",
  addressLine: null,
  city: "Berlin",
  country: "Germany",
  dateOfBirth: null,
  ageGroup: null,
  status: "ACTIVE",
  doNotContact: false,
  active: true,
  contactable: true,
  source: null,
  createdAt: "2026-07-03T12:00:00Z",
  updatedAt: "2026-07-03T12:00:00Z",
  deletedAt: null,
};

function pageOf(customers: unknown[]) {
  return {
    content: customers,
    page: 0,
    size: 50,
    totalElements: customers.length,
    totalPages: 1,
    first: true,
    last: true,
    empty: customers.length === 0,
  };
}

function customerCreationHandlers() {
  let customers: unknown[] = [existingCustomer];

  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url, method) =>
        url.includes("/customers") &&
        !url.includes("/import") &&
        method === "POST" &&
        !/\/customers\/[0-9a-f-]{36}/i.test(url),
      response: () => {
        const created = {
          ...existingCustomer,
          id: CUSTOMER_CREATION_FIXTURES.id,
          customerType: CUSTOMER_CREATION_FIXTURES.customerType,
          firstName: CUSTOMER_CREATION_FIXTURES.firstName,
          lastName: CUSTOMER_CREATION_FIXTURES.lastName,
          fullName: CUSTOMER_CREATION_FIXTURES.fullName,
          email: CUSTOMER_CREATION_FIXTURES.email,
          phone: CUSTOMER_CREATION_FIXTURES.phone,
          city: CUSTOMER_CREATION_FIXTURES.city,
          country: CUSTOMER_CREATION_FIXTURES.country,
          source: CUSTOMER_CREATION_FIXTURES.source,
          status: CUSTOMER_CREATION_FIXTURES.status,
        };
        customers = [created, ...customers];
        return jsonOk(created, "Customer created");
      },
    },
    {
      match: (url, method) => url.includes("/customers") && method === "GET",
      response: () => jsonOk(pageOf(customers), "Customers loaded"),
    },
  ]);
}

describe("customer creation UI integration (item 599)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  }, 15_000);

  it("shows the create customer panel for authorized roles", async () => {
    vi.stubGlobal("fetch", customerCreationHandlers());
    renderApp({ path: "/customers", roles: ["ADMIN"] });

    expect(await screen.findByRole("heading", { name: "Customers", level: 1 })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: CUSTOMER_CREATE_PAGE_HEADING, level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: CUSTOMER_CREATE_SECTION_HEADING }),
    ).toBeInTheDocument();
    expect(screen.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: CUSTOMER_LIST_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
  }, 15_000);

  it("hides create controls for campaign managers", async () => {
    vi.stubGlobal("fetch", customerCreationHandlers());
    renderApp({ path: "/customers", roles: ["CAMPAIGN_MANAGER"] });

    expect(
      await screen.findByRole("table", { name: CUSTOMER_LIST_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: CUSTOMER_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL }),
    ).not.toBeInTheDocument();
  });

  it("creates a customer through the UI and shows success plus list row", async () => {
    const user = userEvent.setup();
    const fetchMock = customerCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/customers", roles: ["CUSTOMER_SERVICE_AGENT"] });
    await screen.findByRole("table", { name: CUSTOMER_LIST_TABLE_ARIA_LABEL });

    const form = screen.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL });
    await user.type(within(form).getByLabelText("First name"), CUSTOMER_CREATION_FIXTURES.firstName);
    await user.type(within(form).getByLabelText("Last name"), CUSTOMER_CREATION_FIXTURES.lastName);
    await user.type(within(form).getByLabelText("Email"), CUSTOMER_CREATION_FIXTURES.email);
    await user.type(within(form).getByLabelText("Phone"), CUSTOMER_CREATION_FIXTURES.phone);
    await user.type(within(form).getByLabelText("City"), CUSTOMER_CREATION_FIXTURES.city);
    await user.type(within(form).getByLabelText("Country"), CUSTOMER_CREATION_FIXTURES.country);
    await user.type(within(form).getByLabelText("Source"), CUSTOMER_CREATION_FIXTURES.source);
    await user.click(within(form).getByRole("button", { name: CUSTOMER_CREATE_SUBMIT_LABEL }));

    expect(await screen.findByText(CUSTOMER_CREATED_NOTICE)).toBeInTheDocument();
    const table = screen.getByRole("table", { name: CUSTOMER_LIST_TABLE_ARIA_LABEL });
    expect(
      await within(table).findByText(CUSTOMER_CREATION_FIXTURES.fullName),
    ).toBeInTheDocument();

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).includes("/customers") &&
          (init as RequestInit | undefined)?.method === "POST" &&
          !String(url).includes("/import")
        );
      });
      expect(createCall).toBeDefined();
      expect(JSON.parse(String((createCall?.[1] as RequestInit).body))).toMatchObject({
        firstName: CUSTOMER_CREATION_FIXTURES.firstName,
        lastName: CUSTOMER_CREATION_FIXTURES.lastName,
        email: CUSTOMER_CREATION_FIXTURES.email,
        phone: CUSTOMER_CREATION_FIXTURES.phone,
        city: CUSTOMER_CREATION_FIXTURES.city,
        country: CUSTOMER_CREATION_FIXTURES.country,
        source: CUSTOMER_CREATION_FIXTURES.source,
      });
    });
  });

  it("validates the create form before calling the API", async () => {
    const user = userEvent.setup();
    const fetchMock = customerCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/customers", roles: ["ADMIN"] });
    await screen.findByRole("table", { name: CUSTOMER_LIST_TABLE_ARIA_LABEL });

    const form = screen.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL });
    await user.type(within(form).getByLabelText("Email"), "bad-email");
    await user.type(within(form).getByLabelText("Phone"), "CALLME");
    await user.click(within(form).getByRole("button", { name: CUSTOMER_CREATE_SUBMIT_LABEL }));

    expect(screen.getByText(customerFormValidationMessages.firstNameRequired)).toBeInTheDocument();
    expect(screen.getByText(customerFormValidationMessages.lastNameRequired)).toBeInTheDocument();
    expect(screen.getByText(customerFormValidationMessages.emailInvalid)).toBeInTheDocument();
    expect(screen.getByText(customerFormValidationMessages.phoneInvalid)).toBeInTheDocument();

    const postCalls = fetchMock.mock.calls.filter(([url, init]) => {
      return (
        String(url).includes("/customers") &&
        (init as RequestInit | undefined)?.method === "POST" &&
        !String(url).includes("/import")
      );
    });
    expect(postCalls).toHaveLength(0);
  });
});
