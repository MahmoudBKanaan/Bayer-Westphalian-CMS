import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SegmentInsightPanel } from "@/components/SegmentInsightPanel";
import type { SegmentPreviewView, SegmentView } from "@/api/segments";

const segments: SegmentView[] = [
  {
    id: "1",
    name: "Munich prospects",
    description: "City filter",
    ownerUserId: "owner-a",
    ownerFullName: "Campaign Manager",
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
        fieldName: "opt_out",
        operator: "EQUALS",
        value: "false",
        logicalGroup: null,
        joinOperator: "AND",
      },
    ],
    createdAt: "2026-07-09T10:00:00Z",
    updatedAt: "2026-07-09T10:05:00Z",
  },
  {
    id: "2",
    name: "Open draft",
    description: null,
    ownerUserId: "owner-b",
    ownerFullName: "Other Owner",
    visibility: "PRIVATE",
    criteria: [],
    createdAt: null,
    updatedAt: null,
  },
];

const preview: SegmentPreviewView = {
  totalAudienceCount: 10,
  eligibleCount: 7,
  excludedCount: 3,
  matchingCustomers: [],
  exclusionReasonSummary: [],
};

describe("SegmentInsightPanel", () => {
  it("renders read-only BI Analyst insight framing and catalog metrics", () => {
    render(
      <SegmentInsightPanel segments={segments} selectedSegment={segments[0]} preview={null} />,
    );

    expect(screen.getByRole("heading", { name: "Segmentation insights" })).toBeInTheDocument();
    expect(screen.getByText(/Read-only BI Analyst view/i)).toBeInTheDocument();
    const metrics = screen.getByLabelText("Segment catalog metrics");
    expect(within(metrics).getByText("Saved segments")).toBeInTheDocument();
    // Catalog size is 2; criteria count also shows 2 elsewhere on the panel.
    expect(within(metrics).getByText("2")).toBeInTheDocument();
    expect(screen.getByLabelText("Visibility mix")).toBeInTheDocument();
    expect(screen.getByLabelText("Most used segment fields")).toBeInTheDocument();
    expect(screen.getByText("City")).toBeInTheDocument();
  });

  it("shows selected segment structure and insight notes", () => {
    render(
      <SegmentInsightPanel segments={segments} selectedSegment={segments[0]} preview={null} />,
    );

    expect(screen.getByText("Selected segment analysis")).toBeInTheDocument();
    // Name appears in the section subheading and selected-audience card.
    expect(screen.getAllByText("Munich prospects").length).toBeGreaterThan(0);
    expect(screen.getByText("City, Marketing opt-out")).toBeInTheDocument();
    expect(screen.getByLabelText("Selected segment insight notes")).toBeInTheDocument();
    expect(screen.getByText(/AND-only joins/i)).toBeInTheDocument();
  });

  it("invokes analyze callback for eligibility preview", async () => {
    const user = userEvent.setup();
    const onAnalyzeSegment = vi.fn();

    render(
      <SegmentInsightPanel
        segments={segments}
        selectedSegment={segments[0]}
        preview={null}
        onAnalyzeSegment={onAnalyzeSegment}
      />,
    );

    await user.click(screen.getByRole("button", { name: "Analyze audience eligibility" }));
    expect(onAnalyzeSegment).toHaveBeenCalledWith(segments[0]);
  });

  it("shows latest eligibility snapshot when preview data is present", () => {
    render(
      <SegmentInsightPanel segments={segments} selectedSegment={segments[0]} preview={preview} />,
    );

    const snapshot = screen.getByLabelText("Latest eligibility insight");
    expect(within(snapshot).getByText(/Total 10/i)).toBeInTheDocument();
    expect(within(snapshot).getByText(/Eligible 7/i)).toBeInTheDocument();
    expect(within(snapshot).getByText(/Excluded 3/i)).toBeInTheDocument();
    expect(within(snapshot).getByText(/70.0% eligibility rate/i)).toBeInTheDocument();
  });

  it("shows empty selected state when no segment is chosen", () => {
    render(<SegmentInsightPanel segments={segments} preview={null} />);

    expect(screen.getByText("No segment selected")).toBeInTheDocument();
  });

  it("shows loading copy while catalog loads", () => {
    render(<SegmentInsightPanel segments={[]} preview={null} isLoading />);

    expect(screen.getByText("Loading segment catalog insights…")).toBeInTheDocument();
  });
});
