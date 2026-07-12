alter table products
    add constraint products_price_non_negative
        check (price is null or price >= 0),
    add constraint products_duration_positive
        check (duration_months is null or duration_months > 0);

create index if not exists idx_products_type on products (product_type);
create index if not exists idx_products_active on products (active);
create index if not exists idx_products_name on products (name);
create index if not exists idx_products_deleted_at on products (deleted_at);
