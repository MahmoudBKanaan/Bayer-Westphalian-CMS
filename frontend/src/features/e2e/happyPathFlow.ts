/**
 * Playwright happy-path E2E workflow definition (KB Testing Plan / item 597 / NFR-010).
 *
 * Documents the critical UI journey:
 * Login → create customer → consent → campaign → approval → launch
 *
 * Runtime browser steps live under `frontend/tests/e2e/`. This module is shared so
 * unit tests can lock the expected order and labels without starting Playwright.
 */

export type HappyPathStepId =
  | "login"
  | "create-customer"
  | "consent"
  | "campaign"
  | "approval"
  | "launch";

export type HappyPathStepDefinition = {
  id: HappyPathStepId;
  index: number;
  title: string;
  /** Primary UI route used for the step (relative path). */
  route: string;
  /** Short success signal asserted after the step. */
  successSignal: string;
};

/** KB Testing Plan E2E chain (order is part of the acceptance contract). */
export const HAPPY_PATH_STEPS: HappyPathStepDefinition[] = [
  {
    id: "login",
    index: 0,
    title: "Login",
    route: "/login",
    successSignal: "Dashboard",
  },
  {
    id: "create-customer",
    index: 1,
    title: "Create customer",
    route: "/customers",
    successSignal: "Customer created.",
  },
  {
    id: "consent",
    index: 2,
    title: "Record consent",
    route: "/customers/:customerId",
    successSignal: "Record consent",
  },
  {
    id: "campaign",
    index: 3,
    title: "Create and submit campaign",
    route: "/campaign-builder",
    successSignal: "Campaign submitted for compliance review.",
  },
  {
    id: "approval",
    index: 4,
    title: "Compliance approval",
    route: "/compliance",
    successSignal: "Campaign approved.",
  },
  {
    id: "launch",
    index: 5,
    title: "Launch campaign",
    route: "/campaigns/:campaignId/recipients/preview",
    successSignal: "Campaign launched.",
  },
];

/** Demo admin used by the Playwright happy-path (role covers every step). */
export const HAPPY_PATH_ADMIN = {
  email: "admin@bayer-westphalian.test",
  password: "StrongPassword!2026",
  fullName: "Platform Admin",
  userId: "10000000-0000-0000-0000-000000009901",
} as const;

/** Deterministic fixtures used by the mocked API happy-path. */
export const HAPPY_PATH_FIXTURES = {
  customerFirstName: "E2E",
  customerLastName: "HappyPath",
  customerEmail: "e2e.happy.path@bayer-westphalian.test",
  consentPurpose: "Marketing outreach for controlled demo campaign",
  consentSource: "E2E_HAPPY_PATH",
  campaignName: "E2E Happy Path Outreach",
  campaignObjective: "Validate login through launch for internal QA evidence",
  campaignSubject: "Your next financial step",
  campaignBody: "Controlled Playwright happy-path message body.",
  segmentId: "40000000-0000-0000-0000-00000000e201",
  segmentName: "E2E Eligible Audience",
  productId: "30000000-0000-0000-0000-00000000e301",
  productName: "E2E Investment Fund",
  campaignId: "50000000-0000-0000-0000-00000000e401",
  customerId: "20000000-0000-0000-0000-00000000e101",
} as const;

export function getHappyPathStep(stepId: HappyPathStepId): HappyPathStepDefinition {
  const step = HAPPY_PATH_STEPS.find((candidate) => candidate.id === stepId);
  if (step == null) {
    throw new Error(`Unknown happy-path step: ${stepId}`);
  }
  return step;
}

export function happyPathStepIdsInOrder(): HappyPathStepId[] {
  return [...HAPPY_PATH_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

/**
 * Returns true when {@code observed} matches the KB happy-path order exactly.
 */
export function isValidHappyPathOrder(observed: readonly HappyPathStepId[]): boolean {
  const expected = happyPathStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

/**
 * Formats a human-readable journey summary for docs and failure messages.
 */
export function formatHappyPathJourney(
  steps: readonly HappyPathStepDefinition[] = HAPPY_PATH_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function nextHappyPathStep(stepId: HappyPathStepId): HappyPathStepDefinition | null {
  const current = getHappyPathStep(stepId);
  return HAPPY_PATH_STEPS.find((step) => step.index === current.index + 1) ?? null;
}

export function previousHappyPathStep(stepId: HappyPathStepId): HappyPathStepDefinition | null {
  const current = getHappyPathStep(stepId);
  return HAPPY_PATH_STEPS.find((step) => step.index === current.index - 1) ?? null;
}
