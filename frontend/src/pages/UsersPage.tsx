import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  assignRole,
  createUser,
  disableUser,
  enableUser,
  listUsers,
  resetPassword,
  updateUser,
  type CreateUserPayload,
  type UserStatus,
} from "@/api/users";
import { useAuth } from "@/auth/AuthProvider";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import { ConfirmationDialog } from "@/components/ConfirmationDialog";
import { FormValidationMessage } from "@/components/FormValidationMessage";
import { StatusBadge } from "@/components/StatusBadge";
import { SuccessNotification } from "@/components/SuccessNotification";

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

type CreateUserFormErrors = Partial<Record<keyof CreateUserPayload, string>>;
type PendingSensitiveAction =
  | "assign-role"
  | "disable-user"
  | "enable-user"
  | "reset-password"
  | null;

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
  const [createErrors, setCreateErrors] = useState<CreateUserFormErrors>({});
  const [editDraft, setEditDraft] = useState<UserEditDraft | null>(null);
  const [newPassword, setNewPassword] = useState("");
  const [notice, setNotice] = useState("");
  const [pendingSensitiveAction, setPendingSensitiveAction] =
    useState<PendingSensitiveAction>(null);

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
      setCreateErrors({});
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
      setPendingSensitiveAction(null);
      setNotice("Role assigned.");
      await refreshUsers();
    },
  });

  const accountStatusMutation = useMutation({
    mutationFn: () =>
      selectedUser?.status === "DISABLED"
        ? enableUser(selectedUser.id, selectedUser.fullName)
        : disableUser(selectedUser?.id ?? ""),
    onSuccess: async (updatedUser) => {
      setPendingSensitiveAction(null);
      setEditDraft(
        createUserEditDraft(
          updatedUser.id,
          updatedUser.fullName,
          updatedUser.status,
          updatedUser.roles,
        ),
      );
      setNotice(updatedUser.status === "ACTIVE" ? "User enabled." : "User disabled.");
      await refreshUsers();
    },
  });

  const resetPasswordMutation = useMutation({
    mutationFn: () => resetPassword(selectedUser?.id ?? "", newPassword),
    onSuccess: async () => {
      setPendingSensitiveAction(null);
      setNewPassword("");
      setNotice("Password reset.");
      await refreshUsers();
    },
  });

  const isBusy =
    createMutation.isPending ||
    updateMutation.isPending ||
    assignRoleMutation.isPending ||
    accountStatusMutation.isPending ||
    resetPasswordMutation.isPending;

  const errorMessage =
    authorizationErrorMessage(
      usersQuery.error,
      createMutation.error,
      updateMutation.error,
      assignRoleMutation.error,
      accountStatusMutation.error,
      resetPasswordMutation.error,
    ) ||
    generalErrorMessage(
      usersQuery.error,
      createMutation.error,
      updateMutation.error,
      assignRoleMutation.error,
      accountStatusMutation.error,
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
          {notice ? <SuccessNotification compact message={notice} /> : null}
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
            noValidate
            onSubmit={(event) => {
              event.preventDefault();
              setNotice("");
              const nextErrors = validateCreateUserForm(createForm);

              if (hasCreateUserFormErrors(nextErrors)) {
                setCreateErrors(nextErrors);
                return;
              }

              setCreateErrors({});
              createMutation.mutate(createForm);
            }}
          >
            <label>
              Full name
              <input
                required
                aria-label="Full name"
                aria-describedby={createErrors.fullName ? "create-user-full-name-error" : undefined}
                aria-invalid={Boolean(createErrors.fullName)}
                value={createForm.fullName}
                onChange={(event) =>
                  setCreateForm((current) => ({ ...current, fullName: event.target.value }))
                }
              />
              <FormValidationMessage
                id="create-user-full-name-error"
                message={createErrors.fullName}
              />
            </label>
            <label>
              Email
              <input
                required
                aria-label="Email"
                aria-describedby={createErrors.email ? "create-user-email-error" : undefined}
                aria-invalid={Boolean(createErrors.email)}
                type="email"
                value={createForm.email}
                onChange={(event) =>
                  setCreateForm((current) => ({ ...current, email: event.target.value }))
                }
              />
              <FormValidationMessage id="create-user-email-error" message={createErrors.email} />
            </label>
            <label>
              Temporary password
              <input
                required
                minLength={8}
                aria-label="Temporary password"
                aria-describedby={
                  createErrors.password ? "create-user-password-error" : undefined
                }
                aria-invalid={Boolean(createErrors.password)}
                type="password"
                value={createForm.password}
                onChange={(event) =>
                  setCreateForm((current) => ({ ...current, password: event.target.value }))
                }
              />
              <FormValidationMessage
                id="create-user-password-error"
                message={createErrors.password}
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
                  disabled={isBusy}
                  onClick={() =>
                    setPendingSensitiveAction(
                      selectedUser.status === "DISABLED" ? "enable-user" : "disable-user",
                    )
                  }
                >
                  {selectedUser.status === "DISABLED" ? "Enable user" : "Disable user"}
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
                onClick={() => setPendingSensitiveAction("assign-role")}
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
                onClick={() => setPendingSensitiveAction("reset-password")}
              >
                Reset password
              </button>
              {pendingSensitiveAction === "assign-role" ? (
                <ConfirmationDialog
                  id="assign-role-confirmation"
                  title="Confirm role assignment"
                  description={
                    <p>
                      Assign {formatEnum(selectedUserDraft?.roleName ?? "CAMPAIGN_MANAGER")} to{" "}
                      {selectedUser.fullName}? This changes the user&apos;s application access.
                    </p>
                  }
                  confirmLabel="Assign role"
                  busy={assignRoleMutation.isPending}
                  onCancel={() => setPendingSensitiveAction(null)}
                  onConfirm={() => assignRoleMutation.mutate()}
                />
              ) : null}
              {pendingSensitiveAction === "disable-user" ||
              pendingSensitiveAction === "enable-user" ? (
                <ConfirmationDialog
                  id={`${pendingSensitiveAction}-confirmation`}
                  title={
                    pendingSensitiveAction === "enable-user"
                      ? "Confirm user enable"
                      : "Confirm user disable"
                  }
                  description={
                    pendingSensitiveAction === "enable-user" ? (
                      <p>
                        Enable {selectedUser.fullName}? This restores the user&apos;s access using
                        their assigned roles.
                      </p>
                    ) : (
                      <p>
                        Disable {selectedUser.fullName}? This prevents the user from accessing the
                        platform until an admin restores access.
                      </p>
                    )
                  }
                  confirmLabel={
                    pendingSensitiveAction === "enable-user" ? "Enable user" : "Disable user"
                  }
                  busy={accountStatusMutation.isPending}
                  onCancel={() => setPendingSensitiveAction(null)}
                  onConfirm={() => accountStatusMutation.mutate()}
                />
              ) : null}
              {pendingSensitiveAction === "reset-password" ? (
                <ConfirmationDialog
                  id="reset-password-confirmation"
                  title="Confirm password reset"
                  description={
                    <p>
                      Reset the password for {selectedUser.fullName}? Share the new temporary
                      password only through an approved secure channel.
                    </p>
                  }
                  confirmLabel="Reset password"
                  busy={resetPasswordMutation.isPending}
                  onCancel={() => setPendingSensitiveAction(null)}
                  onConfirm={() => resetPasswordMutation.mutate()}
                />
              ) : null}
            </div>
          )}
        </section>
      </div>

      <section className="panel" aria-labelledby="users-table-heading">
        <div className="section-heading">
          <h2 id="users-table-heading">Employee accounts</h2>
          <span>{usersQuery.isLoading ? "Loading users" : `${users.length} records`}</span>
        </div>
        <table aria-labelledby="users-table-heading">
          <caption className="sr-only">
            Employee accounts table with name, email, status, roles, and last login.
          </caption>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Email</th>
              <th scope="col">Status</th>
              <th scope="col">Roles</th>
              <th scope="col">Last login</th>
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

function validateCreateUserForm(form: CreateUserPayload): CreateUserFormErrors {
  const errors: CreateUserFormErrors = {};

  if (form.fullName.trim().length === 0) {
    errors.fullName = "Full name is required.";
  }

  if (form.email.trim().length === 0) {
    errors.email = "Email is required.";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = "Enter a valid email address.";
  }

  if (form.password.length === 0) {
    errors.password = "Temporary password is required.";
  } else if (form.password.length < 8) {
    errors.password = "Temporary password must be at least 8 characters.";
  }

  return errors;
}

function hasCreateUserFormErrors(errors: CreateUserFormErrors) {
  return Object.values(errors).some(Boolean);
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
