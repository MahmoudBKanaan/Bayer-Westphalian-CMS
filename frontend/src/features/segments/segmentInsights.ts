import type { SegmentView, SegmentVisibility } from "@/api/segments";
import { formatFieldLabel } from "@/features/segments/criteriaFields";

export type VisibilityBreakdown = {
  visibility: SegmentVisibility;
  count: number;
  share: number;
};

export type FieldUsageInsight = {
  fieldName: string;
  label: string;
  count: number;
  share: number;
};

export type SegmentCatalogInsights = {
  totalSegments: number;
  totalCriteria: number;
  averageCriteriaPerSegment: number;
  emptyCriteriaCount: number;
  multiCriteriaCount: number;
  visibilityBreakdown: VisibilityBreakdown[];
  topFields: FieldUsageInsight[];
  ownerCount: number;
};

export type SelectedSegmentInsight = {
  id: string;
  name: string;
  visibility: SegmentVisibility;
  criteriaCount: number;
  fieldLabels: string[];
  joinOperators: string[];
  hasOrLogic: boolean;
  groups: string[];
  insightNotes: string[];
};

export function buildCatalogInsights(segments: SegmentView[]): SegmentCatalogInsights {
  const totalSegments = segments.length;
  const totalCriteria = segments.reduce((sum, segment) => sum + segment.criteria.length, 0);
  const emptyCriteriaCount = segments.filter((segment) => segment.criteria.length === 0).length;
  const multiCriteriaCount = segments.filter((segment) => segment.criteria.length > 1).length;

  const visibilityCounts: Record<SegmentVisibility, number> = {
    PRIVATE: 0,
    TEAM: 0,
    GLOBAL: 0,
  };
  for (const segment of segments) {
    visibilityCounts[segment.visibility] = (visibilityCounts[segment.visibility] ?? 0) + 1;
  }

  const visibilityBreakdown: VisibilityBreakdown[] = (
    ["PRIVATE", "TEAM", "GLOBAL"] as SegmentVisibility[]
  ).map((visibility) => {
    const count = visibilityCounts[visibility] ?? 0;
    return {
      visibility,
      count,
      share: totalSegments === 0 ? 0 : (count / totalSegments) * 100,
    };
  });

  const fieldCounts = new Map<string, number>();
  for (const segment of segments) {
    for (const criterion of segment.criteria) {
      const key = criterion.fieldName.trim().toLowerCase();
      if (key === "") {
        continue;
      }
      fieldCounts.set(key, (fieldCounts.get(key) ?? 0) + 1);
    }
  }

  const fieldTotal = [...fieldCounts.values()].reduce((sum, count) => sum + count, 0);
  const topFields: FieldUsageInsight[] = [...fieldCounts.entries()]
    .map(([fieldName, count]) => ({
      fieldName,
      label: formatFieldLabel(fieldName),
      count,
      share: fieldTotal === 0 ? 0 : (count / fieldTotal) * 100,
    }))
    .sort((left, right) => {
      if (right.count !== left.count) {
        return right.count - left.count;
      }
      return left.fieldName.localeCompare(right.fieldName);
    })
    .slice(0, 8);

  const owners = new Set(
    segments
      .map((segment) => segment.ownerUserId)
      .filter((ownerId): ownerId is string => Boolean(ownerId)),
  );

  return {
    totalSegments,
    totalCriteria,
    averageCriteriaPerSegment:
      totalSegments === 0 ? 0 : Number((totalCriteria / totalSegments).toFixed(1)),
    emptyCriteriaCount,
    multiCriteriaCount,
    visibilityBreakdown,
    topFields,
    ownerCount: owners.size,
  };
}

export function buildSelectedSegmentInsight(
  segment: SegmentView | undefined,
): SelectedSegmentInsight | null {
  if (segment == null) {
    return null;
  }

  const fieldLabels = segment.criteria.map((criterion) => formatFieldLabel(criterion.fieldName));
  const joinOperators = segment.criteria
    .slice(1)
    .map((criterion) => criterion.joinOperator ?? "AND");
  const hasOrLogic = joinOperators.includes("OR");
  const groups = [
    ...new Set(
      segment.criteria
        .map((criterion) => criterion.logicalGroup?.trim() ?? "")
        .filter((group) => group !== ""),
    ),
  ];

  const insightNotes: string[] = [];
  if (segment.criteria.length === 0) {
    insightNotes.push(
      "No criteria defined — preview will evaluate all active customer profiles before eligibility filtering.",
    );
  } else if (segment.criteria.length === 1) {
    insightNotes.push("Single-filter audience; eligibility still applies on preview.");
  } else {
    insightNotes.push(
      hasOrLogic
        ? "Uses OR join logic; left-to-right evaluation can broaden the matched audience."
        : "Uses AND-only joins; every criterion must match before eligibility filtering.",
    );
  }

  if (groups.length > 0) {
    insightNotes.push(`Logical groups present: ${groups.join(", ")}.`);
  }

  if (segment.visibility === "PRIVATE") {
    insightNotes.push("Private visibility — useful for analyst drafts and exploratory audiences.");
  } else if (segment.visibility === "GLOBAL") {
    insightNotes.push("Global visibility — shared catalog audience available across teams.");
  }

  return {
    id: segment.id,
    name: segment.name,
    visibility: segment.visibility,
    criteriaCount: segment.criteria.length,
    fieldLabels,
    joinOperators,
    hasOrLogic,
    groups,
    insightNotes,
  };
}
