alter table product_change_requests
    add constraint product_change_requests_description_not_blank
        check (length(trim(description)) > 0);

create index if not exists idx_product_change_requests_status
    on product_change_requests (status);
create index if not exists idx_product_change_requests_product
    on product_change_requests (product_id);
create index if not exists idx_product_change_requests_requested_by
    on product_change_requests (requested_by);
create index if not exists idx_product_change_requests_product_status
    on product_change_requests (product_id, status);
