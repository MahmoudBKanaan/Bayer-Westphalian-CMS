import { NavLink, Outlet } from "react-router-dom";
import { API_HEALTH_PLACEHOLDER, PROJECT_TITLE } from "@/app/App";

const navItems = [
  { to: "/dashboard", label: "Dashboard" },
  { to: "/customers", label: "Customers" },
  { to: "/products", label: "Products" },
  { to: "/segments", label: "Segments" },
  { to: "/campaigns", label: "Campaigns" },
  { to: "/compliance", label: "Compliance" },
  { to: "/analytics", label: "Analytics" },
  { to: "/reports", label: "Reports" },
  { to: "/users", label: "Users" },
  { to: "/audit", label: "Audit" },
];

export function AppLayout() {
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
          {navItems.map((item) => (
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
            <div className="health-pill">{API_HEALTH_PLACEHOLDER}</div>
            <div className="user-pill">Admin</div>
          </div>
        </header>
        <Outlet />
      </main>
    </div>
  );
}
