alter table user_roles
    add column assigned_by uuid references users (id) on delete set null;

create index user_roles_assigned_by_idx on user_roles (assigned_by);
