-- Item 537: anchor for configurable uninterested exclusion period.
-- status_changed_at records when customer status last changed (e.g. marked UNINTERESTED).

alter table customers
    add column if not exists status_changed_at timestamptz;

update customers
set status_changed_at = coalesce(updated_at, created_at)
where status_changed_at is null;

create index if not exists idx_customers_status_changed_at on customers (status_changed_at);
