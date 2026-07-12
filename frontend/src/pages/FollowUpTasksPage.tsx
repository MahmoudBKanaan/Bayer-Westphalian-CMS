import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  assignFollowUpTask,
  createFollowUpTask,
  type AssignFollowUpTaskInput,
  type CreateFollowUpTaskInput,
  emptyFollowUpTaskFilters,
  followUpTaskPriorities,
  followUpTaskStatuses,
  formatFollowUpEnum,
  listFollowUpTasks,
  type FollowUpTaskFilters,
  type FollowUpTaskView,
} from "@/api/followUpTasks";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

export function FollowUpTasksPage() {
  const permissions = usePermissions();
  const canReadFollowUps = permissions.canReadFollowUpTasks();
  const canCreateFollowUps = permissions.canCreateFollowUpTasks();
  const canAssignFollowUps = permissions.canAssignFollowUpTasks();
  const queryClient = useQueryClient();
  const [draftFilters, setDraftFilters] = useState<FollowUpTaskFilters>(emptyFollowUpTaskFilters);
  const [appliedFilters, setAppliedFilters] =
    useState<FollowUpTaskFilters>(emptyFollowUpTaskFilters);
  const [createDraft, setCreateDraft] = useState<CreateFollowUpTaskInput>(emptyCreateTaskInput);
  const [assignDrafts, setAssignDrafts] = useState<Record<string, string>>({});

  const tasksQuery = useQuery({
    queryKey: ["follow-up-tasks", appliedFilters],
    queryFn: () => listFollowUpTasks(appliedFilters),
    enabled: canReadFollowUps,
  });
  const tasks = tasksQuery.data ?? [];
  const errorMessage = tasksQuery.isError
    ? isAuthorizationError(tasksQuery.error)
      ? "You are not authorized to view follow-up tasks."
      : "Follow-up tasks could not be loaded."
    : "";
  const createTaskMutation = useMutation({
    mutationFn: createFollowUpTask,
    onSuccess: () => {
      setCreateDraft(emptyCreateTaskInput);
      void queryClient.invalidateQueries({ queryKey: ["follow-up-tasks"] });
    },
  });
  const assignTaskMutation = useMutation({
    mutationFn: assignFollowUpTask,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["follow-up-tasks"] });
    },
  });

  if (!canReadFollowUps) {
    return (
      <section className="panel">
        <div className="section-heading">
          <h2>Follow-up tasks</h2>
          <span>Assigned follow-up work, priority, due dates, and status</span>
        </div>
        <p className="form-error" role="alert">
          You are not authorized to view follow-up tasks.
        </p>
      </section>
    );
  }

  return (
    <section className="page-stack">
      <div className="panel">
        <div className="section-heading">
          <h2>Follow-up tasks</h2>
          <span>
            {tasksQuery.isLoading
              ? "Loading follow-up tasks"
              : formatCount(tasks.length, "follow-up task")}
          </span>
        </div>
        <FollowUpFiltersPanel
          draftFilters={draftFilters}
          onDraftChange={setDraftFilters}
          onApply={() => setAppliedFilters(normalizeFilters(draftFilters))}
          onReset={() => {
            setDraftFilters(emptyFollowUpTaskFilters);
            setAppliedFilters(emptyFollowUpTaskFilters);
          }}
        />
      </div>

      {canCreateFollowUps ? (
        <div className="panel">
          <div className="section-heading">
            <h2>Create follow-up task</h2>
            <span>Customer, assignee, priority, due date, and campaign context</span>
          </div>
          <CreateFollowUpTaskPanel
            draft={createDraft}
            saving={createTaskMutation.isPending}
            errorMessage={
              createTaskMutation.isError ? "Follow-up task could not be created." : ""
            }
            onDraftChange={setCreateDraft}
            onSubmit={() => createTaskMutation.mutate(createDraft)}
          />
        </div>
      ) : null}

      <div className="panel">
        <div className="section-heading">
          <h2>Task worklist</h2>
          <span>Assignee, priority, status, due date, customer, and campaign context</span>
        </div>
        {tasksQuery.isLoading ? (
          <p className="table-state">Loading follow-up task records.</p>
        ) : null}
        {errorMessage ? (
          <p className="form-error" role="alert">
            {errorMessage}
          </p>
        ) : null}
        {!tasksQuery.isLoading && !tasksQuery.isError && tasks.length === 0 ? (
          <p className="table-state">No follow-up tasks match the current filters.</p>
        ) : null}
        {!tasksQuery.isLoading && !tasksQuery.isError && tasks.length > 0 ? (
          <table aria-label="Follow-up tasks table">
            <thead>
              <tr>
                <th>Task</th>
                <th>Customer</th>
                <th>Campaign</th>
                <th>Assignee</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Due</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task) => (
                <FollowUpTaskRow
                  key={task.id}
                  task={task}
                  canAssign={canAssignFollowUps}
                  assignDraft={assignDrafts[task.id] ?? task.assignedToUserId ?? ""}
                  assigning={assignTaskMutation.isPending}
                  onAssignDraftChange={(assignedTo) =>
                    setAssignDrafts((current) => ({ ...current, [task.id]: assignedTo }))
                  }
                  onAssign={(input) => assignTaskMutation.mutate(input)}
                />
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </section>
  );
}

const emptyCreateTaskInput: CreateFollowUpTaskInput = {
  customerId: "",
  campaignId: "",
  assignedTo: "",
  title: "",
  description: "",
  dueDate: "",
  priority: "MEDIUM",
};

function CreateFollowUpTaskPanel({
  draft,
  saving,
  errorMessage,
  onDraftChange,
  onSubmit,
}: {
  draft: CreateFollowUpTaskInput;
  saving: boolean;
  errorMessage: string;
  onDraftChange: (draft: CreateFollowUpTaskInput) => void;
  onSubmit: () => void;
}) {
  const canSubmit = draft.customerId.trim().length > 0 && draft.title.trim().length > 0 && !saving;

  return (
    <form
      className="form-grid"
      onSubmit={(event) => {
        event.preventDefault();
        if (canSubmit) {
          onSubmit();
        }
      }}
    >
      <label>
        Customer ID
        <input
          aria-label="Follow-up customer ID"
          required
          value={draft.customerId}
          onChange={(event) => onDraftChange({ ...draft, customerId: event.target.value })}
          placeholder="Customer UUID"
        />
      </label>
      <label>
        Task title
        <input
          aria-label="Follow-up task title"
          required
          maxLength={255}
          value={draft.title}
          onChange={(event) => onDraftChange({ ...draft, title: event.target.value })}
          placeholder="Call customer back"
        />
      </label>
      <label>
        Assigned user ID
        <input
          aria-label="Follow-up assigned user ID"
          value={draft.assignedTo}
          onChange={(event) => onDraftChange({ ...draft, assignedTo: event.target.value })}
          placeholder="Optional user UUID"
        />
      </label>
      <label>
        Campaign ID
        <input
          aria-label="Follow-up campaign ID"
          value={draft.campaignId}
          onChange={(event) => onDraftChange({ ...draft, campaignId: event.target.value })}
          placeholder="Optional campaign UUID"
        />
      </label>
      <label>
        Priority
        <select
          aria-label="Create follow-up priority"
          value={draft.priority}
          onChange={(event) =>
            onDraftChange({
              ...draft,
              priority: event.target.value as CreateFollowUpTaskInput["priority"],
            })
          }
        >
          {followUpTaskPriorities.map((priority) => (
            <option key={priority} value={priority}>
              {formatFollowUpEnum(priority)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Due date
        <input
          aria-label="Create follow-up due date"
          type="date"
          value={draft.dueDate}
          onChange={(event) => onDraftChange({ ...draft, dueDate: event.target.value })}
        />
      </label>
      <label className="form-field-wide">
        Description
        <textarea
          aria-label="Follow-up description"
          value={draft.description}
          onChange={(event) => onDraftChange({ ...draft, description: event.target.value })}
          placeholder="Notes for the next conversation"
        />
      </label>
      <div className="form-actions">
        <button type="submit" disabled={!canSubmit}>
          {saving ? "Creating..." : "Create task"}
        </button>
      </div>
      {errorMessage ? (
        <p className="form-error" role="alert">
          {errorMessage}
        </p>
      ) : null}
    </form>
  );
}

function FollowUpFiltersPanel({
  draftFilters,
  onDraftChange,
  onApply,
  onReset,
}: {
  draftFilters: FollowUpTaskFilters;
  onDraftChange: (filters: FollowUpTaskFilters) => void;
  onApply: () => void;
  onReset: () => void;
}) {
  return (
    <div className="form-grid">
      <label>
        Assignee ID
        <input
          aria-label="Follow-up assignee ID"
          value={draftFilters.assignedTo}
          onChange={(event) => onDraftChange({ ...draftFilters, assignedTo: event.target.value })}
          placeholder="Filter by user UUID"
        />
      </label>
      <label>
        Priority
        <select
          aria-label="Follow-up priority"
          value={draftFilters.priority}
          onChange={(event) =>
            onDraftChange({
              ...draftFilters,
              priority: event.target.value as FollowUpTaskFilters["priority"],
            })
          }
        >
          <option value="ALL">All priorities</option>
          {followUpTaskPriorities.map((priority) => (
            <option key={priority} value={priority}>
              {formatFollowUpEnum(priority)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Status
        <select
          aria-label="Follow-up status"
          value={draftFilters.status}
          onChange={(event) =>
            onDraftChange({
              ...draftFilters,
              status: event.target.value as FollowUpTaskFilters["status"],
            })
          }
        >
          <option value="ALL">All statuses</option>
          {followUpTaskStatuses.map((status) => (
            <option key={status} value={status}>
              {formatFollowUpEnum(status)}
            </option>
          ))}
        </select>
      </label>
      <label>
        Due from
        <input
          aria-label="Follow-up due date from"
          type="date"
          value={draftFilters.dueDateFrom}
          onChange={(event) => onDraftChange({ ...draftFilters, dueDateFrom: event.target.value })}
        />
      </label>
      <label>
        Due to
        <input
          aria-label="Follow-up due date to"
          type="date"
          value={draftFilters.dueDateTo}
          onChange={(event) => onDraftChange({ ...draftFilters, dueDateTo: event.target.value })}
        />
      </label>
      <div className="form-actions">
        <button type="button" onClick={onApply}>
          Apply filters
        </button>
        <button type="button" className="secondary-button" onClick={onReset}>
          Reset
        </button>
      </div>
    </div>
  );
}

function FollowUpTaskRow({
  task,
  canAssign,
  assignDraft,
  assigning,
  onAssignDraftChange,
  onAssign,
}: {
  task: FollowUpTaskView;
  canAssign: boolean;
  assignDraft: string;
  assigning: boolean;
  onAssignDraftChange: (assignedTo: string) => void;
  onAssign: (input: AssignFollowUpTaskInput) => void;
}) {
  const normalizedAssignee = assignDraft.trim();
  const canSubmitAssignment = normalizedAssignee.length > 0 && !assigning;

  return (
    <tr>
      <td>
        <span className="table-primary-text">{task.title}</span>
        <span className="table-secondary-text">
          {task.description == null || task.description.trim().length === 0
            ? "No description"
            : task.description}
        </span>
      </td>
      <td>
        <span className="table-primary-text">{task.customerFullName}</span>
        <span className="table-secondary-text">{task.customerId}</span>
      </td>
      <td>
        <span className="table-primary-text">{task.campaignName ?? "No campaign"}</span>
        <span className="table-secondary-text">{task.campaignId ?? "No campaign id"}</span>
      </td>
      <td>
        <span className="table-primary-text">
          {task.assignedToFullName ?? task.assignedToUserId ?? "Unassigned"}
        </span>
        {canAssign ? (
          <form
            className="inline-form"
            onSubmit={(event) => {
              event.preventDefault();
              if (canSubmitAssignment) {
                onAssign({ taskId: task.id, assignedTo: normalizedAssignee });
              }
            }}
          >
            <label>
              Assign to user ID
              <input
                aria-label={`Assign follow-up ${task.title}`}
                value={assignDraft}
                onChange={(event) => onAssignDraftChange(event.target.value)}
                placeholder="User UUID"
              />
            </label>
            <button type="submit" disabled={!canSubmitAssignment}>
              Assign
            </button>
          </form>
        ) : null}
      </td>
      <td>
        <StatusBadge value={formatFollowUpEnum(task.priority)} />
      </td>
      <td>
        <StatusBadge value={formatFollowUpEnum(task.status)} />
        {task.completedAt == null ? null : (
          <span className="table-secondary-text">{formatDateTime(task.completedAt)}</span>
        )}
      </td>
      <td>{formatDate(task.dueDate)}</td>
    </tr>
  );
}

function normalizeFilters(filters: FollowUpTaskFilters): FollowUpTaskFilters {
  return {
    customerId: filters.customerId.trim(),
    assignedTo: filters.assignedTo.trim(),
    priority: filters.priority,
    status: filters.status,
    dueDateFrom: filters.dueDateFrom,
    dueDateTo: filters.dueDateTo,
  };
}

function formatDate(value: string | null) {
  if (value == null || value.trim().length === 0) {
    return "No due date";
  }
  return new Intl.DateTimeFormat("en", { dateStyle: "medium" }).format(new Date(value));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatCount(count: number, noun: string) {
  return `${count} ${count === 1 ? noun : `${noun}s`}`;
}
