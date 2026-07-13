import type { ReactNode } from "react";
import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { RoleProtectedRoute } from "@/auth/RoleProtectedRoute";
import { AppLayout } from "@/components/AppLayout";
import { NAV_MENU_SECTIONS } from "@/features/auth/roleBasedMenu";
import { AnalyticsPage } from "@/pages/AnalyticsPage";
import { AuditPage } from "@/pages/AuditPage";
import { CampaignBuilderPage } from "@/pages/CampaignBuilderPage";
import { CampaignRecipientPreviewPage } from "@/pages/CampaignRecipientPreviewPage";
import { CampaignsPage } from "@/pages/CampaignsPage";
import { CompliancePage } from "@/pages/CompliancePage";
import { ContactHistoryPage } from "@/pages/ContactHistoryPage";
import { CustomersPage } from "@/pages/CustomersPage";
import { CustomerDetailsPage } from "@/pages/CustomerDetailsPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { ExecutiveDashboardPage } from "@/pages/ExecutiveDashboardPage";
import { FollowUpTasksPage } from "@/pages/FollowUpTasksPage";
import { LoginPage } from "@/pages/LoginPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ProductChangeRequestsPage } from "@/pages/ProductChangeRequestsPage";
import { ProductDetailsPage } from "@/pages/ProductDetailsPage";
import { ProductsPage } from "@/pages/ProductsPage";
import { ReportsPage } from "@/pages/ReportsPage";
import { RemindersPage } from "@/pages/RemindersPage";
import { SegmentsPage } from "@/pages/SegmentsPage";
import { SystemSettingsPage } from "@/pages/SystemSettingsPage";
import { UsersPage } from "@/pages/UsersPage";

function restricted(path: string, element: ReactNode, permissionPath = path) {
  const item = NAV_MENU_SECTIONS.flatMap((section) => section.items).find(
    (candidate) => candidate.to === `/${permissionPath}`,
  );

  if (item == null) {
    throw new Error(`Missing role configuration for route: ${permissionPath}`);
  }

  return {
    path,
    element: <RoleProtectedRoute allowedRoles={item.roles}>{element}</RoleProtectedRoute>,
  };
}

export const routes: RouteObject[] = [
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/",
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "dashboard", element: <DashboardPage /> },
          restricted("customers", <CustomersPage />),
          restricted("customers/:customerId", <CustomerDetailsPage />, "customers"),
          restricted("products", <ProductsPage />),
          restricted("products/:productId", <ProductDetailsPage />, "products"),
          restricted("product-change-requests", <ProductChangeRequestsPage />),
          restricted("segments", <SegmentsPage />),
          restricted("campaigns", <CampaignsPage />),
          {
            path: "campaigns/:campaignId/recipients/preview",
            element: restricted("campaigns", <CampaignRecipientPreviewPage />).element,
          },
          restricted("campaign-builder", <CampaignBuilderPage />),
          restricted("compliance", <CompliancePage />),
          restricted("contact-history", <ContactHistoryPage />),
          restricted("follow-up-tasks", <FollowUpTasksPage />),
          restricted("reminders", <RemindersPage />),
          restricted("analytics", <AnalyticsPage />),
          restricted("executive", <ExecutiveDashboardPage />),
          restricted("reports", <ReportsPage />),
          restricted("users", <UsersPage />),
          restricted("settings", <SystemSettingsPage />),
          restricted("audit", <AuditPage />),
        ],
      },
    ],
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
];

export function createAppRouter() {
  return createBrowserRouter(routes);
}

export const router = createAppRouter();
