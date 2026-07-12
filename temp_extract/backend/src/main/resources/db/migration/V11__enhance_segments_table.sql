alter table segments
    add constraint segments_name_not_blank
        check (length(trim(name)) > 0),
    add constraint segments_updated_at_after_created_at
        check (updated_at >= created_at);

create index if not exists idx_segments_owner_user
    on segments (owner_user_id);
create index if not exists idx_segments_visibility
    on segments (visibility);
create index if not exists idx_segments_owner_visibility
    on segments (owner_user_id, visibility);
create index if not exists idx_segments_name
    on segments (name);
