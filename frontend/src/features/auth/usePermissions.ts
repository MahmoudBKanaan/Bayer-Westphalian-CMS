import { useAuth } from "@/auth/AuthProvider";
import { createPermissionChecks } from "@/features/auth/permissions";

export function usePermissions() {
  const { hasAnyRole } = useAuth();

  return createPermissionChecks(hasAnyRole);
}
