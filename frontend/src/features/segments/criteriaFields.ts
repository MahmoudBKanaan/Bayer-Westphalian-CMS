import type { SegmentCriteriaPayload, SegmentJoinOperator, SegmentOperator } from "@/api/segments";

export type SegmentFieldCategory =
  | "Demographics"
  | "Location"
  | "Customer type"
  | "Product ownership"
  | "Payment history"
  | "Behavior"
  | "Consent"
  | "Product expiration";

export type SegmentFieldOption = {
  fieldName: string;
  label: string;
  category: SegmentFieldCategory;
  hint?: string;
  suggestedOperators?: SegmentOperator[];
};

/** KB FR-070–076 segment filter fields exposed in the criteria builder. */
export const SEGMENT_FIELD_OPTIONS: SegmentFieldOption[] = [
  {
    fieldName: "age_group",
    label: "Age group",
    category: "Demographics",
    hint: "MINOR, 18_25, 26_40, 41_60, 60_PLUS",
  },
  {
    fieldName: "city",
    label: "City",
    category: "Location",
    hint: "Customer city (EQUALS, CONTAINS, IN)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "CONTAINS", "IN"],
  },
  {
    fieldName: "country",
    label: "Country",
    category: "Location",
    hint: "Customer country",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "CONTAINS", "IN"],
  },
  {
    fieldName: "address_line",
    label: "Address line",
    category: "Location",
    hint: "Street / address line",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "CONTAINS", "IN"],
  },
  {
    fieldName: "customer_type",
    label: "Customer type",
    category: "Customer type",
    hint: "CUSTOMER, PROSPECT, BENEFICIARY",
  },
  {
    fieldName: "product_type",
    label: "Product type",
    category: "Product ownership",
    hint: "HOMEOWNER_INSURANCE, LIFE_INSURANCE, INVESTMENT_FUND, HEALTH_INSURANCE, AUTO_INSURANCE, OTHER",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "product_id",
    label: "Product ID",
    category: "Product ownership",
    hint: "UUID of owned product",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "ownership_status",
    label: "Ownership status",
    category: "Product ownership",
    hint: "ACTIVE, EXPIRED, CANCELLED",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "payment_status",
    label: "Payment status",
    category: "Payment history",
    hint: "DUE, PAID, OVERDUE, DEFAULT_RISK",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "payment_history",
    label: "Payment history",
    category: "Payment history",
    hint: "Alias for payment_status (DUE, PAID, OVERDUE, DEFAULT_RISK)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "default_risk",
    label: "Default risk",
    category: "Payment history",
    hint: "true / false",
    suggestedOperators: ["EQUALS", "NOT_EQUALS"],
  },
  {
    fieldName: "reminder_count",
    label: "Reminder count",
    category: "Payment history",
    hint: "Max reminders across payment records",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "BEFORE", "AFTER", "BETWEEN"],
  },
  {
    fieldName: "days_overdue",
    label: "Days overdue",
    category: "Payment history",
    hint: "Max days overdue across unpaid payments",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "BEFORE", "AFTER", "BETWEEN"],
  },
  {
    fieldName: "status",
    label: "Customer status",
    category: "Behavior",
    hint: "ACTIVE, INTERESTED, UNINTERESTED, CONVERTED, …",
  },
  { fieldName: "interest", label: "Interest", category: "Behavior" },
  { fieldName: "source", label: "Source", category: "Behavior" },
  {
    fieldName: "do_not_contact",
    label: "Do not contact",
    category: "Behavior",
    hint: "true / false",
    suggestedOperators: ["EQUALS", "NOT_EQUALS"],
  },
  {
    fieldName: "consent_status",
    label: "Consent status",
    category: "Consent",
    hint: "GIVEN, WITHDRAWN, REQUIRED, EXPIRED, REJECTED",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "consent_type",
    label: "Consent type",
    category: "Consent",
    hint: "MARKETING_EMAIL, MARKETING_PHONE, MARKETING_SMS, GUARDIAN, DATA_PROCESSING",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN"],
  },
  {
    fieldName: "has_valid_marketing_consent",
    label: "Valid marketing consent",
    category: "Consent",
    hint: "true / false (email/phone/SMS, not opted out)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS"],
  },
  {
    fieldName: "opt_out",
    label: "Marketing opt-out",
    category: "Consent",
    hint: "true / false (WITHDRAWN or REJECTED marketing)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS"],
  },
  {
    fieldName: "guardian_consent",
    label: "Guardian consent",
    category: "Consent",
    hint: "true / false (valid GUARDIAN consent)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS"],
  },
  {
    fieldName: "expiring_within_months",
    label: "Expiring within months",
    category: "Product expiration",
    hint: "KB windows: 3, 6, 12 (months until ownership expiration)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "IN", "BEFORE", "AFTER", "BETWEEN"],
  },
  {
    fieldName: "expiration_date",
    label: "Expiration date",
    category: "Product expiration",
    hint: "ISO date YYYY-MM-DD",
    suggestedOperators: ["EQUALS", "NOT_EQUALS", "BEFORE", "AFTER", "BETWEEN"],
  },
  {
    fieldName: "is_expiring",
    label: "Is expiring (12 months)",
    category: "Product expiration",
    hint: "true / false (any active ownership expires within 12 months)",
    suggestedOperators: ["EQUALS", "NOT_EQUALS"],
  },
];

export const SEGMENT_OPERATORS: SegmentOperator[] = [
  "EQUALS",
  "NOT_EQUALS",
  "CONTAINS",
  "IN",
  "BETWEEN",
  "BEFORE",
  "AFTER",
];

export const SEGMENT_JOIN_OPERATORS: SegmentJoinOperator[] = ["AND", "OR"];

export function emptyCriteriaRow(): SegmentCriteriaPayload {
  return {
    fieldName: "city",
    operator: "EQUALS",
    value: "",
    logicalGroup: "",
    joinOperator: "AND",
  };
}

export function formatFieldLabel(fieldName: string): string {
  const match = SEGMENT_FIELD_OPTIONS.find((option) => option.fieldName === fieldName);
  return match?.label ?? fieldName;
}

export function formatOperatorLabel(operator: SegmentOperator): string {
  switch (operator) {
    case "EQUALS":
      return "Equals";
    case "NOT_EQUALS":
      return "Not equals";
    case "CONTAINS":
      return "Contains";
    case "IN":
      return "In list";
    case "BETWEEN":
      return "Between";
    case "BEFORE":
      return "Before";
    case "AFTER":
      return "After";
    default:
      return operator;
  }
}

export function operatorsForField(fieldName: string): SegmentOperator[] {
  const option = SEGMENT_FIELD_OPTIONS.find((entry) => entry.fieldName === fieldName);
  return option?.suggestedOperators ?? SEGMENT_OPERATORS;
}

export function fieldHint(fieldName: string): string | undefined {
  return SEGMENT_FIELD_OPTIONS.find((entry) => entry.fieldName === fieldName)?.hint;
}

export function countCriteriaRows(criteria: SegmentCriteriaPayload[]): number {
  return criteria.filter((row) => row.fieldName.trim() !== "" && row.value.trim() !== "").length;
}

export function describeCriteriaRow(row: SegmentCriteriaPayload, index: number): string {
  const join = index === 0 ? "Where" : row.joinOperator === "OR" ? "OR" : "AND";
  const field = formatFieldLabel(row.fieldName || "field");
  const operator = formatOperatorLabel(row.operator);
  const value = row.value.trim() === "" ? "…" : row.value.trim();
  return `${join} ${field} ${operator.toLowerCase()} ${value}`;
}

export function fieldsByCategory(): Array<{
  category: SegmentFieldCategory;
  fields: SegmentFieldOption[];
}> {
  const categories: SegmentFieldCategory[] = [
    "Demographics",
    "Location",
    "Customer type",
    "Product ownership",
    "Payment history",
    "Behavior",
    "Consent",
    "Product expiration",
  ];
  return categories
    .map((category) => ({
      category,
      fields: SEGMENT_FIELD_OPTIONS.filter((field) => field.category === category),
    }))
    .filter((group) => group.fields.length > 0);
}
