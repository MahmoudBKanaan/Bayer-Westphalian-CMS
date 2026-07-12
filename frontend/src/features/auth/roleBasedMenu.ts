/**
 * Role-based main navigation menu (KB role matrix / NFR-001 / item 607).
 *
 * Defines which shell nav links appear for each system role so unauthorized
 * features stay hidden from the UI menu (backend still enforces authorization).
 */

import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

export const ALL_SYSTEM_ROLES: SystemRoleName[] = [
  "ADMIN",
  "CAMPAIGN_MANAGER",
  "BI_ANALYST",
  "PRODUCT_MANAGER",
  "COMPLIANCE_OFFICER",
  "CUSTOMER_SERVICE_AGENT",
  "SALES_AGENT",
  "MARKETING_ANALYST",
  "EXECUTIVE_VIEWER",
  "SYSTEM_AUDITOR",
];

export type NavMenuItem = {
  to: string;
  label: string;
  roles: SystemRoleName[];
};

export type NavMenuSection = {
  label: string;
  items: NavMenuItem[];
};

export const MAIN_NAV_ARIA_LABEL = "Main navigation";
export const NAV_EMPTY_TITLE = "No navigation available";
export const NAV_EMPTY_DESCRIPTION =
  "Your account has no assigned application roles. Contact an administrator to update access.";

/**
 * Canonical sidebar navigation. Keep in sync with AppLayout shell filtering.
 */
export const NAV_MENU_SECTIONS: NavMenuSection[] = [
  {
    label: "Workspace",
    items: [
      { to: "/dashboard", label: "Dashboard", roles: ALL_SYSTEM_ROLES },
      {
        to: "/customers",
        label: "Customers",
        roles: [
          "ADMIN",
          "CAMPAIGN_MANAGER",
          "BI_ANALYST",
          "PRODUCT_MANAGER",
          "COMPLIANCE_OFFICER",
          "CUSTOMER_SERVICE_AGENT",
          "SALES_AGENT",
          "SYSTEM_AUDITOR",
        ],
      },
      {
        to: "/products",
        label: "Products",
        roles: ["ADMIN", "PRODUCT_MANAGER", "CAMPAIGN_MANAGER", "BI_ANALYST"],
      },
      {
        to: "/product-change-requests",
        label: "Change requests",
        roles: ["ADMIN", "PRODUCT_MANAGER", "BI_ANALYST"],
      },
      {
        to: "/segments",
        label: "Segments",
        roles: ["ADMIN", "CAMPAIGN_MANAGER", "BI_ANALYST", "COMPLIANCE_OFFICER"],
      },
    ],
  },
  {
    label: "Campaign Operations",
    items: [
      {
        to: "/campaigns",
        label: "Campaigns",
        roles: ["ADMIN", "CAMPAIGN_MANAGER", "COMPLIANCE_OFFICER", "PRODUCT_MANAGER"],
      },
      {
        to: "/campaign-builder",
        label: "Builder",
        roles: ["ADMIN", "CAMPAIGN_MANAGER"],
      },
      {
        to: "/compliance",
        label: "Compliance",
        roles: ["ADMIN", "COMPLIANCE_OFFICER", "CAMPAIGN_MANAGER"],
      },
      {
        to: "/contact-history",
        label: "Contact history",
        roles: [
          "ADMIN",
          "CAMPAIGN_MANAGER",
          "BI_ANALYST",
          "PRODUCT_MANAGER",
          "COMPLIANCE_OFFICER",
          "CUSTOMER_SERVICE_AGENT",
          "SALES_AGENT",
          "EXECUTIVE_VIEWER",
          "SYSTEM_AUDITOR",
        ],
      },
      {
        to: "/follow-up-tasks",
        label: "Follow-ups",
        roles: ["ADMIN", "CUSTOMER_SERVICE_AGENT", "SALES_AGENT", "CAMPAIGN_MANAGER"],
      },
      {
        to: "/reminders",
        label: "Reminders",
        roles: [
          "ADMIN",
          "CAMPAIGN_MANAGER",
          "CUSTOMER_SERVICE_AGENT",
          "SALES_AGENT",
          "COMPLIANCE_OFFICER",
        ],
      },
    ],
  },
  {
    label: "Insights",
    items: [
      {
        to: "/analytics",
        label: "Analytics",
        roles: [
          "ADMIN",
          "CAMPAIGN_MANAGER",
          "BI_ANALYST",
          "PRODUCT_MANAGER",
          "MARKETING_ANALYST",
          "EXECUTIVE_VIEWER",
        ],
      },
      {
        to: "/executive",
        label: "Executive",
        roles: [
          "ADMIN",
          "BI_ANALYST",
          "CAMPAIGN_MANAGER",
          "MARKETING_ANALYST",
          "EXECUTIVE_VIEWER",
        ],
      },
      {
        to: "/reports",
        label: "Reports",
        roles: [
          "ADMIN",
          "BI_ANALYST",
          "CAMPAIGN_MANAGER",
          "MARKETING_ANALYST",
          "EXECUTIVE_VIEWER",
        ],
      },
    ],
  },
  {
    label: "Administration",
    items: [
      { to: "/users", label: "Users", roles: ["ADMIN"] },
      { to: "/settings", label: "Settings", roles: ["ADMIN"] },
      { to: "/audit", label: "Audit", roles: ["ADMIN", "COMPLIANCE_OFFICER", "SYSTEM_AUDITOR"] },
    ],
  },
];

/** Sensitive admin-only menu labels that must stay hidden from non-admins. */
export const ADMIN_ONLY_MENU_LABELS = ["Users", "Settings"] as const;

/** Campaign-management labels that product/BI personas must not see. */
export const CAMPAIGN_BUILDER_MENU_LABEL = "Builder";

export type RoleBasedMenuStepId =
  | "authenticate"
  | "resolve-roles"
  | "filter-nav-items"
  | "render-visible-menu";

export type RoleBasedMenuStepDefinition = {
  id: RoleBasedMenuStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Role-based menu hides unauthorized features” (item 607). */
export const ROLE_BASED_MENU_FLOW_STEPS: RoleBasedMenuStepDefinition[] = [
  {
    id: "authenticate",
    index: 0,
    title: "Authenticate",
    description: "Employee signs in and a JWT with role claims is stored in sessionStorage.",
  },
  {
    id: "resolve-roles",
    index: 1,
    title: "Resolve roles",
    description: "AuthProvider extracts system roles from the access token.",
  },
  {
    id: "filter-nav-items",
    index: 2,
    title: "Filter nav items",
    description: "Shell keeps only menu items whose role allow-list intersects user roles.",
  },
  {
    id: "render-visible-menu",
    index: 3,
    title: "Render visible menu",
    description: "Main navigation shows allowed links only; empty roles show empty state.",
  },
];

export function roleHasMenuAccess(
  userRoles: readonly SystemRoleName[],
  allowedRoles: readonly SystemRoleName[],
): boolean {
  return allowedRoles.some((role) => userRoles.includes(role));
}

/**
 * Filters the full nav tree to sections/items visible for the given roles.
 */
export function filterNavMenuSections(
  userRoles: readonly SystemRoleName[],
  sections: readonly NavMenuSection[] = NAV_MENU_SECTIONS,
): NavMenuSection[] {
  return sections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => roleHasMenuAccess(userRoles, item.roles)),
    }))
    .filter((section) => section.items.length > 0);
}

/**
 * Flat ordered list of visible menu labels (matches AppLayout sidebar order).
 */
export function visibleMenuLabelsForRoles(userRoles: readonly SystemRoleName[]): string[] {
  return filterNavMenuSections(userRoles).flatMap((section) =>
    section.items.map((item) => item.label),
  );
}

export function isMenuLabelVisibleForRoles(
  label: string,
  userRoles: readonly SystemRoleName[],
): boolean {
  return visibleMenuLabelsForRoles(userRoles).includes(label);
}

export function isAdminOnlyMenuLabel(label: string): boolean {
  return (ADMIN_ONLY_MENU_LABELS as readonly string[]).includes(label);
}

/**
 * Expected menu labels for each KB system role (single-role personas).
 * Multi-role users get the union of labels (order preserved from NAV_MENU_SECTIONS).
 */
export const EXPECTED_MENU_LABELS_BY_ROLE: Record<SystemRoleName, string[]> = {
  ADMIN: visibleMenuLabelsForRoles(["ADMIN"]),
  CAMPAIGN_MANAGER: visibleMenuLabelsForRoles(["CAMPAIGN_MANAGER"]),
  BI_ANALYST: visibleMenuLabelsForRoles(["BI_ANALYST"]),
  PRODUCT_MANAGER: visibleMenuLabelsForRoles(["PRODUCT_MANAGER"]),
  COMPLIANCE_OFFICER: visibleMenuLabelsForRoles(["COMPLIANCE_OFFICER"]),
  CUSTOMER_SERVICE_AGENT: visibleMenuLabelsForRoles(["CUSTOMER_SERVICE_AGENT"]),
  SALES_AGENT: visibleMenuLabelsForRoles(["SALES_AGENT"]),
  MARKETING_ANALYST: visibleMenuLabelsForRoles(["MARKETING_ANALYST"]),
  EXECUTIVE_VIEWER: visibleMenuLabelsForRoles(["EXECUTIVE_VIEWER"]),
  SYSTEM_AUDITOR: visibleMenuLabelsForRoles(["SYSTEM_AUDITOR"]),
};

export function roleBasedMenuStepIdsInOrder(): RoleBasedMenuStepId[] {
  return [...ROLE_BASED_MENU_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatRoleBasedMenuJourney(
  steps: readonly RoleBasedMenuStepDefinition[] = ROLE_BASED_MENU_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidRoleBasedMenuOrder(observed: readonly RoleBasedMenuStepId[]): boolean {
  const expected = roleBasedMenuStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

export function findNavMenuItemByLabel(label: string): NavMenuItem | undefined {
  for (const section of NAV_MENU_SECTIONS) {
    const item = section.items.find((candidate) => candidate.label === label);
    if (item != null) {
      return item;
    }
  }
  return undefined;
}
