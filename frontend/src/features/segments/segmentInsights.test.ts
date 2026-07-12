import { describe, expect, it } from "vitest";
import type { SegmentView } from "@/api/segments";
import {
  buildCatalogInsights,
  buildSelectedSegmentInsight,
} from "@/features/segments/segmentInsights";

const segments: SegmentView[] = [
  {
    id: "1",
    name: "Munich prospects",
    description: "City filter",
    ownerUserId: "owner-a",
    ownerFullName: "A",
    visibility: "TEAM",
    criteria: [
      {
        id: "c1",
        segmentId: "1",
        fieldName: "city",
        operator: "EQUALS",
        value: "Munich",
        logicalGroup: "location",
        joinOperator: "AND",
      },
      {
        id: "c2",
        segmentId: "1",
        fieldName: "customer_type",
        operator: "EQUALS",
        value: "PROSPECT",
        logicalGroup: null,
        joinOperator: "AND",
      },
    ],
    createdAt: null,
    updatedAt: null,
  },
  {
    id: "2",
    name: "Open draft",
    description: null,
    ownerUserId: "owner-b",
    ownerFullName: "B",
    visibility: "PRIVATE",
    criteria: [],
    createdAt: null,
    updatedAt: null,
  },
  {
    id: "3",
    name: "Global renewals",
    description: null,
    ownerUserId: "owner-a",
    ownerFullName: "A",
    visibility: "GLOBAL",
    criteria: [
      {
        id: "c3",
        segmentId: "3",
        fieldName: "city",
        operator: "EQUALS",
        value: "Berlin",
        logicalGroup: null,
        joinOperator: "AND",
      },
      {
        id: "c4",
        segmentId: "3",
        fieldName: "is_expiring",
        operator: "EQUALS",
        value: "true",
        logicalGroup: "product",
        joinOperator: "OR",
      },
    ],
    createdAt: null,
    updatedAt: null,
  },
];

describe("segmentInsights", () => {
  it("builds catalog-level segmentation insights", () => {
    const insights = buildCatalogInsights(segments);

    expect(insights.totalSegments).toBe(3);
    expect(insights.totalCriteria).toBe(4);
    expect(insights.averageCriteriaPerSegment).toBeCloseTo(1.3, 1);
    expect(insights.emptyCriteriaCount).toBe(1);
    expect(insights.multiCriteriaCount).toBe(2);
    expect(insights.ownerCount).toBe(2);
    expect(insights.visibilityBreakdown).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ visibility: "PRIVATE", count: 1 }),
        expect.objectContaining({ visibility: "TEAM", count: 1 }),
        expect.objectContaining({ visibility: "GLOBAL", count: 1 }),
      ]),
    );
    expect(insights.topFields[0]).toEqual(
      expect.objectContaining({ fieldName: "city", count: 2, label: "City" }),
    );
  });

  it("returns empty catalog insights for an empty list", () => {
    const insights = buildCatalogInsights([]);
    expect(insights.totalSegments).toBe(0);
    expect(insights.averageCriteriaPerSegment).toBe(0);
    expect(insights.topFields).toEqual([]);
  });

  it("builds selected segment structure insights", () => {
    const selected = buildSelectedSegmentInsight(segments[0]);
    expect(selected).not.toBeNull();
    expect(selected?.criteriaCount).toBe(2);
    expect(selected?.fieldLabels).toEqual(["City", "Customer type"]);
    expect(selected?.hasOrLogic).toBe(false);
    expect(selected?.groups).toEqual(["location"]);
    expect(selected?.insightNotes.some((note) => /AND-only/i.test(note))).toBe(true);
    expect(selected?.insightNotes.some((note) => /eligibility filtering/i.test(note))).toBe(true);
  });

  it("notes OR logic and private visibility for exploratory segments", () => {
    const selected = buildSelectedSegmentInsight(segments[2]);
    expect(selected?.hasOrLogic).toBe(true);
    expect(selected?.insightNotes.some((note) => /OR join logic/i.test(note))).toBe(true);
    expect(selected?.insightNotes.some((note) => /Global visibility/i.test(note))).toBe(true);
  });

  it("returns null when no segment is selected", () => {
    expect(buildSelectedSegmentInsight(undefined)).toBeNull();
  });
});
