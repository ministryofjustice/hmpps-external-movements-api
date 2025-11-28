create index if not exists idx_person_summary_name on person_summary (lower(last_name), lower(first_name))
;