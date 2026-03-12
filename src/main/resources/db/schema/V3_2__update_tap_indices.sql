drop index if exists idx_person_summary_prison_code;
create index concurrently if not exists idx_person_summary_prison_code_identifier on person_summary (prison_code, person_identifier);
create index concurrently if not exists idx_tap_authorisation_status_end_prison on tap.authorisation (status_id, "end", prison_code);
create index concurrently if not exists idx_tap_occurrence_prison_start_end on tap.occurrence (prison_code, start, "end");
create index concurrently if not exists idx_tap_occurrence_prison_end on tap.occurrence (prison_code, "end");
