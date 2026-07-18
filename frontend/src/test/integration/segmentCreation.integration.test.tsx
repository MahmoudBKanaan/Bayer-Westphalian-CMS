/**
 * Segment creation UI integration (KB item 602 / FR-077 / item 201).
 *
 * Full route tree: authorized user opens Segments, fills create form, posts,
 * sees success notice + list row; validation blocks empty name / partial criteria;
 * BI analyst cannot create.
 */
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  SEGMENT_CREATE_FORM_ARIA_LABEL,
  SEGMENT_CREATE_SECTION_HEADING,
  SEGMENT_CREATE_SUBMIT_LABEL,
  SEGMENT_CREATED_NOTICE,
  SEGMENT_CREATION_FIXTURES,
  SEGMENT_LIST_TABLE_ARIA_LABEL,
  segmentFormValidationMessages,
} from "@/features/segments/segmentCreationFlow";
import {
  createFetchRouter,
  emptyDashboardPayload,
  jsonOk,
  renderApp,
} from "@/test/integration/renderApp";

const existingSegment = {
  id: "40000000-0000-0000-0000-000000000201",
  name: "Munich prospects",
  description: "Existing segment",
  ownerUserId: "10000000-0000-0000-0000-000000000101",
  ownerFullName: "Campaign Manager",
  visibility: "TEAM",
  criteria: [],
  createdAt: "2026-07-09T10:00:00Z",
  updatedAt: "2026-07-09T10:05:00Z",
};

function segmentCreationHandlers() {
  let segments: unknown[] = [existingSegment];

  return createFetchRouter([
    {
      match: (url) => url.includes("/analytics/dashboard"),
      response: () => jsonOk(emptyDashboardPayload),
    },
    {
      match: (url, method) => {
        if (method !== "POST") {
          return false;
        }
        try {
          const pathname = new URL(url).pathname.replace(/\/+$/, "");
          return pathname.endsWith("/segments") && !pathname.includes("/preview");
        } catch {
          return url.endsWith("/segments");
        }
      },
      response: () => {
        const created = {
          ...existingSegment,
          id: SEGMENT_CREATION_FIXTURES.id,
          name: SEGMENT_CREATION_FIXTURES.name,
          description: SEGMENT_CREATION_FIXTURES.description,
          visibility: SEGMENT_CREATION_FIXTURES.visibility,
          criteria: [
            {
              id: "40100000-0000-0000-0000-00000000s001",
              segmentId: SEGMENT_CREATION_FIXTURES.id,
              fieldName: SEGMENT_CREATION_FIXTURES.cityCriterionField,
              operator: "EQUALS",
              value: SEGMENT_CREATION_FIXTURES.cityCriterionValue,
              logicalGroup: null,
              joinOperator: "AND",
            },
          ],
        };
        segments = [created, ...segments];
        return jsonOk(created, "Segment created");
      },
    },
    {
      match: (url, method) => url.includes("/segments") && method === "GET",
      response: () => jsonOk(segments, "Segments loaded"),
    },
  ]);
}

describe("segment creation UI integration (item 602)", () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.unstubAllGlobals();
  });

  it("shows the create segment panel for campaign managers", async () => {
    vi.stubGlobal("fetch", segmentCreationHandlers());
    renderApp({ path: "/segments", roles: ["CAMPAIGN_MANAGER"] });

    expect(await screen.findByRole("heading", { name: "Segments", level: 1 })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING }),
    ).toBeInTheDocument();
    expect(screen.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL })).toBeInTheDocument();
    expect(
      await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
  });

  it("hides create controls for BI analysts", async () => {
    vi.stubGlobal("fetch", segmentCreationHandlers());
    renderApp({ path: "/segments", roles: ["BI_ANALYST"] });

    expect(
      await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("heading", { name: SEGMENT_CREATE_SECTION_HEADING }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL }),
    ).not.toBeInTheDocument();
  });

  it("creates a segment through the UI and shows success plus list row", async () => {
    const user = userEvent.setup({ delay: null });
    const fetchMock = segmentCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/segments", roles: ["ADMIN"] });
    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });

    const form = within(screen.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL }));
    await user.type(form.getByLabelText("Name"), SEGMENT_CREATION_FIXTURES.name);
    await user.type(form.getByLabelText("Description"), SEGMENT_CREATION_FIXTURES.description);
    await user.selectOptions(
      form.getByLabelText("Visibility"),
      SEGMENT_CREATION_FIXTURES.visibility,
    );
    await user.click(form.getByRole("button", { name: "Add criterion" }));
    await user.selectOptions(
      form.getByLabelText("Field for rule 1"),
      SEGMENT_CREATION_FIXTURES.cityCriterionField,
    );
    await user.clear(form.getByLabelText("Value for rule 1"));
    await user.type(
      form.getByLabelText("Value for rule 1"),
      SEGMENT_CREATION_FIXTURES.cityCriterionValue,
    );
    await user.click(form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }));

    expect(await screen.findByText(SEGMENT_CREATED_NOTICE)).toBeInTheDocument();
    const table = screen.getByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });
    expect(await within(table).findByText(SEGMENT_CREATION_FIXTURES.name)).toBeInTheDocument();

    await waitFor(() => {
      const createCall = fetchMock.mock.calls.find(([url, init]) => {
        return (
          String(url).includes("/segments") &&
          !String(url).includes("/preview") &&
          (init as RequestInit | undefined)?.method === "POST"
        );
      });
      expect(createCall).toBeDefined();
      expect(JSON.parse(String((createCall?.[1] as RequestInit).body))).toMatchObject({
        name: SEGMENT_CREATION_FIXTURES.name,
        description: SEGMENT_CREATION_FIXTURES.description,
        visibility: SEGMENT_CREATION_FIXTURES.visibility,
      });
    });
  });

  it("validates the create form before calling the API", async () => {
    const user = userEvent.setup();
    const fetchMock = segmentCreationHandlers();
    vi.stubGlobal("fetch", fetchMock);

    renderApp({ path: "/segments", roles: ["CAMPAIGN_MANAGER"] });
    await screen.findByRole("table", { name: SEGMENT_LIST_TABLE_ARIA_LABEL });

    const form = within(screen.getByRole("form", { name: SEGMENT_CREATE_FORM_ARIA_LABEL }));
    await user.click(form.getByRole("button", { name: "Add criterion" }));
    await user.selectOptions(form.getByLabelText("Field for rule 1"), "city");
    await user.click(form.getByRole("button", { name: SEGMENT_CREATE_SUBMIT_LABEL }));

    expect(screen.getByText(segmentFormValidationMessages.nameRequired)).toBeInTheDocument();
    expect(
      screen.getByText(segmentFormValidationMessages.criterionValueRequired),
    ).toBeInTheDocument();

    const postCalls = fetchMock.mock.calls.filter(([url, init]) => {
      return (
        String(url).includes("/segments") &&
        !String(url).includes("/preview") &&
        (init as RequestInit | undefined)?.method === "POST"
      );
    });
    expect(postCalls).toHaveLength(0);
  });
});
