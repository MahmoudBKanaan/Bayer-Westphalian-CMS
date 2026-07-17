import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/api/client";
import { AuthProvider } from "@/auth/AuthProvider";
import { AUTH_STORAGE_KEYS } from "@/auth/sessionStorageStrategy";
import { DuplicateContactWarningPanel } from "@/components/DuplicateContactWarningPanel";

const riskResult = {
  customerId: "20000000-0000-0000-0000-000000000130",
  campaignId: "50000000-0000-0000-0000-000000000130",
  riskDetected: true,
  warning: "Duplicate-contact risk detected: BR-010 same campaign and BR-011 monthly limit",
  explanation:
    "The customer has already received this campaign and has reached the configured marketing-contact limit.",
  contactsInCurrentMonth: 3,
  monthlyContactLimit: 3,
  sameCampaignAlreadyContacted: true,
  storedRecommendationId: "70000000-0000-0000-0000-000000000130",
};

const clearResult = {
  ...riskResult,
  riskDetected: false,
  warning: "No duplicate-contact risk detected",
  explanation: "No same-campaign contact and contacts are under the monthly limit.",
  contactsInCurrentMonth: 1,
  sameCampaignAlreadyContacted: false,
};

vi.mock("@/api/ai", async () => {
  const actual = await vi.importActual<typeof import("@/api/ai")>("@/api/ai");
  return {
    ...actual,
    generateDuplicateContactWarning: vi.fn(),
  };
});

import { generateDuplicateContactWarning } from "@/api/ai";

const generateMock = vi.mocked(generateDuplicateContactWarning);

describe("DuplicateContactWarningPanel", () => {
  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    generateMock.mockReset();
  });

  it("shows idle state and check button", () => {
    renderPanel();

    expect(screen.getByTestId("duplicate-contact-idle")).toHaveTextContent("Risk not checked");
    expect(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    ).toBeInTheDocument();
  });

  it("calls the API with customer and campaign IDs when checking", async () => {
    generateMock.mockResolvedValue(riskResult);
    renderPanel();

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    await waitFor(() => {
      expect(generateMock).toHaveBeenCalledWith({
        customerId: riskResult.customerId,
        campaignId: riskResult.campaignId,
      });
    });
  });

  it("shows loading state while the request is pending", async () => {
    let resolvePromise: (value: typeof riskResult) => void = () => undefined;
    generateMock.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolvePromise = resolve;
        }),
    );
    renderPanel();

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    expect(screen.getByText("Checking contact history…")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Check Duplicate-Contact Risk" })).toBeDisabled();

    resolvePromise(riskResult);
    expect(await screen.findByTestId("duplicate-contact-result")).toBeInTheDocument();
  });

  it("renders risk-detected result with eligibility notice and no override", async () => {
    generateMock.mockResolvedValue(riskResult);
    renderPanel({
      currentEligibilityStatus: "EXCLUDED",
      currentExclusionReason: "MONTHLY_CONTACT_LIMIT",
    });

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    const result = await screen.findByTestId("duplicate-contact-result");
    expect(result).toHaveAttribute("role", "alert");
    expect(result).toHaveTextContent("Duplicate-contact risk detected");
    expect(result).toHaveTextContent("Ahmed Saleh");
    expect(result).toHaveTextContent("Campaign Test 4");
    expect(result).toHaveTextContent("Yes");
    expect(result).toHaveTextContent("3");
    expect(result).toHaveTextContent(riskResult.explanation);
    expect(screen.getByTestId("eligibility-service-notice")).toHaveTextContent(
      "EligibilityService remains authoritative",
    );
    expect(screen.getByTestId("duplicate-contact-eligibility-blocked")).toHaveTextContent(
      "Blocked by normal eligibility rules",
    );
    expect(screen.queryByRole("button", { name: /override/i })).not.toBeInTheDocument();
  });

  it("renders no-risk result without override action", async () => {
    generateMock.mockResolvedValue(clearResult);
    renderPanel();

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    const result = await screen.findByTestId("duplicate-contact-result");
    expect(result).toHaveAttribute("role", "status");
    expect(result).toHaveTextContent("No duplicate-contact risk detected");
    expect(screen.queryByRole("button", { name: /override/i })).not.toBeInTheDocument();
  });

  it("shows a safe error and retry on failure", async () => {
    generateMock
      .mockRejectedValueOnce(new ApiError(500, "Internal boom"))
      .mockResolvedValueOnce(clearResult);
    renderPanel();

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    expect(await screen.findByTestId("duplicate-contact-error")).toHaveTextContent(
      "The duplicate-contact check could not be completed.",
    );
    expect(screen.queryByText("Internal boom")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByTestId("duplicate-contact-result")).toBeInTheDocument();
  });

  it("shows a forbidden message for 403 responses", async () => {
    generateMock.mockRejectedValue(new ApiError(403, "Forbidden"));
    renderPanel();

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    expect(await screen.findByTestId("duplicate-contact-error")).toHaveTextContent(
      "You do not have permission to generate duplicate-contact warnings.",
    );
  });

  it("shows a safe business-conflict message for 409 responses", async () => {
    generateMock.mockRejectedValue(new ApiError(409, "Internal campaign state details"));
    renderPanel();

    await userEvent.click(
      screen.getByRole("button", { name: "Check Duplicate-Contact Risk" }),
    );

    const error = await screen.findByTestId("duplicate-contact-error");
    expect(error).toHaveTextContent(
      "The duplicate-contact check conflicts with the current campaign or recipient state.",
    );
    expect(error).not.toHaveTextContent("Internal campaign state details");
  });

  it("hides the panel when the user lacks permission", () => {
    renderPanel({ roles: ["EXECUTIVE_VIEWER"] });
    expect(screen.queryByTestId("duplicate-contact-warning-panel")).not.toBeInTheDocument();
  });
});

function renderPanel({
  roles = ["CAMPAIGN_MANAGER"],
  currentEligibilityStatus = "ELIGIBLE",
  currentExclusionReason = null as string | null,
}: {
  roles?: string[];
  currentEligibilityStatus?: string;
  currentExclusionReason?: string | null;
} = {}) {
  const user = {
    id: "10000000-0000-0000-0000-000000000101",
    email: "campaign.manager@bayer-westphalian.test",
    fullName: "Campaign Manager",
    status: "ACTIVE",
    lastLoginAt: "2026-07-09T10:00:00Z",
    roles,
  };
  localStorage.setItem(AUTH_STORAGE_KEYS.accessToken, createAccessToken(roles));
  localStorage.setItem(AUTH_STORAGE_KEYS.refreshToken, "refresh-token");
  localStorage.setItem(AUTH_STORAGE_KEYS.currentUser, JSON.stringify(user));

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <DuplicateContactWarningPanel
          customerId={riskResult.customerId}
          customerName="Ahmed Saleh"
          campaignId={riskResult.campaignId}
          campaignName="Campaign Test 4"
          currentEligibilityStatus={currentEligibilityStatus}
          currentExclusionReason={currentExclusionReason}
        />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function createAccessToken(roles: string[]) {
  const payload = btoa(JSON.stringify({ roles }))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
  return `header.${payload}.signature`;
}
