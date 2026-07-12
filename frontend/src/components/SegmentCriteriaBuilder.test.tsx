import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SegmentCriteriaBuilder } from "@/components/SegmentCriteriaBuilder";
import type { SegmentCriteriaPayload } from "@/api/segments";

describe("SegmentCriteriaBuilder", () => {
  it("renders empty state and adds a criterion row", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();

    render(<SegmentCriteriaBuilder criteria={[]} onChange={onChange} idPrefix="create" />);

    expect(screen.getByRole("heading", { name: "Criteria builder" })).toBeInTheDocument();
    expect(screen.getByText(/No criteria yet/i)).toBeInTheDocument();
    expect(screen.getByText(/No filters — matches all active profiles/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Add criterion" }));

    expect(onChange).toHaveBeenCalledWith([
      {
        fieldName: "city",
        operator: "EQUALS",
        value: "",
        logicalGroup: "",
        joinOperator: "AND",
      },
    ]);
  });

  it("edits field, operator, value, group, and join for existing rows", async () => {
    const user = userEvent.setup();
    let criteria: SegmentCriteriaPayload[] = [
      {
        fieldName: "city",
        operator: "EQUALS",
        value: "Munich",
        logicalGroup: "location",
        joinOperator: "AND",
      },
      {
        fieldName: "customer_type",
        operator: "EQUALS",
        value: "PROSPECT",
        logicalGroup: "",
        joinOperator: "AND",
      },
    ];
    const onChange = vi.fn((next: SegmentCriteriaPayload[]) => {
      criteria = next;
    });

    const { rerender } = render(
      <SegmentCriteriaBuilder criteria={criteria} onChange={onChange} idPrefix="edit" />,
    );

    expect(screen.getByLabelText("Segment criteria rows")).toBeInTheDocument();
    // Summary appears in row caption and criteria summary list.
    expect(screen.getAllByText(/Where City equals Munich/i).length).toBeGreaterThan(0);

    await user.selectOptions(screen.getByLabelText("Field for rule 1"), "age_group");
    expect(onChange).toHaveBeenLastCalledWith([
      expect.objectContaining({ fieldName: "age_group" }),
      expect.objectContaining({ fieldName: "customer_type" }),
    ]);

    rerender(<SegmentCriteriaBuilder criteria={criteria} onChange={onChange} idPrefix="edit" />);

    await user.clear(screen.getByLabelText("Value for rule 1"));
    await user.type(screen.getByLabelText("Value for rule 1"), "26_40");
    expect(onChange).toHaveBeenCalled();

    await user.selectOptions(screen.getByLabelText("Join operator for rule 2"), "OR");
    expect(onChange).toHaveBeenLastCalledWith([
      expect.any(Object),
      expect.objectContaining({ joinOperator: "OR" }),
    ]);

    // Controlled inputs only see prop value; set the full group in one change event.
    fireEvent.change(screen.getByLabelText("Logical group for rule 2"), {
      target: { value: "audience" },
    });
    expect(onChange).toHaveBeenLastCalledWith([
      expect.any(Object),
      expect.objectContaining({ logicalGroup: "audience" }),
    ]);
  });

  it("removes and reorders criteria rows", async () => {
    const user = userEvent.setup();
    let criteria: SegmentCriteriaPayload[] = [
      {
        fieldName: "city",
        operator: "EQUALS",
        value: "Munich",
        joinOperator: "AND",
      },
      {
        fieldName: "city",
        operator: "EQUALS",
        value: "Berlin",
        joinOperator: "OR",
      },
    ];
    const onChange = vi.fn((next: SegmentCriteriaPayload[]) => {
      criteria = next;
    });

    const { rerender } = render(<SegmentCriteriaBuilder criteria={criteria} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "Move rule 2 up" }));
    expect(onChange).toHaveBeenLastCalledWith([
      expect.objectContaining({ value: "Berlin", joinOperator: "OR" }),
      expect.objectContaining({ value: "Munich", joinOperator: "AND" }),
    ]);

    rerender(<SegmentCriteriaBuilder criteria={criteria} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "Remove rule 1" }));
    expect(onChange).toHaveBeenLastCalledWith([expect.objectContaining({ value: "Munich" })]);
  });

  it("shows a criteria summary list", () => {
    render(
      <SegmentCriteriaBuilder
        criteria={[
          {
            fieldName: "city",
            operator: "EQUALS",
            value: "Munich",
            logicalGroup: "location",
            joinOperator: "AND",
          },
          {
            fieldName: "opt_out",
            operator: "EQUALS",
            value: "false",
            joinOperator: "AND",
          },
        ]}
        onChange={vi.fn()}
      />,
    );

    const summary = screen.getByLabelText("Criteria summary");
    expect(
      within(summary).getByText(/Where City equals Munich \(group: location\)/i),
    ).toBeInTheDocument();
    expect(within(summary).getByText(/AND Marketing opt-out equals false/i)).toBeInTheDocument();
  });

  it("disables interactive controls when disabled", () => {
    render(
      <SegmentCriteriaBuilder
        criteria={[
          {
            fieldName: "city",
            operator: "EQUALS",
            value: "Munich",
            joinOperator: "AND",
          },
        ]}
        disabled
        onChange={vi.fn()}
      />,
    );

    expect(screen.getByLabelText("Field for rule 1")).toBeDisabled();
    expect(screen.getByLabelText("Value for rule 1")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Add criterion" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Remove rule 1" })).toBeDisabled();
  });
});
