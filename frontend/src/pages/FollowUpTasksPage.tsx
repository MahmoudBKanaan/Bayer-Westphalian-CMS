import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { isAuthorizationError } from "@/api/client";
import {
  assignFollowUpTask,
  completeFollowUpTask,
  createFollowUpTask,
  type AssignFollowUpTaskInput,
  type CreateFollowUpTaskInput,
  emptyFollowUpTaskFilters,
  type FollowUpAssigneeOption,
  followUpTaskPriorities,
  followUpTaskStatuses,
  formatFollowUpEnum,
  listFollowUpAssigneeOptions,
  listFollowUpTasks,
  type FollowUpTaskFilters,
  type FollowUpTaskView,
} from "@/api/followUpTasks";
import { useAuth } from "@/auth/AuthProvider";
import { StatusBadge } from "@/components/StatusBadge";
import { usePermissions } from "@/features/auth/usePermissions";

export function FollowUpTasksPage() {
  const permissions = usePermissions();
  const { user } = useAuth();
  const canReadFollowUps = permissions.canReadFollowUpTasks();
  const canCreateFollowUps = permissions.canCreateFollowUpTasks();
  /** Only Admin / Campaign Manager may assign tasks to Customer Service Agents. */
  const canAssignFollowUps = permissions.canAssignFollowUpTasks();
  const canCompleteFollowUps = permissions.canCompleteFollowUpTasks();
  const isManagerWorklist = canAssignFollowUps;
  const queryClient = useQueryClient();
  const [draftFilters, setDraftFilters] = useState<FollowUpTaskFilters>(emptyFollowUpTaskFilters);
  const [appliedFilters, setAppliedFilters] =
    useState<FollowUpTaskFilters>(emptyFollowUpTaskFilters);
  const [createDraft, setCreateDraft] = useState<CreateFollowUpTaskInput>(emptyCreateTaskInput);
  const [assignDrafts, setAssignDrafts] = useState<Record<string, string>>({});
  /** Filters and create form start minimized; expand via the card header button. */
  const [filtersExpanded, setFiltersExpanded] = useState(false);
  const [createExpanded, setCreateExpanded] = useState(false);

  // Agents only see their Assigned Worklist (server also enforces assigned_to = current user).
  const effectiveFilters: FollowUpTaskFilters = isManagerWorklist
    ? appliedFilters
    : {
        ...appliedFilters,
        assignedTo: user?.id ?? appliedFilters.assignedTo,
      };

  const tasksQuery = useQuery({
    queryKey: ["follow-up-tasks", effectiveFilters, isManagerWorklist ? "manager" : "assigned"],
    queryFn: () => listFollowUpTasks(effectiveFilters),
    enabled: canReadFollowUps && (isManagerWorklist || Boolean(user?.id)),
  });
  const assigneeOptionsQuery = useQuery({
    queryKey: ["follow-up-assignee-options"],
    queryFn: listFollowUpAssigneeOptions,
    enabled: canAssignFollowUps,
  });
  const assigneeOptions = assigneeOptionsQuery.data ?? [];
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
      setCreateExpanded(false);
      void queryClient.invalidateQueries({ queryKey: ["follow-up-tasks"] });
    },
  });
  const assignTaskMutation = useMutation({
    mutationFn: assignFollowUpTask,
    onSuccess: (_data, variables) => {
      setAssignDrafts((current) => {
        const next = { ...current };
        delete next[variables.taskId];
        return next;
      });
      void queryClient.invalidateQueries({ queryKey: ["follow-up-tasks"] });
    },
  });
  const completeTaskMutation = useMutation({
    mutationFn: completeFollowUpTask,
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

  const worklistTitle = isManagerWorklist ? "Task worklist" : "Assigned worklist";
  const worklistHint = isManagerWorklist
    ? "All follow-ups — managers assign tasks to Customer Service Agents"
    : "Only tasks assigned to you";

  return (
    <section className="page-stack">
      <CollapsiblePanel
        title="Filters"
        summary={
          filtersExpanded
            ? isManagerWorklist
              ? "Filter by assignee, priority, status, and due date"
              : "Filter your assigned tasks by priority, status, and due date"
            : filterSummary(effectiveFilters, isManagerWorklist)
        }
        expanded={filtersExpanded}
        onToggle={() => setFiltersExpanded((open) => !open)}
        expandLabel="Expand filters"
        collapseLabel="Minimize filters"
      >
        <FollowUpFiltersPanel
          draftFilters={draftFilters}
          showAssigneeFilter={isManagerWorklist}
          onDraftChange={setDraftFilters}
          onApply={() => {
            setAppliedFilters(normalizeFilters(draftFilters));
            setFiltersExpanded(false);
          }}
          onReset={() => {
            setDraftFilters(emptyFollowUpTaskFilters);
            setAppliedFilters(emptyFollowUpTaskFilters);
          }}
        />
      </CollapsiblePanel>

      {canCreateFollowUps ? (
        <CollapsiblePanel
          title="Create follow-up task"
          summary={
            createExpanded
              ? isManagerWorklist
                ? "Customer, Customer Service Agent, priority, due date, and campaign context"
                : "Task is automatically assigned to you"
              : "Collapsed — expand to create a new task"
          }
          expanded={createExpanded}
          onToggle={() => setCreateExpanded((open) => !open)}
          expandLabel="Expand create task"
          collapseLabel="Minimize create task"
        >
          <CreateFollowUpTaskPanel
            draft={createDraft}
            canChooseAssignee={isManagerWorklist}
            assigneeOptions={assigneeOptions}
            assigneesLoading={assigneeOptionsQuery.isLoading}
            currentUserName={user?.fullName ?? "you"}
            saving={createTaskMutation.isPending}
            errorMessage={
              createTaskMutation.isError ? "Follow-up task could not be created." : ""
            }
            onDraftChange={setCreateDraft}
            onSubmit={() =>
              createTaskMutation.mutate(
                isManagerWorklist
                  ? createDraft
                  : { ...createDraft, assignedTo: user?.id ?? "" },
              )
            }
          />
        </CollapsiblePanel>
      ) : null}

      <div className="panel">
        <div className="section-heading">
          <h2>{worklistTitle}</h2>
          <span>
            {tasksQuery.isLoading
              ? "Loading follow-up tasks"
              : `${formatCount(tasks.length, "follow-up task")} · ${worklistHint}`}
          </span>
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
                  currentUserId={user?.id}
                  canAssign={canAssignFollowUps}
                  canCompleteRole={canCompleteFollowUps}
                  isManager={isManagerWorklist}
                  assigneeOptions={assigneeOptions}
                  assigneesLoading={assigneeOptionsQuery.isLoading}
                  assignDraft={assignDrafts[task.id] ?? ""}
                  assigning={assignTaskMutation.isPending}
                  completing={
                    completeTaskMutation.isPending &&
                    completeTaskMutation.variables === task.id
                  }
                  assignError={
                    assignTaskMutation.isError &&
                    assignTaskMutation.variables?.taskId === task.id
                      ? isAuthorizationError(assignTaskMutation.error)
                        ? "You are not authorized to assign this task."
                        : "Follow-up task could not be assigned."
                      : ""
                  }
                  completeError={
                    completeTaskMutation.isError &&
                    completeTaskMutation.variables === task.id
                      ? isAuthorizationError(completeTaskMutation.error)
                        ? "You are not authorized to complete this task."
                        : "Follow-up task could not be completed."
                      : ""
                  }
                  onAssignDraftChange={(assignedTo) =>
                    setAssignDrafts((current) => ({ ...current, [task.id]: assignedTo }))
                  }
                  onAssign={(input) => assignTaskMutation.mutate(input)}
                  onComplete={(taskId) => completeTaskMutation.mutate(taskId)}
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
  canChooseAssignee,
  assigneeOptions,
  assigneesLoading,
  currentUserName,
  saving,
  errorMessage,
  onDraftChange,
  onSubmit,
}: {
  draft: CreateFollowUpTaskInput;
  canChooseAssignee: boolean;
  assigneeOptions: FollowUpAssigneeOption[];
  assigneesLoading: boolean;
  currentUserName: string;
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
      {canChooseAssignee ? (
        <label>
          Assign to Customer Service Agent
          <CustomerServiceAgentSelect
            ariaLabel="Follow-up assigned Customer Service Agent"
            value={draft.assignedTo}
            options={assigneeOptions}
            loading={assigneesLoading}
            allowEmpty
            emptyLabel="Unassigned (optional)"
            onChange={(assignedTo) => onDraftChange({ ...draft, assignedTo })}
          />
        </label>
      ) : (
        <p className="table-secondary-text" data-testid="follow-up-auto-assign-notice">
          This task will be automatically assigned to you ({currentUserName}). Only managers can
          assign follow-ups to Customer Service Agents.
        </p>
      )}
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
  showAssigneeFilter,
  onDraftChange,
  onApply,
  onReset,
}: {
  draftFilters: FollowUpTaskFilters;
  showAssigneeFilter: boolean;
  onDraftChange: (filters: FollowUpTaskFilters) => void;
  onApply: () => void;
  onReset: () => void;
}) {
  return (
    <div className="form-grid">
      {showAssigneeFilter ? (
        <label>
          Assignee ID
          <input
            aria-label="Follow-up assignee ID"
            value={draftFilters.assignedTo}
            onChange={(event) =>
              onDraftChange({ ...draftFilters, assignedTo: event.target.value })
            }
            placeholder="Filter by user UUID"
          />
        </label>
      ) : null}
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
  currentUserId,
  canAssign,
  canCompleteRole,
  isManager,
  assigneeOptions,
  assigneesLoading,
  assignDraft,
  assigning,
  completing,
  assignError,
  completeError,
  onAssignDraftChange,
  onAssign,
  onComplete,
}: {
  task: FollowUpTaskView;
  currentUserId: string | undefined;
  canAssign: boolean;
  canCompleteRole: boolean;
  isManager: boolean;
  assigneeOptions: FollowUpAssigneeOption[];
  assigneesLoading: boolean;
  assignDraft: string;
  assigning: boolean;
  completing: boolean;
  assignError: string;
  completeError: string;
  onAssignDraftChange: (assignedTo: string) => void;
  onAssign: (input: AssignFollowUpTaskInput) => void;
  onComplete: (taskId: string) => void;
}) {
  const normalizedAssignee = assignDraft.trim();
  // Enable whenever a real agent is chosen (including re-assign to the same CSA).
  // Previously we disabled when draft === current assignee, which blocked Assign for
  // almost every demo row (pre-filled to the only Customer Service Agent).
  const selectedAgentIsValid = assigneeOptions.some((agent) => agent.id === normalizedAssignee);
  const canSubmitAssignment =
    selectedAgentIsValid && !assigning && !assigneesLoading && assigneeOptions.length > 0;
  const isReassign =
    task.assignedToUserId != null &&
    task.assignedToUserId.length > 0 &&
    normalizedAssignee === task.assignedToUserId;
  const isAssignee =
    currentUserId != null &&
    task.assignedToUserId != null &&
    task.assignedToUserId === currentUserId;
  const isOpenWork =
    task.status !== "COMPLETED" && task.status !== "CANCELLED" && task.completedAt == null;
  /** Assignee or manager may tag the task complete. */
  const canCompleteThisTask = canCompleteRole && isOpenWork && (isManager || isAssignee);

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
              Assign to Customer Service Agent
              <CustomerServiceAgentSelect
                ariaLabel={`Assign follow-up ${task.title}`}
                value={assignDraft}
                options={assigneeOptions}
                loading={assigneesLoading}
                allowEmpty
                emptyLabel="Select a Customer Service Agent"
                onChange={onAssignDraftChange}
              />
            </label>
            <button type="submit" disabled={!canSubmitAssignment}>
              {assigning ? "Assigning..." : isReassign ? "Reassign" : "Assign"}
            </button>
            {assignError ? (
              <p className="form-error" role="alert">
                {assignError}
              </p>
            ) : null}
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
        {canCompleteThisTask ? (
          <div className="inline-form">
            <button
              type="button"
              className="secondary-button"
              aria-label={`Mark follow-up ${task.title} complete`}
              disabled={completing}
              onClick={() => onComplete(task.id)}
            >
              {completing ? "Completing..." : "Mark complete"}
            </button>
            {completeError ? (
              <p className="form-error" role="alert">
                {completeError}
              </p>
            ) : null}
          </div>
        ) : null}
      </td>
      <td>{formatDate(task.dueDate)}</td>
    </tr>
  );
}

function CollapsiblePanel({
  title,
  summary,
  expanded,
  onToggle,
  expandLabel,
  collapseLabel,
  children,
}: {
  title: string;
  summary: string;
  expanded: boolean;
  onToggle: () => void;
  expandLabel: string;
  collapseLabel: string;
  children: ReactNode;
}) {
  const contentId = `collapsible-${title.toLowerCase().replace(/\s+/g, "-")}`;

  return (
    <div className={`panel collapsible-panel${expanded ? " is-expanded" : " is-collapsed"}`}>
      <div className="section-heading collapsible-panel-header">
        <div className="collapsible-panel-titles">
          <h2 id={`${contentId}-title`}>{title}</h2>
          <span>{summary}</span>
        </div>
        <button
          type="button"
          className="secondary-button collapsible-panel-toggle"
          aria-expanded={expanded}
          aria-controls={contentId}
          onClick={onToggle}
        >
          {expanded ? collapseLabel : expandLabel}
        </button>
      </div>
      {expanded ? (
        <div
          id={contentId}
          className="collapsible-panel-body"
          role="region"
          aria-labelledby={`${contentId}-title`}
        >
          {children}
        </div>
      ) : null}
    </div>
  );
}

function filterSummary(filters: FollowUpTaskFilters, isManagerWorklist: boolean) {
  const parts: string[] = [];
  if (isManagerWorklist && filters.assignedTo.trim().length > 0) {
    parts.push("assignee set");
  }
  if (!isManagerWorklist) {
    parts.push("my assigned tasks");
  }
  if (filters.priority !== "ALL") {
    parts.push(formatFollowUpEnum(filters.priority));
  }
  if (filters.status !== "ALL") {
    parts.push(formatFollowUpEnum(filters.status));
  }
  if (filters.dueDateFrom.trim().length > 0 || filters.dueDateTo.trim().length > 0) {
    parts.push("due date range");
  }
  if (parts.length === 0 || (parts.length === 1 && parts[0] === "my assigned tasks")) {
    return isManagerWorklist
      ? "Collapsed — expand to filter the worklist"
      : "Collapsed — expand to filter your assigned worklist";
  }
  return `Active: ${parts.join(" · ")}`;
}

function CustomerServiceAgentSelect({
  ariaLabel,
  value,
  options,
  loading,
  allowEmpty,
  emptyLabel,
  onChange,
}: {
  ariaLabel: string;
  value: string;
  options: FollowUpAssigneeOption[];
  loading: boolean;
  allowEmpty: boolean;
  emptyLabel: string;
  onChange: (value: string) => void;
}) {
  return (
    <select
      aria-label={ariaLabel}
      value={value}
      disabled={loading || options.length === 0}
      onChange={(event) => onChange(event.target.value)}
    >
      {allowEmpty || value.length === 0 ? (
        <option value="">{loading ? "Loading agents..." : emptyLabel}</option>
      ) : null}
      {options.map((agent) => (
        <option key={agent.id} value={agent.id}>
          {agent.fullName} ({agent.email})
        </option>
      ))}
    </select>
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
