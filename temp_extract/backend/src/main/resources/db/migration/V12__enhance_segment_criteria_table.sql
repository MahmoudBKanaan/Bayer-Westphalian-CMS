alter table segment_criteria
    add constraint segment_criteria_field_name_not_blank
        check (length(trim(field_name)) > 0),
    add constraint segment_criteria_value_not_blank
        check (length(trim(value)) > 0),
    add constraint segment_criteria_logical_group_not_blank
        check (logical_group is null or length(trim(logical_group)) > 0);

create index if not exists idx_segment_criteria_segment
    on segment_criteria (segment_id);
create index if not exists idx_segment_criteria_field_name
    on segment_criteria (field_name);
create index if not exists idx_segment_criteria_operator
    on segment_criteria (operator);
create index if not exists idx_segment_criteria_segment_field
    on segment_criteria (segment_id, field_name);
