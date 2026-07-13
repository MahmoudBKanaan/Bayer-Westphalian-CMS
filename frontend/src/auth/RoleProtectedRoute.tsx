import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthProvider";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

type RoleProtectedRouteProps = {
  allowedRoles: readonly SystemRoleName[];
  children: ReactNode;
};

export function RoleProtectedRoute({ allowedRoles, children }: RoleProtectedRouteProps) {
  const { hasAnyRole } = useAuth();

  if (!hasAnyRole([...allowedRoles])) {
    return <Navigate to="/dashboard" replace state={{ reason: "access-denied" }} />;
  }

  return children;
}
