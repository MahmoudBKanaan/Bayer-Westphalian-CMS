import type { SegmentCriteriaPayload, SegmentJoinOperator, SegmentOperator } from "@/api/segments";
import {
  SEGMENT_FIELD_OPTIONS,
  SEGMENT_JOIN_OPERATORS,
  SEGMENT_OPERATORS,
  describeCriteriaRow,
  emptyCriteriaRow,
  fieldHint,
  formatFieldLabel,
  formatOperatorLabel,
  operatorsForField,
} from "@/features/segments/criteriaFields";

export type SegmentCriteriaBuilderProps = {
  criteria: SegmentCriteriaPayload[];
  disabled?: boolean;
  idPrefix?: string;
  onChange: (criteria: SegmentCriteriaPayload[]) => void;
};

export function SegmentCriteriaBuilder({
  criteria,
  disabled = false,
  idPrefix = "criteria",
  onChange,
}: SegmentCriteriaBuilderProps) {
  const rows = criteria.length === 0 ? [] : criteria;

  const updateRow = (index: number, patch: Partial<SegmentCriteriaPayload>) => {
    const next = rows.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row));
    onChange(next);
  };

  const addRow = () => {
    onChange([...rows, emptyCriteriaRow()]);
  };

  const removeRow = (index: number) => {
    onChange(rows.filter((_, rowIndex) => rowIndex !== index));
  };

  const moveRow = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= rows.length) {
      return;
    }
    const next = [...rows];
    const [removed] = next.splice(index, 1);
    next.splice(target, 0, removed);
    onChange(next);
  };

  return (
    <section className="criteria-builder" aria-labelledby={`${idPrefix}-heading`}>
      <div className="section-heading">
        <h3 id={`${idPrefix}-heading`}>Criteria builder</h3>
        <span>
          {rows.length === 0
            ? "No filters — matches all active profiles"
            : `${rows.length} filter${rows.length === 1 ? "" : "s"}`}
        </span>
      </div>
      <p className="table-state">
        Combine demographics, product ownership, payment history, consent, and product-expiration
        fields with AND/OR joins (KB FR-070–078).
      </p>

      {rows.length === 0 ? (
        <div className="state-panel" role="status">
          <strong>No criteria yet</strong>
          <p>
            Add a filter row to narrow the audience. Empty criteria lists are allowed for drafts.
          </p>
        </div>
      ) : (
        <ol className="criteria-builder-list" aria-label="Segment criteria rows">
          {rows.map((row, index) => {
            const suggested = operatorsForField(row.fieldName);
            const operators = suggested.includes(row.operator) ? suggested : SEGMENT_OPERATORS;
            const hint = fieldHint(row.fieldName);

            return (
              <li key={`${idPrefix}-row-${index}`} className="criteria-builder-row">
                <div className="criteria-builder-row-header">
                  <span className="eyebrow">Rule {index + 1}</span>
                  <span className="table-secondary-text">{describeCriteriaRow(row, index)}</span>
                </div>

                <div className="criteria-builder-grid">
                  {index > 0 ? (
                    <label>
                      Join with previous
                      <select
                        aria-label={`Join operator for rule ${index + 1}`}
                        value={row.joinOperator ?? "AND"}
                        disabled={disabled}
                        onChange={(event) =>
                          updateRow(index, {
                            joinOperator: event.target.value as SegmentJoinOperator,
                          })
                        }
                      >
                        {SEGMENT_JOIN_OPERATORS.map((join) => (
                          <option key={join} value={join}>
                            {join}
                          </option>
                        ))}
                      </select>
                    </label>
                  ) : (
                    <div className="criteria-builder-static">
                      <span className="field-caption">Join with previous</span>
                      <strong>Where (first rule)</strong>
                    </div>
                  )}

                  <label>
                    Field
                    <select
                      aria-label={`Field for rule ${index + 1}`}
                      value={row.fieldName}
                      disabled={disabled}
                      onChange={(event) => {
                        const fieldName = event.target.value;
                        const nextOperators = operatorsForField(fieldName);
                        updateRow(index, {
                          fieldName,
                          operator: nextOperators.includes(row.operator)
                            ? row.operator
                            : nextOperators[0],
                        });
                      }}
                    >
                      {SEGMENT_FIELD_OPTIONS.map((field) => (
                        <option key={field.fieldName} value={field.fieldName}>
                          {field.category}: {field.label}
                        </option>
                      ))}
                      {!SEGMENT_FIELD_OPTIONS.some((field) => field.fieldName === row.fieldName) &&
                      row.fieldName.trim() !== "" ? (
                        <option value={row.fieldName}>Custom: {row.fieldName}</option>
                      ) : null}
                    </select>
                  </label>

                  <label>
                    Operator
                    <select
                      aria-label={`Operator for rule ${index + 1}`}
                      value={row.operator}
                      disabled={disabled}
                      onChange={(event) =>
                        updateRow(index, {
                          operator: event.target.value as SegmentOperator,
                        })
                      }
                    >
                      {operators.map((operator) => (
                        <option key={operator} value={operator}>
                          {formatOperatorLabel(operator)}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label>
                    Value
                    <input
                      aria-label={`Value for rule ${index + 1}`}
                      value={row.value}
                      disabled={disabled}
                      placeholder={hint ?? "Filter value"}
                      onChange={(event) => updateRow(index, { value: event.target.value })}
                    />
                  </label>

                  <label>
                    Logical group
                    <input
                      aria-label={`Logical group for rule ${index + 1}`}
                      value={row.logicalGroup ?? ""}
                      disabled={disabled}
                      placeholder="Optional group name"
                      maxLength={50}
                      onChange={(event) => updateRow(index, { logicalGroup: event.target.value })}
                    />
                  </label>
                </div>

                {hint ? <p className="field-hint">Hint: {hint}</p> : null}

                <div className="button-row criteria-builder-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={disabled || index === 0}
                    onClick={() => moveRow(index, -1)}
                    aria-label={`Move rule ${index + 1} up`}
                  >
                    Move up
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={disabled || index === rows.length - 1}
                    onClick={() => moveRow(index, 1)}
                    aria-label={`Move rule ${index + 1} down`}
                  >
                    Move down
                  </button>
                  <button
                    type="button"
                    className="danger-button"
                    disabled={disabled}
                    onClick={() => removeRow(index)}
                    aria-label={`Remove rule ${index + 1}`}
                  >
                    Remove
                  </button>
                </div>
              </li>
            );
          })}
        </ol>
      )}

      <div className="button-row">
        <button type="button" className="secondary-button" disabled={disabled} onClick={addRow}>
          Add criterion
        </button>
      </div>

      {rows.length > 0 ? (
        <div className="criteria-builder-summary" aria-label="Criteria summary">
          <span className="field-caption">Rule summary</span>
          <ul>
            {rows.map((row, index) => (
              <li key={`${idPrefix}-summary-${index}`}>
                {describeCriteriaRow(row, index)}
                {row.logicalGroup?.trim() ? ` (group: ${row.logicalGroup.trim()})` : ""}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}

export function formatCriteriaFieldOptionLabel(fieldName: string): string {
  return formatFieldLabel(fieldName);
}
