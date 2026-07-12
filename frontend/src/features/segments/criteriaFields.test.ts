import { describe, expect, it } from "vitest";
import {
  SEGMENT_FIELD_OPTIONS,
  SEGMENT_JOIN_OPERATORS,
  countCriteriaRows,
  describeCriteriaRow,
  emptyCriteriaRow,
  fieldHint,
  fieldsByCategory,
  formatFieldLabel,
  formatOperatorLabel,
  operatorsForField,
} from "@/features/segments/criteriaFields";

describe("segment criteria field catalog", () => {
  it("covers KB segmentation filter domains", () => {
    const names = SEGMENT_FIELD_OPTIONS.map((field) => field.fieldName);
    expect(names).toEqual(
      expect.arrayContaining([
        "age_group",
        "city",
        "country",
        "customer_type",
        "product_type",
        "payment_status",
        "status",
        "consent_status",
        "expiring_within_months",
        "is_expiring",
      ]),
    );
  });

  it("groups fields by category", () => {
    const groups = fieldsByCategory();
    expect(groups.map((group) => group.category)).toContain("Consent");
    expect(groups.find((group) => group.category === "Location")?.fields).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ fieldName: "city" }),
        expect.objectContaining({ fieldName: "country" }),
      ]),
    );
  });

  it("returns suggested operators for boolean-style fields", () => {
    expect(operatorsForField("do_not_contact")).toEqual(["EQUALS", "NOT_EQUALS"]);
    expect(operatorsForField("city").length).toBeGreaterThan(2);
  });

  it("formats field and operator labels", () => {
    expect(formatFieldLabel("age_group")).toBe("Age group");
    expect(formatFieldLabel("unknown_field")).toBe("unknown_field");
    expect(formatOperatorLabel("NOT_EQUALS")).toBe("Not equals");
    expect(formatOperatorLabel("IN")).toBe("In list");
  });

  it("exposes KB FR-070 age group filter with database value hints", () => {
    const ageGroup = SEGMENT_FIELD_OPTIONS.find((field) => field.fieldName === "age_group");
    expect(ageGroup).toMatchObject({
      label: "Age group",
      category: "Demographics",
      fieldName: "age_group",
    });
    expect(ageGroup?.hint).toMatch(/MINOR/);
    expect(ageGroup?.hint).toMatch(/18_25/);
    expect(ageGroup?.hint).toMatch(/26_40/);
    expect(ageGroup?.hint).toMatch(/41_60/);
    expect(ageGroup?.hint).toMatch(/60_PLUS/);
  });

  it("exposes KB FR-071 location filters city, country, and address_line", () => {
    const names = SEGMENT_FIELD_OPTIONS.map((field) => field.fieldName);
    expect(names).toEqual(expect.arrayContaining(["city", "country", "address_line"]));

    const city = SEGMENT_FIELD_OPTIONS.find((field) => field.fieldName === "city");
    expect(city).toMatchObject({
      label: "City",
      category: "Location",
    });
    expect(city?.hint).toMatch(/city/i);
    expect(operatorsForField("city")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "CONTAINS", "IN"]),
    );

    const locationGroup = fieldsByCategory().find((group) => group.category === "Location");
    expect(locationGroup?.fields.map((field) => field.fieldName)).toEqual(
      expect.arrayContaining(["city", "country", "address_line"]),
    );
  });

  it("exposes KB FR-073 product ownership filters with type and status hints", () => {
    const names = SEGMENT_FIELD_OPTIONS.map((field) => field.fieldName);
    expect(names).toEqual(
      expect.arrayContaining(["product_type", "product_id", "ownership_status"]),
    );

    const productType = SEGMENT_FIELD_OPTIONS.find((field) => field.fieldName === "product_type");
    expect(productType).toMatchObject({
      label: "Product type",
      category: "Product ownership",
    });
    expect(productType?.hint).toMatch(/LIFE_INSURANCE/);
    expect(productType?.hint).toMatch(/HOMEOWNER_INSURANCE/);
    expect(productType?.hint).toMatch(/INVESTMENT_FUND/);
    expect(operatorsForField("product_type")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "IN"]),
    );
    expect(operatorsForField("ownership_status")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "IN"]),
    );

    const ownershipGroup = fieldsByCategory().find(
      (group) => group.category === "Product ownership",
    );
    expect(ownershipGroup?.fields.map((field) => field.fieldName)).toEqual(
      expect.arrayContaining(["product_type", "product_id", "ownership_status"]),
    );
  });

  it("exposes KB FR-074 payment history filters with status and numeric fields", () => {
    const names = SEGMENT_FIELD_OPTIONS.map((field) => field.fieldName);
    expect(names).toEqual(
      expect.arrayContaining([
        "payment_status",
        "payment_history",
        "default_risk",
        "reminder_count",
        "days_overdue",
      ]),
    );

    const paymentStatus = SEGMENT_FIELD_OPTIONS.find(
      (field) => field.fieldName === "payment_status",
    );
    expect(paymentStatus).toMatchObject({
      label: "Payment status",
      category: "Payment history",
    });
    expect(paymentStatus?.hint).toMatch(/DUE/);
    expect(paymentStatus?.hint).toMatch(/PAID/);
    expect(paymentStatus?.hint).toMatch(/OVERDUE/);
    expect(paymentStatus?.hint).toMatch(/DEFAULT_RISK/);
    expect(operatorsForField("payment_status")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "IN"]),
    );
    expect(operatorsForField("reminder_count")).toEqual(
      expect.arrayContaining(["EQUALS", "BEFORE", "AFTER", "BETWEEN"]),
    );
    expect(operatorsForField("days_overdue")).toEqual(
      expect.arrayContaining(["EQUALS", "BEFORE", "AFTER", "BETWEEN"]),
    );

    const paymentGroup = fieldsByCategory().find((group) => group.category === "Payment history");
    expect(paymentGroup?.fields.map((field) => field.fieldName)).toEqual(
      expect.arrayContaining([
        "payment_status",
        "payment_history",
        "default_risk",
        "reminder_count",
        "days_overdue",
      ]),
    );
  });

  it("exposes KB consent status filters with status, type, and opt-out fields", () => {
    const names = SEGMENT_FIELD_OPTIONS.map((field) => field.fieldName);
    expect(names).toEqual(
      expect.arrayContaining([
        "consent_status",
        "consent_type",
        "has_valid_marketing_consent",
        "opt_out",
        "guardian_consent",
      ]),
    );

    const consentStatus = SEGMENT_FIELD_OPTIONS.find(
      (field) => field.fieldName === "consent_status",
    );
    expect(consentStatus).toMatchObject({
      label: "Consent status",
      category: "Consent",
    });
    expect(consentStatus?.hint).toMatch(/GIVEN/);
    expect(consentStatus?.hint).toMatch(/WITHDRAWN/);
    expect(consentStatus?.hint).toMatch(/REQUIRED/);
    expect(consentStatus?.hint).toMatch(/EXPIRED/);
    expect(consentStatus?.hint).toMatch(/REJECTED/);
    expect(operatorsForField("consent_status")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "IN"]),
    );
    expect(operatorsForField("consent_type")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "IN"]),
    );
    expect(operatorsForField("opt_out")).toEqual(expect.arrayContaining(["EQUALS", "NOT_EQUALS"]));

    const consentGroup = fieldsByCategory().find((group) => group.category === "Consent");
    expect(consentGroup?.fields.map((field) => field.fieldName)).toEqual(
      expect.arrayContaining([
        "consent_status",
        "consent_type",
        "has_valid_marketing_consent",
        "opt_out",
        "guardian_consent",
      ]),
    );
  });

  it("exposes KB FR-076 product expiration filters with 3/6/12 month windows", () => {
    const names = SEGMENT_FIELD_OPTIONS.map((field) => field.fieldName);
    expect(names).toEqual(
      expect.arrayContaining(["expiring_within_months", "expiration_date", "is_expiring"]),
    );

    const expiringWithin = SEGMENT_FIELD_OPTIONS.find(
      (field) => field.fieldName === "expiring_within_months",
    );
    expect(expiringWithin).toMatchObject({
      label: "Expiring within months",
      category: "Product expiration",
    });
    expect(expiringWithin?.hint).toMatch(/3/);
    expect(expiringWithin?.hint).toMatch(/6/);
    expect(expiringWithin?.hint).toMatch(/12/);
    expect(operatorsForField("expiring_within_months")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS", "IN", "BEFORE", "AFTER", "BETWEEN"]),
    );
    expect(operatorsForField("expiration_date")).toEqual(
      expect.arrayContaining(["EQUALS", "BEFORE", "AFTER", "BETWEEN"]),
    );
    expect(operatorsForField("is_expiring")).toEqual(
      expect.arrayContaining(["EQUALS", "NOT_EQUALS"]),
    );

    const expirationGroup = fieldsByCategory().find(
      (group) => group.category === "Product expiration",
    );
    expect(expirationGroup?.fields.map((field) => field.fieldName)).toEqual(
      expect.arrayContaining(["expiring_within_months", "expiration_date", "is_expiring"]),
    );
  });

  it("exposes field hints and empty row defaults", () => {
    expect(fieldHint("expiring_within_months")).toMatch(/3, 6, 12/);
    expect(emptyCriteriaRow()).toEqual({
      fieldName: "city",
      operator: "EQUALS",
      value: "",
      logicalGroup: "",
      joinOperator: "AND",
    });
  });

  it("counts only complete criteria rows", () => {
    expect(
      countCriteriaRows([
        { fieldName: "city", operator: "EQUALS", value: "Munich" },
        { fieldName: "city", operator: "EQUALS", value: "  " },
        { fieldName: "", operator: "EQUALS", value: "Berlin" },
      ]),
    ).toBe(1);
  });

  it("describes criteria rows with join language", () => {
    expect(
      describeCriteriaRow(
        { fieldName: "city", operator: "EQUALS", value: "Munich", joinOperator: "AND" },
        0,
      ),
    ).toBe("Where City equals Munich");
    expect(
      describeCriteriaRow(
        {
          fieldName: "customer_type",
          operator: "EQUALS",
          value: "PROSPECT",
          joinOperator: "OR",
        },
        1,
      ),
    ).toBe("OR Customer type equals PROSPECT");
  });

  it("defaults empty criteria rows and later AND joins for KB FR-078", () => {
    expect(emptyCriteriaRow().joinOperator).toBe("AND");
    expect(SEGMENT_JOIN_OPERATORS).toContain("AND");
    expect(
      describeCriteriaRow(
        {
          fieldName: "country",
          operator: "EQUALS",
          value: "Germany",
          joinOperator: "AND",
        },
        1,
      ),
    ).toBe("AND Country equals Germany");
  });

  it("describes multi-criterion AND chains for KB item 196 conjunctive audiences", () => {
    const rows = [
      {
        fieldName: "city",
        operator: "EQUALS" as const,
        value: "Munich",
        joinOperator: "AND" as const,
      },
      {
        fieldName: "customer_type",
        operator: "EQUALS" as const,
        value: "PROSPECT",
        joinOperator: "AND" as const,
      },
      {
        fieldName: "country",
        operator: "EQUALS" as const,
        value: "Germany",
        joinOperator: "AND" as const,
      },
    ];
    expect(describeCriteriaRow(rows[0], 0)).toBe("Where City equals Munich");
    expect(describeCriteriaRow(rows[1], 1)).toBe("AND Customer type equals PROSPECT");
    expect(describeCriteriaRow(rows[2], 2)).toBe("AND Country equals Germany");
    expect(countCriteriaRows(rows)).toBe(3);
    expect(SEGMENT_JOIN_OPERATORS[0]).toBe("AND");
  });

  it("describes multi-criterion OR chains for KB item 197 disjunctive audiences", () => {
    const rows = [
      {
        fieldName: "city",
        operator: "EQUALS" as const,
        value: "Munich",
        joinOperator: "AND" as const,
      },
      {
        fieldName: "city",
        operator: "EQUALS" as const,
        value: "Berlin",
        joinOperator: "OR" as const,
      },
      {
        fieldName: "city",
        operator: "EQUALS" as const,
        value: "Hamburg",
        joinOperator: "OR" as const,
      },
    ];
    expect(describeCriteriaRow(rows[0], 0)).toBe("Where City equals Munich");
    expect(describeCriteriaRow(rows[1], 1)).toBe("OR City equals Berlin");
    expect(describeCriteriaRow(rows[2], 2)).toBe("OR City equals Hamburg");
    expect(countCriteriaRows(rows)).toBe(3);
    expect(SEGMENT_JOIN_OPERATORS).toContain("OR");
  });
});
