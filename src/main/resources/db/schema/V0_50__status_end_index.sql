create index if not exists idx_tap_authorisation_status_end on temporary_absence_authorisation (status_id, "end")
;

create index if not exists idx_tap_occurrence_status_end on temporary_absence_occurrence (status_id, "end")
;