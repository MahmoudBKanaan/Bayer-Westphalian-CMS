-- KB item 231: compliance review notes on campaign approval/rejection workflow.
alter table campaigns
    add column if not exists compliance_review_notes text;

comment on column campaigns.compliance_review_notes is
    'Optional Compliance Officer review notes captured during approve/reject (item 231).';
