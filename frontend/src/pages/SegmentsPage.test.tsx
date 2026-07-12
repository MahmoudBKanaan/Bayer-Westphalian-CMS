import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { API_BASE_URL } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS, type SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  SEGMENT_CREATE_FORM_ARIA_LABEL,
  SEGMENT_CREATE_SECTION_HEADING,
  SEGMENT_CREATE_SECTION_HINT,
  SEGMENT_CREATE_SUBMIT_LABEL,
  SEGMENT_CREATED_NOTICE,
  SEGMENT_LIST_TABLE_ARIA_LABEL,
  segmentFormValidationMessages,
} from "@/features/segments/segmentCreationFlow";
import { SegmentsPage } from "@/pages/SegmentsPage";

const munichSegment = {
  id: "42000000-0000-0000-0000-000000000001",
  name: "Munich prospects",
  description: "Customers in Munich",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  visibility: "TEAM",
  criteria: [
    {
      id: "43000000-0000-0000-0000-000000000001",
      segmentId: "42000000-0000-0000-0000-000000000001",
      fieldName: "city",
      operator: "EQUALS",
      value: "Munich",
      logicalGroup: "location",
      joinOperator: "AND",
    },
  ],
  createdAt: "2026-07-09T10:00:00Z",
  updatedAt: "2026-07-09T10:05:00Z",
};

const berlinSegment = {
  id: "42000000-0000-0000-0000-000000000002",
  name: "Berlin renewals",
  description: null,
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  visibility: "PRIVATE",
  criteria: [],
  createdAt: "2026-07-09T11:00:00Z",
  updatedAt: "2026-07-09T11:05:00Z",
};

function renderSegmentsPage(roles: SystemRoleName[] = ["CAMPAIGN_MANAGER"]) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  sessionStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  sessionStorage.setItem(
    AUTH_STORAGE_KEYS.currentUser,
    JSON.stringify({
      id: "10000000-0000-0000-0000-000000000101",
      email: "campaign.manager@bayer-westphalian.test",
      fullName: "Campaign Manager",
      status: "ACTIVE",
      lastLoginAt: "2026-07-09T12:00:00Z",
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
      <MemoryRouter initialEntries={["/segments"]}>
        <AuthProvider>
          <SegmentsPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("SegmentsPage", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("loads saved segments and shows create/edit for campaign managers", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Segmentation" })).toBeInTheDocument();
    expect(
      screen.getByText("Reusable audience criteria with eligibility-aware preview"),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Saved segments" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeInTheDocument();
    expect(
      screen.getByText(SEGMENT_CREATE_SECTION_HINT),
    ).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Edit segment" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Segment details" })).toBeInTheDocument();
    expect(await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL })).toBeInTheDocument();
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    expect(screen.getByText("Berlin renewals")).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Segment search filters" })).toBeInTheDocument();
  });

  it("exposes segment creation UI for campaign managers with create permission", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeInTheDocument();
    expect(
      screen.getByText(SEGMENT_CREATE_SECTION_HINT),
    ).toBeInTheDocument();
    const createSection = screen
      .getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })
      .closest("section");
    expect(createSection).not.toBeNull();
    expect(createSection?.querySelector("#create-segment-form")).not.toBeNull();
  });

  it("exposes segment creation UI for admins with create permission", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["ADMIN"]);

    expect(await screen.findByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeInTheDocument();
  });

  it("hides segment creation UI for product managers without create permission", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["PRODUCT_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Segmentation" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: SEGMENT_CREATE_SECTION_HEADING })).not.toBeInTheDocument();
  });

  it("shows read-only segmentation for BI analysts without create form", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["BI_ANALYST"]);

    expect(await screen.findByRole("heading", { name: "Segmentation" })).toBeInTheDocument();
    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    expect(screen.queryByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Edit segment" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Save changes" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Delete segment/i })).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Segmentation insights" })).toBeInTheDocument();
    expect(screen.getByText(/Read-only BI Analyst view/i)).toBeInTheDocument();
    expect(
      screen.queryByText(/Segment create and edit actions are hidden for your role/i),
    ).not.toBeInTheDocument();
  });

  it("allows BI analyst with campaign manager dual role to edit segments (item 200 unless allowed)", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["BI_ANALYST", "CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Segmentation" })).toBeInTheDocument();
    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Edit segment" })).toBeInTheDocument();
    expect(screen.queryByText(/Read-only BI Analyst view/i)).not.toBeInTheDocument();
  });

  it("lets BI analysts analyze a selected segment for eligibility insights", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["BI_ANALYST"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    expect(screen.getByLabelText("Segment catalog metrics")).toBeInTheDocument();
    expect(screen.getByLabelText("Visibility mix")).toBeInTheDocument();
    expect(screen.getByLabelText("Most used segment fields")).toBeInTheDocument();

    const table = screen.getByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    const munichRow = within(table).getByText("Munich prospects").closest("tr") as HTMLElement;
    await user.click(within(munichRow).getByRole("button", { name: "Select" }));

    await user.click(screen.getByRole("button", { name: "Analyze audience eligibility" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/segments/preview`,
        expect.objectContaining({ method: "POST" }),
      );
    });

    expect(await screen.findByText("Audience preview loaded.")).toBeInTheDocument();
    expect(screen.getByText("Source: Insight analysis: Munich prospects")).toBeInTheDocument();
    expect(screen.getByLabelText("Latest eligibility insight")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Exclusion reason summary" })).toBeInTheDocument();
  });

  it("denies segmentation access for unauthorized roles", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["PRODUCT_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Segmentation" })).toBeInTheDocument();
    expect(screen.getByText("You do not have permission to view segments.")).toBeInTheDocument();
    expect(screen.queryByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL })).not.toBeInTheDocument();
  });

  it("applies search filters when listing segments", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["ADMIN"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    await user.clear(screen.getByLabelText("Search segments"));
    await user.type(screen.getByLabelText("Search segments"), "Munich");
    await user.selectOptions(screen.getByLabelText("Visibility filter"), "TEAM");
    await user.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/segments?term=Munich&visibility=TEAM`,
        expect.any(Object),
      );
    });
  });

  it("creates a segment from the create form", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    const form = within(screen.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL }));

    await user.type(form.getByLabelText("Name"), "Hamburg audience");
    await user.type(form.getByLabelText("Description"), "North region prospects");
    await user.selectOptions(form.getByLabelText("Visibility"), "GLOBAL");
    await user.click(form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/segments` &&
          init != null &&
          typeof init === "object" &&
          "method" in init &&
          init.method === "POST",
      );
      expect(createCall).toBeDefined();
      const body = JSON.parse(String((createCall?.[1] as RequestInit).body));
      expect(body).toMatchObject({
        name: "Hamburg audience",
        description: "North region prospects",
        visibility: "GLOBAL",
        criteria: [],
      });
    });

    expect(await screen.findByText(SEGMENT_CREATED_NOTICE)).toBeInTheDocument();
  });

  it("validates the create segment form before posting", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);
    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });

    const form = within(screen.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL }));
    await user.click(form.getByRole("button", { name: "Add criterion" }));
    await user.selectOptions(form.getByLabelText("Field for rule 1"), "city");
    await user.click(form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }));

    expect(screen.getByText(segmentFormValidationMessages.nameRequired)).toBeInTheDocument();
    expect(
      screen.getByText(segmentFormValidationMessages.criterionValueRequired),
    ).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/segments` &&
          init != null &&
          typeof init === "object" &&
          "method" in init &&
          init.method === "POST",
      ),
    ).toBe(false);
  });

  it(
    "campaign manager can create reusable segment with visibility and criteria (item 201)",
    async () => {
      const fetchMock = createFetchMock();
      vi.stubGlobal("fetch", fetchMock);

      renderSegmentsPage(["CAMPAIGN_MANAGER"]);

      await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
      expect(screen.getByText(SEGMENT_CREATE_SECTION_HINT)).toBeInTheDocument();
      const form = within(screen.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL }));

      fireEvent.change(form.getByLabelText("Name"), {
        target: { value: "Expiring life policies" },
      });
      fireEvent.change(form.getByLabelText("Description"), {
        target: { value: "Reusable CM audience for renewals" },
      });
      fireEvent.change(form.getByLabelText("Visibility"), { target: { value: "TEAM" } });
      fireEvent.click(form.getByRole("button", { name: "Add criterion" }));
      fireEvent.change(form.getByLabelText("Field for rule 1"), { target: { value: "city" } });
      fireEvent.change(form.getByLabelText("Value for rule 1"), { target: { value: "Munich" } });
      fireEvent.click(form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }));

      await waitFor(() => {
        const createCall = fetchMock.mock.calls.find(
          ([url, init]) =>
            String(url) === `${API_BASE_URL}/segments` &&
            init != null &&
            typeof init === "object" &&
            "method" in init &&
            init.method === "POST",
        );
        expect(createCall).toBeDefined();
        const body = JSON.parse(String((createCall?.[1] as RequestInit).body));
        expect(body).toMatchObject({
          name: "Expiring life policies",
          description: "Reusable CM audience for renewals",
          visibility: "TEAM",
        });
        expect(body.criteria).toEqual(
          expect.arrayContaining([
            expect.objectContaining({
              fieldName: "city",
              operator: "EQUALS",
              value: "Munich",
            }),
          ]),
        );
      });

      expect(await screen.findByText(SEGMENT_CREATED_NOTICE)).toBeInTheDocument();
    },
    10_000,
  );

  it("includes the criteria builder on create and edit forms", async () => {
    vi.stubGlobal("fetch", createFetchMock());
    const user = userEvent.setup();

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    const createSection = screen
      .getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })
      .closest("section") as HTMLElement;
    expect(
      within(createSection).getByRole("heading", { name: "Criteria builder" }),
    ).toBeInTheDocument();
    expect(within(createSection).getByText(/No criteria yet/i)).toBeInTheDocument();

    await user.click(within(createSection).getByRole("button", { name: "Add criterion" }));
    expect(within(createSection).getByLabelText("Field for rule 1")).toBeInTheDocument();
    expect(within(createSection).getByLabelText("Value for rule 1")).toBeInTheDocument();
    expect(within(createSection).getByLabelText("Operator for rule 1")).toBeInTheDocument();

    const table = screen.getByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    const munichRow = within(table).getByText("Munich prospects").closest("tr") as HTMLElement;
    await user.click(within(munichRow).getByRole("button", { name: "Select" }));

    const editSection = screen
      .getByRole("heading", { name: "Edit segment" })
      .closest("section") as HTMLElement;
    expect(
      await within(editSection).findByRole("heading", { name: "Criteria builder" }),
    ).toBeInTheDocument();
    expect(within(editSection).getByLabelText("Field for rule 1")).toHaveValue("city");
    expect(within(editSection).getByLabelText("Value for rule 1")).toHaveValue("Munich");
  });

  it("saves criteria built in the create form", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    const createSection = screen
      .getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })
      .closest("section") as HTMLElement;
    const form = within(createSection);

    await user.type(form.getByLabelText("Name"), "Consented Munich");
    await user.click(form.getByRole("button", { name: "Add criterion" }));
    await user.selectOptions(form.getByLabelText("Field for rule 1"), "city");
    await user.clear(form.getByLabelText("Value for rule 1"));
    await user.type(form.getByLabelText("Value for rule 1"), "Munich");
    await user.click(form.getByRole("button", { name: "Add criterion" }));
    await user.selectOptions(form.getByLabelText("Join operator for rule 2"), "AND");
    await user.selectOptions(form.getByLabelText("Field for rule 2"), "opt_out");
    await user.clear(form.getByLabelText("Value for rule 2"));
    await user.type(form.getByLabelText("Value for rule 2"), "false");
    await user.click(form.getByRole("button", { name: SEGMENT_CREATE_SECTION_HEADING }));

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/segments` &&
          init != null &&
          typeof init === "object" &&
          "method" in init &&
          init.method === "POST",
      );
      expect(createCall).toBeDefined();
      const body = JSON.parse(String((createCall?.[1] as RequestInit).body));
      expect(body.criteria).toEqual([
        {
          fieldName: "city",
          operator: "EQUALS",
          value: "Munich",
          logicalGroup: null,
          joinOperator: "AND",
        },
        {
          fieldName: "opt_out",
          operator: "EQUALS",
          value: "false",
          logicalGroup: null,
          joinOperator: "AND",
        },
      ]);
    });
  });

  it("shows selected segment criteria in the details panel", async () => {
    vi.stubGlobal("fetch", createFetchMock());
    const user = userEvent.setup();

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    const table = screen.getByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    const munichRow = within(table).getByText("Munich prospects").closest("tr");
    expect(munichRow).not.toBeNull();
    await user.click(within(munichRow as HTMLElement).getByRole("button", { name: "Select" }));

    expect(
      await screen.findByRole("table", { name: "Segment criteria table" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Segment UUID")).toBeInTheDocument();
    expect(screen.getByText(munichSegment.id)).toBeInTheDocument();
    expect(screen.getByText("city")).toBeInTheDocument();
    expect(screen.getByText("Munich")).toBeInTheDocument();
    expect(screen.getByText("EQUALS")).toBeInTheDocument();
  });

  it("shows the audience preview panel for roles that can preview", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    expect(await screen.findByRole("heading", { name: "Audience preview" })).toBeInTheDocument();
    expect(screen.getByText("No preview yet")).toBeInTheDocument();
  });

  it("hides audience preview for compliance officers", async () => {
    vi.stubGlobal("fetch", createFetchMock());

    renderSegmentsPage(["COMPLIANCE_OFFICER"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    expect(screen.queryByRole("heading", { name: "Audience preview" })).not.toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: "Segmentation insights" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText(/Segment create and edit actions are hidden for your role/i),
    ).toBeInTheDocument();
  });

  it("previews a saved segment and renders audience results", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["BI_ANALYST"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    const table = screen.getByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    const munichRow = within(table).getByText("Munich prospects").closest("tr") as HTMLElement;
    await user.click(within(munichRow).getByRole("button", { name: "Select" }));

    await user.click(screen.getByRole("button", { name: "Preview this segment" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `${API_BASE_URL}/segments/preview`,
        expect.objectContaining({ method: "POST" }),
      );
    });

    expect(await screen.findByText("Audience preview loaded.")).toBeInTheDocument();
    expect(screen.getByText("Source: Saved segment: Munich prospects")).toBeInTheDocument();
    expect(screen.getByLabelText("Audience size metrics")).toBeInTheDocument();
    expect(screen.getByText("Total audience")).toBeInTheDocument();
    expect(
      screen.getByRole("table", { name: "Eligible customers preview table" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Lena Mueller")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Exclusion reason summary" })).toBeInTheDocument();
    expect(screen.getByLabelText("Exclusion reason cards")).toBeInTheDocument();
    expect(screen.getAllByText("Do not contact").length).toBeGreaterThan(0);
    expect(
      screen.getByRole("table", { name: "Exclusion reason summary table" }),
    ).toBeInTheDocument();
  });

  it("previews draft criteria from the create form", async () => {
    const fetchMock = createFetchMock();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();

    renderSegmentsPage(["CAMPAIGN_MANAGER"]);

    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    const createSection = screen
      .getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING })
      .closest("section") as HTMLElement;
    const form = within(createSection);

    await user.click(form.getByRole("button", { name: "Add criterion" }));
    await user.selectOptions(form.getByLabelText("Field for rule 1"), "city");
    await user.clear(form.getByLabelText("Value for rule 1"));
    await user.type(form.getByLabelText("Value for rule 1"), "Hamburg");
    await user.click(form.getByRole("button", { name: "Preview audience" }));

    await waitFor(() => {
      const previewCall = fetchMock.mock.calls.find(
        ([url, init]) =>
          String(url) === `${API_BASE_URL}/segments/preview` &&
          init != null &&
          typeof init === "object" &&
          "method" in init &&
          init.method === "POST",
      );
      expect(previewCall).toBeDefined();
      const body = JSON.parse(String((previewCall?.[1] as RequestInit).body));
      expect(body.criteria).toEqual([
        expect.objectContaining({
          fieldName: "city",
          operator: "EQUALS",
          value: "Hamburg",
        }),
      ]);
    });

    expect(await screen.findByText("Source: Create form draft")).toBeInTheDocument();
  });
});

function createFetchMock() {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";

    if (url.includes("/segments/preview") && method === "POST") {
      return jsonResponse({
        success: true,
        message: "Segment preview loaded",
        data: {
          totalAudienceCount: 4,
          eligibleCount: 2,
          excludedCount: 2,
          matchingCustomers: [
            {
              id: "20000000-0000-0000-0000-000000000201",
              customerType: "PROSPECT",
              firstName: "Lena",
              lastName: "Mueller",
              fullName: "Lena Mueller",
              email: "lena.mueller@bayer-westphalian.test",
              city: "Munich",
              country: "Germany",
              status: "ACTIVE",
              doNotContact: false,
            },
            {
              id: "20000000-0000-0000-0000-000000000202",
              customerType: "CUSTOMER",
              firstName: "Anna",
              lastName: "Weber",
              fullName: "Anna Weber",
              email: null,
              city: "Hamburg",
              country: "Germany",
              status: "INTERESTED",
              doNotContact: false,
            },
          ],
          exclusionReasonSummary: [
            {
              code: "DO_NOT_CONTACT",
              message: "Customer has do-not-contact enabled",
              count: 2,
            },
          ],
        },
      });
    }

    if (url.includes("/segments?") || url.endsWith("/segments")) {
      if (method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}"));
        return jsonResponse({
          success: true,
          message: "Segment created",
          data: {
            id: "42000000-0000-0000-0000-000000000099",
            name: body.name,
            description: body.description,
            ownerUserId: "10000000-0000-0000-0000-000000000101",
            ownerFullName: "Campaign Manager",
            visibility: body.visibility,
            criteria: body.criteria ?? [],
            createdAt: "2026-07-09T12:00:00Z",
            updatedAt: "2026-07-09T12:00:00Z",
          },
        });
      }

      return jsonResponse({
        success: true,
        message: "Segments loaded",
        data: [munichSegment, berlinSegment],
      });
    }

    if (url.includes("/segments/") && method === "PUT") {
      const body = JSON.parse(String(init?.body ?? "{}"));
      return jsonResponse({
        success: true,
        message: "Segment updated",
        data: {
          ...munichSegment,
          name: body.name,
          description: body.description,
          visibility: body.visibility,
        },
      });
    }

    if (url.includes("/segments/") && method === "DELETE") {
      return jsonResponse({ success: true, message: "Segment deleted", data: null });
    }

    return jsonResponse({ success: true, message: "ok", data: null }, 404);
  });
}

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

function createAccessToken(roles: SystemRoleName[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");

  return `header.${payload}.signature`;
}
