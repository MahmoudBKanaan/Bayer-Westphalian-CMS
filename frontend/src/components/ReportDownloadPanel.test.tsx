import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import {
  CampaignReportDownloadActions,
  ReportDownloadPanel,
} from "@/components/ReportDownloadPanel";

const campaignOptions = [
  {
    id: "50000000-0000-0000-0000-000000000445",
    name: "Spring Life Drive",
    label: "Spring Life Drive (ACTIVE)",
  },
];

describe("ReportDownloadPanel (item 445)", () => {
  it("renders campaign download controls and last download summary", async () => {
    const user = userEvent.setup();
    const onDownloadCsv = vi.fn();
    const onDownloadPdf = vi.fn();
    const onCampaignChange = vi.fn();

    render(
      <ReportDownloadPanel
        campaignOptions={campaignOptions}
        selectedCampaignId={campaignOptions[0].id}
        onCampaignChange={onCampaignChange}
        isCsvPending={false}
        isPdfPending={false}
        onDownloadCsv={onDownloadCsv}
        onDownloadPdf={onDownloadPdf}
        lastDownload={{
          filename: "Spring-Life-Drive.csv",
          exportType: "CSV",
          campaignLabel: "Spring Life Drive (ACTIVE)",
          completedAt: "2026-07-11T10:00:00Z",
        }}
      />,
    );

    expect(screen.getByRole("heading", { name: "Report download" })).toBeInTheDocument();
    expect(screen.getByLabelText("Select campaign for report download")).toBeInTheDocument();
    expect(screen.getByLabelText("Download campaign CSV report")).toBeEnabled();
    expect(screen.getByLabelText("Download campaign PDF report")).toBeEnabled();
    expect(screen.getByLabelText("Last report download summary")).toBeInTheDocument();
    expect(screen.getByText("Spring-Life-Drive.csv")).toBeInTheDocument();

    await user.click(screen.getByLabelText("Download campaign CSV report"));
    expect(onDownloadCsv).toHaveBeenCalledTimes(1);
    await user.click(screen.getByLabelText("Download campaign PDF report"));
    expect(onDownloadPdf).toHaveBeenCalledTimes(1);
  });

  it("disables download actions when no campaign is selected", () => {
    render(
      <ReportDownloadPanel
        campaignOptions={[]}
        selectedCampaignId=""
        onCampaignChange={vi.fn()}
        isCsvPending={false}
        isPdfPending={false}
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        lastDownload={null}
        disabledReason="No campaigns are available for report download."
      />,
    );

    expect(screen.getByLabelText("Download campaign CSV report")).toBeDisabled();
    expect(screen.getByLabelText("Download campaign PDF report")).toBeDisabled();
    expect(
      screen.getByText("No campaigns are available for report download."),
    ).toBeInTheDocument();
  });

  it("shows pending labels while downloading", () => {
    render(
      <ReportDownloadPanel
        campaignOptions={campaignOptions}
        selectedCampaignId={campaignOptions[0].id}
        onCampaignChange={vi.fn()}
        isCsvPending
        isPdfPending={false}
        onDownloadCsv={vi.fn()}
        onDownloadPdf={vi.fn()}
        lastDownload={null}
      />,
    );

    expect(screen.getByLabelText("Download campaign CSV report")).toHaveTextContent(
      "Downloading CSV…",
    );
    expect(screen.getByLabelText("Download campaign PDF report")).toBeDisabled();
  });
});

describe("CampaignReportDownloadActions (item 445)", () => {
  it("renders compact CSV and PDF download buttons for a campaign", async () => {
    const user = userEvent.setup();
    const onDownloadCsv = vi.fn();
    const onDownloadPdf = vi.fn();

    render(
      <CampaignReportDownloadActions
        campaignId={campaignOptions[0].id}
        campaignName="Spring Life Drive"
        canDownload
        isCsvPending={false}
        isPdfPending={false}
        onDownloadCsv={onDownloadCsv}
        onDownloadPdf={onDownloadPdf}
      />,
    );

    expect(screen.getByLabelText("Report downloads for Spring Life Drive")).toBeInTheDocument();
    await user.click(screen.getByLabelText("Download CSV report for Spring Life Drive"));
    await user.click(screen.getByLabelText("Download PDF report for Spring Life Drive"));
    expect(onDownloadCsv).toHaveBeenCalledTimes(1);
    expect(onDownloadPdf).toHaveBeenCalledTimes(1);
  });
});
