/**
 * Frontend component test inventory (KB item 595 / Sprint 15).
 *
 * Smoke-renders shared UI primitives used across dashboards, forms, badges,
 * notifications, and charts so component regressions are caught outside page tests.
 */
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AiExplanationDisplay } from "@/components/AiExplanationDisplay";
import { AuditActionBadge } from "@/components/AuditActionBadge";
import { CampaignStatusBadge } from "@/components/CampaignStatusBadge";
import {
  EngagementMixPieChart,
  FinancialLineChart,
  MultiSeriesBarChart,
  NamedCountBarChart,
} from "@/components/charts";
import { ConfirmationDialog } from "@/components/ConfirmationDialog";
import { ConsentStatusBadge } from "@/components/ConsentStatusBadge";
import { CustomerStatusBadge } from "@/components/CustomerStatusBadge";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import { ExclusionReasonSummaryPanel } from "@/components/ExclusionReasonSummaryPanel";
import { FormValidationMessage } from "@/components/FormValidationMessage";
import { MetricCard } from "@/components/MetricCard";
import { ReminderLevelBadge } from "@/components/ReminderLevelBadge";
import { StatusBadge } from "@/components/StatusBadge";
import { SuccessNotification } from "@/components/SuccessNotification";

describe("frontend component inventory (item 595)", () => {
  it("covers status and domain badges used across workflow screens", () => {
    const { unmount: unmountStatus } = render(<StatusBadge value="Eligible" />);
    expect(screen.getByText("Eligible")).toHaveClass("status-badge");
    unmountStatus();

    const { unmount: unmountCampaign } = render(<CampaignStatusBadge status="APPROVED" />);
    expect(screen.getByText("Approved")).toBeInTheDocument();
    unmountCampaign();

    const { unmount: unmountConsent } = render(<ConsentStatusBadge status="GIVEN" />);
    expect(screen.getByLabelText(/Consent status/i)).toBeInTheDocument();
    unmountConsent();

    const { unmount: unmountCustomer } = render(<CustomerStatusBadge status="ACTIVE" />);
    expect(screen.getByLabelText(/Customer status/i)).toBeInTheDocument();
    unmountCustomer();

    const { unmount: unmountReminder } = render(<ReminderLevelBadge level="GREEN" />);
    expect(screen.getByLabelText(/Reminder level/i)).toBeInTheDocument();
    unmountReminder();

    render(<AuditActionBadge action="APPROVE" />);
    expect(screen.getByLabelText(/Audit action/i)).toBeInTheDocument();
  });

  it("covers feedback and form messaging components", () => {
    const { unmount: unmountEmpty } = render(
      <EmptyState title="Nothing here" description="Add a record to continue." />,
    );
    expect(screen.getByRole("status")).toHaveTextContent("Nothing here");
    unmountEmpty();

    const { unmount: unmountError } = render(
      <ErrorState title="Request failed" description="Try again later." />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Request failed");
    unmountError();

    const { unmount: unmountSuccess } = render(
      <SuccessNotification message="Saved successfully." />,
    );
    expect(screen.getByRole("status")).toHaveTextContent("Saved successfully.");
    unmountSuccess();

    render(<FormValidationMessage id="field-error" message="Name is required." />);
    expect(screen.getByText("Name is required.")).toHaveAttribute("id", "field-error");
  });

  it("covers metric cards, AI explanation, confirmation dialog, and exclusion summary", () => {
    const { unmount: unmountMetric } = render(
      <MetricCard
        label="Open rate"
        value="50.0%"
        detail="Opened ÷ sent"
        tone="engagement"
      />,
    );
    expect(screen.getByLabelText("Open rate: 50.0%")).toBeInTheDocument();
    unmountMetric();

    const { unmount: unmountAi } = render(
      <AiExplanationDisplay
        explanation="Rule-based suggestion for review."
        confidenceScore={80}
        storedRecommendationId="70000000-0000-0000-0000-000000000001"
      />,
    );
    expect(screen.getByLabelText("AI explanation")).toBeInTheDocument();
    expect(screen.getByText(/Rule-based suggestion/i)).toBeInTheDocument();
    unmountAi();

    const { unmount: unmountConfirm } = render(
      <ConfirmationDialog
        id="inventory-confirm"
        title="Confirm action"
        description={<p>Sensitive action requires confirmation.</p>}
        confirmLabel="Confirm"
        onCancel={() => undefined}
        onConfirm={() => undefined}
      />,
    );
    expect(screen.getByRole("dialog", { name: "Confirm action" })).toBeInTheDocument();
    unmountConfirm();

    render(
      <ExclusionReasonSummaryPanel
        reasons={[
          {
            code: "INVALID_CONSENT",
            message: "Customer does not have valid required consent",
            count: 2,
          },
        ]}
        excludedCount={2}
      />,
    );
    expect(screen.getByRole("heading", { name: "Exclusion reason summary" })).toBeInTheDocument();
    expect(screen.getAllByText("Invalid or missing consent").length).toBeGreaterThanOrEqual(1);
  });

  it("covers shared analytics chart shells for bar, pie, and line visualizations", () => {
    const { unmount: unmountBars } = render(
      <MultiSeriesBarChart
        ariaLabel="Inventory multi-series chart"
        data={[{ name: "A", sent: 10, conversions: 1 }]}
        series={[
          { dataKey: "sent", name: "Sent" },
          { dataKey: "conversions", name: "Conversions" },
        ]}
      />,
    );
    expect(screen.getByLabelText("Inventory multi-series chart")).toBeInTheDocument();
    unmountBars();

    const { unmount: unmountNamed } = render(
      <NamedCountBarChart
        ariaLabel="Inventory named count chart"
        data={[{ name: "Total", value: 4 }]}
      />,
    );
    expect(screen.getByLabelText("Inventory named count chart")).toBeInTheDocument();
    unmountNamed();

    const { unmount: unmountPie } = render(
      <EngagementMixPieChart
        ariaLabel="Inventory engagement pie chart"
        data={[
          { name: "Opened", value: 4 },
          { name: "Clicked", value: 2 },
        ]}
      />,
    );
    expect(screen.getByLabelText("Inventory engagement pie chart")).toBeInTheDocument();
    unmountPie();

    render(
      <FinancialLineChart
        ariaLabel="Inventory financial line chart"
        data={[{ name: "Life", cost: 10, revenue: 20, roiPercent: 100 }]}
      />,
    );
    expect(screen.getByLabelText("Inventory financial line chart")).toBeInTheDocument();
  });
});
