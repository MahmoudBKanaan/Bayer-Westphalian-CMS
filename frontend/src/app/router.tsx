import { createBrowserRouter, Navigate, type RouteObject } from "react-router-dom";
import { AppLayout } from "@/components/AppLayout";
import { AnalyticsPage } from "@/pages/AnalyticsPage";
import { AuditPage } from "@/pages/AuditPage";
import { CampaignsPage } from "@/pages/CampaignsPage";
import { CompliancePage } from "@/pages/CompliancePage";
import { CustomersPage } from "@/pages/CustomersPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { LoginPage } from "@/pages/LoginPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ProductsPage } from "@/pages/ProductsPage";
import { ReportsPage } from "@/pages/ReportsPage";
import { SegmentsPage } from "@/pages/SegmentsPage";
import { UsersPage } from "@/pages/UsersPage";

export const routes: RouteObject[] = [
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <DashboardPage /> },
      { path: "customers", element: <CustomersPage /> },
      { path: "products", element: <ProductsPage /> },
      { path: "segments", element: <SegmentsPage /> },
      { path: "campaigns", element: <CampaignsPage /> },
      { path: "compliance", element: <CompliancePage /> },
      { path: "analytics", element: <AnalyticsPage /> },
      { path: "reports", element: <ReportsPage /> },
      { path: "users", element: <UsersPage /> },
      { path: "audit", element: <AuditPage /> },
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
