import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { NavLink, Outlet } from "react-router-dom";
import { PROJECT_TITLE } from "@/app/App";
import { apiRequest } from "@/api/client";
import { useAuth } from "@/auth/AuthProvider";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

const ALL_ROLES: SystemRoleName[] = [
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

type NavItem = {
  to: string;
  label: string;
  roles: SystemRoleName[];
};

const navItems: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", roles: ALL_ROLES },
  {
    to: "/customers",
    label: "Customers",
    roles: [
      "ADMIN",
      "CAMPAIGN_MANAGER",
      "BI_ANALYST",
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
    to: "/segments",
    label: "Segments",
    roles: ["ADMIN", "CAMPAIGN_MANAGER", "BI_ANALYST", "MARKETING_ANALYST"],
  },
  {
    to: "/campaigns",
    label: "Campaigns",
    roles: ["ADMIN", "CAMPAIGN_MANAGER", "COMPLIANCE_OFFICER", "PRODUCT_MANAGER"],
  },
  {
    to: "/compliance",
    label: "Compliance",
    roles: ["ADMIN", "COMPLIANCE_OFFICER", "CAMPAIGN_MANAGER"],
  },
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
    to: "/reports",
    label: "Reports",
    roles: [
      "ADMIN",
      "BI_ANALYST",
      "COMPLIANCE_OFFICER",
      "MARKETING_ANALYST",
      "EXECUTIVE_VIEWER",
      "SYSTEM_AUDITOR",
    ],
  },
  { to: "/users", label: "Users", roles: ["ADMIN"] },
  { to: "/audit", label: "Audit", roles: ["ADMIN", "COMPLIANCE_OFFICER", "SYSTEM_AUDITOR"] },
];

export function AppLayout() {
  const { hasAnyRole, user } = useAuth();
  const healthQuery = useQuery({
    queryKey: ["api-health"],
    queryFn: getApiHealth,
    refetchInterval: 30_000,
    retry: false,
  });
  const visibleNavItems = navItems.filter((item) => hasAnyRole(item.roles));

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-mark">BW</span>
          <div>
            <strong>Bayer-Westphalian</strong>
            <span>Campaign Management</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="Main navigation">
          {visibleNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <span className="eyebrow">Internal platform</span>
            <h1>{PROJECT_TITLE}</h1>
          </div>
          <div className="topbar-actions">
            <div className="health-pill">{formatApiHealth(healthQuery)}</div>
            <div className="user-pill">{user?.fullName ?? "Unauthenticated"}</div>
          </div>
        </header>
        <Outlet />
      </main>
    </div>
  );
}

type ApiHealthResponse = {
  status: string;
  service?: string;
  timestamp?: string;
};

function getApiHealth() {
  return apiRequest<ApiHealthResponse>("/health", { authenticated: false });
}

function formatApiHealth({
  data,
  isError,
  isLoading,
}: Pick<UseQueryResult<ApiHealthResponse>, "data" | "isError" | "isLoading">) {
  if (isError) {
    return "API health: backend not connected yet";
  }

  if (isLoading || data == null) {
    return "API health: checking";
  }

  return data.status === "UP" ? "API health: connected" : `API health: ${data.status}`;
}
