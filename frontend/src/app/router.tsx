import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { AppLayout } from "@/components/AppLayout";
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
          { path: "customers", element: <CustomersPage /> },
          { path: "customers/:customerId", element: <CustomerDetailsPage /> },
          { path: "products", element: <ProductsPage /> },
          { path: "products/:productId", element: <ProductDetailsPage /> },
          { path: "product-change-requests", element: <ProductChangeRequestsPage /> },
          { path: "segments", element: <SegmentsPage /> },
          { path: "campaigns", element: <CampaignsPage /> },
          {
            path: "campaigns/:campaignId/recipients/preview",
            element: <CampaignRecipientPreviewPage />,
          },
          { path: "campaign-builder", element: <CampaignBuilderPage /> },
          { path: "compliance", element: <CompliancePage /> },
          { path: "contact-history", element: <ContactHistoryPage /> },
          { path: "follow-up-tasks", element: <FollowUpTasksPage /> },
          { path: "reminders", element: <RemindersPage /> },
          { path: "analytics", element: <AnalyticsPage /> },
          { path: "executive", element: <ExecutiveDashboardPage /> },
          { path: "reports", element: <ReportsPage /> },
          { path: "users", element: <UsersPage /> },
          { path: "settings", element: <SystemSettingsPage /> },
          { path: "audit", element: <AuditPage /> },
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
