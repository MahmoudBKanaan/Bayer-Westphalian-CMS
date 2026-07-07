import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import {
  createCustomer,
  deleteCustomer,
  getCustomer,
  importCustomersCsv,
  listCustomers,
  updateCustomer,
} from "@/api/customers";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";

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

describe("customers api", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads customers from the paginated endpoint", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customers loaded",
        data: {
          content: [customer],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
          empty: false,
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(listCustomers()).resolves.toEqual([customer]);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers?page=0&size=50`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("loads customers with KB search and filter query parameters", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customers loaded",
        data: {
          content: [customer],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true,
          empty: false,
        },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);
    const expectedUrl =
      `${API_BASE_URL}/customers?page=0&size=50&term=Ada&city=Berlin&country=Germany` +
      "&customerType=CUSTOMER&status=ACTIVE&contactable=true";

    await expect(
      listCustomers({
        term: " Ada ",
        customerType: "CUSTOMER",
        status: "ACTIVE",
        city: " Berlin ",
        country: " Germany ",
        contactable: "true",
      }),
    ).resolves.toEqual([customer]);

    expect(fetchMock).toHaveBeenCalledWith(expectedUrl, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("loads a customer profile by id", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customer loaded",
        data: customer,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCustomer(customer.id)).resolves.toEqual(customer);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
    });
  });

  it("sends create and update customer form payloads", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customer changed",
        data: customer,
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const payload = {
      customerType: "CUSTOMER" as const,
      firstName: " Ada ",
      lastName: " Policyholder ",
      email: " ada@bayer-westphalian.test ",
      phone: " +49-555-0100 ",
      addressLine: " Insurance Street 1 ",
      city: " Berlin ",
      country: " Germany ",
      dateOfBirth: "1984-08-21",
      ageGroup: "AGE_41_60" as const,
      status: "ACTIVE" as const,
      doNotContact: false,
      source: " LIFE_INSURANCE_BENEFICIARY ",
    };

    await createCustomer(payload);
    await updateCustomer(customer.id, payload);

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers`, {
      body: JSON.stringify({
        customerType: "CUSTOMER",
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
        doNotContact: false,
        source: "LIFE_INSURANCE_BENEFICIARY",
      }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "POST",
    });
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
      body: JSON.stringify({
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
        doNotContact: false,
        source: "LIFE_INSURANCE_BENEFICIARY",
      }),
      headers: {
        "Content-Type": "application/json",
      },
      method: "PUT",
    });
  });

  it("soft deletes customers through the KB delete endpoint", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customer deleted",
        data: { ...customer, deletedAt: "2026-07-05T12:00:00Z" },
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(deleteCustomer(customer.id)).resolves.toMatchObject({
      id: customer.id,
      deletedAt: "2026-07-05T12:00:00Z",
    });

    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/customers/${customer.id}`, {
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer access-token",
      },
      method: "DELETE",
    });
  });

  it("uploads customer and prospect CSV imports as multipart form data", async () => {
    sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, "access-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: "Customers imported",
        data: {
          importedCount: 1,
          failedCount: 1,
          customers: [customer],
          errors: [
            {
              lineNumber: 3,
              field: "email",
              message: "Enter a valid email address.",
              value: "not-an-email",
            },
          ],
        },
      }),
    });
    const file = new File(["firstName,lastName\nAda,Policyholder"], "customers.csv", {
      type: "text/csv",
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(importCustomersCsv(file)).resolves.toMatchObject({
      importedCount: 1,
      failedCount: 1,
      errors: [{ lineNumber: 3, field: "email" }],
    });

    const importCall = fetchMock.mock.calls[0];
    expect(importCall?.[0]).toBe(`${API_BASE_URL}/customers/import`);
    expect(importCall?.[1]).toMatchObject({
      headers: {
        Authorization: "Bearer access-token",
      },
      method: "POST",
    });
    expect(importCall?.[1]?.headers).not.toHaveProperty("Content-Type");
    expect(importCall?.[1]?.body).toBeInstanceOf(FormData);
  });
});
