import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  assignRole,
  createUser,
  disableUser,
  listUsers,
  resetPassword,
  updateUser,
  type CreateUserPayload,
  type UserStatus,
} from "@/api/users";
import { useAuth } from "@/auth/AuthProvider";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import { StatusBadge } from "@/components/StatusBadge";

const USER_STATUSES: Array<UserStatus | "ALL"> = ["ALL", "ACTIVE", "DISABLED", "LOCKED"];

const SYSTEM_ROLES: SystemRoleName[] = [
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

const emptyCreateForm: CreateUserPayload = {
  email: "",
  password: "",
  fullName: "",
};

type UserEditDraft = {
  userId: string;
  fullName: string;
  status: UserStatus;
  roleName: SystemRoleName;
};

export function UsersPage() {
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuth();
  const [statusFilter, setStatusFilter] = useState<UserStatus | "ALL">("ALL");
  const [selectedUserId, setSelectedUserId] = useState("");
  const [createForm, setCreateForm] = useState<CreateUserPayload>(emptyCreateForm);
  const [editDraft, setEditDraft] = useState<UserEditDraft | null>(null);
  const [newPassword, setNewPassword] = useState("");
  const [notice, setNotice] = useState("");

  const usersQuery = useQuery({
    queryKey: ["users", statusFilter],
    queryFn: () => listUsers(statusFilter),
  });

  const users = useMemo(() => usersQuery.data ?? [], [usersQuery.data]);
  const selectedUser = useMemo(() => {
    if (selectedUserId === "") {
      return users[0];
    }

    return users.find((candidate) => candidate.id === selectedUserId);
  }, [selectedUserId, users]);

  const selectedUserDraft =
    selectedUser == null
      ? null
      : editDraft?.userId === selectedUser.id
        ? editDraft
        : createUserEditDraft(
            selectedUser.id,
            selectedUser.fullName,
            selectedUser.status,
            selectedUser.roles,
          );

  const refreshUsers = async () => {
    await queryClient.invalidateQueries({ queryKey: ["users"] });
  };

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: async (createdUser) => {
      setCreateForm(emptyCreateForm);
      setSelectedUserId(createdUser.id);
      setNotice("User created.");
      await refreshUsers();
    },
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      updateUser(selectedUser?.id ?? "", {
        fullName: selectedUserDraft?.fullName ?? "",
        status: selectedUserDraft?.status ?? "ACTIVE",
      }),
    onSuccess: async () => {
      setNotice("User updated.");
      await refreshUsers();
    },
  });

  const assignRoleMutation = useMutation({
    mutationFn: () =>
      assignRole(
        selectedUser?.id ?? "",
        selectedUserDraft?.roleName ?? "CAMPAIGN_MANAGER",
        currentUser?.id,
      ),
    onSuccess: async () => {
      setNotice("Role assigned.");
      await refreshUsers();
    },
  });

  const disableMutation = useMutation({
    mutationFn: () => disableUser(selectedUser?.id ?? ""),
    onSuccess: async () => {
      setNotice("User disabled.");
      await refreshUsers();
    },
  });

  const resetPasswordMutation = useMutation({
    mutationFn: () => resetPassword(selectedUser?.id ?? "", newPassword),
    onSuccess: async () => {
      setNewPassword("");
      setNotice("Password reset.");
      await refreshUsers();
    },
  });

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    assignRoleMutation.isPending ||
    disableMutation.isPending ||
    resetPasswordMutation.isPending;

  const errorMessage =
    authorizationErrorMessage(
      usersQuery.error,
      createMutation.error,
      updateMutation.error,
      assignRoleMutation.error,
      disableMutation.error,
      resetPasswordMutation.error,
    ) ||
    generalErrorMessage(
      usersQuery.error,
      createMutation.error,
      updateMutation.error,
      assignRoleMutation.error,
      disableMutation.error,
      resetPasswordMutation.error,
    );

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>User management</h2>
          <span>Admin account control, role assignment, and access status</span>
        </div>
        <div className="toolbar-row">
          <label>
            Status
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as UserStatus | "ALL")}
            >
              {USER_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {formatEnum(status)}
                </option>
              ))}
            </select>
          </label>
          {notice ? <p className="form-success">{notice}</p> : null}
          {errorMessage ? (
            <p className="form-error" role="alert">
              {errorMessage}
            </p>
          ) : null}
        </div>
      </div>

      <div className="split-grid user-management-grid">
        <section className="panel" aria-labelledby="create-user-heading">
          <div className="section-heading">
            <h2 id="create-user-heading">Create user</h2>
            <span>Employee login account</span>
          </div>
          <form
            className="form-grid"
            onSubmit={(event) => {
              event.preventDefault();
              setNotice("");
              createMutation.mutate(createForm);
            }}
          >
            <label>
              Full name
              <input
                required
                value={createForm.fullName}
                onChange={(event) =>
                  setCreateForm((current) => ({ ...current, fullName: event.target.value }))
                }
              />
            </label>
            <label>
              Email
              <input
                required
                type="email"
                value={createForm.email}
                onChange={(event) =>
                  setCreateForm((current) => ({ ...current, email: event.target.value }))
                }
              />
            </label>
            <label>
              Temporary password
              <input
                required
                minLength={8}
                type="password"
                value={createForm.password}
                onChange={(event) =>
                  setCreateForm((current) => ({ ...current, password: event.target.value }))
                }
              />
            </label>
            <button type="submit" disabled={isBusy}>
              Create user
            </button>
          </form>
        </section>

        <section className="panel" aria-labelledby="selected-user-heading">
          <div className="section-heading">
            <h2 id="selected-user-heading">Selected user</h2>
            <span>Edit, disable, assign role, or reset password</span>
          </div>
          {selectedUser == null ? (
            <p>No users match the selected filter.</p>
          ) : (
            <div className="form-grid">
              <label>
                User
                <select
                  value={selectedUser.id}
                  onChange={(event) => {
                    const nextUser = users.find((candidate) => candidate.id === event.target.value);
                    setSelectedUserId(event.target.value);
                    setEditDraft(
                      nextUser == null
                        ? null
                        : createUserEditDraft(
                            nextUser.id,
                            nextUser.fullName,
                            nextUser.status,
                            nextUser.roles,
                          ),
                    );
                  }}
                >
                  {users.map((candidate) => (
                    <option key={candidate.id} value={candidate.id}>
                      {candidate.fullName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Full name
                <input
                  value={selectedUserDraft?.fullName ?? ""}
                  onChange={(event) =>
                    setEditDraft((current) => ({
                      ...(current ??
                        createUserEditDraft(
                          selectedUser.id,
                          selectedUser.fullName,
                          selectedUser.status,
                          selectedUser.roles,
                        )),
                      fullName: event.target.value,
                    }))
                  }
                />
              </label>
              <label>
                Status
                <select
                  value={selectedUserDraft?.status ?? "ACTIVE"}
                  onChange={(event) =>
                    setEditDraft((current) => ({
                      ...(current ??
                        createUserEditDraft(
                          selectedUser.id,
                          selectedUser.fullName,
                          selectedUser.status,
                          selectedUser.roles,
                        )),
                      status: event.target.value as UserStatus,
                    }))
                  }
                >
                  {USER_STATUSES.filter((status) => status !== "ALL").map((status) => (
                    <option key={status} value={status}>
                      {formatEnum(status)}
                    </option>
                  ))}
                </select>
              </label>
              <div className="button-row">
                <button type="button" disabled={isBusy} onClick={() => updateMutation.mutate()}>
                  Save changes
                </button>
                <button
                  type="button"
                  disabled={isBusy || selectedUser.status === "DISABLED"}
                  onClick={() => disableMutation.mutate()}
                >
                  Disable user
                </button>
              </div>
              <div>
                <span className="field-caption">Current roles</span>
                <div className="role-chip-list" aria-label="Current roles">
                  {selectedUser.roles.length === 0 ? (
                    <span className="status-badge">No roles</span>
                  ) : (
                    selectedUser.roles.map((role) => (
                      <span className="status-badge" key={role}>
                        {formatEnum(role)}
                      </span>
                    ))
                  )}
                </div>
              </div>
              <label>
                Assign role
                <select
                  value={selectedUserDraft?.roleName ?? "CAMPAIGN_MANAGER"}
                  onChange={(event) =>
                    setEditDraft((current) => ({
                      ...(current ??
                        createUserEditDraft(
                          selectedUser.id,
                          selectedUser.fullName,
                          selectedUser.status,
                          selectedUser.roles,
                        )),
                      roleName: event.target.value as SystemRoleName,
                    }))
                  }
                >
                  {SYSTEM_ROLES.map((role) => (
                    <option key={role} value={role} disabled={selectedUser.roles.includes(role)}>
                      {formatEnum(role)}
                    </option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                disabled={
                  isBusy ||
                  selectedUserDraft == null ||
                  selectedUser.roles.includes(selectedUserDraft.roleName)
                }
                onClick={() => assignRoleMutation.mutate()}
              >
                Assign role
              </button>
              <label>
                New password
                <input
                  minLength={8}
                  type="password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                />
              </label>
              <button
                type="button"
                disabled={isBusy || newPassword.length < 8}
                onClick={() => resetPasswordMutation.mutate()}
              >
                Reset password
              </button>
            </div>
          )}
        </section>
      </div>

      <section className="panel" aria-labelledby="users-table-heading">
        <div className="section-heading">
          <h2 id="users-table-heading">Employee accounts</h2>
          <span>{usersQuery.isLoading ? "Loading users" : `${users.length} records`}</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Status</th>
              <th>Roles</th>
              <th>Last login</th>
            </tr>
          </thead>
          <tbody>
            {users.map((employee) => (
              <tr key={employee.id}>
                <td>{employee.fullName}</td>
                <td>{employee.email}</td>
                <td>
                  <StatusBadge value={formatEnum(employee.status)} />
                </td>
                <td>{employee.roles.map(formatEnum).join(", ") || "No roles"}</td>
                <td>{employee.lastLoginAt == null ? "Never" : formatDate(employee.lastLoginAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </section>
  );
}

function formatEnum(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function firstAssignableRole(currentRoles: SystemRoleName[]) {
  return SYSTEM_ROLES.find((role) => !currentRoles.includes(role)) ?? SYSTEM_ROLES[0];
}

function createUserEditDraft(
  userId: string,
  fullName: string,
  status: UserStatus,
  roles: SystemRoleName[],
): UserEditDraft {
  return {
    userId,
    fullName,
    status,
    roleName: firstAssignableRole(roles),
  };
}

function authorizationErrorMessage(...errors: unknown[]) {
  return errors.some(isAuthorizationError)
    ? "You are not authorized to manage users or roles."
    : "";
}

function generalErrorMessage(...errors: unknown[]) {
  return errors.some(Boolean) ? "User management action failed." : "";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
