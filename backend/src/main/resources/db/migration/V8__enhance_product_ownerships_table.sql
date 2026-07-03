alter table product_ownerships
    add constraint product_ownerships_expiration_after_start
        check (expiration_date is null or expiration_date >= start_date);

create index if not exists idx_product_ownership_expiration
    on product_ownerships (expiration_date);
create index if not exists idx_product_ownerships_product
    on product_ownerships (product_id);
create index if not exists idx_product_ownerships_status
    on product_ownerships (status);
create index if not exists idx_product_ownerships_customer_status
    on product_ownerships (customer_id, status);
