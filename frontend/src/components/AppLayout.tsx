import {
  useIsFetching,
  useIsMutating,
  useQuery,
  type UseQueryResult,
} from "@tanstack/react-query";
import { type KeyboardEvent, useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { PROJECT_TITLE } from "@/app/App";
import { apiRequest } from "@/api/client";
import { useAuth } from "@/auth/AuthProvider";
import { EmptyState } from "@/components/EmptyState";
import { ErrorState } from "@/components/ErrorState";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  filterNavMenuSections,
  MAIN_NAV_ARIA_LABEL,
  NAV_EMPTY_DESCRIPTION,
  NAV_EMPTY_TITLE,
  NAV_MENU_SECTIONS,
} from "@/features/auth/roleBasedMenu";
import {
  MAIN_CONTENT_ID,
  SKIP_TO_CONTENT_LABEL,
} from "@/features/a11y/keyboardNavigationFlow";
import { BrandLogo } from "@/components/BrandLogo";

type PageHeading = {
  title: string;
  breadcrumbs: string[];
};

export function AppLayout() {
  const { roles, signOut, user } = useAuth();
  const location = useLocation();
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const userMenuTriggerRef = useRef<HTMLButtonElement>(null);
  const signOutButtonRef = useRef<HTMLButtonElement>(null);
  const activeFetches = useIsFetching();
  const activeMutations = useIsMutating();
  const healthQuery = useQuery({
    queryKey: ["api-health"],
    queryFn: getApiHealth,
    refetchInterval: 30_000,
    retry: false,
  });
  // Item 607: role-based menu hides unauthorized features (canonical allow-lists).
  const visibleNavSections = filterNavMenuSections(roles);
  const pageHeading = resolvePageHeading(location.pathname);
  const hasPendingWork = activeFetches + activeMutations > 0;

  useEffect(() => {
    if (!userMenuOpen) {
      return undefined;
    }

    function handleDocumentKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") {
        setUserMenuOpen(false);
        userMenuTriggerRef.current?.focus();
      }
    }

    function handleDocumentPointerDown(event: PointerEvent) {
      if (event.target instanceof Node && !userMenuRef.current?.contains(event.target)) {
        setUserMenuOpen(false);
      }
    }

    document.addEventListener("keydown", handleDocumentKeyDown);
    document.addEventListener("pointerdown", handleDocumentPointerDown);

    return () => {
      document.removeEventListener("keydown", handleDocumentKeyDown);
      document.removeEventListener("pointerdown", handleDocumentPointerDown);
    };
  }, [userMenuOpen]);

  function openUserMenu(focusMenuItem = false) {
    setUserMenuOpen(true);

    if (focusMenuItem) {
      window.setTimeout(() => {
        signOutButtonRef.current?.focus();
      }, 0);
    }
  }

  function closeUserMenu() {
    setUserMenuOpen(false);
    userMenuTriggerRef.current?.focus();
  }

  function handleUserMenuTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      openUserMenu(true);
    }

    if (event.key === "Escape" && userMenuOpen) {
      event.preventDefault();
      closeUserMenu();
    }
  }

  function handleUserMenuPanelKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      closeUserMenu();
      return;
    }

    if (["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) {
      event.preventDefault();
      signOutButtonRef.current?.focus();
    }
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href={`#${MAIN_CONTENT_ID}`}>
        {SKIP_TO_CONTENT_LABEL}
      </a>
      <aside className="sidebar">
        <div className="brand-block">
          <BrandLogo variant="mark" size="md" className="brand-mark-logo" />
          <div>
            <strong>Bayer-Westphalian</strong>
            <span>Campaign Management</span>
          </div>
        </div>

        <nav className="sidebar-nav" aria-label={MAIN_NAV_ARIA_LABEL}>
          {visibleNavSections.length === 0 ? (
            <EmptyState
              compact
              title={NAV_EMPTY_TITLE}
              description={NAV_EMPTY_DESCRIPTION}
            />
          ) : null}
          {visibleNavSections.map((section) => (
            <section
              className="nav-section"
              aria-labelledby={navSectionId(section.label)}
              key={section.label}
            >
              <h2 className="nav-section-title" id={navSectionId(section.label)}>
                {section.label}
              </h2>
              <div className="nav-list">
                {section.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </section>
          ))}
        </nav>
      </aside>

      <main
        id={MAIN_CONTENT_ID}
        className="main-panel"
        aria-labelledby="application-shell-title"
        aria-busy={hasPendingWork}
        tabIndex={-1}
      >
        <header className="topbar">
          <div>
            <nav className="breadcrumb" aria-label="Breadcrumb">
              <ol>
                {pageHeading.breadcrumbs.map((crumb, index) => (
                  <li
                    key={`${crumb}-${index}`}
                    aria-current={
                      index === pageHeading.breadcrumbs.length - 1 ? "page" : undefined
                    }
                  >
                    {crumb}
                    {index < pageHeading.breadcrumbs.length - 1 ? (
                      <span aria-hidden="true">/</span>
                    ) : null}
                  </li>
                ))}
              </ol>
            </nav>
            <span className="eyebrow">{PROJECT_TITLE}</span>
            <h1 id="application-shell-title">{pageHeading.title}</h1>
          </div>
          <div className="topbar-actions">
            {hasPendingWork ? (
              <div className="loading-indicator" role="status" aria-live="polite">
                Loading application data
              </div>
            ) : null}
            <div className="health-pill">{formatApiHealth(healthQuery)}</div>
            <div className="user-menu" ref={userMenuRef}>
              <button
                type="button"
                className="user-menu-trigger"
                ref={userMenuTriggerRef}
                aria-haspopup="menu"
                aria-controls="topbar-user-menu"
                aria-expanded={userMenuOpen}
                onClick={() => setUserMenuOpen((open) => !open)}
                onKeyDown={handleUserMenuTriggerKeyDown}
              >
                <span className="user-avatar">{userInitials(user?.fullName)}</span>
                <span>{user?.fullName ?? "Unauthenticated"}</span>
              </button>
              {userMenuOpen ? (
                <div
                  className="user-menu-panel"
                  id="topbar-user-menu"
                  role="menu"
                  aria-label="User menu"
                  onKeyDown={handleUserMenuPanelKeyDown}
                >
                  <div className="user-menu-identity">
                    <strong>{user?.fullName ?? "Unauthenticated"}</strong>
                    <span>{user?.email ?? "No email available"}</span>
                  </div>
                  <div className="user-menu-roles" aria-label="Assigned roles">
                    {roles.length > 0
                      ? roles.map((role) => <span key={role}>{formatRoleLabel(role)}</span>)
                      : "No roles assigned"}
                  </div>
                  <button
                    type="button"
                    className="user-menu-signout"
                    ref={signOutButtonRef}
                    role="menuitem"
                    onClick={signOut}
                  >
                    Sign out
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        </header>
        {healthQuery.isError ? (
          <ErrorState
            compact
            className="shell-error-state"
            title="Backend unavailable"
            description="Application data could not be refreshed. Check the API service before continuing operational work."
          />
        ) : null}
        <Outlet />
      </main>
    </div>
  );
}

function navSectionId(label: string) {
  return `nav-section-${label.toLowerCase().replace(/\s+/g, "-")}`;
}

function resolvePageHeading(pathname: string): PageHeading {
  if (pathname.startsWith("/customers/")) {
    return {
      title: "Customer details",
      breadcrumbs: ["Workspace", "Customers", "Customer details"],
    };
  }

  if (pathname.startsWith("/products/")) {
    return {
      title: "Product details",
      breadcrumbs: ["Workspace", "Products", "Product details"],
    };
  }

  if (pathname.match(/^\/campaigns\/[^/]+\/recipients\/preview$/)) {
    return {
      title: "Recipient Preview",
      breadcrumbs: ["Campaign Operations", "Campaigns", "Recipient Preview"],
    };
  }

  const normalizedPath = pathname === "/" ? "/dashboard" : pathname;
  const navItem = NAV_MENU_SECTIONS.flatMap((section) => section.items).find(
    (item) => item.to === normalizedPath,
  );
  if (navItem != null) {
    const section = NAV_MENU_SECTIONS.find((candidate) =>
      candidate.items.some((item) => item.to === navItem.to),
    );
    return {
      title: navItem.label,
      breadcrumbs: [section?.label ?? "Application", navItem.label],
    };
  }

  return { title: "Page", breadcrumbs: ["Application", "Page"] };
}

function userInitials(fullName: string | undefined) {
  if (fullName == null || fullName.trim() === "") {
    return "BW";
  }

  return fullName
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

function formatRoleLabel(role: SystemRoleName) {
  return role
    .toLowerCase()
    .split("_")
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(" ");
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
